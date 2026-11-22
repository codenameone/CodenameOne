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
package com.codenameone.examples.bazaar.returns;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.notify.Emails;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.api.RefundLineDto;
import com.codenameone.examples.bazaar.api.RefundRequestDto;
import com.codenameone.examples.bazaar.api.ReturnDto;
import com.codenameone.examples.bazaar.api.ReturnReceiveDto;
import com.codenameone.examples.bazaar.api.ReturnRefundDto;
import com.codenameone.examples.bazaar.api.ReturnRequestDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.ReturnLine;
import com.codenameone.examples.bazaar.domain.ReturnRequest;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.api.OrderSettingsDto;
import com.codenameone.examples.bazaar.order.OrderSettings;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.ReturnLineRepository;
import com.codenameone.examples.bazaar.repository.ReturnRequestRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Returns, REQUESTED by the shopper, then APPROVED or DECLINED by the store,
/// RECEIVED when the parcel arrives (back on hand as a RETURN move when the
/// store says so) and REFUNDED through [Orders#refund] -- the same refund as
/// the order page's, so the card, gift cards and the order's totals move the
/// one way. The refund never restocks: receiving did that already.
///
/// Only delivered (or collected) units can come back, within
/// the store's return window ([OrderSettings], thirty days unless it set
/// another) of the last delivery, and never a final-sale product. How the
/// money goes back -- to the card, as store credit, or onto an exchange --
/// is the shopper's choice, settled by [Resolutions].
@Component
public class Returns {
    public static final String REQUESTED = "REQUESTED";
    public static final String APPROVED = "APPROVED";
    public static final String DECLINED = "DECLINED";
    public static final String RECEIVED = "RECEIVED";
    public static final String REFUNDED = "REFUNDED";
    /// The window a store that set none has, in days.
    public static final int WINDOW_DAYS = OrderSettings.WINDOW_DAYS;
    private static final long DAY_MILLIS = 86400000L;
    @com.codename1.backend.annotations.Autowired
    private OrderSettings settings;
    @com.codename1.backend.annotations.Autowired
    private Resolutions resolutions;

    private final ReturnRequestRepository returns;
    private final ReturnLineRepository returnLines;
    private final StoreOrderRepository orders;
    private final OrderLineRepository lines;
    private final StoreRepository stores;
    private final Orders orderService;
    private final Fulfillments fulfillments;
    private final Inventory inventory;
    private final StoreAccess access;
    private final Emails emails;

    private final ReturnLabels returnLabels;

    public Returns(ReturnRequestRepository returns, ReturnLineRepository returnLines,
            StoreOrderRepository orders, OrderLineRepository lines, StoreRepository stores,
            Orders orderService, Fulfillments fulfillments, Inventory inventory,
            StoreAccess access, Emails emails, ReturnLabels returnLabels) {
        this.returnLabels = returnLabels;
        this.emails = emails;
        this.returns = returns;
        this.returnLines = returnLines;
        this.orders = orders;
        this.lines = lines;
        this.stores = stores;
        this.orderService = orderService;
        this.fulfillments = fulfillments;
        this.inventory = inventory;
        this.access = access;
    }

    // -------------------------------------------------------------- shopper

    public ReturnDto request(String username, String orderId, ReturnRequestDto req)
            throws IOException {
        // Answers 404 unless the order is the caller's.
        orderService.mineDetail(username, orderId);
        long delivered = fulfillments.deliveredAt(orderId);
        if (delivered == 0) {
            throw new ResponseStatusException(409, "Nothing of the order has arrived yet");
        }
        StoreOrder order = orders.findById(orderId);
        OrderSettingsDto rules = settings.of(order.getStoreId());
        if (!withinWindow(delivered, rules)) {
            throw new ResponseStatusException(409, "Returns are accepted for "
                    + rules.returnWindowDays() + " days after delivery");
        }
        if (req != null && req.lines() != null) {
            List<OrderLine> ls = lines.findByOrderId(orderId);
            for (int iter = 0; iter < req.lines().size(); iter++) {
                FulfilLineDto w = req.lines().get(iter);
                OrderLine line = w == null ? null : find(ls, w.orderLineId());
                if (line != null && settings.finalSale(rules, line.getProductId())) {
                    throw new ResponseStatusException(409, line.getTitle()
                            + " is final sale and cannot be returned");
                }
            }
        }
        Map<String, Integer> arrived = fulfillments.deliveredUnits(orderId);
        String id = requestTx(username, orderId, req, arrived);
        orderService.publish(orders.findById(orderId));
        return read(id);
    }

