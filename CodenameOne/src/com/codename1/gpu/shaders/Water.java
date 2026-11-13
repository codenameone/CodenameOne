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

/// Water surfaces. Each factory returns a `ShaderMaterial` for a
/// `Material.Type.PBR` material on a finely subdivided plane (see
/// `Primitives.grid`): the waves move its vertices, the normals and colour
/// follow them, and the engine's lighting adds the sky reflection and the
/// sun's glitter.
///
/// ```java
/// Material sea = new Material(Material.Type.PBR).setShader(Water.ocean()
///         .setColorUniform(Water.DEEP, 0x0b3d4f).setUniform(Water.WAVE_HEIGHT, 0.6f));
/// ```
///
/// Waves are computed from world `x`/`z`, so place the plane unrotated and
/// unscaled; a translation is fine. Tune with the uniforms named below,
/// every frame if you like.
public final class Water {
    /// `VEC3` colour of deep water, looking straight down.
    public static final String DEEP = "waterDeep";
    /// `VEC3` colour where light passes through crests and shallows.
    public static final String SHALLOW = "waterShallow";
    /// `VEC3` colour of foam.
    public static final String FOAM = "waterFoam";
    /// `FLOAT` height of the largest wave, world units.
    public static final String WAVE_HEIGHT = "waveHeight";
    /// `FLOAT` length of the largest wave, world units.
    public static final String WAVE_LENGTH = "waveLength";
    /// `FLOAT` speed multiplier; 1 moves like deep sea water.
    public static final String WAVE_SPEED = "waveSpeed";
    /// `FLOAT` 0 for round swells, 1 for sharp crests.
    public static final String CHOPPINESS = "waveChop";
    /// `FLOAT` how much of the crests turns white, 0 to 1.
    public static final String FOAM_AMOUNT = "foamAmount";
    /// `FLOAT` surface roughness; lower is a sharper sun glitter.
    public static final String ROUGHNESS = "waterRough";
    /// `VEC2` the direction the main swell travels.
    public static final String DIRECTION = "waveDir";
    /// `FLOAT` world units per second the surface drifts, for rivers.
    public static final String FLOW = "waterFlow";

    // Direction (relative to DIRECTION), relative length and relative height
    // of each wave. Lengths are not multiples of each other, so the pattern
    // never visibly repeats.
    private static final float[][] OCEAN = {
        {0f, 1f, 1f}, {0.6f, 0.61f, 0.55f}, {-0.45f, 0.37f, 0.33f}, {1.3f, 0.23f, 0.18f}
    };
    private static final float[][] LAKE = {{0f, 1f, 1f}, {0.9f, 0.53f, 0.5f}};

    private Water() {
    }

