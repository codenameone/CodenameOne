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
import com.codenameone.examples.bazaar.api.OrderEditDto;
import com.codenameone.examples.bazaar.api.OrderEditEntryDto;
import com.codenameone.examples.bazaar.api.OrderEditLineDto;
import com.codenameone.examples.bazaar.api.OrderEditResultDto;
import com.codenameone.examples.bazaar.api.PosLineDto;
import com.codenameone.examples.bazaar.api.PosTenderDto;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.cart.Quote;
import com.codenameone.examples.bazaar.domain.Layaway;
import com.codenameone.examples.bazaar.domain.OrderEdit;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.Payment;
import com.codenameone.examples.bazaar.domain.PosApproval;
import com.codenameone.examples.bazaar.domain.Register;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.StoreStaff;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.pos.Layaways;
import com.codenameone.examples.bazaar.pos.PosSales;
import com.codenameone.examples.bazaar.pos.PosTools;
import com.codenameone.examples.bazaar.pricing.Money;
import com.codenameone.examples.bazaar.pricing.PriceLine;
import com.codenameone.examples.bazaar.pricing.TaxCalculator;
import com.codenameone.examples.bazaar.repository.LayawayRepository;
import com.codenameone.examples.bazaar.repository.OrderEditRepository;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.PaymentRepository;
import com.codenameone.examples.bazaar.repository.RegisterRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.TaxRateRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Editing an order after it was placed.
///
/// The edit is worked out whole before anything changes: the lines as they
/// will be, priced again (an added line like a counter sale, a kept line at
/// the price it was sold at), the discount kept pro rata or set, and the tax
/// worked out again where the order is taxed -- the register's place for a
/// counter sale, the shipping address otherwise. Stock follows the units: an
/// order whose goods are still to go out reserves or releases them; a sale
/// already handed over at the counter takes added units off the shelf and
/// puts handed-back ones on it again. Then the difference changes hands:
/// collected (a card at the terminal, cash, or the customer's saved card) or
/// refunded (to the order's own card payment when it can take it, otherwise
/// in cash), and only then is the order rewritten, in one transaction, with
/// an entry in its edit history.
@Component
public class OrderEdits {
    public static final String NONE = "NONE";
    public static final String CASH = "CASH";
    public static final String CARD = "CARD";
    public static final String ON_FILE = "ON_FILE";
    public static final String ORIGINAL = "ORIGINAL";
    public static final String LAYAWAY = "LAYAWAY";
    static final int MAX_LINES = 100;

    private final StoreAccess access;
    private final StoreOrderRepository orders;
    private final OrderLineRepository lines;
    private final OrderEditRepository edits;
    private final RegisterRepository registers;
    private final TaxRateRepository taxes;
    private final StoreRepository stores;
    private final PaymentRepository paymentRows;
    private final LayawayRepository layaways;
    private final VariantRepository variants;
    private final PosSales sales;
    private final PosTools tools;
    private final Inventory inventory;
    private final Payments payments;
    private final Orders orderService;

    public OrderEdits(StoreAccess access, StoreOrderRepository orders, OrderLineRepository lines,
            OrderEditRepository edits, RegisterRepository registers, TaxRateRepository taxes,
            StoreRepository stores, PaymentRepository paymentRows, LayawayRepository layaways,
            VariantRepository variants, PosSales sales, PosTools tools, Inventory inventory,
            Payments payments, Orders orderService) {
        this.access = access;
        this.orders = orders;
        this.lines = lines;
        this.edits = edits;
        this.registers = registers;
        this.taxes = taxes;
        this.stores = stores;
        this.paymentRows = paymentRows;
        this.layaways = layaways;
        this.variants = variants;
        this.sales = sales;
        this.tools = tools;
        this.inventory = inventory;
        this.payments = payments;
        this.orderService = orderService;
    }

    /// A line as the edit leaves it.
    private static final class Next {
        OrderLine row;
        Quote.Line added;
        int quantity;
        int before;
        long discountCents;
        PriceLine price;
        String locationId = "";
    }

    /// What an edit will do, worked out before anything changes.
    private static final class Plan {
        StoreOrder order;
        Store store;
        Layaway layaway;
        PosApproval approval;
        boolean counter;
        final List<Next> next = new ArrayList<Next>();
        long subtotal;
        long discount;
        long tax;
        long total;
        long delta;
        String settlement = NONE;
        boolean capture;
        String reference = "";
        final StringBuilder summary = new StringBuilder();
    }

