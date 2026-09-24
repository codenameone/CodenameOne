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

import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The resumable UTF-8 validator, and the guard on its duplication.
 *
 * {@link Utf8Stream} carries a copy of {@link Utf8}'s RFC 3629 table because the
 * two have genuinely different shapes -- one treats a sequence cut off by the end
 * of its range as invalid, the other treats exactly that as "ask me again". A copy
 * that nothing compares is a copy that drifts, so {@link #agreesWithTheWholeBufferForm}
 * is the load-bearing test in this file: it feeds both forms the same bytes, split
 * every way, and requires the same answer.
 */
class Utf8StreamTest {

    /** Accepts the whole array in `chunk`-sized pieces, the way frames arrive. */
    private static boolean streamed(byte[] bytes, int chunk) {
        Utf8Stream stream = new Utf8Stream();
        for(int at = 0 ; at < bytes.length ; at += chunk) {
            int length = Math.min(chunk, bytes.length - at);
            if(!stream.accept(bytes, at, length)) {
                return false;
            }
        }
        return stream.isComplete();
    }

    @Test
    @DisplayName("agrees with Utf8.isValid over every leading byte and every split")
    void agreesWithTheWholeBufferForm() {
        Random random = new Random(20260921L);
        for(int lead = 0 ; lead < 256 ; lead++) {
            for(int length = 1 ; length <= 4 ; length++) {
                for(int trial = 0 ; trial < 40 ; trial++) {
                    byte[] bytes = new byte[length];
                    bytes[0] = (byte)lead;
                    for(int iter = 1 ; iter < length ; iter++) {
                        // Mostly continuation bytes, sometimes not: the interesting
                        // failures are one byte outside the permitted range, not
                        // uniformly random noise.
                        bytes[iter] = (byte)(random.nextInt(3) == 0
                                ? random.nextInt(256) : 0x80 + random.nextInt(0x40));
                    }
                    boolean whole = Utf8.isValid(bytes, 0, bytes.length);
                    for(int chunk = 1 ; chunk <= length ; chunk++) {
                        assertEquals(whole, streamed(bytes, chunk),
                                "lead=" + lead + " length=" + length + " chunk=" + chunk);
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("agrees with Utf8.isValid over random buffers, split every possible way")
    void agreesOnRandomBuffers() {
        Random random = new Random(11L);
        for(int trial = 0 ; trial < 4000 ; trial++) {
            byte[] bytes = new byte[1 + random.nextInt(12)];
            random.nextBytes(bytes);
            boolean whole = Utf8.isValid(bytes, 0, bytes.length);
            for(int chunk = 1 ; chunk <= bytes.length ; chunk++) {
                assertEquals(whole, streamed(bytes, chunk), "chunk=" + chunk);
            }
        }
    }

    @Test
    @DisplayName("real text stays valid however a frame boundary cuts it")
    void textSplitMidCharacter() {
        // Two, three and four byte characters, which is the case Utf8.isValid
        // cannot express: a frame may legally end in the middle of any of them.
        // Escaped rather than written literally: a .java file in this tree has to
        // be pure ASCII, and a raw multi-byte character makes the build fail with
        // "unmappable character for encoding ASCII".
        String[] texts = {"hello", "sn\u00e9\u00e9uwl", "\u4f60\u597d\u4e16\u754c",
                          "\ud83d\ude00\ud83c\udf10", "a\u00e9\u4f60\ud83d\ude00z"};
        for(int iter = 0 ; iter < texts.length ; iter++) {
            byte[] bytes = texts[iter].getBytes(StandardCharsets.UTF_8);
            for(int chunk = 1 ; chunk <= bytes.length ; chunk++) {
                assertTrue(streamed(bytes, chunk),
                        "valid text rejected at chunk " + chunk + ": " + texts[iter]);
            }
        }
    }

    @Test
    @DisplayName("a sequence left unfinished is not complete, but is not yet a failure")
    void truncatedIsIncompleteNotInvalid() {
        // The first two bytes of a three-byte character.
        byte[] head = {(byte)0xe4, (byte)0xbd};
        Utf8Stream stream = new Utf8Stream();
        assertTrue(stream.accept(head, 0, head.length), "nothing is wrong yet");
        assertFalse(stream.isComplete(), "but the message cannot end here");
        byte[] tail = {(byte)0xa0};
        assertTrue(stream.accept(tail, 0, 1));
        assertTrue(stream.isComplete(), "the character finished in the next frame");
    }

    @Test
    @DisplayName("fails at the offending byte, not at the end of the message")
    void failsImmediately() {
        Utf8Stream stream = new Utf8Stream();
        byte[] bad = {(byte)0xc0};                 // an overlong two-byte lead
        assertFalse(stream.accept(bad, 0, 1),
                "a server that waits for FIN has already accepted whatever came after");
        // Sticky: the connection is going to be closed, and a later range must not
        // report success and let a retry loop carry on.
        byte[] fine = {'a', 'b'};
        assertFalse(stream.accept(fine, 0, fine.length), "failure is sticky");
        assertFalse(stream.isComplete());
    }

    @Test
    @DisplayName("the RFC 3629 boundaries, one byte either side")
    void tableBoundaries() {
        assertFalse(streamed(new byte[]{(byte)0xc0, (byte)0x80}, 1), "overlong two-byte");
        assertFalse(streamed(new byte[]{(byte)0xc1, (byte)0xbf}, 1), "overlong two-byte");
        assertTrue(streamed(new byte[]{(byte)0xc2, (byte)0x80}, 1), "U+0080");
        assertFalse(streamed(new byte[]{(byte)0xe0, (byte)0x9f, (byte)0xbf}, 1), "overlong three-byte");
        assertTrue(streamed(new byte[]{(byte)0xe0, (byte)0xa0, (byte)0x80}, 1), "U+0800");
        assertFalse(streamed(new byte[]{(byte)0xed, (byte)0xa0, (byte)0x80}, 1), "surrogate half");
        assertTrue(streamed(new byte[]{(byte)0xed, (byte)0x9f, (byte)0xbf}, 1), "just below the surrogates");
        assertFalse(streamed(new byte[]{(byte)0xf0, (byte)0x8f, (byte)0xbf, (byte)0xbf}, 1), "overlong four-byte");
        assertTrue(streamed(new byte[]{(byte)0xf0, (byte)0x90, (byte)0x80, (byte)0x80}, 1), "U+10000");
        assertTrue(streamed(new byte[]{(byte)0xf4, (byte)0x8f, (byte)0xbf, (byte)0xbf}, 1), "U+10FFFF");
        assertFalse(streamed(new byte[]{(byte)0xf4, (byte)0x90, (byte)0x80, (byte)0x80}, 1), "past U+10FFFF");
        for(int lead = 0xf5 ; lead <= 0xff ; lead++) {
            assertFalse(streamed(new byte[]{(byte)lead, (byte)0x80}, 1), "F5..FF encodes nothing: " + lead);
        }
        for(int lead = 0x80 ; lead <= 0xbf ; lead++) {
            assertFalse(streamed(new byte[]{(byte)lead}, 1), "continuation with nothing to continue: " + lead);
        }
    }

    @Test
    @DisplayName("reset returns the validator to the start for the next message")
    void resetClearsState() {
        Utf8Stream stream = new Utf8Stream();
        assertFalse(stream.accept(new byte[]{(byte)0xff}, 0, 1));
        stream.reset();
        assertTrue(stream.accept(new byte[]{'o', 'k'}, 0, 2));
        assertTrue(stream.isComplete());
    }
}
