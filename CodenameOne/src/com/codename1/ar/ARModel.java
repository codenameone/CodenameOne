/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ar;

import com.codename1.gpu.GltfLoader;
import com.codename1.gpu.Mesh;
import com.codename1.ui.Image;

import java.io.IOException;
import java.io.InputStream;

/// The renderable content of an `ARNode`: geometry plus an optional base-color
/// texture or solid color. Models are immutable descriptors - the platform
/// backend uploads them into its native renderer when the node is attached to
/// an anchor.
///
/// Create a model either from a glTF 2.0 asset (`.glb` or `.gltf` bytes,
/// parsed with `com.codename1.gpu.GltfLoader`) or directly from a
/// `com.codename1.gpu.Mesh` such as one built by
/// `com.codename1.gpu.Primitives`. Geometry units are meters in the anchor's
/// local frame.
public final class ARModel {
    private final byte[] gltfBytes;
    private Mesh mesh;
    private Image baseColorImage;
    private final int color;
    private boolean parsed;

    private ARModel(byte[] gltfBytes, Mesh mesh, Image baseColorImage, int color) {
        this.gltfBytes = gltfBytes;
        this.mesh = mesh;
        this.baseColorImage = baseColorImage;
        this.color = color;
        this.parsed = gltfBytes == null;
    }

    /// Creates a model from in-memory glTF 2.0 bytes (binary `.glb` or JSON
    /// `.gltf`). The geometry and base-color texture are parsed lazily on
    /// first access.
    ///
    /// #### Parameters
    ///
    /// - `glbOrGltf`: the raw model bytes
    ///
    /// #### Returns
    ///
    /// the model descriptor
    public static ARModel fromGltf(byte[] glbOrGltf) {
        if (glbOrGltf == null || glbOrGltf.length == 0) {
            throw new IllegalArgumentException("model bytes are required");
        }
        byte[] copy = new byte[glbOrGltf.length];
        System.arraycopy(glbOrGltf, 0, copy, 0, glbOrGltf.length);
        return new ARModel(copy, null, null, 0xffffffff);
    }

    /// Reads all bytes from the stream and creates a glTF model. The stream is
    /// closed.
    ///
    /// #### Parameters
    ///
    /// - `in`: a stream over `.glb` or `.gltf` bytes
    ///
    /// #### Returns
    ///
    /// the model descriptor
    public static ARModel fromGltf(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) >= 0) {
                out.write(buf, 0, r);
            }
        } finally {
            com.codename1.io.Util.cleanup(in);
        }
        return fromGltf(out.toByteArray());
    }

    /// Creates a model from a mesh rendered in a single solid color.
    ///
    /// #### Parameters
    ///
    /// - `mesh`: the geometry, in meters
    ///
    /// - `argbColor`: the solid base color as `0xAARRGGBB`
    ///
    /// #### Returns
    ///
    /// the model descriptor
    public static ARModel fromMesh(Mesh mesh, int argbColor) {
        if (mesh == null) {
            throw new IllegalArgumentException("mesh is required");
        }
        return new ARModel(null, mesh, null, argbColor);
    }

    /// Creates a model from a mesh with a base-color texture.
    ///
    /// #### Parameters
    ///
    /// - `mesh`: the geometry, in meters, with texture coordinates
    ///
    /// - `texture`: the base-color image
    ///
    /// #### Returns
    ///
    /// the model descriptor
    public static ARModel fromMesh(Mesh mesh, Image texture) {
        if (mesh == null) {
            throw new IllegalArgumentException("mesh is required");
        }
        if (texture == null) {
            throw new IllegalArgumentException("texture is required");
        }
        return new ARModel(null, mesh, texture, 0xffffffff);
    }

    /// The raw glTF bytes as a newly allocated array, or null for mesh-based
    /// models.
    public byte[] getGltfBytes() {
        if (gltfBytes == null) {
            return null;
        }
        byte[] copy = new byte[gltfBytes.length];
        System.arraycopy(gltfBytes, 0, copy, 0, gltfBytes.length);
        return copy;
    }

    /// The model geometry. For glTF models the geometry is parsed on first
    /// access.
    public synchronized Mesh getMesh() {
        parseIfNeeded();
        return mesh;
    }

    /// The decoded base-color image, or null when the model has none. For
    /// glTF models the image is extracted on first access.
    public synchronized Image getBaseColorImage() {
        parseIfNeeded();
        return baseColorImage;
    }

    /// The solid base color as `0xAARRGGBB`, used when the model carries no
    /// texture. Defaults to opaque white.
    public int getColor() {
        return color;
    }

    private void parseIfNeeded() {
        if (!parsed) {
            GltfLoader.GltfImageModel m = GltfLoader.loadImageModel(gltfBytes);
            mesh = m.getMesh();
            baseColorImage = m.getBaseColorImage();
            parsed = true;
        }
    }
}
