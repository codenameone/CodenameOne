/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.examples.wayline.geo;

import java.io.IOException;

/// Percent-encodes text for a query string or a form body.
public final class UrlText {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private UrlText() {
    }

    /// Everything but the unreserved characters of RFC 3986 is escaped, as the
    /// bytes of its UTF-8 form.
    public static String encode(String text) throws IOException {
        byte[] bytes = text.getBytes("UTF-8");
        StringBuilder out = new StringBuilder(bytes.length + 16);
        for (int iter = 0; iter < bytes.length; iter++) {
            int b = bytes[iter] & 0xff;
            if ((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z') || (b >= '0' && b <= '9')
                    || b == '-' || b == '.' || b == '_' || b == '~') {
                out.append((char) b);
            } else {
                out.append('%').append(HEX[b >> 4]).append(HEX[b & 0xf]);
            }
        }
        return out.toString();
    }
}
