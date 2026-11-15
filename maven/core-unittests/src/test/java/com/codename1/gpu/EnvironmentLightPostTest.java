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

    @Test
    void cascadesShareTheNearViewScaledInXY() {
        Light light = new Light().setDirection(-0.4f, -1f, -0.3f).setShadows(true);
        light.setShadowMap(1024, 10f).setShadowCenter(3f, 0f, -2f);
        assertEquals(1, light.getShadowCascades());
        assertEquals(0f, light.getShadowCascadeRatio(), 0f);
        light.setShadowCascades(2, 40f);
        assertEquals(2, light.getShadowCascades());
        assertEquals(0.25f, light.getShadowCascadeRatio(), 1e-6f);
        float[] near = new float[16];
        float[] far = new float[16];
        light.computeCascadeMatrix(0, near);
        light.computeCascadeMatrix(1, far);
        float[] single = new float[16];
        light.computeShadowMatrix(single);
        assertArrayEquals(single, near, 0f);
        for (int c = 0; c < 4; c++) {
            assertEquals(near[c * 4] * 0.25f, far[c * 4], 1e-6f);
            assertEquals(near[c * 4 + 1] * 0.25f, far[c * 4 + 1], 1e-6f);
            assertEquals(near[c * 4 + 2], far[c * 4 + 2], 0f);
            assertEquals(near[c * 4 + 3], far[c * 4 + 3], 0f);
        }
        // a far extent below the near one never shrinks the far cascade
        light.setShadowCascades(2, 5f);
        assertEquals(1f, light.getShadowCascadeRatio(), 0f);
        light.setShadowCascades(1, 40f);
        assertEquals(0f, light.getShadowCascadeRatio(), 0f);
    }

    @Test
    void everyGeneratorPicksTheCascadeFromTheFourthShadowParameter() {
        Material m = new Material(Material.Type.PBR);
        String frag = new com.codename1.impl.gpu.GlslShaderGenerator(m, VertexFormat.POSITION_NORMAL_TEXCOORD,
                new Environment(), true).getFragmentSource();
        assertTrue(frag.contains("uniform vec4 u_shadow;"), frag);
        assertTrue(frag.contains("if (u_shadow.w > 0.0)"), frag);
        assertTrue(frag.contains("p.xy = p.xy * u_shadow.w;"), frag);
    }

    @Test
    void bakedEnvironmentMapStacksSixBandsThatBlurTowardTheAverage() {
        int w = EnvironmentMaps.WIDTH;
        int h = EnvironmentMaps.BAND_HEIGHT;
        float[] radiance = new float[w * h * 3];
        // a bright horizon line on a dark world
        int spot = (h / 2) * w + w / 2;
        for (int i = (h / 2) * w * 3; i < (h / 2 + 1) * w * 3; i++) {
            radiance[i] = 2f;
        }
        int[] px = EnvironmentMaps.bake(radiance);
        assertEquals(w * h * EnvironmentMaps.BANDS, px.length);
        int sharp = px[spot] & 0xff;
        int blurred = px[(EnvironmentMaps.BANDS - 1) * w * h + spot] & 0xff;
        assertEquals(255, sharp);
        assertTrue(blurred < sharp, "the roughest band spreads the line: " + blurred);
        int far = px[(EnvironmentMaps.BANDS - 1) * w * h + spot + 8 * w] & 0xff;
        assertTrue(far > 0, "the roughest band reaches rows past the line");
        for (int i = 0; i < px.length; i++) {
            assertEquals(0xff, px[i] >>> 24);
        }
    }

    @Test
    void anEnvironmentMapSwitchesEveryGeneratorToImageBasedLighting() {
        Environment env = new Environment().setEnvironmentMap(new Texture(128, 384));
        Environment plain = new Environment();
        assertNotEquals(env.getShaderKey(), plain.getShaderKey());
        Material pbr = new Material(Material.Type.PBR);
        String frag = new com.codename1.impl.gpu.GlslShaderGenerator(pbr, VertexFormat.POSITION_NORMAL_TEXCOORD, env, false)
                .getFragmentSource();
        assertTrue(frag.contains("uniform sampler2D u_envMap;"), frag);
        assertTrue(frag.contains("envSpecular("), frag);
        String none = new com.codename1.impl.gpu.GlslShaderGenerator(pbr, VertexFormat.POSITION_NORMAL_TEXCOORD, plain, false)
                .getFragmentSource();
        assertFalse(none.contains("u_envMap"), none);
        Material unlit = new Material(Material.Type.UNLIT);
        assertFalse(new com.codename1.impl.gpu.GlslShaderGenerator(unlit, VertexFormat.POSITION_NORMAL_TEXCOORD, env, false)
                .getFragmentSource().contains("u_envMap"));
    }
}
