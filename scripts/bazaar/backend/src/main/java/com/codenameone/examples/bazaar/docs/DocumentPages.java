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

import com.codename1.appkit.server.Caller;
import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.RestController;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.ops.Perms;
import com.codenameone.examples.bazaar.store.StoreAccess;

import java.util.ArrayList;
import java.util.List;
/// The documents as real PDF responses, so a browser, a print dialog or the
/// app's PDF viewer opens the address as it is. All sit under `/api`, behind
/// the same sign-in as the JSON API: the merchant ones need staff of the
/// store, and the shopper's invoice needs the order to be the caller's.
@RestController
public class DocumentPages {
    private final Documents documents;
    private final StoreAccess access;
    private final Orders orders;

    public DocumentPages(Documents documents, StoreAccess access, Orders orders) {
        this.documents = documents;
        this.access = access;
        this.orders = orders;
    }

    @GetMapping("/api/merchant/stores/{storeId}/orders/{orderId}/invoice.pdf")
    public HttpServer.Response invoice(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId) throws Exception {
        access.requireStaff(Caller.name(), storeId, StoreAccess.ANYONE);
        int number = documents.order(storeId, orderId).getNumber();
        return pdf(documents.invoice(storeId, orderId), "invoice-" + number);
    }

    @GetMapping("/api/merchant/stores/{storeId}/orders/{orderId}/packing-slip.pdf")
    public HttpServer.Response packingSlip(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId,
            @RequestParam(value = "fulfillment", required = false) String fulfillmentId)
            throws Exception {
        access.requireStaff(Caller.name(), storeId, StoreAccess.ANYONE);
        int number = documents.order(storeId, orderId).getNumber();
        return pdf(documents.packingSlip(storeId, orderId, fulfillmentId),
                "packing-slip-" + number);
    }

    @GetMapping("/api/merchant/stores/{storeId}/fulfillments/{id}/label.pdf")
    public HttpServer.Response label(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id) throws Exception {
        access.requireStaff(Caller.name(), storeId, StoreAccess.STOCK);
        return pdf(documents.label(storeId, id), "label-" + id);
    }

    @GetMapping("/api/merchant/stores/{storeId}/fulfillments/{id}/customs.pdf")
    public HttpServer.Response customs(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id) throws Exception {
        access.requireStaff(Caller.name(), storeId, StoreAccess.STOCK);
        return pdf(documents.customsForm(storeId, id), "customs-" + id);
    }

    @GetMapping("/api/merchant/stores/{storeId}/returns/{id}/return-label.pdf")
    public HttpServer.Response returnLabel(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id) throws Exception {
        access.requireStaff(Caller.name(), storeId, StoreAccess.STOCK);
        return pdf(documents.returnLabel(storeId, id), "return-label-" + id);
    }

    @GetMapping("/api/merchant/stores/{storeId}/purchase-orders/{id}/po.pdf")
    public HttpServer.Response purchaseOrder(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id) throws Exception {
        access.requireStaff(Caller.name(), storeId, StoreAccess.STOCK);
        return pdf(documents.purchaseOrder(storeId, id), "purchase-order-" + id);
    }

    @GetMapping("/api/merchant/stores/{storeId}/refunds/{id}/credit-note.pdf")
    public HttpServer.Response creditNote(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id) throws Exception {
        access.requireStaff(Caller.name(), storeId, StoreAccess.ANYONE);
        return pdf(documents.creditNote(storeId, id), "credit-note-" + id);
    }

    @GetMapping("/api/merchant/stores/{storeId}/shifts/{id}/report.pdf")
    public HttpServer.Response shiftReport(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id) throws Exception {
        access.requireStaff(Caller.name(), storeId, StoreAccess.ANYONE);
        return pdf(documents.shiftReport(storeId, id), "z-report-" + id);
    }

    /// The 80 mm till receipt of an order, for any of the store's staff.
    @GetMapping("/api/merchant/stores/{storeId}/orders/{orderId}/receipt.pdf")
    public HttpServer.Response receipt(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId) throws Exception {
        access.requireStaff(Caller.name(), storeId, StoreAccess.ANYONE);
        int number = documents.order(storeId, orderId).getNumber();
        return pdf(documents.receipt(storeId, orderId), "receipt-" + number);
    }

