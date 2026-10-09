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

import com.codename1.fxcompat.runtime.CssEngine;
import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.Fonts;
import com.codename1.fxcompat.runtime.FrameClock;
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.FxPath;
import com.codename1.fxcompat.runtime.Renderer;
import com.codename1.ui.Component;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/// A round indicator of progress, drawn by the control itself: it has no
/// native component, so it looks and measures the same on every theme.
///
/// A negative progress is indeterminate and shows a ring of dots, the
/// brightest of which runs round the ring while the indicator is in a
/// scene. From 0 to 1 the control draws a disc that fills clockwise from
/// the top and, below it, the percentage; at 1 the disc is full, carries
/// a tick, and the text is `Done`. The pseudo-classes `indeterminate` and
/// `determinate` follow the progress.
///
/// The preferred size is that of a small disc with its line of text below
/// it; a larger indicator is asked for with `setPrefSize`.
public class ProgressIndicator extends Control {

    /// The progress of an indicator that does not know how far it is.
    public static final double INDETERMINATE_PROGRESS = -1;

    /// The colour of the part that is done where no rule defines
    /// `-fx-progress-color` or `-fx-accent`: the accent of JavaFX.
    static final Color ACCENT = Color.rgb(0, 150, 201);

    private static final int PROGRESS = Dirty.USER;
    private static final double DEFAULT_DISC = 32;
    private static final double TEXT_GAP = 2;
    private static final int DOTS = 12;
    private static final long STEP_MILLIS = 100;
    private static final String DONE_TEXT = "Done";
    private static final PseudoClass INDETERMINATE = PseudoClass.getPseudoClass("indeterminate");
    private static final PseudoClass DETERMINATE = PseudoClass.getPseudoClass("determinate");
    private static final Color TRACK = Color.gray(0.73);

    /// The text below the disc, which a style sheet reaches as
    /// `.percentage`; `null` in a bar, which has none.
    private final Text percentage;

    private final DoubleProperty progress = new FxDouble(this, "progress", INDETERMINATE_PROGRESS,
            PROGRESS | Dirty.PAINT);
    private final ReadOnlyBooleanWrapper indeterminate = new ReadOnlyBooleanWrapper(this, "indeterminate", true);
    private long frameMillis;

    /// Whether the constructors are through: the progress is set by the
    /// first of them, before a bar has the regions it would place.
    boolean built;
    private final FrameClock.Pulse frames = new FrameClock.Pulse() {
        @Override
        public void pulse(long nowNanos) {
            frameMillis = nowNanos / 1000000L;
            // A peer that is on no form is not drawn.
            progressShown();
            if (cn1HasPeer() && cn1Peer().getComponentForm() != null) {
                cn1Repaint();
            }
        }
    };

    /// Creates an indeterminate indicator.
    public ProgressIndicator() {
        this(INDETERMINATE_PROGRESS);
    }

    /// Creates an indicator at a progress.
    public ProgressIndicator(double progress) {
        this(progress, true);
        built = true;
    }

