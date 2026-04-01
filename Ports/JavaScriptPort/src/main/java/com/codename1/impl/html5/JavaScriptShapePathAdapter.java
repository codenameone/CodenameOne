/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import com.codename1.ui.Stroke;
import com.codename1.ui.geom.PathIterator;
import com.codename1.ui.geom.Shape;

public final class JavaScriptShapePathAdapter {
    private JavaScriptShapePathAdapter() {
    }

    public interface PathSink {
        void moveTo(float x, float y);
        void closePath();
        void lineTo(float x, float y);
        void quadraticCurveTo(float cpx, float cpy, float x, float y);
        void bezierCurveTo(float cp1x, float cp1y, float cp2x, float cp2y, float x, float y);
    }

    public interface StrokeStyleSink {
        void setLineWidth(float width);
        void setLineJoin(String join);
        void setMiterLimit(float limit);
        void setLineCap(String cap);
    }

    public static void addShapeToPath(PathSink sink, Shape shape) {
        PathIterator it = shape.getPathIterator();
        float[] points = new float[6];
        while (!it.isDone()) {
            int type = it.currentSegment(points);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    sink.moveTo(points[0], points[1]);
                    break;
                case PathIterator.SEG_CLOSE:
                    sink.closePath();
                    break;
                case PathIterator.SEG_LINETO:
                    sink.lineTo(points[0], points[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    sink.quadraticCurveTo(points[0], points[1], points[2], points[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    sink.bezierCurveTo(points[0], points[1], points[2], points[3], points[4], points[5]);
                    break;
                default:
                    break;
            }
            it.next();
        }
    }

    public static void applyStrokeStyle(StrokeStyleSink sink, Stroke stroke) {
        sink.setLineWidth(stroke.getLineWidth());
        sink.setLineJoin(resolveJoin(stroke.getJoinStyle()));
        sink.setMiterLimit(stroke.getMiterLimit());
        sink.setLineCap(resolveCap(stroke.getCapStyle()));
    }

    public static String resolveJoin(int joinStyle) {
        switch (joinStyle) {
            case Stroke.JOIN_BEVEL:
                return "bevel";
            case Stroke.JOIN_ROUND:
                return "round";
            case Stroke.JOIN_MITER:
            default:
                return "miter";
        }
    }

    public static String resolveCap(int capStyle) {
        switch (capStyle) {
            case Stroke.CAP_ROUND:
                return "round";
            case Stroke.CAP_SQUARE:
                return "square";
            case Stroke.CAP_BUTT:
            default:
                return "butt";
        }
    }
}
