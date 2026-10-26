/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.crypto.market;

/// Helpers shared by the feeds.
@com.codename1.impl.SharedWithBackend
final class PriceFeeds {
    private PriceFeeds() {
    }

    // ASCII fold: a ticker is ASCII, and String.toUpperCase is locale sensitive
    static String upper(String s) {
        if (s == null) {
            return null;
        }
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] >= 'a' && c[i] <= 'z') {
                c[i] = (char) (c[i] - 32);
            }
        }
        return new String(c);
    }

    static String lower(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] >= 'A' && c[i] <= 'Z') {
                c[i] = (char) (c[i] + 32);
            }
        }
        return new String(c);
    }

    static double number(Object o, String what) throws java.io.IOException {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        if (o instanceof com.codename1.crypto.UInt256) {
            return Double.parseDouble(o.toString());
        }
        if (o instanceof String) {
            try {
                return Double.parseDouble((String) o);
            } catch (NumberFormatException e) {
                throw new java.io.IOException(what + ": not a number: " + o);
            }
        }
        throw new java.io.IOException(what + ": no price");
    }

    // Percent-encoding for a query or path value; tickers and ids are ASCII
    static String encode(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_'
                    || c == '.' || c == '~') {
                b.append(c);
            } else {
                b.append('%').append("0123456789ABCDEF".charAt((c >> 4) & 15)).append("0123456789ABCDEF".charAt(c & 15));
            }
        }
        return b.toString();
    }
}
