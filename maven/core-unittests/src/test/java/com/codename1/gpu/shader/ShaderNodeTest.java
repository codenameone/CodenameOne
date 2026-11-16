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
package com.codename1.gpu.shader;

import com.codename1.gpu.Environment;
import com.codename1.gpu.Material;
import com.codename1.gpu.Texture;
import com.codename1.gpu.VertexFormat;
import com.codename1.impl.gpu.GlslShaderGenerator;
import org.junit.jupiter.api.Test;

import static com.codename1.gpu.shader.ShaderNode.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The portable shader graph: typing, the emitted source of each dialect, the
 * program cache key, the GLSL generator's splicing and the CPU evaluator.
 */
class ShaderNodeTest {

    private static ShaderMaterial waves() {
        ShaderMaterial sm = new ShaderMaterial()
                .declareUniform("amp", ShaderType.FLOAT)
                .declareUniform("tint", ShaderType.VEC3)
                .declareTexture("detail", null);
        ShaderNode h = sin(position().x().mul(2f).add(time())).mul(uniform("amp", ShaderType.FLOAT));
        sm.set(ShaderMaterial.Hook.VERTEX_OFFSET, vec(f(0f), h, f(0f)));
        ShaderNode d = sample("detail", worldPos().xz().mul(0.1f));
        ShaderNode fres = pow(dot(viewDir(), worldNormal()).oneMinus(), 5f);
        sm.set(ShaderMaterial.Hook.BASE_COLOR, vec(mix(uniform("tint", ShaderType.VEC3), d.rgb(), fres), f(1f)));
        sm.set(ShaderMaterial.Hook.ROUGHNESS, d.w().mul(0.5f));
        sm.set(ShaderMaterial.Hook.EMISSIVE, sunColor().mul(fres));
        return sm;
    }

    @Test
    void typesAreChecked() {
        assertEquals(ShaderType.VEC3, position().add(1f).getType());
        assertEquals(ShaderType.FLOAT, dot(position(), normal()).getType());
        assertEquals(ShaderType.VEC2, position().xz().getType());
        assertThrows(IllegalArgumentException.class, () -> position().add(uv()));
        assertThrows(IllegalArgumentException.class,
                () -> new ShaderMaterial().set(ShaderMaterial.Hook.ROUGHNESS, position()));
        assertThrows(IllegalArgumentException.class, () -> uniform("bad name", ShaderType.FLOAT));
    }

    @Test
    void scalarsNeverReachASwizzleOrAGeometricFunction() {
        // GLSL ES 1.00 and Metal reject both on a float
        ShaderNode t = time();
        assertSame(t, t.x());
        assertEquals(ShaderType.VEC3, time().swizzle("xxx").getType());
        assertThrows(IllegalArgumentException.class, () -> time().swizzle("y"));
        ShaderNode[] scalar = {time().swizzle("rrr").x(), dot(time(), time()), length(time()),
                distance(time(), f(1f)), normalize(time()), reflect(time(), f(1f))};
        for (ShaderNode n : scalar) {
            ShaderMaterial sm = new ShaderMaterial().set(ShaderMaterial.Hook.ROUGHNESS, n);
            for (ShaderEmitter.Dialect d : ShaderEmitter.Dialect.values()) {
                String src = emit(d, sm, ShaderMaterial.Hook.ROUGHNESS);
                assertFalse(src.matches("(?s).*(dot|length|distance|normalize|reflect)\\(.*"), src);
                assertFalse(src.contains("time."), src);
            }
        }
        ShaderEvaluator ev = new ShaderEvaluator(null).setInput("time", 0.25f);
        assertEquals(-0.25f, ev.evaluateFloat(reflect(time(), f(1f))), 1e-6f);
        assertEquals(0.0625f, ev.evaluateFloat(dot(time(), time())), 1e-6f);
    }

    @Test
    void scalarOperandsOfMinMaxAreWidened() {
        ShaderMaterial sm = new ShaderMaterial().set(ShaderMaterial.Hook.EMISSIVE, max(sunColor(), f(0.2f)));
        String msl = emit(ShaderEmitter.Dialect.MSL, sm, ShaderMaterial.Hook.EMISSIVE);
        assertTrue(msl.contains("float3 t0 = float3(0.2, 0.2, 0.2);") && msl.contains("max(sunColor, t0)"), msl);
    }

    @Test
    void dialectsSpellFunctionsTheirOwnWay() {
        ShaderMaterial sm = new ShaderMaterial().set(ShaderMaterial.Hook.EMISSIVE,
                mix(fract(worldPos()), sunColor(), f(0.5f)));
        String glsl = emit(ShaderEmitter.Dialect.GLSL, sm, ShaderMaterial.Hook.EMISSIVE);
        String hlsl = emit(ShaderEmitter.Dialect.HLSL, sm, ShaderMaterial.Hook.EMISSIVE);
        String msl = emit(ShaderEmitter.Dialect.MSL, sm, ShaderMaterial.Hook.EMISSIVE);
        assertTrue(glsl.contains("fract(") && glsl.contains("mix(") && glsl.contains("vec3"), glsl);
        assertTrue(hlsl.contains("frac(") && hlsl.contains("lerp(") && hlsl.contains("float3"), hlsl);
        assertTrue(msl.contains("fract(") && msl.contains("mix(") && msl.contains("float3"), msl);
    }

