/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.wayline.map;

import com.codename1.location.Location;
import com.codename1.location.LocationListener;
import com.codename1.location.LocationManager;
import com.codename1.location.LocationRequest;
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
    /// How often a tracking search asks for a position.
    private static final long TRACK_MILLIS = 5000L;

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
    /// Whether the position is wanted precisely and in the background: a
    /// driver on line. See [#track].
    private static boolean tracking;
    /// Whether the search was put down because the app went to the
    /// background, to be taken up again when it returns.
    private static boolean paused;
    private static double heading;
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

    /// The direction the device last moved in, in degrees from north.
    public static double heading() {
        return heading;
    }

    // tag::wayline-locator-track[]
    /// Asks for the position precisely, often, and on in the background, or
    /// goes back to the ordinary search. A driver on line tracks: riders follow
    /// the car on a map, and the server offers rides by distance.
    ///
    /// The platform decides what "in the background" allows. With the
    /// `location` background mode an iPhone goes on reporting after the app is
    /// put away; Android does while the app runs a foreground service, which is
    /// what [com.codenameone.examples.wayline.driver.Shift] starts.
    public static void track(boolean on) {
        if (tracking == on) {
            return;
        }
        tracking = on;
        if (started && !simulated()) {
            LocationManager.getLocationManager().setLocationListener(null);
            start(true);
        }
    }

    /// The app went to the background. Unless a driver is tracking, the
    /// platform is told to stop looking: an app with a background location
    /// mode is otherwise kept running, and draining the battery, for a map
    /// nobody is looking at.
    public static void paused() {
        if (!started || tracking || simulated()) {
            return;
        }
        paused = true;
        search++;
        LocationManager.getLocationManager().setLocationListener(null);
    }

    /// The app is back: whatever [#paused] put down is taken up again.
    public static void resumed() {
        if (!paused) {
            return;
        }
        paused = false;
        if (started && !simulated()) {
            start(true);
        }
    }
    // end::wayline-locator-track[]

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
            start(true);
        } else if (state != SEARCHING) {
            watcher.located(state, here());
        }
    }

    /// Stops following. When nobody is left the platform is told to stop
    /// too: a location listener left in place keeps the receiver running, and
    /// the battery paying for it, on every screen without a map and after
    /// signing out. The position it found is kept for [#here], and the next
    /// [#watch] starts the search again.
    public static void unwatch(Watcher watcher) {
        WATCHERS.remove(watcher);
        if (started && WATCHERS.isEmpty()) {
            started = false;
            paused = false;
            // Whatever the listener or the patience of this search still has
            // to say is about a search nobody is following.
            search++;
            LocationManager.getLocationManager().setLocationListener(null);
        }
    }

    /// Asks again, after a refusal or a failure.
    public static void retry() {
        if (simulated()) {
            return;
        }
        LocationManager.getLocationManager().setLocationListener(null);
        start(false);
    }

    private static void start(final boolean resumed) {
        started = true;
        final int mine = ++search;
        // A search taken up again -- one screen with a map left, the next
        // arrived -- already knows where the device is, and says so, where
        // a first search has only that it is looking.
        set(resumed && found != null ? FOUND : SEARCHING);
        LocationListener listener = new LocationListener() {
            @Override
            public void locationUpdated(Location location) {
                if (mine != search || location == null) {
                    return;
                }
                found = new LatLng(location.getLatitude(), location.getLongitude());
                if (location.getDirection() >= 0f) {
                    heading = location.getDirection();
                }
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
        };
        if (tracking) {
            // Every few seconds, as precisely as the device can: what a car on
            // a rider's map needs. The ordinary search takes the platform's
            // default, which on an iPhone waits for 100 meters of movement.
            LocationManager.getLocationManager().setLocationListener(listener,
                    new LocationRequest(LocationRequest.PRIORITY_HIGH_ACCUARCY, TRACK_MILLIS));
        } else {
            LocationManager.getLocationManager().setLocationListener(listener);
        }
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
