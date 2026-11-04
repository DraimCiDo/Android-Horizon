//! The part of preparation that needs no Java: everything read straight out of the pinned client
//! JAR. Used on a device (Android's first-run download), where there is no JVM; the files are the
//! same bytes `prepare` writes for them.
use crate::*;

/// The files and folders [`from_client_jar`] writes, relative to the resources folder. Everything
/// else a prepared folder holds comes from the vanilla exporters (Java).
pub const JAR_OUTPUTS: &[&str] = &[
    "assets",
    "data",
    "pack.png",
    "block-tags.json",
    "damage-types.json",
    "damage-tags.json",
    "worldgen/structure",
    "worldgen/noises.json",
    "worldgen/densities.json",
    "worldgen/rules.json",
    "worldgen/conditions.json",
    "worldgen/settings.json",
    "worldgen/carvers.json",
    "worldgen/biomes.json",
    "worldgen/world_presets.json",
    "worldgen/features.json",
    "worldgen/placed_features.json",
    "worldgen/state_providers.json",
    "worldgen/biome_tags.json",
    "worldgen/feature_tags.json",
    "worldgen/block_tags.json",
    "worldgen/fluid_tags.json",
    "worldgen/processor_lists.json",
    "worldgen/template_pools.json",
    "worldgen/structures.json",
    "worldgen/structure_sets.json",
];

/// Install a complete prepared-resource archive embedded in an APK. A failed or
/// interrupted extraction stays unready, so the next launch can safely retry.
pub fn install_bundle<R: std::io::Read + std::io::Seek>(
    reader: R,
    root: &Path,
    progress: &dyn Fn(usize, usize),
) -> Result<(), String> {
    const MARKER: &str = ".horizon-setup-incomplete";
    let mut archive = zip::ZipArchive::new(reader).map_err(|e| e.to_string())?;
    for name in ["horizon-resources.json", "facts.json"] {
        archive
            .by_name(name)
            .map_err(|_| format!("bundle lacks {name}"))?;
    }
    mkdir(root)?;
    write(&root.join(MARKER), b"")?;
    let total = archive.len();
    for i in 0..total {
        let mut entry = archive.by_index(i).map_err(|e| e.to_string())?;
        let relative = entry
            .enclosed_name()
            .ok_or("bundle contains an unsafe path")?;
        if relative == Path::new(MARKER)
            || entry.unix_mode().is_some_and(|m| m & 0o170000 == 0o120000)
        {
            return Err("bundle contains an unsafe entry".into());
        }
        let target = root.join(relative);
        if entry.is_dir() {
            mkdir(&target)?;
        } else {
            mkdir(target.parent().ok_or("bundle entry has no parent")?)?;
            let mut file = fs::File::create(&target).map_err(|e| e.to_string())?;
            std::io::copy(&mut entry, &mut file).map_err(|e| e.to_string())?;
        }
        progress(i + 1, total);
    }
    fs::remove_file(root.join(MARKER)).map_err(|e| e.to_string())?;
    Ok(())
}

