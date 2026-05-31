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
package com.codename1.camera;

import com.codename1.ui.Image;

import java.io.IOException;

/// Result of a still-photo capture via `CameraSession#takePhoto()`.
///
/// Both an in-memory JPEG byte array and a file path are returned; the framework
/// always saves to the app-private storage so that the bytes survive the callback
/// scope.
public final class CapturedPhoto {
    private final byte[] jpegBytes;
    private final String filePath;
    private final int width;
    private final int height;

    /// Used by platform implementations.
    public CapturedPhoto(byte[] jpegBytes, String filePath, int width, int height) {
        this.jpegBytes = jpegBytes;
        this.filePath = filePath;
        this.width = width;
        this.height = height;
    }

    /// JPEG-encoded bytes of the captured photo. Hand directly to the
    /// `com.codename1.ai.*` modules without re-reading from disk.
    public byte[] getJpegBytes() {
        return jpegBytes;
    }

    /// FileSystemStorage path the JPEG was saved to. The file is in the
    /// application's private storage and persists until the app deletes it.
    public String getFilePath() {
        return filePath;
    }

    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }

    /// Convenience: decode the JPEG bytes into a Codename One `Image`.
    public Image toImage() throws IOException {
        return Image.createImage(jpegBytes, 0, jpegBytes.length);
    }
}
