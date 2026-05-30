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

/// Per-capture options for `CameraSession#takePhoto(PhotoCaptureOptions)`.
/// A builder; chain calls fluently.
public final class PhotoCaptureOptions {
    private int width;
    private int height;
    private int jpegQuality = 90;
    private String filePath;

    /// Requested photo dimensions. `0` (the default for either) means use the
    /// camera's default photo size.
    public PhotoCaptureOptions size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /// JPEG encoding quality 1-100. Default 90.
    public PhotoCaptureOptions jpegQuality(int q) {
        if (q < 1) q = 1;
        if (q > 100) q = 100;
        this.jpegQuality = q;
        return this;
    }

    /// Override the destination file path (FileSystemStorage path).
    /// When unset, the framework saves to a temp file under the app home.
    public PhotoCaptureOptions filePath(String path) {
        this.filePath = path;
        return this;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getJpegQuality() { return jpegQuality; }
    public String getFilePath() { return filePath; }
}
