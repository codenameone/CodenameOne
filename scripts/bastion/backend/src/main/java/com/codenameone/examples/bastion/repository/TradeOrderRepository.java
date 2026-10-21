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
package com.codenameone.examples.bastion.repository;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Param;
import com.codename1.orm.repository.Query;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.bastion.domain.TradeOrder;

import java.util.List;

@Repository(entity = TradeOrder.class, key = String.class)
public interface TradeOrderRepository extends CrudRepository<TradeOrder, String> {
    TradeOrder findFirstByOwnerAndClientKey(String owner, String clientKey);

    List<TradeOrder> findByOwnerAndStatusOrderByCreatedAtDesc(String owner, String status);

    List<TradeOrder> findByMarketAndStatus(String market, String status);

    /// The sells a buy at `price` crosses, best (lowest) first, then oldest.
    @Query("select o from TradeOrder o where o.market = :market and o.side = :side "
            + "and o.status = :status and o.price <= :price order by o.price asc, o.seq asc")
    List<TradeOrder> asks(@Param("market") String market, @Param("side") String side,
            @Param("status") String status, @Param("price") long price);

    /// The buys a sell at `price` crosses, best (highest) first, then oldest.
    @Query("select o from TradeOrder o where o.market = :market and o.side = :side "
            + "and o.status = :status and o.price >= :price order by o.price desc, o.seq asc")
    List<TradeOrder> bids(@Param("market") String market, @Param("side") String side,
            @Param("status") String status, @Param("price") long price);

    @Query("select sum(o.held) from TradeOrder o where o.owner = :owner and o.status = :status "
            + "and o.side = :side")
    Long held(@Param("owner") String owner, @Param("status") String status,
            @Param("side") String side);
}
