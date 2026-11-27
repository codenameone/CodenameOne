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

import com.codenameone.examples.bazaar.api.CustomerDto;
import java.util.Arrays;
import java.util.List;
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

    private static CustomerDto person(String id, String email, String phone, String first,
            String last) {
        return new CustomerDto(id, "", email, phone, first, last, null, false, "", 0, 0, 0, 0,
                0);
    }

    @Test
    void duplicatesMatchOnEmailPhoneOrName() {
        CustomerDto me = person("a", "Ada@Example.com", "", "Ada", "Lovelace");
        List<CustomerDto> all = Arrays.asList(me, person("b", "ada@example.com", "", "", ""),
                person("c", "x@example.com", "", "ada", "lovelace"),
                person("d", "y@example.com", "555", "Grace", "Hopper"));
        List<CustomerDto> dups = CustomerMatches.duplicates(all, me);
        assertEquals(2, dups.size());
        assertEquals("b", dups.get(0).id());
        assertEquals("c", dups.get(1).id());
        assertEquals(0, CustomerMatches.duplicates(null, me).size());
    }

    @Test
    void daysAndCountsParseStrictly() {
        assertEquals(0, CustomerMatches.daysFrom("", 1000));
        assertEquals(0, CustomerMatches.daysFrom("abc", 1000));
        assertEquals(0, CustomerMatches.daysFrom("0", 1000));
        assertEquals(1000 + 3 * CustomerMatches.DAY, CustomerMatches.daysFrom(" 3 ", 1000));
        assertEquals(250, CustomerMatches.count("250"));
        assertEquals(0, CustomerMatches.count("-5"));
        assertEquals(0, CustomerMatches.count("123456"));
    }
}
