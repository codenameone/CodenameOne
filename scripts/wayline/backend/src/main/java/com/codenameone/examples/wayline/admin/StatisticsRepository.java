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
package com.codenameone.examples.wayline.admin;

import com.codename1.backend.annotations.Component;
import com.codename1.orm.session.Session;
import com.codenameone.examples.wayline.domain.DriverState;
import com.codenameone.examples.wayline.domain.Profile;
import com.codenameone.examples.wayline.domain.Ride;

import java.util.List;

/// The counts and sums the admin's dashboard and charts are made of.
///
/// These read the entities other packages own and write none of them. They
/// are kept apart from those packages' repositories because none of them is a
/// question the package itself asks: nothing but a chart wants rides counted
/// by state.
///
/// A sum of cents is wrapped in `coalesce(..., 0)`: the sum of no rows is null
/// and not zero, on every database.
@Component
public class StatisticsRepository {
    private final Session session;

    public StatisticsRepository(Session session) {
        this.session = session;
    }

    // ---------------------------------------------------------------- people

    public long profiles() {
        return session.query(Profile.class).count();
    }

    public long flaggedProfiles() {
        return session.query(Profile.class).eq("flagged", Boolean.TRUE).count();
    }

    /// When each account made since `since` was made.
    public List<Long> signUps(long since) {
        return session.createQuery("select p.createdAt from Profile p "
                + "where p.createdAt >= :since", Long.class)
                .setParameter("since", Long.valueOf(since)).list();
    }

    /// The drivers on line who reported in at or after `seenSince`.
    public long driversOnline(long seenSince) {
        return session.query(DriverState.class).eq("online", Boolean.TRUE)
                .ge("lastSeen", Long.valueOf(seenSince)).count();
    }

    /// What is kept about a driver's car and rating, or null for an account
    /// that has never driven.
    public DriverState driver(String username) {
        if (username == null || username.length() == 0 || username.length() > 190) {
            return null;
        }
        return session.find(DriverState.class, username);
    }

    // ----------------------------------------------------------------- rides

    public long ridesSince(long since) {
        return session.query(Ride.class).ge("requestedAt", Long.valueOf(since)).count();
    }

    public long ridesIn(String[] states) {
        return number(session.createQuery("select count(r) from Ride r where r.state in :states",
                Long.class).setParameter("states", states).first());
    }

    /// What riders paid in fares for rides in `state` whose payment is `paid`.
    public long fares(String state, String paid) {
        return number(session.createQuery("select coalesce(sum(r.fareCents), 0) from Ride r "
                + "where r.state = :state and r.paymentStatus = :paid", Long.class)
                .setParameter("state", state).setParameter("paid", paid).first());
    }

    /// The fares of the rides in `state` asked for since `since`, paid or not.
    public long faresSince(long since, String state) {
        return number(session.createQuery("select coalesce(sum(r.fareCents), 0) from Ride r "
                + "where r.requestedAt >= :since and r.state = :state", Long.class)
                .setParameter("since", Long.valueOf(since)).setParameter("state", state).first());
    }

    /// The rides asked for since `since`, counted by state: `{state, count}`.
    public List<Object[]> countByState(long since) {
        return session.createQuery("select r.state, count(r) from Ride r "
                + "where r.requestedAt >= :since group by r.state", Object[].class)
                .setParameter("since", Long.valueOf(since)).list();
    }

    /// The same by product: `{product, count}`.
    public List<Object[]> countByProduct(long since) {
        return session.createQuery("select r.product, count(r) from Ride r "
                + "where r.requestedAt >= :since group by r.product", Object[].class)
                .setParameter("since", Long.valueOf(since)).list();
    }

    /// The rides in `state` asked for since `since`, counted by driver:
    /// `{driver, count}`.
    public List<Object[]> countByDriver(long since, String state) {
        return session.createQuery("select r.driver, count(r) from Ride r "
                + "where r.requestedAt >= :since and r.state = :state group by r.driver",
                Object[].class)
                .setParameter("since", Long.valueOf(since)).setParameter("state", state).list();
    }

    /// One row for each ride asked for since `since`: `{id, requestedAt,
    /// state, fareCents, paymentStatus}`.
    ///
    /// This is what the charts by day and by hour of the day are counted from,
    /// in Java. Grouping by a day would mean grouping by `requestedAt` divided
    /// by the length of one, and a query cannot say that portably: the number
    /// is sent as a bound value, once in the select list and once in the
    /// `group by`, and a database that takes a statement's text at its word
    /// does not see those as one expression.
    public List<Object[]> timeline(long since) {
        return session.createQuery("select r.id, r.requestedAt, r.state, r.fareCents, "
                + "r.paymentStatus from Ride r where r.requestedAt >= :since", Object[].class)
                .setParameter("since", Long.valueOf(since)).list();
    }

    /// When each ride asked for since `since` came into `state`: `{rideId,
    /// createdAt}`. The entities have no relationship to join along, so the
    /// rides are named by a nested query, and the caller pairs these with the
    /// rides' own times from [#timeline].
    public List<Object[]> reached(long since, String state) {
        return session.createQuery("select e.rideId, e.createdAt from RideEvent e "
                + "where e.state = :state and e.rideId in "
                + "(select r.id from Ride r where r.requestedAt >= :since)", Object[].class)
                .setParameter("state", state).setParameter("since", Long.valueOf(since)).list();
    }

    // ----------------------------------------------------------- one account

    /// The rides an account took part in, on either side.
    public long ridesOf(String username) {
        return number(session.createQuery("select count(r) from Ride r "
                + "where r.rider = :who or r.driver = :who", Long.class)
                .setParameter("who", username).first());
    }

    /// The rides an account itself called off, as the rider or as the driver.
    public long cancellationsOf(String username, String asRider, String asDriver) {
        return number(session.createQuery("select count(r) from Ride r where "
                + "(r.rider = :who and r.state = :asRider) or "
                + "(r.driver = :who and r.state = :asDriver)", Long.class)
                .setParameter("who", username).setParameter("asRider", asRider)
                .setParameter("asDriver", asDriver).first());
    }

    /// What a rider paid for rides in `state` whose payment is `paid`:
    /// `{fares, fees, tips}`.
    public Object[] spent(String username, String state, String paid) {
        return session.createQuery("select coalesce(sum(r.fareCents), 0), "
                + "coalesce(sum(r.feeCents), 0), coalesce(sum(r.tipCents), 0) from Ride r "
                + "where r.rider = :who and r.state = :state and r.paymentStatus = :paid",
                Object[].class)
                .setParameter("who", username).setParameter("state", state)
                .setParameter("paid", paid).first();
    }

    /// What a driver made from rides in `state`: `{driver's share, tips}`.
    public Object[] earned(String username, String state) {
        return session.createQuery("select coalesce(sum(r.driverCents), 0), "
                + "coalesce(sum(r.tipCents), 0) from Ride r "
                + "where r.driver = :who and r.state = :state", Object[].class)
                .setParameter("who", username).setParameter("state", state).first();
    }

    private static long number(Long value) {
        return value == null ? 0L : value.longValue();
    }
}
