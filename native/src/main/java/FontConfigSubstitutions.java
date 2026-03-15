import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import java.io.File;

@TargetClass(className = "sun.awt.FontConfiguration")
public final class FontConfigSubstitutions {
    @Alias
    private String javaLib;
    @Alias
    private File fontConfigFile;
    @Alias
    private boolean foundOsSpecificFile;

    @Substitute
    private void findFontConfigFile() {
        foundOsSpecificFile = true;
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) {
            javaLib = System.getProperty("java.io.tmpdir", "/tmp");
            fontConfigFile = NativeFontHelper.createMinimalFontConfig();
            return;
        }
        javaLib = javaHome + File.separator + "lib";
        String javaConfFonts = javaHome + File.separator + "conf" + File.separator + "fonts";
        String userConfigFile = System.getProperty("sun.awt.fontconfig");
        if (userConfigFile != null) {
            fontConfigFile = new File(userConfigFile);
        } else {
            fontConfigFile = findFontConfigFile(javaConfFonts);
            if (fontConfigFile == null) {
                fontConfigFile = findFontConfigFile(javaLib);
            }
        }
    }

    @Alias
    private File findFontConfigFile(String dir) {
        return null;
    }
}
