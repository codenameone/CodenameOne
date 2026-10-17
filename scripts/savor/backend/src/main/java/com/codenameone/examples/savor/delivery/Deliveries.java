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
import com.codenameone.examples.savor.api.FareOptionDto;
import com.codenameone.examples.savor.api.FareQuoteDto;
import com.codenameone.examples.savor.api.NearbyCourierDto;
import com.codenameone.examples.savor.api.PricingDto;
import com.codenameone.examples.savor.api.DeliveryDto;
import com.codenameone.examples.savor.api.DeliveryRequestDto;
import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.domain.CourierState;
import com.codenameone.examples.savor.domain.Profile;
import com.codenameone.examples.savor.domain.Delivery;
import com.codenameone.examples.savor.domain.DeliveryEvent;
import com.codenameone.examples.savor.domain.DeliveryPassed;
import com.codenameone.examples.savor.geo.Geo;
import com.codenameone.examples.savor.live.LiveHub;
import com.codenameone.examples.savor.pay.Payments;
import com.codenameone.examples.savor.push.DeliveryPushes;
import com.codenameone.examples.savor.repository.CourierStateRepository;
import com.codenameone.examples.savor.repository.ProfileRepository;
import com.codenameone.examples.savor.repository.DeliveryEventRepository;
import com.codenameone.examples.savor.repository.DeliveryPassedRepository;
import com.codenameone.examples.savor.repository.DeliveryRepository;

