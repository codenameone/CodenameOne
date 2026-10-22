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
package com.codename1.flutter.plugins.gorouter;

import java.util.ArrayList;
import java.util.List;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Widget;
import com.codename1.flutter.navigation.MaterialPageRoute;
import com.codename1.flutter.navigation.Navigator;
import com.codename1.flutter.navigation.Route;
import com.codename1.flutter.navigation.RouteSettings;
import com.codename1.flutter.navigation.RouterConfigSource;

import dart.async.Completer;
import dart.async.Future;
import dart.core.DartMap;
import dart.core.DartUri;
import dart.runtime.Funcs;

/**
 * go_router's {@code GoRouter}, over the Codename One Navigator: a location is matched
 * against the route tree, its redirects are applied and the matched route's builder
 * becomes a pushed page. {@code go} replaces the pushed pages, {@code push} stacks one.
 *
 * Nested routes are matched by joining their paths, but {@code go} to a nested
 * location shows only the deepest page -- the parent pages are not stacked below it,
 * so back from there returns to the app's first page.
 */
public class GoRouter implements RouterConfigSource {
    private static final int DEFAULT_REDIRECT_LIMIT = 5;
    private static GoRouter current;

    private final List<RouteBase> routes = new ArrayList<RouteBase>();
    private String initialLocation = "/";
    private Funcs.Func2<BuildContext, GoRouterState, Object> redirect;
    private Funcs.Func2<BuildContext, GoRouterState, Widget> errorBuilder;
    private int redirectLimit = DEFAULT_REDIRECT_LIMIT;
    /**
     * The error of an asynchronous redirect that failed, consumed by the next routeFor.
     * A failed auth guard must not read as "no redirect" and open the guarded page, so
     * the navigation shows the error page (errorBuilder) instead, as go_router does.
     */
    private Object redirectFailure;
    /** Set by redirectOf when a redirect answered a Future not yet complete. */
    private boolean sawPendingRedirect;
    private GoRouterState state;
    /// Where the base (runApp) route landed after its redirects; null until it is built.
    private String baseLocation;
    /// The location routeFor's last call landed on after redirects.
    private String resolvedLocation;
    private BuildContext appContext;

    public GoRouter() {
        current = this;
    }

    public void routes(Object v) {
        routes.clear();
        if (v instanceof Iterable) {
            for (Object o : (Iterable<?>) v) {
                if (o instanceof RouteBase) {
                    routes.add((RouteBase) o);
                }
            }
        }
    }

    public void initialLocation(String v) {
        this.initialLocation = v == null ? "/" : v;
    }

    public void redirect(Funcs.Func2<BuildContext, GoRouterState, Object> v) {
        this.redirect = v;
    }

    public void errorBuilder(Funcs.Func2<BuildContext, GoRouterState, Widget> v) {
        this.errorBuilder = v;
    }

    public void redirectLimit(long v) {
        this.redirectLimit = (int) v;
    }

    /** Accepted for source compatibility. */
    public void debugLogDiagnostics(boolean v) {
    }

    /** Accepted for source compatibility. */
    public void navigatorKey(Object v) {
    }

    /** Accepted for source compatibility; redirects are evaluated on each navigation. */
    public void refreshListenable(Object v) {
    }

    /** The router of the running app: the one installed last. */
    public static GoRouter of(BuildContext context) {
        return current;
    }

    public static GoRouter maybeOf(BuildContext context) {
        return current;
    }

    /** The state of the location shown last. */
    public GoRouterState state() {
        return state;
    }

    @Override
    public String initialLocation() {
        return initialLocation;
    }

    @Override
    public void attached(BuildContext context) {
        current = this;
        appContext = context;
    }

