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
package com.codename1.rive;

import com.codename1.gpu.Camera;
import com.codename1.gpu.GpuCapabilities;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.IndexBuffer;
import com.codename1.gpu.Material;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.RenderState;
import com.codename1.gpu.Texture;
import com.codename1.gpu.VertexBuffer;
import com.codename1.impl.rive.GpuRiveRenderer;
import com.codename1.impl.rive.Mat2D;
import com.codename1.impl.rive.RivePaint;
import com.codename1.impl.rive.RivePath;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The GPU half of the Rive renderer, against a recording device.
class RiveGpuTest extends UITestBase {
    private static final float[] V = {0, 0, 10, 0, 0, 10};
    private static final float[] UV = {0, 0, 1, 0, 0, 1};
    private static final int[] IDX = {0, 1, 2};

    private static final class Device extends GraphicsDevice {
        final List<Texture> live = new ArrayList<Texture>();
        final List<Mesh> meshes = new ArrayList<Mesh>();
        final List<RenderState.BlendMode> blends = new ArrayList<RenderState.BlendMode>();
        int created;
        int clears;
        int lastTextureWidth;
        int lastTextureHeight;
        float[] viewProjection;

        public GpuCapabilities getCapabilities() {
            return null;
        }

        public Texture createTexture(Image image) {
            return add(new Texture(image.getWidth(), image.getHeight()));
        }

        public Texture createTexture(int width, int height, int[] argb) {
            assertEquals(width * height, argb.length);
            lastTextureWidth = width;
            lastTextureHeight = height;
            return add(new Texture(width, height));
        }

        private Texture add(Texture t) {
            created++;
            live.add(t);
            return t;
        }

        public void clear(int argbColor, boolean color, boolean depth) {
            clears++;
        }

        public void setViewport(int x, int y, int width, int height) {
        }

        @Override
        public void setCamera(Camera camera) {
            super.setCamera(camera);
            viewProjection = camera.getViewProjection();
        }

        public void draw(Mesh mesh, Material material, float[] modelMatrix) {
            assertTrue(live.contains(material.getTexture()), "drawn with a disposed texture");
            meshes.add(mesh);
            blends.add(material.getRenderState().getBlendMode());
        }

        public void dispose(VertexBuffer buffer) {
        }

        public void dispose(IndexBuffer buffer) {
        }

        public void dispose(Texture texture) {
            assertTrue(live.remove(texture), "disposed twice or never created");
        }
    }

    private static void meshFrame(GpuRiveRenderer r, Image img, float dx, int blend) {
        r.beginFrame(64, 32, new Mat2D());
        r.save();
        Mat2D m = new Mat2D();
        m.set(1, 0, 0, 1, dx, 0);
        r.transform(m);
        r.drawImageMesh(img, V, UV, 3, IDX, 3, blend, 1f);
        r.restore();
        r.endFrame(0xff000000);
    }

    @Test
    void cameraMapsPixelsToClipSpace() {
        Device d = new Device();
        GpuRiveRenderer r = new GpuRiveRenderer();
        meshFrame(r, Image.createImage(4, 4, 0xffff0000), 0, 3);
        r.render(d);
        float[] m = d.viewProjection;
        assertNotNull(m);
        // column-major: (0,0) is the top left corner, (64,32) the bottom right
        assertEquals(-1f, (m[0] * 0 + m[4] * 0 + m[12]) / m[15], 1e-4f);
        assertEquals(1f, (m[1] * 0 + m[5] * 0 + m[13]) / m[15], 1e-4f);
        assertEquals(1f, (m[0] * 64 + m[4] * 32 + m[12]) / m[15], 1e-4f);
        assertEquals(-1f, (m[1] * 64 + m[5] * 32 + m[13]) / m[15], 1e-4f);
    }

    @Test
    void meshBuffersAndTexturesAreReusedAcrossFrames() {
        Device d = new Device();
        GpuRiveRenderer r = new GpuRiveRenderer();
        Image img = Image.createImage(4, 4, 0xffff0000);
        meshFrame(r, img, 0, 3);
        r.render(d);
        meshFrame(r, img, 5, 3);
        r.render(d);
        meshFrame(r, img, 7, 3);
        r.render(d);
        meshFrame(r, img, 9, 3);
        r.render(d);
        assertEquals(1, d.created, "the mesh image is uploaded once");
        assertEquals(4, d.meshes.size());
        // frames are recycled: the fourth frame draws with the second's buffers
        VertexBuffer vb = d.meshes.get(3).getVertices();
        assertSame(d.meshes.get(1).getVertices(), vb);
        assertTrue(vb.isDirty());
        assertEquals(9f, vb.getData()[0], 1e-4f);
        // nothing new published: the last frame is drawn again, nothing is uploaded
        vb.clearDirty();
        r.render(d);
        assertEquals(5, d.meshes.size());
        assertFalse(d.meshes.get(4).getVertices().isDirty());
        assertEquals(5, d.clears);
    }