import java.io.IOException;
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
    private static final String[] COURIER_GENDERS = {"any", "women"};
    private static final String WOMEN = "women";
    /// What a customer is told about each kind of delivery: its name, a line about
    /// it, and how many it seats. In the order of [Fares#PRODUCTS].
    private static final String[][] PRODUCT_TEXT = {
        {"Savor", "Affordable everyday deliveries", "4"},
        {"Comfort", "Newer cars with extra legroom", "4"},
        {"XL", "Room for up to six", "6"}};
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
    private static final DeliveryState[] ENDED_WITH_COURIER = {DeliveryState.COMPLETED,
        DeliveryState.CANCELLED_BY_CUSTOMER, DeliveryState.CANCELLED_BY_COURIER,
        DeliveryState.CANCELLED_BY_ADMIN};

    private final DeliveryRepository deliveries;
    private final DeliveryEventRepository events;
    private final DeliveryPassedRepository passes;
    private final CourierStateRepository couriers;
    private final ProfileRepository profiles;
    private final Matcher matcher;
    private final Fares fares;
    private final Payments payments;
    private final LiveHub live;
    private final DeliveryPushes pushes;
    private final Moderation moderation;
    private final long offerMillis;
    private final long searchMillis;

    /// @param offerSeconds how long a courier has to answer an offer
    /// @param searchSeconds how long a delivery looks for a courier before giving up
    public Deliveries(DeliveryRepository deliveries, DeliveryEventRepository events, DeliveryPassedRepository passes,
            CourierStateRepository couriers, ProfileRepository profiles, Matcher matcher, Fares fares, Payments payments, LiveHub live, DeliveryPushes pushes,
            Moderation moderation,
            @Value("${savor.offer.seconds:15}") int offerSeconds,
            @Value("${savor.search.seconds:90}") int searchSeconds) {
        this.deliveries = deliveries;
        this.events = events;
        this.passes = passes;
        this.couriers = couriers;
        this.profiles = profiles;
        this.moderation = moderation;
        this.matcher = matcher;
        this.fares = fares;
        this.payments = payments;
        this.live = live;
        this.pushes = pushes;
        offerMillis = offerSeconds * 1000L;
        searchMillis = searchSeconds * 1000L;
    }

    // ---------------------------------------------------------------- customers

    @Transactional(readOnly = true)
    public FareQuoteDto quote(DeliveryRequestDto request) throws IOException {
        validate(request);
        double distanceMeters = fares.distanceMeters(request);
        double durationSeconds = fares.durationSeconds(request, distanceMeters);
        String wanted = Fares.product(request.product());
        PricingDto pricing = fares.pricing();
        // The couriers who could take it as the customer asked for it, of every
        // kind of car, nearest first; each kind then counts its own.
        List<Matcher.Candidate> near = matcher.nearby(request.pickupLat(), request.pickupLng(),
                Matcher.FAR_METERS, null, WOMEN.equals(courierGender(request)),
                request.accessibleVehicle(), request.petFriendly());
        List<FareOptionDto> options = new ArrayList<FareOptionDto>();
        long amountCents = 0L;
        int couriersNearby = 0;
        int etaSeconds = 0;
        for (int iter = 0; iter < Fares.PRODUCTS.length; iter++) {
            String product = Fares.PRODUCTS[iter];
            Fares.Price price = Fares.price(pricing, distanceMeters, durationSeconds, product);
            int nearby = 0;
            double nearest = -1d;
            for (int d = 0; d < near.size(); d++) {
                if (product.equals(near.get(d).product)) {
                    nearby++;
                    if (nearest < 0d) {
                        nearest = near.get(d).meters;
                    }
                }
            }
            int eta = nearest < 0d ? 0 : (int) Math.max(60d, nearest * Fares.ROAD_FACTOR
                    / Fares.METERS_PER_SECOND);
            options.add(new FareOptionDto(product, PRODUCT_TEXT[iter][0], PRODUCT_TEXT[iter][1],
                    Integer.parseInt(PRODUCT_TEXT[iter][2]), price.fare, price.fee,
                    price.fare + price.fee, nearby, (eta + 59) / 60));
            if (product.equals(wanted)) {
                // The quote's own fields are the kind of delivery that was asked
                // about, which is the standard one for an app that names none.
                amountCents = price.fare;
                couriersNearby = nearby;
                etaSeconds = eta;
            }
        }
        return new FareQuoteDto(amountCents, fares.currency(), distanceMeters, durationSeconds,
                couriersNearby, etaSeconds, options);
    }

    /// The cars near a point, for the map a customer looks at before asking:
    /// where they are and what kind, and nothing about who drives them. The
    /// positions are rounded to about ten metres, and there are at most twenty.
    @Transactional(readOnly = true)
    public List<NearbyCourierDto> nearbyCouriers(double lat, double lng) throws IOException {
        if (!Geo.isPoint(lat, lng)) {
            throw new ResponseStatusException(400, "That is not a place");
        }
        List<Matcher.Candidate> near = matcher.nearby(lat, lng, Matcher.FAR_METERS);
        List<NearbyCourierDto> out = new ArrayList<NearbyCourierDto>();
        for (int iter = 0; iter < near.size() && iter < 20; iter++) {
            Matcher.Candidate car = near.get(iter);
            out.add(new NearbyCourierDto(Math.round(car.lat * 10000d) / 10000d,
                    Math.round(car.lng * 10000d) / 10000d, car.heading, car.product));
        }
        return out;
    }

    /// Asks for a delivery and offers it to the nearest courier straight away.
    public DeliveryDto request(String customer, DeliveryRequestDto request) throws IOException {
        validate(request);
        String id = attempt(() -> open(customer, request));
        live.deliveryChanged(id, DeliveryState.REQUESTED, customer, "", "");
        search(id);
        return read(id, customer);
    }

    @Transactional
    private String open(String customer, DeliveryRequestDto request) throws IOException {
        // Before anything is read: two requests from one customer take turns
        // here, so the second looks for a delivery under way only once the first
        // has committed its own. Without it both look, both find none, and
        // the customer has two deliveries.
        profiles.hold(customer);
        Profile profile = profiles.findById(customer);
        if (profile == null || !profile.isPhoneVerified()) {
            throw new ResponseStatusException(403, "Verify your phone number before you delivery");
        }
        if (deliveries.findFirstByCustomerAndStateIn(customer, ACTIVE) != null) {
            throw new ResponseStatusException(409, "You already have a delivery under way");
        }
        String product = Fares.product(request.product());
        boolean womenOnly = WOMEN.equals(courierGender(request));
        // Refused here, before there is a delivery, when the card named is not the
        // customer's. With none named it is their default, which is cash until
        // they save a card.
        String[] method = payments.choose(customer, request.paymentMethodId());
        double meters = fares.distanceMeters(request);
        double seconds = fares.durationSeconds(request, meters);
        PricingDto pricing = fares.pricing();
        Fares.Price price = Fares.price(pricing, meters, seconds, product);
        long now = System.currentTimeMillis();
        // The fare is fixed here, from the server's own arithmetic, and so is
        // the courier's share of it: a change of prices or of commission made
        // while the delivery is under way is for the deliveries after it. The quote the
        // customer was shown is not sent back and would not be believed.
        Delivery delivery = new Delivery();
        delivery.setId(Ids.next());
        delivery.setState(DeliveryState.REQUESTED);
        delivery.setCustomer(customer);
        delivery.setPickupLat(request.pickupLat());
        delivery.setPickupLng(request.pickupLng());
        delivery.setPickupAddress(address(request.pickupAddress()));
        delivery.setDropoffLat(request.dropoffLat());
        delivery.setDropoffLng(request.dropoffLng());
        delivery.setDropoffAddress(address(request.dropoffAddress()));
        delivery.setFareCents(price.fare);
        delivery.setCurrency(fares.currency());
        delivery.setDistanceMeters(meters);
        delivery.setDurationSeconds(seconds);
        delivery.setRequestedAt(now);
        delivery.setUpdatedAt(now);
        delivery.setSearchUntil(now + searchMillis);
        delivery.setPaymentStatus(Payments.PENDING);
        delivery.setProduct(product);
        delivery.setMethodId(method[0]);
        delivery.setMethodLabel(method[1]);
        delivery.setBaseCents(price.base);
        delivery.setDistanceCents(price.distance);
        delivery.setTimeCents(price.time);
        delivery.setFeeCents(price.fee);
        delivery.setCourierCents(price.fare - Fares.commission(pricing, price.fare));
        delivery.setWomenOnly(womenOnly);
        delivery.setQuiet(request.quietDelivery());
        delivery.setWheelchair(request.accessibleVehicle());
        delivery.setNote(address(request.note()));
        delivery.setPets(request.petFriendly());
        deliveries.insert(delivery);
        event(delivery.getId(), DeliveryState.REQUESTED, customer);
        return delivery.getId();
    }

    // tag::savor-service-readonly[]
    @Transactional(readOnly = true)
    public DeliveryDto activeForCustomer(String customer) throws IOException {
        Delivery delivery = deliveries.findFirstByCustomerAndStateIn(customer, ACTIVE);
        return delivery == null ? none() : toDto(delivery, customer, new HashMap<String, Profile>());
    }
    // end::savor-service-readonly[]

    @Transactional(readOnly = true)
    public List<DeliveryDto> historyForCustomer(String customer) throws IOException {
        return list(deliveries.findTop50ByCustomerOrderByRequestedAtDesc(customer), customer);
    }

    // tag::savor-service-transaction[]
    public DeliveryDto cancelByCustomer(String customer, String id) throws IOException {
        Change change = attempt(() -> cancelAsCustomer(customer, id));
        // The courier it was offered to or who had taken it is told as well.
        return tell(change);
    }

    @Transactional
    private Change cancelAsCustomer(String customer, String id) throws IOException {
        Delivery before = owned(id, CUSTOMER, customer);
        move(id, DeliveryState.CANCELLED_BY_CUSTOMER, customer, CUSTOMER, null, null);
        return new Change(id, DeliveryState.CANCELLED_BY_CUSTOMER, customer, before.getCourier(), "",
                view(id, customer));
    }
    // end::savor-service-transaction[]

    /// Rates a completed delivery, once, and counts it toward the courier's rating.
    public DeliveryDto rate(String customer, String id, int stars) throws IOException {
        if (stars < 1 || stars > 5) {
            throw new ResponseStatusException(400, "A rating is 1 to 5 stars");
        }
        return attempt(() -> rated(customer, id, stars));
    }

    @Transactional
    private DeliveryDto rated(String customer, String id, int stars) throws IOException {
        String courier = owned(id, CUSTOMER, customer).getCourier();
        if (deliveries.rate(id, customer, stars) != 1) {
            throw new ResponseStatusException(409, "That delivery cannot be rated now");
        }
        couriers.addRating(courier, stars);
        return view(id, customer);
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
        return deliveries.unsettled(after, before, new PageRequest(0, 50));
    }

    @Transactional
    private Change settled(String courier, String id, String status) throws IOException {
        deliveries.paymentStatus(id, status);
        Delivery delivery = find(id);
        return new Change(id, delivery.getState(), delivery.getCustomer(), delivery.getCourier(), "",
                toDto(delivery, courier, new HashMap<String, Profile>()));
    }

    public DeliveryDto cancelByCourier(String courier, String id) throws IOException {
        return tell(attempt(() -> step(courier, id, DeliveryState.CANCELLED_BY_COURIER)));
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
            // drive it.
            profiles.hold(courier);
            if (!matcher.enabled(courier)) {
                throw new ResponseStatusException(403, "This account has been blocked");
            }
        }
        owned(id, COURIER, courier);
        move(id, to, courier, COURIER, null, null);
        Delivery delivery = find(id);
        return new Change(id, delivery.getState(), delivery.getCustomer(), delivery.getCourier(), "",
                toDto(delivery, courier, new HashMap<String, Profile>()));
    }

    // ----------------------------------------------------------------- admin

    /// Ends a delivery that has not ended, whoever has it and whatever it was
    /// doing. Nothing is charged: a delivery pays when it completes, and this one
    /// does not. The reason is kept with the customer's account.
    public DeliveryDto cancelByAdmin(String by, String id, String reason) throws IOException {
        String why = Text.required(reason, 255, "why the delivery is cancelled");
        return tell(attempt(() -> cancelAsAdmin(by, id, why)));
    }

    @Transactional
    private Change cancelAsAdmin(String by, String id, String why) throws IOException {
        Delivery before = find(id);
        if (before == null) {
            throw new ResponseStatusException(404, "No such delivery");
        }
        String customer = before.getCustomer();
        String courier = before.getCourier();
        move(id, DeliveryState.CANCELLED_BY_ADMIN, by, null, null, null);
        moderation.record(customer, by, "cancel_delivery", why);
        return new Change(id, DeliveryState.CANCELLED_BY_ADMIN, customer, courier, "",
                toDto(find(id), courier, new HashMap<String, Profile>(), false));
    }

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
    void search(String id) throws IOException {
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
        if (now > delivery.getSearchUntil()) {
            return tryMove(id, DeliveryState.NO_COURIERS, "", null, null, null)
                    ? new Change(id, DeliveryState.NO_COURIERS, delivery.getCustomer(), "", "", null) : null;
        }
        chosen[0] = matcher.pick(delivery, now - delivery.getRequestedAt());
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
        if (!matcher.fits(delivery, courier, now - delivery.getRequestedAt())) {
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
        return deliveries.findByStateAndOfferExpiresAtLessThan(DeliveryState.OFFERED, now);
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
    static final class Change {
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
        pushes.deliveryChanged(change.id, change.state, change.customer, change.courier);
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
        long now = System.currentTimeMillis();
        int moved;
        if (newOfferExpiresAt != null) {
            moved = deliveries.offer(id, sources, to, now, newCourier, newOfferExpiresAt.longValue());
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
        event.setCreatedAt(System.currentTimeMillis());
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

    /// The delivery as `viewer` sees it, read in a transaction of its own.
    @Transactional(readOnly = true)
    private DeliveryDto read(String id, String viewer) throws IOException {
        return view(id, viewer);
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
                if (DeliveryState.ACCEPTED == state) {
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
                    (delivery.getOfferExpiresAt() - System.currentTimeMillis()) / 1000L);
        }
        long fareCents = delivery.getFareCents();
        long serviceFee = delivery.getFeeCents();
        long tip = delivery.getTipCents();
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
                delivery.getRequestedAt(),
                delivery.getUpdatedAt(),
                delivery.getStars(),
                delivery.getPaymentStatus(),
                offerExpiresInSeconds,
                delivery.getProduct(),
                delivery.getMethodLabel(),
                Payments.CASH.equals(delivery.getMethodId()),
                serviceFee,
                tip,
                fareCents + serviceFee + tip,
                courierPhotoInitials,
                etaMinutes,
                delivery.isWomenOnly() ? WOMEN : COURIER_GENDERS[0],
                delivery.isQuiet(),
                delivery.isWheelchair(),
                delivery.isPets(),
                delivery.getNote(),
                courierPhone,
                customerPhone);
    }

    /// Minutes for a car to drive from one point to another, by the same
    /// estimate the fares are made with. Never less than one.
    public static int etaMinutes(double fromLat, double fromLng, double toLat, double toLng) {
        double seconds = Geo.meters(fromLat, fromLng, toLat, toLng) * Fares.ROAD_FACTOR
                / Fares.METERS_PER_SECOND;
        return Math.max(1, (int) Math.ceil(seconds / 60d));
    }

    private static String courierGender(DeliveryRequestDto request) {
        return Text.oneOf(request.courierGender(), COURIER_GENDERS, COURIER_GENDERS[0], "the courier");
    }

    private Profile profile(String username, Map<String, Profile> cache) {
        if (cache.containsKey(username)) {
            return cache.get(username);
        }
        Profile row = profiles.findById(username);
        cache.put(username, row);
        return row;
    }

    private static void validate(DeliveryRequestDto request) {
        if (request == null || !Geo.isPoint(request.pickupLat(), request.pickupLng())
                || !Geo.isPoint(request.dropoffLat(), request.dropoffLng())) {
            throw new ResponseStatusException(400, "Choose where the delivery starts and ends");
        }
        double meters = Geo.meters(request.pickupLat(), request.pickupLng(), request.dropoffLat(),
                request.dropoffLng());
        if (meters < MIN_METERS) {
            throw new ResponseStatusException(400, "That is too close to need a delivery");
        }
        if (meters > MAX_METERS) {
            throw new ResponseStatusException(400, "That is further than we go");
        }
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
