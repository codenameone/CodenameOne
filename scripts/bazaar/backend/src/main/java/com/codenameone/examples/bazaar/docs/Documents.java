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
import com.codenameone.examples.bazaar.domain.Tender;
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
import com.codenameone.examples.bazaar.repository.TenderRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.web.StoreText;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The printable documents, made on the server with `com.codename1.pdf` --
/// the same classes an app uses on the device -- and answered as PDF bytes:
/// invoice, packing slip, 4x6 shipping label, purchase order, credit note
/// end-of-shift Z report and the 80 mm till receipt.
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
    private final TenderRepository tenders;
    private final com.codenameone.examples.bazaar.repository.ReturnRequestRepository returns;
    private final com.codenameone.examples.bazaar.repository.ReturnLineRepository returnLines;

    public Documents(StoreRepository stores, StoreOrderRepository orders,
            OrderLineRepository lines, LocationRepository locations, VariantRepository variants,
            ProductRepository products, PurchaseOrderRepository purchaseOrders,
            PurchaseLineRepository purchaseLines, SupplierRepository suppliers,
            RefundRepository refunds, RefundLineRepository refundLines, ShiftRepository shifts,
            RegisterRepository registers, CashMoveRepository cashMoves,
            Fulfillments fulfillments, TenderRepository tenders,
            com.codenameone.examples.bazaar.repository.ReturnRequestRepository returns,
            com.codenameone.examples.bazaar.repository.ReturnLineRepository returnLines) {
        this.returns = returns;
        this.returnLines = returnLines;
        this.tenders = tenders;
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
        return invoice(storeId, orderId, null);
    }

    /// [#invoice] with a line of payment terms (`Net 30, due 2026-12-21`)
    /// under the date, for an order placed on net terms; null for none.
    @Transactional(readOnly = true)
    public byte[] invoice(String storeId, String orderId, String terms) throws IOException {
        StoreOrder o = order(storeId, orderId);
        Store s = stores.findById(storeId);
        String cur = o.getCurrency();
        Sheet sheet = new Sheet(s.getName(), "INVOICE", "#" + o.getNumber(), s.getAccentColor(),
                footer(s));
        sheet.blocks("BILL TO", shipTo(o), "INVOICE", new String[] {
            "Invoice #" + o.getNumber(),
            "Date " + Sheet.date(millis(o.getCreatedAt())),
            "Status " + paymentStatus(o.getFinancial()),
            terms == null ? "" : terms,
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
        String link = payLink(orderId);
        if (link != null) {
            sheet.text("Pay online: " + link);
        }
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
        slip(sheet, storeId, o, fulfillmentId);
        return sheet.finish();
    }

    /// The packing slips of several orders, one after another, each on a
    /// page of its own. Every order must be the store's.
    @Transactional(readOnly = true)
    public byte[] packingSlips(String storeId, List<String> orderIds) throws IOException {
        if (orderIds == null || orderIds.isEmpty() || orderIds.size() > 200) {
            throw new ResponseStatusException(400, "Choose 1 to 200 orders");
        }
        List<StoreOrder> os = new java.util.ArrayList<StoreOrder>();
        for (int iter = 0; iter < orderIds.size(); iter++) {
            os.add(order(storeId, orderIds.get(iter)));
        }
        Store s = stores.findById(storeId);
        Sheet sheet = new Sheet(s.getName(), "PACKING SLIPS", os.size() + " orders",
                s.getAccentColor(), footer(s));
        for (int iter = 0; iter < os.size(); iter++) {
            if (iter > 0) {
                sheet.layout.addPageBreak();
            }
            sheet.heading("Order #" + os.get(iter).getNumber());
            slip(sheet, storeId, os.get(iter), null);
        }
        return sheet.finish();
    }

    private void slip(Sheet sheet, String storeId, StoreOrder o, String fulfillmentId)
            throws IOException {
        String orderId = o.getId();
        sheet.blocks("SHIP TO", shipTo(o), "ORDER", new String[] {
            "Order #" + o.getNumber(),
            "Placed " + Sheet.date(millis(o.getCreatedAt())),
            o.getShippingTitle(),
        });
        Sheet.Table t = sheet.table(new float[] {0.5f, 4.2f, 1.7f, 1.8f, 0.7f},
                new String[] {"", "Item", "SKU", "Barcode", "Qty"},
                new boolean[] {false, false, false, false, true});
        List<OrderLine> ls = lines.findByOrderId(orderId);
        List<OrderLine> picked = new java.util.ArrayList<OrderLine>();
        List<Integer> quantities = new java.util.ArrayList<Integer>();
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
                if (l != null) {
                    picked.add(l);
                    quantities.add(Integer.valueOf(fl.get(iter).quantity()));
                }
            }
        } else {
            for (int iter = 0; iter < ls.size(); iter++) {
                OrderLine l = ls.get(iter);
                int q = l.getQuantity() - l.getRefundedQty();
                if (q > 0) {
                    t.checkRow(title(l), l.getSku(), barcode(l), Integer.toString(q));
                    units += q;
                    picked.add(l);
                    quantities.add(Integer.valueOf(q));
                }
            }
        }
        t.end();
        sheet.text(units + (units == 1 ? " item" : " items") + " in this parcel.");
        if (fulfillments.international(o)) {
            customs(sheet, o, fulfillments.customsItems(o, picked, quantities));
        }
        if (o.getNote().length() > 0) {
            sheet.space(6).heading("Note").text(o.getNote());
        }
        sheet.space(10).muted("Packed by ____________________    Checked by ____________________");
    }

    /// The customs data of an international parcel: each item's HS code,
    /// country of origin and declared value, so the slip agrees with the
    /// declaration the label was bought with.
    private static void customs(Sheet sheet, StoreOrder o,
            List<com.codenameone.examples.bazaar.carrier.CarrierProvider.CustomsItem> items) {
        if (items.isEmpty()) {
            return;
        }
        sheet.space(6).heading("Customs");
        Sheet.Table t = sheet.table(new float[] {3.8f, 1.6f, 0.9f, 0.7f, 1.9f},
                new String[] {"Item", "HS code", "Origin", "Qty", "Value"},
                new boolean[] {false, false, false, true, true});
        long total = 0;
        for (int iter = 0; iter < items.size(); iter++) {
            com.codenameone.examples.bazaar.carrier.CarrierProvider.CustomsItem it =
                    items.get(iter);
            t.row(it.description, it.hsCode.length() > 0 ? it.hsCode : "-",
                    it.originCountry.length() > 0 ? it.originCountry : "-",
                    Integer.toString(it.quantity), Sheet.money(it.valueCents, o.getCurrency()));
            total += it.valueCents;
        }
        t.end();
        sheet.text("Declared value " + Sheet.money(total, o.getCurrency()) + ", shipped to "
                + o.getShipCountry() + ".");
    }

    // --------------------------------------------------------- return label

    /// The prepaid return label of an approved return, 4x6 inches: from the
    /// shopper, to the store's location, the carrier's tracking number as a
    /// Code 128 barcode.
    @Transactional(readOnly = true)
    public byte[] returnLabel(String storeId, String returnId) throws IOException {
        com.codenameone.examples.bazaar.domain.ReturnRequest r = returnId == null ? null
                : returns.findById(returnId);
        if (r == null || !storeId.equals(r.getStoreId())) {
            throw new ResponseStatusException(404, "No such return");
        }
        if (r.getReturnTracking().length() == 0) {
            throw new ResponseStatusException(409, "The return has no label");
        }
        StoreOrder o = orders.findById(r.getOrderId());
        Store s = stores.findById(storeId);
        List<OrderLine> ls = lines.findByOrderId(o.getId());
        List<com.codenameone.examples.bazaar.domain.ReturnLine> back =
                returnLines.findByReturnId(returnId);
        int grams = 0;
        Location to = null;
        for (int iter = 0; iter < back.size(); iter++) {
            OrderLine l = find(ls, back.get(iter).getOrderLineId());
            if (l != null) {
                grams += l.getWeightGrams() * back.get(iter).getQuantity();
                if (to == null && l.getLocationId().length() > 0) {
                    Location at = locations.findById(l.getLocationId());
                    to = at != null && storeId.equals(at.getStoreId()) ? at : null;
                }
            }
        }
        PdfDocument doc = new PdfDocument();
        doc.setTitle("Return label " + r.getReturnTracking()).setAuthor(s.getName())
                .setCreator("Bazaar");
        PdfPage page = doc.addPage(LABEL_W, LABEL_H);
        PdfCanvas c = page.getCanvas();
        float m = 12;
        float w = LABEL_W - 2 * m;
        c.setStrokeColor(0x000000).setLineWidth(1.5f).strokeRect(m / 2, m / 2, LABEL_W - m,
                LABEL_H - m);
        c.setFillColor(0x000000).setFont(PdfFont.HELVETICA_BOLD, 7).drawText("FROM", m, m + 12);
        String[] sender = shipTo(o);
        float y = m + 23;
        for (int iter = 0; iter < sender.length - 1; iter++) {
            if (sender[iter] != null && sender[iter].length() > 0) {
                c.setFont(iter == 0 ? PdfFont.HELVETICA_BOLD : PdfFont.HELVETICA, 8)
                        .drawText(Sheet.fit(sender[iter], PdfFont.HELVETICA, 8, w), m, y);
                y += 10;
            }
        }
        y = 86;
        c.fillRect(m / 2, y, LABEL_W - m, 34);
        c.setFillColor(0xffffff).setFont(PdfFont.HELVETICA_BOLD, 20).drawText("RETURN", m,
                y + 24);
        String weight = grams(grams);
        c.setFont(PdfFont.HELVETICA_BOLD, 10).drawText(weight, LABEL_W - m
                - PdfFont.HELVETICA_BOLD.getStringWidth(weight, 10), y + 21);
        c.setFillColor(0x000000).setFont(PdfFont.HELVETICA_BOLD, 8).drawText("RETURN TO", m,
                y + 52);
        String[] dest = {s.getName(), to == null ? "" : to.getName(),
            to == null || to.getAddress().length() == 0 ? s.getAddress() : to.getAddress()};
        y += 70;
        for (int iter = 0; iter < dest.length; iter++) {
            if (dest[iter] == null || dest[iter].length() == 0) {
                continue;
            }
            PdfFont font = iter == 0 ? PdfFont.HELVETICA_BOLD : PdfFont.HELVETICA;
            float size = iter == 0 ? 15 : 13;
            c.setFont(font, size).drawText(Sheet.fit(dest[iter], font, size, w), m + 8, y);
            y += size + 4;
        }
        float barTop = 286;
        c.setLineWidth(1).drawLine(m / 2, barTop - 14, LABEL_W - m / 2, barTop - 14);
        c.setFont(PdfFont.HELVETICA_BOLD, 8).drawText("TRACKING #  " + r.getReturnCarrier()
                + "   POSTAGE PAID", m, barTop - 2);
        Code128.draw(c, r.getReturnTracking(), m, barTop + 4, w, 74);
        String number = r.getReturnTracking();
        c.setFont(PdfFont.COURIER_BOLD, 16);
        float nw = PdfFont.COURIER_BOLD.getStringWidth(number, 16);
        c.drawText(number, (LABEL_W - nw) / 2, barTop + 100);
        c.setFont(PdfFont.HELVETICA, 7).drawText("Return for order #" + o.getNumber() + "   "
                + Sheet.date(millis(r.getUpdatedAt())), m, LABEL_H - m - 4);
        return doc.toByteArray();
    }

    // --------------------------------------------------------- customs form

    /// Declared value (in minor units) above which a parcel needs a CN 23
    /// rather than a CN 22: the UPU sets the line at 300 SDR, given here per
    /// currency, rounded down, so a parcel near the line gets the fuller form.
    private static final String[][] CN22_LIMITS = {
        {"USD", "40000"}, {"EUR", "36000"}, {"GBP", "31000"}, {"CAD", "54000"},
        {"AUD", "60000"}, {"ILS", "145000"}, {"JPY", "6000000"},
    };
    /// A CN 22 goes on a parcel of at most 2 kg.
    static final int CN22_MAX_GRAMS = 2000;

    /// Whether a parcel of `valueCents` in `currency` weighing `grams` is
    /// declared on a CN 22 (else a CN 23). A currency missing from the table
    /// takes the lowest limit.
    public static boolean cn22(long valueCents, String currency, int grams) {
        long limit = 31000;
        for (int iter = 0; iter < CN22_LIMITS.length; iter++) {
            if (CN22_LIMITS[iter][0].equalsIgnoreCase(currency)) {
                limit = Long.parseLong(CN22_LIMITS[iter][1]);
            }
        }
        return valueCents <= limit && grams <= CN22_MAX_GRAMS;
    }

    /// The customs declaration of an international shipment: CN 22 for a
    /// small, low value parcel, CN 23 otherwise. Each item with its HS code
    /// (from the variant, else the product), country of origin, net weight
    /// and value, the parcel's totals, the category (sale of goods) and the
    /// sender's certification.
    @Transactional(readOnly = true)
    public byte[] customsForm(String storeId, String fulfillmentId) throws IOException {
        Fulfillment f = fulfillments.require(storeId, fulfillmentId);
        StoreOrder o = orders.findById(f.getOrderId());
        if (!fulfillments.international(o)) {
            throw new ResponseStatusException(409, "The parcel does not cross a border");
        }
        Store s = stores.findById(storeId);
        List<OrderLine> ls = lines.findByOrderId(o.getId());
        List<FulfilLineDto> fl = fulfillments.linesOf(f);
        List<OrderLine> picked = new java.util.ArrayList<OrderLine>();
        List<Integer> quantities = new java.util.ArrayList<Integer>();
        for (int iter = 0; iter < fl.size(); iter++) {
            OrderLine l = find(ls, fl.get(iter).orderLineId());
            if (l != null) {
                picked.add(l);
                quantities.add(Integer.valueOf(fl.get(iter).quantity()));
            }
        }
        List<com.codenameone.examples.bazaar.carrier.CarrierProvider.CustomsItem> items =
                fulfillments.customsItems(o, picked, quantities);
        long value = 0;
        int grams = 0;
        for (int iter = 0; iter < items.size(); iter++) {
            value += items.get(iter).valueCents;
            grams += items.get(iter).weightGrams;
        }
        boolean small = cn22(value, o.getCurrency(), Math.max(grams, f.getWeightGrams()));
        Location from = locations.findById(f.getLocationId());
        Sheet sheet = new Sheet(s.getName(), small ? "CN 22" : "CN 23",
                "CUSTOMS DECLARATION  -  Order #" + o.getNumber(), s.getAccentColor(), footer(s));
        sheet.text("May be opened officially.");
        sheet.blocks("SENDER", new String[] {s.getName(),
            from == null || from.getAddress().length() == 0 ? s.getAddress() : from.getAddress()},
            "ADDRESSEE", shipTo(o));
        sheet.text("Category of item: [X] Sale of goods   [ ] Gift   [ ] Documents   "
                + "[ ] Commercial sample   [ ] Returned goods   [ ] Other");
        Sheet.Table t = sheet.table(new float[] {3.2f, 0.6f, 1.1f, 1.6f, 1.5f, 0.9f},
                new String[] {"Detailed description of contents", "Qty", "Net weight",
                    "Value", "HS tariff number", "Origin"},
                new boolean[] {false, true, true, true, false, false});
        for (int iter = 0; iter < items.size(); iter++) {
            com.codenameone.examples.bazaar.carrier.CarrierProvider.CustomsItem it =
                    items.get(iter);
            t.row(it.description.length() > 0 ? it.description : "Merchandise",
                    Integer.toString(it.quantity), grams(it.weightGrams),
                    Sheet.money(it.valueCents, o.getCurrency()),
                    it.hsCode.length() > 0 ? it.hsCode : "-",
                    it.originCountry.length() > 0 ? it.originCountry : "-");
        }
        t.end();
        sheet.totals(new String[][] {
            {"Total gross weight", grams(Math.max(grams, f.getWeightGrams()))},
            {"Total value", Sheet.money(value, o.getCurrency())},
        }, true);
        if (!small) {
            sheet.space(6).text("Invoice: order #" + o.getNumber() + ", "
                    + Sheet.date(millis(o.getCreatedAt())) + ".   Postage: "
                    + Sheet.money(o.getShippingCents(), o.getCurrency())
                    + (f.getTrackingNumber().length() > 0 ? ".   Tracking: "
                        + f.getTrackingNumber() : "."));
            sheet.text("In case of non-delivery: [X] Return to sender   [ ] Treat as abandoned");
        }
        sheet.space(10).muted("I certify that the particulars given in this customs "
                + "declaration are correct and that this item does not contain any dangerous "
                + "article prohibited by legislation or by postal or customs regulations.");
        sheet.space(6).muted("Date and sender's signature: " + Sheet.date(System.currentTimeMillis())
                + "   ____________________");
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

    // ------------------------------------------------------- price labels

    /// A shelf label, 2.25 x 1.25 inches, in points.
    static final float TAG_W = 162;
    static final float TAG_H = 90;
    /// Most labels in one document.
    public static final int MAX_TAGS = 500;

    /// Price labels for the store's variants, `copies` of each, one per page
    /// of a label printer's roll: the product, the variant, the price and the
    /// barcode (the SKU, or the variant's id, when it has none) as Code 128.
    @Transactional(readOnly = true)
    public byte[] priceLabels(String storeId, List<String> variantIds, int copies)
            throws IOException {
        if (variantIds == null || variantIds.isEmpty()) {
            throw new ResponseStatusException(400, "Name the variants to label");
        }
        if (copies <= 0 || copies > 100 || variantIds.size() * (long) copies > MAX_TAGS) {
            throw new ResponseStatusException(400, "At most " + MAX_TAGS + " labels at once");
        }
        Store s = stores.findById(storeId);
        PdfDocument doc = new PdfDocument();
        doc.setTitle("Price labels").setAuthor(s.getName()).setCreator("Bazaar");
        float m = 6;
        float w = TAG_W - 2 * m;
        for (int iter = 0; iter < variantIds.size(); iter++) {
            Variant v = variants.findById(variantIds.get(iter));
            if (v == null || !storeId.equals(v.getStoreId())) {
                throw new ResponseStatusException(404, "No such variant");
            }
            Product p = products.findById(v.getProductId());
            String name = p == null ? "" : p.getTitle();
            String code = v.getBarcode().length() > 0 ? v.getBarcode()
                    : v.getSku().length() > 0 ? v.getSku() : v.getId();
            String price = Sheet.money(v.getPriceCents(), s.getCurrency());
            for (int copy = 0; copy < copies; copy++) {
                PdfCanvas c = doc.addPage(TAG_W, TAG_H).getCanvas();
                c.setFillColor(0x000000).setFont(PdfFont.HELVETICA_BOLD, 8)
                        .drawText(Sheet.fit(name, PdfFont.HELVETICA_BOLD, 8, w), m, m + 7);
                if (v.getTitle().length() > 0) {
                    c.setFont(PdfFont.HELVETICA, 7)
                            .drawText(Sheet.fit(v.getTitle(), PdfFont.HELVETICA, 7, w), m, m + 16);
                }
                c.setFont(PdfFont.HELVETICA_BOLD, 13);
                c.drawText(price, TAG_W - m - PdfFont.HELVETICA_BOLD.getStringWidth(price, 13),
                        m + 31);
                Code128.draw(c, code, m, 46, w, 28);
                c.setFont(PdfFont.COURIER, 6).drawText(Sheet.fit(code, PdfFont.COURIER, 6, w), m,
                        TAG_H - m);
            }
        }
        return doc.toByteArray();
    }

    // ---------------------------------------------------------- label sheets

    /// Avery 5160 and compatible: US letter, 3 columns of 10 labels, each
    /// 2 5/8 by 1 inch, 3/16 inch side margins, 1/2 inch top margin and an
    /// 1/8 inch gap between columns.
    public static final String AVERY_30 = "AVERY_30";
    /// A roll of 2 by 1 inch thermal labels, one label to a page.
    public static final String THERMAL_2X1 = "THERMAL_2X1";
    static final float LETTER_W = 612;
    static final float LETTER_H = 792;
    static final float AVERY_W = 189;
    static final float AVERY_H = 72;
    static final float AVERY_LEFT = 13.5f;
    static final float AVERY_TOP = 36;
    static final float AVERY_PITCH = 198;
    static final float THERMAL_W = 144;
    static final float THERMAL_H = 72;

    /// Barcode labels: `quantities[i]` labels of `variantIds[i]`, each with
    /// the product's title, the variant, the price and the barcode (or SKU)
    /// as Code 128, laid out for `layout` ([#AVERY_30], the default, or
    /// [#THERMAL_2X1]).
    @Transactional(readOnly = true)
    public byte[] labelSheet(String storeId, List<String> variantIds, List<Integer> quantities,
            String layout) throws IOException {
        boolean avery = layout == null || layout.length() == 0 || AVERY_30.equals(layout);
        if (!avery && !THERMAL_2X1.equals(layout)) {
            throw new ResponseStatusException(400, "A sheet is AVERY_30 or THERMAL_2X1");
        }
        if (variantIds == null || variantIds.isEmpty()
                || quantities == null || quantities.size() != variantIds.size()) {
            throw new ResponseStatusException(400, "Name the variants and how many of each");
        }
        long total = 0;
        for (int iter = 0; iter < quantities.size(); iter++) {
            int q = quantities.get(iter).intValue();
            if (q < 0) {
                throw new ResponseStatusException(400, "A quantity cannot be negative");
            }
            total += q;
        }
        if (total == 0 || total > MAX_TAGS) {
            throw new ResponseStatusException(400, "1 to " + MAX_TAGS + " labels at once");
        }
        Store s = stores.findById(storeId);
        PdfDocument doc = new PdfDocument();
        doc.setTitle("Barcode labels").setAuthor(s.getName()).setCreator("Bazaar");
        PdfCanvas page = null;
        int slot = 0;
        for (int iter = 0; iter < variantIds.size(); iter++) {
            Variant v = variants.findById(variantIds.get(iter));
            if (v == null || !storeId.equals(v.getStoreId())) {
                throw new ResponseStatusException(404, "No such variant");
            }
            Product p = products.findById(v.getProductId());
            String name = p == null ? "" : p.getTitle();
            String code = v.getBarcode().length() > 0 ? v.getBarcode()
                    : v.getSku().length() > 0 ? v.getSku() : v.getId();
            String price = Sheet.money(v.getPriceCents(), s.getCurrency());
            for (int copy = 0; copy < quantities.get(iter).intValue(); copy++) {
                if (avery) {
                    int at = slot % 30;
                    if (at == 0) {
                        page = doc.addPage(LETTER_W, LETTER_H).getCanvas();
                    }
                    label(page, AVERY_LEFT + (at % 3) * AVERY_PITCH, AVERY_TOP + (at / 3) * AVERY_H,
                            AVERY_W, AVERY_H, name, v.getTitle(), price, code);
                } else {
                    label(doc.addPage(THERMAL_W, THERMAL_H).getCanvas(), 0, 0, THERMAL_W,
                            THERMAL_H, name, v.getTitle(), price, code);
                }
                slot++;
            }
        }
        return doc.toByteArray();
    }

    /// One label in the box at `x`, `y`: the title and the price on the first
    /// line, the variant under them, then the barcode and its text.
    private static void label(PdfCanvas c, float x, float y, float w, float h, String name,
            String variant, String price, String code) {
        float m = 6;
        float inner = w - 2 * m;
        float pw = PdfFont.HELVETICA_BOLD.getStringWidth(price, 9);
        c.setFillColor(0x000000).setFont(PdfFont.HELVETICA_BOLD, 7.5f)
                .drawText(Sheet.fit(name, PdfFont.HELVETICA_BOLD, 7.5f, inner - pw - 4), x + m,
                        y + m + 6);
        c.setFont(PdfFont.HELVETICA_BOLD, 9).drawText(price, x + w - m - pw, y + m + 7);
        if (variant.length() > 0) {
            c.setFont(PdfFont.HELVETICA, 6.5f)
                    .drawText(Sheet.fit(variant, PdfFont.HELVETICA, 6.5f, inner), x + m,
                            y + m + 14);
        }
        Code128.draw(c, code, x + m, y + 25, inner, h - 25 - 14);
        c.setFont(PdfFont.COURIER, 6).drawText(Sheet.fit(code, PdfFont.COURIER, 6, inner),
                x + m, y + h - 5);
    }

    /// Labels for what a purchase order brought in: one per unit received,
    /// or per unit ordered while nothing has arrived.
    @Transactional(readOnly = true)
    public byte[] purchaseOrderLabels(String storeId, String purchaseOrderId, String layout)
            throws IOException {
        PurchaseOrder po = purchaseOrderId == null ? null
                : purchaseOrders.findById(purchaseOrderId);
        if (po == null || !storeId.equals(po.getStoreId())) {
            throw new ResponseStatusException(404, "No such purchase order");
        }
        List<PurchaseLine> pl = purchaseLines.findByPurchaseOrderId(po.getId());
        boolean anyReceived = false;
        for (int iter = 0; iter < pl.size(); iter++) {
            anyReceived |= pl.get(iter).getReceived() > 0;
        }
        List<String> ids = new java.util.ArrayList<String>();
        List<Integer> qty = new java.util.ArrayList<Integer>();
        for (int iter = 0; iter < pl.size(); iter++) {
            int q = anyReceived ? pl.get(iter).getReceived() : pl.get(iter).getQuantity();
            if (q > 0 && pl.get(iter).getVariantId().length() > 0) {
                ids.add(pl.get(iter).getVariantId());
                qty.add(Integer.valueOf(q));
            }
        }
        if (ids.isEmpty()) {
            throw new ResponseStatusException(409, "The purchase order has nothing to label");
        }
        return labelSheet(storeId, ids, qty, layout);
    }

    /// The active checkout link of an order, for its invoice; null for none.
    private String payLink(String orderId) throws IOException {
        java.util.Map r = db.queryOne("SELECT token FROM pay_link WHERE order_id = ? AND "
                + "status = ? AND expires_at > ?", new Object[] {orderId, "ACTIVE",
                    Long.valueOf(System.currentTimeMillis())});
        return r == null ? null : urls.absolute(null, "/pay/"
                + com.codenameone.examples.bazaar.Rows.text(r.get("token")));
    }

    @com.codename1.backend.annotations.Autowired
    private com.codename1.backend.DataSource db;
    @com.codename1.backend.annotations.Autowired
    private com.codenameone.examples.bazaar.web.SiteUrls urls;

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

    // -------------------------------------------------------------- receipt

    /// An 80 mm roll is 226.77 points wide; the printable band is narrower.
    static final float ROLL_W = 226.77f;
    /// Characters per line of the text receipt, which is also how the PDF is
    /// laid out: one monospaced row of this many columns per line.
    static final int ROLL_COLUMNS = 40;

    /// The receipt of an order as rows of `{left, right}` text: the store,
    /// the lines, the totals, how it was paid and the change.
    @Transactional(readOnly = true)
    public List<String[]> receiptRows(String storeId, String orderId) throws IOException {
        return receiptRows(storeId, orderId, "en");
    }

    /// The receipt rows with their words in `lang` ([StoreText#LANGUAGES]),
    /// for a receipt that goes to the shopper rather than onto the till.
    @Transactional(readOnly = true)
    public List<String[]> receiptRows(String storeId, String orderId, String lang)
            throws IOException {
        Map<String, String> words = StoreText.table(StoreText.supported(lang) ? lang : "en");
        StoreOrder o = order(storeId, orderId);
        Store s = stores.findById(storeId);
        String cur = o.getCurrency();
        List<String[]> rows = new ArrayList<String[]>();
        rows.add(new String[] {"=" + s.getName(), ""});
        if (s.getAddress().length() > 0) {
            rows.add(new String[] {"=" + s.getAddress(), ""});
        }
        Location at = o.getLocationId().length() == 0 ? null : locations.findById(o.getLocationId());
        if (at != null) {
            rows.add(new String[] {"=" + at.getName(), ""});
        }
        rows.add(new String[] {"-", ""});
        rows.add(new String[] {fmt(words, "receipt.order", o.getNumber()), Sheet.dateTime(millis(o.getCreatedAt()))});
        if (o.getStaffUsername().length() > 0) {
            rows.add(new String[] {fmt(words, "receipt.servedBy", o.getStaffUsername()), ""});
        }
        rows.add(new String[] {"-", ""});
        List<OrderLine> ls = lines.findByOrderId(orderId);
        for (int iter = 0; iter < ls.size(); iter++) {
            OrderLine l = ls.get(iter);
            rows.add(new String[] {title(l), Sheet.money(l.getPriceCents() * l.getQuantity(), cur)});
            if (l.getQuantity() != 1) {
                rows.add(new String[] {"  " + l.getQuantity() + " x " + Sheet.money(l.getPriceCents(),
                    cur), ""});
            }
            if (l.getDiscountCents() > 0) {
                rows.add(new String[] {"  " + fmt(words, "discount"), Sheet.money(-l.getDiscountCents(), cur)});
            }
            if (l.getRefundedQty() > 0) {
                rows.add(new String[] {"  " + fmt(words, "receipt.returned", l.getRefundedQty()), ""});
            }
        }
        rows.add(new String[] {"-", ""});
        rows.add(new String[] {fmt(words, "subtotal"), Sheet.money(o.getSubtotalCents(), cur)});
        if (o.getDiscountCents() > 0) {
            rows.add(new String[] {fmt(words, "receipt.discounts"), Sheet.money(-o.getDiscountCents(), cur)});
        }
        if (o.getShippingCents() > 0) {
            rows.add(new String[] {fmt(words, "shipping"), Sheet.money(o.getShippingCents(), cur)});
        }
        rows.add(new String[] {s.isTaxInclusive() ? fmt(words, "tax.included") : fmt(words, "tax"),
            Sheet.money(o.getTaxCents(), cur)});
        rows.add(new String[] {"*" + fmt(words, "receipt.total"), Sheet.money(o.getTotalCents(), cur)});
        List<Tender> ts = tenders.findByOrderId(orderId);
        long change = 0;
        for (int iter = 0; iter < ts.size(); iter++) {
            Tender t = ts.get(iter);
            String kind = t.getKind();
            String label = "CASH".equals(kind) ? fmt(words, "receipt.cash") : "CARD".equals(kind)
                    ? fmt(words, "receipt.card") : "GIFT_CARD".equals(kind) ? fmt(words, "receipt.giftCard")
                    : "STORE_CREDIT".equals(kind) ? fmt(words, "receipt.storeCredit") : kind;
            if (t.getReference().length() > 0 && "GIFT_CARD".equals(kind)) {
                label += " ..." + t.getReference();
            }
            rows.add(new String[] {label, Sheet.money(t.getAmountCents() + t.getChangeCents(), cur)});
            change += t.getChangeCents();
        }
        if (change > 0) {
            rows.add(new String[] {fmt(words, "receipt.change"), Sheet.money(change, cur)});
        }
        if (o.getRefundedCents() > 0) {
            rows.add(new String[] {fmt(words, "receipt.refunded"), Sheet.money(-o.getRefundedCents(), cur)});
        }
        rows.add(new String[] {"-", ""});
        rows.add(new String[] {"=" + fmt(words, "receipt.thanks"), ""});
        return rows;
    }

    /// The receipt as plain text, [#ROLL_COLUMNS] wide, for a text message
    /// or an e-mail body.
    public String receiptText(String storeId, String orderId) throws IOException {
        return receiptText(storeId, orderId, "en");
    }

    /// [#receiptText] with its words in `lang`: the receipt texted to a
    /// shopper, in the shopper's language.
    public String receiptText(String storeId, String orderId, String lang) throws IOException {
        List<String[]> rows = receiptRows(storeId, orderId, lang);
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.append(textRow(rows.get(iter))).append('\n');
        }
        return out.toString();
    }

    private static String fmt(Map<String, String> t, String key, Object... args) {
        return StoreText.format(t, key, args);
    }

    private static String textRow(String[] row) {
        String left = row[0];
        if ("-".equals(left)) {
            StringBuilder dash = new StringBuilder();
            for (int iter = 0; iter < ROLL_COLUMNS; iter++) {
                dash.append('-');
            }
            return dash.toString();
        }
        if (left.startsWith("=")) {
            String text = cutText(left.substring(1), ROLL_COLUMNS);
            StringBuilder pad = new StringBuilder();
            for (int iter = 0; iter < (ROLL_COLUMNS - text.length()) / 2; iter++) {
                pad.append(' ');
            }
            return pad + text;
        }
        if (left.startsWith("*")) {
            left = left.substring(1);
        }
        String right = row[1];
        left = cutText(left, Math.max(1, ROLL_COLUMNS - right.length() - 1));
        StringBuilder out = new StringBuilder(left);
        while (out.length() + right.length() < ROLL_COLUMNS) {
            out.append(' ');
        }
        return out.append(right).toString();
    }

    private static String cutText(String text, int max) {
        return text.length() <= max ? text : text.substring(0, Math.max(0, max - 1)) + "~";
    }

    /// The till receipt as a PDF on an 80 mm roll: one page as long as the
    /// receipt, the same rows as [#receiptText] in a monospaced face so it
    /// prints the same on a thermal printer as on paper.
    public byte[] receipt(String storeId, String orderId) throws IOException {
        List<String[]> rows = receiptRows(storeId, orderId);
        StoreOrder o = order(storeId, orderId);
        float size = 7.5f;
        float lead = 10;
        float m = 10;
        float height = 2 * m + 10 + rows.size() * lead;
        PdfDocument doc = new PdfDocument();
        doc.setTitle("Receipt " + o.getNumber()).setCreator("Bazaar");
        PdfPage page = doc.addPage(ROLL_W, height);
        PdfCanvas c = page.getCanvas();
        c.setFillColor(0x000000);
        float y = m + 10;
        for (int iter = 0; iter < rows.size(); iter++) {
            String[] row = rows.get(iter);
            String left = row[0];
            if ("-".equals(left)) {
                c.setStrokeColor(0x000000).setLineWidth(0.5f).drawLine(m, y - 3, ROLL_W - m, y - 3);
            } else if (left.startsWith("=")) {
                String text = Sheet.fit(left.substring(1), PdfFont.HELVETICA_BOLD, size + 1,
                        ROLL_W - 2 * m);
                float w = PdfFont.HELVETICA_BOLD.getStringWidth(text, size + 1);
                c.setFont(PdfFont.HELVETICA_BOLD, size + 1).drawText(text, (ROLL_W - w) / 2, y);
            } else {
                boolean strong = left.startsWith("*");
                PdfFont f = strong ? PdfFont.COURIER_BOLD : PdfFont.COURIER;
                String text = strong ? left.substring(1) : left;
                float rw = f.getStringWidth(row[1], size);
                c.setFont(f, size).drawText(Sheet.fit(text, f, size, ROLL_W - 2 * m - rw - 6), m, y);
                c.drawText(row[1], ROLL_W - m - rw, y);
            }
            y += lead;
        }
        return doc.toByteArray();
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
