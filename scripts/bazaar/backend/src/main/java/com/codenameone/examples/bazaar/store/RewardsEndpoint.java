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
import com.codenameone.examples.bazaar.api.StoreBalanceDto;
import com.codenameone.examples.bazaar.domain.LoyaltyLedger;
import com.codenameone.examples.bazaar.domain.LoyaltyMember;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.loyalty.Loyalty;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.LoyaltyMemberRepository;
import com.codenameone.examples.bazaar.repository.LoyaltyLedgerRepository;
import com.codenameone.examples.bazaar.repository.LoyaltyProgramRepository;
import com.codenameone.examples.bazaar.domain.LoyaltyProgram;
import com.codenameone.examples.bazaar.loyalty.LoyaltyMath;
import com.codenameone.examples.bazaar.repository.ReviewRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.WishlistRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The server's half of `RewardsApi`: the caller's points and tier in each
/// store's loyalty programme, kept apart per store, and the counts of what they did, all read from
/// the same rows the rest of the app writes. Nothing here is kept apart for
/// the badges, so they cannot disagree with the orders and reviews screens.
@Component
public class RewardsEndpoint implements RewardsApiServer {
    private final StoreOrderRepository orders;
    private final ReviewRepository reviews;
    private final WishlistRepository wishlists;
    private final LoyaltyMemberRepository members;
    private final LoyaltyLedgerRepository ledger;
    private final StoreRepository stores;
    private final Loyalty loyalty;
    private final LoyaltyProgramRepository programs;

    public RewardsEndpoint(StoreOrderRepository orders, ReviewRepository reviews,
            WishlistRepository wishlists, LoyaltyMemberRepository members,
            LoyaltyLedgerRepository ledger, StoreRepository stores, Loyalty loyalty,
            LoyaltyProgramRepository programs) {
        this.programs = programs;
        this.orders = orders;
        this.reviews = reviews;
        this.wishlists = wishlists;
        this.members = members;
        this.ledger = ledger;
        this.stores = stores;
        this.loyalty = loyalty;
    }

    @Override
    public RewardsDto mine(String orderId) throws Exception {
        return rewards(Codes.callerOrNull(), orderId);
    }

    /// The standing of `username`; zeros for a guest. Only the caller's own
    /// rows are read: an order of someone else's earns them nothing here.
    @Transactional(readOnly = true)
    public RewardsDto rewards(String username, String orderId) throws IOException {
        if (username == null || username.length() == 0) {
            return new RewardsDto(0, 0, "", "", 0, 0, 0, 0, 0, 0, 0, 0, "",
                    new ArrayList<StoreBalanceDto>(), 0);
        }
        // The badges count what the shopper bought, cancelled orders aside.
        List<StoreOrder> all = orders.findByUsernameOrderByCreatedAtDesc(username);
        List<String> boughtFrom = new ArrayList<String>();
        int placed = 0;
        int delivered = 0;
        for (int iter = 0; iter < all.size(); iter++) {
            StoreOrder o = all.get(iter);
            if (Orders.CANCELLED.equals(o.getStatus())) {
                continue;
            }
            placed++;
            if (Orders.FULFILLED.equals(o.getFulfilment())) {
                delivered++;
            }
            if (!boughtFrom.contains(o.getStoreId())) {
                boughtFrom.add(o.getStoreId());
            }
        }
        // The balances are the shopper's memberships, whatever became of the
        // orders that earned them: a store whose only order was cancelled
        // still holds the points a referral or a register sale put there.
        List<LoyaltyMember> mine = members.findByUsername(username);
        List<StoreBalanceDto> balances = new ArrayList<StoreBalanceDto>();
        LoyaltyMemberDto best = null;
        int bestAt = -1;
        for (int iter = 0; iter < mine.size(); iter++) {
            LoyaltyMemberDto d = loyalty.dto(mine.get(iter));
            Store s = stores.findById(mine.get(iter).getStoreId());
            balances.add(new StoreBalanceDto(mine.get(iter).getStoreId(),
                    s == null ? "" : s.getName(), d.balance(),
                    d.tierName() == null ? "" : d.tierName()));
            if (best == null || d.qualifying() > best.qualifying()) {
                best = d;
                bestAt = balances.size() - 1;
            }
        }
        if (bestAt > 0) {
            balances.add(0, balances.remove(bestAt));
        }
        long orderPoints = 0;
        long orderBalance = 0;
        StoreOrder asked = orderId == null || orderId.length() == 0 ? null
                : orders.findById(orderId);
        if (asked != null && username.equals(asked.getUsername())) {
            // What the ledger credited for this order, never a sum worked out
            // again against today's programme.
            LoyaltyLedger earned = ledger.findByStoreIdAndRef(asked.getStoreId(),
                    Loyalty.EARN + ":" + orderId);
            orderPoints = earned == null ? 0 : earned.getPoints();
            // Points are credited when the payment is captured. Until then the
            // order-placed card shows what the store's programme will credit
            // ("added when your order ships"), never a misleading zero.
            if (earned == null && !Orders.CANCELLED.equals(asked.getStatus())) {
                LoyaltyProgram p = programs.findByStoreId(asked.getStoreId());
                if (p != null && p.isActive()) {
                    orderPoints = LoyaltyMath.points(
                            asked.getSubtotalCents() - asked.getDiscountCents(), p);
                }
            }
            for (int iter = 0; iter < balances.size(); iter++) {
                if (balances.get(iter).storeId().equals(asked.getStoreId())) {
                    orderBalance = balances.get(iter).points();
                }
            }
        }
        StoreBalanceDto top = balances.isEmpty() ? null : balances.get(0);
        return new RewardsDto(best == null ? 0 : best.balance(),
                best == null ? 0 : best.lifetime(),
                best == null || best.tierName() == null ? "" : best.tierName(),
                best == null || best.nextTierName() == null ? "" : best.nextTierName(),
                best == null ? 0 : best.qualifying(), best == null ? 0 : best.toNextTier(),
                placed, delivered, reviews.findByUsernameOrderByCreatedAtDesc(username).size(),
                boughtFrom.size(), wishlists.findByUsernameOrderByCreatedAtDesc(username).size(),
                orderPoints, top == null ? "" : top.storeName(), balances, orderBalance);
    }
}
