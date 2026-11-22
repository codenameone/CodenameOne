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
package com.codenameone.examples.bazaar.order;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.AddressDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.DraftOrderDto;
import com.codenameone.examples.bazaar.api.FulfillmentSummaryDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.OrderEventDto;
import com.codenameone.examples.bazaar.api.OrderLineDto;
import com.codenameone.examples.bazaar.api.OrderNoteDto;
import com.codenameone.examples.bazaar.api.RefundDto;
import com.codenameone.examples.bazaar.api.RefundLineDto;
import com.codenameone.examples.bazaar.api.RefundRequestDto;
import com.codenameone.examples.bazaar.api.TenderDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.cart.Quote;
import com.codenameone.examples.bazaar.cart.Quoter;
import com.codenameone.examples.bazaar.domain.Cart;
import com.codenameone.examples.bazaar.domain.Fulfillment;
import com.codenameone.examples.bazaar.domain.OrderEvent;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.Payment;
import com.codenameone.examples.bazaar.domain.Refund;
import com.codenameone.examples.bazaar.domain.RefundLine;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.StoreStaff;
import com.codenameone.examples.bazaar.domain.Tender;
import com.codenameone.examples.bazaar.giftcard.GiftCards;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.events.StoreEvents;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.live.LiveHub;
import com.codenameone.examples.bazaar.ops.Perms;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.pricing.Money;
import com.codenameone.examples.bazaar.repository.FulfillmentRepository;
import com.codenameone.examples.bazaar.repository.OrderEventRepository;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.PaymentRepository;
import com.codenameone.examples.bazaar.repository.RefundLineRepository;
import com.codenameone.examples.bazaar.repository.RefundRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.StoreStaffRepository;
import com.codenameone.examples.bazaar.repository.TenderRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;

import com.codenameone.examples.bazaar.sale.SaleEvents;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Orders after they are placed: what shoppers and staff read, and every
/// change of state.
///
/// The three states move independently. `status` is OPEN until the order is
/// CANCELLED or CLOSED; `financial` follows the money (PENDING, AUTHORIZED,
/// PAID, PARTIALLY_REFUNDED, REFUNDED, VOIDED); `fulfilment` follows the goods
/// (UNFULFILLED, PARTIAL, FULFILLED). Each change is guarded by a conditional
/// update or happens in one transaction, gets an [OrderEvent] on the
/// timeline, and is announced on the live channel to the shopper and the
/// store's staff once it is committed.
///
/// The fulfilment slice calls [#recordFulfilled] after it ships units (it
/// moves the stock itself through [Inventory#ship]); that also takes the
/// card payment the first time anything ships ([#capturePayment]).
///
/// The processor is never called with a transaction open, as in [Payments].
@Component
public class Orders {
    public static final String ONLINE = "ONLINE";
    public static final String POS = "POS";
    public static final String DRAFT = "DRAFT";
    public static final String OPEN = "OPEN";
    public static final String CLOSED = "CLOSED";
    public static final String CANCELLED = "CANCELLED";
    public static final String PENDING = "PENDING";
    public static final String AUTHORIZED = "AUTHORIZED";
    public static final String PAID = "PAID";
    public static final String PARTIALLY_REFUNDED = "PARTIALLY_REFUNDED";
    public static final String REFUNDED = "REFUNDED";
    public static final String VOIDED = "VOIDED";
    public static final String UNFULFILLED = "UNFULFILLED";
    public static final String PARTIAL = "PARTIAL";
    public static final String FULFILLED = "FULFILLED";
    /// The live channel's type for an order.
    public static final String LIVE_TYPE = "order";
    private static final int NUMBER_ATTEMPTS = 8;

    private final StoreOrderRepository orders;
    private final OrderLineRepository lines;
    private final OrderEventRepository events;
    private final TenderRepository tenders;
    private final FulfillmentRepository fulfillments;
    private final RefundRepository refunds;
    private final RefundLineRepository refundLines;
    private final PaymentRepository paymentRows;
    private final StoreRepository stores;
    private final StoreStaffRepository staff;
    private final Payments payments;
    private final Inventory inventory;
    private final GiftCards giftCards;
    private final StoreAccess access;
    private final LiveHub live;
    private final Quoter quoter;
    private final StoreEvents storeEvents;
    private final CustomerRepository customers;
    private final SaleEvents saleEvents;

    @com.codename1.backend.annotations.Autowired
    private com.codename1.backend.DataSource db;
    @com.codename1.backend.annotations.Autowired
    private OrderSettings orderSettings;

    private final com.codenameone.examples.bazaar.checkout.CheckoutSettings checkoutSettings;

    public Orders(StoreOrderRepository orders, OrderLineRepository lines,
            OrderEventRepository events, TenderRepository tenders,
            FulfillmentRepository fulfillments, RefundRepository refunds,
            RefundLineRepository refundLines, PaymentRepository paymentRows,
            StoreRepository stores, StoreStaffRepository staff, Payments payments,
            Inventory inventory, GiftCards giftCards, StoreAccess access, LiveHub live,
            Quoter quoter, StoreEvents storeEvents, CustomerRepository customers, SaleEvents saleEvents,
            com.codenameone.examples.bazaar.checkout.CheckoutSettings checkoutSettings) {
        this.saleEvents = saleEvents;
        this.checkoutSettings = checkoutSettings;
        this.orders = orders;
        this.lines = lines;
        this.events = events;
        this.tenders = tenders;
        this.fulfillments = fulfillments;
        this.refunds = refunds;
        this.refundLines = refundLines;
        this.paymentRows = paymentRows;
        this.stores = stores;
        this.staff = staff;
        this.payments = payments;
        this.inventory = inventory;
        this.giftCards = giftCards;
        this.access = access;
        this.live = live;
        this.quoter = quoter;
        this.storeEvents = storeEvents;
        this.customers = customers;
    }

    // -------------------------------------------------------------- shopper

