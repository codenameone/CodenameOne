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
package com.codenameone.examples.savor.courier;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.savor.Days;
import com.codenameone.examples.savor.Rows;
import com.codenameone.examples.savor.api.CourierDto;
import com.codenameone.examples.savor.api.CourierStatusDto;
import com.codenameone.examples.savor.api.EarningsDto;
import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.domain.CourierApplication;
import com.codenameone.examples.savor.domain.CourierHours;
import com.codenameone.examples.savor.domain.CourierState;
import com.codenameone.examples.savor.domain.Profile;
import com.codenameone.examples.savor.domain.Delivery;
import com.codenameone.examples.savor.onboarding.Applications;
import com.codenameone.examples.savor.geo.Geo;
import com.codenameone.examples.savor.live.LiveHub;
import com.codenameone.examples.savor.push.OrderPushes;
import com.codenameone.examples.savor.order.Quotes;
import com.codenameone.examples.savor.repository.CourierApplicationRepository;
import com.codenameone.examples.savor.repository.CourierHoursRepository;
import com.codenameone.examples.savor.repository.CourierStateRepository;
import com.codenameone.examples.savor.repository.ProfileRepository;
import com.codenameone.examples.savor.repository.DeliveryEventRepository;
import com.codenameone.examples.savor.repository.DeliveryPassedRepository;
import com.codenameone.examples.savor.repository.DeliveryRepository;
import com.codenameone.examples.savor.delivery.Matcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Couriers' live state and what they have earned.
@Component
public class Couriers {
    private static final long DAY_MILLIS = 86400000L;

    /// The states of a delivery a courier has taken and has yet to finish.
    private static final DeliveryState[] TAKEN = {DeliveryState.ACCEPTED, DeliveryState.ARRIVED,
        DeliveryState.IN_PROGRESS};

    private final CourierStateRepository couriers;
    private final CourierHoursRepository hours;
    private final CourierApplicationRepository made;
    private final DeliveryRepository deliveries;
    private final DeliveryEventRepository events;
    private final DeliveryPassedRepository passes;
    private final ProfileRepository profiles;
    private final LiveHub live;
    private final OrderPushes pushes;
    private final Quotes fares;
    private final Applications applications;

    public Couriers(CourierStateRepository couriers, CourierHoursRepository hours,
            CourierApplicationRepository made, DeliveryRepository deliveries, DeliveryEventRepository events,
            DeliveryPassedRepository passes, ProfileRepository profiles, LiveHub live, OrderPushes pushes,
            Quotes fares, Applications applications) {
        this.couriers = couriers;
        this.hours = hours;
        this.made = made;
        this.deliveries = deliveries;
        this.events = events;
        this.passes = passes;
        this.profiles = profiles;
        this.applications = applications;
        this.live = live;
        this.pushes = pushes;
        this.fares = fares;
    }

    /// Records whether a courier is on line and where the car is. The app sends
    /// this every few seconds while on line; it is both the position customers
    /// follow and the sign the courier is still there.
    ///
    /// The customer being driven and the admin's fleet view are told once the
    /// report is stored, and not before: what they are told sends them here to
    /// read it.
    public CourierDto report(String courier, CourierStatusDto status) throws IOException {
        if (status == null || (status.online() && !Geo.isPoint(status.lat(), status.lng()))) {
            throw new ResponseStatusException(400, "Going on line needs the car's position");
        }
        Report made = store(courier, status);
        CourierDto dto = made.courier;
        if (made.deliveryId != null) {
            live.courierMoved(made.customer, made.deliveryId, dto.lat(), dto.lng(), dto.heading());
            pushes.courierMoved(made.deliveryId, dto.lat(), dto.lng());
        }
        live.fleetChanged(dto.username(), dto.online(), dto.lat(), dto.lng(), dto.heading());
        return dto;
    }

    /// A report as it was stored, and the delivery the courier is on, if any. Not
    /// private: the class the build writes to begin and commit the transaction
    /// of [#store] has to be able to name what it answers.
    static final class Report {
        CourierDto courier;
        String deliveryId;
        String customer;
    }

