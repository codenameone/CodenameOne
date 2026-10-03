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
package com.codename1.flutter;

import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects the errors a Flutter tree throws, keeps the app running, and reports them
 * in a form you can act on.
 *
 * <p>Without this, one failing screen ends the session: an uncaught error on the EDT
 * raises Codename One's modal error dialog, which blocks the EDT, so every screen after
 * it stalls too. During development that turns "this screen is broken" into "the app is
 * dead", and it hides how many OTHER screens would have worked.</p>
 *
 * <p>What makes a report usable is the context, not the stack: transpiled build methods
 * are inlined into the framework's frame on some backends, so a trace often names
 * nothing but {@code Element.updateChild} repeated. Each error is therefore recorded
 * with the widget that was building and the route that was on screen, and identical
 * errors are counted rather than repeated — a rebuild loop would otherwise bury
 * everything else.</p>
 *
 * <p>Enable with {@link #install()}; {@link #summary()} returns the inventory, which is
 * what a sweep over an app's screens should be judged by.</p>
 */
public final class FlutterErrorReport {

    /// One distinct failure and how often it happened.
    public static final class Entry {
        private final String type;
        private final String message;
        private final String building;
        private final String route;
        private final String origin;
        private int count;

        Entry(String type, String message, String building, String route, String origin) {
            this.type = type;
            this.message = message;
            this.building = building;
            this.route = route;
            this.origin = origin;
            this.count = 1;
        }

        /// The first few frames of the throw site, or null when the platform has none.
        ///
        /// "Null check operator used on a null value" names no widget and no line on its
        /// own, and transpiled code has plenty of `!` in it; without this, finding one
        /// means bisecting the tree by hand.
        public String origin() {
            return origin;
        }

        public String type() {
            return type;
        }

        public String message() {
            return message;
        }

        /// The widget that was building when this happened, or null.
        public String building() {
            return building;
        }

        /// The route that was on screen, or null.
        public String route() {
            return route;
        }

        public int count() {
            return count;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(count).append("x ").append(type);
            if (message != null) {
                sb.append(": ").append(message);
            }
            if (building != null) {
                sb.append("  [while ").append(building).append("]");
            }
            if (route != null) {
                sb.append("  [route ").append(route).append("]");
            }
            if (origin != null) {
                sb.append("  [at ").append(origin).append("]");
            }
            return sb.toString();
        }
    }

    private static final Map<String, Entry> ENTRIES = new HashMap<String, Entry>();
    private static final List<Entry> ORDER = new ArrayList<Entry>();
    private static boolean installed;
    private static String currentRoute;

    private FlutterErrorReport() {
    }

    /**
     * Starts collecting. Errors are logged and swallowed instead of stopping the app.
     *
     * <p>Call this from a debug or test build. A shipping app usually wants the opposite —
     * the default dialog, or its own crash reporting — so this is opt-in rather than
     * something {@code runApp} does for you.</p>
     */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        Display.getInstance().addEdtErrorHandler(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                record(evt.getSource());
                // Consume: the default handling is a modal dialog, which would block the
                // EDT and take every later screen down with this one.
                evt.consume();
            }
        });
    }

    /** Whether collecting is active. */
    public static synchronized boolean isInstalled() {
        return installed;
    }

    /// The first frames of a throwable's own stack, trimmed to the generated and runtime
    /// code that actually matters. Best effort: a platform that reports no frames simply
    /// yields null rather than failing the report.
    private static String originOf(Object error) {
        if (!(error instanceof Throwable)) {
            return null;
        }
        try {
            StackTraceElement[] frames = ((Throwable) error).getStackTrace();
            if (frames == null || frames.length == 0) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            int shown = 0;
            for (int i = 0; i < frames.length && shown < 4; i++) {
                String cn = frames[i].getClassName();
                if (cn.startsWith("dart.runtime.DartRuntime")) {
                    continue;   // the thrower itself, never the answer
                }
                if (sb.length() > 0) {
                    sb.append(" <- ");
                }
                sb.append(cn).append('.').append(frames[i].getMethodName());
                if (frames[i].getLineNumber() > 0) {
                    sb.append(':').append(frames[i].getLineNumber());
                }
                shown++;
            }
            return sb.length() == 0 ? null : sb.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Records the route now on screen, so later errors can name where they happened. */
    public static synchronized void route(String name) {
        currentRoute = name;
    }

    /**
     * Records one failure. Public so a caller that catches an error itself — a route
     * mount, a painter — can report it with the same context and de-duplication.
     */
    public static synchronized void record(Object error) {
        String type = error == null ? "unknown" : error.getClass().getName();
        String message = error instanceof Throwable ? ((Throwable) error).getMessage()
                : (error == null ? null : String.valueOf(error));
        String building = dart.runtime.DartRuntime.diagnosticContext();
        String origin = originOf(error);
        String key = type + "|" + message + "|" + building + "|" + currentRoute;
        Entry existing = ENTRIES.get(key);
        if (existing != null) {
            existing.count++;
            return;   // already reported once; counting is enough
        }
        Entry entry = new Entry(type, message, building, currentRoute, origin);
        ENTRIES.put(key, entry);
        ORDER.add(entry);
        try {
            Log.p("Flutter error: " + entry);
        } catch (Throwable ignored) {
            // headless: Log has no storage backend
        }
    }

    /**
     * Records a route name that resolved to nothing.
     *
     * <p>This used to be a log line, and a screen that never opened therefore read
     * as a screen that opened cleanly: a sweep that walks every route and asks for
     * the error inventory said "0 routes reported something" while five of the six
     * studies had silently not opened at all, leaving whatever was already showing
     * on screen to be screenshotted in their place.</p>
     *
     * @param name   the route that was asked for
     * @param detail what went wrong, or null for "nothing claimed the name"
     */
    public static synchronized void noRoute(String name, String detail) {
        String message = "no route for '" + name + "'" + (detail == null ? "" : " " + detail);
        String key = "no-route|" + message;
        Entry existing = ENTRIES.get(key);
        if (existing != null) {
            existing.count++;
            return;
        }
        Entry entry = new Entry("no-route", message,
                dart.runtime.DartRuntime.diagnosticContext(), name, null);
        ENTRIES.put(key, entry);
        ORDER.add(entry);
        try {
            Log.p("Flutter error: " + entry);
        } catch (Throwable ignored) {
            // headless: Log has no storage backend
        }
    }

    /**
     * Records that a widget rendered without its intended effect — a stub that passes
     * its child through, or draws nothing at all.
     *
     * <p>These are the failures that are hardest to find, because nothing throws: the
     * screen simply comes up empty or subtly wrong, and a sweep reports "no errors".
     * Reporting them turns a blank screen into a list of what it needed and did not
     * get.</p>
     *
     * @param widget the widget that is not fully implemented
     * @param effect what is missing, phrased so it reads as a gap ("scale and rotation
     *               are ignored")
     */
    public static synchronized void unimplemented(String widget, String effect) {
        String key = "unimplemented|" + widget + "|" + effect + "|" + currentRoute;
        Entry existing = ENTRIES.get(key);
        if (existing != null) {
            existing.count++;
            return;
        }
        // No origin: an unimplemented report names its own widget already.
        Entry entry = new Entry("unimplemented", widget + ": " + effect,
                dart.runtime.DartRuntime.diagnosticContext(), currentRoute, null);
        ENTRIES.put(key, entry);
        ORDER.add(entry);
        try {
            Log.p("Flutter gap: " + entry);
        } catch (Throwable ignored) {
            // headless: Log has no storage backend
        }
    }

    /** Every distinct failure, in the order first seen. */
    public static synchronized List<Entry> entries() {
        return new ArrayList<Entry>(ORDER);
    }

    /** Distinct failures, and the total including repeats. */
    public static synchronized String summary() {
        int total = 0;
        for (Entry e : ORDER) {
            total += e.count;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(ORDER.size()).append(" distinct error(s), ").append(total).append(" total");
        for (Entry e : ORDER) {
            sb.append('\n').append("  ").append(e);
        }
        return sb.toString();
    }

    /** Forgets everything collected so far — lets a sweep measure one screen at a time. */
    public static synchronized void reset() {
        ENTRIES.clear();
        ORDER.clear();
    }
}
