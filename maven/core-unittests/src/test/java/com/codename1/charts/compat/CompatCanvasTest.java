package com.codename1.charts.compat;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation.FillOperation;
import com.codename1.testing.TestCodenameOneImplementation.GradientOperation;
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
        implementation.clearGraphicsOperations();
        canvas.drawRect(0, 0, 6, 6, fillPaint);
        FillOperation fill = latestFill();
        assertNotNull(fill);
        assertEquals(0, fill.getX());
        assertEquals(0, fill.getY());
        assertEquals(6, fill.getWidth());
        assertEquals(6, fill.getHeight());
        assertEquals(0xFFFF0000, fill.getColor());

        Image strokeImage = Image.createImage(6, 6);
        canvas.g = strokeImage.getGraphics();
        canvas.g.setAlpha(255);
        Paint strokePaint = new Paint();
        strokePaint.setColor(0xFF00FF00);
        strokePaint.setStyle(Paint.Style.STROKE);
        implementation.clearGraphicsOperations();
        canvas.drawRect(0, 0, 6, 6, strokePaint);
        boolean hasHorizontalEdge = false;
        boolean hasVerticalEdge = false;
        for (FillOperation op : implementation.getFillOperationsSnapshot()) {
            if (op.getColor() == 0xFF00FF00 && op.getY() == 0 && op.getHeight() == 1 && op.getWidth() == 6) {
                hasHorizontalEdge = true;
            }
            if (op.getColor() == 0xFF00FF00 && op.getX() == 0 && op.getWidth() == 1 && op.getHeight() >= 4) {
                hasVerticalEdge = true;
            }
        }
        assertTrue(hasHorizontalEdge);
        assertTrue(hasVerticalEdge);
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
        implementation.clearGraphicsOperations();
        horizontal.draw(canvas);
        GradientOperation gradient = implementation.getLastGradientOperation();
        assertNotNull(gradient);
        assertTrue(gradient.isHorizontal());
        assertEquals(0xFF0000FF, gradient.getStartColor());
        assertEquals(0xFFFF0000, gradient.getEndColor());

        Image fallbackImage = Image.createImage(4, 4);
        canvas.g = fallbackImage.getGraphics();
        canvas.g.setAlpha(255);
        GradientDrawable fallback = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF00FF00, 0xFF000000});
        fallback.setBounds(0, 0, 4, 4);
        implementation.clearGraphicsOperations();
        fallback.draw(canvas);
        assertNull(implementation.getLastGradientOperation());
        FillOperation fallbackFill = latestFill();
        assertNotNull(fallbackFill);
        assertEquals(0xFF00FF00, fallbackFill.getColor());
    }

    private FillOperation latestFill() {
        FillOperation fill = null;
        for (FillOperation op : implementation.getFillOperationsSnapshot()) {
            fill = op;
        }
        return fill;
    }
}
