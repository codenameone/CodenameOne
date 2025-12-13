package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.Style;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class DrawStringDecorated extends AbstractGraphicsScreenshotTest {
    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        g.setColor(0xffffff);
        g.fillRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        g.setColor(0);
        int y = bounds.getY();
        g.drawString("No Decoration", bounds.getX(), y, Style.TEXT_DECORATION_NONE);
        y += g.getFont().getHeight();
        g.drawString("3D Decoration", bounds.getX(), y, Style.TEXT_DECORATION_3D);
        y += g.getFont().getHeight();
        g.drawString("3D Lowered Decoration", bounds.getX(), y, Style.TEXT_DECORATION_3D_LOWERED);
        y += g.getFont().getHeight();
        g.drawString("Overline Decoration", bounds.getX(), y, Style.TEXT_DECORATION_OVERLINE);
        y += g.getFont().getHeight();
        g.drawString("Strikethru Decoration", bounds.getX(), y, Style.TEXT_DECORATION_STRIKETHRU);
        y += g.getFont().getHeight();
        g.drawString("3D Shadow North Decoration", bounds.getX(), y, Style.TEXT_DECORATION_3D_SHADOW_NORTH);
        y += g.getFont().getHeight();
        g.drawString("Underline Decoration", bounds.getX(), y, Style.TEXT_DECORATION_UNDERLINE);
    }

    @Override
    protected String screenshotName() {
        return "graphics-draw-string-decorated";
    }
}