    @Override
    public Funcs.Func1<RouteSettings, Route> routeGenerator() {
        return new Funcs.Func1<RouteSettings, Route>() {
            @Override
            public Route call(RouteSettings settings) {
                final Object extra = settings.arguments();
                sawPendingRedirect = false;
                Route r = routeFor(settings.name(), extra);
                if (!sawPendingRedirect) {
                    baseLocation = resolvedLocation;
                    return r;
                }
                // The initial route had an asynchronous redirect still running (an auth
                // check reading its session). Mounting what routeFor chose would show the
                // guarded page; instead the page stays empty until the redirect chain
                // resolves, then shows wherever it lands (or the error page if it failed).
                final Completer<String> landed = new Completer<String>();
                whenRedirected(settings.name(), extra, new Funcs.VoidFunc1<String>() {
                    @Override
                    public void call(String to) {
                        baseLocation = to;
                        landed.complete(to);
                    }
                });
                MaterialPageRoute<Object> wait = new MaterialPageRoute<Object>();
                wait.settings(settings);
                wait.builder(new Funcs.Func1<BuildContext, Widget>() {
                    @Override
                    public Widget call(BuildContext ctx) {
                        com.codename1.flutter.widgets.FutureBuilder<String> fb =
                                new com.codename1.flutter.widgets.FutureBuilder<String>();
                        fb.future(landed.future());
                        fb.builder(new Funcs.Func2<BuildContext,
                                com.codename1.flutter.widgets.AsyncSnapshot<String>, Widget>() {
                            @Override
                            public Widget call(BuildContext c,
                                    com.codename1.flutter.widgets.AsyncSnapshot<String> snap) {
                                if (!snap.hasData()) {
                                    return new com.codename1.flutter.widgets.SizedBox();
                                }
                                Route real = routeFor(snap.data(), extra);
                                return ((MaterialPageRoute<?>) real).getBuilder().call(c);
                            }
                        });
                        return fb;
                    }
                });
                return wait;
            }
        };
    }

    // --- navigation ---------------------------------------------------------

    public void go(String location) {
        go(location, null);
    }

