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
package com.codename1.gaming;

import com.codename1.gpu.GpuCapabilities;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.IndexBuffer;
import com.codename1.gpu.Material;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Texture;
import com.codename1.gpu.VertexBuffer;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// The sprite renderer keeps a texture for every image it draws, and gives
/// one back when it is told the image is no longer drawn. These run its
/// callbacks against a device that only counts.
class SpriteTextureReleaseTest extends UITestBase {

    /// A device that records the textures it made and the ones it was
    /// asked to dispose, and fails on one disposed twice.
    private static final class CountingDevice extends GraphicsDevice {
        final List<Texture> live = new ArrayList<Texture>();
        int created;
        int disposed;
        int draws;

        public GpuCapabilities getCapabilities() {
            return null;
        }

        public Texture createTexture(Image image) {
            Texture t = new Texture(image.getWidth(), image.getHeight());
            t.setHandle(Integer.valueOf(created++));
            live.add(t);
            return t;
        }

        public Texture createTexture(int width, int height, int[] argb) {
            throw new AssertionError("the sprite renderer uploads images");
        }

        public void clear(int argbColor, boolean color, boolean depth) {
        }

        public void setViewport(int x, int y, int width, int height) {
        }

        public void draw(Mesh mesh, Material material, float[] modelMatrix) {
            draws++;
            Texture t = material.getTexture();
            assertTrue(t == null || live.contains(t), "a sprite was drawn with a texture that was disposed");
        }

        public void dispose(VertexBuffer buffer) {
        }

        public void dispose(IndexBuffer buffer) {
        }

        public void dispose(Texture texture) {
            assertTrue(live.remove(texture), "a texture was disposed twice, or was never created here");
            texture.setHandle(null);
            disposed++;
        }
    }

    private static Sprite sprite(Image image) {
        Sprite s = new Sprite();
        s.setImage(image);
        s.setSize(8, 8);
        return s;
    }

    private static SpriteRenderer started(CountingDevice device) {
        SpriteRenderer r = new SpriteRenderer();
        r.onInit(device);
        r.onResize(device, 64, 64);
        return r;
    }

    @Test
    void releasingAnImageDisposesItsTextureAtTheNextFrame() {
        CountingDevice device = new CountingDevice();
        SpriteRenderer r = started(device);
        Image kept = Image.createImage(8, 8, 0xff00ff00);
        Image dropped = Image.createImage(8, 8, 0xffff0000);
        Sprite a = sprite(kept);
        Sprite b = sprite(dropped);
        r.getScene().add(a);
        r.getScene().add(b);
        r.onFrame(device);
        assertEquals(2, r.getTextureCount());
        assertEquals(2, device.created);

        b.setVisible(false);
        r.releaseTexture(dropped);
        // Only a callback has the device.
        assertEquals(0, device.disposed);
        r.onFrame(device);
        assertEquals(1, device.disposed);
        assertEquals(1, r.getTextureCount());
        assertEquals(1, device.live.size());
        // The other image kept the texture it had.
        assertEquals(2, device.created);
    }

    @Test
    void aReleasedImageIsUploadedAgainWhenItIsDrawnAgain() {
        CountingDevice device = new CountingDevice();
        SpriteRenderer r = started(device);
        Image image = Image.createImage(8, 8, 0xff00ff00);
        Sprite s = sprite(image);
        r.getScene().add(s);
        r.onFrame(device);

        // Released while it is still on the scene: the frame that follows
        // gives the old texture back and draws with a new one.
        r.releaseTexture(image);
        int before = device.draws;
        r.onFrame(device);
        assertEquals(before + 1, device.draws);
        assertEquals(1, device.disposed);
        assertEquals(2, device.created);
        assertEquals(1, r.getTextureCount());

        // And released, hidden for a frame, then shown.
        s.setVisible(false);
        r.releaseTexture(image);
        r.onFrame(device);
        assertEquals(0, r.getTextureCount());
        assertEquals(0, device.live.size());
        s.setVisible(true);
        r.onFrame(device);
        assertEquals(3, device.created);
        assertEquals(1, r.getTextureCount());
    }

    @Test
    void anUnknownImageAndARepeatedReleaseDoNothing() {
        CountingDevice device = new CountingDevice();
        SpriteRenderer r = new SpriteRenderer();
        Image image = Image.createImage(8, 8, 0xff00ff00);
        // Before there is a device there is nothing to release.
        r.releaseTexture(image);
        r.releaseTexture(null);
        assertEquals(0, r.getTextureCount());
        r.onInit(device);
        r.onResize(device, 64, 64);
        r.releaseTexture(image);
        r.releaseTexture(null);
        r.onFrame(device);
        assertEquals(0, device.disposed);

        Sprite s = sprite(image);
        r.getScene().add(s);
        r.onFrame(device);
        s.setVisible(false);
        // Twice in one frame is one texture, disposed once: the device
        // above fails on a second.
        r.releaseTexture(image);
        r.releaseTexture(image);
        r.onFrame(device);
        assertEquals(1, device.disposed);
        r.releaseTexture(image);
        r.onFrame(device);
        assertEquals(1, device.disposed);
    }

    @Test
    void disposingTheRendererDoesNotDisposeAReleasedTextureAgain() {
        CountingDevice device = new CountingDevice();
        SpriteRenderer r = started(device);
        Image image = Image.createImage(8, 8, 0xff00ff00);
        Sprite s = sprite(image);
        r.getScene().add(s);
        r.onFrame(device);
        r.releaseTexture(image);
        // No frame came between the release and the end of the device.
        r.onDispose(device);
        assertEquals(1, device.disposed);
        assertEquals(0, device.live.size());
        assertEquals(0, r.getTextureCount());

        // A new device starts from nothing, and owes the old one nothing.
        CountingDevice next = new CountingDevice();
        r.onInit(next);
        r.onResize(next, 64, 64);
        r.onFrame(next);
        assertEquals(0, next.disposed);
        assertEquals(1, next.created);
    }

    @Test
    void theViewHandsAReleaseToItsRenderer() {
        CountingDevice device = new CountingDevice();
        GameView view = new GameView() {
            @Override
            protected void update(double deltaSeconds) {
            }
        };
        Image image = Image.createImage(8, 8, 0xff00ff00);
        Sprite s = sprite(image);
        view.getScene().add(s);
        view.getRenderer().onInit(device);
        view.getRenderer().onResize(device, 64, 64);
        view.getRenderer().onFrame(device);
        assertEquals(1, view.getTextureCount());
        s.setVisible(false);
        view.releaseTexture(image);
        view.getRenderer().onFrame(device);
        assertEquals(0, view.getTextureCount());
        assertEquals(1, device.disposed);
    }
}
