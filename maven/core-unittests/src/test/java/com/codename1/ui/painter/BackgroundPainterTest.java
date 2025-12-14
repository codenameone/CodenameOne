package com.codename1.ui.painter;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.Style;
import org.junit.jupiter.api.Assertions;

public class BackgroundPainterTest extends UITestBase {

    @FormTest
    public void testPaint() {
        Component c = new Component() {};
        Style s = c.getStyle();
        s.setBgColor(0xff0000);
        s.setBgTransparency(255);

        BackgroundPainter painter = new BackgroundPainter(c);

        Image img = Image.createImage(100, 100);
        Graphics g = img.getGraphics();

        // Test color background
        painter.paint(g, new Rectangle(0, 0, 100, 100));

        // Test image background scaled
        Image bgImg = Image.createImage(10, 10, 0x00ff00);
        s.setBgImage(bgImg);
        s.setBackgroundType(Style.BACKGROUND_IMAGE_SCALED);
        painter.paint(g, new Rectangle(0, 0, 100, 100));

        // Test image background tiled
        s.setBackgroundType(Style.BACKGROUND_IMAGE_TILE_BOTH);
        painter.paint(g, new Rectangle(0, 0, 100, 100));

        Assertions.assertNotNull(img); // Just ensure no exception
    }
}
