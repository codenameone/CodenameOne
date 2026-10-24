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
package com.codenameone.examples.savor.delivery;

import com.codename1.backend.ConcurrencyFailureException;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codename1.orm.repository.PageRequest;
import com.codename1.orm.session.Identifier;
import com.codename1.orm.session.PersistenceException;
import com.codenameone.examples.savor.Ids;
import com.codenameone.examples.savor.Text;
import com.codenameone.examples.savor.account.Moderation;
import com.codenameone.examples.savor.api.DeliveryDto;
import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.api.OrderState;
import com.codenameone.examples.savor.domain.CustomerOrder;
import com.codenameone.examples.savor.domain.Restaurant;
import com.codenameone.examples.savor.order.OrderEvents;
import com.codenameone.examples.savor.order.OrderStateMachine;
import com.codenameone.examples.savor.order.Quotes;
import com.codenameone.examples.savor.repository.CustomerOrderRepository;
import com.codenameone.examples.savor.domain.CourierState;
import com.codenameone.examples.savor.domain.Profile;
import com.codenameone.examples.savor.domain.Delivery;
import com.codenameone.examples.savor.domain.DeliveryEvent;
import com.codenameone.examples.savor.domain.DeliveryPassed;
import com.codenameone.examples.savor.geo.Geo;
import com.codenameone.examples.savor.live.LiveHub;
import com.codenameone.examples.savor.pay.Payments;
import com.codenameone.examples.savor.order.OrderNews;
import com.codenameone.examples.savor.push.OrderPushes;
import com.codenameone.examples.savor.repository.CourierStateRepository;
import com.codenameone.examples.savor.repository.ProfileRepository;
import com.codenameone.examples.savor.repository.DeliveryEventRepository;
import com.codenameone.examples.savor.repository.DeliveryPassedRepository;
import com.codenameone.examples.savor.repository.DeliveryRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Deliveries: asking for one, finding it a courier, and taking it through to the end.
///
/// Every change of state goes through [#move], which makes it with a single
/// conditional update ([DeliveryStateMachine]) and records it. The people concerned
/// are then told over the live channel. What they are told is only that the
/// delivery changed; they read the delivery itself from here, so there is one account
/// of a delivery and the live channel cannot disagree with it.
///
/// That is also why the telling waits for the change to be committed. An app
/// told of a change asks for the delivery at once, over another connection, and
/// must find it changed. So the methods that change a delivery come in pairs: a
/// private one that is the transaction, and the public one that calls it and,
/// once it has returned, tells whoever is concerned.
@Component
public class Deliveries {
    /// Deliveries shorter than this are not deliveries, and longer than this are not this
    /// service's.
    static final double MIN_METERS = 100d;
    static final double MAX_METERS = 150000d;
    /// How many couriers one search may choose before it leaves the delivery to the
    /// next tick; see [#search].
    private static final int SEARCH_TRIES = 3;
    /// The two people a move can be held to; see [#tryMove].
    private static final String CUSTOMER = "customer";
    private static final String COURIER = "courier";
    private static final DeliveryState[] ACTIVE = {DeliveryState.REQUESTED, DeliveryState.OFFERED,
        DeliveryState.ACCEPTED, DeliveryState.ARRIVED, DeliveryState.IN_PROGRESS};
    private static final DeliveryState[] WITH_COURIER = {DeliveryState.OFFERED, DeliveryState.ACCEPTED,
        DeliveryState.ARRIVED, DeliveryState.IN_PROGRESS};
    private static final DeliveryState[] TAKEN_NOT_STARTED = {DeliveryState.ACCEPTED,
        DeliveryState.ARRIVED};
    private static final DeliveryState[] ENDED_WITH_COURIER = {DeliveryState.COMPLETED,
        DeliveryState.CANCELLED_BY_CUSTOMER, DeliveryState.CANCELLED_BY_COURIER,
        DeliveryState.CANCELLED_BY_ADMIN};

    private final DeliveryRepository deliveries;
    private final DeliveryEventRepository events;
    private final DeliveryPassedRepository passes;
    private final CourierStateRepository couriers;
    private final ProfileRepository profiles;
    private final Matcher matcher;
    private final CustomerOrderRepository orders;
    private final OrderEvents orderEvents;
    private final Payments payments;
    private final LiveHub live;
    private final OrderPushes pushes;
    private final OrderNews news;
    private final Moderation moderation;
    private final long offerMillis;
    private final long searchMillis;

