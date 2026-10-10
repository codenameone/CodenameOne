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

package com.codename1.impl.html5;

import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.dom.MessageEvent;
import com.codename1.impl.html5.JSOImplementations.GeoCallback;
import com.codename1.impl.html5.JSOImplementations.WindowExt;
import com.codename1.location.Location;
import com.codename1.location.LocationListener;
import com.codename1.location.LocationManager;
import com.codename1.location.LocationRequest;
import com.codename1.ui.Display;
import java.io.IOException;

/// The browser's Geolocation API as a [LocationManager].
///
/// The application runs in a worker and the API lives on the page, so every
/// request goes through `window.cn1GeoRequest` (js/localforage-shim.js), which
/// answers with the position or the error flattened into a string -- a
/// position object does not survive the trip. See there.
///
/// Three things here are deliberate:
///
/// - Nothing waits without a limit. The browser may leave a request
///   unanswered for as long as its permission prompt is open, which is as long
///   as the user likes.
/// - A listener is told about every failure, not only about a change of
///   status: the status starts out as "temporarily unavailable", so a first
///   request that failed that way used to tell nobody.
/// - The position is kept before anybody is told that the provider is
///   available. `getCurrentLocationSync` answers that news by asking for the
///   current location, on the event thread; it gets the fix that just came in
///   instead of a second round trip to the browser.
public class HTML5LocationManager extends LocationManager {
    /// How long [#getCurrentLocation] waits for the browser, and a little
    /// longer for the answer to cross from the page.
    private static final int REQUEST_TIMEOUT = 10000;
    private static final int REQUEST_GRACE = 2000;
    /// A fix this recent answers [#getCurrentLocation] without a new request.
    private static final int FRESH_MILLIS = 5000;

    Location lastKnownLocation;
    private long lastFixTime;
    private int watchId;
    private boolean watching;
    /// Counts the watches started, so an answer to one that has been cleared
    /// since is not taken for an answer to the current one.
    private int watchGeneration;

    public HTML5LocationManager(){

    }

    /// A position or an error, as the page reported it.
    static final class Answer {
        /// Null for an error.
        Location location;
        /// As in GeolocationPositionError: 1 denied, 2 unavailable, 3 timed out.
        int errorCode;
        String errorMessage;
    }

