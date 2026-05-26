package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.Log;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

import java.io.InputStream;

/**
 * Exercises the full build-time SVG transcoder stack end-to-end as a
 * developer would use it:
 *
 * <ol>
 *   <li>{@code theme.css} declares
 *       {@code background: url(star.svg); cn1-source-dpi: very-high;} on
 *       several styles -- standard CSS authoring, no Java-side hardcoding.</li>
 *   <li>The {@code transcode-svg} mojo (auto-bound to {@code generate-sources}
 *       in the cn1app archetype) emits one {@code GeneratedSVGImage} subclass
 *       per .svg file plus a {@code SVGRegistry} that records the CSS
 *       density hint per image.</li>
 *   <li>The {@code css} goal lays down a 1x1 transparent PNG placeholder so
 *       the CSS compiler can complete; the theme stores the SVG name in the
 *       resource bundle.</li>
 *   <li>At runtime this test calls {@code SVGRegistry.install(globalRes)}
 *       which replaces every placeholder with the transcoded SVG, then it
 *       asks {@code Resources.getGlobalResources().getImage("star.svg")} --
 *       the same call any user code in this app would make.</li>
 * </ol>
 *
 * <p>Anything other than {@code Resources.getImage(name)} on the production
 * resource bundle here would defeat the point of the test: the value of the
 * transcoder is that CSS-referenced SVGs are interchangeable with PNGs from
 * the developer's perspective.</p>
 */
public class SVGStaticScreenshotTest extends BaseTest {

    @Override
    public void prepare() {
        super.prepare();
        installSVGRegistry();
    }

    @Override
    public boolean runTest() throws Exception {
        Resources res = resolveGlobalResources();

        Form form = createForm("Static SVG (theme.getImage)", BoxLayout.y(), "SVGStatic");
        form.add(label("star.svg", res == null ? null : res.getImage("star.svg")));
        form.add(label("gradient_circle.svg", res == null ? null : res.getImage("gradient_circle.svg")));
        form.add(label("path_arrow.svg", res == null ? null : res.getImage("path_arrow.svg")));
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
     *  loads the default theme into the global slot at app init; for the test
     *  harness we fall back to opening the bundled theme.res by class-relative
     *  path and remembering it as the global. */
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
        // Last resort -- give up; the caller has to handle null. The
        // SVGRegistry's global fallback still resolves direct
        // Resources.registerGeneratedImage lookups, so user code that
        // doesn't pre-load a theme still gets the SVGs.
        return null;
    }

    /** Install all transcoded SVGs into the global Resources by calling the
     *  build-generated registry directly. This is the same call user code
     *  is expected to make once at app init -- see the developer guide. */
    static void installSVGRegistry() {
        Resources r = resolveGlobalResources();
        com.codename1.generated.svg.SVGRegistry.install(r);
    }
}
