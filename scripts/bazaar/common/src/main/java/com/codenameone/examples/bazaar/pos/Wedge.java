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

import com.codenameone.examples.bazaar.api.PosItemDto;
import java.util.List;

/// What a barcode scanner typed. Pure: no display.
///
/// A USB or Bluetooth scanner is a "keyboard wedge": it types the code into
/// the focused field, fast, and presses Enter. Some add a tab or a carriage
/// return, some a prefix the store configured. The register's scan field
/// hands every Enter here: a quantity can lead (`3*0123456789`), a receipt's
/// QR code names an order (`BAZAAR-ORDER:1042`), and anything else is looked
/// up as a barcode, then a SKU.
public final class Wedge {
    /// What a receipt's QR code says before the order number.
    public static final String ORDER_PREFIX = "BAZAAR-ORDER:";

    /// A parsed scan: the code and how many.
    public static final class Scan {
        public final String code;
        public final int quantity;

        Scan(String code, int quantity) {
            this.code = code;
            this.quantity = quantity;
        }
    }

    private Wedge() {
    }

    /// The text with every control character (Enter, tab, a scanner's
    /// prefix byte) removed and the ends trimmed.
    public static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < raw.length(); iter++) {
            char c = raw.charAt(iter);
            if (c >= ' ' && c != 127) {
                out.append(c);
            }
        }
        return out.toString().trim();
    }

    /// The scan in `raw`, or null for nothing scanned. A leading `n*` is a
    /// quantity from 1 to 999.
    public static Scan parse(String raw) {
        String t = clean(raw);
        if (t.length() == 0) {
            return null;
        }
        int star = t.indexOf('*');
        if (star > 0 && star < t.length() - 1) {
            int q = digits(t.substring(0, star).trim());
            if (q > 0 && q < 1000) {
                return new Scan(t.substring(star + 1).trim(), q);
            }
        }
        return new Scan(t, 1);
    }

    /// The order number a receipt's QR code names, or -1.
    public static int orderNumber(String raw) {
        String t = clean(raw);
        if (t.regionMatches(true, 0, ORDER_PREFIX, 0, ORDER_PREFIX.length())) {
            return digits(t.substring(ORDER_PREFIX.length()).trim());
        }
        if (t.startsWith("#")) {
            return digits(t.substring(1));
        }
        return -1;
    }

    /// Whether the text reads like a scanned code rather than a search: no
    /// spaces and at least four characters with a digit in them.
    public static boolean looksLikeCode(String raw) {
        String t = clean(raw);
        if (t.length() < 4 || t.indexOf(' ') >= 0) {
            return false;
        }
        for (int iter = 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            if (c >= '0' && c <= '9') {
                return true;
            }
        }
        return false;
    }

    /// The item with this barcode, else this SKU (case does not matter), or
    /// null.
    public static PosItemDto find(List<PosItemDto> items, String code) {
        if (items == null || code == null || code.length() == 0) {
            return null;
        }
        for (int iter = 0; iter < items.size(); iter++) {
            PosItemDto i = items.get(iter);
            if (i.barcode() != null && i.barcode().equals(code)) {
                return i;
            }
        }
        for (int iter = 0; iter < items.size(); iter++) {
            PosItemDto i = items.get(iter);
            if (i.sku() != null && i.sku().equalsIgnoreCase(code)) {
                return i;
            }
        }
        return null;
    }

    /// The whole text as a positive number, or -1.
    static int digits(String t) {
        if (t == null || t.length() == 0 || t.length() > 9) {
            return -1;
        }
        int n = 0;
        for (int iter = 0; iter < t.length(); iter++) {
            char c = t.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            n = n * 10 + (c - '0');
        }
        return n;
    }
}
