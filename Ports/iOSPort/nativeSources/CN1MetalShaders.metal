/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
 *
 * Metal Shading Language shaders for the Codename One iOS Metal backend.
 * Compiled by Xcode into default.metallib at build time when -Dios.metal=true
 * is set (which adds these files to the Xcode project).
 *
 * Pipelines (see CN1Metalcompat.h / CN1MetalPipelineCache.m):
 *   SolidColor       — FillRect, DrawLine, FillPolygon (no texture, solid fill)
 *   TexturedRGBA     — DrawImage, TileImage (sample RGBA texture, tint by color)
 *   AlphaMask        — DrawString glyphs, DrawTextureAlphaMask (sample alpha from
 *                      R8/alpha-only texture, colorize with color uniform)
 *   ClearPunch       — ClearRect: write zeros with blend disabled (handled at
 *                      pipeline-state level, shader just outputs zero)
 *   LinearGradient   — (Phase 2)
 *   RadialGradient   — (Phase 2)
 *
 * Buffer layout (matches CN1Metalcompat.m drawQuad):
 *   Vertex:
 *     buffer(0) : float2 positions[4]         (quad corners in 2D)
 *     buffer(1) : CN1MetalMatrices            (projection, modelView, transform)
 *     buffer(2) : float2 texcoords[4]         (only textured/alpha-mask variants)
 *   Fragment:
 *     buffer(0) : float4 color                (solid fill color or texture tint)
 *     texture(0): texture2d<float>            (only textured/alpha-mask variants)
 */
#include <metal_stdlib>
using namespace metal;

struct CN1Matrices {
    float4x4 projection;
    float4x4 modelView;
    float4x4 transform;
};

// --------- Vertex output structs ---------

struct VertexOutPlain {
    float4 position [[position]];
};

struct VertexOutTextured {
    float4 position [[position]];
    float2 texcoord;
};

// --------- Helper: apply the three-matrix pipeline to a 2D position ---------

static inline float4 applyMatrices(float2 p, constant CN1Matrices &m) {
    float4 pos4 = float4(p.x, p.y, 0.0, 1.0);
    return m.projection * m.modelView * m.transform * pos4;
}

// --------- SolidColor pipeline ---------

vertex VertexOutPlain cn1_vs_solid(
    uint vid [[vertex_id]],
    constant float2 *positions [[buffer(0)]],
    constant CN1Matrices &matrices [[buffer(1)]])
{
    VertexOutPlain out;
    out.position = applyMatrices(positions[vid], matrices);
    return out;
}

fragment float4 cn1_fs_solid(
    VertexOutPlain in [[stage_in]],
    constant float4 &color [[buffer(0)]])
{
    return color;
}

// --------- TexturedRGBA pipeline ---------

vertex VertexOutTextured cn1_vs_textured(
    uint vid [[vertex_id]],
    constant float2 *positions [[buffer(0)]],
    constant CN1Matrices &matrices [[buffer(1)]],
    constant float2 *texcoords [[buffer(2)]])
{
    VertexOutTextured out;
    out.position = applyMatrices(positions[vid], matrices);
    out.texcoord = texcoords[vid];
    return out;
}

fragment float4 cn1_fs_textured(
    VertexOutTextured in [[stage_in]],
    constant float4 &tint [[buffer(0)]],
    texture2d<float> tex [[texture(0)]])
{
    constexpr sampler s(mag_filter::linear, min_filter::linear, address::clamp_to_edge);
    // Sample, multiply by tint (which is a uniform alpha modulator for DrawImage).
    // The GL path uses the same formula: gl_FragColor = texture2D(tex, coord) * uColor.
    return tex.sample(s, in.texcoord) * tint;
}

// --------- TexturedRounded pipeline ---------
// The textured draw with the quad's corners rounded analytically, so a picture
// can be drawn with rounded corners without anyone building a rounded COPY of
// the bitmap first.
//
// params.xy is the destination size in pixels and params.z the corner radius.
// Coverage comes from a rounded-rectangle signed distance field and is a smooth
// ramp across the last pixel, so the edge is ANTIALIASED. That is the whole
// point of doing it here: a stencil clip of the same shape has a hard edge, and
// against a reference that anti-aliases its corners a hard edge measures WORSE
// than rounding the bitmap did -- which is why the copy survived the first
// attempt to remove it.
fragment float4 cn1_fs_textured_rounded(
    VertexOutTextured in [[stage_in]],
    constant float4 &tint [[buffer(0)]],
    constant float4 &params [[buffer(1)]],
    texture2d<float> tex [[texture(0)]])
{
    constexpr sampler s(mag_filter::linear, min_filter::linear, address::clamp_to_edge);
    float w = params.x;
    float h = params.y;
    float hw = w * 0.5;
    float hh = h * 0.5;
    float r = min(params.z, min(hw, hh));
    if (r < 0.0) {
        r = 0.0;
    }
    // Position relative to the quad's centre, in pixels.
    float px = in.texcoord.x * w - hw;
    float py = in.texcoord.y * h - hh;
    // Distance to the rounded rectangle: positive inside, and the magnitude in
    // the last pixel is the coverage.
    float dxe = abs(px) - (hw - r);
    float dye = abs(py) - (hh - r);
    float ax = max(dxe, 0.0);
    float ay = max(dye, 0.0);
    float outside = sqrt(ax * ax + ay * ay);
    float inside = min(max(dxe, dye), 0.0);
    // The 0.5 is the pixel centre. The distance is measured from the centre of
    // the fragment's cell, so the outermost cell INSIDE a straight edge has its
    // centre half a pixel in and a bare clamp(-sd) scores it 0.5 -- a uniformly
    // translucent one-pixel frame around all four sides, not just the corner
    // arcs. Offsetting by half a pixel puts full coverage on a cell that lies
    // entirely inside and 0.5 on one the boundary bisects, which is what the
    // corner antialiasing wanted in the first place.
    float coverage = clamp(0.5 - (outside + inside - r), 0.0, 1.0);
    if (coverage <= 0.0) {
        return float4(0.0);
    }
    // Coverage scales every channel: the texture pipeline works in
    // premultiplied terms, exactly as the tint modulator above it does.
    return tex.sample(s, in.texcoord) * tint * coverage;
}

