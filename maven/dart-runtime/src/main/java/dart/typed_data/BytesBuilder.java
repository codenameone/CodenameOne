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

/// Dart's {@code BytesBuilder}: accumulates byte lists into one Uint8List.
public final class BytesBuilder {
    private final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    private boolean copy = true;

    public BytesBuilder() {
        this(true);
    }

    public BytesBuilder(boolean copy) {
        this.copy = copy;
    }

    public void add(Object bytes) {
        if (bytes instanceof Uint8List) {
            byte[] b = ((Uint8List) bytes).toBytes();
            out.write(b, 0, b.length);
            return;
        }
        if (bytes instanceof Iterable) {
            for (Object o : (Iterable<?>) bytes) {
                out.write((int) ((Number) o).longValue());
            }
        }
    }

    public void addByte(long b) {
        out.write((int) b);
    }

    public Uint8List takeBytes() {
        Uint8List r = Uint8List.fromBytes(out.toByteArray());
        out.reset();
        return r;
    }

    public Uint8List toBytes() {
        return Uint8List.fromBytes(out.toByteArray());
    }

    public long length() {
        return out.size();
    }

    public boolean isEmpty() {
        return out.size() == 0;
    }

    public boolean isNotEmpty() {
        return out.size() != 0;
    }

    public void clear() {
        out.reset();
    }

    /// The named {@code copy:} argument. Every add copies here regardless.
    public void copy(boolean copy) {
        this.copy = copy;
    }

    boolean copies() {
        return copy;
    }
}
