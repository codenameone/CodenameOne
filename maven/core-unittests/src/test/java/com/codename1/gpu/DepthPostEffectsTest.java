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

import com.codename1.impl.gpu.GlslShaderGenerator;
import com.codename1.ui.Image;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DepthPostEffectsTest {
    private static final class Packer extends GraphicsDevice {
        float[] pack(PostProcess pp) {
            float[] out = new float[14];
            packDepthEffects(pp, out, 2);
            return out;
        }

        public GpuCapabilities getCapabilities() { // NOPMD MissingOverride
            return null;
        }

        public Texture createTexture(Image image) { // NOPMD MissingOverride
            return null;
        }

        public Texture createTexture(int width, int height, int[] argb) { // NOPMD MissingOverride
            return null;
        }

        public void clear(int argbColor, boolean color, boolean depth) { // NOPMD MissingOverride
        }

        public void setViewport(int x, int y, int width, int height) { // NOPMD MissingOverride
        }

        public void draw(Mesh mesh, Material material, float[] modelMatrix) { // NOPMD MissingOverride
        }

        public void dispose(VertexBuffer buffer) { // NOPMD MissingOverride
        }

        public void dispose(IndexBuffer buffer) { // NOPMD MissingOverride
        }

        public void dispose(Texture texture) { // NOPMD MissingOverride
        }
    }

    @Test
    void depthEffectsPackInTheOrderEveryCompositeReads() {
        Packer d = new Packer();
        d.setCamera(new Camera().setPerspective(50f, 0.5f, 80f));
        PostProcess pp = new PostProcess().setAmbientOcclusion(0.8f, 10f).setDepthOfField(6f, 4f)
                .setOutline(0.5f, 0xff336699).setColorLut(new Texture(256, 16), 0.7f);
        float[] p = d.pack(pp);
        assertEquals(0f, p[0], 0f);
        assertArrayEquals(new float[] {0.8f, 10f, 6f, 4f, 0.5f, 0.5f, 80f, 0.7f}, slice(p, 2, 8), 1e-6f);
        assertEquals(0x33 / 255f, p[10], 1e-6f);
        assertEquals(0x66 / 255f, p[11], 1e-6f);
        assertEquals(0x99 / 255f, p[12], 1e-6f);
        assertEquals(0f, p[13], 0f);
        assertTrue(pp.isDepthEffects());
        // a strength without a table is no lookup
        assertEquals(0f, new PostProcess().setColorLut(null, 1f).getColorLutStrength(), 0f);
        assertFalse(new PostProcess().isDepthEffects());
    }

    @Test
    void settersClampToTheRangesTheShadersAssume() {
        PostProcess pp = new PostProcess().setAmbientOcclusion(3f, 500f).setDepthOfField(-1f, 99f).setOutline(-2f, 0);
        assertEquals(1f, pp.getAmbientOcclusion(), 0f);
        assertEquals(64f, pp.getAmbientOcclusionRadius(), 0f);
        assertTrue(pp.getFocusDistance() > 0f);
        assertEquals(32f, pp.getDepthOfFieldBlur(), 0f);
        assertEquals(0f, pp.getOutline(), 0f);
    }

    @Test
    void theIdentityLutMapsEveryCellToItsOwnColour() {
        int[] lut = ColorLuts.identity();
        assertEquals(ColorLuts.WIDTH * ColorLuts.HEIGHT, lut.length);
        for (int g = 0; g < 16; g++) {
            for (int b = 0; b < 16; b++) {
                for (int r = 0; r < 16; r++) {
                    int px = lut[g * ColorLuts.WIDTH + b * 16 + r];
                    assertEquals(0xff, px >>> 24);
                    assertEquals(r * 17, (px >> 16) & 0xff);
                    assertEquals(g * 17, (px >> 8) & 0xff);
                    assertEquals(b * 17, px & 0xff);
                }
            }
        }
        int[] poster = ColorLuts.posterize(2);
        assertEquals(0xffffffff, poster[15 * ColorLuts.WIDTH + 15 * 16 + 15]);
        assertEquals(0xff000000, poster[0]);
        int[] film = ColorLuts.filmic(1f, 0.2f);
        assertTrue((film[0] & 0xff) > 0, "black is lifted");
    }

    @Test
    void everyCompositeReadsDepthAndTheLookup() {
        String glsl = GlslShaderGenerator.compositeFragment();
        assertTrue(glsl.contains("uniform sampler2D u_depth;"), glsl);
        assertTrue(glsl.contains("uniform sampler2D u_lut;"), glsl);
        assertTrue(glsl.contains("cn1Depth(uv0)"), glsl);
        assertTrue(glsl.contains("cn1Lut(t0)"), glsl);
        String msl = GlslShaderGenerator.depthEffects("msl", "u.dfx", "u.dfx2", "u.ocol.xyz");
        assertTrue(msl.contains("cn1Depth(depth, uv0)"), msl);
        assertFalse(msl.contains("vec2"), msl);
        String hlsl = GlslShaderGenerator.depthEffects("hlsl", "g8", "g9", "g10.xyz");
        assertFalse(hlsl.contains("mix("), hlsl);
        assertTrue(GlslShaderGenerator.lutEffect("hlsl", "g9.w").contains("lerp("));
    }

    private static float[] slice(float[] a, int from, int n) {
        float[] out = new float[n];
        System.arraycopy(a, from, out, 0, n);
        return out;
    }
}
