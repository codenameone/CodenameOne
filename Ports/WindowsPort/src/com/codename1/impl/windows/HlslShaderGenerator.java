/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.windows;

import com.codename1.gpu.Material;
import com.codename1.gpu.VertexAttribute;
import com.codename1.gpu.VertexFormat;

/// Runtime generator of HLSL (Direct3D 11, Shader Model 4) source for the native
/// Windows 3D backend. It mirrors the logic of the portable GLSL generator
/// (com.codename1.impl.gpu.GlslShaderGenerator) and the iOS Metal generator, but
/// emits a single HLSL source string holding both the vertex and pixel entry
/// points for a given Material and VertexFormat. The native side compiles the
/// string once per entry point (D3DCompile with `vs_4_0` / `ps_4_0`) and caches
/// the resulting pipeline by the material+format key.
///
/// The generated shaders follow a fixed contract the native renderer relies on:
///
/// - vertex entry point "cn1_vertex_main", pixel entry point "cn1_fragment_main"
/// - the interleaved vertex data is decoded with a `VSInput` struct whose
///   semantics are POSITION, NORMAL, TEXCOORD0 in the order the VertexFormat
///   lists them
/// - one constant buffer `CN1Uniforms` at register b0 (mvp, model, normalMatrix,
///   color, lightDir, lightColor, ambient, eye, shininess). It is uploaded from
///   the same column-major float[16] matrices the Java side packs; HLSL's default
///   column-major cbuffer packing means `mul(mvp, v)` matches the GL/Metal
///   `mvp * v`, so no transpose is needed.
/// - the diffuse texture is bound at register t0 with a sampler at s0
public final class HlslShaderGenerator {
    /// The HLSL vertex entry point name.
    public static final String VERTEX_FUNCTION = "cn1_vertex_main";
    /// The HLSL pixel entry point name.
    public static final String FRAGMENT_FUNCTION = "cn1_fragment_main";

    private final String source;

    /// Generates the combined HLSL source for a material and vertex layout.
    ///
    /// #### Parameters
    ///
    /// - `material`: the material describing the lighting model and inputs
    ///
    /// - `format`: the mesh vertex layout
    public HlslShaderGenerator(Material material, VertexFormat format) {
        boolean hasNormal = format.findByUsage(VertexAttribute.Usage.NORMAL) != null;
        boolean hasTexcoord = format.findByUsage(VertexAttribute.Usage.TEXCOORD) != null;
        boolean textured = material.getTexture() != null && hasTexcoord;
        Material.Type type = material.getType();
        boolean lit = (type == Material.Type.LAMBERT || type == Material.Type.PHONG) && hasNormal;
        boolean phong = type == Material.Type.PHONG && hasNormal;
        this.source = build(lit, phong, textured, hasNormal, hasTexcoord);
    }

    private static String build(boolean lit, boolean phong, boolean textured,
                                boolean hasNormal, boolean hasTexcoord) {
        StringBuilder sb = new StringBuilder();

        // Constant buffer. Layout matches the float[] the Java side packs and the
        // C struct the native renderer copies into the cbuffer.
        sb.append("cbuffer CN1Uniforms : register(b0) {\n");
        sb.append("  float4x4 mvp;\n");
        sb.append("  float4x4 model;\n");
        sb.append("  float4x4 normalMatrix;\n");
        sb.append("  float4 color;\n");
        sb.append("  float4 lightDir;\n");
        sb.append("  float4 lightColor;\n");
        sb.append("  float4 ambient;\n");
        sb.append("  float4 eye;\n");
        sb.append("  float shininess;\n");
        sb.append("};\n");

        if (textured) {
            sb.append("Texture2D cn1_tex : register(t0);\n");
            sb.append("SamplerState cn1_sampler : register(s0);\n");
        }

        // Vertex input/output structs.
        sb.append("struct VSInput {\n");
        sb.append("  float3 position : POSITION;\n");
        if (hasNormal) {
            sb.append("  float3 normal : NORMAL;\n");
        }
        if (hasTexcoord) {
            sb.append("  float2 texcoord : TEXCOORD0;\n");
        }
        sb.append("};\n");

        sb.append("struct VSOutput {\n");
        sb.append("  float4 position : SV_Position;\n");
        if (lit) {
            sb.append("  float3 worldNormal : TEXCOORD1;\n");
            sb.append("  float3 worldPos : TEXCOORD2;\n");
        }
        if (textured) {
            sb.append("  float2 texcoord : TEXCOORD0;\n");
        }
        sb.append("};\n");

        // Vertex shader.
        sb.append("VSOutput ").append(VERTEX_FUNCTION).append("(VSInput input) {\n");
        sb.append("  VSOutput output;\n");
        sb.append("  float4 clip = mul(mvp, float4(input.position, 1.0));\n");
        // Adapt the portable GL-convention clip space to Direct3D: only remap Z
        // from GL's [-w, w] to D3D's [0, w] depth range. Y is NOT flipped here --
        // the GL backend (the reference) does not flip either, and D3D's viewport
        // already maps NDC +Y to the top of the render target, so a flip would
        // render the scene upside down. Winding is handled by the rasterizer state
        // in cn1_windows_d3d.cpp (front faces are counter-clockwise).
        sb.append("  clip.z = (clip.z + clip.w) * 0.5;\n");
        sb.append("  output.position = clip;\n");
        if (lit) {
            sb.append("  output.worldNormal = mul(normalMatrix, float4(input.normal, 0.0)).xyz;\n");
            sb.append("  output.worldPos = mul(model, float4(input.position, 1.0)).xyz;\n");
        }
        if (textured) {
            sb.append("  output.texcoord = input.texcoord;\n");
        }
        sb.append("  return output;\n");
        sb.append("}\n");

        // Pixel shader.
        sb.append("float4 ").append(FRAGMENT_FUNCTION).append("(VSOutput input) : SV_Target {\n");
        sb.append("  float4 base = color;\n");
        if (textured) {
            sb.append("  base = base * cn1_tex.Sample(cn1_sampler, input.texcoord);\n");
        }
        if (lit) {
            sb.append("  float3 n = normalize(input.worldNormal);\n");
            sb.append("  float3 l = normalize(-lightDir.xyz);\n");
            sb.append("  float ndotl = max(dot(n, l), 0.0);\n");
            sb.append("  float3 lighting = ambient.xyz + lightColor.xyz * ndotl;\n");
            sb.append("  float3 rgb = base.rgb * lighting;\n");
            if (phong) {
                sb.append("  if (ndotl > 0.0) {\n");
                sb.append("    float3 v = normalize(eye.xyz - input.worldPos);\n");
                sb.append("    float3 h = normalize(l + v);\n");
                sb.append("    float spec = pow(max(dot(n, h), 0.0), shininess);\n");
                sb.append("    rgb += lightColor.xyz * spec;\n");
                sb.append("  }\n");
            }
            sb.append("  return float4(rgb, base.a);\n");
        } else {
            sb.append("  return base;\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    /// Returns the generated combined HLSL source.
    public String getSource() {
        return source;
    }
}
