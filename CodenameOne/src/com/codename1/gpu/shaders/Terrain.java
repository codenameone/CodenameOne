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

/// Ground. Layers are chosen per pixel from height and slope, so any mesh
/// (a heightfield, a `Primitives.grid` displaced by `VERTEX_OFFSET`, a level
/// built from boxes) is painted without UVs or splat textures. Use with
/// `Material.Type.PBR` or `Material.Type.LAMBERT`.
public final class Terrain {
    /// `VEC3` colour below `LEVELS.x`: beach, mud.
    public static final String SAND = "terSand";
    /// `VEC3` colour of flat ground.
    public static final String GRASS = "terGrass";
    /// `VEC3` colour of steep ground.
    public static final String ROCK = "terRock";
    /// `VEC3` colour above `LEVELS.y`.
    public static final String SNOW = "terSnow";
    /// `VEC4`: sand top height, snow line height, the slope (normal y) below
    /// which rock shows, and the blend width.
    public static final String LEVELS = "terLevels";
    /// `FLOAT` repeats of the detail noise per world unit.
    public static final String SCALE = "terScale";
    /// `FLOAT` world height of the waterline, under which ground darkens as
    /// if wet. Set it below the terrain to turn it off.
    public static final String WATERLINE = "terWater";

    private Terrain() {
    }

    /// Sand, grass, rock and snow by height and slope, with noise breaking
    /// up every boundary and wet ground along the waterline.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial layered() {
        ShaderMaterial m = new ShaderMaterial()
                .declareUniform(SAND, ShaderType.VEC3).declareUniform(GRASS, ShaderType.VEC3)
                .declareUniform(ROCK, ShaderType.VEC3).declareUniform(SNOW, ShaderType.VEC3)
                .declareUniform(LEVELS, ShaderType.VEC4).declareUniform(SCALE, ShaderType.FLOAT)
                .declareUniform(WATERLINE, ShaderType.FLOAT);
        ShaderNode lv = uniform(LEVELS, ShaderType.VEC4);
        ShaderNode p = worldPos().xz().mul(uniform(SCALE, ShaderType.FLOAT));
        ShaderNode big = Noise.fbm(p.mul(0.15f), 3);
        ShaderNode fine = Noise.fbm(p, 3);
        ShaderNode y = worldPos().y().add(big.sub(0.5f).mul(lv.w().mul(2f)));
        ShaderNode up = normalize(worldNormal()).y();
        ShaderNode blend = max(lv.w(), f(0.001f));

        ShaderNode grass = uniform(GRASS, ShaderType.VEC3).mul(fine.mul(0.5f).add(0.75f));
        ShaderNode sand = uniform(SAND, ShaderType.VEC3).mul(fine.mul(0.2f).add(0.9f));
        ShaderNode rock = uniform(ROCK, ShaderType.VEC3).mul(Noise.ridged(p.mul(1.7f), 3).mul(0.5f).add(0.7f));
        ShaderNode snow = uniform(SNOW, ShaderType.VEC3);

        ShaderNode rgb = mix(sand, grass, smoothstep(lv.x(), lv.x().add(blend), y));
        ShaderNode rockAmt = smoothstep(lv.z().add(0.08f), lv.z().sub(0.08f), up.add(fine.sub(0.5f).mul(0.15f)));
        rgb = mix(rgb, rock, rockAmt);
        // snow settles on what is flat enough to hold it
        ShaderNode snowAmt = smoothstep(lv.y(), lv.y().add(blend), y).mul(smoothstep(0.45f, 0.75f, up));
        rgb = mix(rgb, snow, snowAmt);
        ShaderNode wet = smoothstep(f(0.6f), f(0f), worldPos().y().sub(uniform(WATERLINE, ShaderType.FLOAT)));
        rgb = rgb.mul(f(1f).sub(wet.mul(0.45f)));
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(rgb, f(1f)));
        ShaderNode rough = mix(mix(f(0.9f), f(0.75f), rockAmt), f(0.35f), snowAmt);
        m.set(ShaderMaterial.Hook.ROUGHNESS, mix(rough, f(0.15f), wet));
        m.set(ShaderMaterial.Hook.METALLIC, f(0f));
        ShaderNode bump = Noise.bumpNormal(p.mul(2f), 2, 0.25f);
        m.set(ShaderMaterial.Hook.NORMAL, perturbNormal(mix(bump, vec3(0f, 0f, 1f), wet)));
        m.setColorUniform(SAND, 0xc9b48a).setColorUniform(GRASS, 0x5a7f3a).setColorUniform(ROCK, 0x7a756c)
                .setColorUniform(SNOW, 0xf1f4f7).setUniform(LEVELS, 0.4f, 9f, 0.72f, 0.6f)
                .setUniform(SCALE, 0.5f).setUniform(WATERLINE, -1000f);
        return m;
    }

    /// Bare stone on every face, its strata and grain mapped by `Shading.boxUv`
    /// so cliffs and boulders need no UVs.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial rock() {
        ShaderMaterial m = new ShaderMaterial().declareUniform(ROCK, ShaderType.VEC3)
                .declareUniform(SCALE, ShaderType.FLOAT);
        ShaderNode uv = Shading.boxUv(1f).mul(uniform(SCALE, ShaderType.FLOAT));
        ShaderNode strata = sin(worldPos().y().mul(3f).add(Noise.fbm(uv.mul(0.5f), 3).mul(4f))).mul(0.5f).add(0.5f);
        ShaderNode grain = Noise.fbm(uv.mul(3f), 3);
        ShaderNode tone = strata.mul(0.15f).add(grain.mul(0.4f)).add(0.65f);
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(uniform(ROCK, ShaderType.VEC3).mul(tone), f(1f)));
        m.set(ShaderMaterial.Hook.ROUGHNESS, f(0.85f).sub(grain.mul(0.2f)));
        m.set(ShaderMaterial.Hook.NORMAL, perturbNormal(Noise.bumpNormal(uv.mul(2f), 3, 0.4f)));
        m.setColorUniform(ROCK, 0x8a8178).setUniform(SCALE, 0.8f);
        return m;
    }
}
