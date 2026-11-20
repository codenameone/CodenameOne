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
import com.codenameone.examples.bazaar.store.StoreAccess;

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
