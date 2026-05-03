package com.codename1.tools.skindesigner.screenshots;

import com.codename1.io.Log;
import com.codename1.io.Preferences;
import com.codename1.io.Storage;
import com.codename1.system.Lifecycle;
import com.codename1.tools.skindesigner.SkinDesigner;
import com.codename1.tools.skindesigner.SkinModel;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;
import com.codename1.ui.util.UITimer;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Lifecycle entry point used by the developer-guide screenshot pipeline.
 * Drives the Skin Designer through each wizard stage and saves a PNG per
 * stage via {@link Display#captureScreen()} into Storage.
 *
 * <p>On the JavaSE port, Storage maps to {@code ~/.cn1/&lt;name&gt;}, so
 * {@code take-screenshots.sh} just copies those files into
 * {@code docs/developer-guide/img/skin-designer/}.
 *
 * <p>Run via:
 * <pre>
 *   ./run.sh simulator          # but with -Dcodename1.mainName=ScreenshotApp
 * </pre>
 * which the wrapper script handles.
 */
public class ScreenshotApp extends Lifecycle {

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

    /** Persistent wizard state keys we have to wipe between scenarios so
     *  each screenshot starts from a clean slate. Mirrors the keys used
     *  inside SkinDesigner / SkinModel. */
    private static final String[] WIZARD_PREF_KEYS = {
            "wiz.step", "wiz.deviceId", "wiz.source", "wiz.hasImage",
    };

    /** Time to wait after each renderStep before capturing, in ms. The
     *  wizard rebuilds via revalidate() + applyDarkRecursive — give it a
     *  full 1.5 s on JavaSE so layout/animations settle before the
     *  screenshot. */
    private static final int RENDER_DELAY_MS = 1500;

    @Override
    public void runApp() {
        Log.p("[skin-designer-screenshots] starting; scenarios=" + SCENARIOS.length);
        runScenario(0);
    }

    private void runScenario(final int idx) {
        if (idx >= SCENARIOS.length) {
            Log.p("[skin-designer-screenshots] done; exiting");
            // Small delay so the final capture flushes to disk before VM exit.
            UITimer.timer(300, false, Display.getInstance().getCurrent(),
                    () -> Display.getInstance().exitApplication());
            return;
        }
        final String[] s = SCENARIOS[idx];
        Log.p("[skin-designer-screenshots] scenario " + (idx + 1) + "/" + SCENARIOS.length
                + " -> " + s[0]);

        resetWizardState();
        applyDemoProperties(s);

        // Re-initialise the wizard. Each scenario builds a fresh
        // SkinDesigner because runApp captures `this` into action listeners
        // and we want the screenshot to reflect a clean restart.
        new SkinDesigner().runApp();

        UITimer.timer(RENDER_DELAY_MS, false, Display.getInstance().getCurrent(), () -> {
            captureAndSave(s[0]);
            // Yield once before the next scenario so the EDT has a chance
            // to drain any queued repaints from the capture step.
            CN.callSerially(() -> runScenario(idx + 1));
        });
    }

    private void resetWizardState() {
        for (String k : WIZARD_PREF_KEYS) {
            Preferences.delete(k);
        }
        SkinModel.clearPersisted();
    }

    private void applyDemoProperties(String[] s) {
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

    private void captureAndSave(String name) {
        try {
            Image img = Display.getInstance().captureScreen();
            if (img == null) {
                Log.p("[skin-designer-screenshots] captureScreen returned null for " + name);
                return;
            }
            String fileName = name + ".png";
            try (OutputStream os = Storage.getInstance().createOutputStream(fileName)) {
                ImageIO.getImageIO().save(img, os, ImageIO.FORMAT_PNG, 1.0f);
            }
            Log.p("[skin-designer-screenshots] saved " + fileName
                    + " (" + img.getWidth() + "x" + img.getHeight() + ")");
        } catch (IOException err) {
            Log.e(err);
        }
    }
}
