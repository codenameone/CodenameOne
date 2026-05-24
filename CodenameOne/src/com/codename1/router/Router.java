/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.router;

import com.codename1.io.Log;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Declarative, fluent navigation router on top of `Form`. **Optional.** Existing
/// `Form.show()` / `Form.showBack()` code keeps working -- `Router` layers URL-based
/// addressing, deep-link integration, guards, redirects, and a navigation stack on
/// top so apps can speak in URLs instead of explicit form references.
///
/// #### Quick start
///
/// ```java
/// Router.getInstance()
///     .route("/", new RouteBuilder() {
///         public Form build(RouteContext c) { return new HomeForm(); }
///     })
///     .route("/users/:id", new RouteBuilder() {
///         public Form build(RouteContext c) { return new ProfileForm(c.param("id")); }
///     })
///     .guard("/account/**", new RouteGuard() {
///         public RouteGuard.Decision check(RouteContext c) {
///             return UserSession.isLoggedIn() ? RouteGuard.Decision.PROCEED
///                     : RouteGuard.Decision.redirect("/login");
///         }
///     })
///     .notFound(new RouteBuilder() {
///         public Form build(RouteContext c) { return new NotFoundForm(); }
///     })
///     .start("/");
///
/// // Later, anywhere in the app:
/// Router.push("/users/42");
/// Router.replace("/login");
/// Router.pop();
/// ```
///
/// #### Deep-link integration
///
/// Install `Router.asDeepLinkHandler()` as the platform link handler and every
/// universal-link / custom-scheme launch will be routed automatically:
///
/// ```java
/// Display.getInstance().setDeepLinkHandler(Router.getInstance().asDeepLinkHandler());
/// ```
///
/// #### Threading
///
/// All Router methods must be called on the EDT. The Router itself never calls
/// builders off-thread.
///
/// #### Since 8.0
public final class Router {

    private static final Router INSTANCE = new Router();

    /// Returns the singleton Router. There is exactly one router per app; nested
    /// routers (e.g. inside a `TabsForm` tab) are implemented as scopes on this one.
    public static Router getInstance() { return INSTANCE; }

    // ---- registry -----------------------------------------------------------

    private final List<RouteMatch> routes = new ArrayList<RouteMatch>();
    private final List<GuardEntry> guards = new ArrayList<GuardEntry>();
    private final List<RedirectEntry> redirects = new ArrayList<RedirectEntry>();
    private RouteBuilder notFoundBuilder;

    // ---- runtime state ------------------------------------------------------

    private final List<StackEntry> stack = new ArrayList<StackEntry>();
    private final List<LocationListener> listeners = new ArrayList<LocationListener>();
    private boolean navigating;
    private BrowserHistoryBridge historyBridge;
    /// Guard flag: when the browser history bridge is the one that informed the
    /// router about a navigation (e.g., user pressed browser back), we skip
    /// notifying the bridge again to avoid double-pushing entries.
    private boolean suppressBridgeOnce;

    private Router() { }

    // -------------------------------------------------------------------------
    // Registration (fluent)
    // -------------------------------------------------------------------------

    /// Registers a route. The pattern supports `:name` params, `*` single-segment
    /// wildcards, and `**` catch-all wildcards. Last registration wins on exact
    /// duplicate; on overlap, the more specific pattern wins regardless of order.
    public Router route(String pattern, RouteBuilder builder) {
        if (builder == null) { throw new IllegalArgumentException("builder cannot be null"); }
        // Replace any existing exact pattern.
        for (int i = 0; i < routes.size(); i++) {
            if (routes.get(i).getPattern().equals(normalize(pattern))) {
                routes.set(i, new RouteMatch(pattern, builder));
                return this;
            }
        }
        routes.add(new RouteMatch(pattern, builder));
        return this;
    }

    /// Static permanent redirect: any navigation matching `fromPattern` is rewritten
    /// to `toPattern`. Path params from the source are not transferred; for that,
    /// use a `RouteGuard` returning #Decision#redirect.
    public Router redirect(String fromPattern, String toPattern) {
        redirects.add(new RedirectEntry(new RouteMatch(fromPattern, null), toPattern));
        return this;
    }

    /// Registers a guard scoped to a path pattern (typically with a `**` suffix).
    /// Guards run in registration order, before the route builder.
    public Router guard(String pathPattern, RouteGuard guard) {
        guards.add(new GuardEntry(new RouteMatch(pathPattern, null), guard));
        return this;
    }

