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
package com.codename1.desktopcompat.javax.swing;

/// The minimum, preferred and maximum extent of a component along one axis
/// together with its alignment, and the arithmetic layouts use to place a
/// row of such components end to end or over one another.
public class SizeRequirements {

    public int minimum;

    public int preferred;

    public int maximum;

    public float alignment;

    public SizeRequirements() {
        minimum = 0;
        preferred = 0;
        maximum = 0;
        alignment = 0.5f;
    }

    public SizeRequirements(int min, int pref, int max, float a) {
        minimum = min;
        preferred = pref;
        maximum = max;
        alignment = a > 1.0f ? 1.0f : a < 0.0f ? 0.0f : a;
    }

    @Override
    public String toString() {
        return "[" + minimum + "," + preferred + "," + maximum + "]@" + alignment;
    }

    private static int clamp(long v) {
        return v > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) v;
    }

    public static SizeRequirements getTiledSizeRequirements(SizeRequirements[] children) {
        SizeRequirements total = new SizeRequirements();
        long min = 0;
        long pref = 0;
        long max = 0;
        for (int i = 0; i < children.length; i++) {
            SizeRequirements req = children[i];
            min += req.minimum;
            pref += req.preferred;
            max += req.maximum;
        }
        total.minimum = clamp(min);
        total.preferred = clamp(pref);
        total.maximum = clamp(max);
        return total;
    }

    public static SizeRequirements getAlignedSizeRequirements(SizeRequirements[] children) {
        int minAbove = 0;
        int minBelow = 0;
        int prefAbove = 0;
        int prefBelow = 0;
        int maxAbove = 0;
        int maxBelow = 0;
        for (int i = 0; i < children.length; i++) {
            SizeRequirements req = children[i];
            int above = (int) (req.alignment * req.minimum);
            int below = req.minimum - above;
            minAbove = Math.max(above, minAbove);
            minBelow = Math.max(below, minBelow);
            above = (int) (req.alignment * req.preferred);
            below = req.preferred - above;
            prefAbove = Math.max(above, prefAbove);
            prefBelow = Math.max(below, prefBelow);
            above = (int) (req.alignment * req.maximum);
            below = req.maximum - above;
            maxAbove = Math.max(above, maxAbove);
            maxBelow = Math.max(below, maxBelow);
        }
        int min = clamp((long) minAbove + (long) minBelow);
        int pref = clamp((long) prefAbove + (long) prefBelow);
        int max = clamp((long) maxAbove + (long) maxBelow);
        float alignment = 0.0f;
        if (min > 0) {
            alignment = (float) minAbove / min;
            alignment = alignment > 1.0f ? 1.0f : alignment < 0.0f ? 0.0f : alignment;
        }
        return new SizeRequirements(min, pref, max, alignment);
    }

    public static void calculateTiledPositions(int allocated, SizeRequirements total, SizeRequirements[] children,
            int[] offsets, int[] spans) {
        calculateTiledPositions(allocated, total, children, offsets, spans, true);
    }

    public static void calculateTiledPositions(int allocated, SizeRequirements total, SizeRequirements[] children,
            int[] offsets, int[] spans, boolean forward) {
        long min = 0;
        long pref = 0;
        long max = 0;
        for (int i = 0; i < children.length; i++) {
            min += children[i].minimum;
            pref += children[i].preferred;
            max += children[i].maximum;
        }
        boolean grow = allocated >= pref;
        // The fraction of each child's slack (towards its maximum when
        // growing, towards its minimum when shrinking) that is taken up.
        float play;
        float range;
        if (grow) {
            play = (float) Math.min(allocated - pref, max - pref);
            range = (float) (max - pref);
        } else {
            play = (float) Math.min(pref - allocated, pref - min);
            range = (float) (pref - min);
        }
        float factor = (grow ? max - pref : pref - min) == 0 ? 0.0f : play / range;
        int at = forward ? 0 : allocated;
        for (int i = 0; i < children.length; i++) {
            SizeRequirements req = children[i];
            if (grow) {
                int extra = (int) (factor * (req.maximum - req.preferred));
                spans[i] = clamp((long) req.preferred + (long) extra);
            } else {
                float less = factor * (req.preferred - req.minimum);
                spans[i] = (int) (req.preferred - less);
            }
            if (forward) {
                offsets[i] = at;
                at = clamp((long) at + (long) spans[i]);
            } else {
                offsets[i] = at - spans[i];
                at = (int) Math.max((long) at - (long) spans[i], 0L);
            }
        }
    }

    public static void calculateAlignedPositions(int allocated, SizeRequirements total, SizeRequirements[] children,
            int[] offsets, int[] spans) {
        calculateAlignedPositions(allocated, total, children, offsets, spans, true);
    }

    public static void calculateAlignedPositions(int allocated, SizeRequirements total, SizeRequirements[] children,
            int[] offsets, int[] spans, boolean normal) {
        float align = normal ? total.alignment : 1.0f - total.alignment;
        int above = (int) (allocated * align);
        int below = allocated - above;
        for (int i = 0; i < children.length; i++) {
            SizeRequirements req = children[i];
            float a = normal ? req.alignment : 1.0f - req.alignment;
            int maxAbove = (int) (req.maximum * a);
            int maxBelow = req.maximum - maxAbove;
            int up = Math.min(above, maxAbove);
            int down = Math.min(below, maxBelow);
            offsets[i] = above - up;
            spans[i] = clamp((long) up + (long) down);
        }
    }

    public static int[] adjustSizes(int delta, SizeRequirements[] children) {
        return new int[0];
    }
}
