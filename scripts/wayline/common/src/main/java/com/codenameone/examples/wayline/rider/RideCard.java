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
package com.codenameone.examples.wayline.rider;

import com.codename1.surfaces.LiveActivity;
import com.codename1.surfaces.LiveActivityDescriptor;
import com.codename1.surfaces.SurfaceColor;
import com.codename1.surfaces.SurfaceColumn;
import com.codename1.surfaces.SurfaceFontWeight;
import com.codename1.surfaces.SurfaceNode;
import com.codename1.surfaces.SurfaceProgress;
import com.codename1.surfaces.SurfaceRow;
import com.codename1.surfaces.SurfaceText;
import com.codename1.ui.CN;
import com.codenameone.examples.wayline.api.RideActivityDto;
import com.codenameone.examples.wayline.api.RideActivityKeys;
import com.codenameone.examples.wayline.api.RideDto;
import com.codenameone.examples.wayline.api.RideState;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;

import java.util.HashMap;
import java.util.Map;

/// The ride on the lock screen: a Live Activity that says the driver is on
/// the way, who they are, the car to look for and how long until it is there.
///
/// It starts when a driver takes the ride and ends with the ride. While the
/// app is open the rider screen moves it on with every change it sees; once
/// the app is put away the server does, with push notifications addressed to
/// the activity's id, which is why the id is sent to the server here.
///
/// Where it shows: the lock screen and the Dynamic Island on an iPhone, an
/// ongoing notification on Android, and a small window on the desktop and in
/// the simulator. Elsewhere [LiveActivity#start] answers a handle that does
/// nothing, and this class has nothing to check.
final class RideCard {
    private static final Net.Failed QUIET = (status, message) -> { };
    private static final int ACCENT = 0xff1f6feb;

    private static LiveActivity activity;
    private static String rideId;
    /// What the activity last showed, so that a poll that changes nothing
    /// sends nothing.
    private static String shown = "";
    /// A start or an update is under way on another thread.
    private static boolean busy;
    /// The ride a start was last tried for. A platform with nothing to show
    /// answers an inactive handle, and asking again on every poll would not
    /// change that.
    private static String tried;

    private RideCard() {
    }

    // tag::wayline-ride-card-follow[]
    /// The rider screen has `ride` as it now is. Called on the event thread.
    static void follow(final RideDto ride) {
        if (ride == null || busy) {
            return;
        }
        boolean meeting = RideState.ACCEPTED == ride.state() || RideState.ARRIVED == ride.state()
                || RideState.IN_PROGRESS == ride.state();
        if (activity != null && !ride.id().equals(rideId)) {
            // A ride this screen no longer shows: its activity goes.
            final LiveActivity stale = activity;
            activity = null;
            rideId = null;
            shown = "";
            CN.scheduleBackgroundTask(() -> stale.end(null, true));
        }
        final boolean ours = activity != null;
        if (!meeting) {
            if (ours) {
                final LiveActivity ending = activity;
                final boolean done = RideState.COMPLETED == ride.state();
                activity = null;
                rideId = null;
                shown = "";
                // A finished ride stays on the lock screen a little while, as
                // the platform decides; a cancelled one goes at once.
                CN.scheduleBackgroundTask(() -> ending.end(done ? state(ride) : null, !done));
            }
            return;
        }
        final Map<String, Object> now = state(ride);
        String key = ride.id() + ":" + now;
        if (ours ? key.equals(shown) : ride.id().equals(tried)) {
            return;
        }
        if (!ours) {
            tried = ride.id();
        }
        shown = key;
        busy = true;
        final LiveActivity current = ours ? activity : null;
        // Off the event thread: on a phone starting and updating an activity
        // is a round trip to the system.
        CN.scheduleBackgroundTask(() -> {
            if (current != null) {
                current.update(now);
                CN.callSerially(() -> busy = false);
                return;
            }
            final LiveActivity started = LiveActivity.start(descriptor(), now);
            CN.callSerially(() -> {
                busy = false;
                if (!started.isActive()) {
                    return;
                }
                activity = started;
                rideId = ride.id();
                Api.rider().activity(ride.id(), new RideActivityDto(started.getId()),
                        Net.to(saved -> { }, QUIET));
            });
        });
    }
    // end::wayline-ride-card-follow[]

