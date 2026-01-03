package com.codename1.ui.plaf;

import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.ui.util.Resources;
import com.codename1.ui.geom.Rectangle2D;
import org.junit.jupiter.api.Assertions;

public class CSSBorderTest extends UITestBase {

    @FormTest
    public void testCSSBorder() {
        CSSBorder border = new CSSBorder();
        // Use hex colors as named colors are not supported by CSSBorder.Color
        border.backgroundColor("#ff0000"); // red
        border.borderColor("#0000ff");     // blue
        border.borderWidth("2px");
        border.borderStyle("solid");

        Assertions.assertNotNull(border.toCSSString());

        // Test painting
        Form f = new Form();
        Component c = new Component() {};
        c.getStyle().setBorder(border);
        f.add(c);

        Image img = Image.createImage(100, 100);
        Graphics ig = img.getGraphics();

        c.setSize(new com.codename1.ui.geom.Dimension(100, 100));
        c.setX(0);
        c.setY(0);

        // Paint border
        border.paintBorderBackground(ig, c);

        // Test Arrow
        // Note: CSSBorder seems to have a bug where Arrow drawing assumes borderRadius is not null.
        // We set borderRadius to avoid NPE.
        border.borderRadius("5px");

        Component track = new Component() {};
        border.setTrackComponent(track);

        // Repaint to trigger arrow logic
        border.paintBorderBackground(ig, c);

    }

    @FormTest
    public void testBorderImage() {
        Image img = Image.createImage(20, 20);
        CSSBorder border = new CSSBorder();
        border.borderImage(img, 5, 5, 5, 5);

        Component c = new Component() {};
        c.setSize(new com.codename1.ui.geom.Dimension(100, 100));

        Image buffer = Image.createImage(100, 100);
        border.paintBorderBackground(buffer.getGraphics(), c);

        Assertions.assertTrue(border.toCSSString().contains("border-image"));
    }
    
    @FormTest
    public void testBoxShadow() {
        CSSBorder border = new CSSBorder();
        border.boxShadow("5px 5px 5px 5px #000000"); // h v blur spread color

        Component c = new Component() {};
        c.setSize(new com.codename1.ui.geom.Dimension(100, 100));
        c.getStyle().setBorder(border);

        Image buffer = Image.createImage(100, 100);
        // This will trigger BoxShadow.paint
        border.paintBorderBackground(buffer.getGraphics(), c);

        // We can't easily verify the pixels painted, but we ensure no exception occurs
        // and exercise the BoxShadow logic.

        // Test parsing variants
        CSSBorder border2 = new CSSBorder();
        border2.boxShadow("inset 2px 2px 2px #ff0000");
        border2.paintBorderBackground(buffer.getGraphics(), c);

        CSSBorder border3 = new CSSBorder();
        border3.boxShadow("none");
        // Should be null shadow

        Assertions.assertThrows(RuntimeException.class, () -> {
            border.toCSSString(); // BoxShadow toCSSString throws RuntimeException as per source
        });
    }

    @FormTest
    public void testRadialGradient() throws Exception {
        // Use reflection to test RadialGradient
        CSSBorder border = new CSSBorder((Resources)null);

        // Reflection to create RadialGradient
        Class<?> radialGradientClass = Class.forName("com.codename1.ui.plaf.CSSBorder$RadialGradient");
        java.lang.reflect.Constructor<?> rgCtor = radialGradientClass.getDeclaredConstructor();
        rgCtor.setAccessible(true);
        Object radialGradient = rgCtor.newInstance();

        // Create BackgroundImage and attach RadialGradient
        Class<?> bgImageClass = Class.forName("com.codename1.ui.plaf.CSSBorder$BackgroundImage");
        java.lang.reflect.Constructor<?> bgCtor = bgImageClass.getDeclaredConstructor();
        bgCtor.setAccessible(true);
        Object bgImage = bgCtor.newInstance();

        java.lang.reflect.Field rgField = bgImageClass.getDeclaredField("radialGradient");
        rgField.setAccessible(true);
        rgField.set(bgImage, radialGradient);

        // Attach BackgroundImage to CSSBorder
        java.lang.reflect.Field bgImagesField = CSSBorder.class.getDeclaredField("backgroundImages");
        bgImagesField.setAccessible(true);
        Object bgImagesArray = java.lang.reflect.Array.newInstance(bgImageClass, 1);
        java.lang.reflect.Array.set(bgImagesArray, 0, bgImage);
        bgImagesField.set(border, bgImagesArray);

        // Verify toCSSString
        try {
            border.toCSSString();
            Assertions.fail("RadialGradient toCSSString should throw RuntimeException");
        } catch(RuntimeException ex) {
            // Expected
            Assertions.assertEquals("RadialGradlient toCSSString() not implemented yet", ex.getMessage());
        }
    }
}
