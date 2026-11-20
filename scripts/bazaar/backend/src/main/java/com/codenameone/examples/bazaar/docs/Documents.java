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
package com.codenameone.examples.bazaar.docs;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.pdf.PdfCanvas;
import com.codename1.pdf.PdfDocument;
import com.codename1.pdf.PdfFont;
import com.codename1.pdf.PdfPage;
import com.codenameone.examples.bazaar.api.FulfilLineDto;
import com.codenameone.examples.bazaar.domain.CashMove;
import com.codenameone.examples.bazaar.domain.Fulfillment;
import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.PurchaseLine;
import com.codenameone.examples.bazaar.domain.PurchaseOrder;
import com.codenameone.examples.bazaar.domain.Refund;
import com.codenameone.examples.bazaar.domain.RefundLine;
import com.codenameone.examples.bazaar.domain.Register;
import com.codenameone.examples.bazaar.domain.Shift;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.domain.Supplier;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.fulfil.Fulfillments;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.CashMoveRepository;
import com.codenameone.examples.bazaar.repository.LocationRepository;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.PurchaseLineRepository;
import com.codenameone.examples.bazaar.repository.PurchaseOrderRepository;
import com.codenameone.examples.bazaar.repository.RefundLineRepository;
import com.codenameone.examples.bazaar.repository.RefundRepository;
import com.codenameone.examples.bazaar.repository.RegisterRepository;
import com.codenameone.examples.bazaar.repository.ShiftRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.SupplierRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/// The printable documents, made on the server with `com.codename1.pdf` --
/// the same classes an app uses on the device -- and answered as PDF bytes:
/// invoice, packing slip, 4x6 shipping label, purchase order, credit note
/// and end-of-shift Z report.
///
/// Every method checks that what it prints belongs to `storeId`; who may ask
/// is [DocumentPages]'s concern. Nothing here sends mail: the app shares or
/// prints the file itself.
@Component
public class Documents {
    /// A 4x6 inch label, in points.
    static final float LABEL_W = 288;
    static final float LABEL_H = 432;

    private final StoreRepository stores;
    private final StoreOrderRepository orders;
    private final OrderLineRepository lines;
    private final LocationRepository locations;
    private final VariantRepository variants;
    private final ProductRepository products;
    private final PurchaseOrderRepository purchaseOrders;
    private final PurchaseLineRepository purchaseLines;
    private final SupplierRepository suppliers;
    private final RefundRepository refunds;
    private final RefundLineRepository refundLines;
    private final ShiftRepository shifts;
    private final RegisterRepository registers;
    private final CashMoveRepository cashMoves;
    private final Fulfillments fulfillments;

    public Documents(StoreRepository stores, StoreOrderRepository orders,
            OrderLineRepository lines, LocationRepository locations, VariantRepository variants,
            ProductRepository products, PurchaseOrderRepository purchaseOrders,
            PurchaseLineRepository purchaseLines, SupplierRepository suppliers,
            RefundRepository refunds, RefundLineRepository refundLines, ShiftRepository shifts,
            RegisterRepository registers, CashMoveRepository cashMoves,
            Fulfillments fulfillments) {
        this.stores = stores;
        this.orders = orders;
        this.lines = lines;
        this.locations = locations;
        this.variants = variants;
        this.products = products;
        this.purchaseOrders = purchaseOrders;
        this.purchaseLines = purchaseLines;
        this.suppliers = suppliers;
        this.refunds = refunds;
        this.refundLines = refundLines;
        this.shifts = shifts;
        this.registers = registers;
        this.cashMoves = cashMoves;
        this.fulfillments = fulfillments;
    }

    // -------------------------------------------------------------- invoice

