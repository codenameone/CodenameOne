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

/// Sky domes: draw an inside-out sphere (`Primitives.sphere(r, 24, 32, true)`)
/// around the camera with a `Material.Type.UNLIT` material and `setFogged(false)`. The
/// colour depends only on the direction from the camera, so the dome's size
/// and position do not matter. The sun follows the scene's light.
///
/// For the sky to light the scene as well, give the `Environment` matching
/// hemisphere colours; `ScenePresets` does both.
public final class Sky {
    /// `VEC3` the colour straight up.
    public static final String ZENITH = "skyZenith";
    /// `VEC3` the colour at the horizon.
    public static final String HORIZON = "skyHorizon";
    /// `VEC3` the colour below the horizon.
    public static final String GROUND = "skyGround";
    /// `VEC3` the glow around the sun, scattered into the haze.
    public static final String GLOW = "skyGlow";
    /// `FLOAT` cloud cover, 0 to 1.
    public static final String CLOUDS = "skyClouds";
    /// `FLOAT` star brightness, 0 for none.
    public static final String STARS = "skyStars";

    private Sky() {
    }

    /// A clear or hazy sky with a sun disc, a wide glow around it, the
    /// horizon band and drifting clouds.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial gradient() {
        ShaderMaterial m = new ShaderMaterial()
                .declareUniform(ZENITH, ShaderType.VEC3).declareUniform(HORIZON, ShaderType.VEC3)
                .declareUniform(GROUND, ShaderType.VEC3).declareUniform(GLOW, ShaderType.VEC3)
                .declareUniform(CLOUDS, ShaderType.FLOAT).declareUniform(STARS, ShaderType.FLOAT);
        ShaderNode dir = normalize(worldPos().sub(cameraPos()));
        ShaderNode y = dir.y();
        ShaderNode up = pow(saturate(y), 0.45f);
        ShaderNode sky = mix(uniform(HORIZON, ShaderType.VEC3), uniform(ZENITH, ShaderType.VEC3), up);
        ShaderNode rgb = mix(sky, uniform(GROUND, ShaderType.VEC3), smoothstep(0f, -0.08f, y));
        ShaderNode sd = saturate(dot(dir, sunDir()));
        // Mie-like forward scattering: a broad glow and a tight halo
        ShaderNode glow = pow(sd, 6f).mul(0.6f).add(pow(sd, 48f).mul(0.8f));
        rgb = rgb.add(uniform(GLOW, ShaderType.VEC3).mul(glow).mul(smoothstep(-0.1f, 0.2f, y).mul(0.7f).add(0.3f)));
        rgb = rgb.add(sunColor().mul(smoothstep(0.9994f, 0.9997f, sd).mul(3f)));
        // clouds on a plane above: project the ray, thin out toward the horizon
        ShaderNode cp = dir.xz().div(max(y, f(0.05f))).mul(0.6f).add(vec2(0.02f, 0.01f).mul(time()));
        ShaderNode c = Noise.fbm(cp, 5);
        ShaderNode cover = uniform(CLOUDS, ShaderType.FLOAT);
        ShaderNode cl = smoothstep(f(1f).sub(cover), f(1.35f).sub(cover), c).mul(smoothstep(0.02f, 0.25f, y));
        ShaderNode cloudRgb = mix(uniform(HORIZON, ShaderType.VEC3).mul(0.85f), vec3(1f, 1f, 1f), c)
                .add(uniform(GLOW, ShaderType.VEC3).mul(pow(sd, 4f).mul(0.8f)));
        rgb = mix(rgb, cloudRgb, cl);
        // stars, hidden behind clouds and the horizon haze
        ShaderNode cell = floor(dir.mul(220f));
        ShaderNode star = step(f(0.9975f), Noise.hash(cell.xy().add(cell.z().mul(31.7f))));
        ShaderNode twinkle = sin(time().mul(3f).add(Noise.hash(cell.swizzle("zx")).mul(40f))).mul(0.3f).add(0.7f);
        rgb = rgb.add(vec3(1f, 1f, 1f).mul(star.mul(twinkle).mul(uniform(STARS, ShaderType.FLOAT))
                .mul(f(1f).sub(cl)).mul(smoothstep(0.05f, 0.3f, y))));
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(rgb, f(1f)));
        day(m);
        return m;
    }

    /// Sets a `gradient` sky to a clear midday.
    ///
    /// #### Parameters
    ///
    /// - `sky`: a material from `gradient`
    ///
    /// #### Returns
    ///
    /// the material
    public static ShaderMaterial day(ShaderMaterial sky) {
        return sky.setColorUniform(ZENITH, 0x2f6fc4).setColorUniform(HORIZON, 0xbcd8ec)
                .setColorUniform(GROUND, 0x5d6a72).setColorUniform(GLOW, 0xfff2d8)
                .setUniform(CLOUDS, 0.35f).setUniform(STARS, 0f);
    }

    /// Sets a `gradient` sky to a warm dusk: deep blue overhead, apricot at
    /// the horizon, a big rose glow around a low sun, first stars.
    ///
    /// #### Parameters
    ///
    /// - `sky`: a material from `gradient`
    ///
    /// #### Returns
    ///
    /// the material
    public static ShaderMaterial dusk(ShaderMaterial sky) {
        return sky.setColorUniform(ZENITH, 0x1c2a52).setColorUniform(HORIZON, 0xf0a46a)
                .setColorUniform(GROUND, 0x2a2730).setColorUniform(GLOW, 0xff8a5a)
                .setUniform(CLOUDS, 0.3f).setUniform(STARS, 0.6f);
    }

    /// Sets a `gradient` sky to night: dark blue, a cold moon glow, stars.
    ///
    /// #### Parameters
    ///
    /// - `sky`: a material from `gradient`
    ///
    /// #### Returns
    ///
    /// the material
    public static ShaderMaterial night(ShaderMaterial sky) {
        return sky.setColorUniform(ZENITH, 0x03060f).setColorUniform(HORIZON, 0x16233a)
                .setColorUniform(GROUND, 0x05070b).setColorUniform(GLOW, 0x6f88b0)
                .setUniform(CLOUDS, 0.2f).setUniform(STARS, 1.2f);
    }

    /// Sets a `gradient` sky to a grey overcast.
    ///
    /// #### Parameters
    ///
    /// - `sky`: a material from `gradient`
    ///
    /// #### Returns
    ///
    /// the material
    public static ShaderMaterial overcast(ShaderMaterial sky) {
        return sky.setColorUniform(ZENITH, 0x7d8690).setColorUniform(HORIZON, 0xb4b9bd)
                .setColorUniform(GROUND, 0x4e5257).setColorUniform(GLOW, 0x404040)
                .setUniform(CLOUDS, 0.95f).setUniform(STARS, 0f);
    }
}
