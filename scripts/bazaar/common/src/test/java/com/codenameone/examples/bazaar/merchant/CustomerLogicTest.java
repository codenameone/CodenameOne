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

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/// The customer screen's tag handling, without a display.
class CustomerLogicTest {
    @Test
    void tagsAreTrimmedDeduplicatedAndJoined() {
        assertEquals(Arrays.asList("vip", "wholesale"), CustomersPane.tags(" vip, ,wholesale,vip,"));
        assertEquals(0, CustomersPane.tags("").size());
        assertEquals(0, CustomersPane.tags(null).size());
        assertEquals("vip, wholesale", CustomersPane.join(Arrays.asList("vip", "wholesale")));
        assertEquals("", CustomersPane.join(null));
    }

    @Test
    void reviewStatusesHaveWordsAndBadges() {
        assertEquals("Published", Words.of("PUBLISHED"));
        assertEquals("SvBadgeGood", Words.badge("PUBLISHED"));
        assertEquals("SvBadgeBad", Words.badge("REJECTED"));
        assertEquals("SvBadgeWarn", Words.badge("PENDING"));
    }
}