    /// @param offerSeconds how long a courier has to answer an offer
    /// @param searchSeconds how long a delivery looks for a courier before giving up
    public Deliveries(DeliveryRepository deliveries, DeliveryEventRepository events, DeliveryPassedRepository passes,
            CourierStateRepository couriers, ProfileRepository profiles, Matcher matcher,
            CustomerOrderRepository orders, OrderEvents orderEvents, Payments payments, LiveHub live,
            OrderPushes pushes, OrderNews news, Moderation moderation,
            @Value("${savor.offer.seconds:15}") int offerSeconds,
            @Value("${savor.search.seconds:900}") int searchSeconds) {
        this.deliveries = deliveries;
        this.events = events;
        this.passes = passes;
        this.couriers = couriers;
        this.profiles = profiles;
        this.moderation = moderation;
        this.matcher = matcher;
        this.orders = orders;
        this.orderEvents = orderEvents;
        this.payments = payments;
        this.live = live;
        this.pushes = pushes;
        this.news = news;
        offerMillis = offerSeconds * 1000L;
        searchMillis = searchSeconds * 1000L;
    }

    // ---------------------------------------------------------------- orders

    // tag::savor-dispatch[]
    /// Creates the delivery for an order the kitchen has just accepted, with the
    /// order's id, from the restaurant to the order's door. Runs inside the
    /// transaction that accepts the order, so the two commit together; [#tick]
    /// finds the delivery a courier from then on, while the kitchen cooks.
    ///
    /// The courier is paid the delivery fee and the tip, so that is the fare a
    /// courier is offered.
    public void dispatch(CustomerOrder order, Restaurant restaurant) throws IOException {
        long now = System.currentTimeMillis();
        Delivery delivery = new Delivery();
        delivery.setId(order.getId());
        delivery.setState(DeliveryState.REQUESTED);
        delivery.setCustomer(order.getCustomer());
        delivery.setCourier("");
        delivery.setPickupLat(restaurant.getLat());
        delivery.setPickupLng(restaurant.getLng());
        delivery.setPickupAddress(address(restaurant.getName() + ", " + restaurant.getAddress()));
        delivery.setDropoffLat(order.getDropoffLat());
        delivery.setDropoffLng(order.getDropoffLng());
        delivery.setDropoffAddress(address(order.getDropoffAddress()));
        double meters = Quotes.roadMeters(restaurant.getLat(), restaurant.getLng(),
                order.getDropoffLat(), order.getDropoffLng());
        delivery.setDistanceMeters(meters);
        delivery.setDurationSeconds(meters / Quotes.METERS_PER_SECOND);
        delivery.setFareCents(order.getDeliveryFeeCents());
        delivery.setFeeCents(order.getDeliveryFeeCents());
        // A tip given at checkout is part of the order's total, taken with it,
        // so it is in the courier's share. The delivery's own tip is the one
        // given after it arrives, charged on its own: counting the checkout
        // tip there too would pay it to the courier twice.
        delivery.setTipCents(0L);
        delivery.setCourierCents(order.getDeliveryFeeCents() + order.getTipCents());
        delivery.setCurrency(order.getCurrency());
        delivery.setMethodId(order.getMethodId());
        delivery.setMethodLabel(order.getMethodLabel());
        delivery.setPaymentStatus(order.getPaymentStatus());
        delivery.setNote(order.getNote());
        delivery.setRequestedAt(Instant.ofEpochMilli(now));
        delivery.setUpdatedAt(Instant.ofEpochMilli(now));
        delivery.setSearchUntil(Instant.ofEpochMilli(now + searchMillis));
        deliveries.insert(delivery);
        event(order.getId(), DeliveryState.REQUESTED, "");
    }
    // end::savor-dispatch[]

    /// The customer's stars for the order, given to its courier too: inside
    /// the transaction that rates the order. An order that had no courier --
    /// or whose delivery is already rated -- changes nothing here.
    public void rate(String id, String customer, int stars) {
        if (deliveries.rate(id, customer, stars) == 1) {
            Delivery delivery = find(id);
            if (delivery != null && delivery.getCourier() != null
                    && delivery.getCourier().length() > 0) {
                couriers.addRating(delivery.getCourier(), stars);
            }
        }
    }

    /// Ends the delivery of an order an admin cancelled, inside that
    /// transaction, and answers what changed for [#told], or null when the
    /// order had no delivery under way.
    public Change endForOrder(String id, String by) throws IOException {
        Delivery before = find(id);
        if (before == null || !tryMove(id, DeliveryState.CANCELLED_BY_ADMIN, by, null, null,
                null)) {
            return null;
        }
        return new Change(id, DeliveryState.CANCELLED_BY_ADMIN, before.getCustomer(),
                before.getCourier(), "", null);
    }

    /// Tells the people concerned about a change made by another class's
    /// transaction, once it has committed.
    public void told(Change change) {
        tell(change);
    }

    // --------------------------------------------------------------- couriers

    /// The offer waiting for this courier's answer, or the delivery they are on.
    @Transactional(readOnly = true)
    public DeliveryDto activeForCourier(String courier) throws IOException {
        Delivery delivery = deliveries.findFirstByCourierAndStateIn(courier, WITH_COURIER);
        return delivery == null ? none() : toDto(delivery, courier, new HashMap<String, Profile>());
    }

