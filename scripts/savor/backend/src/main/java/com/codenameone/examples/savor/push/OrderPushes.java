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
import com.codenameone.examples.savor.api.DeliveryActivityDto;
import com.codenameone.examples.savor.api.DeliveryActivityKeys;
import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.api.OrderState;
import com.codenameone.examples.savor.delivery.Deliveries;
import com.codenameone.examples.savor.domain.CourierState;
import com.codenameone.examples.savor.domain.CustomerOrder;
import com.codenameone.examples.savor.domain.Delivery;
import com.codenameone.examples.savor.domain.DeliveryActivity;
import com.codenameone.examples.savor.domain.Preference;
import com.codenameone.examples.savor.domain.Profile;
import com.codenameone.examples.savor.domain.RestaurantStaff;
import com.codenameone.examples.savor.repository.CourierStateRepository;
import com.codenameone.examples.savor.repository.CustomerOrderRepository;
import com.codenameone.examples.savor.repository.DeliveryActivityRepository;
import com.codenameone.examples.savor.repository.DeliveryRepository;
import com.codenameone.examples.savor.repository.PreferenceRepository;
import com.codenameone.examples.savor.repository.ProfileRepository;
import com.codenameone.examples.savor.repository.RestaurantStaffRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The push notifications an order sends, and the Live Activity it keeps up to
/// date on the customer's lock screen from the moment it is placed to the
/// moment it reaches the door.
///
/// [com.codenameone.examples.savor.order.OrderNews] calls [#orderChanged] once
/// a change to an order has been committed, the same moment it tells the live
/// channel; [Deliveries] calls [#deliveryChanged] for what concerns the courier
/// alone (an offer, a delivery taken back), and
/// [com.codenameone.examples.savor.courier.Couriers] calls [#courierMoved] for
/// every position a courier carrying an order reports. A notification is a
/// courtesy on top of what the app reads from the server, so nothing here ever
/// fails the change it is about: whatever goes wrong is logged.
///
/// The live channel already tells an app that is open. A notification is for
/// the phone in a pocket: the customer whose food is on its way, the kitchen
/// with a new order, the courier with an offer.
///
/// The **Notifications** switches in the app's settings decide what is said:
/// order updates for every step, receipts for its end. A switch that is off
/// silences the notification and nothing else, so the Live Activity still
/// follows the order and still ends with it.
@Component
public class OrderPushes {
    private static final String CUSTOMER = "customer";
    private static final String COURIER = "courier";
    private static final String MERCHANT = "merchant";

    /// How long an end the phone was not told of is tried again. iOS ends a
    /// Live Activity on its own within twelve hours, so past that there is
    /// nothing left on the lock screen to end.
    private static final long END_RETRY_MILLIS = 12L * 60 * 60 * 1000;

    /// How old an end has to be before the sweep sends it again: the call
    /// that ended the order is still sending it until then.
    private static final long END_GRACE_MILLIS = 60000L;

    private static final OrderState[] FOLLOWED = {
        OrderState.PLACED, OrderState.ACCEPTED, OrderState.READY, OrderState.PICKED_UP
    };

    private final PushSender sender;
    private final PushDevices devices;
    private final CustomerOrderRepository orders;
    private final DeliveryRepository deliveries;
    private final RestaurantStaffRepository staff;
    private final DeliveryActivityRepository activities;
    private final ProfileRepository profiles;
    private final CourierStateRepository couriers;
    private final PreferenceRepository preferences;
    private final long activityMillis;
    private final int offerSeconds;

