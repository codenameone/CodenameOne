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

/// Stylized, non photographic shading. These compute their own lighting
/// from the sun, so use them on a `Material.Type.UNLIT` material: the
/// material's colour and texture are the paint, the graph decides the light.
public final class Toon {
    /// `FLOAT` the number of light bands, 2 to 5.
    public static final String BANDS = "toonBands";
    /// `VEC3` the colour of the shadowed side, multiplied into the paint.
    public static final String SHADE = "toonShade";
    /// `VEC3` the rim light colour; black for none.
    public static final String RIM = "toonRim";
    /// `FLOAT` hatch lines per world unit.
    public static final String DENSITY = "toonDensity";

    private Toon() {
    }

    private static ShaderNode light() {
        return dot(normalize(worldNormal()), sunDir()).mul(0.5f).add(0.5f);
    }

    /// Cel shading: light in hard bands with a thin soft step between them,
    /// a cool shadow tint and a bright rim.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial cel() {
        ShaderMaterial m = new ShaderMaterial().declareUniform(BANDS, ShaderType.FLOAT)
                .declareUniform(SHADE, ShaderType.VEC3).declareUniform(RIM, ShaderType.VEC3);
        ShaderNode bands = uniform(BANDS, ShaderType.FLOAT);
        ShaderNode x = light().mul(bands);
        // a band edge 0.08 of a band wide keeps the steps from aliasing
        ShaderNode stepped = floor(x).add(smoothstep(0.46f, 0.54f, fract(x))).div(bands);
        ShaderNode lit = mix(uniform(SHADE, ShaderType.VEC3), sunColor(), saturate(stepped));
        ShaderNode rim = uniform(RIM, ShaderType.VEC3).mul(smoothstep(0.55f, 0.6f, Shading.fresnel(1f)))
                .mul(step(f(0.5f), light()));
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(baseColor().rgb().mul(lit).add(rim), baseColor().w()));
        m.setUniform(BANDS, 3f).setColorUniform(SHADE, 0x4a5578).setColorUniform(RIM, 0x606060);
        return m;
    }

    /// Pen hatching: the darker the light, the more crossing lines, drawn in
    /// world space so they stay put as the camera moves.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial hatching() {
        ShaderMaterial m = new ShaderMaterial().declareUniform(DENSITY, ShaderType.FLOAT)
                .declareUniform(SHADE, ShaderType.VEC3);
        ShaderNode p = Shading.boxUv(1f).mul(uniform(DENSITY, ShaderType.FLOAT));
        ShaderNode l = light();
        ShaderNode line1 = smoothstep(0.35f, 0.5f, abs(fract(p.x().add(p.y())).sub(0.5f)));
        ShaderNode line2 = smoothstep(0.35f, 0.5f, abs(fract(p.x().sub(p.y())).sub(0.5f)));
        ShaderNode ink = max(line1.mul(step(l, f(0.6f))), line2.mul(step(l, f(0.35f))));
        ShaderNode rgb = mix(baseColor().rgb(), uniform(SHADE, ShaderType.VEC3), ink);
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(rgb, baseColor().w()));
        m.setUniform(DENSITY, 12f).setColorUniform(SHADE, 0x202430);
        return m;
    }

    /// Flat faceted low poly look: one colour per face by height, toward the
    /// sun or away, with no smooth gradients.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial lowPoly() {
        ShaderMaterial m = new ShaderMaterial().declareUniform(SHADE, ShaderType.VEC3);
        ShaderNode l = floor(light().mul(4f)).div(4f);
        ShaderNode rgb = baseColor().rgb().mul(mix(uniform(SHADE, ShaderType.VEC3), sunColor(), l));
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(rgb, baseColor().w()));
        m.setColorUniform(SHADE, 0x55607a);
        return m;
    }
}
