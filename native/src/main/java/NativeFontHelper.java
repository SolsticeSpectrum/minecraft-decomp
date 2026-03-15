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
                String[] sansNames = {"DejaVu Sans", "DejaVu Sans Bold", "DejaVu Sans Oblique", "DejaVu Sans Bold Oblique"};
                String[] serifNames = {"DejaVu Serif", "DejaVu Serif Bold", "DejaVu Serif Oblique", "DejaVu Serif Bold Oblique"};
                String[] monoNames = {"DejaVu Sans Mono", "DejaVu Sans Mono Bold", "DejaVu Sans Mono Oblique", "DejaVu Sans Mono Bold Oblique"};
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
