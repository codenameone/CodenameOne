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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The atmosphere, shadow and post processing settings: defaults that change
 * nothing, clamping, and the shader key that selects a variant.
 */
class EnvironmentLightPostTest {

    @Test
    void defaultEnvironmentIsInert() {
        Environment e = new Environment();
        assertFalse(e.isFog());
        assertFalse(e.isHemisphere());
        assertFalse(e.isToneMapping());
        assertFalse(e.isSky());
        assertEquals(1f, e.getExposure());
        assertEquals("fhtk", e.getShaderKey());
    }

    @Test
    void shaderKeyAndVersionFollowSettings() {
        Environment e = new Environment();
        int v = e.getVersion();
        e.setFog(0xff808080, 0.05f, -1f).setHemisphere(0xff0000ff, 0xff00ff00).setToneMapping(true)
                .setSky(0xff0000ff, 0xffffffff, 0xffffff00, 2f);
        assertTrue(e.getVersion() > v);
        assertEquals("FHTK", e.getShaderKey());
        assertEquals(0f, e.getFogHeightFalloff());
        assertEquals(1f, e.getCloudCover());
        e.setFog(0, 0, 0).clearHemisphere().clearSky();
        assertEquals("fhTk", e.getShaderKey());
    }

    @Test
    void lightClampsShadowSettings() {
        Light l = new Light().setShadows(true).setShadowMap(99999, -3f).setShadowStyle(4f, -1f);
        assertTrue(l.isShadows());
        assertEquals(4096, l.getShadowMapSize());
        assertTrue(l.getShadowExtent() > 0f);
        assertEquals(1f, l.getShadowStrength());
        assertEquals(0f, l.getShadowBias());
        l.setShadowCenter(3f, 0f, -4f);
        assertEquals(3f, l.getShadowCenterX());
        assertEquals(-4f, l.getShadowCenterZ());
    }

    @Test
    void shadowMatrixMapsTheCentreToTheMiddleOfTheMap() {
        Light l = new Light().setDirection(-0.3f, -1f, -0.2f).setShadowMap(1024, 10f).setShadowCenter(0f, 0f, 0f);
        float[] m = new float[16];
        l.computeShadowMatrix(m);
        // column-major: clip x/y of the world origin are the translation column
        assertEquals(0f, m[12], 0.05f);
        assertEquals(0f, m[13], 0.05f);
        assertTrue(m[14] > -1f && m[14] < 1f);
    }

    @Test
    void postProcessStartsOffAndClamps() {
        PostProcess p = new PostProcess();
        assertFalse(p.isBloom());
        assertEquals(0f, p.getVignette());
        p.setBloom(3f, 0.5f).setVignette(2f).setGrain(-1f).setGrade(-1f, 0.5f, 0xff000000, 0xffffffff);
        assertTrue(p.isBloom());
        assertEquals(1f, p.getBloomThreshold());
        assertEquals(1f, p.getVignette());
        assertEquals(0f, p.getGrain());
        assertEquals(0f, p.getContrast());
    }

    @Test
    void capabilitiesDefaultToNoShadowsOrPost() {
        GpuCapabilities c = new GpuCapabilities(2048, 8, false, false, false, "x");
        assertFalse(c.isShadowsSupported());
        assertFalse(c.isPostProcessSupported());
        assertTrue(new GpuCapabilities(2048, 8, false, false, false, "x", true, true).isPostProcessSupported());
    }
}
