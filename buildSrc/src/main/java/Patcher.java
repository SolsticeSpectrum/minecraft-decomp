import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class Patcher {

    public static void apply(File dir, File patch) throws Exception {
        if (!patch.exists()) return;

        run(dir, "patch", "-p1", "-i", patch.getAbsolutePath());
    }

    public static void unapply(File dir, File patch) throws Exception {
        if (!patch.exists()) return;

        run(dir, "patch", "-R", "-p1", "-i", patch.getAbsolutePath());
    }

    public static void snap(File src, File dest) throws IOException {
        dest.getParentFile().mkdirs();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dest))) {
            Path base = src.toPath();
            Files.walk(base).filter(Files::isRegularFile).forEach(p -> {
                try {
                    zos.putNextEntry(new ZipEntry(base.relativize(p).toString()));
                    Files.copy(p, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    public static void diff(File base, File cur, File out, boolean isZip) throws Exception {
        if (isZip && !base.exists()) throw new RuntimeException("Run setup first");

        Path tmp = Files.createTempDirectory("diff");
        try {
            if (isZip) unzip(base, tmp.toFile());
            else cp(base.toPath(), tmp);

            StringBuilder sb = new StringBuilder();
            for (String pkg : new String[]{"com", "net"}) {
                Path pkgPath = tmp.resolve(pkg);
                if (!Files.exists(pkgPath)) continue;

                Files.walk(pkgPath).filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                    String rel = tmp.relativize(p).toString();
                    File curFile = new File(cur, rel);
                    if (!curFile.exists()) return;

                    try {
                        String d = runOut(tmp.toFile(), "diff", "-u", rel, curFile.getAbsolutePath());
                        if (!d.isEmpty()) {
                            d = d.replace(rel, "a/" + rel);
                            d = d.replace(curFile.getAbsolutePath(), "b/" + rel);
                            sb.append(d);
                        }
                    } catch (Exception e) {}
                });
            }

            out.getParentFile().mkdirs();
            Files.writeString(out.toPath(), sb.toString());
        } finally {
            rm(tmp);
        }
    }

    private static void run(File dir, String... cmd) throws Exception {
        Process p = new ProcessBuilder(cmd).directory(dir).inheritIO().start();
        if (p.waitFor() != 0) throw new RuntimeException("Command failed: " + String.join(" ", cmd));
    }

    private static String runOut(File dir, String... cmd) throws Exception {
        Process p = new ProcessBuilder(cmd).directory(dir).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        p.waitFor();

        return out;
    }

    private static void unzip(File zip, File dest) throws IOException {
        try (ZipFile zf = new ZipFile(zip)) {
            for (ZipEntry e : Collections.list(zf.entries())) {
                if (e.isDirectory()) continue;

                File f = new File(dest, e.getName());
                f.getParentFile().mkdirs();
                try (InputStream in = zf.getInputStream(e)) {
                    Files.copy(in, f.toPath());
                }
            }
        }
    }

    private static void cp(Path src, Path dst) throws IOException {
        Files.walk(src).forEach(s -> {
            try {
                Files.copy(s, dst.resolve(src.relativize(s)), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static void rm(Path p) throws IOException {
        if (!Files.exists(p)) return;

        Files.walk(p).sorted(Comparator.reverseOrder()).forEach(f -> {
            try {
                Files.delete(f);
            } catch (IOException e) {}
        });
    }
}
