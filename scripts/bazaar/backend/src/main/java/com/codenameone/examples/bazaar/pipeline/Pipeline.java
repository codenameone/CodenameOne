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

package com.codenameone.examples.bazaar.pipeline;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.api.BoardColumnDto;
import com.codenameone.examples.bazaar.api.BoardMoveDto;
import com.codenameone.examples.bazaar.api.OrderCardDto;
import com.codenameone.examples.bazaar.api.PipelineCardDto;
import com.codenameone.examples.bazaar.api.PipelineColumnDto;
import com.codenameone.examples.bazaar.api.PipelineMoveDto;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.domain.CartLine;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.ProductViewEvent;
import com.codenameone.examples.bazaar.domain.ReturnRequest;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.StoreStaff;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.events.StoreEvents;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.live.LiveHub;
import com.codenameone.examples.bazaar.ops.Perms;
import com.codenameone.examples.bazaar.ops.SalesPipeline;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.CartLineRepository;
import com.codenameone.examples.bazaar.repository.CartRepository;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.ProductViewEventRepository;
import com.codenameone.examples.bazaar.repository.ReturnRequestRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreStaffRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.returns.Returns;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The sales pipeline: every live cart and open order of a store as a card on
/// one board, from a shopper browsing to a return request (see
/// [SalesPipeline] for the columns and the moves).
///
/// The columns are worked out from the rows each time, never stored: a cart's
/// from whether it can be reached and was reminded, an order's from the
/// fulfilment board ([Fulfillments#board]) and its return requests. A card is
/// only moved by doing the real thing -- the recovery mail, packing, shipping,
/// marking delivered -- so the board can never say something the store has
/// not done.
///
/// Carts count while they have items and were touched in the last
/// [#CART_DAYS] days; each carries a purchase-intent score from the views,
/// visits and time on product of its visitor (the cart token, or the
/// account), the cart's value and the shopper's earlier orders.
@Component
public class Pipeline {
    /// Cards per page of a column.
    public static final int PAGE = 20;
    /// How far back a cart is still on the board.
    public static final long CART_DAYS = 30;
    private static final long DAY = 86400000L;

    private final CartRepository carts;
    private final CartLineRepository cartLines;
    private final VariantRepository variants;
    private final CustomerRepository customers;
    private final StoreOrderRepository orders;
    private final ReturnRequestRepository returns;
    private final ProductViewEventRepository views;
    private final StoreStaffRepository staff;
    private final Fulfillments fulfillments;
    private final StoreEvents events;
    private final LiveHub live;
    private final StoreAccess access;

    public Pipeline(CartRepository carts, CartLineRepository cartLines,
            VariantRepository variants, CustomerRepository customers,
            StoreOrderRepository orders, ReturnRequestRepository returns,
            ProductViewEventRepository views, StoreStaffRepository staff,
            Fulfillments fulfillments, StoreEvents events, LiveHub live, StoreAccess access) {
        this.carts = carts;
        this.cartLines = cartLines;
        this.variants = variants;
        this.customers = customers;
        this.orders = orders;
        this.returns = returns;
        this.views = views;
        this.staff = staff;
        this.fulfillments = fulfillments;
        this.events = events;
        this.live = live;
        this.access = access;
    }

    /// The board; `column` blank for the first page of every column.
    public List<PipelineColumnDto> board(String username, String storeId, String column,
            int page) throws IOException {
        access.requireArea(username, storeId, Perms.ORDERS, false, StoreAccess.ANYONE);
        Store store = access.requireStore(username, storeId);
        boolean one = column != null && column.trim().length() > 0;
        if (one && SalesPipeline.indexOf(column.trim()) < 0) {
            throw new ResponseStatusException(400, "There is no column " + column.trim());
        }
        Map<String, List<PipelineCardDto>> cards = cards(username, store,
                System.currentTimeMillis());
        String[] ids = SalesPipeline.columns();
        List<PipelineColumnDto> out = new ArrayList<PipelineColumnDto>();
        for (int iter = 0; iter < ids.length; iter++) {
            if (one && !ids[iter].equals(column.trim())) {
                continue;
            }
            int p = one ? Math.max(0, page) : 0;
            List<PipelineCardDto> all = cards.get(ids[iter]);
            int start = Math.min(all.size(), p * PAGE);
            int end = Math.min(all.size(), start + PAGE);
            out.add(new PipelineColumnDto(ids[iter], SalesPipeline.title(ids[iter]), all.size(),
                    p, end < all.size(), new ArrayList<PipelineCardDto>(all.subList(start, end))));
        }
        return out;
    }

