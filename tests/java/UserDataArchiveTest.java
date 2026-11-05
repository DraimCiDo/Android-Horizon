package dev.horizonlab.horizon;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Executed on a real JVM; checks archive bytes and restored files, not Android mocks. */
public final class UserDataArchiveTest {
    private static final UserDataArchive.Progress PROGRESS = (files, bytes) -> {};
    private static final List<Path> TEMP = new ArrayList<>();

    private static File home() throws IOException {
        Path path = Files.createTempDirectory("horizon-user-data-test-");
        TEMP.add(path);
        return path.toFile();
    }

    private static void put(File home, String name, String data) throws IOException {
        File out = new File(home, name);
        out.getParentFile().mkdirs();
        Files.write(out.toPath(), data.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(File home, String name) throws IOException {
        return new String(Files.readAllBytes(new File(home, name).toPath()), StandardCharsets.UTF_8);
    }

    private static byte[] backup(File home) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        UserDataArchive.backup(home, out, PROGRESS);
        return out.toByteArray();
    }

    private static void fails(File home, byte[] zip) throws Exception {
        try {
            UserDataArchive.restore(home, new ByteArrayInputStream(zip), PROGRESS);
            throw new AssertionError("Expected restore to fail");
        } catch (IOException expected) {}
        try (java.util.stream.Stream<Path> paths = Files.list(home.toPath())) {
            if (paths.anyMatch(p -> p.getFileName().toString().startsWith(".horizon-restore-"))) {
                throw new AssertionError("Restore staging was not cleaned");
            }
        }
    }

    private static byte[] archive(String... entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (int i = 0; i < entries.length; i += 2) {
                zip.putNextEntry(new ZipEntry(entries[i]));
                zip.write(entries[i + 1].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static void roundTripAndConflicts() throws Exception {
        File source = home(), target = home();
        put(source, "saves/World/level.dat", "world metadata");
        put(source, "saves/World/region/r.0.0.mca", "chunks");
        put(source, "options.txt", "new options");
        put(source, "horizon-skin.png", "skin");
        put(source, "resourcepacks/pack.zip", "pack");
        put(source, "screenshots/image.png", "image");
        put(source, "msa-account.json", "secret");
        put(source, "resources/facts.json", "generated");
        put(source, "mods/unsafe.hnmod", "native code");
        byte[] zip = backup(source);
        Set<String> names = new HashSet<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry entry; (entry = in.getNextEntry()) != null;) names.add(entry.getName());
        }
        assert !names.contains("msa-account.json");
        assert !names.contains("resources/facts.json");
        assert !names.contains("mods/unsafe.hnmod");
        put(target, "saves/World/level.dat", "original world");
        put(target, "options.txt", "original options");
        put(target, "resourcepacks/pack.zip", "original pack");
        UserDataArchive.Result result = UserDataArchive.restore(target, new ByteArrayInputStream(zip), PROGRESS);
        assert result.files == 6;
        assert result.skipped == 1;
        assert read(target, "saves/World/level.dat").equals("original world");
        assert read(target, "saves/World (imported 1)/region/r.0.0.mca").equals("chunks");
        assert read(target, "options.txt").equals("original options");
        assert read(target, "resourcepacks/pack.zip").equals("original pack");
        assert read(target, "resourcepacks/pack (imported 1).zip").equals("pack");
        assert read(target, "horizon-skin.png").equals("skin");
    }

    private static void malformedArchivesDoNotTouchPlayerData() throws Exception {
        File target = home();
        put(target, "options.txt", "original");
        for (String path : new String[]{"../outside.txt", "/absolute.txt", "saves/../options.txt", "saves\\evil", "resources/facts.json", "msa-account.json"}) {
            fails(target, archive("horizon-backup.txt", "horizon-user-data-v1\n", path, "bad"));
            assert read(target, "options.txt").equals("original");
        }
        fails(target, archive("horizon-backup.txt", "horizon-user-data-v1\n", "saves/Bad/chunk", "partial"));
        fails(target, archive("horizon-backup.txt", "horizon-user-data-v1\n", "saves/Bad/chunk", "partial", "horizon-backup-complete.txt", "1\n7\n"));
        assert !new File(target, "saves").exists();
        fails(target, archive("horizon-backup.txt", "horizon-user-data-v1\n", "options.txt", "x", "horizon-backup-complete.txt", "0\n0\n"));
        fails(target, archive("horizon-backup.txt", "horizon-user-data-v1\n", "horizon-backup-complete.txt", "0\n0\n", "options.txt", "x"));
        assert read(target, "options.txt").equals("original");
    }

    private static void symlinksNeverExportOrRedirectRestore() throws Exception {
        File source = home(), outside = home(), target = home();
        put(outside, "token", "private");
        Files.createSymbolicLink(new File(source, "saves").toPath(), outside.toPath());
        try { backup(source); throw new AssertionError("Export followed a symlink"); }
        catch (IOException expected) {}
        File clean = home();
        put(clean, "saves/World/level.dat", "metadata");
        Files.createSymbolicLink(new File(target, "saves").toPath(), outside.toPath());
        fails(target, backup(clean));
        assert !new File(outside, "World").exists();
        assert read(outside, "token").equals("private");
    }

    private static void unsupportedBackupNamesFailBeforeWriting() throws Exception {
        File source = home();
        put(source, "screenshots/invalid:name.png", "image");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try { UserDataArchive.backup(source, output, PROGRESS); throw new AssertionError("Export accepted an unrestorable name"); }
        catch (IOException expected) {}
        assert output.size() == 0;
    }

    public static void main(String[] args) throws Exception {
        try {
            roundTripAndConflicts();
            malformedArchivesDoNotTouchPlayerData();
            symlinksNeverExportOrRedirectRestore();
            unsupportedBackupNamesFailBeforeWriting();
            System.out.println("Archive roundtrip, conflicts, invalid inputs, secret exclusion and symlinks passed.");
        } finally {
            for (Path home : TEMP) try (java.util.stream.Stream<Path> paths = Files.walk(home)) {
                paths.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
            }
        }
    }
}
