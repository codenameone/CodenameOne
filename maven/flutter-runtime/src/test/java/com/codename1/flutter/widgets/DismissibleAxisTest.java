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
package com.codename1.flutter.widgets;

import com.codename1.flutter.testsupport.ProbeBox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code DismissDirection.vertical}/{@code up}/{@code down} used to fall straight
 * through to the horizontal handling: {@code allows()} only ever compared the
 * horizontal drag distance against the startToEnd/endToStart signs (every other
 * direction, including the Y-axis ones, took the final "either way" branch), and
 * {@code directionOf()} always reported endToStart/startToEnd regardless of the
 * configured direction. A Dismissible configured for {@code up} therefore accepted a
 * downward drag just as readily as an upward one, and reported it dismissed
 * {@code endToStart} rather than {@code up}.
 */
class DismissibleAxisTest {

    private static DismissibleRenderElement element(DismissDirection direction) {
        Dismissible w = new Dismissible();
        w.child(new ProbeBox(60, 40));
        if (direction != null) {
            w.direction(direction);
        }
        return new DismissibleRenderElement(w);
    }

    @Test
    @DisplayName("vertical, up and down are the Y axis; horizontal, the explicit X-axis "
            + "directions and unstated are the X axis")
    void axisFollowsTheConfiguredDirection() {
        assertFalse(element(DismissDirection.vertical).directionIsXAxis());
        assertFalse(element(DismissDirection.up).directionIsXAxis());
        assertFalse(element(DismissDirection.down).directionIsXAxis());
        assertTrue(element(DismissDirection.horizontal).directionIsXAxis());
        assertTrue(element(DismissDirection.startToEnd).directionIsXAxis());
        assertTrue(element(DismissDirection.endToStart).directionIsXAxis());
        assertTrue(element(null).directionIsXAxis(), "unstated defaults to Flutter's horizontal");
    }

    @Test
    @DisplayName("up only allows dragging up, down only allows dragging down")
    void upAndDownAreOneWayOnly() {
        DismissibleRenderElement up = element(DismissDirection.up);
        assertTrue(up.allows(-5), "dragging up (negative dy)");
        assertFalse(up.allows(5), "dragging down must not be accepted as up");

        DismissibleRenderElement down = element(DismissDirection.down);
        assertTrue(down.allows(5), "dragging down (positive dy)");
        assertFalse(down.allows(-5), "dragging up must not be accepted as down");
    }

    @Test
    @DisplayName("vertical allows dragging either up or down")
    void verticalAllowsEitherWay() {
        DismissibleRenderElement v = element(DismissDirection.vertical);
        assertTrue(v.allows(-5));
        assertTrue(v.allows(5));
    }

    @Test
    @DisplayName("directionOf reports up/down for a Y-axis direction, never endToStart/startToEnd")
    void directionOfReportsTheYAxisDirections() {
        DismissibleRenderElement up = element(DismissDirection.up);
        assertEquals(DismissDirection.up, up.directionOf(-5));

        DismissibleRenderElement down = element(DismissDirection.down);
        assertEquals(DismissDirection.down, down.directionOf(5));

        DismissibleRenderElement vertical = element(DismissDirection.vertical);
        assertEquals(DismissDirection.up, vertical.directionOf(-5));
        assertEquals(DismissDirection.down, vertical.directionOf(5));
    }

    @Test
    @DisplayName("directionOf still reports endToStart/startToEnd for an X-axis direction")
    void directionOfStillReportsTheXAxisDirectionsForHorizontal() {
        DismissibleRenderElement h = element(DismissDirection.horizontal);
        assertEquals(DismissDirection.endToStart, h.directionOf(-5));
        assertEquals(DismissDirection.startToEnd, h.directionOf(5));
    }
}
