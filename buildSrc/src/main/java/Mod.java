import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class Mod {

    public static void pack(File base, File src, File classes, File out) throws Exception {
        if (!base.exists()) throw new RuntimeException("Run setup first");

        Set<String> changed = changed(base, src);
        if (changed.isEmpty()) {
            System.out.println("No changes to pack");
            return;
        }

        zip(classes, changed, out);
        System.out.println("Packed " + changed.size() + " classes to " + out.getName());
    }

    private static Set<String> changed(File base, File src) throws IOException {
        Set<String> changed = new HashSet<>();
        Map<String, byte[]> orig = new HashMap<>();

        try (ZipFile zf = new ZipFile(base)) {
            for (ZipEntry e : Collections.list(zf.entries())) {
                if (e.isDirectory() || !e.getName().endsWith(".java")) continue;

                orig.put(e.getName(), zf.getInputStream(e).readAllBytes());
            }
        }

        Path s = src.toPath();
        for (String pkg : new String[]{"com", "net"}) {
            Path p = s.resolve(pkg);
            if (!Files.exists(p)) continue;

            Files.walk(p).filter(f -> f.toString().endsWith(".java")).forEach(f -> {
                String rel = s.relativize(f).toString();
                try {
                    byte[] cur = Files.readAllBytes(f);
                    byte[] o = orig.get(rel);
                    if (o == null || !Arrays.equals(cur, o)) changed.add(rel);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }

        return changed;
    }

    private static void zip(File classes, Set<String> srcs, File out) throws IOException {
        out.getParentFile().mkdirs();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out))) {
            Path base = classes.toPath();
            for (String s : srcs) {
                String name = s.replace(".java", "");
                Path dir = base.resolve(name).getParent();
                String prefix = base.resolve(name).getFileName().toString();
                if (!Files.exists(dir)) continue;

                Files.list(dir).filter(p -> p.getFileName().toString().startsWith(prefix) && p.toString().endsWith(".class")).forEach(p -> {
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
    }
}
