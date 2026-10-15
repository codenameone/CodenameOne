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
package com.codenameone.examples.wayline.push;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushTarget;
import com.codenameone.examples.wayline.Text;
import com.codenameone.examples.wayline.api.RideActivityKeys;
import com.codenameone.examples.wayline.api.RideActivityDto;
import com.codenameone.examples.wayline.api.RideState;
import com.codenameone.examples.wayline.domain.DriverState;
import com.codenameone.examples.wayline.domain.Preference;
import com.codenameone.examples.wayline.domain.Profile;
import com.codenameone.examples.wayline.domain.Ride;
import com.codenameone.examples.wayline.domain.RideActivity;
import com.codenameone.examples.wayline.repository.DriverStateRepository;
import com.codenameone.examples.wayline.repository.PreferenceRepository;
import com.codenameone.examples.wayline.repository.ProfileRepository;
import com.codenameone.examples.wayline.repository.RideActivityRepository;
import com.codenameone.examples.wayline.repository.RideRepository;
import com.codenameone.examples.wayline.ride.Rides;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The push notifications a ride sends, and the Live Activity it keeps up to
/// date on the rider's lock screen.
///
/// [Rides] calls [#rideChanged] once a change has been committed, the same
/// moment it tells the live channel; [com.codenameone.examples.wayline.driver.Drivers]
/// calls [#driverMoved] for every position a driver on a ride reports. A
/// notification is a courtesy on top of what the app reads from the server,
/// so nothing here ever fails the change it is about: whatever goes wrong is
/// logged.
///
/// The live channel already tells an app that is open. A notification is for
/// the phone in a pocket: the rider whose driver has arrived, the driver who
/// has been offered a ride.
///
/// The **Notifications** switches in the app's settings decide what is said:
/// ride updates for every step of a ride, receipts for its end. A switch that
/// is off silences the notification and nothing else, so the Live Activity
/// still follows the ride and still ends with it.
@Component
public class RidePushes {
    private static final String RIDER = "rider";
    private static final String DRIVER = "driver";

    private final PushSender sender;
    private final PushDevices devices;
    private final RideRepository rides;
    private final RideActivityRepository activities;
    private final ProfileRepository profiles;
    private final DriverStateRepository drivers;
    private final PreferenceRepository preferences;
    private final long activityMillis;
    private final int offerSeconds;

    /// @param activitySeconds how often, at most, a moving car updates the
    ///     rider's Live Activity
    /// @param offerSeconds how long a driver has to answer an offer, which is
    ///     how long the notification of it is worth delivering
    public RidePushes(PushSender sender, PushDevices devices, RideRepository rides,
            RideActivityRepository activities, ProfileRepository profiles,
            DriverStateRepository drivers, PreferenceRepository preferences,
            @Value("${wayline.push.activity.seconds:45}") int activitySeconds,
            @Value("${wayline.offer.seconds:15}") int offerSeconds) {
        this.sender = sender;
        this.devices = devices;
        this.rides = rides;
        this.activities = activities;
        this.profiles = profiles;
        this.drivers = drivers;
        this.preferences = preferences;
        this.activityMillis = activitySeconds * 1000L;
        this.offerSeconds = offerSeconds;
    }

    /// One notification and the devices it goes to.
    static final class Outgoing {
        final List<PushTarget> to;
        final PushNotification message;

        Outgoing(List<PushTarget> to, PushNotification message) {
            this.to = to;
            this.message = message;
        }
    }

    // tag::wayline-push-ride-changed[]
    /// Tells the people concerned about a change to a ride that has been
    /// committed. Called after the transaction, so what it reads is the ride as
    /// it now is, and a phone that opens the app at once finds it so.
    public void rideChanged(String id, RideState state, String rider, String driver) {
        try {
            send(compose(id, state, rider, driver));
        } catch (IOException | RuntimeException err) {
            System.err.println("[push] nothing was sent for ride " + id + " (" + state + "): "
                    + err);
        }
    }
    // end::wayline-push-ride-changed[]

    /// Moves the rider's Live Activity on as the car moves: its minutes to
    /// the pickup or the destination. At most once every
    /// `wayline.push.activity.seconds`, and only for a ride that has one.
    public void driverMoved(String rideId, double lat, double lng) {
        try {
            send(moved(rideId, lat, lng));
        } catch (IOException | RuntimeException err) {
            System.err.println("[push] the Live Activity of ride " + rideId
                    + " was not updated: " + err);
        }
    }

