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

import com.codenameone.examples.bazaar.api.CustomerDto;
import java.util.ArrayList;
import java.util.List;

/// The rules behind the customer page's merge, credit and gift card dialogs,
/// kept free of UI classes so they can be tested on their own.
final class CustomerMatches {
    static final long DAY = 86400000L;

    private CustomerMatches() {
    }

    /// The customers in `all` that look like the same person as `c`: the same
    /// e-mail (case aside), phone or full name. `c` itself is never one.
    static List<CustomerDto> duplicates(List<CustomerDto> all, CustomerDto c) {
        List<CustomerDto> out = new ArrayList<CustomerDto>();
        if (all == null || c == null) {
            return out;
        }
        String name = name(c);
        for (int iter = 0; iter < all.size(); iter++) {
            CustomerDto o = all.get(iter);
            if (o == null || o.id() == null || o.id().equals(c.id())) {
                continue;
            }
            boolean same = filled(c.email()) && c.email().equalsIgnoreCase(o.email())
                    || filled(c.phone()) && c.phone().equals(o.phone())
                    || name.length() > 0 && name.equalsIgnoreCase(name(o));
            if (same) {
                out.add(o);
            }
        }
        return out;
    }

    /// The instant `days` whole days after `now`, or 0 (never) when `days`
    /// is not a positive number.
    static long daysFrom(String days, long now) {
        int n = count(days);
        return n <= 0 ? 0 : now + n * DAY;
    }

    /// `text` as a whole number of up to five digits, or 0 when it is blank
    /// or anything else.
    static int count(String text) {
        if (text == null) {
            return 0;
        }
        String t = text.trim();
        if (t.length() == 0 || t.length() > 5) {
            return 0;
        }
        int n = 0;
        for (int iter = 0; iter < t.length(); iter++) {
            char ch = t.charAt(iter);
            if (ch < '0' || ch > '9') {
                return 0;
            }
            n = n * 10 + (ch - '0');
        }
        return n;
    }

    static String name(CustomerDto c) {
        String f = c.firstName() == null ? "" : c.firstName();
        String l = c.lastName() == null ? "" : c.lastName();
        return (f + " " + l).trim();
    }

    private static boolean filled(String s) {
        return s != null && s.length() > 0;
    }
}
