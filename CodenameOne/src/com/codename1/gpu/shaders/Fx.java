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

/// Effects: energy, magic, damage and sci-fi surfaces. Most glow through the
/// `EMISSIVE` hook, so they pair well with `PostProcess.setBloom`. Those that
/// fade out need an alpha blended or additive render state; each says so.
public final class Fx {
    /// `VEC3` the effect's main colour.
    public static final String COLOR = "fxColor";
    /// `VEC3` its second colour: an edge, a core, a hot spot.
    public static final String COLOR2 = "fxColor2";
    /// `FLOAT` progress or strength, 0 to 1: how dissolved, how charged.
    public static final String AMOUNT = "fxAmount";
    /// `FLOAT` pattern repeats per unit.
    public static final String SCALE = "fxScale";
    /// `FLOAT` animation speed multiplier.
    public static final String SPEED = "fxSpeed";
    /// `VEC4` a world point (xyz) and the time (w) it was hit, for ripples.
    public static final String HIT = "fxHit";

    private Fx() {
    }

    private static ShaderMaterial base(int color, int color2, float amount, float scale) {
        return new ShaderMaterial().declareUniform(COLOR, ShaderType.VEC3).declareUniform(COLOR2, ShaderType.VEC3)
                .declareUniform(AMOUNT, ShaderType.FLOAT).declareUniform(SCALE, ShaderType.FLOAT)
                .declareUniform(SPEED, ShaderType.FLOAT)
                .setColorUniform(COLOR, color).setColorUniform(COLOR2, color2).setUniform(AMOUNT, amount)
                .setUniform(SCALE, scale).setUniform(SPEED, 1f);
    }

    private static ShaderNode c1() {
        return uniform(COLOR, ShaderType.VEC3);
    }

    private static ShaderNode c2() {
        return uniform(COLOR2, ShaderType.VEC3);
    }

    private static ShaderNode amount() {
        return uniform(AMOUNT, ShaderType.FLOAT);
    }

    private static ShaderNode scale() {
        return uniform(SCALE, ShaderType.FLOAT);
    }

    private static ShaderNode t() {
        return time().mul(uniform(SPEED, ShaderType.FLOAT));
    }

