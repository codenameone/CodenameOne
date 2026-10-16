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
package com.codenameone.examples.wayline.driver;

import com.codename1.background.ForegroundService;
import com.codename1.maps.LatLng;
import com.codename1.ui.CN;
import com.codenameone.examples.wayline.api.DriverStatusDto;
import com.codenameone.examples.wayline.map.Locator;
import com.codenameone.examples.wayline.map.Maps;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;

import java.util.concurrent.atomic.AtomicBoolean;

// tag::wayline-shift[]
/// A driver's time on line, as far as the phone is concerned: the position
/// keeps going to the server while the app is in the background.
///
/// In the foreground the driver screen reports every few seconds itself. Put
/// away -- the driver switched to a navigation app, or locked the phone -- that
/// screen stops, and this takes over: it follows the position in the
/// background and reports it when the car has moved [#MIN_METERS], and at
/// least every [#KEEP_ALIVE_MILLIS] even when it has not, because a driver who
/// goes quiet for a minute stops being offered rides.
///
/// - On iOS the `location` background mode keeps the app running while it
///   tracks, and each new position is the moment to report.
/// - On Android a foreground service keeps it running, with the notification
///   the system requires, and wakes it for the reports a parked car would not
///   send.
/// - The simulator and the browser stand still and report nothing here.
public final class Shift {
    /// How far the car moves before the server hears of it again.
    static final double MIN_METERS = 25d;
    /// The longest the server goes without hearing, well inside the minute
    /// after which it stops offering this driver rides.
    static final long KEEP_ALIVE_MILLIS = 20000L;
    private static final String CHANNEL = "wayline-driving";
    private static final Net.Failed QUIET = (status, message) -> { };

    private static boolean on;
    private static boolean background;
    private static long sentAt;
    private static LatLng sentFrom;
    private static ForegroundService service;
    /// Read by the service's own thread, so not a plain field.
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    private static final Locator.Watcher WATCHER = (state, where) -> {
        if (state == Locator.FOUND && background) {
            report(where);
        }
    };

    private Shift() {
    }

    /// The driver went on line.
    public static void begin() {
        if (on) {
            return;
        }
        on = true;
        sentFrom = null;
        Locator.track(true);
        Locator.watch(WATCHER);
        if (ForegroundService.isSupported() && "and".equals(CN.getPlatformName())
                && !CN.isSimulator()) {
            RUNNING.set(true);
            service = ForegroundService.start(CHANNEL, "You're online",
                    "Wayline shares your location with riders while you drive.", null,
                    Shift::keepAlive);
        }
    }

    /// The driver went off line, signed out or left the driver's side of the
    /// app. Nothing goes on following where the phone is.
    public static void end() {
        if (!on) {
            return;
        }
        on = false;
        Locator.unwatch(WATCHER);
        Locator.track(false);
        RUNNING.set(false);
        if (service != null) {
            service.stop();
            service = null;
        }
    }

    public static boolean isOn() {
        return on;
    }

    /// The app went to the background, and the reports are this class's.
    public static void paused() {
        background = true;
    }

    /// The app is back, and the driver screen reports again.
    public static void resumed() {
        background = false;
    }
    // end::wayline-shift[]

    /// The foreground service's task, on a thread of its own: wakes now and
    /// then for the car that is not moving, which brings no new positions.
    private static void keepAlive(ForegroundService running) {
        while (RUNNING.get() && running.isRunning()) {
            try {
                Thread.sleep(KEEP_ALIVE_MILLIS);
            } catch (InterruptedException stop) {
                return;
            }
            if (RUNNING.get()) {
                CN.callSerially(() -> {
                    if (on && background) {
                        report(Locator.here());
                    }
                });
            }
        }
    }

    /// Reports `where` if the car has moved far enough or long enough has
    /// passed. The answer says whether the server still has this driver on
    /// line; one that does not ends the shift here too.
    private static void report(LatLng where) {
        long now = System.currentTimeMillis();
        boolean moved = sentFrom == null || Maps.meters(sentFrom, where) >= MIN_METERS;
        if (!moved && now - sentAt < KEEP_ALIVE_MILLIS) {
            return;
        }
        sentAt = now;
        sentFrom = where;
        Api.driver().status(new DriverStatusDto(true, where.getLatitude(), where.getLongitude(),
                Locator.heading()), Net.to(me -> {
                    if (!me.online()) {
                        end();
                    }
                }, QUIET));
    }
}
