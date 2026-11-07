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
package com.codename1.impl.ios;

import com.codename1.gpu.Environment;
import com.codename1.gpu.Material;
import com.codename1.gpu.VertexAttribute;
import com.codename1.gpu.VertexFormat;

/// Runtime generator of Metal Shading Language (MSL) source for the iOS 3D
/// backend. This mirrors the logic of the portable GLSL generator
/// (com.codename1.impl.gpu.GlslShaderGenerator) but emits a single MSL source string
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
///   lightColor, ambient, eye, shininess, then the environment, PBR and sky
///   float4s) is bound at buffer index 1 for both
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
        this(material, format, null);
    }

    /// Generates the combined MSL source for a material and vertex layout in
    /// an environment, without shadows.
    ///
    /// #### Parameters
    ///
    /// - `material`: the material describing the lighting model and inputs
    ///
    /// - `format`: the mesh vertex layout
    ///
    /// - `environment`: the fog, ambient and tone mapping in effect, or null
    ///   for the default
    public IOSMetalShaderGenerator(Material material, VertexFormat format, Environment environment) {
        this(material, format, environment, false);
    }

    /// Generates the combined MSL source for a material and vertex layout in
    /// an environment, optionally sampling the directional shadow map.
    ///
    /// #### Parameters
    ///
    /// - `material`: the material describing the lighting model and inputs
    ///
    /// - `format`: the mesh vertex layout
    ///
    /// - `environment`: the fog, ambient and tone mapping in effect, or null
    ///   for the default
    ///
    /// - `shadowed`: true if a shadow map is bound (texture 1) and the
    ///   material receives it
    public IOSMetalShaderGenerator(Material material, VertexFormat format, Environment environment,
            boolean shadowed) {
        boolean hasNormal = format.findByUsage(VertexAttribute.Usage.NORMAL) != null;
        boolean hasTexcoord = format.findByUsage(VertexAttribute.Usage.TEXCOORD) != null;
        Features f = new Features();
        f.textured = material.getTexture() != null && hasTexcoord;
        Material.Type type = material.getType();
        f.lit = material.isLit() && hasNormal;
        f.phong = type == Material.Type.PHONG && hasNormal;
        f.pbr = type == Material.Type.PBR && hasNormal;
        boolean sky = type == Material.Type.SKYBOX;
        f.fog = environment != null && environment.isFog() && !sky;
        f.sky = sky && environment != null && environment.isSky();
        f.hemisphere = f.lit && environment != null && environment.isHemisphere();
        f.toneMap = f.lit && environment != null && environment.isToneMapping();
        f.shadow = f.lit && shadowed && material.isReceiveShadows();
        f.emissive = f.lit && (material.getEmissive() & 0xffffff) != 0;
        f.hasNormal = hasNormal;
        f.hasTexcoord = hasTexcoord;
        this.source = build(format, f);
    }

    private static final class Features {
        boolean textured;
        boolean lit;
        boolean phong;
        boolean pbr;
        boolean fog;
        boolean hemisphere;
        boolean toneMap;
        boolean emissive;
        boolean sky;
        boolean shadow;
        boolean hasNormal;
        boolean hasTexcoord;
    }

    private static String build(VertexFormat format, Features f) {
        boolean lit = f.lit;
        boolean textured = f.textured;
        boolean hasNormal = f.hasNormal;
        boolean hasTexcoord = f.hasTexcoord;
        boolean world = f.lit || f.fog || f.sky;
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
        // environment block: always present, only read by the variants that need it
        sb.append("  float4 fogColor;\n");   // rgb, exposure
        sb.append("  float4 fog;\n");        // density, height falloff, base height
        sb.append("  float4 skyC;\n");
        sb.append("  float4 ground;\n");
        sb.append("  float4 pbrP;\n");       // metallic, roughness
        sb.append("  float4 emissive;\n");
        sb.append("  float4 zenith;\n");
        sb.append("  float4 horizon;\n");
        sb.append("  float4 sun;\n");        // rgb, cloud cover
        sb.append("  float4x4 shadowMatrix;\n");
        sb.append("  float4 shadowP;\n");   // bias, strength, texel size
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
        }
        if (world) {
            sb.append("  float3 worldPos;\n");
        }
        if (textured) {
            sb.append("  float2 texcoord;\n");
        }
        if (f.shadow) {
            sb.append("  float4 shadowPos;\n");
        }
        sb.append("};\n");

        // Vertex function.
        sb.append("vertex CN1VertexOut ").append(VERTEX_FUNCTION).append("(\n");
        sb.append("    CN1VertexIn in [[stage_in]],\n");
        sb.append("    constant CN1Uniforms& u [[buffer(1)]]) {\n");
        sb.append("  CN1VertexOut out;\n");
        sb.append("  float4 clip = u.mvp * float4(in.position, 1.0);\n");
        // Adapt the portable GL-convention clip space to Metal: only remap Z from
        // GL's [-w, w] to Metal's [0, w] depth range. Y is NOT flipped here -- the
        // GL backend (the reference) does not flip either, and Metal's viewport
        // already maps NDC +Y to the top of the framebuffer, so a flip would render
        // the scene upside down. Winding is handled in CN1GL3D.m (front faces are
        // counter-clockwise to match the portable convention).
        sb.append("  clip.z = (clip.z + clip.w) * 0.5;\n");
        sb.append("  out.position = clip;\n");
        if (lit) {
            sb.append("  out.worldNormal = (u.normalMatrix * float4(in.normal, 0.0)).xyz;\n");
        }
        if (world) {
            sb.append("  out.worldPos = (u.model * float4(in.position, 1.0)).xyz;\n");
        }
        if (textured) {
            sb.append("  out.texcoord = in.texcoord;\n");
        }
        if (f.shadow) {
            sb.append("  out.shadowPos = u.shadowMatrix * float4(out.worldPos, 1.0);\n");
        }
        sb.append("  return out;\n");
        sb.append("}\n");

        // Fragment helpers.
        if (f.sky) {
            appendSkyNoise(sb);
        }
        if (f.shadow) {
            appendShadowLookup(sb);
        }
        if (f.toneMap) {
            // Narkowicz's fit of the ACES filmic curve
            sb.append("float3 aces(float3 x) {\n");
            sb.append("  return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);\n");
            sb.append("}\n");
        }
        // Fragment function.
        sb.append("fragment float4 ").append(FRAGMENT_FUNCTION).append("(\n");
        sb.append("    CN1VertexOut in [[stage_in]],\n");
        sb.append("    constant CN1Uniforms& u [[buffer(1)]]");
        if (textured) {
            sb.append(",\n    texture2d<float> tex [[texture(0)]],\n");
            sb.append("    sampler texSampler [[sampler(0)]]");
        }
        if (f.shadow) {
            sb.append(",\n    depth2d<float> shadowMap [[texture(1)]]");
        }
        sb.append(") {\n");
        sb.append("  float4 base = u.color;\n");
        if (textured) {
            sb.append("  base = base * tex.sample(texSampler, in.texcoord);\n");
        }
        sb.append("  float3 rgb = base.rgb;\n");
        if (f.sky) {
            appendSky(sb);
        }
        if (lit) {
            sb.append("  float3 n = normalize(in.worldNormal);\n");
            sb.append("  float3 l = normalize(-u.lightDir.xyz);\n");
            sb.append("  float ndotl = max(dot(n, l), 0.0);\n");
            if (f.hemisphere) {
                sb.append("  float3 ambient = mix(u.ground.xyz, u.skyC.xyz, n.y * 0.5 + 0.5);\n");
            } else {
                sb.append("  float3 ambient = u.ambient.xyz;\n");
            }
            sb.append(f.shadow ? "  float shade = cn1ShadowAt(in.shadowPos, n, l, u, shadowMap);\n"
                    : "  float shade = 1.0;\n");
            if (f.pbr) {
                appendPbr(sb);
            } else {
                sb.append("  rgb = base.rgb * (ambient + u.lightColor.xyz * ndotl * shade);\n");
                if (f.phong) {
                    sb.append("  if (ndotl > 0.0) {\n");
                    sb.append("    float3 v = normalize(u.eye.xyz - in.worldPos);\n");
                    sb.append("    float3 h = normalize(l + v);\n");
                    sb.append("    float spec = pow(max(dot(n, h), 0.0), u.shininess);\n");
                    sb.append("    rgb += u.lightColor.xyz * spec * shade;\n");
                    sb.append("  }\n");
                }
            }
            if (f.emissive) {
                sb.append("  rgb += u.emissive.xyz;\n");
            }
            if (f.toneMap) {
                sb.append("  rgb = aces(rgb * u.fogColor.w);\n");
            }
        }
        if (f.fog) {
            sb.append("  float dist = length(in.worldPos - u.eye.xyz);\n");
            sb.append("  float fd = u.fog.x * dist;\n");
            sb.append("  float height = exp(-u.fog.y * max(in.worldPos.y - u.fog.z, 0.0));\n");
            sb.append("  float fogAmount = clamp((1.0 - exp(-fd * fd)) * height, 0.0, 1.0);\n");
            sb.append("  rgb = mix(rgb, u.fogColor.xyz, fogAmount);\n");
        }
        sb.append("  return float4(rgb, base.a);\n");
        sb.append("}\n");
        return sb.toString();
    }

    // 3x3 percentage closer filtering against the depth texture, comparing by
    // hand exactly as the GLSL does. The map was rendered with +Y at the top of
    // the texture, so the lookup flips v.
    private static void appendShadowLookup(StringBuilder sb) {
        sb.append("float cn1ShadowAt(float4 sp, float3 n, float3 l, constant CN1Uniforms& u, depth2d<float> map) {\n");
        sb.append("  constexpr sampler ss(coord::normalized, address::clamp_to_edge, filter::nearest);\n");
        sb.append("  float3 p = sp.xyz / sp.w * 0.5 + 0.5;\n");
        sb.append("  if (p.x < 0.0 || p.x > 1.0 || p.y < 0.0 || p.y > 1.0 || p.z > 1.0) {\n");
        sb.append("    return 1.0;\n  }\n");
        sb.append("  float bias = u.shadowP.x * (1.0 + 2.0 * (1.0 - max(dot(n, l), 0.0)));\n");
        sb.append("  float lit = 0.0;\n");
        sb.append("  for (int y = -1; y <= 1; y++) {\n");
        sb.append("    for (int x = -1; x <= 1; x++) {\n");
        sb.append("      float d = map.sample(ss, float2(p.x, 1.0 - p.y) + float2(float(x), float(y)) * u.shadowP.z);\n");
        sb.append("      lit += p.z - bias > d ? 0.0 : 1.0;\n");
        sb.append("    }\n  }\n");
        sb.append("  return 1.0 - u.shadowP.y * (1.0 - lit / 9.0);\n");
        sb.append("}\n");
    }

    /// Returns the MSL of the shadow caster pipeline: a vertex function that
    /// transforms by the matrix in the first uniform slot (the shadow matrix
    /// times the model matrix) and no fragment function, so only depth is
    /// written into the shadow map.
    ///
    /// #### Parameters
    ///
    /// - `format`: the mesh vertex layout
    ///
    /// #### Returns
    ///
    /// the MSL source with the `cn1_vertex_main` entry point
    public static String shadowSource(VertexFormat format) {
        int posOff = offsetOf(format, VertexAttribute.Usage.POSITION);
        StringBuilder sb = new StringBuilder();
        sb.append("#include <metal_stdlib>\n");
        sb.append("using namespace metal;\n");
        sb.append("struct CN1ShadowUniforms {\n  float4x4 mvp;\n};\n");
        sb.append("struct CN1ShadowIn {\n  float3 position [[attribute(").append(posOff).append(")]];\n};\n");
        sb.append("vertex float4 ").append(VERTEX_FUNCTION).append("(\n");
        sb.append("    CN1ShadowIn in [[stage_in]],\n");
        sb.append("    constant CN1ShadowUniforms& u [[buffer(1)]]) {\n");
        sb.append("  float4 clip = u.mvp * float4(in.position, 1.0);\n");
        sb.append("  clip.z = (clip.z + clip.w) * 0.5;\n");
        sb.append("  return clip;\n");
        sb.append("}\n");
        return sb.toString();
    }

    /// Returns the MSL of the post processing chain: one full screen vertex
    /// function (`cn1_post_vertex`, a triangle strip of four vertices generated
    /// from the vertex id) and the bright, blur and composite fragment
    /// functions. Each fragment function reads one `float4` (bright, blur) or a
    /// `CN1PostU` (composite) at fragment buffer 0 and the scene at texture 0,
    /// the bloom at texture 1. Mirrors the GLSL passes term for term.
    ///
    /// #### Returns
    ///
    /// the MSL source
    public static String postSource() {
        StringBuilder sb = new StringBuilder();
        sb.append("#include <metal_stdlib>\n");
        sb.append("using namespace metal;\n");
        sb.append("struct CN1PostV {\n  float4 position [[position]];\n  float2 uv;\n};\n");
        sb.append("struct CN1PostU {\n");
        sb.append("  float4 a;\n");     // bloom intensity, contrast, saturation, vignette
        sb.append("  float4 lift;\n");
        sb.append("  float4 gain;\n");
        sb.append("  float4 b;\n");     // grain, time, width, height
        sb.append("};\n");
        sb.append("vertex CN1PostV cn1_post_vertex(uint vid [[vertex_id]]) {\n");
        sb.append("  float2 p = float2((vid & 1u) != 0u ? 1.0 : -1.0, (vid & 2u) != 0u ? 1.0 : -1.0);\n");
        sb.append("  CN1PostV o;\n");
        sb.append("  o.position = float4(p, 0.0, 1.0);\n");
        // texture space has v = 0 at the top, clip space has +Y at the top
        sb.append("  o.uv = float2(p.x * 0.5 + 0.5, 0.5 - p.y * 0.5);\n");
        sb.append("  return o;\n");
        sb.append("}\n");
        sb.append("constexpr sampler cn1PostS(coord::normalized, address::clamp_to_edge, filter::linear);\n");
        // a.x = threshold, a.yz = one source texel in uv
        sb.append("fragment float4 cn1_bright_fragment(CN1PostV in [[stage_in]], constant float4& a [[buffer(0)]],\n");
        sb.append("    texture2d<float> scene [[texture(0)]]) {\n");
        sb.append("  float2 t = a.yz;\n");
        sb.append("  float3 c = (scene.sample(cn1PostS, in.uv + t * float2(-0.5, -0.5)).rgb\n");
        sb.append("      + scene.sample(cn1PostS, in.uv + t * float2(0.5, -0.5)).rgb\n");
        sb.append("      + scene.sample(cn1PostS, in.uv + t * float2(-0.5, 0.5)).rgb\n");
        sb.append("      + scene.sample(cn1PostS, in.uv + t * float2(0.5, 0.5)).rgb) * 0.25;\n");
        sb.append("  float peak = max(c.r, max(c.g, c.b));\n");
        sb.append("  float over = max(peak - a.x, 0.0) / max(1.0 - a.x, 0.0001);\n");
        sb.append("  return float4(c * (over / max(peak, 0.0001)), 1.0);\n");
        sb.append("}\n");
        // a.xy = one texel along the blur direction in uv
        sb.append("fragment float4 cn1_blur_fragment(CN1PostV in [[stage_in]], constant float4& a [[buffer(0)]],\n");
        sb.append("    texture2d<float> scene [[texture(0)]]) {\n");
        sb.append("  float3 c = scene.sample(cn1PostS, in.uv).rgb * 0.2270270;\n");
        sb.append("  c += scene.sample(cn1PostS, in.uv + a.xy * 1.3846154).rgb * 0.3162162;\n");
        sb.append("  c += scene.sample(cn1PostS, in.uv - a.xy * 1.3846154).rgb * 0.3162162;\n");
        sb.append("  c += scene.sample(cn1PostS, in.uv + a.xy * 3.2307692).rgb * 0.0702703;\n");
        sb.append("  c += scene.sample(cn1PostS, in.uv - a.xy * 3.2307692).rgb * 0.0702703;\n");
        sb.append("  return float4(c, 1.0);\n");
        sb.append("}\n");
        sb.append("fragment float4 cn1_composite_fragment(CN1PostV in [[stage_in]], constant CN1PostU& u [[buffer(0)]],\n");
        sb.append("    texture2d<float> scene [[texture(0)]], texture2d<float> bloom [[texture(1)]]) {\n");
        sb.append("  float3 c = scene.sample(cn1PostS, in.uv).rgb;\n");
        sb.append("  c += bloom.sample(cn1PostS, in.uv).rgb * u.a.x;\n");
        sb.append("  c = (c - 0.5) * u.a.y + 0.5;\n");
        sb.append("  float luma = dot(c, float3(0.2126, 0.7152, 0.0722));\n");
        sb.append("  c = mix(float3(luma), c, u.a.z);\n");
        sb.append("  c = clamp(c, 0.0, 1.0);\n");
        sb.append("  c = (u.lift.xyz + c * (1.0 - u.lift.xyz)) * u.gain.xyz;\n");
        sb.append("  float2 d = in.uv - 0.5;\n");
        sb.append("  c *= 1.0 - u.a.w * smoothstep(0.25, 0.75, dot(d, d) * 2.0);\n");
        sb.append("  float2 px = floor(in.uv * u.b.zw);\n");
        sb.append("  float noise = fract(sin(dot(px + fract(u.b.y * 7.0) * 61.0, float2(12.9898, 78.233))) * 43758.5453);\n");
        sb.append("  c += (noise - 0.5) * u.b.x;\n");
        sb.append("  return float4(c, 1.0);\n");
        sb.append("}\n");
        return sb.toString();
    }

    // Value noise and four octaves of it, for the clouds.
    private static void appendSkyNoise(StringBuilder sb) {
        sb.append("float cn1Hash(float2 p) {\n");
        sb.append("  return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);\n");
        sb.append("}\n");
        sb.append("float cn1Noise(float2 p) {\n");
        sb.append("  float2 i = floor(p);\n");
        sb.append("  float2 fr = fract(p);\n");
        sb.append("  fr = fr * fr * (3.0 - 2.0 * fr);\n");
        sb.append("  return mix(mix(cn1Hash(i), cn1Hash(i + float2(1.0, 0.0)), fr.x),\n");
        sb.append("             mix(cn1Hash(i + float2(0.0, 1.0)), cn1Hash(i + float2(1.0, 1.0)), fr.x), fr.y);\n");
        sb.append("}\n");
        sb.append("float cn1Fbm(float2 p) {\n");
        sb.append("  float v = 0.0;\n");
        sb.append("  float a = 0.5;\n");
        sb.append("  for (int i = 0; i < 4; i++) {\n");
        sb.append("    v += a * cn1Noise(p);\n");
        sb.append("    p = p * 2.03 + float2(1.7, 9.2);\n");
        sb.append("    a *= 0.5;\n");
        sb.append("  }\n");
        sb.append("  return v;\n");
        sb.append("}\n");
    }

    // The open sky from the direction the pixel is seen in; u.sun is the sun
    // colour and the cloud cover.
    private static void appendSky(StringBuilder sb) {
        sb.append("  float3 dir = normalize(in.worldPos - u.eye.xyz);\n");
        sb.append("  float up = clamp(dir.y, 0.0, 1.0);\n");
        sb.append("  rgb = mix(u.horizon.xyz, u.zenith.xyz, pow(up, 0.55));\n");
        sb.append("  float3 sunDir = normalize(-u.lightDir.xyz);\n");
        sb.append("  float sd = max(dot(dir, sunDir), 0.0);\n");
        sb.append("  if (dir.y > 0.0 && u.sun.w > 0.0) {\n");
        sb.append("    float2 cp = dir.xz / (dir.y + 0.12) * 1.6;\n");
        sb.append("    float c = cn1Fbm(cp);\n");
        sb.append("    float cover = smoothstep(1.0 - u.sun.w, 1.0 - u.sun.w + 0.35, c);\n");
        sb.append("    float cl = 0.75 + 0.25 * cn1Fbm(cp + sunDir.xz * 0.3);\n");
        sb.append("    float3 cloud = mix(u.horizon.xyz * 0.82, float3(1.0), cl) + u.sun.xyz * pow(sd, 6.0) * 0.35;\n");
        sb.append("    rgb = mix(rgb, cloud, cover * smoothstep(0.0, 0.25, dir.y));\n");
        sb.append("  }\n");
        sb.append("  rgb += u.sun.xyz * (pow(sd, 8.0) * 0.25 + pow(sd, 64.0) * 0.5 + smoothstep(0.9993, 0.9997, sd) * 2.0);\n");
    }

    // Cook-Torrance with a GGX distribution, Smith-Schlick geometry and
    // Schlick's Fresnel, matching the GLSL generator term for term.
    private static void appendPbr(StringBuilder sb) {
        sb.append("  float metallic = u.pbrP.x;\n");
        sb.append("  float rough = u.pbrP.y;\n");
        sb.append("  float3 v = normalize(u.eye.xyz - in.worldPos);\n");
        sb.append("  float3 h = normalize(l + v);\n");
        sb.append("  float ndotv = max(dot(n, v), 0.001);\n");
        sb.append("  float ndoth = max(dot(n, h), 0.0);\n");
        sb.append("  float vdoth = max(dot(v, h), 0.0);\n");
        sb.append("  float3 f0 = mix(float3(0.04), base.rgb, metallic);\n");
        sb.append("  float a = rough * rough;\n");
        sb.append("  float a2 = a * a;\n");
        sb.append("  float dd = ndoth * ndoth * (a2 - 1.0) + 1.0;\n");
        sb.append("  float ggx = a2 / (3.14159265 * dd * dd);\n");
        sb.append("  float k = (rough + 1.0) * (rough + 1.0) / 8.0;\n");
        sb.append("  float geo = (ndotl / (ndotl * (1.0 - k) + k)) * (ndotv / (ndotv * (1.0 - k) + k));\n");
        sb.append("  float3 fres = f0 + (1.0 - f0) * pow(1.0 - vdoth, 5.0);\n");
        sb.append("  float3 spec = ggx * geo * fres / (4.0 * max(ndotl, 0.001) * ndotv);\n");
        sb.append("  float3 kd = (float3(1.0) - fres) * (1.0 - metallic);\n");
        sb.append("  float3 direct = (kd * base.rgb + spec * 3.14159265) * u.lightColor.xyz * ndotl * shade;\n");
        sb.append("  float3 fa = f0 + (max(float3(1.0 - rough), f0) - f0) * pow(1.0 - ndotv, 5.0);\n");
        sb.append("  float3 amb = ambient * (base.rgb * (1.0 - metallic) * (float3(1.0) - fa) + fa * (1.0 - rough * 0.7));\n");
        sb.append("  rgb = direct + amb;\n");
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