    // tag::wayline-push-activity[]
    /// Records the Live Activity the rider's phone shows `rideId` in. Only for
    /// the ride's rider, and only while a driver is bringing or carrying them:
    /// that is when the activity is on screen.
    @Transactional
    public RideActivityDto attach(String rider, String rideId, RideActivityDto activity)
            throws IOException {
        String activityId = Text.required(activity == null ? null : activity.activityId(), 190,
                "the activity");
        Ride ride = rides.findById(rideId);
        if (ride == null || !ride.getRider().equals(rider)) {
            throw new ResponseStatusException(404, "No such ride");
        }
        if (!meeting(ride.getState())) {
            throw new ResponseStatusException(409, "That ride has no driver on the way");
        }
        RideActivity row = activities.findById(rideId);
        if (row == null) {
            row = new RideActivity();
            row.setRideId(rideId);
            row.setUsername(rider);
            row.setActivityId(activityId);
            row.setSentAt(System.currentTimeMillis());
            activities.insert(row);
        } else {
            row.setActivityId(activityId);
            row.setSentAt(System.currentTimeMillis());
        }
        return new RideActivityDto(activityId);
    }
    // end::wayline-push-activity[]

    private void send(List<Outgoing> outgoing) {
        for (Outgoing each : outgoing) {
            if (!each.to.isEmpty()) {
                sender.send(each.to, each.message);
            }
        }
    }

    /// What to send about a change, read and decided in one transaction. A
    /// ride that has ended also forgets its Live Activity here.
    @Transactional
    private List<Outgoing> compose(String id, RideState state, String rider, String driver)
            throws IOException {
        List<Outgoing> out = new ArrayList<Outgoing>();
        Ride ride = rides.findById(id);
        if (ride == null) {
            return out;
        }
        Profile who = driver == null || driver.length() == 0 ? null : profiles.findById(driver);
        String name = who == null ? "Your driver" : who.getDisplayName();
        String car = who == null ? "" : describe(who);
        RideActivity activity = activities.findById(id);
        String activityId = activity == null ? null : activity.getActivityId();
        Map<String, Object> surface = null;
        PushNotification.Builder toRider = null;
        PushNotification.Builder toDriver = null;
        // tag::wayline-push-messages[]
        switch (state) {
            case OFFERED:
                toDriver = message(id, state, DRIVER, "New ride request",
                        "Pickup at " + ride.getPickupAddress() + ". Open Wayline to accept.")
                        .ttlSeconds(offerSeconds);
                break;
            case ACCEPTED:
                toRider = message(id, state, RIDER, "Your driver is on the way",
                        name + " is coming" + (car.length() > 0 ? " in a " + car : "") + ". About "
                        + eta(ride, driver, false) + " min.");
                break;
            case ARRIVED:
                toRider = message(id, state, RIDER, "Your driver is here",
                        car.length() > 0 ? "Look for " + car + "." : name + " is waiting for you.");
                surface = update(activityId, state(state, name, who, 0));
                break;
            case IN_PROGRESS:
                toRider = message(id, state, RIDER, "Your trip has started",
                        "Arriving at " + ride.getDropoffAddress() + " in about "
                        + eta(ride, driver, true) + " min.");
                surface = update(activityId, state(state, name, who, eta(ride, driver, true)));
                break;
            case COMPLETED:
                toRider = message(id, state, RIDER, "You've arrived",
                        "Thanks for riding with Wayline. Your receipt is in the app.");
                surface = end(activityId, state(state, name, who, 0), false);
                break;
            case CANCELLED_BY_DRIVER:
                toRider = message(id, state, RIDER, "Your driver canceled",
                        "Sorry about that. Request again to find another driver.");
                surface = end(activityId, null, true);
                break;
            case CANCELLED_BY_ADMIN:
                toRider = message(id, state, RIDER, "Your ride was canceled",
                        "Wayline support canceled this ride.");
                toDriver = driver == null || driver.length() == 0 ? null
                        : message(id, state, DRIVER, "Ride canceled",
                        "Wayline support canceled this ride.");
                surface = end(activityId, null, true);
                break;
            case CANCELLED_BY_RIDER:
                toDriver = driver == null || driver.length() == 0 ? null
                        : message(id, state, DRIVER, "Ride canceled",
                        "The rider canceled. You're available for new requests.");
                surface = end(activityId, null, true);
                break;
            case NO_DRIVERS:
                toRider = message(id, state, RIDER, "No drivers available",
                        "Nobody could take your ride right now. Try again in a few minutes.");
                break;
            default:
                break;
        }
        // end::wayline-push-messages[]
        if (!state.isActive() && activity != null) {
            activities.deleteByRideId(id);
        }
        if (toRider != null && !wants(rider, RideState.COMPLETED == state)) {
            toRider = null;
        }
        if (toDriver != null && !wants(driver, false)) {
            toDriver = null;
        }
        if (toRider != null) {
            if (surface != null) {
                toRider.surface(surface);
            }
            out.add(new Outgoing(devices.targetsOf(rider), toRider.build()));
        } else if (surface != null) {
            // Nothing to say to the rider, but their lock screen still has to
            // let go of the ride.
            out.add(new Outgoing(devices.targetsOf(rider), quiet(id, state).surface(surface)
                    .build()));
        }
        if (toDriver != null) {
            out.add(new Outgoing(devices.targetsOf(driver), toDriver.build()));
        }
        return out;
    }

