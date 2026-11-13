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
package com.codename1.gpu.shaders;

import com.codename1.gpu.Environment;
import com.codename1.gpu.Material;
import com.codename1.gpu.VertexFormat;
import com.codename1.gpu.shader.ShaderEvaluator;
import com.codename1.gpu.shader.ShaderMaterial;
import com.codename1.impl.gpu.GlslShaderGenerator;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ShaderLibraryTest {

    /// Every factory in the library, by name. Shared with the scratch drivers
    /// that run the real shader compilers over the same set.
    static Map<String, ShaderMaterial> all() {
        Map<String, ShaderMaterial> m = new LinkedHashMap<>();
        m.put("Water.ocean", Water.ocean());
        m.put("Water.lake", Water.lake());
        m.put("Water.river", Water.river());
        m.put("Terrain.layered", Terrain.layered());
        m.put("Terrain.rock", Terrain.rock());
        m.put("Foliage.wind", Foliage.wind());
        m.put("Foliage.leaves", Foliage.leaves());
        m.put("Foliage.grass", Foliage.grass());
        m.put("Surface.paintedMetal", Surface.paintedMetal());
        m.put("Surface.rust", Surface.rust());
        m.put("Surface.wood", Surface.wood());
        m.put("Surface.concrete", Surface.concrete());
        m.put("Surface.glass", Surface.glass());
        m.put("Surface.ice", Surface.ice());
        m.put("Surface.fabric", Surface.fabric());
        m.put("Surface.carPaint", Surface.carPaint());
        m.put("Surface.paintSplats", Surface.paintSplats());
        m.put("Toon.cel", Toon.cel());
        m.put("Toon.hatching", Toon.hatching());
        m.put("Toon.lowPoly", Toon.lowPoly());
        m.put("Fx.dissolve", Fx.dissolve());
        m.put("Fx.hologram", Fx.hologram());
        m.put("Fx.forceField", Fx.forceField());
        m.put("Fx.beam", Fx.beam());
        m.put("Fx.fire", Fx.fire());
        m.put("Fx.smoke", Fx.smoke());
        m.put("Fx.portal", Fx.portal());
        m.put("Fx.xray", Fx.xray());
        m.put("Fx.pulse", Fx.pulse());
        m.put("Liquid.lava", Liquid.lava());
        m.put("Liquid.toxic", Liquid.toxic());
        m.put("Liquid.caustics", Liquid.caustics());
        m.put("Sky.gradient", Sky.gradient());
        return m;
    }

    @Test
    void everyShaderGeneratesAndEvaluates() {
        Environment env = ScenePresets.harbourDusk().getEnvironment();
        for (Map.Entry<String, ShaderMaterial> e : all().entrySet()) {
            ShaderMaterial sm = e.getValue();
            Material mat = new Material(Material.Type.PBR).setShader(sm);
            GlslShaderGenerator g = new GlslShaderGenerator(mat, VertexFormat.POSITION_NORMAL_TEXCOORD, env, true);
            assertFalse(g.getFragmentSource().isEmpty(), e.getKey());
            assertFalse(g.getVertexSource().isEmpty(), e.getKey());

            ShaderEvaluator ev = new ShaderEvaluator(sm).setInput("time", 3.7f)
                    .setInput("position", 0.3f, 1.2f, -0.4f).setInput("worldPos", 2.5f, 0.4f, -1.25f)
                    .setInput("worldNormal", 0.2f, 0.9f, 0.1f).setInput("viewDir", 0f, 0.6f, 0.8f)
                    .setInput("cameraPos", 0f, 3f, 6f).setInput("sunDir", -0.5f, 0.6f, -0.6f)
                    .setInput("sunColor", 1f, 0.9f, 0.8f).setInput("uv", 0.37f, 0.61f)
                    .setInput("baseColor", 0.6f, 0.6f, 0.6f, 1f);
            for (ShaderMaterial.Hook h : ShaderMaterial.Hook.values()) {
                if (sm.get(h) == null) {
                    continue;
                }
                float[] v = ev.evaluate(sm.get(h));
                for (float x : v) {
                    assertFalse(Float.isNaN(x) || Float.isInfinite(x), e.getKey() + " " + h + " is not finite");
                }
            }
        }
    }

    @Test
    void presetsAreComplete() {
        ScenePresets.Preset[] all = {ScenePresets.harbourDusk(), ScenePresets.clearNoon(), ScenePresets.overcast(),
            ScenePresets.night(), ScenePresets.desert(), ScenePresets.arctic()};
        for (ScenePresets.Preset p : all) {
            assertNotNull(p.getLight());
            assertTrue(p.getEnvironment().isHemisphere());
            assertNotNull(p.getPost());
            assertNotNull(p.getSky().get(ShaderMaterial.Hook.BASE_COLOR));
        }
    }

    @Test
    void splatsStayWithinTheUniformBudget() {
        ShaderMaterial s = Surface.paintSplats();
        assertTrue(s.getUniformCount() <= ShaderMaterial.MAX_UNIFORMS);
        Surface.setSplat(s, Surface.MAX_SPLATS - 1, 1f, 2f, 3f, 0.5f, 0xff0000);
        int slot = s.getUniformSlot("splatP" + (Surface.MAX_SPLATS - 1));
        assertEquals(0.5f, s.getUniformValues()[slot * 4 + 3], 0f);
    }
}
