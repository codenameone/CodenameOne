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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Which page a released drag settles on — Flutter's {@code PageScrollPhysics}.
 *
 * <p>The rule is not "nearest page": past the velocity tolerance the release carries you to
 * the NEXT page even from a barely-moved carousel, which is what makes a flick feel like a
 * flick rather than a nudge that springs back.</p>
 */
class PageSettleTargetTest {

    private static final double EXTENT = 900;
    private static final int MAX = 4500;      // six pages
    private static final float FLICK = 5f;    // comfortably past the tolerance
    private static final float STILL = 0f;

    @Test
    @DisplayName("a release with no velocity falls to the nearest page")
    void noVelocitySettlesToTheNearest() {
        assertEquals(0, PageViewRenderElement.settleTarget(100, EXTENT, STILL, MAX));
        assertEquals(900, PageViewRenderElement.settleTarget(800, EXTENT, STILL, MAX));
        assertEquals(900, PageViewRenderElement.settleTarget(1300, EXTENT, STILL, MAX));
        assertEquals(1800, PageViewRenderElement.settleTarget(1360, EXTENT, STILL, MAX));
    }

    @Test
    @DisplayName("a flick advances a whole page even from a barely-moved carousel")
    void aFlickAdvancesAPage() {
        // 40px in: nearest is page 0, but the flick means page 1.
        assertEquals(900, PageViewRenderElement.settleTarget(40, EXTENT, FLICK, MAX));
        // ...and a backward flick from page 1 goes back to page 0. Note the offset is
        // BELOW the boundary: a flick is always preceded by some drag, so by the time the
        // finger lifts the carousel has already moved off the page it started on. That is
        // why the half-page bias is applied to where the release actually happened and not
        // to the page it began from.
        assertEquals(0, PageViewRenderElement.settleTarget(880, EXTENT, -FLICK, MAX));
    }

    @Test
    @DisplayName("a flick never skips more than one page")
    void aFlickIsOnePageAtATime() {
        // Even a very fast flick: Flutter's page physics is one page per gesture.
        assertEquals(900, PageViewRenderElement.settleTarget(0, EXTENT, 500f, MAX));
        assertEquals(1800, PageViewRenderElement.settleTarget(900, EXTENT, 500f, MAX));
    }

    @Test
    @DisplayName("the settle stays inside the scrollable range")
    void targetIsClampedToTheEnds() {
        assertEquals(MAX, PageViewRenderElement.settleTarget(MAX, EXTENT, FLICK, MAX),
                "a flick past the last page cannot leave the range");
        assertEquals(0, PageViewRenderElement.settleTarget(0, EXTENT, -FLICK, MAX),
                "a flick before the first page cannot go negative");
    }

    @Test
    @DisplayName("a slow drag-and-hold release is not treated as a flick")
    void belowToleranceIsNotAFlick() {
        // Just under the tolerance: this is someone stopping, not flicking, so the
        // carousel returns to the page it is nearest rather than advancing.
        float justUnder = 1e-4f;
        assertEquals(0, PageViewRenderElement.settleTarget(100, EXTENT, justUnder, MAX));
    }
}
