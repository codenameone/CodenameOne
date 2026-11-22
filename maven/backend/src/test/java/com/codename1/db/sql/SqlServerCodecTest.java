/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * in the LICENSE file that accompanied this code.
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
package com.codename1.db.sql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Offline checks of the SQL Server value decoding.
class SqlServerCodecTest {
    /// A VARCHAR under the default collation is Windows-1252: the euro sign,
    /// curly quotes and dashes at 0x80-0x9F, Latin-1 everywhere else.
    @Test void varcharUnderTheDefaultCollationIsWindows1252() {
        byte[] data = {(byte) 0x80, (byte) 0x93, 'a', (byte) 0x94, (byte) 0x96, (byte) 0x85, (byte) 0xe9, (byte) 0x81};
        assertEquals("\u20ac\u201ca\u201d\u2013\u2026\u00e9\u0081", SqlServer.narrow(data, false));
        assertEquals("\u00e9", SqlServer.narrow(new byte[] {(byte) 0xc3, (byte) 0xa9}, true));
    }
}
