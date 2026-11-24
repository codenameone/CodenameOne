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
package com.codenameone.examples.bazaar.store;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.api.LoyaltyMemberDto;
import com.codenameone.examples.bazaar.api.RewardsApiServer;
import com.codenameone.examples.bazaar.api.RewardsDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.LoyaltyMember;
import com.codenameone.examples.bazaar.domain.LoyaltyProgram;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.loyalty.Loyalty;
import com.codenameone.examples.bazaar.loyalty.LoyaltyMath;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.LoyaltyMemberRepository;
import com.codenameone.examples.bazaar.repository.LoyaltyProgramRepository;
import com.codenameone.examples.bazaar.repository.ReviewRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.WishlistRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The server's half of `RewardsApi`: the caller's points and tier from every
/// store's loyalty programme, and the counts of what they did, all read from
/// the same rows the rest of the app writes. Nothing here is kept apart for
/// the badges, so they cannot disagree with the orders and reviews screens.
@Component
public class RewardsEndpoint implements RewardsApiServer {
    private final StoreOrderRepository orders;
    private final ReviewRepository reviews;
    private final WishlistRepository wishlists;
    private final LoyaltyMemberRepository members;
    private final LoyaltyProgramRepository programs;
    private final Loyalty loyalty;

    public RewardsEndpoint(StoreOrderRepository orders, ReviewRepository reviews,
            WishlistRepository wishlists, LoyaltyMemberRepository members,
            LoyaltyProgramRepository programs, Loyalty loyalty) {
        this.orders = orders;
        this.reviews = reviews;
        this.wishlists = wishlists;
        this.members = members;
        this.programs = programs;
        this.loyalty = loyalty;
    }

    @Override
    public RewardsDto mine(String orderId) throws Exception {
        return rewards(Codes.callerOrNull(), orderId);
    }

    /// The standing of `username`; zeros for a guest.
    @Transactional(readOnly = true)
    public RewardsDto rewards(String username, String orderId) throws IOException {
        if (username == null || username.length() == 0) {
            return new RewardsDto(0, 0, "", "", 0, 0, 0, 0, 0, 0, 0, 0);
        }
        List<StoreOrder> all = orders.findByUsernameOrderByCreatedAtDesc(username);
        List<String> storeIds = new ArrayList<String>();
        int placed = 0;
        int delivered = 0;
        long orderPoints = 0;
        for (int iter = 0; iter < all.size(); iter++) {
            StoreOrder o = all.get(iter);
            if (o.getId().equals(orderId)) {
                LoyaltyProgram p = programs.findByStoreId(o.getStoreId());
                if (p != null && p.isActive()) {
                    orderPoints = LoyaltyMath.points(o.getSubtotalCents() - o.getDiscountCents(),
                            p);
                }
            }
            if (Orders.CANCELLED.equals(o.getStatus())) {
                continue;
            }
            placed++;
            if (Orders.FULFILLED.equals(o.getFulfilment())) {
                delivered++;
            }
            if (!storeIds.contains(o.getStoreId())) {
                storeIds.add(o.getStoreId());
            }
        }
        long points = 0;
        long lifetime = 0;
        LoyaltyMemberDto best = null;
        for (int iter = 0; iter < storeIds.size(); iter++) {
            LoyaltyMember m = members.findByStoreIdAndUsername(storeIds.get(iter), username);
            if (m == null) {
                continue;
            }
            LoyaltyMemberDto d = loyalty.dto(m);
            points += d.balance();
            lifetime += d.lifetime();
            if (best == null || d.qualifying() > best.qualifying()) {
                best = d;
            }
        }
        return new RewardsDto(points, lifetime,
                best == null || best.tierName() == null ? "" : best.tierName(),
                best == null || best.nextTierName() == null ? "" : best.nextTierName(),
                best == null ? 0 : best.qualifying(), best == null ? 0 : best.toNextTier(),
                placed, delivered, reviews.findByUsernameOrderByCreatedAtDesc(username).size(),
                storeIds.size(), wishlists.findByUsernameOrderByCreatedAtDesc(username).size(),
                orderPoints);
    }
}
