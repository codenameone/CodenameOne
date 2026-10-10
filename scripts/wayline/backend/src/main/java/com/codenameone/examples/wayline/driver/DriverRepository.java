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
package com.codenameone.examples.wayline.driver;

import com.codename1.backend.annotations.Component;
import com.codename1.orm.session.Identifier;
import com.codename1.orm.session.JpqlQuery;
import com.codename1.orm.session.Session;
import com.codenameone.examples.wayline.api.RideStates;
import com.codenameone.examples.wayline.domain.DriverApplication;
import com.codenameone.examples.wayline.domain.DriverHours;
import com.codenameone.examples.wayline.domain.DriverState;
import com.codenameone.examples.wayline.domain.Payout;
import com.codenameone.examples.wayline.domain.PayoutAccount;
import com.codenameone.examples.wayline.domain.Profile;
import com.codenameone.examples.wayline.driving.Applications;

import java.util.ArrayList;
import java.util.List;

/// Where drivers' live state is read from and written to: whether each is on
/// line, where the car is, the rating, and the hours spent on line.
///
/// It also reads, and never writes, the applications those drivers made: what
/// kind of car an approved driver has is part of every question asked here.
@Component
public class DriverRepository {
    private static final String[] WITH_RIDE = {RideStates.OFFERED, RideStates.ACCEPTED,
        RideStates.ARRIVED, RideStates.IN_PROGRESS};
    private static final String FEMALE = "female";

    private final Session session;

    public DriverRepository(Session session) {
        this.session = session;
    }

    // ------------------------------------------------------------------ state

    /// A driver's state, or null for an account that has never gone on line.
    public DriverState find(String username) {
        if (username == null || username.length() == 0 || username.length() > 190) {
            return null;
        }
        return session.find(DriverState.class, username);
    }

    public void add(DriverState state) {
        session.persist(state);
    }

    /// Every account that has a state, those on line first and then the ones
    /// heard from most recently.
    public List<DriverState> all() {
        return session.query(DriverState.class).orderBy("online", false)
                .orderBy("lastSeen", false).list();
    }

    /// The drivers on line, heard from since `freshSince`, inside a box of
    /// latitude and longitude, whose application stands approved and who have
    /// no ride in hand: one offer, or one ride, at a time.
    ///
    /// The approval is read here, in the query, and not trusted to have been
    /// put right elsewhere: an approval withdrawn takes a driver out of
    /// matching at once, whatever their state still says.
    ///
    /// @param product the kind of car wanted, or null for any
    /// @param womenOnly whether only drivers whose profile says they are women
    /// @param wheelchair whether only cars that take a wheelchair
    /// @param pets whether only drivers who take pets
    public List<DriverState> freeInBox(long freshSince, double south, double north, double west,
            double east, String product, boolean womenOnly, boolean wheelchair, boolean pets) {
        StringBuilder text = new StringBuilder("select d from DriverState d "
                + "where d.online = :yes and d.lastSeen >= :fresh "
                + "and d.lat between :south and :north and d.lng between :west and :east "
                + "and d.username in (select a.username from DriverApplication a "
                + "where a.status = :approved");
        if (product != null) {
            text.append(" and a.product = :product");
        }
        if (wheelchair) {
            text.append(" and a.wheelchair = :yes");
        }
        if (pets) {
            text.append(" and a.pets = :yes");
        }
        text.append(')');
        // Always held to an account that has a profile, whatever is asked of
        // it: a driver's state left behind by an account that is gone is not
        // somebody to send a rider to.
        text.append(" and d.username in (select p.username from Profile p");
        if (womenOnly) {
            text.append(" where p.gender = :gender");
        }
        text.append(')');
        text.append(" and d.username not in (select r.driver from Ride r "
                + "where r.state in :withRide)");
        JpqlQuery<DriverState> query = session.createQuery(text.toString(), DriverState.class)
                .setParameter("yes", Boolean.TRUE).setParameter("fresh", Long.valueOf(freshSince))
                .setParameter("south", Double.valueOf(south))
                .setParameter("north", Double.valueOf(north))
                .setParameter("west", Double.valueOf(west))
                .setParameter("east", Double.valueOf(east))
                .setParameter("approved", Applications.APPROVED)
                .setParameter("withRide", WITH_RIDE);
        if (product != null) {
            query.setParameter("product", product);
        }
        if (womenOnly) {
            query.setParameter("gender", FEMALE);
        }
        return query.list();
    }

    /// Counts a rating toward a driver's. Arithmetic done by the database, so
    /// two ratings arriving together are both counted. False for a driver who
    /// has no state to count it in.
    public boolean addRating(String username, int stars) {
        return session.createQuery("update DriverState d set d.ratingSum = d.ratingSum + :stars, "
                + "d.ratingCount = d.ratingCount + 1 where d.username = :username")
                .setParameter("stars", Long.valueOf(stars)).setParameter("username", username)
                .executeUpdate() == 1;
    }

    // ------------------------------------------------------------------ hours

    /// Adds time on line to a driver's day. The addition is the database's, so
    /// reports that overlap are both counted.
    public void addHours(String username, long dayNumber, long millis) {
        Object key = Identifier.of(username, Long.valueOf(dayNumber));
        if (session.find(DriverHours.class, key) != null) {
            session.increment(DriverHours.class, key, "millis", millis);
            return;
        }
        DriverHours hours = new DriverHours();
        hours.username = username;
        hours.dayNumber = dayNumber;
        hours.millis = millis;
        session.persist(hours);
    }

    // ----------------------------------------------------------- applications

    /// The application an account made to drive, or null when it made none.
    public DriverApplication application(String username) {
        if (username == null || username.length() == 0 || username.length() > 190) {
            return null;
        }
        return session.find(DriverApplication.class, username);
    }

    /// The applications of these accounts; none for a name that made none.
    public List<DriverApplication> applications(List<String> usernames) {
        if (usernames.isEmpty()) {
            return new ArrayList<DriverApplication>();
        }
        return session.createQuery("select a from DriverApplication a "
                + "where a.username in :usernames", DriverApplication.class)
                .setParameter("usernames", usernames).list();
    }

    /// The applications of every account that has a state.
    public List<DriverApplication> applicationsOfAll() {
        return session.createQuery("select a from DriverApplication a where a.username in "
                + "(select d.username from DriverState d)", DriverApplication.class).list();
    }

    /// The profiles of every account that has a state. Asked here and not of
    /// the profiles' own repository because it is the fleet's question: the
    /// drivers, and not every account there is.
    public List<Profile> profilesOfAll() {
        return session.createQuery("select p from Profile p where p.username in "
                + "(select d.username from DriverState d)", Profile.class).list();
    }

    // ---------------------------------------------------------------- payouts

    /// Stores a payout and the account it went to as history already made: the
    /// demo data's. A payout a driver asks for is `Earnings`' to make.
    public void add(PayoutAccount account) {
        session.persist(account);
    }

    public void add(Payout payout) {
        session.persist(payout);
    }
}
