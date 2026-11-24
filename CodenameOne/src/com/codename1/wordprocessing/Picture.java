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

    /// The MIME type of `data()`, such as `image/png`; empty for a linked picture.
    public final String mimeType;

    /// The URL a linked picture names, as Markdown's `![alt](url)` or HTML's `<img src>`
    /// gives it; null for a picture that holds its own bytes. A linked picture has no bytes:
    /// HTML, Markdown and `.cn1doc` write the URL back, while DOCX, ODT, RTF, a page view and
    /// PDF export, which can only show an image they hold, leave its box empty or skip it.
    public final String source;

    /// The displayed width in points.
    public final float width;

    /// The displayed height in points.
    public final float height;

    /// Alternative text for readers that cannot show the picture.
    public final String description;

    /// Laid out in the line like a large character; the default.
    public static final String INLINE = "inline";

    /// Floats at a side of the column; text wraps around its box.
    public static final String SQUARE = "square";

    /// Floats on lines of its own; no text beside it.
    public static final String TOP_BOTTOM = "topAndBottom";

    /// Floats behind the text, which ignores it.
    public static final String BEHIND = "behind";

    /// Floats over the text, which ignores it.
    public static final String IN_FRONT = "inFront";

    /// Horizontal placement of a floating picture: at the left edge of the column.
    public static final String LEFT = "left";

    /// Horizontal placement of a floating picture: centered in the column.
    public static final String CENTER = "center";

    /// Horizontal placement of a floating picture: at the right edge of the column.
    public static final String RIGHT = "right";

    /// How text flows around the picture: `INLINE`, `SQUARE`, `TOP_BOTTOM`, `BEHIND` or
    /// `IN_FRONT`.
    public final String wrap;

    /// Where a floating picture sits across the column: `LEFT`, `CENTER` or `RIGHT`.
    public final String align;

    /// How far below the top of its anchor paragraph a floating picture starts, in points.
    public final float offsetY;

    /// The gap kept between a floating picture and the text wrapped around it, in points.
    public final float distance;

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
        this(id, data, null, mimeType, width, height, description, INLINE, LEFT, 0, 9);
    }

    /// A picture that names its image by URL instead of holding it.
    ///
    /// #### Parameters
    ///
    /// - `id`: the block id
    ///
    /// - `source`: the image URL, not empty
    ///
    /// - `width`: the displayed width in points, greater than zero
    ///
    /// - `height`: the displayed height in points, greater than zero
    ///
    /// - `description`: alternative text, or null
    ///
    /// #### Returns
    ///
    /// the picture
    public static Picture linked(String id, String source, float width, float height,
            String description) {
        if (source == null || source.length() == 0) {
            throw new IllegalArgumentException("Picture source is required");
        }
        return new Picture(id, new byte[0], source, "", width, height, description, INLINE, LEFT,
                0, 9);
    }

    @SuppressWarnings("PMD.ExcessiveParameterList")
    private Picture(String id, byte[] data, String source, String mimeType, float width,
            float height, String description, String wrap, String align, float offsetY,
            float distance) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("Picture id is required");
        }
        if (source == null && (data == null || data.length == 0)) {
            throw new IllegalArgumentException("Picture data is required");
        }
        if (Float.isNaN(width) || Float.isNaN(height) || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid picture size");
        }
        this.id = id;
        this.data = copy(data);
        this.source = source;
        // the type is read from files and written into markup (a data: URL, a content type):
        // anything but a plain token is replaced by what the bytes say
        this.mimeType = source != null ? "" : isToken(mimeType) ? mimeType : sniff(data);
        this.width = width;
        this.height = height;
        this.description = description == null ? "" : description;
        this.wrap = SQUARE.equals(wrap) || TOP_BOTTOM.equals(wrap) || BEHIND.equals(wrap)
                || IN_FRONT.equals(wrap) ? wrap : INLINE;
        this.align = CENTER.equals(align) || RIGHT.equals(align) ? align : LEFT;
        // hostile values from a file must not push the picture off every page
        this.offsetY = Float.isNaN(offsetY) ? 0 : Math.max(-2000, Math.min(2000, offsetY));
        this.distance = Float.isNaN(distance) ? 0 : Math.max(0, Math.min(72, distance));
    }

    /// Whether the picture names its image by URL and holds no bytes.
    ///
    /// #### Returns
    ///
    /// true when `source` is set
    public boolean isLinked() {
        return source != null;
    }

    /// Whether the picture floats instead of sitting in a line.
    ///
    /// #### Returns
    ///
    /// true unless the wrap is `INLINE`
    public boolean isFloating() {
        return !INLINE.equals(wrap);
    }

    /// A copy placed differently.
    ///
    /// #### Parameters
    ///
    /// - `wrapMode`: `INLINE`, `SQUARE`, `TOP_BOTTOM`, `BEHIND` or `IN_FRONT`
    ///
    /// - `alignment`: `LEFT`, `CENTER` or `RIGHT`
    ///
    /// - `offset`: points below the top of the anchor paragraph
    ///
    /// - `gap`: points kept between the picture and wrapped text, 0 to 72
    ///
    /// #### Returns
    ///
    /// the copy
    public Picture withWrap(String wrapMode, String alignment, float offset, float gap) {
        return new Picture(id, data, source, mimeType, width, height, description, wrapMode, alignment,
                offset, gap);
    }

    private static boolean isToken(String s) {
        if (s == null || s.length() == 0 || s.length() > 100) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '/' || c == '.' || c == '+' || c == '-')) {
                return false;
            }
        }
        return true;
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
    /// a copy of the bytes; empty for a linked picture
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
        return new Picture(id, data, source, mimeType, w, h, description, wrap, align, offsetY,
                distance);
    }

    /// Two pictures are equal when their id, bytes, source, type, size, description and placement
    /// are.
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
                || data.length != p.data.length || !(source == null ? p.source == null : source.equals(p.source))
                || !wrap.equals(p.wrap) || !align.equals(p.align)
                || Float.floatToIntBits(offsetY) != Float.floatToIntBits(p.offsetY)
                || Float.floatToIntBits(distance) != Float.floatToIntBits(p.distance)) {
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