    /// Units of each order line the shopper may still ask back: what arrived
    /// within the window, less refunds and earlier requests. Empty when
    /// nothing arrived yet or the window closed. The same arithmetic
    /// [#request] holds a request to, so a page that offers the return form
    /// only for these lines offers it exactly when it would be accepted.
    @Transactional(readOnly = true)
    public Map<String, Integer> returnable(String orderId) throws IOException {
        Map<String, Integer> out = new HashMap<String, Integer>();
        long delivered = fulfillments.deliveredAt(orderId);
        StoreOrder order = orders.findById(orderId);
        OrderSettingsDto rules = order == null ? OrderSettings.DEFAULTS
                : settings.of(order.getStoreId());
        if (delivered == 0 || !withinWindow(delivered, rules)) {
            return out;
        }
        Map<String, Integer> arrived = fulfillments.deliveredUnits(orderId);
        Map<String, Integer> asked = askedBefore(orderId);
        List<OrderLine> ls = lines.findByOrderId(orderId);
        for (int iter = 0; iter < ls.size(); iter++) {
            OrderLine line = ls.get(iter);
            int left = count(arrived, line.getId()) - line.getRefundedQty()
                    - count(asked, line.getId());
            if (left > 0 && !settings.finalSale(rules, line.getProductId())) {
                out.put(line.getId(), Integer.valueOf(left));
            }
        }
        return out;
    }

    /// Whether `delivered` is within the store's return window.
    public static boolean withinWindow(long delivered, OrderSettingsDto rules) {
        return System.currentTimeMillis() - delivered <= rules.returnWindowDays() * DAY_MILLIS;
    }

    @Transactional(rollbackFor = IOException.class)
    private String requestTx(String username, String orderId, ReturnRequestDto req,
            Map<String, Integer> arrived) throws IOException {
        StoreOrder order = orders.findById(orderId);
        if (Orders.CANCELLED.equals(order.getStatus())) {
            throw new ResponseStatusException(409, "The order was cancelled");
        }
        if (req == null || req.lines() == null || req.lines().isEmpty()) {
            throw new ResponseStatusException(400, "Choose what to send back");
        }
        Map<String, Integer> asked = askedBefore(orderId);
        List<OrderLine> ls = lines.findByOrderId(orderId);
        ReturnRequest r = new ReturnRequest();
        r.setId(Ids.next());
        r.setOrderId(orderId);
        r.setStoreId(order.getStoreId());
        r.setUsername(username);
        r.setStatus(REQUESTED);
        r.setReason(Codes.cut(req.reason(), 200));
        r.setNote(Codes.cut(req.note(), 1000));
        r.setCreatedAt(Instant.now());
        r.setUpdatedAt(r.getCreatedAt());
        returns.insert(r);
        for (int iter = 0; iter < req.lines().size(); iter++) {
            FulfilLineDto w = req.lines().get(iter);
            OrderLine line = w == null ? null : find(ls, w.orderLineId());
            if (line == null || w.quantity() <= 0) {
                throw new ResponseStatusException(400, "That line is not on the order");
            }
            int left = count(arrived, line.getId()) - line.getRefundedQty()
                    - count(asked, line.getId());
            if (w.quantity() > left) {
                throw new ResponseStatusException(409, "More of " + line.getTitle()
                        + " than arrived and can still be returned");
            }
            asked.put(line.getId(), Integer.valueOf(count(asked, line.getId()) + w.quantity()));
            ReturnLine row = new ReturnLine();
            row.setId(Ids.next());
            row.setReturnId(r.getId());
            row.setOrderLineId(line.getId());
            row.setQuantity(w.quantity());
            returnLines.insert(row);
        }
        orderService.event(orderId, "RETURN_REQUESTED", "Return requested", username);
        return r.getId();
    }

