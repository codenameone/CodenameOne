/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps.vector;

import com.codename1.io.Util;
import com.codename1.io.gzip.GZIPInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/// Small helpers shared by the tile sources and cache.
final class TileUtil {

    private TileUtil() {
    }

    /// A canonical cache/identity key for a slippy-map tile address.
    static String key(int z, int x, int y) {
        return z + "/" + x + "/" + y;
    }

    /// Transparently gunzips `data` when it carries the gzip magic header,
    /// otherwise returns it unchanged. Vector tiles are frequently served
    /// gzip-compressed regardless of the `Content-Encoding` the HTTP stack
    /// reports.
    static byte[] maybeGunzip(byte[] data) throws IOException {
        if (data == null || data.length < 2) {
            return data;
        }
        if ((data[0] & 0xFF) == 0x1F && (data[1] & 0xFF) == 0x8B) {
            GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data));
            try {
                return Util.readInputStream(in);
            } finally {
                in.close();
            }
        }
        return data;
    }
}
