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
package com.codenameone.examples.savor.map;

import com.codename1.maps.LatLng;
import com.codename1.maps.MapView;
import com.codename1.maps.Marker;
import com.codename1.maps.MarkerOptions;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Form;
import com.codename1.ui.util.UITimer;

/// A car on a map, that drives from one reported position to the next.
///
/// Positions arrive every few seconds. Put where each one says, the car would
/// jump; so it is moved there a little at a time over about the time until
/// the next report, and turned to face the way it is going.
public final class CarMarker {
    /// How long a move takes: the interval couriers report at.
    private static final int GLIDE_MILLIS = 3000;
    private static final int FRAME_MILLIS = 50;
    /// The turn is done in this much of the move, as a car turns at a corner
    /// and not all along the street.
    private static final double TURN_PART = 0.3;

    private final MapView map;
    private final Form form;
    private Marker marker;
    private EncodedImage icon;
    private LatLng at;
    private double heading;
    private LatLng from;
    private LatLng to;
    private double fromHeading;
    private double toHeading;
    private long startedAt;
    private UITimer timer;

    /// @param form the screen the map is on; the car moves while it is showing
    /// @param heading degrees clockwise from north
    public CarMarker(MapView map, Form form, LatLng at, double heading) {
        this.map = map;
        this.form = form;
        place(at, heading);
    }

    public LatLng position() {
        return at;
    }

    public double heading() {
        return heading;
    }

    /// Drives the car to `target`.
    ///
    /// @param facing the heading reported with the position, or a negative
    ///     number when none was, and the way the car moved says which way it
    ///     faces
    public void moveTo(LatLng target, double facing) {
        double moved = Maps.meters(at, target);
        double wanted = facing >= 0 ? facing : moved > 2 ? Maps.heading(at, target) : heading;
        // Too far to have been driven in one interval: the car was somewhere
        // else, and is shown where it is.
        if (moved > 1500) {
            stop();
            place(target, wanted);
            map.repaint();
            return;
        }
        from = at;
        to = target;
        fromHeading = heading;
        toHeading = wanted;
        startedAt = System.currentTimeMillis();
        if (timer == null) {
            timer = UITimer.timer(FRAME_MILLIS, true, form, this::step);
        }
    }

    public void remove() {
        stop();
        if (marker != null) {
            map.removeMarker(marker);
            marker = null;
        }
    }

    private void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void step() {
        double part = Math.min(1d, (System.currentTimeMillis() - startedAt)
                / (double) GLIDE_MILLIS);
        // The shorter way round from one heading to the other.
        double turn = toHeading - fromHeading;
        while (turn > 180) {
            turn -= 360;
        }
        while (turn < -180) {
            turn += 360;
        }
        place(new LatLng(from.getLatitude() + (to.getLatitude() - from.getLatitude()) * part,
                        from.getLongitude() + (to.getLongitude() - from.getLongitude()) * part),
                fromHeading + turn * Math.min(1d, part / TURN_PART));
        map.repaint();
        if (part >= 1d) {
            stop();
        }
    }

    private void place(LatLng position, double facing) {
        at = position;
        heading = facing;
        EncodedImage wanted = Maps.car(facing);
        if (marker != null && wanted == icon) { //NOPMD CompareObjectsWithEquals
            marker.setPosition(position);
            return;
        }
        // A marker's picture cannot be changed, so a car that has turned is a
        // new marker.
        if (marker != null) {
            map.removeMarker(marker);
        }
        icon = wanted;
        marker = map.addMarker(new MarkerOptions(position).icon(wanted).anchor(0.5f, 0.5f));
    }
}