    /// Units already in a return that was not declined and not yet refunded
    /// (refunded ones count through the line's refunded quantity).
    private Map<String, Integer> askedBefore(String orderId) throws IOException {
        Map<String, Integer> out = new HashMap<String, Integer>();
        List<ReturnRequest> rs = returns.findByOrderId(orderId);
        for (int iter = 0; iter < rs.size(); iter++) {
            String s = rs.get(iter).getStatus();
            if (DECLINED.equals(s) || REFUNDED.equals(s)) {
                continue;
            }
            List<ReturnLine> rl = returnLines.findByReturnId(rs.get(iter).getId());
            for (int l = 0; l < rl.size(); l++) {
                out.put(rl.get(l).getOrderLineId(), Integer.valueOf(
                        count(out, rl.get(l).getOrderLineId()) + rl.get(l).getQuantity()));
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<ReturnDto> mine(String username, String orderId) throws IOException {
        orderService.mineDetail(username, orderId);
        List<ReturnDto> out = new ArrayList<ReturnDto>();
        List<ReturnRequest> rs = returns.findByOrderId(orderId);
        for (int iter = 0; iter < rs.size(); iter++) {
            out.add(dto(rs.get(iter)));
        }
        return out;
    }

    /// The most of their returns a shopper's account lists.
    static final int MAX_REQUESTED = 100;

    /// Every return `username` asked for, newest first: only their own,
    /// whatever store it went to.
    @Transactional(readOnly = true)
    public List<ReturnDto> requested(String username) throws IOException {
        List<ReturnDto> out = new ArrayList<ReturnDto>();
        List<ReturnRequest> rs = returns.findByUsernameOrderByCreatedAtDesc(username);
        for (int iter = 0; iter < rs.size() && iter < MAX_REQUESTED; iter++) {
            out.add(dto(rs.get(iter)));
        }
        return out;
    }

    // ------------------------------------------------------------- merchant

    @Transactional(readOnly = true)
    public List<ReturnDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<ReturnDto> out = new ArrayList<ReturnDto>();
        List<ReturnRequest> rs = returns.findByStoreIdOrderByCreatedAtDesc(storeId);
        for (int iter = 0; iter < rs.size(); iter++) {
            out.add(dto(rs.get(iter)));
        }
        return out;
    }

    public ReturnDto approve(String username, String storeId, String id) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        ReturnDto approved = step(storeId, id, REQUESTED, APPROVED, "Return approved", username);
        // The prepaid label is bought once the approval is recorded, outside
        // its transaction: a carrier that fails never undoes the approval,
        // and the mail below goes out with the label or without it.
        returnLabels.buy(id);
        emails.returnApproved(id);
        return approved;
    }

    public ReturnDto decline(String username, String storeId, String id) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return step(storeId, id, REQUESTED, DECLINED, "Return declined", username);
    }

