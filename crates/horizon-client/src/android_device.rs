//! Conservative first-launch defaults for Galaxy S25 Ultra (SM-S938 variants).
//! Kept independent of Android/JNI so matching and persistence can be tested on a host.
use std::{fs, io::Write, path::Path};

pub fn is_s25_ultra(manufacturer: &str, model: &str) -> bool {
    manufacturer.trim().eq_ignore_ascii_case("samsung")
        && model.trim().to_ascii_uppercase().starts_with("SM-S938")
}

const OPTIONS: &str = "renderDistance:8\nsimulationDistance:5\nmaxFps:60\nenableVsync:true\nrenderClouds:\"false\"\nparticles:1\nentityDistanceScaling:0.75\n";
const GRAPHICS: &str = "resolutionScale:75\nworkers:4\nchunkUpdates:4\nuploadBudgetKiB:4096\nframePacing:efficient\ngraphicsApi:auto\nhdr:false\nfxaa:true\n";

/// Create missing settings only: upgrades and later launches preserve player choices.
pub fn configure(home: &Path, manufacturer: &str, model: &str) -> std::io::Result<bool> {
    if !is_s25_ultra(manufacturer, model) {
        return Ok(false);
    }
    fs::create_dir_all(home)?;
    for (name, contents) in [("options.txt", OPTIONS), ("horizon-graphics.txt", GRAPHICS)] {
        match fs::OpenOptions::new()
            .write(true)
            .create_new(true)
            .open(home.join(name))
        {
            Ok(mut file) => file.write_all(contents.as_bytes())?,
            Err(e) if e.kind() == std::io::ErrorKind::AlreadyExists => {}
            Err(e) => return Err(e),
        }
    }
    Ok(true)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn recognizes_regional_ultra_models_only() {
        for model in [
            "SM-S938B",
            "SM-S938B/DS",
            "SM-S938U",
            "SM-S938U1",
            "SM-S938W",
            "SM-S938N",
            "SM-S9380",
        ] {
            assert!(is_s25_ultra("Samsung", model));
        }
        for (manufacturer, model) in [
            ("samsung", "SM-S931B"),
            ("samsung", "SM-S936B"),
            ("other", "SM-S938B"),
        ] {
            assert!(!is_s25_ultra(manufacturer, model));
        }
    }

    #[test]
    fn preserves_player_settings_and_ignores_other_devices() {
        let home = std::env::temp_dir().join(format!("horizon-device-test-{}", std::process::id()));
        let _ = fs::remove_dir_all(&home);
        assert!(!configure(&home, "samsung", "SM-S931B").unwrap());
        assert!(!home.exists());
        configure(&home, "samsung", "SM-S938B").unwrap();
        assert_eq!(
            fs::read_to_string(home.join("options.txt")).unwrap(),
            OPTIONS
        );
        fs::write(home.join("options.txt"), "maxFps:90\n").unwrap();
        fs::write(home.join("horizon-graphics.txt"), "resolutionScale:100\n").unwrap();
        configure(&home, "samsung", "SM-S938B").unwrap();
        assert_eq!(
            fs::read_to_string(home.join("options.txt")).unwrap(),
            "maxFps:90\n"
        );
        assert_eq!(
            fs::read_to_string(home.join("horizon-graphics.txt")).unwrap(),
            "resolutionScale:100\n"
        );
        fs::remove_dir_all(home).unwrap();
    }
}