/// Writes the client JAR's assets, data and `pack.png`, the block and damage tag bundles and the
/// world generator's JSON bundles and structure templates into `root`. Returns the number of
/// resource files (the manifest's `resource_files`). Indexed assets already in `root` with the
/// asset index's SHA-1 (`indexed`: name -> sha1) are kept, as `prepare` keeps them.
pub fn from_client_jar(
    client: &Path,
    root: &Path,
    indexed: &dyn Fn(&str) -> Option<String>,
    progress: &dyn Fn(usize, usize),
) -> Result<usize, String> {
    let mut jar = open_zip(client)?;
    let names = zip_names(&jar)?;
    let total = names.len();
    let mut count = 0;
    for (i, name) in names.iter().enumerate() {
        if i % 256 == 0 {
            progress(i, total);
        }
        if !(name.starts_with("assets/") || name.starts_with("data/") || name == "pack.png")
            || name.ends_with('/')
        {
            continue;
        }
        let relative = member_path(name, "Invalid resource path")?;
        count += 1;
        let target = root.join(&relative);
        if let Some(asset) = name.strip_prefix("assets/") {
            if let Some(hash) = indexed(asset) {
                if target.is_file() && digest(&target, "sha1")? == hash {
                    continue;
                }
            }
        }
        let member = jar.by_name(name).map_err(|e| e.to_string())?;
        let size = member.size();
        drop(member);
        if target.is_file() && fs::metadata(&target).map_err(|e| e.to_string())?.len() == size {
            // Same size: compare the CRC as `prepare` does.
            let crc = jar.by_name(name).map_err(|e| e.to_string())?.crc32();
            let data = read(&target)?;
            if crc32fast::hash(&data) == crc {
                continue;
            }
        }
        mkdir(target.parent().unwrap())?;
        write(&target, &zip_bytes(&mut jar, name)?)?;
    }
    progress(total, total);
    write_json(
        &root.join("block-tags.json"),
        &json_resources(&mut jar, "data/minecraft/tags/block/")?,
    )?;
    write_json(
        &root.join("damage-types.json"),
        &json_resources(&mut jar, "data/minecraft/damage_type/")?,
    )?;
    write_json(
        &root.join("damage-tags.json"),
        &json_resources(&mut jar, "data/minecraft/tags/damage_type/")?,
    )?;
    let out = root.join("worldgen");
    mkdir(&out)?;
    let prefix = "data/minecraft/structure/";
    for name in &names {
        if name.starts_with(prefix) && name.ends_with(".nbt") {
            let relative = member_path(&name[prefix.len()..], "Invalid structure path")?;
            let data = zip_bytes(&mut jar, name)?;
            write_if_changed(&out.join("structure/minecraft").join(relative), &data)?;
        }
    }
    for (name, path) in WORLDGEN_BUNDLES {
        let resources = json_resources(&mut jar, &format!("data/minecraft/{path}/"))?;
        if resources.as_object().is_none_or(<[_]>::is_empty) {
            return Err(format!("The client JAR has no {path}"));
        }
        write_text(
            &out.join(format!("{name}.json")),
            &pyjson::dumps(&resources, &pyjson::COMPACT),
        )?;
    }
    Ok(count)
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::{Cursor, Write};

    fn archive(extra: &str) -> Cursor<Vec<u8>> {
        let mut zip = zip::ZipWriter::new(Cursor::new(Vec::new()));
        for name in ["horizon-resources.json", "facts.json", extra] {
            zip.start_file(name, zip::write::SimpleFileOptions::default())
                .unwrap();
            zip.write_all(b"owned test fixture").unwrap();
        }
        zip.finish().unwrap()
    }

    #[test]
    fn complete_bundle_extracts_and_becomes_ready() {
        let root =
            std::env::temp_dir().join(format!("horizon-bundle-complete-{}", std::process::id()));
        let _ = fs::remove_dir_all(&root);
        install_bundle(archive("assets/test.txt"), &root, &|_, _| {}).unwrap();
        assert_eq!(
            fs::read(root.join("assets/test.txt")).unwrap(),
            b"owned test fixture"
        );
        assert!(root.join("facts.json").is_file());
        assert!(!root.join(".horizon-setup-incomplete").exists());
        fs::remove_dir_all(root).unwrap();
    }

    #[test]
    fn unsafe_bundle_stays_unready_and_cannot_escape_resources() {
        let parent =
            std::env::temp_dir().join(format!("horizon-bundle-invalid-{}", std::process::id()));
        let _ = fs::remove_dir_all(&parent);
        let root = parent.join("resources");
        assert!(install_bundle(archive("../escaped.txt"), &root, &|_, _| {}).is_err());
        assert!(root.join(".horizon-setup-incomplete").is_file());
        assert!(!parent.join("escaped.txt").exists());
        fs::remove_dir_all(parent).unwrap();
    }
}
