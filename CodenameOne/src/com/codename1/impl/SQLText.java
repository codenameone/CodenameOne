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
package com.codename1.impl;

import java.io.IOException;

/// Carries SQL text between a port and its native SQLite engine as UTF-8 bytes.
///
/// SQLite stores TEXT as UTF-8 and its C API measures every text value in bytes, so a port written
/// against that API has to agree with the framework on both the encoding and the length. Passing a
/// C string instead loses on both counts: the length becomes "up to the first zero byte", which
/// truncates a value holding the character with code point zero -- SQLite stores that perfectly
/// well, and Android, the simulator and the browser all round-trip it -- and the byte-to-character
/// conversions available in the VM's C runtime are not UTF-8 aware, so anything outside ASCII comes
/// back as one character per byte.
///
/// Exchanging a `byte[]` removes both problems: the length is the array's, and the encoding is the
/// same `String` conversion every other port already relies on.
public final class SQLText {

    private SQLText() {
    }

    /// Encodes a string as the UTF-8 bytes a native `sqlite3_bind_text` call expects.
    ///
    /// #### Parameters
    ///
    /// - `value`: the string to encode, may be null
    ///
    /// #### Returns
    ///
    /// the UTF-8 bytes, or null if the value was null
    ///
    /// #### Throws
    ///
    /// - `IOException`: if this VM has no UTF-8 charset, which would make SQL text unusable
    public static byte[] toUTF8(String value) throws IOException {
        if (value == null) {
            return null;
        }
        return value.getBytes("UTF-8");
    }

    /// Decodes the UTF-8 bytes of a native SQLite text value.
    ///
    /// #### Parameters
    ///
    /// - `utf8`: the bytes read from the engine, may be null
    ///
    /// #### Returns
    ///
    /// the decoded string, or null if the bytes were null
    ///
    /// #### Throws
    ///
    /// - `IOException`: if this VM has no UTF-8 charset
    public static String fromUTF8(byte[] utf8) throws IOException {
        if (utf8 == null) {
            return null;
        }
        return new String(utf8, "UTF-8");
    }
}