    /// The activity's state, in the keys the server sends too.
    static Map<String, Object> state(RideDto ride) {
        Map<String, Object> out = new HashMap<String, Object>();
        String status;
        double progress;
        if (RideState.ARRIVED == ride.state()) {
            status = "Your driver is here";
            progress = RideActivityKeys.ARRIVED;
        } else if (RideState.IN_PROGRESS == ride.state()) {
            status = "On the trip";
            progress = RideActivityKeys.IN_TRIP;
        } else if (RideState.COMPLETED == ride.state()) {
            status = "You've arrived";
            progress = RideActivityKeys.DONE;
        } else {
            status = "On the way";
            progress = RideActivityKeys.ON_THE_WAY;
        }
        out.put(RideActivityKeys.STATUS, status);
        out.put(RideActivityKeys.PROGRESS, Double.valueOf(progress));
        out.put(RideActivityKeys.DRIVER, text(ride.driverName()));
        out.put(RideActivityKeys.CAR, text(ride.vehicle()));
        out.put(RideActivityKeys.PLATE, text(ride.plate()));
        out.put(RideActivityKeys.ETA, ride.etaMinutes() > 0
                && RideState.ARRIVED != ride.state() ? ride.etaMinutes() + " min" : "");
        return out;
    }

    // tag::wayline-ride-card-layout[]
    /// The layout, filled in from the state on the device: an update sends the
    /// state alone.
    private static LiveActivityDescriptor descriptor() {
        return new LiveActivityDescriptor(RideActivityKeys.KIND)
                .setContent(content())
                .setCompactLeading(new SurfaceText("${driver}").setFontSize(13)
                        .setMaxLines(1))
                .setCompactTrailing(new SurfaceText("${eta}").setFontSize(13)
                        .setColor(SurfaceColor.ACCENT))
                .setMinimal(new SurfaceText("${eta}").setFontSize(11))
                .setExpandedCenter(new SurfaceText("${status}").setFontSize(16)
                        .setFontWeight(SurfaceFontWeight.SEMIBOLD))
                .setExpandedBottom(content())
                .setTint(SurfaceColor.rgb(ACCENT));
    }

    private static SurfaceNode content() {
        return new SurfaceColumn().setSpacing(6).setPadding(12)
                .add(new SurfaceRow().setSpacing(10)
                        .add(new SurfaceColumn().setSpacing(2).setWeight(1)
                                .add(new SurfaceText("${status}").setFontSize(15)
                                        .setFontWeight(SurfaceFontWeight.SEMIBOLD)
                                        .setMaxLines(1))
                                .add(new SurfaceText("${driver} - ${car}").setFontSize(13)
                                        .setColor(SurfaceColor.SECONDARY_LABEL)
                                        .setMaxLines(1))
                                .add(new SurfaceText("${plate}").setFontSize(13)
                                        .setFontWeight(SurfaceFontWeight.BOLD)
                                        .setMaxLines(1)))
                        .add(new SurfaceText("${eta}").setFontSize(22)
                                .setFontWeight(SurfaceFontWeight.BOLD)
                                .setColor(SurfaceColor.ACCENT)))
                .add(new SurfaceProgress(SurfaceProgress.STYLE_LINEAR)
                        .setValueState(RideActivityKeys.PROGRESS)
                        .setColor(SurfaceColor.rgb(ACCENT)));
    }
    // end::wayline-ride-card-layout[]

    private static String text(String value) {
        return value == null ? "" : value;
    }
}