    /// Reads "P|lat|lng|accuracy|altitude|heading|speed|timestamp" or
    /// "E|code|message". Anything else is an error of its own: an answer that
    /// cannot be read must still end the wait for it.
    static Answer parse(String text) {
        Answer out = new Answer();
        if (text == null || text.length() < 2 || text.charAt(1) != '|') {
            out.errorCode = 2;
            out.errorMessage = "Unreadable answer from the location service";
            return out;
        }
        if (text.charAt(0) == 'P') {
            String[] part = new String[7];
            int from = 2;
            for (int iter = 0; iter < part.length; iter++) {
                int to = text.indexOf('|', from);
                if (to < 0) {
                    to = text.length();
                }
                part[iter] = from <= to ? text.substring(from, to) : "";
                from = Math.min(text.length(), to) + 1;
            }
            double latitude = number(part[0]);
            double longitude = number(part[1]);
            if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
                out.errorCode = 2;
                out.errorMessage = "The location service answered without a position";
                return out;
            }
            Location loc = new Location();
            loc.setLatitude(latitude);
            loc.setLongitude(longitude);
            loc.setAccuracy((float) orZero(number(part[2])));
            loc.setAltitude(orZero(number(part[3])));
            loc.setDirection((float) orZero(number(part[4])));
            loc.setVelocity((float) orZero(number(part[5])));
            double stamp = number(part[6]);
            loc.setTimeStamp(Double.isNaN(stamp) ? System.currentTimeMillis() : (long) stamp);
            out.location = loc;
            return out;
        }
        int bar = text.indexOf('|', 2);
        String code = bar < 0 ? text.substring(2) : text.substring(2, bar);
        double parsed = number(code);
        out.errorCode = Double.isNaN(parsed) || parsed < 1 || parsed > 3 ? 2 : (int) parsed;
        out.errorMessage = bar < 0 || bar + 1 >= text.length() ? "Location error " + out.errorCode
                : text.substring(bar + 1);
        return out;
    }

    private static double number(String text) {
        if (text == null || text.length() == 0) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException err) {
            return Double.NaN;
        }
    }

    private static double orZero(double value) {
        return Double.isNaN(value) ? 0 : value;
    }

    private static WindowExt window() {
        return (WindowExt) Window.current();
    }

    private void keep(Location loc) {
        lastKnownLocation = loc;
        lastFixTime = System.currentTimeMillis();
    }

    /// Blocks until the browser answers, for at most [#REQUEST_TIMEOUT] and a
    /// moment more. Call it off the event thread, or use a listener.
    @Override
    public Location getCurrentLocation() throws IOException {
        Location recent = lastKnownLocation;
        if (recent != null && System.currentTimeMillis() - lastFixTime <= FRESH_MILLIS) {
            return recent;
        }
        final Answer[] result = new Answer[1];
        window().cn1GeoRequest(new GeoCallback() {
            @Override
            public void onAnswer(MessageEvent answer) {
                Answer parsed = parse(answer == null ? null : answer.getDataAsString());
                synchronized (result) {
                    result[0] = parsed;
                    result.notifyAll();
                }
            }
        }, false, REQUEST_TIMEOUT, 0, false);

        long deadline = System.currentTimeMillis() + REQUEST_TIMEOUT + REQUEST_GRACE;
        synchronized (result) {
            while (result[0] == null) {
                long left = deadline - System.currentTimeMillis();
                if (left <= 0) {
                    break;
                }
                try {
                    result.wait(left);
                } catch (InterruptedException ie) {
                    throw new IOException("Interrupted while waiting for location", ie);
                }
            }
        }
        Answer answer = result[0];
        if (answer == null) {
            // The permission prompt is still open, most likely.
            throw new IOException("The browser did not answer the location request");
        }
        if (answer.location == null) {
            throw new IOException(answer.errorMessage);
        }
        keep(answer.location);
        return answer.location;
    }

    @Override
    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    @Override
    protected void bindListener() {
        if (watching) {
            return;
        }
        watching = true;
        final int generation = ++watchGeneration;
        LocationRequest request = getRequest();
        boolean precise = request != null
                && request.getPriority() == LocationRequest.PRIORITY_HIGH_ACCUARCY;
        watchId = window().cn1GeoRequest(new GeoCallback() {
            @Override
            public void onAnswer(MessageEvent answer) {
                final Answer parsed = parse(answer == null ? null : answer.getDataAsString());
                // On the event thread: the listener is the application's, and
                // so is everything it touches.
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        if (generation == watchGeneration && watching) {
                            deliver(parsed);
                        }
                    }
                });
            }
        }, true, 0, 0, precise);
    }

    private void deliver(Answer answer) {
        int oldStatus = getStatus();
        if (answer.location != null) {
            keep(answer.location);
            setStatus(LocationManager.AVAILABLE);
            LocationListener l = getLocationListener();
            if (l != null && oldStatus != LocationManager.AVAILABLE) {
                l.providerStateChanged(LocationManager.AVAILABLE);
            }
            // Read again: a listener may take itself off when it hears that.
            l = getLocationListener();
            if (l != null) {
                l.locationUpdated(answer.location);
            }
            return;
        }
        // Refused is for good, as far as this page can tell; no fix and no
        // answer in time may both pass.
        setStatus(answer.errorCode == 1 ? LocationManager.OUT_OF_SERVICE
                : LocationManager.TEMPORARILY_UNAVAILABLE);
        LocationListener l = getLocationListener();
        if (l != null) {
            l.providerStateChanged(getStatus());
        }
    }

    @Override
    protected void clearListener() {
        if (watching) {
            watching = false;
            watchGeneration++;
            window().cn1GeoClear(watchId);
            watchId = 0;
        }
    }

}