    /// Moves a card, doing what the column means; any move [SalesPipeline]
    /// does not allow, or a card that is no longer where the caller saw it,
    /// is a 409.
    public List<PipelineColumnDto> move(String username, String storeId, PipelineMoveDto req)
            throws IOException {
        // Who is asking comes first, so another store's staff learn nothing
        // from the shape of the answer.
        access.requireArea(username, storeId, Perms.ORDERS, true, StoreAccess.STOCK);
        if (req == null || req.cardId() == null || req.toColumn() == null) {
            throw new ResponseStatusException(400, "Say which card goes where");
        }
        Store store = access.requireStore(username, storeId);
        String from = columnOf(username, store, req.cardId());
        if (from == null) {
            throw new ResponseStatusException(404, "No such card");
        }
        if (!SalesPipeline.canMove(from, req.toColumn())) {
            throw new ResponseStatusException(409, "A card cannot move from "
                    + SalesPipeline.title(from) + " to " + SalesPipeline.title(req.toColumn()));
        }
        if (SalesPipeline.REMINDED.equals(req.toColumn())) {
            remind(storeId, req.cardId());
        } else {
            String to = SalesPipeline.FULFILLING.equals(req.toColumn())
                    ? Fulfillments.COLUMN_PACKED : SalesPipeline.SHIPPED.equals(req.toColumn())
                    ? Fulfillments.COLUMN_SHIPPED : Fulfillments.COLUMN_DELIVERED;
            // Publishes the order to the live channel itself.
            fulfillments.moveCard(username, storeId, new BoardMoveDto(req.cardId(), to));
        }
        return board(username, storeId, null, 0);
    }

    /// The recovery mail, once: the same conditional mark the hourly sweep
    /// uses, so a drag and the sweep never both send it.
    private void remind(String storeId, String cartId) throws IOException {
        Cart cart = cartRead(storeId, cartId);
        long now = System.currentTimeMillis();
        if (!claim(cartId, now)) {
            throw new ResponseStatusException(409, "The recovery mail already went");
        }
        events.cartAbandoned(cart, customerRead(cart));
        List<String> users = staffRead(storeId);
        live.changed("cart", cartId, SalesPipeline.REMINDED, users);
    }

    @Transactional(readOnly = true)
    private Cart cartRead(String storeId, String cartId) {
        Cart cart = carts.findById(cartId);
        if (cart == null || !storeId.equals(cart.getStoreId())) {
            throw new ResponseStatusException(404, "No such cart");
        }
        return cart;
    }

    @Transactional(rollbackFor = IOException.class)
    private boolean claim(String cartId, long now) throws IOException {
        return carts.claimReminder(cartId, Instant.EPOCH, Instant.ofEpochMilli(now)) == 1;
    }

    @Transactional(readOnly = true)
    private Customer customerRead(Cart cart) {
        return cart.getUsername().length() == 0 ? null
                : customers.findByStoreIdAndUsername(cart.getStoreId(), cart.getUsername());
    }

    @Transactional(readOnly = true)
    private List<String> staffRead(String storeId) {
        List<String> users = new ArrayList<String>();
        List<StoreStaff> team = staff.findByStoreIdOrderByDisplayNameAsc(storeId);
        for (int iter = 0; iter < team.size(); iter++) {
            users.add(team.get(iter).getUsername());
        }
        return users;
    }

    /// The column a card is in now, or null for no card of this store.
    private String columnOf(String username, Store store, String cardId) throws IOException {
        Map<String, List<PipelineCardDto>> all = cards(username, store,
                System.currentTimeMillis());
        for (List<PipelineCardDto> list : all.values()) {
            for (int iter = 0; iter < list.size(); iter++) {
                if (list.get(iter).id().equals(cardId)) {
                    return list.get(iter).column();
                }
            }
        }
        return null;
    }