    @Test
    void sharedSubgraphsAreEmittedOnce() {
        ShaderNode s = sin(time());
        ShaderMaterial sm = new ShaderMaterial().set(ShaderMaterial.Hook.EMISSIVE, vec(s, s, s));
        String src = emit(ShaderEmitter.Dialect.GLSL, sm, ShaderMaterial.Hook.EMISSIVE);
        assertEquals(1, count(src, "sin("), src);
    }

    @Test
    void shaderKeyFollowsStructureNotValues() {
        ShaderMaterial a = waves();
        ShaderMaterial b = waves();
        assertEquals(a.getShaderKey(), b.getShaderKey());
        int v = a.getVersion();
        a.setUniform("amp", 3f);
        assertEquals(b.getShaderKey(), a.getShaderKey());
        assertEquals(v, a.getVersion());
        a.set(ShaderMaterial.Hook.DISCARD, f(0f));
        assertNotEquals(b.getShaderKey(), a.getShaderKey());
        Material m1 = new Material(Material.Type.PBR).setShader(b);
        Material m2 = new Material(Material.Type.PBR);
        assertNotEquals(m1.getShaderKey(), m2.getShaderKey());
    }

    @Test
    void glslGeneratorSplicesEveryHook() {
        Material m = new Material(Material.Type.PBR).setShader(waves());
        GlslShaderGenerator g = new GlslShaderGenerator(m, VertexFormat.POSITION_NORMAL_TEXCOORD,
                new Environment(), false);
        String vs = g.getVertexSource();
        String fs = g.getFragmentSource();
        assertTrue(vs.contains("uniform vec4 u_cu[2]"), vs);
        assertTrue(vs.contains("vec3 pos = a_position +") && vs.contains("vec4(pos, 1.0)"), vs);
        assertTrue(fs.contains("uniform sampler2D u_ct0"), fs);
        assertTrue(fs.contains("texture2D(u_ct0"), fs);
        assertTrue(fs.contains("base = "), fs);
        assertTrue(fs.contains("clamp(") && fs.contains("0.04"), fs);
        assertFalse(fs.contains("discard"), fs);
    }

    @Test
    void vertexStageCannotSample() {
        ShaderMaterial sm = new ShaderMaterial().declareTexture("t", null);
        sm.set(ShaderMaterial.Hook.VERTEX_OFFSET, sample("t", uv()).rgb());
        Material m = new Material(Material.Type.UNLIT).setShader(sm);
        assertThrows(IllegalArgumentException.class,
                () -> new GlslShaderGenerator(m, VertexFormat.POSITION_NORMAL_TEXCOORD, null, false));
    }

    @Test
    void evaluatorMatchesTheMath() {
        ShaderMaterial sm = waves().setUniform("amp", 0.5f).setColorUniform("tint", 0xff0000);
        ShaderEvaluator ev = new ShaderEvaluator(sm).setInput("time", 1f).setInput("position", 0.25f, 0f, 0f);
        float[] off = ev.evaluate(sm.get(ShaderMaterial.Hook.VERTEX_OFFSET));
        assertEquals(0f, off[0], 1e-6f);
        assertEquals((float) Math.sin(1.5) * 0.5f, off[1], 1e-5f);
        assertEquals(0f, off[2], 1e-6f);

        // facing the camera the fresnel term is zero, so the tint comes through
        ev.setInput("viewDir", 0f, 1f, 0f).setInput("worldNormal", 0f, 1f, 0f);
        float[] base = ev.evaluate(sm.get(ShaderMaterial.Hook.BASE_COLOR));
        assertEquals(1f, base[0], 1e-5f);
        assertEquals(0f, base[1], 1e-5f);
        assertEquals(1f, base[3], 1e-5f);
        // an unsampled texture reads white, alpha 1
        assertEquals(0.5f, ev.evaluateFloat(sm.get(ShaderMaterial.Hook.ROUGHNESS)), 1e-6f);

        // uniforms are live
        sm.setUniform("amp", 1f);
        assertEquals((float) Math.sin(1.5), ev.evaluate(sm.get(ShaderMaterial.Hook.VERTEX_OFFSET))[1], 1e-5f);
    }

    @Test
    void evaluatorCoversTheFunctionSet() {
        ShaderEvaluator ev = new ShaderEvaluator(null).setInput("time", 0.75f);
        assertEquals(0.75f, ev.evaluateFloat(fract(time().add(2f))), 1e-6f);
        assertEquals(0.5f, ev.evaluateFloat(smoothstep(0.5f, 1f, time())), 1e-6f);
        assertEquals(1f, ev.evaluateFloat(step(f(0.5f), time())), 0f);
        assertEquals(0.25f, ev.evaluateFloat(mod(time(), f(0.5f))), 1e-6f);
        assertEquals((float) Math.atan2(0.75, -1), ev.evaluateFloat(atan2(time(), f(-1f))), 1e-5f);
        assertEquals(2f, ev.evaluateFloat(select(f(1f), f(2f), f(3f))), 0f);
        float[] c = ev.evaluate(cross(vec3(1f, 0f, 0f), vec3(0f, 1f, 0f)));
        assertEquals(1f, c[2], 0f);
        assertEquals(5f, ev.evaluateFloat(length(vec2(3f, 4f))), 1e-6f);
        float[] sw = ev.evaluate(vec4(1f, 2f, 3f, 4f).swizzle("wzy"));
        assertArrayEquals(new float[]{4f, 3f, 2f, 0f}, sw, 0f);
    }

