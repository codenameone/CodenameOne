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

import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Texture;

/// Supplies and updates the texture rendered by a `Media360View`, enabling
/// dynamic content such as procedural animations - and, once a platform path
/// from media frames to GPU textures exists, 360 video. All methods run on
/// the render thread.
public interface TextureSource {
    /// Creates the texture. Invoked once on the render thread after the GPU
    /// context is ready.
    ///
    /// #### Parameters
    ///
    /// - `device`: the graphics device bound to the view
    ///
    /// #### Returns
    ///
    /// the texture to map onto the sphere
    Texture createTexture(GraphicsDevice device);

    /// Invoked once per frame before drawing. Update the texture contents
    /// here and return true when they changed (so continuous sources keep the
    /// view animating).
    ///
    /// #### Parameters
    ///
    /// - `device`: the graphics device bound to the view
    ///
    /// - `texture`: the texture returned by `#createTexture(GraphicsDevice)`
    ///
    /// #### Returns
    ///
    /// true when the texture contents changed this frame
    boolean updateTexture(GraphicsDevice device, Texture texture);

    /// Invoked when the view tears down. Release anything the source owns;
    /// the texture itself is disposed by the view.
    ///
    /// #### Parameters
    ///
    /// - `device`: the graphics device bound to the view
    void dispose(GraphicsDevice device);
}