    /// Every card of the store by column, each column in its order.
    private Map<String, List<PipelineCardDto>> cards(String username, Store store, long now)
            throws IOException {
        Map<String, List<PipelineCardDto>> out = new HashMap<String, List<PipelineCardDto>>();
        String[] ids = SalesPipeline.columns();
        for (int iter = 0; iter < ids.length; iter++) {
            out.put(ids[iter], new ArrayList<PipelineCardDto>());
        }
        Data d = read(store.getId(), now);
        cartCards(store, d, now, out);
        orderCards(username, store, d, out);
        return out;
    }

    /// The rows the board reads, in one read-only transaction.
    static final class Data {
        final List<Cart> carts = new ArrayList<Cart>();
        final Map<String, List<CartLine>> lines = new HashMap<String, List<CartLine>>();
        final Map<String, Long> prices = new HashMap<String, Long>();
        final Map<String, List<Long>> viewsBy = new HashMap<String, List<Long>>();
        final Map<String, Integer> ordersBy = new HashMap<String, Integer>();
        final Map<String, StoreOrder> ordersById = new HashMap<String, StoreOrder>();
        final Set<String> openReturns = new HashSet<String>();
    }

    @Transactional(readOnly = true)
    private Data read(String storeId, long now) {
        Data d = new Data();
        Instant since = Instant.ofEpochMilli(now - CART_DAYS * DAY);
        List<Cart> recent = carts.findByStoreIdAndUpdatedAtGreaterThanEqualOrderByUpdatedAtDesc(
                storeId, since);
        for (int iter = 0; iter < recent.size(); iter++) {
            Cart c = recent.get(iter);
            List<CartLine> ls = cartLines.findByCartId(c.getId());
            if (ls.isEmpty()) {
                continue;
            }
            d.carts.add(c);
            d.lines.put(c.getId(), ls);
            for (int at = 0; at < ls.size(); at++) {
                String v = ls.get(at).getVariantId();
                if (!d.prices.containsKey(v)) {
                    Variant row = variants.findById(v);
                    d.prices.put(v, Long.valueOf(row == null ? 0 : row.getPriceCents()));
                }
            }
        }
        List<ProductViewEvent> vs = views.findByStoreIdAndViewedAtGreaterThanEqual(storeId,
                since);
        for (int iter = 0; iter < vs.size(); iter++) {
            ProductViewEvent v = vs.get(iter);
            if (v.getVisitor().length() == 0) {
                continue;
            }
            List<Long> times = d.viewsBy.get(v.getVisitor());
            if (times == null) {
                times = new ArrayList<Long>();
                d.viewsBy.put(v.getVisitor(), times);
            }
            times.add(Long.valueOf(v.getViewedAt().toEpochMilli()));
        }
        List<StoreOrder> all = orders.findByStoreIdOrderByCreatedAtDesc(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            StoreOrder o = all.get(iter);
            d.ordersById.put(o.getId(), o);
            if (!Orders.CANCELLED.equals(o.getStatus()) && !Orders.isUnconvertedDraft(o)
                    && o.getUsername().length() > 0) {
                Integer n = d.ordersBy.get(o.getUsername());
                d.ordersBy.put(o.getUsername(), Integer.valueOf(n == null ? 1 : n + 1));
            }
        }
        List<ReturnRequest> rs = returns.findByStoreIdOrderByCreatedAtDesc(storeId);
        for (int iter = 0; iter < rs.size(); iter++) {
            String s = rs.get(iter).getStatus();
            if (Returns.REQUESTED.equals(s) || Returns.APPROVED.equals(s)) {
                d.openReturns.add(rs.get(iter).getOrderId());
            }
        }
        return d;
    }

