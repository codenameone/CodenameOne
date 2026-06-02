/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.ios;

import com.codename1.gpu.Material;
import com.codename1.gpu.VertexAttribute;
import com.codename1.gpu.VertexFormat;

/// Runtime generator of Metal Shading Language (MSL) source for the iOS 3D
/// backend. This mirrors the logic of the portable GLSL generator
/// (com.codename1.gpu.GlslShaderGenerator) but emits a single MSL source string
/// containing both the vertex and fragment functions for a given Material and
/// VertexFormat. The native side compiles the string once with
/// newLibraryWithSource and caches the resulting MTLRenderPipelineState by the
/// pipeline key.
///
/// The generated functions follow a fixed contract that the native renderer
/// relies on:
///
/// - vertex function name "cn1_vertex_main", fragment "cn1_fragment_main"
/// - the interleaved vertex data is bound at buffer index 0 and decoded with a
///   [[stage_in]] VertexIn struct whose attribute indices match the float
///   offsets supplied by the VertexFormat
/// - a single Uniforms struct (mvp, model, normalMatrix, color, lightDir,
///   lightColor, ambient, eye, shininess) is bound at buffer index 1 for both
///   stages
/// - the diffuse texture is bound at texture index 0 with a sampler at index 0
public final class IOSMetalShaderGenerator {
    /// The MSL vertex function entry point name.
    public static final String VERTEX_FUNCTION = "cn1_vertex_main";
    /// The MSL fragment function entry point name.
    public static final String FRAGMENT_FUNCTION = "cn1_fragment_main";

    private final String source;

    /// Generates the combined MSL source for a material and vertex layout.
    ///
    /// #### Parameters
    ///
    /// - `material`: the material describing the lighting model and inputs
    ///
    /// - `format`: the mesh vertex layout
    public IOSMetalShaderGenerator(Material material, VertexFormat format) {
        boolean hasNormal = format.findByUsage(VertexAttribute.Usage.NORMAL) != null;
        boolean hasTexcoord = format.findByUsage(VertexAttribute.Usage.TEXCOORD) != null;
        boolean textured = material.getTexture() != null && hasTexcoord;
        Material.Type type = material.getType();
        boolean lit = (type == Material.Type.LAMBERT || type == Material.Type.PHONG) && hasNormal;
        boolean phong = type == Material.Type.PHONG && hasNormal;
        this.source = build(format, lit, phong, textured, hasNormal, hasTexcoord);
    }

    private static String build(VertexFormat format, boolean lit, boolean phong,
                                boolean textured, boolean hasNormal, boolean hasTexcoord) {
        int posOff = offsetOf(format, VertexAttribute.Usage.POSITION);
        int normOff = offsetOf(format, VertexAttribute.Usage.NORMAL);
        int uvOff = offsetOf(format, VertexAttribute.Usage.TEXCOORD);

        StringBuilder sb = new StringBuilder();
        sb.append("#include <metal_stdlib>\n");
        sb.append("using namespace metal;\n");

        // Uniform block. Layout must match the float[] the Java side packs and
        // the C struct the native renderer copies into the uniform buffer.
        sb.append("struct CN1Uniforms {\n");
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

        // Vertex input. The interleaved buffer is decoded with explicit
        // attribute indices matching the float offsets of each component.
        sb.append("struct CN1VertexIn {\n");
        sb.append("  float3 position [[attribute(").append(posOff).append(")]];\n");
        if (hasNormal) {
            sb.append("  float3 normal [[attribute(").append(normOff).append(")]];\n");
        }
        if (hasTexcoord) {
            sb.append("  float2 texcoord [[attribute(").append(uvOff).append(")]];\n");
        }
        sb.append("};\n");

        sb.append("struct CN1VertexOut {\n");
        sb.append("  float4 position [[position]];\n");
        if (lit) {
            sb.append("  float3 worldNormal;\n");
            sb.append("  float3 worldPos;\n");
        }
        if (textured) {
            sb.append("  float2 texcoord;\n");
        }
        sb.append("};\n");

        // Vertex function.
        sb.append("vertex CN1VertexOut ").append(VERTEX_FUNCTION).append("(\n");
        sb.append("    CN1VertexIn in [[stage_in]],\n");
        sb.append("    constant CN1Uniforms& u [[buffer(1)]]) {\n");
        sb.append("  CN1VertexOut out;\n");
        sb.append("  out.position = u.mvp * float4(in.position, 1.0);\n");
        if (lit) {
            sb.append("  out.worldNormal = (u.normalMatrix * float4(in.normal, 0.0)).xyz;\n");
            sb.append("  out.worldPos = (u.model * float4(in.position, 1.0)).xyz;\n");
        }
        if (textured) {
            sb.append("  out.texcoord = in.texcoord;\n");
        }
        sb.append("  return out;\n");
        sb.append("}\n");

        // Fragment function.
        sb.append("fragment float4 ").append(FRAGMENT_FUNCTION).append("(\n");
        sb.append("    CN1VertexOut in [[stage_in]],\n");
        sb.append("    constant CN1Uniforms& u [[buffer(1)]]");
        if (textured) {
            sb.append(",\n    texture2d<float> tex [[texture(0)]],\n");
            sb.append("    sampler texSampler [[sampler(0)]]");
        }
        sb.append(") {\n");
        sb.append("  float4 base = u.color;\n");
        if (textured) {
            sb.append("  base = base * tex.sample(texSampler, in.texcoord);\n");
        }
        if (lit) {
            sb.append("  float3 n = normalize(in.worldNormal);\n");
            sb.append("  float3 l = normalize(-u.lightDir.xyz);\n");
            sb.append("  float ndotl = max(dot(n, l), 0.0);\n");
            sb.append("  float3 lighting = u.ambient.xyz + u.lightColor.xyz * ndotl;\n");
            sb.append("  float3 rgb = base.rgb * lighting;\n");
            if (phong) {
                sb.append("  if (ndotl > 0.0) {\n");
                sb.append("    float3 v = normalize(u.eye.xyz - in.worldPos);\n");
                sb.append("    float3 h = normalize(l + v);\n");
                sb.append("    float spec = pow(max(dot(n, h), 0.0), u.shininess);\n");
                sb.append("    rgb += u.lightColor.xyz * spec;\n");
                sb.append("  }\n");
            }
            sb.append("  return float4(rgb, base.a);\n");
        } else {
            sb.append("  return base;\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static int offsetOf(VertexFormat fmt, VertexAttribute.Usage usage) {
        for (int i = 0; i < fmt.getAttributeCount(); i++) {
            if (fmt.getAttribute(i).getUsage() == usage) {
                return fmt.getAttributeOffset(i);
            }
        }
        return -1;
    }

    /// Returns the generated combined MSL source.
    public String getSource() {
        return source;
    }
}
