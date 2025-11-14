/*
 * Shaders.metal
 * Codename One - Metal Shading Language Shaders
 *
 * Created for Codename One iOS Metal Backend 2025
 * Copyright (c) 2012-2025, Codename One and/or its affiliates. All rights reserved.
 */

#include <metal_stdlib>
using namespace metal;

// ============================================================================
// SHARED STRUCTURES
// ============================================================================

// Uniforms passed to all shaders
struct Uniforms {
    float4x4 mvpMatrix;  // Model-View-Projection matrix
    float4 color;        // Modulation color (RGBA)
};

// ============================================================================
// SOLID COLOR SHADERS (for FillRect, DrawRect, etc.)
// ============================================================================

struct SolidColorVertexIn {
    float2 position [[attribute(0)]];
};

struct SolidColorVertexOut {
    float4 position [[position]];
    float4 color;
};

vertex SolidColorVertexOut solidColor_vertex(
    SolidColorVertexIn in [[stage_in]],
    constant Uniforms &uniforms [[buffer(1)]]
) {
    SolidColorVertexOut out;
    out.position = uniforms.mvpMatrix * float4(in.position, 0.0, 1.0);
    out.color = uniforms.color;
    return out;
}

fragment float4 solidColor_fragment(SolidColorVertexOut in [[stage_in]]) {
    return in.color;
}

// ============================================================================
// TEXTURED SHADERS (for DrawImage, DrawString, etc.)
// ============================================================================

struct TexturedVertexIn {
    float2 position [[attribute(0)]];
    float2 texCoord [[attribute(1)]];
};

struct TexturedVertexOut {
    float4 position [[position]];
    float2 texCoord;
    float4 color;
};

vertex TexturedVertexOut textured_vertex(
    TexturedVertexIn in [[stage_in]],
    constant Uniforms &uniforms [[buffer(1)]]
) {
    TexturedVertexOut out;
    out.position = uniforms.mvpMatrix * float4(in.position, 0.0, 1.0);
    out.texCoord = in.texCoord;
    out.color = uniforms.color;
    return out;
}

fragment float4 textured_fragment(
    TexturedVertexOut in [[stage_in]],
    texture2d<float> tex [[texture(0)]],
    sampler texSampler [[sampler(0)]]
) {
    float4 texColor = tex.sample(texSampler, in.texCoord);
    return texColor * in.color;  // Modulate by color (for alpha, tint)
}

// ============================================================================
// LINEAR GRADIENT SHADERS
// ============================================================================

struct GradientUniforms {
    float4x4 mvpMatrix;
    float4 color1;       // Start color
    float4 color2;       // End color
    float2 startPoint;   // Gradient start (normalized 0-1)
    float2 endPoint;     // Gradient end (normalized 0-1)
};

struct GradientVertexIn {
    float2 position [[attribute(0)]];
};

struct GradientVertexOut {
    float4 position [[position]];
    float2 worldPosition;  // For interpolation calculation
};

vertex GradientVertexOut gradient_vertex(
    GradientVertexIn in [[stage_in]],
    constant GradientUniforms &uniforms [[buffer(1)]]
) {
    GradientVertexOut out;
    out.position = uniforms.mvpMatrix * float4(in.position, 0.0, 1.0);
    out.worldPosition = in.position;
    return out;
}

fragment float4 gradient_fragment(
    GradientVertexOut in [[stage_in]],
    constant GradientUniforms &uniforms [[buffer(1)]]
) {
    // Calculate interpolation factor along gradient vector
    float2 gradVec = uniforms.endPoint - uniforms.startPoint;
    float2 fragVec = in.worldPosition - uniforms.startPoint;
    float t = dot(fragVec, gradVec) / dot(gradVec, gradVec);
    t = clamp(t, 0.0, 1.0);

    // Interpolate between colors
    return mix(uniforms.color1, uniforms.color2, t);
}

// ============================================================================
// RADIAL GRADIENT SHADERS
// ============================================================================

struct RadialGradientUniforms {
    float4x4 mvpMatrix;
    float4 color1;         // Center color
    float4 color2;         // Outer color
    float2 centerPoint;    // Center of gradient
    float radius;          // Radius of gradient
};

fragment float4 radialGradient_fragment(
    GradientVertexOut in [[stage_in]],
    constant RadialGradientUniforms &uniforms [[buffer(1)]]
) {
    // Calculate distance from center
    float2 diff = in.worldPosition - uniforms.centerPoint;
    float distance = length(diff);

    // Normalize by radius
    float t = clamp(distance / uniforms.radius, 0.0, 1.0);

    // Interpolate between colors
    return mix(uniforms.color1, uniforms.color2, t);
}

// ============================================================================
// LINE SHADERS (for DrawLine)
// ============================================================================

// Uses same vertex/fragment as solidColor shaders
// Lines are drawn as thin rectangles or using Metal line primitives

// ============================================================================
// ALPHA MASK SHADERS (for DrawTextureAlphaMask)
// ============================================================================

struct AlphaMaskVertexIn {
    float2 position [[attribute(0)]];
    float2 texCoord [[attribute(1)]];
    float2 maskCoord [[attribute(2)]];
};

struct AlphaMaskVertexOut {
    float4 position [[position]];
    float2 texCoord;
    float2 maskCoord;
    float4 color;
};

vertex AlphaMaskVertexOut alphaMask_vertex(
    AlphaMaskVertexIn in [[stage_in]],
    constant Uniforms &uniforms [[buffer(1)]]
) {
    AlphaMaskVertexOut out;
    out.position = uniforms.mvpMatrix * float4(in.position, 0.0, 1.0);
    out.texCoord = in.texCoord;
    out.maskCoord = in.maskCoord;
    out.color = uniforms.color;
    return out;
}

fragment float4 alphaMask_fragment(
    AlphaMaskVertexOut in [[stage_in]],
    texture2d<float> tex [[texture(0)]],
    texture2d<float> mask [[texture(1)]],
    sampler texSampler [[sampler(0)]]
) {
    float4 texColor = tex.sample(texSampler, in.texCoord);
    float maskAlpha = mask.sample(texSampler, in.maskCoord).a;

    return float4(texColor.rgb, texColor.a * maskAlpha) * in.color;
}

// ============================================================================
// DEBUG SHADERS (for testing)
// ============================================================================

// Simple vertex color shader for debugging
struct DebugVertexIn {
    float2 position [[attribute(0)]];
    float4 color [[attribute(1)]];
};

struct DebugVertexOut {
    float4 position [[position]];
    float4 color;
};

vertex DebugVertexOut debug_vertex(
    DebugVertexIn in [[stage_in]],
    constant Uniforms &uniforms [[buffer(1)]]
) {
    DebugVertexOut out;
    out.position = uniforms.mvpMatrix * float4(in.position, 0.0, 1.0);
    out.color = in.color;
    return out;
}

fragment float4 debug_fragment(DebugVertexOut in [[stage_in]]) {
    return in.color;
}