    /// The invoice of an order: lines after discounts, shipping, tax, total,
    /// what was refunded and whether it is paid.
    @Transactional(readOnly = true)
    public byte[] invoice(String storeId, String orderId) throws IOException {
        StoreOrder o = order(storeId, orderId);
        Store s = stores.findById(storeId);
        String cur = o.getCurrency();
        Sheet sheet = new Sheet(s.getName(), "INVOICE", "#" + o.getNumber(), s.getAccentColor(),
                footer(s));
        sheet.blocks("BILL TO", shipTo(o), "INVOICE", new String[] {
            "Invoice #" + o.getNumber(),
            "Date " + Sheet.date(millis(o.getCreatedAt())),
            "Status " + paymentStatus(o.getFinancial()),
            o.getShippingTitle().length() > 0 ? "Delivery " + o.getShippingTitle() : "",
        });
        Sheet.Table t = sheet.table(new float[] {4.2f, 1.6f, 0.7f, 1.2f, 1.3f},
                new String[] {"Item", "SKU", "Qty", "Price", "Amount"},
                new boolean[] {false, false, true, true, true});
        List<OrderLine> ls = lines.findByOrderId(orderId);
        for (int iter = 0; iter < ls.size(); iter++) {
            OrderLine l = ls.get(iter);
            long gross = l.getPriceCents() * l.getQuantity();
            String item = title(l);
            if (l.getDiscountCents() > 0) {
                item += "\nDiscount -" + Sheet.money(l.getDiscountCents(), cur);
            }
            t.row(item, l.getSku(), Integer.toString(l.getQuantity()),
                    Sheet.money(l.getPriceCents(), cur),
                    Sheet.money(gross - l.getDiscountCents(), cur));
        }
        t.end();
        String disc = o.getDiscountCode().length() > 0 ? "Discounts (" + o.getDiscountCode() + ")"
                : "Discounts";
        String[][] rows = new String[][] {
            {"Subtotal", Sheet.money(o.getSubtotalCents(), cur)},
            {disc, "-" + Sheet.money(o.getDiscountCents(), cur)},
            {"Shipping", Sheet.money(o.getShippingCents(), cur)},
            {s.isTaxInclusive() ? "Tax (included)" : "Tax", Sheet.money(o.getTaxCents(), cur)},
            {"Total", Sheet.money(o.getTotalCents(), cur)},
        };
        sheet.totals(rows, true);
        if (o.getRefundedCents() > 0) {
            sheet.totals(new String[][] {
                {"Refunded", "-" + Sheet.money(o.getRefundedCents(), cur)},
                {"Net", Sheet.money(o.getTotalCents() - o.getRefundedCents(), cur)},
            }, false);
        }
        sheet.space(6).heading(paymentLine(o));
        if (s.getPolicies().length() > 0) {
            sheet.space(8).muted(s.getPolicies());
        }
        sheet.space(8).muted("Thank you for shopping with " + s.getName() + ".");
        return sheet.finish();
    }

    // --------------------------------------------------------- packing slip

    /// What goes in the box, without prices: a box to tick, the item, its
    /// SKU and barcode, the quantity. With `fulfillmentId` only that
    /// shipment's lines; otherwise everything on the order not refunded.
    @Transactional(readOnly = true)
    public byte[] packingSlip(String storeId, String orderId, String fulfillmentId)
            throws IOException {
        StoreOrder o = order(storeId, orderId);
        Store s = stores.findById(storeId);
        Sheet sheet = new Sheet(s.getName(), "PACKING SLIP", "Order #" + o.getNumber(),
                s.getAccentColor(), footer(s));
        sheet.blocks("SHIP TO", shipTo(o), "ORDER", new String[] {
            "Order #" + o.getNumber(),
            "Placed " + Sheet.date(millis(o.getCreatedAt())),
            o.getShippingTitle(),
        });
        Sheet.Table t = sheet.table(new float[] {0.5f, 4.2f, 1.7f, 1.8f, 0.7f},
                new String[] {"", "Item", "SKU", "Barcode", "Qty"},
                new boolean[] {false, false, false, false, true});
        List<OrderLine> ls = lines.findByOrderId(orderId);
        int units = 0;
        if (fulfillmentId != null && fulfillmentId.length() > 0) {
            Fulfillment f = fulfillments.require(storeId, fulfillmentId);
            if (!f.getOrderId().equals(orderId)) {
                throw new ResponseStatusException(404, "No such fulfilment");
            }
            List<FulfilLineDto> fl = fulfillments.linesOf(f);
            for (int iter = 0; iter < fl.size(); iter++) {
                OrderLine l = find(ls, fl.get(iter).orderLineId());
                t.checkRow(fl.get(iter).title(), fl.get(iter).sku(), barcode(l),
                        Integer.toString(fl.get(iter).quantity()));
                units += fl.get(iter).quantity();
            }
        } else {
            for (int iter = 0; iter < ls.size(); iter++) {
                OrderLine l = ls.get(iter);
                int q = l.getQuantity() - l.getRefundedQty();
                if (q > 0) {
                    t.checkRow(title(l), l.getSku(), barcode(l), Integer.toString(q));
                    units += q;
                }
            }
        }
        t.end();
        sheet.text(units + (units == 1 ? " item" : " items") + " in this parcel.");
        if (o.getNote().length() > 0) {
            sheet.space(6).heading("Note").text(o.getNote());
        }
        sheet.space(10).muted("Packed by ____________________    Checked by ____________________");
        return sheet.finish();
    }

