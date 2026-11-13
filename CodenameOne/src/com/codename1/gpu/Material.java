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
package com.codename1.gpu;

import com.codename1.gpu.shader.ShaderMaterial;
import com.codename1.gpu.shader.ShaderNode;
import com.codename1.gpu.shader.ShaderType;

/// A declarative description of how a surface should be shaded. The 3D engine is
/// responsible for translating a material into a platform shader (GLSL on OpenGL
/// ES and WebGL, Metal Shading Language on iOS); applications never write shader
/// source. A material combines a lighting model (`Type`), a base color, an
/// optional texture and pipeline `RenderState`.
public final class Material {
    /// The built in lighting model used to shade a surface.
    public enum Type {
        /// Flat, unlit shading. The fragment color is the base color modulated
        /// by the texture and vertex color. Ideal for UI, sprites and emissive
        /// surfaces.
        UNLIT,
        /// Diffuse only (Lambert) lighting using a single directional light.
        LAMBERT,
        /// Diffuse and specular (Blinn-Phong) lighting using a single
        /// directional light and the `shininess` property.
        PHONG,
        /// Physically based shading (metallic/roughness with a GGX specular
        /// lobe, the model glTF describes) using the directional light, the
        /// environment's hemisphere ambient and, when the light casts them,
        /// shadows. See `setMetallic`, `setRoughness` and `setEmissive`.
        PBR,
        /// Unlit shading intended for screen aligned sprites and billboards.
        SPRITE,
        /// Unlit shading sampling a background cube/sky texture, rendered behind
        /// all other geometry.
        SKYBOX
    }

    private Type type = Type.UNLIT;
    private int color = 0xffffffff;
    private Texture texture;
    private float shininess = 32.0f;
    private RenderState renderState = RenderState.opaque();
    private float metallic;
    private float roughness = 0.8f;
    private int emissive = 0xff000000;
    private boolean receiveShadows = true;
    private boolean castShadows = true;
    private ShaderMaterial shader;
    private Texture normalMap;
    private float normalStrength = 1f;
    private Texture detailNormal;
    private float detailScale = 1f;
    private float detailScrollU;
    private float detailScrollV;
    private float detailStrength = 1f;
    private Texture metallicRoughnessMap;
    private Texture occlusionMap;
    private float occlusionStrength = 1f;
    private Texture emissiveMap;
    private ShaderMaterial composed;
    private int composedUserVersion = -1;
    private ShaderMaterial composedUser;
    private boolean mapsChanged = true;

    /// Creates an unlit white material.
    public Material() {
    }

    /// Creates a material of the supplied type.
    ///
    /// #### Parameters
    ///
    /// - `type`: the lighting model
    public Material(Type type) {
        this.type = type;
    }

    /// Returns the lighting model.
    public Type getType() {
        return type;
    }

    /// Sets the lighting model.
    ///
    /// #### Parameters
    ///
    /// - `type`: the lighting model
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setType(Type type) {
        this.type = type;
        return this;
    }

    /// Returns the base color as a packed ARGB integer.
    public int getColor() {
        return color;
    }

    /// Sets the base color as a packed ARGB integer (0xAARRGGBB).
    ///
    /// #### Parameters
    ///
    /// - `argb`: the packed ARGB color
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setColor(int argb) {
        this.color = argb;
        return this;
    }

    /// Returns the diffuse texture, or null when the material is untextured.
    public Texture getTexture() {
        return texture;
    }

    /// Sets the diffuse texture. Pass null for an untextured material.
    ///
    /// #### Parameters
    ///
    /// - `texture`: the diffuse texture or null
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setTexture(Texture texture) {
        this.texture = texture;
        return this;
    }

    /// Returns the Phong specular exponent.
    public float getShininess() {
        return shininess;
    }

    /// Sets the Phong specular exponent (used only by `Type.PHONG`).
    ///
    /// #### Parameters
    ///
    /// - `shininess`: the specular exponent
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setShininess(float shininess) {
        this.shininess = shininess;
        return this;
    }

    /// Returns the pipeline render state.
    public RenderState getRenderState() {
        return renderState;
    }

    /// Sets the pipeline render state.
    ///
    /// #### Parameters
    ///
    /// - `renderState`: the render state
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setRenderState(RenderState renderState) {
        this.renderState = renderState;
        return this;
    }

    /// Returns how metallic a `Type.PBR` surface is, from 0 (a dielectric such
    /// as paint, plastic or wood) to 1 (bare metal).
    public float getMetallic() {
        return metallic;
    }

