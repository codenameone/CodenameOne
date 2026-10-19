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
package com.codenameone.examples.savor.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.PageRequest;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.domain.Delivery;

import java.util.List;

/// Deliveries.
///
/// Which state may follow which is `DeliveryStateMachine`'s to say; what is here is
/// the statement that makes a move, told the states it may be made from. Every
/// move is one conditional update, and the count it answers is the whole
/// answer: 1 when this call made the move, 0 when the delivery was somewhere else by
/// then. The database applies the rule and settles a race in the same breath:
/// of two couriers accepting at once, or a customer cancelling as a courier accepts,
/// one statement changes a row and the other changes none. Each is marked
/// `clearAutomatically`, which empties the session after it, so a delivery read
/// before it is read again after it, as the statement left it.
///
/// A move comes in one of five shapes -- the customer's, the courier's, the
/// courier's taking an offer, the courier's giving one back, and the server's --
/// each its own statement rather than one assembled at run time.
// tag::savor-repository[]
@Repository(entity = Delivery.class, key = String.class)
public interface DeliveryRepository extends CrudRepository<Delivery, String> {
    /// The delivery a customer has in one of `states`, or null.
    Delivery findFirstByCustomerAndStateIn(String customer, DeliveryState[] states);

    /// The delivery a courier has in one of `states`, or null.
    Delivery findFirstByCourierAndStateIn(String courier, DeliveryState[] states);
    // end::savor-repository[]

    /// Whether the account is the customer of a delivery in one of `asCustomer` or the
    /// courier of one in one of `asCourier`.
    boolean existsByCustomerAndStateInOrCourierAndStateIn(String customer, DeliveryState[] asCustomer,
            String courier, DeliveryState[] asCourier);

    boolean existsByCourierAndStateIn(String courier, DeliveryState[] states);

    /// Whether a delivery of the customer's in one of `states` is to be paid with
    /// this method.
    boolean existsByCustomerAndMethodIdAndStateIn(String customer, String methodId,
            DeliveryState[] states);

    /// Whether a completed delivery of the customer's, whose payment is
    /// `paymentStatus` and which ended after `since`, is to be charged to
    /// this method.
    boolean existsByCustomerAndStateAndPaymentStatusAndUpdatedAtGreaterThanAndMethodId(
            String customer, DeliveryState state, String paymentStatus, long since, String methodId);

    /// The same, to be charged to any method but `methodId`.
    boolean existsByCustomerAndStateAndPaymentStatusAndUpdatedAtGreaterThanAndMethodIdNot(
            String customer, DeliveryState state, String paymentStatus, long since, String methodId);

    /// A customer's deliveries whose payment is `paymentStatus` and that carry a tip.
    List<Delivery> findByCustomerAndPaymentStatusAndTipCentsGreaterThan(String customer,
            String paymentStatus, long tipCents);

    /// A customer's last fifty deliveries, newest first.
    List<Delivery> findTop50ByCustomerOrderByRequestedAtDesc(String customer);

    /// The last fifty of a courier's deliveries in one of `states`, newest first.
    List<Delivery> findTop50ByCourierAndStateInOrderByRequestedAtDesc(String courier,
            DeliveryState[] states);

    /// A customer's deliveries in `state`, newest first.
    List<Delivery> findByCustomerAndStateOrderByRequestedAtDesc(String customer, DeliveryState state,
            PageRequest page);

    /// Everyone's deliveries in `state`, newest first.
    List<Delivery> findByStateOrderByRequestedAtDesc(DeliveryState state, PageRequest page);

    /// The most recent deliveries of all.
    @Query("select r from Delivery r order by r.requestedAt desc")
    List<Delivery> newest(PageRequest page);

    /// The deliveries in `state` whose offer ran out before `now`.
    List<Delivery> findByStateAndOfferExpiresAtLessThan(DeliveryState state, long now);

    /// The deliveries in `state`, the one that has waited longest first.
    List<Delivery> findByStateOrderByRequestedAtAsc(DeliveryState state);

    /// A courier's deliveries in `state` that changed at or after a time.
    List<Delivery> findByCourierAndStateAndUpdatedAtGreaterThanEqual(String courier,
            DeliveryState state, long since);

    long countByRequestedAtGreaterThanEqual(long since);

    long countByStateIn(DeliveryState[] states);

    /// The deliveries an account took part in, on either side.
    long countByCustomerOrCourier(String customer, String courier);

