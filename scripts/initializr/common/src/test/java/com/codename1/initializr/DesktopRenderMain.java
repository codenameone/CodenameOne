package com.codename1.initializr;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;

import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Renders the Initializr the way it actually ships -- as a <b>desktop / web app</b>,
 * not on a phone skin -- and writes a full-height PNG of the single scrolling column
 * to {@code target/mockup-compare/}. This is the right lens for comparing against the
 * design mockup: it uses the desktop px/mm ratio (so {@code mm}-sized chrome is not
 * blown up by a high-DPI phone skin) and a wide, low-DPI surface like a browser.
 *
 * <p>Not a unit test (no {@code @Test}, not a {@code UnitTest}) so neither Surefire
 * nor {@code cn1:test} runs it; invoke it directly:
 * <pre>
 *   CP=common/target/classes:$(deps)
 *   java -Dtheme=light -Dw=1180 -Dh=2560 -cp "$CP" com.codename1.initializr.DesktopRenderMain
 *   java -Dtheme=dark  -Dw=1180 -Dh=2560 -cp "$CP" com.codename1.initializr.DesktopRenderMain
 * </pre>
 * Mirrors the generated desktop app stub: {@code Display.init(JFrame contentPane)}
 * with no skin and {@code setDefaultPixelMilliRatio(screenDPI/25.4 * retina)}.
 */
public final class DesktopRenderMain {

    public static void main(String[] args) throws Exception {
        final int w = Integer.parseInt(System.getProperty("w", "1180"));
        final int capH = Integer.parseInt(System.getProperty("h", "2560"));
        final boolean dark = "dark".equalsIgnoreCase(System.getProperty("theme", "light"));

        JavaSEPort.blockMonitors();
        JavaSEPort.setAppHomeDir(".initializr-render");
        JavaSEPort.setExposeFilesystem(true);
        JavaSEPort.setUseNativeInput(true);
        JavaSEPort.setShowEDTViolationStacks(false);
        JavaSEPort.setShowEDTWarnings(false);
        JavaSEPort.setFontFaces("Arial", "SansSerif", "Monospaced");

        JFrame frm = new JFrame("Initializr");
        Toolkit tk = Toolkit.getDefaultToolkit();
        JavaSEPort.setDefaultPixelMilliRatio(tk.getScreenResolution() / 25.4 * JavaSEPort.getRetinaScale());
        frm.getContentPane().setPreferredSize(new Dimension(w, 900));
        frm.pack();
        Display.init(frm.getContentPane());
        Display.getInstance().setDarkMode(dark ? Boolean.TRUE : Boolean.FALSE);

        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                Initializr app = new Initializr();
                app.init(null);
                app.runApp();
            }
        });
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                try {
                    Form f = Initializr.lastBuiltForm;
                    int realW = Display.getInstance().getDisplayWidth();
                    f.setX(0);
                    f.setY(0);
                    f.setWidth(realW);
                    f.setHeight(capH);
                    f.revalidate();
                    f.layoutContainer();
                    Image shot = Image.createImage(realW, capH, 0xffffffff);
                    f.paintComponent(shot.getGraphics(), true);
                    ImageIO io = ImageIO.getImageIO();
                    File out = new File("target/mockup-compare/initializr-desktop-" + (dark ? "dark" : "light") + ".png");
                    out.getParentFile().mkdirs();
                    try (OutputStream os = new FileOutputStream(out)) {
                        io.save(shot, os, ImageIO.FORMAT_PNG, 1);
                    }
                    System.out.println("[DesktopRenderMain] wrote " + out.getAbsolutePath() + " (" + realW + "x" + capH + ")");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        System.exit(0);
    }

    private DesktopRenderMain() {
    }
}
