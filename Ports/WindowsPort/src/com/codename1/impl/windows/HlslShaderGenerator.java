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
package com.codename1.impl.windows;

import com.codename1.gpu.Environment;
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
///   color, lightDir, lightColor, ambient, eye, params, fog, hemisphere, emissive
///   and sky vectors, all float4). It is uploaded from
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

    /// Generates the combined HLSL source for a material and vertex layout in
    /// the default environment.
    ///
    /// #### Parameters
    ///
    /// - `material`: the material describing the lighting model and inputs
    ///
    /// - `format`: the mesh vertex layout
    public HlslShaderGenerator(Material material, VertexFormat format) {
        this(material, format, null, false);
    }

    /// Generates the combined HLSL source for a material, vertex layout and
    /// environment. The features switched on match
    /// `GlslShaderGenerator.variantKey`: fog, hemisphere ambient, tone mapping
    /// and the procedural sky.
    ///
    /// #### Parameters
    ///
    /// - `material`: the material describing the lighting model and inputs
    ///
    /// - `format`: the mesh vertex layout
    ///
    /// - `environment`: the fog, ambient and tone mapping in effect, or null
    public HlslShaderGenerator(Material material, VertexFormat format, Environment environment) {
        this(material, format, environment, false);
    }

    /// Generates the combined HLSL source for a material, vertex layout and
    /// environment, optionally sampling the directional shadow map.
    ///
    /// #### Parameters
    ///
    /// - `material`: the material describing the lighting model and inputs
    ///
    /// - `format`: the mesh vertex layout
    ///
    /// - `environment`: the fog, ambient and tone mapping in effect, or null
    ///
    /// - `shadowed`: true if the shadow map (texture t1, sampler s1) is bound and
    ///   the material receives it
    public HlslShaderGenerator(Material material, VertexFormat format, Environment environment,
                               boolean shadowed) {
        boolean hasNormal = format.findByUsage(VertexAttribute.Usage.NORMAL) != null;
        boolean hasTexcoord = format.findByUsage(VertexAttribute.Usage.TEXCOORD) != null;
        Material.Type type = material.getType();
        boolean sky = type == Material.Type.SKYBOX;
        boolean lit = material.isLit() && hasNormal;
        boolean phong = type == Material.Type.PHONG && hasNormal;
        boolean pbr = type == Material.Type.PBR && hasNormal;
        boolean textured = material.getTexture() != null && hasTexcoord;
        boolean fog = environment != null && environment.isFog() && !sky;
        boolean skyOn = sky && environment != null && environment.isSky();
        boolean hemi = lit && environment != null && environment.isHemisphere();
        boolean tone = lit && environment != null && environment.isToneMapping();
        boolean emissive = lit && (material.getEmissive() & 0xffffff) != 0;
        boolean shadow = lit && shadowed && material.isReceiveShadows();
        this.source = build(lit, phong, pbr, textured, hasNormal, hasTexcoord, fog, skyOn,
                hemi, tone, emissive, shadow);
    }

    private static String build(boolean lit, boolean phong, boolean pbr, boolean textured,
                                boolean hasNormal, boolean hasTexcoord, boolean fog,
                                boolean sky, boolean hemi, boolean tone, boolean emissive,
                                boolean shadow) {
        boolean world = lit || fog || sky;
        StringBuilder sb = new StringBuilder();

        // Constant buffer. Layout matches the float[] the Java side packs and the
        // C struct the native renderer copies into the cbuffer. Every field is a
        // whole float4 (or matrix) so packing never straddles a 16 byte row.
        sb.append("cbuffer CN1Uniforms : register(b0) {\n");
        sb.append("  float4x4 mvp;\n");
        sb.append("  float4x4 model;\n");
        sb.append("  float4x4 normalMatrix;\n");
        sb.append("  float4 color;\n");
        sb.append("  float4 lightDir;\n");
        sb.append("  float4 lightColor;\n");
        sb.append("  float4 ambient;\n");
        sb.append("  float4 eye;\n");
        sb.append("  float4 params;\n");      // shininess, metallic, roughness, exposure
        sb.append("  float4 fogColor;\n");
        sb.append("  float4 fogParams;\n");   // density, height falloff, base height
        sb.append("  float4 skyColor;\n");
        sb.append("  float4 groundColor;\n");
        sb.append("  float4 emissiveColor;\n");
        sb.append("  float4 zenith;\n");
        sb.append("  float4 horizon;\n");
        sb.append("  float4 sun;\n");         // sun colour, cloud cover
        sb.append("  float4x4 shadowMatrix;\n");
        sb.append("  float4 shadowParams;\n"); // bias, strength, texel size
        sb.append("};\n");

        if (textured) {
            sb.append("Texture2D cn1_tex : register(t0);\n");
            sb.append("SamplerState cn1_sampler : register(s0);\n");
        }

        if (shadow) {
            sb.append("Texture2D cn1_shadowMap : register(t1);\n");
            sb.append("SamplerState cn1_shadowSampler : register(s1);\n");
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
        }
        if (world) {
            sb.append("  float3 worldPos : TEXCOORD2;\n");
        }
        if (textured) {
            sb.append("  float2 texcoord : TEXCOORD0;\n");
        }
        if (shadow) {
            sb.append("  float4 shadowPos : TEXCOORD3;\n");
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
        }
        if (world) {
            sb.append("  output.worldPos = mul(model, float4(input.position, 1.0)).xyz;\n");
        }
        if (textured) {
            sb.append("  output.texcoord = input.texcoord;\n");
        }
        if (shadow) {
            sb.append("  output.shadowPos = mul(shadowMatrix, mul(model, float4(input.position, 1.0)));\n");
        }
        sb.append("  return output;\n");
        sb.append("}\n");

        if (sky) {
            appendSkyNoise(sb);
        }
        if (tone) {
            // Narkowicz's fit of the ACES filmic curve
            sb.append("float3 aces(float3 x) {\n");
            sb.append("  return saturate((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14));\n");
            sb.append("}\n");
        }

        if (shadow) {
            appendShadowLookup(sb);
        }

        // Pixel shader.
        sb.append("float4 ").append(FRAGMENT_FUNCTION).append("(VSOutput input) : SV_Target {\n");
        sb.append("  float4 base = color;\n");
        if (textured) {
            sb.append("  base = base * cn1_tex.Sample(cn1_sampler, input.texcoord);\n");
        }
        sb.append("  float3 rgb = base.rgb;\n");
        if (sky) {
            appendSky(sb);
        }
        if (lit) {
            sb.append("  float3 n = normalize(input.worldNormal);\n");
            sb.append("  float3 l = normalize(-lightDir.xyz);\n");
            sb.append("  float ndotl = max(dot(n, l), 0.0);\n");
            if (hemi) {
                sb.append("  float3 amb = lerp(groundColor.xyz, skyColor.xyz, n.y * 0.5 + 0.5);\n");
            } else {
                sb.append("  float3 amb = ambient.xyz;\n");
            }
            sb.append(shadow ? "  float shade = shadowAt(input.shadowPos, n, l);\n" : "  float shade = 1.0;\n");
            if (pbr) {
                appendPbr(sb);
            } else {
                sb.append("  rgb = base.rgb * (amb + lightColor.xyz * ndotl * shade);\n");
                if (phong) {
                    sb.append("  if (ndotl > 0.0) {\n");
                    sb.append("    float3 v = normalize(eye.xyz - input.worldPos);\n");
                    sb.append("    float3 h = normalize(l + v);\n");
                    sb.append("    float spec = pow(max(dot(n, h), 0.0), params.x);\n");
                    sb.append("    rgb += lightColor.xyz * spec * shade;\n");
                    sb.append("  }\n");
                }
            }
            if (emissive) {
                sb.append("  rgb += emissiveColor.xyz;\n");
            }
            if (tone) {
                sb.append("  rgb = aces(rgb * params.w);\n");
            }
        }
        if (fog) {
            sb.append("  float camDist = length(input.worldPos - eye.xyz);\n");
            sb.append("  float fd = fogParams.x * camDist;\n");
            sb.append("  float fogHeight = exp(-fogParams.y * max(input.worldPos.y - fogParams.z, 0.0));\n");
            sb.append("  float fogAmount = saturate((1.0 - exp(-fd * fd)) * fogHeight);\n");
            sb.append("  rgb = lerp(rgb, fogColor.xyz, fogAmount);\n");
        }
        sb.append("  return float4(rgb, base.a);\n");
        sb.append("}\n");
        return sb.toString();
    }

    // The 3x3 PCF lookup of GlslShaderGenerator. The shadow matrix is in GL clip
    // convention, so x, y and z are remapped with * 0.5 + 0.5 exactly as in GLSL;
    // only v is flipped (1 - y), because Direct3D texture row 0 is the top of the
    // render target the caster pass drew with +Y up. The map holds that same
    // GL-convention depth in an R32_FLOAT target, so there is nothing to unpack.
    private static void appendShadowLookup(StringBuilder sb) {
        sb.append("float shadowAt(float4 sp, float3 n, float3 l) {\n");
        sb.append("  float3 p = sp.xyz / sp.w * 0.5 + 0.5;\n");
        sb.append("  if (p.x < 0.0 || p.x > 1.0 || p.y < 0.0 || p.y > 1.0 || p.z > 1.0) {\n");
        sb.append("    return 1.0;\n  }\n");
        sb.append("  float bias = shadowParams.x * (1.0 + 2.0 * (1.0 - max(dot(n, l), 0.0)));\n");
        sb.append("  float lit = 0.0;\n");
        sb.append("  [unroll] for (int y = -1; y <= 1; y++) {\n");
        sb.append("    [unroll] for (int x = -1; x <= 1; x++) {\n");
        sb.append("      float2 uv = float2(p.x + float(x) * shadowParams.z, (1.0 - p.y) + float(y) * shadowParams.z);\n");
        sb.append("      float d = cn1_shadowMap.SampleLevel(cn1_shadowSampler, uv, 0).r;\n");
        sb.append("      lit += (p.z - bias > d) ? 0.0 : 1.0;\n");
        sb.append("    }\n  }\n");
        sb.append("  return 1.0 - shadowParams.y * (1.0 - lit / 9.0);\n");
        sb.append("}\n");
    }

    /// Returns the HLSL for the shadow caster pass: depth only, written to an
    /// R32_FLOAT target. The first uniform matrix is the light matrix times the
    /// model matrix.
    ///
    /// #### Returns
    ///
    /// the combined vertex and pixel source
    public static String shadowSource() {
        StringBuilder sb = new StringBuilder();
        sb.append("cbuffer CN1Uniforms : register(b0) {\n  float4x4 mvp;\n};\n");
        sb.append("struct VSInput {\n  float3 position : POSITION;\n};\n");
        sb.append("struct VSOutput {\n  float4 position : SV_Position;\n  float depth : TEXCOORD0;\n};\n");
        sb.append("VSOutput ").append(VERTEX_FUNCTION).append("(VSInput input) {\n");
        sb.append("  VSOutput output;\n");
        sb.append("  float4 clip = mul(mvp, float4(input.position, 1.0));\n");
        sb.append("  output.depth = clip.z / clip.w * 0.5 + 0.5;\n");
        sb.append("  clip.z = (clip.z + clip.w) * 0.5;\n");
        sb.append("  output.position = clip;\n");
        sb.append("  return output;\n}\n");
        sb.append("float4 ").append(FRAGMENT_FUNCTION).append("(VSOutput input) : SV_Target {\n");
        sb.append("  float d = saturate(input.depth);\n");
        sb.append("  return float4(d, d, d, 1.0);\n}\n");
        return sb.toString();
    }

    // Common prologue of the post passes: the quad vertex shader and the
    // constant buffer. v is flipped so uv (0, 0) is the top left texel of the
    // render target, which is where Direct3D puts row 0.
    private static String postPrologue() {
        return "cbuffer CN1Post : register(b0) {\n"
                + "  float4 g0;\n"   // threshold, bloom intensity, contrast, saturation
                + "  float4 g1;\n"   // lift rgb, vignette
                + "  float4 g2;\n"   // gain rgb, grain
                + "  float4 g3;\n"   // time, resolution x, resolution y, bloom on
                + "  float4 g4;\n"   // bright pass: texel in uv
                + "  float4 g5;\n"   // blur pass: direction in uv
                + "};\n"
                + "Texture2D cn1_scene : register(t0);\n"
                + "Texture2D cn1_bloom : register(t1);\n"
                + "SamplerState cn1_sampler : register(s0);\n"
                + "struct VSInput {\n  float3 position : POSITION;\n};\n"
                + "struct VSOutput {\n  float4 position : SV_Position;\n  float2 uv : TEXCOORD0;\n};\n"
                + "VSOutput " + VERTEX_FUNCTION + "(VSInput input) {\n"
                + "  VSOutput output;\n"
                + "  output.uv = float2(input.position.x * 0.5 + 0.5, 0.5 - input.position.y * 0.5);\n"
                + "  output.position = float4(input.position.xy, 0.0, 1.0);\n"
                + "  return output;\n}\n"
                + "float4 " + FRAGMENT_FUNCTION + "(VSOutput input) : SV_Target {\n";
    }

    /// Returns the bloom bright pass (see GlslShaderGenerator.brightFragment).
    ///
    /// #### Returns
    ///
    /// the combined vertex and pixel source
    public static String postBrightSource() {
        return postPrologue()
                + "  float2 t = g4.xy;\n"
                + "  float3 c = (cn1_scene.SampleLevel(cn1_sampler, input.uv + t * float2(-0.5, -0.5), 0).rgb\n"
                + "      + cn1_scene.SampleLevel(cn1_sampler, input.uv + t * float2(0.5, -0.5), 0).rgb\n"
                + "      + cn1_scene.SampleLevel(cn1_sampler, input.uv + t * float2(-0.5, 0.5), 0).rgb\n"
                + "      + cn1_scene.SampleLevel(cn1_sampler, input.uv + t * float2(0.5, 0.5), 0).rgb) * 0.25;\n"
                + "  float peak = max(c.r, max(c.g, c.b));\n"
                + "  float over = max(peak - g0.x, 0.0) / max(1.0 - g0.x, 0.0001);\n"
                + "  return float4(c * (over / max(peak, 0.0001)), 1.0);\n}\n";
    }

    /// Returns one direction of the separable blur (see
    /// GlslShaderGenerator.blurFragment).
    ///
    /// #### Returns
    ///
    /// the combined vertex and pixel source
    public static String postBlurSource() {
        return postPrologue()
                + "  float2 d = g5.xy;\n"
                + "  float3 c = cn1_scene.SampleLevel(cn1_sampler, input.uv, 0).rgb * 0.2270270;\n"
                + "  c += cn1_scene.SampleLevel(cn1_sampler, input.uv + d * 1.3846154, 0).rgb * 0.3162162;\n"
                + "  c += cn1_scene.SampleLevel(cn1_sampler, input.uv - d * 1.3846154, 0).rgb * 0.3162162;\n"
                + "  c += cn1_scene.SampleLevel(cn1_sampler, input.uv + d * 3.2307692, 0).rgb * 0.0702703;\n"
                + "  c += cn1_scene.SampleLevel(cn1_sampler, input.uv - d * 3.2307692, 0).rgb * 0.0702703;\n"
                + "  return float4(c, 1.0);\n}\n";
    }

    /// Returns the final composite pass: bloom, grade, vignette and grain (see
    /// GlslShaderGenerator.compositeFragment).
    ///
    /// #### Returns
    ///
    /// the combined vertex and pixel source
    public static String postCompositeSource() {
        return postPrologue()
                + "  float3 c = cn1_scene.SampleLevel(cn1_sampler, input.uv, 0).rgb;\n"
                + "  c += cn1_bloom.SampleLevel(cn1_sampler, input.uv, 0).rgb * g0.y;\n"
                + "  c = (c - 0.5) * g0.z + 0.5;\n"
                + "  float luma = dot(c, float3(0.2126, 0.7152, 0.0722));\n"
                + "  c = lerp(float3(luma, luma, luma), c, g0.w);\n"
                + "  c = saturate(c);\n"
                + "  c = (g1.xyz + c * (1.0 - g1.xyz)) * g2.xyz;\n"
                + "  float2 d = input.uv - 0.5;\n"
                + "  c *= 1.0 - g1.w * smoothstep(0.25, 0.75, dot(d, d) * 2.0);\n"
                + "  float2 px = floor(input.uv * g3.yz);\n"
                + "  float noise = frac(sin(dot(px + frac(g3.x * 7.0) * 61.0, float2(12.9898, 78.233))) * 43758.5453);\n"
                + "  c += (noise - 0.5) * g2.w;\n"
                + "  return float4(c, 1.0);\n}\n";
    }

    // Value noise and four octaves of it, for the clouds. Named cn1* because
    // HLSL has an intrinsic called noise.
    private static void appendSkyNoise(StringBuilder sb) {
        sb.append("float cn1Hash(float2 p) {\n");
        sb.append("  return frac(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);\n");
        sb.append("}\n");
        sb.append("float cn1Noise(float2 p) {\n");
        sb.append("  float2 i = floor(p);\n");
        sb.append("  float2 u = frac(p);\n");
        sb.append("  u = u * u * (3.0 - 2.0 * u);\n");
        sb.append("  return lerp(lerp(cn1Hash(i), cn1Hash(i + float2(1.0, 0.0)), u.x),\n");
        sb.append("              lerp(cn1Hash(i + float2(0.0, 1.0)), cn1Hash(i + float2(1.0, 1.0)), u.x), u.y);\n");
        sb.append("}\n");
        sb.append("float cn1Fbm(float2 p) {\n");
        sb.append("  float v = 0.0;\n");
        sb.append("  float a = 0.5;\n");
        sb.append("  [unroll] for (int i = 0; i < 4; i++) {\n");
        sb.append("    v += a * cn1Noise(p);\n");
        sb.append("    p = p * 2.03 + float2(1.7, 9.2);\n");
        sb.append("    a *= 0.5;\n");
        sb.append("  }\n");
        sb.append("  return v;\n");
        sb.append("}\n");
    }

    // The open sky, from the direction the pixel is seen in: a horizon to
    // zenith gradient, clouds projected onto a plane overhead and lit from
    // the sun's side, and the sun's disc with a wide glow around it.
    private static void appendSky(StringBuilder sb) {
        sb.append("  float3 dir = normalize(input.worldPos - eye.xyz);\n");
        sb.append("  float up = saturate(dir.y);\n");
        sb.append("  rgb = lerp(horizon.xyz, zenith.xyz, pow(up, 0.55));\n");
        sb.append("  float3 sunDir = normalize(-lightDir.xyz);\n");
        sb.append("  float sd = max(dot(dir, sunDir), 0.0);\n");
        sb.append("  if (dir.y > 0.0 && sun.w > 0.0) {\n");
        sb.append("    float2 cp = dir.xz / (dir.y + 0.12) * 1.6;\n");
        sb.append("    float cv = cn1Fbm(cp);\n");
        sb.append("    float cover = smoothstep(1.0 - sun.w, 1.0 - sun.w + 0.35, cv);\n");
        sb.append("    float cl = 0.75 + 0.25 * cn1Fbm(cp + sunDir.xz * 0.3);\n");
        sb.append("    float3 cloud = lerp(horizon.xyz * 0.82, float3(1.0, 1.0, 1.0), cl) + sun.xyz * pow(sd, 6.0) * 0.35;\n");
        sb.append("    rgb = lerp(rgb, cloud, cover * smoothstep(0.0, 0.25, dir.y));\n");
        sb.append("  }\n");
        sb.append("  rgb += sun.xyz * (pow(sd, 8.0) * 0.25 + pow(sd, 64.0) * 0.5 + smoothstep(0.9993, 0.9997, sd) * 2.0);\n");
    }

    // Cook-Torrance with a GGX distribution, Smith-Schlick geometry and
    // Schlick's Fresnel, as in the GLSL generator.
    private static void appendPbr(StringBuilder sb) {
        sb.append("  float metallic = params.y;\n");
        sb.append("  float rough = params.z;\n");
        sb.append("  float3 v = normalize(eye.xyz - input.worldPos);\n");
        sb.append("  float3 h = normalize(l + v);\n");
        sb.append("  float ndotv = max(dot(n, v), 0.001);\n");
        sb.append("  float ndoth = max(dot(n, h), 0.0);\n");
        sb.append("  float vdoth = max(dot(v, h), 0.0);\n");
        sb.append("  float3 f0 = lerp(float3(0.04, 0.04, 0.04), base.rgb, metallic);\n");
        sb.append("  float a = rough * rough;\n");
        sb.append("  float a2 = a * a;\n");
        sb.append("  float dd = ndoth * ndoth * (a2 - 1.0) + 1.0;\n");
        sb.append("  float ggx = a2 / (3.14159265 * dd * dd);\n");
        sb.append("  float k = (rough + 1.0) * (rough + 1.0) / 8.0;\n");
        sb.append("  float geo = (ndotl / (ndotl * (1.0 - k) + k)) * (ndotv / (ndotv * (1.0 - k) + k));\n");
        sb.append("  float3 fres = f0 + (1.0 - f0) * pow(1.0 - vdoth, 5.0);\n");
        sb.append("  float3 spec = ggx * geo * fres / (4.0 * max(ndotl, 0.001) * ndotv);\n");
        sb.append("  float3 kd = (float3(1.0, 1.0, 1.0) - fres) * (1.0 - metallic);\n");
        sb.append("  float3 direct = (kd * base.rgb + spec * 3.14159265) * lightColor.xyz * ndotl * shade;\n");
        sb.append("  float3 fa = f0 + (max(float3(1.0 - rough, 1.0 - rough, 1.0 - rough), f0) - f0) * pow(1.0 - ndotv, 5.0);\n");
        sb.append("  float3 ambLit = amb * (base.rgb * (1.0 - metallic) * (float3(1.0, 1.0, 1.0) - fa) + fa * (1.0 - rough * 0.7));\n");
        sb.append("  rgb = direct + ambLit;\n");
    }

    /// Returns the generated combined HLSL source.
    public String getSource() {
        return source;
    }
}