    /// @param activitySeconds how often, at most, a moving courier updates the
    ///     customer's Live Activity
    /// @param offerSeconds how long a courier has to answer an offer, which is
    ///     how long the notification of it is worth delivering
    public OrderPushes(PushSender sender, PushDevices devices, CustomerOrderRepository orders,
            DeliveryRepository deliveries, RestaurantStaffRepository staff,
            DeliveryActivityRepository activities, ProfileRepository profiles,
            CourierStateRepository couriers, PreferenceRepository preferences,
            @Value("${savor.push.activity.seconds:45}") int activitySeconds,
            @Value("${savor.offer.seconds:15}") int offerSeconds) {
        this.sender = sender;
        this.devices = devices;
        this.orders = orders;
        this.deliveries = deliveries;
        this.staff = staff;
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
        /// The order whose Live Activity this ends, forgotten once the push
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

    // tag::savor-push-order-changed[]
    /// Tells the people concerned about a change to an order that has been
    /// committed. Called after the transaction, so what it reads is the order
    /// as it now is, and a phone that opens the app at once finds it so.
    public void orderChanged(String id, OrderState state) {
        try {
            send(compose(id, state));
        } catch (IOException | RuntimeException err) {
            System.err.println("[push] nothing was sent for order " + id + " (" + state + "): "
                    + err);
        }
    }
    // end::savor-push-order-changed[]

    /// Tells a courier what concerns them alone: an offer, or a delivery an
    /// admin took back. Everything the customer and the kitchen hear of goes
    /// through [#orderChanged].
    public void deliveryChanged(String id, DeliveryState state, String courier) {
        try {
            send(composeForCourier(id, state, courier));
        } catch (IOException | RuntimeException err) {
            System.err.println("[push] nothing was sent for delivery " + id + " (" + state + "): "
                    + err);
        }
    }

    /// Sends again the ends of Live Activities the push provider did not take
    /// when their order ended, and forgets those too old to be on screen.
    ///
    /// Not a transaction itself: the provider is called with none open, and
    /// what went wrong with the first end that failed again is reported once
    /// the rest have been tried.
    @Scheduled(fixedDelay = 60000, initialDelay = 50000, lock = "savor-activity-ends")
    public void endLeftBehind() throws IOException {
        long now = System.currentTimeMillis();
        dropEndedBefore(Instant.ofEpochMilli(now - END_RETRY_MILLIS));
        List<DeliveryActivity> left = leftBehind(Instant.ofEpochMilli(now - END_GRACE_MILLIS));
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
    private void dropEndedBefore(Instant before) {
        activities.dropEndedBefore(Instant.EPOCH, before);
    }

    @Transactional(readOnly = true)
    private List<DeliveryActivity> leftBehind(Instant before) {
        return activities.leftBehind(Instant.EPOCH, before, new PageRequest(0, 50));
    }

    @Transactional
    private void forget(String id) {
        activities.deleteByDeliveryId(id);
    }

    /// The end of the Live Activity of order `id`, sent quietly: the customer
    /// was told of the order's end, or chose not to be, when it happened.
    @Transactional(readOnly = true)
    private List<Outgoing> ending(String id) throws IOException {
        List<Outgoing> out = new ArrayList<Outgoing>();
        DeliveryActivity activity = activities.findById(id);
        CustomerOrder order = orders.findById(id);
        if (activity == null || order == null) {
            return out;
        }
        Profile who = courierOf(id);
        out.add(new Outgoing(devices.targetsOf(order.getCustomer()), quiet(id, order.getState())
                .surface(endOf(activity.getActivityId(), order, who)).build(), id));
        return out;
    }

    /// Moves the customer's Live Activity on as the courier rides: the minutes
    /// to the door. At most once every `savor.push.activity.seconds`, and only
    /// for an order that has one.
    public void courierMoved(String orderId, double lat, double lng) {
        try {
            send(moved(orderId, lat, lng));
        } catch (IOException | RuntimeException err) {
            System.err.println("[push] the Live Activity of order " + orderId
                    + " was not updated: " + err);
        }
    }

    // tag::savor-push-activity[]
    /// Records the Live Activity the customer's phone shows `orderId` in. Only
    /// for the order's customer, and only while it is on its way: placed,
    /// cooking, waiting for its courier, or riding.
    @Transactional
    public DeliveryActivityDto attach(String customer, String orderId, DeliveryActivityDto activity)
            throws IOException {
        String activityId = Text.required(activity == null ? null : activity.activityId(), 190,
                "the activity");
        CustomerOrder order = orders.findById(orderId);
        if (order == null || !order.getCustomer().equals(customer)) {
            throw new ResponseStatusException(404, "No such order");
        }
        // The state check and the insert have to be one step against the order
        // ending: checked from the order read above, a courier delivering it or
        // an admin cancelling it could commit in between, its compose() would
        // find no activity to end, and this one would be inserted after it and
        // stay on the lock screen. holdWhile checks the state in the database
        // and keeps the order's row locked until this commits, so an ending
        // either happened first (409 here) or waits and then finds the row.
        if (orders.holdWhile(orderId, customer, FOLLOWED) == 0) {
            throw new ResponseStatusException(409, "That order is no longer on its way");
        }
        // One activity per order, the last one attached. A customer following
        // their own order on two phones at once is not a case the sample
        // serves: the earlier phone's activity stops moving and the system
        // removes it. A row per device is listed in the implementor's guide
        // among what a product adds.
        DeliveryActivity row = activities.findById(orderId);
        if (row == null) {
            row = new DeliveryActivity();
            row.setDeliveryId(orderId);
            row.setUsername(customer);
            row.setActivityId(activityId);
            row.setSentAt(Instant.now());
            activities.insert(row);
        } else {
            row.setActivityId(activityId);
            row.setSentAt(Instant.now());
        }
        return new DeliveryActivityDto(activityId);
    }
    // end::savor-push-activity[]

    /// Hands each message to the sender. The end of a Live Activity forgets
    /// it only once the push service has taken the message, which the sender
    /// says later, on its own thread. Until then the row stays marked ended,
    /// so a refusal, an outage or a server stopped first leaves it for
    /// [#endLeftBehind]: the order's transition is committed and never
    /// retried, so nothing else would end the activity on the lock screen.
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

    /// What to send about a change to an order, read and decided in one
    /// transaction. An order that has ended marks its Live Activity ended
    /// here; [#send] forgets it once the end has been sent.
    @Transactional
    private List<Outgoing> compose(String id, OrderState state) throws IOException {
        List<Outgoing> out = new ArrayList<Outgoing>();
        CustomerOrder order = orders.findById(id);
        // The change this was called for may no longer be the order's latest.
        // Each transition commits and then composes on its own thread, so two
        // in quick succession can compose out of order: the kitchen accepts,
        // an admin cancels and that commits, and only then does the acceptance
        // get here. When what was committed since differs from `state`,
        // nothing is said: the later transition says what is true. Nothing is
        // cleaned up here either, for the same reason: the later call ends the
        // Live Activity, and deleting its row first would leave it on screen.
        if (order == null || order.getState() != state) {
            return out;
        }
        String customer = order.getCustomer();
        String restaurant = order.getRestaurantName();
        Delivery delivery = deliveries.findById(id);
        String courier = delivery == null ? null : delivery.getCourier();
        boolean hasCourier = courier != null && courier.length() > 0;
        Profile who = hasCourier ? profiles.findById(courier) : null;
        DeliveryActivity activity = activities.findById(id);
        String activityId = activity == null ? null : activity.getActivityId();
        Map<String, Object> surface = null;
        PushNotification.Builder toCustomer = null;
        PushNotification.Builder toKitchen = null;
        PushNotification.Builder toCourier = null;
        // tag::savor-push-messages[]
        switch (state) {
            case PLACED:
                Profile buyer = profiles.findById(customer);
                toKitchen = message(id, state.name(), MERCHANT, "New order",
                        "An order for " + (buyer == null ? "a customer" : buyer.getDisplayName())
                        + ". Open Savor to accept.");
                surface = update(activityId, state(order, who, 0));
                break;
            case ACCEPTED:
                toCustomer = message(id, state.name(), CUSTOMER, restaurant + " is preparing your order",
                        "Ready in about " + Math.max(1, order.getPrepMinutes()) + " min.");
                surface = update(activityId, state(order, who, 0));
                break;
            case READY:
                toCourier = hasCourier ? message(id, state.name(), COURIER, "Order ready",
                        "Pick it up at " + restaurant + ".") : null;
                surface = update(activityId, state(order, who, 0));
                break;
            case PICKED_UP:
                int minutes = eta(order, delivery, courier);
                toCustomer = message(id, state.name(), CUSTOMER, "Your order is on its way",
                        (who == null ? "Your courier" : who.getDisplayName())
                        + " is bringing it. About " + minutes + " min.");
                surface = update(activityId, state(order, who, minutes));
                break;
            case DELIVERED:
                toCustomer = message(id, state.name(), CUSTOMER, "Your order has arrived",
                        "Enjoy your meal. Your receipt is in the app.");
                surface = endOf(activityId, order, who);
                break;
            case REJECTED:
                toCustomer = message(id, state.name(), CUSTOMER, restaurant + " can't take your order",
                        "You have not been charged.");
                surface = endOf(activityId, order, who);
                break;
            case NO_COURIERS:
                toCustomer = message(id, state.name(), CUSTOMER, "No couriers available",
                        "Nobody could bring your order right now. You have not been charged.");
                toKitchen = message(id, state.name(), MERCHANT, "Order canceled",
                        "No courier could be found for it.");
                surface = endOf(activityId, order, who);
                break;
            case CANCELLED_BY_CUSTOMER:
                toKitchen = message(id, state.name(), MERCHANT, "Order canceled",
                        "The customer canceled before you answered.");
                surface = endOf(activityId, order, who);
                break;
            case CANCELLED_BY_ADMIN:
                toCustomer = message(id, state.name(), CUSTOMER, "Your order was canceled",
                        "Savor support canceled this order.");
                toKitchen = message(id, state.name(), MERCHANT, "Order canceled",
                        "Savor support canceled this order.");
                surface = endOf(activityId, order, who);
                break;
            default:
                break;
        }
        // end::savor-push-messages[]
        if (!state.isActive() && activity != null) {
            activities.markEnded(id, Instant.now(), Instant.EPOCH);
        }
        if (toCustomer != null && !wants(customer, OrderState.DELIVERED == state)) {
            toCustomer = null;
        }
        if (toCustomer != null) {
            if (surface != null) {
                toCustomer.surface(surface);
            }
            out.add(new Outgoing(devices.targetsOf(customer), toCustomer.build(),
                    surface != null && !state.isActive() ? id : null));
        } else if (surface != null) {
            // Nothing to say to the customer, but their lock screen still has
            // to follow the order, or let go of it.
            out.add(new Outgoing(devices.targetsOf(customer), quiet(id, state).surface(surface)
                    .build(), state.isActive() ? null : id));
        }
        if (toKitchen != null) {
            List<RestaurantStaff> rows = staff.findByRestaurantId(order.getRestaurantId());
            for (int iter = 0; iter < rows.size(); iter++) {
                String member = rows.get(iter).getUsername();
                if (wants(member, false)) {
                    out.add(new Outgoing(devices.targetsOf(member), toKitchen.build()));
                }
            }
        }
        if (toCourier != null && wants(courier, false)) {
            out.add(new Outgoing(devices.targetsOf(courier), toCourier.build()));
        }
        return out;
    }

    @Transactional(readOnly = true)
    private List<Outgoing> composeForCourier(String id, DeliveryState state, String courier)
            throws IOException {
        List<Outgoing> out = new ArrayList<Outgoing>();
        if (courier == null || courier.length() == 0 || !wants(courier, false)) {
            return out;
        }
        Delivery delivery = deliveries.findById(id);
        // Said only while it is still so: an offer that lapsed or was taken
        // back before this ran is not news.
        if (delivery == null || delivery.getState() != state
                || (DeliveryState.OFFERED == state && !courier.equals(delivery.getCourier()))) {
            return out;
        }
        PushNotification.Builder message = null;
        if (DeliveryState.OFFERED == state) {
            message = message(id, state.name(), COURIER, "New delivery",
                    "Pick up at " + delivery.getPickupAddress() + ". Open Savor to accept.")
                    .ttlSeconds(offerSeconds);
        } else if (DeliveryState.CANCELLED_BY_ADMIN == state) {
            message = message(id, state.name(), COURIER, "Delivery canceled",
                    "Savor support canceled this delivery. You're free for the next one.");
        }
        if (message != null) {
            out.add(new Outgoing(devices.targetsOf(courier), message.build()));
        }
        return out;
    }

    // tag::savor-push-preferences[]
    /// Whether `username` asked to be told: of receipts when `receipt`, of
    /// order updates otherwise. An account that never saved its settings gets
    /// both, as the settings screen shows it.
    private boolean wants(String username, boolean receipt) throws IOException {
        Preference row = preferences.findById(username);
        if (row == null) {
            return true;
        }
        return receipt ? row.isNotifyReceipts() : row.isNotifyDeliveries();
    }
    // end::savor-push-preferences[]

    /// The silent update of a moving courier, when one is due.
    @Transactional
    private List<Outgoing> moved(String orderId, double lat, double lng) throws IOException {
        List<Outgoing> out = new ArrayList<Outgoing>();
        DeliveryActivity activity = activities.findById(orderId);
        if (activity == null) {
            return out;
        }
        long now = System.currentTimeMillis();
        // A conditional update, so that two reports arriving together send one
        // update between them.
        if (activities.claim(orderId, Instant.ofEpochMilli(now), Instant.ofEpochMilli(now - activityMillis)) != 1) {
            return out;
        }
        CustomerOrder order = orders.findById(orderId);
        if (order == null || OrderState.PICKED_UP != order.getState()) {
            return out;
        }
        int minutes = Deliveries.etaMinutes(lat, lng, order.getDropoffLat(), order.getDropoffLng());
        out.add(new Outgoing(devices.targetsOf(order.getCustomer()), quiet(orderId, order.getState())
                .surface(PushNotification.liveUpdate(activity.getActivityId(),
                        state(order, courierOf(orderId), minutes)))
                .build()));
        return out;
    }

    private Profile courierOf(String orderId) throws IOException {
        Delivery delivery = deliveries.findById(orderId);
        String courier = delivery == null ? null : delivery.getCourier();
        return courier == null || courier.length() == 0 ? null : profiles.findById(courier);
    }

    /// A notification for the customer, the kitchen or the courier of an
    /// order: `role` is which, for the app to open the side it is about.
    private static PushNotification.Builder message(String id, String state, String role,
            String title, String body) {
        return PushNotification.builder()
                .title(title)
                .body(body)
                .collapseKey("order-" + id + "-" + role)
                .data("orderId", id)
                .data("state", state)
                .data("role", role);
    }

    /// A message the phone acts on without showing anything.
    private static PushNotification.Builder quiet(String id, OrderState state) {
        return PushNotification.builder()
                .silent(true)
                .collapseKey("order-" + id + "-activity")
                .data("orderId", id)
                .data("state", state.name())
                .data("role", CUSTOMER);
    }

    private static Map<String, Object> update(String activityId, Map<String, Object> state) {
        return activityId == null ? null : PushNotification.liveUpdate(activityId, state);
    }

    /// The end of a Live Activity: a delivered order stays on the lock screen
    /// a while showing its arrival, one that ended otherwise goes at once.
    private static Map<String, Object> endOf(String activityId, CustomerOrder order, Profile who) {
        if (activityId == null) {
            return null;
        }
        return OrderState.DELIVERED == order.getState()
                ? PushNotification.liveEnd(activityId, state(order, who, 0), false)
                : PushNotification.liveEnd(activityId, null, true);
    }

    /// The Live Activity's state, in the keys the app laid it out with.
    static Map<String, Object> state(CustomerOrder order, Profile who, int minutes) {
        Map<String, Object> out = new HashMap<String, Object>();
        String status;
        double progress;
        switch (order.getState()) {
            case ACCEPTED:
                status = "Preparing your order";
                progress = DeliveryActivityKeys.PREPARING;
                break;
            case READY:
                status = who == null ? "Ready, finding a courier" : "Ready for pickup";
                progress = DeliveryActivityKeys.READY;
                break;
            case PICKED_UP:
                status = "On its way";
                progress = DeliveryActivityKeys.ON_THE_WAY;
                break;
            case DELIVERED:
                status = "Delivered";
                progress = DeliveryActivityKeys.DONE;
                break;
            default:
                status = "Order placed";
                progress = DeliveryActivityKeys.PLACED;
                break;
        }
        out.put(DeliveryActivityKeys.STATUS, status);
        out.put(DeliveryActivityKeys.PROGRESS, Double.valueOf(progress));
        out.put(DeliveryActivityKeys.RESTAURANT, order.getRestaurantName());
        out.put(DeliveryActivityKeys.COURIER, who == null ? "" : who.getDisplayName());
        out.put(DeliveryActivityKeys.CAR, who == null || who.getVehicle() == null ? ""
                : who.getVehicle());
        out.put(DeliveryActivityKeys.PLATE, who == null || who.getPlate() == null ? ""
                : who.getPlate());
        out.put(DeliveryActivityKeys.ETA, minutes > 0 ? minutes + " min" : "");
        return out;
    }

    /// Minutes from where the courier is to the door, at least one; from the
    /// restaurant when the courier has not been heard from.
    private int eta(CustomerOrder order, Delivery delivery, String courier) {
        CourierState at = courier == null || courier.length() == 0 ? null : couriers.findById(courier);
        if (at == null) {
            return delivery == null ? 1 : Deliveries.etaMinutes(delivery.getPickupLat(),
                    delivery.getPickupLng(), order.getDropoffLat(), order.getDropoffLng());
        }
        return Deliveries.etaMinutes(at.getLat(), at.getLng(), order.getDropoffLat(),
                order.getDropoffLng());
    }
}
