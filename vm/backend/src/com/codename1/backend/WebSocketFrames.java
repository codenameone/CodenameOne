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
 * The RFC 6455 frame layer: where the bytes on the wire become frames, and where
 * every frame a conformant peer would never send is refused.
 *
 * Pure and static on purpose. It takes a byte array and an offset and answers a
 * question; it holds no connection, opens nothing and closes nothing. That is what
 * lets the whole of it be driven from a unit test on a plain JVM, which matters
 * more here than anywhere else in this package: a frame parser is mostly made of
 * cases no ordinary client ever produces, and the only way to exercise those is to
 * construct them by hand.
 *
 * The rules that are easy to leave out, and what each one costs:
 *
 * - A length MUST use its shortest form. Accepting 126 for a 3-byte payload sounds
 *   harmless, but it means the same message has two spellings, and a length field
 *   with two spellings in front of a proxy is a request-smuggling primitive.
 * - A client frame MUST be masked. Unmasked client data is exactly the cache
 *   -poisoning attack masking was added to prevent, so an unmasked frame is a
 *   close, not a warning -- and note this server is stricter here than the
 *   hand-rolled one it replaces, which ignored the mask bit entirely.
 * - A control frame is at most 125 bytes and is never fragmented. Both are what
 *   let a receiver handle one without buffering, which is the point of a PING
 *   arriving in the middle of a 10MB message.
 * - RSV bits are zero unless an extension negotiated them. A set bit that nothing
 *   negotiated means the peer is speaking a protocol this server did not agree to.
 */
final class WebSocketFrames {
    static final int OP_CONTINUATION = 0x0;
    static final int OP_TEXT = 0x1;
    static final int OP_BINARY = 0x2;
    static final int OP_CLOSE = 0x8;
    static final int OP_PING = 0x9;
    static final int OP_PONG = 0xa;

    static final int CLOSE_NORMAL = 1000;
    static final int CLOSE_GOING_AWAY = 1001;
    static final int CLOSE_PROTOCOL_ERROR = 1002;
    static final int CLOSE_UNACCEPTABLE_DATA = 1003;
    /** Reserved with no meaning; a peer that sends it is refused. */
    static final int CLOSE_RESERVED = 1004;
    static final int CLOSE_NO_STATUS = 1005;
    static final int CLOSE_ABNORMAL = 1006;
    static final int CLOSE_BAD_PAYLOAD = 1007;
    static final int CLOSE_POLICY = 1008;
    static final int CLOSE_TOO_BIG = 1009;
    static final int CLOSE_EXTENSION_EXPECTED = 1010;
    static final int CLOSE_INTERNAL_ERROR = 1011;
    static final int CLOSE_TLS_FAILURE = 1015;

    /** What {@link #headerLength} answers when the header is not all here yet. */
    static final int NEED_MORE = -1;

    private WebSocketFrames() {
    }

    static boolean isControl(int opcode) {
        return (opcode & 0x8) != 0;
    }

    static boolean fin(byte[] b, int off) {
        return (b[off] & 0x80) != 0;
    }

    /** The three reserved bits, as a 0..7 value, RSV1 being the high one. */
    static int rsv(byte[] b, int off) {
        return (b[off] >> 4) & 0x7;
    }

    static int opcode(byte[] b, int off) {
        return b[off] & 0x0f;
    }

    static boolean masked(byte[] b, int off) {
        return (b[off + 1] & 0x80) != 0;
    }

    /**
     * How many bytes the header occupies, or {@link #NEED_MORE} when `avail` does
     * not yet cover it.
     *
     * Answers from the first two bytes plus whatever the length form demands, so a
     * caller can size its read without having the payload.
     */
    static int headerLength(byte[] b, int off, int avail) {
        if(avail < 2) {
            return NEED_MORE;
        }
        int length = b[off + 1] & 0x7f;
        int header = 2;
        if(length == 126) {
            header += 2;
        } else if(length == 127) {
            header += 8;
        }
        if((b[off + 1] & 0x80) != 0) {
            header += 4;
        }
        return avail < header ? NEED_MORE : header;
    }

    /**
     * The payload length. Requires that {@link #headerLength} has already said the
     * header is present.
     *
     * Answers -1 for a 64-bit length whose top bit is set, which RFC 6455 5.2
     * forbids. Signalling it here rather than letting it through as a negative
     * long is deliberate: every caller downstream treats the length as a size, and
     * a negative size reaches an array allocation before anything looks at it.
     */
    static long payloadLength(byte[] b, int off) {
        int seven = b[off + 1] & 0x7f;
        if(seven < 126) {
            return seven;
        }
        if(seven == 126) {
            return ((long)(b[off + 2] & 0xff) << 8) | (b[off + 3] & 0xff);
        }
        long value = 0;
        for(int iter = 0 ; iter < 8 ; iter++) {
            value = (value << 8) | (b[off + 2 + iter] & 0xff);
        }
        return value < 0 ? -1 : value;
    }

