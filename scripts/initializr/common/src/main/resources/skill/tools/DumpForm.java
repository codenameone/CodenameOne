import com.codename1.impl.javase.JavaSEPort;
import com.codename1.system.Lifecycle;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;

import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/// Boots a Codename One app as a DESKTOP app (no phone skin, desktop px/mm) and
/// prints a flat, machine-readable model of the current Form to stdout. This is
/// the capture step for the form-analysis tools - it lets an agent "see" a screen
/// without vision and feed it to DescribeForm / AlignmentCheck / GuiLint.
///
/// Why desktop mode: the simulator's phone skins are high-DPI, so `mm`-sized
/// chrome is blown up and the responsive layout never reaches its wide form. The
/// Initializr (and most CN1 web/desktop UIs) ship as desktop/web apps, so this
/// mirrors the generated desktop-app stub: `Display.init(JFrame contentPane)`
/// with no skin and `setDefaultPixelMilliRatio(screenDPI/25.4 * retina)`.
///
/// Usage (run with the project + Codename One jars on the classpath):
///
///     CP="common/target/classes:$(mvn -q -pl common dependency:build-classpath \
///           -Dmdep.outputFile=/dev/stdout | tail -1)"
///     java -cp "$CP" tools/DumpForm.java com.example.myapp.MyApp > target/form-model.tsv
///     # then analyse it:
///     java tools/DescribeForm.java   target/form-model.tsv
///     java tools/AlignmentCheck.java target/form-model.tsv
///     java tools/GuiLint.java        target/form-model.tsv
///
/// Options:
///     --width N    desktop content width in px  (default 1180)
///     --height N   desktop content height in px (default 800)
///     --dark       boot in dark mode
///
/// Output format (TAB-delimited, one `C` line per component, depth gives the tree):
///     # cn1-form-model v1
///     DISPLAY <w> <h>
///     C <depth> <class> <uiid> <x> <y> <w> <h> <sx> <sy> <svis> <opaque> <bg> <bgAlpha> <border> <imgBorder> <layout> <name> <text>
///   where sx/sy/svis/opaque/imgBorder are 0/1, bg is hex, bgAlpha is 0..255,
///   x/y are ABSOLUTE coordinates, and <text> (last field) is sanitised of tabs.
///
/// Exit codes: 0 dumped, 2 usage / boot error.
public class DumpForm {
    public static void main(String[] args) throws Exception {
        String mainClass = null;
        String outPath = "cn1-form-model.tsv";
        int w = 1180, h = 800;
        boolean dark = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--width":  w = Integer.parseInt(args[++i]); break;
                case "--height": h = Integer.parseInt(args[++i]); break;
                case "--out":    outPath = args[++i]; break;
                case "--dark":   dark = true; break;
                default:
                    if (args[i].startsWith("--")) { usage(); return; }
                    mainClass = args[i];
            }
        }
        if (mainClass == null) { usage(); return; }

        JavaSEPort.blockMonitors();
        JavaSEPort.setAppHomeDir(".cn1-dumpform");
        JavaSEPort.setUseNativeInput(true);
        JavaSEPort.setShowEDTViolationStacks(false);
        JavaSEPort.setShowEDTWarnings(false);
        JavaSEPort.setFontFaces("Arial", "SansSerif", "Monospaced");

        JFrame frm = new JFrame("DumpForm");
        Toolkit tk = Toolkit.getDefaultToolkit();
        JavaSEPort.setDefaultPixelMilliRatio(tk.getScreenResolution() / 25.4 * JavaSEPort.getRetinaScale());
        frm.getContentPane().setPreferredSize(new Dimension(w, h));
        frm.pack();
        Display.init(frm.getContentPane());
        Display.getInstance().setDarkMode(dark ? Boolean.TRUE : Boolean.FALSE);

        final String mc = mainClass;
        final int fw = w, fh = h;
        final StringBuilder out = new StringBuilder();
        Display.getInstance().callSeriallyAndWait(() -> {
            try {
                Object app = Class.forName(mc).getDeclaredConstructor().newInstance();
                if (app instanceof Lifecycle) {
                    Lifecycle lc = (Lifecycle) app;
                    lc.init(null);
                    lc.runApp();
                } else {
                    // Legacy main with init(Object)/start() reflection.
                    try { app.getClass().getMethod("init", Object.class).invoke(app, new Object[]{null}); } catch (NoSuchMethodException ignore) { }
                    app.getClass().getMethod("start").invoke(app);
                }
            } catch (Throwable t) {
                System.err.println("[DumpForm] failed to start " + mc + ": " + t);
                t.printStackTrace();
            }
        });
        Display.getInstance().callSeriallyAndWait(() -> {
            Form f = Display.getInstance().getCurrent();
            if (f == null) {
                System.err.println("[DumpForm] no current form after start");
                return;
            }
            f.setX(0); f.setY(0); f.setWidth(fw); f.setHeight(fh);
            f.revalidate();
            f.layoutContainer();
            out.append("# cn1-form-model v1\n");
            out.append("DISPLAY\t").append(fw).append('\t').append(fh).append('\n');
            dump(f, 0, out);
        });
        // The model goes to a FILE, not stdout: the CN1 boot logs to stdout, which
        // would otherwise corrupt the model. Logs are noise; the file is the result.
        Path outFile = Paths.get(outPath);
        Path parent = outFile.getParent();
        if (parent != null && !Files.isDirectory(parent)) Files.createDirectories(parent);
        Files.writeString(outFile, out.toString());
        System.err.println("[DumpForm] wrote " + outFile.toAbsolutePath() + " ("
                + (out.length() == 0 ? 0 : out.toString().split("\n").length) + " lines)");
        System.exit(0);
    }

    private static void dump(Component c, int depth, StringBuilder out) {
        Style s = c.getStyle();
        Border b = s.getBorder();
        String[] bt = borderType(b);
        String layout = "-";
        if (c instanceof Container && ((Container) c).getLayout() != null) {
            layout = ((Container) c).getLayout().getClass().getSimpleName();
        }
        out.append("C\t").append(depth)
           .append('\t').append(c.getClass().getSimpleName())
           .append('\t').append(nz(c.getUIID()))
           .append('\t').append(c.getAbsoluteX())
           .append('\t').append(c.getAbsoluteY())
           .append('\t').append(c.getWidth())
           .append('\t').append(c.getHeight())
           .append('\t').append(c.isScrollableX() ? 1 : 0)
           .append('\t').append(c.isScrollableY() ? 1 : 0)
           .append('\t').append(c.isScrollVisible() ? 1 : 0)
           .append('\t').append(s.getBgTransparency() == (byte) 0xff || (s.getBgTransparency() & 0xff) == 255 ? 1 : 0)
           .append('\t').append(hex(s.getBgColor()))
           .append('\t').append(s.getBgTransparency() & 0xff)
           .append('\t').append(bt[0])
           .append('\t').append(bt[1])
           .append('\t').append(layout)
           .append('\t').append(nz(c.getName()))
           .append('\t').append(text(c))
           .append('\n');
        if (c instanceof Container) {
            Container cnt = (Container) c;
            for (int i = 0; i < cnt.getComponentCount(); i++) {
                dump(cnt.getComponentAt(i), depth + 1, out);
            }
        }
    }

    /// Returns {borderTypeName, isImage("1"/"0")}.
    private static String[] borderType(Border b) {
        if (b == null) return new String[]{"none", "0"};
        String cls = b.getClass().getSimpleName();
        if (!"Border".equals(cls)) {
            // RoundBorder / RoundRectBorder / custom - vector, not image.
            return new String[]{cls, "0"};
        }
        try {
            if (b.isEmptyBorder()) return new String[]{"empty", "0"};
        } catch (Throwable ignore) { }
        // Base Border: read the private int 'type' and compare to the IMAGE_* ids.
        try {
            Field tf = Border.class.getDeclaredField("type");
            tf.setAccessible(true);
            int type = tf.getInt(b);
            for (String name : new String[]{"TYPE_IMAGE", "TYPE_IMAGE_SCALED",
                    "TYPE_IMAGE_HORIZONTAL", "TYPE_IMAGE_VERTICAL"}) {
                Field cf = Border.class.getDeclaredField(name);
                cf.setAccessible(true);
                if (cf.getInt(null) == type) return new String[]{"image", "1"};
            }
        } catch (Throwable ignore) { }
        return new String[]{"line", "0"};
    }

    private static String text(Component c) {
        String t = null;
        if (c instanceof Button) t = ((Button) c).getText();
        else if (c instanceof Label) t = ((Label) c).getText();
        else if (c instanceof TextArea) t = ((TextArea) c).getText();
        if (t == null) return "";
        return t.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static String hex(int rgb) {
        return String.format("%06x", rgb & 0xffffff);
    }

    private static String nz(String s) {
        return (s == null || s.isEmpty()) ? "-" : s.replace('\t', ' ');
    }

    private static void usage() {
        System.err.println("Usage: java -cp <project-cp> tools/DumpForm.java <MainClass> [--width N] [--height N] [--dark]");
        System.exit(2);
    }
}
