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
package com.codenameone.examples.wayline.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.PageRequest;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.wayline.api.RideState;
import com.codenameone.examples.wayline.domain.Ride;

import java.util.List;

/// Rides.
///
/// Which state may follow which is `RideStateMachine`'s to say; what is here is
/// the statement that makes a move, told the states it may be made from. Every
/// move is one conditional update, and the count it answers is the whole
/// answer: 1 when this call made the move, 0 when the ride was somewhere else by
/// then. The database applies the rule and settles a race in the same breath:
/// of two drivers accepting at once, or a rider cancelling as a driver accepts,
/// one statement changes a row and the other changes none. Each is marked
/// `clearAutomatically`, which empties the session after it, so a ride read
/// before it is read again after it, as the statement left it.
///
/// A move comes in one of five shapes -- the rider's, the driver's, the
/// driver's taking an offer, the driver's giving one back, and the server's --
/// each its own statement rather than one assembled at run time.
// tag::wayline-repository[]
@Repository(entity = Ride.class, key = String.class)
public interface RideRepository extends CrudRepository<Ride, String> {
    /// The ride a rider has in one of `states`, or null.
    Ride findFirstByRiderAndStateIn(String rider, RideState[] states);

    /// The ride a driver has in one of `states`, or null.
    Ride findFirstByDriverAndStateIn(String driver, RideState[] states);
    // end::wayline-repository[]

    /// Whether the account is the rider of a ride in one of `asRider` or the
    /// driver of one in one of `asDriver`.
    boolean existsByRiderAndStateInOrDriverAndStateIn(String rider, RideState[] asRider,
            String driver, RideState[] asDriver);

    boolean existsByDriverAndStateIn(String driver, RideState[] states);

    /// Whether a ride of the rider's in one of `states` is to be paid with
    /// this method.
    boolean existsByRiderAndMethodIdAndStateIn(String rider, String methodId,
            RideState[] states);

    /// Whether a completed ride of the rider's, whose payment is
    /// `paymentStatus` and which ended after `since`, is to be charged to
    /// this method.
    boolean existsByRiderAndStateAndPaymentStatusAndUpdatedAtGreaterThanAndMethodId(
            String rider, RideState state, String paymentStatus, long since, String methodId);

    /// The same, to be charged to any method but `methodId`.
    boolean existsByRiderAndStateAndPaymentStatusAndUpdatedAtGreaterThanAndMethodIdNot(
            String rider, RideState state, String paymentStatus, long since, String methodId);

    /// A rider's rides whose payment is `paymentStatus` and that carry a tip.
    List<Ride> findByRiderAndPaymentStatusAndTipCentsGreaterThan(String rider,
            String paymentStatus, long tipCents);

    /// A rider's last fifty rides, newest first.
    List<Ride> findTop50ByRiderOrderByRequestedAtDesc(String rider);

    /// The last fifty of a driver's rides in one of `states`, newest first.
    List<Ride> findTop50ByDriverAndStateInOrderByRequestedAtDesc(String driver,
            RideState[] states);

    /// A rider's rides in `state`, newest first.
    List<Ride> findByRiderAndStateOrderByRequestedAtDesc(String rider, RideState state,
            PageRequest page);

    /// Everyone's rides in `state`, newest first.
    List<Ride> findByStateOrderByRequestedAtDesc(RideState state, PageRequest page);

    /// The most recent rides of all.
    @Query("select r from Ride r order by r.requestedAt desc")
    List<Ride> newest(PageRequest page);

    /// The rides in `state` whose offer ran out before `now`.
    List<Ride> findByStateAndOfferExpiresAtLessThan(RideState state, long now);

    /// The rides in `state`, the one that has waited longest first.
    List<Ride> findByStateOrderByRequestedAtAsc(RideState state);

    /// A driver's rides in `state` that changed at or after a time.
    List<Ride> findByDriverAndStateAndUpdatedAtGreaterThanEqual(String driver,
            RideState state, long since);

    long countByDriverAndState(String driver, RideState state);

    long countByRequestedAtGreaterThanEqual(long since);

    long countByStateIn(RideState[] states);

