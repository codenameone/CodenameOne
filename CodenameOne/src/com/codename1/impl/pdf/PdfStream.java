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
package com.codename1.impl.pdf;

import java.io.IOException;

/// A parsed stream: its dictionary and its encoded data.
@com.codename1.impl.SharedWithBackend
public final class PdfStream extends PdfDict {
    /// The data as stored in the file, after decryption.
    public byte[] raw;
    private byte[] decoded;

    /// The data with every filter but an image one undone; see
    /// [PdfFilters#decode(PdfStream)]. Small results (fonts, functions,
    /// shared forms) are cached; a large one is decoded again when asked, so
    /// a file's images do not all stay in memory for the life of the file.
    public byte[] getData() throws IOException {
        if (decoded != null) {
            return decoded;
        }
        byte[] d = PdfFilters.decode(this);
        if (d.length <= CACHE_LIMIT) {
            decoded = d;
        }
        return d;
    }

    private static final int CACHE_LIMIT = 1 << 20;
}
