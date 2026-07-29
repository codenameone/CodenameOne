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
package com.codename1.surfaces;

import com.codename1.io.Log;
import com.codename1.ui.Display;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/// Simulator-only guard rails for the surfaces API.
///
/// The surfaces wire format is deliberately cheap in the simulator: publishing serializes to JSON,
/// encodes PNG blobs through Java2D and writes them to the local filesystem, all in microseconds.
/// The same code on a device writes into a shared app-group container, hands the payload to
/// WidgetKit or ActivityKit over IPC and, when an image still has to be rasterized, blocks on the
/// platform UI thread to read pixels back off the GPU. Mistakes that are invisible in the
/// simulator therefore surface as an unresponsive app on hardware, which is the worst possible
/// place to discover them.
///
/// These checks close that gap. They run ONLY in the simulator (or when a test forces them on with
/// [Surfaces#setDiagnosticsEnabled(Boolean)]), so they cost nothing in a shipped build. Conditions
/// that are certain to misbehave on a device throw [IllegalStateException] with a description of
/// the fix; conditions that merely degrade log a one-time warning.
///
/// @see Surfaces#setDiagnosticsEnabled(Boolean)
final class SurfaceDiagnostics {
    /// Rolling window for the republish rate warning.
    private static final long RATE_WINDOW_MILLIS = 60000L;

    /// Republishes of one kind (or updates of one activity) per window before warning. WidgetKit
    /// gives an app roughly 40-70 reloads per day per widget, so a steady stream this dense is
    /// always a bug rather than a tight but legitimate loop.
    private static final int RATE_LIMIT = 20;

    /// null = follow the platform (simulator on, everything else off), non-null = forced.
    private static Boolean override;

    /// null = ask the real Display, non-null = forced (tests only, see [#setEdtForTests(Boolean)]).
    private static Boolean edtForTests;

    private static final Set<String> warnedOnce = new HashSet<String>();

    /// key -> {window start millis, count in window}
    private static final Map<String, long[]> rateWindows = new HashMap<String, long[]>();

    private SurfaceDiagnostics() {
    }

    static void setEnabled(Boolean value) {
        override = value;
        clearCaches();
    }

    static boolean enabled() {
        if (override != null) {
            return override.booleanValue();
        }
        // Display may not be initialized at all (unit tests, a static initializer racing init):
        // absence of a platform is not a diagnostics failure, it just means there is nothing to
        // check against.
        try {
            return Display.isInitialized() && Display.getInstance().isSimulator();
        } catch (Throwable t) {
            return false;
        }
    }

    static void reset() {
        override = null;
        edtForTests = null;
        clearCaches();
    }

    private static void clearCaches() {
        synchronized (warnedOnce) {
            warnedOnce.clear();
        }
        synchronized (rateWindows) {
            rateWindows.clear();
        }
    }

    /// Fails when an image that still has to be rasterized is serialized on the EDT. Called only
    /// for images that are not already `EncodedImage`s.
    ///
    /// `SurfaceSerializer` ships an `EncodedImage` by handing over its existing PNG bytes, but any
    /// other `Image` has to go through `ImageIO.save`. On iOS that native call drains the render
    /// queue and does a `dispatch_sync` onto the main thread to read the pixels back, so calling it
    /// from the EDT stalls the UI thread on the platform UI thread for as long as the readback
    /// takes. In the simulator the same call is a Java2D encode that returns immediately, which is
    /// exactly why this only ever reproduces on hardware.
    static void beforeRasterizingImageEncode() {
        if (!enabled() || !isEdt()) {
            return;
        }
        throw new IllegalStateException("Surfaces: a surface image is being encoded on the EDT. "
                + "SurfaceImage was given a com.codename1.ui.Image that is not an EncodedImage, so "
                + "publishing has to rasterize it to PNG. On a device (iOS in particular) that "
                + "encode blocks the calling thread on the platform UI thread while the pixels are "
                + "read back off the GPU, so doing it on the EDT freezes the app even though the "
                + "simulator handles it instantly. Fix it either way: publish off the EDT, or hand "
                + "SurfaceImage an EncodedImage, which ships the PNG bytes with no native work at "
                + "all. For bundled art that is EncodedImage.create(\"/icon.png\"). For generated "
                + "art, convert ONCE with EncodedImage.createFromImage(img, false) and cache the "
                + "result - and run that conversion off the EDT (inside invokeAndBlock or on a "
                + "background thread), because it performs this very same encode: converting here "
                + "on the EDT would pay the stall this check is stopping and hide it from the "
                + "check. This check runs only in the simulator; see "
                + "Surfaces.setDiagnosticsEnabled(Boolean).");
    }

    /// Fails when a timeline is published for a kind that was never registered.
    ///
    /// A widget kind has to be declared twice: at build time in `surfaces.json` (the native widget
    /// galleries are compiled into the app) and at runtime with
    /// [Surfaces#registerWidgetKind(WidgetKind)]. Publishing to an unregistered id silently
    /// produces a timeline no renderer will ever pick up, and on a device that looks like "the
    /// widget never appears" with nothing in the log.
    static void requireRegisteredKind(String kindId) {
        if (!enabled() || Surfaces.isKindRegistered(kindId)) {
            return;
        }
        throw new IllegalStateException("Surfaces: publish(\"" + kindId + "\", ...) was called but "
                + "no widget kind with that id is registered, so nothing will render it. Call "
                + "Surfaces.registerWidgetKind(new WidgetKind(\"" + kindId + "\")...) once, "
                + "typically from init(), and declare the same id in the project's surfaces.json "
                + "so the native widget gallery is built for it. Registered kinds: "
                + describeRegisteredKinds() + ". This check runs only in the simulator; see "
                + "Surfaces.setDiagnosticsEnabled(Boolean).");
    }