    /// The rides an account took part in, on either side.
    long countByRiderOrDriver(String rider, String driver);

    /// The rides an account itself called off, as the rider or as the driver.
    long countByRiderAndStateOrDriverAndState(String rider, RideState asRider, String driver,
            RideState asDriver);

    // ---------------------------------------------------------------- moves

    /// Moves a ride of `rider`'s from one of `from` to `to`.
    @Modifying(clearAutomatically = true)
    @Query("update Ride r set r.state = :to, r.updatedAt = :now "
            + "where r.id = :id and r.state in :from and r.rider = :rider")
    int moveAsRider(@Param("id") String id, @Param("from") RideState[] from,
            @Param("to") RideState to, @Param("now") long now, @Param("rider") String rider);

    /// Moves a ride of `driver`'s from one of `from` to `to`.
    @Modifying(clearAutomatically = true)
    @Query("update Ride r set r.state = :to, r.updatedAt = :now "
            + "where r.id = :id and r.state in :from and r.driver = :driver")
    int moveAsDriver(@Param("id") String id, @Param("from") RideState[] from,
            @Param("to") RideState to, @Param("now") long now, @Param("driver") String driver);

    // tag::wayline-ride-move[]
    /// Takes an offer made to `driver`: refused once the offer has lapsed, as
    /// of `now`, even in the moment before the sweep takes it back.
    @Modifying(clearAutomatically = true)
    @Query("update Ride r set r.state = :to, r.updatedAt = :now "
            + "where r.id = :id and r.state in :from and r.driver = :driver "
            + "and r.offerExpiresAt >= :now")
    int takeOffer(@Param("id") String id, @Param("from") RideState[] from,
            @Param("to") RideState to, @Param("now") long now, @Param("driver") String driver);
    // end::wayline-ride-move[]

    /// Gives a ride of `driver`'s back to be offered again, writing
    /// `newDriver` (empty: nobody) as its driver.
    @Modifying(clearAutomatically = true)
    @Query("update Ride r set r.state = :to, r.updatedAt = :now, r.driver = :newDriver "
            + "where r.id = :id and r.state in :from and r.driver = :driver")
    int giveBack(@Param("id") String id, @Param("from") RideState[] from,
            @Param("to") RideState to, @Param("now") long now, @Param("driver") String driver,
            @Param("newDriver") String newDriver);

    /// Holds `rider`'s ride while it is in one of `states`, answering 0 when
    /// it is not. The update writes nothing new; it is there for the row lock
    /// it takes until the transaction ends, so a move that would take the ride
    /// out of `states` waits for this transaction, or has already happened
    /// and makes it answer 0. The state is checked by the database at that
    /// moment, never from a copy read earlier.
    @Modifying
    @Query("update Ride r set r.rider = :rider "
            + "where r.id = :id and r.rider = :rider and r.state in :states")
    int holdWhile(@Param("id") String id, @Param("rider") String rider,
            @Param("states") RideState[] states);

    /// A move the server makes itself, whoever has the ride.
    @Modifying(clearAutomatically = true)
    @Query("update Ride r set r.state = :to, r.updatedAt = :now "
            + "where r.id = :id and r.state in :from")
    int moveByServer(@Param("id") String id, @Param("from") RideState[] from,
            @Param("to") RideState to, @Param("now") long now);

    /// Offers a ride to `driver` until `expiresAt`.
    @Modifying(clearAutomatically = true)
    @Query("update Ride r set r.state = :to, r.updatedAt = :now, r.driver = :driver, "
            + "r.offerExpiresAt = :expiresAt where r.id = :id and r.state in :from")
    int offer(@Param("id") String id, @Param("from") RideState[] from,
            @Param("to") RideState to, @Param("now") long now, @Param("driver") String driver,
            @Param("expiresAt") long expiresAt);

    // ----------------------------------------------------------- the rest

    /// Gives a completed ride its stars, if it has none yet. One statement,
    /// for the reason a move is one: a ride rated twice at the same moment is
    /// rated once.
    @Modifying(clearAutomatically = true)
    @Query("update Ride r set r.stars = :stars where r.id = :id and r.rider = :rider "
            + "and r.state = 'COMPLETED' and r.stars = 0")
    int rate(@Param("id") String id, @Param("rider") String rider, @Param("stars") int stars);