    /// Burns the surface away: pixels vanish as `AMOUNT` rises from 0 to 1,
    /// with a glowing `COLOR` edge ahead of the hole. Opaque.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial dissolve() {
        ShaderMaterial m = base(0xff7a1a, 0xffe08a, 0f, 2f);
        ShaderNode n = Noise.fbm(Shading.boxUv(1f).mul(scale()), 4);
        ShaderNode cut = amount().mul(1.1f).sub(0.05f);
        m.set(ShaderMaterial.Hook.DISCARD, step(n, cut));
        ShaderNode edge = smoothstep(cut.add(0.08f), cut, n);
        m.set(ShaderMaterial.Hook.EMISSIVE, mix(c1(), c2(), edge).mul(edge.mul(3f)));
        return m;
    }

    /// A hologram: scan lines climbing the surface, a bright rim, flicker and
    /// see-through body. Alpha blended.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial hologram() {
        ShaderMaterial m = base(0x3fd8ff, 0xffffff, 0.5f, 30f);
        ShaderNode lines = smoothstep(0.3f, 0.5f, abs(fract(worldPos().y().mul(scale()).sub(t().mul(1.5f))).sub(0.5f)));
        ShaderNode flick = Noise.value(vec(t().mul(12f), f(0f))).mul(0.3f).add(0.8f);
        ShaderNode fr = Shading.fresnel(2f);
        ShaderNode glow = c1().mul(lines.mul(0.4f).add(fr.mul(1.5f)).add(0.2f)).mul(flick);
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(c1().mul(0.1f), saturate(amount().add(fr).mul(0.6f).add(lines.mul(0.2f)))));
        m.set(ShaderMaterial.Hook.EMISSIVE, glow);
        return m;
    }

    /// A force field bubble: a honeycomb that lights up at the silhouette and
    /// ripples out from the last hit (`HIT`). Additive or alpha blended.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial forceField() {
        ShaderMaterial m = base(0x5aa8ff, 0xffffff, 0.6f, 4f).declareUniform(HIT, ShaderType.VEC4)
                .setUniform(HIT, 0f, -1000f, 0f, -1000f);
        ShaderNode cells = Noise.cellular(Shading.boxUv(1f).mul(scale()));
        ShaderNode hex = smoothstep(0.85f, 1f, cells.mul(1.4f));
        ShaderNode fr = Shading.fresnel(3f);
        ShaderNode hit = uniform(HIT, ShaderType.VEC4);
        ShaderNode age = time().sub(hit.w());
        ShaderNode ring = smoothstep(0.4f, 0f, abs(distance(worldPos(), hit.swizzle("xyz")).sub(age.mul(4f))))
                .mul(saturate(f(1f).sub(age)));
        ShaderNode glow = fr.mul(amount()).add(hex.mul(0.25f)).add(ring.mul(1.5f));
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(c1().mul(0.2f), saturate(glow)));
        m.set(ShaderMaterial.Hook.EMISSIVE, mix(c1(), c2(), ring).mul(glow.mul(2f)));
        return m;
    }

    /// A beam or laser along the texture's u: a white hot core, a coloured
    /// sheath and energy streaming along it. Additive, on a quad or a thin
    /// cylinder.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial beam() {
        ShaderMaterial m = base(0xff3a6a, 0xffffff, 1f, 3f);
        ShaderNode across = abs(uv().y().sub(0.5f)).mul(2f);
        ShaderNode core = smoothstep(0.25f, 0f, across);
        ShaderNode sheath = smoothstep(1f, 0.1f, across);
        ShaderNode flow = Noise.fbm(vec(uv().x().mul(scale()).sub(t().mul(6f)), uv().y().mul(2f)), 3);
        ShaderNode rgb = c1().mul(sheath.mul(flow.add(0.4f))).add(c2().mul(core)).mul(amount().mul(3f));
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(f(0f), f(0f), f(0f), saturate(sheath.mul(amount()))));
        m.set(ShaderMaterial.Hook.EMISSIVE, rgb);
        return m;
    }

    /// Flames on a camera facing quad: v runs from the base (0) to the tips
    /// (1). Turbulence rises, the colour runs from `COLOR2` (hot) to `COLOR`
    /// (cool) to nothing. Additive.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial fire() {
        ShaderMaterial m = base(0xff4a10, 0xffe9a0, 1f, 3f);
        ShaderNode p = uv();
        ShaderNode n = Noise.fbm(vec(p.x().mul(scale()), p.y().mul(scale()).sub(t().mul(2.5f))), 4);
        ShaderNode width = f(1f).sub(abs(p.x().sub(0.5f)).mul(2f));
        ShaderNode shape = saturate(width.mul(1.4f).sub(p.y()).add(n.sub(0.5f).mul(0.9f)));
        ShaderNode heat = smoothstep(0f, 0.6f, shape);
        ShaderNode rgb = Shading.ramp(heat, vec3(0f, 0f, 0f), c1(), c2()).mul(amount().mul(2.5f));
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(f(0f), f(0f), f(0f), heat));
        m.set(ShaderMaterial.Hook.EMISSIVE, rgb);
        m.set(ShaderMaterial.Hook.DISCARD, step(heat, f(0.01f)));
        return m;
    }

    /// Smoke puffs on a camera facing quad: soft billowing grey that thins
    /// toward the edges and drifts up. Alpha blended.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial smoke() {
        ShaderMaterial m = base(0x6c6a66, 0xb0aca6, 0.7f, 2.5f);
        ShaderNode p = uv().sub(0.5f);
        ShaderNode n = Noise.fbm(p.mul(scale()).sub(vec2(0f, 1f).mul(t().mul(0.3f))), 4);
        ShaderNode a = smoothstep(0.5f, 0.1f, length(p)).mul(n.mul(1.4f).sub(0.2f)).mul(amount());
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(mix(c1(), c2(), n), saturate(a)));
        return m;
    }

    /// A swirling portal on a disc or quad centred on uv (0.5, 0.5): spiral
    /// arms pulled toward a bright rim and a dark centre. Opaque.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial portal() {
        ShaderMaterial m = base(0x8a3cff, 0x3cf0ff, 1f, 3f);
        ShaderNode p = uv().sub(0.5f).mul(2f);
        ShaderNode r = length(p);
        ShaderNode a = atan2(p.y(), p.x());
        ShaderNode swirl = Noise.fbm(vec(a.mul(scale()).add(r.mul(6f)).sub(t().mul(2f)), r.mul(4f).sub(t())), 3);
        ShaderNode ring = smoothstep(0.75f, 0.95f, r).mul(smoothstep(1f, 0.95f, r));
        ShaderNode rgb = mix(c1(), c2(), swirl).mul(swirl.mul(r).mul(1.6f).add(ring.mul(3f))).mul(amount());
        m.set(ShaderMaterial.Hook.DISCARD, step(f(1f), r));
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(f(0f), f(0f), f(0f), f(1f)));
        m.set(ShaderMaterial.Hook.EMISSIVE, rgb);
        return m;
    }

    /// X-ray: only the silhouette glows, the body is invisible. Additive;
    /// draw it with depth testing off to see it through walls.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial xray() {
        ShaderMaterial m = base(0x4cff9a, 0xffffff, 1f, 1f);
        ShaderNode fr = Shading.fresnel(2.5f);
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(f(0f), f(0f), f(0f), fr.mul(amount())));
        m.set(ShaderMaterial.Hook.EMISSIVE, c1().mul(fr.mul(amount()).mul(2f)));
        return m;
    }

    /// A damage flash and pulse: the surface keeps its look and glows
    /// `COLOR` by `AMOUNT`, throbbing. Drive `AMOUNT` from 1 down to 0 after a
    /// hit, or hold it for a power-up. Opaque.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial pulse() {
        ShaderMaterial m = base(0xff2a2a, 0xffffff, 0f, 1f);
        ShaderNode beat = sin(t().mul(12f)).mul(0.25f).add(0.75f);
        m.set(ShaderMaterial.Hook.EMISSIVE, c1().mul(amount().mul(beat))
                .add(Shading.rim(c2(), 3f).mul(amount())));
        return m;
    }
}
