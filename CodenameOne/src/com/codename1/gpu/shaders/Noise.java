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

/// Procedural noise built from `ShaderNode` math, so it runs in either stage
/// on every backend and needs no texture. Every function takes a `VEC2`
/// coordinate; scale it to set the feature size.
///
/// The hashes avoid `sin`, whose precision differs between GPUs, and use
/// only additions, multiplications and `fract`.
public final class Noise {
    private Noise() {
    }

    /// A pseudo random `FLOAT` in [0, 1) for each point, constant over no
    /// region: feed it `floor` of a coordinate for one value per cell.
    ///
    /// #### Parameters
    ///
    /// - `p`: a `VEC2`
    ///
    /// #### Returns
    ///
    /// a `FLOAT`
    public static ShaderNode hash(ShaderNode p) {
        ShaderNode p3 = fract(p.swizzle("xyx").mul(0.1031f));
        p3 = p3.add(dot(p3, p3.swizzle("yzx").add(33.33f)));
        return fract(p3.x().add(p3.y()).mul(p3.z()));
    }

    /// Two independent pseudo random values in [0, 1).
    ///
    /// #### Parameters
    ///
    /// - `p`: a `VEC2`
    ///
    /// #### Returns
    ///
    /// a `VEC2`
    public static ShaderNode hash2(ShaderNode p) {
        ShaderNode p3 = fract(p.swizzle("xyx").mul(vec3(0.1031f, 0.1030f, 0.0973f)));
        p3 = p3.add(dot(p3, p3.swizzle("yzx").add(33.33f)));
        return fract(p3.swizzle("xx").add(p3.swizzle("yz")).mul(p3.swizzle("zy")));
    }

    /// Smooth value noise in [0, 1], one feature per unit.
    ///
    /// #### Parameters
    ///
    /// - `p`: a `VEC2`
    ///
    /// #### Returns
    ///
    /// a `FLOAT`
    public static ShaderNode value(ShaderNode p) {
        ShaderNode i = floor(p);
        ShaderNode fr = fract(p);
        ShaderNode u = fr.mul(fr).mul(f(3f).sub(fr.mul(2f)));
        ShaderNode a = hash(i);
        ShaderNode b = hash(i.add(vec2(1f, 0f)));
        ShaderNode c = hash(i.add(vec2(0f, 1f)));
        ShaderNode d = hash(i.add(vec2(1f, 1f)));
        return mix(mix(a, b, u.x()), mix(c, d, u.x()), u.y());
    }

    /// Fractal value noise: `octaves` layers, each twice the frequency and
    /// half the weight of the one before. Clouds, terrain, dirt.
    ///
    /// #### Parameters
    ///
    /// - `p`: a `VEC2`
    ///
    /// - `octaves`: 1 to 6
    ///
    /// #### Returns
    ///
    /// a `FLOAT` in about [0, 1]
    public static ShaderNode fbm(ShaderNode p, int octaves) {
        int n = Math.max(1, Math.min(6, octaves));
        ShaderNode sum = null;
        float amp = 0.5f;
        float total = 0f;
        ShaderNode q = p;
        for (int o = 0; o < n; o++) {
            ShaderNode layer = value(q).mul(amp);
            sum = sum == null ? layer : sum.add(layer);
            total += amp;
            // rotate and shift each octave so the grids do not line up
            q = vec(q.x().mul(1.6f).add(q.y().mul(1.2f)), q.y().mul(1.6f).sub(q.x().mul(1.2f))).add(vec2(5.2f, 1.3f));
            amp *= 0.5f;
        }
        return sum.div(total);
    }

    /// Ridged fractal noise: sharp creases where plain noise crosses its
    /// middle. Mountain ridges, lightning, cracks.
    ///
    /// #### Parameters
    ///
    /// - `p`: a `VEC2`
    ///
    /// - `octaves`: 1 to 6
    ///
    /// #### Returns
    ///
    /// a `FLOAT` in [0, 1]
    public static ShaderNode ridged(ShaderNode p, int octaves) {
        ShaderNode r = f(1f).sub(abs(fbm(p, octaves).mul(2f).sub(1f)));
        return r.mul(r);
    }

    /// Cellular (Worley) noise: the distance to the nearest of one random
    /// point per cell. Foam, caustics, scales, cobbles, lava crust.
    ///
    /// #### Parameters
    ///
    /// - `p`: a `VEC2`
    ///
    /// #### Returns
    ///
    /// a `FLOAT`, 0 at a feature point and about 1 at most
    public static ShaderNode cellular(ShaderNode p) {
        return cellular(p, null);
    }

    /// Cellular noise whose feature points drift in circles over time, for
    /// moving water light and bubbling surfaces.
    ///
    /// #### Parameters
    ///
    /// - `p`: a `VEC2`
    ///
    /// - `phase`: a `FLOAT` that animates the points, such as `time()`, or
    ///   null for still points
    ///
    /// #### Returns
    ///
    /// a `FLOAT`
    public static ShaderNode cellular(ShaderNode p, ShaderNode phase) {
        ShaderNode i = floor(p);
        ShaderNode fr = fract(p);
        ShaderNode best = null;
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                ShaderNode o = vec2(x, y);
                ShaderNode pt = hash2(i.add(o));
                if (phase != null) {
                    pt = sin(pt.mul((float) (2 * Math.PI)).add(phase)).mul(0.5f).add(0.5f);
                }
                ShaderNode d = length(o.add(pt).sub(fr));
                best = best == null ? d : min(best, d);
            }
        }
        return best;
    }

    /// The slope of fractal noise, as a tangent space normal for
    /// `ShaderNode.perturbNormal`: bumps without a normal map.
    ///
    /// #### Parameters
    ///
    /// - `p`: a `VEC2`
    ///
    /// - `octaves`: 1 to 6
    ///
    /// - `strength`: how steep the bumps are
    ///
    /// #### Returns
    ///
    /// a `VEC3`
    public static ShaderNode bumpNormal(ShaderNode p, int octaves, float strength) {
        float e = 0.05f;
        ShaderNode h = fbm(p, octaves);
        ShaderNode hx = fbm(p.add(vec2(e, 0f)), octaves);
        ShaderNode hy = fbm(p.add(vec2(0f, e)), octaves);
        return vec(h.sub(hx).mul(strength / e), h.sub(hy).mul(strength / e), f(1f));
    }
}