    /**
     * Whether the length field uses the shortest form that could carry its value.
     *
     * RFC 6455 5.2 makes this a MUST, and nothing else in the parser notices: a
     * 3-byte payload announced with a 64-bit length decodes perfectly.
     */
    static boolean lengthIsMinimal(byte[] b, int off) {
        int seven = b[off + 1] & 0x7f;
        if(seven < 126) {
            return true;
        }
        long value = payloadLength(b, off);
        if(value < 0) {
            return false;
        }
        if(seven == 126) {
            return value >= 126;
        }
        return value > 0xffffL;
    }

    /** Where the four mask bytes start; only meaningful when {@link #masked}. */
    static int maskOffset(byte[] b, int off) {
        int seven = b[off + 1] & 0x7f;
        if(seven == 126) {
            return off + 4;
        }
        if(seven == 127) {
            return off + 10;
        }
        return off + 2;
    }

    /**
     * XORs `length` bytes in place, continuing a mask that began `phase` bytes ago,
     * and answers the phase to carry into the next call.
     *
     * The phase is the whole reason this is not a one-liner. A frame's payload
     * arrives in as many reads as the network feels like, and the mask repeats
     * every four bytes counted from the START of the payload -- so a chunk that
     * begins three bytes in must start at key byte 3, not key byte 0. Restarting
     * the key per chunk corrupts every payload that does not arrive whole, which
     * on loopback is almost none of them and in production is most of the large
     * ones.
     */
    static int unmask(byte[] data, int offset, int length, byte[] key, int phase) {
        int at = phase;
        int end = offset + length;
        for(int iter = offset ; iter < end ; iter++) {
            data[iter] = (byte)(data[iter] ^ key[at & 3]);
            at++;
        }
        return at & 3;
    }

    /**
     * Whether a peer is allowed to send this close code.
     *
     * 1005 and 1006 are the two that matter: they are what a LOCAL implementation
     * reports when there was no code or no close at all, so a peer that sends
     * either is claiming something only this side can know. 1015 is the same for
     * TLS. 1004 has no defined meaning -- it was "frame too large" in a draft and
     * became 1009 -- and 1012..1014 are reserved, so all of them are refused for
     * the same reason: accepting a code with no agreed meaning is how a future
     * protocol change becomes a compatibility problem for whoever guessed.
     *
     * The three exclusions inside 1000..1011 are easy to lose to a range check.
     * `code >= 1000 && code <= 1011` reads as if it were the whole rule and lets
     * 1004, 1005 and 1006 through, which is what this method did until the test
     * below caught 1004.
     */
    static boolean isValidCloseCode(int code) {
        if(code >= 3000 && code <= 4999) {
            return true;                      // registered and private use
        }
        if(code < 1000 || code > 1011) {
            return false;
        }
        return code != CLOSE_RESERVED && code != CLOSE_NO_STATUS && code != CLOSE_ABNORMAL;
    }

    /**
     * Writes a server frame header into `out` and answers its length.
     *
     * Never masked: RFC 6455 5.1 says a server MUST NOT mask, and a masked server
     * frame is a close on every client that checks. So there is no mask parameter
     * to get wrong, and the header is 2, 4 or 10 bytes.
     */
    static int writeHeader(byte[] out, int offset, int opcode, boolean fin, boolean rsv1,
                           long length) {
        int first = opcode & 0x0f;
        if(fin) {
            first |= 0x80;
        }
        if(rsv1) {
            first |= 0x40;
        }
        out[offset] = (byte)first;
        if(length < 126) {
            out[offset + 1] = (byte)length;
            return 2;
        }
        if(length <= 0xffffL) {
            out[offset + 1] = (byte)126;
            out[offset + 2] = (byte)((length >> 8) & 0xff);
            out[offset + 3] = (byte)(length & 0xff);
            return 4;
        }
        out[offset + 1] = (byte)127;
        for(int iter = 0 ; iter < 8 ; iter++) {
            out[offset + 2 + iter] = (byte)((length >> ((7 - iter) * 8)) & 0xff);
        }
        return 10;
    }

    /** The largest header {@link #writeHeader} can produce, for sizing a scratch buffer. */
    static final int MAX_SERVER_HEADER = 10;
}
