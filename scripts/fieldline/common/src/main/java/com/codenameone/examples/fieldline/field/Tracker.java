package com.codenameone.examples.fieldline.field;

import com.codename1.appkit.Net;
import com.codename1.maps.LatLng;
import com.codenameone.examples.fieldline.api.LocationDto;
import com.codenameone.examples.fieldline.map.Locator;
import com.codenameone.examples.fieldline.net.Api;

/// Tells dispatch where a technician is while they work their day.
///
/// The crew map and the planner's travel estimates both read the last
/// position reported, so it is sent at most once a minute and only when it
/// moved by more than a city block: a technician sitting at a site for two
/// hours costs the server nothing. Precise tracking -- and with it reporting
/// from the background -- is on only while a job is under way, from leaving
/// for it to finishing it.
public final class Tracker {
    private static final long EVERY = 60000L;
    private static final double MOVED_METERS = 120;

    private static boolean on;
    private static boolean precise;
    private static long sentAt;
    private static LatLng sent;

    private static final Locator.Watcher WATCHER = (state, where) -> {
        if (state == Locator.FOUND || state == Locator.SIMULATED) {
            report(where);
        }
    };

    private Tracker() {
    }

    /// Starts reporting. `underWay` asks for the precise, background search.
    public static void start(boolean underWay) {
        if (!on) {
            on = true;
            Locator.watch(WATCHER);
        }
        if (precise != underWay) {
            precise = underWay;
            Locator.track(underWay);
        }
    }

    /// Stops reporting: signed out, or in the dispatcher's seat.
    public static void stop() {
        if (on) {
            on = false;
            Locator.unwatch(WATCHER);
        }
        if (precise) {
            precise = false;
            Locator.track(false);
        }
    }

    public static boolean running() {
        return on;
    }

    /// The app went to the background: the plain search is put down by the
    /// locator; a job under way keeps the precise one.
    public static void paused() {
        // Nothing of its own: Locator decides what survives the background.
    }

    public static void resumed() {
        if (on) {
            sentAt = 0;
        }
    }

    private static void report(LatLng where) {
        if (!on || where == null) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean far = sent == null || distance(sent, where) > MOVED_METERS;
        if (!far && now - sentAt < EVERY * 5 || far && now - sentAt < EVERY) {
            return;
        }
        sentAt = now;
        sent = where;
        Api.field().reportLocation(new LocationDto(where.getLatitude(), where.getLongitude()),
                Net.to(answer -> {
                }, (status, message) -> {
                    // Best effort: the next fix tries again.
                }));
    }

    private static double distance(LatLng a, LatLng b) {
        return com.codenameone.examples.fieldline.map.Maps.meters(a, b);
    }
}