    @Transactional(readOnly = true)
    public List<OrderDto> mine(String username) throws IOException {
        List<StoreOrder> rows = orders.findByUsernameOrderByCreatedAtDesc(username);
        List<OrderDto> out = new ArrayList<OrderDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            if (!isUnconvertedDraft(rows.get(iter))) {
                out.add(dto(rows.get(iter), false));
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public OrderDto mineDetail(String username, String orderId) throws IOException {
        return dto(own(username, orderId), true);
    }

    /// Cancels the shopper's own order while nothing of it has shipped.
    public OrderDto cancelMine(String username, String orderId) throws IOException {
        StoreOrder order = own(username, orderId);
        cancelOrder(order, username);
        return detailOf(orderId);
    }

    private StoreOrder own(String username, String orderId) throws IOException {
        StoreOrder order = orderId == null ? null : orders.findById(orderId);
        if (order == null || !username.equals(order.getUsername()) || isUnconvertedDraft(order)) {
            throw new ResponseStatusException(404, "No such order");
        }
        return order;
    }

    // ------------------------------------------------------------- merchant

    /// A store's orders, newest first, filtered; see `OrderApi.list`.
    @Transactional(readOnly = true)
    public List<OrderDto> list(String username, String storeId, String status, String financial,
            String fulfilment, String channel, String from, String to, String q)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        long fromMs = millis(from);
        long toMs = millis(to);
        String query = q == null ? "" : q.trim();
        List<StoreOrder> rows = orders.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<OrderDto> out = new ArrayList<OrderDto>();
        for (int iter = 0; iter < rows.size() && out.size() < 500; iter++) {
            StoreOrder o = rows.get(iter);
            if (o.isArchived()) {
                // Out of the working list; [#archived] lists these.
                continue;
            }
            long at = o.getCreatedAt() == null ? 0 : o.getCreatedAt().toEpochMilli();
            if (!matches(status, o.getStatus()) || !matches(financial, o.getFinancial())
                    || !matches(fulfilment, o.getFulfilment()) || !matches(channel, o.getChannel())
                    || (fromMs > 0 && at < fromMs) || (toMs > 0 && at >= toMs)) {
                continue;
            }
            if (query.length() > 0 && !contains(String.valueOf(o.getNumber()), query)
                    && !contains(o.getEmail(), query) && !contains(o.getShipName(), query)
                    && !contains(o.getUsername(), query)) {
                continue;
            }
            out.add(dto(o, false));
        }
        return out;
    }

    /// The store's archived orders, newest first.
    @Transactional(readOnly = true)
    public List<OrderDto> archived(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<StoreOrder> rows = orders.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<OrderDto> out = new ArrayList<OrderDto>();
        for (int iter = 0; iter < rows.size() && out.size() < 500; iter++) {
            if (rows.get(iter).isArchived()) {
                out.add(dto(rows.get(iter), false));
            }
        }
        return out;
    }

    /// Archives or unarchives an order. Archiving changes nothing about the
    /// order but where it is listed; unarchiving by hand also keeps
    /// auto-archiving from taking it again.
    public OrderDto archive(String username, String storeId, String orderId, boolean archived)
            throws IOException {
        access.requireArea(username, storeId, Perms.ORDERS, true, StoreAccess.ANYONE);
        StoreOrder order = inStoreRead(storeId, orderId);
        if (order.isArchived() != archived) {
            archiveTx(orderId, archived, username, archived ? "Archived" : "Unarchived");
            publish(orders.findById(orderId));
        }
        return detailOf(orderId);
    }

    @Transactional(rollbackFor = IOException.class)
    private void archiveTx(String orderId, boolean archived, String username, String message)
            throws IOException {
        db.execute("UPDATE store_order SET archived = ? WHERE id = ?",
                new Object[] {Integer.valueOf(archived ? 1 : 0), orderId});
        event(orderId, archived ? "ARCHIVED" : "UNARCHIVED", message, username);
    }

    /// Archives an order that has just closed -- the register's, or one
    /// fulfilled and paid (or refunded) -- when its store archives closed
    /// orders, unless someone unarchived it by hand.
    private void autoArchive(StoreOrder order) throws IOException {
        if (order.isArchived() || !closed(order)
                || !orderSettings.of(order.getStoreId()).autoArchive()) {
            return;
        }
        if (db.queryOne("SELECT id FROM order_event WHERE order_id = ? AND kind = ?",
                new Object[] {order.getId(), "UNARCHIVED"}) != null) {
            return;
        }
        archiveTx(order.getId(), true, "", "Archived when it closed");
        order.setArchived(true);
    }

    /// Whether nothing is left to do on an order.
    public static boolean closed(StoreOrder o) {
        if (CLOSED.equals(o.getStatus())) {
            return true;
        }
        String money = o.getFinancial();
        return OPEN.equals(o.getStatus()) && FULFILLED.equals(o.getFulfilment())
                && (PAID.equals(money) || PARTIALLY_REFUNDED.equals(money)
                    || REFUNDED.equals(money));
    }

    private static boolean matches(String wanted, String value) {
        return wanted == null || wanted.length() == 0 || wanted.equals(value);
    }

    /// Whether `text` holds `part`, ignoring ASCII case.
    private static boolean contains(String text, String part) {
        if (text == null) {
            return false;
        }
        int max = text.length() - part.length();
        for (int iter = 0; iter <= max; iter++) {
            if (text.regionMatches(true, iter, part, 0, part.length())) {
                return true;
            }
        }
        return false;
    }

    private static long millis(String text) {
        if (text == null || text.length() == 0) {
            return 0;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException bad) {
            throw new ResponseStatusException(400, "A date is epoch milliseconds");
        }
    }

    @Transactional(readOnly = true)
    public OrderDto detail(String username, String storeId, String orderId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return dto(inStore(storeId, orderId), true);
    }

    public OrderDto note(String username, String storeId, String orderId, OrderNoteDto note)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGER, StoreAccess.OWNER,
                StoreAccess.FULFILMENT);
        StoreOrder order = writeNote(storeId, orderId, note, username);
        publish(order);
        return detailOf(orderId);
    }

    @Transactional(rollbackFor = IOException.class)
    private StoreOrder writeNote(String storeId, String orderId, OrderNoteDto note,
            String username) throws IOException {
        StoreOrder order = inStore(storeId, orderId);
        if (note != null && note.note() != null) {
            order.setNote(Codes.cut(note.note(), 1000));
        }
        if (note != null && note.tags() != null) {
            order.setTags(Codes.cut(note.tags(), 500));
        }
        order.setUpdatedAt(Instant.now());
        orders.save(order);
        event(orderId, "NOTE", "Note or tags changed", username);
        return order;
    }

    public OrderDto cancel(String username, String storeId, String orderId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        cancelOrder(inStoreRead(storeId, orderId), username);
        return detailOf(orderId);
    }

    /// Records a payment taken outside the app (cash, a bank transfer) for an
    /// order still PENDING.
    public OrderDto markPaid(String username, String storeId, String orderId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        StoreOrder order = inStoreRead(storeId, orderId);
        if (CANCELLED.equals(order.getStatus())) {
            throw new ResponseStatusException(409, "The order was cancelled");
        }
        Payment payment = paymentRows.findById(orderId);
        if (payment != null && Payments.AUTHORIZED.equals(payment.getStatus())) {
            // A cash "hold" from checkout, or a card one being settled by hand.
            payments.capture(orderId);
        }
        StoreOrder done = recordPaid(orderId, username);
        publish(done);
        storeEvents.orderPaid(orderId);
        return detailOf(orderId);
    }

    @Transactional(rollbackFor = IOException.class)
    private StoreOrder recordPaid(String orderId, String username) throws IOException {
        StoreOrder order = orders.findById(orderId);
        String from = order.getFinancial();
        if (!PENDING.equals(from) && !AUTHORIZED.equals(from)) {
            throw new ResponseStatusException(409, "The order is not awaiting payment");
        }
        if (orders.claimFinancial(orderId, from, PAID) != 1) {
            throw new ResponseStatusException(409, "The order is changing; try again");
        }
        if (PENDING.equals(from)) {
            long paid = order.getTotalCents() - giftCards.spentOn(orderId);
            tender(orderId, "CASH", paid, "manual");
        }
        event(orderId, "PAID", "Marked as paid", username);
        return orders.findById(orderId);
    }

    /// Records that the processor settled an order's payment-pending payment
    /// (`payment` is [Payments#PAID], or [Payments#AUTHORIZED] for a hold):
    /// the order moves from [#PENDING] and gets its tender. Safe to repeat;
    /// answers false when the order was not waiting.
    public boolean settledOnline(String orderId, String payment, String label)
            throws IOException {
        StoreOrder done = recordSettled(orderId, payment, label);
        if (done == null) {
            return false;
        }
        publish(done);
        if (PAID.equals(done.getFinancial())) {
            storeEvents.orderPaid(orderId);
        }
        return true;
    }

    @Transactional(rollbackFor = IOException.class)
    private StoreOrder recordSettled(String orderId, String payment, String label)
            throws IOException {
        StoreOrder order = orders.findById(orderId);
        String to = Payments.AUTHORIZED.equals(payment) ? AUTHORIZED : PAID;
        if (order == null || !PENDING.equals(order.getFinancial())
                || CANCELLED.equals(order.getStatus())
                || orders.claimFinancial(orderId, PENDING, to) != 1) {
            return null;
        }
        long paid = order.getTotalCents() + order.getTipCents() - giftCards.spentOn(orderId);
        Payment row = paymentRows.findById(orderId);
        tender(orderId, "CARD", paid, row == null ? "" : row.getProviderRef());
        event(orderId, PAID.equals(to) ? "PAID" : "AUTHORIZED", (PAID.equals(to)
                ? "Paid with " : "Payment held by ") + label, "processor");
        return orders.findById(orderId);
    }

    // --------------------------------------------------------- transitions

    /// Cancels an order nothing of which has shipped: its stock is released,
    /// a card hold is let go (or a captured payment refunded) and gift cards
    /// get their money back.
    public void cancelOrder(StoreOrder order, String actor) throws IOException {
        claimCancel(order.getId(), actor);
        Payment payment = paymentRows.findById(order.getId());
        boolean refunded = false;
        if (payment != null) {
            String now = payments.voidOrder(order.getId());
            if (Payments.PAID.equals(now) || Payments.PARTIALLY_REFUNDED.equals(now)) {
                payments.refund(order.getId(), 0, "Order cancelled");
                refunded = true;
            }
        }
        String card = giftCards.cardOf(order.getId());
        long onCard = giftCards.spentOn(order.getId());
        if (card != null && onCard > 0) {
            giftCards.credit(card, order.getId(), onCard, "Order cancelled");
        }
        StoreOrder done = finishCancel(order.getId(), refunded, actor);
        publish(done);
        storeEvents.orderCancelled(order.getId());
    }

    @Transactional(rollbackFor = IOException.class)
    private void claimCancel(String orderId, String actor) throws IOException {
        StoreOrder order = orders.findById(orderId);
        if (!OPEN.equals(order.getStatus())) {
            throw new ResponseStatusException(409, "The order is not open");
        }
        List<OrderLine> rows = lines.findByOrderId(orderId);
        for (int iter = 0; iter < rows.size(); iter++) {
            if (rows.get(iter).getFulfilledQty() > 0) {
                throw new ResponseStatusException(409, "Part of the order has shipped");
            }
        }
        if (orders.claimStatus(orderId, OPEN, CANCELLED) != 1) {
            throw new ResponseStatusException(409, "The order is changing; try again");
        }
        for (int iter = 0; iter < rows.size(); iter++) {
            OrderLine line = rows.get(iter);
            int held = line.getQuantity() - line.getRefundedQty();
            if (line.getLocationId().length() > 0 && held > 0) {
                inventory.release(order.getStoreId(), line.getVariantId(), line.getLocationId(),
                        held, orderId, actor);
            }
        }
    }

    @Transactional(rollbackFor = IOException.class)
    private StoreOrder finishCancel(String orderId, boolean refunded, String actor)
            throws IOException {
        StoreOrder order = orders.findById(orderId);
        // Money that was taken (a captured card, a gift card, a payment by
        // hand) came back: REFUNDED. Money only held was let go: VOIDED.
        boolean taken = refunded || PAID.equals(order.getFinancial())
                || PARTIALLY_REFUNDED.equals(order.getFinancial());
        order.setFinancial(taken ? REFUNDED : VOIDED);
        if (taken) {
            order.setRefundedCents(order.getTotalCents());
        }
        order.setClosedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        orders.save(order);
        // A cancelled order no longer counts toward the customer's orders and
        // spend, so segments such as VIP see what the customer really bought.
        Customer customer = order.getCustomerId().length() == 0 ? null
                : customers.findById(order.getCustomerId());
        if (customer != null) {
            customer.setOrdersCount(Math.max(0, customer.getOrdersCount() - 1));
            customer.setSpentCents(Math.max(0L, customer.getSpentCents() - order.getTotalCents()));
            customer.setUpdatedAt(Instant.now());
            customers.save(customer);
        }
        event(orderId, "CANCELLED", "Order cancelled", actor);
        return order;
    }

    /// Takes a held card payment: called when the first units ship, or for
    /// pickup when the goods are handed over. Answers the order's financial
    /// status after.
    public String capturePayment(String orderId, String actor) throws IOException {
        StoreOrder order = orders.findById(orderId);
        if (order == null) {
            throw new ResponseStatusException(404, "No such order");
        }
        if (!AUTHORIZED.equals(order.getFinancial())) {
            return order.getFinancial();
        }
        String result = payments.capture(orderId);
        if (Payments.PAID.equals(result)) {
            capturedTx(orderId, actor);
            publish(orders.findById(orderId));
            storeEvents.orderPaid(orderId);
            return PAID;
        }
        if (Payments.FAILED.equals(result)) {
            event(orderId, "PAYMENT_FAILED", "The card could not be charged", actor);
        }
        return orders.findById(orderId).getFinancial();
    }

    @Transactional(rollbackFor = IOException.class)
    private void capturedTx(String orderId, String actor) throws IOException {
        if (orders.claimFinancial(orderId, AUTHORIZED, PAID) == 1) {
            event(orderId, "CAPTURED", "Payment taken", actor);
        }
    }

    /// Records units shipped (or handed over): `shipped` maps order line ids
    /// to how many more left. Updates each line and the order's fulfilment
    /// status, then takes the payment if it was only held. The stock itself
    /// is the caller's to move ([Inventory#ship]).
    public StoreOrder recordFulfilled(String orderId, Map<String, Integer> shipped, String actor)
            throws IOException {
        StoreOrder before = orders.findById(orderId);
        boolean wasFulfilled = before != null && FULFILLED.equals(before.getFulfilment());
        fulfilledTx(orderId, shipped, actor);
        capturePayment(orderId, actor);
        StoreOrder order = orders.findById(orderId);
        publish(order);
        if (!wasFulfilled && order != null && FULFILLED.equals(order.getFulfilment())) {
            storeEvents.orderFulfilled(orderId);
        }
        return order;
    }

    @Transactional(rollbackFor = IOException.class)
    private void fulfilledTx(String orderId, Map<String, Integer> shipped, String actor)
            throws IOException {
        StoreOrder order = orders.findById(orderId);
        if (order == null || CANCELLED.equals(order.getStatus())) {
            throw new ResponseStatusException(409, "The order cannot be fulfilled");
        }
        List<OrderLine> rows = lines.findByOrderId(orderId);
        boolean any = false;
        boolean all = true;
        for (int iter = 0; iter < rows.size(); iter++) {
            OrderLine line = rows.get(iter);
            Integer more = shipped.get(line.getId());
            if (more != null && more.intValue() > 0) {
                int open = line.getQuantity() - line.getRefundedQty() - line.getFulfilledQty();
                if (more.intValue() > open) {
                    throw new ResponseStatusException(409, "More of " + line.getTitle()
                            + " than is left to ship");
                }
                line.setFulfilledQty(line.getFulfilledQty() + more.intValue());
                lines.save(line);
            }
            any |= line.getFulfilledQty() > 0;
            all &= line.getFulfilledQty() >= line.getQuantity() - line.getRefundedQty();
        }
        order.setFulfilment(all ? FULFILLED : any ? PARTIAL : UNFULFILLED);
        order.setUpdatedAt(Instant.now());
        orders.save(order);
        event(orderId, "FULFILLED", all ? "Everything shipped" : "Part of the order shipped", actor);
    }

    // --------------------------------------------------------------- refunds

    /// Refunds units of lines and part of the shipping. Each unit is refunded
    /// at its share of the line after discounts (plus its tax when the tax
    /// was added on top), rounded half to even. The card takes the refund
    /// first, gift cards the rest. A held payment is captured first, since
    /// only taken money can be given back. Units not yet shipped are always
    /// released; shipped ones go back on hand when `restock` is set.
    public OrderDto refund(String username, String storeId, String orderId, RefundRequestDto req)
            throws IOException {
        refundParts(username, storeId, orderId, req, false, StoreAccess.MANAGERS);
        return detailOf(orderId);
    }

    /// [#refund] for a caller holding one of `roles`, answering what went
    /// where: `{total, card, gift card, by hand}`. With `byHand` nothing goes
    /// back to the card or a gift card: a counter return paid out of the
    /// drawer. Otherwise what neither takes (cash, store credit) is by hand.
    public long[] refundParts(String username, String storeId, String orderId,
            RefundRequestDto req, boolean byHand, String... roles) throws IOException {
        return refundParts(username, storeId, orderId, req, byHand, 0, roles);
    }

    /// [#refundParts] keeping `keepCents` of the returned goods' value: a
    /// restocking fee, a return shipping fee, or what an exchange moves onto
    /// the replacement order. Only the rest goes back (and is recorded as the
    /// refund); the lines still count as returned. The answer's first element
    /// is what went back.
    public long[] refundParts(String username, String storeId, String orderId,
            RefundRequestDto req, boolean byHand, long keepCents, String... roles)
            throws IOException {
        access.requireStaff(username, storeId, roles);
        StoreOrder order = inStoreRead(storeId, orderId);
        if (req == null) {
            throw new ResponseStatusException(400, "Say what to refund");
        }
        if (CANCELLED.equals(order.getStatus())) {
            throw new ResponseStatusException(409, "The order was cancelled");
        }
        if (AUTHORIZED.equals(order.getFinancial())) {
            capturePayment(orderId, username);
            order = orders.findById(orderId);
        }
        if (!PAID.equals(order.getFinancial()) && !PARTIALLY_REFUNDED.equals(order.getFinancial())) {
            throw new ResponseStatusException(409, "Nothing has been paid to refund");
        }
        Store store = stores.findById(storeId);
        long amount = Math.max(0, refundAmount(order, req, store.isTaxInclusive())
                - Math.max(0, keepCents));
        Payment payment = paymentRows.findById(orderId);
        long cardPart = 0;
        long presentmentPart = 0;
        boolean foreign = foreign(order);
        if (payment != null && !byHand) {
            long cardLeft = payment.getAmountCents() - payment.getRefundedCents();
            if (foreign) {
                // The card was charged in the shopper's currency: what is left
                // on it is worth this much of the store's, at the order's rate.
                long settlementLeft = scale(cardLeft, order.getTotalCents(),
                        order.getPresentmentTotalCents());
                cardPart = Math.min(amount, settlementLeft);
                // The refund that empties the card takes exactly what is left,
                // so rounding never strands a cent on it.
                presentmentPart = cardPart >= settlementLeft ? cardLeft
                        : presentmentRefund(order, cardPart, cardLeft);
            } else {
                cardPart = Math.min(amount, cardLeft);
                presentmentPart = cardPart;
            }
        } else if (foreign && payment == null && !byHand) {
            // A marketplace order imported from another currency: the
            // marketplace holds the money and refunds it in the buyer's
            // currency, so the refund records that amount at the order's
            // locked rate, never more than the order's presentment total.
            presentmentPart = presentmentRefund(order, amount, order.getPresentmentTotalCents());
        }
        long giftPart = amount - cardPart;
        String card = giftCards.cardOf(orderId);
        if (byHand || giftPart > 0 && (card == null || giftPart > giftCards.spentOn(orderId))) {
            // Paid by hand (mark paid): the money goes back the same way.
            giftPart = 0;
        }
        if (cardPart > 0 && presentmentPart > 0) {
            payments.refund(orderId, presentmentPart, Codes.cut(req.reason(), 200));
        }
        if (giftPart > 0) {
            giftCards.credit(card, orderId, giftPart, "Refund");
        }
        StoreOrder done = recordRefund(orderId, req, amount, username,
                foreign ? presentmentPart : 0);
        publish(done);
        storeEvents.refunded(orderId, amount);
        return new long[] {amount, cardPart, giftPart, amount - cardPart - giftPart};
    }

    /// Whether the order was charged in a currency other than the store's.
    public static boolean foreign(StoreOrder o) {
        String p = o.getPresentmentCurrency();
        return p != null && p.length() > 0 && !p.equals(o.getCurrency())
                && o.getPresentmentTotalCents() > 0;
    }

    /// The part of the order's card charge, in its presentment currency, that
    /// gives back `settlementCents` of the store's: the same share of the
    /// presentment total as of the store total, half up, never more than
    /// `cardLeft`. For an order in the store's currency it is `settlementCents`.
    public static long presentmentRefund(StoreOrder o, long settlementCents, long cardLeft) {
        if (!foreign(o)) {
            return Math.min(settlementCents, cardLeft);
        }
        long p = scale(settlementCents, o.getPresentmentTotalCents(), o.getTotalCents());
        return Math.max(0, Math.min(p, cardLeft));
    }

    /// `amount * to / from`, half up; 0 when `from` is 0 or less. Overflow is a 400.
    static long scale(long amount, long to, long from) {
        if (from <= 0) {
            return 0;
        }
        try {
            return Math.addExact(Math.multiplyExact(amount, to), from / 2) / from;
        } catch (ArithmeticException overflow) {
            throw new ResponseStatusException(400, "That amount is too large");
        }
    }

    /// What a refund request is worth; 400 for one that asks for more than
    /// is left.
    /// What refunding `req` would come to, without refunding anything: the
    /// same arithmetic and checks as [#refund].
    @Transactional(readOnly = true)
    public long quoteRefund(String orderId, RefundRequestDto req) throws IOException {
        StoreOrder order = orders.findById(orderId);
        return refundAmount(order, req, stores.findById(order.getStoreId()).isTaxInclusive());
    }

    private long refundAmount(StoreOrder order, RefundRequestDto req, boolean inclusive)
            throws IOException {
        long amount = 0;
        List<OrderLine> rows = lines.findByOrderId(order.getId());
        List<RefundLineDto> wanted = req.lines() == null ? new ArrayList<RefundLineDto>() : req.lines();
        for (int iter = 0; iter < wanted.size(); iter++) {
            RefundLineDto w = wanted.get(iter);
            OrderLine line = find(rows, w.orderLineId());
            if (line == null || w.quantity() < 0
                    || w.quantity() > line.getQuantity() - line.getRefundedQty()) {
                throw new ResponseStatusException(400, "That is more than is left to refund of a line");
            }
            amount += lineRefund(line, w.quantity(), inclusive);
        }
        long shippingLeft = order.getShippingCents() - shippingRefunded(order.getId());
        if (req.shippingCents() < 0 || req.shippingCents() > shippingLeft) {
            throw new ResponseStatusException(400, "That is more shipping than is left to refund");
        }
        amount += req.shippingCents();
        long left = order.getTotalCents() - order.getRefundedCents();
        if (amount <= 0) {
            throw new ResponseStatusException(400, "Nothing to refund");
        }
        return Math.min(amount, left);
    }

    /// `quantity` units' share of a line: its price after discounts and, when
    /// the tax was added on top, its tax.
    public static long lineRefund(OrderLine line, int quantity, boolean inclusive) {
        if (quantity <= 0 || line.getQuantity() <= 0) {
            return 0;
        }
        long net = line.getPriceCents() * line.getQuantity() - line.getDiscountCents();
        long out = Money.divideHalfEven(net * quantity, line.getQuantity());
        if (!inclusive) {
            out += Money.divideHalfEven(line.getTaxCents() * quantity, line.getQuantity());
        }
        return out;
    }

    private long shippingRefunded(String orderId) throws IOException {
        List<Refund> rows = refunds.findByOrderId(orderId);
        long sum = 0;
        for (int iter = 0; iter < rows.size(); iter++) {
            sum += rows.get(iter).getShippingCents();
        }
        return sum;
    }

    private static OrderLine find(List<OrderLine> rows, String id) {
        for (int iter = 0; iter < rows.size(); iter++) {
            if (rows.get(iter).getId().equals(id)) {
                return rows.get(iter);
            }
        }
        return null;
    }

    @Transactional(rollbackFor = IOException.class)
    private StoreOrder recordRefund(String orderId, RefundRequestDto req, long amount,
            String username, long presentmentCents) throws IOException {
        StoreOrder order = orders.findById(orderId);
        Refund refund = new Refund();
        refund.setId(Ids.next());
        refund.setOrderId(orderId);
        refund.setAmountCents(amount);
        refund.setPresentmentCents(presentmentCents);
        refund.setShippingCents(req.shippingCents());
        refund.setReason(Codes.cut(req.reason(), 500));
        refund.setRestock(req.restock());
        refund.setUsername(username);
        refund.setCreatedAt(Instant.now());
        refunds.insert(refund);
        List<OrderLine> rows = lines.findByOrderId(orderId);
        List<RefundLineDto> wanted = req.lines() == null ? new ArrayList<RefundLineDto>() : req.lines();
        for (int iter = 0; iter < wanted.size(); iter++) {
            RefundLineDto w = wanted.get(iter);
            OrderLine line = find(rows, w.orderLineId());
            if (line == null || w.quantity() <= 0) {
                continue;
            }
            RefundLine row = new RefundLine();
            row.setId(Ids.next());
            row.setRefundId(refund.getId());
            row.setOrderLineId(line.getId());
            row.setQuantity(w.quantity());
            refundLines.insert(row);
            int unshipped = Math.max(0, line.getQuantity() - line.getFulfilledQty()
                    - line.getRefundedQty());
            int release = Math.min(unshipped, w.quantity());
            int back = w.quantity() - release;
            if (release > 0 && line.getLocationId().length() > 0) {
                inventory.release(order.getStoreId(), line.getVariantId(), line.getLocationId(),
                        release, refund.getId(), username);
            }
            if (back > 0 && req.restock() && line.getLocationId().length() > 0) {
                inventory.restock(order.getStoreId(), line.getVariantId(), line.getLocationId(),
                        back, refund.getId(), username, Inventory.RESTOCK);
            }
            line.setRefundedQty(line.getRefundedQty() + w.quantity());
            lines.save(line);
        }
        order = orders.findById(orderId);
        order.setRefundedCents(order.getRefundedCents() + amount);
        order.setFinancial(order.getRefundedCents() >= order.getTotalCents() ? REFUNDED
                : PARTIALLY_REFUNDED);
        order.setUpdatedAt(Instant.now());
        orders.save(order);
        event(orderId, "REFUNDED", "Refunded " + amount + " cents", username);
        return order;
    }

    @Transactional(readOnly = true)
    public List<RefundDto> refunds(String username, String storeId, String orderId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        inStore(storeId, orderId);
        List<Refund> rows = refunds.findByOrderId(orderId);
        List<RefundDto> out = new ArrayList<RefundDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Refund r = rows.get(iter);
            out.add(new RefundDto(r.getId(), r.getOrderId(), r.getAmountCents(),
                    r.getShippingCents(), r.isRestock(), r.getReason(),
                    r.getCreatedAt() == null ? 0 : r.getCreatedAt().toEpochMilli()));
        }
        return out;
    }

    // ---------------------------------------------------------------- drafts

    /// Writes a DRAFT order priced like a cart. Nothing is reserved until
    /// [#convertDraft]; until then its order-level `locationId` is empty.
    public OrderDto createDraft(String username, String storeId, DraftOrderDto draft)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (draft == null || draft.lines() == null || draft.lines().isEmpty()) {
            throw new ResponseStatusException(400, "A draft needs at least one line");
        }
        Cart scratch = new Cart();
        scratch.setId("");
        scratch.setStoreId(storeId);
        List<Quoter.Want> wants = new ArrayList<Quoter.Want>();
        for (int iter = 0; iter < draft.lines().size(); iter++) {
            CartLineChangeDto l = draft.lines().get(iter);
            if (l == null || l.quantity() <= 0) {
                throw new ResponseStatusException(400, "Every line needs a quantity");
            }
            wants.add(new Quoter.Want(Integer.toString(iter), l.variantId(), l.quantity()));
        }
        Quote quote = quoter.price(scratch, wants, null, null, draft.address(),
                draft.shippingRateId(), null);
        if (quote.lines.size() != wants.size()) {
            throw new ResponseStatusException(400, "An item in the draft is not sold");
        }
        final StoreOrder order = newOrder(quote, DRAFT, "", draft.email(), draft.address(),
                Ids.next());
        order.setNote(Codes.cut(draft.note(), 1000));
        StoreOrder saved = insertNumbered(order, new Writer() {
            @Override
            public void write(StoreOrder o) throws IOException {
                insertLines(o, quote, null);
                event(o.getId(), "DRAFT_CREATED", "Draft written", username);
            }
        });
        return detailOf(saved.getId());
    }

    public OrderDto sendInvoice(String username, String storeId, String orderId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        StoreOrder order = inStoreRead(storeId, orderId);
        if (!isUnconvertedDraft(order)) {
            throw new ResponseStatusException(409, "That is not a draft");
        }
        if (order.getEmail().length() == 0) {
            throw new ResponseStatusException(400, "The draft has no e-mail to send to");
        }
        event(orderId, "INVOICE_SENT", "Invoice sent to " + order.getEmail(), username);
        return detailOf(orderId);
    }

    /// Turns a draft into an open order awaiting payment: every line's stock
    /// is reserved, all or nothing, as at checkout.
    public OrderDto convertDraft(String username, String storeId, String orderId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        convert(inStoreRead(storeId, orderId), username);
        return detailOf(orderId);
    }

    /// [#convertDraft] with no access check, for a draft its buyer pays
    /// through a checkout link and for an exchange's replacement order.
    public void convert(StoreOrder order, String username) throws IOException {
        String storeId = order.getStoreId();
        String orderId = order.getId();
        if (!isUnconvertedDraft(order) || !OPEN.equals(order.getStatus())) {
            throw new ResponseStatusException(409, "That is not a draft");
        }
        List<OrderLine> rows = lines.findByOrderId(orderId);
        List<String[]> held = new ArrayList<String[]>();
        for (int iter = 0; iter < rows.size(); iter++) {
            OrderLine line = rows.get(iter);
            String at = inventory.pickLocation(storeId, line.getVariantId(), line.getQuantity());
            int over = at == null ? -1 : inventory.reserveCounted(storeId, line.getVariantId(),
                    at, line.getQuantity(), orderId, username);
            if (over < 0) {
                releaseAll(storeId, held, orderId, username);
                throw new ResponseStatusException(409, "Not enough " + line.getTitle()
                        + " in stock");
            }
            held.add(new String[] {line.getId(), line.getVariantId(), at,
                Integer.toString(line.getQuantity()), Integer.toString(over)});
        }
        StoreOrder done = convertedTx(orderId, held, username);
        publish(done);
        storeEvents.orderPlaced(orderId);
    }

    /// Pays an order awaiting payment with a card typed by its buyer, through
    /// a checkout link ([com.codenameone.examples.bazaar.order.PayLinks]). A
    /// draft is turned into an order first. What is due is the total less
    /// gift cards and earlier part payments (an exchange's credit).
    public OrderDto payOnline(String orderId, String payer, Payments.OneOffCard card)
            throws IOException {
        StoreOrder order = orders.findById(orderId);
        if (order == null) {
            throw new ResponseStatusException(404, "No such order");
        }
        if (isUnconvertedDraft(order)) {
            convert(order, payer);
            order = orders.findById(orderId);
        }
        if (!OPEN.equals(order.getStatus()) || !PENDING.equals(order.getFinancial())) {
            throw new ResponseStatusException(409, "The order is not awaiting payment");
        }
        long due = order.getTotalCents() - giftCards.spentOn(orderId) - tendered(orderId);
        String reference = "";
        if (due > 0) {
            reference = payments.authorizeCard(orderId, payer, card, due, order.getCurrency());
            payments.capture(orderId);
        }
        StoreOrder done = settleTx(orderId, "CARD", due, reference, payer,
                "Paid through a checkout link");
        publish(done);
        storeEvents.orderPaid(orderId);
        return detailOf(orderId);
    }

    /// Records a part payment that is not money taken now -- an exchange's
    /// credit moved onto its replacement order -- and marks the order paid
    /// once nothing is left due.
    public void partPay(String orderId, String method, long cents, String reference,
            String actor) throws IOException {
        StoreOrder order = orders.findById(orderId);
        long left = order.getTotalCents() - giftCards.spentOn(orderId) - tendered(orderId);
        long paid = Math.min(Math.max(0, cents), Math.max(0, left));
        if (paid >= left) {
            StoreOrder done = settleTx(orderId, method, paid, reference, actor,
                    "Paid by " + method);
            publish(done);
            storeEvents.orderPaid(orderId);
        } else if (paid > 0) {
            tender(orderId, method, paid, reference);
            event(orderId, "PART_PAID", "Part paid by " + method, actor);
        }
    }

    /// What the order's tenders already hold, gift cards and cards aside.
    private long tendered(String orderId) throws IOException {
        List<com.codenameone.examples.bazaar.domain.Tender> ts = tenders.findByOrderId(orderId);
        long out = 0;
        for (int iter = 0; iter < ts.size(); iter++) {
            if ("EXCHANGE".equals(ts.get(iter).getKind())) {
                out += ts.get(iter).getAmountCents();
            }
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    private StoreOrder settleTx(String orderId, String method, long cents, String reference,
            String actor, String message) throws IOException {
        if (orders.claimFinancial(orderId, PENDING, PAID) != 1) {
            throw new ResponseStatusException(409, "The order is not awaiting payment");
        }
        if (cents > 0) {
            tender(orderId, method, cents, reference);
        }
        event(orderId, "PAID", message, actor);
        return orders.findById(orderId);
    }

    @Transactional(rollbackFor = IOException.class)
    private StoreOrder convertedTx(String orderId, List<String[]> held, String username)
            throws IOException {
        StoreOrder order = orders.findById(orderId);
        for (int iter = 0; iter < held.size(); iter++) {
            OrderLine line = lines.findById(held.get(iter)[0]);
            line.setLocationId(held.get(iter)[2]);
            if (held.get(iter).length > 4) {
                line.setBackorderedQty(Integer.parseInt(held.get(iter)[4]));
            }
            lines.save(line);
        }
        order.setLocationId(held.isEmpty() ? "-" : held.get(0)[2]);
        order.setUpdatedAt(Instant.now());
        orders.save(order);
        event(orderId, "DRAFT_CONVERTED", "Draft turned into an order", username);
        return order;
    }

    /// Releases reservations made for an order: `{lineKey, variantId,
    /// locationId, qty}` each.
    public void releaseAll(String storeId, List<String[]> held, String reference, String actor)
            throws IOException {
        for (int iter = 0; iter < held.size(); iter++) {
            String[] h = held.get(iter);
            inventory.release(storeId, h[1], h[2], Integer.parseInt(h[3]), reference, actor);
        }
    }

    /// A draft not yet turned into an order: its order-level location is
    /// empty until [#convertDraft] reserves stock.
    public static boolean isUnconvertedDraft(StoreOrder order) {
        return DRAFT.equals(order.getChannel()) && order.getLocationId().length() == 0;
    }

    // ------------------------------------------------------- building orders

    /// Writes the rest of an order inside the transaction that inserts it.
    public interface Writer {
        void write(StoreOrder order) throws IOException;
    }

    /// Inserts `order` with the store's next number and lets `rest` write the
    /// lines and anything else in the same transaction. Two orders taking a
    /// number at once collide on the unique key; the loser's transaction
    /// rolls back whole (`rest` included) and it tries the next number.
    /// Answers null when the failure was the order's `clientId` already
    /// being taken: the same order was placed meanwhile.
    public StoreOrder insertNumbered(StoreOrder order, Writer rest) throws IOException {
        RuntimeException lastRuntime = null;
        IOException lastIo = null;
        for (int attempt = 0; attempt < NUMBER_ATTEMPTS; attempt++) {
            try {
                return insertTx(order, rest);
            } catch (ResponseStatusException answered) {
                throw answered;
            } catch (RuntimeException failed) {
                lastRuntime = failed;
            } catch (IOException failed) {
                lastIo = failed;
            }
            if (orders.findByClientId(order.getClientId()) != null) {
                return null;
            }
        }
        if (lastIo != null) {
            throw lastIo;
        }
        throw lastRuntime;
    }

    @Transactional(rollbackFor = IOException.class)
    private StoreOrder insertTx(StoreOrder order, Writer rest) throws IOException {
        StoreOrder last = orders.findFirstByStoreIdOrderByNumberDesc(order.getStoreId());
        order.setNumber(last == null ? 1001 : last.getNumber() + 1);
        orders.insert(order);
        rest.write(order);
        return order;
    }

    /// A new order row from a priced quote; not yet inserted.
    public StoreOrder newOrder(Quote quote, String channel, String username, String email,
            AddressDto address, String clientId) throws IOException {
        StoreOrder order = new StoreOrder();
        order.setId(Ids.next());
        order.setStoreId(quote.store.getId());
        order.setUsername(username == null ? "" : username);
        order.setEmail(Codes.cut(email, 190));
        order.setChannel(channel);
        order.setStatus(OPEN);
        order.setFinancial(PENDING);
        order.setFulfilment(UNFULFILLED);
        order.setSubtotalCents(quote.subtotalCents);
        order.setDiscountCents(quote.discountCents());
        order.setShippingCents(quote.shippingCents);
        order.setTaxCents(quote.taxCents());
        order.setTotalCents(quote.totalCents);
        order.setCurrency(quote.store.getCurrency());
        order.setDiscountCode(Codes.cut(quote.discountCode(), 64));
        if (address != null) {
            order.setShipName(Codes.cut(address.name(), 120));
            order.setShipLine1(Codes.cut(address.line1(), 200));
            order.setShipLine2(Codes.cut(address.line2(), 200));
            order.setShipCity(Codes.cut(address.city(), 120));
            order.setShipRegion(Codes.cut(address.region(), 80));
            order.setShipPostal(Codes.cut(address.postal(), 20));
            order.setShipCountry(Codes.cut(Codes.normalize(address.country()), 2));
            order.setShipPhone(Codes.cut(address.phone(), 32));
            order.setShipLat(address.lat());
            order.setShipLng(address.lng());
        }
        if (quote.shipping != null) {
            order.setShippingRateId(quote.shipping.rate.getId());
            order.setShippingTitle(Codes.cut(quote.shipping.name, 120));
        }
        order.setClientId(clientId);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        return order;
    }

    /// Inserts a line per quote line.
    ///
    /// @param locations where each quote line's stock is held, by index, or
    ///     null when nothing is reserved yet
    public void insertLines(StoreOrder order, Quote quote, String[] locations) throws IOException {
        insertLines(order, quote, locations, null);
    }

    /// [#insertLines] with each line's backordered units
    /// ([Inventory#reserveCounted]), or null for none.
    public void insertLines(StoreOrder order, Quote quote, String[] locations, int[] backordered)
            throws IOException {
        // A capped flash sale counts these units now, in the order's own
        // transaction, so the cap cannot be passed by two orders at once.
        saleEvents.claim(order, quote);
        for (int iter = 0; iter < quote.lines.size(); iter++) {
            Quote.Line q = quote.lines.get(iter);
            OrderLine line = new OrderLine();
            line.setId(Ids.next());
            line.setOrderId(order.getId());
            line.setProductId(q.product.getId());
            line.setVariantId(q.variant.getId());
            line.setTitle(Codes.cut(q.product.getTitle(), 200));
            line.setVariantTitle(Codes.cut(q.variant.getTitle(), 200));
            line.setSku(Codes.cut(q.variant.getSku(), 64));
            line.setPhotoId(q.product.getPhotoId() == null ? "" : q.product.getPhotoId());
            line.setQuantity(q.price.quantity);
            line.setPriceCents(q.price.unitCents);
            line.setDiscountCents(q.price.discountCents);
            line.setTaxCents(q.price.taxCents);
            line.setWeightGrams(q.variant.getWeightGrams());
            line.setPreorder(q.preorder);
            line.setBackorderedQty(backordered == null ? 0 : Math.max(0, backordered[iter]));
            // A custom sale item (no variant) is held nowhere: nothing to
            // release, ship or restock for it later.
            line.setLocationId(locations == null || locations[iter] == null
                    || q.variant.getId().length() == 0 ? "" : locations[iter]);
            lines.insert(line);
        }
    }

    /// Adds a tender row.
    public void tender(String orderId, String kind, long amount, String reference)
            throws IOException {
        Tender t = new Tender();
        t.setId(Ids.next());
        t.setOrderId(orderId);
        t.setKind(kind);
        t.setAmountCents(amount);
        t.setReference(Codes.cut(reference, 64));
        t.setPaymentId("CARD".equals(kind) ? orderId : "");
        t.setCreatedAt(Instant.now());
        tenders.insert(t);
    }

    /// Adds a step to an order's timeline.
    public void event(String orderId, String kind, String message, String username)
            throws IOException {
        OrderEvent e = new OrderEvent();
        e.setId(Ids.next());
        e.setOrderId(orderId);
        e.setKind(kind);
        e.setMessage(Codes.cut(message, 500));
        e.setUsername(username == null ? "" : username);
        e.setCreatedAt(Instant.now());
        events.insert(e);
    }

    /// Deletes an order that never went through (its card was declined), so
    /// a retry with the same key can place it again.
    @Transactional(rollbackFor = IOException.class)
    public void erase(String orderId) throws IOException {
        lines.deleteByOrderId(orderId);
        events.deleteByOrderId(orderId);
        tenders.deleteByOrderId(orderId);
        StoreOrder order = orders.findById(orderId);
        if (order != null) {
            orders.delete(order);
        }
    }

    /// Tells the shopper and the store's staff that an order changed.
    public void publish(StoreOrder order) {
        if (order == null) {
            return;
        }
        try {
            autoArchive(order);
        } catch (IOException | RuntimeException ignored) {
            // Archiving is housekeeping; the change it follows already stands.
        }
        try {
            List<String> users = new ArrayList<String>();
            if (order.getUsername().length() > 0) {
                users.add(order.getUsername());
            }
            List<StoreStaff> team = staff.findByStoreIdOrderByDisplayNameAsc(order.getStoreId());
            for (int iter = 0; iter < team.size(); iter++) {
                users.add(team.get(iter).getUsername());
            }
            live.changed(LIVE_TYPE, order.getId(), order.getStatus() + "/" + order.getFinancial()
                    + "/" + order.getFulfilment(), users);
        } catch (RuntimeException ignored) {
            // A dropped socket must never undo a committed change.
        }
    }

    // ------------------------------------------------------------- reading

    private StoreOrder inStore(String storeId, String orderId) throws IOException {
        StoreOrder order = orderId == null ? null : orders.findById(orderId);
        if (order == null || !storeId.equals(order.getStoreId())) {
            throw new ResponseStatusException(404, "No such order");
        }
        return order;
    }

    @Transactional(readOnly = true)
    private StoreOrder inStoreRead(String storeId, String orderId) throws IOException {
        return inStore(storeId, orderId);
    }

    /// The whole order as its detail page shows it.
    @Transactional(readOnly = true)
    public OrderDto detailOf(String orderId) throws IOException {
        return dto(orders.findById(orderId), true);
    }

    private OrderDto dto(StoreOrder o, boolean full) throws IOException {
        List<OrderLineDto> ls = null;
        List<OrderEventDto> es = null;
        List<TenderDto> ts = null;
        List<FulfillmentSummaryDto> fs = null;
        List<com.codenameone.examples.bazaar.api.OrderFieldDto> answers = null;
        String paymentStatus = "";
        if (full) {
            ls = new ArrayList<OrderLineDto>();
            List<OrderLine> rows = lines.findByOrderId(o.getId());
            for (int iter = 0; iter < rows.size(); iter++) {
                OrderLine l = rows.get(iter);
                ls.add(new OrderLineDto(l.getId(), l.getVariantId(), l.getProductId(), l.getTitle(),
                        l.getVariantTitle(), l.getSku(), l.getPhotoId(), l.getQuantity(),
                        l.getFulfilledQty(), l.getRefundedQty(), l.getPriceCents(),
                        l.getDiscountCents(), l.getTaxCents(), l.getLocationId()));
            }
            es = new ArrayList<OrderEventDto>();
            List<OrderEvent> evs = events.findByOrderIdOrderByCreatedAtAsc(o.getId());
            for (int iter = 0; iter < evs.size(); iter++) {
                OrderEvent e = evs.get(iter);
                es.add(new OrderEventDto(e.getKind(), e.getMessage(), e.getUsername(),
                        e.getCreatedAt() == null ? 0 : e.getCreatedAt().toEpochMilli()));
            }
            ts = new ArrayList<TenderDto>();
            List<Tender> trs = tenders.findByOrderId(o.getId());
            for (int iter = 0; iter < trs.size(); iter++) {
                Tender t = trs.get(iter);
                ts.add(new TenderDto(t.getKind(), t.getAmountCents(), t.getReference()));
            }
            fs = new ArrayList<FulfillmentSummaryDto>();
            List<Fulfillment> frs = fulfillments.findByOrderId(o.getId());
            for (int iter = 0; iter < frs.size(); iter++) {
                Fulfillment f = frs.get(iter);
                fs.add(new FulfillmentSummaryDto(f.getId(), f.getStatus(), f.getCarrier(),
                        f.getTrackingNumber(),
                        f.getCreatedAt() == null ? 0 : f.getCreatedAt().toEpochMilli()));
            }
            Payment payment = paymentRows.findById(o.getId());
            paymentStatus = payment == null ? "" : payment.getStatus();
            answers = checkoutSettings.answersOf(o.getId());
        }
        AddressDto address = new AddressDto(o.getShipName(), o.getShipLine1(), o.getShipLine2(),
                o.getShipCity(), o.getShipRegion(), o.getShipPostal(), o.getShipCountry(),
                o.getShipPhone(), o.getShipLat(), o.getShipLng());
        return new OrderDto(o.getId(), o.getStoreId(), o.getNumber(), o.getChannel(),
                o.getStatus(), o.getFinancial(), o.getFulfilment(), o.getUsername(), o.getEmail(),
                o.getSubtotalCents(), o.getDiscountCents(), o.getShippingCents(), o.getTaxCents(),
                o.getTotalCents(), o.getRefundedCents(), o.getCurrency(), o.getDiscountCode(),
                address, o.getShippingRateId(), o.getShippingTitle(), o.getNote(), o.getTags(),
                o.getCreatedAt() == null ? 0 : o.getCreatedAt().toEpochMilli(), paymentStatus, ls,
                es, ts, fs, o.getTipCents(), answers);
    }
}