    @Transactional(readOnly = true)
    public List<DeliveryDto> historyForCourier(String courier) throws IOException {
        return list(deliveries.findTop50ByCourierAndStateInOrderByRequestedAtDesc(courier,
                ENDED_WITH_COURIER), courier);
    }

    public DeliveryDto accept(String courier, String id) throws IOException {
        return tell(attempt(() -> step(courier, id, DeliveryState.ACCEPTED)));
    }

    /// Passes on an offer. The delivery goes back to looking, and not at this
    /// courier again.
    public DeliveryDto decline(String courier, String id) throws IOException {
        tell(attempt(() -> pass(courier, id)));
        search(id);
        return none();
    }

    @Transactional
    private Change pass(String courier, String id) throws IOException {
        Delivery delivery = owned(id, COURIER, courier);
        move(id, DeliveryState.REQUESTED, courier, COURIER, "", null);
        passed(id, courier);
        return new Change(id, DeliveryState.REQUESTED, delivery.getCustomer(), "", courier, null);
    }

    public DeliveryDto arrived(String courier, String id) throws IOException {
        return tell(attempt(() -> step(courier, id, DeliveryState.ARRIVED)));
    }

    public DeliveryDto start(String courier, String id) throws IOException {
        return tell(attempt(() -> step(courier, id, DeliveryState.IN_PROGRESS)));
    }

    /// Ends the trip and charges the customer.
    ///
    /// In three steps, and only the first and the last are transactions. The
    /// charge between them may be a call to a payment provider, which can take
    /// seconds, and a transaction held open across it would hold a connection
    /// for as long.
    public DeliveryDto complete(String courier, String id) throws IOException {
        Delivery delivery = attempt(() -> finish(courier, id));
        // After the move, and only by the request whose update made it: a delivery
        // completes once, so it is charged once.
        String status = payments.settle(delivery);
        return tell(settled(courier, id, status));
    }

    @Transactional
    private Delivery finish(String courier, String id) throws IOException {
        Delivery delivery = owned(id, COURIER, courier);
        move(id, DeliveryState.COMPLETED, courier, COURIER, null, null);
        if (orders.deliver(id, OrderStateMachine.sources(OrderState.DELIVERED),
                OrderState.DELIVERED, Instant.now()) == 1) {
            orderEvents.record(id, OrderState.DELIVERED, courier);
        }
        return delivery;
    }

    /// How long a completed delivery may wait for its payment before it is taken
    /// to have been left behind. Longer than any charge takes.
    private static final long UNSETTLED_MILLIS = 2L * 60L * 1000L;

    /// How long a completed delivery's charge goes on being asked for. Short of
    /// the day a processor remembers the key a charge was first sent under.
    private static final long RETRY_MILLIS = Payments.RETRY_MILLIS;

