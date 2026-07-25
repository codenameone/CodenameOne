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
package com.codename1.ai.vision;

/// Portable barcode observation.
public final class Barcode {
    private final String value;
    private final String format;
    private final byte[] rawBytes;
    private final VisionRect bounds;
    private final VisionPoint[] corners;
    private final VisionMetadata metadata;

    public Barcode(String value, String format, byte[] rawBytes,
                   VisionRect bounds, VisionPoint[] corners) {
        this(value, format, rawBytes, bounds, corners, null);
    }

    public Barcode(String value, String format, byte[] rawBytes,
                   VisionRect bounds, VisionPoint[] corners,
                   VisionMetadata metadata) {
        this.value = value;
        this.format = format;
        this.rawBytes = copy(rawBytes);
        this.bounds = bounds == null ? VisionRect.EMPTY : bounds;
        if (corners == null) {
            this.corners = new VisionPoint[0];
        } else {
            this.corners = new VisionPoint[corners.length];
            System.arraycopy(corners, 0, this.corners, 0, corners.length);
        }
        this.metadata = metadata;
    }

    public String getValue() {
        return value;
    }

    public String getFormat() {
        return format;
    }

    public byte[] getRawBytes() {
        return copy(rawBytes);
    }

    public VisionRect getBounds() {
        return bounds;
    }

    public VisionPoint[] getCorners() {
        VisionPoint[] out = new VisionPoint[corners.length];
        System.arraycopy(corners, 0, out, 0, corners.length);
        return out;
    }

    public VisionMetadata getMetadata() {
        return metadata;
    }

    private static byte[] copy(byte[] value) {
        if (value == null) {
            return null;
        }
        byte[] out = new byte[value.length];
        System.arraycopy(value, 0, out, 0, value.length);
        return out;
    }
}
