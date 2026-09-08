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

import com.codename1.flutter.rendering.Dp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Where a Scaffold puts its FloatingActionButton.
 *
 * <p>{@code floatingActionButtonLocation} was accepted and discarded, so every
 * FAB floated at the bottom right whatever it asked for. Reply's compose button
 * is centreDocked and drew in the corner on top of the bottom bar instead of
 * centred and straddling it.</p>
 *
 * <p>Headless, so one logical pixel is one device pixel.</p>
 */
class FabPlacementTest {

    private static final double SCAFFOLD_W = 400;
    private static final double SCAFFOLD_H = 800;
    private static final double FAB = 56;
    private static final double NAV = 80;

    private static double x(FloatingActionButtonLocation where) {
        return ScaffoldRenderElement.fabX(where, SCAFFOLD_W, FAB);
    }

    private static double y(FloatingActionButtonLocation where) {
        return ScaffoldRenderElement.fabY(where, SCAFFOLD_H, FAB, NAV);
    }

    @Test
    void centreLocationsCentreTheFab() {
        assertEquals((SCAFFOLD_W - FAB) / 2, x(FloatingActionButtonLocation.centerDocked), 0.001);
        assertEquals((SCAFFOLD_W - FAB) / 2, x(FloatingActionButtonLocation.centerFloat), 0.001);
        assertEquals((SCAFFOLD_W - FAB) / 2, x(FloatingActionButtonLocation.miniCenterTop), 0.001);
    }

    @Test
    void startAndEndSitAgainstTheirEdges() {
        double margin = Dp.px(16);
        assertEquals(margin, x(FloatingActionButtonLocation.startFloat), 0.001);
        assertEquals(SCAFFOLD_W - FAB - margin, x(FloatingActionButtonLocation.endFloat), 0.001);
    }

    @Test
    void noLocationIsFluttersEndFloat() {
        double margin = Dp.px(16);
        assertEquals(SCAFFOLD_W - FAB - margin, x(null), 0.001);
        assertEquals(SCAFFOLD_H - NAV - FAB - margin, y(null), 0.001);
    }

    @Test
    void aDockedFabStraddlesTheBottomStripsTopEdge() {
        // Its CENTRE sits on the edge, which is what lets a notched bar cut a
        // hole for it; a floating one clears the strip by the margin instead.
        double contentBottom = SCAFFOLD_H - NAV;
        assertEquals(contentBottom - FAB / 2, y(FloatingActionButtonLocation.centerDocked), 0.001);
        assertEquals(contentBottom - FAB - Dp.px(16),
                y(FloatingActionButtonLocation.centerFloat), 0.001);
    }

    @Test
    void theContentStopsAtTheBarWhenThereIsOne() {
        // A bottom bar is what holds the content off the edge of the display,
        // so the safe-area inset is NOT added on top of it. Adding both lifted
        // the reply study's docked button clear of its own bar and into the mail
        // list, where a card painted over it and it vanished.
        assertEquals(80.0, ScaffoldRenderElement.contentInset(80, 102), 0.001);
        // With no bar, the display's own padding is what the content stops at.
        assertEquals(102.0, ScaffoldRenderElement.contentInset(0, 102), 0.001);
    }

    @Test
    void aTopFabSitsAtTheTop() {
        assertEquals(Dp.px(16), y(FloatingActionButtonLocation.centerTop), 0.001);
    }

    @Test
    void aDockedFabNeverFallsOffTheBottom() {
        // Flutter clamps it to the scaffold; a bottom strip taller than the
        // scaffold would otherwise push it past the edge.
        double got = ScaffoldRenderElement.fabY(
                FloatingActionButtonLocation.centerDocked, SCAFFOLD_H, FAB, 0);
        assertEquals(SCAFFOLD_H - FAB - Dp.px(16), got, 0.001);
    }
}
