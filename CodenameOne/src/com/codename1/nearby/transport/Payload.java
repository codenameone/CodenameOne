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
package com.codename1.nearby.transport;

import java.util.concurrent.atomic.AtomicInteger;

/// Something to send to a connected endpoint: either a block of bytes or a
/// file.
///
/// Bytes are the simple case and are capped at
/// [NearbyTransport#getMaxPayloadSize()], which is a few kilobytes on both
/// platforms. Anything larger goes as a file, which streams and reports
/// progress.
public final class Payload {

    /// This payload carries bytes; [#getBytes()] has them.
    public static final int TYPE_BYTES = 0;

    /// This payload carries a file; [#getPath()] names it.
    public static final int TYPE_FILE = 1;

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final int id;
    private final int type;
    private final byte[] bytes;
    private final String path;

    private Payload(int id, int type, byte[] bytes, String path) {
        this.id = id;
        this.type = type;
        this.bytes = bytes;
        this.path = path;
    }

    /// Wraps a block of bytes.
    ///
    /// #### Parameters
    ///
    /// - `bytes`: the payload, no larger than
    ///   [NearbyTransport#getMaxPayloadSize()]
    ///
    /// #### Returns
    ///
    /// the payload
    public static Payload fromBytes(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes are required");
        }
        return new Payload(NEXT_ID.getAndIncrement(), TYPE_BYTES, bytes, null);
    }

    /// Wraps a file, which is streamed rather than loaded.
    ///
    /// #### Parameters
    ///
    /// - `path`: a `com.codename1.io.FileSystemStorage` path
    ///
    /// #### Returns
    ///
    /// the payload
    public static Payload fromFile(String path) {
        if (path == null || path.length() == 0) {
            throw new IllegalArgumentException("a file path is required");
        }
        return new Payload(NEXT_ID.getAndIncrement(), TYPE_FILE, null, path);
    }

    /// Rebuilds a received payload.
    ///
    /// @hidden not part of the public API; called by ports.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id the sending side used
    /// - `type`: [#TYPE_BYTES] or [#TYPE_FILE]
    /// - `bytes`: the bytes for a byte payload, otherwise null
    /// - `path`: the file for a file payload, otherwise null
    ///
    /// #### Returns
    ///
    /// the payload
    public static Payload received(int id, int type, byte[] bytes,
            String path) {
        return new Payload(id, type, bytes, path);
    }

    /// The id progress updates and [NearbyTransport#cancel] use.
    public int getId() {
        return id;
    }

    /// [#TYPE_BYTES] or [#TYPE_FILE].
    public int getType() {
        return type;
    }

    /// The bytes, or `null` for a file payload. The array is not copied --
    /// do not mutate it while the payload is in flight.
    public byte[] getBytes() {
        return bytes;
    }

    /// The file path, or `null` for a byte payload. On a received file
    /// payload this names a file the port already wrote, in the app's
    /// storage.
    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "Payload[" + id + ", "
                + (type == TYPE_FILE ? "file " + path
                        : bytes.length + " bytes") + "]";
    }
}