    /// Price labels with barcodes for `variants` (ids separated by commas),
    /// `copies` of each, for any of the store's staff.
    @GetMapping("/api/merchant/stores/{storeId}/pos/labels.pdf")
    public HttpServer.Response priceLabels(@PathVariable("storeId") String storeId,
            @RequestParam(value = "variants", required = false) String variants,
            @RequestParam(value = "copies", required = false) String copies) throws Exception {
        access.requireStaff(Caller.name(), storeId, StoreAccess.ANYONE);
        return pdf(documents.priceLabels(storeId, ids(variants), count(copies)), "labels");
    }

    /// Barcode labels for a sheet or a thermal roll. `items` is
    /// `variantId:quantity` pairs separated by commas; `layout` is
    /// `AVERY_30` (the default) or `THERMAL_2X1`. For the store's
    /// inventory staff.
    @GetMapping("/api/merchant/stores/{storeId}/label-sheets.pdf")
    public HttpServer.Response labelSheet(@PathVariable("storeId") String storeId,
            @RequestParam(value = "items", required = false) String items,
            @RequestParam(value = "layout", required = false) String layout) throws Exception {
        access.requireArea(Caller.name(), storeId, Perms.INVENTORY, false, StoreAccess.ANYONE);
        List<String> pairs = items == null || items.length() > 20000 ? new ArrayList<String>()
                : com.codenameone.examples.bazaar.content.Lists.split(items);
        List<String> variants = new ArrayList<String>();
        List<Integer> quantities = new ArrayList<Integer>();
        for (int iter = 0; iter < pairs.size(); iter++) {
            String pair = pairs.get(iter);
            int colon = pair.indexOf(':');
            variants.add(colon < 0 ? pair : pair.substring(0, colon));
            quantities.add(Integer.valueOf(colon < 0 ? 1 : count(pair.substring(colon + 1))));
        }
        return pdf(documents.labelSheet(storeId, variants, quantities, layout), "labels");
    }

    /// Barcode labels for what a purchase order brought in.
    @GetMapping("/api/merchant/stores/{storeId}/purchase-orders/{purchaseOrderId}/labels.pdf")
    public HttpServer.Response purchaseOrderLabels(@PathVariable("storeId") String storeId,
            @PathVariable("purchaseOrderId") String purchaseOrderId,
            @RequestParam(value = "layout", required = false) String layout) throws Exception {
        access.requireArea(Caller.name(), storeId, Perms.INVENTORY, false, StoreAccess.ANYONE);
        return pdf(documents.purchaseOrderLabels(storeId, purchaseOrderId, layout), "labels");
    }

    /// The packing slips of the orders in `orders` (ids separated by commas),
    /// one document, for the store's staff.
    @GetMapping("/api/merchant/stores/{storeId}/bulk-orders/packing-slips.pdf")
    public HttpServer.Response packingSlips(@PathVariable("storeId") String storeId,
            @RequestParam(value = "orders", required = false) String orders) throws Exception {
        access.requireStaff(Caller.name(), storeId, StoreAccess.ANYONE);
        return pdf(documents.packingSlips(storeId, ids(orders)), "packing-slips");
    }

    /// The ids of a comma separated list, blanks dropped.
    static List<String> ids(String text) {
        List<String> out = new ArrayList<String>();
        if (text == null) {
            return out;
        }
        int start = 0;
        for (int iter = 0; iter <= text.length(); iter++) {
            if (iter == text.length() || text.charAt(iter) == ',') {
                String id = text.substring(start, iter).trim();
                if (id.length() > 0 && id.length() <= 32) {
                    out.add(id);
                }
                start = iter + 1;
            }
        }
        return out;
    }

    static int count(String text) {
        if (text == null || text.trim().length() == 0) {
            return 1;
        }
        String t = text.trim();
        int n = 0;
        for (int iter = 0; iter < t.length(); iter++) {
            char ch = t.charAt(iter);
            if (ch < '0' || ch > '9' || n > 1000) {
                return -1;
            }
            n = n * 10 + (ch - '0');
        }
        return n;
    }

    /// The shopper's own invoice; 404 for anybody else's order.
    @GetMapping("/api/orders/{orderId}/invoice.pdf")
    public HttpServer.Response myInvoice(@PathVariable("orderId") String orderId)
            throws Exception {
        OrderDto mine = orders.mineDetail(Caller.name(), orderId);
        return pdf(documents.invoice(mine.storeId(), orderId), "invoice-" + mine.number());
    }

    private static HttpServer.Response pdf(byte[] bytes, String name) {
        return new HttpServer.Response(200, "application/pdf", bytes)
                .header("Content-Disposition", "inline; filename=\"" + name + ".pdf\"")
                // An invoice carries an address: no shared cache keeps it.
                .header("Cache-Control", "private, no-store")
                .header("X-Content-Type-Options", "nosniff");
    }
}
