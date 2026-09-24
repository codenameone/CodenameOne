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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The RFC 6455 frame layer, driven directly.
 *
 * Almost every case here is a frame no conformant client will ever send, which is
 * the reason this test exists at all: the functional half of a websocket server is
 * covered by any client at all talking to it, and the protocol half is covered by
 * nothing unless the frames are built by hand.
 */
class WebSocketFramesTest {

    @Test
    @DisplayName("every length class round-trips through writeHeader and back")
    void lengthRoundTrip() {
        long[] lengths = {0, 1, 2, 125, 126, 127, 200, 65534, 65535, 65536, 65537,
                          1L << 20, (1L << 31) + 5};
        for(int iter = 0 ; iter < lengths.length ; iter++) {
            long length = lengths[iter];
            byte[] out = new byte[WebSocketFrames.MAX_SERVER_HEADER];
            int written = WebSocketFrames.writeHeader(out, 0, WebSocketFrames.OP_BINARY,
                    true, false, length);
            assertEquals(written, WebSocketFrames.headerLength(out, 0, written),
                    "header length disagrees with what was written, for " + length);
            assertEquals(length, WebSocketFrames.payloadLength(out, 0),
                    "payload length round-trip, for " + length);
            assertTrue(WebSocketFrames.lengthIsMinimal(out, 0),
                    "what this class writes must itself be minimal, for " + length);
            assertTrue(WebSocketFrames.fin(out, 0));
            assertEquals(0, WebSocketFrames.rsv(out, 0));
            assertEquals(WebSocketFrames.OP_BINARY, WebSocketFrames.opcode(out, 0));
        }
    }

    @Test
    @DisplayName("a server frame is never masked")
    void serverFramesAreNotMasked() {
        byte[] out = new byte[WebSocketFrames.MAX_SERVER_HEADER];
        // There is no mask parameter to get wrong; this pins that it stays that way.
        WebSocketFrames.writeHeader(out, 0, WebSocketFrames.OP_TEXT, true, false, 70000);
        assertFalse(WebSocketFrames.masked(out, 0));
    }

    @Test
    @DisplayName("a header that has not all arrived reports NEED_MORE, at every truncation")
    void partialHeaderNeedsMore() {
        byte[] out = new byte[WebSocketFrames.MAX_SERVER_HEADER];
        long[] lengths = {5, 200, 70000};
        for(int iter = 0 ; iter < lengths.length ; iter++) {
            int written = WebSocketFrames.writeHeader(out, 0, WebSocketFrames.OP_BINARY,
                    true, false, lengths[iter]);
            for(int avail = 0 ; avail < written ; avail++) {
                assertEquals(WebSocketFrames.NEED_MORE,
                        WebSocketFrames.headerLength(out, 0, avail),
                        "avail=" + avail + " for length " + lengths[iter]);
            }
            assertEquals(written, WebSocketFrames.headerLength(out, 0, written));
        }
    }

    @Test
    @DisplayName("a masked client header is four bytes longer, and the mask is where it says")
    void maskedHeaderLength() {
        // 0x81 = FIN|TEXT, 0x85 = masked, length 5
        byte[] frame = {(byte)0x81, (byte)0x85, 1, 2, 3, 4, 0, 0, 0, 0, 0};
        assertTrue(WebSocketFrames.masked(frame, 0));
        assertEquals(6, WebSocketFrames.headerLength(frame, 0, frame.length));
        assertEquals(2, WebSocketFrames.maskOffset(frame, 0));
        assertEquals(5, WebSocketFrames.payloadLength(frame, 0));
    }

    @Test
    @DisplayName("a length that could have been spelled shorter is refused")
    void nonMinimalLengthsAreRefused() {
        // Three bytes announced with the 16-bit form.
        byte[] twoByte = {(byte)0x82, (byte)126, 0, 3};
        assertEquals(3, WebSocketFrames.payloadLength(twoByte, 0), "it still decodes");
        assertFalse(WebSocketFrames.lengthIsMinimal(twoByte, 0), "but it is not minimal");

        // Two hundred bytes announced with the 64-bit form.
        byte[] eightByte = {(byte)0x82, (byte)127, 0, 0, 0, 0, 0, 0, 0, (byte)200};
        assertEquals(200, WebSocketFrames.payloadLength(eightByte, 0));
        assertFalse(WebSocketFrames.lengthIsMinimal(eightByte, 0));

        // 126 and 65536 are the smallest values their forms are allowed to carry.
        byte[] legal16 = {(byte)0x82, (byte)126, 0, 126};
        assertTrue(WebSocketFrames.lengthIsMinimal(legal16, 0));
        byte[] legal64 = {(byte)0x82, (byte)127, 0, 0, 0, 0, 0, 1, 0, 0};
        assertEquals(65536, WebSocketFrames.payloadLength(legal64, 0));
        assertTrue(WebSocketFrames.lengthIsMinimal(legal64, 0));
    }