    /// Finishes the payment of deliveries that completed and were never settled.
    ///
    /// Completing a delivery commits before the card is charged, and what came of
    /// the charge is written after it. A server stopped between the two leaves
    /// a delivery that is over and says its payment is pending, and nothing would
    /// come back for it: the courier's request cannot be made again, the delivery
    /// having completed. This does. Settling is safe to repeat -- see
    /// [Payments#settle] -- so a delivery whose card was charged just before the
    /// server stopped is recorded as paid and not charged again.
    ///
    /// A delivery whose charge failed without the processor saying whether it took
    /// the money is left pending too, and is asked about again here, every
    /// minute for [#RETRY_MILLIS]. After that it stays pending and is not asked
    /// about: the key that makes asking twice safe is kept by a processor for a
    /// day, and a charge sent under it later than that is a second charge. What
    /// became of such a delivery is for a person to find in the processor's records.
    @Scheduled(fixedDelay = 60000, initialDelay = 30000, lock = "savor-settlement")
    public void settleLeftBehind() throws IOException {
        long now = System.currentTimeMillis();
        List<Delivery> unsettled = unsettled(now - RETRY_MILLIS, now - UNSETTLED_MILLIS);
        Exception failed = null;
        for (int iter = 0; iter < unsettled.size(); iter++) {
            Delivery delivery = unsettled.get(iter);
            try {
                tell(settled(delivery.getCourier(), delivery.getId(), payments.settle(delivery)));
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

    @Transactional(readOnly = true)
    private List<Delivery> unsettled(long after, long before) {
        return deliveries.unsettled(Instant.ofEpochMilli(after), Instant.ofEpochMilli(before), new PageRequest(0, 50));
    }

    @Transactional
    private Change settled(String courier, String id, String status) throws IOException {
        deliveries.paymentStatus(id, status);
        orders.paymentStatus(id, status, Instant.now());
        Delivery delivery = find(id);
        return new Change(id, delivery.getState(), delivery.getCustomer(), delivery.getCourier(), "",
                toDto(delivery, courier, new HashMap<String, Profile>()));
    }

    /// Gives back a delivery a courier took and cannot finish, before they have
    /// the food. The order still has to reach its door, so the delivery goes
    /// back to looking, and not at this courier again.
    public DeliveryDto cancelByCourier(String courier, String id) throws IOException {
        tell(attempt(() -> giveUp(courier, id)));
        search(id);
        return none();
    }

    @Transactional
    private Change giveUp(String courier, String id) throws IOException {
        Delivery delivery = owned(id, COURIER, courier);
        if (deliveries.giveBack(id, TAKEN_NOT_STARTED, DeliveryState.REQUESTED,
                Instant.now(), courier, "") != 1) {
            throw new ResponseStatusException(409, "That delivery is "
                    + describe(find(id).getState()));
        }
        // Recorded as what the courier did, though the delivery itself is
        // looking again: the admin's count of a courier's cancellations is
        // read from these.
        event(id, DeliveryState.CANCELLED_BY_COURIER, courier);
        passed(id, courier);
        return new Change(id, DeliveryState.REQUESTED, delivery.getCustomer(), "", courier, null);
    }

    /// One of a courier's own moves of a delivery that is theirs, and the delivery as
    /// they see it afterwards.
    @Transactional
    private Change step(String courier, String id, DeliveryState to) throws IOException {
        if (DeliveryState.ACCEPTED == to) {
            // Blocking a courier is refused while they have a delivery taken, and
            // takes the courier's profile to look. Taking an offer holds the
            // same row first and then asks whether the account is still
            // allowed: a block that committed meanwhile is seen here, and a
            // block that comes after this commits sees the accepted delivery.
            // Either way no delivery is left with a courier who can no longer
            // carry it. Withdrawing the courier's role holds the same row and
            // is asked the same way.
            profiles.hold(courier);
            if (!matcher.mayDeliver(courier)) {
                throw new ResponseStatusException(403, "This account can no longer deliver");
            }
        }
        owned(id, COURIER, courier);
        if (DeliveryState.IN_PROGRESS == to) {
            // The courier has the food: the order is picked up in the same
            // transaction, and only once the kitchen said it is ready.
            if (orders.move(id, OrderStateMachine.sources(OrderState.PICKED_UP),
                    OrderState.PICKED_UP, Instant.now()) != 1) {
                throw new ResponseStatusException(409, "The kitchen has not marked it ready yet");
            }
            orderEvents.record(id, OrderState.PICKED_UP, courier);
        }
        move(id, to, courier, COURIER, null, null);
        Delivery delivery = find(id);
        return new Change(id, delivery.getState(), delivery.getCustomer(), delivery.getCourier(), "",
                toDto(delivery, courier, new HashMap<String, Profile>()));
    }

    // ----------------------------------------------------------------- admin

    // ---------------------------------------------------------------- anyone

    /// A delivery, for someone entitled to see it: its customer, its courier, or an
    /// admin. Anyone else is told it does not exist, which is all they should
    /// learn about it.
    @Transactional(readOnly = true)
    public DeliveryDto get(String viewer, boolean admin, String id) throws IOException {
        Delivery delivery = find(id);
        if (delivery == null || !(admin || viewer.equals(delivery.getCustomer())
                || viewer.equals(delivery.getCourier()))) {
            throw new ResponseStatusException(404, "No such delivery");
        }
        return toDto(delivery, admin ? delivery.getCourier() : viewer, new HashMap<String, Profile>(),
                !admin);
    }

    /// The delivery of an order, for showing inside the order, or null when the
    /// kitchen has not accepted it yet. The caller has already decided that
    /// `viewer` may see the order; `party` is false only for an admin, and true
    /// for its customer, its courier and its restaurant's staff alike: a
    /// merchant is not shown the customer's sign-in name.
    @Transactional(readOnly = true)
    public DeliveryDto forOrder(String id, String viewer, boolean party) throws IOException {
        Delivery delivery = find(id);
        return delivery == null ? null : toDto(delivery, viewer, new HashMap<String, Profile>(),
                party);
    }

    /// The most recent deliveries, for the admin.
    @Transactional(readOnly = true)
    public List<DeliveryDto> recent() throws IOException {
        List<Delivery> rows = deliveries.newest(new PageRequest(0, 100));
        Map<String, Profile> cache = new HashMap<String, Profile>();
        List<DeliveryDto> out = new ArrayList<DeliveryDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Delivery delivery = rows.get(iter);
            out.add(toDto(delivery, delivery.getCourier(), cache, false));
        }
        return out;
    }

    // -------------------------------------------------------------- matching

    /// Offers a waiting delivery to the nearest courier who has not passed on it.
    /// Does nothing when nobody is in reach; [#tick] tries again.
    ///
    /// In two transactions: one that chooses a courier and one that makes the
    /// offer. Choosing reads who is free, and two deliveries asked for together
    /// each read the same courier as free; the second transaction is where
    /// they take turns over that courier, and it has to open with that -- see
    /// [#offer].
    ///
    /// A courier the offer finds no longer fit for the delivery is passed over for
    /// the next, as a decline or a lapse would be, without waiting for the
    /// next tick: choosing again reads them as they now are, and does not pick
    /// them. Bounded, so couriers changing faster than this can choose cost the
    /// delivery a tick and no more.
    public void search(String id) throws IOException {
        String[] chosen = new String[1];
        Change change = null;
        for (int tries = 0; tries < SEARCH_TRIES; tries++) {
            change = attempt(() -> choose(id, chosen));
            if (chosen[0] == null) {
                break;
            }
            change = attempt(() -> offer(id, chosen[0]));
            if (change != null) {
                break;
            }
        }
        tell(change);
        if (change != null && DeliveryState.NO_COURIERS == change.state) {
            // The order will not come, so the money held for it is let go.
            payments.release(id);
        }
    }

    /// Gives up on a delivery that has waited too long, or names the courier to
    /// offer it to in `chosen`. Returns what it changed, or null for nothing.
    @Transactional
    private Change choose(String id, String[] chosen) throws IOException {
        chosen[0] = null;
        Delivery delivery = find(id);
        if (delivery == null || DeliveryState.REQUESTED != delivery.getState()) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now > delivery.getSearchUntil().toEpochMilli()) {
            if (!tryMove(id, DeliveryState.NO_COURIERS, "", null, null, null)) {
                return null;
            }
            if (orders.end(id, OrderStateMachine.sources(OrderState.NO_COURIERS),
                    OrderState.NO_COURIERS, "No courier was free to bring it", Instant.ofEpochMilli(now)) == 1) {
                orderEvents.record(id, OrderState.NO_COURIERS, "");
            }
            return new Change(id, DeliveryState.NO_COURIERS, delivery.getCustomer(), "", "", null);
        }
        chosen[0] = matcher.pick(delivery, now - delivery.getRequestedAt().toEpochMilli());
        return null;
    }

    // tag::savor-delivery-offer[]
    /// Offers a delivery to the courier chosen for it, unless they were given
    /// another in the meantime. Returns what it changed, or null for nothing,
    /// in which case [#tick] looks again.
    ///
    /// The courier is held before anything is read, so two deliveries that chose
    /// the same courier come through here one after the other, and the second
    /// reads the offer the first one committed. The update of the delivery cannot
    /// do this on its own: it is conditional on its own delivery's row, and each
    /// of the two has a row of its own.
    ///
    /// Everything that made the courier the choice is asked again once they
    /// are held: a courier who went off line, drove out of reach, lost their
    /// approval or was blocked since [#choose] read them would otherwise get
    /// the offer and keep the customer waiting for it to lapse. Not private, so
    /// that a test can make the offer to a courier who changed in between.
    @Transactional
    Change offer(String id, String courier) throws IOException {
        if (couriers.hold(courier) == 0
                || deliveries.findFirstByCourierAndStateIn(courier, WITH_COURIER) != null) {
            return null;
        }
        Delivery delivery = find(id);
        if (delivery == null || DeliveryState.REQUESTED != delivery.getState()) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (!matcher.fits(delivery, courier, now - delivery.getRequestedAt().toEpochMilli())) {
            return null;
        }
        return tryMove(id, DeliveryState.OFFERED, "", null, courier, Long.valueOf(now + offerMillis))
                ? new Change(id, DeliveryState.OFFERED, delivery.getCustomer(), courier, "", null) : null;
    }
    // end::savor-delivery-offer[]

    /// Takes back the offers nobody answered and looks again for every delivery
    /// still waiting. Runs every two seconds, on one instance at a time: `lock`
    /// keeps a second server from offering the same delivery to two couriers.
    ///
    /// Not a transaction itself. Each delivery is dealt with in one of its own, so
    /// a delivery that cannot be dealt with costs nobody else their turn; what went
    /// wrong with the first such delivery is reported once the rest are done.
    @Scheduled(fixedDelay = 2000, initialDelay = 2000, lock = "savor-matching")
    public void tick() throws IOException {
        Exception failed = null;
        List<Delivery> lapsed = lapsedOffers(System.currentTimeMillis());
        for (int iter = 0; iter < lapsed.size(); iter++) {
            Delivery delivery = lapsed.get(iter);
            try {
                tell(attempt(() -> lapse(delivery.getId(), delivery.getCustomer(), delivery.getCourier())));
            } catch (IOException err) {
                failed = failed == null ? err : failed;
            } catch (RuntimeException err) {
                failed = failed == null ? err : failed;
            }
        }
        List<Delivery> waiting = waitingDeliveries();
        for (int iter = 0; iter < waiting.size(); iter++) {
            try {
                search(waiting.get(iter).getId());
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

    @Transactional(readOnly = true)
    private List<Delivery> lapsedOffers(long now) throws IOException {
        return deliveries.findByStateAndOfferExpiresAtLessThan(DeliveryState.OFFERED, Instant.ofEpochMilli(now));
    }

    @Transactional(readOnly = true)
    private List<Delivery> waitingDeliveries() throws IOException {
        return deliveries.findByStateOrderByRequestedAtAsc(DeliveryState.REQUESTED);
    }

    /// Takes a lapsed offer back from the courier it was made to, or changes
    /// nothing and answers null.
    @Transactional
    private Change lapse(String id, String customer, String courier) throws IOException {
        // Held to the courier it lapsed for: if they accepted in the instant
        // since the query, this changes nothing and the delivery is theirs.
        if (!tryMove(id, DeliveryState.REQUESTED, courier, COURIER, "", null)) {
            return null;
        }
        passed(id, courier);
        return new Change(id, DeliveryState.REQUESTED, customer, "", courier, null);
    }

    // ------------------------------------------------------------- internals

    /// What a transaction did to a delivery, for telling the people concerned once
    /// it has committed, and the delivery as the caller is to see it. Not private:
    /// the transactional methods answer it, and the class the build writes to
    /// begin and commit their transactions has to be able to name it.
    public static final class Change {
        final String id;
        final DeliveryState state;
        final String customer;
        final String courier;
        /// Someone else to tell: the courier an offer was taken back from.
        final String also;
        final DeliveryDto view;

        Change(String id, DeliveryState state, String customer, String courier, String also, DeliveryDto view) {
            this.id = id;
            this.state = state;
            this.customer = customer;
            this.courier = courier;
            this.also = also;
            this.view = view;
        }
    }

    /// Tells the people concerned about a change that has been committed, and
    /// answers the view that came with it. Nothing changed, nothing said.
    private DeliveryDto tell(Change change) {
        if (change == null) {
            return null;
        }
        live.deliveryChanged(change.id, change.state, change.customer, change.courier, change.also);
        // The same moment, and for the same reason: a phone told of a change
        // must find it made.
        pushes.deliveryChanged(change.id, change.state, change.courier);
        // The steps that move the order too: the customer and the kitchen
        // hear of those as the order's.
        if (DeliveryState.IN_PROGRESS == change.state || DeliveryState.COMPLETED == change.state
                || DeliveryState.NO_COURIERS == change.state) {
            news.told(change.id);
        }
        return change.view;
    }

    /// One of the transactions of this class, for [#attempt] to run.
    private interface Work<T> {
        T run() throws IOException;
    }

    /// Runs a transaction that changes a delivery, and runs it again if the database
    /// ended it because another transaction changed the same delivery first.
    ///
    /// Every change here is a delivery read and then moved by a conditional update,
    /// and two of them racing are meant to be settled by that update: the loser
    /// changes no rows. Most databases do exactly that. One that holds a
    /// transaction to the snapshot it first read from -- MariaDB, as it is
    /// configured out of the box -- refuses the loser's update instead, because
    /// the delivery it read is no longer the delivery that is there, and ends its
    /// transaction with a [ConcurrencyFailureException].
    ///
    /// That is the race lost and not a fault, so it gets the answer the other
    /// databases give. The transaction was rolled back as the exception left
    /// it, so nothing of it remains; run again, it reads the delivery as the winner
    /// left it and its update changes no rows. This is the one place that
    /// happens, and it has to be outside the transaction: the database has
    /// already ended that one, and a statement sent after the failure would run
    /// on its own or be refused, depending on the database.
    ///
    /// Three attempts, because each one lost means someone else moved the delivery,
    /// and a delivery has only so many moves in it.
    private static <T> T attempt(Work<T> work) throws IOException {
        for (int tries = 1; ; tries++) {
            try {
                return work.run();
            } catch (PersistenceException err) {
                // The session reports what the database said as its own
                // unchecked exception, with the database's as the cause.
                if (tries == 3 || !ConcurrencyFailureException.isCauseOf(err)) {
                    throw err;
                }
            } catch (ConcurrencyFailureException err) {
                if (tries == 3) {
                    throw err;
                }
            }
        }
    }

    /// Moves a delivery to `to`, or answers 409 when its state does not allow it.
    private void move(String id, DeliveryState to, String actor, String ownerField, String newCourier,
            Long newOfferExpiresAt) throws IOException {
        if (!tryMove(id, to, actor, ownerField, newCourier, newOfferExpiresAt)) {
            Delivery now = find(id);
            throw new ResponseStatusException(409, "That delivery is "
                    + (now == null ? "gone" : describe(now.getState())));
        }
    }

    /// The conditional update behind every change of state. True when this call
    /// is the one that made the change.
    ///
    /// The session is empty afterwards, every move in `DeliveryRepository` being
    /// marked `clearAutomatically`: a delivery read before this is no longer the
    /// session's, and is read again by whoever needs it as it is now.
    ///
    /// @param ownerField [#CUSTOMER] or [#COURIER] to hold the change to deliveries
    ///     whose customer or courier is `actor`, or null for a change the server
    ///     makes itself
    /// @param newCourier the courier to write with the change, or null to leave
    ///     it as it is
    /// @param newOfferExpiresAt when the offer this change makes lapses, or null
    // tag::savor-delivery-trymove[]
    private boolean tryMove(String id, DeliveryState to, String actor, String ownerField,
            String newCourier, Long newOfferExpiresAt) throws IOException {
        DeliveryState[] sources = DeliveryStateMachine.sources(to);
        if (sources.length == 0) {
            return false;
        }
        Instant now = Instant.now();
        int moved;
        if (newOfferExpiresAt != null) {
            moved = deliveries.offer(id, sources, to, now, newCourier,
                    Instant.ofEpochMilli(newOfferExpiresAt.longValue()));
        } else if (CUSTOMER.equals(ownerField)) {
            moved = deliveries.moveAsCustomer(id, sources, to, now, actor);
        } else if (!COURIER.equals(ownerField)) {
            moved = deliveries.moveByServer(id, sources, to, now);
        } else if (newCourier != null) {
            moved = deliveries.giveBack(id, sources, to, now, actor, newCourier);
        } else if (DeliveryState.ACCEPTED == to) {
            // An offer that has lapsed cannot be taken, even in the moment
            // before the sweep takes it back.
            moved = deliveries.takeOffer(id, sources, to, now, actor);
        } else {
            moved = deliveries.moveAsCourier(id, sources, to, now, actor);
        }
        if (moved != 1) {
            return false;
        }
        event(id, to, actor);
        return true;
    }
    // end::savor-delivery-trymove[]

    private void event(String id, DeliveryState state, String actor) throws IOException {
        DeliveryEvent event = new DeliveryEvent();
        event.setId(Ids.next());
        event.setDeliveryId(id);
        event.setState(state);
        event.setActor(actor);
        event.setCreatedAt(Instant.now());
        events.insert(event);
    }

    private void passed(String id, String courier) {
        if (!passes.existsById(Identifier.of(id, courier))) {
            DeliveryPassed passed = new DeliveryPassed();
            passed.setDeliveryId(id);
            passed.setCourier(courier);
            passes.insert(passed);
        }
    }

    /// The delivery, when `actor` is its customer or its courier, whichever `field`
    /// names; 404 otherwise.
    private Delivery owned(String id, String field, String actor) {
        Delivery delivery = find(id);
        if (delivery == null || !actor.equals(CUSTOMER.equals(field) ? delivery.getCustomer()
                : delivery.getCourier())) {
            throw new ResponseStatusException(404, "No such delivery");
        }
        return delivery;
    }

    /// The delivery of this id, or null when there is none. An id that cannot be
    /// one is not looked for.
    private Delivery find(String id) {
        if (id == null || id.length() == 0 || id.length() > 32) {
            return null;
        }
        return deliveries.findById(id);
    }

    private DeliveryDto view(String id, String viewer) throws IOException {
        return toDto(find(id), viewer, new HashMap<String, Profile>());
    }

    private List<DeliveryDto> list(List<Delivery> rows, String viewer) throws IOException {
        Map<String, Profile> cache = new HashMap<String, Profile>();
        List<DeliveryDto> out = new ArrayList<DeliveryDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(toDto(rows.get(iter), viewer, cache));
        }
        return out;
    }

    static DeliveryDto none() {
        return DeliveryDto.none();
    }

    private DeliveryDto toDto(Delivery delivery, String viewer, Map<String, Profile> cache)
            throws IOException {
        return toDto(delivery, viewer, cache, true);
    }

    /// @param party whether `viewer` is asking as the delivery's customer or courier.
    ///     An admin is shown a delivery as its courier sees it, and is not a party:
    ///     the two phone numbers are for the two people who have to find each
    ///     other, and only while they do.
    private DeliveryDto toDto(Delivery delivery, String viewer, Map<String, Profile> cache, boolean party)
            throws IOException {
        DeliveryState state = delivery.getState();
        // An account's name is the e-mail it signs in with. The other party
        // to a delivery is shown a display name, and a phone number while the two
        // are meeting; the address is for its owner and for an admin.
        boolean asCustomer = !party || (viewer != null && viewer.equals(delivery.getCustomer()));
        boolean asCourier = !party || (viewer != null && viewer.equals(delivery.getCourier()));
        Profile customer = profile(delivery.getCustomer(), cache);
        // What is known of the courier, filled in below for a delivery that has one
        // this viewer may see.
        String courierUsername = null;
        String courierName = null;
        String courierPhotoInitials = "";
        String vehicle = null;
        String plate = null;
        String courierPhone = null;
        String customerPhone = null;
        double courierRating = 0d;
        double courierLat = 0d;
        double courierLng = 0d;
        double courierHeading = 0d;
        int etaMinutes = 0;
        int offerExpiresInSeconds = 0;
        String courier = delivery.getCourier();
        boolean offered = DeliveryState.OFFERED == state;
        // An offer is between the server and one courier. Until it is taken the
        // customer has no courier, and is not shown who was asked.
        if (courier.length() > 0 && (!offered || courier.equals(viewer))) {
            if (asCourier) {
                courierUsername = courier;
            }
            Profile who = profile(courier, cache);
            if (who != null) {
                courierName = who.getDisplayName();
                courierPhotoInitials = Text.initials(courierName);
                vehicle = who.getVehicle();
                plate = who.getPlate();
            }
            boolean meeting = DeliveryState.ACCEPTED == state
                    || DeliveryState.ARRIVED == state
                    || DeliveryState.IN_PROGRESS == state;
            if (party && meeting && viewer != null) {
                if (viewer.equals(delivery.getCustomer()) && who != null) {
                    courierPhone = who.getPhone();
                } else if (viewer.equals(courier) && customer != null) {
                    customerPhone = customer.getPhone();
                }
            }
            CourierState car = couriers.findById(courier);
            if (car != null) {
                courierRating = car.rating();
            }
            // Where the car is belongs to the delivery while it brings the two
            // together and carries one of them. A delivery that has ended stays in
            // its customer's history for good, and the courier goes on reporting
            // where they are for other people's deliveries: read from here, a past
            // customer could follow them indefinitely.
            if (car != null && meeting) {
                courierLat = car.getLat();
                courierLng = car.getLng();
                courierHeading = car.getHeading();
                // How long until the car is where it is going next: the pickup
                // until the customer is in it, the destination after.
                if (DeliveryState.ACCEPTED == state || DeliveryState.ARRIVED == state) {
                    etaMinutes = etaMinutes(courierLat, courierLng, delivery.getPickupLat(),
                            delivery.getPickupLng());
                } else if (DeliveryState.IN_PROGRESS == state) {
                    etaMinutes = etaMinutes(courierLat, courierLng, delivery.getDropoffLat(),
                            delivery.getDropoffLng());
                }
            }
        }
        if (offered) {
            offerExpiresInSeconds = (int) Math.max(0L,
                    (delivery.getOfferExpiresAt().toEpochMilli() - System.currentTimeMillis()) / 1000L);
        }
        long fareCents = delivery.getFareCents();
        long tip = delivery.getTipCents();
        // What the customer pays is the order's whole total (food, fees, tax,
        // discount and the checkout tip) plus a tip given after arrival. The
        // courier collects exactly this for a cash order, so the fare alone is
        // far too little. A delivery shares its order's id.
        CustomerOrder order = orders.findById(delivery.getId());
        long total = (order == null ? fareCents : order.getTotalCents()) + tip;
        return new DeliveryDto(
                delivery.getId(),
                state,
                asCustomer ? delivery.getCustomer() : null,
                customer == null ? "" : customer.getDisplayName(),
                courierUsername,
                courierName,
                vehicle,
                plate,
                courierRating,
                delivery.getPickupLat(),
                delivery.getPickupLng(),
                delivery.getPickupAddress(),
                delivery.getDropoffLat(),
                delivery.getDropoffLng(),
                delivery.getDropoffAddress(),
                courierLat,
                courierLng,
                courierHeading,
                fareCents,
                delivery.getCurrency(),
                delivery.getDistanceMeters(),
                delivery.getDurationSeconds(),
                delivery.getRequestedAt().toEpochMilli(),
                delivery.getUpdatedAt().toEpochMilli(),
                delivery.getStars(),
                delivery.getPaymentStatus(),
                offerExpiresInSeconds,
                delivery.getMethodLabel(),
                Payments.CASH.equals(delivery.getMethodId()),
                0L,
                tip,
                total,
                courierPhotoInitials,
                etaMinutes,
                delivery.getNote(),
                courierPhone,
                customerPhone);
    }

    /// Minutes for a courier to travel from one point to another, by the same
    /// estimate the fares are made with. Never less than one.
    public static int etaMinutes(double fromLat, double fromLng, double toLat, double toLng) {
        double seconds = Geo.meters(fromLat, fromLng, toLat, toLng) * Quotes.ROAD_FACTOR
                / Quotes.METERS_PER_SECOND;
        return Math.max(1, (int) Math.ceil(seconds / 60d));
    }

    private Profile profile(String username, Map<String, Profile> cache) {
        if (cache.containsKey(username)) {
            return cache.get(username);
        }
        Profile row = profiles.findById(username);
        cache.put(username, row);
        return row;
    }

    private static String address(String value) {
        String text = value == null ? "" : value.trim();
        return text.length() > 255 ? text.substring(0, 255) : text;
    }

    private static String describe(DeliveryState state) {
        if (DeliveryState.COMPLETED == state) {
            return "already completed";
        }
        if (DeliveryState.IN_PROGRESS == state) {
            return "already under way";
        }
        if (DeliveryState.ACCEPTED == state || DeliveryState.ARRIVED == state) {
            return "already taken";
        }
        if (DeliveryState.REQUESTED == state || DeliveryState.OFFERED == state) {
            return "no longer on offer to you";
        }
        return "no longer available";
    }
}
