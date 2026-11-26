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

package com.codenameone.examples.bazaar.repository;

import com.codename1.orm.repository.PageRequest;
import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import java.util.List;
import java.time.Instant;

/// Reads and writes [StoreOrder] rows.
@Repository(entity = StoreOrder.class, key = String.class)
public interface StoreOrderRepository extends CrudRepository<StoreOrder, String> {
    List<StoreOrder> findByStoreIdOrderByCreatedAtDesc(String storeId);

    List<StoreOrder> findByUsernameOrderByCreatedAtDesc(String username);

    List<StoreOrder> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    StoreOrder findByClientId(String clientId);

    StoreOrder findByStoreIdAndNumber(String storeId, int number);

    List<StoreOrder> findByShiftId(String shiftId);

    long countByStoreId(String storeId);

    StoreOrder findFirstByStoreIdOrderByNumberDesc(String storeId);

    /// Moves an order from one status to another; 1 for the one caller that did.
    @Modifying
    @Query("update StoreOrder o set o.status = :to where o.id = :id and o.status = :from")
    int claimStatus(@Param("id") String id, @Param("from") String from, @Param("to") String to);

    /// Moves an order's financial status; 1 for the one caller that did.
    @Modifying
    @Query("update StoreOrder o set o.financial = :to where o.id = :id and o.financial = :from")
    int claimFinancial(@Param("id") String id, @Param("from") String from, @Param("to") String to);

    List<StoreOrder> findByStoreIdAndCreatedAtGreaterThanEqual(String storeId, Instant since);

    List<StoreOrder> findByCreatedAtGreaterThanEqual(Instant since);
    /// A page of a shopper's orders, newest first (index `store_order_username_created_at`).
    List<StoreOrder> findByUsernameOrderByCreatedAtDesc(String username, PageRequest page);

    /// Moves every row of one customer to another (a merge).
    @com.codename1.orm.repository.Modifying(clearAutomatically = true)
    @com.codename1.orm.repository.Query("update StoreOrder x set x.customerId = :to where x.customerId = :from")
    int moveCustomer(@com.codename1.orm.repository.Param("from") String from,
            @com.codename1.orm.repository.Param("to") String to);

    /// Orders of `[from, to)` by channel and source, drafts and cancelled
    /// orders aside: each row is channel, source, orders, subtotals, discounts.
    @Query("select o.channel, o.source, count(o), sum(o.subtotalCents), sum(o.discountCents) "
            + "from StoreOrder o where o.storeId = :storeId and o.createdAt >= :from "
            + "and o.createdAt < :to and o.status <> :cancelled and o.channel <> :draft "
            + "group by o.channel, o.source")
    List<Object[]> channelMix(@Param("storeId") String storeId, @Param("from") Instant from,
            @Param("to") Instant to, @Param("cancelled") String cancelled,
            @Param("draft") String draft);

    /// Draft orders of `[from, to)` that were turned into orders (they have a
    /// location), not cancelled: one row of orders, subtotals, discounts.
    @Query("select count(o), sum(o.subtotalCents), sum(o.discountCents) from StoreOrder o "
            + "where o.storeId = :storeId and o.createdAt >= :from and o.createdAt < :to "
            + "and o.status <> :cancelled and o.channel = :draft and o.locationId <> :blank")
    List<Object[]> convertedDrafts(@Param("storeId") String storeId, @Param("from") Instant from,
            @Param("to") Instant to, @Param("cancelled") String cancelled,
            @Param("draft") String draft, @Param("blank") String blank);

    /// Online orders of `[from, to)` that are not cancelled.
    @Query("select count(o) from StoreOrder o where o.storeId = :storeId and o.createdAt >= :from "
            + "and o.createdAt < :to and o.status <> :cancelled and o.channel = :online")
    Long countOnline(@Param("storeId") String storeId, @Param("from") Instant from,
            @Param("to") Instant to, @Param("cancelled") String cancelled,
            @Param("online") String online);
}
