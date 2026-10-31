/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.dash.run;

import com.codename1.gaming.GameCamera;
import com.codename1.gaming.Model;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Material;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.RenderState;
import com.codename1.gpu.Texture;
import com.codename1.util.MathUtil;

/// Neon light without a bloom pass: soft white textures drawn additively on
/// flat quads, tinted by their material. A halo is a quad that turns to face
/// the camera; a streak is a stretched one along a tube or a beam; a
/// reflection is one lying on the floor under a light.
///
/// The textures are painted once, in code, as alpha ramps -- white where the
/// light is, transparent where it is not -- so one texture serves every
/// colour.
final class Glow {
    /// A round falloff, bright in the middle.
    final Texture soft;
    /// A long bar of light, soft across and fading at the ends.
    final Texture streak;
    /// A four-pointed sparkle with a hot core.
    final Texture glint;
    /// A dark round blot for a contact shadow, drawn with ordinary blending.
    final Texture shadow;

    Glow(GraphicsDevice d) {
        soft = texture(d, 64, 64, 0);
        streak = texture(d, 64, 32, 1);
        glint = texture(d, 64, 64, 2);
        shadow = texture(d, 64, 64, 3);
    }

    /// An additive, unlit material in `argb` over `tex`; its alpha sets how
    /// strong the light is.
    static Material additive(Texture tex, int argb) {
        RenderState rs = RenderState.transparent().setBlendMode(RenderState.BlendMode.ADDITIVE)
                .setDepthWrite(false).setCullMode(RenderState.CullMode.NONE);
        return new Material(Material.Type.UNLIT).setTexture(tex).setColor(argb).setRenderState(rs);
    }

    /// An alpha blended, unlit material, for shadows and haze.
    static Material blended(Texture tex, int argb) {
        RenderState rs = RenderState.transparent().setDepthWrite(false).setCullMode(RenderState.CullMode.NONE);
        return new Material(Material.Type.UNLIT).setTexture(tex).setColor(argb).setRenderState(rs);
    }

    /// A quad of `w` by `h` with `mat`, at a place.
    static Model sprite(Mesh quad, Material mat, float x, float y, float z, float w, float h) {
        Model m = new Model(quad, mat);
        m.setScale(w, h, 1f);
        m.setPosition(x, y, z);
        return m;
    }

    /// Turns an upright quad about Y so its face points at the camera.
    static void face(Model m, GameCamera cam) {
        float dx = cam.getEyeX() - m.getX();
        float dz = cam.getEyeZ() - m.getZ();
        if (dx * dx + dz * dz < 1e-6f) {
            return;
        }
        float yaw = (float) Math.toDegrees(MathUtil.atan2(dx, dz));
        m.setRotation(m.getRotationX(), yaw, m.getRotationZ());
    }

    private static Texture texture(GraphicsDevice d, int w, int h, int kind) {
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) {
            float fy = (y + 0.5f) / h * 2f - 1f;
            for (int x = 0; x < w; x++) {
                float fx = (x + 0.5f) / w * 2f - 1f;
                float a;
                int rgb = 0xffffff;
                if (kind == 1) {
                    float across = clamp(1f - Math.abs(fy));
                    float along = clamp((1f - Math.abs(fx)) * 4f);
                    a = across * across * along;
                } else if (kind == 2) {
                    float r = (float) Math.sqrt(fx * fx + fy * fy);
                    float core = clamp(1f - r * 2.2f);
                    float rays = clamp(1f - Math.abs(fx) * 14f) * clamp(1f - Math.abs(fy))
                            + clamp(1f - Math.abs(fy) * 14f) * clamp(1f - Math.abs(fx));
                    float halo = clamp(1f - r);
                    a = clamp(core * core + rays * 0.9f + halo * halo * 0.25f);
                } else {
                    float r = (float) Math.sqrt(fx * fx + fy * fy);
                    float k = clamp(1f - r);
                    if (kind == 3) {
                        rgb = 0;
                        a = k * k * (3f - 2f * k);
                    } else {
                        a = k * k * k + k * 0.15f;
                    }
                }
                px[y * w + x] = ((int) (clamp(a) * 255f) << 24) | rgb;
            }
        }
        Texture t = d.createTexture(w, h, px);
        t.setFilter(Texture.Filter.LINEAR);
        return t;
    }

    private static float clamp(float f) {
        return f < 0f ? 0f : f > 1f ? 1f : f;
    }
}
