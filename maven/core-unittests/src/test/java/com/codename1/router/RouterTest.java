package com.codename1.router;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RouterTest extends UITestBase {

    @BeforeEach
    void resetRouter() {
        Router.getInstance().reset();
    }

    /// `Router.start` actually shows a form; the UITestBase Display is enough
    /// for the show() machinery to run without throwing.
    @Test
    void startShowsRootForm() {
        final AtomicInteger built = new AtomicInteger();
        Router.getInstance()
                .route("/", new RouteBuilder() {
                    public Form build(RouteContext c) {
                        built.incrementAndGet();
                        return new Form();
                    }
                })
                .start("/");
        flushSerialCalls();
        assertEquals(1, built.get(), "root builder must be invoked once on start");
        Location loc = Router.getInstance().getCurrentLocation();
        assertNotNull(loc);
        assertEquals("/", loc.getPath());
        assertEquals(0, loc.getStackIndex());
    }

    @Test
    void pushIncrementsStack() {
        Router.getInstance()
                .route("/", builderReturning(new Form()))
                .route("/users/:id", new RouteBuilder() {
                    public Form build(RouteContext c) {
                        Form f = new Form();
                        f.putClientProperty("id", c.param("id"));
                        return f;
                    }
                })
                .start("/");
        Router.push("/users/42");
        flushSerialCalls();
        assertEquals(2, Router.getInstance().getStackDepth());
        Location loc = Router.getInstance().getCurrentLocation();
        assertEquals("/users/42", loc.getPath());
        assertEquals("/users/:id", loc.getMatchedPattern());
    }

    @Test
    void popReturnsToPrevious() {
        Router.getInstance()
                .route("/", builderReturning(new Form()))
                .route("/a", builderReturning(new Form()))
                .start("/");
        Router.push("/a");
        assertTrue(Router.pop());
        flushSerialCalls();
        assertEquals(1, Router.getInstance().getStackDepth());
        assertEquals("/", Router.getInstance().getCurrentLocation().getPath());
    }

    @Test
    void popOnRootReturnsFalse() {
        Router.getInstance()
                .route("/", builderReturning(new Form()))
                .start("/");
        assertFalse(Router.pop());
    }

    @Test
    void replaceSwapsTopWithoutChangingDepth() {
        Router.getInstance()
                .route("/", builderReturning(new Form()))
                .route("/a", builderReturning(new Form()))
                .route("/b", builderReturning(new Form()))
                .start("/");
        Router.push("/a");
        Router.replace("/b");
        flushSerialCalls();
        assertEquals(2, Router.getInstance().getStackDepth());
        assertEquals("/b", Router.getInstance().getCurrentLocation().getPath());
    }

    @Test
    void specificityChoosesLiteralOverParam() {
        final AtomicReference<String> hit = new AtomicReference<String>();
        Router.getInstance()
                .route("/", builderReturning(new Form()))
                .route("/users/:id", new RouteBuilder() {
                    public Form build(RouteContext c) { hit.set("param"); return new Form(); }
                })
                .route("/users/me", new RouteBuilder() {
                    public Form build(RouteContext c) { hit.set("literal"); return new Form(); }
                })
                .start("/");
        Router.push("/users/me");
        assertEquals("literal", hit.get());
    }

    @Test
    void notFoundFallsBack() {
        final AtomicBoolean hit = new AtomicBoolean();
        Router.getInstance()
                .route("/", builderReturning(new Form()))
                .notFound(new RouteBuilder() {
                    public Form build(RouteContext c) { hit.set(true); return new Form(); }
                })
                .start("/");
        Router.push("/no/such/route");
        assertTrue(hit.get());
    }

    @Test
    void guardCanRedirect() {
        final AtomicBoolean loginShown = new AtomicBoolean();
        Router.getInstance()
                .route("/", builderReturning(new Form()))
                .route("/admin", builderReturning(new Form()))
                .route("/login", new RouteBuilder() {
                    public Form build(RouteContext c) { loginShown.set(true); return new Form(); }
                })
                .guard("/admin/**", new RouteGuard() {
                    public Decision check(RouteContext c) { return Decision.redirect("/login"); }
                })
                .start("/");
        Router.push("/admin");
        assertTrue(loginShown.get(), "guard redirect must route to /login");
        assertEquals("/login", Router.getInstance().getCurrentLocation().getPath());
    }

    @Test
    void guardCanBlock() {
        Router.getInstance()
                .route("/", builderReturning(new Form()))
                .route("/secret", builderReturning(new Form()))
                .guard("/secret", new RouteGuard() {
                    public Decision check(RouteContext c) { return Decision.BLOCK; }
                })
                .start("/");
        Router.push("/secret");
        assertEquals("/", Router.getInstance().getCurrentLocation().getPath(),
                "blocked navigation should not move the stack");
    }

    @Test
    void redirectIsRewritten() {
        final AtomicReference<String> hit = new AtomicReference<String>();
        Router.getInstance()
                .route("/", builderReturning(new Form()))
                .route("/new/x", new RouteBuilder() {
                    public Form build(RouteContext c) { hit.set("/new/x"); return new Form(); }
                })
                .redirect("/old/x", "/new/x")
                .start("/");
        Router.push("/old/x");
        assertEquals("/new/x", hit.get());
        assertEquals("/new/x", Router.getInstance().getCurrentLocation().getPath());
    }

    @Test
    void locationListenerFiresInOrder() {
        final List<String> events = new ArrayList<String>();
        Router.getInstance()
                .route("/", builderReturning(new Form()))
                .route("/a", builderReturning(new Form()))
                .addLocationListener(new LocationListener() {
                    public void onLocationChanged(Location prev, Location current, Kind kind) {
                        events.add(kind + " " + current.getPath());
                    }
                })
                .start("/");
        Router.push("/a");
        Router.pop();
        assertEquals(3, events.size());
        assertEquals("RESET /", events.get(0));
        assertEquals("PUSH /a", events.get(1));
        assertEquals("POP /", events.get(2));
    }

    @Test
    void deepLinkHandlerRoutes() {
        Router.getInstance()
                .route("/", builderReturning(new Form()))
                .route("/share/:id", builderReturning(new Form()))
                .start("/");
        boolean consumed = Router.getInstance()
                .asDeepLinkHandler()
                .handle(DeepLink.parse("https://example.com/share/abc"));
        assertTrue(consumed);
        assertEquals("/share/abc", Router.getInstance().getCurrentLocation().getPath());
    }

    private static RouteBuilder builderReturning(final Form f) {
        return new RouteBuilder() {
            public Form build(RouteContext c) { return f; }
        };
    }
}