    @Transactional
    private Report store(String courier, CourierStatusDto status) throws IOException {
        // Two reports from one courier take turns, before anything is read: read together,
        // both would count the same stretch since the last one as time on
        // line. The clock is read after the wait, so reports are also stored
        // in the order they were let through and none moves the time back.
        //
        // A courier's first report has no state row to hold, so two first
        // reports would both find none and both insert one under the same
        // name. They take turns on the profile instead, the order blocking an
        // account takes them in, and the second then finds the first's row.
        // No profile either means the account was closed meanwhile, after its
        // clean-up deleted the state row; inserting one now would leave it
        // behind for good.
        if (couriers.hold(courier) == 0 && profiles.hold(courier) == 0) {
            throw new ResponseStatusException(404, "No such account");
        }
        // The role lets a courier into this part of the API; the approval is
        // what lets them take deliveries. They are granted together, but they are
        // kept apart, and it is the approval that is asked for here.
        if (status.online() && !applications.approved(courier)) {
            throw new ResponseStatusException(403, "Your application to deliver is not approved");
        }
        Delivery delivery = deliveries.findFirstByCourierAndStateIn(courier, TAKEN);
        if (!status.online() && delivery != null) {
            throw new ResponseStatusException(409, "Finish the delivery before going off line");
        }
        long now = System.currentTimeMillis();
        CourierState state = couriers.findById(courier);
        // The time since the last report was time on line, unless it was so
        // long ago that the app had plainly stopped reporting. It is counted
        // here, from the row as it was, and added last, once the row has been
        // changed. Adding it is a bulk update that leaves the session as it
        // is, so the row read here stays managed and its change is written
        // either way; a statement marked clearAutomatically would detach it,
        // and a change made after that would be lost.
        long since = state != null && state.isOnline() ? now - state.getLastSeen() : 0L;
        boolean first = state == null;
        if (first) {
            state = new CourierState();
            state.setUsername(courier);
        }
        state.setOnline(status.online());
        state.setLastSeen(now);
        // Off line keeps the last position: there is none to report. A first
        // report stores whatever came with it.
        if (status.online() || first) {
            state.setLat(status.lat());
            state.setLng(status.lng());
            state.setHeading(status.heading());
        }
        if (first) {
            couriers.insert(state);
        }
        if (since > 0L && since < Matcher.FRESH_MILLIS) {
            // Cut at midnight: a stretch on line across it belongs half to
            // each day, not all to the new one.
            long[] days = Days.split(now - since, now);
            for (int iter = 0; iter < days.length; iter += 2) {
                addHours(courier, days[iter], days[iter + 1]);
            }
        }
        Report made = new Report();
        made.courier = view(courier, state);
        if (delivery != null) {
            made.deliveryId = delivery.getId();
            made.customer = delivery.getCustomer();
        }
        return made;
    }

    @Transactional(readOnly = true)
    public CourierDto describe(String courier) throws IOException {
        return view(courier, couriers.findById(courier));
    }

    /// A courier with `deliveries` given rather than counted: the deliveries of a period
    /// the caller is describing, not of all time.
    @Transactional(readOnly = true)
    public CourierDto describe(String courier, int deliveries) throws IOException {
        return view(courier, couriers.findById(courier), deliveries);
    }

    private CourierDto view(String courier, CourierState state) {
        return view(courier, state, (int) deliveries.countByCourierAndState(courier,
                DeliveryState.COMPLETED));
    }

    private CourierDto view(String courier, CourierState state, int completed) {
        Profile profile = profiles.findById(courier);
        if (profile == null) {
            throw new ResponseStatusException(404, "No such courier");
        }
        return toDto(profile, made.findById(courier), state, completed,
                acceptanceRate(events.countByActorAndState(courier, DeliveryState.ACCEPTED),
                        passes.countByCourier(courier)));
    }

    /// Adds time on line to a courier's day. The addition is the database's,
    /// so reports that overlap are both counted.
    private void addHours(String courier, long dayNumber, long millis) {
        if (hours.addMillis(courier, dayNumber, millis) == 0) {
            CourierHours day = new CourierHours();
            day.setUsername(courier);
            day.setDayNumber(dayNumber);
            day.setMillis(millis);
            hours.insert(day);
        }
    }

