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
import com.codename1.gpu.shader.ShaderEmitter;
import com.codename1.gpu.shader.ShaderMaterial;
import com.codename1.gpu.shader.ShaderNode;

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
/// - custom shading (`Material.getShader()`): `u_cu` (vec4 array, one entry
///   per declared uniform, both stages), `u_ct0` to `u_ct3` (sampler2D,
///   texture units 2 to 5) and `u_time` (float)
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
    /// The attribute name bound to a skinned vertex's four joint indices.
    public static final String A_JOINTS = "a_joints";
    /// The attribute name bound to a skinned vertex's four joint weights.
    public static final String A_WEIGHTS = "a_weights";
    /// The vertex uniform holding the joint rows of a skinned material, three
    /// vec4 per joint.
    public static final String U_JOINTS = "u_joints";

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
        this(material, format, environment, shadowed, false);
    }

    /// Generates the depth only program of a material: what its vertex offset
    /// moves and its discard hook keeps, written as packed depth
    /// (`unpackDepth` reads it back). The shadow pass draws a material through
    /// it when `needsOwnDepthProgram` says the shared caster program would put
    /// its depth in the wrong place; the backend sets `u_mvp` to the light's
    /// (or the camera's) view projection times the model matrix, and the
    /// rest of the uniforms as for a normal draw.
    ///
    /// #### Parameters
    ///
    /// - `material`: the material
    ///
    /// - `format`: the mesh vertex layout
    ///
    /// #### Returns
    ///
    /// the generator
    public static GlslShaderGenerator depthOnly(Material material, VertexFormat format) {
        return new GlslShaderGenerator(material, format, null, false, true);
    }

    // The environment map lookup (EnvironmentMaps): six equirectangular bands
    // stacked in v, five roughness levels then the irradiance, at half
    // intensity. v keeps half a texel inside its band so filtering never
    // reads the next one.
    private static final String ENV_LOOKUP =
            "vec3 envAt(vec3 d, float band) {\n"
            + "  vec2 uv = vec2(atan(d.z, d.x) * 0.15915494 + 0.5, acos(clamp(d.y, -1.0, 1.0)) * 0.31830989);\n"
            + "  uv = vec2(clamp(uv.x, 0.0039, 0.9961), (clamp(uv.y, 0.0079, 0.9921) + band) / 6.0);\n"
            + "  return texture2D(u_envMap, uv).rgb * 2.0;\n"
            + "}\n"
            + "vec3 envSpecular(vec3 d, float rough) {\n"
            + "  float lv = clamp(rough, 0.0, 1.0) * 4.0;\n"
            + "  float l0 = floor(lv);\n"
            + "  return mix(envAt(d, l0), envAt(d, min(l0 + 1.0, 4.0)), lv - l0);\n"
            + "}\n";

    /// Prints the cascade choice every generator's shadow lookup shares, on
    /// `p`, the position in the near cascade's normalized device coordinates.
    /// With a ratio above 0 the map is two squares wide: a position inside
    /// the near square (with a margin for the filter) moves to the left half,
    /// any other is scaled into the far cascade and moves to the right half,
    /// and the horizontal texel step halves. Public so the Metal and HLSL
    /// generators print the same lines in their own dialect.
    ///
    /// #### Parameters
    ///
    /// - `sb`: the source being built
    ///
    /// - `ratio`: the expression of the cascade ratio
    ///
    /// - `texel`: the name of the two component texel step variable
    public static void appendCascadePick(StringBuilder sb, String ratio, String texel) {
        sb.append("  if (").append(ratio).append(" > 0.0) {\n");
        sb.append("    ").append(texel).append(".x *= 0.5;\n");
        sb.append("    if (max(abs(p.x), abs(p.y)) < 0.97) {\n");
        sb.append("      p.x = p.x * 0.5 - 0.5;\n");
        sb.append("    } else {\n");
        sb.append("      p.xy = p.xy * ").append(ratio).append(";\n");
        sb.append("      p.x = p.x * 0.5 + 0.5;\n");
        sb.append("    }\n  }\n");
    }

    /// True if a material moves its vertices or discards fragments, so its
    /// depth only needs a program of its own rather than the shared caster
    /// program that only reads the position.
    ///
    /// #### Parameters
    ///
    /// - `material`: the material
    ///
    /// #### Returns
    ///
    /// true if `depthOnly` must be used for this material
    public static boolean needsOwnDepthProgram(Material material) {
        ShaderMaterial sm = material.getEffectiveShader();
        return material.isSkinned() || sm != null && (sm.get(ShaderMaterial.Hook.VERTEX_OFFSET) != null
                || sm.get(ShaderMaterial.Hook.DISCARD) != null);
    }

    private GlslShaderGenerator(Material material, VertexFormat format, Environment environment,
            boolean shadowed, boolean depthOnly) {
        boolean hasNormal = format.findByUsage(VertexAttribute.Usage.NORMAL) != null;
        boolean hasTexcoord = format.findByUsage(VertexAttribute.Usage.TEXCOORD) != null;
        Features f = new Features();
        f.skinned = isSkinned(material, format);
        f.joints = material.getJointCount();
        f.textured = material.getTexture() != null && hasTexcoord;
        Material.Type type = material.getType();
        f.lit = material.isLit() && hasNormal;
        f.phong = type == Material.Type.PHONG && hasNormal;
        f.pbr = type == Material.Type.PBR && hasNormal;
        boolean sky = type == Material.Type.SKYBOX;
        f.fog = environment != null && environment.isFog() && !sky && material.isFogged();
        f.sky = sky && environment != null && environment.isSky();
        f.hemisphere = f.lit && environment != null && environment.isHemisphere();
        f.toneMap = f.lit && environment != null && environment.isToneMapping();
        f.ibl = f.lit && environment != null && environment.isEnvironmentMap();
        f.shadow = f.lit && shadowed && material.isReceiveShadows();
        f.emissive = f.lit && (material.getEmissive() & 0xffffff) != 0;
        ShaderMaterial sm = material.getEffectiveShader();
        if (sm != null) {
            f.shader = sm;
            f.hasNormal = hasNormal;
            f.hasTexcoord = hasTexcoord;
            f.fragWorld = ShaderEmitter.uses(sm, true, "worldPos") || ShaderEmitter.uses(sm, true, "viewDir");
            f.fragEye = f.fragWorld || ShaderEmitter.uses(sm, true, "cameraPos");
            f.fragNormal = hasNormal && ShaderEmitter.uses(sm, true, "worldNormal");
            f.fragUv = hasTexcoord && ShaderEmitter.uses(sm, true, "uv");
            f.fragSun = ShaderEmitter.uses(sm, true, "sunDir") || ShaderEmitter.uses(sm, true, "sunColor");
            f.vertWorld = ShaderEmitter.uses(sm, false, "worldPos");
            f.vertUv = hasTexcoord && ShaderEmitter.uses(sm, false, "uv");
            f.vertNormal = hasNormal && ShaderEmitter.uses(sm, false, "normal");
            f.vertEye = ShaderEmitter.uses(sm, false, "cameraPos");
            f.vertSun = ShaderEmitter.uses(sm, false, "sunDir") || ShaderEmitter.uses(sm, false, "sunColor");
            f.time = ShaderEmitter.uses(sm, "time");
        }
        if (depthOnly) {
            f.depthOnly = true;
            f.lit = false;
            f.phong = false;
            f.pbr = false;
            f.fog = false;
            f.sky = false;
            f.hemisphere = false;
            f.toneMap = false;
            f.ibl = false;
            f.shadow = false;
            f.emissive = false;
        }
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
        boolean ibl;
        boolean shadow;
        boolean emissive;
        boolean sky;
        ShaderMaterial shader;
        boolean hasNormal;
        boolean hasTexcoord;
        boolean fragWorld;
        boolean fragEye;
        boolean fragNormal;
        boolean fragUv;
        boolean fragSun;
        boolean vertWorld;
        boolean vertUv;
        boolean vertNormal;
        boolean vertEye;
        boolean vertSun;
        boolean time;
        boolean depthOnly;
        boolean skinned;
        int joints;

        boolean world() {
            return lit || fog || sky || fragWorld;
        }

        boolean normalVarying() {
            return lit || fragNormal;
        }

        boolean uvVarying() {
            return textured || fragUv;
        }
    }

    // custom shading inputs of the vertex stage
    private static final class VertexInputs implements ShaderEmitter.Inputs {
        private final Features f;

        VertexInputs(Features f) {
            this.f = f;
        }

        public String builtin(String name) { // NOPMD MissingOverride - Java 5 source forbids @Override on an interface method
            if ("position".equals(name)) {
                return f.skinned ? "p0" : A_POSITION;
            }
            if ("normal".equals(name)) {
                return f.hasNormal ? (f.skinned ? "n0" : A_NORMAL) : "vec3(0.0, 1.0, 0.0)";
            }
            if ("uv".equals(name)) {
                return f.hasTexcoord ? A_TEXCOORD : "vec2(0.0)";
            }
            if ("worldPos".equals(name)) {
                return "(u_model * vec4(" + (f.skinned ? "p0" : A_POSITION) + ", 1.0)).xyz";
            }
            return commonBuiltin(name);
        }

        public String uniform(int slot) { // NOPMD MissingOverride - Java 5 source forbids @Override on an interface method
            return "u_cu[" + slot + "]";
        }

        public String sample(int slot, String uv) { // NOPMD MissingOverride - Java 5 source forbids @Override on an interface method
            // GLES 2 does not promise vertex texture fetch
            return null;
        }
    }

    // custom shading inputs of the fragment stage
    private static final class FragmentInputs implements ShaderEmitter.Inputs {
        private final Features f;

        FragmentInputs(Features f) {
            this.f = f;
        }

        public String builtin(String name) { // NOPMD MissingOverride - Java 5 source forbids @Override on an interface method
            if ("worldPos".equals(name)) {
                return "v_worldPos";
            }
            if ("worldNormal".equals(name)) {
                return f.normalVarying() ? "normalize(v_normal)" : "vec3(0.0, 1.0, 0.0)";
            }
            if ("viewDir".equals(name)) {
                return "normalize(u_eye - v_worldPos)";
            }
            if ("uv".equals(name)) {
                return f.uvVarying() ? "v_texcoord" : "vec2(0.0)";
            }
            if ("baseColor".equals(name)) {
                return "base";
            }
            return commonBuiltin(name);
        }

        public String uniform(int slot) { // NOPMD MissingOverride - Java 5 source forbids @Override on an interface method
            return "u_cu[" + slot + "]";
        }

        public String sample(int slot, String uv) { // NOPMD MissingOverride - Java 5 source forbids @Override on an interface method
            return "texture2D(u_ct" + slot + ", " + uv + ")";
        }
    }

    private static String commonBuiltin(String name) {
        if ("time".equals(name)) {
            return "u_time";
        }
        if ("cameraPos".equals(name)) {
            return "u_eye";
        }
        if ("sunDir".equals(name)) {
            return "normalize(-u_lightDir)";
        }
        if ("sunColor".equals(name)) {
            return "u_lightColor";
        }
        return null;
    }

    private static void appendCustomUniforms(StringBuilder sb, Features f, boolean fragment) {
        ShaderMaterial sm = f.shader;
        if (sm == null) {
            return;
        }
        if (sm.getUniformCount() > 0) {
            sb.append("uniform vec4 u_cu[").append(sm.getUniformCount()).append("];\n");
        }
        if (f.time) {
            sb.append("uniform float u_time;\n");
        }
        if (fragment) {
            for (int i = 0; i < sm.getTextureCount(); i++) {
                sb.append("uniform sampler2D u_ct").append(i).append(";\n");
            }
        }
    }

    private static String buildVertex(Features f) {
        boolean world = f.world();
        ShaderNode offset = f.shader == null ? null : f.shader.get(ShaderMaterial.Hook.VERTEX_OFFSET);
        String p0 = f.skinned ? "p0" : A_POSITION;
        String pos = offset != null ? "pos" : p0;
        boolean normalIn = f.normalVarying() || f.vertNormal;
        StringBuilder sb = new StringBuilder();
        sb.append("attribute vec3 ").append(A_POSITION).append(";\n");
        if (normalIn) {
            sb.append("attribute vec3 ").append(A_NORMAL).append(";\n");
        }
        if (f.skinned) {
            sb.append("attribute vec4 ").append(A_JOINTS).append(";\n");
            sb.append("attribute vec4 ").append(A_WEIGHTS).append(";\n");
            sb.append("uniform vec4 ").append(U_JOINTS).append("[").append(f.joints * 3).append("];\n");
        }
        if (f.uvVarying() || f.vertUv) {
            sb.append("attribute vec2 ").append(A_TEXCOORD).append(";\n");
        }
        sb.append("uniform mat4 u_mvp;\n");
        if (world || f.vertWorld) {
            sb.append("uniform mat4 u_model;\n");
        }
        if (world) {
            sb.append("varying vec3 v_worldPos;\n");
        }
        if (f.vertEye) {
            sb.append("uniform vec3 u_eye;\n");
        }
        if (f.vertSun) {
            sb.append("uniform vec3 u_lightDir;\n");
            sb.append("uniform vec3 u_lightColor;\n");
        }
        appendCustomUniforms(sb, f, false);
        if (f.normalVarying()) {
            sb.append("uniform mat4 u_normalMatrix;\n");
            sb.append("varying vec3 v_normal;\n");
        }
        if (f.shadow) {
            sb.append("uniform mat4 u_shadowMatrix;\n");
            sb.append("varying vec4 v_shadowPos;\n");
        }
        if (f.uvVarying()) {
            sb.append("varying vec2 v_texcoord;\n");
        }
        sb.append("void main() {\n");
        if (f.skinned) {
            appendSkin(sb, normalIn);
        }
        if (offset != null) {
            ShaderEmitter e = new ShaderEmitter(ShaderEmitter.Dialect.GLSL, f.shader, new VertexInputs(f), sb,
                    "  ", "_v");
            String o = e.emit(offset);
            sb.append("  vec3 pos = ").append(p0).append(" + ").append(o).append(";\n");
        }
        if (world) {
            sb.append("  vec4 world = u_model * vec4(").append(pos).append(", 1.0);\n");
            sb.append("  v_worldPos = world.xyz;\n");
        }
        if (f.normalVarying()) {
            sb.append("  v_normal = (u_normalMatrix * vec4(").append(f.skinned ? "n0" : A_NORMAL)
                    .append(", 0.0)).xyz;\n");
        }
        if (f.shadow) {
            sb.append("  v_shadowPos = u_shadowMatrix * world;\n");
        }
        if (f.uvVarying()) {
            sb.append("  v_texcoord = ").append(A_TEXCOORD).append(";\n");
        }
        sb.append("  gl_Position = u_mvp * vec4(").append(pos).append(", 1.0);\n");
        sb.append("}\n");
        return sb.toString();
    }

    /// True if a draw of `format` with `material` is skinned in the vertex
    /// stage: the material has joints and the format carries joints and
    /// weights. Every generator and every device asks this one question.
    ///
    /// #### Parameters
    ///
    /// - `material`: the material
    ///
    /// - `format`: the mesh's vertex format
    ///
    /// #### Returns
    ///
    /// true to skin
    public static boolean isSkinned(Material material, VertexFormat format) {
        return material.isSkinned() && format.findByUsage(VertexAttribute.Usage.JOINTS) != null
                && format.findByUsage(VertexAttribute.Usage.WEIGHTS) != null;
    }

    // Blends the three rows of each of the four joints by weight into
    // p0 and n0, the skinned position and normal. Three rows rather than a
    // matrix per joint, and an array only as long as the material's joints
    // (the count is in Material.getShaderKey), keep the uniform space small:
    // OpenGL ES 2 promises only 128 vertex vectors, though the devices in use
    // have 256 or more.
    private static void appendSkin(StringBuilder sb, boolean normal) {
        sb.append("  vec4 r0 = vec4(0.0);\n  vec4 r1 = vec4(0.0);\n  vec4 r2 = vec4(0.0);\n");
        String[] c = {"x", "y", "z", "w"};
        for (int k = 0; k < 4; k++) {
            sb.append("  int j").append(k).append(" = int(").append(A_JOINTS).append('.').append(c[k])
                    .append(" + 0.5) * 3;\n");
            for (int row = 0; row < 3; row++) {
                sb.append("  r").append(row).append(" += ").append(A_WEIGHTS).append('.').append(c[k])
                        .append(" * ").append(U_JOINTS).append("[j").append(k).append(" + ").append(row)
                        .append("];\n");
            }
        }
        sb.append("  vec4 sp = vec4(").append(A_POSITION).append(", 1.0);\n");
        sb.append("  vec3 p0 = vec3(dot(r0, sp), dot(r1, sp), dot(r2, sp));\n");
        if (normal) {
            sb.append("  vec3 n0 = vec3(dot(r0.xyz, ").append(A_NORMAL).append("), dot(r1.xyz, ")
                    .append(A_NORMAL).append("), dot(r2.xyz, ").append(A_NORMAL).append("));\n");
        }
    }

    private static String buildFragment(Features f) {
        boolean world = f.world();
        StringBuilder sb = new StringBuilder();
        // custom graphs (noise, waves) need the precision world space math does
        sb.append(f.shadow || f.shader != null || f.depthOnly ? HIGHP : "precision mediump float;\n");
        if (f.depthOnly) {
            sb.append(PACK);
        }
        sb.append("uniform vec4 u_color;\n");
        if (f.textured) {
            sb.append("uniform sampler2D u_texture;\n");
        }
        if (f.uvVarying()) {
            sb.append("varying vec2 v_texcoord;\n");
        }
        if (world) {
            sb.append("varying vec3 v_worldPos;\n");
        }
        if (world || f.fragEye) {
            sb.append("uniform vec3 u_eye;\n");
        }
        // the sky declares u_lightDir itself
        if (f.lit || (f.fragSun && !f.sky)) {
            sb.append("uniform vec3 u_lightDir;\n");
            sb.append("uniform vec3 u_lightColor;\n");
        } else if (f.fragSun) {
            sb.append("uniform vec3 u_lightColor;\n");
        }
        if (f.lit) {
            sb.append("uniform vec3 u_ambient;\n");
        }
        if (f.normalVarying()) {
            sb.append("varying vec3 v_normal;\n");
        }
        appendCustomUniforms(sb, f, true);
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
        if (f.ibl) {
            sb.append("uniform sampler2D u_envMap;\n");
            sb.append(ENV_LOOKUP);
        }
        if (f.shadow) {
            sb.append("uniform sampler2D u_shadowMap;\n");
            // bias, strength, texel size, cascade ratio (0 = one map)
            sb.append("uniform vec4 u_shadow;\n");
            sb.append("varying vec4 v_shadowPos;\n");
            sb.append(UNPACK);
            sb.append("float shadowAt(vec3 n, vec3 l) {\n");
            sb.append("  vec3 p = v_shadowPos.xyz / v_shadowPos.w;\n");
            sb.append("  vec2 tx = vec2(u_shadow.z);\n");
            appendCascadePick(sb, "u_shadow.w", "tx");
            sb.append("  p = p * 0.5 + 0.5;\n");
            sb.append("  if (p.x < 0.0 || p.x > 1.0 || p.y < 0.0 || p.y > 1.0 || p.z > 1.0) {\n");
            sb.append("    return 1.0;\n  }\n");
            // a steeper surface needs a larger offset to stay off its own map
            sb.append("  float bias = u_shadow.x * (1.0 + 2.0 * (1.0 - max(dot(n, l), 0.0)));\n");
            sb.append("  float lit = 0.0;\n");
            sb.append("  for (int y = -1; y <= 1; y++) {\n");
            sb.append("    for (int x = -1; x <= 1; x++) {\n");
            sb.append("      float d = unpackDepth(texture2D(u_shadowMap, p.xy + vec2(float(x), float(y)) * tx));\n");
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
        String[] hook = emitFragmentHooks(f, sb);
        if (hook[ShaderMaterial.Hook.BASE_COLOR.ordinal()] != null) {
            sb.append("  base = ").append(hook[ShaderMaterial.Hook.BASE_COLOR.ordinal()]).append(";\n");
        }
        if (hook[ShaderMaterial.Hook.DISCARD.ordinal()] != null) {
            sb.append("  if (").append(hook[ShaderMaterial.Hook.DISCARD.ordinal()]).append(" > 0.5) {\n");
            sb.append("    discard;\n  }\n");
        }
        if (f.depthOnly) {
            // packDepth(1.0) wraps to 0, the nearest depth there is
            sb.append("  gl_FragColor = packDepth(min(gl_FragCoord.z, 0.99999));\n");
            sb.append("}\n");
            return sb.toString();
        }
        sb.append("  vec3 rgb = base.rgb;\n");
        if (f.sky) {
            appendSky(sb);
        }
        String customEmissive = hook[ShaderMaterial.Hook.EMISSIVE.ordinal()];
        if (f.lit) {
            sb.append("  vec3 n = normalize(v_normal);\n");
            if (hook[ShaderMaterial.Hook.NORMAL.ordinal()] != null) {
                sb.append("  n = normalize(").append(hook[ShaderMaterial.Hook.NORMAL.ordinal()]).append(");\n");
            }
            sb.append("  vec3 l = normalize(-u_lightDir);\n");
            sb.append("  float ndotl = max(dot(n, l), 0.0);\n");
            if (f.ibl) {
                sb.append("  vec3 ambient = envAt(n, 5.0);\n");
            } else if (f.hemisphere) {
                sb.append("  vec3 ambient = mix(u_ground, u_sky, n.y * 0.5 + 0.5);\n");
            } else {
                sb.append("  vec3 ambient = u_ambient;\n");
            }
            sb.append(f.shadow ? "  float shade = shadowAt(n, l);\n" : "  float shade = 1.0;\n");
            if (f.pbr) {
                appendPbr(sb, f.hemisphere, f.ibl, hook[ShaderMaterial.Hook.METALLIC.ordinal()],
                        hook[ShaderMaterial.Hook.ROUGHNESS.ordinal()]);
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
            if (customEmissive != null) {
                sb.append("  rgb += ").append(customEmissive).append(";\n");
            }
            if (f.toneMap) {
                sb.append("  rgb = aces(rgb * u_exposure);\n");
            }
        } else if (customEmissive != null) {
            sb.append("  rgb += ").append(customEmissive).append(";\n");
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

    // Prints every fragment hook as temporaries at the top of main, after the
    // base colour, and returns the expression of each, by hook ordinal.
    private static String[] emitFragmentHooks(Features f, StringBuilder sb) {
        ShaderMaterial.Hook[] all = ShaderMaterial.Hook.values();
        String[] out = new String[all.length];
        if (f.shader == null) {
            return out;
        }
        ShaderEmitter e = new ShaderEmitter(ShaderEmitter.Dialect.GLSL, f.shader, new FragmentInputs(f), sb,
                "  ", "_f");
        for (int i = 0; i < all.length; i++) {
            ShaderNode g = f.shader.get(all[i]);
            boolean wanted = !f.depthOnly || all[i] == ShaderMaterial.Hook.DISCARD
                    || all[i] == ShaderMaterial.Hook.BASE_COLOR;
            if (g != null && wanted && all[i].isFragment()) {
                out[i] = e.emit(g);
            }
        }
        return out;
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
    private static void appendPbr(StringBuilder sb, boolean hemi, boolean ibl, String metallicHook,
            String roughHook) {
        sb.append("  float metallic = ")
                .append(metallicHook != null ? "clamp(" + metallicHook + ", 0.0, 1.0)" : "u_pbr.x").append(";\n");
        // below 0.04 the GGX lobe is a sub-pixel spike that aliases
        sb.append("  float rough = ")
                .append(roughHook != null ? "clamp(" + roughHook + ", 0.04, 1.0)" : "u_pbr.y").append(";\n");
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
        // ambient (analytic image based lighting): the diffuse term reads the
        // environment along the normal; the specular term along the reflected
        // view ray, pulled toward the normal as roughness blurs it, weighted by
        // Karis' fitted split-sum BRDF instead of a baked lookup table
        sb.append("  vec3 rr = reflect(-v, n);\n");
        sb.append("  vec3 rdir = normalize(mix(rr, n, rough * rough));\n");
        if (ibl) {
            // the baked map is already blurred per roughness, so it is read
            // along the true reflection
            sb.append("  vec3 envSpec = envSpecular(rr, rough);\n");
        } else {
            sb.append("  vec3 envSpec = " + (hemi ? "mix(u_ground, u_sky, rdir.y * 0.5 + 0.5)" : "ambient") + ";\n");
        }
        sb.append("  vec4 ec0 = vec4(-1.0, -0.0275, -0.572, 0.022);\n");
        sb.append("  vec4 ec1 = vec4(1.0, 0.0425, 1.04, -0.04);\n");
        sb.append("  vec4 er = rough * ec0 + ec1;\n");
        sb.append("  float ea = min(er.x * er.x, exp2(-9.28 * ndotv)) * er.x + er.y;\n");
        sb.append("  vec3 envF = f0 * (-1.04 * ea + er.z) + (1.04 * ea + er.w);\n");
        sb.append("  vec3 amb = ambient * base.rgb * (1.0 - metallic) * (1.0 - envF) + envSpec * envF;\n");
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

    /// The post effects every composite pass shares, in one of the three
    /// shading languages: FXAA, chromatic aberration and god rays. Written
    /// once so GLSL, Metal and HLSL cannot drift apart. The block declares
    /// `c` (the scene colour, before bloom) from `uv0`, which the caller
    /// declares. `fx` is (aberration, fxaa, rays, unused) and `sun` the
    /// sun's texture position, as `GraphicsDevice.packPostEffects` packs
    /// them; `res` is the target size in pixels.
    ///
    /// #### Parameters
    ///
    /// - `lang`: "glsl", "msl" or "hlsl"
    ///
    /// - `fx`: the expression of the effect parameters
    ///
    /// - `sun`: the expression of the sun position
    ///
    /// - `res`: the expression of the resolution
    ///
    /// #### Returns
    ///
    /// the statements
    public static String postEffects(String lang, String fx, String sun, String res) {
        String t = "  V2 px1 = 1.0 / RES;\n"
                + "  V3 L = V3(0.299, 0.587, 0.114);\n"
                + "  V3 c = PX(uv0);\n"
                + "  if (FX.y > 0.5) {\n"
                + "    float lNW = dot(PX(uv0 + V2(-1.0, -1.0) * px1), L);\n"
                + "    float lNE = dot(PX(uv0 + V2(1.0, -1.0) * px1), L);\n"
                + "    float lSW = dot(PX(uv0 + V2(-1.0, 1.0) * px1), L);\n"
                + "    float lSE = dot(PX(uv0 + V2(1.0, 1.0) * px1), L);\n"
                + "    float lM = dot(c, L);\n"
                + "    float lMin = min(lM, min(min(lNW, lNE), min(lSW, lSE)));\n"
                + "    float lMax = max(lM, max(max(lNW, lNE), max(lSW, lSE)));\n"
                + "    V2 dir = V2(-((lNW + lNE) - (lSW + lSE)), (lNW + lSW) - (lNE + lSE));\n"
                + "    float reduce = max((lNW + lNE + lSW + lSE) * 0.03125, 0.0078125);\n"
                + "    float rcp = 1.0 / (min(abs(dir.x), abs(dir.y)) + reduce);\n"
                + "    dir = clamp(dir * rcp, V2(-8.0, -8.0), V2(8.0, 8.0)) * px1;\n"
                + "    V3 a = 0.5 * (PX(uv0 + dir * -0.1666667) + PX(uv0 + dir * 0.1666667));\n"
                + "    V3 b = a * 0.5 + 0.25 * (PX(uv0 + dir * -0.5) + PX(uv0 + dir * 0.5));\n"
                + "    float lB = dot(b, L);\n"
                + "    float useA = step(lB, lMin) + step(lMax, lB);\n"
                + "    c = mix(b, a, min(useA, 1.0));\n"
                + "  }\n"
                + "  if (FX.x > 0.0) {\n"
                + "    V2 off = (uv0 - 0.5) * FX.x;\n"
                + "    c.r = PX(uv0 + off).r;\n"
                + "    c.b = PX(uv0 - off).b;\n"
                + "  }\n"
                + "  if (FX.z > 0.0) {\n"
                + "    V2 step1 = (uv0 - SUN) * 0.0625;\n"
                + "    V2 tuv = uv0;\n"
                + "    float w = 1.0;\n"
                + "    V3 acc = V3(0.0, 0.0, 0.0);\n"
                + "    for (int i = 0; i < 16; i++) {\n"
                + "      tuv -= step1;\n"
                + "      V3 s = PX(tuv);\n"
                + "      acc += s * (smoothstep(0.6, 1.0, dot(s, L)) * w);\n"
                + "      w *= 0.92;\n"
                + "    }\n"
                + "    c += acc * (FX.z * 0.125);\n"
                + "  }\n";
        boolean glsl = "glsl".equals(lang);
        boolean msl = "msl".equals(lang);
        t = t.replace("V2", glsl ? "vec2" : "float2").replace("V3", glsl ? "vec3" : "float3")
                .replace("FX", fx).replace("SUN", sun).replace("RES", res)
                .replace("PX(", msl ? "cn1Px(scene, " : "cn1Px(");
        if (!glsl) {
            // HLSL and MSL have no vector by scalar clamp overload difference; lerp is HLSL's mix
            if (!msl) {
                t = t.replace("mix(", "lerp(");
            }
        }
        return t;
    }

    /// The depth effects every composite pass shares: depth of field,
    /// ambient occlusion and outline, in that order, applied to `c` after
    /// `postEffects`. The caller declares `cn1Depth(uv)` (window depth, 0 to
    /// 1; Metal takes the depth texture first) and the block linearizes it
    /// with the near and far planes. `dfx` and `dfx2` are the first eight
    /// floats `GraphicsDevice.packDepthEffects` packs, `ocol` the outline
    /// colour.
    ///
    /// #### Parameters
    ///
    /// - `lang`: "glsl", "msl" or "hlsl"
    ///
    /// - `dfx`: the expression of (ao strength, ao radius, focus, blur)
    ///
    /// - `dfx2`: the expression of (outline, near, far, lut strength)
    ///
    /// - `ocol`: the expression of the outline colour
    ///
    /// #### Returns
    ///
    /// the statements
    public static String depthEffects(String lang, String dfx, String dfx2, String ocol) {
        String t = "  if (DFX.x + DFX.w + DFX2.x > 0.0) {\n"
                + "    float zn = DFX2.y;\n"
                + "    float zf = DFX2.z;\n"
                + "    float d0 = DEPTH(uv0);\n"
                + "    float z0 = 2.0 * zn * zf / (zf + zn - (2.0 * d0 - 1.0) * (zf - zn));\n"
                + "    if (DFX.w > 0.0) {\n"
                + "      float coc = clamp(abs(z0 - DFX.z) / max(DFX.z, 0.001), 0.0, 1.0) * DFX.w;\n"
                + "      if (coc > 0.5) {\n"
                + "        V3 acc = c;\n"
                + "        for (int i = 0; i < 12; i++) {\n"
                + "          float fi = float(i);\n"
                + "          float a = fi * 2.399963;\n"
                + "          float r = sqrt((fi + 0.5) / 12.0) * coc;\n"
                + "          acc += PX(uv0 + V2(cos(a), sin(a)) * r * px1);\n"
                + "        }\n"
                + "        c = acc / 13.0;\n"
                + "      }\n"
                + "    }\n"
                + "    if (DFX.x > 0.0 && d0 < 0.99999) {\n"
                + "      float occ = 0.0;\n"
                + "      for (int i = 0; i < 8; i++) {\n"
                + "        float fi = float(i);\n"
                + "        float a = fi * 2.399963 + 0.7;\n"
                + "        float r = (fi + 1.0) * 0.125 * DFX.y;\n"
                + "        float ds = DEPTH(uv0 + V2(cos(a), sin(a)) * r * px1);\n"
                + "        float zs = 2.0 * zn * zf / (zf + zn - (2.0 * ds - 1.0) * (zf - zn));\n"
                + "        float dz = (z0 - zs) / z0;\n"
                + "        occ += smoothstep(0.005, 0.04, dz) * (1.0 - smoothstep(0.15, 0.3, dz));\n"
                + "      }\n"
                + "      c *= 1.0 - DFX.x * occ * 0.125;\n"
                + "    }\n"
                + "    if (DFX2.x > 0.0) {\n"
                + "      float e = 0.0;\n"
                + "      for (int i = 0; i < 4; i++) {\n"
                + "        float fi = float(i);\n"
                + "        V2 o = V2(cos(fi * 1.5707963), sin(fi * 1.5707963)) * px1;\n"
                + "        float ds = DEPTH(uv0 + o);\n"
                + "        e += 2.0 * zn * zf / (zf + zn - (2.0 * ds - 1.0) * (zf - zn));\n"
                + "      }\n"
                + "      float lap = abs(e - 4.0 * z0) / z0;\n"
                + "      c = mix(c, OCOL, clamp(smoothstep(0.04, 0.16, lap) * DFX2.x, 0.0, 1.0));\n"
                + "    }\n"
                + "  }\n";
        return translate(lang, t.replace("DFX2", dfx2).replace("DFX", dfx).replace("OCOL", ocol));
    }

    /// The colour lookup every composite pass applies after its grade. The
    /// caller declares `cn1Lut(uv)` (Metal takes the table first).
    ///
    /// #### Parameters
    ///
    /// - `lang`: "glsl", "msl" or "hlsl"
    ///
    /// - `strength`: the expression of the lookup strength
    ///
    /// #### Returns
    ///
    /// the statements
    public static String lutEffect(String lang, String strength) {
        String t = "  if (STR > 0.0) {\n"
                + "    V3 q = clamp(c, 0.0, 1.0) * 15.0;\n"
                + "    float b0 = floor(q.b);\n"
                + "    float b1 = min(b0 + 1.0, 15.0);\n"
                + "    V2 t0 = V2((b0 * 16.0 + q.r + 0.5) / 256.0, (q.g + 0.5) / 16.0);\n"
                + "    V2 t1 = V2((b1 * 16.0 + q.r + 0.5) / 256.0, t0.y);\n"
                + "    c = mix(c, mix(LUT(t0), LUT(t1), q.b - b0), STR);\n"
                + "  }\n";
        return translate(lang, t.replace("STR", strength));
    }

    private static String translate(String lang, String t) {
        boolean glsl = "glsl".equals(lang);
        boolean msl = "msl".equals(lang);
        t = t.replace("V2", glsl ? "vec2" : "float2").replace("V3", glsl ? "vec3" : "float3")
                .replace("PX(", msl ? "cn1Px(scene, " : "cn1Px(")
                .replace("DEPTH(", msl ? "cn1Depth(depth, " : "cn1Depth(")
                .replace("LUT(", msl ? "cn1Lut(lut, " : "cn1Lut(");
        if (!glsl && !msl) {
            t = t.replace("mix(", "lerp(");
        }
        return t;
    }

    /// Returns the final pass: the scene plus bloom, colour graded, with
    /// vignette and grain. Uniforms: `u_scene` (unit 0), `u_bloom` (unit 1),
    /// `u_bloomIntensity`, `u_grade` (vec2: contrast, saturation),
    /// `u_lift` and `u_gain` (vec3), `u_vignette`, `u_grain`, `u_time` (float)
    /// and `u_resolution` (vec2, pixels), `u_fx` (vec3: chromatic
    /// aberration, FXAA, god rays), `u_sun` (vec2, the sun in uv),
    /// `u_depth` (unit 2, the scene's depth), `u_lut` (unit 3, the colour
    /// lookup strip), and `u_dfx`, `u_dfx2` (vec4) and `u_ocol` (vec3) as
    /// `depthEffects` reads them.
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
                + "uniform vec3 u_fx;\n"
                + "uniform vec2 u_sun;\n"
                + "uniform sampler2D u_depth;\n"
                + "uniform sampler2D u_lut;\n"
                + "uniform vec4 u_dfx;\n"
                + "uniform vec4 u_dfx2;\n"
                + "uniform vec3 u_ocol;\n"
                + "varying vec2 v_uv;\n"
                + "vec3 cn1Px(vec2 p) {\n  return texture2D(u_scene, p).rgb;\n}\n"
                + "float cn1Depth(vec2 p) {\n  return texture2D(u_depth, p).r;\n}\n"
                + "vec3 cn1Lut(vec2 p) {\n  return texture2D(u_lut, p).rgb;\n}\n"
                + "void main() {\n"
                + "  vec2 uv0 = v_uv;\n"
                + postEffects("glsl", "u_fx", "u_sun", "u_resolution")
                + depthEffects("glsl", "u_dfx", "u_dfx2", "u_ocol")
                + "  c += texture2D(u_bloom, v_uv).rgb * u_bloomIntensity;\n"
                + "  c = (c - 0.5) * u_grade.x + 0.5;\n"
                + "  float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));\n"
                + "  c = mix(vec3(luma), c, u_grade.y);\n"
                + "  c = clamp(c, 0.0, 1.0);\n"
                + "  c = (u_lift + c * (1.0 - u_lift)) * u_gain;\n"
                + lutEffect("glsl", "u_dfx2.w")
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