    /// Warns once per API when a publishing call is made on the EDT.
    ///
    /// Unlike [#beforeRasterizingImageEncode()] this is not certain to hang: the surfaces API is
    /// documented as callable from any thread and a single publish of an already-encoded payload
    /// is quick. It is still the wrong thread. On a device the call writes JSON and PNG blobs into
    /// the shared container and makes a native round trip (`Activity.request` is a synchronous XPC
    /// hop on iOS), none of which the simulator's local filesystem makes you pay for.
    static void offEdtPreferred(String api) {
        if (!enabled() || !isEdt()) {
            return;
        }
        warnOnce("edt:" + api, api + " was called on the EDT. Surface publishing is data only and "
                + "is callable from any thread; on a device it writes the payload into the shared "
                + "container and makes a synchronous native call, so running it on the EDT stalls "
                + "the UI for as long as that takes. Move it to a background thread - there is no "
                + "reason to wrap these calls in callSerially(). This warning appears only in the "
                + "simulator.");
    }

    /// Warns when one kind or activity is republished far too often.
    ///
    /// Both WidgetKit and the Android app-widget host throttle reloads against a daily budget, so
    /// a republish-per-tick loop does not merely waste work: once the budget is gone the surface
    /// stops updating for the rest of the day, on the device only.
    ///
    /// #### Parameters
    ///
    /// - `key`: identity of the thing being republished, used to scope the window
    /// - `description`: how to name it in the warning
    static void noteRepublish(String key, String description) {
        if (!enabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        int count;
        synchronized (rateWindows) {
            long[] window = rateWindows.get(key);
            if (window == null || now - window[0] > RATE_WINDOW_MILLIS) {
                window = new long[]{now, 0L};
                rateWindows.put(key, window);
            }
            window[1]++;
            count = (int) window[1];
        }
        if (count != RATE_LIMIT) {
            // Warn on the crossing only, so a genuinely busy app does not drown its own log.
            return;
        }
        warn("Surfaces: " + description + " has been published " + RATE_LIMIT + " times in under "
                + (RATE_WINDOW_MILLIS / 1000) + " seconds. WidgetKit and the Android app-widget "
                + "host both throttle reloads against a daily budget, so on a device most of these "
                + "updates are dropped and the surface then stops refreshing entirely. Publish "
                + "only when the underlying data actually changes: SurfaceDynamicText timers and "
                + "countdowns tick on the OS clock with no republish at all, and a WidgetTimeline "
                + "can carry future entries the renderer applies on its own. This warning appears "
                + "only in the simulator.");
    }

    /// Warns once when an inert live activity handle is used.
    ///
    /// `LiveActivity.start` returns an inert handle rather than throwing when the platform refuses
    /// (live activities disabled by the user, ActivityKit rejecting the request), and `update` and
    /// `end` on that handle are documented no-ops. That is the right production behaviour and a
    /// terrible debugging experience, because an app that never checks `isActive()` sees nothing at
    /// all and usually goes on to start a second activity.
    static void inertActivity(String api) {
        if (!enabled()) {
            return;
        }
        warnOnce("inert:" + api, api + " was called on an inert LiveActivity handle and did "
                + "nothing. Either LiveActivity.start(...) never created an activity (the platform "
                + "does not support live activities, or the user disabled them - check "
                + "LiveActivity.isSupported() and the returned handle's isActive()), or end(...) "
                + "already ran on this handle. Track the handle's isActive() rather than your own "
                + "persisted flag, otherwise the next start(...) leaves an orphan activity the app "
                + "can no longer end. This warning appears only in the simulator.");
    }

    // --- internals ------------------------------------------------------------

    /// Test seam: the checks that matter most are the ones that only fire on the EDT, and the
    /// portable unit tests run with no platform at all. Null follows the real Display.
    static void setEdtForTests(Boolean value) {
        edtForTests = value;
    }

    private static boolean isEdt() {
        if (edtForTests != null) {
            return edtForTests.booleanValue();
        }
        try {
            return Display.isInitialized() && Display.getInstance().isEdt();
        } catch (Throwable t) {
            return false;
        }
    }

    private static void warnOnce(String key, String message) {
        synchronized (warnedOnce) {
            if (!warnedOnce.add(key)) {
                return;
            }
        }
        warn("Surfaces: " + message);
    }

    /// A diagnostic must never be the thing that breaks the app, and `Log` needs a platform
    /// implementation it does not always have (a test forcing diagnostics on, a warning raised
    /// before `Display.init` finished). Falling back to stdout keeps the message rather than
    /// trading it for a stack trace.
    private static void warn(String message) {
        try {
            Log.p(message);
        } catch (Throwable t) {
            System.out.println(message);
        }
    }

    private static String describeRegisteredKinds() {
        StringBuilder b = new StringBuilder();
        for (WidgetKind k : Surfaces.getRegisteredKinds()) {
            if (b.length() > 0) {
                b.append(", ");
            }
            b.append('"').append(k.getId()).append('"');
        }
        if (b.length() == 0) {
            return "none";
        }
        return b.toString();
    }
}
