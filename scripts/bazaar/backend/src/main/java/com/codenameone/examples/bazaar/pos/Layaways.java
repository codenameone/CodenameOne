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

package com.codenameone.examples.bazaar.pos;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.LayawayCancelDto;
import com.codenameone.examples.bazaar.api.LayawayDto;
import com.codenameone.examples.bazaar.api.LayawayPaymentDto;
import com.codenameone.examples.bazaar.api.LayawayRequestDto;
import com.codenameone.examples.bazaar.api.PosTenderDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.cart.Quote;
import com.codenameone.examples.bazaar.domain.Customer;
import com.codenameone.examples.bazaar.domain.Layaway;
import com.codenameone.examples.bazaar.domain.LayawayPayment;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.Register;
import com.codenameone.examples.bazaar.domain.Shift;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.pricing.Money;
import com.codenameone.examples.bazaar.repository.CustomerRepository;
import com.codenameone.examples.bazaar.repository.LayawayPaymentRepository;
import com.codenameone.examples.bazaar.repository.LayawayRepository;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.ShiftRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Layaways: goods put aside for a customer on a deposit.
///
/// A layaway is an order of the counter that stays OPEN and unfulfilled,
/// with its units reserved at the register's location so nobody else sells
/// them. Payments toward it (cash, or a card at the terminal) are kept as
/// rows of their own; the one that pays the balance hands the goods over --
/// the reservation ships, the order closes PAID and FULFILLED. Cancelling
/// releases the reservation and refunds what was paid less the layaway's
/// cancellation fee (a share of its total, never more than was paid), in
/// cash or as store credit.
@Component
public class Layaways {
    public static final String OPEN = "OPEN";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    private final Pos pos;
    private final PosSales sales;
    private final PosTools tools;
    private final StoreAccess access;
    private final CustomerRepository customers;
    private final LayawayRepository layaways;
    private final LayawayPaymentRepository layawayPayments;
    private final StoreOrderRepository orderRows;
    private final OrderLineRepository lineRows;
    private final ShiftRepository shifts;
    private final Orders orders;
    private final Inventory inventory;
    private final Payments payments;

    /// Field-injected, so the constructor stays as it was.
    @com.codename1.backend.annotations.Autowired
    private com.codenameone.examples.bazaar.customer.StoreCredit storeCredit;

    public Layaways(Pos pos, PosSales sales, PosTools tools, StoreAccess access,
            CustomerRepository customers, LayawayRepository layaways,
            LayawayPaymentRepository layawayPayments, StoreOrderRepository orderRows,
            OrderLineRepository lineRows, ShiftRepository shifts, Orders orders,
            Inventory inventory, Payments payments) {
        this.pos = pos;
        this.sales = sales;
        this.tools = tools;
        this.access = access;
        this.customers = customers;
        this.layaways = layaways;
        this.layawayPayments = layawayPayments;
        this.orderRows = orderRows;
        this.lineRows = lineRows;
        this.shifts = shifts;
        this.orders = orders;
        this.inventory = inventory;
        this.payments = payments;
    }

    /// What a layaway's start was checked to be.
    private static final class Start {
        Register register;
        Customer customer;
        Quote quote;
        Shift shift;
        long deposit;
        String kind;
        String reference = "";
        long feeBp;
    }

    public LayawayDto create(String username, String storeId, String registerId,
            LayawayRequestDto req) throws IOException {
        final Start s = check(username, storeId, registerId, req);
        final String clientId = PosSales.clientId(storeId, "layaway\n" + req.clientId().trim());
        StoreOrder again = existing(clientId);
        if (again != null) {
            return read(again.getId());
        }
        StoreOrder order = orders.newOrder(s.quote, Orders.POS, s.customer.getUsername(),
                s.customer.getEmail(), null, clientId);
        order.setLocationId(s.register.getLocationId());
        order.setRegisterId(s.register.getId());
        order.setShiftId(s.shift == null ? "" : s.shift.getId());
        order.setStaffUsername(username);
        order.setCommissionBp(tools.rateFor(storeId, username));
        order.setCustomerId(s.customer.getId());
        order.setNote("Layaway");
        final String who = username;
        StoreOrder placed = orders.insertNumbered(order, new Orders.Writer() {
            @Override
            public void write(StoreOrder o) throws IOException {
                start(o, s, who);
            }
        });
        if (placed == null) {
            StoreOrder first = existing(clientId);
            if (first == null) {
                throw new ResponseStatusException(409, "The layaway is being recorded; send it again");
            }
            placed = first;
        }
        orders.publish(placed);
        return read(placed.getId());
    }

