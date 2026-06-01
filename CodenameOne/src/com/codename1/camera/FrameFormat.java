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

/// Pixel format requested for `CameraFrame` data delivered to a `FrameListener`.
///
/// `JPEG` is the default and the right choice for feeding frames into the
/// `com.codename1.ai.*` modules, all of which accept JPEG `byte[]` directly.
/// Raw formats (`NV21`, `RGBA8888`) are useful when an application performs
/// its own pixel processing and wants to avoid the JPEG encode/decode round-trip.
public enum FrameFormat {
    /// JPEG-encoded bytes available via `CameraFrame#getJpegBytes()`.
    /// Always available regardless of the requested format; this is the
    /// universal format for AI/vision module integration.
    JPEG,

    /// YUV 4:2:0 NV21 layout available via `CameraFrame#getRawBytes()`.
    /// Useful for low-level pixel work on Android-style pipelines.
    NV21,

    /// 32-bit RGBA available via `CameraFrame#getRawBytes()`.
    /// Width * height * 4 bytes.
    RGBA8888
}
