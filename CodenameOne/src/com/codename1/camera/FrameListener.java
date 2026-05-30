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

/// Receives camera frames from an active `CameraSession`.
///
/// **Threading**: `#onFrame(CameraFrame)` is invoked on a non-EDT background thread.
/// Do not touch UI directly. The standard pattern is to hand the JPEG bytes to one of
/// the `com.codename1.ai.*` modules (whose `AsyncResource.ready(...)` callbacks already
/// hop back to EDT), or to call `com.codename1.ui.Display#callSerially(Runnable)` yourself
/// before updating the UI.
///
/// **Backpressure**: only one frame is delivered at a time per session. If a previous
/// `onFrame` call is still running when a new frame arrives, the older frame is
/// dropped silently. This is the right default for AI scanning use cases.
///
/// **Memory**: the byte arrays returned by `CameraFrame#getJpegBytes()` and
/// `CameraFrame#getRawBytes()` are owned by the framework and become invalid once
/// `onFrame` returns. Listeners that need to retain bytes past the callback must
/// clone them.
public interface FrameListener {
    void onFrame(CameraFrame frame);
}
