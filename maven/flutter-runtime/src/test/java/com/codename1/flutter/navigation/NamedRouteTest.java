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
