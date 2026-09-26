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
package dart.core;

import dart.runtime.DartRuntime;

/**
 * Dart's {@code dart:core} {@code StringBuffer} — a mutable sequence of
 * characters used to build strings efficiently. Backed by a
 * {@link java.lang.StringBuilder}.
 *
 * <p>Dart's {@code write}/{@code writeln} accept any {@code Object?} and append
 * its Dart string representation (via {@link DartRuntime#str(Object)} so a Dart
 * object's {@code toString()} semantics are honoured, and {@code null} renders
 * as {@code "null"}). {@code writeCharCode} appends the UTF-16 code unit.</p>
 */
public final class StringBuffer {

    private final StringBuilder sb = new StringBuilder();

    public StringBuffer() {
    }

    /** {@code StringBuffer([Object content = ""])} — seeds with the content's string. */
    public StringBuffer(Object content) {
        sb.append(DartRuntime.str(content));
    }

    /** Dart's {@code StringBuffer.length} getter — number of UTF-16 code units. */
    public long length() {
        return sb.length();
    }

    /** Dart's {@code StringBuffer.isEmpty} getter. */
    public boolean isEmpty() {
        return sb.length() == 0;
    }

    /** Dart's {@code StringBuffer.isNotEmpty} getter. */
    public boolean isNotEmpty() {
        return sb.length() != 0;
    }

    /** Dart's {@code StringBuffer.write(Object? object)}. */
    public void write(Object object) {
        sb.append(DartRuntime.str(object));
    }

    /** Dart's {@code StringBuffer.writeln([Object? object = ""])}. */
    public void writeln() {
        sb.append('\n');
    }

    public void writeln(Object object) {
        sb.append(DartRuntime.str(object));
        sb.append('\n');
    }

    /** Dart's {@code StringBuffer.writeCharCode(int charCode)}. */
    public void writeCharCode(long charCode) {
        if (charCode < 0 || charCode > 0x10FFFF) {
            throw new RangeError("Invalid value: Not in inclusive range 0..1114111: " + charCode);
        }
        if (charCode > 0xFFFF) {
            // A supplementary code point is TWO UTF-16 code units. Narrowing it to
            // one char kept only the low bits: 0x1F600 became U+F600.
            long v = charCode - 0x10000;
            sb.append((char) (0xD800 + (v >> 10))).append((char) (0xDC00 + (v & 0x3FF)));
            return;
        }
        sb.append((char) charCode);
    }

    /** Dart's {@code StringBuffer.writeAll(Iterable objects, [String separator = ""])}. */
    public void writeAll(Iterable<?> objects) {
        writeAll(objects, "");
    }

    public void writeAll(Iterable<?> objects, String separator) {
        boolean first = true;
        for (Object o : objects) {
            if (!first && separator != null) {
                sb.append(separator);
            }
            sb.append(DartRuntime.str(o));
            first = false;
        }
    }

    /** Dart's {@code StringBuffer.clear()}. */
    public void clear() {
        sb.setLength(0);
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