    /// Completed rides whose payment has been pending since before `before`
    /// and no longer than since `after`, newest first.
    @Query("select r from Ride r where r.state = 'COMPLETED' and r.paymentStatus = 'PENDING' "
            + "and r.updatedAt < :before and r.updatedAt > :after order by r.updatedAt desc")
    List<Ride> unsettled(@Param("after") long after, @Param("before") long before,
            PageRequest page);

    /// Records how the charge for a ride came out. A statement of its own and
    /// not a change to a ride read earlier, so that it writes this one column
    /// whatever else was written to the ride meanwhile.
    @Modifying(clearAutomatically = true)
    @Query("update Ride r set r.paymentStatus = :status where r.id = :id")
    int paymentStatus(@Param("id") String id, @Param("status") String status);

    /// Sets the tip of a completed, paid ride that has none.
    ///
    /// @return 1 for the one caller whose tip it now is
    @Modifying(clearAutomatically = true)
    @Query("update Ride r set r.tipCents = :tip where r.id = :id and r.rider = :rider "
            + "and r.state = 'COMPLETED' and r.paymentStatus = 'PAID' and r.tipCents = 0")
    int claimTip(@Param("id") String id, @Param("rider") String rider, @Param("tip") long tip);

    /// Takes back a tip that could not be charged.
    @Modifying(clearAutomatically = true)
    @Query("update Ride r set r.tipCents = 0 where r.id = :id")
    int releaseTip(@Param("id") String id);

    /// Marks a paid ride refunded.
    ///
    /// @return 1 for the one caller that is to refund it
    @Modifying(clearAutomatically = true)
    @Query("update Ride r set r.paymentStatus = 'REFUNDED' where r.id = :id "
            + "and r.paymentStatus = 'PAID'")
    int claimRefund(@Param("id") String id);

    /// Gives a refund's claim back: the ride is paid again, if it is still
    /// marked refunded.
    ///
    /// @return 1 when the ride was
    @Modifying(clearAutomatically = true)
    @Query("update Ride r set r.paymentStatus = 'PAID' where r.id = :id "
            + "and r.paymentStatus = 'REFUNDED'")
    int releaseRefund(@Param("id") String id);

    // ---------------------------------------------------------------- sums
    // A sum over no rows is null and not zero, on every database, so each
    // one is wrapped in `coalesce(..., 0)`.

    /// How many rides each driver has in `state`, as `{driver, count}`.
    @Query("select r.driver, count(r) from Ride r where r.state = :state group by r.driver")
    List<Object[]> countByDriver(@Param("state") RideState state);

    /// How many of a driver's completed rides were paid for, and what the
    /// driver's share of their fares came to: `{count, cents}`.
    ///
    /// The share and not the fare: this is the figure the driving screen
    /// shows as earned, and the fare includes the service's commission,
    /// which the driver never sees and cannot cash out.
    @Query("select count(r), coalesce(sum(r.driverCents), 0) from Ride r "
            + "where r.driver = :driver and r.state = 'COMPLETED' and r.paymentStatus = 'PAID'")
    Object[] paidTotals(@Param("driver") String driver);

    /// [#paidTotals] for the rides that ended since a time: `{count, cents}`.
    @Query("select count(r), coalesce(sum(r.driverCents), 0) from Ride r "
            + "where r.driver = :driver and r.state = 'COMPLETED' and r.paymentStatus = 'PAID' "
            + "and r.updatedAt >= :since")
    Object[] paidTotalsSince(@Param("driver") String driver, @Param("since") long since);

    /// What the driver's share of their completed, paid rides came to since
    /// a time.
    @Query("select coalesce(sum(r.driverCents), 0) from Ride r where r.driver = :driver "
            + "and r.state = 'COMPLETED' and r.paymentStatus = 'PAID' and r.updatedAt >= :since")
    Long paidSince(@Param("driver") String driver, @Param("since") long since);

