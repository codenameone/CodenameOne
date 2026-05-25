package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.svg.GeneratedSVGImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/**
 * Renders the three static SVGs (star, gradient circle, path arrow) generated
 * by the build-time SVG transcoder into a single form so the screenshot
 * framework can verify shapes, gradients, paths and stroke styling all
 * compose correctly on every platform.
 *
 * <p>The generated classes live in {@code com.codename1.generated.svg.*} —
 * they are emitted by the {@code transcode-svg} goal in this module's pom
 * based on the SVG files under {@code src/main/svg}.</p>
 */
public class SVGStaticScreenshotTest extends BaseTest {

    @Override
    public boolean runTest() throws Exception {
        Form form = createForm("Static SVG", BoxLayout.y(), "SVGStatic");
        form.add(label("Star", new com.codename1.generated.svg.Star()));
        form.add(label("Gradient Circle", new com.codename1.generated.svg.GradientCircle()));
        form.add(label("Path Arrow", new com.codename1.generated.svg.PathArrow()));
        form.show();
        return true;
    }

    private Label label(String text, GeneratedSVGImage img) {
        Label l = new Label(text, img);
        Style s = l.getAllStyles();
        s.setMargin(8, 8, 8, 8);
        return l;
    }
}