// --------- AlphaMask pipeline (Phase 2/4) ---------
// Samples alpha from an R8/alpha-only texture and colorizes with the uniform.
// Used for DrawString glyph atlas in Phase 4.

fragment float4 cn1_fs_alpha_mask(
    VertexOutTextured in [[stage_in]],
    constant float4 &color [[buffer(0)]],
    texture2d<float> tex [[texture(0)]])
{
    constexpr sampler s(mag_filter::linear, min_filter::linear, address::clamp_to_edge);
    float a = tex.sample(s, in.texcoord).r;
    return float4(color.rgb * a, color.a * a);
}

// --------- ClearPunch pipeline ---------
// Writes a transparent pixel with blending disabled — punches holes in the
// existing framebuffer content. Matches the GL path's ClearRect semantics.

fragment float4 cn1_fs_clear(
    VertexOutPlain in [[stage_in]])
{
    return float4(0.0, 0.0, 0.0, 0.0);
}

// --------- AlphaMaskRadial pipeline (Phase 5+) ---------
// Same vertex stage as AlphaMask (cn1_vs_textured); the fragment computes a
// radial gradient analytically and multiplies by the alpha mask sampled from
// texture(0). Mirrors the GL DrawTextureAlphaMask radial-gradient shader at
// DrawTextureAlphaMask.m:181-360 — gradient colours interpolate from
// `startColor` at the centre to `endColor` at radius 1, computed in
// texcoord-space using elliptical normalisation (radiusX, radiusY can differ
// to support gradients with non-square bounds).
//
// Buffer layout for this pipeline:
//   buffer(0): float4 startColor — colour at gradient centre
//   buffer(1): float4 endColor   — colour at gradient edge
//   buffer(2): float4 params     — (centerX, centerY, radiusX, radiusY) all in
//                                  texcoord-space (0..1)

fragment float4 cn1_fs_alpha_mask_radial(
    VertexOutTextured in [[stage_in]],
    constant float4 &startColor [[buffer(0)]],
    constant float4 &endColor   [[buffer(1)]],
    constant float4 &params     [[buffer(2)]],
    texture2d<float> tex [[texture(0)]])
{
    constexpr sampler s(mag_filter::linear, min_filter::linear, address::clamp_to_edge);
    float a = tex.sample(s, in.texcoord).r;
    float2 center = params.xy;
    float2 radii  = params.zw;
    // Elliptical distance: scale (texcoord - centre) by radii so that t=1
    // sits on the ellipse boundary. clamp(0,1) keeps colours in range when
    // the gradient bbox doesn't cover the whole alpha mask.
    float2 d = (in.texcoord - center) / max(radii, float2(1e-6, 1e-6));
    float t = clamp(length(d), 0.0, 1.0);
    float4 grad = mix(startColor, endColor, t);
    // Premultiplied output: rgb already includes alpha, multiply by mask.
    return float4(grad.rgb * a, grad.a * a);
}

// --------- LinearGradient pipeline ---------
// Pure GPU horizontal/vertical gradient -- no CGContextDrawLinearGradient,
// no offscreen bitmap upload. The vertex stage feeds the quad's per-corner
// 0..1 texcoord into the fragment, and the fragment lerps between
// startColor and endColor along whichever axis params.x picks.
//
// Buffer layout:
//   buffer(0): float4 startColor       (premultiplied, alpha in .a)
//   buffer(1): float4 endColor         (premultiplied)
//   buffer(2): float4 axis             (axis.x = 1.0 horizontal, 0.0 vertical;
//                                       remaining components reserved)

fragment float4 cn1_fs_linear_gradient(
    VertexOutTextured in [[stage_in]],
    constant float4 &startColor [[buffer(0)]],
    constant float4 &endColor   [[buffer(1)]],
    constant float4 &axis       [[buffer(2)]])
{
    float t = clamp(mix(in.texcoord.y, in.texcoord.x, axis.x), 0.0, 1.0);
    float4 grad = mix(startColor, endColor, t);
    return grad;
}

