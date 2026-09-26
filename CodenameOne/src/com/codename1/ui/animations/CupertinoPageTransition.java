/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

package com.codename1.ui.animations;

import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

/// The iOS page push: two pages moving at different speeds, over different distances.
///
/// A plain slide holds the two pages a fixed screen apart and moves the pair, which makes
/// them one rigid object. This transition is the reason iOS depth reads the way it does:
/// the arriving page crosses the WHOLE screen while the page it covers drifts only a
/// THIRD of it, and each rides its own curve. The gap between them closes as they travel,
/// which is what says one is in front of the other.
///
/// Sliding both the full distance is not a subtle difference. A third of the way through
/// a push the strip of the old page still showing is not dimmer or shifted -- it is a
/// different PART of that page: measured against the reference at 150ms of a 500ms push
/// on a 1125px screen, the old page belongs 270px to the left and ours had it 855px to
/// the left, so the visible strip showed its far edge where the reference shows its
/// middle.
///
/// @author Shai Almog
public final class CupertinoPageTransition extends Transition {

    /// The arriving page's curve: fast ease in to slow ease out, a three-point cubic.
    private static final float[] ARRIVING = {
        0.056f, 0.024f, 0.108f, 0.3085f,
        0.198f, 0.541f,
        0.3655f, 1.0f, 0.5465f, 0.989f,
    };

    /// The departing page's curve: linear to ease out. A DIFFERENT curve, which is half
    /// of why the two do not move as one piece.
    private static final float[] DEPARTING = {0.35f, 0.91f, 0.33f, 0.97f};

    /// How far the covered page travels, as a fraction of the screen. The other half of
    /// why they do not move as one piece.
    private static final int PARALLAX_DENOMINATOR = 3;

    private static final int SCALE = 1000;

    private final int duration;
    private boolean back;

    private Motion arriving;
    private Motion departing;
    private Image sourceBuffer;
    private Image destBuffer;

    private CupertinoPageTransition(int duration) {
        this.duration = duration;
    }

    /// Creates the transition.
    ///
    /// #### Parameters
    ///
    /// - `duration`: the push duration in milliseconds
    ///
    /// #### Returns
    ///
    /// the transition
    public static CupertinoPageTransition create(int duration) {
        return new CupertinoPageTransition(duration);
    }

    /// The push duration in milliseconds.
    ///
    /// #### Returns
    ///
    /// the duration this transition was created with
    public int getDuration() {
        return duration;
    }

    /// Whether this instance plays the way BACK, with the roles of the two pages swapped.
    ///
    /// #### Returns
    ///
    /// true if this is the pop half of the transition
    public boolean isBack() {
        return back;
    }

    @Override
    public void initTransition() {
        Component source = getSource();
        Component destination = getDestination();
        if (source == null || destination == null) {
            return;
        }
        int w = destination.getWidth();
        int h = destination.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        arriving = Motion.createThreePointCubicMotion(0, SCALE, duration,
                ARRIVING[0], ARRIVING[1], ARRIVING[2], ARRIVING[3], ARRIVING[4],
                ARRIVING[5], ARRIVING[6], ARRIVING[7], ARRIVING[8], ARRIVING[9]);
        departing = Motion.createCubicBezierMotion(0, SCALE, duration,
                DEPARTING[0], DEPARTING[1], DEPARTING[2], DEPARTING[3]);
        arriving.start();
        departing.start();

        sourceBuffer = Image.createImage(source.getWidth(), source.getHeight());
        source.paintComponent(sourceBuffer.getGraphics(), true);
        destBuffer = Image.createImage(w, h);
        destination.paintComponent(destBuffer.getGraphics(), true);
    }

    @Override
    public boolean animate() {
        // Both, though they are set and cleared together: a reader (and a static analyser)
        // should not have to know that to see this is safe.
        if (arriving == null || departing == null) {
            return false;
        }
        departing.getValue();
        return !arriving.isFinished();
    }

    @Override
    public void paint(Graphics g) {
        Component destination = getDestination();
        if (arriving == null || departing == null || destination == null) {
            return;
        }
        int w = destination.getWidth();
        float front = arriving.getValue() / (float) SCALE;
        float behind = departing.getValue() / (float) SCALE;
        int parallax = w / PARALLAX_DENOMINATOR;

        int sourceX;
        int destX;
        if (back) {
            // Going back: the page on top leaves across the whole screen, and the one
            // underneath comes home from the third of the way out it was left at.
            sourceX = Math.round(w * front);
            destX = -Math.round(parallax * (1 - behind));
        } else {
            sourceX = -Math.round(parallax * behind);
            destX = Math.round(w * (1 - front));
        }

        // The covered page first: the arriving one is opaque and passes over it.
        if (sourceBuffer != null) {
            g.drawImage(sourceBuffer, sourceX, 0);
        }
        if (destBuffer != null) {
            g.drawImage(destBuffer, destX, 0);
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
        sourceBuffer = null;
        destBuffer = null;
        arriving = null;
        departing = null;
    }

    @Override
    public Transition copy(boolean reverse) {
        CupertinoPageTransition t = new CupertinoPageTransition(duration);
        t.back = reverse;
        return t;
    }
}
