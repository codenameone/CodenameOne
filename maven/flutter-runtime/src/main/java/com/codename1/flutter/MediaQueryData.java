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

import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Display;

/**
 * A snapshot of the display metrics {@link MediaQuery#of} returns, expressed
 * the way Flutter expresses them:
 * <ul>
 *   <li>{@link #size()} — the display size in LOGICAL pixels (CN1 device
 *       pixels divided by {@link Dp#scale()})</li>
 *   <li>{@link #devicePixelRatio()} — device pixels per logical pixel
 *       ({@link Dp#scale()}, bucketed like Android/Flutter density
 *       buckets)</li>
 *   <li>{@link #platformBrightness()} — the platform dark-mode setting
 *       (light when unknown)</li>
 * </ul>
 *
 * <p>Headless (no Display) the sensible defaults are a 0x0 size, ratio 1.0
 * and light brightness.</p>
 */
public class MediaQueryData {

    private final Size size;
    private final double devicePixelRatio;
    private final Brightness platformBrightness;
    private final double textScaleFactor;
    private final EdgeInsets padding;

    public MediaQueryData(Size size, double devicePixelRatio, Brightness platformBrightness) {
        this(size, devicePixelRatio, platformBrightness, 1.0, EdgeInsets.all(0));
    }

    public MediaQueryData(Size size, double devicePixelRatio, Brightness platformBrightness,
                          double textScaleFactor, EdgeInsets padding) {
        this.size = size;
        this.devicePixelRatio = devicePixelRatio;
        this.platformBrightness = platformBrightness == null ? Brightness.light : platformBrightness;
        this.textScaleFactor = textScaleFactor;
        this.padding = padding == null ? EdgeInsets.all(0) : padding;
    }

    public Size size() {
        return size;
    }

    public double devicePixelRatio() {
        return devicePixelRatio;
    }

    public Brightness platformBrightness() {
        return platformBrightness;
    }

    /**
     * The number of font pixels per logical pixel (legacy Flutter accessor;
     * defaults to 1.0 — this pass does not read the platform text-scale).
     */
    public double textScaleFactor() {
        return textScaleFactor;
    }

    /**
     * The parts of the display partially obscured by system UI (defaults to
     * {@link EdgeInsets#all(double) EdgeInsets.all(0)}).
     */
    public EdgeInsets padding() {
        return padding;
    }

    /**
     * The parts of the display obscured by system UI that the app can still
     * draw under (e.g. the on-screen keyboard). This pass does not track the
     * keyboard, so it reports no insets.
     */
    public EdgeInsets viewInsets() {
        return EdgeInsets.all(0);
    }

    /**
     * The parts of the display obscured by system UI regardless of whether the
     * app can draw under them (e.g. a hardware notch, the home indicator). With no
     * keyboard inset -- this runtime tracks none, so viewInsets is zero -- Flutter's
     * viewPadding equals padding, which holds the safe-area insets fromDisplay read.
     * It reported zero, so a bar sized from viewPadding ran under the notch.
     */
    public EdgeInsets viewPadding() {
        return padding;
    }

    /**
     * Returns a copy with the supplied (non-null) values overridden. Parameter
     * order matches the Dart stub.
     */
    public MediaQueryData copyWith(Size size, Double devicePixelRatio, Double textScaleFactor,
                                   EdgeInsets padding, Brightness platformBrightness) {
        return new MediaQueryData(
                size != null ? size : this.size,
                devicePixelRatio != null ? devicePixelRatio : this.devicePixelRatio,
                platformBrightness != null ? platformBrightness : this.platformBrightness,
                textScaleFactor != null ? textScaleFactor : this.textScaleFactor,
                padding != null ? padding : this.padding);
    }

    /**
     * Returns a copy with the selected padding edges zeroed — Flutter's
     * {@code MediaQueryData.removePadding}.
     *
     * <p>This is how a safe area is spent exactly once. A {@code SafeArea} or a
     * {@code Scaffold} that insets its child for the notch hands the child a
     * media query with that edge already consumed; without it, every nested
     * safe area insets for the same notch again. Returning {@code this} made
     * the whole idiom inert.</p>
     */
    public MediaQueryData removePadding(Boolean removeLeft, Boolean removeTop,
            Boolean removeRight, Boolean removeBottom) {
        boolean left = Boolean.TRUE.equals(removeLeft);
        boolean top = Boolean.TRUE.equals(removeTop);
        boolean right = Boolean.TRUE.equals(removeRight);
        boolean bottom = Boolean.TRUE.equals(removeBottom);
        if (!left && !top && !right && !bottom) {
            return this;
        }
        EdgeInsets p = padding();
        return copyWith(null, null, null, EdgeInsets.fromLTRB(
                left ? 0 : p.left(),
                top ? 0 : p.top(),
                right ? 0 : p.right(),
                bottom ? 0 : p.bottom()), null);
    }