    private static String emit(ShaderEmitter.Dialect d, ShaderMaterial sm, ShaderMaterial.Hook hook) {
        StringBuilder sb = new StringBuilder();
        ShaderEmitter e = new ShaderEmitter(d, sm, new ShaderEmitter.Inputs() {
            public String builtin(String name) {
                return name;
            }

            public String uniform(int slot) {
                return "cu[" + slot + "]";
            }

            public String sample(int slot, String uv) {
                return "tex" + slot + "(" + uv + ")";
            }
        }, sb, "  ", "t");
        sb.append("  out = ").append(e.emit(sm.get(hook))).append(";\n");
        return sb.toString();
    }

    private static int count(String s, String sub) {
        int n = 0;
        for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + 1)) {
            n++;
        }
        return n;
    }

    @Test
    void materialMapsComposeWithTheAttachedShader() {
        Texture nm = new Texture(4, 4);
        Texture mr = new Texture(4, 4);
        Material m = new Material(Material.Type.PBR).setMetallic(0.5f).setRoughness(0.6f)
                .setNormalMap(nm, 1f).setMetallicRoughnessMap(mr);
        ShaderMaterial eff = m.getEffectiveShader();
        assertNotNull(eff);
        assertNotNull(eff.get(ShaderMaterial.Hook.NORMAL));
        assertNotNull(eff.get(ShaderMaterial.Hook.METALLIC));
        assertSame(nm, eff.getTexture(eff.getTextureSlot("cn1NormalMap")));
        float[] u = eff.getUniformValues();
        int slot = eff.getUniformSlot("cn1MetalRough");
        assertEquals(0.5f, u[slot * 4], 1e-6f);
        assertEquals(0.6f, u[slot * 4 + 1], 1e-6f);
        assertSame(eff, m.getEffectiveShader(), "the composition is cached");

        // a hook the application sets wins over the map, and its uniforms follow it
        ShaderMaterial user = new ShaderMaterial().declareUniform("k", ShaderType.FLOAT)
                .set(ShaderMaterial.Hook.METALLIC, uniform("k", ShaderType.FLOAT)).setUniform("k", 0.25f);
        m.setShader(user);
        eff = m.getEffectiveShader();
        assertSame(user.get(ShaderMaterial.Hook.METALLIC), eff.get(ShaderMaterial.Hook.METALLIC));
        user.setUniform("k", 0.75f);
        eff = m.getEffectiveShader();
        assertEquals(0.75f, eff.getUniformValues()[eff.getUniformSlot("k") * 4], 1e-6f);
        assertTrue(m.getShaderKey().contains("cn1NormalMap"));

        GlslShaderGenerator g = new GlslShaderGenerator(m, VertexFormat.POSITION_NORMAL_TEXCOORD,
                new Environment().setHemisphere(0xff8090a0, 0xff302010), false);
        String fs = g.getFragmentSource();
        assertTrue(fs.contains("n = normalize("), fs);
        assertTrue(fs.contains("envF") && fs.contains("mix(u_ground, u_sky, rdir.y"), fs);
    }

    @Test
    void perturbNormalLeavesAFlatMapAlone() {
        ShaderMaterial sm = new ShaderMaterial();
        ShaderEvaluator ev = new ShaderEvaluator(sm);
        float[][] normals = {{0f, 1f, 0f}, {1f, 0f, 0f}, {0f, 0f, -1f}, {0.6f, 0.8f, 0f}};
        for (float[] n : normals) {
            ev.setInput("worldNormal", n);
            float[] out = ev.evaluate(perturbNormal(vec3(0f, 0f, 1f)));
            for (int c = 0; c < 3; c++) {
                assertEquals(n[c], out[c], 1e-5f);
            }
            // a tilted map bends the normal off it but keeps it unit length
            float[] bent = ev.evaluate(perturbNormal(vec3(0.5f, 0f, 1f)));
            float len = (float) Math.sqrt(bent[0] * bent[0] + bent[1] * bent[1] + bent[2] * bent[2]);
            assertEquals(1f, len, 1e-5f);
            assertTrue(bent[0] * n[0] + bent[1] * n[1] + bent[2] * n[2] < 0.99f);
        }
        // a texel of (0.5, 0.5, 1) is the flat normal
        ev.setTextureSampler(null);
        float[] t = ev.evaluate(unpackNormal(vec4(0.5f, 0.5f, 1f, 1f), 1f));
        assertEquals(0f, t[0], 1e-6f);
        assertEquals(1f, t[2], 1e-6f);
    }
}