    /// Sets how metallic a `Type.PBR` surface is.
    ///
    /// #### Parameters
    ///
    /// - `metallic`: 0 for a dielectric, 1 for metal
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setMetallic(float metallic) {
        this.metallic = Math.max(0f, Math.min(1f, metallic));
        return this;
    }

    /// Returns how rough a `Type.PBR` surface is, from 0 (mirror) to 1 (chalk).
    public float getRoughness() {
        return roughness;
    }

    /// Sets how rough a `Type.PBR` surface is. Rough surfaces spread their
    /// highlight wide and dim; smooth ones make it small and bright.
    ///
    /// #### Parameters
    ///
    /// - `roughness`: 0 for a mirror, 1 for fully diffuse
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setRoughness(float roughness) {
        this.roughness = Math.max(0.04f, Math.min(1f, roughness));
        return this;
    }

    /// Returns the light the surface gives off by itself, as RGB in an ARGB
    /// int; black for none.
    public int getEmissive() {
        return emissive;
    }

    /// Sets the light the surface gives off by itself: a lamp, a screen, wet
    /// paint catching the sun. Emissive colour is added after lighting and is
    /// what a bloom post effect picks up. Used by `Type.PBR`, `Type.LAMBERT`
    /// and `Type.PHONG`.
    ///
    /// #### Parameters
    ///
    /// - `argb`: the emitted colour; black for none
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setEmissive(int argb) {
        this.emissive = argb | 0xff000000;
        return this;
    }

    /// Returns true if the surface is darkened by shadows the light casts.
    public boolean isReceiveShadows() {
        return receiveShadows;
    }

    /// Sets whether the surface is darkened by the light's shadows. A ground
    /// plane receives them; a particle or a muzzle flash should not.
    ///
    /// #### Parameters
    ///
    /// - `receiveShadows`: true to receive shadows
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setReceiveShadows(boolean receiveShadows) {
        this.receiveShadows = receiveShadows;
        return this;
    }

    /// Returns true if geometry drawn with this material casts a shadow.
    public boolean isCastShadows() {
        return castShadows;
    }

    /// Sets whether geometry drawn with this material casts a shadow.
    /// Transparent and unlit effects normally should not.
    ///
    /// #### Parameters
    ///
    /// - `castShadows`: true to cast shadows
    ///
    /// #### Returns
    ///
    /// this material for chaining
    public Material setCastShadows(boolean castShadows) {
        this.castShadows = castShadows;
        return this;
    }

    /// Returns true if this material is lit by the directional light.
    public boolean isLit() {
        return type == Type.LAMBERT || type == Type.PHONG || type == Type.PBR;
    }

    /// Returns a stable string identifying the shader variant required by this
    /// material. Backends use it together with the mesh `VertexFormat` to cache
    /// generated and compiled shader programs. The key intentionally depends
    /// only on properties that change the generated source, not on values such
    /// as the color which are passed as uniforms.
    ///
    /// #### Returns
    ///
    /// a shader variant cache key
    public String getShaderKey() {
        String k = type.name() + (texture != null ? "|tex" : "|notex")
                + (receiveShadows ? "|rs" : "|nrs") + ((emissive & 0xffffff) != 0 ? "|em" : "");
        ShaderMaterial eff = getEffectiveShader();
        return eff == null ? k : k + "|sh:" + eff.getShaderKey();
    }

    /// Returns the custom shading attached to this material.
    ///
    /// #### Returns
    ///
    /// the shader, or null for the engine's own
    public ShaderMaterial getShader() {
        return shader;
    }

    /// Attaches custom shading: graphs that replace inputs of the shader the
    /// material's type selects. The library in `com.codename1.gpu.shaders`
    /// builds ready made ones.
    ///
    /// #### Parameters
    ///
    /// - `shader`: the shader, or null for the engine's own
    ///
    /// #### Returns
    ///
    /// this material
    public Material setShader(ShaderMaterial shader) {
        this.shader = shader;
        return this;
    }

    /// Sets a tangent space normal map, sampled with the primary texture
    /// coordinate. The tangent frame comes from the surface normal (see
    /// `ShaderNode.perturbNormal`), so a mesh needs no tangents. Lit types
    /// only.
    ///
    /// #### Parameters
    ///
    /// - `map`: the normal map, or null for none
    ///
    /// - `strength`: 1 for the map as authored
    ///
    /// #### Returns
    ///
    /// this material
    public Material setNormalMap(Texture map, float strength) {
        normalMap = map;
        normalStrength = strength;
        mapsChanged = true;
        return this;
    }

