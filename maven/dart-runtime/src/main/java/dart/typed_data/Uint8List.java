/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.typed_data;

import java.util.List;

/**
 * Dart's {@code dart:typed_data} Uint8List: a fixed-length list of unsigned
 * 8-bit integers backed by a Java {@code byte[]}. Only the surface used by
 * the Flutter gallery (construction + length) is implemented.
 */
public final class Uint8List {

    private final byte[] bytes;

    public Uint8List(long length) {
        // Checked on the long, before narrowing, as ByteData and the lists are:
        // Uint8List(2^32) cast to an empty list instead of failing.
        dart.core.RangeError.checkNotNegative(length, "length");
        dart.core.RangeError.checkAllocatable(length);
        this.bytes = new byte[(int) length];
    }

    private Uint8List(byte[] bytes) {
        this.bytes = bytes;
    }

    /** {@code Uint8List.fromList(<int>[...])}. */
    public static Uint8List fromList(List<?> elements) {
        byte[] b = new byte[elements.size()];
        for (int i = 0; i < b.length; i++) {
            Object o = elements.get(i);
            b[i] = o instanceof Number ? ((Number) o).byteValue() : 0;
        }
        return new Uint8List(b);
    }

    public long length() {
        return bytes.length;
    }

    /** The raw backing array (used by image decoders). */
    public byte[] toBytes() {
        return bytes;
    }
}
