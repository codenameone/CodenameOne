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
package com.codename1.flutter;

import dart.typed_data.Uint8List;

/**
 * An {@link ImageProvider} that decodes an image from an in-memory byte buffer
 * — Flutter's {@code MemoryImage}. new_gallery uses it for asset thumbnails it
 * has already loaded into a {@code Uint8List}.
 */
public class MemoryImage extends ImageProvider {

    private final Uint8List bytes;
    private double scale = 1.0;

    public MemoryImage(Uint8List bytes) {
        this.bytes = bytes;
    }

    /** Named parameter setter for the Dart {@code scale:} parameter. */
    public void scale(double v) {
        this.scale = v;
    }

    public Uint8List getBytes() {
        return bytes;
    }

    public double getScale() {
        return scale;
    }

    @Override
    public String sourceKey() {
        return "memory:" + System.identityHashCode(bytes) + "@" + scale;
    }
}
