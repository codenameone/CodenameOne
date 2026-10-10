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

import com.codename1.backend.annotations.Component;
import com.codename1.orm.session.Identifier;
import com.codename1.orm.session.JpqlQuery;
import com.codename1.orm.session.Session;
import com.codenameone.examples.wayline.api.RideStates;
import com.codenameone.examples.wayline.domain.Payment;
import com.codenameone.examples.wayline.domain.Ride;
import com.codenameone.examples.wayline.domain.RideEvent;
import com.codenameone.examples.wayline.domain.RidePassed;
import com.codenameone.examples.wayline.pay.Payments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Where rides are read from and written to, with the record of what happened
/// to each: its events, and the drivers who passed on it.
///
/// It holds the queries and nothing else. Which state may follow which is
/// [RideStateMachine]'s to say and [Rides]' to apply; what is here is the one
/// statement that makes a move, told the states it may be made from.
@Component
public class RideRepository {
    /// The two people a move can be held to; see [#move].
    public static final String RIDER = "rider";
    public static final String DRIVER = "driver";

    private static final String[] ACTIVE = {RideStates.REQUESTED, RideStates.OFFERED,
        RideStates.ACCEPTED, RideStates.ARRIVED, RideStates.IN_PROGRESS};
    private static final String[] WITH_DRIVER = {RideStates.OFFERED, RideStates.ACCEPTED,
        RideStates.ARRIVED, RideStates.IN_PROGRESS};
    private static final String[] TAKEN = {RideStates.ACCEPTED, RideStates.ARRIVED,
        RideStates.IN_PROGRESS};
    private static final String[] ENDED_WITH_DRIVER = {RideStates.COMPLETED,
        RideStates.CANCELLED_BY_RIDER, RideStates.CANCELLED_BY_DRIVER,
        RideStates.CANCELLED_BY_ADMIN};

    private final Session session;

    public RideRepository(Session session) {
        this.session = session;
    }

    // ------------------------------------------------------------------ rides

    /// The ride of this id, or null when there is none.
    public Ride find(String id) {
        if (id == null || id.length() == 0 || id.length() > 32) {
            return null;
        }
        return session.find(Ride.class, id);
    }

    public void add(Ride ride) {
        session.persist(ride);
    }

    /// The ride a rider has under way, or null.
    public Ride activeForRider(String rider) {
        return session.query(Ride.class).eq("rider", rider).in("state", (Object[]) ACTIVE).first();
    }

    /// The offer waiting for a driver's answer or the ride they are on, or null.
    public Ride activeForDriver(String driver) {
        return session.query(Ride.class).eq("driver", driver).in("state", (Object[]) WITH_DRIVER)
                .first();
    }

    /// The ride a driver has taken and not finished, or null. An offer not yet
    /// answered is not one.
    public Ride takenBy(String driver) {
        return session.query(Ride.class).eq("driver", driver).in("state", (Object[]) TAKEN)
                .first();
    }

    /// A rider's last fifty rides, newest first.
    public List<Ride> historyForRider(String rider) {
        return session.query(Ride.class).eq("rider", rider).orderBy("requestedAt", false)
                .limit(50).list();
    }

    /// The last fifty rides a driver took that have ended, newest first.
    public List<Ride> historyForDriver(String driver) {
        return session.query(Ride.class).eq("driver", driver)
                .in("state", (Object[]) ENDED_WITH_DRIVER).orderBy("requestedAt", false)
                .limit(50).list();
    }

    /// The most recent rides of all.
    public List<Ride> recent(int most) {
        return session.query(Ride.class).orderBy("requestedAt", false).limit(most).list();
    }

    /// The offers whose time ran out before `now`.
    public List<Ride> lapsedOffers(long now) {
        return session.query(Ride.class).eq("state", RideStates.OFFERED)
                .lt("offerExpiresAt", Long.valueOf(now)).list();
    }

    /// The rides waiting for a driver, the one that has waited longest first.
    public List<Ride> waiting() {
        return session.query(Ride.class).eq("state", RideStates.REQUESTED)
                .orderBy("requestedAt", true).list();
    }

    /// Moves a ride to state `to` if it is in one of the states `from`, and
    /// answers how many rows that changed: 1 when this call made the move, 0
    /// when the ride was somewhere else by then.
    ///
    /// It is one statement, and the count is the whole answer. The database
    /// applies the rule and settles a race in the same breath: of two drivers
    /// accepting at once, or a rider cancelling as a driver accepts, one
    /// statement changes a row and the other changes none. Like every bulk
    /// statement it empties the session, so a ride read before it is read again
    /// after it.
    ///
    /// @param ownerField [#RIDER] or [#DRIVER] to hold the move to rides whose
    ///     rider or driver is `owner`, or null for a move the server makes
    ///     itself
    /// @param newDriver the driver to write with the move, or null to leave the
    ///     ride's driver as it is
    /// @param newOfferExpiresAt when the offer made by this move lapses, or
    ///     null to leave that as it is
    /// @param offerOpen whether the move is refused once the ride's offer has
    ///     lapsed, as of `now`
    public int move(String id, String[] from, String to, long now, String ownerField,
            String owner, String newDriver, Long newOfferExpiresAt, boolean offerOpen) {
        StringBuilder text = new StringBuilder("update Ride r set r.state = :to, "
                + "r.updatedAt = :now");
        if (newDriver != null) {
            text.append(", r.driver = :newDriver");
        }
        if (newOfferExpiresAt != null) {
            text.append(", r.offerExpiresAt = :newExpiry");
        }
        text.append(" where r.id = :id and r.state in :from");
        boolean owned = RIDER.equals(ownerField) || DRIVER.equals(ownerField);
        if (owned) {
            text.append(RIDER.equals(ownerField) ? " and r.rider = :owner"
                    : " and r.driver = :owner");
        }
        if (offerOpen) {
            text.append(" and r.offerExpiresAt >= :now");
        }
        JpqlQuery<Object> update = session.createQuery(text.toString())
                .setParameter("to", to).setParameter("now", Long.valueOf(now))
                .setParameter("id", id).setParameter("from", from);
        if (newDriver != null) {
            update.setParameter("newDriver", newDriver);
        }
        if (newOfferExpiresAt != null) {
            update.setParameter("newExpiry", newOfferExpiresAt);
        }
        if (owned) {
            update.setParameter("owner", owner);
        }
        return update.executeUpdate();
    }

