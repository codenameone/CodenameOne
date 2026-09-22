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
package com.codename1.flutter.material;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.geom.Dimension;

import dart.runtime.Funcs;

/**
 * Leaf render box for {@link Slider}: an editable CN1
 * {@link com.codename1.ui.Slider} (UIID "FlutterSlider"). The double range
 * [min, max] is scaled onto CN1's int progress 0..steps where steps =
 * divisions (when given) or {@link #DEFAULT_STEPS} for a continuous feel.
 * Controlled: drags fire onChanged with the scaled double, then the progress
 * snaps back to the configured value. Fills the available width, 44lp
 * minimum height (Material tap-friendly track area).
 */
public class SliderRenderElement extends RenderElement {

    /** Steps used for a "continuous" slider (no divisions). */
    public static final long DEFAULT_STEPS = 1000;
    /** Material slider interaction height in logical pixels. */
    public static final double MIN_HEIGHT_LP = 44;
    /** Intrinsic width when the incoming width is unbounded. */
    public static final double DEFAULT_WIDTH_LP = 160;

    private boolean applying;

    public SliderRenderElement(Slider widget) {
        super(widget);
    }

    private Slider slider() {
        return (Slider) widget();
    }

    /**
     * The number of int steps the double range is scaled onto.
     */
    public long steps() {
        Long d = slider().getDivisions();
        return (d != null && d > 0) ? d : DEFAULT_STEPS;
    }

    // ------------------------------------------------------------------
    // double <-> int scaling (pure, headless-testable)
    // ------------------------------------------------------------------

    /**
     * Maps a double value in [min, max] to an int progress in [0, steps].
     */
    public static int progressFor(double value, double min, double max, long steps) {
        if (max <= min || steps <= 0) {
            return 0;
        }
        double clamped = Math.max(min, Math.min(max, value));
        return (int) Math.round((clamped - min) / (max - min) * steps);
    }

    /**
     * Maps an int progress in [0, steps] back to the double range.
     */
    public static double valueFor(int progress, double min, double max, long steps) {
        if (steps <= 0) {
            return min;
        }
        int clamped = Math.max(0, Math.min((int) steps, progress));
        return min + (max - min) * clamped / steps;
    }

    /**
     * The int progress the current configuration maps to.
     */
    public int configuredProgress() {
        Slider w = slider();
        return progressFor(w.getValue(), w.getMin(), w.getMax(), steps());
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        com.codename1.ui.Slider s = new com.codename1.ui.Slider();
        s.setUIID("FlutterSlider");
        s.setEditable(true);
        s.addDataChangedListener(new DataChangedListener() {
            @Override
            public void dataChanged(int type, int index) {
                if (applying) {
                    return;
                }
                userDragged(index);
            }
        });
        apply(s);
        return s;
    }

    @Override
    protected void updateComponent(Component c) {
        apply((com.codename1.ui.Slider) c);
    }

    private void apply(com.codename1.ui.Slider s) {
        applying = true;
        try {
            s.setMinValue(0);
            s.setMaxValue((int) steps());
            s.setProgress(configuredProgress());
            // A Slider with no onChanged is DISABLED, and Material greys every
            // part of it. Never set, our disabled sliders kept the accent and
            // the demo's second slider was indistinguishable from its first.
            s.setEnabled(slider().getOnChanged() != null);
            applyColours(s);
        } finally {
            applying = false;
        }
    }

    /**
     * Paints the track and thumb from the theme, directly on the component.
     *
     * <p>Not through theme properties: Codename One's Slider caches its
     * {@code *Full} styles inside {@code setUIID}, which runs when the component
     * is created -- before a Flutter ThemeData has been turned into properties.
     * Set that way the colours were simply missed, and the demo's sliders came
     * up with a grey active track and a navy thumb against the reference's
     * accent purple.</p>
     */
    private void applyColours(com.codename1.ui.Slider s) {
        try {
            ColorScheme cs = Theme.of(this).colorScheme();
            if (cs == null || cs.primary() == null) {
                return;
            }
            int accent = cs.primary().rgb();
            int track = cs.surfaceVariant() != null
                    ? cs.surfaceVariant().rgb() : cs.surface().rgb();
            // Inactive track and thumb.
            s.getSliderEmptyUnselectedStyle().setBgColor(track);
            s.getSliderEmptyUnselectedStyle().setBgTransparency(255);
            s.getSliderEmptyUnselectedStyle().setFgColor(accent);
            s.getSliderEmptySelectedStyle().setBgColor(track);
            s.getSliderEmptySelectedStyle().setBgTransparency(255);
            s.getSliderEmptySelectedStyle().setFgColor(accent);
            // Active track.
            s.getSliderFullSelectedStyle().setBgColor(accent);
            s.getSliderFullSelectedStyle().setBgTransparency(255);
            s.getSliderFullUnselectedStyle().setBgColor(accent);
            s.getSliderFullUnselectedStyle().setBgTransparency(255);
        } catch (Throwable noTheme) {
            // an unthemed slider keeps the base look
        }
    }

    /**
     * Controlled drag entry point (public so headless tests can drive it):
     * fires onChanged with the progress scaled back to the double range,
     * then re-applies the configured value.
     */
    public void userDragged(int progress) {
        Slider w = slider();
        Funcs.VoidFunc1<Double> f = w.getOnChanged();
        if (f != null) {
            f.call(valueFor(progress, w.getMin(), w.getMax(), steps()));
        }
        Component c = component();
        if (c != null) {
            apply((com.codename1.ui.Slider) c);
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        Component c = component();
        double prefW = Dp.px(DEFAULT_WIDTH_LP);
        double prefH = Dp.px(MIN_HEIGHT_LP);
        if (c != null) {
            Dimension d = c.getPreferredSize();
            prefW = Math.max(prefW, d.getWidth());
            prefH = Math.max(prefH, d.getHeight());
        }
        double w = constraints.hasBoundedWidth() ? constraints.maxWidth() : prefW;
        return constraints.constrain(new Size(w, prefH));
    }
}