// --------- RadialGradient pipeline ---------
// Pure GPU radial gradient -- no CGContextDrawRadialGradient, no offscreen
// bitmap. Texcoord 0..1 across the quad; params carries the centre and
// radius (also in 0..1 texcoord space) so the same shader handles whatever
// rectangular bounds the caller specifies.
//
// Buffer layout:
//   buffer(0): float4 startColor       (premultiplied, alpha in .a)
//   buffer(1): float4 endColor         (premultiplied)
//   buffer(2): float4 params           (.xy = centre in 0..1 texcoord-space,
//                                       .zw = radii (rx, ry) in 0..1 texcoord-space)

fragment float4 cn1_fs_radial_gradient(
    VertexOutTextured in [[stage_in]],
    constant float4 &startColor [[buffer(0)]],
    constant float4 &endColor   [[buffer(1)]],
    constant float4 &params     [[buffer(2)]])
{
    float2 center = params.xy;
    float2 radii  = max(params.zw, float2(1e-6, 1e-6));
    float t = clamp(length((in.texcoord - center) / radii), 0.0, 1.0);
    return mix(startColor, endColor, t);
}

// --------- MultiStopGradient pipeline ---------
//
// Single shader covering CSS linear / radial / conic gradients with up to
// CN1_GRAD_MAX_STOPS stops, premultiplied stop colours, and cycle modes
// NONE / REPEAT / REFLECT. The Java/Obj-C side computes raw 0..1 stop
// positions and uploads them along with the geometry; the shader maps
// each fragment to a t value in 0..1 and samples the stop list.
//
// Header buffer layout (buffer(0)):
//   .x  = kind                (0=linear, 1=radial, 2=conic)
//   .y  = cycle method        (0=NONE, 1=REPEAT, 2=REFLECT)
//   .z  = stop count          (>= 2, <= CN1_GRAD_MAX_STOPS)
//   .w  = radial shape        (0=circle, 1=ellipse) -- unused for linear/conic
//
// Geometry buffer (buffer(1)):
//   linear:   .x = sin(angle), .y = -cos(angle), .zw unused
//             (so CSS 0deg points up; t = 0.5 + dot(normalised, axis))
//   radial:   .xy = center (texcoord 0..1), .zw = (rx, ry) (texcoord 0..1)
//   conic:    .xy = center (texcoord 0..1), .z = fromAngleRadians, .w unused
//
// Stops are passed as two arrays at buffer(2)/buffer(3):
//   buffer(2): float4 positions[CN1_GRAD_MAX_STOPS / 4 packed as float4]
//   buffer(3): float4 colors[CN1_GRAD_MAX_STOPS] -- premultiplied RGBA
//
// We pack positions into float4s (4 stops per float4) so the constant
// argument footprint stays small and Metal can keep everything in
// register memory. 8 stops -> 2 float4s.

#define CN1_GRAD_MAX_STOPS 8
#define CN1_GRAD_POS_PACKED 2  // ceil(CN1_GRAD_MAX_STOPS / 4)

static inline float cn1_grad_position(constant float4 *packed, int idx) {
    int p = idx >> 2;
    int s = idx & 3;
    float4 v = packed[p];
    return (s == 0) ? v.x : ((s == 1) ? v.y : ((s == 2) ? v.z : v.w));
}

// Apply cycle method to t. REPEAT and REFLECT wrap across
// [positions[0], positions[last]] (matching CSS repeating-*-gradient
// semantics: the repeat period is the stop-list span, not [0, 1]).
static inline float cn1_grad_cycle(float t, int cycle, float p0, float pN) {
    float period = pN - p0;
    if (cycle == 1 && period > 1e-6) {
        float rel = (t - p0) / period;
        rel = rel - floor(rel);
        return p0 + rel * period;
    }
    if (cycle == 2 && period > 1e-6) {
        float rel = fabs((t - p0) / period);
        float intp = floor(rel);
        float frac = rel - intp;
        // Reflect on odd periods so the stops mirror across each tile boundary.
        if (((int)intp & 1) != 0) {
            frac = 1.0 - frac;
        }
        return p0 + frac * period;
    }
    return clamp(t, p0, pN);
}

static inline float4 cn1_grad_sample_stops(float t, int stopCount,
                                           constant float4 *positions,
                                           constant float4 *colors) {
    // Linear walk -- N <= 8 so a manual loop costs less than a sorted
    // search and avoids dynamic indexing into the color array (Metal
    // constant indexing is fastest with constant integers).
    float prevP = cn1_grad_position(positions, 0);
    float4 prevC = colors[0];
    if (t <= prevP) return prevC;
    for (int i = 1; i < stopCount; i++) {
        float curP = cn1_grad_position(positions, i);
        float4 curC = colors[i];
        if (t <= curP) {
            float span = curP - prevP;
            float local = (span <= 1e-6) ? 0.0 : (t - prevP) / span;
            return mix(prevC, curC, local);
        }
        prevP = curP;
        prevC = curC;
    }
    return prevC;
}

