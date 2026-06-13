/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.linux;

import com.codename1.gpu.Material;
import com.codename1.gpu.VertexAttribute;
import com.codename1.gpu.VertexFormat;

/// Runtime generator of GLSL ES 3.0 source for the native Linux 3D backend
/// (OpenGL ES via EGL, cn1_linux_gl.c). It mirrors the HLSL generator of the
/// Windows port and the iOS Metal generator, but emits the vertex and fragment
/// stages as GLSL ES 3.00 packed into a single source string separated by the
/// {@link #STAGE_SEPARATOR} marker; the native side splits on the marker,
/// compiles the two stages and caches the program as a pipeline keyed by the
/// material+format key.
///
/// The generated shaders follow a fixed contract the native renderer relies on:
///
/// - vertex attributes are bound by explicit location: 0 = position, 1 = normal,
///   2 = texcoord, in the interleaved order the VertexFormat lists them
/// - one std140 uniform block {@code CN1Uniforms} (mvp, model, normalMatrix,
///   color, lightDir, lightColor, ambient, eye, shininess) uploaded from the same
///   column-major float[16] matrices the Java side packs (72 floats: 69 used +
///   std140 tail padding). GL's {@code mvp * v} needs no transpose or Z remap.
/// - the diffuse texture is the sampler2D {@code cn1_tex}
public final class GlslShaderGenerator {
    /// Marker separating the vertex stage from the fragment stage in the combined
    /// source; the native compiler splits on it.
    public static final String STAGE_SEPARATOR = "//@@CN1_FRAGMENT@@";

    private final String source;

    public GlslShaderGenerator(Material material, VertexFormat format) {
        boolean hasNormal = format.findByUsage(VertexAttribute.Usage.NORMAL) != null;
        boolean hasTexcoord = format.findByUsage(VertexAttribute.Usage.TEXCOORD) != null;
        boolean textured = material.getTexture() != null && hasTexcoord;
        Material.Type type = material.getType();
        boolean lit = (type == Material.Type.LAMBERT || type == Material.Type.PHONG) && hasNormal;
        boolean phong = type == Material.Type.PHONG && hasNormal;
        this.source = build(lit, phong, textured, hasNormal, hasTexcoord);
    }

    private static String uniformBlock() {
        // std140 layout; must match the float[] packed by LinuxGraphicsDevice and
        // uploaded into the UBO by the native renderer.
        return "layout(std140) uniform CN1Uniforms {\n"
                + "  mat4 mvp;\n"
                + "  mat4 model;\n"
                + "  mat4 normalMatrix;\n"
                + "  vec4 color;\n"
                + "  vec4 lightDir;\n"
                + "  vec4 lightColor;\n"
                + "  vec4 ambient;\n"
                + "  vec4 eye;\n"
                + "  float shininess;\n"
                + "};\n";
    }

    private static String build(boolean lit, boolean phong, boolean textured,
                                boolean hasNormal, boolean hasTexcoord) {
        StringBuilder sb = new StringBuilder();

        // ---- vertex stage ----
        sb.append("#version 300 es\n");
        sb.append(uniformBlock());
        sb.append("layout(location = 0) in vec3 position;\n");
        if (hasNormal) {
            sb.append("layout(location = 1) in vec3 normal;\n");
        }
        if (hasTexcoord) {
            sb.append("layout(location = 2) in vec2 texcoord;\n");
        }
        if (lit) {
            sb.append("out vec3 vWorldNormal;\n");
            sb.append("out vec3 vWorldPos;\n");
        }
        if (textured) {
            sb.append("out vec2 vTexcoord;\n");
        }
        sb.append("void main() {\n");
        sb.append("  gl_Position = mvp * vec4(position, 1.0);\n");
        if (lit) {
            sb.append("  vWorldNormal = (normalMatrix * vec4(normal, 0.0)).xyz;\n");
            sb.append("  vWorldPos = (model * vec4(position, 1.0)).xyz;\n");
        }
        if (textured) {
            sb.append("  vTexcoord = texcoord;\n");
        }
        sb.append("}\n");

        sb.append(STAGE_SEPARATOR).append("\n");

        // ---- fragment stage ----
        sb.append("#version 300 es\n");
        sb.append("precision highp float;\n");
        sb.append(uniformBlock());
        if (textured) {
            sb.append("uniform sampler2D cn1_tex;\n");
        }
        if (lit) {
            sb.append("in vec3 vWorldNormal;\n");
            sb.append("in vec3 vWorldPos;\n");
        }
        if (textured) {
            sb.append("in vec2 vTexcoord;\n");
        }
        sb.append("out vec4 fragColor;\n");
        sb.append("void main() {\n");
        sb.append("  vec4 base = color;\n");
        if (textured) {
            sb.append("  base = base * texture(cn1_tex, vTexcoord);\n");
        }
        if (lit) {
            sb.append("  vec3 n = normalize(vWorldNormal);\n");
            sb.append("  vec3 l = normalize(-lightDir.xyz);\n");
            sb.append("  float ndotl = max(dot(n, l), 0.0);\n");
            sb.append("  vec3 lighting = ambient.xyz + lightColor.xyz * ndotl;\n");
            sb.append("  vec3 rgb = base.rgb * lighting;\n");
            if (phong) {
                sb.append("  if (ndotl > 0.0) {\n");
                sb.append("    vec3 v = normalize(eye.xyz - vWorldPos);\n");
                sb.append("    vec3 h = normalize(l + v);\n");
                sb.append("    float spec = pow(max(dot(n, h), 0.0), shininess);\n");
                sb.append("    rgb += lightColor.xyz * spec;\n");
                sb.append("  }\n");
            }
            sb.append("  fragColor = vec4(rgb, base.a);\n");
        } else {
            sb.append("  fragColor = base;\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    /// Returns the combined GLSL source (vertex stage, {@link #STAGE_SEPARATOR},
    /// fragment stage).
    public String getSource() {
        return source;
    }
}
