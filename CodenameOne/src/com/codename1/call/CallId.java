/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.call;

import java.util.Random;

/// Canonical form of the identifier that names one call everywhere: in this
/// API, in CallKit, in Telecom, and in the VoIP push payload a server sends.
///
/// It is an RFC 4122 identifier written as 36 characters, **uppercase**, with
/// hyphens -- `6B29FC40-CA47-1067-B31D-00DD010662DA`. The case is fixed
/// rather than ignored because the identifier is compared as a string on
/// every hop, including by a server that did not come from this API, and
/// "compare case-insensitively everywhere" is a rule that only has to be
/// forgotten once.
///
/// #### Who allocates one
///
/// - For a call this app places or learns about over its own connection,
///   **this app allocates** with [#random()].
/// - For a call that arrives as a VoIP push, **the sending server
///   allocates** and the identifier travels in the payload, because on iOS
///   the call must be reported to the system before any of this app's code
///   runs. See [com.codename1.call.voip.VoipPush].
///
/// Either way the same identifier must be used by both ends for the whole
/// life of the call, or the two sides will disagree about which call an
/// action refers to.
///
/// This class is not instantiated; it holds the format and the generator.
public final class CallId {

    /// Uppercase hex digits. `Character.forDigit` is not part of the device
    /// API, so the table is spelled out.
    private static final char[] HEX = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static final Random RANDOM = new Random();

    private CallId() {
    }

    /// A fresh random (version 4) identifier in canonical form.
    ///
    /// @return 36 uppercase characters with hyphens
    public static String random() {
        byte[] bytes = new byte[16];
        synchronized (RANDOM) {
            RANDOM.nextBytes(bytes);
        }
        // Version 4, variant 1, per RFC 4122. Without these the identifier
        // is still unique but is not a valid UUID, and CallKit rejects it.
        bytes[6] = (byte) ((bytes[6] & 0x0f) | 0x40);
        bytes[8] = (byte) ((bytes[8] & 0x3f) | 0x80);
        return format(bytes);
    }

    /// Renders 16 bytes as a canonical identifier.
    ///
    /// @param bytes exactly 16 bytes
    /// @return the canonical form
    /// @throws IllegalArgumentException if `bytes` is not 16 long
    public static String format(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            throw new IllegalArgumentException("A call id is exactly 16 bytes");
        }
        StringBuilder sb = new StringBuilder(36);
        for (int i = 0; i < 16; i++) {
            if (i == 4 || i == 6 || i == 8 || i == 10) {
                sb.append('-');
            }
            int v = bytes[i] & 0xff;
            sb.append(HEX[v >> 4]).append(HEX[v & 0x0f]);
        }
        return sb.toString();
    }

    /// Whether `id` is a canonical identifier: 36 characters, hyphens in the
    /// four expected places, hex everywhere else. Case is **not** checked
    /// here -- use [#normalize(String)] to both check and fix the case.
    ///
    /// @param id the candidate, may be null
    /// @return true if the shape is right
    public static boolean isValid(String id) {
        if (id == null || id.length() != 36) {
            return false;
        }
        for (int i = 0; i < 36; i++) {
            char c = id.charAt(i);
            if (i == 8 || i == 13 || i == 18 || i == 23) {
                if (c != '-') {
                    return false;
                }
            } else if (!isHex(c)) {
                return false;
            }
        }
        return true;
    }

    /// Upper-cases a well-formed identifier, or returns null if it is not
    /// well-formed.
    ///
    /// Ports call this on the way in, so a lowercase identifier from a
    /// server payload is accepted and stored canonically rather than
    /// becoming a call nothing can later find. A null return is the
    /// signal to answer [CallError#INVALID_ID]; this never throws, because
    /// the value routinely comes from off the device.
    ///
    /// @param id the candidate, may be null
    /// @return the canonical form, or null if `id` is not an identifier
    public static String normalize(String id) {
        if (!isValid(id)) {
            return null;
        }
        StringBuilder sb = new StringBuilder(36);
        for (int i = 0; i < 36; i++) {
            char c = id.charAt(i);
            if (c >= 'a' && c <= 'f') {
                c = (char) (c - ('a' - 'A'));
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