    /// The deliveries an account itself called off, as the customer or as the courier.
    long countByCustomerAndStateOrCourierAndState(String customer, DeliveryState asCustomer, String courier,
            DeliveryState asCourier);

    // ---------------------------------------------------------------- moves

    /// Moves a delivery of `customer`'s from one of `from` to `to`.
    @Modifying(clearAutomatically = true)
    @Query("update Delivery r set r.state = :to, r.updatedAt = :now "
            + "where r.id = :id and r.state in :from and r.customer = :customer")
    int moveAsCustomer(@Param("id") String id, @Param("from") DeliveryState[] from,
            @Param("to") DeliveryState to, @Param("now") long now, @Param("customer") String customer);

    /// Moves a delivery of `courier`'s from one of `from` to `to`.
    @Modifying(clearAutomatically = true)
    @Query("update Delivery r set r.state = :to, r.updatedAt = :now "
            + "where r.id = :id and r.state in :from and r.courier = :courier")
    int moveAsCourier(@Param("id") String id, @Param("from") DeliveryState[] from,
            @Param("to") DeliveryState to, @Param("now") long now, @Param("courier") String courier);

    // tag::savor-delivery-move[]
    /// Takes an offer made to `courier`: refused once the offer has lapsed, as
    /// of `now`, even in the moment before the sweep takes it back.
    @Modifying(clearAutomatically = true)
    @Query("update Delivery r set r.state = :to, r.updatedAt = :now "
            + "where r.id = :id and r.state in :from and r.courier = :courier "
            + "and r.offerExpiresAt >= :now")
    int takeOffer(@Param("id") String id, @Param("from") DeliveryState[] from,
            @Param("to") DeliveryState to, @Param("now") long now, @Param("courier") String courier);
    // end::savor-delivery-move[]

    /// Gives a delivery of `courier`'s back to be offered again, writing
    /// `newCourier` (empty: nobody) as its courier.
    @Modifying(clearAutomatically = true)
    @Query("update Delivery r set r.state = :to, r.updatedAt = :now, r.courier = :newCourier "
            + "where r.id = :id and r.state in :from and r.courier = :courier")
    int giveBack(@Param("id") String id, @Param("from") DeliveryState[] from,
            @Param("to") DeliveryState to, @Param("now") long now, @Param("courier") String courier,
            @Param("newCourier") String newCourier);

    /// Holds `customer`'s delivery while it is in one of `states`, answering 0 when
    /// it is not. The update writes nothing new; it is there for the row lock
    /// it takes until the transaction ends, so a move that would take the delivery
    /// out of `states` waits for this transaction, or has already happened
    /// and makes it answer 0. The state is checked by the database at that
    /// moment, never from a copy read earlier.
    @Modifying
    @Query("update Delivery r set r.customer = :customer "
            + "where r.id = :id and r.customer = :customer and r.state in :states")
    int holdWhile(@Param("id") String id, @Param("customer") String customer,
            @Param("states") DeliveryState[] states);

    /// A move the server makes itself, whoever has the delivery.
    @Modifying(clearAutomatically = true)
    @Query("update Delivery r set r.state = :to, r.updatedAt = :now "
            + "where r.id = :id and r.state in :from")
    int moveByServer(@Param("id") String id, @Param("from") DeliveryState[] from,
            @Param("to") DeliveryState to, @Param("now") long now);

    /// Offers a delivery to `courier` until `expiresAt`.
    @Modifying(clearAutomatically = true)
    @Query("update Delivery r set r.state = :to, r.updatedAt = :now, r.courier = :courier, "
            + "r.offerExpiresAt = :expiresAt where r.id = :id and r.state in :from")
    int offer(@Param("id") String id, @Param("from") DeliveryState[] from,
            @Param("to") DeliveryState to, @Param("now") long now, @Param("courier") String courier,
            @Param("expiresAt") long expiresAt);

    // ----------------------------------------------------------- the rest

    /// Gives a completed delivery its stars, if it has none yet. One statement,
    /// for the reason a move is one: a delivery rated twice at the same moment is
    /// rated once.
    @Modifying(clearAutomatically = true)
    @Query("update Delivery r set r.stars = :stars where r.id = :id and r.customer = :customer "
            + "and r.state = 'COMPLETED' and r.stars = 0")
    int rate(@Param("id") String id, @Param("customer") String customer, @Param("stars") int stars);

