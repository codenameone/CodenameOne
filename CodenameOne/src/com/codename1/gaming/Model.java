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
    private float[] instances;
    private int[] instanceColors;
    private int instanceCount;
    private float[] worldInstances;
    private final float[] one = new float[16];
    private final float[] oneWorld = new float[16];

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
    /// array is reused each call -- copy it if you need to keep it. The three
    /// rotations are always applied (an identity rotation is cheap) and the
    /// multiplications alternate between two scratch buffers so no `Matrix4#multiply`
    /// ever aliases its source and destination.
    float[] modelMatrix() {
        float[] scale = Matrix4.scaling(scaleX, scaleY, scaleZ);
        Matrix4.multiply(Matrix4.rotation((float) Math.toRadians(rotX), 1f, 0f, 0f), scale, scratchA);
        Matrix4.multiply(Matrix4.rotation((float) Math.toRadians(rotY), 0f, 1f, 0f), scratchA, scratchB);
        Matrix4.multiply(Matrix4.rotation((float) Math.toRadians(rotZ), 0f, 0f, 1f), scratchB, scratchA);
        Matrix4.multiply(Matrix4.translation(x, y, z), scratchA, matrix);
        return matrix;
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

    /// Draws this model as many copies, each placed by its own transform
    /// relative to the model's position, rotation and scale: a stack of
    /// crates, a row of lamp posts, a field of grass from one blade. The
    /// arrays are read every frame, so moving the copies is writing new
    /// values into them; they are not copied.
    ///
    /// #### Parameters
    ///
    /// - `matrices`: 16 column-major floats per copy, or null to draw the
    ///   model once again
    ///
    /// - `colors`: an ARGB colour per copy for `ShaderNode.instanceColor`,
    ///   or null for white
    ///
    /// - `count`: how many copies to draw
    ///
    /// #### Returns
    ///
    /// this model
    public Model setInstances(float[] matrices, int[] colors, int count) {
        if (matrices != null && matrices.length < count * 16) {
            throw new IllegalArgumentException("Need " + count * 16 + " floats for " + count + " copies");
        }
        this.instances = matrices;
        this.instanceColors = colors;
        this.instanceCount = matrices == null ? 0 : count;
        return this;
    }

    /// Returns how many copies `setInstances` asked for, 0 for a single model.
    public int getInstanceCount() {
        return instanceCount;
    }

    int[] instanceColors() {
        return instanceColors;
    }

    /// The copies' world transforms for this frame, reused between calls.
    float[] instanceMatrices() {
        float[] base = modelMatrix();
        if (worldInstances == null || worldInstances.length < instanceCount * 16) {
            worldInstances = new float[instanceCount * 16];
        }
        for (int i = 0; i < instanceCount; i++) {
            System.arraycopy(instances, i * 16, one, 0, 16);
            Matrix4.multiply(base, one, oneWorld);
            System.arraycopy(oneWorld, 0, worldInstances, i * 16, 16);
        }
        return worldInstances;
    }
}