    // ---------------------------------------------------------------- label

    /// A 4x6 inch shipping label: sender, recipient, service, the tracking
    /// number as a Code 128 barcode and in large type.
    @Transactional(readOnly = true)
    public byte[] label(String storeId, String fulfillmentId) throws IOException {
        Fulfillment f = fulfillments.require(storeId, fulfillmentId);
        if (f.getTrackingNumber().length() == 0) {
            throw new ResponseStatusException(409, "The parcel has no label yet");
        }
        StoreOrder o = orders.findById(f.getOrderId());
        Store s = stores.findById(storeId);
        Location from = locations.findById(f.getLocationId());
        PdfDocument doc = new PdfDocument();
        doc.setTitle("Label " + f.getTrackingNumber()).setAuthor(s.getName())
                .setCreator("Bazaar");
        PdfPage page = doc.addPage(LABEL_W, LABEL_H);
        PdfCanvas c = page.getCanvas();
        float m = 12;
        float w = LABEL_W - 2 * m;
        c.setStrokeColor(0x000000).setLineWidth(1.5f).strokeRect(m / 2, m / 2, LABEL_W - m,
                LABEL_H - m);
        // From.
        c.setFillColor(0x000000).setFont(PdfFont.HELVETICA_BOLD, 7).drawText("FROM", m, m + 12);
        String[] sender = {s.getName(), from == null ? "" : from.getName(),
            from == null ? s.getAddress() : from.getAddress()};
        float y = m + 23;
        for (int iter = 0; iter < sender.length; iter++) {
            if (sender[iter] != null && sender[iter].length() > 0) {
                c.setFont(iter == 0 ? PdfFont.HELVETICA_BOLD : PdfFont.HELVETICA, 8)
                        .drawText(Sheet.fit(sender[iter], PdfFont.HELVETICA, 8, w), m, y);
                y += 10;
            }
        }
        // Service band.
        y = 72;
        c.fillRect(m / 2, y, LABEL_W - m, 34);
        String service = f.getService().length() > 0 ? f.getService() : "PARCEL";
        c.setFillColor(0xffffff).setFont(PdfFont.HELVETICA_BOLD, 20).drawText(service, m, y + 24);
        String weight = grams(f.getWeightGrams());
        c.setFont(PdfFont.HELVETICA_BOLD, 10).drawText(weight, LABEL_W - m
                - PdfFont.HELVETICA_BOLD.getStringWidth(weight, 10), y + 21);
        // To.
        c.setFillColor(0x000000).setFont(PdfFont.HELVETICA_BOLD, 8).drawText("SHIP TO", m,
                y + 52);
        String[] to = shipTo(o);
        y += 70;
        for (int iter = 0; iter < to.length; iter++) {
            if (to[iter] == null || to[iter].length() == 0) {
                continue;
            }
            PdfFont font = iter == 0 ? PdfFont.HELVETICA_BOLD : PdfFont.HELVETICA;
            float size = iter == 0 ? 15 : 13;
            c.setFont(font, size).drawText(Sheet.fit(to[iter], font, size, w), m + 8, y);
            y += size + 4;
        }
        // Tracking.
        float barTop = 286;
        c.setLineWidth(1).drawLine(m / 2, barTop - 14, LABEL_W - m / 2, barTop - 14);
        c.setFont(PdfFont.HELVETICA_BOLD, 8).drawText("TRACKING #  " + carrierName(f), m,
                barTop - 2);
        Code128.draw(c, f.getTrackingNumber(), m, barTop + 4, w, 74);
        String number = f.getTrackingNumber();
        c.setFont(PdfFont.COURIER_BOLD, 16);
        float nw = PdfFont.COURIER_BOLD.getStringWidth(number, 16);
        c.drawText(number, (LABEL_W - nw) / 2, barTop + 100);
        c.setFont(PdfFont.HELVETICA, 7).drawText("Order #" + o.getNumber() + "   "
                + Sheet.date(millis(f.getUpdatedAt())), m, LABEL_H - m - 4);
        return doc.toByteArray();
    }