    /// Completed deliveries whose payment has been pending -- or is still the
    /// hold made at checkout, never captured because the server stopped
    /// between completing the delivery and settling it -- since before `before`
    /// and no longer than since `after`, oldest first: the sweep takes one
    /// page at a time, and a backlog read newest first would keep the oldest
    /// behind the new arrivals until they aged out of the window unsettled.
    @Query("select r from Delivery r where r.state = 'COMPLETED' "
            + "and (r.paymentStatus = 'PENDING' or r.paymentStatus = 'AUTHORIZED') "
            + "and r.updatedAt < :before and r.updatedAt > :after order by r.updatedAt asc")
    List<Delivery> unsettled(@Param("after") long after, @Param("before") long before,
            PageRequest page);

    /// Records how the charge for a delivery came out. A statement of its own and
    /// not a change to a delivery read earlier, so that it writes this one column
    /// whatever else was written to the delivery meanwhile.
    @Modifying(clearAutomatically = true)
    @Query("update Delivery r set r.paymentStatus = :status where r.id = :id")
    int paymentStatus(@Param("id") String id, @Param("status") String status);

    /// Sets the tip of a completed, paid delivery that has none.
    ///
    /// @return 1 for the one caller whose tip it now is
    @Modifying(clearAutomatically = true)
    @Query("update Delivery r set r.tipCents = :tip where r.id = :id and r.customer = :customer "
            + "and r.state = 'COMPLETED' and r.paymentStatus = 'PAID' and r.tipCents = 0")
    int claimTip(@Param("id") String id, @Param("customer") String customer, @Param("tip") long tip);

    /// Takes back a tip that could not be charged.
    @Modifying(clearAutomatically = true)
    @Query("update Delivery r set r.tipCents = 0 where r.id = :id")
    int releaseTip(@Param("id") String id);

    /// Marks a paid delivery refunded.
    ///
    /// @return 1 for the one caller that is to refund it
    @Modifying(clearAutomatically = true)
    @Query("update Delivery r set r.paymentStatus = 'REFUNDED' where r.id = :id "
            + "and r.paymentStatus = 'PAID'")
    int claimRefund(@Param("id") String id);

    /// Gives a refund's claim back: the delivery is paid again, if it is still
    /// marked refunded.
    ///
    /// @return 1 when the delivery was
    @Modifying(clearAutomatically = true)
    @Query("update Delivery r set r.paymentStatus = 'PAID' where r.id = :id "
            + "and r.paymentStatus = 'REFUNDED'")
    int releaseRefund(@Param("id") String id);

    // ---------------------------------------------------------------- sums
    // A sum over no rows is null and not zero, on every database, so each
    // one is wrapped in `coalesce(..., 0)`.

    /// How many deliveries each courier has in `state`, as `{courier, count}`.
    @Query("select r.courier, count(r) from Delivery r where r.state = :state group by r.courier")
    List<Object[]> countByCourier(@Param("state") DeliveryState state);

    /// How many of a courier's completed deliveries were paid for, and what the
    /// courier's share of their fares came to: `{count, cents}`.
    ///
    /// The share and not the fare: this is the figure the onboarding screen
    /// shows as earned, and the fare includes the service's commission,
    /// which the courier never sees and cannot cash out.
    @Query("select count(r), coalesce(sum(r.courierCents), 0) from Delivery r "
            + "where r.courier = :courier and r.state = 'COMPLETED' and r.paymentStatus = 'PAID'")
    Object[] paidTotals(@Param("courier") String courier);

    /// [#paidTotals] for the deliveries that ended since a time: `{count, cents}`.
    @Query("select count(r), coalesce(sum(r.courierCents), 0) from Delivery r "
            + "where r.courier = :courier and r.state = 'COMPLETED' and r.paymentStatus = 'PAID' "
            + "and r.updatedAt >= :since")
    Object[] paidTotalsSince(@Param("courier") String courier, @Param("since") long since);

    /// What the courier's share of their completed, paid deliveries came to since
    /// a time.
    @Query("select coalesce(sum(r.courierCents), 0) from Delivery r where r.courier = :courier "
            + "and r.state = 'COMPLETED' and r.paymentStatus = 'PAID' and r.updatedAt >= :since")
    Long paidSince(@Param("courier") String courier, @Param("since") long since);

