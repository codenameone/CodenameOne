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
package com.codename1.car;

import com.codename1.car.spi.CarBridge;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Platform-independent coverage for the portable com.codename1.car runtime: the {@link CarContext}
 * back-stack management against a fake {@link CarBridge}, screen lifecycle dispatch, and the
 * template builder model. Needs no platform Display.
 */
class CarTest {

    /** Records the bridge calls so the stack behaviour can be asserted. */
    private static final class FakeBridge implements CarBridge {
        final List<CarScreen> pushed = new ArrayList<CarScreen>();
        int pops;
        int invalidations;
        boolean connected = true;

        public void pushScreen(CarScreen screen) {
            pushed.add(screen);
        }

        public void popScreen() {
            pops++;
        }

        public void invalidate(CarScreen screen) {
            invalidations++;
        }

        public void finish() {
        }

        public boolean isConnected() {
            return connected;
        }

        public void showToast(String message, int durationSeconds) {
        }

        public int getListRowLimit() {
            return 6;
        }

        public int getGridItemLimit() {
            return 8;
        }
    }

    /** Counts lifecycle callbacks so screen transitions can be asserted. */
    private static final class CountingScreen extends CarScreen {
        int created, resumed, paused, destroyed;

        protected CarTemplate onCreateTemplate() {
            return new CarListTemplate().setTitle("screen");
        }

        protected void onCreate() {
            created++;
        }

        protected void onResume() {
            resumed++;
        }

        protected void onPause() {
            paused++;
        }

        protected void onDestroy() {
            destroyed++;
        }
    }

    @Test
    void rootScreenIsRenderedAndStarted() {
        FakeBridge bridge = new FakeBridge();
        CarContext ctx = new CarContext(bridge);
        CountingScreen root = new CountingScreen();
        ctx.setRootScreen(root);

        assertEquals(1, bridge.pushed.size());
        assertSame(root, bridge.pushed.get(0));
        assertSame(root, ctx.getTopScreen());
        assertEquals(1, root.created);
        assertEquals(1, root.resumed);
        assertSame(ctx, root.getContext());
    }

    @Test
    void pushPausesPreviousAndResumesNew() {
        FakeBridge bridge = new FakeBridge();
        CarContext ctx = new CarContext(bridge);
        CountingScreen root = new CountingScreen();
        CountingScreen child = new CountingScreen();
        ctx.setRootScreen(root);
        ctx.pushScreen(child);

        assertSame(child, ctx.getTopScreen());
        assertEquals(2, bridge.pushed.size());
        assertEquals(1, root.paused);
        assertEquals(1, child.created);
        assertEquals(1, child.resumed);
    }

    @Test
    void popReturnsToPreviousAndDestroysTop() {
        FakeBridge bridge = new FakeBridge();
        CarContext ctx = new CarContext(bridge);
        CountingScreen root = new CountingScreen();
        CountingScreen child = new CountingScreen();
        ctx.setRootScreen(root);
        ctx.pushScreen(child);
        ctx.popScreen();

        assertSame(root, ctx.getTopScreen());
        assertEquals(1, bridge.pops);
        assertEquals(1, child.destroyed);
        assertNull(child.getContext());
        // root is resumed twice: initial push, then again after the child pops.
        assertEquals(2, root.resumed);
    }

    @Test
    void poppingTheRootIsANoOp() {
        FakeBridge bridge = new FakeBridge();
        CarContext ctx = new CarContext(bridge);
        CountingScreen root = new CountingScreen();
        ctx.setRootScreen(root);
        ctx.popScreen();

        assertSame(root, ctx.getTopScreen());
        assertEquals(0, bridge.pops);
        assertEquals(0, root.destroyed);
    }

    @Test
    void invalidateOnlyForAttachedScreen() {
        FakeBridge bridge = new FakeBridge();
        CarContext ctx = new CarContext(bridge);
        CountingScreen root = new CountingScreen();
        ctx.setRootScreen(root);
        root.invalidate();
        assertEquals(1, bridge.invalidations);

        // A detached screen does not reach the bridge.
        new CountingScreen().invalidate();
        assertEquals(1, bridge.invalidations);
    }

    @Test
    void contextExposesBridgeConstraints() {
        FakeBridge bridge = new FakeBridge();
        CarContext ctx = new CarContext(bridge);
        assertEquals(6, ctx.getListRowLimit());
        assertEquals(8, ctx.getGridItemLimit());
        assertTrue(ctx.isConnected());
    }

    @Test
    void listTemplateAddRowUsesSingleDefaultSection() {
        CarListTemplate t = new CarListTemplate().setTitle("Library");
        t.addRow(new CarRow("Songs"));
        t.addRow(new CarRow("Albums"));
        assertEquals("Library", t.getTitle());
        assertEquals(1, t.getSections().size());
        assertNull(t.getSections().get(0).getHeader());
        assertEquals(2, t.getSections().get(0).getRows().size());
        assertEquals("Songs", t.getSections().get(0).getRows().get(0).getTitle());
    }

    @Test
    void listTemplateSectionsArePreserved() {
        CarListTemplate t = new CarListTemplate();
        t.addSection(new CarSection("Recents").addRow(new CarRow("A")));
        t.addSection(new CarSection("Favorites").addRow(new CarRow("B")).addRow(new CarRow("C")));
        assertEquals(2, t.getSections().size());
        assertEquals("Recents", t.getSections().get(0).getHeader());
        assertEquals(2, t.getSections().get(1).getRows().size());
    }

    @Test
    void rowFluentSettersRoundTrip() {
        CarRow r = new CarRow("Take Five").setText("Dave Brubeck").setBrowsable(true);
        assertEquals("Take Five", r.getTitle());
        assertEquals("Dave Brubeck", r.getText());
        assertTrue(r.isBrowsable());
    }
}
