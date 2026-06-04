/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.gpu;

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
public final class GlslShaderGenerator {
    /// The attribute name bound to vertex positions.
    public static final String A_POSITION = "a_position";
    /// The attribute name bound to vertex normals.
    public static final String A_NORMAL = "a_normal";
    /// The attribute name bound to vertex texture coordinates.
    public static final String A_TEXCOORD = "a_texcoord";

    private final String vertexSource;
    private final String fragmentSource;

    /// Generates the shader pair for a material and vertex layout.
    ///
    /// #### Parameters
    ///
    /// - `material`: the material describing the lighting model and inputs
    ///
    /// - `format`: the mesh vertex layout
    public GlslShaderGenerator(Material material, VertexFormat format) {
        boolean hasNormal = format.findByUsage(VertexAttribute.Usage.NORMAL) != null;
        boolean hasTexcoord = format.findByUsage(VertexAttribute.Usage.TEXCOORD) != null;
        boolean textured = material.getTexture() != null && hasTexcoord;
        Material.Type type = material.getType();
        boolean lit = (type == Material.Type.LAMBERT || type == Material.Type.PHONG) && hasNormal;
        boolean phong = type == Material.Type.PHONG && hasNormal;

        this.vertexSource = buildVertex(lit, textured);
        this.fragmentSource = buildFragment(lit, phong, textured);
    }

    private static String buildVertex(boolean lit, boolean textured) {
        StringBuilder sb = new StringBuilder();
        sb.append("attribute vec3 ").append(A_POSITION).append(";\n");
        if (lit) {
            sb.append("attribute vec3 ").append(A_NORMAL).append(";\n");
        }
        if (textured) {
            sb.append("attribute vec2 ").append(A_TEXCOORD).append(";\n");
        }
        sb.append("uniform mat4 u_mvp;\n");
        if (lit) {
            sb.append("uniform mat4 u_model;\n");
            sb.append("uniform mat4 u_normalMatrix;\n");
            sb.append("varying vec3 v_normal;\n");
            sb.append("varying vec3 v_worldPos;\n");
        }
        if (textured) {
            sb.append("varying vec2 v_texcoord;\n");
        }
        sb.append("void main() {\n");
        if (lit) {
            sb.append("  v_normal = (u_normalMatrix * vec4(").append(A_NORMAL).append(", 0.0)).xyz;\n");
            sb.append("  v_worldPos = (u_model * vec4(").append(A_POSITION).append(", 1.0)).xyz;\n");
        }
        if (textured) {
            sb.append("  v_texcoord = ").append(A_TEXCOORD).append(";\n");
        }
        sb.append("  gl_Position = u_mvp * vec4(").append(A_POSITION).append(", 1.0);\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String buildFragment(boolean lit, boolean phong, boolean textured) {
        StringBuilder sb = new StringBuilder();
        sb.append("precision mediump float;\n");
        sb.append("uniform vec4 u_color;\n");
        if (textured) {
            sb.append("uniform sampler2D u_texture;\n");
            sb.append("varying vec2 v_texcoord;\n");
        }
        if (lit) {
            sb.append("uniform vec3 u_lightDir;\n");
            sb.append("uniform vec3 u_lightColor;\n");
            sb.append("uniform vec3 u_ambient;\n");
            sb.append("varying vec3 v_normal;\n");
            sb.append("varying vec3 v_worldPos;\n");
        }
        if (phong) {
            sb.append("uniform vec3 u_eye;\n");
            sb.append("uniform float u_shininess;\n");
        }
        sb.append("void main() {\n");
        sb.append("  vec4 base = u_color;\n");
        if (textured) {
            sb.append("  base = base * texture2D(u_texture, v_texcoord);\n");
        }
        if (lit) {
            sb.append("  vec3 n = normalize(v_normal);\n");
            sb.append("  vec3 l = normalize(-u_lightDir);\n");
            sb.append("  float ndotl = max(dot(n, l), 0.0);\n");
            sb.append("  vec3 lighting = u_ambient + u_lightColor * ndotl;\n");
            sb.append("  vec3 rgb = base.rgb * lighting;\n");
            if (phong) {
                sb.append("  if (ndotl > 0.0) {\n");
                sb.append("    vec3 v = normalize(u_eye - v_worldPos);\n");
                sb.append("    vec3 h = normalize(l + v);\n");
                sb.append("    float spec = pow(max(dot(n, h), 0.0), u_shininess);\n");
                sb.append("    rgb += u_lightColor * spec;\n");
                sb.append("  }\n");
            }
            sb.append("  gl_FragColor = vec4(rgb, base.a);\n");
        } else {
            sb.append("  gl_FragColor = base;\n");
        }
        sb.append("}\n");
        return sb.toString();
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
