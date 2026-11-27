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
package com.codenameone.examples.bazaar.api;

import com.codename1.annotations.Mapped;

/// A sale at the counter shipped to the customer from the register's location rather than
/// handed over. As [PosSaleDto], plus the `address` and the `shippingRateId` picked from
/// `PosCounterApi.shipQuote`; the shipping is part of the total the tenders pay. It needs the
/// network: its stock is reserved and it is packed and shipped like an online order.
@Mapped
public record PosShipSaleDto(
        String clientId,
        String shiftId,
        String customerId,
        String email,
        String note,
        java.util.List<PosLineDto> lines,
        java.util.List<PosTenderDto> tenders,
        String staffUsername,
        String approvalId,
        AddressDto address,
        String shippingRateId) {
}