    /// The driver's share of the fares of every ride they completed and that
    /// was paid for -- in cash, or by a card whose charge came back. A charge
    /// that failed, was refunded, or has not been answered yet is no money of
    /// the service's, so it is not the driver's to cash out either.
    @Query("select coalesce(sum(r.driverCents), 0) from Ride r where r.driver = :driver "
            + "and r.state = 'COMPLETED' and r.paymentStatus = 'PAID'")
    Long driverShare(@Param("driver") String driver);

    /// The tips handed to a driver in cash.
    @Query("select coalesce(sum(r.tipCents), 0) from Ride r where r.driver = :driver "
            + "and r.state = 'COMPLETED' and r.methodId = 'cash'")
    Long cashTips(@Param("driver") String driver);

    /// The tips put on a card for a driver whose charge has come back, while
    /// the ride is still paid: a refund returns the tip with the fare.
    @Query("select coalesce(sum(r.tipCents), 0) from Ride r where r.driver = :driver "
            + "and r.state = 'COMPLETED' and r.methodId <> 'cash' and r.paymentStatus = 'PAID' "
            + "and r.id in (select p.rideId from Payment p where p.tipRef <> '')")
    Long chargedTips(@Param("driver") String driver);

    /// The ids of a driver's rides, completed since a time, whose tip was put
    /// on a card and whose charge has come back: [#chargedTips] by ride.
    @Query("select r.id from Ride r where r.driver = :driver and r.state = 'COMPLETED' "
            + "and r.updatedAt >= :since and r.methodId <> 'cash' and r.paymentStatus = 'PAID' "
            + "and r.id in (select p.rideId from Payment p where p.tipRef <> '')")
    List<String> tipChargedSince(@Param("driver") String driver, @Param("since") long since);

    /// What riders paid in fares for rides in `state` whose payment is `paid`.
    @Query("select coalesce(sum(r.fareCents), 0) from Ride r "
            + "where r.state = :state and r.paymentStatus = :paid")
    Long fares(@Param("state") RideState state, @Param("paid") String paid);

    /// The fares of the rides in `state` asked for since `since`, paid or not.
    @Query("select coalesce(sum(r.fareCents), 0) from Ride r "
            + "where r.requestedAt >= :since and r.state = :state")
    Long faresSince(@Param("since") long since, @Param("state") RideState state);

    /// The rides asked for since `since`, counted by state: `{state, count}`.
    @Query("select r.state, count(r) from Ride r where r.requestedAt >= :since group by r.state")
    List<Object[]> countByState(@Param("since") long since);

    /// The same by product: `{product, count}`.
    @Query("select r.product, count(r) from Ride r where r.requestedAt >= :since "
            + "group by r.product")
    List<Object[]> countByProduct(@Param("since") long since);

    /// The rides in `state` asked for since `since`, counted by driver:
    /// `{driver, count}`.
    @Query("select r.driver, count(r) from Ride r where r.requestedAt >= :since "
            + "and r.state = :state group by r.driver")
    List<Object[]> countByDriver(@Param("since") long since, @Param("state") RideState state);

    /// One row for each ride asked for since `since`: `{id, requestedAt,
    /// state, fareCents, paymentStatus}`.
    ///
    /// This is what the charts by day and by hour of the day are counted
    /// from, in Java. Grouping by a day would mean grouping by `requestedAt`
    /// divided by the length of one, and a query cannot say that portably:
    /// the number is sent as a bound value, once in the select list and once
    /// in the `group by`, and a database that takes a statement's text at its
    /// word does not see those as one expression.
    @Query("select r.id, r.requestedAt, r.state, r.fareCents, r.paymentStatus from Ride r "
            + "where r.requestedAt >= :since")
    List<Object[]> timeline(@Param("since") long since);

    /// What a rider paid for rides in `state` whose payment is `paid`:
    /// `{fares, fees, tips}`.
    @Query("select coalesce(sum(r.fareCents), 0), coalesce(sum(r.feeCents), 0), "
            + "coalesce(sum(r.tipCents), 0) from Ride r "
            + "where r.rider = :who and r.state = :state and r.paymentStatus = :paid")
    Object[] spent(@Param("who") String username, @Param("state") RideState state,
            @Param("paid") String paid);
}
