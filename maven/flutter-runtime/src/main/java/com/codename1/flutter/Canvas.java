/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.flutter;

/**
 * The drawing surface handed to a CustomPainter — Flutter's dart:ui
 * {@code Canvas}. This milestone provides the API surface (so painters can be
 * transpiled and their draw/transform calls resolve); the concrete backend
 * that binds these calls to a Codename One {@code Graphics} is supplied by the
 * render layer.
 */
public class Canvas {

    /// The recorder this canvas writes into, when it was made as
    /// {@code Canvas(recorder)}; null for a canvas a painter is handed.
    private PictureRecorder recorder;

    public Canvas() {
    }

    /// {@code Canvas(recorder, [cullRect])}: every call is recorded into the recorder
    /// and replayed when the resulting Picture is drawn or turned into an image. The
    /// cull rect is accepted and not applied.
    public Canvas(PictureRecorder recorder, Rect cullRect) {
        this.recorder = recorder;
        if (recorder != null) {
            recorder.begin();
        }
    }

    private void rec(String op, Object... args) {
        if (recorder != null) {
            recorder.record(op, args);
        }
    }

    public void drawPath(Path path, Paint paint) {
        rec("drawPath", path, paint);
    }

    public void drawRect(Rect rect, Paint paint) {
        rec("drawRect", rect, paint);
    }

    public void drawRRect(RRect rrect, Paint paint) {
        rec("drawRRect", rrect, paint);
    }

    public void drawCircle(Offset c, double radius, Paint paint) {
        rec("drawCircle", c, radius, paint);
    }

    public void drawOval(Rect rect, Paint paint) {
        rec("drawOval", rect, paint);
    }

    public void drawLine(Offset p1, Offset p2, Paint paint) {
        rec("drawLine", p1, p2, paint);
    }

    public void drawArc(Rect rect, double startAngle, double sweepAngle, boolean useCenter, Paint paint) {
        rec("drawArc", rect, startAngle, sweepAngle, useCenter, paint);
    }

    public void drawPoints(Object pointMode, Object points, Paint paint) {
        rec("drawPoints", pointMode, points, paint);
    }

    public void drawColor(Color color, Object blendMode) {
        rec("drawColor", color, blendMode);
    }

    public void drawShadow(Path path, Color color, double elevation, boolean transparentOccluder) {
        rec("drawShadow", path, color, elevation, transparentOccluder);
    }

    public void drawVertices(Object vertices, Object blendMode, Paint paint) {
        rec("drawVertices", vertices, blendMode, paint);
    }

    public void drawImage(Object image, Offset offset, Paint paint) {
        rec("drawImage", image, offset, paint);
    }

    public void translate(double dx, double dy) {
        rec("translate", dx, dy);
    }

    public void scale(double sx, double sy) {
        rec("scale", sx, sy);
    }

    public void rotate(double radians) {
        rec("rotate", radians);
    }

    public void skew(double sx, double sy) {
        rec("skew", sx, sy);
    }

    public void save() {
        rec("save");
    }

    public void saveLayer(Rect bounds, Paint paint) {
        rec("saveLayer", bounds, paint);
    }

    public void restore() {
        rec("restore");
    }

    public void clipRect(Rect rect) {
        rec("clipRect", rect);
    }

    public void clipRRect(RRect rrect) {
        rec("clipRRect", rrect);
    }

    public void clipPath(Path path) {
        rec("clipPath", path);
    }

    /// Replays one recorded call onto {@code target}.
    static void replay(Canvas target, String op, Object[] a) {
        switch (op) {
            case "drawPath":
                target.drawPath((Path) a[0], (Paint) a[1]);
                break;
            case "drawRect":
                target.drawRect((Rect) a[0], (Paint) a[1]);
                break;
            case "drawRRect":
                target.drawRRect((RRect) a[0], (Paint) a[1]);
                break;
            case "drawCircle":
                target.drawCircle((Offset) a[0], ((Double) a[1]).doubleValue(), (Paint) a[2]);
                break;
            case "drawOval":
                target.drawOval((Rect) a[0], (Paint) a[1]);
                break;
            case "drawLine":
                target.drawLine((Offset) a[0], (Offset) a[1], (Paint) a[2]);
                break;
            case "drawArc":
                target.drawArc((Rect) a[0], ((Double) a[1]).doubleValue(), ((Double) a[2]).doubleValue(), ((Boolean) a[3]).booleanValue(), (Paint) a[4]);
                break;
            case "drawPoints":
                target.drawPoints(a[0], a[1], (Paint) a[2]);
                break;
            case "drawColor":
                target.drawColor((Color) a[0], a[1]);
                break;
            case "drawShadow":
                target.drawShadow((Path) a[0], (Color) a[1], ((Double) a[2]).doubleValue(), ((Boolean) a[3]).booleanValue());
                break;
            case "drawVertices":
                target.drawVertices(a[0], a[1], (Paint) a[2]);
                break;
            case "drawImage":
                target.drawImage(a[0], (Offset) a[1], (Paint) a[2]);
                break;
            case "translate":
                target.translate(((Double) a[0]).doubleValue(), ((Double) a[1]).doubleValue());
                break;
            case "scale":
                target.scale(((Double) a[0]).doubleValue(), ((Double) a[1]).doubleValue());
                break;
            case "rotate":
                target.rotate(((Double) a[0]).doubleValue());
                break;
            case "skew":
                target.skew(((Double) a[0]).doubleValue(), ((Double) a[1]).doubleValue());
                break;
            case "save":
                target.save();
                break;
            case "saveLayer":
                target.saveLayer((Rect) a[0], (Paint) a[1]);
                break;
            case "restore":
                target.restore();
                break;
            case "clipRect":
                target.clipRect((Rect) a[0]);
                break;
            case "clipRRect":
                target.clipRRect((RRect) a[0]);
                break;
            case "clipPath":
                target.clipPath((Path) a[0]);
                break;
            default:
                break;
        }
    }
}
