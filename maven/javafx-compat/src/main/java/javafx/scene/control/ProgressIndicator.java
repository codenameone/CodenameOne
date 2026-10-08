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

import com.codename1.components.InfiniteProgress;
import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.FxPath;
import com.codename1.fxcompat.runtime.Renderer;
import com.codename1.ui.Component;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/// A round indicator of progress.
///
/// A negative progress is indeterminate and shows the Codename One
/// `InfiniteProgress` spinner. From 0 to 1 the control draws a disc that
/// fills clockwise from the top; the percentage JavaFX prints below it is
/// not drawn. The pseudo-classes `indeterminate` and `determinate` follow
/// the progress.
public class ProgressIndicator extends Control {

    /// The progress of an indicator that does not know how far it is.
    public static final double INDETERMINATE_PROGRESS = -1;

    private static final int PROGRESS = Dirty.USER;
    private static final double DEFAULT_SIZE = 24;
    private static final PseudoClass INDETERMINATE = PseudoClass.getPseudoClass("indeterminate");
    private static final PseudoClass DETERMINATE = PseudoClass.getPseudoClass("determinate");
    private static final Color TRACK = Color.gray(0.75);
    private static final Color DONE = Color.rgb(0, 122, 204);

    private final DoubleProperty progress = new FxDouble(this, "progress", INDETERMINATE_PROGRESS,
            PROGRESS | Dirty.NATIVE | Dirty.PAINT);
    private final ReadOnlyBooleanWrapper indeterminate = new ReadOnlyBooleanWrapper(this, "indeterminate", true);

    /// Creates an indeterminate indicator.
    public ProgressIndicator() {
        this(INDETERMINATE_PROGRESS);
    }

    /// Creates an indicator at a progress.
    public ProgressIndicator(double progress) {
        getStyleClass().add("progress-indicator");
        setFocusTraversable(false);
        pseudoClassStateChanged(INDETERMINATE, true);
        setProgress(progress);
    }

    @Override
    protected Component cn1CreateNative() {
        InfiniteProgress p = new InfiniteProgress();
        p.setMaterialDesignMode(true);
        return p;
    }

    @Override
    protected void cn1SyncNative() {
        super.cn1SyncNative();
        Component c = cn1NativeIfCreated();
        if (c instanceof InfiniteProgress) {
            c.setVisible(isIndeterminate());
        }
    }

    @Override
    public void cn1Invalidated(int what) {
        if ((what & PROGRESS) != 0) {
            boolean unknown = getProgress() < 0;
            indeterminate.set(unknown);
            pseudoClassStateChanged(INDETERMINATE, unknown);
            pseudoClassStateChanged(DETERMINATE, !unknown);
        }
        super.cn1Invalidated(what);
    }

    /// Returns whether this indicator draws the determinate disc itself;
    /// a progress bar leaves everything to its native component.
    boolean drawsDisc() {
        return true;
    }

    @Override
    public void cn1Paint(Renderer renderer) {
        super.cn1Paint(renderer);
        if (!drawsDisc() || isIndeterminate()) {
            return;
        }
        Insets in = getInsets();
        double w = getWidth() - in.getLeft() - in.getRight();
        double h = getHeight() - in.getTop() - in.getBottom();
        double d = Math.min(w, h);
        if (!(d > 2)) {
            return;
        }
        double r = d / 2 - 1;
        double cx = in.getLeft() + w / 2;
        double cy = in.getTop() + h / 2;
        FxPath ring = new FxPath();
        ring.addEllipse(cx, cy, r, r);
        renderer.stroke(ring, TRACK, 1, StrokeLineCap.BUTT, StrokeLineJoin.MITER, 10, null, 0);
        double p = Math.min(1, getProgress());
        if (p > 0) {
            FxPath pie = new FxPath();
            if (p >= 1) {
                pie.addEllipse(cx, cy, r, r);
            } else {
                pie.moveTo(cx, cy);
                pie.addArc(cx, cy, r, r, 90, -360 * p, true);
                pie.closePath();
            }
            renderer.fill(pie, DONE, cx - r, cy - r, 2 * r, 2 * r);
        }
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        return Math.max(super.computePrefWidth(height), in.getLeft() + DEFAULT_SIZE + in.getRight());
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        return Math.max(super.computePrefHeight(width), in.getTop() + DEFAULT_SIZE + in.getBottom());
    }

    /// Returns the progress: negative for unknown, else 0 to 1.
    public final double getProgress() {
        return progress.get();
    }

    /// Sets the progress: negative for unknown, else 0 to 1.
    public final void setProgress(double value) {
        progress.set(value);
    }

    /// The progress.
    public final DoubleProperty progressProperty() {
        return progress;
    }

    /// Returns whether the progress is unknown.
    public final boolean isIndeterminate() {
        return indeterminate.get();
    }

    /// Whether the progress is unknown.
    public final ReadOnlyBooleanProperty indeterminateProperty() {
        return indeterminate.getReadOnlyProperty();
    }
}
