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

/// Pure, testable model of the iOS-26 "Liquid Glass" tab selection morph.
///
/// Given the animation time `t` (0..1), the source and target cell bounds, the
/// resolved bar geometry and the theme tokens, {@link #compute} returns a plain data
/// object describing the frame: the grey selection-pill rect, the glass lens (drop) rect
/// plus its lens parameters (magnify / aberration / tint), the travel envelope, and the
/// optional whole-bar grow rect.
///
/// It has no dependency on {@link Graphics}, the component tree or the theme, so it can
/// be unit-tested at fixed progress values and reused by the fidelity animation-frame
/// probes. `Tabs.paintSelectionCapsule` is the single production caller; it resolves
/// the tokens + geometry from the theme/component and then paints from the returned model.
final class TabSelectionMorph {

    // At 60fps the 350ms native transition advances about 5% per frame. JavaSE can
    // occasionally coalesce repaints and hand the next animation tick a completed
    // wall-clock Motion; cap the visible jump so at least the characteristic liquid
    // frames are painted instead of snapping directly from 0 to 100.
    private static final int MAX_VISIBLE_FRAME_STEP = 14;

    static int paceVisibleFrame(int previous, int wallClockValue) {
        int prior = previous < 0 ? 0 : (previous > 100 ? 100 : previous);
        int raw = wallClockValue < prior ? prior : (wallClockValue > 100 ? 100 : wallClockValue);
        int capped = prior + MAX_VISIBLE_FRAME_STEP;
        return raw > capped ? capped : raw;
    }

    /// The morph's tuning tokens. Themes do not set these individually: a NAMED
    /// PRESET ({@link #preset}) supplies the full envelope set, and the theme
    /// exposes only high-level controls -- the preset name (tabsMorphPreset),
    /// the lens intensity (tabsMorphLensIntensityPct, {@link #scaleLensIntensity})
    /// and the springiness (tabsMorphSpringPct, {@link #spring}) -- so the
    /// motion stays coherent instead of being tuned one constant at a time.
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
        float spring = 1f;    // settle-deformation scale (0 = no stop squash/swell)

        /// The named motion presets. "ios26" (the default, and any unknown name)
        /// is bounded from the recorded iOS 26 UITabBar transition: the travelling
        /// drop stays close to one cell, grows only a few percent vertically, and
        /// uses barely-visible chromatic separation. "subtle" reduces those cues
        /// further for applications that want an almost-flat selection change.
        static Tokens preset(String name) {
            Tokens tk = new Tokens();
            if ("subtle".equals(name)) {
                tk.stretch = 0f;
                tk.squashW = 0.03f;
                tk.grow = 0.005f;
                tk.squashH = 0.025f;
                tk.liftMm = 0.03f;
                tk.bubbleWidthPct = 100;
                tk.overflowPct = 0;
                tk.downBiasMm = 0f;
                tk.restMag = 1.01f;
                tk.peakMag = 1.05f;
                tk.peakAb = 0.00025f;
                tk.tintStrength = 1f;
                tk.barGrowPct = 0;
                tk.spring = 0.20f;
                return tk;
            }
            // "ios26" -- the shipped iOS Modern tuning.
            // The recorded native drop keeps essentially one cell's width while
            // it travels.  Its liquid character comes from refraction across the
            // moving edge, not from stretching across the neighbouring glyphs.
            // A wider lens duplicated two tabs at once and looked like distortion
            // instead of the compact UIKit bubble.
            tk.stretch = 0f;
            tk.squashW = 0.05f;
            tk.grow = 0.01f;
            tk.squashH = 0.04f;
            tk.liftMm = 0.05f;
            tk.bubbleWidthPct = 100;
            tk.overflowPct = 0;
            tk.downBiasMm = 0f;
            tk.restMag = 1.02f;
            tk.peakMag = 1.08f;
            tk.peakAb = 0.0005f;
            tk.tintStrength = 1f;
            tk.barGrowPct = 0;
            // Native keeps a restrained stop deformation without positional
            // overshoot; scale the width/height settle envelope to 35%.
            tk.spring = 0.35f;
            return tk;
        }

        /// Scales the lens OPTICS (magnification delta, aberration, tint)
        /// around the preset values: 1 = as authored, 0 = an optically flat
        /// drop, 2 = twice the optical strength. Geometry is unaffected.
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
    // grey selection pill (bar height)
    int capX;
    int capY;
    int capW;
    int capH;
    // glass drop lens rect + optics
    int lensX;
    int lensY;
    int lensW;
    int lensH;
    float magnify;
    float aberration;
    float tintStrength;
    float flight;         // 0 settled .. ~1 travelling (drives the pill alpha fade)
    boolean barGrow;      // whether the whole-bar grow pass is active this frame
    int barGrowX;
    int barGrowY;
    int barGrowW;
    int barGrowH;
    float barGrowMag;

    private TabSelectionMorph() {
    }

