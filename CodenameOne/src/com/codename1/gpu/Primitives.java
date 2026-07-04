/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.gpu;

/// Factory helpers that build common `Mesh` primitives. Every primitive uses the
/// `VertexFormat.POSITION_NORMAL_TEXCOORD` layout so it can be drawn with any of
/// the built in materials, lit or unlit, textured or not.
public final class Primitives {
    private Primitives() {
    }

    /// Builds a unit-normal axis aligned cube centered at the origin with the
    /// supplied edge length. Each face has its own normals and a full 0..1
    /// texture coordinate quad.
    ///
    /// #### Parameters
    ///
    /// - `device`: the device that allocates the buffers
    ///
    /// - `size`: the edge length of the cube
    ///
    /// #### Returns
    ///
    /// an indexed triangle mesh
    public static Mesh cube(GraphicsDevice device, float size) {
        float h = size * 0.5f;
        // 6 faces * 4 vertices, interleaved px,py,pz, nx,ny,nz, u,v
        float[] v = {
                // front (+z)
                -h, -h, h, 0, 0, 1, 0, 1,
                h, -h, h, 0, 0, 1, 1, 1,
                h, h, h, 0, 0, 1, 1, 0,
                -h, h, h, 0, 0, 1, 0, 0,
                // back (-z)
                h, -h, -h, 0, 0, -1, 0, 1,
                -h, -h, -h, 0, 0, -1, 1, 1,
                -h, h, -h, 0, 0, -1, 1, 0,
                h, h, -h, 0, 0, -1, 0, 0,
                // left (-x)
                -h, -h, -h, -1, 0, 0, 0, 1,
                -h, -h, h, -1, 0, 0, 1, 1,
                -h, h, h, -1, 0, 0, 1, 0,
                -h, h, -h, -1, 0, 0, 0, 0,
                // right (+x)
                h, -h, h, 1, 0, 0, 0, 1,
                h, -h, -h, 1, 0, 0, 1, 1,
                h, h, -h, 1, 0, 0, 1, 0,
                h, h, h, 1, 0, 0, 0, 0,
                // top (+y)
                -h, h, h, 0, 1, 0, 0, 1,
                h, h, h, 0, 1, 0, 1, 1,
                h, h, -h, 0, 1, 0, 1, 0,
                -h, h, -h, 0, 1, 0, 0, 0,
                // bottom (-y)
                -h, -h, -h, 0, -1, 0, 0, 1,
                h, -h, -h, 0, -1, 0, 1, 1,
                h, -h, h, 0, -1, 0, 1, 0,
                -h, -h, h, 0, -1, 0, 0, 0
        };
        int[] idx = new int[36];
        for (int face = 0; face < 6; face++) {
            int b = face * 4;
            int o = face * 6;
            idx[o] = b;
            idx[o + 1] = b + 1;
            idx[o + 2] = b + 2;
            idx[o + 3] = b;
            idx[o + 4] = b + 2;
            idx[o + 5] = b + 3;
        }

        VertexBuffer vb = device.createVertexBuffer(VertexFormat.POSITION_NORMAL_TEXCOORD, 24);
        vb.setData(v);
        IndexBuffer ib = device.createIndexBuffer(36);
        ib.setData(idx);
        return new Mesh(vb, ib, PrimitiveType.TRIANGLES);
    }

    /// Builds a flat quad in the XY plane centered at the origin facing +Z.
    ///
    /// #### Parameters
    ///
    /// - `device`: the device that allocates the buffers
    ///
    /// - `size`: the edge length of the quad
    ///
    /// #### Returns
    ///
    /// an indexed triangle mesh
    public static Mesh quad(GraphicsDevice device, float size) {
        float h = size * 0.5f;
        float[] v = {
                -h, -h, 0, 0, 0, 1, 0, 1,
                h, -h, 0, 0, 0, 1, 1, 1,
                h, h, 0, 0, 0, 1, 1, 0,
                -h, h, 0, 0, 0, 1, 0, 0
        };
        int[] idx = {0, 1, 2, 0, 2, 3};
        VertexBuffer vb = device.createVertexBuffer(VertexFormat.POSITION_NORMAL_TEXCOORD, 4);
        vb.setData(v);
        IndexBuffer ib = device.createIndexBuffer(6);
        ib.setData(idx);
        return new Mesh(vb, ib, PrimitiveType.TRIANGLES);
    }

