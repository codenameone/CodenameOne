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

/// Plants that move in the wind. The sway is a vertex offset that grows with
/// height above the model's origin, so put each plant's origin at its root.
/// Gusts roll across the scene in world space, so neighbours move together.
public final class Foliage {
    /// `VEC2` the direction the wind blows, in world `x`/`z`.
    public static final String WIND = "windDir";
    /// `FLOAT` how far a point one unit above the root bends, world units.
    public static final String STRENGTH = "windStrength";
    /// `FLOAT` sways per second.
    public static final String SPEED = "windSpeed";
    /// `FLOAT` alpha below which a pixel of the texture is cut away.
    public static final String CUTOFF = "leafCutoff";
    /// `VEC3` colour at the root of a blade of grass.
    public static final String ROOT = "grassRoot";
    /// `VEC3` colour at its tip.
    public static final String TIP = "grassTip";

    private Foliage() {
    }

    private static ShaderNode sway() {
        ShaderNode dir = normalize(uniform(WIND, ShaderType.VEC2));
        ShaderNode wp = worldPos().xz();
        ShaderNode t = time().mul(uniform(SPEED, ShaderType.FLOAT));
        ShaderNode gust = Noise.value(wp.mul(0.08f).sub(dir.mul(time().mul(0.6f))));
        ShaderNode flutter = sin(t.mul((float) (2 * Math.PI)).add(wp.x().mul(0.9f)).add(wp.y().mul(0.7f)));
        ShaderNode h = max(position().y(), f(0f));
        ShaderNode bend = h.mul(h).mul(uniform(STRENGTH, ShaderType.FLOAT))
                .mul(gust.mul(1.4f).add(flutter.mul(0.35f)));
        return vec(dir.x().mul(bend), bend.mul(bend).mul(-0.2f), dir.y().mul(bend));
    }

    private static ShaderMaterial base() {
        return new ShaderMaterial().declareUniform(WIND, ShaderType.VEC2)
                .declareUniform(STRENGTH, ShaderType.FLOAT).declareUniform(SPEED, ShaderType.FLOAT);
    }

    /// Wind sway alone; the surface keeps its own colour and texture.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial wind() {
        ShaderMaterial m = base();
        m.set(ShaderMaterial.Hook.VERTEX_OFFSET, sway());
        m.setUniform(WIND, 1f, 0.3f).setUniform(STRENGTH, 0.08f).setUniform(SPEED, 0.5f);
        return m;
    }

    /// Leaf cards: wind sway, the texture's alpha cut out, and sunlight
    /// glowing through leaves seen against the sun.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial leaves() {
        ShaderMaterial m = wind().declareUniform(CUTOFF, ShaderType.FLOAT);
        m.set(ShaderMaterial.Hook.DISCARD, step(baseColor().w(), uniform(CUTOFF, ShaderType.FLOAT)));
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(baseColor().rgb(), f(1f)));
        ShaderNode through = pow(saturate(dot(viewDir().negate(), sunDir())), 3f);
        m.set(ShaderMaterial.Hook.EMISSIVE, baseColor().rgb().mul(sunColor()).mul(through.mul(0.5f)));
        m.setUniform(CUTOFF, 0.5f);
        return m;
    }

    /// Grass blades: a root to tip gradient along the texture's v (0 at the
    /// root), wind, and a sheen toward the sun. Pair with instancing or a
    /// field of crossed quads.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial grass() {
        ShaderMaterial m = wind().declareUniform(ROOT, ShaderType.VEC3).declareUniform(TIP, ShaderType.VEC3);
        ShaderNode tip = saturate(uv().y());
        ShaderNode variety = Noise.value(worldPos().xz().mul(0.3f)).mul(0.3f).add(0.85f);
        ShaderNode rgb = mix(uniform(ROOT, ShaderType.VEC3), uniform(TIP, ShaderType.VEC3), tip).mul(variety);
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(rgb, f(1f)));
        ShaderNode through = pow(saturate(dot(viewDir().negate(), sunDir())), 2f);
        m.set(ShaderMaterial.Hook.EMISSIVE, rgb.mul(sunColor()).mul(through.mul(tip).mul(0.4f)));
        m.setColorUniform(ROOT, 0x2f4a1c).setColorUniform(TIP, 0x9cbf5a).setUniform(STRENGTH, 0.25f)
                .setUniform(SPEED, 0.8f);
        return m;
    }
}
