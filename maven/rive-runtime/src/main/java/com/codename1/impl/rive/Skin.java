/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

import java.util.ArrayList;

/// Port of `Skin`: deforms a [Skinnable]'s vertices by the bones of its
/// tendons.
public class Skin extends SkinBase {
    private final Mat2D worldTransform = new Mat2D();
    private final ArrayList<Tendon> tendons = new ArrayList<Tendon>();
    private final Mat2D scratch = new Mat2D();
    private float[] boneTransforms;
    private Skinnable skinnable;

    public void addTendon(Tendon tendon) {
        tendons.add(tendon);
    }

    public ArrayList<Tendon> getTendons() {
        return tendons;
    }

    /// The bind transform of the skinned geometry.
    public Mat2D getBindTransform() {
        return worldTransform;
    }

    /// `(tendons + 1) * 6` floats; slot 0 is identity.
    public float[] getBoneTransforms() {
        return boneTransforms;
    }

    @Override
    public int onAddedDirty(Artboard context) {
        int code = super.onAddedDirty(context);
        if (code != STATUS_OK) {
            return code;
        }
        worldTransform.set(mXx, mXy, mYx, mYy, mTx, mTy);
        if (!(parent instanceof Skinnable)) {
            // Upstream answers MissingObject for a parent that can never be
            // skinned, and keeps the object either way.
            return STATUS_OK;
        }
        skinnable = (Skinnable) parent;
        skinnable.setSkin(this);
        return STATUS_OK;
    }

    @Override
    public void buildDependencies() {
        for (int i = 0; i < tendons.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            Bone bone = tendons.get(i).getBone();
            bone.addDependent(this);
            ArrayList<Constraint> peers = bone.getPeerConstraints();
            for (int j = 0; j < peers.size(); j++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
                ContainerComponent p = peers.get(j).getParent();
                if (p != null) {
                    p.addDependent(this);
                }
            }
        }
        boneTransforms = new float[(tendons.size() + 1) * 6];
        boneTransforms[0] = 1;
        boneTransforms[3] = 1;
    }

    @Override
    public void update(int value) {
        int b = 6;
        for (int i = 0; i < tendons.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            Tendon t = tendons.get(i);
            scratch.multiply(t.getBone().getWorldTransform(), t.getInverseBind());
            float[] w = scratch.v;
            for (int k = 0; k < 6; k++) {
                boneTransforms[b++] = w[k];
            }
        }
    }

    /// Port of `Skin::deform`.
    public void deform(ArrayList<? extends Vertex> vertices) {
        if (boneTransforms == null) {
            return;
        }
        for (int i = 0; i < vertices.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            vertices.get(i).deform(worldTransform, boneTransforms);
        }
    }

    @Override
    protected void onDirty(int value) {
        if ((value & ~DIRT_COLLAPSED) == DIRT_RENDER_OPACITY) {
            return;
        }
        if (skinnable != null) {
            skinnable.markSkinDirty();
        }
    }
}