fragment float4 cn1_fs_multistop_gradient(
    VertexOutTextured in [[stage_in]],
    constant float4 &header    [[buffer(0)]],
    constant float4 &geom      [[buffer(1)]],
    constant float4 *positions [[buffer(2)]],
    constant float4 *colors    [[buffer(3)]])
{
    int kind = (int)header.x;
    int cycle = (int)header.y;
    int stopCount = (int)header.z;
    if (stopCount < 2) {
        return colors[0];
    }

    float t;
    if (kind == 0) {
        float2 axis = geom.xy;
        float2 p = in.texcoord - float2(0.5, 0.5);
        // axis encodes (sin(angle), -cos(angle)); dot(p, axis) gives signed
        // distance along the gradient line from the rect centre. Max distance
        // for a [-0.5, 0.5]^2 rect is |sin|*0.5 + |cos|*0.5, which we use to
        // normalise back into [0, 1].
        float halfLen = abs(axis.x) * 0.5 + abs(axis.y) * 0.5;
        t = 0.5 + dot(p, axis) / max(2.0 * halfLen, 1e-6);
    } else if (kind == 1) {
        float2 center = geom.xy;
        float2 radii = max(geom.zw, float2(1e-6, 1e-6));
        t = length((in.texcoord - center) / radii);
    } else {
        float2 center = geom.xy;
        float fromAngle = geom.z;
        float2 d = in.texcoord - center;
        // CSS conic: 0deg points up (north), sweep clockwise.
        float theta = atan2(d.x, -d.y) - fromAngle;
        t = theta / (2.0 * M_PI_F);
        t = t - floor(t);
    }

    float p0 = cn1_grad_position(positions, 0);
    float pN = cn1_grad_position(positions, stopCount - 1);
    if (kind != 2) {
        t = cn1_grad_cycle(t, cycle, p0, pN);
    }
    return cn1_grad_sample_stops(t, stopCount, positions, colors);
}

// --------- iOS-26 selection-DROP LENS pipeline ---------
// GPU equivalent of METALView.m glassApplyLens / JavaSEPort.applyLensBuffer. Samples a
// blitted copy of the bar region (texture(0), fw x fh px) and warps + tints + lights it
// per pixel, fully on the GPU so the morph runs at frame rate (no CPU readback). texcoord
// 0..1 over the drop quad. Outputs PREMULTIPLIED (premultiplied-alpha blend over the bar).
// Constants MUST stay in sync with glassApplyLens.
constant float LENS_MAG_FLAT      = 0.75;
constant float LENS_TINT_HI       = 150.0;
constant float LENS_TINT_LO       = 55.0;
constant float LENS_LIFT_COEF     = 0.40;
constant float LENS_GLARE         = 0.09;
constant float LENS_RIM           = 0.06;
constant float LENS_RIM_W         = 0.06;
constant float LENS_REFRACT       = 0.16;
constant float LENS_EDGE_SHADOW   = 0.12;
constant float LENS_RIM_SCALE     = 0.84;
constant float3 LENS_GLASS_TINT   = float3(188.0, 216.0, 255.0); // 0xbcd8ff, 0..255
constant float LENS_GLASS_TINT_STR = 0.10;
constant float LENS_SAT_BOOST     = 1.32;

static inline float cn1_lens_smoothstep(float a, float b, float x) {
    float t = clamp((x - a) / (b - a), 0.0, 1.0);
    return t * t * (3.0 - 2.0 * t);
}