    private void cartCards(Store store, Data d, long now,
            Map<String, List<PipelineCardDto>> out) {
        for (int iter = 0; iter < d.carts.size(); iter++) {
            Cart c = d.carts.get(iter);
            long value = 0;
            List<CartLine> ls = d.lines.get(c.getId());
            for (int at = 0; at < ls.size(); at++) {
                Long price = d.prices.get(ls.get(at).getVariantId());
                value += (price == null ? 0 : price.longValue()) * ls.get(at).getQuantity();
            }
            SalesPipeline.Signals s = SalesPipeline.fromViews(viewTimes(d, c), value,
                    c.getUsername().length() == 0 ? 0 : count(d.ordersBy.get(c.getUsername())));
            boolean reachable = c.getEmail().length() > 0 || c.getUsername().length() > 0;
            String column = SalesPipeline.cartColumn(reachable,
                    !Instant.EPOCH.equals(c.getReminderSentAt()));
            String who = c.getEmail().length() > 0 ? c.getEmail() : c.getUsername();
            out.get(column).add(new PipelineCardDto(c.getId(), SalesPipeline.CART, 0, who, value,
                    store.getCurrency(), column, c.getCreatedAt().toEpochMilli(),
                    c.getUpdatedAt().toEpochMilli(), SalesPipeline.score(s),
                    SalesPipeline.reasons(s)));
        }
        Comparator<PipelineCardDto> hottest = new Comparator<PipelineCardDto>() {
            @Override
            public int compare(PipelineCardDto a, PipelineCardDto b) {
                if (a.intent() != b.intent()) {
                    return b.intent() - a.intent();
                }
                return Long.compare(b.updatedAt(), a.updatedAt());
            }
        };
        Collections.sort(out.get(SalesPipeline.BROWSING), hottest);
        Collections.sort(out.get(SalesPipeline.CHECKOUT), hottest);
        Collections.sort(out.get(SalesPipeline.REMINDED), hottest);
    }

    /// A cart's view times, oldest first: those of its token and its account.
    private static long[] viewTimes(Data d, Cart c) {
        List<Long> all = new ArrayList<Long>();
        List<Long> byToken = c.getToken().length() == 0 ? null : d.viewsBy.get(c.getToken());
        List<Long> byUser = c.getUsername().length() == 0 ? null : d.viewsBy.get(c.getUsername());
        if (byToken != null) {
            all.addAll(byToken);
        }
        if (byUser != null && byUser != byToken) { // NOPMD CompareObjectsWithEquals - one visitor twice
            all.addAll(byUser);
        }
        Collections.sort(all);
        long[] out = new long[all.size()];
        for (int iter = 0; iter < out.length; iter++) {
            out[iter] = all.get(iter).longValue();
        }
        return out;
    }

    private static int count(Integer n) {
        return n == null ? 0 : n.intValue();
    }

    private void orderCards(String username, Store store, Data d,
            Map<String, List<PipelineCardDto>> out) throws IOException {
        Set<String> placed = new HashSet<String>();
        List<BoardColumnDto> board = fulfillments.board(username, store.getId());
        for (int iter = 0; iter < board.size(); iter++) {
            BoardColumnDto col = board.get(iter);
            for (int at = 0; col.cards() != null && at < col.cards().size(); at++) {
                OrderCardDto o = col.cards().get(at);
                String column = SalesPipeline.orderColumn(col.id(),
                        d.openReturns.contains(o.orderId()));
                if (column == null) {
                    continue;
                }
                placed.add(o.orderId());
                out.get(column).add(orderCard(o.orderId(), o.number(), o.customer(),
                        o.totalCents(), o.currency(), column, o.createdAt(), d));
            }
        }
        // A return asked of an order the fulfilment board no longer shows (a
        // delivered order that was closed).
        for (String orderId : d.openReturns) {
            StoreOrder o = d.ordersById.get(orderId);
            if (o == null || placed.contains(orderId)) {
                continue;
            }
            String who = o.getEmail().length() > 0 ? o.getEmail() : o.getUsername();
            out.get(SalesPipeline.RETURNS).add(orderCard(orderId, o.getNumber(), who,
                    o.getTotalCents(), o.getCurrency(), SalesPipeline.RETURNS,
                    o.getCreatedAt().toEpochMilli(), d));
        }
    }

    private static PipelineCardDto orderCard(String id, int number, String customer, long cents,
            String currency, String column, long createdAt, Data d) {
        StoreOrder row = d.ordersById.get(id);
        long updated = row == null ? createdAt : row.getUpdatedAt().toEpochMilli();
        return new PipelineCardDto(id, SalesPipeline.ORDER, number,
                customer == null ? "" : customer, cents, currency, column, createdAt, updated, -1,
                new ArrayList<String>());
    }
}