    /// The courier's share of the fares of every delivery they completed and that
    /// was paid for -- in cash, or by a card whose charge came back. A charge
    /// that failed, was refunded, or has not been answered yet is no money of
    /// the service's, so it is not the courier's to cash out either.
    @Query("select coalesce(sum(r.courierCents), 0) from Delivery r where r.courier = :courier "
            + "and r.state = 'COMPLETED' and r.paymentStatus = 'PAID'")
    Long courierShare(@Param("courier") String courier);

    /// [#courierShare] for the deliveries paid by card only: what the service
    /// holds for the courier. A cash delivery's money is in the courier's hand.
    @Query("select coalesce(sum(r.courierCents), 0) from Delivery r where r.courier = :courier "
            + "and r.state = 'COMPLETED' and r.paymentStatus = 'PAID' and r.methodId <> 'cash'")
    Long cardShare(@Param("courier") String courier);

    /// The tips handed to a courier in cash.
    @Query("select coalesce(sum(r.tipCents), 0) from Delivery r where r.courier = :courier "
            + "and r.state = 'COMPLETED' and r.methodId = 'cash'")
    Long cashTips(@Param("courier") String courier);

    /// The tips put on a card for a courier whose charge has come back, while
    /// the delivery is still paid: a refund returns the tip with the fare.
    @Query("select coalesce(sum(r.tipCents), 0) from Delivery r where r.courier = :courier "
            + "and r.state = 'COMPLETED' and r.methodId <> 'cash' and r.paymentStatus = 'PAID' "
            + "and r.id in (select p.deliveryId from Payment p where p.tipRef <> '')")
    Long chargedTips(@Param("courier") String courier);

    /// The ids of a courier's deliveries, completed since a time, whose tip was put
    /// on a card and whose charge has come back: [#chargedTips] by delivery.
    @Query("select r.id from Delivery r where r.courier = :courier and r.state = 'COMPLETED' "
            + "and r.updatedAt >= :since and r.methodId <> 'cash' and r.paymentStatus = 'PAID' "
            + "and r.id in (select p.deliveryId from Payment p where p.tipRef <> '')")
    List<String> tipChargedSince(@Param("courier") String courier, @Param("since") long since);

    /// What customers paid in fares for deliveries in `state` whose payment is `paid`.
    @Query("select coalesce(sum(r.fareCents), 0) from Delivery r "
            + "where r.state = :state and r.paymentStatus = :paid")
    Long fares(@Param("state") DeliveryState state, @Param("paid") String paid);

    /// The fares of the deliveries in `state` asked for since `since`, paid or not.
    @Query("select coalesce(sum(r.fareCents), 0) from Delivery r "
            + "where r.requestedAt >= :since and r.state = :state")
    Long faresSince(@Param("since") long since, @Param("state") DeliveryState state);

    /// The deliveries asked for since `since`, counted by state: `{state, count}`.
    @Query("select r.state, count(r) from Delivery r where r.requestedAt >= :since group by r.state")
    List<Object[]> countByState(@Param("since") long since);

    /// The deliveries in `state` asked for since `since`, counted by courier:
    /// `{courier, count}`.
    @Query("select r.courier, count(r) from Delivery r where r.requestedAt >= :since "
            + "and r.state = :state group by r.courier")
    List<Object[]> countByCourier(@Param("since") long since, @Param("state") DeliveryState state);

    /// One row for each delivery asked for since `since`: `{id, requestedAt,
    /// state, fareCents, paymentStatus}`.
    ///
    /// This is what the charts by day and by hour of the day are counted
    /// from, in Java. Grouping by a day would mean grouping by `requestedAt`
    /// divided by the length of one, and a query cannot say that portably:
    /// the number is sent as a bound value, once in the select list and once
    /// in the `group by`, and a database that takes a statement's text at its
    /// word does not see those as one expression.
    @Query("select r.id, r.requestedAt, r.state, r.fareCents, r.paymentStatus from Delivery r "
            + "where r.requestedAt >= :since")
    List<Object[]> timeline(@Param("since") long since);

    /// What a customer paid for deliveries in `state` whose payment is `paid`:
    /// `{fares, fees, tips}`.
    @Query("select coalesce(sum(r.fareCents), 0), coalesce(sum(r.feeCents), 0), "
            + "coalesce(sum(r.tipCents), 0) from Delivery r "
            + "where r.customer = :who and r.state = :state and r.paymentStatus = :paid")
    Object[] spent(@Param("who") String username, @Param("state") DeliveryState state,
            @Param("paid") String paid);

    long countByCourier(String courier);

    long countByCourierAndState(String courier, DeliveryState state);
}
