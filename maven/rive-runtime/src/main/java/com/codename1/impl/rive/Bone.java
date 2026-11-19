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

/// Port of `Bone`: a transform component placed at the tip of its parent bone.
public class Bone extends BoneBase {
    private final ArrayList<Bone> childBones = new ArrayList<Bone>();
    private final ArrayList<Constraint> peerConstraints = new ArrayList<Constraint>();

    public void addChildBone(Bone bone) {
        childBones.add(bone);
    }

    public ArrayList<Bone> getChildBones() {
        return childBones;
    }

    /// Constraints on a descendant bone that also move this bone (IK chains).
    public void addPeerConstraint(Constraint peer) {
        if (!peerConstraints.contains(peer)) {
            peerConstraints.add(peer);
        }
    }

    public ArrayList<Constraint> getPeerConstraints() {
        return peerConstraints;
    }

    /// False for [RootBone], which may sit under any transform component.
    protected boolean requiresParentBone() {
        return true;
    }

    @Override
    public int onAddedClean(Artboard context) {
        int code = super.onAddedClean(context);
        if (!requiresParentBone()) {
            return code;
        }
        if (!(parent instanceof Bone)) {
            return STATUS_MISSING;
        }
        ((Bone) parent).addChildBone(this);
        return STATUS_OK;
    }

    @Override
    protected void lengthChanged() {
        for (int i = 0; i < childBones.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated per frame
            childBones.get(i).markTransformDirty();
        }
    }

    @Override
    public float getX() {
        return parent instanceof Bone ? ((Bone) parent).getLength() : 0f;
    }

    @Override
    public float getY() {
        return 0f;
    }

    public float getTipWorldX() {
        return worldTransform.mapX(mLength, 0f);
    }

    public float getTipWorldY() {
        return worldTransform.mapY(mLength, 0f);
    }
}