    /// Creates an indicator with or without the line of text below it.
    ProgressIndicator(double progress, boolean text) {
        if (text) {
            percentage = new Percentage();
            cn1Children().add(percentage);
        } else {
            percentage = null;
        }
        getStyleClass().add("progress-indicator");
        setFocusTraversable(false);
        pseudoClassStateChanged(INDETERMINATE, true);
        sceneProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                animate();
            }
        });
        setProgress(progress);
    }

    @Override
    protected Component cn1CreateNative() {
        return null;
    }

    /// Runs the frames of the indeterminate look while it can be seen:
    /// the progress is unknown and the indicator is in a scene.
    private void animate() {
        if (isIndeterminate() && getScene() != null) {
            FrameClock.add(frames);
        } else {
            FrameClock.remove(frames);
        }
    }

    @Override
    public void cn1Invalidated(int what) {
        if ((what & PROGRESS) != 0) {
            boolean unknown = getProgress() < 0;
            indeterminate.set(unknown);
            pseudoClassStateChanged(INDETERMINATE, unknown);
            pseudoClassStateChanged(DETERMINATE, !unknown);
            animate();
            if (built) {
                progressShown();
            }
        }
        super.cn1Invalidated(what);
    }

    /// Returns the time of the frame the indeterminate look is at, in
    /// milliseconds.
    final long frameMillis() {
        return frameMillis;
    }

    /// The text of an indicator: the text of the scene's background until
    /// a style sheet fills it otherwise.
    private static final class Percentage extends Text {
        Percentage() {
            getStyleClass().add("percentage");
            setManaged(false);
            setMouseTransparent(true);
            setTextOrigin(javafx.geometry.VPos.TOP);
        }

        @Override
        public String cn1DefaultStyle() {
            return "-fx-fill: -fx-text-background-color;";
        }
    }

    /// Returns the colour of the part that is done: `-fx-progress-color`
    /// where a rule defines it, which the standard theme does as the
    /// accent colour.
    final Color progressColor() {
        Color c = CssEngine.themeColor(this, "-fx-progress-color");
        return c == null ? ACCENT : c;
    }

    /// Brings what the indicator is made of up to date with its progress
    /// and its size: here the text, in a bar its two regions.
    void progressShown() {
        if (percentage == null) {
            return;
        }
        boolean shown = !isIndeterminate();
        percentage.setVisible(shown);
        if (!shown) {
            return;
        }
        String text = progressText();
        if (!text.equals(percentage.getText())) {
            percentage.setText(text);
        }
        Insets in = getInsets();
        double w = getWidth() - in.getLeft() - in.getRight();
        double h = getHeight() - in.getTop() - in.getBottom();
        Font font = percentage.getFont();
        double below = TEXT_GAP + Fonts.lineHeight(font);
        double d = Math.min(w, h - below);
        if (!(d > 2)) {
            percentage.setVisible(false);
            return;
        }
        double top = in.getTop() + Math.max(0, (h - d - below) / 2);
        percentage.relocate(in.getLeft() + (w - Fonts.width(font, text)) / 2, top + d + TEXT_GAP);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        progressShown();
    }

    /// Returns the text shown below the disc of a determinate indicator.
    private String progressText() {
        double p = getProgress();
        if (p >= 1) {
            return DONE_TEXT;
        }
        return ((int) Math.round(Math.max(0, p) * 100)) + "%";
    }

    @Override
    public void cn1Paint(Renderer renderer) {
        super.cn1Paint(renderer);
        Insets in = getInsets();
        double w = getWidth() - in.getLeft() - in.getRight();
        double h = getHeight() - in.getTop() - in.getBottom();
        if (w > 0 && h > 0) {
            paintProgress(renderer, in.getLeft(), in.getTop(), w, h);
        }
    }

    /// Draws the indicator in a rectangle, the control less its insets; a
    /// progress bar draws a bar instead.
    void paintProgress(Renderer renderer, double x, double y, double w, double h) {
        if (isIndeterminate()) {
            paintRing(renderer, x, y, w, h);
            return;
        }
        Font font = percentage == null ? Font.getDefault() : percentage.getFont();
        Color done = progressColor();
        double below = TEXT_GAP + Fonts.lineHeight(font);
        double d = Math.min(w, h - below);
        if (!(d > 2)) {
            // No room for the text: the disc alone.
            d = Math.min(w, h);
            below = 0;
        }
        if (!(d > 2)) {
            return;
        }
        double r = d / 2 - 0.5;
        double cx = x + w / 2;
        double top = y + Math.max(0, (h - d - below) / 2);
        double cy = top + d / 2;
        double p = Math.min(1, getProgress());
        FxPath disc = new FxPath();
        disc.addEllipse(cx, cy, r, r);
        if (p >= 1) {
            renderer.fill(disc, done, cx - r, cy - r, 2 * r, 2 * r);
            FxPath tick = new FxPath();
            tick.moveTo(cx - r * 0.45, cy + r * 0.05);
            tick.lineTo(cx - r * 0.1, cy + r * 0.4);
            tick.lineTo(cx + r * 0.5, cy - r * 0.35);
            renderer.stroke(tick, Color.WHITE, Math.max(1, r * 0.2), StrokeLineCap.ROUND, StrokeLineJoin.ROUND, 10,
                    null, 0);
        } else {
            renderer.fill(disc, Color.WHITE, cx - r, cy - r, 2 * r, 2 * r);
            if (p > 0) {
                FxPath pie = new FxPath();
                pie.moveTo(cx, cy);
                pie.addArc(cx, cy, r, r, 90, -360 * p, true);
                pie.closePath();
                renderer.fill(pie, done, cx - r, cy - r, 2 * r, 2 * r);
            }
            renderer.stroke(disc, TRACK, 1, StrokeLineCap.BUTT, StrokeLineJoin.MITER, 10, null, 0);
        }
    }

    private void paintRing(Renderer renderer, double x, double y, double w, double h) {
        double d = Math.min(w, h);
        if (!(d > 4)) {
            return;
        }
        double dot = d * 0.06;
        double ring = d / 2 - dot;
        double cx = x + w / 2;
        double cy = y + h / 2;
        Color done = progressColor();
        int lead = (int) ((frameMillis / STEP_MILLIS) % DOTS);
        for (int i = 0; i < DOTS; i++) {
            // The dot the lead left longest ago is the faintest.
            int age = (lead - i + DOTS) % DOTS;
            double strength = 1 - age / (double) DOTS;
            double angle = 2 * Math.PI * i / DOTS - Math.PI / 2;
            double px = cx + ring * Math.cos(angle);
            double py = cy + ring * Math.sin(angle);
            FxPath p = new FxPath();
            p.addEllipse(px, py, dot, dot);
            renderer.fill(p, done.deriveColor(0, 1, 1, 0.15 + 0.85 * strength), px - dot, py - dot, 2 * dot,
                    2 * dot);
        }
    }

    /// Returns the preferred width inside the insets.
    double contentPrefWidth() {
        Font font = Font.getDefault();
        return Math.max(DEFAULT_DISC, Math.max(Fonts.width(font, DONE_TEXT), Fonts.width(font, "100%")));
    }

    /// Returns the preferred height inside the insets.
    double contentPrefHeight() {
        return DEFAULT_DISC + TEXT_GAP + Fonts.lineHeight(Font.getDefault());
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + snapSizeX(contentPrefWidth()) + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        return in.getTop() + snapSizeY(contentPrefHeight()) + in.getBottom();
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
