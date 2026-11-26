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
package com.codenameone.examples.bazaar.trade;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.RestController;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.docs.Documents;
import com.codenameone.examples.bazaar.store.StoreAccess;

/// The invoice of a net-terms order as a PDF, with its due date: for the
/// store's staff, and for the buyers of the company it was sold to.
@RestController
public class TradeDocumentPages {
    private final Documents documents;
    private final StoreAccess access;
    private final Trade trade;

    public TradeDocumentPages(Documents documents, StoreAccess access, Trade trade) {
        this.documents = documents;
        this.access = access;
        this.trade = trade;
    }

    @GetMapping("/api/merchant/stores/{storeId}/net-terms/{orderId}/invoice.pdf")
    public HttpServer.Response invoice(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId) throws Exception {
        access.requireStaff(Caller.name(), storeId, StoreAccess.ANYONE);
        trade.netTerm(storeId, orderId);
        return pdf(storeId, orderId);
    }

    @GetMapping("/api/shop/stores/{storeId}/b2b/net-terms/{orderId}/invoice.pdf")
    public HttpServer.Response buyerInvoice(@PathVariable("storeId") String storeId,
            @PathVariable("orderId") String orderId) throws Exception {
        trade.myNetTerm(Codes.callerOrNull(), storeId, orderId);
        return pdf(storeId, orderId);
    }

    private HttpServer.Response pdf(String storeId, String orderId) throws Exception {
        int number = documents.order(storeId, orderId).getNumber();
        byte[] bytes = documents.invoice(storeId, orderId, trade.dueLine(storeId, orderId));
        return new HttpServer.Response(200, "application/pdf", bytes)
                .header("Content-Disposition", "inline; filename=\"invoice-" + number + ".pdf\"")
                .header("Cache-Control", "private, no-store")
                .header("X-Content-Type-Options", "nosniff");
    }
}
