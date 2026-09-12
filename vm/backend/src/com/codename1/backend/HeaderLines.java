/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend;

import java.io.IOException;
import java.util.List;

/**
 * Validation for the "Name: value" header lines an OUTBOUND request carries.
 *
 * <p>Shared by both Web implementations on purpose. The packaged arm passes the
 * headers to its native as one string with '\n' between them, and the native
 * splits on that byte and hands each line to libcurl -- so a value carrying a
 * newline becomes an additional header the caller never wrote. Anything derived
 * from untrusted input reaches that: a bearerToken of "abc\nX-Admin: true"
 * injects a second header into a request the upstream trusts. The Java SE arm
 * hands the same headers to HttpURLConnection, whose behaviour here is the JDK's
 * business rather than ours.
 *
 * <p>Two arms relying on two different underlying stacks to refuse the same input
 * is how they end up disagreeing, so neither is trusted to: both call this, and
 * the answer is the same before either stack is reached.
 */
final class HeaderLines {
    private HeaderLines() {
    }

    /**
     * @throws IOException if any entry is not a header line whose name is a token
     *                     and whose value is a legal field value.
     */
    static void validate(List headers) throws IOException {
        if(headers == null) {
            return;
        }
        for(int iter = 0 ; iter < headers.size() ; iter++) {
            Object entry = headers.get(iter);
            if(entry == null) {
                throw new IOException("A request header is null");
            }
            check(String.valueOf(entry));
        }
    }

    private static void check(String line) throws IOException {
        int colon = line.indexOf(':');
        if(colon < 1) {
            // The NAME only. A value is the part an attacker supplies and the part
            // that carries credentials, so it must not reach a message or a log --
            // the whole reason this class exists is that values are untrusted.
            throw new IOException("A request header is not a \"Name: value\" line");
        }
        for(int iter = 0 ; iter < colon ; iter++) {
            char c = line.charAt(iter);
            boolean tchar = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '!' || c == '#' || c == '$' || c == '%' || c == '&'
                    || c == '\'' || c == '*' || c == '+' || c == '-' || c == '.'
                    || c == '^' || c == '_' || c == '`' || c == '|' || c == '~';
            if(!tchar) {
                throw new IOException("A request header name is not a token");
            }
        }
        for(int iter = colon + 1 ; iter < line.length() ; iter++) {
            char c = line.charAt(iter);
            if(c == '\t') {
                continue;
            }
            // The same field-value rule the server applies to what it SENDS: HTAB,
            // SP, VCHAR and obs-text. Tested against the byte that will be emitted,
            // which is what narrowed() below makes true -- so U+010A is not '\n'
            // to a char test and is 0x0A on the wire, the same newline by another
            // spelling. Until narrowed() existed this path handed the native a
            // String for stringToUTF8 to encode, and the byte checked here was not
            // the byte that went out.
            if(c < 0x20 || c == 0x7f || c > 0xff) {
                throw new IOException("A request header value carries a character no "
                        + "field value may hold, in " + line.substring(0, colon));
            }
        }
    }

    /**
     * A validated field-value line as bytes, ONE PER CHARACTER.
     *
     * A field value is octets. validate() above accepts 0x20 to 0xff except
     * 0x7f -- obs-text included -- so the only faithful conversion narrows each
     * character with a cast, which is exactly what the HTTP/1.1 writer's
     * Conn.put does and the inverse of the newStringFromAsciiLen the inbound
     * natives use. Encoding to UTF-8 instead sent one byte over HTTP/1.1 and two
     * over h2 for the same header.
     *
     * <p>Shared rather than copied into each caller: Http2 and Web both hand a
     * header block to a native, and both arms compile this file, so the rule and
     * the test for it live in one place.
     */
    static byte[] narrowed(String value) {
        byte[] out = new byte[value.length()];
        for(int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (byte)value.charAt(iter);
        }
        return out;
    }
}
