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
package com.codenameone.examples.bazaar.ops;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// A report subtracts only the merchandise part of a refund: tax and
/// shipping refunded with it never take sales below zero.
class ReportsRefundTest {
    @Test
    void theRefundIsClampedToTheMerchandise() {
        // 50.00 of goods, 10.00 discount; a full refund of 47.50 with tax and
        // shipping subtracts 40.00, never more.
        assertEquals(4000, Reports.merchandiseRefund(4000, 4750));
        assertEquals(1500, Reports.merchandiseRefund(4000, 1500));
        assertEquals(0, Reports.merchandiseRefund(4000, 0));
        assertEquals(0, Reports.merchandiseRefund(0, 900));
        assertEquals(0, Reports.merchandiseRefund(-100, 900));
    }

    @Test
    void aProductLineTakesItsShareOfTheRefund() {
        // Lines of 30.00 and 10.00, 20.00 refunded: 15.00 and 5.00.
        assertEquals(1500, Reports.lineShare(2000, 3000, 4000));
        assertEquals(500, Reports.lineShare(2000, 1000, 4000));
        assertEquals(1000, Reports.lineShare(4000, 1000, 4000));
        assertEquals(0, Reports.lineShare(0, 1000, 4000));
        assertEquals(0, Reports.lineShare(2000, 1000, 0));
    }
}
