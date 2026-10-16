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
package com.codenameone.examples.wayline.ride;

import com.codename1.backend.ConcurrencyFailureException;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codename1.orm.repository.PageRequest;
import com.codename1.orm.session.Identifier;
import com.codename1.orm.session.PersistenceException;
import com.codenameone.examples.wayline.Ids;
import com.codenameone.examples.wayline.Text;
import com.codenameone.examples.wayline.account.Moderation;
import com.codenameone.examples.wayline.api.FareOptionDto;
import com.codenameone.examples.wayline.api.FareQuoteDto;
import com.codenameone.examples.wayline.api.NearbyDriverDto;
import com.codenameone.examples.wayline.api.PricingDto;
import com.codenameone.examples.wayline.api.RideDto;
import com.codenameone.examples.wayline.api.RideRequestDto;
import com.codenameone.examples.wayline.api.RideState;
import com.codenameone.examples.wayline.domain.DriverState;
import com.codenameone.examples.wayline.domain.Profile;
import com.codenameone.examples.wayline.domain.Ride;
import com.codenameone.examples.wayline.domain.RideEvent;
import com.codenameone.examples.wayline.domain.RidePassed;
import com.codenameone.examples.wayline.geo.Geo;
import com.codenameone.examples.wayline.live.LiveHub;
import com.codenameone.examples.wayline.pay.Payments;
import com.codenameone.examples.wayline.push.RidePushes;
import com.codenameone.examples.wayline.repository.DriverStateRepository;
import com.codenameone.examples.wayline.repository.ProfileRepository;
import com.codenameone.examples.wayline.repository.RideEventRepository;
import com.codenameone.examples.wayline.repository.RidePassedRepository;
import com.codenameone.examples.wayline.repository.RideRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Rides: asking for one, finding it a driver, and taking it through to the end.
///
/// Every change of state goes through [#move], which makes it with a single
/// conditional update ([RideStateMachine]) and records it. The people concerned
/// are then told over the live channel. What they are told is only that the
/// ride changed; they read the ride itself from here, so there is one account
/// of a ride and the live channel cannot disagree with it.
///
/// That is also why the telling waits for the change to be committed. An app
/// told of a change asks for the ride at once, over another connection, and
/// must find it changed. So the methods that change a ride come in pairs: a
/// private one that is the transaction, and the public one that calls it and,
/// once it has returned, tells whoever is concerned.
@Component
public class Rides {
    /// Rides shorter than this are not rides, and longer than this are not this
    /// service's.
    static final double MIN_METERS = 100d;
    static final double MAX_METERS = 150000d;
    private static final String[] DRIVER_GENDERS = {"any", "women"};
    private static final String WOMEN = "women";
    /// What a rider is told about each kind of ride: its name, a line about
    /// it, and how many it seats. In the order of [Fares#PRODUCTS].
    private static final String[][] PRODUCT_TEXT = {
        {"Wayline", "Affordable everyday rides", "4"},
        {"Comfort", "Newer cars with extra legroom", "4"},
        {"XL", "Room for up to six", "6"}};
    /// How many drivers one search may choose before it leaves the ride to the
    /// next tick; see [#search].
    private static final int SEARCH_TRIES = 3;
    /// The two people a move can be held to; see [#tryMove].
    private static final String RIDER = "rider";
    private static final String DRIVER = "driver";
    private static final RideState[] ACTIVE = {RideState.REQUESTED, RideState.OFFERED,
        RideState.ACCEPTED, RideState.ARRIVED, RideState.IN_PROGRESS};
    private static final RideState[] WITH_DRIVER = {RideState.OFFERED, RideState.ACCEPTED,
        RideState.ARRIVED, RideState.IN_PROGRESS};
    private static final RideState[] ENDED_WITH_DRIVER = {RideState.COMPLETED,
        RideState.CANCELLED_BY_RIDER, RideState.CANCELLED_BY_DRIVER,
        RideState.CANCELLED_BY_ADMIN};

    private final RideRepository rides;
    private final RideEventRepository events;
    private final RidePassedRepository passes;
    private final DriverStateRepository drivers;
    private final ProfileRepository profiles;
    private final Matcher matcher;
    private final Fares fares;
    private final Payments payments;
    private final LiveHub live;
    private final RidePushes pushes;
    private final Moderation moderation;
    private final long offerMillis;
    private final long searchMillis;

