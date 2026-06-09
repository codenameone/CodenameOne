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
package com.codename1.gaming;

import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;

/// A 3D mesh placed in the world: a `com.codename1.gpu.Mesh` plus a
/// `com.codename1.gpu.Material` and a position / rotation / scale transform. Add one
/// to a `GameView` (`GameView#addModel(Model)`) to draw it in the perspective camera
/// alongside the billboarded sprites.
///
/// Because GPU meshes need the `com.codename1.gpu.GraphicsDevice` to be created, build
/// your models from inside `GameView#onSetup(com.codename1.gpu.GraphicsDevice)`:
///
/// ```java
/// protected void onSetup(GraphicsDevice device) {
///     Mesh cubeMesh = Primitives.cube(device, 1f);
///     Material gold = new Material(Material.Type.PHONG).setColor(0xffffcc33).setShininess(32);
///     Model crate = new Model(cubeMesh, gold);
///     crate.setPosition(0, 0.5f, 0);
///     addModel(crate);
/// }
/// ```
///
/// Rotation is applied in Z, then Y, then X (extrinsic), in degrees. The model is
/// drawn with the `GameView`'s shared `com.codename1.gpu.Light`, so lit material
/// types (`com.codename1.gpu.Material.Type#LAMBERT`/`#PHONG`) are shaded by it.
public class Model {
    private Mesh mesh;
    private Material material;

    private float x;
    private float y;
    private float z;
    private float rotX;
    private float rotY;
    private float rotZ;
    private float scaleX = 1f;
    private float scaleY = 1f;
    private float scaleZ = 1f;
    private boolean visible = true;
    private Object userData;

    private final float[] scratchA = new float[16];
    private final float[] scratchB = new float[16];
    private final float[] matrix = new float[16];

    /// Creates a model for the given mesh with a default unlit white material.
    public Model(Mesh mesh) {
        this(mesh, new Material());
    }

    /// Creates a model for the given mesh and material.
    public Model(Mesh mesh, Material material) {
        this.mesh = mesh;
        this.material = material;
    }

    /// Builds the world transform `T * Rz * Ry * Rx * S` for this model. The returned
    /// array is reused each call -- copy it if you need to keep it. Rotations
    /// ping-pong between two scratch buffers so no `Matrix4#multiply` ever aliases
    /// its source and destination.
    float[] modelMatrix() {
        // `cur` starts as a fresh scaling matrix, then each applied rotation writes
        // into the scratch buffer `cur` is NOT currently in.
        float[] cur = Matrix4.scaling(scaleX, scaleY, scaleZ);
        if (rotX != 0f) {
            float[] dst = next(cur);
            Matrix4.multiply(Matrix4.rotation((float) Math.toRadians(rotX), 1f, 0f, 0f), cur, dst);
            cur = dst;
        }
        if (rotY != 0f) {
            float[] dst = next(cur);
            Matrix4.multiply(Matrix4.rotation((float) Math.toRadians(rotY), 0f, 1f, 0f), cur, dst);
            cur = dst;
        }
        if (rotZ != 0f) {
            float[] dst = next(cur);
            Matrix4.multiply(Matrix4.rotation((float) Math.toRadians(rotZ), 0f, 0f, 1f), cur, dst);
            cur = dst;
        }
        Matrix4.multiply(Matrix4.translation(x, y, z), cur, matrix);
        return matrix;
    }

    /// The scratch buffer that is not `cur` (so a multiply into it never aliases).
    private float[] next(float[] cur) {
        return cur == scratchA ? scratchB : scratchA;
    }

    public Mesh getMesh() {
        return mesh;
    }

    public void setMesh(Mesh mesh) {
        this.mesh = mesh;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public Model setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /// Sets the Euler rotation in degrees (applied Z, then Y, then X).
    public Model setRotation(float degX, float degY, float degZ) {
        this.rotX = degX;
        this.rotY = degY;
        this.rotZ = degZ;
        return this;
    }

    public float getRotationX() {
        return rotX;
    }

    public float getRotationY() {
        return rotY;
    }

    public float getRotationZ() {
        return rotZ;
    }

    public Model setScale(float scale) {
        this.scaleX = scale;
        this.scaleY = scale;
        this.scaleZ = scale;
        return this;
    }

    public Model setScale(float scaleX, float scaleY, float scaleZ) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
        return this;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Object getUserData() {
        return userData;
    }

    public void setUserData(Object userData) {
        this.userData = userData;
    }
}
