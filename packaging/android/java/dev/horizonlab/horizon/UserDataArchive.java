package dev.horizonlab.horizon;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;

/** Portable user data only. Resources, account credentials and running code are never exported. */
final class UserDataArchive {
    private static final String MARKER = "horizon-backup.txt";
    private static final String FORMAT = "horizon-user-data-v1\n";
    private static final String COMPLETE = "horizon-backup-complete.txt";
    private static final Set<String> FOLDERS = new HashSet<>(Arrays.asList(
            "saves", "screenshots", "resourcepacks", "shaderpacks"));
    private static final Set<String> SETTINGS = new HashSet<>(Arrays.asList(
            "options.txt", "horizon-graphics.txt", "horizon-extras.txt", "horizon-player.txt",
            "horizon-skin.txt", "horizon-skin.png", "servers.dat", "touch_layout.json"));
    private static final long MAX_BYTES = 8L << 30;
    private static final int MAX_FILES = 100000;
    interface Progress { void update(int files, long bytes); }
    static final class Result {
        int files, restored, skipped;
        long bytes;
    }

    private static boolean allowed(String name) {
        int slash = name.indexOf('/');
        return slash < 0 ? SETTINGS.contains(name) : FOLDERS.contains(name.substring(0, slash));
    }

    private static boolean safeName(String name) {
        return !name.isEmpty() && !name.contains("\\") && !name.contains(":") && !name.startsWith("/")
                && Arrays.stream(name.split("/", -1)).noneMatch(p -> p.isEmpty() || p.equals(".") || p.equals(".."));
    }

