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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PostPassTest {
    // Records the order of what GraphicsDevice asks of a device in a frame.
    private static class Recorder extends GraphicsDevice {
        final List<String> log = new ArrayList<String>();
        final boolean targets;
        PostProcess chain;
        int created;

        Recorder(boolean targets) {
            this.targets = targets;
        }

        public Texture createRenderTarget(int width, int height) { // NOPMD MissingOverride
            if (!targets) {
                return null;
            }
            created++;
            return new Texture(width, height);
        }

        public boolean beginRenderTarget(Texture target, int clearArgb) { // NOPMD MissingOverride
            log.add("target");
            return true;
        }

        public void endRenderTarget() { // NOPMD MissingOverride
            log.add("/target");
        }

        protected boolean beginPostEffects(PostProcess effects, int width, int height) { // NOPMD MissingOverride
            chain = effects;
            log.add("chain");
            return true;
        }

        protected void endPostEffects(float timeSeconds) { // NOPMD MissingOverride
            log.add("/chain");
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

        public VertexBuffer createVertexBuffer(VertexFormat format, int vertexCount) { // NOPMD MissingOverride
            return new VertexBuffer(format, vertexCount);
        }

        public IndexBuffer createIndexBuffer(int indexCount) { // NOPMD MissingOverride
            return new IndexBuffer(indexCount);
        }

        public void clear(int argbColor, boolean color, boolean depth) { // NOPMD MissingOverride
        }

        public void setViewport(int x, int y, int width, int height) { // NOPMD MissingOverride
        }

        public void draw(Mesh mesh, Material material, float[] modelMatrix) { // NOPMD MissingOverride
            ShaderMaterial s = material.getShader();
            Texture t = s == null ? null : s.getTexture(s.getTextureSlot(PostProcess.SCENE_COLOR));
            log.add("draw " + (t == null ? "scene" : "pass:" + t.getWidth()));
        }

        public void dispose(VertexBuffer buffer) { // NOPMD MissingOverride
        }

        public void dispose(IndexBuffer buffer) { // NOPMD MissingOverride
        }

        public void dispose(Texture texture) { // NOPMD MissingOverride
        }
    }

    private static ShaderMaterial copy() {
        return new ShaderMaterial().set(ShaderMaterial.Hook.BASE_COLOR, ShaderNode.sceneColor(ShaderNode.screenUv()));
    }

    @Test
    void passesRunBetweenTheSceneAndTheBuiltInChain() {
        Recorder d = new Recorder(true);
        Camera cam = new Camera();
        d.setCamera(cam);
        PostProcess pp = new PostProcess().setVignette(0.5f).setAmbientOcclusion(0.8f, 10f)
                .addPass(copy()).addPass(copy());
        assertTrue(d.beginPostProcess(pp, 64, 32));
        d.draw(null, new Material(Material.Type.UNLIT), Matrix4.identity());
        d.endPostProcess(1f);
        assertEquals("[target, draw scene, /target, target, draw pass:64, /target, chain, draw pass:64, /chain]",
                d.log.toString());
        // the built-in chain keeps its own effects but not the depth ones
        assertEquals(0.5f, d.chain.getVignette(), 0f);
        assertFalse(d.chain.isDepthEffects());
        assertSame(cam, d.getCamera());
        // the targets are kept for the next frame of the same size
        d.beginPostProcess(pp, 64, 32);
        d.endPostProcess(1f);
        assertEquals(2, d.created);
    }

    @Test
    void withoutPassesOrTargetsTheChainRunsAsBefore() {
        Recorder d = new Recorder(false);
        PostProcess pp = new PostProcess().setAmbientOcclusion(0.8f, 10f).addPass(copy());
        assertTrue(d.beginPostProcess(pp, 64, 32));
        d.endPostProcess(1f);
        assertEquals("[chain, /chain]", d.log.toString());
        assertSame(pp, d.chain);
    }

    @Test
    void aPassNeedsAColourGraph() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                new PostProcess().addPass(new ShaderMaterial());
            }
        });
        ShaderMaterial p = copy();
        new PostProcess().addPass(p);
        assertTrue(p.getTextureSlot(PostProcess.SCENE_COLOR) >= 0);
    }
}