fragment float4 cn1_fs_lens(
    VertexOutTextured in [[stage_in]],
    constant float4 &p0 [[buffer(0)]],   // fw, fh, magnify, aberration
    constant float4 &p1 [[buffer(1)]],   // tintR, tintG, tintB (0..1), tintStrength
    constant float4 &p2 [[buffer(2)]],   // cornerRadiusPx (neg = capsule), unused...
    texture2d<float> src [[texture(0)]])
{
    constexpr sampler smp(mag_filter::linear, min_filter::linear, address::clamp_to_edge);
    float fw = p0.x, fh = p0.y, magnify = p0.z, aberration = p0.w;
    float3 tintc = p1.xyz * 255.0;
    float tintStrength = p1.w;
    float cornerRadius = p2.x;

    float hw = fw * 0.5, hh = fh * 0.5;
    float r = (cornerRadius < 0.0) ? min(hw, hh) : min(cornerRadius, min(hw, hh));
    if (r < 0.0) r = 0.0;
    float px = in.texcoord.x * fw - hw;
    float py = in.texcoord.y * fh - hh;

    // superellipse SDF -> depth (>0 inside)
    float dxe = abs(px) - (hw - r);
    float dye = abs(py) - (hh - r);
    float ax = max(dxe, 0.0), ay = max(dye, 0.0);
    float outside = sqrt(ax * ax + ay * ay);
    float inside = min(max(dxe, dye), 0.0);
    float depth = -(outside + inside - r);
    if (depth <= 0.0) { return float4(0.0); }
    float alpha = min(depth, 1.0);

    float rd = min(1.0, sqrt((px * px) / (hw * hw) + (py * py) / (hh * hh)));
    float liftMax = LENS_LIFT_COEF * (magnify - 1.0) * hh;
    float glassAmt = cn1_lens_smoothstep(1.085, 1.25, magnify);

    float edge = cn1_lens_smoothstep(LENS_MAG_FLAT, 1.0, rd);
    float rimScale = 1.0 + (LENS_RIM_SCALE - 1.0) * glassAmt;
    float mag = magnify + (rimScale - magnify) * edge;
    mag = max(mag, 0.2);
    float abr = aberration * edge;
    float magR = max(mag * (1.0 - abr), 0.05), magB = max(mag * (1.0 + abr), 0.05);
    float lift = liftMax * (1.0 - rd * rd);
    float refr = 1.0 + LENS_REFRACT * glassAmt * cn1_lens_smoothstep(0.70, 1.0, rd);

    float2 cR = float2((hw + (px / magR) * refr) / fw, (hh + (py / magR) * refr + lift) / fh);
    float2 cG = float2((hw + (px / mag)  * refr) / fw, (hh + (py / mag)  * refr + lift) / fh);
    float2 cB = float2((hw + (px / magB) * refr) / fw, (hh + (py / magB) * refr + lift) / fh);
    float sr = src.sample(smp, cR).r * 255.0;
    float sg = src.sample(smp, cG).g * 255.0;
    float sb = src.sample(smp, cB).b * 255.0;

    float lum = 0.2126 * sr + 0.7152 * sg + 0.0722 * sb;
    float t = tintStrength * cn1_lens_smoothstep(LENS_TINT_HI, LENS_TINT_LO, lum);
    float fr = sr + (tintc.r - sr) * t;
    float fg = sg + (tintc.g - sg) * t;
    float fb = sb + (tintc.b - sb) * t;

    float gt = LENS_GLASS_TINT_STR * glassAmt;
    fr += (LENS_GLASS_TINT.r - fr) * gt;
    fg += (LENS_GLASS_TINT.g - fg) * gt;
    fb += (LENS_GLASS_TINT.b - fb) * gt;

    float sl = 0.2126 * fr + 0.7152 * fg + 0.0722 * fb;
    fr = sl + (fr - sl) * LENS_SAT_BOOST;
    fg = sl + (fg - sl) * LENS_SAT_BOOST;
    fb = sl + (fb - sl) * LENS_SAT_BOOST;

    float gx = px / hw, gy = (py + 0.42 * hh) / hh;
    float glare = LENS_GLARE * glassAmt * exp(-(gx * gx * 1.15 + gy * gy * 2.6) * 2.1);
    float rimW = max(2.0, LENS_RIM_W * hh);
    float rim = depth < rimW ? (1.0 - depth / rimW) * LENS_RIM : 0.0;
    float bright = glare + rim;
    fr += bright * (255.0 - fr);
    fg += bright * (255.0 - fg);
    fb += bright * (255.0 - fb);

    float esW = max(2.0, 0.13 * min(hw, hh));
    if (depth < esW) {
        float es = (1.0 - depth / esW) * LENS_EDGE_SHADOW * glassAmt;
        fr *= (1.0 - es); fg *= (1.0 - es); fb *= (1.0 - es);
    }
    fr = clamp(fr, 0.0, 255.0); fg = clamp(fg, 0.0, 255.0); fb = clamp(fb, 0.0, 255.0);
    return float4(fr / 255.0 * alpha, fg / 255.0 * alpha, fb / 255.0 * alpha, alpha);
}

// Graphics.colorMatrixRegion. Mirrors com.codename1.ui.plaf.ColorMatrixBlend (the
// reference the JavaSE port runs): v = clamp(M.p + off), k = amount * shape * mask
// alpha, result mix(p, v, k). Drawn premultiplied as (v*k, k) OVER p, which is the
// same mix and leaves the destination alpha alone.
fragment float4 cn1_fs_colormatrix(
    VertexOutTextured in [[stage_in]],
    constant float4 *rows [[buffer(0)]],  // 3 matrix rows (r,g,b,off), then (fw, fh, cornerRadiusPx, amount)
    constant float4 &flags [[buffer(1)]], // x: 1 = mask bound
    texture2d<float> src [[texture(0)]],
    texture2d<float> mask [[texture(1)]])
{
    constexpr sampler nearestSmp(mag_filter::nearest, min_filter::nearest, address::clamp_to_edge);
    // The mask may be stretched over the region (the pulsing tab bar), so it is
    // filtered; the source is the region itself, pixel for pixel.
    constexpr sampler maskSmp(mag_filter::linear, min_filter::linear, address::clamp_to_edge);
    float fw = rows[3].x, fh = rows[3].y, cornerRadius = rows[3].z, amount = rows[3].w;
    float hw = fw * 0.5, hh = fh * 0.5;
    float k = amount;
    if (cornerRadius != 0.0) {
        float r = (cornerRadius < 0.0) ? min(hw, hh) : min(cornerRadius, min(hw, hh));
        float px = in.texcoord.x * fw - hw;
        float py = in.texcoord.y * fh - hh;
        float dxe = abs(px) - (hw - r);
        float dye = abs(py) - (hh - r);
        float ax = max(dxe, 0.0), ay = max(dye, 0.0);
        float sdf = sqrt(ax * ax + ay * ay) + min(max(dxe, dye), 0.0) - r;
        k *= clamp(0.5 - sdf, 0.0, 1.0);
    }
    if (flags.x > 0.5) {
        k *= mask.sample(maskSmp, in.texcoord).a;
    }
    if (k <= 0.0) { return float4(0.0); }
    float3 p = src.sample(nearestSmp, in.texcoord).rgb;
    float3 v = float3(dot(rows[0].xyz, p) + rows[0].w,
                      dot(rows[1].xyz, p) + rows[1].w,
                      dot(rows[2].xyz, p) + rows[2].w);
    v = clamp(v, 0.0, 1.0);
    k = min(k, 1.0);
    return float4(v * k, k);
}

