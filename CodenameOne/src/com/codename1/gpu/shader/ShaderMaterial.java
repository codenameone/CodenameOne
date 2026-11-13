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
package com.codename1.gpu.shader;

import com.codename1.gpu.Texture;

/// Custom shading for a `com.codename1.gpu.Material`: a set of `ShaderNode`
/// graphs, each replacing one input of the engine's own shader. What a hook
/// does not replace keeps working as before, so a custom surface is still lit,
/// shadowed, fogged and tone mapped like every other.
///
/// ```java
/// ShaderMaterial waves = new ShaderMaterial()
///         .declareUniform("amp", ShaderType.FLOAT)
///         .set(ShaderMaterial.Hook.VERTEX_OFFSET, ShaderNode.vec(ShaderNode.f(0f),
///                 ShaderNode.sin(ShaderNode.position().x().add(ShaderNode.time()))
///                         .mul(ShaderNode.uniform("amp", ShaderType.FLOAT)), ShaderNode.f(0f)))
///         .setUniform("amp", 0.3f);
/// material.setShader(waves);
/// ```
///
/// Uniform values and textures are per material and may change every frame
/// without recompiling anything. Changing a graph, or declaring a uniform or
/// texture, makes the backend build a new program the next time the material
/// is drawn, so do that once, not per frame.
///
/// Limits, the same on every backend: 16 uniforms and 4 textures per material.
public final class ShaderMaterial {
    /// The most uniforms one material can declare.
    public static final int MAX_UNIFORMS = 16;
    /// The most textures one material can declare.
    public static final int MAX_TEXTURES = 4;

    /// An input of the engine's shader that a graph can replace.
    public enum Hook {
        /// A `VEC3` added to the model space vertex position. Vertex stage.
        VERTEX_OFFSET(ShaderType.VEC3, false),
        /// The `VEC4` surface colour and alpha, in place of the material
        /// colour times its texture.
        BASE_COLOR(ShaderType.VEC4, true),
        /// The `VEC3` world space surface normal used for lighting; it is
        /// normalized for you.
        NORMAL(ShaderType.VEC3, true),
        /// The `FLOAT` metallic factor of a PBR material.
        METALLIC(ShaderType.FLOAT, true),
        /// The `FLOAT` roughness of a PBR material.
        ROUGHNESS(ShaderType.FLOAT, true),
        /// A `VEC3` colour added after lighting, before fog and tone mapping.
        EMISSIVE(ShaderType.VEC3, true),
        /// A `FLOAT`; the pixel is dropped where it is above 0.5. For cut
        /// outs such as leaves, or a dissolve.
        DISCARD(ShaderType.FLOAT, true);

        private final ShaderType type;
        private final boolean fragment;

        Hook(ShaderType type, boolean fragment) {
            this.type = type;
            this.fragment = fragment;
        }

        /// Returns the type a graph for this hook must have.
        ///
        /// #### Returns
        ///
        /// the type
        public ShaderType getType() {
            return type;
        }

        /// Returns true for the fragment stage, false for the vertex stage.
        ///
        /// #### Returns
        ///
        /// the stage
        public boolean isFragment() {
            return fragment;
        }
    }

    private final ShaderNode[] hooks = new ShaderNode[Hook.values().length];
    private final String[] uniformNames = new String[MAX_UNIFORMS];
    private final ShaderType[] uniformTypes = new ShaderType[MAX_UNIFORMS];
    private int uniformCount;
    private final float[] uniformValues = new float[MAX_UNIFORMS * 4];
    private final String[] textureNames = new String[MAX_TEXTURES];
    private final Texture[] textures = new Texture[MAX_TEXTURES];
    private int textureCount;
    private int version;
    private String key;

    /// Sets the graph for a hook, or null to use the engine's own input.
    ///
    /// #### Parameters
    ///
    /// - `hook`: the input to replace
    ///
    /// - `graph`: a node of the hook's type, or null
    ///
    /// #### Returns
    ///
    /// this material
    public ShaderMaterial set(Hook hook, ShaderNode graph) {
        if (graph != null && graph.getType() != hook.getType()) {
            throw new IllegalArgumentException(hook + " takes a " + hook.getType() + ", not a " + graph.getType());
        }
        hooks[hook.ordinal()] = graph;
        changed();
        return this;
    }

    /// Returns the graph set for a hook.
    ///
    /// #### Parameters
    ///
    /// - `hook`: the input
    ///
    /// #### Returns
    ///
    /// the graph, or null
    public ShaderNode get(Hook hook) {
        return hooks[hook.ordinal()];
    }

    /// Declares a uniform the graphs read with `ShaderNode.uniform`.
    /// Declaring a name twice with the same type does nothing.
    ///
    /// #### Parameters
    ///
    /// - `name`: the uniform name
    ///
    /// - `type`: its type
    ///
    /// #### Returns
    ///
    /// this material
    public ShaderMaterial declareUniform(String name, ShaderType type) {
        ShaderNode.checkName(name);
        int i = uniformIndex(name);
        if (i >= 0) {
            if (uniformTypes[i] != type) {
                throw new IllegalArgumentException("Uniform " + name + " is already a " + uniformTypes[i]);
            }
            return this;
        }
        if (uniformCount == MAX_UNIFORMS) {
            throw new IllegalStateException("A material has at most " + MAX_UNIFORMS + " uniforms");
        }
        uniformNames[uniformCount] = name;
        uniformTypes[uniformCount] = type;
        uniformCount++;
        changed();
        return this;
    }

