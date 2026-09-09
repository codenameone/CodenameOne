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
package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Canvas;
import com.codename1.flutter.Color;
import com.codename1.flutter.Offset;
import com.codename1.flutter.Paint;
import com.codename1.flutter.PaintingStyle;
import com.codename1.flutter.Path;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.animation.Curve;
import com.codename1.flutter.rendering.CustomPainter;
import com.codename1.flutter.rendering.Size;

import dart.core.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * The Flutter logo as a widget — Flutter's {@code FlutterLogo}.
 *
 * <p>Drawn rather than declared: it rendered nothing at all before, which left a hole in
 * every screen that used it as sample content. The mark is four flat polygons, so it is
 * exact at any size with no asset to ship.</p>
 */
public class FlutterLogo extends StatelessWidget {

    /** Flutter's default logo size in logical pixels. */
    private static final double DEFAULT_SIZE_LP = 24;

    private double size = DEFAULT_SIZE_LP;
    private Color textColor;
    private Object style;
    private Duration duration;
    private Curve curve;

    public void size(double v) {
        this.size = v;
    }

    public void textColor(Color v) {
        this.textColor = v;
    }

    public void style(Object v) {
        this.style = v;
    }

    public void duration(Duration v) {
        this.duration = v;
    }

    public void curve(Curve v) {
        this.curve = v;
    }

    @Override
    public Widget build(BuildContext context) {
        CustomPaint paint = new CustomPaint();
        paint.painter(new LogoPainter());
        paint.size(new Size(size, size));
        return paint;
    }

    /**
     * Paints the mark from its polygons, in a square box.
     *
     * <p>Coordinates are the official artwork's, normalised out of its 256x317 frame, so
     * the proportions hold at any size. The logo is not square, so it is centred in the box
     * the way Flutter's own painter does.</p>
     */
    static final class LogoPainter extends CustomPainter {

        /** Light beam, dark fold, and the mid-blue shadow between them. */
        private static final int LIGHT = 0x47C5FB;
        private static final int DARK = 0x00569E;
        private static final int MID = 0x00B5F8;

        private static final double ART_W = 256;
        private static final double ART_H = 317;

        @Override
        public void paint(Canvas canvas, Size box) {
            double scale = Math.min(box.width() / ART_W, box.height() / ART_H);
            double dx = (box.width() - ART_W * scale) / 2;
            double dy = (box.height() - ART_H * scale) / 2;

            // Upper beam.
            fill(canvas, scale, dx, dy, LIGHT,
                    157.7, 0, 0, 157.7, 48.8, 206.5, 255.3, 0);
            // Lower beam.
            fill(canvas, scale, dx, dy, LIGHT,
                    156.6, 145.2, 73.0, 228.8, 121.9, 278.7, 170.6, 230.0, 256.3, 145.2);
            // The fold, in the darker blue.
            fill(canvas, scale, dx, dy, DARK,
                    121.9, 278.7, 159.0, 315.8, 255.3, 315.8, 170.7, 230.1);
            // The small shadow where the beams meet.
            fill(canvas, scale, dx, dy, MID,
                    72.4, 229.3, 121.2, 180.5, 170.6, 230.0, 121.9, 278.7);
        }

        private static void fill(Canvas canvas, double scale, double dx, double dy,
                int rgb, double... xy) {
            List<Offset> points = new ArrayList<Offset>();
            for (int i = 0; i + 1 < xy.length; i += 2) {
                points.add(new Offset(dx + xy[i] * scale, dy + xy[i + 1] * scale));
            }
            Path p = new Path();
            p.addPolygon(points, true);
            Paint paint = new Paint();
            paint.color(new Color(0xFF000000 | rgb));
            paint.style(PaintingStyle.fill);
            canvas.drawPath(p, paint);
        }

        @Override
        public boolean shouldRepaint(CustomPainter oldDelegate) {
            return !(oldDelegate instanceof LogoPainter);
        }
    }
}
