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
package com.codename1.impl.gpu;

import com.codename1.gpu.Environment;
import com.codename1.gpu.Material;
import com.codename1.gpu.VertexFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The GLSL the shared generator writes for the atmosphere, PBR, the sky and
 * shadows: each feature appears only when asked for, so a scene that sets
 * none of them gets the shaders it always had.
 */
class GlslShaderGeneratorFeaturesTest {

    private static String frag(Material m, Environment e, boolean shadowed) {
        return new GlslShaderGenerator(m, VertexFormat.POSITION_NORMAL_TEXCOORD, e, shadowed).getFragmentSource();
    }

    @Test
    void plainEnvironmentAddsNothing() {
        Material m = new Material(Material.Type.LAMBERT);
        String plain = new GlslShaderGenerator(m, VertexFormat.POSITION_NORMAL_TEXCOORD).getFragmentSource();
        assertEquals(plain, frag(m, new Environment(), false));
        assertFalse(plain.contains("u_fogColor"));
        assertFalse(plain.contains("aces"));
    }

    @Test
    void fogHemisphereAndToneMappingAreEmitted() {
        Environment e = new Environment().setFog(0xff808080, 0.05f, 0.1f).setHemisphere(0xff8090a0, 0xff403020)
                .setToneMapping(true);
        String f = frag(new Material(Material.Type.LAMBERT), e, false);
        assertTrue(f.contains("u_fogColor"));
        assertTrue(f.contains("u_sky"));
        assertTrue(f.contains("aces("));
    }

    @Test
    void skyOnlyForSkyboxWithSkyOn() {
        Environment e = new Environment().setSky(0xff3060a0, 0xffb0c0d0, 0xfffff0d0, 0.4f);
        assertTrue(frag(new Material(Material.Type.SKYBOX), e, false).contains("u_zenith"));
        assertFalse(frag(new Material(Material.Type.LAMBERT), e, false).contains("u_zenith"));
        assertFalse(frag(new Material(Material.Type.SKYBOX), new Environment(), false).contains("u_zenith"));
    }

    @Test
    void shadowSamplingOnlyWhenShadowed() {
        Material m = new Material(Material.Type.PBR);
        assertTrue(frag(m, new Environment(), true).contains("u_shadowMap"));
        assertFalse(frag(m, new Environment(), false).contains("u_shadowMap"));
        assertNotEquals(GlslShaderGenerator.variantKey(new Environment(), true),
                GlslShaderGenerator.variantKey(new Environment(), false));
    }

    @Test
    void postAndShadowShadersAreComplete() {
        for (String s : new String[]{GlslShaderGenerator.shadowVertex(), GlslShaderGenerator.shadowFragment(),
                GlslShaderGenerator.postVertex(), GlslShaderGenerator.brightFragment(),
                GlslShaderGenerator.blurFragment(), GlslShaderGenerator.compositeFragment()}) {
            assertTrue(s.contains("void main()"), s);
        }
        assertTrue(GlslShaderGenerator.compositeFragment().contains("u_bloom"));
    }
}