    /// Sets a uniform's value. Pass as many floats as its type has
    /// components; missing ones are zero.
    ///
    /// #### Parameters
    ///
    /// - `name`: a declared uniform
    ///
    /// - `values`: its components
    ///
    /// #### Returns
    ///
    /// this material
    public ShaderMaterial setUniform(String name, float... values) {
        int i = uniformIndex(name);
        if (i < 0) {
            throw new IllegalArgumentException("Undeclared uniform: " + name);
        }
        int n = uniformTypes[i].getComponents();
        for (int c = 0; c < 4; c++) {
            uniformValues[i * 4 + c] = c < n && c < values.length ? values[c] : 0f;
        }
        return this;
    }

    /// Sets a `VEC3` uniform from a packed `0xRRGGBB` colour.
    ///
    /// #### Parameters
    ///
    /// - `name`: a declared uniform
    ///
    /// - `rgb`: the colour
    ///
    /// #### Returns
    ///
    /// this material
    public ShaderMaterial setColorUniform(String name, int rgb) {
        return setUniform(name, ((rgb >> 16) & 0xff) / 255f, ((rgb >> 8) & 0xff) / 255f, (rgb & 0xff) / 255f);
    }

    /// Declares a texture the graphs read with `ShaderNode.sample`.
    ///
    /// #### Parameters
    ///
    /// - `name`: the texture name
    ///
    /// - `texture`: the texture, or null to bind it later with `setTexture`
    ///
    /// #### Returns
    ///
    /// this material
    public ShaderMaterial declareTexture(String name, Texture texture) {
        ShaderNode.checkName(name);
        int i = textureIndex(name);
        if (i < 0) {
            if (textureCount == MAX_TEXTURES) {
                throw new IllegalStateException("A material has at most " + MAX_TEXTURES + " textures");
            }
            i = textureCount++;
            textureNames[i] = name;
            changed();
        }
        textures[i] = texture;
        return this;
    }

    /// Binds a declared texture.
    ///
    /// #### Parameters
    ///
    /// - `name`: the declared name
    ///
    /// - `texture`: the texture
    ///
    /// #### Returns
    ///
    /// this material
    public ShaderMaterial setTexture(String name, Texture texture) {
        int i = textureIndex(name);
        if (i < 0) {
            throw new IllegalArgumentException("Undeclared texture: " + name);
        }
        textures[i] = texture;
        return this;
    }

    private int uniformIndex(String name) {
        for (int i = 0; i < uniformCount; i++) {
            if (uniformNames[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private int textureIndex(String name) {
        for (int i = 0; i < textureCount; i++) {
            if (textureNames[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /// Returns the slot of a uniform: its four floats start at `slot * 4` in
    /// `getUniformValues`. For backends.
    ///
    /// #### Parameters
    ///
    /// - `name`: the uniform name
    ///
    /// #### Returns
    ///
    /// the slot, or -1 if it is not declared
    public int getUniformSlot(String name) {
        return uniformIndex(name);
    }

    /// Returns the type of a declared uniform.
    ///
    /// #### Parameters
    ///
    /// - `name`: the uniform name
    ///
    /// #### Returns
    ///
    /// its type, or null if it is not declared
    public ShaderType getUniformType(String name) {
        int i = uniformIndex(name);
        return i < 0 ? null : uniformTypes[i];
    }

    /// Returns the number of declared uniforms.
    ///
    /// #### Returns
    ///
    /// the count
    public int getUniformCount() {
        return uniformCount;
    }

    /// Returns the uniform values packed four floats per slot, in
    /// declaration order. For backends, which upload `getUniformCount() * 4`
    /// floats of it as one vec4 array. Do not modify it.
    ///
    /// #### Returns
    ///
    /// the shared value array
    public float[] getUniformValues() {
        return uniformValues;
    }

    /// Returns the slot of a texture, its index among the material's custom
    /// textures. For backends.
    ///
    /// #### Parameters
    ///
    /// - `name`: the texture name
    ///
    /// #### Returns
    ///
    /// the slot, or -1 if it is not declared
    public int getTextureSlot(String name) {
        return textureIndex(name);
    }

    /// Returns the number of declared textures.
    ///
    /// #### Returns
    ///
    /// the count
    public int getTextureCount() {
        return textureCount;
    }

    /// Returns the texture bound to a slot.
    ///
    /// #### Parameters
    ///
    /// - `slot`: 0 to `getTextureCount() - 1`
    ///
    /// #### Returns
    ///
    /// the texture, or null when none is bound
    public Texture getTexture(int slot) {
        return textures[slot];
    }

    /// Returns a number that changes whenever the generated program would.
    ///
    /// #### Returns
    ///
    /// the version
    public int getVersion() {
        return version;
    }

    /// Returns a string identifying the generated program: equal for two
    /// materials whose graphs, uniforms and textures produce the same source.
    /// Backends add it to their program cache key.
    ///
    /// #### Returns
    ///
    /// the key
    public String getShaderKey() {
        if (key == null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < uniformCount; i++) {
                sb.append('u').append(uniformNames[i]).append(':').append(uniformTypes[i].getComponents());
            }
            for (int i = 0; i < textureCount; i++) {
                sb.append('t').append(textureNames[i]);
            }
            Hook[] all = Hook.values();
            for (int i = 0; i < all.length; i++) {
                if (hooks[i] != null) {
                    sb.append('|').append(all[i].ordinal()).append('=');
                    ShaderEmitter.describe(hooks[i], sb);
                }
            }
            key = sb.toString();
        }
        return key;
    }

    private void changed() {
        version++;
        key = null;
    }
}
