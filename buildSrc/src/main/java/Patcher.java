import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class Patcher {

    public static void apply(File dir, File patch) throws Exception {
        if (!patch.exists()) return;

        for (var e : parse(patch).entrySet()) {
            File f = new File(dir, e.getKey());
            if (!f.exists()) throw new RuntimeException("File not found: " + e.getKey());

            List<String> orig = Files.readAllLines(f.toPath());
            Patch<String> p = UnifiedDiffUtils.parseUnifiedDiff(e.getValue());

            try {
                Files.write(f.toPath(), DiffUtils.patch(orig, p));
            } catch (PatchFailedException ex) {
                throw new RuntimeException("Patch failed for " + e.getKey() + ": " + ex.getMessage());
            }
        }
    }

    public static void unapply(File dir, File patch) throws Exception {
        if (!patch.exists()) return;

        for (var e : parse(patch).entrySet()) {
            File f = new File(dir, e.getKey());
            if (!f.exists()) throw new RuntimeException("File not found: " + e.getKey());

            List<String> cur = Files.readAllLines(f.toPath());
            Patch<String> p = UnifiedDiffUtils.parseUnifiedDiff(e.getValue());
            List<String> result = DiffUtils.unpatch(cur, p);

            try {
                List<String> check = DiffUtils.patch(result, p);
                if (!check.equals(cur))
                    throw new RuntimeException("Revert failed for " + e.getKey() + ": mismatch");
            } catch (PatchFailedException ex) {
                throw new RuntimeException("Revert failed for " + e.getKey() + ": " + ex.getMessage());
            }

            Files.write(f.toPath(), result);
        }
    }

    private static Map<String, List<String>> parse(File patch) throws IOException {
        Map<String, List<String>> m = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(patch.toPath());
        List<String> cur = null;
        String file = null;

        for (String line : lines) {
            if (line.startsWith("--- a/")) {
                if (cur != null && file != null) m.put(file, cur);
                file = line.substring(6).split("\t")[0];

                cur = new ArrayList<>();
                cur.add(line);
            } else if (cur != null) {
                cur.add(line);
            }
        }

        if (cur != null && file != null) m.put(file, cur);

        return m;
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
            if (isZip) Utils.unzip(base, tmp.toFile());
            else Utils.cp(base.toPath(), tmp);

            StringBuilder sb = new StringBuilder();
            for (String pkg : new String[]{"com", "net"}) {
                Path d = tmp.resolve(pkg);
                if (!Files.exists(d)) continue;

                Files.walk(d).filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                    String rel = tmp.relativize(p).toString();
                    File f = new File(cur, rel);
                    if (!f.exists()) return;

                    try {
                        List<String> a = Files.readAllLines(p);
                        List<String> b = Files.readAllLines(f.toPath());
                        Patch<String> patch = DiffUtils.diff(a, b);

                        if (!patch.getDeltas().isEmpty()) {
                            List<String> u = UnifiedDiffUtils.generateUnifiedDiff(rel, rel, a, patch, 3);
                            for (int i = 0; i < u.size(); i++) {
                                String line = u.get(i);
                                if (line.startsWith("--- ")) u.set(i, "--- a/" + rel);
                                else if (line.startsWith("+++ ")) u.set(i, "+++ b/" + rel);
                            }

                            for (String line : u) sb.append(line).append("\n");
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }

            out.getParentFile().mkdirs();
            Files.writeString(out.toPath(), sb.toString());
        } finally {
            Utils.rm(tmp);
        }
    }
}