    /// Gives a completed ride its stars, if it has none yet, and answers how
    /// many rows that changed. One statement, for the reason [#move] is one: a
    /// ride rated twice at the same moment is rated once.
    public int rate(String id, String rider, int stars) {
        return session.createQuery("update Ride r set r.stars = :stars where r.id = :id "
                + "and r.rider = :rider and r.state = :completed and r.stars = 0")
                .setParameter("stars", Integer.valueOf(stars)).setParameter("id", id)
                .setParameter("rider", rider).setParameter("completed", RideStates.COMPLETED)
                .executeUpdate();
    }

    /// Records how the charge for a ride came out. A statement of its own and
    /// not a change to a ride read earlier, so that it writes this one column
    /// whatever else was written to the ride meanwhile.
    public int paymentStatus(String id, String status) {
        return session.createQuery("update Ride r set r.paymentStatus = :status "
                + "where r.id = :id").setParameter("status", status).setParameter("id", id)
                .executeUpdate();
    }

    /// How many rides a driver has completed.
    public long completedBy(String driver) {
        return session.query(Ride.class).eq("driver", driver)
                .eq("state", RideStates.COMPLETED).count();
    }

    /// How many rides each driver has completed, by name.
    public Map<String, Long> completedByDriver() {
        return counts(session.createQuery("select r.driver, count(r) from Ride r "
                + "where r.state = :completed group by r.driver", Object[].class)
                .setParameter("completed", RideStates.COMPLETED).list());
    }

    /// How many of a driver's completed rides were paid for, and what their
    /// fares came to, in that order.
    public long[] paidTotals(String driver) {
        Object[] row = session.createQuery("select count(r), coalesce(sum(r.fareCents), 0) "
                + "from Ride r where r.driver = :driver and r.state = :completed "
                + "and r.paymentStatus = :paid", Object[].class)
                .setParameter("driver", driver).setParameter("completed", RideStates.COMPLETED)
                .setParameter("paid", Payments.PAID).first();
        return row == null ? new long[2] : new long[] {number(row[0]), number(row[1])};
    }

    /// What the fares of a driver's completed, paid rides came to since a time.
    public long paidSince(String driver, long since) {
        Long cents = session.createQuery("select coalesce(sum(r.fareCents), 0) from Ride r "
                + "where r.driver = :driver and r.state = :completed "
                + "and r.paymentStatus = :paid and r.updatedAt >= :since", Long.class)
                .setParameter("driver", driver).setParameter("completed", RideStates.COMPLETED)
                .setParameter("paid", Payments.PAID).setParameter("since", Long.valueOf(since))
                .first();
        return cents == null ? 0L : cents.longValue();
    }

    // ----------------------------------------------------------------- events

    public void add(RideEvent event) {
        session.persist(event);
    }

    /// How many offers a driver has accepted.
    public long acceptedBy(String driver) {
        return session.query(RideEvent.class).eq("actor", driver)
                .eq("state", RideStates.ACCEPTED).count();
    }

    /// How many offers each driver has accepted, by name.
    public Map<String, Long> acceptedByDriver() {
        return counts(session.createQuery("select e.actor, count(e) from RideEvent e "
                + "where e.state = :accepted group by e.actor", Object[].class)
                .setParameter("accepted", RideStates.ACCEPTED).list());
    }

    // ----------------------------------------------------------------- passed

    /// Whether a driver has already passed on a ride.
    public boolean hasPassed(String rideId, String driver) {
        return session.find(RidePassed.class, Identifier.of(rideId, driver)) != null;
    }

    public void add(RidePassed passed) {
        session.persist(passed);
    }

    /// The drivers who passed on a ride.
    public List<String> passedOn(String rideId) {
        List<RidePassed> rows = session.query(RidePassed.class).eq("rideId", rideId).list();
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(rows.get(iter).driver);
        }
        return out;
    }

    /// How many offers a driver has passed on or let lapse.
    public long passedBy(String driver) {
        return session.query(RidePassed.class).eq("driver", driver).count();
    }

    /// How many offers each driver has passed on or let lapse, by name.
    public Map<String, Long> passedByDriver() {
        return counts(session.createQuery("select p.driver, count(p.rideId) from RidePassed p "
                + "group by p.driver", Object[].class).list());
    }

    // --------------------------------------------------------------- payments

    /// Stores the payment of a ride that is written whole, already paid: the
    /// demo data's. A ride taken through the API is paid for by `Payments`.
    public void add(Payment payment) {
        session.persist(payment);
    }

    private static Map<String, Long> counts(List<Object[]> rows) {
        Map<String, Long> out = new HashMap<String, Long>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Object[] row = rows.get(iter);
            out.put((String) row[0], Long.valueOf(number(row[1])));
        }
        return out;
    }

    private static long number(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }
}
