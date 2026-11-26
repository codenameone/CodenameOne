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

import com.codenameone.examples.bazaar.api.BulkActionDto;
import com.codenameone.examples.bazaar.api.BulkActionResultDto;
import java.util.ArrayList;
import java.util.List;

/// The pure logic behind bulk order actions and serial scanning: which
/// actions the app offers, the request each one sends, and how the server's
/// per-order answers read back. No UI, so it is unit tested.
final class OrderBulk {
    static final String FULFIL = "FULFIL";
    static final String LABELS = "LABELS";
    static final String MARK_PAID = "MARK_PAID";
    static final String ARCHIVE = "ARCHIVE";
    static final String UNARCHIVE = "UNARCHIVE";
    static final String TAG = "TAG";

    /// Every bulk action the app offers; packing slips are web only.
    static final String[] ACTIONS = {FULFIL, LABELS, MARK_PAID, ARCHIVE, UNARCHIVE, TAG};
    /// The English title of each action, a Bundle key.
    static final String[] TITLES = {"Fulfil", "Buy labels", "Mark as paid", "Archive",
        "Unarchive", "Add tag"};

    private OrderBulk() {
    }

    private static int indexOf(String action) {
        for (int iter = 0; iter < ACTIONS.length; iter++) {
            if (ACTIONS[iter].equals(action)) {
                return iter;
            }
        }
        return -1;
    }

    /// The title of `action`, or the code itself for an unknown one.
    static String title(String action) {
        int at = indexOf(action);
        return at < 0 ? action : TITLES[at];
    }

    /// The request for `action` on `ids`, or null when there is nothing to
    /// send: no orders, an unknown action, or a tag action without a tag.
    static BulkActionDto request(List<String> ids, String action, String tag) {
        if (ids == null || ids.isEmpty() || indexOf(action) < 0) {
            return null;
        }
        String t = null;
        if (TAG.equals(action)) {
            t = tag == null ? "" : tag.trim();
            if (t.length() == 0) {
                return null;
            }
        }
        return new BulkActionDto(new ArrayList<String>(ids), action, t);
    }

    /// How many of `results` succeeded.
    static int succeeded(List<BulkActionResultDto> results) {
        int n = 0;
        for (int iter = 0; results != null && iter < results.size(); iter++) {
            if (results.get(iter).ok()) {
                n++;
            }
        }
        return n;
    }

    /// One line for a result: the order number and the server's message.
    static String line(BulkActionResultDto r) {
        String m = r.message();
        return "#" + r.number() + (m == null || m.length() == 0 ? "" : ": " + m);
    }

    /// The serial numbers in `text`, one per comma, semicolon, space or line,
    /// in order and without duplicates.
    static List<String> serials(String text) {
        List<String> out = new ArrayList<String>();
        if (text == null) {
            return out;
        }
        StringBuilder cur = new StringBuilder();
        for (int iter = 0; iter <= text.length(); iter++) {
            char c = iter < text.length() ? text.charAt(iter) : ',';
            if (c == ',' || c == ';' || c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                if (cur.length() > 0) {
                    String s = cur.toString();
                    if (!out.contains(s)) {
                        out.add(s);
                    }
                    cur.setLength(0);
                }
            } else {
                cur.append(c);
            }
        }
        return out;
    }
}
