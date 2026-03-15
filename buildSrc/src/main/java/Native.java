import java.io.*;
import java.util.*;

public class Native {

    public static void requireGraalVM() {
        if (!new File(bin()).exists())
            throw new RuntimeException("Current JDK is not GraalVM (native-image not found)");
    }

    public static String agentArg(File root, String side) {
        return "-agentlib:native-image-agent=config-merge-dir=" + new File(root, "native/configs/" + side);
    }

    public static String classpath(File projectJar, String runtimeCp, File nativeJar) {
        return projectJar.getAbsolutePath() + File.pathSeparator + runtimeCp +
            File.pathSeparator + nativeJar.getAbsolutePath();
    }

    public static List<String> buildCommand(String cp, File root, String side, String mainClass) {
        return new ArrayList<>(Arrays.asList(
            bin(), "-cp", cp,
            "-H:ConfigurationFileDirectories=" + new File(root, "native/configs/" + side),
            "-H:Name=" + side,
            "--no-fallback", "--gc=serial", "--enable-http", "--enable-https",
            "-H:+AddAllCharsets", "-H:+UnlockExperimentalVMOptions", "-Djava.awt.headless=false",
            "-H:IncludeResources=data/.*", "-H:IncludeResources=assets/.*",
            "--initialize-at-run-time=net.minecraft.TracingExecutor,sun.net.dns.ResolverConfigurationImpl," +
                "io.netty,org.apache.logging.log4j,org.apache.logging.slf4j,org.slf4j,com.mojang.logging",
            mainClass
        ));
    }

    public static List<String> runCommand(File root, String side, String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add(new File(root, "native/build/" + side).getAbsolutePath());
        cmd.addAll(Arrays.asList(args));
        return cmd;
    }

    private static String bin() {
        return System.getProperty("java.home") + "/bin/native-image";
    }
}
