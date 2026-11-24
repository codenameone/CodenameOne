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
package com.codenameone.examples.bazaar.shop;

import com.codenameone.examples.bazaar.api.AddressDto;
import java.util.ArrayList;
import java.util.List;

/// Saved shipping addresses, written to one preference string.
///
/// The server keeps no address book yet, so the device does: one address per
/// line, its fields separated by a tab. Neither character can come from a
/// one-line text field, and [#clean] takes them out of anything that could.
/// Pure, so the round trip is tested.
public final class Addresses {
    private static final char FIELD = '\t';
    private static final char RECORD = '\n';

    private Addresses() {
    }

    public static String encode(List<AddressDto> addresses) {
        StringBuilder out = new StringBuilder();
        for (AddressDto a : addresses) {
            if (out.length() > 0) {
                out.append(RECORD);
            }
            out.append(clean(a.name())).append(FIELD).append(clean(a.line1())).append(FIELD)
                    .append(clean(a.line2())).append(FIELD).append(clean(a.city())).append(FIELD)
                    .append(clean(a.region())).append(FIELD).append(clean(a.postal()))
                    .append(FIELD).append(clean(a.country())).append(FIELD)
                    .append(clean(a.phone()));
        }
        return out.toString();
    }

    public static List<AddressDto> decode(String text) {
        List<AddressDto> out = new ArrayList<AddressDto>();
        if (text == null || text.length() == 0) {
            return out;
        }
        int start = 0;
        while (start <= text.length()) {
            int end = text.indexOf(RECORD, start);
            if (end < 0) {
                end = text.length();
            }
            String[] f = split(text.substring(start, end));
            if (f[0].length() > 0 || f[1].length() > 0) {
                out.add(new AddressDto(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], 0, 0));
            }
            start = end + 1;
        }
        return out;
    }

    /// Adds `address` at the front, dropping an older copy of it, keeping at
    /// most five.
    public static List<AddressDto> remember(List<AddressDto> list, AddressDto address) {
        List<AddressDto> out = new ArrayList<AddressDto>();
        out.add(address);
        for (AddressDto a : list) {
            if (out.size() >= 5) {
                break;
            }
            if (!same(a, address)) {
                out.add(a);
            }
        }
        return out;
    }

    /// One line for a list: "Dana Levi, 12 Main St, Springfield".
    public static String summary(AddressDto a) {
        StringBuilder out = new StringBuilder();
        append(out, a.name());
        append(out, a.line1());
        append(out, a.city());
        append(out, a.postal());
        return out.toString();
    }

    /// Everything after the street, for a row titled by the street:
    /// "Apt 12, San Francisco, CA 94105".
    public static String place(AddressDto a) {
        StringBuilder out = new StringBuilder();
        append(out, a.line2());
        append(out, a.city());
        String rp = ((a.region() == null ? "" : a.region()) + " "
                + (a.postal() == null ? "" : a.postal())).trim();
        append(out, rp);
        return out.toString();
    }

    public static boolean same(AddressDto a, AddressDto b) {
        return eq(a.name(), b.name()) && eq(a.line1(), b.line1()) && eq(a.line2(), b.line2())
                && eq(a.city(), b.city()) && eq(a.postal(), b.postal())
                && eq(a.country(), b.country());
    }

    public static String clean(String s) {
        if (s == null) {
            return "";
        }
        return s.replace(FIELD, ' ').replace(RECORD, ' ').replace('\r', ' ').trim();
    }

    private static String[] split(String line) {
        String[] f = new String[8];
        int at = 0;
        for (int iter = 0; iter < 8; iter++) {
            if (at > line.length()) {
                f[iter] = "";
                continue;
            }
            int end = iter == 7 ? -1 : line.indexOf(FIELD, at);
            if (end < 0) {
                end = line.length();
            }
            f[iter] = line.substring(at, end);
            at = end + 1;
        }
        return f;
    }

    private static void append(StringBuilder out, String s) {
        if (s != null && s.trim().length() > 0) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(s.trim());
        }
    }

    private static boolean eq(String a, String b) {
        return clean(a).equals(clean(b));
    }
}