    @Test
    @DisplayName("a 64-bit length with the top bit set answers -1 rather than a negative size")
    void topBitLengthIsRefused() {
        byte[] frame = {(byte)0x82, (byte)127, (byte)0x80, 0, 0, 0, 0, 0, 0, 1};
        assertEquals(-1, WebSocketFrames.payloadLength(frame, 0),
                "a negative size would reach an array allocation before anything checked it");
        assertFalse(WebSocketFrames.lengthIsMinimal(frame, 0));
    }

    @Test
    @DisplayName("RSV1 is readable, and writeHeader sets only the bit it was asked for")
    void reservedBits() {
        byte[] out = new byte[WebSocketFrames.MAX_SERVER_HEADER];
        WebSocketFrames.writeHeader(out, 0, WebSocketFrames.OP_TEXT, false, true, 5);
        assertEquals(4, WebSocketFrames.rsv(out, 0), "RSV1 is the high bit of the three");
        assertFalse(WebSocketFrames.fin(out, 0));
        WebSocketFrames.writeHeader(out, 0, WebSocketFrames.OP_TEXT, true, false, 5);
        assertEquals(0, WebSocketFrames.rsv(out, 0));
    }

    @Test
    @DisplayName("control opcodes are told apart from data opcodes")
    void controlClassification() {
        assertTrue(WebSocketFrames.isControl(WebSocketFrames.OP_CLOSE));
        assertTrue(WebSocketFrames.isControl(WebSocketFrames.OP_PING));
        assertTrue(WebSocketFrames.isControl(WebSocketFrames.OP_PONG));
        assertFalse(WebSocketFrames.isControl(WebSocketFrames.OP_CONTINUATION));
        assertFalse(WebSocketFrames.isControl(WebSocketFrames.OP_TEXT));
        assertFalse(WebSocketFrames.isControl(WebSocketFrames.OP_BINARY));
        // The reserved opcodes fall on the right side of the line too, which is
        // what lets a decoder refuse them without a second table.
        for(int opcode = 0x3 ; opcode <= 0x7 ; opcode++) {
            assertFalse(WebSocketFrames.isControl(opcode), "reserved data opcode " + opcode);
        }
        for(int opcode = 0xb ; opcode <= 0xf ; opcode++) {
            assertTrue(WebSocketFrames.isControl(opcode), "reserved control opcode " + opcode);
        }
    }

    @Test
    @DisplayName("close codes: the three holes inside 1000..1011 are not a range check")
    void closeCodes() {
        int[] permitted = {1000, 1001, 1002, 1003, 1007, 1008, 1009, 1010, 1011, 3000, 4000, 4999};
        for(int iter = 0 ; iter < permitted.length ; iter++) {
            assertTrue(WebSocketFrames.isValidCloseCode(permitted[iter]),
                    "should be permitted: " + permitted[iter]);
        }
        // 1004 is the one a range check lets through: it sits between 1003 and
        // 1005 and has no meaning. 1005 and 1006 are what a local implementation
        // reports, so a PEER may not claim either. 1015 is the same, for TLS.
        int[] refused = {-1, 0, 999, 1004, 1005, 1006, 1012, 1013, 1014, 1015, 2999, 5000, 65536};
        for(int iter = 0 ; iter < refused.length ; iter++) {
            assertFalse(WebSocketFrames.isValidCloseCode(refused[iter]),
                    "should be refused: " + refused[iter]);
        }
    }

    @Test
    @DisplayName("the mask phase survives a payload split anywhere, which is the common case")
    void unmaskAcrossArbitrarySplits() {
        Random random = new Random(20260921L);
        for(int trial = 0 ; trial < 5000 ; trial++) {
            int length = random.nextInt(300);
            byte[] plain = new byte[length];
            random.nextBytes(plain);
            byte[] key = new byte[4];
            random.nextBytes(key);

            // XOR is its own inverse, so masking is the same call.
            byte[] wire = Arrays.copyOf(plain, length);
            WebSocketFrames.unmask(wire, 0, length, key, 0);

            byte[] recovered = Arrays.copyOf(wire, length);
            int phase = 0;
            int at = 0;
            while(at < length) {
                int chunk = Math.min(1 + random.nextInt(7), length - at);
                phase = WebSocketFrames.unmask(recovered, at, chunk, key, phase);
                at += chunk;
            }
            assertArrayEquals(plain, recovered,
                    "a payload that did not arrive whole must still unmask, length " + length);
        }
    }

    @Test
    @DisplayName("restarting the mask key per chunk corrupts the payload, so the phase is load-bearing")
    void unmaskWithoutPhaseIsWrong() {
        byte[] plain = {10, 20, 30, 40, 50, 60, 70};
        byte[] key = {1, 2, 3, 4};
        byte[] wire = Arrays.copyOf(plain, plain.length);
        WebSocketFrames.unmask(wire, 0, wire.length, key, 0);

        byte[] naive = Arrays.copyOf(wire, wire.length);
        WebSocketFrames.unmask(naive, 0, 3, key, 0);
        WebSocketFrames.unmask(naive, 3, 4, key, 0);      // phase reset: the bug
        assertFalse(Arrays.equals(plain, naive),
                "if this ever passes, the phase argument has stopped doing anything");
    }
}
