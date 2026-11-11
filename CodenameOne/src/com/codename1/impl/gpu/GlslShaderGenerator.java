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
package com.codename1.impl.gpu;

import com.codename1.gpu.Environment;
import com.codename1.gpu.Material;
import com.codename1.gpu.VertexAttribute;
import com.codename1.gpu.VertexFormat;

/// Generates portable GLSL ES 1.00 vertex and fragment shader source for a given
/// `Material` and `VertexFormat`. This is the "engine-managed shader" code path
/// shared by every OpenGL ES class backend (Android OpenGL ES and the browser's
/// WebGL, which share the same shading language). Applications never call this
/// directly; it exists so the platform backends do not each reimplement shader
/// emission. The iOS backend has an equivalent Metal generator.
///
/// The generated programs use a fixed naming contract that backends rely on when
/// binding attributes and uniforms:
///
/// - attributes: `a_position` (vec3), `a_normal` (vec3), `a_texcoord` (vec2)
/// - uniforms: `u_mvp`, `u_model`, `u_normalMatrix` (mat4); `u_color` (vec4);
///   `u_texture` (sampler2D); `u_lightDir`, `u_lightColor`, `u_ambient`,
///   `u_eye` (vec3); `u_shininess` (float)
/// - environment: `u_fogColor` (vec3), `u_fog` (vec3: density, height falloff,
///   base height), `u_sky`, `u_ground` (vec3), `u_exposure` (float)
/// - physically based: `u_pbr` (vec2: metallic, roughness), `u_emissive` (vec3)
/// - shadows: `u_shadowMatrix` (mat4), `u_shadowMap` (sampler2D, texture unit 1),
///   `u_shadow` (vec3: bias, strength, texel size)
///
/// Shadow maps store depth packed into RGBA8 (`packDepth`), so they need no
/// depth texture or float render target and work on every GLES2 and WebGL1
/// device.
///
/// The post processing programs (`postVertex` with `brightFragment`,
/// `blurFragment` and `compositeFragment`) draw a full screen quad whose
/// `a_position` is in clip space and read `u_scene` (unit 0) and `u_bloom`
/// (unit 1).
public final class GlslShaderGenerator {
    /// The attribute name bound to vertex positions.
    public static final String A_POSITION = "a_position";
    /// The attribute name bound to vertex normals.
    public static final String A_NORMAL = "a_normal";
    /// The attribute name bound to vertex texture coordinates.
    public static final String A_TEXCOORD = "a_texcoord";

    private static final String HIGHP = "#ifdef GL_FRAGMENT_PRECISION_HIGH\n"
            + "precision highp float;\n#else\nprecision mediump float;\n#endif\n";

    private static final String PACK = "vec4 packDepth(float d) {\n"
            + "  vec4 e = fract(d * vec4(1.0, 255.0, 65025.0, 16581375.0));\n"
            + "  e -= e.yzww * vec4(1.0 / 255.0, 1.0 / 255.0, 1.0 / 255.0, 0.0);\n"
            + "  return e;\n}\n";

    private static final String UNPACK = "float unpackDepth(vec4 c) {\n"
            + "  return dot(c, vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 16581375.0));\n}\n";

    private final String vertexSource;
    private final String fragmentSource;

    /// Generates the shader pair for a material and vertex layout in the
    /// default environment, without shadows.
    ///
    /// #### Parameters
    ///
    /// - `material`: the material describing the lighting model and inputs
    ///
    /// - `format`: the mesh vertex layout
    public GlslShaderGenerator(Material material, VertexFormat format) {
        this(material, format, null, false);
    }

    /// Generates the shader pair for a material and vertex layout.
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
    /// - `shadowed`: true if a shadow map is bound and the material receives it
    public GlslShaderGenerator(Material material, VertexFormat format, Environment environment,
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
        this.vertexSource = buildVertex(f);
        this.fragmentSource = buildFragment(f);
    }

