/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package javafx.scene.control;

import com.codename1.fxcompat.runtime.FxPath;
import com.codename1.fxcompat.runtime.Renderer;

import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/// A bar that fills with progress, drawn by the control itself.
///
/// The bar is a rounded track with, inside it, a filled part in the
/// accent colour whose width is the progress, from 0 to 1. A negative
/// progress is indeterminate: a segment of the track runs from one end
/// to the other and back while the bar is in a scene.
///
/// The preferred size is 100 by 20 logical pixels, and `setPrefWidth`
/// makes a longer or a shorter bar.
public class ProgressBar extends ProgressIndicator {

    private static final double DEFAULT_WIDTH = 100;
    private static final double DEFAULT_HEIGHT = 20;
    private static final double EDGE = 2;
    private static final double SEGMENT = 0.4;
    private static final long SWEEP_MILLIS = 1200;
    private static final Color TRACK = Color.gray(0.96);
    private static final Color TRACK_EDGE = Color.gray(0.73);

    /// Creates an indeterminate progress bar.
    public ProgressBar() {
        this(INDETERMINATE_PROGRESS);
    }

    /// Creates a progress bar at a progress.
    public ProgressBar(double progress) {
        super(progress);
        getStyleClass().setAll("progress-bar");
    }

    /// Returns the part of the track that is filled, as
    /// `{start, length}` in fractions of the track: the progress from the
    /// start, or where the running segment is at a time in milliseconds.
    static double[] filledPart(double progress, long millis) {
        if (progress >= 0) {
            return new double[] {0, Math.min(1, progress)};
        }
        long t = millis % (2 * SWEEP_MILLIS);
        double along = t < SWEEP_MILLIS ? t / (double) SWEEP_MILLIS : 2 - t / (double) SWEEP_MILLIS;
        return new double[] {along * (1 - SEGMENT), SEGMENT};
    }

    @Override
    void paintProgress(Renderer renderer, double x, double y, double w, double h) {
        double radius = Math.min(3, Math.min(w, h) / 2);
        FxPath track = new FxPath();
        track.addRoundRect(x + 0.5, y + 0.5, w - 1, h - 1, radius, radius);
        renderer.fill(track, TRACK, x, y, w, h);
        renderer.stroke(track, TRACK_EDGE, 1, StrokeLineCap.BUTT, StrokeLineJoin.MITER, 10, null, 0);
        double innerW = w - 2 * EDGE;
        double innerH = h - 2 * EDGE;
        if (!(innerW > 0) || !(innerH > 0)) {
            return;
        }
        double[] part = filledPart(getProgress(), frameMillis());
        double fillW = innerW * part[1];
        if (fillW > 0) {
            double r = Math.min(2, Math.min(fillW, innerH) / 2);
            double fx = x + EDGE + innerW * part[0];
            FxPath fill = new FxPath();
            fill.addRoundRect(fx, y + EDGE, fillW, innerH, r, r);
            renderer.fill(fill, ACCENT, fx, y + EDGE, fillW, innerH);
        }
    }

    @Override
    double contentPrefWidth() {
        return DEFAULT_WIDTH;
    }

    @Override
    double contentPrefHeight() {
        return DEFAULT_HEIGHT;
    }
}