    // tag::wayline-push-preferences[]
    /// Whether `username` asked to be told: of receipts when `receipt`, of
    /// ride updates otherwise. An account that never saved its settings gets
    /// both, as the settings screen shows it.
    private boolean wants(String username, boolean receipt) throws IOException {
        Preference row = preferences.findById(username);
        if (row == null) {
            return true;
        }
        return receipt ? row.isNotifyReceipts() : row.isNotifyRides();
    }
    // end::wayline-push-preferences[]

    /// The silent update of a moving car, when one is due.
    @Transactional
    private List<Outgoing> moved(String rideId, double lat, double lng) throws IOException {
        List<Outgoing> out = new ArrayList<Outgoing>();
        RideActivity activity = activities.findById(rideId);
        if (activity == null) {
            return out;
        }
        long now = System.currentTimeMillis();
        // A conditional update, so that two reports arriving together send one
        // update between them.
        if (activities.claim(rideId, now, now - activityMillis) != 1) {
            return out;
        }
        Ride ride = rides.findById(rideId);
        if (ride == null || !(RideState.ACCEPTED == ride.getState()
                || RideState.IN_PROGRESS == ride.getState())) {
            return out;
        }
        Profile who = profiles.findById(ride.getDriver());
        String name = who == null ? "Your driver" : who.getDisplayName();
        boolean toDropoff = RideState.IN_PROGRESS == ride.getState();
        int minutes = toDropoff
                ? Rides.etaMinutes(lat, lng, ride.getDropoffLat(), ride.getDropoffLng())
                : Rides.etaMinutes(lat, lng, ride.getPickupLat(), ride.getPickupLng());
        out.add(new Outgoing(devices.targetsOf(ride.getRider()), quiet(rideId, ride.getState())
                .surface(PushNotification.liveUpdate(activity.getActivityId(),
                        state(ride.getState(), name, who, minutes)))
                .build()));
        return out;
    }

    /// A notification for the rider or the driver of a ride: `role` is which,
    /// for the app to open the side it is about.
    private static PushNotification.Builder message(String id, RideState state, String role,
            String title, String body) {
        return PushNotification.builder()
                .title(title)
                .body(body)
                .collapseKey("ride-" + id + "-" + role)
                .data("rideId", id)
                .data("state", state.name())
                .data("role", role);
    }

    /// A message the phone acts on without showing anything.
    private static PushNotification.Builder quiet(String id, RideState state) {
        return PushNotification.builder()
                .silent(true)
                .collapseKey("ride-" + id + "-activity")
                .data("rideId", id)
                .data("state", state.name())
                .data("role", RIDER);
    }

    private static Map<String, Object> update(String activityId, Map<String, Object> state) {
        return activityId == null ? null : PushNotification.liveUpdate(activityId, state);
    }

    private static Map<String, Object> end(String activityId, Map<String, Object> state,
            boolean dismiss) {
        return activityId == null ? null : PushNotification.liveEnd(activityId, state, dismiss);
    }

    /// The Live Activity's state, in the keys the app laid it out with.
    static Map<String, Object> state(RideState state, String name, Profile who, int minutes) {
        Map<String, Object> out = new HashMap<String, Object>();
        String status;
        double progress;
        if (RideState.ARRIVED == state) {
            status = "Your driver is here";
            progress = RideActivityKeys.ARRIVED;
        } else if (RideState.IN_PROGRESS == state) {
            status = "On the trip";
            progress = RideActivityKeys.IN_TRIP;
        } else if (RideState.COMPLETED == state) {
            status = "You've arrived";
            progress = RideActivityKeys.DONE;
        } else {
            status = "On the way";
            progress = RideActivityKeys.ON_THE_WAY;
        }
        out.put(RideActivityKeys.STATUS, status);
        out.put(RideActivityKeys.PROGRESS, Double.valueOf(progress));
        out.put(RideActivityKeys.DRIVER, name);
        out.put(RideActivityKeys.CAR, who == null ? "" : who.getVehicle());
        out.put(RideActivityKeys.PLATE, who == null ? "" : who.getPlate());
        out.put(RideActivityKeys.ETA, minutes > 0 ? minutes + " min" : "");
        return out;
    }

    private int eta(Ride ride, String driver, boolean toDropoff) {
        DriverState car = driver == null || driver.length() == 0 ? null : drivers.findById(driver);
        if (car == null) {
            return 1;
        }
        return toDropoff
                ? Rides.etaMinutes(car.getLat(), car.getLng(), ride.getDropoffLat(),
                        ride.getDropoffLng())
                : Rides.etaMinutes(car.getLat(), car.getLng(), ride.getPickupLat(),
                        ride.getPickupLng());
    }

    private static boolean meeting(RideState state) {
        return RideState.ACCEPTED == state || RideState.ARRIVED == state
                || RideState.IN_PROGRESS == state;
    }

    private static String describe(Profile who) {
        String vehicle = who.getVehicle() == null ? "" : who.getVehicle().trim();
        String plate = who.getPlate() == null ? "" : who.getPlate().trim();
        if (vehicle.length() == 0) {
            return plate;
        }
        return plate.length() == 0 ? vehicle : vehicle + ", " + plate;
    }
}
