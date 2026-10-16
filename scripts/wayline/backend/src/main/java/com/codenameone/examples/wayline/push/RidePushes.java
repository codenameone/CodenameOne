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
package com.codenameone.examples.wayline.push;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushTarget;
import com.codename1.orm.repository.PageRequest;
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

    /// How long an end the phone was not told of is tried again. iOS ends a
    /// Live Activity on its own within twelve hours, so past that there is
    /// nothing left on the lock screen to end.
    private static final long END_RETRY_MILLIS = 12L * 60 * 60 * 1000;

    /// How old an end has to be before the sweep sends it again: the call
    /// that ended the ride is still sending it until then.
    private static final long END_GRACE_MILLIS = 60000L;

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
            // Forgotten only once the provider took the end. If it threw, the
            // row stays marked ended and endLeftBehind sends the end again: the
            // ride's transition is committed and is never retried, so nothing
            // else would, and the lock screen would keep showing the ride.
            if (!state.isActive()) {
                forget(id);
            }
        } catch (IOException | RuntimeException err) {
            System.err.println("[push] nothing was sent for ride " + id + " (" + state + "): "
                    + err);
        }
    }
    // end::wayline-push-ride-changed[]

    /// Sends again the ends of Live Activities the push provider did not take
    /// when their ride ended, and forgets those too old to be on screen.
    ///
    /// Not a transaction itself, as `Rides.settleLeftBehind` is not: the
    /// provider is called with none open, and what went wrong with the first
    /// end that failed again is reported once the rest have been tried.
    @Scheduled(fixedDelay = 60000, initialDelay = 50000, lock = "wayline-activity-ends")
    public void endLeftBehind() throws IOException {
        long now = System.currentTimeMillis();
        dropEndedBefore(now - END_RETRY_MILLIS);
        List<RideActivity> left = leftBehind(now - END_GRACE_MILLIS);
        Exception failed = null;
        for (int iter = 0; iter < left.size(); iter++) {
            String id = left.get(iter).getRideId();
            try {
                send(ending(id));
                forget(id);
            } catch (IOException err) {
                failed = failed == null ? err : failed;
            } catch (RuntimeException err) {
                failed = failed == null ? err : failed;
            }
        }
        if (failed instanceof IOException) {
            throw (IOException) failed;
        }
        if (failed instanceof RuntimeException) {
            throw (RuntimeException) failed;
        }
    }

    @Transactional
    private void dropEndedBefore(long before) {
        activities.dropEndedBefore(before);
    }

    @Transactional(readOnly = true)
    private List<RideActivity> leftBehind(long before) {
        return activities.leftBehind(before, new PageRequest(0, 50));
    }

    @Transactional
    private void forget(String id) {
        activities.deleteByRideId(id);
    }

    /// The end of the Live Activity of ride `id`, sent quietly: the rider
    /// was told of the ride's end, or chose not to be, when it happened.
    @Transactional(readOnly = true)
    private List<Outgoing> ending(String id) throws IOException {
        List<Outgoing> out = new ArrayList<Outgoing>();
        RideActivity activity = activities.findById(id);
        Ride ride = rides.findById(id);
        if (activity == null || ride == null) {
            return out;
        }
        String driver = ride.getDriver();
        Profile who = driver == null || driver.length() == 0 ? null : profiles.findById(driver);
        String name = who == null ? "Your driver" : who.getDisplayName();
        out.add(new Outgoing(devices.targetsOf(ride.getRider()), quiet(id, ride.getState())
                .surface(endOf(activity.getActivityId(), ride.getState(), name, who)).build()));
        return out;
    }

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
        // The state check and the insert have to be one step against the ride
        // ending: checked from the ride read above, a driver completing it or
        // an admin cancelling it could commit in between, its compose() would
        // find no activity to end, and this one would be inserted after it and
        // stay on the lock screen. holdWhile checks the state in the database
        // and keeps the ride's row locked until this commits, so an ending
        // either happened first (409 here) or waits and then finds the row.
        if (rides.holdWhile(rideId, rider, MEETING) == 0) {
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
    /// ride that has ended marks its Live Activity ended here; [#rideChanged]
    /// forgets it once the end has been sent.
    @Transactional
    private List<Outgoing> compose(String id, RideState state, String rider, String driver)
            throws IOException {
        List<Outgoing> out = new ArrayList<Outgoing>();
        Ride ride = rides.findById(id);
        if (ride == null) {
            return out;
        }
        // The change this was called for may no longer be the ride's latest.
        // Each transition commits and then composes on its own thread, so two
        // in quick succession can compose out of order: an acceptance commits,
        // the rider cancels and that commits, and only then does the
        // acceptance get here. Switching on `state` would tell the rider their
        // driver is on the way to a ride they just cancelled. When what was
        // committed since differs from `state`, nothing is said: the later
        // transition calls this with its own state and says what is true.
        // An offer is compared by its driver too, because a lapsed offer goes
        // back to REQUESTED and on to OFFERED again to somebody else.
        //
        // Nothing is cleaned up here either. A ride that has ended since is
        // ended by that later call, which reads the Live Activity, sends its
        // live-end and then forgets it; deleting the row here first would
        // leave that call nothing to end and the activity on the lock screen.
        if (ride.getState() != state || (RideState.OFFERED == state
                && (driver == null || !driver.equals(ride.getDriver())))) {
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
                surface = endOf(activityId, state, name, who);
                break;
            case CANCELLED_BY_DRIVER:
                toRider = message(id, state, RIDER, "Your driver canceled",
                        "Sorry about that. Request again to find another driver.");
                surface = endOf(activityId, state, name, who);
                break;
            case CANCELLED_BY_ADMIN:
                toRider = message(id, state, RIDER, "Your ride was canceled",
                        "Wayline support canceled this ride.");
                toDriver = driver == null || driver.length() == 0 ? null
                        : message(id, state, DRIVER, "Ride canceled",
                        "Wayline support canceled this ride.");
                surface = endOf(activityId, state, name, who);
                break;
            case CANCELLED_BY_RIDER:
                toDriver = driver == null || driver.length() == 0 ? null
                        : message(id, state, DRIVER, "Ride canceled",
                        "The rider canceled. You're available for new requests.");
                surface = endOf(activityId, state, name, who);
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
            activities.markEnded(id, System.currentTimeMillis());
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

    /// The end of a Live Activity whose ride ended in `state`: a completed
    /// ride stays on the lock screen a while showing its arrival, a cancelled
    /// one goes at once.
    private static Map<String, Object> endOf(String activityId, RideState state, String name,
            Profile who) {
        return RideState.COMPLETED == state
                ? end(activityId, state(state, name, who, 0), false)
                : end(activityId, null, true);
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

    private static final RideState[] MEETING = {
        RideState.ACCEPTED, RideState.ARRIVED, RideState.IN_PROGRESS
    };

    private static String describe(Profile who) {
        String vehicle = who.getVehicle() == null ? "" : who.getVehicle().trim();
        String plate = who.getPlate() == null ? "" : who.getPlate().trim();
        if (vehicle.length() == 0) {
            return plate;
        }
        return plate.length() == 0 ? vehicle : vehicle + ", " + plate;
    }
}
