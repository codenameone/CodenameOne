package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class DrawString extends AbstractGraphicsScreenshotTest {
    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        g.setColor(0);
        g.fillRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        g.setColor(0xffffff);
        int y = bounds.getY();
        g.drawString("Default Font", bounds.getX(), y);
        y += g.getFont().getHeight();
        g.setFont(Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_SMALL));
        g.drawString("Small Bold Monospace", bounds.getX(), y);
        y += g.getFont().getHeight();
        String[] ttfFonts = {"native:MainThin", "native:MainLight", "native:MainRegular",
                "native:MainBold", "native:MainBlack", "native:ItalicThin", "native:ItalicLight",
                "native:ItalicRegular", "native:ItalicBold", "native:ItalicBlack"};
        for(String name : ttfFonts) {
            g.setFont(Font.createTrueTypeFont(name, 4));
            g.drawString(name, bounds.getX(), y);
            y += g.getFont().getHeight();
        }

        g.setColor(0xff0000);
        g.drawStringBaseline("Baseline and עברית", bounds.getX(), bounds.getY() + bounds.getHeight());
    }

    @Override
    protected String screenshotName() {
        return "graphics-draw-string";
    }
}