    // ------------------------------------------------------- purchase order

    @Transactional(readOnly = true)
    public byte[] purchaseOrder(String storeId, String purchaseOrderId) throws IOException {
        PurchaseOrder po = purchaseOrderId == null ? null
                : purchaseOrders.findById(purchaseOrderId);
        if (po == null || !storeId.equals(po.getStoreId())) {
            throw new ResponseStatusException(404, "No such purchase order");
        }
        Store s = stores.findById(storeId);
        Supplier sup = suppliers.findById(po.getSupplierId());
        Location to = locations.findById(po.getLocationId());
        String cur = s.getCurrency();
        Sheet sheet = new Sheet(s.getName(), "PURCHASE ORDER", "PO-" + po.getNumber(),
                s.getAccentColor(), footer(s));
        sheet.blocks("SUPPLIER", sup == null ? new String[] {"-"} : new String[] {
            sup.getName(), sup.getContact(), sup.getAddress(), sup.getEmail(), sup.getPhone()},
            "DELIVER TO", new String[] {s.getName(), to == null ? "" : to.getName(),
                to == null ? s.getAddress() : to.getAddress(),
                "Ordered " + Sheet.date(millis(po.getCreatedAt())),
                po.getExpectedAt() != null && po.getExpectedAt().toEpochMilli() > 0
                    ? "Expected " + Sheet.date(po.getExpectedAt().toEpochMilli()) : "",
                "Status " + po.getStatus()});
        Sheet.Table t = sheet.table(new float[] {1.6f, 4f, 0.8f, 1.2f, 1.3f},
                new String[] {"SKU", "Item", "Qty", "Unit cost", "Amount"},
                new boolean[] {false, false, true, true, true});
        long total = 0;
        int units = 0;
        List<PurchaseLine> pl = purchaseLines.findByPurchaseOrderId(po.getId());
        for (int iter = 0; iter < pl.size(); iter++) {
            PurchaseLine l = pl.get(iter);
            Variant v = variants.findById(l.getVariantId());
            String item = "?";
            String sku = "";
            if (v != null) {
                Product p = products.findById(v.getProductId());
                item = (p == null ? "" : p.getTitle()) + (v.getTitle().length() > 0
                        ? " - " + v.getTitle() : "");
                sku = v.getSku();
            }
            long amount = l.getCostCents() * l.getQuantity();
            total += amount;
            units += l.getQuantity();
            t.row(sku, item, Integer.toString(l.getQuantity()), Sheet.money(l.getCostCents(), cur),
                    Sheet.money(amount, cur));
        }
        t.end();
        sheet.totals(new String[][] {{"Units", Integer.toString(units)},
            {"Total", Sheet.money(total, cur)}}, true);
        if (po.getNote().length() > 0) {
            sheet.heading("Notes").text(po.getNote());
        }
        sheet.space(8).muted("Please quote PO-" + po.getNumber() + " on the delivery note and "
                + "invoice.");
        return sheet.finish();
    }

    // ---------------------------------------------------------- credit note