    public OrderEditResultDto edit(String username, String storeId, String orderId,
            OrderEditDto edit) throws IOException {
        Plan plan = plan(username, storeId, orderId, edit);
        String editId = Ids.next();
        if (plan.capture
                && !Orders.PAID.equals(orderService.capturePayment(orderId, username))) {
            throw new ResponseStatusException(402, "The card hold could not be taken");
        }
        settle(plan, edit, editId, username);
        write(plan, edit, editId, username);
        orderService.publish(orders.findById(orderId));
        return new OrderEditResultDto(editId, orderId, plan.subtotal, plan.discount, plan.tax,
                plan.total, plan.delta, plan.settlement);
    }

    @Transactional(readOnly = true)
    public List<OrderEditEntryDto> edits(String username, String storeId, String orderId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        inStore(storeId, orderId);
        List<OrderEdit> rows = edits.findByOrderIdOrderByCreatedAtAsc(orderId);
        List<OrderEditEntryDto> out = new ArrayList<OrderEditEntryDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            OrderEdit e = rows.get(iter);
            out.add(new OrderEditEntryDto(e.getId(), e.getUsername(), e.getSummary(),
                    e.getBeforeCents(), e.getAfterCents(), e.getDeltaCents(), e.getSettlement(),
                    e.getCreatedAt().toEpochMilli()));
        }
        return out;
    }

    private StoreOrder inStore(String storeId, String orderId) {
        StoreOrder o = orderId == null ? null : orders.findById(orderId);
        if (o == null || !storeId.equals(o.getStoreId())) {
            throw new ResponseStatusException(404, "No such order");
        }
        return o;
    }

    // ----------------------------------------------------------------- plan

    @Transactional(readOnly = true)
    private Plan plan(String username, String storeId, String orderId, OrderEditDto edit)
            throws IOException {
        StoreStaff me = access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Plan plan = new Plan();
        plan.order = inStore(storeId, orderId);
        StoreOrder o = plan.order;
        if (edit == null) {
            throw new ResponseStatusException(400, "Say what to change");
        }
        if (!PosTools.approvesItself(me) && tools.policyOf(storeId).isApproveOrderEdits()) {
            plan.approval = tools.requireApproval(storeId, edit.approvalId(), PosTools.ORDER_EDIT);
        }
        if (Orders.CANCELLED.equals(o.getStatus()) || Orders.isUnconvertedDraft(o)) {
            throw new ResponseStatusException(409, "That order cannot be edited");
        }
        plan.layaway = layaways.findByOrderId(orderId);
        if (plan.layaway != null && !Layaways.OPEN.equals(plan.layaway.getStatus())) {
            throw new ResponseStatusException(409, "That layaway is " + plan.layaway.getStatus());
        }
        boolean held = Orders.AUTHORIZED.equals(o.getFinancial());
        boolean paid = held || Orders.PAID.equals(o.getFinancial())
                || Orders.PARTIALLY_REFUNDED.equals(o.getFinancial());
        plan.counter = Orders.POS.equals(o.getChannel()) && Orders.CLOSED.equals(o.getStatus());
        plan.store = stores.findById(storeId);
        List<OrderLine> rows = lines.findByOrderId(orderId);
        List<OrderEditLineDto> changes = edit.change() == null
                ? new ArrayList<OrderEditLineDto>() : edit.change();
        List<PosLineDto> adds = edit.add() == null ? new ArrayList<PosLineDto>() : edit.add();
        if (changes.size() > MAX_LINES || adds.size() > MAX_LINES) {
            throw new ResponseStatusException(400, "Too many lines in one edit");
        }
        for (int iter = 0; iter < rows.size(); iter++) {
            OrderLine row = rows.get(iter);
            Next n = new Next();
            n.row = row;
            n.before = row.getQuantity();
            n.quantity = row.getQuantity();
            n.locationId = row.getLocationId();
            plan.next.add(n);
        }
        for (int iter = 0; iter < changes.size(); iter++) {
            OrderEditLineDto c = changes.get(iter);
            Next n = c == null ? null : find(plan.next, c.lineId());
            if (n == null || c.quantity() < 0 || c.quantity() > 10000) {
                throw new ResponseStatusException(400, "A change names a line of the order and a quantity");
            }
            OrderLine row = n.row;
            if (c.quantity() < row.getRefundedQty()) {
                throw new ResponseStatusException(409, "Units of a line were refunded already");
            }
            if (!plan.counter && c.quantity() < row.getFulfilledQty()) {
                throw new ResponseStatusException(409, "Units of a line were already sent");
            }
            n.quantity = c.quantity();
            if (n.quantity != n.before) {
                plan.summary.append(plan.summary.length() > 0 ? "; " : "").append(title(row))
                        .append(' ').append(n.before).append(" -> ").append(n.quantity);
            }
        }
        if (!adds.isEmpty()) {
            Register reg = o.getRegisterId().length() == 0 ? null
                    : registers.findById(o.getRegisterId());
            Quote q = sales.price(storeId, reg == null ? "" : reg.getTaxCountry(),
                    reg == null ? "" : reg.getTaxRegion(), adds, "");
            for (int iter = 0; iter < q.lines.size(); iter++) {
                Quote.Line l = q.lines.get(iter);
                Next n = new Next();
                n.added = l;
                n.quantity = l.price.quantity;
                if (l.variant.getId().length() > 0) {
                    n.locationId = o.getLocationId().length() > 0 ? o.getLocationId()
                            : inventory.pickLocation(storeId, l.variant.getId(), n.quantity);
                    if (n.locationId == null) {
                        n.locationId = "";
                    }
                }
                plan.next.add(n);
                plan.summary.append(plan.summary.length() > 0 ? "; " : "").append("added ")
                        .append(n.quantity).append(" x ").append(l.product.getTitle());
            }
        }
        price(plan, edit.discountCents());
        plan.delta = plan.total - o.getTotalCents();
        if (edit.discountCents() >= 0) {
            plan.summary.append(plan.summary.length() > 0 ? "; " : "").append("discount ")
                    .append(edit.discountCents()).append(" cents");
        }
        if (plan.summary.length() == 0) {
            throw new ResponseStatusException(400, "The edit changes nothing");
        }
        if (plan.total < 0) {
            throw new ResponseStatusException(400, "The discount is more than the order");
        }
        if (plan.delta != 0) {
            if (plan.layaway != null) {
                if (plan.total < plan.layaway.getPaidCents()) {
                    throw new ResponseStatusException(409,
                            "More was paid toward the layaway than the new total");
                }
                plan.settlement = LAYAWAY;
            } else if (paid && plan.delta > 0) {
                PosTenderDto c = edit.collect();
                if (c == null || c.kind() == null) {
                    throw new ResponseStatusException(400, "Say how the difference is paid");
                }
                if (!CASH.equals(c.kind()) && !CARD.equals(c.kind()) && !ON_FILE.equals(c.kind())) {
                    throw new ResponseStatusException(400, "The difference is paid CASH, CARD or ON_FILE");
                }
                if (ON_FILE.equals(c.kind()) && o.getUsername().length() == 0) {
                    throw new ResponseStatusException(409, "The order has no customer account");
                }
                plan.settlement = c.kind();
                plan.reference = c.reference() == null ? "" : Codes.cut(c.reference(), 40);
            } else if (paid) {
                Payment p = paymentRows.findById(orderId);
                boolean refundable = p != null && (Payments.PAID.equals(p.getStatus())
                        || held && Payments.AUTHORIZED.equals(p.getStatus())
                        || Payments.PARTIALLY_REFUNDED.equals(p.getStatus()))
                        && p.getAmountCents() - p.getRefundedCents() >= -plan.delta;
                plan.settlement = refundable ? ORIGINAL : CASH;
            }
        }
        // A card still on hold is taken first, at the amount it was held for,
        // and the difference then moves like any paid order's: the provider
        // has no way to change a hold's amount.
        plan.capture = held && plan.delta != 0 && plan.layaway == null;
        return plan;
    }

    private static Next find(List<Next> next, String lineId) {
        for (int iter = 0; lineId != null && iter < next.size(); iter++) {
            Next n = next.get(iter);
            if (n.row != null && lineId.equals(n.row.getId())) {
                return n;
            }
        }
        return null;
    }

    private static String title(OrderLine l) {
        return l.getTitle().length() > 0 ? l.getTitle() : l.getSku();
    }

    /// Prices the lines as the edit leaves them: kept lines at the price they
    /// were sold at with their discount scaled to the new quantity (or the
    /// order's new discount spread over every line by its share of the
    /// gross), then taxed again.
    private void price(Plan plan, long discountCents) throws IOException {
        StoreOrder o = plan.order;
        List<PriceLine> priced = new ArrayList<PriceLine>();
        long gross = 0;
        for (int iter = 0; iter < plan.next.size(); iter++) {
            Next n = plan.next.get(iter);
            PriceLine p;
            if (n.added != null) {
                PriceLine a = n.added.price;
                p = new PriceLine(Integer.toString(iter), a.productId, a.variantId, a.unitCents,
                        n.quantity, a.weightGrams, a.taxable);
            } else {
                OrderLine row = n.row;
                Variant v = row.getVariantId().length() == 0 ? null
                        : variants.findById(row.getVariantId());
                boolean taxable = v == null ? row.getTaxCents() > 0 : v.isTaxable();
                p = new PriceLine(Integer.toString(iter), row.getProductId(), row.getVariantId(),
                        row.getPriceCents(), n.quantity, row.getWeightGrams(), taxable);
                n.discountCents = n.before <= 0 ? 0
                        : Money.divideHalfEven(row.getDiscountCents() * n.quantity, n.before);
            }
            n.price = p;
            priced.add(p);
            gross += p.unitCents * p.quantity;
        }
        if (discountCents >= 0) {
            long cap = Math.min(discountCents, gross);
            long left = cap;
            int last = -1;
            for (int iter = 0; iter < plan.next.size(); iter++) {
                if (plan.next.get(iter).quantity > 0) {
                    last = iter;
                }
            }
            for (int iter = 0; iter < plan.next.size(); iter++) {
                Next n = plan.next.get(iter);
                long g = n.price.unitCents * n.quantity;
                long share = iter == last ? left : gross <= 0 ? 0
                        : Money.divideHalfEven(cap * g, gross);
                share = Math.max(0, Math.min(share, Math.min(left, g)));
                n.discountCents = share;
                left -= share;
            }
        }
        long subtotal = 0;
        long discount = 0;
        for (int iter = 0; iter < plan.next.size(); iter++) {
            Next n = plan.next.get(iter);
            n.price.discountCents = Math.min(n.discountCents, n.price.unitCents * n.quantity);
            subtotal += n.price.unitCents * n.quantity;
            discount += n.price.discountCents;
        }
        String country;
        String region;
        if (Orders.POS.equals(o.getChannel())) {
            Register reg = o.getRegisterId().length() == 0 ? null
                    : registers.findById(o.getRegisterId());
            country = reg == null ? "" : reg.getTaxCountry();
            region = reg == null ? "" : reg.getTaxRegion();
        } else {
            country = o.getShipCountry();
            region = o.getShipRegion();
        }
        TaxCalculator.Result tax = TaxCalculator.apply(priced, o.getShippingCents(),
                country.length() == 0 ? null : taxes.findByStoreId(o.getStoreId()),
                country.length() == 0 ? null : country, region, plan.store.isTaxInclusive());
        plan.subtotal = subtotal;
        plan.discount = discount;
        plan.tax = tax.totalCents();
        plan.total = subtotal - discount + o.getShippingCents() + tax.addedCents();
    }

    // --------------------------------------------------------------- settle

    /// Moves the difference, before the order is rewritten: a card that is
    /// declined leaves the order as it was.
    private void settle(Plan plan, OrderEditDto edit, String editId, String username)
            throws IOException {
        StoreOrder o = plan.order;
        if (CARD.equals(plan.settlement)) {
            plan.reference = payments.terminal(editId, username, plan.delta, o.getCurrency(),
                    plan.reference);
        } else if (ON_FILE.equals(plan.settlement)) {
            plan.reference = payments.authorize(editId, o.getUsername(), null, plan.delta,
                    o.getCurrency());
            if (!Payments.PAID.equals(payments.capture(editId))) {
                throw new ResponseStatusException(402, "The saved card was declined");
            }
        } else if (ORIGINAL.equals(plan.settlement)) {
            payments.refund(o.getId(), -plan.delta, "Order edited");
        }
    }

    // ---------------------------------------------------------------- write

    @Transactional(rollbackFor = IOException.class)
    private void write(Plan plan, OrderEditDto edit, String editId, String username)
            throws IOException {
        StoreOrder o = orders.findById(plan.order.getId());
        String storeId = o.getStoreId();
        if (plan.approval != null) {
            tools.use(storeId, plan.approval.getId(), o.getId(), username);
        }
        for (int iter = 0; iter < plan.next.size(); iter++) {
            Next n = plan.next.get(iter);
            if (n.added != null) {
                addLine(o, n, editId, username, plan.counter);
            } else {
                changeLine(o, n, editId, username, plan.counter);
            }
        }
        long before = o.getTotalCents();
        o.setSubtotalCents(plan.subtotal);
        o.setDiscountCents(plan.discount);
        o.setTaxCents(plan.tax);
        o.setTotalCents(plan.total);
        o.setUpdatedAt(Instant.now());
        orders.save(o);
        if (plan.layaway != null) {
            Layaway l = layaways.findById(plan.layaway.getId());
            l.setTotalCents(plan.total);
            l.setUpdatedAt(Instant.now());
            layaways.save(l);
        }
        if (CASH.equals(plan.settlement) || CARD.equals(plan.settlement)
                || ON_FILE.equals(plan.settlement)) {
            orderService.tender(o.getId(), CASH.equals(plan.settlement) ? CASH : CARD,
                    plan.delta, plan.reference);
        } else if (ORIGINAL.equals(plan.settlement)) {
            orderService.tender(o.getId(), "REFUND", plan.delta, "Order edited");
        }
        OrderEdit e = new OrderEdit();
        e.setId(editId);
        e.setStoreId(storeId);
        e.setOrderId(o.getId());
        e.setUsername(username);
        String note = edit.note() == null ? "" : edit.note().trim();
        e.setSummary(Codes.cut(plan.summary + (note.length() > 0 ? " (" + note + ")" : ""), 1000));
        e.setBeforeCents(before);
        e.setAfterCents(plan.total);
        e.setDeltaCents(plan.delta);
        e.setSettlement(plan.settlement);
        e.setCreatedAt(Instant.now());
        edits.insert(e);
        orderService.event(o.getId(), "EDITED", e.getSummary() + "; total " + before + " -> "
                + plan.total + " cents" + (plan.delta == 0 ? "" : ", " + plan.settlement),
                username);
    }

    private void addLine(StoreOrder o, Next n, String editId, String username, boolean counter)
            throws IOException {
        Quote.Line q = n.added;
        String variantId = q.variant.getId();
        if (variantId.length() > 0 && n.locationId.length() > 0) {
            if (counter) {
                inventory.recordPosSale(o.getStoreId(), variantId, n.locationId, n.quantity,
                        o.getId(), username);
            } else if (!inventory.reserve(o.getStoreId(), variantId, n.locationId, n.quantity,
                    editId, username)) {
                throw new ResponseStatusException(409, "Not enough " + q.product.getTitle()
                        + " in stock");
            }
        }
        OrderLine line = new OrderLine();
        line.setId(Ids.next());
        line.setOrderId(o.getId());
        line.setProductId(q.product.getId());
        line.setVariantId(variantId);
        line.setTitle(Codes.cut(q.product.getTitle(), 200));
        line.setVariantTitle(Codes.cut(q.variant.getTitle(), 200));
        line.setSku(Codes.cut(q.variant.getSku(), 64));
        line.setPhotoId(q.product.getPhotoId() == null ? "" : q.product.getPhotoId());
        line.setQuantity(n.quantity);
        line.setFulfilledQty(counter ? n.quantity : 0);
        line.setPriceCents(n.price.unitCents);
        line.setDiscountCents(n.price.discountCents);
        line.setTaxCents(n.price.taxCents);
        line.setWeightGrams(q.variant.getWeightGrams());
        line.setLocationId(variantId.length() == 0 ? "" : n.locationId);
        lines.insert(line);
    }

    private void changeLine(StoreOrder o, Next n, String editId, String username,
            boolean counter) throws IOException {
        OrderLine line = lines.findById(n.row.getId());
        int diff = n.quantity - line.getQuantity();
        String variantId = line.getVariantId();
        boolean stocked = variantId.length() > 0 && line.getLocationId().length() > 0;
        if (diff > 0 && stocked) {
            if (counter) {
                inventory.recordPosSale(o.getStoreId(), variantId, line.getLocationId(), diff,
                        o.getId(), username);
            } else if (!inventory.reserve(o.getStoreId(), variantId, line.getLocationId(), diff,
                    editId, username)) {
                throw new ResponseStatusException(409, "Not enough " + line.getTitle()
                        + " in stock");
            }
        } else if (diff < 0 && stocked) {
            int fewer = -diff;
            int unsent = Math.max(0, line.getQuantity() - line.getFulfilledQty()
                    - line.getRefundedQty());
            int release = Math.min(unsent, fewer);
            if (release > 0) {
                inventory.release(o.getStoreId(), variantId, line.getLocationId(), release,
                        editId, username);
            }
            if (fewer - release > 0) {
                // Handed back across the counter: on the shelf again.
                inventory.restock(o.getStoreId(), variantId, line.getLocationId(),
                        fewer - release, editId, username, Inventory.RETURN);
            }
        }
        if (counter) {
            line.setFulfilledQty(n.quantity);
        } else {
            line.setFulfilledQty(Math.min(line.getFulfilledQty(), n.quantity));
        }
        if (n.quantity == 0 && line.getRefundedQty() == 0) {
            lines.delete(line);
            return;
        }
        line.setQuantity(n.quantity);
        line.setDiscountCents(n.price.discountCents);
        line.setTaxCents(n.price.taxCents);
        lines.save(line);
    }
}
