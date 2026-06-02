/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.gpu;

import com.codename1.ui.Image;

/// The low level command surface of the 3D API, bound to a single `RenderView`
/// and its GPU context. A concrete subclass is provided by each platform
/// backend (OpenGL ES on Android, WebGL on the browser, Metal on iOS, desktop GL
/// on the simulator). Applications obtain the device from the `Renderer`
/// callbacks and never construct it directly.
///
/// The device owns shader generation and caching: when `draw` is called it looks
/// at the `Material` and the mesh `VertexFormat`, generates (once) the matching
/// platform shader, uploads any dirty buffers and issues the draw call.
public abstract class GraphicsDevice {
    private Camera camera;
    private Light light = new Light();

    /// Returns the capabilities and limits of the underlying GPU.
    public abstract GpuCapabilities getCapabilities();

    /// Allocates a vertex buffer. The backing array is SIMD aligned so it can be
    /// uploaded to the GPU without an intermediate copy on ParparVM.
    ///
    /// #### Parameters
    ///
    /// - `format`: the interleaved vertex layout
    ///
    /// - `vertexCount`: the number of vertices
    ///
    /// #### Returns
    ///
    /// a new vertex buffer tracked by this device
    public VertexBuffer createVertexBuffer(VertexFormat format, int vertexCount) {
        return new VertexBuffer(format, vertexCount);
    }

    /// Allocates an index buffer.
    ///
    /// #### Parameters
    ///
    /// - `indexCount`: the number of indices
    ///
    /// #### Returns
    ///
    /// a new index buffer tracked by this device
    public IndexBuffer createIndexBuffer(int indexCount) {
        return new IndexBuffer(indexCount);
    }

    /// Creates a GPU texture from a Codename One image.
    ///
    /// #### Parameters
    ///
    /// - `image`: the source image
    ///
    /// #### Returns
    ///
    /// a new texture
    public abstract Texture createTexture(Image image);

    /// Creates a GPU texture from raw ARGB pixel data.
    ///
    /// #### Parameters
    ///
    /// - `width`: the texture width in pixels
    ///
    /// - `height`: the texture height in pixels
    ///
    /// - `argb`: `width * height` packed ARGB pixels in row major order
    ///
    /// #### Returns
    ///
    /// a new texture
    public abstract Texture createTexture(int width, int height, int[] argb);

    /// Clears the framebuffer.
    ///
    /// #### Parameters
    ///
    /// - `argbColor`: the packed ARGB clear color
    ///
    /// - `color`: true to clear the color buffer
    ///
    /// - `depth`: true to clear the depth buffer
    public abstract void clear(int argbColor, boolean color, boolean depth);

    /// Sets the viewport rectangle in pixels.
    public abstract void setViewport(int x, int y, int width, int height);

    /// Sets the active camera supplying the view and projection matrices used by
    /// subsequent draws.
    ///
    /// #### Parameters
    ///
    /// - `camera`: the camera
    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    /// Returns the active camera, or null if none was set.
    public Camera getCamera() {
        return camera;
    }

    /// Sets the active directional light used by lit materials.
    ///
    /// #### Parameters
    ///
    /// - `light`: the light
    public void setLight(Light light) {
        this.light = light;
    }

    /// Returns the active light.
    public Light getLight() {
        return light;
    }

    /// Draws a mesh with the supplied material and model matrix. The device
    /// composes `camera.getViewProjection() * modelMatrix`, binds the generated
    /// shader for the material, applies the material render state and issues the
    /// draw call.
    ///
    /// #### Parameters
    ///
    /// - `mesh`: the geometry to draw
    ///
    /// - `material`: how to shade the geometry
    ///
    /// - `modelMatrix`: the 16 element column-major model transform, or null for
    ///   the identity
    public abstract void draw(Mesh mesh, Material material, float[] modelMatrix);

    /// Releases the GPU resources backing a vertex buffer.
    public abstract void dispose(VertexBuffer buffer);

    /// Releases the GPU resources backing an index buffer.
    public abstract void dispose(IndexBuffer buffer);

    /// Releases the GPU resources backing a texture.
    public abstract void dispose(Texture texture);
}
