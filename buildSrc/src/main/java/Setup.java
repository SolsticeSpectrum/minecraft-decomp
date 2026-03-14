import groovy.json.JsonSlurper;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

@SuppressWarnings("unchecked")
public class Setup {

    public static void copySrcClient(File root) throws IOException {
        Path d = root.toPath().resolve("decompSrc/client");
        Path j = root.toPath().resolve("client/src/main/java");
        Path r = root.toPath().resolve("client/src/main/resources");

        Utils.rm(j); Utils.mkdir(j);
        Utils.cp(d.resolve("com"), j.resolve("com"));
        Utils.cp(d.resolve("net"), j.resolve("net"));

        Utils.rm(r); Utils.mkdir(r);
        Utils.cp(d.resolve("assets"), r.resolve("assets"));
        Utils.cp(d.resolve("data"), r.resolve("data"));
        Utils.cp(d.resolve("META-INF"), r.resolve("META-INF"));

        for (File f : d.toFile().listFiles((dir, name) -> name.endsWith(".json") || name.endsWith(".jfc")))
            Files.copy(f.toPath(), r.resolve(f.getName()));

        for (File f : d.toFile().listFiles((dir, name) -> name.endsWith(".png") || name.endsWith(".icns")))
            Files.copy(f.toPath(), r.resolve(f.getName()));
    }

    public static void copySrcServer(File root) throws IOException {
        Path d = root.toPath().resolve("decompSrc/client");
        Path j = root.toPath().resolve("server/src/main/java");
        Path r = root.toPath().resolve("server/src/main/resources");

        Set<String> classes = new HashSet<>();
        Set<String> resources = new HashSet<>();
        try (ZipFile zip = new ZipFile(root.toPath().resolve("jars/server.jar").toFile())) {
            for (ZipEntry e : Collections.list(zip.entries())) {
                if (e.isDirectory()) continue;
                if (e.getName().endsWith(".class") && !e.getName().contains("$"))
                    classes.add(e.getName().replace(".class", ".java"));
                else if (!e.getName().endsWith(".class"))
                    resources.add(e.getName());
            }
        }

        Utils.rm(j); Utils.mkdir(j);
        copyFiltered(d, j, classes);

        Utils.rm(r); Utils.mkdir(r);
        copyFiltered(d, r, resources);
    }

    public static void stripServer(File root) throws IOException {
        Path s = root.toPath().resolve("server/src/main/java");
        Path c = root.toPath().resolve("client/src/main/java");

        Files.walk(s).filter(p -> p.toString().endsWith(".java")).forEach(p -> {
            try { Files.deleteIfExists(c.resolve(s.relativize(p))); } catch (IOException e) {}
        });

        Files.walk(c).sorted(Comparator.reverseOrder()).filter(Files::isDirectory).forEach(p -> {
            try {
                if (p.toFile().list() != null && p.toFile().list().length == 0) Files.delete(p);
            } catch (IOException e) {}
        });
    }

    private static void copyFiltered(Path src, Path dst, Set<String> filter) throws IOException {
        Files.walk(src).filter(p -> !Files.isDirectory(p)).forEach(p -> {
            String rel = src.relativize(p).toString();
            if (!filter.contains(rel)) return;
            try {
                Files.createDirectories(dst.resolve(rel).getParent());
                Files.copy(p, dst.resolve(rel), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) { throw new UncheckedIOException(e); }
        });
    }

    public static List<String> vfArgs(Set<String> libs) {
        List<String> args = new ArrayList<>(Arrays.asList(
            "java", "-jar", "vineflower.jar",
            "-din=1", "-rbr=1", "-dgs=1", "-asc=1", "-rsy=1",
            "--excluded-classes=.*package-info", "--indent-string=    "
        ));
        for (String lib : libs) args.add("-e=libs/" + lib);

        return args;
    }

    public static void assets(File run, String version, String url) throws Exception {
        File dir = new File(run, "assets/indexes");
        dir.mkdirs();

        File idx = new File(dir, version + ".json");
        if (!idx.exists())
            Files.copy(URI.create(url).toURL().openStream(), idx.toPath());

        File objs = new File(run, "assets/objects");
        objs.mkdirs();

        Map<String, Map<String, Object>> objects = (Map) ((Map) new JsonSlurper().parse(idx)).get("objects");
        for (Map<String, Object> m : objects.values()) {
            String h = (String) m.get("hash");
            long size = ((Number) m.get("size")).longValue();

            File f = new File(objs, h.substring(0, 2) + "/" + h);
            if (!f.exists() || f.length() != size) {
                f.getParentFile().mkdirs();
                Files.copy(URI.create("https://resources.download.minecraft.net/" + h.substring(0, 2) + "/" + h)
                        .toURL().openStream(), f.toPath());
            }
        }
    }
}
