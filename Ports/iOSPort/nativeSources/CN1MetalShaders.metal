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
    const device float2 *positions [[buffer(0)]],
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
    const device float2 *positions [[buffer(0)]],
    constant CN1Matrices &matrices [[buffer(1)]],
    const device float2 *texcoords [[buffer(2)]])
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
