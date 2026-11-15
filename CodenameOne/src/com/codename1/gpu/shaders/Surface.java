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

/// Man made and natural surfaces for `Material.Type.PBR`. Patterns are laid
/// out by `Shading.boxUv`, in world units, so they need no UVs and stay the
/// same size on every mesh; `SCALE` sets repeats per unit.
public final class Surface {
    /// `VEC3` the main colour: paint, wood, stone or tint.
    public static final String COLOR = "srfColor";
    /// `VEC3` the second colour: bare metal, rust, grain or flakes.
    public static final String COLOR2 = "srfColor2";
    /// `FLOAT` how much of the second layer shows, 0 to 1.
    public static final String AMOUNT = "srfAmount";
    /// `FLOAT` pattern repeats per world unit.
    public static final String SCALE = "srfScale";
    /// `FLOAT` opacity of glass and ice facing the camera.
    public static final String OPACITY = "srfOpacity";
    /// The most paint splats `paintSplats` keeps.
    public static final int MAX_SPLATS = 6;

    private Surface() {
    }

    private static ShaderMaterial base(int color, int color2, float amount, float scale) {
        return new ShaderMaterial().declareUniform(COLOR, ShaderType.VEC3).declareUniform(COLOR2, ShaderType.VEC3)
                .declareUniform(AMOUNT, ShaderType.FLOAT).declareUniform(SCALE, ShaderType.FLOAT)
                .setColorUniform(COLOR, color).setColorUniform(COLOR2, color2).setUniform(AMOUNT, amount)
                .setUniform(SCALE, scale);
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

    private static ShaderNode uvw() {
        return Shading.boxUv(1f).mul(uniform(SCALE, ShaderType.FLOAT));
    }

    /// Painted metal worn through to bare steel in patches: containers,
    /// hulls, railings. `COLOR` is the paint, `COLOR2` the metal, `AMOUNT`
    /// the wear.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial paintedMetal() {
        ShaderMaterial m = base(0x2f6f8f, 0x9a9c9e, 0.35f, 1.5f);
        ShaderNode n = Noise.fbm(uvw(), 4);
        ShaderNode wear = smoothstep(f(1f).sub(amount().mul(0.6f)), f(1.05f).sub(amount().mul(0.6f)), n);
        ShaderNode streak = Noise.value(vec(uvw().x().mul(6f), uvw().y().mul(0.4f))).mul(0.12f).add(0.94f);
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(mix(c1().mul(streak), c2(), wear), f(1f)));
        m.set(ShaderMaterial.Hook.METALLIC, wear);
        m.set(ShaderMaterial.Hook.ROUGHNESS, mix(f(0.55f), f(0.3f), wear));
        return m;
    }

    /// Rust eating into metal: orange-brown blotches, rough where rusted.
    /// `COLOR` is the metal, `COLOR2` the rust, `AMOUNT` how far it has
    /// spread.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial rust() {
        ShaderMaterial m = base(0x8c8e90, 0x7a3a16, 0.5f, 1.2f);
        ShaderNode n = Noise.fbm(uvw(), 5);
        ShaderNode r = smoothstep(f(0.75f).sub(amount().mul(0.5f)), f(0.9f).sub(amount().mul(0.5f)), n);
        ShaderNode tone = Noise.value(uvw().mul(9f)).mul(0.35f).add(0.8f);
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(mix(c1(), c2().mul(tone), r), f(1f)));
        m.set(ShaderMaterial.Hook.METALLIC, f(1f).sub(r));
        m.set(ShaderMaterial.Hook.ROUGHNESS, mix(f(0.35f), f(0.95f), r));
        m.set(ShaderMaterial.Hook.NORMAL, perturbNormal(mix(vec3(0f, 0f, 1f), Noise.bumpNormal(uvw().mul(4f), 2, 0.6f), r)));
        return m;
    }

    /// Wood planks along the texture's u: grain lines, knots and a board
    /// pattern. Uses the mesh's UVs so the grain follows each plank.
    /// `COLOR` is the light wood, `COLOR2` the dark grain, `SCALE` boards per
    /// unit of v.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial wood() {
        ShaderMaterial m = base(0xa8784a, 0x5a3a20, 0.6f, 4f);
        ShaderNode p = uv().mul(uniform(SCALE, ShaderType.FLOAT));
        ShaderNode board = floor(p.y());
        ShaderNode jitter = Noise.hash(vec(board, f(3f)));
        ShaderNode grain = sin(p.y().mul(40f).add(Noise.fbm(vec(p.x().mul(1.5f).add(jitter.mul(9f)), p.y().mul(3f)), 3)
                .mul(9f))).mul(0.5f).add(0.5f);
        ShaderNode gap = smoothstep(0.02f, 0.05f, fract(p.y())).mul(smoothstep(0.98f, 0.95f, fract(p.y())));
        ShaderNode rgb = mix(c1(), c2(), grain.mul(amount())).mul(jitter.mul(0.25f).add(0.85f)).mul(gap.mul(0.6f).add(0.4f));
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(rgb, f(1f)));
        m.set(ShaderMaterial.Hook.ROUGHNESS, f(0.7f).add(grain.mul(0.15f)));
        m.set(ShaderMaterial.Hook.METALLIC, f(0f));
        return m;
    }

    /// Cast concrete: mottled grey, pores and darker stains low down.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial concrete() {
        ShaderMaterial m = base(0x9a978f, 0x5e5b55, 0.4f, 1f);
        ShaderNode mottle = Noise.fbm(uvw().mul(2f), 4);
        ShaderNode pores = smoothstep(0.08f, 0.02f, Noise.cellular(uvw().mul(25f)));
        ShaderNode stain = smoothstep(f(1.2f), f(0f), worldPos().y()).mul(amount());
        ShaderNode rgb = mix(c1().mul(mottle.mul(0.25f).add(0.85f)), c2(), saturate(stain.add(pores.mul(0.6f))));
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(rgb, f(1f)));
        m.set(ShaderMaterial.Hook.ROUGHNESS, f(0.92f));
        m.set(ShaderMaterial.Hook.METALLIC, f(0f));
        return m;
    }

    /// Clear glass: see through facing the camera, mirror-like at the edges.
    /// Use an alpha blended render state. `COLOR` tints it.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial glass() {
        ShaderMaterial m = base(0xd8ecf0, 0xffffff, 0f, 1f).declareUniform(OPACITY, ShaderType.FLOAT)
                .setUniform(OPACITY, 0.12f);
        ShaderNode fr = Shading.fresnel(4f);
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(c1(), mix(uniform(OPACITY, ShaderType.FLOAT), f(1f), fr)));
        m.set(ShaderMaterial.Hook.ROUGHNESS, f(0.05f));
        m.set(ShaderMaterial.Hook.METALLIC, f(0f));
        return m;
    }

    /// Ice: blue translucent depths, frosted cracks and a cold rim. Use an
    /// alpha blended render state for thin ice, opaque for blocks.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial ice() {
        ShaderMaterial m = base(0xbfe3f2, 0x3c86b4, 0.5f, 1.5f).declareUniform(OPACITY, ShaderType.FLOAT)
                .setUniform(OPACITY, 0.85f);
        ShaderNode cracks = smoothstep(0.06f, 0.0f, abs(Noise.cellular(uvw()).sub(0.45f)));
        ShaderNode depth = Noise.fbm(uvw().mul(0.7f), 3);
        ShaderNode rgb = mix(c2(), c1(), saturate(depth.add(cracks)));
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(rgb, uniform(OPACITY, ShaderType.FLOAT)));
        m.set(ShaderMaterial.Hook.ROUGHNESS, mix(f(0.08f), f(0.6f), cracks));
        m.set(ShaderMaterial.Hook.EMISSIVE, Shading.rim(c1().mul(amount()), 3f));
        return m;
    }

    /// Woven cloth: a fine cross weave and a soft sheen at grazing angles.
    /// Uses the mesh's UVs.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial fabric() {
        ShaderMaterial m = base(0x8a2f3a, 0xffffff, 0.25f, 80f);
        ShaderNode p = uv().mul(uniform(SCALE, ShaderType.FLOAT));
        ShaderNode weave = sin(p.x().mul((float) Math.PI)).mul(sin(p.y().mul((float) Math.PI)));
        ShaderNode rgb = c1().mul(weave.mul(0.15f).add(0.9f));
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(rgb, f(1f)));
        m.set(ShaderMaterial.Hook.ROUGHNESS, f(0.95f));
        m.set(ShaderMaterial.Hook.METALLIC, f(0f));
        m.set(ShaderMaterial.Hook.EMISSIVE, Shading.rim(c1().mul(amount()).add(c2().mul(amount().mul(0.2f))), 2f)
                .mul(sunColor()));
        return m;
    }

    /// Car paint: a glossy coat over a metallic base with sparkling flakes.
    /// `COLOR` is the paint, `COLOR2` the flake colour.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial carPaint() {
        ShaderMaterial m = base(0x9c1020, 0xffe0d0, 0.6f, 60f);
        ShaderNode cell = floor(worldPos().mul(uniform(SCALE, ShaderType.FLOAT)));
        ShaderNode flake = step(f(0.93f), Noise.hash(cell.xy().add(cell.z().mul(17f))));
        ShaderNode fr = Shading.fresnel(5f);
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(mix(c1(), c1().mul(0.4f), fr), f(1f)));
        m.set(ShaderMaterial.Hook.METALLIC, f(0.6f));
        m.set(ShaderMaterial.Hook.ROUGHNESS, f(0.18f));
        ShaderNode sparkle = pow(saturate(dot(reflect(viewDir().negate(), normalize(worldNormal())), sunDir())), 12f);
        m.set(ShaderMaterial.Hook.EMISSIVE, c2().mul(sunColor()).mul(flake.mul(sparkle).mul(amount()))
                .add(Shading.rim(vec3(0.25f, 0.28f, 0.32f), 4f)));
        return m;
    }

    /// Paint splats on any surface: up to `MAX_SPLATS` blobs with ragged
    /// edges and drips, glossier than what they cover. Place them with
    /// `setSplat`; the surface underneath keeps its own colour and texture.
    ///
    /// #### Returns
    ///
    /// a new shader material
    public static ShaderMaterial paintSplats() {
        ShaderMaterial m = new ShaderMaterial();
        for (int i = 0; i < MAX_SPLATS; i++) {
            m.declareUniform("splatP" + i, ShaderType.VEC4).declareUniform("splatC" + i, ShaderType.VEC3);
        }
        ShaderNode p = worldPos();
        ShaderNode rgb = baseColor().rgb();
        ShaderNode wet = f(0f);
        for (int i = 0; i < MAX_SPLATS; i++) {
            ShaderNode sp = uniform("splatP" + i, ShaderType.VEC4);
            ShaderNode d = p.sub(sp.swizzle("xyz"));
            ShaderNode r = max(sp.w(), f(0.0001f));
            // ragged rim: noise around the centre, plus a drip running down walls
            ShaderNode edge = Noise.value(vec(atan2(d.z(), d.x()).mul(2.5f), f(i * 7.3f))).mul(0.45f).add(0.75f);
            ShaderNode drip = smoothstep(f(0f), r.mul(-1.5f), d.y()).mul(smoothstep(r.mul(0.3f), f(0f), abs(d.x()).add(abs(d.z()).mul(0.2f))));
            ShaderNode cover = smoothstep(r.mul(edge), r.mul(edge).mul(0.85f), length(d)).add(drip);
            cover = saturate(cover).mul(step(f(0.0002f), sp.w()));
            rgb = mix(rgb, uniform("splatC" + i, ShaderType.VEC3), cover);
            wet = max(wet, cover);
        }
        m.set(ShaderMaterial.Hook.BASE_COLOR, vec(rgb, baseColor().w()));
        m.set(ShaderMaterial.Hook.ROUGHNESS, mix(f(0.8f), f(0.25f), wet));
        return m;
    }

    /// Places or clears a splat of a `paintSplats` material.
    ///
    /// #### Parameters
    ///
    /// - `splats`: a material from `paintSplats`
    ///
    /// - `index`: 0 to `MAX_SPLATS - 1`; reuse the oldest when full
    ///
    /// - `x`: world centre
    ///
    /// - `y`: world centre
    ///
    /// - `z`: world centre
    ///
    /// - `radius`: world radius, 0 to clear the slot
    ///
    /// - `rgb`: paint colour as `0xRRGGBB`
    public static void setSplat(ShaderMaterial splats, int index, float x, float y, float z, float radius, int rgb) {
        splats.setUniform("splatP" + index, x, y, z, radius);
        splats.setColorUniform("splatC" + index, rgb);
    }
}
