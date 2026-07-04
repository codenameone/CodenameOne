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
package com.codename1.vr;

import com.codename1.gpu.Camera;
import com.codename1.gpu.GraphicsDevice;

/// Application supplied callback that draws the scene for a `VRView`. Like
/// `com.codename1.gpu.Renderer`, every method runs on the platform render
/// thread that owns the GPU context - never touch Codename One UI components
/// from these callbacks.
///
/// The view clears the frame, sets the per-eye viewport and camera, then
/// invokes `#onEyeFrame(GraphicsDevice, VREye, Camera)` once per eye (twice
/// per frame in stereo, once with `VREye#CENTER` in mono). Draw the same
/// scene for every eye; the camera differences produce the stereo effect.
public interface VRRenderer {
    /// Invoked once after the GPU context is created. Allocate buffers,
    /// textures and materials here.
    ///
    /// #### Parameters
    ///
    /// - `device`: the graphics device bound to this view
    void onInit(GraphicsDevice device);

    /// Invoked once per eye per frame to render the scene. The viewport and
    /// camera are already configured for this eye.
    ///
    /// #### Parameters
    ///
    /// - `device`: the graphics device bound to this view
    ///
    /// - `eye`: which eye is being rendered
    ///
    /// - `eyeCamera`: the camera for this eye, already applied to the device
    void onEyeFrame(GraphicsDevice device, VREye eye, Camera eyeCamera);

    /// Invoked when the GPU context is being torn down. Release resources not
    /// owned by the device. May be invoked with a null device when the context
    /// was lost.
    ///
    /// #### Parameters
    ///
    /// - `device`: the graphics device, or null if the context was lost
    void onDispose(GraphicsDevice device);
}
