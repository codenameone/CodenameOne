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
package com.codename1.security.vault;

import com.codename1.junit.UITestBase;
import com.codename1.security.Base32;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/// The two encoders that exist so a secret never becomes a String.
///
/// The checks on how Vault USES them live in VaultSourceInvariantsTest, with the other
/// properties that are about the shape of the code rather than about behaviour.
///
/// A String cannot be zeroed and a StringBuilder's buffer cannot be reached, so anything routed
/// through either survives in the heap after every array the caller can wipe has been wiped --
/// which is the opposite of what `getSecret` and `createRecoveryCode` promise about the
/// characters they hand back. Both encoders are private copies of logic that also exists in its
/// String-producing form, so what these tests hold is that the copies cannot drift.
class BytesTest extends UITestBase {

    @Test
    void base32CharsMatchesTheStringEncoderExactly() {
        // A private copy of an encoding is safe only while it agrees with the one the rest of the
        // system uses. Held against Base32.encode rather than against a table of expected
        // strings, so a change to either side fails here instead of producing recovery codes the
        // alphabet no longer describes.
        Random random = new Random(20260915L);
        for (int length = 0; length <= 40; length++) {
            byte[] data = new byte[length];
            random.nextBytes(data);
            assertEquals(Base32.encode(data), new String(Bytes.base32Chars(data)),
                    "base32Chars disagreed at length " + length);
        }
    }

    @Test
    void base32CharsCoversThePaddingCases() {
        // Every remainder mod 5, because the padding run is the part a hand-written copy gets
        // wrong and the part a random sweep can under-sample.
        for (int length = 1; length <= 11; length++) {
            byte[] data = new byte[length];
            for (int iter = 0; iter < length; iter++) {
                data[iter] = (byte) (iter * 37 + 11);
            }
            char[] chars = Bytes.base32Chars(data);
            assertEquals(Base32.encode(data), new String(chars));
            assertEquals(((length + 4) / 5) * 8, chars.length, "padded length at " + length);
            // And it round-trips through the decoder the vault would actually use.
            assertArrayEquals(data, Base32.decode(new String(chars)));
        }
        assertEquals(0, Bytes.base32Chars(new byte[0]).length);
        assertEquals(0, Bytes.base32Chars(null).length);
    }

    @Test
    void charsFromUtf8MatchesTheStringDecoderExactly() {
        // Same contract on the decode side: identical answers to fromUtf8, including for the
        // malformed input a decoder is most likely to diverge on.
        String[] cases = {
            "", "a", "hello world", "\u00e9\u00e8\u00ea", "\u4f60\u597d",
            "\ud83d\ude00 emoji", "mixed \u00e9 \u4f60 \ud83d\ude00 end",
        };
        for (String one : cases) {
            byte[] utf8 = Bytes.utf8(one);
            assertEquals(Bytes.fromUtf8(utf8, 0, utf8.length),
                    new String(Bytes.charsFromUtf8(utf8, 0, utf8.length)),
                    "disagreed on " + one.length() + " chars");
        }
        // Malformed: a truncated sequence, a stray continuation byte, an over-long code point.
        byte[][] broken = {
            { (byte) 0xe4, (byte) 0xbd },
            { (byte) 0x80, 0x41 },
            { (byte) 0xf7, (byte) 0xbf, (byte) 0xbf, (byte) 0xbf },
            { (byte) 0xc3 },
            { 0x41, (byte) 0xff, 0x42 },
        };
        for (byte[] one : broken) {
            assertEquals(Bytes.fromUtf8(one, 0, one.length),
                    new String(Bytes.charsFromUtf8(one, 0, one.length)),
                    "disagreed on malformed input");
        }
    }

    @Test
    void charsFromUtf8RoundTripsWhatTheEncoderProduces() {
        // The pair the vault actually uses: putSecret encodes with Bytes.utf8(char[]) and
        // getSecret decodes with this.
        Random random = new Random(20260915L);
        for (int iter = 0; iter < 200; iter++) {
            int length = random.nextInt(40);
            char[] value = new char[length];
            for (int at = 0; at < length; at++) {
                // Basic multilingual plane only: a lone surrogate is not a round-trippable
                // input and fromUtf8 documents what it substitutes instead.
                int cp = random.nextInt(0xd000) + 1;
                value[at] = (char) cp;
            }
            byte[] utf8 = Bytes.utf8(value);
            assertArrayEquals(value, Bytes.charsFromUtf8(utf8, 0, utf8.length),
                    "round trip failed at length " + length);
        }
        assertEquals(0, Bytes.charsFromUtf8(null, 0, 0).length);
        assertEquals(0, Bytes.charsFromUtf8(new byte[4], 0, 0).length);
    }
}
