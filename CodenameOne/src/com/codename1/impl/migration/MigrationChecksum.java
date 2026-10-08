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
package com.codename1.impl.migration;

import com.codename1.io.gzip.CRC32;

/// The checksum recorded beside an applied script, computed the way Flyway computes it so a
/// history written by either tool validates under the other: a CRC32 over the UTF-8 bytes of
/// each line, with the line terminators left out and a leading byte order mark dropped.
///
/// Leaving the terminators out is what makes the value survive a checkout that rewrote line
/// endings. The same script reaches a device and a server through different packaging, and a
/// checksum that noticed CRLF would fail validation on a file nobody edited.
///
/// Internal migration runtime; not an application API.
/// @hidden
@com.codename1.impl.SharedWithBackend
public final class MigrationChecksum {
    private MigrationChecksum() {
    }

    /// Computes the checksum of a script.
    public static int of(String script) {
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[4096];
        int used = 0;
        int length = script.length();
        int i = 0;
        if (length > 0 && script.charAt(0) == 0xFEFF) {
            i = 1;
        }
        while (i < length) {
            char c = script.charAt(i++);
            if (c == '\n' || c == '\r') {
                continue;
            }
            if (used > buffer.length - 4) {
                crc.update(buffer, 0, used);
                used = 0;
            }
            if (c < 0x80) {
                buffer[used++] = (byte) c;
            } else if (c < 0x800) {
                buffer[used++] = (byte) (0xC0 | (c >> 6));
                buffer[used++] = (byte) (0x80 | (c & 0x3F));
            } else if (c >= 0xD800 && c <= 0xDBFF && i < length && script.charAt(i) >= 0xDC00
                    && script.charAt(i) <= 0xDFFF) {
                int point = 0x10000 + ((c - 0xD800) << 10) + (script.charAt(i++) - 0xDC00);
                buffer[used++] = (byte) (0xF0 | (point >> 18));
                buffer[used++] = (byte) (0x80 | ((point >> 12) & 0x3F));
                buffer[used++] = (byte) (0x80 | ((point >> 6) & 0x3F));
                buffer[used++] = (byte) (0x80 | (point & 0x3F));
            } else if (c >= 0xD800 && c <= 0xDFFF) {
                // An unpaired surrogate has no UTF-8 form; the JDK encoder writes '?', and the
                // build computes its literal with the JDK.
                buffer[used++] = (byte) '?';
            } else {
                buffer[used++] = (byte) (0xE0 | (c >> 12));
                buffer[used++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                buffer[used++] = (byte) (0x80 | (c & 0x3F));
            }
        }
        if (used > 0) {
            crc.update(buffer, 0, used);
        }
        return (int) crc.getValue();
    }
}
