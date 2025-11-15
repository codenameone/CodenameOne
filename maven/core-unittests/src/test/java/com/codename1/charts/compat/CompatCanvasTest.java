package com.codename1.charts.compat;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Rectangle;

import static org.junit.jupiter.api.Assertions.*;

class CompatCanvasTest extends UITestBase {

    @FormTest
    void drawRectFillsOrStrokesBasedOnPaintStyle() {
        Image image = Image.createImage(6, 6);
        Canvas canvas = new Canvas();
        canvas.g = image.getGraphics();
        canvas.g.setAlpha(255);
        canvas.bounds = new Rectangle(0, 0, 6, 6);

        Paint fillPaint = new Paint();
        fillPaint.setColor(0xFFFF0000);
        fillPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, 6, 6, fillPaint);
        assertEquals(0xFFFF0000, image.getRGB()[0]);

        Image strokeImage = Image.createImage(6, 6);
        canvas.g = strokeImage.getGraphics();
        canvas.g.setAlpha(255);
        Paint strokePaint = new Paint();
        strokePaint.setColor(0xFF00FF00);
        strokePaint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(0, 0, 6, 6, strokePaint);
        int[] stroked = strokeImage.getRGB();
        assertEquals(0xFF00FF00, stroked[0]);
        assertEquals(0, stroked[7]);
    }

    @FormTest
    void gradientDrawableDrawsUsingCanvasOrientation() {
        Image image = Image.createImage(4, 4);
        Canvas canvas = new Canvas();
        canvas.g = image.getGraphics();
        canvas.g.setAlpha(255);
        canvas.bounds = new Rectangle(0, 0, 4, 4);

        GradientDrawable horizontal = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF0000FF, 0xFFFF0000});
        horizontal.setBounds(0, 0, 4, 4);
        horizontal.draw(canvas);
        int[] rgb = image.getRGB();
        assertEquals(0xFF0000FF, rgb[0]);
        assertEquals(0xFFFF0000, rgb[3]);

        Image fallbackImage = Image.createImage(4, 4);
        canvas.g = fallbackImage.getGraphics();
        canvas.g.setAlpha(255);
        GradientDrawable fallback = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF00FF00, 0xFF000000});
        fallback.setBounds(0, 0, 4, 4);
        fallback.draw(canvas);
        int[] fallbackRgb = fallbackImage.getRGB();
        assertEquals(0xFF00FF00, fallbackRgb[0]);
    }
}
