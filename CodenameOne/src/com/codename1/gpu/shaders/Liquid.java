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

import com.codename1.gpu.shader.ShaderMaterial;
import com.codename1.gpu.shader.ShaderNode;
import com.codename1.gpu.shader.ShaderType;

import static com.codename1.gpu.shader.ShaderNode.*;

/// Glowing and moving liquids, and the light water throws on what lies
/// under it. Use with `Material.Type.PBR`; the glow is emissive, so add
/// bloom for the full effect.
public final class Liquid {
    /// `VEC3` the cool colour: lava crust, slime, the floor under water.
    public static final String COLOR = "liqColor";
    /// `VEC3` the hot colour: molten cracks, glow, caustic light.
    public static final String GLOW = "liqGlow";
    /// `FLOAT` how bright the glow is.
    public static final String INTENSITY = "liqIntensity";
    /// `FLOAT` pattern repeats per world unit.
    public static final String SCALE = "liqScale";
    /// `FLOAT` world height of the water surface, for `caustics`.
    public static final String SURFACE = "liqSurface";

    private Liquid() {
    }

    private static ShaderMaterial base(int color, int glow, float intensity, float scale) {
        return new ShaderMaterial().declareUniform(COLOR, ShaderType.VEC3).declareUniform(GLOW, ShaderType.VEC3)
                .declareUniform(INTENSITY, ShaderType.FLOAT).declareUniform(SCALE, ShaderType.FLOAT)
                .setColorUniform(COLOR, color).setColorUniform(GLOW, glow).setUniform(INTENSITY, intensity)
                .setUniform(SCALE, scale);
    }

    /// Lava: dark cooling plates drifting over molten cracks that pulse.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial lava() {
        ShaderMaterial m = base(0x2a1610, 0xff5a10, 2.5f, 0.7f);
        ShaderNode p = Shading.worldUv(1f).mul(uniform(SCALE, ShaderType.FLOAT));
        ShaderNode warp = vec(Noise.fbm(p.add(time().mul(0.05f)), 3), Noise.fbm(p.add(vec2(4f, 1f)).sub(time().mul(0.04f)), 3));
        ShaderNode cells = Noise.cellular(p.add(warp.mul(0.8f)), time().mul(0.3f));
        ShaderNode crack = smoothstep(0.25f, 0.05f, cells);
        ShaderNode pulse = sin(time().mul(1.7f).add(p.x())).mul(0.2f).add(0.8f);
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(mix(uniform(COLOR, ShaderType.VEC3), vec3(0.1f, 0.03f, 0f), crack), f(1f)));
        m.set(ShaderMaterial.Hook.ROUGHNESS, mix(f(0.9f), f(0.4f), crack));
        m.set(ShaderMaterial.Hook.METALLIC, f(0f));
        m.set(ShaderMaterial.Hook.EMISSIVE, uniform(GLOW, ShaderType.VEC3)
                .mul(crack.mul(pulse).mul(uniform(INTENSITY, ShaderType.FLOAT))));
        m.set(ShaderMaterial.Hook.NORMAL, perturbNormal(mix(Noise.bumpNormal(p.mul(3f), 2, 0.5f), vec3(0f, 0f, 1f), crack)));
        return m;
    }

    /// Toxic sludge: glossy, slowly churning, with glowing bubbles that swell
    /// and pop.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial toxic() {
        ShaderMaterial m = base(0x2f5a12, 0x9cff3a, 1.4f, 1.2f);
        ShaderNode p = Shading.worldUv(1f).mul(uniform(SCALE, ShaderType.FLOAT));
        ShaderNode churn = Noise.fbm(p.add(vec2(0.1f, 0.07f).mul(time())), 4);
        ShaderNode bubble = smoothstep(0.22f, 0.12f, Noise.cellular(p.mul(2f), time().mul(1.3f)));
        ShaderNode rgb = mix(uniform(COLOR, ShaderType.VEC3).mul(churn.mul(0.6f).add(0.6f)),
                uniform(GLOW, ShaderType.VEC3), bubble.mul(0.5f));
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(rgb, f(1f)));
        m.set(ShaderMaterial.Hook.ROUGHNESS, f(0.12f));
        m.set(ShaderMaterial.Hook.METALLIC, f(0f));
        m.set(ShaderMaterial.Hook.EMISSIVE, uniform(GLOW, ShaderType.VEC3)
                .mul(bubble.add(churn.mul(0.15f)).mul(uniform(INTENSITY, ShaderType.FLOAT)).mul(0.5f)));
        m.set(ShaderMaterial.Hook.NORMAL, perturbNormal(Noise.bumpNormal(p.mul(1.5f).add(time().mul(0.1f)), 2, 0.3f)));
        return m;
    }

    /// Caustics: the dancing net of light under a water surface, added to
    /// whatever the mesh already looks like. Only below `SURFACE`, fading
    /// with depth. Put it on the sea bed, pool walls and anything sunk.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial caustics() {
        ShaderMaterial m = base(0x000000, 0xcff4ff, 0.8f, 0.9f).declareUniform(SURFACE, ShaderType.FLOAT)
                .setUniform(SURFACE, 0f);
        ShaderNode p = Shading.worldUv(1f).mul(uniform(SCALE, ShaderType.FLOAT));
        ShaderNode c1 = Noise.cellular(p, time().mul(0.9f));
        ShaderNode c2 = Noise.cellular(p.mul(1.3f).add(vec2(2.7f, 1.1f)), time().mul(-0.7f));
        ShaderNode net = pow(saturate(f(1f).sub(min(c1, c2))), 6f);
        ShaderNode depth = uniform(SURFACE, ShaderType.FLOAT).sub(worldPos().y());
        ShaderNode under = step(f(0f), depth).mul(saturate(f(1f).sub(depth.mul(0.12f))));
        ShaderNode facingUp = saturate(normalize(worldNormal()).y());
        m.set(ShaderMaterial.Hook.EMISSIVE, uniform(GLOW, ShaderType.VEC3)
                .mul(net.mul(under).mul(facingUp).mul(uniform(INTENSITY, ShaderType.FLOAT))).mul(sunColor()));
        return m;
    }
}