    /// Builds a UV sphere centered at the origin with equirectangular texture
    /// coordinates: `u` wraps the longitude from 0 to 1 and `v` runs from 0 at
    /// the north pole (+Y) to 1 at the south pole. This is the mapping used by
    /// 360 degree panorama images. The seam column and the pole rows duplicate
    /// vertices so texture coordinates stay continuous.
    ///
    /// #### Parameters
    ///
    /// - `device`: the device that allocates the buffers
    ///
    /// - `radius`: the sphere radius
    ///
    /// - `latBands`: the number of latitude subdivisions, at least 2
    ///
    /// - `lonBands`: the number of longitude subdivisions, at least 3
    ///
    /// - `insideOut`: when true the normals point toward the center and the
    ///   winding is flipped so the inner surface faces the viewer, as needed
    ///   when rendering a panorama from inside the sphere
    ///
    /// #### Returns
    ///
    /// an indexed triangle mesh
    public static Mesh sphere(GraphicsDevice device, float radius, int latBands, int lonBands,
                              boolean insideOut) {
        return sphere(radius, latBands, lonBands, insideOut);
    }

    /// Builds a UV sphere without a `GraphicsDevice`, allocating the buffers
    /// directly. Behaves exactly like
    /// `sphere(GraphicsDevice, float, int, int, boolean)`; useful for preparing
    /// geometry off the render thread or handing meshes to non GPU consumers
    /// such as the AR content pipeline.
    ///
    /// #### Parameters
    ///
    /// - `radius`: the sphere radius
    ///
    /// - `latBands`: the number of latitude subdivisions, at least 2
    ///
    /// - `lonBands`: the number of longitude subdivisions, at least 3
    ///
    /// - `insideOut`: when true the normals point toward the center and the
    ///   winding is flipped
    ///
    /// #### Returns
    ///
    /// an indexed triangle mesh
    public static Mesh sphere(float radius, int latBands, int lonBands, boolean insideOut) {
        if (radius <= 0.0f) {
            throw new IllegalArgumentException("radius must be positive");
        }
        if (latBands < 2) {
            throw new IllegalArgumentException("latBands must be at least 2");
        }
        if (lonBands < 3) {
            throw new IllegalArgumentException("lonBands must be at least 3");
        }
        int vertexCount = (latBands + 1) * (lonBands + 1);
        if (vertexCount > 65536) {
            throw new IllegalArgumentException("sphere tessellation exceeds the 16 bit index range");
        }
        float[] v = new float[vertexCount * 8];
        int o = 0;
        for (int lat = 0; lat <= latBands; lat++) {
            double theta = Math.PI * lat / latBands;
            float sinTheta = (float) Math.sin(theta);
            float cosTheta = (float) Math.cos(theta);
            for (int lon = 0; lon <= lonBands; lon++) {
                double phi = 2.0 * Math.PI * lon / lonBands;
                float nx = sinTheta * (float) Math.sin(phi);
                float ny = cosTheta;
                float nz = sinTheta * (float) Math.cos(phi);
                v[o] = nx * radius;
                v[o + 1] = ny * radius;
                v[o + 2] = nz * radius;
                if (insideOut) {
                    v[o + 3] = -nx;
                    v[o + 4] = -ny;
                    v[o + 5] = -nz;
                } else {
                    v[o + 3] = nx;
                    v[o + 4] = ny;
                    v[o + 5] = nz;
                }
                v[o + 6] = (float) lon / lonBands;
                v[o + 7] = (float) lat / latBands;
                o += 8;
            }
        }
        int[] idx = new int[latBands * lonBands * 6];
        int i = 0;
        for (int lat = 0; lat < latBands; lat++) {
            for (int lon = 0; lon < lonBands; lon++) {
                int first = lat * (lonBands + 1) + lon;
                int second = first + lonBands + 1;
                if (insideOut) {
                    idx[i] = first;
                    idx[i + 1] = first + 1;
                    idx[i + 2] = second;
                    idx[i + 3] = second;
                    idx[i + 4] = first + 1;
                    idx[i + 5] = second + 1;
                } else {
                    idx[i] = first;
                    idx[i + 1] = second;
                    idx[i + 2] = first + 1;
                    idx[i + 3] = second;
                    idx[i + 4] = second + 1;
                    idx[i + 5] = first + 1;
                }
                i += 6;
            }
        }
        VertexBuffer vb = new VertexBuffer(VertexFormat.POSITION_NORMAL_TEXCOORD, vertexCount);
        vb.setData(v);
        IndexBuffer ib = new IndexBuffer(idx.length);
        ib.setData(idx);
        return new Mesh(vb, ib, PrimitiveType.TRIANGLES);
    }
}