    /// Returns the normal map.
    ///
    /// #### Returns
    ///
    /// the map, or null
    public Texture getNormalMap() {
        return normalMap;
    }

    /// Sets a tiling detail normal map, layered over the normal map, that can
    /// scroll with time: ripples on water, grain on stone.
    ///
    /// #### Parameters
    ///
    /// - `map`: a tileable normal map, or null for none
    ///
    /// - `scale`: how many times it repeats per unit of texture coordinate
    ///
    /// - `scrollU`: texture coordinate units per second along u
    ///
    /// - `scrollV`: texture coordinate units per second along v
    ///
    /// - `strength`: 1 for the map as authored
    ///
    /// #### Returns
    ///
    /// this material
    public Material setDetailNormal(Texture map, float scale, float scrollU, float scrollV, float strength) {
        detailNormal = map;
        detailScale = scale;
        detailScrollU = scrollU;
        detailScrollV = scrollV;
        detailStrength = strength;
        mapsChanged = true;
        return this;
    }

    /// Returns the detail normal map.
    ///
    /// #### Returns
    ///
    /// the map, or null
    public Texture getDetailNormal() {
        return detailNormal;
    }

    /// Sets a glTF style metallic-roughness map: roughness in green and
    /// metallic in blue, each multiplying the material's own factor. PBR only.
    ///
    /// #### Parameters
    ///
    /// - `map`: the map, or null for none
    ///
    /// #### Returns
    ///
    /// this material
    public Material setMetallicRoughnessMap(Texture map) {
        metallicRoughnessMap = map;
        mapsChanged = true;
        return this;
    }

    /// Returns the metallic-roughness map.
    ///
    /// #### Returns
    ///
    /// the map, or null
    public Texture getMetallicRoughnessMap() {
        return metallicRoughnessMap;
    }

    /// Sets an ambient occlusion map, read from its red channel, that darkens
    /// creases and contact areas.
    ///
    /// #### Parameters
    ///
    /// - `map`: the map, or null for none
    ///
    /// - `strength`: 0 for no effect, 1 for the full map
    ///
    /// #### Returns
    ///
    /// this material
    public Material setOcclusionMap(Texture map, float strength) {
        occlusionMap = map;
        occlusionStrength = strength;
        mapsChanged = true;
        return this;
    }

    /// Returns the occlusion map.
    ///
    /// #### Returns
    ///
    /// the map, or null
    public Texture getOcclusionMap() {
        return occlusionMap;
    }

    /// Sets an emissive map, multiplied by the emissive colour (or by white
    /// when that is black): windows lit at night, glowing runes.
    ///
    /// #### Parameters
    ///
    /// - `map`: the map, or null for none
    ///
    /// #### Returns
    ///
    /// this material
    public Material setEmissiveMap(Texture map) {
        emissiveMap = map;
        mapsChanged = true;
        return this;
    }

    /// Returns the emissive map.
    ///
    /// #### Returns
    ///
    /// the map, or null
    public Texture getEmissiveMap() {
        return emissiveMap;
    }

    private boolean hasMaps() {
        return normalMap != null || detailNormal != null || metallicRoughnessMap != null
                || occlusionMap != null || emissiveMap != null;
    }

    /// Returns the shading backends draw this material with: the attached
    /// `ShaderMaterial` combined with the graphs the texture maps on this
    /// material need. A hook the attached shader sets wins over a map. For
    /// backends; applications use `getShader`.
    ///
    /// #### Returns
    ///
    /// the shader to draw with, or null for the engine's own
    public ShaderMaterial getEffectiveShader() {
        if (!hasMaps()) {
            return shader;
        }
        int userVersion = shader == null ? -1 : shader.getVersion();
        if (composed == null || mapsChanged || composedUser != shader || composedUserVersion != userVersion) { //NOPMD CompareObjectsWithEquals - a different ShaderMaterial instance needs a new composition
            composed = compose();
            composedUser = shader;
            composedUserVersion = userVersion;
            mapsChanged = false;
        }
        syncComposed();
        return composed;
    }

    private static final String U_MR = "cn1MetalRough";
    private static final String U_DETAIL = "cn1Detail";
    private static final String U_EMIT = "cn1EmitColor";

