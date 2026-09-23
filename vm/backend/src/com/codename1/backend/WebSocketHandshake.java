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

/**
 * The opening handshake: what makes an upgrade request acceptable, and what goes
 * back in the 101.
 *
 * Everything here is pure -- a String in, a String out -- so the whole of it runs
 * under a unit test on a plain JVM. The parts that need a Request (which headers
 * are present, how many times) stay in HttpServer, because that is where the
 * Request's lifetime rules apply.
 *
 * The security-shaped part is {@link #selectSubprotocol}. The value it returns is
 * echoed into a response header, and the offer it chooses from came off the wire.
 * A 101 is not written by `writeHeadAndBody`, so it does not pass through the
 * response-splitting guard that protects every other response in this server --
 * which is why this method answers with a string from the SERVER's own list and
 * never with the client's bytes. A client cannot inject a header through a value
 * it did not supply.
 */
final class WebSocketHandshake {
    /**
     * RFC 6455 1.3. A fixed string, not a secret: it exists so that a cache or a
     * proxy cannot be talked into replaying an ordinary response as a handshake,
     * not to authenticate anybody.
     */
    static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    /** The only version this server speaks, and what a 426 advertises. */
    static final String VERSION = "13";

    private WebSocketHandshake() {
    }

    /**
     * Whether `Sec-WebSocket-Key` is what RFC 6455 4.1 requires: base64 of exactly
     * sixteen bytes.
     *
     * Checked rather than assumed because the accept value is computed from it
     * either way, so a malformed key produces a perfectly well-formed 101 and a
     * connection whose peer disagrees about whether the handshake succeeded.
     */
    static boolean keyIsWellFormed(String key) {
        if(key == null) {
            return false;
        }
        byte[] decoded = Base64.decode(key);
        return decoded != null && decoded.length == 16;
    }

    /**
     * The `Sec-WebSocket-Accept` value for a key.
     *
     * SHA-1 here is RFC 6455 4.2.2's, which specifies it by name. It is not being
     * chosen, and it is not standing in for a signature -- see the note on
     * {@link Crypto#sha1}.
     */
    static String accept(String key) {
        String combined = key + GUID;
        byte[] bytes = new byte[combined.length()];
        for(int iter = 0 ; iter < bytes.length ; iter++) {
            bytes[iter] = (byte)combined.charAt(iter);
        }
        return Base64.encode(Crypto.sha1(bytes));
    }

    /**
     * The subprotocol to answer with, or null for none.
     *
     * SERVER preference order, not the client's. RFC 6455 4.2.2 leaves the choice
     * open, and taking the client's order would let a peer select a deprecated
     * protocol over a current one simply by listing it first.
     *
     * A null answer is not an error. RFC 6455 says a server that supports none of
     * the offered protocols omits the header, and it is the CLIENT's business
     * whether to accept that -- browsers close the connection, other clients carry
     * on. Refusing the handshake here instead would be a different answer from the
     * one the standard describes.
     */
    static String selectSubprotocol(String offered, String[] supported) {
        if(offered == null || supported == null || supported.length == 0) {
            return null;
        }
        for(int iter = 0 ; iter < supported.length ; iter++) {
            String candidate = supported[iter];
            if(candidate != null && isToken(candidate) && listHasToken(offered, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Whether a comma-separated list names this token, case-sensitively.
     *
     * Subprotocol names are case-sensitive (RFC 6455 4.1), unlike almost every
     * other list this server parses -- so this deliberately does not reuse the
     * case-insensitive token search on Request.
     */
    static boolean listHasToken(String list, String token) {
        int at = 0;
        int length = list.length();
        while(at <= length) {
            int comma = list.indexOf(',', at);
            int end = comma < 0 ? length : comma;
            int start = at;
            while(start < end && (list.charAt(start) == ' ' || list.charAt(start) == '\t')) {
                start++;
            }
            int stop = end;
            while(stop > start && (list.charAt(stop - 1) == ' ' || list.charAt(stop - 1) == '\t')) {
                stop--;
            }
            if(stop - start == token.length() && list.regionMatches(false, start, token, 0, token.length())) {
                return true;
            }
            if(comma < 0) {
                return false;
            }
            at = comma + 1;
        }
        return false;
    }

    /**
     * An RFC 9110 token, which is what may be written into a header value without
     * any further escaping.
     *
     * The reason this is here at all: a token cannot hold CR, LF or a colon, so a
     * value that passes this check cannot start a new header line. That is the
     * property the 101 writer depends on.
     */
    static boolean isToken(String value) {
        if(value == null || value.length() == 0) {
            return false;
        }
        for(int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            boolean alphanumeric = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9');
            if(alphanumeric) {
                continue;
            }
            if("!#$%&'*+-.^_`|~".indexOf(c) < 0) {
                return false;
            }
        }
        return true;
    }
}
