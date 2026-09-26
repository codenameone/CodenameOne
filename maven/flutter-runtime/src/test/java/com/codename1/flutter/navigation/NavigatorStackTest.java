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
package com.codename1.flutter.navigation;

import com.codename1.flutter.material.Dialogs;
import com.codename1.flutter.testsupport.ProbeBox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Navigator route-stack and dialog-stack bookkeeping, headless: no Display
 * means no Forms are created and the route builders never run — only the
 * stack logic is exercised. Dialogs DO mount their subtree headless (bare
 * RenderHost), and Navigator.pop always dismisses the topmost dialog before
 * popping a route.
 */
class NavigatorStackTest {

    @BeforeEach
    void resetStacks() {
        Navigator.reset();
        Dialogs.reset();
    }

    private static MaterialPageRoute route() {
        MaterialPageRoute r = new MaterialPageRoute();
        r.builder((context) -> new ProbeBox(10, 10));
        return r;
    }

    @Test
    void pushGrowsAndPopShrinksTheStack() {
        assertEquals(0, Navigator.stackSize());
        Navigator.push(null, route());
        Navigator.push(null, route());
        assertEquals(2, Navigator.stackSize());
        Navigator.pop(null);
        assertEquals(1, Navigator.stackSize());
        Navigator.pop(null);
        assertEquals(0, Navigator.stackSize());
    }

    @Test
    void poppingTheLastRouteIsANoOp() {
        Navigator.pop(null);
        Navigator.pop(null);
        assertEquals(0, Navigator.stackSize(), "the implicit base route can never be popped");
    }

    @Test
    void dialogMountsHeadlessAndPopDismissesItBeforeRoutes() {
        Navigator.push(null, route());
        assertEquals(1, Navigator.stackSize());

        final int[] built = {0};
        Dialogs.showDialog(null, (context) -> {
            built[0]++;
            return new ProbeBox(5, 5);
        });
        assertEquals(1, built[0], "the dialog builder ran on mount");
        assertEquals(1, Dialogs.openDialogCount());

        // pop dismisses the dialog, NOT the route
        Navigator.pop(null);
        assertEquals(0, Dialogs.openDialogCount());
        assertEquals(1, Navigator.stackSize());

        // next pop takes the route
        Navigator.pop(null);
        assertEquals(0, Navigator.stackSize());
    }

    @Test
    void stackedDialogsPopInLifoOrder() {
        Dialogs.showDialog(null, (context) -> new ProbeBox(1, 1));
        Dialogs.showDialog(null, (context) -> new ProbeBox(2, 2));
        assertEquals(2, Dialogs.openDialogCount());
        Navigator.pop(null);
        assertEquals(1, Dialogs.openDialogCount());
        Navigator.pop(null);
        assertEquals(0, Dialogs.openDialogCount());
    }

    @Test
    void headlessPushDoesNotInvokeTheBuilder() {
        final int[] built = {0};
        MaterialPageRoute r = new MaterialPageRoute();
        r.builder((context) -> {
            built[0]++;
            return new ProbeBox(1, 1);
        });
        Navigator.push(null, r);
        assertEquals(0, built[0], "no Display: the page never mounts, the builder never runs");
        assertEquals(1, Navigator.stackSize());
    }

    @Test
    void anOpenDialogCanBePoppedFromTheBaseRoute() {
        // A dialog over the base route: the page stack is empty, but the dialog is the
        // route on top, and maybePop -- what a system-back handler calls -- must close it.
        Dialogs.showDialog(null, (context) -> new ProbeBox(10, 10));
        NavigatorState nav = Navigator.of(null, null);
        assertEquals(true, nav.canPop());
        nav.maybePop(null);
        assertEquals(0, Dialogs.openDialogCount());
        assertEquals(false, nav.canPop());
    }
}
