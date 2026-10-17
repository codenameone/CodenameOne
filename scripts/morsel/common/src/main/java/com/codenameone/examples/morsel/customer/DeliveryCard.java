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
package com.codenameone.examples.morsel.customer;

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
import com.codenameone.examples.morsel.api.DeliveryActivityDto;
import com.codenameone.examples.morsel.api.DeliveryActivityKeys;
import com.codenameone.examples.morsel.api.DeliveryDto;
import com.codenameone.examples.morsel.api.DeliveryState;
import com.codenameone.examples.morsel.net.Api;
import com.codenameone.examples.morsel.net.Net;

import java.util.HashMap;
import java.util.Map;

/// The delivery on the lock screen: a Live Activity that says the courier is on
/// the way, who they are, the car to look for and how long until it is there.
///
/// It starts when a courier takes the delivery and ends with the delivery. While the
/// app is open the customer screen moves it on with every change it sees; once
/// the app is put away the server does, with push notifications addressed to
/// the activity's id, which is why the id is sent to the server here.
///
/// Where it shows: the lock screen and the Dynamic Island on an iPhone, an
/// ongoing notification on Android, and a small window on the desktop and in
/// the simulator. Elsewhere [LiveActivity#start] answers a handle that does
/// nothing, and this class has nothing to check.
final class DeliveryCard {
    private static final Net.Failed QUIET = (status, message) -> { };
    private static final int ACCENT = 0xff1f6feb;

    private static LiveActivity activity;
    private static String deliveryId;
    /// What the activity last showed, so that a poll that changes nothing
    /// sends nothing.
    private static String shown = "";
    /// A start or an update is under way on another thread.
    private static boolean busy;
    /// The delivery a start was last tried for. A platform with nothing to show
    /// answers an inactive handle, and asking again on every poll would not
    /// change that.
    private static String tried;

    private DeliveryCard() {
    }

    // tag::morsel-delivery-card-follow[]
    /// The customer screen has `delivery` as it now is. Called on the event thread.
    static void follow(final DeliveryDto delivery) {
        if (delivery == null || busy) {
            return;
        }
        boolean meeting = DeliveryState.ACCEPTED == delivery.state() || DeliveryState.ARRIVED == delivery.state()
                || DeliveryState.IN_PROGRESS == delivery.state();
        if (activity != null && !delivery.id().equals(deliveryId)) {
            // A delivery this screen no longer shows: its activity goes.
            final LiveActivity stale = activity;
            activity = null;
            deliveryId = null;
            shown = "";
            CN.scheduleBackgroundTask(() -> stale.end(null, true));
        }
        final boolean ours = activity != null;
        if (!meeting) {
            if (ours) {
                final LiveActivity ending = activity;
                final boolean done = DeliveryState.COMPLETED == delivery.state();
                activity = null;
                deliveryId = null;
                shown = "";
                // A finished delivery stays on the lock screen a little while, as
                // the platform decides; a cancelled one goes at once.
                CN.scheduleBackgroundTask(() -> ending.end(done ? state(delivery) : null, !done));
            }
            return;
        }
        final Map<String, Object> now = state(delivery);
        String key = delivery.id() + ":" + now;
        if (ours ? key.equals(shown) : delivery.id().equals(tried)) {
            return;
        }
        if (!ours) {
            tried = delivery.id();
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
                deliveryId = delivery.id();
                Api.customer().activity(delivery.id(), new DeliveryActivityDto(started.getId()),
                        Net.to(saved -> { }, QUIET));
            });
        });
    }
    // end::morsel-delivery-card-follow[]

    /// The activity's state, in the keys the server sends too.
    static Map<String, Object> state(DeliveryDto delivery) {
        Map<String, Object> out = new HashMap<String, Object>();
        String status;
        double progress;
        if (DeliveryState.ARRIVED == delivery.state()) {
            status = "Your courier is here";
            progress = DeliveryActivityKeys.ARRIVED;
        } else if (DeliveryState.IN_PROGRESS == delivery.state()) {
            status = "On the trip";
            progress = DeliveryActivityKeys.IN_TRIP;
        } else if (DeliveryState.COMPLETED == delivery.state()) {
            status = "You've arrived";
            progress = DeliveryActivityKeys.DONE;
        } else {
            status = "On the way";
            progress = DeliveryActivityKeys.ON_THE_WAY;
        }
        out.put(DeliveryActivityKeys.STATUS, status);
        out.put(DeliveryActivityKeys.PROGRESS, Double.valueOf(progress));
        out.put(DeliveryActivityKeys.COURIER, text(delivery.courierName()));
        out.put(DeliveryActivityKeys.CAR, text(delivery.vehicle()));
        out.put(DeliveryActivityKeys.PLATE, text(delivery.plate()));
        out.put(DeliveryActivityKeys.ETA, delivery.etaMinutes() > 0
                && DeliveryState.ARRIVED != delivery.state() ? delivery.etaMinutes() + " min" : "");
        return out;
    }

    // tag::morsel-delivery-card-layout[]
    /// The layout, filled in from the state on the device: an update sends the
    /// state alone.
    private static LiveActivityDescriptor descriptor() {
        return new LiveActivityDescriptor(DeliveryActivityKeys.KIND)
                .setContent(content())
                .setCompactLeading(new SurfaceText("${courier}").setFontSize(13)
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
                                .add(new SurfaceText("${courier} - ${car}").setFontSize(13)
                                        .setColor(SurfaceColor.SECONDARY_LABEL)
                                        .setMaxLines(1))
                                .add(new SurfaceText("${plate}").setFontSize(13)
                                        .setFontWeight(SurfaceFontWeight.BOLD)
                                        .setMaxLines(1)))
                        .add(new SurfaceText("${eta}").setFontSize(22)
                                .setFontWeight(SurfaceFontWeight.BOLD)
                                .setColor(SurfaceColor.ACCENT)))
                .add(new SurfaceProgress(SurfaceProgress.STYLE_LINEAR)
                        .setValueState(DeliveryActivityKeys.PROGRESS)
                        .setColor(SurfaceColor.rgb(ACCENT)));
    }
    // end::morsel-delivery-card-layout[]

    private static String text(String value) {
        return value == null ? "" : value;
    }
}