// ---- Liquid Glass on the GPU (CN1MetalGlassEncode / CN1MetalDrawGlass) ----
// A port of METALView.m's CPU glass (the material loop, glassGaussianBlur and
// glassApplyOptics) that keeps its integer rounding: every intermediate is an
// 8-bit value, truncated or rounded exactly where the C code does, so the GPU
// patch matches the CPU one.

struct VertexOutFullscreen {
    float4 position [[position]];
};

// One triangle covering the viewport; the fragment works from [[position]].
vertex VertexOutFullscreen cn1_vs_fullscreen(uint vid [[vertex_id]]) {
    float2 p = float2(float((vid << 1) & 2), float(vid & 2));
    VertexOutFullscreen o;
    o.position = float4(p.x * 2.0 - 1.0, 1.0 - p.y * 2.0, 0.0, 1.0);
    return o;
}

static inline int cn1_glass_byte(float unit) {
    return int(rint(unit * 255.0));
}

// params: x,y = the padded buffer's origin inside `src`; z,w = src size.
// material: sat, scale, offset, curve; material2.x = curveMid.
fragment float4 cn1_fs_glass_material(
    VertexOutFullscreen in [[stage_in]],
    constant int4 &params [[buffer(0)]],
    constant float4 &material [[buffer(1)]],
    constant float4 &material2 [[buffer(2)]],
    texture2d<float, access::read> src [[texture(0)]])
{
    int bx = int(in.position.x);
    int by = int(in.position.y);
    int ax = clamp(bx + params.x, 0, params.z - 1);
    int ay = clamp(by + params.y, 0, params.w - 1);
    float4 c = src.read(uint2(ax, ay));
    float rch = float(cn1_glass_byte(c.r));
    float gch = float(cn1_glass_byte(c.g));
    float bch = float(cn1_glass_byte(c.b));
    float sat = material.x, scale = material.y, offset = material.z, curve = material.w;
    float lum = 0.2126 * rch + 0.7152 * gch + 0.0722 * bch;
    float rr = (lum + (rch - lum) * sat) * scale + offset;
    float gg = (lum + (gch - lum) * sat) * scale + offset;
    float bb = (lum + (bch - lum) * sat) * scale + offset;
    if (curve != 0.0) {
        float d = lum / 255.0 - material2.x;
        float k = curve * 255.0 * d * d;
        rr += k; gg += k; bb += k;
    }
    // (int) in C truncates toward zero; the clamp comes first.
    float ri = rr < 0.0 ? 0.0 : (rr > 255.0 ? 255.0 : trunc(rr));
    float gi = gg < 0.0 ? 0.0 : (gg > 255.0 ? 255.0 : trunc(gg));
    float bi = bb < 0.0 ? 0.0 : (bb > 255.0 ? 255.0 : trunc(bb));
    return float4(ri, gi, bi, 255.0) / 255.0;
}

// One direction of a box-blur iteration: the mean of 2r+1 edge-clamped
// samples, rounded like glassBoxBlurOnce. params: x = r, y = width, z = height.
static inline float4 cn1_glass_box(VertexOutFullscreen in, int4 params,
                                   texture2d<float, access::read> src, bool horizontal) {
    int x = int(in.position.x);
    int y = int(in.position.y);
    int r = params.x;
    int sr = 0, sg = 0, sb = 0;
    for (int k = -r; k <= r; k++) {
        int xx = horizontal ? clamp(x + k, 0, params.y - 1) : x;
        int yy = horizontal ? y : clamp(y + k, 0, params.z - 1);
        float4 c = src.read(uint2(xx, yy));
        sr += cn1_glass_byte(c.r);
        sg += cn1_glass_byte(c.g);
        sb += cn1_glass_byte(c.b);
    }
    float norm = 1.0 / float(2 * r + 1);
    return float4(float(int(float(sr) * norm + 0.5)), float(int(float(sg) * norm + 0.5)),
                  float(int(float(sb) * norm + 0.5)), 255.0) / 255.0;
}

fragment float4 cn1_fs_glass_box_h(VertexOutFullscreen in [[stage_in]], constant int4 &params [[buffer(0)]],
                                   texture2d<float, access::read> src [[texture(0)]]) {
    return cn1_glass_box(in, params, src, true);
}

fragment float4 cn1_fs_glass_box_v(VertexOutFullscreen in [[stage_in]], constant int4 &params [[buffer(0)]],
                                   texture2d<float, access::read> src [[texture(0)]]) {
    return cn1_glass_box(in, params, src, false);
}

static inline int cn1_glass_bilerp(int c00, int c10, int c01, int c11, float tx, float ty) {
    float top = float(c00) + float(c10 - c00) * tx;
    float bot = float(c01) + float(c11 - c01) * tx;
    return int(top + (bot - top) * ty + 0.5);
}

static inline int3 cn1_glass_texel(texture2d<float, access::read> t, int x, int y) {
    float4 c = t.read(uint2(x, y));
    return int3(cn1_glass_byte(c.r), cn1_glass_byte(c.g), cn1_glass_byte(c.b));
}