    /// Returns the part of a backend's program cache key that this generator
    /// adds beyond `Material.getShaderKey`.
    ///
    /// #### Parameters
    ///
    /// - `environment`: the environment in effect, or null
    ///
    /// - `shadowed`: true if a shadow map is bound
    ///
    /// #### Returns
    ///
    /// the key suffix
    public static String variantKey(Environment environment, boolean shadowed) {
        return (environment == null ? "fht" : environment.getShaderKey()) + (shadowed ? "S" : "s");
    }

    private static final class Features {
        boolean textured;
        boolean lit;
        boolean phong;
        boolean pbr;
        boolean fog;
        boolean hemisphere;
        boolean toneMap;
        boolean shadow;
        boolean emissive;
        boolean sky;
    }

    private static String buildVertex(Features f) {
        boolean world = f.lit || f.fog || f.sky;
        StringBuilder sb = new StringBuilder();
        sb.append("attribute vec3 ").append(A_POSITION).append(";\n");
        if (f.lit) {
            sb.append("attribute vec3 ").append(A_NORMAL).append(";\n");
        }
        if (f.textured) {
            sb.append("attribute vec2 ").append(A_TEXCOORD).append(";\n");
        }
        sb.append("uniform mat4 u_mvp;\n");
        if (world) {
            sb.append("uniform mat4 u_model;\n");
            sb.append("varying vec3 v_worldPos;\n");
        }
        if (f.lit) {
            sb.append("uniform mat4 u_normalMatrix;\n");
            sb.append("varying vec3 v_normal;\n");
        }
        if (f.shadow) {
            sb.append("uniform mat4 u_shadowMatrix;\n");
            sb.append("varying vec4 v_shadowPos;\n");
        }
        if (f.textured) {
            sb.append("varying vec2 v_texcoord;\n");
        }
        sb.append("void main() {\n");
        if (world) {
            sb.append("  vec4 world = u_model * vec4(").append(A_POSITION).append(", 1.0);\n");
            sb.append("  v_worldPos = world.xyz;\n");
        }
        if (f.lit) {
            sb.append("  v_normal = (u_normalMatrix * vec4(").append(A_NORMAL).append(", 0.0)).xyz;\n");
        }
        if (f.shadow) {
            sb.append("  v_shadowPos = u_shadowMatrix * world;\n");
        }
        if (f.textured) {
            sb.append("  v_texcoord = ").append(A_TEXCOORD).append(";\n");
        }
        sb.append("  gl_Position = u_mvp * vec4(").append(A_POSITION).append(", 1.0);\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String buildFragment(Features f) {
        boolean world = f.lit || f.fog || f.sky;
        StringBuilder sb = new StringBuilder();
        sb.append(f.shadow ? HIGHP : "precision mediump float;\n");
        sb.append("uniform vec4 u_color;\n");
        if (f.textured) {
            sb.append("uniform sampler2D u_texture;\n");
            sb.append("varying vec2 v_texcoord;\n");
        }
        if (world) {
            sb.append("varying vec3 v_worldPos;\n");
            sb.append("uniform vec3 u_eye;\n");
        }
        if (f.lit) {
            sb.append("uniform vec3 u_lightDir;\n");
            sb.append("uniform vec3 u_lightColor;\n");
            sb.append("uniform vec3 u_ambient;\n");
            sb.append("varying vec3 v_normal;\n");
        }
        if (f.phong) {
            sb.append("uniform float u_shininess;\n");
        }
        if (f.pbr) {
            sb.append("uniform vec2 u_pbr;\n");
        }
        if (f.emissive) {
            sb.append("uniform vec3 u_emissive;\n");
        }
        if (f.hemisphere) {
            sb.append("uniform vec3 u_sky;\n");
            sb.append("uniform vec3 u_ground;\n");
        }
        if (f.toneMap) {
            sb.append("uniform float u_exposure;\n");
        }
        if (f.fog) {
            sb.append("uniform vec3 u_fogColor;\n");
            sb.append("uniform vec3 u_fog;\n");
        }
        if (f.shadow) {
            sb.append("uniform sampler2D u_shadowMap;\n");
            sb.append("uniform vec3 u_shadow;\n");
            sb.append("varying vec4 v_shadowPos;\n");
            sb.append(UNPACK);
            sb.append("float shadowAt(vec3 n, vec3 l) {\n");
            sb.append("  vec3 p = v_shadowPos.xyz / v_shadowPos.w * 0.5 + 0.5;\n");
            sb.append("  if (p.x < 0.0 || p.x > 1.0 || p.y < 0.0 || p.y > 1.0 || p.z > 1.0) {\n");
            sb.append("    return 1.0;\n  }\n");
            // a steeper surface needs a larger offset to stay off its own map
            sb.append("  float bias = u_shadow.x * (1.0 + 2.0 * (1.0 - max(dot(n, l), 0.0)));\n");
            sb.append("  float lit = 0.0;\n");
            sb.append("  for (int y = -1; y <= 1; y++) {\n");
            sb.append("    for (int x = -1; x <= 1; x++) {\n");
            sb.append("      float d = unpackDepth(texture2D(u_shadowMap, p.xy + vec2(float(x), float(y)) * u_shadow.z));\n");
            sb.append("      lit += p.z - bias > d ? 0.0 : 1.0;\n");
            sb.append("    }\n  }\n");
            sb.append("  return 1.0 - u_shadow.y * (1.0 - lit / 9.0);\n");
            sb.append("}\n");
        }
        if (f.sky) {
            sb.append("uniform vec3 u_lightDir;\n");
            sb.append("uniform vec3 u_zenith;\n");
            sb.append("uniform vec3 u_horizon;\n");
            sb.append("uniform vec4 u_sun;\n");
            appendSkyNoise(sb);
        }
        if (f.toneMap) {
            // Narkowicz's fit of the ACES filmic curve
            sb.append("vec3 aces(vec3 x) {\n");
            sb.append("  return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);\n");
            sb.append("}\n");
        }
        sb.append("void main() {\n");
        sb.append("  vec4 base = u_color;\n");
        if (f.textured) {
            sb.append("  base = base * texture2D(u_texture, v_texcoord);\n");
        }
        sb.append("  vec3 rgb = base.rgb;\n");
        if (f.sky) {
            appendSky(sb);
        }
        if (f.lit) {
            sb.append("  vec3 n = normalize(v_normal);\n");
            sb.append("  vec3 l = normalize(-u_lightDir);\n");
            sb.append("  float ndotl = max(dot(n, l), 0.0);\n");
            if (f.hemisphere) {
                sb.append("  vec3 ambient = mix(u_ground, u_sky, n.y * 0.5 + 0.5);\n");
            } else {
                sb.append("  vec3 ambient = u_ambient;\n");
            }
            sb.append(f.shadow ? "  float shade = shadowAt(n, l);\n" : "  float shade = 1.0;\n");
            if (f.pbr) {
                appendPbr(sb);
            } else {
                sb.append("  rgb = base.rgb * (ambient + u_lightColor * ndotl * shade);\n");
                if (f.phong) {
                    sb.append("  if (ndotl > 0.0) {\n");
                    sb.append("    vec3 v = normalize(u_eye - v_worldPos);\n");
                    sb.append("    vec3 h = normalize(l + v);\n");
                    sb.append("    float spec = pow(max(dot(n, h), 0.0), u_shininess);\n");
                    sb.append("    rgb += u_lightColor * spec * shade;\n");
                    sb.append("  }\n");
                }
            }
            if (f.emissive) {
                sb.append("  rgb += u_emissive;\n");
            }
            if (f.toneMap) {
                sb.append("  rgb = aces(rgb * u_exposure);\n");
            }
        }
        if (f.fog) {
            sb.append("  float dist = length(v_worldPos - u_eye);\n");
            sb.append("  float fd = u_fog.x * dist;\n");
            sb.append("  float height = exp(-u_fog.y * max(v_worldPos.y - u_fog.z, 0.0));\n");
            sb.append("  float fogAmount = clamp((1.0 - exp(-fd * fd)) * height, 0.0, 1.0);\n");
            sb.append("  rgb = mix(rgb, u_fogColor, fogAmount);\n");
        }
        sb.append("  gl_FragColor = vec4(rgb, base.a);\n");
        sb.append("}\n");
        return sb.toString();
    }

    // Value noise and four octaves of it, for the clouds.
    private static void appendSkyNoise(StringBuilder sb) {
        sb.append("float hash(vec2 p) {\n");
        sb.append("  return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);\n");
        sb.append("}\n");
        sb.append("float noise(vec2 p) {\n");
        sb.append("  vec2 i = floor(p);\n");
        sb.append("  vec2 u = fract(p);\n");
        sb.append("  u = u * u * (3.0 - 2.0 * u);\n");
        sb.append("  return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),\n");
        sb.append("             mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);\n");
        sb.append("}\n");
        sb.append("float fbm(vec2 p) {\n");
        sb.append("  float v = 0.0;\n");
        sb.append("  float a = 0.5;\n");
        sb.append("  for (int i = 0; i < 4; i++) {\n");
        sb.append("    v += a * noise(p);\n");
        sb.append("    p = p * 2.03 + vec2(1.7, 9.2);\n");
        sb.append("    a *= 0.5;\n");
        sb.append("  }\n");
        sb.append("  return v;\n");
        sb.append("}\n");
    }

    // The open sky, from the direction the pixel is seen in: a horizon to
    // zenith gradient, clouds projected onto a plane overhead and lit from
    // the sun's side, and the sun's disc with a wide glow around it.
    // u_sun is the sun colour and the cloud cover.
    private static void appendSky(StringBuilder sb) {
        sb.append("  vec3 dir = normalize(v_worldPos - u_eye);\n");
        sb.append("  float up = clamp(dir.y, 0.0, 1.0);\n");
        sb.append("  rgb = mix(u_horizon, u_zenith, pow(up, 0.55));\n");
        sb.append("  vec3 sunDir = normalize(-u_lightDir);\n");
        sb.append("  float sd = max(dot(dir, sunDir), 0.0);\n");
        sb.append("  if (dir.y > 0.0 && u_sun.w > 0.0) {\n");
        sb.append("    vec2 cp = dir.xz / (dir.y + 0.12) * 1.6;\n");
        sb.append("    float c = fbm(cp);\n");
        sb.append("    float cover = smoothstep(1.0 - u_sun.w, 1.0 - u_sun.w + 0.35, c);\n");
        sb.append("    float lit = 0.75 + 0.25 * fbm(cp + sunDir.xz * 0.3);\n");
        sb.append("    vec3 cloud = mix(u_horizon * 0.82, vec3(1.0), lit) + u_sun.rgb * pow(sd, 6.0) * 0.35;\n");
        // clouds thin toward the horizon, where they would bunch into stripes
        sb.append("    rgb = mix(rgb, cloud, cover * smoothstep(0.0, 0.25, dir.y));\n");
        sb.append("  }\n");
        sb.append("  rgb += u_sun.rgb * (pow(sd, 8.0) * 0.25 + pow(sd, 64.0) * 0.5 + smoothstep(0.9993, 0.9997, sd) * 2.0);\n");
    }

    // Cook-Torrance with a GGX distribution, Smith-Schlick geometry and
    // Schlick's Fresnel: the metallic/roughness model glTF assets are authored
    // for. The light colour is scaled by pi so a white light on a white
    // dielectric gives the same brightness as LAMBERT does.
    private static void appendPbr(StringBuilder sb) {
        sb.append("  float metallic = u_pbr.x;\n");
        sb.append("  float rough = u_pbr.y;\n");
        sb.append("  vec3 v = normalize(u_eye - v_worldPos);\n");
        sb.append("  vec3 h = normalize(l + v);\n");
        sb.append("  float ndotv = max(dot(n, v), 0.001);\n");
        sb.append("  float ndoth = max(dot(n, h), 0.0);\n");
        sb.append("  float vdoth = max(dot(v, h), 0.0);\n");
        sb.append("  vec3 f0 = mix(vec3(0.04), base.rgb, metallic);\n");
        sb.append("  float a = rough * rough;\n");
        sb.append("  float a2 = a * a;\n");
        sb.append("  float dd = ndoth * ndoth * (a2 - 1.0) + 1.0;\n");
        sb.append("  float ggx = a2 / (3.14159265 * dd * dd);\n");
        sb.append("  float k = (rough + 1.0) * (rough + 1.0) / 8.0;\n");
        sb.append("  float geo = (ndotl / (ndotl * (1.0 - k) + k)) * (ndotv / (ndotv * (1.0 - k) + k));\n");
        sb.append("  vec3 fres = f0 + (1.0 - f0) * pow(1.0 - vdoth, 5.0);\n");
        sb.append("  vec3 spec = ggx * geo * fres / (4.0 * max(ndotl, 0.001) * ndotv);\n");
        sb.append("  vec3 kd = (vec3(1.0) - fres) * (1.0 - metallic);\n");
        sb.append("  vec3 direct = (kd * base.rgb + spec * 3.14159265) * u_lightColor * ndotl * shade;\n");
        // ambient: diffuse from the hemisphere plus a rough stand-in for the
        // reflection of it, so metal is not black in shadow
        sb.append("  vec3 fa = f0 + (max(vec3(1.0 - rough), f0) - f0) * pow(1.0 - ndotv, 5.0);\n");
        sb.append("  vec3 amb = ambient * (base.rgb * (1.0 - metallic) * (vec3(1.0) - fa) + fa * (1.0 - rough * 0.7));\n");
        sb.append("  rgb = direct + amb;\n");
    }

    /// Returns the vertex shader that renders shadow casters into the shadow
    /// map. The backend sets `u_mvp` to the shadow matrix times the model
    /// matrix.
    public static String shadowVertex() {
        return "attribute vec3 " + A_POSITION + ";\n"
                + "uniform mat4 u_mvp;\n"
                + "varying float v_depth;\n"
                + "void main() {\n"
                + "  gl_Position = u_mvp * vec4(" + A_POSITION + ", 1.0);\n"
                + "  v_depth = gl_Position.z / gl_Position.w * 0.5 + 0.5;\n"
                + "}\n";
    }

    /// Returns the fragment shader that writes packed depth into the shadow map.
    public static String shadowFragment() {
        return HIGHP + PACK
                + "varying float v_depth;\n"
                + "void main() {\n"
                + "  gl_FragColor = packDepth(clamp(v_depth, 0.0, 1.0));\n"
                + "}\n";
    }

    /// Returns the vertex shader of every post processing pass: a full screen
    /// quad with `a_position` in clip space.
    public static String postVertex() {
        return "attribute vec3 " + A_POSITION + ";\n"
                + "varying vec2 v_uv;\n"
                + "void main() {\n"
                + "  v_uv = " + A_POSITION + ".xy * 0.5 + 0.5;\n"
                + "  gl_Position = vec4(" + A_POSITION + ".xy, 0.0, 1.0);\n"
                + "}\n";
    }

    /// Returns the bloom bright pass: what is brighter than `u_threshold`,
    /// rescaled to start from black. Uniforms: `u_scene`, `u_threshold` and
    /// `u_texel` (vec2, one source texel in uv), which it uses to average a
    /// 2x2 block while downsampling.
    public static String brightFragment() {
        return "precision mediump float;\n"
                + "uniform sampler2D u_scene;\n"
                + "uniform float u_threshold;\n"
                + "uniform vec2 u_texel;\n"
                + "varying vec2 v_uv;\n"
                + "void main() {\n"
                + "  vec3 c = (texture2D(u_scene, v_uv + u_texel * vec2(-0.5, -0.5)).rgb\n"
                + "      + texture2D(u_scene, v_uv + u_texel * vec2(0.5, -0.5)).rgb\n"
                + "      + texture2D(u_scene, v_uv + u_texel * vec2(-0.5, 0.5)).rgb\n"
                + "      + texture2D(u_scene, v_uv + u_texel * vec2(0.5, 0.5)).rgb) * 0.25;\n"
                + "  float peak = max(c.r, max(c.g, c.b));\n"
                + "  float over = max(peak - u_threshold, 0.0) / max(1.0 - u_threshold, 0.0001);\n"
                + "  gl_FragColor = vec4(c * (over / max(peak, 0.0001)), 1.0);\n"
                + "}\n";
    }

    /// Returns one direction of a separable 9 tap Gaussian blur. Uniforms:
    /// `u_scene` and `u_dir` (vec2, one texel along the blur direction in uv).
    public static String blurFragment() {
        return "precision mediump float;\n"
                + "uniform sampler2D u_scene;\n"
                + "uniform vec2 u_dir;\n"
                + "varying vec2 v_uv;\n"
                + "void main() {\n"
                + "  vec3 c = texture2D(u_scene, v_uv).rgb * 0.2270270;\n"
                + "  c += texture2D(u_scene, v_uv + u_dir * 1.3846154).rgb * 0.3162162;\n"
                + "  c += texture2D(u_scene, v_uv - u_dir * 1.3846154).rgb * 0.3162162;\n"
                + "  c += texture2D(u_scene, v_uv + u_dir * 3.2307692).rgb * 0.0702703;\n"
                + "  c += texture2D(u_scene, v_uv - u_dir * 3.2307692).rgb * 0.0702703;\n"
                + "  gl_FragColor = vec4(c, 1.0);\n"
                + "}\n";
    }

    /// Returns the final pass: the scene plus bloom, colour graded, with
    /// vignette and grain. Uniforms: `u_scene` (unit 0), `u_bloom` (unit 1),
    /// `u_bloomIntensity`, `u_grade` (vec2: contrast, saturation),
    /// `u_lift` and `u_gain` (vec3), `u_vignette`, `u_grain`, `u_time` (float)
    /// and `u_resolution` (vec2, pixels).
    public static String compositeFragment() {
        return "precision mediump float;\n"
                + "uniform sampler2D u_scene;\n"
                + "uniform sampler2D u_bloom;\n"
                + "uniform float u_bloomIntensity;\n"
                + "uniform vec2 u_grade;\n"
                + "uniform vec3 u_lift;\n"
                + "uniform vec3 u_gain;\n"
                + "uniform float u_vignette;\n"
                + "uniform float u_grain;\n"
                + "uniform float u_time;\n"
                + "uniform vec2 u_resolution;\n"
                + "varying vec2 v_uv;\n"
                + "void main() {\n"
                + "  vec3 c = texture2D(u_scene, v_uv).rgb;\n"
                + "  c += texture2D(u_bloom, v_uv).rgb * u_bloomIntensity;\n"
                + "  c = (c - 0.5) * u_grade.x + 0.5;\n"
                + "  float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));\n"
                + "  c = mix(vec3(luma), c, u_grade.y);\n"
                + "  c = clamp(c, 0.0, 1.0);\n"
                + "  c = (u_lift + c * (1.0 - u_lift)) * u_gain;\n"
                + "  vec2 d = v_uv - 0.5;\n"
                + "  c *= 1.0 - u_vignette * smoothstep(0.25, 0.75, dot(d, d) * 2.0);\n"
                + "  vec2 px = floor(v_uv * u_resolution);\n"
                + "  float noise = fract(sin(dot(px + fract(u_time * 7.0) * 61.0, vec2(12.9898, 78.233))) * 43758.5453);\n"
                + "  c += (noise - 0.5) * u_grain;\n"
                + "  gl_FragColor = vec4(c, 1.0);\n"
                + "}\n";
    }

    /// Returns the generated vertex shader source.
    public String getVertexSource() {
        return vertexSource;
    }

    /// Returns the generated fragment shader source.
    public String getFragmentSource() {
        return fragmentSource;
    }
}
