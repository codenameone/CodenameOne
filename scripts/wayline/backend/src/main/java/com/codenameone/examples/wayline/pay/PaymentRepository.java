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
package com.codenameone.examples.wayline.pay;

import com.codename1.backend.annotations.Component;
import com.codename1.orm.session.Session;
import com.codenameone.examples.wayline.domain.Payment;
import com.codenameone.examples.wayline.domain.PaymentCustomer;
import com.codenameone.examples.wayline.domain.PaymentMethod;
import com.codenameone.examples.wayline.domain.PaymentSetup;
import com.codenameone.examples.wayline.domain.Ride;

import java.util.List;

/// Where saved cards, card setups, the processor's names for accounts and the
/// payments themselves are read from and written to, with the few questions
/// payments ask of a ride.
///
/// Every claim here is one conditional update that answers with the number of
/// rows it changed: of two requests racing for the same thing, the database
/// lets one of them change the row, and the count tells each which it was. A
/// bulk statement also empties the session, so an entity read before one is
/// read again before it is changed.
@Component
public class PaymentRepository {
    private final Session session;

    public PaymentRepository(Session session) {
        this.session = session;
    }

    // ----------------------------------------------------------------- cards

    /// An account's saved cards, oldest first.
    public List<PaymentMethod> cards(String username) {
        return session.query(PaymentMethod.class).eq("username", username)
                .orderBy("createdAt", true).orderBy("id", true).list();
    }

    public long countCards(String username) {
        return session.query(PaymentMethod.class).eq("username", username).count();
    }

    /// The card an account pays with by default, or null when that is cash.
    public PaymentMethod defaultCard(String username) {
        return session.query(PaymentMethod.class).eq("username", username)
                .eq("preferred", Boolean.TRUE).first();
    }

    /// One of an account's cards, or null when the id is not a card of theirs.
    public PaymentMethod card(String username, String id) {
        if (id == null || id.length() == 0 || id.length() > 32) {
            return null;
        }
        return session.query(PaymentMethod.class).eq("id", id).eq("username", username).first();
    }

    /// When the newest of an account's cards was added, or null with none.
    public Long newestCardAt(String username) {
        return session.createQuery("select max(m.createdAt) from PaymentMethod m "
                + "where m.username = :username", Long.class)
                .setParameter("username", username).first();
    }

    public void add(PaymentMethod card) {
        session.persist(card);
    }

    public void remove(PaymentMethod card) {
        session.remove(card);
    }

    public void removeCards(String username) {
        session.createQuery("delete from PaymentMethod m where m.username = :username")
                .setParameter("username", username).executeUpdate();
    }

    // ---------------------------------------------------------------- setups

    /// A card setup an account began, or null.
    public PaymentSetup setup(String username, String id) {
        if (id == null || id.length() == 0 || id.length() > 32) {
            return null;
        }
        return session.query(PaymentSetup.class).eq("id", id).eq("username", username).first();
    }

    public void add(PaymentSetup setup) {
        session.persist(setup);
    }

    /// Marks a setup finished, if it is not yet.
    ///
    /// @return true for the one caller that finished it
    public boolean claimSetup(String id) {
        return session.createQuery("update PaymentSetup s set s.completed = :done "
                + "where s.id = :id and s.completed = :open")
                .setParameter("done", Boolean.TRUE).setParameter("open", Boolean.FALSE)
                .setParameter("id", id).executeUpdate() == 1;
    }

    public void removeSetups(String username) {
        session.createQuery("delete from PaymentSetup s where s.username = :username")
                .setParameter("username", username).executeUpdate();
    }

    // ------------------------------------------------------------- customers

    /// The processor's name for an account, or null before it has one.
    public PaymentCustomer customer(String username) {
        return session.find(PaymentCustomer.class, username);
    }

    public void add(PaymentCustomer customer) {
        session.persist(customer);
    }

    public void removeCustomer(String username) {
        session.createQuery("delete from PaymentCustomer c where c.username = :username")
                .setParameter("username", username).executeUpdate();
    }

    // -------------------------------------------------------------- payments

    /// What a ride was charged, or null for a ride not settled.
    public Payment payment(String rideId) {
        return session.find(Payment.class, rideId);
    }

    /// The payments of several rides, in no order.
    public List<Payment> payments(List<String> rideIds) {
        return session.createQuery("select p from Payment p where p.rideId in :ids",
                Payment.class).setParameter("ids", rideIds).list();
    }

    public void add(Payment payment) {
        session.persist(payment);
    }

    // ----------------------------------------------------------------- rides

    public Ride ride(String id) {
        if (id == null || id.length() == 0 || id.length() > 32) {
            return null;
        }
        return session.find(Ride.class, id);
    }

    /// Whether a ride of the rider's that has not ended is to be paid with
    /// this method.
    public boolean paysForRideUnderWay(String rider, String methodId) {
        return session.createQuery("select count(r) from Ride r where r.rider = :rider "
                + "and r.methodId = :method and r.state in ('REQUESTED', 'OFFERED', 'ACCEPTED', "
                + "'ARRIVED', 'IN_PROGRESS')", Long.class).setParameter("rider", rider)
                .setParameter("method", methodId).first().longValue() > 0L;
    }

    /// A rider's completed rides, newest first.
    public List<Ride> completed(String rider, int limit) {
        return session.createQuery("select r from Ride r where r.rider = :rider "
                + "and r.state = 'COMPLETED' order by r.requestedAt desc", Ride.class)
                .setParameter("rider", rider).limit(limit).list();
    }

    /// Everyone's completed rides, newest first.
    public List<Ride> completed(int limit) {
        return session.createQuery("select r from Ride r where r.state = 'COMPLETED' "
                + "order by r.requestedAt desc", Ride.class).limit(limit).list();
    }

    /// Sets the tip of a completed, paid ride that has none.
    ///
    /// @return true for the one caller whose tip it now is
    public boolean claimTip(String rideId, String rider, long amountCents) {
        return session.createQuery("update Ride r set r.tipCents = :tip where r.id = :id "
                + "and r.rider = :rider and r.state = 'COMPLETED' and r.paymentStatus = 'PAID' "
                + "and r.tipCents = 0").setParameter("tip", Long.valueOf(amountCents))
                .setParameter("id", rideId).setParameter("rider", rider).executeUpdate() == 1;
    }

    /// Takes back a tip that could not be charged.
    public void releaseTip(String rideId) {
        session.createQuery("update Ride r set r.tipCents = 0 where r.id = :id")
                .setParameter("id", rideId).executeUpdate();
    }

    /// Marks a paid ride refunded.
    ///
    /// @return true for the one caller that is to refund it
    public boolean claimRefund(String rideId) {
        return session.createQuery("update Ride r set r.paymentStatus = 'REFUNDED' "
                + "where r.id = :id and r.paymentStatus = 'PAID'")
                .setParameter("id", rideId).executeUpdate() == 1;
    }

    /// Marks a ride paid again, its refund having failed.
    public void releaseRefund(String rideId) {
        session.createQuery("update Ride r set r.paymentStatus = 'PAID' where r.id = :id")
                .setParameter("id", rideId).executeUpdate();
    }
}
