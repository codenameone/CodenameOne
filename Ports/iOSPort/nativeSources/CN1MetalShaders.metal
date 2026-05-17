/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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

// --------- GaussianBlur pipelines (horizontal + vertical separable pass) ---------
//
// Two-pass separable Gaussian blur. Horizontal pass samples 13 taps along
// the x axis, vertical pass samples 13 taps along the y axis. Sigma is
// passed in `params.x` along with `texelSize` (1.0 / dimension) in .y so
// the same shader handles arbitrary source sizes.
//
// Weights computed in-shader (exp(-0.5 * (i/sigma)^2)) so a single uniform
// (sigma) drives the kernel -- no host-side table upload.
//
// Buffer layout:
//   buffer(0): float4 params -- (sigma, texelSize, kind, 0)
//              kind == 0 horizontal, kind == 1 vertical
//   texture(0): source RGBA

static inline float cn1_blur_weight(float i, float invSigma2) {
    return exp(-0.5 * i * i * invSigma2);
}

fragment float4 cn1_fs_gaussian_blur(
    VertexOutTextured in [[stage_in]],
    constant float4 &params [[buffer(0)]],
    texture2d<float> tex [[texture(0)]])
{
    constexpr sampler s(mag_filter::linear, min_filter::linear, address::clamp_to_edge);
    float sigma = max(params.x, 0.5);
    float texelSize = params.y;
    int horizontal = (int)params.z == 0 ? 1 : 0;
    float invSigma2 = 1.0 / (sigma * sigma);

    // 13-tap kernel (centre + 6 samples each side). Sigma >= 6 saturates
    // a much wider kernel; in practice CSS filter:blur(N) maps to a
    // pixel radius around N and the visible spread stays within ~3*sigma.
    float wTotal = cn1_blur_weight(0.0, invSigma2);
    float4 acc = tex.sample(s, in.texcoord) * wTotal;
    for (int i = 1; i <= 6; i++) {
        float w = cn1_blur_weight((float)i, invSigma2);
        float2 off = horizontal != 0
            ? float2((float)i * texelSize, 0.0)
            : float2(0.0, (float)i * texelSize);
        acc += tex.sample(s, in.texcoord + off) * w;
        acc += tex.sample(s, in.texcoord - off) * w;
        wTotal += 2.0 * w;
    }
    return acc / wTotal;
}
