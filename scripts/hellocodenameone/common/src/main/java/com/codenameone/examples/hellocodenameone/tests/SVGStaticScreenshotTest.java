package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.Log;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.Resources;

import java.io.InputStream;

/**
 * End-to-end test for the build-time SVG transcoder.
 *
 * <p>Renders the six SVGs declared in {@code theme.css} via
 * {@code Resources.getGlobalResources().getImage(name)} -- exactly the
 * call any user app would make. The CSS compiler stored a 1×1 PNG
 * placeholder under each name, and the per-port wiring (JavaSE port,
 * IPhoneBuilder, AndroidGradleBuilder) ran the auto-generated
 * {@code com.codename1.generated.svg.SVGRegistry.installGlobal()} on
 * startup, replacing every placeholder with the transcoded
 * {@code GeneratedSVGImage}. No glue code in app land.</p>
 *
 * <p>If this test calls {@code SVGRegistry.install(...)} explicitly it
 * has failed the point of the test: the registry is supposed to be
 * seamless to the developer.</p>
 */
public class SVGStaticScreenshotTest extends BaseTest {

    @Override
    public boolean runTest() throws Exception {
        // Runs on the JS port now too: the build-time SVG transcoder generates
        // com.codename1.generated.svg.SVGRegistry, and the JS launcher calls
        // installGlobal() at startup (see JavaScriptBuilder.writeLauncher), so the
        // transcoded SVGs render instead of the 1x1 placeholder. This is the
        // cross-platform affine + text rendering check the test exists for.
        Resources res = resolveGlobalResources();

        // 2x3 grid so every transcoded SVG fits in a single screenshot
        // capture -- a BoxLayout.y stacked the entries off the bottom of
        // the iOS / Android viewport and the user only saw the first three.
        Form form = createForm("Static SVG (theme.getImage)", new GridLayout(3, 2), "SVGStatic");
        form.add(label("star.svg", res == null ? null : res.getImage("star.svg")));
        form.add(label("gradient_circle.svg", res == null ? null : res.getImage("gradient_circle.svg")));
        form.add(label("path_arrow.svg", res == null ? null : res.getImage("path_arrow.svg")));
        // <text> end-to-end -- font-weight + anchor + multi-color fills:
        form.add(label("logo_text.svg", res == null ? null : res.getImage("logo_text.svg")));
        // S/T smooth curves + dashed strokes:
        form.add(label("wave_path.svg", res == null ? null : res.getImage("wave_path.svg")));
        // clip-path: rounded-rect outline gating a gradient-filled rect + text:
        form.add(label("clipped_badge.svg", res == null ? null : res.getImage("clipped_badge.svg")));
        form.show();
        return true;
    }

    private Label label(String text, Image img) {
        if (img == null) {
            Label l = new Label(text + " <missing>");
            l.getAllStyles().setFgColor(0xFF0000);
            return l;
        }
        Label l = new Label(text, img);
        Style s = l.getAllStyles();
        s.setMargin(8, 8, 8, 8);
        return l;
    }

    /** Locate the project's global Resources bundle. The framework normally
     *  loads the default theme into the global slot at app init; if it
     *  hasn't (or this test is run in isolation), open the bundled
     *  theme.res by class-relative path and remember it. */
    static Resources resolveGlobalResources() {
        Resources r = Resources.getGlobalResources();
        if (r != null) {
            return r;
        }
        InputStream in = null;
        try {
            in = SVGStaticScreenshotTest.class.getResourceAsStream("/theme");
            if (in == null) {
                in = SVGStaticScreenshotTest.class.getResourceAsStream("/theme.res");
            }
            if (in != null) {
                Resources opened = Resources.open(in);
                Resources.setGlobalResources(opened);
                return opened;
            }
        } catch (Throwable t) {
            Log.e(t);
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignored) { /* no-op */ }
        }
        return null;
    }
}