    /** Shows {@code location} in place of every pushed page. */
    public void go(String location, final Object extra) {
        while (Navigator.stackSize() > 0) {
            Navigator.pop(null);
        }
        // Redirects run first, even for the initial location: '/' that sent a signed-out
        // user to /login must reach the home page after sign-in, not re-expose the login
        // page the base route still shows. Only a location that lands where the base
        // page already is reuses it (with state updated to this navigation).
        whenRedirected(location, extra, new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String loc) {
                Route r = routeFor(loc, extra);
                if (samePath(resolvedLocation, baseLocation != null ? baseLocation : initialLocation)) {
                    return;
                }
                if (r != null) {
                    Navigator.push(appContext, r);
                }
            }
        });
    }

    public Future<Object> push(String location) {
        return push(location, null);
    }

    /** Pushes {@code location} above the current page. */
    public Future<Object> push(String location, final Object extra) {
        final dart.async.Completer<Object> result = new dart.async.Completer<Object>();
        whenRedirected(location, extra, new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String loc) {
                Route r = routeFor(loc, extra);
                if (r == null) {
                    result.complete(null);
                    return;
                }
                Navigator.push(appContext, r).then(new Funcs.VoidFunc1<Object>() {
                    @Override
                    public void call(Object v) {
                        result.complete(v);
                    }
                }, new Funcs.VoidFunc1<Object>() {
                    @Override
                    public void call(Object e) {
                        result.completeError(e);
                    }
                });
            }
        });
        return result.future();
    }

    /**
     * Runs the redirect chain from {@code location}, waiting for any redirect that
     * answers a Future (an auth check reading its session), then hands the final
     * location to {@code then}. routeFor applies the same redirects again, now all
     * already answered; without the wait an incomplete future read as "no redirect"
     * and a guarded page opened.
     */
    private void whenRedirected(final String location, final Object extra, final Funcs.VoidFunc1<String> then) {
        resolveRedirects(location == null ? "/" : location, extra, 0, then);
    }

    private void resolveRedirects(final String loc, final Object extra, final int depth,
            final Funcs.VoidFunc1<String> then) {
        if (depth > redirectLimit) {
            then.call(loc);
            return;
        }
        Match m = match(loc);
        GoRouterState s = stateOf(loc, m, extra, null);
        List<Funcs.Func2<BuildContext, GoRouterState, Object>> fs =
                new java.util.ArrayList<Funcs.Func2<BuildContext, GoRouterState, Object>>();
        fs.add(redirect);
        if (m != null) {
            for (GoRoute r : m.chain) {
                fs.add(r.redirectFunc());
            }
        }
        nextRedirect(fs, 0, s, loc, extra, depth, then);
    }

    @SuppressWarnings("unchecked")
    private void nextRedirect(final List<Funcs.Func2<BuildContext, GoRouterState, Object>> fs, final int i,
            final GoRouterState s, final String loc, final Object extra, final int depth,
            final Funcs.VoidFunc1<String> then) {
        if (i >= fs.size()) {
            then.call(loc);
            return;
        }
        Funcs.Func2<BuildContext, GoRouterState, Object> f = fs.get(i);
        Object to = f == null ? null : f.call(appContext, s);
        Funcs.VoidFunc1<Object> answered = new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
                if (v instanceof String && !samePath((String) v, loc)) {
                    resolveRedirects((String) v, extra, depth + 1, then);
                } else {
                    nextRedirect(fs, i + 1, s, loc, extra, depth, then);
                }
            }
        };
        if (to instanceof Future) {
            ((Future<Object>) to).then(answered, new Funcs.VoidFunc1<Object>() {
                @Override
                public void call(Object e) {
                    redirectFailure = e == null ? new dart.core.DartException("redirect failed") : e;
                    then.call(loc);
                }
            });
        } else {
            answered.call(to);
        }
    }

    public void pushReplacement(String location, Object extra) {
        if (Navigator.stackSize() > 0) {
            Navigator.pop(null);
        }
        push(location, extra);
    }

    public void replace(String location, Object extra) {
        pushReplacement(location, extra);
    }

    public void pop() {
        Navigator.pop(null, null);
    }

    public void pop(Object result) {
        Navigator.pop(null, result);
    }

    public boolean canPop() {
        // Upstream delegates to its navigator, so an open dialog counts: pop() below
        // dismisses it, and `if (router.canPop()) router.pop()` must reach it.
        return Navigator.canPopAnything();
    }

    public void goNamed(String name, Object pathParameters, Object queryParameters, Object extra) {
        go(namedLocation(name, pathParameters, queryParameters), extra);
    }

    public Future<Object> pushNamed(String name, Object pathParameters, Object queryParameters, Object extra) {
        return push(namedLocation(name, pathParameters, queryParameters), extra);
    }

    /** Builds the location of the route called {@code name}, filling its parameters. */
    public String namedLocation(String name, Object pathParameters, Object queryParameters) {
        String pattern = findNamed(routes, "", name);
        if (pattern == null) {
            throw new dart.core.ArgumentError("no route named " + name);
        }
        StringBuilder sb = new StringBuilder();
        for (String seg : segments(pattern)) {
            sb.append('/');
            if (seg.startsWith(":")) {
                Object v = pathParameters instanceof java.util.Map
                        ? ((java.util.Map<?, ?>) pathParameters).get(seg.substring(1)) : null;
                if (v == null) {
                    throw new dart.core.ArgumentError("missing param \"" + seg.substring(1)
                            + "\" for " + pattern);
                }
                sb.append(encode(String.valueOf(v)));
            } else {
                sb.append(seg);
            }
        }
        if (sb.length() == 0) {
            sb.append('/');
        }
        if (queryParameters instanceof java.util.Map && !((java.util.Map<?, ?>) queryParameters).isEmpty()) {
            char sep = '?';
            for (java.util.Map.Entry<?, ?> e : ((java.util.Map<?, ?>) queryParameters).entrySet()) {
                sb.append(sep).append(encode(String.valueOf(e.getKey())))
                        .append('=').append(encode(String.valueOf(e.getValue())));
                sep = '&';
            }
        }
        return sb.toString();
    }

    // --- matching -----------------------------------------------------------

    /** The route for {@code location}, after redirects; an error page when nothing matches. */
    Route routeFor(String location, Object extra) {
        String loc = location == null ? "/" : location;
        Match m = null;
        for (int i = 0; i <= redirectLimit; i++) {
            m = match(loc);
            GoRouterState s = stateOf(loc, m, extra, null);
            String to = redirectOf(redirect, s);
            if (to == null && m != null && redirectFailure == null) {
                for (GoRoute r : m.chain) {
                    to = redirectOf(r.redirectFunc(), s);
                    if (to != null || redirectFailure != null) {
                        break;
                    }
                }
            }
            if (to == null || samePath(to, loc) || redirectFailure != null) {
                break;
            }
            if (i == redirectLimit) {
                m = null;
                break;
            }
            loc = to;
        }
        Object failure = redirectFailure;
        redirectFailure = null;
        if (failure != null) {
            m = null;
        }
        resolvedLocation = loc;
        final Match match = m;
        final GoRouterState s = stateOf(loc, match, extra, failure != null ? failure
                : match == null ? new dart.core.DartException("no routes for location: " + loc) : null);
        state = s;
        MaterialPageRoute<Object> route = new MaterialPageRoute<Object>();
        RouteSettings settings = new RouteSettings();
        settings.name(loc);
        settings.arguments(extra);
        route.settings(settings);
        route.builder(new Funcs.Func1<BuildContext, Widget>() {
            @Override
            public Widget call(BuildContext ctx) {
                return buildPage(ctx, match, s);
            }
        });
        return route;
    }

    private String redirectOf(Funcs.Func2<BuildContext, GoRouterState, Object> f, GoRouterState s) {
        if (f == null) {
            return null;
        }
        Object to = f.call(appContext, s);
        if (to instanceof Future) {
            // FutureOr<String?>: only an already-completed answer can steer a navigation
            // that is happening now; a pending one is noted for routeGenerator.
            if (!((Future<?>) to).completedNow()) {
                sawPendingRedirect = true;
            }
            Throwable failed = ((Future<?>) to).completedError();
            if (failed != null) {
                // An already-failed redirect (Future.error) is a navigation failure,
                // exactly as one that fails later: routeFor shows the error page rather
                // than building the guarded route as if no redirect existed.
                redirectFailure = failed;
                return null;
            }
            to = ((Future<?>) to).completedValue();
        }
        return to instanceof String ? (String) to : null;
    }

    Widget buildPage(BuildContext ctx, Match m, GoRouterState s) {
        if (m == null) {
            if (errorBuilder != null) {
                return errorBuilder.call(ctx, s);
            }
            return new com.codename1.flutter.widgets.Text(String.valueOf(s.error()));
        }
        Widget page;
        GoRoute leaf = m.chain.get(m.chain.size() - 1);
        if (leaf.builder() != null) {
            page = leaf.builder().call(ctx, s);
        } else if (leaf.pageBuilder() != null) {
            Object p = leaf.pageBuilder().call(ctx, s);
            page = p instanceof GoPage ? ((GoPage) p).child() : p instanceof Widget ? (Widget) p : null;
        } else {
            page = null;
        }
        for (int i = m.shells.size() - 1; i >= 0; i--) {
            ShellRoute sh = m.shells.get(i);
            if (sh.builder() != null) {
                page = sh.builder().call(ctx, s, page);
            }
        }
        return page;
    }

    static final class Match {
        final List<GoRoute> chain = new ArrayList<GoRoute>();
        final List<ShellRoute> shells = new ArrayList<ShellRoute>();
        final DartMap<String, String> params = new DartMap<String, String>();
        String fullPath;
    }

    Match match(String location) {
        DartUri uri = DartUri.parse(location);
        List<String> target = segments(uri.path());
        Match m = new Match();
        return matchIn(routes, new ArrayList<String>(), target, m) ? m : null;
    }

    private static boolean matchIn(List<RouteBase> list, List<String> prefix, List<String> target, Match m) {
        for (RouteBase rb : list) {
            if (rb instanceof ShellRoute) {
                m.shells.add((ShellRoute) rb);
                if (matchIn(rb.routes(), prefix, target, m)) {
                    return true;
                }
                m.shells.remove(m.shells.size() - 1);
                continue;
            }
            if (!(rb instanceof GoRoute)) {
                continue;
            }
            GoRoute r = (GoRoute) rb;
            String p = r.path() == null ? "" : r.path();
            List<String> full = p.startsWith("/") ? segments(p) : join(prefix, segments(p));
            DartMap<String, String> captured = new DartMap<String, String>();
            if (!prefixMatches(full, target, captured)) {
                continue;
            }
            m.chain.add(r);
            if (full.size() == target.size()) {
                m.params.putAll(captured);
                m.fullPath = "/" + joinPath(full);
                return true;
            }
            if (matchIn(r.routes(), full, target, m)) {
                m.params.putAll(captured);
                return true;
            }
            m.chain.remove(m.chain.size() - 1);
        }
        return false;
    }

    private static boolean prefixMatches(List<String> pattern, List<String> target, DartMap<String, String> captured) {
        if (pattern.size() > target.size()) {
            return false;
        }
        for (int i = 0; i < pattern.size(); i++) {
            String p = pattern.get(i);
            String t = target.get(i);
            if (p.startsWith(":")) {
                captured.put(p.substring(1), DartUri.decodeComponent(t));
            } else if (!p.equals(t)) {  // go_router literals are case-sensitive by default
                return false;
            }
        }
        return true;
    }

    private static String findNamed(List<RouteBase> list, String prefix, String name) {
        for (RouteBase rb : list) {
            String here = prefix;
            if (rb instanceof GoRoute) {
                GoRoute r = (GoRoute) rb;
                String p = r.path() == null ? "" : r.path();
                here = p.startsWith("/") ? p : (prefix.endsWith("/") ? prefix : prefix + "/") + p;
                if (name.equals(r.name())) {
                    return here;
                }
            }
            String found = findNamed(rb.routes(), here, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private GoRouterState stateOf(String loc, Match m, Object extra, Object error) {
        DartUri uri = DartUri.parse(loc);
        DartMap<String, String> params = m == null ? new DartMap<String, String>() : m.params;
        String fullPath = m == null ? null : m.fullPath;
        String name = m == null || m.chain.isEmpty() ? null : m.chain.get(m.chain.size() - 1).name();
        return new GoRouterState(uri, uri.path(), fullPath, name, params, extra, error);
    }

    /** Percent-encodes everything but RFC 3986's unreserved characters, as UTF-8. */
    static String encode(String s) {
        StringBuilder sb = new StringBuilder();
        byte[] bytes;
        try {
            bytes = s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            bytes = s.getBytes();
        }
        for (byte b : bytes) {
            int c = b & 0xff;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~') {
                sb.append((char) c);
            } else {
                sb.append('%').append("0123456789ABCDEF".charAt(c >> 4))
                        .append("0123456789ABCDEF".charAt(c & 15));
            }
        }
        return sb.toString();
    }

    static List<String> segments(String path) {
        List<String> out = new ArrayList<String>();
        if (path == null) {
            return out;
        }
        int start = 0;
        int n = path.length();
        for (int i = 0; i <= n; i++) {
            if (i == n || path.charAt(i) == '/') {
                if (i > start) {
                    out.add(path.substring(start, i));
                }
                start = i + 1;
            }
        }
        return out;
    }

    private static List<String> join(List<String> a, List<String> b) {
        List<String> out = new ArrayList<String>(a);
        out.addAll(b);
        return out;
    }

    private static String joinPath(List<String> segs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segs.size(); i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(segs.get(i));
        }
        return sb.toString();
    }

    private static boolean samePath(String a, String b) {
        return segments(DartUri.parse(a).path()).equals(segments(DartUri.parse(b).path()))
                && DartUri.parse(a).query().equals(DartUri.parse(b).query());
    }
}
