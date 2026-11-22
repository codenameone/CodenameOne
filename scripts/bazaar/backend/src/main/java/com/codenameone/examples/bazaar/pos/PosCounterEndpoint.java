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

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.CardPaymentDto;
import com.codenameone.examples.bazaar.api.CardReaderDto;
import com.codenameone.examples.bazaar.api.PosCounterApiServer;
import com.codenameone.examples.bazaar.api.PosPickupDto;
import com.codenameone.examples.bazaar.api.PosSaleResultDto;
import com.codenameone.examples.bazaar.api.PosShipQuoteDto;
import com.codenameone.examples.bazaar.api.PosShipSaleDto;
import com.codenameone.examples.bazaar.api.PosShipOptionDto;
import java.util.List;

/// The server's half of `PosCounterApi`.
@Component
public class PosCounterEndpoint implements PosCounterApiServer {
    private final CardReaders readers;
    private final Pickups pickups;
    private final PosSales sales;

    public PosCounterEndpoint(CardReaders readers, Pickups pickups, PosSales sales) {
        this.readers = readers;
        this.pickups = pickups;
        this.sales = sales;
    }

    @Override
    public List<CardReaderDto> readers(String storeId) throws Exception {
        return readers.readers(Caller.name(), storeId);
    }

    @Override
    public CardReaderDto addReader(String storeId, CardReaderDto body) throws Exception {
        return readers.addReader(Caller.name(), storeId, body);
    }

    @Override
    public CardReaderDto removeReader(String storeId, String readerId) throws Exception {
        return readers.removeReader(Caller.name(), storeId, readerId);
    }

    @Override
    public CardPaymentDto startCardPayment(String storeId, String registerId, CardPaymentDto body)
            throws Exception {
        return readers.start(Caller.name(), storeId, registerId, body);
    }

    @Override
    public CardPaymentDto cardPayment(String storeId, String paymentId) throws Exception {
        return readers.payment(Caller.name(), storeId, paymentId);
    }

    @Override
    public CardPaymentDto cancelCardPayment(String storeId, String paymentId) throws Exception {
        return readers.cancel(Caller.name(), storeId, paymentId);
    }

    @Override
    public List<PosPickupDto> pickups(String storeId, String registerId) throws Exception {
        return pickups.queue(Caller.name(), storeId, registerId);
    }

    @Override
    public PosPickupDto pickupReady(String storeId, String orderId) throws Exception {
        return pickups.ready(Caller.name(), storeId, orderId);
    }

    @Override
    public PosPickupDto pickedUp(String storeId, String orderId) throws Exception {
        return pickups.pickedUp(Caller.name(), storeId, orderId);
    }

    @Override
    public List<PosShipOptionDto> shipQuote(String storeId, String registerId,
            PosShipQuoteDto body) throws Exception {
        return sales.shipQuote(Caller.name(), storeId, registerId, body);
    }

    @Override
    public PosSaleResultDto shipSale(String storeId, String registerId, PosShipSaleDto body)
            throws Exception {
        return sales.shipSale(Caller.name(), storeId, registerId, body);
    }
}
