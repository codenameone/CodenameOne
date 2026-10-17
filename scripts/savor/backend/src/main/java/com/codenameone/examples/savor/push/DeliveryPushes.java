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
package com.codenameone.examples.savor.push;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushTarget;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.savor.Text;
import com.codenameone.examples.savor.api.DeliveryActivityKeys;
import com.codenameone.examples.savor.api.DeliveryActivityDto;
import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.domain.CourierState;
import com.codenameone.examples.savor.domain.Preference;
import com.codenameone.examples.savor.domain.Profile;
import com.codenameone.examples.savor.domain.Delivery;
import com.codenameone.examples.savor.domain.DeliveryActivity;
import com.codenameone.examples.savor.repository.CourierStateRepository;
import com.codenameone.examples.savor.repository.PreferenceRepository;
import com.codenameone.examples.savor.repository.ProfileRepository;
import com.codenameone.examples.savor.repository.DeliveryActivityRepository;
import com.codenameone.examples.savor.repository.DeliveryRepository;
import com.codenameone.examples.savor.delivery.Deliveries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The push notifications a delivery sends, and the Live Activity it keeps up to
/// date on the customer's lock screen.
///
/// [Deliveries] calls [#deliveryChanged] once a change has been committed, the same
/// moment it tells the live channel; [com.codenameone.examples.savor.courier.Couriers]
/// calls [#courierMoved] for every position a courier on a delivery reports. A
/// notification is a courtesy on top of what the app reads from the server,
/// so nothing here ever fails the change it is about: whatever goes wrong is
/// logged.
///
/// The live channel already tells an app that is open. A notification is for
/// the phone in a pocket: the customer whose courier has arrived, the courier who
/// has been offered a delivery.
///
/// The **Notifications** switches in the app's settings decide what is said:
/// delivery updates for every step of a delivery, receipts for its end. A switch that
/// is off silences the notification and nothing else, so the Live Activity
/// still follows the delivery and still ends with it.
@Component
public class DeliveryPushes {
    private static final String CUSTOMER = "customer";
    private static final String COURIER = "courier";

    /// How long an end the phone was not told of is tried again. iOS ends a
    /// Live Activity on its own within twelve hours, so past that there is
    /// nothing left on the lock screen to end.
    private static final long END_RETRY_MILLIS = 12L * 60 * 60 * 1000;

    /// How old an end has to be before the sweep sends it again: the call
    /// that ended the delivery is still sending it until then.
    private static final long END_GRACE_MILLIS = 60000L;

    private final PushSender sender;
    private final PushDevices devices;
    private final DeliveryRepository deliveries;
    private final DeliveryActivityRepository activities;
    private final ProfileRepository profiles;
    private final CourierStateRepository couriers;
    private final PreferenceRepository preferences;
    private final long activityMillis;
    private final int offerSeconds;

    /// @param activitySeconds how often, at most, a moving car updates the
    ///     customer's Live Activity
    /// @param offerSeconds how long a courier has to answer an offer, which is
    ///     how long the notification of it is worth delivering
    public DeliveryPushes(PushSender sender, PushDevices devices, DeliveryRepository deliveries,
            DeliveryActivityRepository activities, ProfileRepository profiles,
            CourierStateRepository couriers, PreferenceRepository preferences,
            @Value("${savor.push.activity.seconds:45}") int activitySeconds,
            @Value("${savor.offer.seconds:15}") int offerSeconds) {
        this.sender = sender;
        this.devices = devices;
        this.deliveries = deliveries;
        this.activities = activities;
        this.profiles = profiles;
        this.couriers = couriers;
        this.preferences = preferences;
        this.activityMillis = activitySeconds * 1000L;
        this.offerSeconds = offerSeconds;
    }

    /// One notification and the devices it goes to.
    static final class Outgoing {
        final List<PushTarget> to;
        final PushNotification message;
        /// The delivery whose Live Activity this ends, forgotten once the push
        /// service takes it; null for every other message.
        final String ends;

        Outgoing(List<PushTarget> to, PushNotification message) {
            this(to, message, null);
        }

        Outgoing(List<PushTarget> to, PushNotification message, String ends) {
            this.to = to;
            this.message = message;
            this.ends = ends;
        }
    }

