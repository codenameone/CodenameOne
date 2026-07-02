/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.ui;

/**
 * Pure, testable model of the iOS-26 "Liquid Glass" tab selection morph.
 *
 * <p>Given the animation time {@code t} (0..1), the source and target cell bounds, the
 * resolved bar geometry and the theme tokens, {@link #compute} returns a plain data
 * object describing the frame: the grey selection-pill rect, the glass lens (drop) rect
 * plus its lens parameters (magnify / aberration / tint), the travel envelope, and the
 * optional whole-bar grow rect.</p>
 *
 * <p>It has no dependency on {@link Graphics}, the component tree or the theme, so it can
 * be unit-tested at fixed progress values and reused by the fidelity animation-frame
 * probes. {@code Tabs.paintSelectionCapsule} is the single production caller; it resolves
 * the tokens + geometry from the theme/component and then paints from the returned model.
 */
final class TabSelectionMorph {

    /**
     * The morph's tuning tokens. Themes do not set these individually: a NAMED
     * PRESET ({@link #preset}) supplies the full envelope set, and the theme
     * exposes only high-level controls -- the preset name (tabsMorphPreset),
     * the lens intensity (tabsMorphLensIntensityPct, {@link #scaleLensIntensity})
     * and the springiness (tabsMorphSpringPct, {@link #spring}) -- so the
     * motion stays coherent instead of being tuned one constant at a time.
     */
    static final class Tokens {
        float stretch;        // horizontal elongation while moving (fraction)
        float squashW;        // width compression at the stop (fraction)
        float grow;           // vertical grow mid-flight (fraction)
        float squashH;        // extra height at the stop (fraction)
        float liftMm;         // upward content lift in mm (the caller converts to px)
        int liftPx;           // upward content lift in px
        int bubbleWidthPct;   // drop width vs. cell (percent)
        int overflowPct;      // vertical bulge past the bar (percent)
        float downBiasMm;     // downward bias in mm (the caller converts to px)
        int downBiasPx;       // downward bias in px
        float restMag;        // settled lens magnification (1.0 = none)
        float peakMag;        // mid-flight lens magnification
        float peakAb;         // mid-flight chromatic aberration (fraction)
        float tintStrength;   // lens accent-tint strength 0..1
        int barGrowPct;       // whole-bar grow pulse percent (0 = off)
        float spring = 1f;    // settle-overshoot scale (1 = preset amount, 0 = none)

        /**
         * The named motion presets. "ios26" (the default, and any unknown name)
         * is the measured iOS 26 Liquid Glass morph; "subtle" halves the
         * deformation and optics for a calmer selection change.
         */
        static Tokens preset(String name) {
            Tokens tk = new Tokens();
            if ("subtle".equals(name)) {
                tk.stretch = 0.16f;
                tk.squashW = 0.08f;
                tk.grow = 0.07f;
                tk.squashH = 0.09f;
                tk.liftMm = 0.25f;
                tk.bubbleWidthPct = 96;
                tk.overflowPct = 12;
                tk.downBiasMm = 0.15f;
                tk.restMag = 1.04f;
                tk.peakMag = 1.09f;
                tk.peakAb = 0.01f;
                tk.tintStrength = 1f;
                tk.barGrowPct = 0;
                return tk;
            }
            // "ios26" -- the shipped iOS Modern tuning.
            tk.stretch = 0.32f;
            tk.squashW = 0.16f;
            tk.grow = 0.14f;
            tk.squashH = 0.18f;
            tk.liftMm = 0.5f;
            tk.bubbleWidthPct = 96;
            tk.overflowPct = 18;
            tk.downBiasMm = 0.3f;
            tk.restMag = 1.08f;
            tk.peakMag = 1.18f;
            tk.peakAb = 0.02f;
            tk.tintStrength = 1f;
            tk.barGrowPct = 0;
            return tk;
        }

        /**
         * Scales the lens OPTICS (magnification delta, aberration, tint)
         * around the preset values: 1 = as authored, 0 = an optically flat
         * drop, 2 = twice the optical strength. Geometry is unaffected.
         */
        void scaleLensIntensity(float intensity) {
            float i = intensity < 0 ? 0 : intensity;
            restMag = 1f + (restMag - 1f) * i;
            peakMag = 1f + (peakMag - 1f) * i;
            peakAb = peakAb * i;
            float t = tintStrength * i;
            tintStrength = t > 1f ? 1f : t;
        }
    }

    // ---- outputs (all in the same coordinate space as the input geometry) ----
    int capX, capY, capW, capH;        // grey selection pill (bar height)
    int lensX, lensY, lensW, lensH;    // glass drop lens
    float magnify, aberration, tintStrength;
    float flight;                      // 0 settled .. ~1 travelling (drives the pill alpha fade)
    boolean barGrow;                   // whether the whole-bar grow pass is active this frame
    int barGrowX, barGrowY, barGrowW, barGrowH;
    float barGrowMag;