    /// @param offerSeconds how long a driver has to answer an offer
    /// @param searchSeconds how long a ride looks for a driver before giving up
    public Rides(RideRepository rides, RideEventRepository events, RidePassedRepository passes,
            DriverStateRepository drivers, ProfileRepository profiles, Matcher matcher, Fares fares, Payments payments, LiveHub live, RidePushes pushes,
            Moderation moderation,
            @Value("${wayline.offer.seconds:15}") int offerSeconds,
            @Value("${wayline.search.seconds:90}") int searchSeconds) {
        this.rides = rides;
        this.events = events;
        this.passes = passes;
        this.drivers = drivers;
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

    // ---------------------------------------------------------------- riders

    @Transactional(readOnly = true)
    public FareQuoteDto quote(RideRequestDto request) throws IOException {
        validate(request);
        double distanceMeters = fares.distanceMeters(request);
        double durationSeconds = fares.durationSeconds(request, distanceMeters);
        String wanted = Fares.product(request.product());
        PricingDto pricing = fares.pricing();
        // The drivers who could take it as the rider asked for it, of every
        // kind of car, nearest first; each kind then counts its own.
        List<Matcher.Candidate> near = matcher.nearby(request.pickupLat(), request.pickupLng(),
                Matcher.FAR_METERS, null, WOMEN.equals(driverGender(request)),
                request.accessibleVehicle(), request.petFriendly());
        List<FareOptionDto> options = new ArrayList<FareOptionDto>();
        long amountCents = 0L;
        int driversNearby = 0;
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
                // The quote's own fields are the kind of ride that was asked
                // about, which is the standard one for an app that names none.
                amountCents = price.fare;
                driversNearby = nearby;
                etaSeconds = eta;
            }
        }
        return new FareQuoteDto(amountCents, fares.currency(), distanceMeters, durationSeconds,
                driversNearby, etaSeconds, options);
    }

    /// The cars near a point, for the map a rider looks at before asking:
    /// where they are and what kind, and nothing about who drives them. The
    /// positions are rounded to about ten metres, and there are at most twenty.
    @Transactional(readOnly = true)
    public List<NearbyDriverDto> nearbyDrivers(double lat, double lng) throws IOException {
        if (!Geo.isPoint(lat, lng)) {
            throw new ResponseStatusException(400, "That is not a place");
        }
        List<Matcher.Candidate> near = matcher.nearby(lat, lng, Matcher.FAR_METERS);
        List<NearbyDriverDto> out = new ArrayList<NearbyDriverDto>();
        for (int iter = 0; iter < near.size() && iter < 20; iter++) {
            Matcher.Candidate car = near.get(iter);
            out.add(new NearbyDriverDto(Math.round(car.lat * 10000d) / 10000d,
                    Math.round(car.lng * 10000d) / 10000d, car.heading, car.product));
        }
        return out;
    }

    /// Asks for a ride and offers it to the nearest driver straight away.
    public RideDto request(String rider, RideRequestDto request) throws IOException {
        validate(request);
        String id = attempt(() -> open(rider, request));
        live.rideChanged(id, RideState.REQUESTED, rider, "", "");
        search(id);
        return read(id, rider);
    }

    @Transactional
    private String open(String rider, RideRequestDto request) throws IOException {
        // Before anything is read: two requests from one rider take turns
        // here, so the second looks for a ride under way only once the first
        // has committed its own. Without it both look, both find none, and
        // the rider has two rides.
        profiles.hold(rider);
        Profile profile = profiles.findById(rider);
        if (profile == null || !profile.isPhoneVerified()) {
            throw new ResponseStatusException(403, "Verify your phone number before you ride");
        }
        if (rides.findFirstByRiderAndStateIn(rider, ACTIVE) != null) {
            throw new ResponseStatusException(409, "You already have a ride under way");
        }
        String product = Fares.product(request.product());
        boolean womenOnly = WOMEN.equals(driverGender(request));
        // Refused here, before there is a ride, when the card named is not the
        // rider's. With none named it is their default, which is cash until
        // they save a card.
        String[] method = payments.choose(rider, request.paymentMethodId());
        double meters = fares.distanceMeters(request);
        double seconds = fares.durationSeconds(request, meters);
        PricingDto pricing = fares.pricing();
        Fares.Price price = Fares.price(pricing, meters, seconds, product);
        long now = System.currentTimeMillis();
        // The fare is fixed here, from the server's own arithmetic, and so is
        // the driver's share of it: a change of prices or of commission made
        // while the ride is under way is for the rides after it. The quote the
        // rider was shown is not sent back and would not be believed.
        Ride ride = new Ride();
        ride.setId(Ids.next());
        ride.setState(RideState.REQUESTED);
        ride.setRider(rider);
        ride.setPickupLat(request.pickupLat());
        ride.setPickupLng(request.pickupLng());
        ride.setPickupAddress(address(request.pickupAddress()));
        ride.setDropoffLat(request.dropoffLat());
        ride.setDropoffLng(request.dropoffLng());
        ride.setDropoffAddress(address(request.dropoffAddress()));
        ride.setFareCents(price.fare);
        ride.setCurrency(fares.currency());
        ride.setDistanceMeters(meters);
        ride.setDurationSeconds(seconds);
        ride.setRequestedAt(now);
        ride.setUpdatedAt(now);
        ride.setSearchUntil(now + searchMillis);
        ride.setPaymentStatus(Payments.PENDING);
        ride.setProduct(product);
        ride.setMethodId(method[0]);
        ride.setMethodLabel(method[1]);
        ride.setBaseCents(price.base);
        ride.setDistanceCents(price.distance);
        ride.setTimeCents(price.time);
        ride.setFeeCents(price.fee);
        ride.setDriverCents(price.fare - Fares.commission(pricing, price.fare));
        ride.setWomenOnly(womenOnly);
        ride.setQuiet(request.quietRide());
        ride.setWheelchair(request.accessibleVehicle());
        ride.setNote(address(request.note()));
        ride.setPets(request.petFriendly());
        rides.insert(ride);
        event(ride.getId(), RideState.REQUESTED, rider);
        return ride.getId();
    }

    // tag::wayline-service-readonly[]
    @Transactional(readOnly = true)
    public RideDto activeForRider(String rider) throws IOException {
        Ride ride = rides.findFirstByRiderAndStateIn(rider, ACTIVE);
        return ride == null ? none() : toDto(ride, rider, new HashMap<String, Profile>());
    }
    // end::wayline-service-readonly[]

    @Transactional(readOnly = true)
    public List<RideDto> historyForRider(String rider) throws IOException {
        return list(rides.findTop50ByRiderOrderByRequestedAtDesc(rider), rider);
    }

    // tag::wayline-service-transaction[]
    public RideDto cancelByRider(String rider, String id) throws IOException {
        Change change = attempt(() -> cancelAsRider(rider, id));
        // The driver it was offered to or who had taken it is told as well.
        return tell(change);
    }

    @Transactional
    private Change cancelAsRider(String rider, String id) throws IOException {
        Ride before = owned(id, RIDER, rider);
        move(id, RideState.CANCELLED_BY_RIDER, rider, RIDER, null, null);
        return new Change(id, RideState.CANCELLED_BY_RIDER, rider, before.getDriver(), "",
                view(id, rider));
    }
    // end::wayline-service-transaction[]

    /// Rates a completed ride, once, and counts it toward the driver's rating.
    public RideDto rate(String rider, String id, int stars) throws IOException {
        if (stars < 1 || stars > 5) {
            throw new ResponseStatusException(400, "A rating is 1 to 5 stars");
        }
        return attempt(() -> rated(rider, id, stars));
    }

    @Transactional
    private RideDto rated(String rider, String id, int stars) throws IOException {
        String driver = owned(id, RIDER, rider).getDriver();
        if (rides.rate(id, rider, stars) != 1) {
            throw new ResponseStatusException(409, "That ride cannot be rated now");
        }
        drivers.addRating(driver, stars);
        return view(id, rider);
    }

    // --------------------------------------------------------------- drivers

    /// The offer waiting for this driver's answer, or the ride they are on.
    @Transactional(readOnly = true)
    public RideDto activeForDriver(String driver) throws IOException {
        Ride ride = rides.findFirstByDriverAndStateIn(driver, WITH_DRIVER);
        return ride == null ? none() : toDto(ride, driver, new HashMap<String, Profile>());
    }

    @Transactional(readOnly = true)
    public List<RideDto> historyForDriver(String driver) throws IOException {
        return list(rides.findTop50ByDriverAndStateInOrderByRequestedAtDesc(driver,
                ENDED_WITH_DRIVER), driver);
    }

    public RideDto accept(String driver, String id) throws IOException {
        return tell(attempt(() -> step(driver, id, RideState.ACCEPTED)));
    }

    /// Passes on an offer. The ride goes back to looking, and not at this
    /// driver again.
    public RideDto decline(String driver, String id) throws IOException {
        tell(attempt(() -> pass(driver, id)));
        search(id);
        return none();
    }

    @Transactional
    private Change pass(String driver, String id) throws IOException {
        Ride ride = owned(id, DRIVER, driver);
        move(id, RideState.REQUESTED, driver, DRIVER, "", null);
        passed(id, driver);
        return new Change(id, RideState.REQUESTED, ride.getRider(), "", driver, null);
    }

    public RideDto arrived(String driver, String id) throws IOException {
        return tell(attempt(() -> step(driver, id, RideState.ARRIVED)));
    }

    public RideDto start(String driver, String id) throws IOException {
        return tell(attempt(() -> step(driver, id, RideState.IN_PROGRESS)));
    }

    /// Ends the trip and charges the rider.
    ///
    /// In three steps, and only the first and the last are transactions. The
    /// charge between them may be a call to a payment provider, which can take
    /// seconds, and a transaction held open across it would hold a connection
    /// for as long.
    public RideDto complete(String driver, String id) throws IOException {
        Ride ride = attempt(() -> finish(driver, id));
        // After the move, and only by the request whose update made it: a ride
        // completes once, so it is charged once.
        String status = payments.settle(ride);
        return tell(settled(driver, id, status));
    }

    @Transactional
    private Ride finish(String driver, String id) throws IOException {
        Ride ride = owned(id, DRIVER, driver);
        move(id, RideState.COMPLETED, driver, DRIVER, null, null);
        return ride;
    }

    /// How long a completed ride may wait for its payment before it is taken
    /// to have been left behind. Longer than any charge takes.
    private static final long UNSETTLED_MILLIS = 2L * 60L * 1000L;

    /// How long a completed ride's charge goes on being asked for. Short of
    /// the day a processor remembers the key a charge was first sent under.
    private static final long RETRY_MILLIS = Payments.RETRY_MILLIS;

    /// Finishes the payment of rides that completed and were never settled.
    ///
    /// Completing a ride commits before the card is charged, and what came of
    /// the charge is written after it. A server stopped between the two leaves
    /// a ride that is over and says its payment is pending, and nothing would
    /// come back for it: the driver's request cannot be made again, the ride
    /// having completed. This does. Settling is safe to repeat -- see
    /// [Payments#settle] -- so a ride whose card was charged just before the
    /// server stopped is recorded as paid and not charged again.
    ///
    /// A ride whose charge failed without the processor saying whether it took
    /// the money is left pending too, and is asked about again here, every
    /// minute for [#RETRY_MILLIS]. After that it stays pending and is not asked
    /// about: the key that makes asking twice safe is kept by a processor for a
    /// day, and a charge sent under it later than that is a second charge. What
    /// became of such a ride is for a person to find in the processor's records.
    @Scheduled(fixedDelay = 60000, initialDelay = 30000, lock = "wayline-settlement")
    public void settleLeftBehind() throws IOException {
        long now = System.currentTimeMillis();
        List<Ride> unsettled = unsettled(now - RETRY_MILLIS, now - UNSETTLED_MILLIS);
        Exception failed = null;
        for (int iter = 0; iter < unsettled.size(); iter++) {
            Ride ride = unsettled.get(iter);
            try {
                tell(settled(ride.getDriver(), ride.getId(), payments.settle(ride)));
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
    private List<Ride> unsettled(long after, long before) {
        return rides.unsettled(after, before, new PageRequest(0, 50));
    }

    @Transactional
    private Change settled(String driver, String id, String status) throws IOException {
        rides.paymentStatus(id, status);
        Ride ride = find(id);
        return new Change(id, ride.getState(), ride.getRider(), ride.getDriver(), "",
                toDto(ride, driver, new HashMap<String, Profile>()));
    }

    public RideDto cancelByDriver(String driver, String id) throws IOException {
        return tell(attempt(() -> step(driver, id, RideState.CANCELLED_BY_DRIVER)));
    }

    /// One of a driver's own moves of a ride that is theirs, and the ride as
    /// they see it afterwards.
    @Transactional
    private Change step(String driver, String id, RideState to) throws IOException {
        if (RideState.ACCEPTED == to) {
            // Blocking a driver is refused while they have a ride taken, and
            // takes the driver's profile to look. Taking an offer holds the
            // same row first and then asks whether the account is still
            // allowed: a block that committed meanwhile is seen here, and a
            // block that comes after this commits sees the accepted ride.
            // Either way no ride is left with a driver who can no longer
            // drive it.
            profiles.hold(driver);
            if (!matcher.enabled(driver)) {
                throw new ResponseStatusException(403, "This account has been blocked");
            }
        }
        owned(id, DRIVER, driver);
        move(id, to, driver, DRIVER, null, null);
        Ride ride = find(id);
        return new Change(id, ride.getState(), ride.getRider(), ride.getDriver(), "",
                toDto(ride, driver, new HashMap<String, Profile>()));
    }

    // ----------------------------------------------------------------- admin

    /// Ends a ride that has not ended, whoever has it and whatever it was
    /// doing. Nothing is charged: a ride pays when it completes, and this one
    /// does not. The reason is kept with the rider's account.
    public RideDto cancelByAdmin(String by, String id, String reason) throws IOException {
        String why = Text.required(reason, 255, "why the ride is cancelled");
        return tell(attempt(() -> cancelAsAdmin(by, id, why)));
    }

    @Transactional
    private Change cancelAsAdmin(String by, String id, String why) throws IOException {
        Ride before = find(id);
        if (before == null) {
            throw new ResponseStatusException(404, "No such ride");
        }
        String rider = before.getRider();
        String driver = before.getDriver();
        move(id, RideState.CANCELLED_BY_ADMIN, by, null, null, null);
        moderation.record(rider, by, "cancel_ride", why);
        return new Change(id, RideState.CANCELLED_BY_ADMIN, rider, driver, "",
                toDto(find(id), driver, new HashMap<String, Profile>(), false));
    }

    // ---------------------------------------------------------------- anyone

    /// A ride, for someone entitled to see it: its rider, its driver, or an
    /// admin. Anyone else is told it does not exist, which is all they should
    /// learn about it.
    @Transactional(readOnly = true)
    public RideDto get(String viewer, boolean admin, String id) throws IOException {
        Ride ride = find(id);
        if (ride == null || !(admin || viewer.equals(ride.getRider())
                || viewer.equals(ride.getDriver()))) {
            throw new ResponseStatusException(404, "No such ride");
        }
        return toDto(ride, admin ? ride.getDriver() : viewer, new HashMap<String, Profile>(),
                !admin);
    }

    /// The most recent rides, for the admin.
    @Transactional(readOnly = true)
    public List<RideDto> recent() throws IOException {
        List<Ride> rows = rides.newest(new PageRequest(0, 100));
        Map<String, Profile> cache = new HashMap<String, Profile>();
        List<RideDto> out = new ArrayList<RideDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Ride ride = rows.get(iter);
            out.add(toDto(ride, ride.getDriver(), cache, false));
        }
        return out;
    }

    // -------------------------------------------------------------- matching

    /// Offers a waiting ride to the nearest driver who has not passed on it.
    /// Does nothing when nobody is in reach; [#tick] tries again.
    ///
    /// In two transactions: one that chooses a driver and one that makes the
    /// offer. Choosing reads who is free, and two rides asked for together
    /// each read the same driver as free; the second transaction is where
    /// they take turns over that driver, and it has to open with that -- see
    /// [#offer].
    ///
    /// A driver the offer finds no longer fit for the ride is passed over for
    /// the next, as a decline or a lapse would be, without waiting for the
    /// next tick: choosing again reads them as they now are, and does not pick
    /// them. Bounded, so drivers changing faster than this can choose cost the
    /// ride a tick and no more.
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

    /// Gives up on a ride that has waited too long, or names the driver to
    /// offer it to in `chosen`. Returns what it changed, or null for nothing.
    @Transactional
    private Change choose(String id, String[] chosen) throws IOException {
        chosen[0] = null;
        Ride ride = find(id);
        if (ride == null || RideState.REQUESTED != ride.getState()) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now > ride.getSearchUntil()) {
            return tryMove(id, RideState.NO_DRIVERS, "", null, null, null)
                    ? new Change(id, RideState.NO_DRIVERS, ride.getRider(), "", "", null) : null;
        }
        chosen[0] = matcher.pick(ride, now - ride.getRequestedAt());
        return null;
    }

    // tag::wayline-ride-offer[]
    /// Offers a ride to the driver chosen for it, unless they were given
    /// another in the meantime. Returns what it changed, or null for nothing,
    /// in which case [#tick] looks again.
    ///
    /// The driver is held before anything is read, so two rides that chose
    /// the same driver come through here one after the other, and the second
    /// reads the offer the first one committed. The update of the ride cannot
    /// do this on its own: it is conditional on its own ride's row, and each
    /// of the two has a row of its own.
    ///
    /// Everything that made the driver the choice is asked again once they
    /// are held: a driver who went off line, drove out of reach, lost their
    /// approval or was blocked since [#choose] read them would otherwise get
    /// the offer and keep the rider waiting for it to lapse. Not private, so
    /// that a test can make the offer to a driver who changed in between.
    @Transactional
    Change offer(String id, String driver) throws IOException {
        if (drivers.hold(driver) == 0
                || rides.findFirstByDriverAndStateIn(driver, WITH_DRIVER) != null) {
            return null;
        }
        Ride ride = find(id);
        if (ride == null || RideState.REQUESTED != ride.getState()) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (!matcher.fits(ride, driver, now - ride.getRequestedAt())) {
            return null;
        }
        return tryMove(id, RideState.OFFERED, "", null, driver, Long.valueOf(now + offerMillis))
                ? new Change(id, RideState.OFFERED, ride.getRider(), driver, "", null) : null;
    }
    // end::wayline-ride-offer[]

    /// Takes back the offers nobody answered and looks again for every ride
    /// still waiting. Runs every two seconds, on one instance at a time: `lock`
    /// keeps a second server from offering the same ride to two drivers.
    ///
    /// Not a transaction itself. Each ride is dealt with in one of its own, so
    /// a ride that cannot be dealt with costs nobody else their turn; what went
    /// wrong with the first such ride is reported once the rest are done.
    @Scheduled(fixedDelay = 2000, initialDelay = 2000, lock = "wayline-matching")
    public void tick() throws IOException {
        Exception failed = null;
        List<Ride> lapsed = lapsedOffers(System.currentTimeMillis());
        for (int iter = 0; iter < lapsed.size(); iter++) {
            Ride ride = lapsed.get(iter);
            try {
                tell(attempt(() -> lapse(ride.getId(), ride.getRider(), ride.getDriver())));
            } catch (IOException err) {
                failed = failed == null ? err : failed;
            } catch (RuntimeException err) {
                failed = failed == null ? err : failed;
            }
        }
        List<Ride> waiting = waitingRides();
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
    private List<Ride> lapsedOffers(long now) throws IOException {
        return rides.findByStateAndOfferExpiresAtLessThan(RideState.OFFERED, now);
    }

    @Transactional(readOnly = true)
    private List<Ride> waitingRides() throws IOException {
        return rides.findByStateOrderByRequestedAtAsc(RideState.REQUESTED);
    }

    /// Takes a lapsed offer back from the driver it was made to, or changes
    /// nothing and answers null.
    @Transactional
    private Change lapse(String id, String rider, String driver) throws IOException {
        // Held to the driver it lapsed for: if they accepted in the instant
        // since the query, this changes nothing and the ride is theirs.
        if (!tryMove(id, RideState.REQUESTED, driver, DRIVER, "", null)) {
            return null;
        }
        passed(id, driver);
        return new Change(id, RideState.REQUESTED, rider, "", driver, null);
    }

    // ------------------------------------------------------------- internals

    /// What a transaction did to a ride, for telling the people concerned once
    /// it has committed, and the ride as the caller is to see it. Not private:
    /// the transactional methods answer it, and the class the build writes to
    /// begin and commit their transactions has to be able to name it.
    static final class Change {
        final String id;
        final RideState state;
        final String rider;
        final String driver;
        /// Someone else to tell: the driver an offer was taken back from.
        final String also;
        final RideDto view;

        Change(String id, RideState state, String rider, String driver, String also, RideDto view) {
            this.id = id;
            this.state = state;
            this.rider = rider;
            this.driver = driver;
            this.also = also;
            this.view = view;
        }
    }

    /// Tells the people concerned about a change that has been committed, and
    /// answers the view that came with it. Nothing changed, nothing said.
    private RideDto tell(Change change) {
        if (change == null) {
            return null;
        }
        live.rideChanged(change.id, change.state, change.rider, change.driver, change.also);
        // The same moment, and for the same reason: a phone told of a change
        // must find it made.
        pushes.rideChanged(change.id, change.state, change.rider, change.driver);
        return change.view;
    }

    /// One of the transactions of this class, for [#attempt] to run.
    private interface Work<T> {
        T run() throws IOException;
    }

    /// Runs a transaction that changes a ride, and runs it again if the database
    /// ended it because another transaction changed the same ride first.
    ///
    /// Every change here is a ride read and then moved by a conditional update,
    /// and two of them racing are meant to be settled by that update: the loser
    /// changes no rows. Most databases do exactly that. One that holds a
    /// transaction to the snapshot it first read from -- MariaDB, as it is
    /// configured out of the box -- refuses the loser's update instead, because
    /// the ride it read is no longer the ride that is there, and ends its
    /// transaction with a [ConcurrencyFailureException].
    ///
    /// That is the race lost and not a fault, so it gets the answer the other
    /// databases give. The transaction was rolled back as the exception left
    /// it, so nothing of it remains; run again, it reads the ride as the winner
    /// left it and its update changes no rows. This is the one place that
    /// happens, and it has to be outside the transaction: the database has
    /// already ended that one, and a statement sent after the failure would run
    /// on its own or be refused, depending on the database.
    ///
    /// Three attempts, because each one lost means someone else moved the ride,
    /// and a ride has only so many moves in it.
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

    /// Moves a ride to `to`, or answers 409 when its state does not allow it.
    private void move(String id, RideState to, String actor, String ownerField, String newDriver,
            Long newOfferExpiresAt) throws IOException {
        if (!tryMove(id, to, actor, ownerField, newDriver, newOfferExpiresAt)) {
            Ride now = find(id);
            throw new ResponseStatusException(409, "That ride is "
                    + (now == null ? "gone" : describe(now.getState())));
        }
    }

    /// The conditional update behind every change of state. True when this call
    /// is the one that made the change.
    ///
    /// The session is empty afterwards, every move in `RideRepository` being
    /// marked `clearAutomatically`: a ride read before this is no longer the
    /// session's, and is read again by whoever needs it as it is now.
    ///
    /// @param ownerField [#RIDER] or [#DRIVER] to hold the change to rides
    ///     whose rider or driver is `actor`, or null for a change the server
    ///     makes itself
    /// @param newDriver the driver to write with the change, or null to leave
    ///     it as it is
    /// @param newOfferExpiresAt when the offer this change makes lapses, or null
    // tag::wayline-ride-trymove[]
    private boolean tryMove(String id, RideState to, String actor, String ownerField,
            String newDriver, Long newOfferExpiresAt) throws IOException {
        RideState[] sources = RideStateMachine.sources(to);
        if (sources.length == 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        int moved;
        if (newOfferExpiresAt != null) {
            moved = rides.offer(id, sources, to, now, newDriver, newOfferExpiresAt.longValue());
        } else if (RIDER.equals(ownerField)) {
            moved = rides.moveAsRider(id, sources, to, now, actor);
        } else if (!DRIVER.equals(ownerField)) {
            moved = rides.moveByServer(id, sources, to, now);
        } else if (newDriver != null) {
            moved = rides.giveBack(id, sources, to, now, actor, newDriver);
        } else if (RideState.ACCEPTED == to) {
            // An offer that has lapsed cannot be taken, even in the moment
            // before the sweep takes it back.
            moved = rides.takeOffer(id, sources, to, now, actor);
        } else {
            moved = rides.moveAsDriver(id, sources, to, now, actor);
        }
        if (moved != 1) {
            return false;
        }
        event(id, to, actor);
        return true;
    }
    // end::wayline-ride-trymove[]

    private void event(String id, RideState state, String actor) throws IOException {
        RideEvent event = new RideEvent();
        event.setId(Ids.next());
        event.setRideId(id);
        event.setState(state);
        event.setActor(actor);
        event.setCreatedAt(System.currentTimeMillis());
        events.insert(event);
    }

    private void passed(String id, String driver) {
        if (!passes.existsById(Identifier.of(id, driver))) {
            RidePassed passed = new RidePassed();
            passed.setRideId(id);
            passed.setDriver(driver);
            passes.insert(passed);
        }
    }

    /// The ride, when `actor` is its rider or its driver, whichever `field`
    /// names; 404 otherwise.
    private Ride owned(String id, String field, String actor) {
        Ride ride = find(id);
        if (ride == null || !actor.equals(RIDER.equals(field) ? ride.getRider()
                : ride.getDriver())) {
            throw new ResponseStatusException(404, "No such ride");
        }
        return ride;
    }

    /// The ride of this id, or null when there is none. An id that cannot be
    /// one is not looked for.
    private Ride find(String id) {
        if (id == null || id.length() == 0 || id.length() > 32) {
            return null;
        }
        return rides.findById(id);
    }

    /// The ride as `viewer` sees it, read in a transaction of its own.
    @Transactional(readOnly = true)
    private RideDto read(String id, String viewer) throws IOException {
        return view(id, viewer);
    }

    private RideDto view(String id, String viewer) throws IOException {
        return toDto(find(id), viewer, new HashMap<String, Profile>());
    }

    private List<RideDto> list(List<Ride> rows, String viewer) throws IOException {
        Map<String, Profile> cache = new HashMap<String, Profile>();
        List<RideDto> out = new ArrayList<RideDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(toDto(rows.get(iter), viewer, cache));
        }
        return out;
    }

    static RideDto none() {
        return RideDto.none();
    }

    private RideDto toDto(Ride ride, String viewer, Map<String, Profile> cache)
            throws IOException {
        return toDto(ride, viewer, cache, true);
    }

    /// @param party whether `viewer` is asking as the ride's rider or driver.
    ///     An admin is shown a ride as its driver sees it, and is not a party:
    ///     the two phone numbers are for the two people who have to find each
    ///     other, and only while they do.
    private RideDto toDto(Ride ride, String viewer, Map<String, Profile> cache, boolean party)
            throws IOException {
        RideState state = ride.getState();
        // An account's name is the e-mail it signs in with. The other party
        // to a ride is shown a display name, and a phone number while the two
        // are meeting; the address is for its owner and for an admin.
        boolean asRider = !party || (viewer != null && viewer.equals(ride.getRider()));
        boolean asDriver = !party || (viewer != null && viewer.equals(ride.getDriver()));
        Profile rider = profile(ride.getRider(), cache);
        // What is known of the driver, filled in below for a ride that has one
        // this viewer may see.
        String driverUsername = null;
        String driverName = null;
        String driverPhotoInitials = "";
        String vehicle = null;
        String plate = null;
        String driverPhone = null;
        String riderPhone = null;
        double driverRating = 0d;
        double driverLat = 0d;
        double driverLng = 0d;
        double driverHeading = 0d;
        int etaMinutes = 0;
        int offerExpiresInSeconds = 0;
        String driver = ride.getDriver();
        boolean offered = RideState.OFFERED == state;
        // An offer is between the server and one driver. Until it is taken the
        // rider has no driver, and is not shown who was asked.
        if (driver.length() > 0 && (!offered || driver.equals(viewer))) {
            if (asDriver) {
                driverUsername = driver;
            }
            Profile who = profile(driver, cache);
            if (who != null) {
                driverName = who.getDisplayName();
                driverPhotoInitials = Text.initials(driverName);
                vehicle = who.getVehicle();
                plate = who.getPlate();
            }
            boolean meeting = RideState.ACCEPTED == state
                    || RideState.ARRIVED == state
                    || RideState.IN_PROGRESS == state;
            if (party && meeting && viewer != null) {
                if (viewer.equals(ride.getRider()) && who != null) {
                    driverPhone = who.getPhone();
                } else if (viewer.equals(driver) && rider != null) {
                    riderPhone = rider.getPhone();
                }
            }
            DriverState car = drivers.findById(driver);
            if (car != null) {
                driverRating = car.rating();
            }
            // Where the car is belongs to the ride while it brings the two
            // together and carries one of them. A ride that has ended stays in
            // its rider's history for good, and the driver goes on reporting
            // where they are for other people's rides: read from here, a past
            // rider could follow them indefinitely.
            if (car != null && meeting) {
                driverLat = car.getLat();
                driverLng = car.getLng();
                driverHeading = car.getHeading();
                // How long until the car is where it is going next: the pickup
                // until the rider is in it, the destination after.
                if (RideState.ACCEPTED == state) {
                    etaMinutes = etaMinutes(driverLat, driverLng, ride.getPickupLat(),
                            ride.getPickupLng());
                } else if (RideState.IN_PROGRESS == state) {
                    etaMinutes = etaMinutes(driverLat, driverLng, ride.getDropoffLat(),
                            ride.getDropoffLng());
                }
            }
        }
        if (offered) {
            offerExpiresInSeconds = (int) Math.max(0L,
                    (ride.getOfferExpiresAt() - System.currentTimeMillis()) / 1000L);
        }
        long fareCents = ride.getFareCents();
        long serviceFee = ride.getFeeCents();
        long tip = ride.getTipCents();
        return new RideDto(
                ride.getId(),
                state,
                asRider ? ride.getRider() : null,
                rider == null ? "" : rider.getDisplayName(),
                driverUsername,
                driverName,
                vehicle,
                plate,
                driverRating,
                ride.getPickupLat(),
                ride.getPickupLng(),
                ride.getPickupAddress(),
                ride.getDropoffLat(),
                ride.getDropoffLng(),
                ride.getDropoffAddress(),
                driverLat,
                driverLng,
                driverHeading,
                fareCents,
                ride.getCurrency(),
                ride.getDistanceMeters(),
                ride.getDurationSeconds(),
                ride.getRequestedAt(),
                ride.getUpdatedAt(),
                ride.getStars(),
                ride.getPaymentStatus(),
                offerExpiresInSeconds,
                ride.getProduct(),
                ride.getMethodLabel(),
                Payments.CASH.equals(ride.getMethodId()),
                serviceFee,
                tip,
                fareCents + serviceFee + tip,
                driverPhotoInitials,
                etaMinutes,
                ride.isWomenOnly() ? WOMEN : DRIVER_GENDERS[0],
                ride.isQuiet(),
                ride.isWheelchair(),
                ride.isPets(),
                ride.getNote(),
                driverPhone,
                riderPhone);
    }

    /// Minutes for a car to drive from one point to another, by the same
    /// estimate the fares are made with. Never less than one.
    public static int etaMinutes(double fromLat, double fromLng, double toLat, double toLng) {
        double seconds = Geo.meters(fromLat, fromLng, toLat, toLng) * Fares.ROAD_FACTOR
                / Fares.METERS_PER_SECOND;
        return Math.max(1, (int) Math.ceil(seconds / 60d));
    }

    private static String driverGender(RideRequestDto request) {
        return Text.oneOf(request.driverGender(), DRIVER_GENDERS, DRIVER_GENDERS[0], "the driver");
    }

    private Profile profile(String username, Map<String, Profile> cache) {
        if (cache.containsKey(username)) {
            return cache.get(username);
        }
        Profile row = profiles.findById(username);
        cache.put(username, row);
        return row;
    }

    private static void validate(RideRequestDto request) {
        if (request == null || !Geo.isPoint(request.pickupLat(), request.pickupLng())
                || !Geo.isPoint(request.dropoffLat(), request.dropoffLng())) {
            throw new ResponseStatusException(400, "Choose where the ride starts and ends");
        }
        double meters = Geo.meters(request.pickupLat(), request.pickupLng(), request.dropoffLat(),
                request.dropoffLng());
        if (meters < MIN_METERS) {
            throw new ResponseStatusException(400, "That is too close to need a ride");
        }
        if (meters > MAX_METERS) {
            throw new ResponseStatusException(400, "That is further than we go");
        }
    }

    private static String address(String value) {
        String text = value == null ? "" : value.trim();
        return text.length() > 255 ? text.substring(0, 255) : text;
    }

    private static String describe(RideState state) {
        if (RideState.COMPLETED == state) {
            return "already completed";
        }
        if (RideState.IN_PROGRESS == state) {
            return "already under way";
        }
        if (RideState.ACCEPTED == state || RideState.ARRIVED == state) {
            return "already taken";
        }
        if (RideState.REQUESTED == state || RideState.OFFERED == state) {
            return "no longer on offer to you";
        }
        return "no longer available";
    }
}