static inline int cn1_glass_outline_channel(int c, int b, float alpha, float w) {
    float v = float(c) * alpha;
    float under = float(b) * (1.0 - alpha);
    v = v + under;
    float dark = 76.0 * w;
    float alt = 0.78 * w * float(b);
    if (alt < dark) { dark = alt; }
    v = v - dark;
    return v <= 0.0 ? 0 : (v >= 255.0 ? 255 : int(v + 0.5));
}

// The optics of glassApplyOptics, drawn as the patch itself.
// p0: fw, fh, cornerRadius (logical; < 0 = capsule), s
// p1: refract, specular, outline, unused
// p2 (int): pad, bw, bh, unused;  p3 (int): rawX, rawY, rawW, rawH
fragment float4 cn1_fs_glass_optics(
    VertexOutTextured in [[stage_in]],
    constant float4 &p0 [[buffer(0)]],
    constant float4 &p1 [[buffer(1)]],
    constant int4 &p2 [[buffer(2)]],
    constant int4 &p3 [[buffer(3)]],
    texture2d<float, access::read> blurred [[texture(0)]],
    texture2d<float, access::read> raw [[texture(1)]])
{
    float fw = p0.x, fh = p0.y, cornerRadius = p0.z, s = p0.w;
    float refract = p1.x, specular = p1.y, outline = p1.z;
    int pad = p2.x, bw = p2.y, bh = p2.z;
    int x = clamp(int(in.texcoord.x * fw), 0, int(fw) - 1);
    int y = clamp(int(in.texcoord.y * fh), 0, int(fh) - 1);
    int rw = int(fw), rh = int(fh);
    float hw = float(rw) / 2.0, hh = float(rh) / 2.0;
    float minhh = hw < hh ? hw : hh;
    float r;
    if (cornerRadius < 0.0) { r = minhh; } else { r = cornerRadius * s; if (r > minhh) r = minhh; }
    if (r < 0.0) r = 0.0;
    float band = minhh * 0.6;
    float rimW = 3.0 * s;
    float px = float(x) + 0.5, py = float(y) + 0.5;
    float dx = fabs(px - hw) - (hw - r);
    float dy = fabs(py - hh) - (hh - r);
    float axx = dx > 0.0 ? dx : 0.0, ayy = dy > 0.0 ? dy : 0.0;
    float outside = sqrt(axx * axx + ayy * ayy);
    float mxv = dx > dy ? dx : dy;
    float inside = mxv < 0.0 ? mxv : 0.0;
    float depth = -(outside + inside - r);
    if (depth <= 0.0) { return float4(0.0); }
    float alpha = depth >= 1.0 ? 1.0 : depth;
    if (cornerRadius == 0.0) {
        float fb = float(rh) * 0.22;
        if (fb > 1.0 && py > float(rh) - fb) {
            float fade = (float(rh) - py) / fb;
            if (fade < 0.0) fade = 0.0;
            alpha *= fade;
        }
    }
    float sx = float(x), sy = float(y);
    if (refract > 0.0 && band > 0.0 && depth < band) {
        float t = 1.0 - depth / band;
        float distortion = 1.0 - sqrt(max(0.0, 1.0 - t * t));
        sx = float(x) - (px - hw) * distortion * refract;
        sy = float(y) - (py - hh) * distortion * refract;
    }
    float fx = sx + float(pad), fy = sy + float(pad);
    fx = clamp(fx, 0.0, float(bw - 1));
    fy = clamp(fy, 0.0, float(bh - 1));
    int x0 = int(fx), y0 = int(fy);
    int x1 = x0 + 1 < bw ? x0 + 1 : x0, y1 = y0 + 1 < bh ? y0 + 1 : y0;
    float tx = fx - float(x0), ty = fy - float(y0);
    int3 c00 = cn1_glass_texel(blurred, x0, y0), c10 = cn1_glass_texel(blurred, x1, y0);
    int3 c01 = cn1_glass_texel(blurred, x0, y1), c11 = cn1_glass_texel(blurred, x1, y1);
    int rr = cn1_glass_bilerp(c00.x, c10.x, c01.x, c11.x, tx, ty);
    int gg = cn1_glass_bilerp(c00.y, c10.y, c01.y, c11.y, tx, ty);
    int bb = cn1_glass_bilerp(c00.z, c10.z, c01.z, c11.z, tx, ty);
    if (specular > 0.0 && depth < rimW) {
        float rim = 1.0 - depth / rimW;
        float topBias = 0.55 + 0.45 * (1.0 - py / float(rh));
        int add = int(specular * rim * topBias * 70.0);
        rr = min(rr + add, 255);
        gg = min(gg + add, 255);
        bb = min(bb + add, 255);
    }
    if (outline > 0.0 && depth < 1.0 && p3.z > 0) {
        float wx;
        if (dx > 0.0 && dy > 0.0) { wx = outside > 0.0 ? axx / outside : 0.0; }
        else { wx = dx >= dy ? 1.0 : 0.0; }
        float ow = outline * wx;
        if (ow > 0.0) {
            int3 bk = cn1_glass_texel(raw, clamp(x + p3.x, 0, p3.z - 1), clamp(y + p3.y, 0, p3.w - 1));
            rr = cn1_glass_outline_channel(rr, bk.x, alpha, ow);
            gg = cn1_glass_outline_channel(gg, bk.y, alpha, ow);
            bb = cn1_glass_outline_channel(bb, bk.z, alpha, ow);
            return float4(float(rr), float(gg), float(bb), 255.0) / 255.0;
        }
    }
    int a = int(alpha * 255.0);
    return float4(float(rr * a / 255), float(gg * a / 255), float(bb * a / 255), float(a)) / 255.0;
}

