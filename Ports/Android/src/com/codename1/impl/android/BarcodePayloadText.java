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
package com.codename1.impl.android;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;

/// Turns the payload bytes of a scanned barcode into text.
///
/// Lives outside the `ai` package, which only compiles inside an application
/// build that has ML Kit, so that the port's own build compiles and tests it.
public final class BarcodePayloadText {
    private BarcodePayloadText() {
    }

    /// Decodes `bytes` as text.
    ///
    /// With no `charsetName`: as UTF-8 when the bytes are valid UTF-8, else as
    /// ISO-8859-1, which maps every byte and so never fails.
    ///
    /// With a `charsetName` (the `android.barcodeCharset` Display property):
    /// bytes that are valid UTF-8 *and* contain a multi-byte sequence are
    /// still read as UTF-8, and only the rest are read in the named charset.
    /// The named charset alone cannot act as the test: the single-byte
    /// charsets an application names here (windows-1250, ISO-8859-2) map
    /// almost every byte, so decoding UTF-8 in them never fails -- it just
    /// turns each accented letter into two wrong ones. Measured on Croatian HUB-3
    /// payment slips, where one issuer encodes UTF-8 and another windows-1250
    /// and an app must read both. The reverse misreading is rare: text in a
    /// single-byte charset is valid UTF-8 only if every byte from 0xC2 up is
    /// followed by the exact continuation bytes UTF-8 requires, which
    /// ordinary accented text does not do. Pure ASCII reads the same either
    /// way. A leading UTF-8 byte order mark is dropped. A named charset that cannot be used -- an unknown name, or bytes
    /// not valid in it -- falls back to the default decoding.
    public static String decode(byte[] bytes, String charsetName) {
        String utf8 = utf8OrNull(bytes);
        if (charsetName != null && charsetName.length() > 0
                && (utf8 == null || !hasNonAscii(bytes))) {
            try {
                return strict(bytes, Charset.forName(charsetName));
            } catch (IllegalArgumentException unsupported) {
                // An unknown charset name, and...
            } catch (CharacterCodingException notInCharset) {
                // ...bytes that are not valid in the named charset both fall
                // back to the default decoding rather than losing the code.
            }
        }
        if (utf8 != null) {
            // A UTF-8 byte order mark is a marker, not text.
            if (utf8.length() > 0 && utf8.charAt(0) == (char) 0xFEFF) {
                return utf8.substring(1);
            }
            return utf8;
        }
        return new String(bytes, Charset.forName("ISO-8859-1"));
    }

    private static String utf8OrNull(byte[] bytes) {
        try {
            return strict(bytes, Charset.forName("UTF-8"));
        } catch (CharacterCodingException notUtf8) {
            return null;
        }
    }

    private static boolean hasNonAscii(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] < 0) {
                return true;
            }
        }
        return false;
    }

    private static String strict(byte[] bytes, Charset charset)
            throws CharacterCodingException {
        return charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
    }
}
