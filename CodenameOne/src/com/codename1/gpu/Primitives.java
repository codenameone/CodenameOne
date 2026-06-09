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
}
