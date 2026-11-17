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
import com.codename1.gpu.shader.ShaderNode;
import com.codename1.ui.Image;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstancingTest {

    /// Records what each draw saw: its translation and the per copy uniforms.
    private static final class Recorder extends GraphicsDevice {
        final List<float[]> seen = new ArrayList<float[]>();

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
            ShaderMaterial sm = material.getShader();
            float[] u = sm.getUniformValues();
            int id = sm.getUniformSlot(ShaderMaterial.INSTANCE);
            int c = sm.getUniformSlot(ShaderMaterial.INSTANCE_COLOR);
            seen.add(new float[] {modelMatrix[12], u[id * 4], u[id * 4 + 1], u[c * 4], u[c * 4 + 3]});
        }

        public void dispose(VertexBuffer buffer) { // NOPMD MissingOverride
        }

        public void dispose(IndexBuffer buffer) { // NOPMD MissingOverride
        }

        public void dispose(Texture texture) { // NOPMD MissingOverride
        }
    }

    @Test
    void everyCopyGetsItsTransformIndexRandomAndColour() {
        ShaderMaterial sm = new ShaderMaterial();
        sm.set(ShaderMaterial.Hook.BASE_COLOR, ShaderNode.instanceColor()
                .mul(ShaderNode.instanceRandom().add(ShaderNode.instanceId())));
        Material m = new Material(Material.Type.UNLIT).setShader(sm);
        float[] mats = new float[3 * 16];
        for (int i = 0; i < 3; i++) {
            System.arraycopy(Matrix4.translation(i * 2f, 0f, 0f), 0, mats, i * 16, 16);
        }
        Recorder r = new Recorder();
        r.drawInstanced(null, m, mats, new int[] {0xffff0000, 0x80000000, 0xff0000ff}, 3);
        assertEquals(3, r.seen.size());
        for (int i = 0; i < 3; i++) {
            float[] s = r.seen.get(i);
            assertEquals(i * 2f, s[0], 0f);
            assertEquals(i, s[1], 0f);
            assertTrue(s[2] >= 0f && s[2] <= 1f);
        }
        assertEquals(1f, r.seen.get(0)[3], 0f);
        assertEquals(128f / 255f, r.seen.get(1)[4], 1e-6f);
        assertNotEquals(r.seen.get(0)[2], r.seen.get(1)[2]);
        // a plain draw afterwards is copy 0 again
        assertEquals(0f, sm.getUniformValues()[sm.getUniformSlot(ShaderMaterial.INSTANCE) * 4], 0f);
    }
}
