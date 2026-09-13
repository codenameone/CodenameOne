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
        requireNotTransportOwned(line.substring(0, colon));
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

    /**
     * Refuses a method that is not a single HTTP token.
     *
     * THE SAME INJECTION THIS CLASS EXISTS FOR, through the other field of the
     * request line. The packaged arm hands the method to libcurl as
     * CURLOPT_CUSTOMREQUEST, which writes it into the request line verbatim: a
     * method of "GET /admin HTTP/1.1", a CRLF and a header line puts a second
     * request and a header of the caller's choosing on the wire, with the URL
     * this code chose left dangling on the end. An application that forwards a
     * caller's verb -- a proxy, a webhook relay -- hands that over. HttpURLConnection
     * refuses the same string, so the arms disagreed about whether it was a
     * request at all.
     *
     * <p>tchar, as RFC 9110 defines it for a method and a field name alike.
     * Null is left to the caller: both arms read it as GET.
     */
    static void requireMethod(String method) throws IOException {
        if(method == null) {
            return;
        }
        if(method.length() == 0) {
            throw new IOException("An HTTP method cannot be empty");
        }
        for(int iter = 0 ; iter < method.length() ; iter++) {
            char c = method.charAt(iter);
            boolean tchar = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '!' || c == '#' || c == '$' || c == '%' || c == '&'
                    || c == '\'' || c == '*' || c == '+' || c == '-' || c == '.'
                    || c == '^' || c == '_' || c == '`' || c == '|' || c == '~';
            if(!tchar) {
                throw new IOException("An HTTP method is one token and this is not: "
                        + "the character at index " + iter + " cannot appear in one");
            }
        }
    }

    /**
     * Refuses a request target that is not origin-form.
     *
     * The other half of the request line, and the same injection: Http.request
     * writes the path into it verbatim, so a path carrying a CR or LF adds
     * whatever the caller likes to a request the upstream trusts. A space ends
     * the target and makes the rest of it the HTTP version.
     *
     * <p>Origin-form because that is what this client sends: a path beginning
     * with "/" and, optionally, a query. Anything else -- an absolute URL, an
     * authority, an asterisk -- is a shape it does not construct.
     */
    static void requireOriginForm(String path) throws IOException {
        if(path == null || path.length() == 0) {
            throw new IOException("No request path");
        }
        if(path.charAt(0) != '/') {
            throw new IOException("A request path is origin-form and begins with "
                    + "'/'; this one does not");
        }
        for(int iter = 0 ; iter < path.length() ; iter++) {
            char c = path.charAt(iter);
            if(c <= 0x20 || c == 0x7f) {
                throw new IOException("A request path cannot hold a control "
                        + "character or a space; this one does, at index " + iter
                        + ". Percent-encode it");
            }
        }
    }

    /**
     * Refuses a header the transport owns.
     *
     * <p>FRAMING AND ROUTING ARE NOT THE CALLER'S. libcurl documents that a
     * header given to CURLOPT_HTTPHEADER REPLACES the one it would have
     * generated, and the body is configured separately -- so a caller-supplied
     * "Content-Length: 0" sent alongside a real body tells the upstream the
     * request ends where it does not. A keep-alive peer then reads the rest of
     * that body as the next request on the connection, which is request
     * smuggling, offered to any application that forwards a caller's headers.
     * Transfer-Encoding does the same through the other framing field.
     *
     * <p>Host is refused for the routing half of it: overriding it picks a
     * different virtual host on the destination this code chose, so the request
     * goes somewhere the caller was never entitled to name. Both arms derive it
     * from the URL, which is the one place it should come from.
     *
     * <p>HttpURLConnection ignores all three as restricted headers, so the arms
     * disagreed as well: dangerous when packaged, silently dropped locally.
     */
    private static void requireNotTransportOwned(String name) throws IOException {
        if(name.equalsIgnoreCase("content-length")
                || name.equalsIgnoreCase("transfer-encoding")
                || name.equalsIgnoreCase("host")) {
            throw new IOException("The transport owns " + name + ", and a request "
                    + "cannot supply its own: framing is derived from the body and "
                    + "the host from the URL");
        }
    }
}
