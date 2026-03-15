import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class Utils {

    public static void rm(Path p) throws IOException {
        if (!Files.exists(p)) return;

        Files.walk(p).sorted(Comparator.reverseOrder()).forEach(f -> {
            try {
                Files.delete(f);
            } catch (IOException e) {}
        });
    }

    public static void mkdir(Path p) throws IOException {
        Files.createDirectories(p);
    }

    public static void cp(Path src, Path dst) throws IOException {
        if (!Files.exists(src)) return;

        Files.walk(src).forEach(s -> {
            try {
                Files.copy(s, dst.resolve(src.relativize(s)), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public static void unzip(File zip, File dest) throws IOException {
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

    public static void run(File dir, String... cmd) throws Exception {
        Process p = new ProcessBuilder(cmd).directory(dir).inheritIO().start();
        if (p.waitFor() != 0) throw new RuntimeException("Command failed: " + String.join(" ", cmd));
    }

    public static String runOut(File dir, String... cmd) throws Exception {
        Process p = new ProcessBuilder(cmd).directory(dir).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        p.waitFor();

        return out;
    }

    public static List<String> clientArgs(String ver, String assetsVer, Map<String, Object> p) {
        List<String> args = new ArrayList<>(Arrays.asList(
            "--version", ver, "--accessToken", "0",
            "--assetsDir", "assets", "--assetIndex", assetsVer, "--gameDir", "."));
        String user = (String) p.getOrDefault("mc.username", "");
        if (!user.isEmpty()) { args.add("--username"); args.add(user); }
        return args;
    }
}