    /// The share of the offers a courier answered that they took: 1 before they
    /// have answered any. An offer left to lapse counts as passed on.
    static double acceptanceRate(long accepted, long passed) {
        long offers = accepted + passed;
        return offers == 0L ? 1d : Math.round(accepted * 100d / offers) / 100d;
    }

    /// Every account that has driven or gone on line, for the admin's fleet
    /// view; those on line first.
    @Transactional(readOnly = true)
    public List<CourierDto> all() throws IOException {
        Map<String, Long> done = Rows.counts(deliveries.countByCourier(DeliveryState.COMPLETED));
        Map<String, Long> accepted = Rows.counts(events.countByActor(DeliveryState.ACCEPTED));
        Map<String, Long> passed = Rows.counts(passes.countByCourier());
        Map<String, Profile> people = new HashMap<String, Profile>();
        List<Profile> profilesOfAll = profiles.couriers();
        for (int iter = 0; iter < profilesOfAll.size(); iter++) {
            people.put(profilesOfAll.get(iter).getUsername(), profilesOfAll.get(iter));
        }
        Map<String, CourierApplication> forms = new HashMap<String, CourierApplication>();
        List<CourierApplication> applicationsOfAll = made.ofCouriers();
        for (int iter = 0; iter < applicationsOfAll.size(); iter++) {
            forms.put(applicationsOfAll.get(iter).getUsername(), applicationsOfAll.get(iter));
        }
        List<CourierState> states = couriers.onLineFirst();
        List<CourierDto> out = new ArrayList<CourierDto>();
        for (int iter = 0; iter < states.size(); iter++) {
            CourierState state = states.get(iter);
            Profile profile = people.get(state.getUsername());
            if (profile == null) {
                // A state left behind by an account that is gone.
                continue;
            }
            Long rode = done.get(state.getUsername());
            Long took = accepted.get(state.getUsername());
            Long left = passed.get(state.getUsername());
            out.add(toDto(profile, forms.get(state.getUsername()), state,
                    rode == null ? 0 : (int) rode.longValue(),
                    acceptanceRate(took == null ? 0L : took.longValue(),
                            left == null ? 0L : left.longValue())));
        }
        return out;
    }

    /// What a courier's completed, paid deliveries earned them: their share of
    /// each fare, without tips. "Today" is the day in UTC; a service in one
    /// city would use that city's midnight.
    @Transactional(readOnly = true)
    public EarningsDto earnings(String courier) throws IOException {
        long now = System.currentTimeMillis();
        // The day's trips and the day's money from one query, so the header
        // that shows them together counts the deliveries that made the sum.
        Object[] day = deliveries.paidTotalsSince(courier, now - now % DAY_MILLIS);
        int todayCount = day == null ? 0 : (int) Rows.number(day[0]);
        long today = day == null ? 0L : Rows.number(day[1]);
        long week = Rows.number(deliveries.paidSince(courier, now - 7 * DAY_MILLIS));
        Object[] total = deliveries.paidTotals(courier);
        int count = total == null ? 0 : (int) Rows.number(total[0]);
        long all = total == null ? 0L : Rows.number(total[1]);
        return new EarningsDto(today, week, all, count, todayCount, fares.currency());
    }

    /// @param application the account's application to deliver, or null
    /// @param car the account's state, or null when it has never gone on line
    private static CourierDto toDto(Profile profile, CourierApplication application,
            CourierState car, int deliveries, double acceptanceRate) {
        boolean approved = application != null
                && Applications.APPROVED.equals(application.getStatus());
        boolean online = false;
        double lat = 0d;
        double lng = 0d;
        double heading = 0d;
        double rating = 5d;
        long lastSeen = 0L;
        if (car != null) {
            lastSeen = car.getLastSeen();
            // On line means said so and heard from lately; see Matcher.
            online = car.isOnline()
                    && System.currentTimeMillis() - lastSeen < Matcher.FRESH_MILLIS;
            lat = car.getLat();
            lng = car.getLng();
            heading = car.getHeading();
            rating = car.rating();
        }
        return new CourierDto(profile.getUsername(), profile.getDisplayName(),
                profile.getVehicle(), profile.getPlate(), online, lat, lng, heading, rating,
                lastSeen, deliveries, approved, acceptanceRate, profile.isFlagged());
    }
}
