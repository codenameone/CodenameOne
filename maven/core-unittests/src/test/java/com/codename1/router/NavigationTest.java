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
package com.codename1.router;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the static in-app navigation API {@link Navigation}.
 *
 * <p>The navigation stack is process-global static state, so each test works
 * relative to a {@link #baseline()} captured at the start of the method and
 * normalises the stack with the public {@link Navigation#popTo} API rather than
 * reaching into the class (no reflection, no inner-state mutation). The
 * package-private {@link NavigationEntry} constructor is reachable because the
 * test shares the {@code com.codename1.router} package.
 */
class NavigationTest extends UITestBase {

    /** A {@link RouteDispatcher} test double: returns a fresh titled Form for any
     * registered path, null for unknown paths, or throws on demand. */
    private static final class FakeDispatcher implements RouteDispatcher {
        final Map<String, Boolean> known = new HashMap<String, Boolean>();
        boolean explode;

        FakeDispatcher route(String path) {
            known.put(path, Boolean.TRUE);
            return this;
        }

        public Form dispatch(String url) {
            if (explode) {
                throw new RuntimeException("dispatch blew up");
            }
            if (known.containsKey(url)) {
                Form f = new Form();
                f.setTitle(url);
                return f;
            }
            return null;
        }
    }

    private int baseline() {
        return Navigation.getStack().size();
    }

    @FormTest
    void navigateReturnsFalseWithoutDispatcher() {
        Navigation.setDispatcher(null);
        int before = baseline();
        assertFalse(Navigation.navigate("/anything"));
        assertEquals(before, baseline());
    }

    @FormTest
    void navigateReturnsFalseForNullPath() {
        Navigation.setDispatcher(new FakeDispatcher().route("/x"));
        assertFalse(Navigation.navigate(null));
    }

    @FormTest
    void navigateReturnsFalseWhenNoRouteMatches() {
        Navigation.setDispatcher(new FakeDispatcher().route("/known"));
        int before = baseline();
        assertFalse(Navigation.navigate("/unknown"));
        assertEquals(before, baseline());
    }

    @FormTest
    void navigateReturnsFalseWhenDispatcherThrows() {
        FakeDispatcher d = new FakeDispatcher().route("/x");
        d.explode = true;
        Navigation.setDispatcher(d);
        int before = baseline();
        assertFalse(Navigation.navigate("/x"));
        assertEquals(before, baseline());
    }

    @FormTest
    void navigatePushesEntryAndBecomesCurrent() {
        Navigation.setDispatcher(new FakeDispatcher().route("/home"));
        int before = baseline();
        assertTrue(Navigation.navigate("/home"));
        assertEquals(before + 1, baseline());
        assertEquals("/home", Navigation.getCurrent().getPath());
        assertEquals("/home", Navigation.getCurrent().getTitle());
    }

    @FormTest
    void getCurrentReturnsTopOfStack() {
        Navigation.setDispatcher(new FakeDispatcher().route("/a").route("/b"));
        Navigation.navigate("/a");
        Navigation.navigate("/b");
        assertEquals("/b", Navigation.getCurrent().getPath());
    }

    @FormTest
    void backReturnsToPreviousEntry() {
        Navigation.setDispatcher(new FakeDispatcher().route("/a").route("/b"));
        Navigation.navigate("/a");
        NavigationEntry a = Navigation.getCurrent();
        Navigation.navigate("/b");
        assertTrue(Navigation.back());
        assertSame(a, Navigation.getCurrent());
    }

    @FormTest
    void backReturnsFalseAtRoot() {
        // Normalise to a single (or empty) stack using only the public API.
        List<NavigationEntry> stack = Navigation.getStack();
        if (!stack.isEmpty()) {
            Navigation.popTo(stack.get(0));
        }
        assertTrue(Navigation.getStack().size() <= 1);
        assertFalse(Navigation.back());
    }

    @FormTest
    void getStackReturnsUnmodifiableSnapshotCopy() {
        Navigation.setDispatcher(new FakeDispatcher().route("/x").route("/y"));
        Navigation.navigate("/x");
        final List<NavigationEntry> snapshot = Navigation.getStack();
        int snapSize = snapshot.size();

        assertThrows(UnsupportedOperationException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                snapshot.add(null);
            }
        });

        // A later navigation must not retroactively grow the earlier snapshot.
        Navigation.navigate("/y");
        assertEquals(snapSize, snapshot.size());
    }

    @FormTest
    void popToNullReturnsFalse() {
        assertFalse(Navigation.popTo(null));
    }

    @FormTest
    void popToEntryNotOnStackReturnsFalse() {
        NavigationEntry ghost = new NavigationEntry("/ghost", new Form());
        assertFalse(Navigation.popTo(ghost));
    }

    @FormTest
    void popToCurrentEntryIsNoopReturningTrue() {
        Navigation.setDispatcher(new FakeDispatcher().route("/a"));
        Navigation.navigate("/a");
        NavigationEntry current = Navigation.getCurrent();
        int before = baseline();
        assertTrue(Navigation.popTo(current));
        assertSame(current, Navigation.getCurrent());
        assertEquals(before, baseline());
    }

    @FormTest
    void popToEarlierEntryPopsInterveningFrames() {
        Navigation.setDispatcher(new FakeDispatcher().route("/a").route("/b").route("/c"));
        Navigation.navigate("/a");
        NavigationEntry a = Navigation.getCurrent();
        Navigation.navigate("/b");
        Navigation.navigate("/c");
        assertTrue(Navigation.popTo(a));
        assertSame(a, Navigation.getCurrent());
    }

    @FormTest
    void dispatchExternalUrlReturnsFalseForNullOrEmpty() {
        assertFalse(Navigation.dispatchExternalUrl(null));
        assertFalse(Navigation.dispatchExternalUrl(""));
    }

    @FormTest
    void dispatchExternalUrlNavigatesWhenOnEdt() {
        Navigation.setDispatcher(new FakeDispatcher().route("/deep"));
        assertTrue(Navigation.dispatchExternalUrl("/deep"));
        assertEquals("/deep", Navigation.getCurrent().getPath());
    }

    @FormTest
    void navigationEntryTitleFallsBackToEmptyWhenFormHasNoTitle() {
        NavigationEntry e = new NavigationEntry("/p", new Form());
        assertEquals("/p", e.getPath());
        assertEquals("", e.getTitle());
        assertTrue(e.toString().contains("/p"));
    }
}
