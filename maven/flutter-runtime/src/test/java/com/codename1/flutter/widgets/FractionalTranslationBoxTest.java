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
package com.codename1.flutter.widgets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A fractional translation's pane has to cover where the subtree ends up.
 *
 * <p>Codename One clips every component to its own rectangle before calling its
 * paint, so a paint-only shift out of that rectangle is discarded. With the pane
 * left at the laid-out box, the gallery's feature-discovery circle -- positioned
 * at its centre and pulled back by half its own size -- rendered as the
 * bottom-right quadrant of a circle, with two straight edges meeting exactly at
 * the centre point.</p>
 */
class FractionalTranslationBoxTest {

    /** The real case: a 1618px circle at (657, 216) pulled back by half. */
    @Test
    void aNegativeShiftMovesTheOriginAndGrowsTheBox() {
        int[] box = FractionalTranslationRenderElement.translatedBox(
                657, 216, 1618, 1618, -809, -809);
        assertArrayEquals(new int[] {-152, -593, 2427, 2427, 0, 0}, box);
    }

    /**
     * A positive shift leaves the origin alone and pays for itself while painting,
     * because the box already starts where the subtree is laid out.
     */
    @Test
    void aPositiveShiftIsPaidAtPaintTime() {
        int[] box = FractionalTranslationRenderElement.translatedBox(
                100, 50, 200, 80, 60, 20);
        assertArrayEquals(new int[] {100, 50, 260, 100, 60, 20}, box);
    }

    /** Whatever the sign, origin plus paint offset lands the subtree exactly dx away. */
    @Test
    void theOriginAndThePaintOffsetAlwaysSumToTheShift() {
        for (int d = -300; d <= 300; d += 37) {
            int[] box = FractionalTranslationRenderElement.translatedBox(500, 500, 40, 40, d, d);
            assertEquals(500 + d, box[0] + box[4], "x for shift " + d);
            assertEquals(500 + d, box[1] + box[5], "y for shift " + d);
        }
    }

    @Test
    void noShiftIsTheBoxItself() {
        assertArrayEquals(new int[] {10, 20, 30, 40, 0, 0},
                FractionalTranslationRenderElement.translatedBox(10, 20, 30, 40, 0, 0));
    }
}