    @Transactional(readOnly = true)
    private StoreOrder existing(String clientId) {
        return orderRows.findByClientId(clientId);
    }

    @Transactional(readOnly = true)
    private Start check(String username, String storeId, String registerId,
            LayawayRequestDto req) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Start s = new Start();
        s.register = pos.register(storeId, registerId);
        if (req == null || req.clientId() == null || req.clientId().trim().length() == 0
                || req.clientId().length() > 100) {
            throw new ResponseStatusException(400, "A layaway needs its register's id for it");
        }
        s.customer = req.customerId() == null ? null : customers.findById(req.customerId());
        if (s.customer == null || !storeId.equals(s.customer.getStoreId())) {
            throw new ResponseStatusException(400, "A layaway is for a customer of the store");
        }
        if (req.shiftId() != null && req.shiftId().length() > 0) {
            s.shift = pos.shiftOf(storeId, req.shiftId());
            if (!s.register.getId().equals(s.shift.getRegisterId())) {
                throw new ResponseStatusException(400, "That shift is another register's");
            }
        }
        s.quote = sales.price(storeId, s.register.getTaxCountry(), s.register.getTaxRegion(),
                req.lines(), "");
        long total = s.quote.totalCents;
        PosTenderDto d = req.deposit();
        if (d == null || d.amountCents() <= 0 || d.kind() == null
                || !"CASH".equals(d.kind()) && !"CARD".equals(d.kind())) {
            throw new ResponseStatusException(400, "A layaway starts with a CASH or CARD deposit");
        }
        long min = Money.basisPoints(total, tools.policyOf(storeId).getLayawayDepositBp());
        if (d.amountCents() < min) {
            throw new ResponseStatusException(400, "The deposit is at least "
                    + com.codenameone.examples.bazaar.web.Money.format(min, s.quote.store.getCurrency()));
        }
        if (d.amountCents() > total) {
            throw new ResponseStatusException(400, "The deposit is more than the total");
        }
        s.deposit = d.amountCents();
        s.kind = d.kind();
        s.reference = d.reference() == null ? "" : Codes.cut(d.reference(), 40);
        long fee = req.cancelFeeBp() < 0 ? tools.policyOf(storeId).getLayawayCancelFeeBp()
                : req.cancelFeeBp();
        if (fee > PosTools.MAX_BP) {
            throw new ResponseStatusException(400, "A fee is at most 100%");
        }
        s.feeBp = fee;
        return s;
    }

    /// Everything a layaway's start writes, in the order's own transaction.
    private void start(StoreOrder o, Start s, String username) throws IOException {
        String storeId = o.getStoreId();
        String[] where = new String[s.quote.lines.size()];
        for (int iter = 0; iter < where.length; iter++) {
            where[iter] = s.register.getLocationId();
        }
        orders.insertLines(o, s.quote, where);
        for (int iter = 0; iter < s.quote.lines.size(); iter++) {
            Quote.Line l = s.quote.lines.get(iter);
            if (l.variant.getId().length() > 0 && !inventory.reserve(storeId, l.variant.getId(),
                    s.register.getLocationId(), l.price.quantity, o.getId(), username)) {
                throw new ResponseStatusException(409, "Not enough " + l.product.getTitle()
                        + " to put aside");
            }
        }
        Layaway l = new Layaway();
        l.setId(Ids.next());
        l.setStoreId(storeId);
        l.setOrderId(o.getId());
        l.setRegisterId(s.register.getId());
        l.setCustomerId(s.customer.getId());
        l.setStatus(OPEN);
        l.setTotalCents(o.getTotalCents());
        l.setCancelFeeBp(s.feeBp);
        l.setCreatedAt(Instant.now());
        l.setUpdatedAt(Instant.now());
        layaways.insert(l);
        orders.event(o.getId(), "LAYAWAY", "Put aside on a deposit of " + s.deposit + " cents",
                username);
        payIn(o, l, s.kind, s.deposit, s.reference, s.shift, username);
    }

    /// Takes a payment toward a layaway, writing its row, the order's tender
    /// and the shift's takings, and completes the layaway when it is paid.
    private void payIn(StoreOrder o, Layaway l, String kind, long amount, String typed,
            Shift shift, String username) throws IOException {
        LayawayPayment p = new LayawayPayment();
        p.setId(Ids.next());
        String reference = typed;
        if ("CARD".equals(kind)) {
            reference = payments.terminal(p.getId(), username, amount, o.getCurrency(), typed);
        }
        p.setStoreId(l.getStoreId());
        p.setLayawayId(l.getId());
        p.setKind(kind);
        p.setAmountCents(amount);
        p.setReference(Codes.cut(reference, 64));
        p.setUsername(username);
        p.setCreatedAt(Instant.now());
        layawayPayments.insert(p);
        orders.tender(o.getId(), kind, amount, reference);
        if (shift != null) {
            boolean cash = "CASH".equals(kind);
            shifts.addTotals(shift.getId(), cash ? amount : 0, cash ? 0 : amount, 0, 0,
                    cash ? amount : 0, 0);
        }
        l.setPaidCents(l.getPaidCents() + amount);
        l.setUpdatedAt(Instant.now());
        if (l.getPaidCents() >= l.getTotalCents()) {
            complete(o, l, username);
        }
        layaways.save(l);
    }

    /// Paid in full: the reserved units leave with the customer.
    private void complete(StoreOrder o, Layaway l, String username) throws IOException {
        List<OrderLine> rows = lineRows.findByOrderId(o.getId());
        for (int iter = 0; iter < rows.size(); iter++) {
            OrderLine line = rows.get(iter);
            int held = line.getQuantity() - line.getFulfilledQty() - line.getRefundedQty();
            if (held > 0 && line.getLocationId().length() > 0) {
                inventory.ship(o.getStoreId(), line.getVariantId(), line.getLocationId(), held,
                        o.getId(), username);
            }
            line.setFulfilledQty(line.getQuantity() - line.getRefundedQty());
            lineRows.save(line);
        }
        StoreOrder fresh = orderRows.findById(o.getId());
        fresh.setStatus(Orders.CLOSED);
        fresh.setFinancial(Orders.PAID);
        fresh.setFulfilment(Orders.FULFILLED);
        fresh.setClosedAt(Instant.now());
        fresh.setUpdatedAt(Instant.now());
        orderRows.save(fresh);
        l.setStatus(COMPLETED);
        orders.event(o.getId(), "LAYAWAY_COMPLETED", "Paid in full and collected", username);
    }

    public LayawayDto pay(String username, String storeId, String layawayId, PosTenderDto t)
            throws IOException {
        payTx(username, storeId, layawayId, t);
        return layaway(username, storeId, layawayId);
    }

    @Transactional(rollbackFor = IOException.class)
    private void payTx(String username, String storeId, String layawayId, PosTenderDto t)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Layaway l = layaway(storeId, layawayId);
        if (!OPEN.equals(l.getStatus())) {
            throw new ResponseStatusException(409, "That layaway is " + l.getStatus());
        }
        if (t == null || t.kind() == null || t.amountCents() <= 0
                || !"CASH".equals(t.kind()) && !"CARD".equals(t.kind())) {
            throw new ResponseStatusException(400, "A payment is CASH or CARD and an amount");
        }
        long balance = l.getTotalCents() - l.getPaidCents();
        if (t.amountCents() > balance) {
            throw new ResponseStatusException(400, "That is more than the balance of " + balance
                    + " cents");
        }
        StoreOrder o = orderRows.findById(l.getOrderId());
        payIn(o, l, t.kind(), t.amountCents(),
                t.reference() == null ? "" : Codes.cut(t.reference(), 40), null, username);
        orders.publish(o);
    }

    public LayawayDto cancel(String username, String storeId, String layawayId,
            LayawayCancelDto req) throws IOException {
        cancelTx(username, storeId, layawayId, req);
        return layaway(username, storeId, layawayId);
    }

    @Transactional(rollbackFor = IOException.class)
    private void cancelTx(String username, String storeId, String layawayId,
            LayawayCancelDto req) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Layaway l = layaway(storeId, layawayId);
        if (!OPEN.equals(l.getStatus())) {
            throw new ResponseStatusException(409, "That layaway is " + l.getStatus());
        }
        String to = req == null || req.refundTo() == null ? "CASH" : req.refundTo();
        if (!"CASH".equals(to) && !"STORE_CREDIT".equals(to)) {
            throw new ResponseStatusException(400, "A layaway is refunded in CASH or STORE_CREDIT");
        }
        Shift shift = req == null || req.shiftId() == null || req.shiftId().length() == 0 ? null
                : pos.shiftOf(storeId, req.shiftId());
        StoreOrder o = orderRows.findById(l.getOrderId());
        if (orderRows.claimStatus(o.getId(), Orders.OPEN, Orders.CANCELLED) != 1) {
            throw new ResponseStatusException(409, "The layaway is changing; try again");
        }
        List<OrderLine> rows = lineRows.findByOrderId(o.getId());
        for (int iter = 0; iter < rows.size(); iter++) {
            OrderLine line = rows.get(iter);
            int held = line.getQuantity() - line.getFulfilledQty() - line.getRefundedQty();
            if (held > 0 && line.getLocationId().length() > 0) {
                inventory.release(storeId, line.getVariantId(), line.getLocationId(), held,
                        l.getId(), username);
            }
        }
        long paid = l.getPaidCents();
        long fee = Math.min(paid, Money.basisPoints(l.getTotalCents(), l.getCancelFeeBp()));
        long refund = paid - fee;
        if (refund > 0) {
            if ("STORE_CREDIT".equals(to)) {
                storeCredit.grant(storeId, l.getCustomerId(), refund, null,
                        "Layaway " + l.getId() + " cancelled");
            } else if (shift != null) {
                shifts.addTotals(shift.getId(), -refund, 0, 0, 0, -refund, 0);
            }
            LayawayPayment p = new LayawayPayment();
            p.setId(Ids.next());
            p.setStoreId(storeId);
            p.setLayawayId(l.getId());
            p.setKind(to);
            p.setAmountCents(-refund);
            p.setReference("Cancelled");
            p.setUsername(username);
            p.setCreatedAt(Instant.now());
            layawayPayments.insert(p);
            orders.tender(o.getId(), to, -refund, "Layaway cancelled");
        }
        StoreOrder fresh = orderRows.findById(o.getId());
        fresh.setRefundedCents(refund);
        fresh.setFinancial(paid == 0 ? Orders.VOIDED : refund == paid ? Orders.REFUNDED
                : Orders.PARTIALLY_REFUNDED);
        fresh.setUpdatedAt(Instant.now());
        orderRows.save(fresh);
        l.setStatus(CANCELLED);
        l.setRefundedCents(refund);
        l.setUpdatedAt(Instant.now());
        layaways.save(l);
        orders.event(o.getId(), "LAYAWAY_CANCELLED", "Cancelled; " + refund + " cents back as "
                + to + ", " + fee + " cents kept"
                + (req != null && req.reason() != null && req.reason().length() > 0
                ? ": " + Codes.cut(req.reason(), 200) : ""), username);
        orders.publish(fresh);
    }

    @Transactional(readOnly = true)
    public List<LayawayDto> layaways(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<Layaway> rows = layaways.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<LayawayDto> out = new ArrayList<LayawayDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public LayawayDto layaway(String username, String storeId, String layawayId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return dto(layaway(storeId, layawayId));
    }

    private Layaway layaway(String storeId, String layawayId) {
        Layaway l = layawayId == null ? null : layaways.findById(layawayId);
        if (l == null || !storeId.equals(l.getStoreId())) {
            throw new ResponseStatusException(404, "No such layaway");
        }
        return l;
    }

    @Transactional(readOnly = true)
    private LayawayDto read(String orderId) {
        return dto(layaways.findByOrderId(orderId));
    }

    private LayawayDto dto(Layaway l) {
        StoreOrder o = orderRows.findById(l.getOrderId());
        List<LayawayPayment> rows = layawayPayments.findByLayawayIdOrderByCreatedAtAsc(l.getId());
        List<LayawayPaymentDto> ps = new ArrayList<LayawayPaymentDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            LayawayPayment p = rows.get(iter);
            ps.add(new LayawayPaymentDto(p.getKind(), p.getAmountCents(), p.getReference(),
                    p.getCreatedAt().toEpochMilli()));
        }
        long balance = OPEN.equals(l.getStatus()) ? l.getTotalCents() - l.getPaidCents() : 0;
        return new LayawayDto(l.getId(), l.getOrderId(), o == null ? 0 : o.getNumber(),
                l.getCustomerId(), l.getStatus(), l.getTotalCents(), l.getPaidCents(), balance,
                l.getCancelFeeBp(), l.getRefundedCents(), l.getCreatedAt().toEpochMilli(), ps);
    }
}