// Graphics.glassLensRegion -- a line-for-line port of
// com.codename1.ui.plaf.GlassLensBlend.apply (the reference the JavaSE port runs).
// geo: the quad in physical pixels; lens: the lens rect (physical);
// src: the blit's physical origin and size; misc.x: corner radius (< 0 capsule),
// misc.y: amount; o: the GlassLensBlend parameters (lengths in physical pixels).
static inline float cn1_glass_lens_disp(float s, float h, float mx) {
    if (s >= h || h <= 0.0) { return 0.0; }
    float t = (h - s) / h;
    float d = (h - s) / sqrt(max(1e-6, 1.0 - t * t));
    return min(d, mx);
}

fragment float4 cn1_fs_glass_lens(
    VertexOutTextured in [[stage_in]],
    constant float4 &geo [[buffer(0)]],
    constant float4 &lens [[buffer(1)]],
    constant float4 &srcr [[buffer(2)]],
    constant float4 &misc [[buffer(3)]],
    constant float *o [[buffer(4)]],
    texture2d<float> src [[texture(0)]])
{
    constexpr sampler lin(mag_filter::linear, min_filter::linear, address::clamp_to_edge);
    float2 P = geo.xy + in.texcoord * geo.zw;              // pixel centre, physical
    float2 size = float2(src.get_width(), src.get_height());
    float2 tp = P - srcr.xy;                               // inside the blit
    float4 base4 = src.sample(lin, tp / size);
    float3 base = base4.rgb;
    float amount = misc.y;
    float hw = lens.z * 0.5, hh = lens.w * 0.5;
    float2 c = lens.xy + float2(hw, hh);
    float r = misc.x < 0.0 ? min(hw, hh) : min(misc.x, min(hw, hh));
    float px = P.x - c.x, py = P.y - c.y;
    float dx = fabs(px) - (hw - r), dy = fabs(py) - (hh - r);
    float ax = max(dx, 0.0), ay = max(dy, 0.0);
    float outsideD = sqrt(ax * ax + ay * ay);
    float sdf = outsideD + min(max(dx, dy), 0.0) - r;
    float nx, ny;
    if (dx > 0.0 && dy > 0.0 && outsideD > 0.0) { nx = ax / outsideD; ny = ay / outsideD; }
    else if (dx > dy) { nx = 1.0; ny = 0.0; }
    else { nx = 0.0; ny = 1.0; }
    nx = px < 0.0 ? -nx : nx;
    ny = py < 0.0 ? -ny : ny;
    float s = -sdf;
    float3 outc = base;
    if (s > 0.0) {
        float d = cn1_glass_lens_disp(s, o[0], o[1]);
        float disp = o[2];
        float2 n = float2(nx, ny);
        float cr = src.sample(lin, (tp + n * d * (1.0 - disp)) / size).r;
        float cg = src.sample(lin, (tp + n * d) / size).g;
        float cb = src.sample(lin, (tp + n * d * (1.0 + disp)) / size).b;
        float shade = 1.0;
        if (o[9] > 0.0) {
            float w = o[9];
            float band = s < w * 0.7 ? 1.0 : (s >= w ? 0.0 : smoothstep(0.0, 1.0, (w - s) / (w * 0.3)));
            shade -= o[8] * fabs(nx) * band;
        }
        float rim = 0.0;
        float past = s - o[4];
        if (o[7] > 0.0 && past >= 0.0) {
            rim = (o[5] + o[6] * fabs(ny)) * exp(-past / o[7]);
        }
        float3 inside = float3(cr, cg, cb) * shade + o[10] + rim;
        float cov = min(s, 1.0);
        outc = base + (inside - base) * cov;
    } else if (o[11] > 0.0 && o[12] > 0.0 && ny > 0.0) {
        float t = (sdf - o[12]) / o[12];
        outc *= 1.0 - o[11] * ny * exp(-t * t);
    }
    if (o[4] > 0.0) {
        float halfW = o[4] * 0.5;
        float ring = 1.0 - fabs(s - halfW) / halfW;
        if (ring > 0.0) { outc *= 1.0 - o[3] * min(ring, 1.0); }
    }
    outc = base + (clamp(outc, 0.0, 1.0) - base) * amount;
    return float4(outc, base4.a);
}

// Gaussian blur is implemented via MPSImageGaussianBlur on the host side
// (CN1Metalcompat.m) rather than a hand-rolled fragment shader. MPS picks
// the kernel width automatically from sigma and stays accurate across the
// full sigma range CSS filter:blur and Image.gaussianBlur request - a
// fixed-tap shader either undersamples (large sigma) or degenerates into a
// near-box filter (small sigma), and isn't worth carrying.
