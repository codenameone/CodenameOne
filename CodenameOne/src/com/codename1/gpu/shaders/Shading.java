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

import com.codename1.gpu.shader.ShaderNode;

import static com.codename1.gpu.shader.ShaderNode.*;

/// Small building blocks most surface shaders share: view dependent terms,
/// fades, world space texture coordinates and signed distance shapes.
/// Fragment stage unless a method says otherwise.
public final class Shading {
    private Shading() {
    }

    /// Schlick style edge factor: 0 facing the camera, 1 at grazing angles.
    ///
    /// #### Parameters
    ///
    /// - `power`: higher keeps it closer to the silhouette; 5 is physical
    ///
    /// #### Returns
    ///
    /// a `FLOAT`
    public static ShaderNode fresnel(float power) {
        return pow(f(1f).sub(saturate(dot(normalize(worldNormal()), viewDir()))), power);
    }

    /// A coloured glow along the silhouette.
    ///
    /// #### Parameters
    ///
    /// - `color`: a `VEC3`
    ///
    /// - `power`: how tight the rim is, 2 to 8
    ///
    /// #### Returns
    ///
    /// a `VEC3`, for the `EMISSIVE` hook
    public static ShaderNode rim(ShaderNode color, float power) {
        return color.mul(fresnel(power));
    }

    /// 1 below `from` and 0 above `to` along world height, smoothly between.
    /// Both stages.
    ///
    /// #### Parameters
    ///
    /// - `from`: the height where the fade starts
    ///
    /// - `to`: the height where it ends
    ///
    /// #### Returns
    ///
    /// a `FLOAT`
    public static ShaderNode heightFade(float from, float to) {
        return f(1f).sub(smoothstep(from, to, worldPos().y()));
    }

    /// 1 near the camera and 0 beyond `to`, for fading detail out with
    /// distance. Both stages.
    ///
    /// #### Parameters
    ///
    /// - `from`: the distance where the fade starts
    ///
    /// - `to`: the distance where it ends
    ///
    /// #### Returns
    ///
    /// a `FLOAT`
    public static ShaderNode distanceFade(float from, float to) {
        return f(1f).sub(smoothstep(from, to, distance(worldPos(), cameraPos())));
    }

    /// Texture coordinates from world position seen from above, so a pattern
    /// tiles across meshes without seams and ignores their UVs. Both stages.
    ///
    /// #### Parameters
    ///
    /// - `scale`: repeats per world unit
    ///
    /// #### Returns
    ///
    /// a `VEC2`
    public static ShaderNode worldUv(float scale) {
        return worldPos().xz().mul(scale);
    }

    /// Texture coordinates that follow the surface on every face: the world
    /// plane the normal faces most, so walls do not smear a pattern seen from
    /// above. A cheap one sample stand-in for triplanar blending.
    ///
    /// #### Parameters
    ///
    /// - `scale`: repeats per world unit
    ///
    /// #### Returns
    ///
    /// a `VEC2`
    public static ShaderNode boxUv(float scale) {
        ShaderNode n = abs(normalize(worldNormal()));
        ShaderNode p = worldPos().mul(scale);
        ShaderNode wall = select(step(n.x(), n.z()), p.swizzle("xy"), p.swizzle("zy"));
        return select(step(max(n.x(), n.z()), n.y()), p.xz(), wall);
    }

    /// A texture coordinate that scrolls with time. Both stages.
    ///
    /// #### Parameters
    ///
    /// - `uv`: a `VEC2`
    ///
    /// - `u`: units per second along u
    ///
    /// - `v`: units per second along v
    ///
    /// #### Returns
    ///
    /// a `VEC2`
    public static ShaderNode scroll(ShaderNode uv, float u, float v) {
        return uv.add(vec2(u, v).mul(time()));
    }

    /// The signed distance to a circle: negative inside.
    ///
    /// #### Parameters
    ///
    /// - `p`: a `VEC2` point
    ///
    /// - `center`: a `VEC2`
    ///
    /// - `radius`: the radius
    ///
    /// #### Returns
    ///
    /// a `FLOAT`
    public static ShaderNode sdCircle(ShaderNode p, ShaderNode center, float radius) {
        return length(p.sub(center)).sub(radius);
    }

    /// The signed distance to an axis aligned box: negative inside.
    ///
    /// #### Parameters
    ///
    /// - `p`: a `VEC2` point
    ///
    /// - `center`: a `VEC2`
    ///
    /// - `halfSize`: a `VEC2` of half the width and height
    ///
    /// #### Returns
    ///
    /// a `FLOAT`
    public static ShaderNode sdBox(ShaderNode p, ShaderNode center, ShaderNode halfSize) {
        ShaderNode d = abs(p.sub(center)).sub(halfSize);
        return length(max(d, f(0f))).add(min(max(d.x(), d.y()), f(0f)));
    }

    /// The distance to a line segment.
    ///
    /// #### Parameters
    ///
    /// - `p`: a `VEC2` point
    ///
    /// - `a`: a `VEC2` end
    ///
    /// - `b`: the other `VEC2` end
    ///
    /// #### Returns
    ///
    /// a `FLOAT`, never negative
    public static ShaderNode sdSegment(ShaderNode p, ShaderNode a, ShaderNode b) {
        ShaderNode pa = p.sub(a);
        ShaderNode ba = b.sub(a);
        ShaderNode h = clamp(dot(pa, ba).div(dot(ba, ba)), 0f, 1f);
        return length(pa.sub(ba.mul(h)));
    }

    /// Turns a signed distance into coverage with a soft edge: 1 inside, 0
    /// outside.
    ///
    /// #### Parameters
    ///
    /// - `distance`: a signed distance
    ///
    /// - `softness`: the width of the edge
    ///
    /// #### Returns
    ///
    /// a `FLOAT`
    public static ShaderNode fill(ShaderNode distance, float softness) {
        return f(1f).sub(smoothstep(-softness, softness, distance));
    }

    /// A band of `width` around the zero line of a signed distance: outlines,
    /// rings, foam lines along a shore.
    ///
    /// #### Parameters
    ///
    /// - `distance`: a signed distance
    ///
    /// - `width`: the band width
    ///
    /// - `softness`: the width of its edges
    ///
    /// #### Returns
    ///
    /// a `FLOAT`
    public static ShaderNode stroke(ShaderNode distance, float width, float softness) {
        return fill(abs(distance).sub(width * 0.5f), softness);
    }

    /// A smooth three colour ramp: `a` at 0, `b` at 0.5, `c` at 1.
    ///
    /// #### Parameters
    ///
    /// - `t`: a `FLOAT`
    ///
    /// - `a`: a `VEC3`
    ///
    /// - `b`: a `VEC3`
    ///
    /// - `c`: a `VEC3`
    ///
    /// #### Returns
    ///
    /// a `VEC3`
    public static ShaderNode ramp(ShaderNode t, ShaderNode a, ShaderNode b, ShaderNode c) {
        ShaderNode s = saturate(t);
        return mix(mix(a, b, saturate(s.mul(2f))), c, saturate(s.mul(2f).sub(1f)));
    }

    /// Opaque `VEC4` from a `VEC3` colour.
    ///
    /// #### Parameters
    ///
    /// - `rgb`: a `VEC3`
    ///
    /// #### Returns
    ///
    /// a `VEC4` with alpha 1
    public static ShaderNode opaque(ShaderNode rgb) {
        return vec(rgb, f(1f));
    }
}
