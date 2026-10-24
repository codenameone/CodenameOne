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
import com.codenameone.examples.savor.api.OrderState;
import com.codenameone.examples.savor.domain.CustomerOrder;

import java.util.List;

/// Every order.
@Repository(entity = CustomerOrder.class, key = String.class)
public interface CustomerOrderRepository extends CrudRepository<CustomerOrder, String> {
    List<CustomerOrder> findByCustomerAndStateInOrderByPlacedAtDesc(String customer, OrderState[] states);

    List<CustomerOrder> findTop50ByCustomerAndStateInOrderByPlacedAtDesc(String customer,
            OrderState[] states);

    List<CustomerOrder> findByRestaurantIdAndStateInOrderByPlacedAtAsc(String restaurantId,
            OrderState[] states);

    List<CustomerOrder> findTop50ByRestaurantIdAndStateInOrderByPlacedAtDesc(String restaurantId,
            OrderState[] states);

    List<CustomerOrder> findByRestaurantIdAndStateAndDeliveredAtGreaterThanEqual(String restaurantId,
            OrderState state, long since);

    List<CustomerOrder> findByStateAndPlacedAtLessThan(OrderState state, long before);

    List<CustomerOrder> findByStateAndAcceptedAtLessThan(OrderState state, long before);

    boolean existsByCustomerAndMethodIdAndStateIn(String customer, String methodId,
            OrderState[] states);

    List<CustomerOrder> findByPaymentStatusAndUpdatedAtLessThan(String paymentStatus, long before);

    /// Orders in one of `states` whose payment is one of `statuses` and has
    /// not changed since `before`, oldest first: ended orders whose hold was
    /// never let go.
    @Query("select o from CustomerOrder o where o.state in :states "
            + "and o.paymentStatus in :statuses and o.updatedAt < :before "
            + "order by o.updatedAt asc")
    List<CustomerOrder> unreleased(@Param("states") OrderState[] states,
            @Param("statuses") String[] statuses, @Param("before") long before, PageRequest page);

    @Query("select o from CustomerOrder o order by o.placedAt desc")
    List<CustomerOrder> newest(PageRequest page);

    List<CustomerOrder> findByPlacedAtGreaterThanEqual(long since);

    long countByCustomerAndStateIn(String customer, OrderState[] states);

    /// Moves an order from one of `from` to `to`, and answers 0 when it was in
    /// none of them: the database applies the rule and settles a race in the
    /// same statement.
    @Modifying(clearAutomatically = true)
    @Query("update CustomerOrder o set o.state = :to, o.updatedAt = :now "
            + "where o.id = :id and o.state in :from")
    int move(@Param("id") String id, @Param("from") OrderState[] from, @Param("to") OrderState to,
            @Param("now") long now);

    @Modifying(clearAutomatically = true)
    @Query("update CustomerOrder o set o.paymentStatus = :status, o.updatedAt = :now "
            + "where o.id = :id")
    int paymentStatus(@Param("id") String id, @Param("status") String status, @Param("now") long now);

    /// The kitchen takes an order and says how long it needs.
    @Modifying(clearAutomatically = true)
    @Query("update CustomerOrder o set o.state = :to, o.updatedAt = :now, o.acceptedAt = :now, "
            + "o.prepMinutes = :prep where o.id = :id and o.state in :from")
    int accept(@Param("id") String id, @Param("from") OrderState[] from,
            @Param("to") OrderState to, @Param("prep") int prep, @Param("now") long now);

    /// The kitchen has finished an order.
    @Modifying(clearAutomatically = true)
    @Query("update CustomerOrder o set o.state = :to, o.updatedAt = :now, o.readyAt = :now "
            + "where o.id = :id and o.state in :from")
    int ready(@Param("id") String id, @Param("from") OrderState[] from,
            @Param("to") OrderState to, @Param("now") long now);

    /// The order reached the door.
    @Modifying(clearAutomatically = true)
    @Query("update CustomerOrder o set o.state = :to, o.updatedAt = :now, o.deliveredAt = :now "
            + "where o.id = :id and o.state in :from")
    int deliver(@Param("id") String id, @Param("from") OrderState[] from,
            @Param("to") OrderState to, @Param("now") long now);

    /// An order ends without reaching the door, and why.
    @Modifying(clearAutomatically = true)
    @Query("update CustomerOrder o set o.state = :to, o.updatedAt = :now, o.reason = :reason "
            + "where o.id = :id and o.state in :from")
    int end(@Param("id") String id, @Param("from") OrderState[] from,
            @Param("to") OrderState to, @Param("reason") String reason, @Param("now") long now);

    /// A customer's stars for a delivered order, once.
    @Modifying(clearAutomatically = true)
    @Query("update CustomerOrder o set o.stars = :stars, o.updatedAt = :now "
            + "where o.id = :id and o.customer = :customer and o.stars = 0")
    int rate(@Param("id") String id, @Param("customer") String customer,
            @Param("stars") int stars, @Param("now") long now);

    /// The orders placed since `since`, counted by restaurant: `{id, count}`.
    @Query("select o.restaurantId, count(o) from CustomerOrder o where o.placedAt >= :since "
            + "group by o.restaurantId")
    List<Object[]> countByRestaurant(@Param("since") long since);

    long countByPlacedAtGreaterThanEqual(long since);

    long countByStateIn(OrderState[] states);

    /// What customers paid for orders in `state` whose payment is `paid`.
    @Query("select coalesce(sum(o.totalCents), 0) from CustomerOrder o "
            + "where o.state = :state and o.paymentStatus = :paid")
    Long totals(@Param("state") OrderState state, @Param("paid") String paid);

    /// The orders placed since `since`, counted by state: `{state, count}`.
    @Query("select o.state, count(o) from CustomerOrder o where o.placedAt >= :since "
            + "group by o.state")
    List<Object[]> countByState(@Param("since") long since);

    /// One row for each order placed since `since`: `{id, placedAt, state,
    /// totalCents, paymentStatus, deliveredAt}`.
    ///
    /// The charts by day and by hour are counted from this in Java: grouping
    /// by a day would mean grouping by `placedAt` divided by the length of
    /// one, and a query cannot say that portably -- the number is bound twice,
    /// and a database that takes the statement's text at its word does not
    /// see the two as one expression.
    @Query("select o.id, o.placedAt, o.state, o.totalCents, o.paymentStatus, o.deliveredAt "
            + "from CustomerOrder o where o.placedAt >= :since")
    List<Object[]> timeline(@Param("since") long since);

    /// Locks the order's row until the transaction ends, if it is still the
    /// customer's and in one of `states`; 1 when it is, 0 when it is not.
    @Modifying
    @Query("update CustomerOrder o set o.customer = :customer "
            + "where o.id = :id and o.customer = :customer and o.state in :states")
    int holdWhile(@Param("id") String id, @Param("customer") String customer,
            @Param("states") OrderState[] states);

    long countByCustomer(String customer);

    long countByCustomerAndState(String customer, OrderState state);

    /// What a customer paid for orders in `state` whose payment is `paid`.
    @Query("select coalesce(sum(o.totalCents), 0) from CustomerOrder o "
            + "where o.customer = :who and o.state = :state and o.paymentStatus = :paid")
    Long spent(@Param("who") String customer, @Param("state") OrderState state,
            @Param("paid") String paid);
}