    // tag::savor-push-delivery-changed[]
    /// Tells the people concerned about a change to a delivery that has been
    /// committed. Called after the transaction, so what it reads is the delivery as
    /// it now is, and a phone that opens the app at once finds it so.
    public void deliveryChanged(String id, DeliveryState state, String customer, String courier) {
        try {
            send(compose(id, state, customer, courier));
        } catch (IOException | RuntimeException err) {
            System.err.println("[push] nothing was sent for delivery " + id + " (" + state + "): "
                    + err);
        }
    }
    // end::savor-push-delivery-changed[]

    /// Sends again the ends of Live Activities the push provider did not take
    /// when their delivery ended, and forgets those too old to be on screen.
    ///
    /// Not a transaction itself, as `Deliveries.settleLeftBehind` is not: the
    /// provider is called with none open, and what went wrong with the first
    /// end that failed again is reported once the rest have been tried.
    @Scheduled(fixedDelay = 60000, initialDelay = 50000, lock = "savor-activity-ends")
    public void endLeftBehind() throws IOException {
        long now = System.currentTimeMillis();
        dropEndedBefore(now - END_RETRY_MILLIS);
        List<DeliveryActivity> left = leftBehind(now - END_GRACE_MILLIS);
        Exception failed = null;
        for (int iter = 0; iter < left.size(); iter++) {
            String id = left.get(iter).getDeliveryId();
            try {
                send(ending(id));
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
    private List<DeliveryActivity> leftBehind(long before) {
        return activities.leftBehind(before, new PageRequest(0, 50));
    }

    @Transactional
    private void forget(String id) {
        activities.deleteByDeliveryId(id);
    }

    /// The end of the Live Activity of delivery `id`, sent quietly: the customer
    /// was told of the delivery's end, or chose not to be, when it happened.
    @Transactional(readOnly = true)
    private List<Outgoing> ending(String id) throws IOException {
        List<Outgoing> out = new ArrayList<Outgoing>();
        DeliveryActivity activity = activities.findById(id);
        Delivery delivery = deliveries.findById(id);
        if (activity == null || delivery == null) {
            return out;
        }
        String courier = delivery.getCourier();
        Profile who = courier == null || courier.length() == 0 ? null : profiles.findById(courier);
        String name = who == null ? "Your courier" : who.getDisplayName();
        out.add(new Outgoing(devices.targetsOf(delivery.getCustomer()), quiet(id, delivery.getState())
                .surface(endOf(activity.getActivityId(), delivery.getState(), name, who)).build(), id));
        return out;
    }

    /// Moves the customer's Live Activity on as the car moves: its minutes to
    /// the pickup or the destination. At most once every
    /// `savor.push.activity.seconds`, and only for a delivery that has one.
    public void courierMoved(String deliveryId, double lat, double lng) {
        try {
            send(moved(deliveryId, lat, lng));
        } catch (IOException | RuntimeException err) {
            System.err.println("[push] the Live Activity of delivery " + deliveryId
                    + " was not updated: " + err);
        }
    }

    // tag::savor-push-activity[]
    /// Records the Live Activity the customer's phone shows `deliveryId` in. Only for
    /// the delivery's customer, and only while a courier is bringing or carrying them:
    /// that is when the activity is on screen.
    @Transactional
    public DeliveryActivityDto attach(String customer, String deliveryId, DeliveryActivityDto activity)
            throws IOException {
        String activityId = Text.required(activity == null ? null : activity.activityId(), 190,
                "the activity");
        Delivery delivery = deliveries.findById(deliveryId);
        if (delivery == null || !delivery.getCustomer().equals(customer)) {
            throw new ResponseStatusException(404, "No such delivery");
        }
        // The state check and the insert have to be one step against the delivery
        // ending: checked from the delivery read above, a courier completing it or
        // an admin cancelling it could commit in between, its compose() would
        // find no activity to end, and this one would be inserted after it and
        // stay on the lock screen. holdWhile checks the state in the database
        // and keeps the delivery's row locked until this commits, so an ending
        // either happened first (409 here) or waits and then finds the row.
        if (deliveries.holdWhile(deliveryId, customer, MEETING) == 0) {
            throw new ResponseStatusException(409, "That delivery has no courier on the way");
        }
        // One activity per delivery, the last one attached. A customer following
        // their own delivery on two phones at once is not a case the sample
        // serves: the earlier phone's activity stops moving and the system
        // removes it. A row per device is listed in the implementor's guide
        // among what a product adds.
        DeliveryActivity row = activities.findById(deliveryId);
        if (row == null) {
            row = new DeliveryActivity();
            row.setDeliveryId(deliveryId);
            row.setUsername(customer);
            row.setActivityId(activityId);
            row.setSentAt(System.currentTimeMillis());
            activities.insert(row);
        } else {
            row.setActivityId(activityId);
            row.setSentAt(System.currentTimeMillis());
        }
        return new DeliveryActivityDto(activityId);
    }
    // end::savor-push-activity[]

    /// Hands each message to the sender. The end of a Live Activity forgets
    /// it only once the push service has taken the message, which the sender
    /// says later, on its own thread. Until then the row stays marked ended,
    /// so a refusal, an outage or a server stopped first leaves it for
    /// [#endLeftBehind]: the delivery's transition is committed and never retried,
    /// so nothing else would end the activity on the lock screen.
    private void send(List<Outgoing> outgoing) {
        for (Outgoing each : outgoing) {
            final String ends = each.ends;
            if (each.to.isEmpty()) {
                // No phone left to tell: nothing will ever take it.
                if (ends != null) {
                    forget(ends);
                }
                continue;
            }
            sender.send(each.to, each.message, ends == null ? null : () -> forget(ends));
        }
    }

    /// What to send about a change, read and decided in one transaction. A
    /// delivery that has ended marks its Live Activity ended here; [#deliveryChanged]
    /// forgets it once the end has been sent.
    @Transactional
    private List<Outgoing> compose(String id, DeliveryState state, String customer, String courier)
            throws IOException {
        List<Outgoing> out = new ArrayList<Outgoing>();
        Delivery delivery = deliveries.findById(id);
        if (delivery == null) {
            return out;
        }
        // The change this was called for may no longer be the delivery's latest.
        // Each transition commits and then composes on its own thread, so two
        // in quick succession can compose out of order: an acceptance commits,
        // the customer cancels and that commits, and only then does the
        // acceptance get here. Switching on `state` would tell the customer their
        // courier is on the way to a delivery they just cancelled. When what was
        // committed since differs from `state`, nothing is said: the later
        // transition calls this with its own state and says what is true.
        // An offer is compared by its courier too, because a lapsed offer goes
        // back to REQUESTED and on to OFFERED again to somebody else.
        //
        // Nothing is cleaned up here either. A delivery that has ended since is
        // ended by that later call, which reads the Live Activity, sends its
        // live-end and then forgets it; deleting the row here first would
        // leave that call nothing to end and the activity on the lock screen.
        if (delivery.getState() != state || (DeliveryState.OFFERED == state
                && (courier == null || !courier.equals(delivery.getCourier())))) {
            return out;
        }
        Profile who = courier == null || courier.length() == 0 ? null : profiles.findById(courier);
        String name = who == null ? "Your courier" : who.getDisplayName();
        String car = who == null ? "" : describe(who);
        DeliveryActivity activity = activities.findById(id);
        String activityId = activity == null ? null : activity.getActivityId();
        Map<String, Object> surface = null;
        PushNotification.Builder toCustomer = null;
        PushNotification.Builder toCourier = null;
        // tag::savor-push-messages[]
        switch (state) {
            case OFFERED:
                toCourier = message(id, state, COURIER, "New delivery request",
                        "Pickup at " + delivery.getPickupAddress() + ". Open Savor to accept.")
                        .ttlSeconds(offerSeconds);
                break;
            case ACCEPTED:
                toCustomer = message(id, state, CUSTOMER, "Your courier is on the way",
                        name + " is coming" + (car.length() > 0 ? " in a " + car : "") + ". About "
                        + eta(delivery, courier, false) + " min.");
                break;
            case ARRIVED:
                toCustomer = message(id, state, CUSTOMER, "Your courier is here",
                        car.length() > 0 ? "Look for " + car + "." : name + " is waiting for you.");
                surface = update(activityId, state(state, name, who, 0));
                break;
            case IN_PROGRESS:
                toCustomer = message(id, state, CUSTOMER, "Your trip has started",
                        "Arriving at " + delivery.getDropoffAddress() + " in about "
                        + eta(delivery, courier, true) + " min.");
                surface = update(activityId, state(state, name, who, eta(delivery, courier, true)));
                break;
            case COMPLETED:
                toCustomer = message(id, state, CUSTOMER, "You've arrived",
                        "Thanks for riding with Savor. Your receipt is in the app.");
                surface = endOf(activityId, state, name, who);
                break;
            case CANCELLED_BY_COURIER:
                toCustomer = message(id, state, CUSTOMER, "Your courier canceled",
                        "Sorry about that. Request again to find another courier.");
                surface = endOf(activityId, state, name, who);
                break;
            case CANCELLED_BY_ADMIN:
                toCustomer = message(id, state, CUSTOMER, "Your delivery was canceled",
                        "Savor support canceled this delivery.");
                toCourier = courier == null || courier.length() == 0 ? null
                        : message(id, state, COURIER, "Delivery canceled",
                        "Savor support canceled this delivery.");
                surface = endOf(activityId, state, name, who);
                break;
            case CANCELLED_BY_CUSTOMER:
                toCourier = courier == null || courier.length() == 0 ? null
                        : message(id, state, COURIER, "Delivery canceled",
                        "The customer canceled. You're available for new requests.");
                surface = endOf(activityId, state, name, who);
                break;
            case NO_COURIERS:
                toCustomer = message(id, state, CUSTOMER, "No couriers available",
                        "Nobody could take your delivery right now. Try again in a few minutes.");
                break;
            default:
                break;
        }
        // end::savor-push-messages[]
        if (!state.isActive() && activity != null) {
            activities.markEnded(id, System.currentTimeMillis());
        }
        if (toCustomer != null && !wants(customer, DeliveryState.COMPLETED == state)) {
            toCustomer = null;
        }
        if (toCourier != null && !wants(courier, false)) {
            toCourier = null;
        }
        if (toCustomer != null) {
            if (surface != null) {
                toCustomer.surface(surface);
            }
            out.add(new Outgoing(devices.targetsOf(customer), toCustomer.build(),
                    surface != null && !state.isActive() ? id : null));
        } else if (surface != null) {
            // Nothing to say to the customer, but their lock screen still has to
            // let go of the delivery.
            out.add(new Outgoing(devices.targetsOf(customer), quiet(id, state).surface(surface)
                    .build(), state.isActive() ? null : id));
        }
        if (toCourier != null) {
            out.add(new Outgoing(devices.targetsOf(courier), toCourier.build()));
        }
        return out;
    }

    // tag::savor-push-preferences[]
    /// Whether `username` asked to be told: of receipts when `receipt`, of
    /// delivery updates otherwise. An account that never saved its settings gets
    /// both, as the settings screen shows it.
    private boolean wants(String username, boolean receipt) throws IOException {
        Preference row = preferences.findById(username);
        if (row == null) {
            return true;
        }
        return receipt ? row.isNotifyReceipts() : row.isNotifyDeliveries();
    }
    // end::savor-push-preferences[]

    /// The silent update of a moving car, when one is due.
    @Transactional
    private List<Outgoing> moved(String deliveryId, double lat, double lng) throws IOException {
        List<Outgoing> out = new ArrayList<Outgoing>();
        DeliveryActivity activity = activities.findById(deliveryId);
        if (activity == null) {
            return out;
        }
        long now = System.currentTimeMillis();
        // A conditional update, so that two reports arriving together send one
        // update between them.
        if (activities.claim(deliveryId, now, now - activityMillis) != 1) {
            return out;
        }
        Delivery delivery = deliveries.findById(deliveryId);
        if (delivery == null || !(DeliveryState.ACCEPTED == delivery.getState()
                || DeliveryState.IN_PROGRESS == delivery.getState())) {
            return out;
        }
        Profile who = profiles.findById(delivery.getCourier());
        String name = who == null ? "Your courier" : who.getDisplayName();
        boolean toDropoff = DeliveryState.IN_PROGRESS == delivery.getState();
        int minutes = toDropoff
                ? Deliveries.etaMinutes(lat, lng, delivery.getDropoffLat(), delivery.getDropoffLng())
                : Deliveries.etaMinutes(lat, lng, delivery.getPickupLat(), delivery.getPickupLng());
        out.add(new Outgoing(devices.targetsOf(delivery.getCustomer()), quiet(deliveryId, delivery.getState())
                .surface(PushNotification.liveUpdate(activity.getActivityId(),
                        state(delivery.getState(), name, who, minutes)))
                .build()));
        return out;
    }

    /// A notification for the customer or the courier of a delivery: `role` is which,
    /// for the app to open the side it is about.
    private static PushNotification.Builder message(String id, DeliveryState state, String role,
            String title, String body) {
        return PushNotification.builder()
                .title(title)
                .body(body)
                .collapseKey("delivery-" + id + "-" + role)
                .data("deliveryId", id)
                .data("state", state.name())
                .data("role", role);
    }

    /// A message the phone acts on without showing anything.
    private static PushNotification.Builder quiet(String id, DeliveryState state) {
        return PushNotification.builder()
                .silent(true)
                .collapseKey("delivery-" + id + "-activity")
                .data("deliveryId", id)
                .data("state", state.name())
                .data("role", CUSTOMER);
    }

    private static Map<String, Object> update(String activityId, Map<String, Object> state) {
        return activityId == null ? null : PushNotification.liveUpdate(activityId, state);
    }

    private static Map<String, Object> end(String activityId, Map<String, Object> state,
            boolean dismiss) {
        return activityId == null ? null : PushNotification.liveEnd(activityId, state, dismiss);
    }

    /// The end of a Live Activity whose delivery ended in `state`: a completed
    /// delivery stays on the lock screen a while showing its arrival, a cancelled
    /// one goes at once.
    private static Map<String, Object> endOf(String activityId, DeliveryState state, String name,
            Profile who) {
        return DeliveryState.COMPLETED == state
                ? end(activityId, state(state, name, who, 0), false)
                : end(activityId, null, true);
    }

    /// The Live Activity's state, in the keys the app laid it out with.
    static Map<String, Object> state(DeliveryState state, String name, Profile who, int minutes) {
        Map<String, Object> out = new HashMap<String, Object>();
        String status;
        double progress;
        if (DeliveryState.ARRIVED == state) {
            status = "Your courier is here";
            progress = DeliveryActivityKeys.ARRIVED;
        } else if (DeliveryState.IN_PROGRESS == state) {
            status = "On the trip";
            progress = DeliveryActivityKeys.IN_TRIP;
        } else if (DeliveryState.COMPLETED == state) {
            status = "You've arrived";
            progress = DeliveryActivityKeys.DONE;
        } else {
            status = "On the way";
            progress = DeliveryActivityKeys.ON_THE_WAY;
        }
        out.put(DeliveryActivityKeys.STATUS, status);
        out.put(DeliveryActivityKeys.PROGRESS, Double.valueOf(progress));
        out.put(DeliveryActivityKeys.COURIER, name);
        out.put(DeliveryActivityKeys.CAR, who == null ? "" : who.getVehicle());
        out.put(DeliveryActivityKeys.PLATE, who == null ? "" : who.getPlate());
        out.put(DeliveryActivityKeys.ETA, minutes > 0 ? minutes + " min" : "");
        return out;
    }

    private int eta(Delivery delivery, String courier, boolean toDropoff) {
        CourierState car = courier == null || courier.length() == 0 ? null : couriers.findById(courier);
        if (car == null) {
            return 1;
        }
        return toDropoff
                ? Deliveries.etaMinutes(car.getLat(), car.getLng(), delivery.getDropoffLat(),
                        delivery.getDropoffLng())
                : Deliveries.etaMinutes(car.getLat(), car.getLng(), delivery.getPickupLat(),
                        delivery.getPickupLng());
    }

    private static final DeliveryState[] MEETING = {
        DeliveryState.ACCEPTED, DeliveryState.ARRIVED, DeliveryState.IN_PROGRESS
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