    static Result backup(File home, OutputStream output, Progress progress) throws IOException {
        Result result = new Result();
        List<File> files = new ArrayList<>();
        for (String dir : FOLDERS) collect(new File(home, dir), files);
        for (String name : SETTINGS) {
            File file = new File(home, name);
            if (Files.isSymbolicLink(file.toPath())) throw new IOException("Symbolic link: " + name);
            if (file.isFile()) files.add(file);
        }
        if (files.size() > MAX_FILES) throw new IOException("Too many files");
        for (File file : files) {
            String name = home.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
            if (!safeName(name) || !allowed(name)) throw new IOException("Unsupported backup path: " + name);
        }
        files.sort(Comparator.comparing(File::getPath));
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(output))) {
            zip.setLevel(3);
            zip.putNextEntry(new ZipEntry(MARKER));
            zip.write(FORMAT.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            for (File file : files) {
                String name = home.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
                if (!allowed(name)) throw new IOException("Unexpected user data path");
                ZipEntry entry = new ZipEntry(name);
                entry.setTime(file.lastModified());
                zip.putNextEntry(entry);
                try (InputStream in = new FileInputStream(file)) { copy(in, zip, result); }
                zip.closeEntry();
                result.files++;
                progress.update(result.files, result.bytes);
            }
            zip.putNextEntry(new ZipEntry(COMPLETE));
            zip.write((result.files + "\n" + result.bytes + "\n").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return result;
    }

    private static void collect(File file, List<File> files) throws IOException {
        if (Files.isSymbolicLink(file.toPath())) throw new IOException("Symbolic link: " + file.getName());
        if (!file.exists()) return;
        if (file.isFile()) {
            if (files.size() >= MAX_FILES) throw new IOException("Too many files");
            files.add(file);
        } else {
            File[] children = file.listFiles();
            if (children == null) throw new IOException("Cannot read folder: " + file.getName());
            for (File child : children) collect(child, files);
        }
    }

    private static void copy(InputStream in, OutputStream out, Result result) throws IOException {
        byte[] buffer = new byte[65536];
        for (int n; (n = in.read(buffer)) != -1;) {
            result.bytes += n;
            if (result.bytes > MAX_BYTES) throw new IOException("Archive exceeds 8 GiB");
            out.write(buffer, 0, n);
        }
    }

    static Result restore(File home, InputStream input, Progress progress) throws IOException {
        // Stage beside the destination so complete worlds can be moved without partial copies.
        File stage = new File(home, ".horizon-restore-" + UUID.randomUUID());
        if (!stage.mkdirs()) throw new IOException("Cannot create restore folder");
        Result result = new Result();
        try {
            extract(stage, input, result, progress);
            File saves = new File(stage, "saves");
            File[] worlds = saves.listFiles();
            if (worlds != null) for (File world : worlds) {
                if (!world.isDirectory() || !new File(world, "level.dat").isFile()) {
                    throw new IOException("World has no level.dat: " + world.getName());
                }
            }
            // Validate every destination before moving any player data.
            for (String folder : FOLDERS) {
                File target = new File(home, folder);
                if (Files.isSymbolicLink(target.toPath()) || target.exists() && !target.isDirectory()) {
                    throw new IOException("Invalid destination: " + folder);
                }
            }
            for (String folder : FOLDERS) {
                File[] items = new File(stage, folder).listFiles();
                if (items == null) continue;
                File target = new File(home, folder);
                if (!target.isDirectory() && !target.mkdirs()) throw new IOException("Cannot create " + folder);
                for (File item : items) {
                    File destination = available(target, item.getName(), item.isDirectory());
                    Files.move(item.toPath(), destination.toPath());
                    result.restored++;
                }
            }
            for (String name : SETTINGS) {
                File source = new File(stage, name), target = new File(home, name);
                if (!source.isFile()) continue;
                if (target.exists() || Files.isSymbolicLink(target.toPath())) { result.skipped++; continue; }
                Files.move(source.toPath(), target.toPath());
                result.restored++;
            }
            return result;
        } finally { remove(stage); }
    }

    private static void extract(File stage, InputStream input, Result result, Progress progress) throws IOException {
        Set<String> seen = new HashSet<>();
        boolean marker = false;
        boolean complete = false;
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(input))) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                String name = entry.getName();
                if (complete) throw new IOException("Data after backup completion marker");
                if (entry.isDirectory()) name = name.substring(0, name.length() - 1);
                if (!safeName(name)) {
                    throw new IOException("Invalid archive path");
                }
                if (!seen.add(name) || seen.size() > MAX_FILES + 2) throw new IOException("Duplicate path or too many entries");
                if (name.equals(COMPLETE)) {
                    byte[] bytes = new byte[100];
                    int count = 0, n;
                    while (count < bytes.length && (n = zip.read(bytes, count, bytes.length - count)) != -1) count += n;
                    if (!new String(bytes, 0, count, StandardCharsets.UTF_8).equals(result.files + "\n" + result.bytes + "\n")) {
                        throw new IOException("Incomplete backup");
                    }
                    complete = true;
                } else if (name.equals(MARKER)) {
                    byte[] bytes = new byte[FORMAT.length() + 1];
                    int count = 0, n;
                    while (count < bytes.length && (n = zip.read(bytes, count, bytes.length - count)) != -1) count += n;
                    if (!new String(bytes, 0, count, StandardCharsets.UTF_8).equals(FORMAT)) throw new IOException("Unsupported backup version");
                    marker = true;
                } else {
                    if (!allowed(name) && !(entry.isDirectory() && FOLDERS.contains(name))) throw new IOException("Not user data: " + name);
                    File out = new File(stage, name);
                    if (entry.isDirectory()) {
                        if (!out.isDirectory() && !out.mkdirs()) throw new IOException("Invalid folder");
                    } else {
                        if (!out.getParentFile().isDirectory() && !out.getParentFile().mkdirs()) throw new IOException("Invalid parent folder");
                        try (OutputStream stream = new FileOutputStream(out)) { copy(zip, stream, result); }
                        result.files++;
                        progress.update(result.files, result.bytes);
                    }
                }
                zip.closeEntry();
            }
        }
        if (!marker || !complete) throw new IOException("Not a complete Horizon user-data backup");
    }

    private static File available(File parent, String name, boolean directory) throws IOException {
        File out = new File(parent, name);
        int dot = directory ? -1 : name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        String extension = dot > 0 ? name.substring(dot) : "";
        for (int i = 1; out.exists() || Files.isSymbolicLink(out.toPath()); i++) {
            if (i > 10000) throw new IOException("Too many restore conflicts");
            out = new File(parent, stem + " (imported " + i + ")" + extension);
        }
        return out;
    }

    private static void remove(File file) {
        if (!Files.isSymbolicLink(file.toPath()) && file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) remove(child);
        }
        file.delete();
    }
}
