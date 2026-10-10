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
package com.codenameone.examples.wayline.ride;

import com.codename1.backend.ConcurrencyFailureException;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codename1.orm.session.PersistenceException;
import com.codenameone.examples.wayline.Ids;
import com.codenameone.examples.wayline.Text;
import com.codenameone.examples.wayline.account.Moderation;
import com.codenameone.examples.wayline.account.ProfileRepository;
import com.codenameone.examples.wayline.api.FareOptionDto;
import com.codenameone.examples.wayline.api.FareQuoteDto;
import com.codenameone.examples.wayline.api.NearbyDriverDto;
import com.codenameone.examples.wayline.api.PricingDto;
import com.codenameone.examples.wayline.api.RideDto;
import com.codenameone.examples.wayline.api.RideRequestDto;
import com.codenameone.examples.wayline.api.RideStates;
import com.codenameone.examples.wayline.domain.DriverState;
import com.codenameone.examples.wayline.domain.Profile;
import com.codenameone.examples.wayline.domain.Ride;
import com.codenameone.examples.wayline.domain.RideEvent;
import com.codenameone.examples.wayline.domain.RidePassed;
import com.codenameone.examples.wayline.driver.DriverRepository;
import com.codenameone.examples.wayline.geo.Geo;
import com.codenameone.examples.wayline.live.LiveHub;
import com.codenameone.examples.wayline.pay.Payments;

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

    private final RideRepository rides;
    private final DriverRepository drivers;
    private final ProfileRepository profiles;
    private final Matcher matcher;
    private final Fares fares;
    private final Payments payments;
    private final LiveHub live;
    private final Moderation moderation;
    private final long offerMillis;
    private final long searchMillis;

    /// @param offerSeconds how long a driver has to answer an offer
    /// @param searchSeconds how long a ride looks for a driver before giving up
    public Rides(RideRepository rides, DriverRepository drivers, ProfileRepository profiles,
            Matcher matcher, Fares fares, Payments payments, LiveHub live, Moderation moderation,
            @Value("${wayline.offer.seconds:15}") int offerSeconds,
            @Value("${wayline.search.seconds:90}") int searchSeconds) {
        this.rides = rides;
        this.drivers = drivers;
        this.profiles = profiles;
        this.moderation = moderation;
        this.matcher = matcher;
        this.fares = fares;
        this.payments = payments;
        this.live = live;
        offerMillis = offerSeconds * 1000L;
        searchMillis = searchSeconds * 1000L;
    }

    // ---------------------------------------------------------------- riders

    @Transactional(readOnly = true)
    public FareQuoteDto quote(RideRequestDto request) throws IOException {
        validate(request);
        FareQuoteDto quote = new FareQuoteDto();
        quote.distanceMeters = fares.distanceMeters(request);
        quote.durationSeconds = fares.durationSeconds(request, quote.distanceMeters);
        quote.currency = fares.currency();
        String wanted = Fares.product(request.product);
        PricingDto pricing = fares.pricing();
        // The drivers who could take it as the rider asked for it, of every
        // kind of car, nearest first; each kind then counts its own.
        List<Matcher.Candidate> near = matcher.nearby(request.pickupLat, request.pickupLng,
                Matcher.FAR_METERS, null, WOMEN.equals(driverGender(request)),
                request.accessibleVehicle, request.petFriendly);
        quote.options = new ArrayList<FareOptionDto>();
        for (int iter = 0; iter < Fares.PRODUCTS.length; iter++) {
            String product = Fares.PRODUCTS[iter];
            Fares.Price price = Fares.price(pricing, quote.distanceMeters, quote.durationSeconds,
                    product);
            FareOptionDto option = new FareOptionDto();
            option.product = product;
            option.name = PRODUCT_TEXT[iter][0];
            option.description = PRODUCT_TEXT[iter][1];
            option.seats = Integer.parseInt(PRODUCT_TEXT[iter][2]);
            option.fare = price.fare;
            option.serviceFee = price.fee;
            option.total = price.fare + price.fee;
            double nearest = -1d;
            for (int d = 0; d < near.size(); d++) {
                if (product.equals(near.get(d).product)) {
                    option.driversNearby++;
                    if (nearest < 0d) {
                        nearest = near.get(d).meters;
                    }
                }
            }
            int eta = nearest < 0d ? 0 : (int) Math.max(60d, nearest * Fares.ROAD_FACTOR
                    / Fares.METERS_PER_SECOND);
            option.etaMinutes = (eta + 59) / 60;
            quote.options.add(option);
            if (product.equals(wanted)) {
                // The quote's own fields are the kind of ride that was asked
                // about, which is the standard one for an app that names none.
                quote.amountCents = price.fare;
                quote.driversNearby = option.driversNearby;
                quote.etaSeconds = eta;
            }
        }
        return quote;
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
            NearbyDriverDto dto = new NearbyDriverDto();
            dto.lat = Math.round(car.lat * 10000d) / 10000d;
            dto.lng = Math.round(car.lng * 10000d) / 10000d;
            dto.heading = car.heading;
            dto.product = car.product;
            out.add(dto);
        }
        return out;
    }

    /// Asks for a ride and offers it to the nearest driver straight away.
    public RideDto request(String rider, RideRequestDto request) throws IOException {
        validate(request);
        String id = open(rider, request);
        live.rideChanged(id, RideStates.REQUESTED, rider, "", "");
        search(id);
        return read(id, rider);
    }

    @Transactional
    private String open(String rider, RideRequestDto request) throws IOException {
        Profile profile = profiles.find(rider);
        if (profile == null || !profile.phoneVerified) {
            throw new ResponseStatusException(403, "Verify your phone number before you ride");
        }
        if (rides.activeForRider(rider) != null) {
            throw new ResponseStatusException(409, "You already have a ride under way");
        }
        String product = Fares.product(request.product);
        boolean womenOnly = WOMEN.equals(driverGender(request));
        // Refused here, before there is a ride, when the card named is not the
        // rider's. With none named it is their default, which is cash until
        // they save a card.
        String[] method = payments.choose(rider, request.paymentMethodId);
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
        ride.id = Ids.next();
        ride.state = RideStates.REQUESTED;
        ride.rider = rider;
        ride.pickupLat = request.pickupLat;
        ride.pickupLng = request.pickupLng;
        ride.pickupAddress = address(request.pickupAddress);
        ride.dropoffLat = request.dropoffLat;
        ride.dropoffLng = request.dropoffLng;
        ride.dropoffAddress = address(request.dropoffAddress);
        ride.fareCents = price.fare;
        ride.currency = fares.currency();
        ride.distanceMeters = meters;
        ride.durationSeconds = seconds;
        ride.requestedAt = now;
        ride.updatedAt = now;
        ride.searchUntil = now + searchMillis;
        ride.paymentStatus = Payments.PENDING;
        ride.product = product;
        ride.methodId = method[0];
        ride.methodLabel = method[1];
        ride.baseCents = price.base;
        ride.distanceCents = price.distance;
        ride.timeCents = price.time;
        ride.feeCents = price.fee;
        ride.driverCents = price.fare - Fares.commission(pricing, price.fare);
        ride.womenOnly = womenOnly;
        ride.quiet = request.quietRide;
        ride.wheelchair = request.accessibleVehicle;
        ride.note = address(request.note);
        ride.pets = request.petFriendly;
        rides.add(ride);
        event(ride.id, RideStates.REQUESTED, rider);
        return ride.id;
    }

    @Transactional(readOnly = true)
    public RideDto activeForRider(String rider) throws IOException {
        Ride ride = rides.activeForRider(rider);
        return ride == null ? none() : toDto(ride, rider, new HashMap<String, Profile>());
    }

    @Transactional(readOnly = true)
    public List<RideDto> historyForRider(String rider) throws IOException {
        return list(rides.historyForRider(rider), rider);
    }

    public RideDto cancelByRider(String rider, String id) throws IOException {
        Change change = attempt(() -> cancelAsRider(rider, id));
        // The driver it was offered to or who had taken it is told as well.
        return tell(change);
    }

    @Transactional
    private Change cancelAsRider(String rider, String id) throws IOException {
        Ride before = owned(id, RideRepository.RIDER, rider);
        move(id, RideStates.CANCELLED_BY_RIDER, rider, RideRepository.RIDER, null, null);
        return new Change(id, RideStates.CANCELLED_BY_RIDER, rider, before.driver, "",
                view(id, rider));
    }

    /// Rates a completed ride, once, and counts it toward the driver's rating.
    public RideDto rate(String rider, String id, int stars) throws IOException {
        if (stars < 1 || stars > 5) {
            throw new ResponseStatusException(400, "A rating is 1 to 5 stars");
        }
        return attempt(() -> rated(rider, id, stars));
    }

    @Transactional
    private RideDto rated(String rider, String id, int stars) throws IOException {
        String driver = owned(id, RideRepository.RIDER, rider).driver;
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
        Ride ride = rides.activeForDriver(driver);
        return ride == null ? none() : toDto(ride, driver, new HashMap<String, Profile>());
    }

    @Transactional(readOnly = true)
    public List<RideDto> historyForDriver(String driver) throws IOException {
        return list(rides.historyForDriver(driver), driver);
    }

    public RideDto accept(String driver, String id) throws IOException {
        return tell(attempt(() -> step(driver, id, RideStates.ACCEPTED)));
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
        Ride ride = owned(id, RideRepository.DRIVER, driver);
        move(id, RideStates.REQUESTED, driver, RideRepository.DRIVER, "", null);
        passed(id, driver);
        return new Change(id, RideStates.REQUESTED, ride.rider, "", driver, null);
    }

    public RideDto arrived(String driver, String id) throws IOException {
        return tell(attempt(() -> step(driver, id, RideStates.ARRIVED)));
    }

    public RideDto start(String driver, String id) throws IOException {
        return tell(attempt(() -> step(driver, id, RideStates.IN_PROGRESS)));
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
        Ride ride = owned(id, RideRepository.DRIVER, driver);
        move(id, RideStates.COMPLETED, driver, RideRepository.DRIVER, null, null);
        return ride;
    }

    @Transactional
    private Change settled(String driver, String id, String status) throws IOException {
        rides.paymentStatus(id, status);
        Ride ride = rides.find(id);
        return new Change(id, ride.state, ride.rider, ride.driver, "",
                toDto(ride, driver, new HashMap<String, Profile>()));
    }

    public RideDto cancelByDriver(String driver, String id) throws IOException {
        return tell(attempt(() -> step(driver, id, RideStates.CANCELLED_BY_DRIVER)));
    }

    /// One of a driver's own moves of a ride that is theirs, and the ride as
    /// they see it afterwards.
    @Transactional
    private Change step(String driver, String id, String to) throws IOException {
        owned(id, RideRepository.DRIVER, driver);
        move(id, to, driver, RideRepository.DRIVER, null, null);
        Ride ride = rides.find(id);
        return new Change(id, ride.state, ride.rider, ride.driver, "",
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
        Ride before = rides.find(id);
        if (before == null) {
            throw new ResponseStatusException(404, "No such ride");
        }
        String rider = before.rider;
        String driver = before.driver;
        move(id, RideStates.CANCELLED_BY_ADMIN, by, null, null, null);
        moderation.record(rider, by, "cancel_ride", why);
        return new Change(id, RideStates.CANCELLED_BY_ADMIN, rider, driver, "",
                toDto(rides.find(id), driver, new HashMap<String, Profile>(), false));
    }

    // ---------------------------------------------------------------- anyone

    /// A ride, for someone entitled to see it: its rider, its driver, or an
    /// admin. Anyone else is told it does not exist, which is all they should
    /// learn about it.
    @Transactional(readOnly = true)
    public RideDto get(String viewer, boolean admin, String id) throws IOException {
        Ride ride = rides.find(id);
        if (ride == null || !(admin || viewer.equals(ride.rider)
                || viewer.equals(ride.driver))) {
            throw new ResponseStatusException(404, "No such ride");
        }
        return toDto(ride, admin ? ride.driver : viewer, new HashMap<String, Profile>(),
                !admin);
    }

    /// The most recent rides, for the admin.
    @Transactional(readOnly = true)
    public List<RideDto> recent() throws IOException {
        List<Ride> rows = rides.recent(100);
        Map<String, Profile> cache = new HashMap<String, Profile>();
        List<RideDto> out = new ArrayList<RideDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Ride ride = rows.get(iter);
            out.add(toDto(ride, ride.driver, cache, false));
        }
        return out;
    }

    // -------------------------------------------------------------- matching

    /// Offers a waiting ride to the nearest driver who has not passed on it.
    /// Does nothing when nobody is in reach; [#tick] tries again.
    void search(String id) throws IOException {
        tell(attempt(() -> offer(id)));
    }

    /// The transaction of [#search]: what it changed, or null for nothing.
    @Transactional
    private Change offer(String id) throws IOException {
        Ride ride = rides.find(id);
        if (ride == null || !RideStates.REQUESTED.equals(ride.state)) {
            return null;
        }
        long now = System.currentTimeMillis();
        String rider = ride.rider;
        if (now > ride.searchUntil) {
            return tryMove(id, RideStates.NO_DRIVERS, "", null, null, null)
                    ? new Change(id, RideStates.NO_DRIVERS, rider, "", "", null) : null;
        }
        String driver = matcher.pick(ride, now - ride.requestedAt);
        if (driver == null) {
            return null;
        }
        return tryMove(id, RideStates.OFFERED, "", null, driver, Long.valueOf(now + offerMillis))
                ? new Change(id, RideStates.OFFERED, rider, driver, "", null) : null;
    }

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
                tell(attempt(() -> lapse(ride.id, ride.rider, ride.driver)));
            } catch (IOException err) {
                failed = failed == null ? err : failed;
            } catch (RuntimeException err) {
                failed = failed == null ? err : failed;
            }
        }
        List<Ride> waiting = waitingRides();
        for (int iter = 0; iter < waiting.size(); iter++) {
            try {
                search(waiting.get(iter).id);
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
        return rides.lapsedOffers(now);
    }

    @Transactional(readOnly = true)
    private List<Ride> waitingRides() throws IOException {
        return rides.waiting();
    }

    /// Takes a lapsed offer back from the driver it was made to, or changes
    /// nothing and answers null.
    @Transactional
    private Change lapse(String id, String rider, String driver) throws IOException {
        // Held to the driver it lapsed for: if they accepted in the instant
        // since the query, this changes nothing and the ride is theirs.
        if (!tryMove(id, RideStates.REQUESTED, driver, RideRepository.DRIVER, "", null)) {
            return null;
        }
        passed(id, driver);
        return new Change(id, RideStates.REQUESTED, rider, "", driver, null);
    }

    // ------------------------------------------------------------- internals

    /// What a transaction did to a ride, for telling the people concerned once
    /// it has committed, and the ride as the caller is to see it. Not private:
    /// the transactional methods answer it, and the class the build writes to
    /// begin and commit their transactions has to be able to name it.
    static final class Change {
        final String id;
        final String state;
        final String rider;
        final String driver;
        /// Someone else to tell: the driver an offer was taken back from.
        final String also;
        final RideDto view;

        Change(String id, String state, String rider, String driver, String also, RideDto view) {
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
    private void move(String id, String to, String actor, String ownerField, String newDriver,
            Long newOfferExpiresAt) throws IOException {
        if (!tryMove(id, to, actor, ownerField, newDriver, newOfferExpiresAt)) {
            Ride now = rides.find(id);
            throw new ResponseStatusException(409, "That ride is "
                    + (now == null ? "gone" : describe(now.state)));
        }
    }

    /// The conditional update behind every change of state. True when this call
    /// is the one that made the change.
    ///
    /// The session is empty afterwards, as after any bulk statement: a ride
    /// read before this is no longer the session's, and is read again by
    /// whoever needs it as it is now.
    ///
    /// @param ownerField [RideRepository#RIDER] or [RideRepository#DRIVER] to
    ///     hold the change to rides whose rider or driver is `actor`, or null
    ///     for a change the server makes itself
    /// @param newDriver the driver to write with the change, or null
    /// @param newOfferExpiresAt when the offer this change makes lapses, or null
    private boolean tryMove(String id, String to, String actor, String ownerField,
            String newDriver, Long newOfferExpiresAt) throws IOException {
        String[] sources = RideStateMachine.sources(to);
        if (sources.length == 0) {
            return false;
        }
        // An offer that has lapsed cannot be taken, even in the moment before
        // the sweep takes it back.
        boolean offerOpen = RideStates.ACCEPTED.equals(to);
        if (rides.move(id, sources, to, System.currentTimeMillis(), ownerField, actor, newDriver,
                newOfferExpiresAt, offerOpen) != 1) {
            return false;
        }
        event(id, to, actor);
        return true;
    }

    private void event(String id, String state, String actor) throws IOException {
        RideEvent event = new RideEvent();
        event.id = Ids.next();
        event.rideId = id;
        event.state = state;
        event.actor = actor;
        event.createdAt = System.currentTimeMillis();
        rides.add(event);
    }

    private void passed(String id, String driver) {
        if (!rides.hasPassed(id, driver)) {
            RidePassed passed = new RidePassed();
            passed.rideId = id;
            passed.driver = driver;
            rides.add(passed);
        }
    }

    /// The ride, when `actor` is its rider or its driver, whichever `field`
    /// names; 404 otherwise.
    private Ride owned(String id, String field, String actor) {
        Ride ride = rides.find(id);
        if (ride == null || !actor.equals(RideRepository.RIDER.equals(field) ? ride.rider
                : ride.driver)) {
            throw new ResponseStatusException(404, "No such ride");
        }
        return ride;
    }

    /// The ride as `viewer` sees it, read in a transaction of its own.
    @Transactional(readOnly = true)
    private RideDto read(String id, String viewer) throws IOException {
        return view(id, viewer);
    }

    private RideDto view(String id, String viewer) throws IOException {
        return toDto(rides.find(id), viewer, new HashMap<String, Profile>());
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
        RideDto dto = new RideDto();
        dto.state = RideStates.NONE;
        return dto;
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
        RideDto dto = new RideDto();
        dto.id = ride.id;
        dto.state = ride.state;
        dto.riderUsername = ride.rider;
        Profile rider = profile(dto.riderUsername, cache);
        dto.riderName = rider == null ? "" : rider.displayName;
        dto.pickupLat = ride.pickupLat;
        dto.pickupLng = ride.pickupLng;
        dto.pickupAddress = ride.pickupAddress;
        dto.dropoffLat = ride.dropoffLat;
        dto.dropoffLng = ride.dropoffLng;
        dto.dropoffAddress = ride.dropoffAddress;
        dto.fareCents = ride.fareCents;
        dto.currency = ride.currency;
        dto.distanceMeters = ride.distanceMeters;
        dto.durationSeconds = ride.durationSeconds;
        dto.requestedAt = ride.requestedAt;
        dto.updatedAt = ride.updatedAt;
        dto.rating = ride.stars;
        dto.paymentStatus = ride.paymentStatus;
        dto.product = ride.product;
        dto.paymentMethodLabel = ride.methodLabel;
        dto.cash = Payments.CASH.equals(ride.methodId);
        dto.serviceFee = ride.feeCents;
        dto.tip = ride.tipCents;
        dto.total = dto.fareCents + dto.serviceFee + dto.tip;
        dto.driverGender = ride.womenOnly ? WOMEN : DRIVER_GENDERS[0];
        dto.quietRide = ride.quiet;
        dto.accessibleVehicle = ride.wheelchair;
        dto.petFriendly = ride.pets;
        dto.note = ride.note;
        dto.driverPhotoInitials = "";
        String driver = ride.driver;
        boolean offered = RideStates.OFFERED.equals(dto.state);
        // An offer is between the server and one driver. Until it is taken the
        // rider has no driver, and is not shown who was asked.
        if (driver.length() > 0 && (!offered || driver.equals(viewer))) {
            dto.driverUsername = driver;
            Profile who = profile(driver, cache);
            if (who != null) {
                dto.driverName = who.displayName;
                dto.driverPhotoInitials = Text.initials(dto.driverName);
                dto.vehicle = who.vehicle;
                dto.plate = who.plate;
            }
            boolean meeting = RideStates.ACCEPTED.equals(dto.state)
                    || RideStates.ARRIVED.equals(dto.state)
                    || RideStates.IN_PROGRESS.equals(dto.state);
            if (party && meeting && viewer != null) {
                if (viewer.equals(dto.riderUsername) && who != null) {
                    dto.driverPhone = who.phone;
                } else if (viewer.equals(driver) && rider != null) {
                    dto.riderPhone = rider.phone;
                }
            }
            DriverState car = drivers.find(driver);
            if (car != null) {
                dto.driverLat = car.lat;
                dto.driverLng = car.lng;
                dto.driverHeading = car.heading;
                dto.driverRating = car.rating();
                // How long until the car is where it is going next: the pickup
                // until the rider is in it, the destination after.
                if (RideStates.ACCEPTED.equals(dto.state)) {
                    dto.etaMinutes = minutes(dto.driverLat, dto.driverLng, dto.pickupLat,
                            dto.pickupLng);
                } else if (RideStates.IN_PROGRESS.equals(dto.state)) {
                    dto.etaMinutes = minutes(dto.driverLat, dto.driverLng, dto.dropoffLat,
                            dto.dropoffLng);
                }
            }
        }
        if (offered) {
            dto.offerExpiresInSeconds = (int) Math.max(0L,
                    (ride.offerExpiresAt - System.currentTimeMillis()) / 1000L);
        }
        return dto;
    }

    private static int minutes(double fromLat, double fromLng, double toLat, double toLng) {
        double seconds = Geo.meters(fromLat, fromLng, toLat, toLng) * Fares.ROAD_FACTOR
                / Fares.METERS_PER_SECOND;
        return Math.max(1, (int) Math.ceil(seconds / 60d));
    }

    private static String driverGender(RideRequestDto request) {
        return Text.oneOf(request.driverGender, DRIVER_GENDERS, DRIVER_GENDERS[0], "the driver");
    }

    private Profile profile(String username, Map<String, Profile> cache) {
        if (cache.containsKey(username)) {
            return cache.get(username);
        }
        Profile row = profiles.find(username);
        cache.put(username, row);
        return row;
    }

    private static void validate(RideRequestDto request) {
        if (request == null || !Geo.isPoint(request.pickupLat, request.pickupLng)
                || !Geo.isPoint(request.dropoffLat, request.dropoffLng)) {
            throw new ResponseStatusException(400, "Choose where the ride starts and ends");
        }
        double meters = Geo.meters(request.pickupLat, request.pickupLng, request.dropoffLat,
                request.dropoffLng);
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

    private static String describe(String state) {
        if (RideStates.COMPLETED.equals(state)) {
            return "already completed";
        }
        if (RideStates.IN_PROGRESS.equals(state)) {
            return "already under way";
        }
        if (RideStates.ACCEPTED.equals(state) || RideStates.ARRIVED.equals(state)) {
            return "already taken";
        }
        if (RideStates.REQUESTED.equals(state) || RideStates.OFFERED.equals(state)) {
            return "no longer on offer to you";
        }
        return "no longer available";
    }
}
