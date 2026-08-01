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
package com.codename1.ai.inference;

/// Describes where an {@link InferenceSession} should obtain a `.tflite`
/// model. Byte sources are defensively copied. File sources are especially
/// useful with {@link ModelCache} because native ports can open the cached
/// path without loading the full model into the Java heap.
public final class ModelSource {
    public static final int BYTES = 1;
    public static final int FILE = 2;
    public static final int RESOURCE = 3;

    private final int kind;
    private final byte[] bytes;
    private final String path;

    private ModelSource(int kind, byte[] bytes, String path) {
        this.kind = kind;
        this.bytes = bytes;
        this.path = path;
    }

    /// Creates an in-memory model source.
    /// @param value complete model bytes, copied by this method
    /// @return a byte-backed source
    public static ModelSource bytes(byte[] value) {
        if (value == null || value.length == 0) {
            throw new IllegalArgumentException("Model bytes must not be empty");
        }
        byte[] copy = new byte[value.length];
        System.arraycopy(value, 0, copy, 0, value.length);
        return new ModelSource(BYTES, copy, null);
    }

    /// Creates a source for an existing private filesystem path.
    /// @param path path understood by {@code FileSystemStorage}
    /// @return a file-backed source; the session never owns or deletes the file
    public static ModelSource file(String path) {
        return named(FILE, path);
    }

    /// Creates a source for a model packaged in application resources.
    /// @param path absolute classpath-style resource path
    /// @return a resource-backed source
    public static ModelSource resource(String path) {
        return named(RESOURCE, path);
    }

    private static ModelSource named(int kind, String path) {
        if (path == null || path.length() == 0) {
            throw new IllegalArgumentException("Model path must not be empty");
        }
        return new ModelSource(kind, null, path);
    }

    /// @return {@link #BYTES}, {@link #FILE}, or {@link #RESOURCE}
    public int getKind() {
        return kind;
    }

    /// @return a defensive copy of model bytes, or {@code null} for non-byte sources
    public byte[] getBytes() {
        if (bytes == null) {
            return null;
        }
        byte[] out = new byte[bytes.length];
        System.arraycopy(bytes, 0, out, 0, bytes.length);
        return out;
    }

    /// Returns the internal model byte array without copying it.
    ///
    /// This escape hatch is intended for native backend handoff and
    /// memory-sensitive applications. The returned array must be treated as
    /// read-only: modifying it changes the model source and violates this
    /// class's immutability contract. Prefer {@link #getBytes()} unless the
    /// additional full-model copy is known to be unacceptable.
    ///
    /// @return internal model bytes, or {@code null} for non-byte sources
    public byte[] getBytesUnsafe() {
        return bytes;
    }

    /// @return the file/resource path, or {@code null} for a byte source
    public String getPath() {
        return path;
    }
}
