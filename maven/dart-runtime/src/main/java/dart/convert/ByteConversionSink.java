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
package dart.convert;

import dart.runtime.Funcs;

/// Dart's {@code ByteConversionSink}: a sink of byte chunks. {@link #withCallback}
/// collects every chunk and hands the callback all of them once closed.
public abstract class ByteConversionSink {

    // add and addSlice take the List<int> the stub declares, as DartLongList, so that a
    // transpiled subclass's override (typed from the Dart source) matches them.

    public abstract void add(dart.core.DartLongList chunk);

    public abstract void close();

    /// {@code addSlice(chunk, start, end, isLast)}: adds the slice, then closes when
    /// it is the last, as Dart's default does.
    public void addSlice(dart.core.DartLongList chunk, long start, long end, boolean isLast) {
        byte[] b = ByteCodec.bytesOf(chunk);
        dart.core.RangeError.checkValueInInterval(start, 0, b.length, "start");
        dart.core.RangeError.checkValueInInterval(end, start, b.length, "end");
        byte[] part = new byte[(int) (end - start)];
        System.arraycopy(b, (int) start, part, 0, part.length);
        add(dart.typed_data.Uint8List.fromBytes(part));
        if (isLast) {
            close();
        }
    }

    public static ByteConversionSink withCallback(final Funcs.VoidFunc1<Object> callback) {
        return new ByteConversionSink() {
            private final java.io.ByteArrayOutputStream all = new java.io.ByteArrayOutputStream();
            private boolean closed;

            @Override
            public void add(dart.core.DartLongList chunk) {
                byte[] b = ByteCodec.bytesOf(chunk);
                all.write(b, 0, b.length);
            }

            @Override
            public void close() {
                if (closed) {
                    return;
                }
                closed = true;
                // A List<int>, as Dart hands the callback; the transpiler types it as one
                byte[] b = all.toByteArray();
                dart.core.DartLongList out = new dart.core.DartLongList();
                for (byte x : b) {
                    out.addLong(x & 0xff);
                }
                callback.call(out);
            }
        };
    }
}
