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
 * The same RFC 3629 rules {@link Utf8} applies, carried ACROSS calls.
 *
 * {@link Utf8#isValid} answers about a range it can see all of, and a sequence cut
 * off by the end of that range is invalid -- which is right for a request body,
 * where the range IS the body. A websocket text message is the opposite: RFC 6455
 * 5.6 lets a client split one character across two frames, so the last byte of a
 * frame can legitimately be the middle of a character, and the validator has to
 * remember where it was.
 *
 * It also has to fail AT the offending byte rather than at the end of the message.
 * A server that buffers a whole fragmented message and validates once answers the
 * same in the end, but it has already accepted however many megabytes the client
 * chose to send after the byte that made the message invalid.
 *
 * The table below is a copy of the one in Utf8, and the duplication is deliberate:
 * the shapes are genuinely different -- one scans a range with `at + following >=
 * end` as a failure, the other treats exactly that case as "ask me again" -- and
 * threading a resumable state machine through the request path would slow the hot
 * case down to serve the rare one. `Utf8StreamTest` cross-checks the two over
 * every leading byte and every boundary in the table, so the copies cannot drift
 * without a red test.
 */
final class Utf8Stream {
    /** Continuation bytes still expected for the character in progress. */
    private int pending;
    /**
     * Whether the NEXT byte is the second byte of its character, which is the one
     * with the narrowed range. Bytes after it are always 80..BF.
     */
    private boolean atSecond;
    private int lowest;
    private int highest;
    /**
     * Sticky. Once a stream is invalid it stays invalid: the caller is going to
     * close the connection, and answering "valid" for a later range would let a
     * retry loop keep going on bytes that already failed.
     */
    private boolean failed;

    /**
     * Feeds the next bytes of the message.
     *
     * Returns false as soon as the stream cannot be UTF-8. A true answer means
     * "nothing wrong so far", NOT "complete" -- a range ending mid-character is a
     * true answer, and {@link #isComplete} is what asks the other question.
     */
    boolean accept(byte[] bytes, int offset, int length) {
        if(failed) {
            return false;
        }
        int at = offset;
        int end = offset + length;
        while(at < end) {
            int b = bytes[at] & 0xff;
            if(pending > 0) {
                int floor = atSecond ? lowest : 0x80;
                int ceiling = atSecond ? highest : 0xbf;
                if(b < floor || b > ceiling) {
                    failed = true;
                    return false;
                }
                pending--;
                atSecond = false;
                at++;
                continue;
            }
            if(b < 0x80) {
                at++;
                continue;
            }
            if(b >= 0xc2 && b <= 0xdf) {
                pending = 1;
                lowest = 0x80;
                highest = 0xbf;
            } else if(b == 0xe0) {
                // A second byte below A0 would be an overlong two-byte value.
                pending = 2;
                lowest = 0xa0;
                highest = 0xbf;
            } else if(b >= 0xe1 && b <= 0xec) {
                pending = 2;
                lowest = 0x80;
                highest = 0xbf;
            } else if(b == 0xed) {
                // ED A0..BF is the surrogate range, which UTF-8 does not encode.
                pending = 2;
                lowest = 0x80;
                highest = 0x9f;
            } else if(b == 0xee || b == 0xef) {
                pending = 2;
                lowest = 0x80;
                highest = 0xbf;
            } else if(b == 0xf0) {
                // Below 90 is an overlong three-byte value.
                pending = 3;
                lowest = 0x90;
                highest = 0xbf;
            } else if(b >= 0xf1 && b <= 0xf3) {
                pending = 3;
                lowest = 0x80;
                highest = 0xbf;
            } else if(b == 0xf4) {
                // F4 90 and above is past U+10FFFF.
                pending = 3;
                lowest = 0x80;
                highest = 0x8f;
            } else {
                // 80..C1 is a continuation with nothing to continue, or an
                // overlong one-byte form; F5..FF encodes nothing at all.
                failed = true;
                return false;
            }
            atSecond = true;
            at++;
        }
        return true;
    }

    /** True when every character fed so far is whole and none of them was invalid. */
    boolean isComplete() {
        return !failed && pending == 0;
    }

    /** Back to the start, for the next message on the same connection. */
    void reset() {
        pending = 0;
        atSecond = false;
        failed = false;
    }
}