    @Test
    void vectorLayersAreUploadedOncePerFrameAndReleased() {
        Device d = new Device();
        GpuRiveRenderer r = new GpuRiveRenderer();
        Image img = Image.createImage(4, 4, 0xffff0000);
        for (int i = 0; i < 3; i++) {
            r.beginFrame(16, 16, new Mat2D());
            r.drawPath(new RivePath(), new RivePaint());
            r.drawImageMesh(img, V, UV, 3, IDX, 3, 3, 1f);
            r.drawPath(new RivePath(), new RivePaint());
            r.endFrame(0);
            r.render(d);
            assertEquals(2, r.getLayerCount());
            assertEquals(1, r.getMeshCount());
        }
        int created = d.created;
        r.render(d);
        assertEquals(created, d.created, "redrawing a frame uploads nothing");
        // two frames are alive (current + spare), two layers each, plus the image
        assertTrue(d.live.size() <= 5, "layer textures leak: " + d.live.size());
        r.dispose(d);
        assertEquals(0, d.live.size());
    }

    private static RivePath triangle(float x0, float y0, float x1, float y1, float x2, float y2) {
        RivePath p = new RivePath();
        p.moveTo(x0, y0);
        p.lineTo(x1, y1);
        p.lineTo(x2, y2);
        p.close();
        return p;
    }

    private static int uploads(RivePath path, RivePaint paint) {
        Device d = new Device();
        GpuRiveRenderer r = new GpuRiveRenderer();
        r.beginFrame(64, 64, new Mat2D());
        r.drawPath(path, paint);
        r.endFrame(0);
        r.render(d);
        int created = d.created;
        r.dispose(d);
        return created;
    }

    @Test
    void aLongPathIsBoundedByAllOfItsPoints() {
        // pointCount counts floats; reading it as a point count ran past the
        // array on any path longer than half of it and threw out of draw(),
        // which dropped the rest of the frame on the GPU path
        RivePath p = new RivePath();
        p.moveTo(1, 1);
        for (int i = 1; i < 40; i++) {
            p.lineTo(1 + i * 1.5f, 1 + i * 1.5f);
        }
        Device d = new Device();
        GpuRiveRenderer r = new GpuRiveRenderer();
        r.beginFrame(64, 64, new Mat2D());
        r.drawPath(p, new RivePaint());
        r.endFrame(0);
        r.render(d);
        assertTrue(d.created > 0, "the layer was uploaded");
        assertTrue(d.lastTextureWidth >= 59 && d.lastTextureHeight >= 59,
                "the crop covers the far end of the path: " + d.lastTextureWidth + "x" + d.lastTextureHeight);
        r.dispose(d);
    }

    @Test
    void nonFiniteAndHugePointsKeepTheLayer() {
        RivePaint stroke = new RivePaint();
        stroke.stroke = true;
        stroke.thickness = 2;
        assertTrue(uploads(triangle(4, 4, 8, 30, 30, 8), new RivePaint()) > 0, "control");
        // a NaN point used to poison the dirty bounds and drop the layer
        assertTrue(uploads(triangle(Float.NaN, 4, 8, 30, 30, 8), new RivePaint()) > 0, "NaN point");
        // and a huge coordinate overflowed the int cast the same way
        assertTrue(uploads(triangle(-1e30f, -1e30f, 1e30f, 4, 4, 1e30f), stroke) > 0, "huge coordinates");
    }

    @Test
    void blendModesAndClipsTheGpuCannotExpressGoToTheCpuLayer() {
        Device d = new Device();
        GpuRiveRenderer r = new GpuRiveRenderer();
        Image img = Image.createImage(4, 4, 0xffff0000);
        r.beginFrame(16, 16, new Mat2D());
        r.drawImageMesh(img, V, UV, 3, IDX, 3, 12, 1f); // additive
        r.drawImageMesh(img, V, UV, 3, IDX, 3, 24, 1f); // multiply
        r.drawImageMesh(img, V, UV, 3, IDX, 3, 14, 1f); // screen
        r.save();
        r.clipPath(new RivePath());
        r.drawImageMesh(img, V, UV, 3, IDX, 3, 3, 1f);
        r.restore();
        r.drawImageMesh(img, V, UV, 3, IDX, 3, 3, 1f);
        r.endFrame(0);
        assertEquals(2, r.getMeshCount());
        assertEquals(3, r.getCpuMeshCount());
        r.render(d);
        // additive mesh, the layer holding the CPU meshes, the source-over mesh
        assertEquals(3, d.blends.size());
        assertEquals(RenderState.BlendMode.ADDITIVE, d.blends.get(0));
        assertEquals(RenderState.BlendMode.ALPHA, d.blends.get(1));
        assertEquals(RenderState.BlendMode.ALPHA, d.blends.get(2));
    }

    @Test
    void aLostContextIsNeverDisposedInto() {
        Device d = new Device();
        GpuRiveRenderer r = new GpuRiveRenderer();
        Image img = Image.createImage(4, 4, 0xffff0000);
        meshFrame(r, img, 0, 3);
        r.render(d);
        Device fresh = new Device();
        r.contextLost();
        meshFrame(r, img, 1, 3);
        r.render(fresh);
        r.render(fresh);
        assertEquals(1, fresh.created, "the image is uploaded again into the new context");
    }
}
