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
package com.codenameone.examples.bazaar.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.codenameone.examples.bazaar.api.BulkActionDto;
import com.codenameone.examples.bazaar.api.BulkActionResultDto;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/// Bulk order requests, their results and serial parsing.
class OrderBulkLogicTest {
    @Test
    void requestsNeedOrdersAKnownActionAndATagForTag() {
        List<String> ids = Arrays.asList("a", "b");
        assertNull(OrderBulk.request(Collections.<String>emptyList(), OrderBulk.FULFIL, null));
        assertNull(OrderBulk.request(ids, "PACKING_SLIPS", null));
        assertNull(OrderBulk.request(ids, OrderBulk.TAG, "  "));
        BulkActionDto paid = OrderBulk.request(ids, OrderBulk.MARK_PAID, "ignored");
        assertEquals(ids, paid.orderIds());
        assertNull(paid.tag());
        assertEquals(" vip".trim(), OrderBulk.request(ids, OrderBulk.TAG, " vip ").tag());
        assertEquals("Add tag", OrderBulk.title(OrderBulk.TAG));
    }

    @Test
    void resultsCountAndRead() {
        List<BulkActionResultDto> r = Arrays.asList(
                new BulkActionResultDto("a", 1001, true, null),
                new BulkActionResultDto("b", 1002, false, "Already paid"));
        assertEquals(1, OrderBulk.succeeded(r));
        assertEquals("#1001", OrderBulk.line(r.get(0)));
        assertEquals("#1002: Already paid", OrderBulk.line(r.get(1)));
    }

    @Test
    void serialsSplitOnSeparatorsWithoutDuplicates() {
        assertEquals(Arrays.asList("SN1", "SN2", "SN3"),
                OrderBulk.serials(" SN1,SN2\nSN1; SN3 "));
        assertEquals(0, OrderBulk.serials(null).size());
    }
}