    @Transactional(readOnly = true)
    public byte[] creditNote(String storeId, String refundId) throws IOException {
        Refund r = refundId == null ? null : refunds.findById(refundId);
        StoreOrder o = r == null ? null : orders.findById(r.getOrderId());
        if (o == null || !storeId.equals(o.getStoreId())) {
            throw new ResponseStatusException(404, "No such refund");
        }
        Store s = stores.findById(storeId);
        String cur = o.getCurrency();
        String ref = "CN-" + o.getNumber() + "-" + r.getId().substring(Math.max(0,
                r.getId().length() - 6));
        Sheet sheet = new Sheet(s.getName(), "CREDIT NOTE", ref, s.getAccentColor(), footer(s));
        sheet.blocks("CUSTOMER", shipTo(o), "CREDIT NOTE", new String[] {ref,
            "Date " + Sheet.date(millis(r.getCreatedAt())),
            "For invoice #" + o.getNumber(),
            r.getReason().length() > 0 ? "Reason " + r.getReason() : ""});
        Sheet.Table t = sheet.table(new float[] {4.5f, 1.6f, 0.8f, 1.4f},
                new String[] {"Item", "SKU", "Qty", "Credit"},
                new boolean[] {false, false, true, true});
        List<OrderLine> ls = lines.findByOrderId(o.getId());
        List<RefundLine> rl = refundLines.findByRefundId(r.getId());
        long itemized = 0;
        for (int iter = 0; iter < rl.size(); iter++) {
            OrderLine l = find(ls, rl.get(iter).getOrderLineId());
            if (l == null) {
                continue;
            }
            long credit = Orders.lineRefund(l, rl.get(iter).getQuantity(), s.isTaxInclusive());
            itemized += credit;
            t.row(title(l), l.getSku(), Integer.toString(rl.get(iter).getQuantity()),
                    Sheet.money(credit, cur));
        }
        if (r.getShippingCents() > 0) {
            itemized += r.getShippingCents();
            t.row("Shipping", "", "", Sheet.money(r.getShippingCents(), cur));
        }
        if (r.getAmountCents() != itemized) {
            t.row("Adjustment", "", "", Sheet.money(r.getAmountCents() - itemized, cur));
        }
        t.end();
        sheet.totals(new String[][] {{"Total credited", Sheet.money(r.getAmountCents(), cur)}},
                true);
        sheet.muted(r.isRestock() ? "Returned goods were put back in stock."
                : "Credited to the original payment method.");
        return sheet.finish();
    }

    // ------------------------------------------------------------- Z report

    /// The end-of-shift Z report of a register shift: float, sales by
    /// tender, cash movements, expected against counted cash.
    @Transactional(readOnly = true)
    public byte[] shiftReport(String storeId, String shiftId) throws IOException {
        Shift sh = shiftId == null ? null : shifts.findById(shiftId);
        if (sh == null || !storeId.equals(sh.getStoreId())) {
            throw new ResponseStatusException(404, "No such shift");
        }
        Store s = stores.findById(storeId);
        Register reg = registers.findById(sh.getRegisterId());
        String cur = s.getCurrency();
        boolean closed = sh.getClosedAt() != null && sh.getClosedAt().toEpochMilli() > 0;
        Sheet sheet = new Sheet(s.getName(), closed ? "Z REPORT" : "X REPORT",
                "Shift " + Sheet.date(millis(sh.getOpenedAt())), s.getAccentColor(), footer(s));
        sheet.blocks("REGISTER", new String[] {reg == null ? "Register" : reg.getName(),
            "Cashier " + sh.getUsername(), "Status " + sh.getStatus()},
            "SHIFT", new String[] {"Opened " + Sheet.dateTime(millis(sh.getOpenedAt())),
                closed ? "Closed " + Sheet.dateTime(millis(sh.getClosedAt())) : "Still open",
                "Sales " + sh.getSalesCount()});
        sheet.heading("Sales by tender");
        long sales = sh.getCashSalesCents() + sh.getCardSalesCents() + sh.getOtherSalesCents();
        sheet.table(new float[] {4, 2}, new String[] {"Tender", "Amount"},
                new boolean[] {false, true})
                .row("Cash", Sheet.money(sh.getCashSalesCents(), cur))
                .row("Card", Sheet.money(sh.getCardSalesCents(), cur))
                .row("Gift card and store credit", Sheet.money(sh.getOtherSalesCents(), cur))
                .row("Total sales", Sheet.money(sales, cur))
                .end();
        List<CashMove> moves = cashMoves.findByShiftIdOrderByCreatedAtAsc(sh.getId());
        if (!moves.isEmpty()) {
            sheet.heading("Cash in and out");
            Sheet.Table t = sheet.table(new float[] {1.8f, 3f, 1.4f, 1.4f},
                    new String[] {"Time", "Reason", "By", "Amount"},
                    new boolean[] {false, false, false, true});
            for (int iter = 0; iter < moves.size(); iter++) {
                CashMove cm = moves.get(iter);
                t.row(Sheet.dateTime(millis(cm.getCreatedAt())), cm.getReason(), cm.getUsername(),
                        Sheet.money(cm.getDeltaCents(), cur));
            }
            t.end();
        }
        sheet.heading("Cash drawer");
        long over = sh.getCountedCents() - sh.getExpectedCents();
        sheet.totals(new String[][] {
            {"Opening float", Sheet.money(sh.getOpeningCents(), cur)},
            {"Cash sales", Sheet.money(sh.getCashSalesCents(), cur)},
            {"Cash in and out", Sheet.money(sh.getCashMovesCents(), cur)},
            {"Expected in drawer", Sheet.money(sh.getExpectedCents(), cur)},
            {"Counted", closed ? Sheet.money(sh.getCountedCents(), cur) : "-"},
            {over >= 0 ? "Over" : "Short", closed ? Sheet.money(over < 0 ? -over : over, cur)
                : "-"},
        }, true);
        if (sh.getNote().length() > 0) {
            sheet.heading("Note").text(sh.getNote());
        }
        sheet.space(16).muted("Counted by ____________________    Manager ____________________");
        return sheet.finish();
    }

