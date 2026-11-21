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
package com.codename1.wordprocessing;

/// An immutable picture block: the original encoded image bytes, kept as they were read so a
/// document written back carries the same image, plus its displayed size in points.
public final class Picture implements Block, Inline {
    /// The block id.
    public final String id;

    /// The MIME type of `data()`, such as `image/png`.
    public final String mimeType;

    /// The displayed width in points.
    public final float width;

    /// The displayed height in points.
    public final float height;

    /// Alternative text for readers that cannot show the picture.
    public final String description;

    private final byte[] data;

    /// Creates a picture.
    ///
    /// #### Parameters
    ///
    /// - `id`: the block id
    ///
    /// - `data`: the encoded image; the array is copied
    ///
    /// - `mimeType`: its MIME type
    ///
    /// - `width`: the displayed width in points, greater than zero
    ///
    /// - `height`: the displayed height in points, greater than zero
    ///
    /// - `description`: alternative text, or null
    public Picture(String id, byte[] data, String mimeType, float width, float height,
            String description) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("Picture id is required");
        }
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Picture data is required");
        }
        if (Float.isNaN(width) || Float.isNaN(height) || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid picture size");
        }
        this.id = id;
        this.data = copy(data);
        this.mimeType = mimeType == null || mimeType.length() == 0 ? sniff(data) : mimeType;
        this.width = width;
        this.height = height;
        this.description = description == null ? "" : description;
    }

    private static byte[] copy(byte[] b) {
        byte[] out = new byte[b.length];
        System.arraycopy(b, 0, out, 0, b.length);
        return out;
    }

    /// Guesses a MIME type from an image's leading bytes.
    ///
    /// #### Parameters
    ///
    /// - `b`: the encoded image
    ///
    /// #### Returns
    ///
    /// the MIME type, `application/octet-stream` when unknown
    static String sniff(byte[] b) {
        if (b.length > 3 && (b[0] & 0xff) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length > 2 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8) {
            return "image/jpeg";
        }
        if (b.length > 3 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F') {
            return "image/gif";
        }
        return "application/octet-stream";
    }

    /// The block id.
    ///
    /// #### Returns
    ///
    /// the id
    @Override
    public String id() {
        return id;
    }

    /// The encoded image.
    ///
    /// #### Returns
    ///
    /// a copy of the bytes
    public byte[] data() {
        return copy(data);
    }

    /// The size of the encoded image.
    ///
    /// #### Returns
    ///
    /// the byte count
    public int dataLength() {
        return data.length;
    }

    /// A copy shown at another size.
    ///
    /// #### Parameters
    ///
    /// - `w`: the width in points
    ///
    /// - `h`: the height in points
    ///
    /// #### Returns
    ///
    /// the copy
    public Picture withSize(float w, float h) {
        return new Picture(id, data, mimeType, w, h, description);
    }

    /// Two pictures are equal when their id, bytes, type, size and description are.
    ///
    /// #### Parameters
    ///
    /// - `o`: the other object
    ///
    /// #### Returns
    ///
    /// true when equal
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Picture)) {
            return false;
        }
        Picture p = (Picture) o;
        if (!id.equals(p.id) || !mimeType.equals(p.mimeType) || !description.equals(p.description)
                || Float.floatToIntBits(width) != Float.floatToIntBits(p.width)
                || Float.floatToIntBits(height) != Float.floatToIntBits(p.height)
                || data.length != p.data.length) {
            return false;
        }
        for (int i = 0; i < data.length; i++) {
            if (data[i] != p.data[i]) {
                return false;
            }
        }
        return true;
    }

    /// A hash consistent with `equals`.
    ///
    /// #### Returns
    ///
    /// the hash
    @Override
    public int hashCode() {
        return id.hashCode() * 31 + data.length;
    }

    /// A picture in a line reads as nothing in plain text.
    ///
    /// #### Returns
    ///
    /// the empty string
    @Override
    public String displayText() {
        return "";
    }
}
