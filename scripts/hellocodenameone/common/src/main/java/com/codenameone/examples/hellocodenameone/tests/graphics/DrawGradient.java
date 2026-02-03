package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Rectangle;
import com.codenameone.examples.hellocodenameone.tests.AbstractGraphicsScreenshotTest;

public class DrawGradient extends AbstractGraphicsScreenshotTest {
    private Image gradientImage;
    private int cachedWidth = -1;
    private int cachedHeight = -1;

    @Override
    protected void drawContent(Graphics g, Rectangle bounds) {
        int width = bounds.getWidth();
        int height = bounds.getHeight();
        if (gradientImage == null || cachedWidth != width || cachedHeight != height) {
            gradientImage = buildGradientImage(width, height);
            cachedWidth = width;
            cachedHeight = height;
        }
        g.drawImage(gradientImage, bounds.getX(), bounds.getY());
    }

    @Override
    protected String screenshotName() {
        return "graphics-draw-gradient";
    }

    private Image buildGradientImage(int width, int height) {
        int[] rgb = new int[width * height];
        int background = 0xff000000;
        for (int i = 0; i < rgb.length; i++) {
            rgb[i] = background;
        }

        int cellWidth = width / 2;
        int cellHeight = height / 3;

        fillRadialGradient(rgb, width, 0, 0, cellWidth, cellHeight, 0xff, 0xff00, null, null);
        fillRadialGradient(rgb, width, cellWidth, 0, cellWidth, cellHeight, 0xff, 0xff00, 20, 200);

        fillLinearGradient(rgb, width, 0, cellHeight, cellWidth, cellHeight, 0xff, 0x999999, true);
        fillRectRadialGradient(rgb, width, cellWidth, cellHeight, cellWidth, cellHeight, 0xff0000, 0xcccccc, 0.5f, 0.5f, 2f);

        fillLinearGradient(rgb, width, 0, cellHeight * 2, cellWidth, cellHeight, 0xff, 0x999999, false);

        return Image.createImage(rgb, width, height);
    }

    private void fillLinearGradient(int[] rgb, int imageWidth, int x, int y, int width, int height, int startColor, int endColor, boolean horizontal) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int max = Math.max(1, horizontal ? width - 1 : height - 1);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int offset = (y + row) * imageWidth + x + col;
                float ratio = horizontal ? (float) col / max : (float) row / max;
                rgb[offset] = blendColor(startColor, endColor, ratio);
            }
        }
    }

    private void fillRectRadialGradient(int[] rgb, int imageWidth, int x, int y, int width, int height, int startColor, int endColor,
                                        float relativeX, float relativeY, float relativeSize) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int end = 0xff000000 | (endColor & 0xffffff);
        for (int row = 0; row < height; row++) {
            int offset = (y + row) * imageWidth + x;
            for (int col = 0; col < width; col++) {
                rgb[offset + col] = end;
            }
        }

        float centerX = x + width * (1f - relativeX);
        float centerY = y + height * (1f - relativeY);
        float radius = Math.min(width, height) * relativeSize / 2f;
        if (radius <= 0f) {
            return;
        }
        for (int row = 0; row < height; row++) {
            float dy = (y + row + 0.5f) - centerY;
            for (int col = 0; col < width; col++) {
                float dx = (x + col + 0.5f) - centerX;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist <= radius) {
                    float ratio = dist / radius;
                    int offset = (y + row) * imageWidth + x + col;
                    rgb[offset] = blendColor(startColor, endColor, ratio);
                }
            }
        }
    }

    private void fillRadialGradient(int[] rgb, int imageWidth, int x, int y, int width, int height, int startColor, int endColor,
                                    Integer startAngle, Integer arcAngle) {
        if (width <= 0 || height <= 0) {
            return;
        }
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        float radius = Math.min(width, height) / 2f;
        if (radius <= 0f) {
            return;
        }
        for (int row = 0; row < height; row++) {
            float dy = (y + row + 0.5f) - centerY;
            for (int col = 0; col < width; col++) {
                float dx = (x + col + 0.5f) - centerX;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > radius) {
                    continue;
                }
                if (startAngle != null && arcAngle != null) {
                    double angle = Math.toDegrees(Math.atan2(-dy, dx));
                    if (angle < 0) {
                        angle += 360;
                    }
                    if (!angleInArc(angle, startAngle, arcAngle)) {
                        continue;
                    }
                }
                float ratio = dist / radius;
                int offset = (y + row) * imageWidth + x + col;
                rgb[offset] = blendColor(startColor, endColor, ratio);
            }
        }
    }

    private boolean angleInArc(double angle, int startAngle, int arcAngle) {
        int normalizedStart = ((startAngle % 360) + 360) % 360;
        int normalizedEnd = (normalizedStart + arcAngle) % 360;
        if (arcAngle >= 360 || arcAngle <= -360) {
            return true;
        }
        if (arcAngle >= 0) {
            if (normalizedStart <= normalizedEnd) {
                return angle >= normalizedStart && angle <= normalizedEnd;
            }
            return angle >= normalizedStart || angle <= normalizedEnd;
        }
        if (normalizedEnd <= normalizedStart) {
            return angle <= normalizedStart && angle >= normalizedEnd;
        }
        return angle <= normalizedStart || angle >= normalizedEnd;
    }

    private int blendColor(int startColor, int endColor, float ratio) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        int sr = (startColor >> 16) & 0xff;
        int sg = (startColor >> 8) & 0xff;
        int sb = startColor & 0xff;
        int er = (endColor >> 16) & 0xff;
        int eg = (endColor >> 8) & 0xff;
        int eb = endColor & 0xff;
        int r = Math.round(sr + (er - sr) * ratio);
        int g = Math.round(sg + (eg - sg) * ratio);
        int b = Math.round(sb + (eb - sb) * ratio);
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }
}
