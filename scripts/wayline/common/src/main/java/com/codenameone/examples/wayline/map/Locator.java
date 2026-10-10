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
package com.codenameone.examples.wayline.map;

import com.codename1.location.Location;
import com.codename1.location.LocationListener;
import com.codename1.location.LocationManager;
import com.codename1.maps.LatLng;
import com.codename1.ui.CN;
import com.codenameone.examples.wayline.AppConfig;

import java.util.ArrayList;
import java.util.List;

/// Where the device is.
///
/// On a phone, and in a browser, that is the position the platform reports. In
/// the simulator there is no position worth the name, so the app stands at a
/// fixed point instead -- and since a simulated driver would then never move,
/// the driver screen drives the car along the route itself when [#simulated]
/// says so.
///
/// Nothing here waits. The platform answers when it answers -- after the user
/// has dealt with a permission prompt, which may be never -- so [#here] is
/// always the best answer there is at that moment, and a screen that wants to
/// follow the search [#watch]es it: the position when it comes, or that it was
/// refused, or that there is none to be had, or that nothing has been heard
/// yet. The screen stays usable through all of them, on the middle of the demo
/// city until something better is known.
public final class Locator {
    /// Positions are made up; nothing is being looked for.
    public static final int SIMULATED = 0;
    /// Asked, and not answered yet.
    public static final int SEARCHING = 1;
    /// [#here] is where the device is.
    public static final int FOUND = 2;
    /// The user, or a setting, refused.
    public static final int DENIED = 3;
    /// The platform has no position to give.
    public static final int UNAVAILABLE = 4;
    /// Still not answered after [#PATIENCE]: a prompt nobody has dealt with,
    /// or a fix that is slow to come. An answer is still taken when it does.
    public static final int SLOW = 5;

    /// How long a search may go unanswered before a screen says so.
    private static final int PATIENCE = 8000;

    /// Told every time the search gets somewhere, on the event thread.
    public interface Watcher {
        /// @param state one of the constants of [Locator]
        /// @param where the position for [#FOUND]; otherwise where the app
        ///     stands in the meantime
        void located(int state, LatLng where);
    }

    private static LatLng pinned;
    private static LatLng found;
    private static int state = SIMULATED;
    private static boolean started;
    /// Counts the searches, so that the patience of one that has been
    /// restarted does not run out on the next.
    private static int search;
    private static final List<Watcher> WATCHERS = new ArrayList<Watcher>();

    private Locator() {
    }

    /// Stands the device at `point`, whatever it reports. For the tests.
    public static void pin(LatLng point) {
        pinned = point;
    }

    /// Whether positions are made up, because there is no real one to read.
    public static boolean simulated() {
        return pinned != null || CN.isSimulator();
    }

    /// The device's position as far as it is known: the last one reported,
    /// and until there is one the middle of the demo city. Never waits.
    public static LatLng here() {
        if (pinned != null) {
            return pinned;
        }
        if (found != null && !CN.isSimulator()) {
            return found;
        }
        return new LatLng(AppConfig.DEFAULT_LAT, AppConfig.DEFAULT_LNG);
    }

    /// How the search stands: one of the constants above.
    public static int state() {
        return simulated() ? SIMULATED : state;
    }

    /// Follows the search, starting it if nothing has yet. Asking is what
    /// makes the platform ask the user for permission. `watcher` is told
    /// where things stand now unless that is nothing worth telling.
    public static void watch(Watcher watcher) {
        if (!WATCHERS.contains(watcher)) {
            WATCHERS.add(watcher);
        }
        if (simulated()) {
            return;
        }
        if (!started) {
            start();
        } else if (state != SEARCHING) {
            watcher.located(state, here());
        }
    }

    public static void unwatch(Watcher watcher) {
        WATCHERS.remove(watcher);
    }

    /// Asks again, after a refusal or a failure.
    public static void retry() {
        if (simulated()) {
            return;
        }
        LocationManager.getLocationManager().setLocationListener(null);
        start();
    }

    private static void start() {
        started = true;
        final int mine = ++search;
        set(SEARCHING);
        LocationManager.getLocationManager().setLocationListener(new LocationListener() {
            @Override
            public void locationUpdated(Location location) {
                if (mine != search || location == null) {
                    return;
                }
                found = new LatLng(location.getLatitude(), location.getLongitude());
                set(FOUND);
            }

            @Override
            public void providerStateChanged(int newState) {
                if (mine != search || newState == LocationManager.AVAILABLE) {
                    return;
                }
                if (newState == LocationManager.OUT_OF_SERVICE) {
                    set(DENIED);
                } else if (found == null) {
                    // With a position in hand, losing the signal for a while
                    // is not worth a word.
                    set(UNAVAILABLE);
                }
            }
        });
        CN.setTimeout(PATIENCE, new Runnable() {
            @Override
            public void run() {
                if (mine == search && state == SEARCHING) {
                    set(SLOW);
                }
            }
        });
    }

    private static void set(int next) {
        state = next;
        LatLng where = here();
        // A copy: a watcher may stop watching when it hears.
        List<Watcher> all = new ArrayList<Watcher>(WATCHERS);
        for (Watcher watcher : all) {
            watcher.located(next, where);
        }
    }
}
