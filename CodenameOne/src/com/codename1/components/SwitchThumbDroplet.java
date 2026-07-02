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
package com.codename1.components;

/// Pure, testable model of the iOS-26 "Liquid Glass" switch-thumb droplet.
///
/// While the thumb slides between the off and on positions it behaves like a
/// liquid droplet: it stretches along the travel axis and squashes vertically,
/// peaking mid-slide and settling back to a circle at either end. Given the
/// slide progress (0 at either resting end .. 1 at the opposite end) and the
/// resting thumb size, {@link #compute} returns the frame's draw size and the
/// centre-preserving offsets. The envelope is `sin(progress * PI)` so the
/// deformation is zero at both ends and maximal exactly mid-travel.
///
/// No {@link com.codename1.ui.Graphics}/theme/tree dependency, so the motion
/// can be unit-tested at fixed progress values and validated by the fidelity
/// animation-frame probes, mirroring `TabSelectionMorph` for the tab
/// lens. `Switch.paint` is the single production caller.
final class SwitchThumbDroplet {

    /// Theme-resolved tuning tokens (percentages already divided to fractions).
    static final class Tokens {
        float stretch;   // switchLiquidStretchPct/100 -- elongation along the travel axis
        float squash;    // switchLiquidSquashPct/100  -- vertical squash per unit of stretch
    }

    // ---- outputs ----
    int drawW;      // stretched thumb draw width
    int drawH;      // squashed thumb draw height
    int offsetX;    // add to the resting thumb x so the stretch stays centred
    int offsetY;    // add to the resting thumb y so the squash stays centred

    private SwitchThumbDroplet() {
    }

    /// Computes one droplet frame.
    ///
    /// @param progress slide progress 0..1 (0 and 1 are the resting ends)
    /// @param thumbW   resting thumb width in px
    /// @param thumbH   resting thumb height in px
    /// @param tk       resolved theme tokens
    static SwitchThumbDroplet compute(float progress, int thumbW, int thumbH, Tokens tk) {
        SwitchThumbDroplet d = new SwitchThumbDroplet();
        float p = progress < 0 ? 0 : (progress > 1 ? 1 : progress);
        float env = (float) Math.sin(p * Math.PI);   // 0 at the ends, 1 mid-slide
        float st = env * tk.stretch;
        d.drawW = Math.round(thumbW * (1f + st));
        d.drawH = Math.round(thumbH * (1f - tk.squash * st));   // partial volume preservation
        d.offsetX = (thumbW - d.drawW) / 2;
        d.offsetY = (thumbH - d.drawH) / 2;
        return d;
    }
}