    private ShaderMaterial compose() {
        ShaderMaterial m = new ShaderMaterial();
        ShaderMaterial.Hook[] hooks = ShaderMaterial.Hook.values();
        if (shader != null) {
            for (int i = 0; i < shader.getUniformCount(); i++) {
                m.declareUniform(shader.getUniformName(i), shader.getUniformType(i));
            }
            for (int i = 0; i < shader.getTextureCount(); i++) {
                m.declareTexture(shader.getTextureName(i), shader.getTexture(i));
            }
            for (ShaderMaterial.Hook h : hooks) {
                m.set(h, shader.get(h));
            }
        }
        ShaderNode uv = ShaderNode.uv();
        if (m.get(ShaderMaterial.Hook.NORMAL) == null && (normalMap != null || detailNormal != null)) {
            ShaderNode tn = null;
            if (normalMap != null) {
                m.declareTexture("cn1NormalMap", normalMap);
                tn = ShaderNode.unpackNormal(ShaderNode.sample("cn1NormalMap", uv), normalStrength);
            }
            if (detailNormal != null) {
                m.declareTexture("cn1DetailMap", detailNormal);
                m.declareUniform(U_DETAIL, ShaderType.VEC3);
                ShaderNode d = ShaderNode.uniform(U_DETAIL, ShaderType.VEC3);
                ShaderNode duv = uv.mul(d.x()).add(d.swizzle("yz").mul(ShaderNode.time()));
                ShaderNode dn = ShaderNode.unpackNormal(ShaderNode.sample("cn1DetailMap", duv), detailStrength);
                tn = tn == null ? dn : ShaderNode.blendNormals(tn, dn);
            }
            m.set(ShaderMaterial.Hook.NORMAL, ShaderNode.perturbNormal(tn));
        }
        if (metallicRoughnessMap != null && (m.get(ShaderMaterial.Hook.METALLIC) == null
                || m.get(ShaderMaterial.Hook.ROUGHNESS) == null)) {
            m.declareTexture("cn1MrMap", metallicRoughnessMap);
            m.declareUniform(U_MR, ShaderType.VEC2);
            ShaderNode mr = ShaderNode.sample("cn1MrMap", uv);
            ShaderNode factors = ShaderNode.uniform(U_MR, ShaderType.VEC2);
            if (m.get(ShaderMaterial.Hook.METALLIC) == null) {
                m.set(ShaderMaterial.Hook.METALLIC, mr.z().mul(factors.x()));
            }
            if (m.get(ShaderMaterial.Hook.ROUGHNESS) == null) {
                m.set(ShaderMaterial.Hook.ROUGHNESS, mr.y().mul(factors.y()));
            }
        }
        if (occlusionMap != null && m.get(ShaderMaterial.Hook.BASE_COLOR) == null) {
            m.declareTexture("cn1AoMap", occlusionMap);
            ShaderNode ao = ShaderNode.mix(ShaderNode.f(1f), ShaderNode.sample("cn1AoMap", uv).x(),
                    ShaderNode.f(occlusionStrength));
            ShaderNode base = ShaderNode.baseColor();
            m.set(ShaderMaterial.Hook.BASE_COLOR, ShaderNode.vec(base.rgb().mul(ao), base.w()));
        }
        if (emissiveMap != null && m.get(ShaderMaterial.Hook.EMISSIVE) == null) {
            m.declareTexture("cn1EmitMap", emissiveMap);
            m.declareUniform(U_EMIT, ShaderType.VEC3);
            m.set(ShaderMaterial.Hook.EMISSIVE, ShaderNode.sample("cn1EmitMap", uv).rgb()
                    .mul(ShaderNode.uniform(U_EMIT, ShaderType.VEC3)));
        }
        return m;
    }

    private void syncComposed() {
        if (shader != null) {
            for (int i = 0; i < shader.getUniformCount(); i++) {
                float[] v = shader.getUniformValues();
                composed.setUniform(shader.getUniformName(i), v[i * 4], v[i * 4 + 1], v[i * 4 + 2], v[i * 4 + 3]);
            }
            for (int i = 0; i < shader.getTextureCount(); i++) {
                composed.setTexture(shader.getTextureName(i), shader.getTexture(i));
            }
        }
        if (composed.getUniformSlot(U_MR) >= 0) {
            composed.setUniform(U_MR, metallic, roughness);
        }
        if (composed.getUniformSlot(U_DETAIL) >= 0) {
            composed.setUniform(U_DETAIL, detailScale, detailScrollU, detailScrollV);
        }
        if (composed.getUniformSlot(U_EMIT) >= 0) {
            if ((emissive & 0xffffff) == 0) {
                composed.setUniform(U_EMIT, 1f, 1f, 1f);
            } else {
                composed.setColorUniform(U_EMIT, emissive);
            }
        }
    }
}