    /// smoothstep(a,b,x): 0 below a, 1 above b, smooth between; a may be > b.
    static float smooth(float a, float b, float x) {
        float t = (x - a) / (b - a);
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return t * t * (3 - 2 * t);
    }

    // Measured centres of the travelling native selection drop, normalized from
    // the first visible frame through its 350ms settle.  UIKit's curve accelerates
    // more quickly than smoothstep, then spends the last third easing into place;
    // approximating it with a short 200ms ease made the CN1 drop look like a jump.
    private static final float[] NATIVE_POSITION_TIME = {
        0f, 0.049f, 0.100f, 0.151f, 0.200f, 0.251f, 0.294f,
        0.337f, 0.391f, 0.443f, 0.486f, 0.537f, 0.577f,
        0.629f, 0.677f, 0.723f, 0.766f, 0.820f, 0.863f, 0.914f, 1f
    };
    private static final float[] NATIVE_POSITION_VALUE = {
        0f, 0.032f, 0.099f, 0.219f, 0.288f, 0.394f, 0.492f,
        0.582f, 0.678f, 0.728f, 0.770f, 0.804f, 0.876f,
        0.907f, 0.932f, 0.951f, 0.966f, 0.981f, 0.996f, 1f, 1f
    };

    /// Position easing sampled from the recorded iOS 26 UITabBar transition.
    /// The path is monotonic: the native drop deforms at the stop but does not
    /// shoot past the end of the bar and snap back.
    static float springEase(float t) {
        if (t <= 0f) {
            return 0f;
        }
        if (t >= 1f) {
            return 1f;
        }
        for (int i = 1; i < NATIVE_POSITION_TIME.length; i++) {
            if (t <= NATIVE_POSITION_TIME[i]) {
                float ta = NATIVE_POSITION_TIME[i - 1];
                float tb = NATIVE_POSITION_TIME[i];
                float u = (t - ta) / (tb - ta);
                float va = NATIVE_POSITION_VALUE[i - 1];
                float vb = NATIVE_POSITION_VALUE[i];
                return va + (vb - va) * u;
            }
        }
        return 1f;
    }

    /// Computes one morph frame.
    ///
    /// @param t         linear animation time 0..1 (1 == settled; pass 1 with from==to for the resting frame)
    /// @param fromX     source cell x (in the bar's inner-x space)
    /// @param fromW     source cell width
    /// @param toX       target cell x
    /// @param toW       target cell width
    /// @param innerX    the tab bar's inner-left x (added to cell x to get the paint x)
    /// @param capYBase  the settled pill top (bar-height span top)
    /// @param capHBase  the settled pill height
    /// @param barLeftX  whole-bar left edge (paint space) for the grow pass
    /// @param barRightX whole-bar right edge (paint space) for the grow pass
    /// @param tk        resolved theme tokens
    static TabSelectionMorph compute(float t, int fromX, int fromW, int toX, int toW,
            int innerX, int capYBase, int capHBase, int barLeftX, int barRightX, Tokens tk) {
        TabSelectionMorph m = new TabSelectionMorph();
        float tp = t < 0 ? 0 : (t > 1 ? 1 : t);

        float pos = springEase(tp);
        int x = fromX + (int) ((toX - fromX) * pos);
        int w = fromW + (int) ((toW - fromW) * pos);

        // travel envelopes (see the original Tabs.paintSelectionCapsule commentary)
        m.flight = smooth(0f, 0.12f, tp) * (1f - smooth(0.64f, 0.86f, tp));
        float moving = smooth(0f, 0.08f, tp) * (1f - smooth(0.88f, 1f, tp));
        float sd = (tp - 0.80f) / 0.14f;
        float squash = (sd > -1f && sd < 1f) ? (1f - sd * sd) * (1f - sd * sd) : 0f; // settle bump
        // Springiness controls the stop DEFORMATION, not positional overshoot.
        // The native centre path above is monotonic; the liquid cue is its brief
        // width compression/height swell as it comes to rest.
        squash *= tk.spring;
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

        // The drop may be wider than its cell (bubbleWidthPct > 100), but it
        // must never leave the bar:
        // the native pill overlaps NEIGHBOUR cells, not the backdrop.
        if (capX < barLeftX) {
            w -= barLeftX - capX;
            capX = barLeftX;
        }
        if (capX + w > barRightX) {
            w = barRightX - capX;
        }

        m.capX = capX;
        m.capY = capYBase;
        m.capW = w;
        m.capH = capHBase;

        m.magnify = tk.restMag + (tk.peakMag - tk.restMag) * m.flight;
        m.aberration = tk.peakAb * m.flight;
        m.tintStrength = tk.tintStrength;

        // The vertical bulge past the bar is a FLIGHT effect: the native drop
        // swells while travelling but its settled pill sits fully inside the
        // bar -- a constant overflow left a tinted crescent past the bar's
        // rounded ends at rest.
        int baseLensH = capHBase + (int) (capHBase * (tk.overflowPct / 100f) * m.flight);
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
