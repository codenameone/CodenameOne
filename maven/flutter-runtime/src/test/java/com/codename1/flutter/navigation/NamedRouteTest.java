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

import com.codename1.flutter.testsupport.ProbeBox;

import dart.runtime.Funcs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Named-route resolution: the {@code routes} map first, then
 * {@code onGenerateRoute}, then {@code onUnknownRoute} — Flutter's order.
 * Headless, so no Forms are created and route builders never run; only the
 * resolution and stack bookkeeping are exercised.
 */
class NamedRouteTest {

    @BeforeEach
    void reset() {
        Navigator.reset();
        Navigator.resetRouteTable();
    }

    @AfterEach
    void clear() {
        Navigator.reset();
        Navigator.resetRouteTable();
    }

    private static MaterialPageRoute route() {
        MaterialPageRoute r = new MaterialPageRoute();
        r.builder((context) -> new ProbeBox(10, 10));
        return r;
    }

    @Test
    void routesMapWins() {
        Map<Object, Object> routes = new HashMap<Object, Object>();
        routes.put("/a", (Funcs.Func1<Object, Object>) context -> new ProbeBox(1, 1));
        Navigator.installRouteTable(routes, settings -> route(), null);

        Route r = Navigator.resolveRoute("/a", null);
        assertNotNull(r);
        assertNotNull(((MaterialPageRoute) r).getBuilder(),
                "a routes-map entry becomes a MaterialPageRoute around that builder");
        assertEquals("/a", r.settings().name());
    }

    @Test
    void onGenerateRouteHandlesWhatTheMapDoesNot() {
        MaterialPageRoute generated = route();
        Navigator.installRouteTable(new HashMap<Object, Object>(), settings -> generated, null);
        assertSame(generated, Navigator.resolveRoute("/demo/app-bar", null));
    }

    @Test
    void onUnknownRouteIsTheLastResort() {
        MaterialPageRoute fallback = route();
        Navigator.installRouteTable(null, settings -> null, settings -> fallback);
        assertSame(fallback, Navigator.resolveRoute("/nope", null));
    }

    @Test
    void argumentsAndNameReachTheFactory() {
        final String[] seenName = new String[1];
        final Object[] seenArgs = new Object[1];
        Object args = new Object();
        Navigator.installRouteTable(null, settings -> {
            seenName[0] = settings.name();
            seenArgs[0] = settings.arguments();
            return route();
        }, null);

        Navigator.resolveRoute("/demo/banner", args);
        assertEquals("/demo/banner", seenName[0]);
        assertSame(args, seenArgs[0]);
    }

    @Test
    void unresolvableNameIsReportedNotThrown() {
        Navigator.installRouteTable(null, settings -> null, null);
        assertNull(Navigator.resolveRoute("/missing", null));
        assertFalse(Navigator.pushNamed(null, "/missing", null));
        assertEquals(0, Navigator.stackSize(), "a dead link must not push anything");
    }

    @Test
    void pushNamedGrowsTheStack() {
        Navigator.installRouteTable(null, settings -> route(), null);
        assertTrue(Navigator.pushNamed(null, "/demo/app-bar", null));
        assertEquals(1, Navigator.stackSize());
    }

    @Test
    void navigatorStateRoutesNamedPushesToTheStack() {
        Navigator.installRouteTable(null, settings -> route(), null);
        NavigatorState state = Navigator.of(null, null);

        state.pushNamed("/demo/a", null);
        assertEquals(1, Navigator.stackSize());
        assertEquals("/demo/b", state.restorablePushNamed("/demo/b", null),
                "the restoration id is informational — the route name itself");
        assertEquals(2, Navigator.stackSize());
        assertTrue(state.canPop());

        state.pop(null);
        assertEquals(1, Navigator.stackSize());
    }

    @Test
    void pushReplacementSwapsTheTopRoute() {
        Navigator.installRouteTable(null, settings -> route(), null);
        NavigatorState state = Navigator.of(null, null);
        state.pushNamed("/a", null);
        state.pushNamed("/b", null);
        assertEquals(2, Navigator.stackSize());

        state.pushReplacementNamed("/c", null, null);
        assertEquals(2, Navigator.stackSize(), "one popped, one pushed");
    }

    @Test
    void aReplacementThatCannotBeBuiltLeavesTheCurrentRoute() {
        // Only "/known" resolves; anything else has no route.
        Navigator.installRouteTable(null, settings -> "/unknown".equals(settings.name()) ? null : route(), null);
        NavigatorState state = Navigator.of(null, null);
        state.pushNamed("/a", null);
        state.pushNamed("/b", null);
        state.pushReplacementNamed("/unknown", null, null);
        assertEquals(2, Navigator.stackSize(),
                "the page stays: popping first sent the user back when the replacement failed");
    }

    @Test
    void popUntilUnwindsToTheAcceptedRoute() {
        Navigator.installRouteTable(null, settings -> route(), null);
        NavigatorState state = Navigator.of(null, null);
        state.pushNamed("/a", null);
        state.pushNamed("/b", null);
        state.pushNamed("/c", null);

        state.popUntil(r -> Boolean.FALSE);
        assertEquals(0, Navigator.stackSize(), "an always-false predicate unwinds to the base route");
    }
}
