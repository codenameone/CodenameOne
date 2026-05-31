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

import com.codename1.impl.CameraImpl;
import com.codename1.util.AsyncResource;

/// Handle for an in-progress video recording started via
/// `CameraSession#startVideoRecording(String)`. Call `#stop()` (fire-and-forget)
/// or `#stopAndAwait()` (returns the final file path) to finish recording.
///
/// **Container format** is platform-specific: iOS produces `.mov`, Android `.mp4`,
/// the JavaScript port `.webm`. Inspect the file extension of the returned path
/// before consuming the result.
public final class VideoRecording {
    private final CameraImpl impl;
    private final String requestedPath;
    private final long startMillis;
    // volatile is intentional: stop() may be called from a background thread
    // while isRecording() is polled from the EDT, and the publish/subscribe
    // semantics are exactly what volatile provides.
    @SuppressWarnings("PMD.AvoidUsingVolatile")
    private volatile boolean stopped;

    /// Used by platform implementations.
    public VideoRecording(CameraImpl impl, String requestedPath) {
        this.impl = impl;
        this.requestedPath = requestedPath;
        this.startMillis = System.currentTimeMillis();
    }

    /// Stop recording without waiting for the file to be finalized.
    /// Use `#stopAndAwait()` if you need the final file path.
    public void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        impl.stopVideoRecording(null);
    }

    /// Stop recording and resolve with the final file path once the file is closed.
    public AsyncResource<String> stopAndAwait() {
        AsyncResource<String> out = new AsyncResource<String>();
        if (stopped) {
            out.complete(requestedPath);
            return out;
        }
        stopped = true;
        impl.stopVideoRecording(out);
        return out;
    }

    public long getElapsedMillis() {
        return System.currentTimeMillis() - startMillis;
    }

    public boolean isRecording() { return !stopped; }

    /// The file path that was requested when the recording started. The actual
    /// final path returned by `#stopAndAwait()` may differ slightly (different
    /// extension) depending on the platform's container format.
    public String getRequestedPath() { return requestedPath; }
}
