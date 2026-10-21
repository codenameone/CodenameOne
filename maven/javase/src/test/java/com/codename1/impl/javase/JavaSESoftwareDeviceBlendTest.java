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
package com.codename1.impl.javase;

import com.codename1.gpu.Camera;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.RenderState;
import com.codename1.gpu.Texture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Holds the software rasteriser to `RenderState.BlendMode`'s contract. It is
/// the renderer the screenshots of every GPU backend are compared with by
/// eye, and the one the Unity compatibility samples are checked on without a
/// window, so what it draws is what the others are expected to.
///
/// A sprite is a white texture multiplied by a color in straight alpha:
/// black at 40% over an opaque background leaves six tenths of the
/// background, in a frame that is still opaque.
class JavaSESoftwareDeviceBlendTest {
    private static final int W = 16;
    private static final int H = 12;
    private static final int BACKGROUND = 0xff26334d;

    /// One quad covering the whole frame, textured white and colored `argb`.
    private static int drawOver(int background, int argb, RenderState.BlendMode mode) {
        JavaSESoftwareDevice device = new JavaSESoftwareDevice();
        device.resize(W, H);
        device.setViewport(0, 0, W, H);
        Mesh quad = Primitives.quad(device, 1f);
        int[] white = {0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff};
        Texture texture = device.createTexture(2, 2, white);
        RenderState state = RenderState.transparent().setBlendMode(mode).setDepthTest(false)
                .setCullMode(RenderState.CullMode.NONE);
        Material material = new Material(Material.Type.SPRITE).setRenderState(state).setTexture(texture)
                .setColor(argb);
        Camera camera = new Camera();
        camera.setOrthographic(H, -1000f, 1000f)
                .setAspect((float) W / H)
                .setPosition(0f, 0f, 1f)
                .setTarget(0f, 0f, 0f)
                .setUp(0f, 1f, 0f);
        device.clear(background, true, true);
        device.setCamera(camera);
        device.draw(quad, material, Matrix4.scaling(W, H, 1f));
        int centre = device.getImage().getRGB(W / 2, H / 2);
        // The quad covers the frame: what is true of the centre is true of
        // a pixel well inside each quarter.
        assertEquals(centre, device.getImage().getRGB(2, 2), "the quad does not cover the frame");
        assertEquals(centre, device.getImage().getRGB(W - 3, H - 3), "the quad does not cover the frame");
        return centre;
    }

    private static void assertNear(int expected, int actual, String what) {
        assertTrue(Math.abs(expected - actual) <= 1, what + ": expected " + expected + ", was " + actual);
    }

    private static void assertColor(int r, int g, int b, int argb, String what) {
        assertEquals(0xff, argb >>> 24, what + ": the frame is no longer opaque");
        assertNear(r, (argb >> 16) & 0xff, what + ", red");
        assertNear(g, (argb >> 8) & 0xff, what + ", green");
        assertNear(b, argb & 0xff, what + ", blue");
    }

    @Test
    void blackAtFortyPercentLeavesSixTenthsOfTheBackground() {
        int out = drawOver(BACKGROUND, 0x66000000, RenderState.BlendMode.ALPHA);
        // 0.6 * (38, 51, 77)
        assertColor(23, 31, 46, out, "black at 40% over the background");
    }

    @Test
    void aTintIsNotPremultiplied() {
        int out = drawOver(0xff000000, 0x80ff8040, RenderState.BlendMode.ALPHA);
        // (255, 128, 64) * 128 / 255
        assertColor(128, 64, 32, out, "a tint at 50% over black");
    }

    @Test
    void opaqueReplacesAndClearLeaves() {
        assertColor(0x33, 0x66, 0x99, drawOver(BACKGROUND, 0xff336699, RenderState.BlendMode.ALPHA),
                "an opaque sprite");
        assertColor(38, 51, 77, drawOver(BACKGROUND, 0x00336699, RenderState.BlendMode.ALPHA),
                "a sprite with no alpha");
    }

    @Test
    void additiveLightIsWeightedByItsAlpha() {
        int out = drawOver(BACKGROUND, 0x66808080, RenderState.BlendMode.ADDITIVE);
        // (38, 51, 77) + 128 * 0.4
        assertColor(89, 102, 128, out, "additive grey at 40%");
        assertColor(38, 51, 77, drawOver(BACKGROUND, 0x00ffffff, RenderState.BlendMode.ADDITIVE),
                "additive light with no alpha");
    }
}