    // -------------------------------------------------------------- helpers

    /// Order `orderId` of `storeId`, or 404.
    @Transactional(readOnly = true)
    public StoreOrder order(String storeId, String orderId) {
        StoreOrder o = orderId == null ? null : orders.findById(orderId);
        if (o == null || !storeId.equals(o.getStoreId())) {
            throw new ResponseStatusException(404, "No such order");
        }
        return o;
    }

    private static String[] shipTo(StoreOrder o) {
        String cityLine = o.getShipCity()
                + (o.getShipRegion().length() > 0 ? ", " + o.getShipRegion() : "")
                + (o.getShipPostal().length() > 0 ? " " + o.getShipPostal() : "");
        return new String[] {o.getShipName().length() > 0 ? o.getShipName() : o.getEmail(),
            o.getShipLine1(), o.getShipLine2(), cityLine.trim(), o.getShipCountry(),
            o.getShipName().length() > 0 ? o.getEmail() : ""};
    }

    private static String footer(Store s) {
        StringBuilder out = new StringBuilder(s.getName());
        if (s.getAddress().length() > 0) {
            out.append("  -  ").append(s.getAddress());
        }
        if (s.getEmail().length() > 0) {
            out.append("  -  ").append(s.getEmail());
        }
        if (s.getPhone().length() > 0) {
            out.append("  -  ").append(s.getPhone());
        }
        return out.toString();
    }

    static String paymentStatus(String financial) {
        if (Orders.PAID.equals(financial)) {
            return "Paid";
        }
        if (Orders.AUTHORIZED.equals(financial)) {
            return "Authorized";
        }
        if (Orders.PARTIALLY_REFUNDED.equals(financial)) {
            return "Partially refunded";
        }
        if (Orders.REFUNDED.equals(financial)) {
            return "Refunded";
        }
        if (Orders.VOIDED.equals(financial)) {
            return "Voided";
        }
        return "Payment due";
    }

    private static String paymentLine(StoreOrder o) {
        String f = o.getFinancial();
        if (Orders.PAID.equals(f) || Orders.PARTIALLY_REFUNDED.equals(f)
                || Orders.REFUNDED.equals(f)) {
            return "Paid in full. Nothing is due.";
        }
        if (Orders.AUTHORIZED.equals(f)) {
            return "Your card is authorized and is charged when the order ships.";
        }
        if (Orders.VOIDED.equals(f)) {
            return "This order was cancelled and nothing was charged.";
        }
        return "Amount due: " + Sheet.money(o.getTotalCents() - o.getRefundedCents(),
                o.getCurrency());
    }

    private String barcode(OrderLine l) {
        if (l == null) {
            return "";
        }
        Variant v = variants.findById(l.getVariantId());
        return v == null ? "" : v.getBarcode();
    }

    private static String title(OrderLine l) {
        return l.getVariantTitle().length() > 0 ? l.getTitle() + " - " + l.getVariantTitle()
                : l.getTitle();
    }

    private static String carrierName(Fulfillment f) {
        return f.getCarrier();
    }

    private static String grams(int g) {
        if (g <= 0) {
            return "";
        }
        if (g < 1000) {
            return g + " g";
        }
        int tenths = (g + 50) / 100;
        return tenths / 10 + "." + tenths % 10 + " kg";
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
