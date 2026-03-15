import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class NativeFontHelper {
    public static File createMinimalFontConfig() {
        try {
            File f = File.createTempFile("fontconfig", ".properties");
            f.deleteOnExit();
            try (FileWriter w = new FileWriter(f)) {
                w.write("version=1\n");
                String[] families = {"dialog", "sansserif", "serif", "monospaced", "dialoginput"};
                String[] styles = {"plain", "bold", "italic", "bolditalic"};
                String[] sansNames = {"SansSerif", "SansSerif Bold", "SansSerif Italic", "SansSerif Bold Italic"};
                String[] serifNames = {"Serif", "Serif Bold", "Serif Italic", "Serif Bold Italic"};
                String[] monoNames = {"Monospaced", "Monospaced Bold", "Monospaced Italic", "Monospaced Bold Italic"};
                for (String family : families) {
                    String[] names = family.equals("serif") ? serifNames :
                                     (family.equals("monospaced") || family.equals("dialoginput")) ? monoNames : sansNames;
                    for (int i = 0; i < styles.length; i++) {
                        w.write(family + "." + styles[i] + ".latin-1=" + names[i] + "\n");
                    }
                }
                w.write("sequence.allfonts=latin-1\n");
                w.write("sequence.fallback=\n");
            }
            return f;
        } catch (IOException e) {
            return null;
        }
    }
}
