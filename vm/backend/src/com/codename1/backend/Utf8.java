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
 * Whether a byte range really is UTF-8.
 *
 * `new String(bytes, "UTF-8")` never fails: a malformed sequence becomes U+FFFD
 * and the caller is handed text the client did not send. For a request body that
 * turns a protocol error into silent corruption -- the JSON parses, the handler
 * runs, and whatever was validated was validated against the REPLACEMENT, not
 * against what arrived. So the bytes are checked before they are decoded, and a
 * body that is not UTF-8 is answered with a 400.
 *
 * Written out by hand because there is no CharsetDecoder here: vm/JavaAPI has
 * Charset and StandardCharsets and nothing else, so CodingErrorAction.REPORT --
 * the way this is normally done -- does not exist on the target.
 *
 * The table is RFC 3629's, which is narrower than "any sequence that decodes":
 * an overlong encoding, a surrogate half and anything above U+10FFFF are all
 * rejected, because each of them is a way of spelling a character twice and the
 * second spelling is what slips past a filter that only checked the first.
 */
final class Utf8 {
    private Utf8() {
    }

    static boolean isValid(byte[] bytes, int offset, int length) {
        int at = offset;
        int end = offset + length;
        while(at < end) {
            int first = bytes[at] & 0xff;
            if(first < 0x80) {
                at++;
                continue;
            }
            int following;
            int lowest;
            int highest;
            if(first >= 0xc2 && first <= 0xdf) {
                following = 1;
                lowest = 0x80;
                highest = 0xbf;
            } else if(first == 0xe0) {
                // A second byte below A0 would be an overlong two-byte value.
                following = 2;
                lowest = 0xa0;
                highest = 0xbf;
            } else if(first >= 0xe1 && first <= 0xec) {
                following = 2;
                lowest = 0x80;
                highest = 0xbf;
            } else if(first == 0xed) {
                // ED A0..BF is the surrogate range, which UTF-8 does not encode.
                following = 2;
                lowest = 0x80;
                highest = 0x9f;
            } else if(first == 0xee || first == 0xef) {
                following = 2;
                lowest = 0x80;
                highest = 0xbf;
            } else if(first == 0xf0) {
                // Below 90 is an overlong three-byte value.
                following = 3;
                lowest = 0x90;
                highest = 0xbf;
            } else if(first >= 0xf1 && first <= 0xf3) {
                following = 3;
                lowest = 0x80;
                highest = 0xbf;
            } else if(first == 0xf4) {
                // F4 90 and above is past U+10FFFF.
                following = 3;
                lowest = 0x80;
                highest = 0x8f;
            } else {
                // 80..C1 is a continuation with nothing to continue, or an
                // overlong one-byte form; F5..FF encodes nothing at all.
                return false;
            }
            if(at + following >= end) {
                return false;                 // truncated at the end of the range
            }
            int second = bytes[at + 1] & 0xff;
            if(second < lowest || second > highest) {
                return false;
            }
            for(int iter = 2 ; iter <= following ; iter++) {
                int next = bytes[at + iter] & 0xff;
                if(next < 0x80 || next > 0xbf) {
                    return false;
                }
            }
            at += following + 1;
        }
        return true;
    }
}
