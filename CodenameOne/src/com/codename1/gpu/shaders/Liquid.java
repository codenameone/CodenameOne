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

    /// Lava: a black basalt crust broken into large plates that drift apart
    /// over a molten flow. The seams glow white-yellow at their core and cool
    /// to orange and deep red toward the crust, and the molten pools churn.
    /// Fine detail fades with distance, so a sea of it reads as plates and
    /// glowing rivers at the horizon rather than shimmering speckle.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial lava() {
        ShaderMaterial m = base(0x100807, 0xff6a18, 3.2f, 0.05f);
        ShaderNode p = Shading.worldUv(1f).mul(uniform(SCALE, ShaderType.FLOAT));
        ShaderNode flow = time().mul(0.035f);
        ShaderNode warp = vec(Noise.fbm(p.mul(0.6f).add(flow), 3), Noise.fbm(p.mul(0.6f).add(vec2(5.2f, 1.3f)).sub(flow), 3));
        ShaderNode q = p.add(warp.mul(0.9f));
        // Distance to the nearest plate seam: large plates, then a second
        // octave of cracks inside each plate that fades out far away.
        ShaderNode near = Shading.distanceFade(30f, 140f);
        ShaderNode cells = Noise.cellularEdge(q, time().mul(0.12f));
        ShaderNode fine = Noise.cellularEdge(q.mul(3.1f).add(vec2(1.7f, 9.2f)), time().mul(0.2f));
        ShaderNode seam = smoothstep(0.14f, 0.0f, cells);
        ShaderNode hair = smoothstep(0.07f, 0.0f, fine).mul(near).mul(0.5f);
        // Here and there the crust has melted through into a pool.
        ShaderNode pool = smoothstep(0.72f, 0.82f, Noise.fbm(q.mul(0.45f).add(vec2(3.1f, 7.7f)), 3));
        ShaderNode molten = max(max(seam, hair), pool);
        // The molten flow under the crust: churning brightness, so the
        // rivers between plates are never one flat colour.
        ShaderNode churn = Noise.fbm(q.mul(2.3f).sub(flow.mul(3f)), 3);
        ShaderNode heat = clamp(molten.mul(churn.mul(0.7f).add(0.6f)), 0f, 1f);
        ShaderNode deep = vec3(0.55f, 0.06f, 0.01f);
        ShaderNode hot = uniform(GLOW, ShaderType.VEC3);
        ShaderNode core = vec3(1f, 0.86f, 0.45f);
        ShaderNode glow = Shading.ramp(heat, deep, hot, core);
        // Crust: dark, rough, with a faint ember tint near the seams.
        ShaderNode crustTone = Noise.fbm(q.mul(4f), 2).mul(0.5f).add(0.5f);
        ShaderNode crust = uniform(COLOR, ShaderType.VEC3).mul(crustTone)
                .add(vec3(0.18f, 0.03f, 0f).mul(smoothstep(0.25f, 0.02f, cells)).mul(0.5f));
        ShaderNode pulse = sin(time().mul(1.3f).add(p.x().mul(2f)).add(p.y())).mul(0.12f).add(0.88f);
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(mix(crust, glow.mul(0.3f), molten), f(1f)));
        m.set(ShaderMaterial.Hook.ROUGHNESS, mix(f(0.95f), f(0.35f), molten));
        m.set(ShaderMaterial.Hook.METALLIC, f(0f));
        m.set(ShaderMaterial.Hook.EMISSIVE, glow.mul(heat.mul(pulse).mul(uniform(INTENSITY, ShaderType.FLOAT))));
        m.set(ShaderMaterial.Hook.NORMAL, perturbNormal(mix(Noise.bumpNormal(q.mul(5f), 2, 0.8f), vec3(0f, 0f, 1f),
                max(molten, f(1f).sub(near)))));
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