    /**
     * Builds the snapshot from the current CN1 Display, or the headless
     * defaults when no Display is initialized.
     */
    public static MediaQueryData fromDisplay() {
        return fromDisplay(null);
    }

    /**
     * As {@link #fromDisplay()}, for a subtree that knows which Form it is in.
     *
     * <p>The safe area has to be asked of a Form, and the first screen is BUILT BEFORE
     * IT IS SHOWN, so {@code Display.getCurrent()} is null throughout that build and
     * every inset came back zero. Passing the Form the subtree actually belongs to
     * removes the dependency on what happens to be on screen.</p>
     */
    public static MediaQueryData fromDisplay(com.codename1.ui.Form form) {
        if (!Display.isInitialized()) {
            return new MediaQueryData(new Size(0, 0), 1.0, Brightness.light);
        }
        Display d = Display.getInstance();
        Boolean dark = null;
        try {
            dark = d.isDarkMode();
        } catch (Throwable ignore) {
            // ports without dark-mode detection
        }
        noteFirstSize(d.getDisplayWidth(), d.getDisplayHeight());
        return compute(d.getDisplayWidth(), d.getDisplayHeight(), Dp.scale(), dark,
                safeAreaInsets(d, form));
    }

    /// The display size the FIRST ambient lookup saw, against the one in force
    /// now. An adaptive application asks the media query which layout it is, so
    /// if these differ the app was built for a screen it is not on.
    private static String firstSize;

    private static void noteFirstSize(int w, int h) {
        if (firstSize == null) {
            firstSize = w + "x" + h;
        }
    }

    /** {@code first@now} display sizes; see {@link #firstSize}. */
    public static String sizeHistory() {
        String now = "?";
        try {
            if (Display.isInitialized()) {
                Display d = Display.getInstance();
                now = d.getDisplayWidth() + "x" + d.getDisplayHeight();
            }
        } catch (Throwable ignore) {
            now = "?";
        }
        return (firstSize == null ? "-" : firstSize) + "->" + now + "@" + Dp.scale();
    }

    /**
     * The device's safe-area insets, in LOGICAL pixels — what {@code MediaQuery.padding}
     * means in Flutter.
     *
     * <p>This used to be left at zero, and that is not a cosmetic omission: content ran
     * under the notch, the status bar and the home indicator, and controls near the bottom
     * edge could not be reached. Anything reading {@code MediaQuery.of(context).padding} got
     * nothing to work with — the gallery sizes its settings button as
     * {@code height + padding.top}, so that button came out short by the notch.</p>
     *
     * <p>Codename One already knows the answer ({@code Form.getSafeArea()}, backed by the
     * port's {@code getDisplaySafeArea}); the runtime simply never asked.</p>
     */
    private static EdgeInsets safeAreaInsets(Display d, com.codename1.ui.Form form) {
        try {
            // The caller's own Form first: during runApp's build nothing is current yet.
            com.codename1.ui.Form f = form != null ? form : d.getCurrent();
            if (f == null) {
                return EdgeInsets.all(0);
            }
            com.codename1.ui.geom.Rectangle safe = f.getSafeArea();
            if (safe == null || safe.getWidth() <= 0 || safe.getHeight() <= 0) {
                return EdgeInsets.all(0);
            }
            double scale = Dp.scale();
            if (scale <= 0) {
                scale = 1;
            }
            int w = d.getDisplayWidth();
            int h = d.getDisplayHeight();
            // The safe rectangle is in pixels; the insets are the margins around it.
            double left = Math.max(0, safe.getX());
            double top = Math.max(0, safe.getY());
            double right = Math.max(0, w - (safe.getX() + safe.getWidth()));
            double bottom = Math.max(0, h - (safe.getY() + safe.getHeight()));
            return EdgeInsets.fromLTRB(left / scale, top / scale, right / scale, bottom / scale);
        } catch (Throwable t) {
            // a port without safe-area support behaves as it did before
            return EdgeInsets.all(0);
        }
    }

    /**
     * The pure metric math (headless-testable): logical size is the pixel
     * size divided by the scale; a non-positive scale falls back to 1; a null
     * or FALSE dark flag maps to light.
     */
    public static MediaQueryData compute(int widthPx, int heightPx, double scale, Boolean darkMode) {
        return compute(widthPx, heightPx, scale, darkMode, EdgeInsets.all(0));
    }

    /** As above, with the device's safe-area insets (already in logical pixels). */
    public static MediaQueryData compute(int widthPx, int heightPx, double scale,
            Boolean darkMode, EdgeInsets padding) {
        if (scale <= 0) {
            scale = 1;
        }
        return new MediaQueryData(
                new Size(widthPx / scale, heightPx / scale),
                scale,
                Boolean.TRUE.equals(darkMode) ? Brightness.dark : Brightness.light,
                1.0,
                padding);
    }
}
