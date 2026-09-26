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

import com.codename1.flutter.Brightness;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Element;
import com.codename1.flutter.Locale;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.ThemeMode;
import com.codename1.flutter.Widget;
import com.codename1.flutter.navigation.MaterialPageRoute;
import com.codename1.flutter.navigation.Route;
import com.codename1.flutter.navigation.RouteSettings;
import com.codename1.flutter.services.SystemUiOverlayStyle;
import com.codename1.flutter.widgets.ScrollBehavior;
import com.codename1.ui.Display;

import dart.core.DartIterable;
import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * The material application shell. Renders its {@code home} as its only
 * child, provides the theme that {@link Theme#of} resolves by walking up the
 * element tree, and (M4) selects the EFFECTIVE theme from
 * {@code theme}/{@code darkTheme} per {@code themeMode} — installing it into
 * the CN1 UIManager via {@link ThemeDataAdapter} when the app mounts and
 * whenever the effective theme changes across rebuilds (see
 * {@link MaterialAppElement}).
 */
public class MaterialApp extends StatelessWidget
        implements com.codename1.flutter.navigation.Navigator.RouteTableHost {

    /** This app's own route table — see {@link #build}. */
    private com.codename1.flutter.navigation.Navigator.RouteTable routeTable;

    @Override
    public void routeTable(com.codename1.flutter.navigation.Navigator.RouteTable table) {
        this.routeTable = table;
    }

    @Override
    public com.codename1.flutter.navigation.Navigator.RouteTable routeTable() {
        return routeTable;
    }

    private String title;
    private ThemeData theme;
    private ThemeData darkTheme;
    private ThemeMode themeMode;
    private Widget home;
    private Object routes;
    private String initialRoute;
    private String restorationScopeId;
    private boolean debugShowCheckedModeBanner = true;
    private boolean resizeToAvoidBottomInset = true;
    private Object localizationsDelegates;
    private Object supportedLocales;
    private Locale locale;
    private SystemUiOverlayStyle systemOverlayStyle;
    private Funcs.Func1<RouteSettings, Route> onGenerateRoute;
    private Funcs.Func1<RouteSettings, Route> onUnknownRoute;
    private ScrollBehavior scrollBehavior;
    private Funcs.Func2<DartList<Locale>, DartIterable<Locale>, Locale> localeListResolutionCallback;

    public void title(String v) {
        this.title = v;
    }

    /** The key for the app's root Navigator — Flutter's {@code navigatorKey}. */
    public void navigatorKey(Object v) {
    }

    public void theme(ThemeData v) {
        this.theme = v;
    }

    public void darkTheme(ThemeData v) {
        this.darkTheme = v;
    }

    public void themeMode(ThemeMode v) {
        this.themeMode = v;
    }

    public void home(Widget v) {
        this.home = v;
    }

    /**
     * The app's named-route table — Flutter's {@code MaterialApp.routes} (a
     * {@code Map<String, WidgetBuilder>}). Held untyped; the Navigator resolves
     * a pushed route name against it.
     */
    public void routes(Object v) {
        this.routes = v;
    }

    /** The name of the first route shown — Flutter's {@code initialRoute}. */
    public void initialRoute(String v) {
        this.initialRoute = v;
    }

    /**
     * The identifier under which this app's state is saved and restored —
     * Flutter's {@code restorationScopeId}.
     */
    public void restorationScopeId(String v) {
        this.restorationScopeId = v;
    }

    /** Whether the debug "DEBUG" banner shows — Flutter's flag of the same name. */
    public void debugShowCheckedModeBanner(boolean v) {
        this.debugShowCheckedModeBanner = v;
    }

    /**
     * Whether the body resizes when the on-screen keyboard appears — Flutter's
     * {@code resizeToAvoidBottomInset} (mirrored on MaterialApp for apps that
     * set it app-wide).
     */
    public void resizeToAvoidBottomInset(boolean v) {
        this.resizeToAvoidBottomInset = v;
    }

    /**
     * The app's localizations delegates — Flutter's {@code localizationsDelegates}
     * (an {@code Iterable<LocalizationsDelegate>}). Held untyped.
     */
    public void localizationsDelegates(Object v) {
        this.localizationsDelegates = v;
    }

    /**
     * The locales this app declares support for — Flutter's
     * {@code supportedLocales} (an {@code Iterable<Locale>}). Held untyped.
     */
    public void supportedLocales(Object v) {
        this.supportedLocales = v;
    }

    /** Forces a specific locale, overriding the device locale — Flutter's {@code locale}. */
    public void locale(Locale v) {
        this.locale = v;
    }

    /**
     * The overlay style (status/navigation bar) applied app-wide — Flutter's
     * {@code SystemUiOverlayStyle}.
     */
    public void systemOverlayStyle(SystemUiOverlayStyle v) {
        this.systemOverlayStyle = v;
    }

    /**
     * A callback that builds a route for a name not found in {@link #routes} —
     * Flutter's {@code onGenerateRoute} ({@code RouteFactory}).
     */
    public void onGenerateRoute(Funcs.Func1<RouteSettings, Route> v) {
        this.onGenerateRoute = v;
    }

    /**
     * The last-resort route factory for a name neither {@link #routes} nor
     * {@link #onGenerateRoute} could build — Flutter's {@code onUnknownRoute}.
     */
    public void onUnknownRoute(Funcs.Func1<RouteSettings, Route> v) {
        this.onUnknownRoute = v;
    }

    /** The app-wide scroll behavior — Flutter's {@code scrollBehavior}. */
    public void scrollBehavior(ScrollBehavior v) {
        this.scrollBehavior = v;
    }

    /**
     * Resolves the device's preferred locale list against the supported locales
     * — Flutter's {@code localeListResolutionCallback}.
     */
    public void localeListResolutionCallback(Funcs.Func2<DartList<Locale>, DartIterable<Locale>, Locale> v) {
        this.localeListResolutionCallback = v;
    }

    public Object getRoutes() {
        return routes;
    }

    public String getInitialRoute() {
        return initialRoute;
    }

    public String getRestorationScopeId() {
        return restorationScopeId;
    }

    public boolean isDebugShowCheckedModeBanner() {
        return debugShowCheckedModeBanner;
    }

    public boolean isResizeToAvoidBottomInset() {
        return resizeToAvoidBottomInset;
    }

    public Object getLocalizationsDelegates() {
        return localizationsDelegates;
    }

    public Object getSupportedLocales() {
        return supportedLocales;
    }

    public Locale getLocale() {
        return locale;
    }

    public SystemUiOverlayStyle getSystemOverlayStyle() {
        return systemOverlayStyle;
    }

    public String getTitle() {
        return title;
    }

    public ThemeData getTheme() {
        return theme;
    }

    public ThemeData getDarkTheme() {
        return darkTheme;
    }

    public ThemeMode getThemeMode() {
        return themeMode;
    }

    public Widget getHome() {
        return home;
    }

    /**
     * The theme this app is actually showing right now: {@code darkTheme}
     * when dark is in effect (per {@link #wantsDark}) and one was provided,
     * else {@code theme} (matching Flutter's fallback to {@code theme} when
     * {@code darkTheme} is absent). With neither set, a default ThemeData is
     * returned whose brightness follows the dark request.
     */
    public ThemeData effectiveTheme() {
        boolean dark = wantsDark(themeMode, platformDark());
        ThemeData t = (dark && darkTheme != null) ? darkTheme : theme;
        if (t == null) {
            t = new ThemeData();
            if (dark) {
                t.brightness(Brightness.dark);
            }
        }
        return t;
    }

    /**
     * The themeMode decision table (pure — headless-testable): {@code dark}
     * and {@code light} are absolute; {@code system} (or null, its default)
     * follows the platform flag, treating null/unknown as light.
     */
    public static boolean wantsDark(ThemeMode mode, Boolean platformDark) {
        if (mode == ThemeMode.dark) {
            return true;
        }
        if (mode == ThemeMode.light) {
            return false;
        }
        return Boolean.TRUE.equals(platformDark);
    }

    /**
     * The platform dark-mode flag from the CN1 Display, or null when no
     * Display exists (headless) or the port can't report it.
     */
    public static Boolean platformDark() {
        try {
            if (Display.isInitialized()) {
                return Display.getInstance().isDarkMode();
            }
        } catch (Throwable ignore) {
            // headless or unsupported port
        }
        return null;
    }

    @Override
    public Widget build(BuildContext context) {
        // Publish the app's route table so Navigator.pushNamed(...) from anywhere
        // below can resolve a name the same way this build does. The table belongs
        // to THIS app: a study is a MaterialApp of its own, and one global table
        // meant opening a study permanently replaced the gallery's.
        boolean rootApp = com.codename1.flutter.navigation.Navigator.installRouteTable(
                context, routes, onGenerateRoute, onUnknownRoute);

        Widget content = home;
        // A routing-based app (no home widget) renders its initial route — Flutter
        // calls onGenerateRoute with the initialRoute (default "/") and mounts the
        // resulting route's page. new_gallery relies on this entirely.
        //
        // Resolved ONCE per element, not per build. An app-wide model above MaterialApp
        // rebuilds it whenever a setting changes, and re-running the route builder each
        // time hands back a brand new page, throwing away what the user was looking at.
        // Flutter does not re-resolve either: its route stack is Navigator state, not
        // something recomputed from the widget on every build.
        if (content == null) {
            MaterialAppElement self = context instanceof MaterialAppElement
                    ? (MaterialAppElement) context : null;
            content = self == null ? null : self.routeContent();
            if (content == null) {
                Route route = com.codename1.flutter.navigation.Navigator.resolveRoute(
                        context, initialRoute != null ? initialRoute : "/", null);
                if (route instanceof MaterialPageRoute) {
                    Funcs.Func1<BuildContext, Widget> b =
                            ((MaterialPageRoute) route).getBuilder();
                    if (b != null) {
                        content = b.call(context);
                    }
                }
                if (self != null) {
                    self.routeContent(content);
                }
            }
        }
        // Below the localizations scope, exactly where Flutter puts the app's Navigator.
        // A push that arrives from outside the widget tree - a deep link, a notification
        // tap, a test harness - inherits from here, so it sees the same Theme,
        // MediaQuery, Localizations and providers a push from a widget would.
        // The app's theme is INSTALLED as a widget, exactly as Flutter installs
        // one below WidgetsApp. Without it `Theme.of` found no Theme ancestor,
        // fell through to `findAncestorWidgetOfExactType(MaterialApp)` -- a walk
        // to the root -- and reported a missing ancestor on the way. Every themed
        // widget calls Theme.of on every build, so the intended hash lookup was
        // never actually taken, and the diagnostic budget for genuinely missing
        // providers was spent on this one false alarm.
        return wrapWithTheme(wrapWithLocalizations(
                new com.codename1.flutter.navigation.Navigator.RootScope(content, rootApp)));
    }

    /**
     * Publishes {@link #effectiveTheme()} to the subtree as a real Theme
     * widget, together with the two ambient defaults Flutter installs
     * alongside it: the icon theme and the default text style.
     *
     * <p>Neither was present, so {@code IconTheme.of} and
     * {@code DefaultTextStyle.of} found nothing above them anywhere in the app
     * and each fell back to a walk plus a synthesised default -- on every Icon
     * and every Text, on every build. Installing them is both the faithful
     * shape and the one that makes those lookups a hash hit.</p>
     */
    private Widget wrapWithTheme(Widget content) {
        if (content == null) {
            return null;
        }
        ThemeData data = effectiveTheme();

        com.codename1.flutter.widgets.DefaultTextStyle text =
                new com.codename1.flutter.widgets.DefaultTextStyle();
        com.codename1.flutter.TextStyle body = null;
        try {
            body = data.textTheme() == null ? null : data.textTheme().bodyMedium();
        } catch (Throwable ignore) {
            body = null;
        }
        if (body != null) {
            text.style(body);
        }
        text.child(content);

        IconTheme icons = new IconTheme();
        icons.data(data.iconTheme());
        icons.child(text);

        Theme t = new Theme();
        t.data(data);
        t.child(icons);
        // No MediaQuery is installed here, deliberately. Flutter's
        // MediaQuery.fromView is fed by a view whose metrics are already known;
        // ours would have to snapshot the Display during the app's FIRST build,
        // which happens before the Form is showing and therefore before the
        // safe-area insets exist. Every descendant then inherited a zero top
        // inset and the whole app rode 44dp too high. MediaQuery.of resolves
        // against the Display instead, and MediaQueryData caches that.
        return t;
    }

    /**
     * Publishes the localized resources loaded from {@link #localizationsDelegates}
     * so {@code Foo.of(context)} lookups below resolve. Flutter installs a
     * Localizations widget above the app content; this mirrors that with a single
     * {@link LocalizationsScope} carrying every delegate's synchronously-loaded value.
     */
    private Widget wrapWithLocalizations(Widget content) {
        if (content == null) {
            return null;
        }
        // The scope is installed UNCONDITIONALLY and loads its resources on
        // first lookup rather than here. Loading eagerly during build reads
        // the app's delegate list at the earliest possible moment — before,
        // on a lazily-initialised backend, the class holding it has
        // necessarily run its static initialiser — and an empty result then
        // meant no scope at all, so every `Foo.of(context)!` below died with
        // no clue why. Deferring makes the lookup ask when the answer is
        // knowable.
        LocalizationsScope scope = new LocalizationsScope(new Funcs.Func0<java.util.List<Object>>() {
            @Override
            public java.util.List<Object> call() {
                return loadLocalizations();
            }
        });
        scope.child(content);
        return scope;
    }

    private java.util.List<Object> loadLocalizations() {
        java.util.List<Object> out = new java.util.ArrayList<Object>();
        if (localizationsDelegates instanceof Iterable) {
            Locale loc = effectiveLocale();
            logResolvedLocale(loc);
            for (Object d : (Iterable<?>) localizationsDelegates) {
                if (d instanceof com.codename1.flutter.l10n.LocalizationsDelegate) {
                    try {
                        dart.async.Future<?> f =
                                ((com.codename1.flutter.l10n.LocalizationsDelegate<?>) d).load(loc);
                        Object v = f != null ? f.getNow() : null;
                        if (v != null) {
                            out.add(v);
                            logLoaded(d, v);
                        } else {
                            logLoaded(d, null);
                        }
                    } catch (Throwable err) {
                        // A delegate that fails contributes nothing, and the app
                        // then dies on `Foo.of(context)!` far away — so say which
                        // one failed rather than swallowing it.
                        try {
                            com.codename1.io.Log.p("Flutter runtime: localizations delegate "
                                    + d.getClass().getName() + " failed for locale "
                                    + loc + ": " + err);
                        } catch (Throwable ignore) {
                            // headless: Log has no storage backend
                        }
                    }
                }
            }
        }
        return out;
    }

    /**
     * Records which locale the app's localizations were resolved for. A
     * delegate that has no table for that locale returns nothing, and the app
     * then dies on {@code Foo.of(context)!} — so the locale actually used is
     * the first thing worth knowing.
     */
    private static boolean loggedLocale;

    /** What each delegate actually produced — the list is what every lookup searches. */
    /**
     * Localisation tracing, GATED. These ran on every app's startup path, and a
     * Log.p is not free -- on a device it is file IO, and six of them landed
     * inside the first frame. They answer a question ("which delegate produced
     * the resources this app is using?") worth keeping, just not worth paying
     * for when nobody asked it.
     */
    private static void logLoaded(Object delegate, Object value) {
        if (!com.codename1.flutter.Trace.on()) {
            return;
        }
        try {
            com.codename1.io.Log.p("Flutter runtime:   delegate "
                    + delegate.getClass().getName() + " -> "
                    + (value == null ? "null" : value.getClass().getName()));
        } catch (Throwable ignore) {
            // headless: Log has no storage backend
        }
    }

    private void logResolvedLocale(Locale loc) {
        if (loggedLocale || !com.codename1.flutter.Trace.on()) {
            return;
        }
        loggedLocale = true;
        try {
            com.codename1.io.Log.p("Flutter runtime: resolving localizations for locale "
                    + (loc == null ? "null" : loc.languageCode() + "_" + loc.countryCode()));
        } catch (Throwable ignore) {
            // headless: Log has no storage backend
        }
    }

    /**
     * The locale the app runs in — Flutter's resolution order: an explicit {@code locale}
     * wins, otherwise the app's {@code localeListResolutionCallback} is asked to choose
     * from the device's locales, otherwise the first supported locale.
     *
     * <p>Asking the callback is not optional politeness. It is where an app learns what the
     * device asked for: the gallery's records the device locale in a global that its own
     * {@code GalleryOptions.locale} falls back to, and everything derived from that — the
     * text direction, most obviously — is null until the callback has run. Skipping it left
     * {@code resolvedTextDirection()} returning null, and the settings icon's tap handler
     * asserts that value is non-null, so opening settings threw.</p>
     */
    private Locale effectiveLocale() {
        if (locale != null) {
            return locale;
        }
        Locale chosen = askResolutionCallback();
        if (chosen != null) {
            return chosen;
        }
        if (supportedLocales instanceof Iterable) {
            for (Object l : (Iterable<?>) supportedLocales) {
                if (l instanceof Locale) {
                    return (Locale) l;
                }
            }
        }
        return new Locale("en", null);
    }

    /// Runs the app's locale-resolution callback once, over the device's locales.
    private Locale askResolutionCallback() {
        if (localeListResolutionCallback == null || resolutionAsked) {
            return resolvedByCallback;
        }
        resolutionAsked = true;
        try {
            DartList<Locale> device = deviceLocales();
            DartIterable<Locale> supported = supportedLocaleList();
            resolvedByCallback = localeListResolutionCallback.call(device, supported);
        } catch (Throwable t) {
            com.codename1.flutter.FlutterErrorReport.record(t);
        }
        return resolvedByCallback;
    }

    private boolean resolutionAsked;
    private Locale resolvedByCallback;

    /// What the platform reports, as Flutter's ordered preference list.
    private DartList<Locale> deviceLocales() {
        DartList<Locale> out = new DartList<Locale>();
        try {
            String lang = com.codename1.l10n.L10NManager.getInstance().getLanguage();
            if (lang != null && lang.length() > 0) {
                out.add(new Locale(lang, null));
            }
        } catch (Throwable ignore) {
            // headless
        }
        if (out.isEmpty()) {
            out.add(new Locale("en", null));
        }
        return out;
    }

    private DartIterable<Locale> supportedLocaleList() {
        DartList<Locale> out = new DartList<Locale>();
        if (supportedLocales instanceof Iterable) {
            for (Object l : (Iterable<?>) supportedLocales) {
                if (l instanceof Locale) {
                    out.add((Locale) l);
                }
            }
        }
        return DartIterable.wrap(out);
    }

    @Override
    public Element createElement() {
        return new MaterialAppElement(this);
    }
}
