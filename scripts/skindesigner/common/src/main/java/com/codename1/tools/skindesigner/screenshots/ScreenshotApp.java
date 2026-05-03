package com.codename1.tools.skindesigner.screenshots;

import com.codename1.io.Preferences;
import com.codename1.tools.skindesigner.SkinDesigner;
import com.codename1.tools.skindesigner.SkinModel;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;

import java.awt.Container;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Headless screenshot harness for the developer guide. Boots Codename One
 * in quiet mode (no skin window, no source-watcher), walks the wizard
 * scenarios on the EDT, captures each form via {@link Form#toImage()},
 * and writes the PNG straight to disk.
 *
 * <p>Run via the website CI step or the helper script:
 * <pre>
 *   mvn -pl common exec:java \
 *       -Dexec.mainClass=com.codename1.tools.skindesigner.screenshots.ScreenshotApp \
 *       -Dexec.args="path/to/output/dir"
 * </pre>
 *
 * <p>The previous version of this class extended {@code Lifecycle} and was
 * driven by the cn1:simulator goal; that path booted the full simulator
 * (skin window, CEF, source-change watcher) and never reliably exited
 * inside CI. This rewrite avoids the simulator entirely.
 */
public class ScreenshotApp {

    /** Each row drives one screenshot:
     *  {fileName, demoStep, demoDevice, demoSource, demoSidebarTab, demoPreset}. */
    private static final String[][] SCENARIOS = {
            {"skin-designer-stage-1-device", "0", "apple_apple_iphone_16_pro", null, null, null},
            {"skin-designer-stage-2-source", "1", "apple_apple_iphone_16_pro", null, null, null},
            {"skin-designer-stage-3-editor-shape",   "2", "apple_apple_iphone_16_pro", "shape", "shape",   "island"},
            {"skin-designer-stage-3-editor-cutouts", "2", "apple_apple_iphone_16_pro", "shape", "cutouts", "island"},
            {"skin-designer-stage-3-editor-info",    "2", "apple_apple_iphone_16_pro", "shape", "info",    "island"},
            {"skin-designer-stage-4-done",           "3", "apple_apple_iphone_16_pro", "shape", null,      "island"},
    };

    /** Persistent wizard state keys we wipe between scenarios so each
     *  screenshot starts from a clean slate. */
    private static final String[] WIZARD_PREF_KEYS = {
            "wiz.step", "wiz.deviceId", "wiz.source", "wiz.hasImage",
    };

    /** Render dimensions. iPhone 16 Pro point size — wide enough that the
     *  wizard's two-column editor layout matches what a user sees on a
     *  phone-class viewport. */
    private static final int WIDTH = 390;
    private static final int HEIGHT = 844;

    public static void main(String[] args) throws Exception {
        File outDir = resolveOutDir(args);
        if (!outDir.mkdirs() && !outDir.isDirectory()) {
            throw new RuntimeException("Could not create output dir: " + outDir);
        }
        log("output dir: " + outDir.getAbsolutePath());

        Container hostPanel = new Container();
        hostPanel.setSize(WIDTH, HEIGHT);
        Display.init(hostPanel);

        for (String[] s : SCENARIOS) {
            renderAndSave(s, outDir);
        }

        Display.deinitialize();
        // exitApplication closes the EDT; explicit System.exit guarantees
        // the harness returns control to the shell even if a stray AWT
        // thread is still alive.
        System.exit(0);
    }

    private static void renderAndSave(final String[] s, final File outDir) {
        Display.getInstance().callSeriallyAndWait(() -> {
            try {
                resetWizardState();
                applyDemoProperties(s);

                new SkinDesigner().runApp();
                Form f = Display.getInstance().getCurrent();
                if (f == null) {
                    log("no current form for " + s[0]);
                    return;
                }
                f.setSize(new com.codename1.ui.geom.Dimension(WIDTH, HEIGHT));
                f.setX(0);
                f.setY(0);
                f.layoutContainer();
                f.revalidate();

                Image img = f.toImage();
                if (img == null) {
                    log("toImage returned null for " + s[0]);
                    return;
                }
                File out = new File(outDir, s[0] + ".png");
                try (OutputStream os = new FileOutputStream(out)) {
                    ImageIO.getImageIO().save(img, os, ImageIO.FORMAT_PNG, 1.0f);
                }
                log("saved " + out.getName() + " (" + img.getWidth() + "x" + img.getHeight() + ")");
            } catch (Exception err) {
                err.printStackTrace();
            }
        });
    }

    private static File resolveOutDir(String[] args) {
        if (args != null && args.length > 0 && args[0] != null && !args[0].isEmpty()) {
            return new File(args[0]).getAbsoluteFile();
        }
        String prop = System.getProperty("skindesigner.screenshotsDir");
        if (prop != null && !prop.isEmpty()) {
            return new File(prop).getAbsoluteFile();
        }
        // Default: <repo>/docs/developer-guide/img/skin-designer relative
        // to the common module's basedir (scripts/skindesigner/common/).
        return new File(System.getProperty("user.dir"),
                "../../../docs/developer-guide/img/skin-designer").getAbsoluteFile();
    }

    private static void resetWizardState() {
        for (String k : WIZARD_PREF_KEYS) {
            Preferences.delete(k);
        }
        SkinModel.clearPersisted();
    }

    private static void applyDemoProperties(String[] s) {
        setProp("cn1.skindesigner.demoStep",       s[1]);
        setProp("cn1.skindesigner.demoDevice",     s[2]);
        setProp("cn1.skindesigner.demoSource",     s[3]);
        setProp("cn1.skindesigner.demoSidebarTab", s[4]);
        setProp("cn1.skindesigner.demoPreset",     s[5]);
    }

    private static void setProp(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private static void log(String msg) {
        System.out.println("[skin-designer-screenshots] " + msg);
    }
}