    private TabSelectionMorph() {
    }

    /** smoothstep(a,b,x): 0 below a, 1 above b, smooth between; a may be &gt; b. */
    static float smooth(float a, float b, float x) {
        float t = (x - a) / (b - a);
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return t * t * (3 - 2 * t);
    }

    /**
     * Position easing: an even ease-in-out travel reaching the target ~t=0.78, then a
     * small damped overshoot that settles by t=1 (the "stop" bounce). The
     * springiness scales the overshoot amplitude: 1 = the preset 0.09, 0 = no
     * overshoot (a plain ease-in-out stop), 2 = double the bounce.
     */
    static float springEase(float t, float springiness) {
        if (t <= 0f) {
            return 0f;
        }
        if (t >= 1f) {
            return 1f;
        }
        float travelEnd = 0.78f;
        if (t <= travelEnd) {
            float u = t / travelEnd;
            return u * u * (3 - 2 * u);
        }
        float u = (t - travelEnd) / (1f - travelEnd);
        return 1f + 0.09f * springiness * (float) (Math.sin(u * Math.PI) * (1f - u));
    }

    /**
     * Computes one morph frame.
     *
     * @param t         linear animation time 0..1 (1 == settled; pass 1 with from==to for the resting frame)
     * @param fromX     source cell x (in the bar's inner-x space)
     * @param fromW     source cell width
     * @param toX       target cell x
     * @param toW       target cell width
     * @param innerX    the tab bar's inner-left x (added to cell x to get the paint x)
     * @param capYBase  the settled pill top (bar-height span top)
     * @param capHBase  the settled pill height
     * @param barLeftX  whole-bar left edge (paint space) for the grow pass
     * @param barRightX whole-bar right edge (paint space) for the grow pass
     * @param tk        resolved theme tokens
     */
    static TabSelectionMorph compute(float t, int fromX, int fromW, int toX, int toW,
            int innerX, int capYBase, int capHBase, int barLeftX, int barRightX, Tokens tk) {
        TabSelectionMorph m = new TabSelectionMorph();
        float tp = t < 0 ? 0 : (t > 1 ? 1 : t);

        float pos = springEase(tp, tk.spring);
        int x = fromX + (int) ((toX - fromX) * pos);
        int w = fromW + (int) ((toW - fromW) * pos);

        // travel envelopes (see the original Tabs.paintSelectionCapsule commentary)
        m.flight = smooth(0f, 0.12f, tp) * (1f - smooth(0.64f, 0.86f, tp));
        float moving = smooth(0f, 0.08f, tp) * (1f - smooth(0.88f, 1f, tp));
        float sd = (tp - 0.80f) / 0.14f;
        float squash = (sd > -1f && sd < 1f) ? (1f - sd * sd) * (1f - sd * sd) : 0f; // settle bump
        float grow = smooth(0f, 0.10f, tp) * (1f - smooth(0.20f, 0.42f, tp));        // early whole-bar swell

        // horizontal elongation while moving, then width compression at the stop
        w = (int) (w * (1f + moving * tk.stretch));
        w = (int) (w * (1f - squash * tk.squashW));
        float vScale = 1f + m.flight * tk.grow + squash * tk.squashH;
        int liftPx = (int) (m.flight * tk.liftPx);

        int capX = innerX + x;
        // compact the drop around the cell centre
        int bubbleW = w * tk.bubbleWidthPct / 100;
        capX += (w - bubbleW) / 2;
        w = bubbleW;

        m.capX = capX;
        m.capY = capYBase;
        m.capW = w;
        m.capH = capHBase;

        m.magnify = tk.restMag + (tk.peakMag - tk.restMag) * m.flight;
        m.aberration = tk.peakAb * m.flight;
        m.tintStrength = tk.tintStrength;

        int baseLensH = capHBase + capHBase * tk.overflowPct / 100;
        int lensH = (int) (baseLensH * vScale);
        int lensY = capYBase + capHBase / 2 - lensH / 2 - liftPx + tk.downBiasPx;
        m.lensX = capX;
        m.lensY = lensY;
        m.lensW = w;
        m.lensH = lensH;

        // brief whole-bar grow pass (uniform magnify) at the very start
        if (grow > 0.01f && tk.barGrowPct > 0) {
            int barW0 = barRightX - barLeftX;
            m.barGrowMag = 1f + grow * (tk.barGrowPct / 100f);
            int mgx = (int) (barW0 * 0.18f);
            int mgy = (int) (capHBase * 0.18f);
            m.barGrowX = barLeftX - mgx;
            m.barGrowY = capYBase - mgy;
            m.barGrowW = barW0 + 2 * mgx;
            m.barGrowH = capHBase + 2 * mgy;
            m.barGrow = true;
        }
        return m;
    }
}
