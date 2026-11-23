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

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Modifying;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.bazaar.domain.GiftCard;
import java.time.Instant;
import java.util.List;

/// Reads and writes [GiftCard] rows.
@Repository(entity = GiftCard.class, key = String.class)
public interface GiftCardRepository extends CrudRepository<GiftCard, String> {
    GiftCard findByCode(String code);

    List<GiftCard> findByStoreIdOrderByCreatedAtDesc(String storeId);

    /// Takes `amount` off a card that is active, unexpired at `now` and has
    /// that much. An expiry at the epoch means the card never expires, as in
    /// `Quoter.usable`.
    ///
    /// @return 1 when taken
    @Modifying
    @Query("update GiftCard g set g.balanceCents = g.balanceCents - :amount where g.id = :id "
            + "and g.active = true and g.balanceCents >= :amount "
            + "and (g.expiresAt <= :epoch or g.expiresAt > :now)")
    int debitIf(@Param("id") String id, @Param("amount") long amount,
            @Param("epoch") Instant epoch, @Param("now") Instant now);

    /// Puts `amount` back on a card.
    @Modifying
    @Query("update GiftCard g set g.balanceCents = g.balanceCents + :amount where g.id = :id")
    int credit(@Param("id") String id, @Param("amount") long amount);

    /// Gift cards waiting to be mailed whose delivery time is before `before`.
    List<GiftCard> findByDeliveredAtAndDeliverAtLessThan(java.time.Instant never,
            java.time.Instant before);

    /// Marks a scheduled card delivered, once.
    @com.codename1.orm.repository.Modifying
    @com.codename1.orm.repository.Query("update GiftCard g set g.deliveredAt = :at "
            + "where g.id = :id and g.deliveredAt = :never")
    int claimDelivery(@com.codename1.orm.repository.Param("id") String id,
            @com.codename1.orm.repository.Param("never") java.time.Instant never,
            @com.codename1.orm.repository.Param("at") java.time.Instant at);

    /// Moves every row of one customer to another (a merge).
    @com.codename1.orm.repository.Modifying(clearAutomatically = true)
    @com.codename1.orm.repository.Query("update GiftCard x set x.customerId = :to where x.customerId = :from")
    int moveCustomer(@com.codename1.orm.repository.Param("from") String from,
            @com.codename1.orm.repository.Param("to") String to);

    List<GiftCard> findByCustomerId(String customerId);
}