    /// Registers the fallback builder used when no route matches.
    public Router notFound(RouteBuilder builder) {
        this.notFoundBuilder = builder;
        return this;
    }

    /// Convenience: register a "shell" -- a builder used as a wrapper for child
    /// routes that share persistent chrome (e.g. a `TabsForm`). The shell itself is
    /// the route at `pattern`; children at `pattern + childPath` are normal routes
    /// whose builder can call `shellHost.embed(...)` to slot content into the
    /// persistent chrome.
    ///
    /// This is a thin sugar on `route(...)` -- shells are not a separate object kind.
    public Router shell(String pattern, RouteBuilder builder) {
        return route(pattern, builder);
    }

    /// Removes all routes, guards, redirects, listeners, and stack. Mostly for tests.
    public Router reset() {
        routes.clear();
        guards.clear();
        redirects.clear();
        listeners.clear();
        stack.clear();
        notFoundBuilder = null;
        return this;
    }

    /// Initializes the navigation stack with `initialPath` and shows the matching
    /// Form. Equivalent to `push(initialPath)` but fires a `RESET` location event.
    public Router start(String initialPath) {
        stack.clear();
        navigate(initialPath, NavKind.RESET);
        return this;
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    /// Pushes a new entry on the stack and shows its Form. Static shortcut over
    /// `getInstance().pushPath(path)`.
    public static void push(String path) { INSTANCE.pushPath(path); }

    /// Replaces the top stack entry. Static shortcut.
    public static void replace(String path) { INSTANCE.replacePath(path); }

    /// Pops the top stack entry and shows the entry beneath. Static shortcut.
    public static boolean pop() { return INSTANCE.popOne(); }

    /// Instance form of #push.
    public Router pushPath(String path) {
        navigate(path, NavKind.PUSH);
        return this;
    }

    /// Instance form of #replace.
    public Router replacePath(String path) {
        navigate(path, NavKind.REPLACE);
        return this;
    }

    /// Instance form of #pop. Returns false if the stack has 0 or 1 entries
    /// (nothing to pop back to).
    public boolean popOne() {
        if (stack.size() <= 1) { return false; }
        StackEntry leaving = stack.get(stack.size() - 1);
        Form current = leaving.form;
        if (current != null && !current.checkPopGuard(PopReason.PROGRAMMATIC)) {
            return false;
        }
        StackEntry previous = stack.remove(stack.size() - 1);
        StackEntry now = stack.get(stack.size() - 1);
        if (now.form != null) {
            now.form.showBack();
        }
        Location prevLoc = locationFor(previous, stack.size());
        Location nowLoc = locationFor(now, stack.size() - 1);
        fireLocation(prevLoc, nowLoc, LocationListener.Kind.POP);
        notifyBridge(LocationListener.Kind.POP, nowLoc);
        return true;
    }

    /// Returns the current `Location`, or null if the stack is empty.
    public Location getCurrentLocation() {
        if (stack.isEmpty()) { return null; }
        return locationFor(stack.get(stack.size() - 1), stack.size() - 1);
    }

    /// Returns the stack depth (1 for a single entry).
    public int getStackDepth() { return stack.size(); }

    /// Installs a `BrowserHistoryBridge` (typically only used by the JavaScript
    /// port). When set, every push/pop/replace is reflected in the bridge so the
    /// host's URL bar and history stack stay in sync.
    ///
    /// #### Since 8.0
    public Router setBrowserHistoryBridge(BrowserHistoryBridge bridge) {
        this.historyBridge = bridge;
        return this;
    }

    /// Returns the installed `BrowserHistoryBridge`, or null.
    public BrowserHistoryBridge getBrowserHistoryBridge() { return historyBridge; }

    /// Called by the `BrowserHistoryBridge` when the host history reported a
    /// navigation that the Router should mirror **without** re-notifying the
    /// bridge (which would cause a feedback loop).
    ///
    /// `kind` corresponds to the kind of host event (`PUSH` for a forward
    /// navigation triggered from outside, `POP` for browser back, `REPLACE` for
    /// a replaceState call).
    ///
    /// #### Since 8.0
    public boolean onBrowserNavigated(String path, LocationListener.Kind kind) {
        suppressBridgeOnce = true;
        try {
            if (kind == LocationListener.Kind.POP) {
                return popOne();
            } else if (kind == LocationListener.Kind.REPLACE) {
                replacePath(path);
                return true;
            } else {
                pushPath(path);
                return true;
            }
        } finally {
            suppressBridgeOnce = false;
        }
    }

    /// Adds a location listener. Listeners are notified after every push/pop/replace/reset.
    public Router addLocationListener(LocationListener l) {
        if (l != null && !listeners.contains(l)) { listeners.add(l); }
        return this;
    }

    /// Removes a previously added location listener.
    public Router removeLocationListener(LocationListener l) {
        listeners.remove(l);
        return this;
    }

    // -------------------------------------------------------------------------
    // Deep-link integration
    // -------------------------------------------------------------------------

    /// Returns a `LinkHandler` that routes incoming deep links through this Router.
    /// Each incoming link replaces the current stack-top if it matches the same
    /// pattern (avoiding duplicate entries from app-relaunches via the same URL);
    /// otherwise it pushes.
    public LinkHandler asDeepLinkHandler() {
        return new LinkHandler() {
            @Override
            public boolean handle(DeepLink link) {
                return Router.this.handle(link);
            }
        };
    }

    /// Routes a parsed `DeepLink`. Equivalent to `pushPath(link.getPath())` for now;
    /// retained as its own method so we can pass the raw link to guards/builders in
    /// the future (e.g. include host in matching for multi-host universal links).
    public boolean handle(DeepLink link) {
        if (link == null || link.isEmpty()) { return false; }
        // If the same pattern is already on top, replace rather than push so two
        // taps of the same universal link don't accumulate history.
        String path = link.getPath();
        if (!stack.isEmpty()) {
            StackEntry top = stack.get(stack.size() - 1);
            if (top.link != null && top.link.getPath().equals(path)) {
                return navigate(path, NavKind.REPLACE) != null;
            }
        }
        return navigate(path, NavKind.PUSH) != null;
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private enum NavKind { PUSH, REPLACE, RESET }

    private Form navigate(String path, NavKind kind) {
        if (navigating) {
            Log.p("Router.navigate called re-entrantly; ignoring " + path);
            return null;
        }
        navigating = true;
        try {
            String norm = normalize(path);
            DeepLink link = DeepLink.parse(norm);

            // Redirects (static rewrites). Loop-protected by a small bound.
            for (int hops = 0; hops < 8; hops++) {
                boolean redirected = false;
                for (RedirectEntry r : redirects) {
                    if (r.from.match(link.getPath()) != null) {
                        link = link.withPath(r.to);
                        redirected = true;
                        break;
                    }
                }
                if (!redirected) { break; }
            }

            MatchResult match = findMatch(link);

            // Guard chain.
            for (GuardEntry ge : guards) {
                if (ge.scope.match(link.getPath()) == null) { continue; }
                RouteContext ctx = new RouteContext(link,
                        match == null ? new LinkedHashMap<String, String>() : match.params,
                        match == null ? null : match.route.getPattern());
                RouteGuard.Decision d = ge.guard.check(ctx);
                if (d == null || d.getKind() == RouteGuard.Decision.Kind.PROCEED) { continue; }
                if (d.getKind() == RouteGuard.Decision.Kind.BLOCK) {
                    return null;
                }
                if (d.getKind() == RouteGuard.Decision.Kind.REDIRECT) {
                    navigating = false;
                    return navigate(d.getRedirectTo(), kind);
                }
            }

            RouteBuilder builder;
            String matchedPattern;
            Map<String, String> params;
            if (match != null) {
                builder = match.route.getBuilder();
                matchedPattern = match.route.getPattern();
                params = match.params;
            } else if (notFoundBuilder != null) {
                builder = notFoundBuilder;
                matchedPattern = null;
                params = new LinkedHashMap<String, String>();
            } else {
                Log.p("Router: no route for " + link.getPath() + " and no notFound builder");
                return null;
            }

            RouteContext ctx = new RouteContext(link, params, matchedPattern);
            Form built = builder.build(ctx);
            if (built == null) {
                Log.p("Router: builder for " + link.getPath() + " returned null");
                return null;
            }

            StackEntry previousTop = stack.isEmpty() ? null : stack.get(stack.size() - 1);
            StackEntry entry = new StackEntry(link, matchedPattern, built);
            LocationListener.Kind kindForEvent;
            switch (kind) {
                case REPLACE:
                    if (previousTop != null) {
                        // Honor a pop guard on the form being replaced.
                        if (previousTop.form != null
                                && !previousTop.form.checkPopGuard(PopReason.REPLACE)) {
                            return null;
                        }
                        stack.set(stack.size() - 1, entry);
                    } else {
                        stack.add(entry);
                    }
                    kindForEvent = LocationListener.Kind.REPLACE;
                    break;
                case RESET:
                    stack.clear();
                    stack.add(entry);
                    kindForEvent = LocationListener.Kind.RESET;
                    break;
                default:
                    stack.add(entry);
                    kindForEvent = LocationListener.Kind.PUSH;
                    break;
            }

            // Show the form. Use show() for forward navigation; replaces and resets
            // also use forward transition by convention.
            if (CN.isEdt()) {
                built.show();
            } else {
                Display.getInstance().callSerially(new ShowOnEdt(built));
            }

            Location prevLoc = previousTop == null ? null
                    : locationFor(previousTop,
                            kindForEvent == LocationListener.Kind.PUSH ? stack.size() - 2 : stack.size() - 1);
            Location nowLoc = locationFor(entry, stack.size() - 1);
            fireLocation(prevLoc, nowLoc, kindForEvent);
            notifyBridge(kindForEvent, nowLoc);
            return built;
        } finally {
            navigating = false;
        }
    }

    private MatchResult findMatch(DeepLink link) {
        MatchResult best = null;
        int bestScore = Integer.MIN_VALUE;
        for (RouteMatch r : routes) {
            Map<String, String> p = r.match(link.getPath());
            if (p == null) { continue; }
            int sc = r.specificity();
            if (sc > bestScore) {
                bestScore = sc;
                best = new MatchResult(r, p);
            }
        }
        return best;
    }

    private void fireLocation(Location prev, Location now, LocationListener.Kind k) {
        // Snapshot so a listener can remove itself without ConcurrentModification.
        LocationListener[] snap = listeners.toArray(new LocationListener[listeners.size()]);
        for (LocationListener l : snap) {
            try {
                l.onLocationChanged(prev, now, k);
            } catch (Throwable t) {
                Log.e(t);
            }
        }
    }

    private static Location locationFor(StackEntry e, int idx) {
        return new Location(e.link, e.matchedPattern, idx);
    }

    private void notifyBridge(LocationListener.Kind kind, Location loc) {
        BrowserHistoryBridge b = historyBridge;
        if (b == null || suppressBridgeOnce) { return; }
        try {
            switch (kind) {
                case PUSH:    b.onPush(loc); break;
                case REPLACE: b.onReplace(loc); break;
                case POP:     b.onPop(loc); break;
                case RESET:   b.onReplace(loc); break;
            }
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    private static String normalize(String path) {
        if (path == null || path.length() == 0) { return "/"; }
        return path.charAt(0) == '/' ? path : "/" + path;
    }

    // -------------------------------------------------------------------------
    // Aggregate types
    // -------------------------------------------------------------------------

    private static final class StackEntry {
        final DeepLink link;
        final String matchedPattern;
        final Form form;
        StackEntry(DeepLink l, String mp, Form f) { this.link = l; this.matchedPattern = mp; this.form = f; }
    }

    private static final class MatchResult {
        final RouteMatch route;
        final Map<String, String> params;
        MatchResult(RouteMatch r, Map<String, String> p) { this.route = r; this.params = p; }
    }

    private static final class GuardEntry {
        final RouteMatch scope;
        final RouteGuard guard;
        GuardEntry(RouteMatch s, RouteGuard g) { this.scope = s; this.guard = g; }
    }

    private static final class RedirectEntry {
        final RouteMatch from;
        final String to;
        RedirectEntry(RouteMatch f, String t) { this.from = f; this.to = t; }
    }

    /// Carries a Form through `Display.callSerially` when `navigate()` is invoked
    /// off-EDT. Named/static so it doesn't carry an implicit outer reference.
    private static final class ShowOnEdt implements Runnable {
        private final Form form;
        ShowOnEdt(Form form) { this.form = form; }
        @Override
        public void run() { form.show(); }
    }
}
