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
package com.codenameone.examples.bazaar.pay;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/// Amounts typed into a payment form, in the minor units of the receipt's
/// currency. Nothing here opens a display.
class ReceiptAmountTest {
    @Test
    void typedAmountsUseTheCurrencysDecimals() {
        assertEquals(1250, ReceiptForm.cents("12.5", "USD"));
        assertEquals(1250, ReceiptForm.cents("1250", "JPY"));
        assertEquals(1250, ReceiptForm.cents("1250.9", "JPY"));
        assertEquals(1250, ReceiptForm.cents("1,25", "BHD"));
        assertEquals(1205, ReceiptForm.cents("1.205", "BHD"));
        assertEquals(0, ReceiptForm.cents("1x", "BHD"));
    }
}
