/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.gpu;

import com.codename1.gpu.shader.ShaderMaterial;
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
    private boolean shadowMapCurrent;
    private Light light = new Light();
    private Environment environment = new Environment();

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
    /// Setting the camera starts a new frame as far as shadows go: a shadow map
    /// rendered before it is not used again, so set the camera first and then
    /// run `beginShadowPass`.
    ///
    /// #### Parameters
    ///
    /// - `camera`: the camera
    public void setCamera(Camera camera) {
        this.camera = camera;
        shadowMapCurrent = false;
    }

    /// For a backend: true once a shadow pass has ended since the camera was
    /// last set, so the map belongs to this frame. A frame that skipped the pass
    /// must not sample the one before it.
    protected final boolean isShadowMapCurrent() {
        return shadowMapCurrent;
    }

    /// For a backend: records that a shadow pass of this frame has ended.
    protected final void shadowMapRendered() {
        shadowMapCurrent = true;
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

    /// Sets the environment (fog, hemisphere ambient, tone mapping) used by
    /// subsequent draws.
    ///
    /// #### Parameters
    ///
    /// - `environment`: the environment; null restores the default, which
    ///   changes nothing
    public void setEnvironment(Environment environment) {
        this.environment = environment == null ? new Environment() : environment;
    }

    /// Returns the active environment.
    public Environment getEnvironment() {
        return environment;
    }

    private float time;

    /// Sets the clock that custom shaders read with
    /// `com.codename1.gpu.shader.ShaderNode.time()`, in seconds. Call it once
    /// per frame before drawing; animated library shaders such as water move
    /// with it.
    ///
    /// #### Parameters
    ///
    /// - `seconds`: the time
    public void setTime(float seconds) {
        this.time = seconds;
    }

    /// Returns the clock set with `setTime`.
    ///
    /// #### Returns
    ///
    /// seconds
    public float getTime() {
        return time;
    }

    /// Starts rendering the light's shadow map. Until `endShadowPass`, `draw`
    /// writes only depth as seen from the light, and skips materials that do
    /// not cast shadows. Draws after `endShadowPass` that receive shadows
    /// sample the map.
    ///
    /// The default implementation supports no shadows and returns false; the
    /// caller then skips the pass and draws as usual.
    ///
    /// Render the shadow pass before `beginPostProcess`, never inside it: both
    /// passes return to the target that was active when they began, so a
    /// shadow pass started offscreen would hand the post process its own
    /// image to draw into.
    ///
    /// #### Parameters
    ///
    /// - `light`: the light whose shadows to render; it must have shadows on
    ///
    /// #### Returns
    ///
    /// true if the pass started and the shadow casters should be drawn now
    public boolean beginShadowPass(Light light) {
        return false;
    }

    /// Ends the shadow pass started by `beginShadowPass`, and restores the
    /// viewport and target that were active before it.
    public void endShadowPass() {
    }

    /// Creates an offscreen image the scene can be drawn into with
    /// `beginRenderTarget` and then sampled like any other texture: a mirror,
    /// a security camera screen, a minimap, the refraction behind glass.
    ///
    /// The result is an ordinary `Texture`; hand it to a material or to a
    /// `ShaderMaterial` texture slot, and release it with `dispose(Texture)`.
    /// It has a depth buffer of its own while it is drawn into.
    ///
    /// The default implementation cannot draw offscreen and returns null, as
    /// the software simulator device does.
    ///
    /// #### Parameters
    ///
    /// - `width`: the width in pixels
    ///
    /// - `height`: the height in pixels
    ///
    /// #### Returns
    ///
    /// the target, or null when this device has no offscreen drawing
    public Texture createRenderTarget(int width, int height) {
        return null;
    }

    /// True if this device skins meshes on the GPU: it draws a mesh in
    /// `VertexFormat.SKINNED` with a material given `Material.setSkinning`
    /// by moving each vertex in its vertex stage. Where it answers false,
    /// as the software simulator device does, skin on the CPU and draw the
    /// result as an ordinary mesh; `com.codename1.gaming.AnimatedModel`
    /// picks the path by itself.
    ///
    /// #### Returns
    ///
    /// false by default
    public boolean isGpuSkinning() {
        return false;
    }

    /// Sends the draws that follow into `target` until `endRenderTarget`,
    /// clearing its colour to `clearArgb` and its depth first. The viewport
    /// covers the whole target, and the camera, light and environment are the
    /// device's as usual, so set a camera for the view the target shows.
    ///
    /// Draw render targets first in a frame: before the shadow pass, before
    /// `beginPostProcess` and before the scene itself. They do not nest, and
    /// on some devices the depth of scene draws made earlier in the frame is
    /// not kept across one.
    ///
    /// #### Parameters
    ///
    /// - `target`: a texture from `createRenderTarget` on this device
    ///
    /// - `clearArgb`: the colour the target starts from
    ///
    /// #### Returns
    ///
    /// true if drawing now goes into the target
    public boolean beginRenderTarget(Texture target, int clearArgb) {
        return false;
    }

    /// Ends the pass `beginRenderTarget` started; drawing returns to the
    /// target and viewport that were active before it.
    public void endRenderTarget() {
    }

    /// Redirects drawing into an offscreen image so `endPostProcess` can apply
    /// `effects` to it on its way to the screen.
    ///
    /// The default implementation supports no post processing and returns
    /// false; the caller then draws straight to the screen.
    ///
    /// #### Parameters
    ///
    /// - `effects`: the effects to apply
    ///
    /// - `width`: the width of the frame in pixels
    ///
    /// - `height`: the height of the frame in pixels
    ///
    /// #### Returns
    ///
    /// true if drawing now goes offscreen
    public boolean beginPostProcess(PostProcess effects, int width, int height) {
        return false;
    }

    /// Applies the effects passed to `beginPostProcess` and draws the result
    /// to the screen. Drawing goes to the screen again afterwards.
    ///
    /// #### Parameters
    ///
    /// - `timeSeconds`: a running clock, which animates the film grain
    public void endPostProcess(float timeSeconds) {
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

    private final float[] instanceMatrix = new float[16];

    /// Draws `count` copies of a mesh, each with its own transform and,
    /// optionally, colour: crates along a quay, a field of grass, a flock.
    /// The material's shader tells the copies apart with
    /// `ShaderNode.instanceId`, `instanceRandom` and `instanceColor`, so
    /// one material varies size, tint and motion across thousands of
    /// copies.
    ///
    /// Every device draws the copies one after another from a single
    /// compiled pipeline, changing only the transform and the per copy
    /// uniforms between them, which is what makes it cheaper than a model
    /// per copy: no state is looked up or rebound and the scene walks one
    /// node.
    ///
    /// #### Parameters
    ///
    /// - `mesh`: the mesh
    ///
    /// - `material`: the material shared by every copy
    ///
    /// - `matrices`: 16 column-major floats per copy
    ///
    /// - `colors`: an ARGB colour per copy, or null for white
    ///
    /// - `count`: how many copies to draw
    public void drawInstanced(Mesh mesh, Material material, float[] matrices, int[] colors, int count) {
        ShaderMaterial sm = material.getShader();
        int idSlot = sm == null ? -1 : sm.getUniformSlot(ShaderMaterial.INSTANCE);
        int colorSlot = sm == null ? -1 : sm.getUniformSlot(ShaderMaterial.INSTANCE_COLOR);
        float[] u = sm == null ? null : sm.getUniformValues();
        float[] m = instanceMatrix;
        for (int i = 0; i < count; i++) {
            System.arraycopy(matrices, i * 16, m, 0, 16);
            if (idSlot >= 0) {
                u[idSlot * 4] = i;
                // a cheap integer hash, the same copy always gets the same number
                int h = i * 0x27d4eb2d;
                h ^= h >>> 15;
                h *= 0x165667b1;
                h ^= h >>> 13;
                u[idSlot * 4 + 1] = (h & 0xffff) / 65535f;
            }
            if (colorSlot >= 0) {
                int c = colors == null ? 0xffffffff : colors[i];
                u[colorSlot * 4] = ((c >> 16) & 0xff) / 255f;
                u[colorSlot * 4 + 1] = ((c >> 8) & 0xff) / 255f;
                u[colorSlot * 4 + 2] = (c & 0xff) / 255f;
                u[colorSlot * 4 + 3] = ((c >>> 24) & 0xff) / 255f;
            }
            draw(mesh, material, m);
        }
        if (idSlot >= 0) {
            u[idSlot * 4] = 0f;
            u[idSlot * 4 + 1] = 0f;
        }
    }

    /// Releases the GPU resources backing a vertex buffer.
    public abstract void dispose(VertexBuffer buffer);

    /// Releases the GPU resources backing an index buffer.
    public abstract void dispose(IndexBuffer buffer);

    /// Releases the GPU resources backing a texture.
    public abstract void dispose(Texture texture);

    /// Writes the parameters of the post effects added after grain into
    /// `out` from `offset`: chromatic aberration, FXAA (0 or 1), god ray
    /// strength scaled by how visible the sun is, and the sun's position in
    /// texture space (0, 0 is the top left). Every device packs them in this
    /// order, so the composite passes of every shading language agree.
    ///
    /// #### Parameters
    ///
    /// - `effects`: the post process being composited
    ///
    /// - `out`: the parameter array
    ///
    /// - `offset`: where to write the six floats
    protected final void packPostEffects(PostProcess effects, float[] out, int offset) {
        out[offset] = effects.getChromaticAberration();
        out[offset + 1] = effects.isFxaa() ? 1f : 0f;
        out[offset + 2] = 0f;
        out[offset + 3] = 0.5f;
        out[offset + 4] = 0.5f;
        out[offset + 5] = 0f;
        Camera cam = getCamera();
        Light light = getLight();
        if (effects.getGodRays() <= 0f || cam == null || light == null) {
            return;
        }
        // the sun is far along the reverse of the light's direction
        float sx = cam.getEyeX() - light.getDirectionX() * 1000f;
        float sy = cam.getEyeY() - light.getDirectionY() * 1000f;
        float sz = cam.getEyeZ() - light.getDirectionZ() * 1000f;
        float[] m = cam.getViewProjection();
        float cx = m[0] * sx + m[4] * sy + m[8] * sz + m[12];
        float cy = m[1] * sx + m[5] * sy + m[9] * sz + m[13];
        float cw = m[3] * sx + m[7] * sy + m[11] * sz + m[15];
        if (cw <= 0.0001f) {
            return;
        }
        float u = cx / cw * 0.5f + 0.5f;
        float v = 0.5f - cy / cw * 0.5f;
        // full strength on screen, fading to nothing half a screen outside it
        float du = Math.max(0f, Math.abs(u - 0.5f) - 0.5f);
        float dv = Math.max(0f, Math.abs(v - 0.5f) - 0.5f);
        float fade = Math.max(0f, 1f - (du + dv) * 2f);
        out[offset + 2] = effects.getGodRays() * fade;
        out[offset + 3] = u;
        out[offset + 4] = v;
    }

    /// Writes the parameters of the depth effects and the colour lookup into
    /// `out` from `offset`, twelve floats: ambient occlusion strength and
    /// radius in pixels, focus distance, depth of field blur in pixels,
    /// outline strength, the camera's near and far planes, lookup strength,
    /// and the outline colour as red, green, blue (0 to 1) and a zero. Every
    /// device packs them in this order, so the composite passes of every
    /// shading language agree.
    ///
    /// #### Parameters
    ///
    /// - `effects`: the post process being composited
    ///
    /// - `out`: the parameter array
    ///
    /// - `offset`: where to write the twelve floats
    protected final void packDepthEffects(PostProcess effects, float[] out, int offset) {
        Camera cam = getCamera();
        out[offset] = effects.getAmbientOcclusion();
        out[offset + 1] = effects.getAmbientOcclusionRadius();
        out[offset + 2] = effects.getFocusDistance();
        out[offset + 3] = effects.getDepthOfFieldBlur();
        out[offset + 4] = effects.getOutline();
        out[offset + 5] = cam == null ? 0.1f : cam.getNear();
        out[offset + 6] = cam == null ? 100f : cam.getFar();
        out[offset + 7] = effects.getColorLutStrength();
        int c = effects.getOutlineColor();
        out[offset + 8] = ((c >> 16) & 0xff) / 255f;
        out[offset + 9] = ((c >> 8) & 0xff) / 255f;
        out[offset + 10] = (c & 0xff) / 255f;
        out[offset + 11] = 0f;
    }
}