    /// Open sea: four crossing Gerstner waves, foamy crests, cellular foam
    /// streaks and fine moving ripples.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial ocean() {
        ShaderMaterial m = build(OCEAN, true, false);
        m.setColorUniform(DEEP, 0x0a3346).setColorUniform(SHALLOW, 0x2f8f8c).setColorUniform(FOAM, 0xf2f6f5)
                .setUniform(WAVE_HEIGHT, 0.45f).setUniform(WAVE_LENGTH, 14f).setUniform(WAVE_SPEED, 1f)
                .setUniform(CHOPPINESS, 0.6f).setUniform(FOAM_AMOUNT, 0.5f).setUniform(ROUGHNESS, 0.08f)
                .setUniform(DIRECTION, 1f, 0.3f);
        return m;
    }

    /// Calm water: two long, low waves and wind ripples, no foam.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial lake() {
        ShaderMaterial m = build(LAKE, false, false);
        m.setColorUniform(DEEP, 0x1d3a3a).setColorUniform(SHALLOW, 0x4f7f6a)
                .setUniform(WAVE_HEIGHT, 0.06f).setUniform(WAVE_LENGTH, 6f).setUniform(WAVE_SPEED, 0.6f)
                .setUniform(CHOPPINESS, 0.2f).setUniform(ROUGHNESS, 0.04f).setUniform(DIRECTION, 1f, 0.2f);
        return m;
    }

    /// Running water: the ripples and foam drift along `DIRECTION` at
    /// `FLOW` units per second.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial river() {
        ShaderMaterial m = build(LAKE, true, true);
        m.setColorUniform(DEEP, 0x24453c).setColorUniform(SHALLOW, 0x5f8f70).setColorUniform(FOAM, 0xe8efe8)
                .setUniform(WAVE_HEIGHT, 0.04f).setUniform(WAVE_LENGTH, 3f).setUniform(WAVE_SPEED, 1.4f)
                .setUniform(CHOPPINESS, 0.3f).setUniform(FOAM_AMOUNT, 0.25f).setUniform(ROUGHNESS, 0.12f)
                .setUniform(DIRECTION, 1f, 0f).setUniform(FLOW, 1.5f);
        return m;
    }

    private static ShaderNode u(String name, ShaderType t) {
        return uniform(name, t);
    }

    private static ShaderMaterial build(float[][] waves, boolean foam, boolean flow) {
        ShaderMaterial m = new ShaderMaterial()
                .declareUniform(DEEP, ShaderType.VEC3).declareUniform(SHALLOW, ShaderType.VEC3)
                .declareUniform(WAVE_HEIGHT, ShaderType.FLOAT).declareUniform(WAVE_LENGTH, ShaderType.FLOAT)
                .declareUniform(WAVE_SPEED, ShaderType.FLOAT).declareUniform(CHOPPINESS, ShaderType.FLOAT)
                .declareUniform(ROUGHNESS, ShaderType.FLOAT).declareUniform(DIRECTION, ShaderType.VEC2);
        if (foam) {
            m.declareUniform(FOAM, ShaderType.VEC3).declareUniform(FOAM_AMOUNT, ShaderType.FLOAT);
        }
        if (flow) {
            m.declareUniform(FLOW, ShaderType.FLOAT);
        }
        ShaderNode dir = normalize(u(DIRECTION, ShaderType.VEC2));
        ShaderNode side = vec(dir.y().negate(), dir.x());

        // vertex stage: displace by the sum of the waves at the undisplaced point
        ShaderNode offset = null;
        for (float[] wave : waves) {
            Wave w = new Wave(wave, dir, side, worldPos().xz());
            ShaderNode qa = w.q.mul(w.a);
            ShaderNode o = vec(w.d.x().mul(qa).mul(w.cos), w.a.mul(w.sin), w.d.y().mul(qa).mul(w.cos));
            offset = offset == null ? o : offset.add(o);
        }
        m.set(ShaderMaterial.Hook.VERTEX_OFFSET, offset);

        // fragment stage: the analytic normal and crest height at this pixel
        ShaderNode p = worldPos().xz();
        ShaderNode nx = null;
        ShaderNode nz = null;
        ShaderNode ny = null;
        ShaderNode height = null;
        for (float[] wave : waves) {
            Wave w = new Wave(wave, dir, side, p);
            ShaderNode wa = w.k.mul(w.a);
            ShaderNode dx = w.d.x().mul(wa).mul(w.cos);
            ShaderNode dz = w.d.y().mul(wa).mul(w.cos);
            ShaderNode dy = w.q.mul(wa).mul(w.sin);
            ShaderNode h = w.sin.mul(wave[2]);
            nx = nx == null ? dx : nx.add(dx);
            nz = nz == null ? dz : nz.add(dz);
            ny = ny == null ? dy : ny.add(dy);
            height = height == null ? h : height.add(h);
        }
        ShaderNode crest = saturate(height.mul(0.5f).add(0.5f));

        // fine ripples: two value noise layers drifting against each other
        ShaderNode drift = flow ? dir.mul(u(FLOW, ShaderType.FLOAT)).mul(time()) : dir.mul(time().mul(0.35f));
        ShaderNode rp = p.mul(1.7f).sub(drift);
        ShaderNode r1 = Noise.value(rp);
        ShaderNode r2 = Noise.value(rp.mul(2.3f).add(vec2(3.1f, 7.7f)).add(side.mul(time().mul(0.4f))));
        ShaderNode ripple = vec(r1.sub(0.5f), r2.sub(0.5f)).mul(0.35f);
        ShaderNode normal = normalize(vec(nx.negate().add(ripple.x()), f(1f).sub(ny), nz.negate().add(ripple.y())));
        m.set(ShaderMaterial.Hook.NORMAL, normal);

        // colour: deep looking down, lighter through the crests and at grazing angles
        ShaderNode facing = saturate(dot(normal, viewDir()));
        ShaderNode tint = mix(u(SHALLOW, ShaderType.VEC3), u(DEEP, ShaderType.VEC3),
                saturate(facing.mul(1.2f).sub(crest.mul(0.35f))));
        ShaderNode rgb = tint;
        if (foam) {
            ShaderNode cells = Noise.cellular(p.mul(0.9f).sub(drift.mul(0.5f)), time().mul(0.6f));
            ShaderNode streak = smoothstep(0.55f, 0.1f, cells).mul(Noise.value(p.mul(0.35f).add(drift.mul(0.2f))));
            ShaderNode amount = u(FOAM_AMOUNT, ShaderType.FLOAT);
            ShaderNode foamMask = saturate(smoothstep(f(1f).sub(amount.mul(0.5f)), f(1.02f), crest).mul(1.5f)
                    .add(streak.mul(amount).mul(crest)));
            rgb = mix(tint, u(FOAM, ShaderType.VEC3), foamMask);
            m.set(ShaderMaterial.Hook.ROUGHNESS, mix(u(ROUGHNESS, ShaderType.FLOAT), f(0.85f), foamMask));
        } else {
            m.set(ShaderMaterial.Hook.ROUGHNESS, u(ROUGHNESS, ShaderType.FLOAT));
        }
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(rgb, baseColor().w()));
        m.set(ShaderMaterial.Hook.METALLIC, f(0f));
        // light scattered through a crest toward a viewer looking into the sun
        ShaderNode back = pow(saturate(dot(viewDir().negate(), sunDir())), 4f);
        m.set(ShaderMaterial.Hook.EMISSIVE, u(SHALLOW, ShaderType.VEC3).mul(sunColor()).mul(back.mul(crest).mul(0.6f)));
        return m;
    }

    // One Gerstner wave evaluated at a point.
    private static final class Wave {
        final ShaderNode d;
        final ShaderNode k;
        final ShaderNode a;
        final ShaderNode q;
        final ShaderNode sin;
        final ShaderNode cos;

        Wave(float[] spec, ShaderNode dir, ShaderNode side, ShaderNode p) {
            float c = (float) Math.cos(spec[0]);
            float s = (float) Math.sin(spec[0]);
            d = normalize(dir.mul(c).add(side.mul(s)));
            ShaderNode len = uniform(WAVE_LENGTH, ShaderType.FLOAT).mul(spec[1]);
            k = f(6.2831853f).div(len);
            a = uniform(WAVE_HEIGHT, ShaderType.FLOAT).mul(spec[2] * 0.5f);
            // steepness shared between the waves so crests never loop over
            q = uniform(CHOPPINESS, ShaderType.FLOAT).mul(0.9f / 4f).div(max(k.mul(a), f(0.0001f)));
            ShaderNode omega = sqrt(k.mul(9.8f)).mul(uniform(WAVE_SPEED, ShaderType.FLOAT));
            ShaderNode phase = k.mul(dot(d, p)).sub(omega.mul(time()));
            sin = ShaderNode.sin(phase); // NOPMD UnnecessaryFullyQualifiedName - the field shadows the static import
            cos = ShaderNode.cos(phase); // NOPMD UnnecessaryFullyQualifiedName - the field shadows the static import
        }
    }
}
