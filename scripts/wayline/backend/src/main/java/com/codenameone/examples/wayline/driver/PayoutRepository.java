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
import com.codename1.orm.session.Session;
import com.codenameone.examples.wayline.domain.DriverHours;
import com.codenameone.examples.wayline.domain.DriverState;
import com.codenameone.examples.wayline.domain.Payout;
import com.codenameone.examples.wayline.domain.PayoutAccount;
import com.codenameone.examples.wayline.domain.Ride;
import com.codenameone.examples.wayline.domain.RideEvent;
import com.codenameone.examples.wayline.domain.RidePassed;

import java.util.List;

/// Where payouts and the accounts they are paid into are read from and written
/// to, with the sums over a driver's rides that their earnings are made of.
///
/// A sum over no rows is nothing at all to a database, so each one here is
/// asked for with a zero to fall back on and always answers with a number.
@Component
public class PayoutRepository {
    private final Session session;

    public PayoutRepository(Session session) {
        this.session = session;
    }

    // --------------------------------------------------------------- payouts

    /// The payouts made to a driver, newest first.
    public List<Payout> payouts(String driver) {
        return session.query(Payout.class).eq("driver", driver).orderBy("createdAt", false)
                .orderBy("id", true).limit(100).list();
    }

    public Payout payout(String id) {
        return session.find(Payout.class, id);
    }

    public void add(Payout payout) {
        session.persist(payout);
    }

    public void remove(Payout payout) {
        session.remove(payout);
    }

    /// Everything ever paid out to a driver.
    public long paidOut(String driver) {
        return session.createQuery("select coalesce(sum(p.amountCents), 0) from Payout p "
                + "where p.driver = :driver", Long.class).setParameter("driver", driver)
                .first().longValue();
    }

    /// Where a driver is paid, or null before they have said.
    public PayoutAccount account(String driver) {
        return session.find(PayoutAccount.class, driver);
    }

    public void add(PayoutAccount account) {
        session.persist(account);
    }

    // -------------------------------------------------------------- earnings

    /// A driver's completed rides that ended at or after a time.
    public List<Ride> completedSince(String driver, long since) {
        return session.createQuery("select r from Ride r where r.driver = :driver "
                + "and r.state = 'COMPLETED' and r.updatedAt >= :since", Ride.class)
                .setParameter("driver", driver).setParameter("since", Long.valueOf(since)).list();
    }

    /// How many rides a driver has completed.
    public long trips(String driver) {
        return session.createQuery("select count(r) from Ride r where r.driver = :driver "
                + "and r.state = 'COMPLETED'", Long.class).setParameter("driver", driver)
                .first().longValue();
    }

    /// The driver's share of the fares of every ride they completed.
    public long fares(String driver) {
        return session.createQuery("select coalesce(sum(r.driverCents), 0) from Ride r "
                + "where r.driver = :driver and r.state = 'COMPLETED'", Long.class)
                .setParameter("driver", driver).first().longValue();
    }

    /// The tips of every ride a driver completed.
    public long tips(String driver) {
        return session.createQuery("select coalesce(sum(r.tipCents), 0) from Ride r "
                + "where r.driver = :driver and r.state = 'COMPLETED'", Long.class)
                .setParameter("driver", driver).first().longValue();
    }

    /// How long a driver was on line on one day, in milliseconds.
    public long onlineMillis(String driver, long dayNumber) {
        DriverHours hours = session.find(DriverHours.class,
                Identifier.of(driver, Long.valueOf(dayNumber)));
        return hours == null ? 0L : hours.millis;
    }

    /// A driver's car and ratings, or null for someone who never drove.
    public DriverState state(String driver) {
        return session.find(DriverState.class, driver);
    }

    /// How many offers a driver took.
    public long accepted(String driver) {
        return session.query(RideEvent.class).eq("actor", driver).eq("state", "ACCEPTED").count();
    }

    /// How many offers a driver passed on or let lapse.
    public long passed(String driver) {
        return session.query(RidePassed.class).eq("driver", driver).count();
    }
}
