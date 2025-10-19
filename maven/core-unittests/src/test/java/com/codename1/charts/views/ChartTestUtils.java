package com.codename1.charts.views;

import com.codename1.charts.compat.Canvas;
import com.codename1.charts.compat.Paint;
import com.codename1.ui.geom.Rectangle2D;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility helpers used by chart related unit tests.
 */
class ChartTestUtils {
    private static final sun.misc.Unsafe UNSAFE;

    static {
        try {
            Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe) field.get(null);
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private ChartTestUtils() {
    }

    static <T> T allocateInstance(Class<T> type) {
        try {
            return type.cast(UNSAFE.allocateInstance(type));
        } catch (InstantiationException e) {
            throw new IllegalStateException("Failed to allocate instance for " + type, e);
        }
    }

    static final class RecordingCanvas extends Canvas {
        List<float[]> rectangles;
        List<float[]> circles;
        List<ArcCall> arcs;
        List<float[]> lines;
        List<TextCall> texts;
        int width = 100;
        int height = 100;

        RecordingCanvas prepare() {
            rectangles = new ArrayList<float[]>();
            circles = new ArrayList<float[]>();
            arcs = new ArrayList<ArcCall>();
            lines = new ArrayList<float[]>();
            texts = new ArrayList<TextCall>();
            return this;
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom, Paint paint) {
            if (rectangles == null) {
                prepare();
            }
            rectangles.add(new float[]{left, top, right, bottom});
        }

        @Override
        public void drawCircle(float cx, float cy, float r, Paint paint) {
            if (circles == null) {
                prepare();
            }
            circles.add(new float[]{cx, cy, r});
        }

        @Override
        public void drawArc(Rectangle2D oval, float currentAngle, float sweepAngle, boolean useCenter, Paint paint) {
            if (arcs == null) {
                prepare();
            }
            arcs.add(new ArcCall(oval, currentAngle, sweepAngle, useCenter));
        }

        @Override
        public void drawLine(float x1, float y1, float x2, float y2, Paint paint) {
            if (lines == null) {
                prepare();
            }
            lines.add(new float[]{x1, y1, x2, y2});
        }

        @Override
        public void drawText(String string, float x, float y, Paint paint) {
            if (texts == null) {
                prepare();
            }
            texts.add(new TextCall(string, x, y));
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public boolean isShapeClipSupported() {
            return false;
        }
    }

    static final class ArcCall {
        final Rectangle2D oval;
        final float startAngle;
        final float sweepAngle;
        final boolean useCenter;

        ArcCall(Rectangle2D oval, float startAngle, float sweepAngle, boolean useCenter) {
            this.oval = oval;
            this.startAngle = startAngle;
            this.sweepAngle = sweepAngle;
            this.useCenter = useCenter;
        }
    }

    static final class TextCall {
        final String text;
        final float x;
        final float y;

        TextCall(String text, float x, float y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }
    }
}
