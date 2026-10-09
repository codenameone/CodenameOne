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

import javafx.geometry.Insets;
import javafx.scene.layout.Region;

/// A bar that fills with progress.
///
/// The bar is two regions a style sheet reaches as it does in JavaFX: a
/// `.track` the size of the control and, in it, a `.bar` whose width is
/// the progress, from 0 to 1. Until a sheet says otherwise the track is
/// the standard theme's inset well and the bar its accent colour. A
/// negative progress is indeterminate: a segment of the track runs from
/// one end to the other and back while the bar is in a scene.
///
/// The preferred size is 100 by 18 logical pixels, and `setPrefWidth`
/// makes a longer or a shorter bar.
public class ProgressBar extends ProgressIndicator {

    private static final double DEFAULT_WIDTH = 100;
    private static final double DEFAULT_HEIGHT = 18;
    private static final double SEGMENT = 0.4;
    private static final long SWEEP_MILLIS = 1200;

    private final Region track = new Part("track",
            "-fx-background-color: -fx-shadow-highlight-color, -fx-text-box-border,"
                    + " derive(-fx-control-inner-background, -4%); -fx-background-insets: 0, 0 0 1 0, 1 1 2 1;"
                    + " -fx-background-radius: 4, 3, 2;");
    private final Region bar = new Part("bar",
            "-fx-background-color: -fx-accent; -fx-background-insets: 3 3 4 3; -fx-background-radius: 2;");

    /// One of the two regions of a bar, with the standard theme's look
    /// below whatever a style sheet gives it.
    private static final class Part extends Region {
        private final String look;

        Part(String styleClass, String look) {
            this.look = look;
            getStyleClass().add(styleClass);
            setManaged(false);
            setMouseTransparent(true);
        }

        @Override
        public String cn1DefaultStyle() {
            return look;
        }
    }

    /// Creates an indeterminate progress bar.
    public ProgressBar() {
        this(INDETERMINATE_PROGRESS);
    }

    /// Creates a progress bar at a progress.
    public ProgressBar(double progress) {
        super(progress, false);
        getStyleClass().setAll("progress-bar");
        cn1Children().add(track);
        cn1Children().add(bar);
        built = true;
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
    void paintProgress(com.codename1.fxcompat.runtime.Renderer renderer, double x, double y, double w, double h) {
        // The two regions are the bar.
    }

    @Override
    void progressShown() {
        Insets in = getInsets();
        double x = in.getLeft();
        double y = in.getTop();
        double w = Math.max(0, getWidth() - x - in.getRight());
        double h = Math.max(0, getHeight() - y - in.getBottom());
        track.resizeRelocate(x, y, w, h);
        double[] part = filledPart(getProgress(), frameMillis());
        double filled = w * part[1];
        bar.setVisible(filled > 0);
        bar.resizeRelocate(x + w * part[0], y, filled, h);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        progressShown();
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