    /// The parcel arrived. With `restock` its units go back on hand where
    /// they were sold from, as RETURN moves.
    public ReturnDto receive(String username, String storeId, String id, ReturnReceiveDto req)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.STOCK);
        inStore(storeId, id);
        receiveTx(id, req != null && req.restock(), username);
        return published(id);
    }

    @Transactional(rollbackFor = IOException.class)
    private void receiveTx(String id, boolean restock, String username) throws IOException {
        if (returns.claimStatus(id, APPROVED, RECEIVED) != 1) {
            throw new ResponseStatusException(409, "Only an approved return is received");
        }
        ReturnRequest r = returns.findById(id);
        r.setUpdatedAt(Instant.now());
        returns.save(r);
        if (restock) {
            List<OrderLine> ls = lines.findByOrderId(r.getOrderId());
            List<ReturnLine> rl = returnLines.findByReturnId(id);
            for (int iter = 0; iter < rl.size(); iter++) {
                OrderLine line = find(ls, rl.get(iter).getOrderLineId());
                if (line != null && line.getLocationId().length() > 0) {
                    inventory.restock(r.getStoreId(), line.getVariantId(), line.getLocationId(),
                            rl.get(iter).getQuantity(), id, username, Inventory.RETURN);
                }
            }
        }
        orderService.event(r.getOrderId(), "RETURN_RECEIVED", restock
                ? "Returned goods received and restocked" : "Returned goods received", username);
    }

    /// Refunds the returned units (and `shippingCents` of the shipping)
    /// through the order's own refund. The return is claimed first, so a
    /// second click cannot refund twice, and let go again if the refund fails.
    public ReturnDto refund(String username, String storeId, String id, ReturnRefundDto req)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        ReturnRequest r = inStore(storeId, id);
        String from = r.getStatus();
        if (!APPROVED.equals(from) && !RECEIVED.equals(from)) {
            throw new ResponseStatusException(409, "Only an approved or received return is "
                    + "refunded");
        }
        claim(id, from, REFUNDED);
        try {
            List<RefundLineDto> wanted = new ArrayList<RefundLineDto>();
            List<FulfilLineDto> ls = dto(r).lines();
            for (int iter = 0; iter < ls.size(); iter++) {
                wanted.add(new RefundLineDto(ls.get(iter).orderLineId(), ls.get(iter).quantity()));
            }
            resolutions.settle(username, storeId, r, new RefundRequestDto(wanted,
                    req == null ? 0 : req.shippingCents(), false, "Return " + id));
        } catch (IOException | RuntimeException err) {
            claim(id, REFUNDED, from);
            throw err;
        }
        return published(id);
    }

    private ReturnDto step(String storeId, String id, String from, String to, String message,
            String username) throws IOException {
        inStore(storeId, id);
        stepTx(id, from, to, message, username);
        return published(id);
    }

    @Transactional(rollbackFor = IOException.class)
    private void stepTx(String id, String from, String to, String message, String username)
            throws IOException {
        if (returns.claimStatus(id, from, to) != 1) {
            throw new ResponseStatusException(409, "The return is not " + from);
        }
        ReturnRequest r = returns.findById(id);
        r.setUpdatedAt(Instant.now());
        returns.save(r);
        orderService.event(r.getOrderId(), "RETURN_" + to, message, username);
    }

    @Transactional(rollbackFor = IOException.class)
    private void claim(String id, String from, String to) throws IOException {
        if (returns.claimStatus(id, from, to) != 1) {
            throw new ResponseStatusException(409, "The return is changing; try again");
        }
    }

    // -------------------------------------------------------------- reading

    private ReturnDto published(String id) throws IOException {
        ReturnDto out = read(id);
        orderService.publish(orderRead(out.orderId()));
        return out;
    }

    @Transactional(readOnly = true)
    private StoreOrder orderRead(String id) {
        return orders.findById(id);
    }

    @Transactional(readOnly = true)
    private ReturnRequest inStore(String storeId, String id) {
        ReturnRequest r = id == null ? null : returns.findById(id);
        if (r == null || !storeId.equals(r.getStoreId())) {
            throw new ResponseStatusException(404, "No such return");
        }
        return r;
    }

    @Transactional(readOnly = true)
    private ReturnDto read(String id) throws IOException {
        return dto(returns.findById(id));
    }

    @Transactional(readOnly = true)
    private ReturnDto dto(ReturnRequest r) throws IOException {
        StoreOrder order = orders.findById(r.getOrderId());
        List<OrderLine> ls = lines.findByOrderId(r.getOrderId());
        List<ReturnLine> rl = returnLines.findByReturnId(r.getId());
        Store store = stores.findById(r.getStoreId());
        boolean inclusive = store != null && store.isTaxInclusive();
        List<FulfilLineDto> out = new ArrayList<FulfilLineDto>();
        long refunded = 0;
        for (int iter = 0; iter < rl.size(); iter++) {
            OrderLine line = find(ls, rl.get(iter).getOrderLineId());
            if (line == null) {
                continue;
            }
            out.add(new FulfilLineDto(line.getId(), line.getTitle(), line.getSku(),
                    rl.get(iter).getQuantity()));
            if (REFUNDED.equals(r.getStatus())) {
                refunded += Orders.lineRefund(line, rl.get(iter).getQuantity(), inclusive);
            }
        }
        return new ReturnDto(r.getId(), r.getOrderId(), order == null ? 0 : order.getNumber(),
                r.getStoreId(), r.getUsername(), r.getStatus(), r.getReason(), r.getNote(),
                refunded, millis(r.getCreatedAt()), millis(r.getUpdatedAt()), out);
    }

    private static int count(Map<String, Integer> m, String key) {
        Integer v = m.get(key);
        return v == null ? 0 : v.intValue();
    }

    private static OrderLine find(List<OrderLine> rows, String id) {
        for (int iter = 0; iter < rows.size(); iter++) {
            if (rows.get(iter).getId().equals(id)) {
                return rows.get(iter);
            }
        }
        return null;
    }

    private static long millis(Instant at) {
        return at == null ? 0 : at.toEpochMilli();
    }
}
