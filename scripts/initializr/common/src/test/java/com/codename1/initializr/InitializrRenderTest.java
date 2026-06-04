package com.codename1.initializr;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Smoke-tests that the redesigned Initializr builds and renders, and captures a
 * PNG to {@code target/mockup-compare/} so the render can be scored against the
 * Claude design mockups via tools/CompareToMockup.java.
 *
 * <p>The theme is selected by the {@code initializr.theme} system property
 * ({@code light} or {@code dark}); the harness runs this test once per theme in
 * a fresh JVM so a single {@code Form.show()} settles before capture (we run on
 * the EDT, so a second show() in the same process would not transition).
 */
public class InitializrRenderTest extends AbstractTest {

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }

    @Override
    public boolean runTest() throws Exception {
        boolean dark = "dark".equalsIgnoreCase(System.getProperty("initializr.theme", "light"));
        Display.getInstance().setDarkMode(dark ? Boolean.TRUE : Boolean.FALSE);

        String widthProp = System.getProperty("initializr.width");
        String heightProp = System.getProperty("initializr.height");
        boolean wide = widthProp != null;
        if (wide) {
            // Render the two-column desktop split. The phone skin is high-DPI, so
            // the canvas is sized so each column is roughly a phone-width wide.
            Initializr.forceWideForTesting = Boolean.TRUE;
        }

        new Initializr().runApp();
        Form form = Initializr.lastBuiltForm;
        assertNotNull(form, "Initializr should build a form");

        int w = wide ? Integer.parseInt(widthProp) : Display.getInstance().getDisplayWidth();
        int h = heightProp != null ? Integer.parseInt(heightProp) : Display.getInstance().getDisplayHeight();
        String suffix = (wide ? "wide-" : "") + (dark ? "dark" : "light");
        form.setX(0);
        form.setY(0);
        form.setWidth(w);
        form.setHeight(h);
        form.revalidate();
        form.layoutContainer();

        capture(form, "initializr-" + suffix, w, h);
        return true;
    }

    private static void capture(Form form, String name, int w, int h) throws Exception {
        ImageIO io = ImageIO.getImageIO();
        if (io == null || !io.isFormatSupported(ImageIO.FORMAT_PNG)) {
            throw new IllegalStateException("PNG ImageIO not available in this environment");
        }
        Image shot = Image.createImage(w, h, 0xffffffff);
        form.paintComponent(shot.getGraphics(), true);
        File outDir = new File("target/mockup-compare");
        outDir.mkdirs();
        File outFile = new File(outDir, name + ".png");
        try (OutputStream out = new FileOutputStream(outFile)) {
            io.save(shot, out, ImageIO.FORMAT_PNG, 1);
        }
        System.out.println("[InitializrRenderTest] wrote " + outFile.getAbsolutePath()
                + " (" + w + "x" + h + ") formUIID=" + form.getUIID()
                + " bg=" + Integer.toHexString(form.getStyle().getBgColor()));
    }
}
