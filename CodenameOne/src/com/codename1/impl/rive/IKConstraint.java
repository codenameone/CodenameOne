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

/// Port of `IKConstraint`: rotates a chain of `parentBoneCount + 1` bones so
/// the tip reaches the target. One and two bone chains are solved
/// analytically; longer chains run the two bone solve from each link to the
/// tip, as upstream does.
public class IKConstraint extends IKConstraintBase {
    private BoneChainLink[] fkChain = new BoneChainLink[0];
    private final Mat2D scratch = new Mat2D();

    /// One bone of the solved chain.
    static final class BoneChainLink {
        int index;
        Bone bone;
        float angle;
        final Mat2D parentWorldInverse = new Mat2D();
        final float[] components = new float[6];
        float baseRotation;
        boolean solved;
        boolean ours;
        final Mat2D solvedLocal = new Mat2D();
    }

    @Override
    public void buildDependencies() {
        super.buildDependencies();
        if (target != null) {
            target.addDependent(this);
        }
    }

    @Override
    public int onAddedClean(Artboard context) {
        if (!(parent instanceof Bone)) {
            return STATUS_INVALID;
        }
        int boneCount = mParentBoneCount;
        Bone bone = (Bone) parent;
        ArrayList<Bone> bones = new ArrayList<Bone>();
        bones.add(bone);
        while (bone.getParent() instanceof Bone && boneCount > 0) {
            boneCount--;
            bone = (Bone) bone.getParent();
            bone.addPeerConstraint(this);
            bones.add(bone);
        }
        int numBones = bones.size();
        fkChain = new BoneChainLink[numBones];
        for (int i = 0; i < numBones; i++) {
            BoneChainLink link = new BoneChainLink();
            link.index = i;
            link.bone = bones.get(numBones - 1 - i);
            fkChain[i] = link;
        }
        Bone tip = (Bone) parent;
        for (int i = 1; i < numBones; i++) {
            Bone ancestor = bones.get(i);
            Bone chainChild = bones.get(i - 1);
            ArrayList<Component> children = ancestor.getChildren();
            for (int j = 0; j < children.size(); j++) { // NOPMD ForLoopCanBeForeach - indexed
                Component child = children.get(j);
                if (!(child instanceof TransformComponent) || child == chainChild) { // NOPMD CompareObjectsWithEquals - identity is the intent
                    continue;
                }
                tip.addDependent(child);
            }
        }
        return super.onAddedClean(context);
    }

    @Override
    public void markConstraintDirty() {
        super.markConstraintDirty();
        for (int i = 0; i < fkChain.length - 1; i++) {
            fkChain[i].bone.markTransformDirty();
        }
    }

    @Override
    protected void invertDirectionChanged() {
        markConstraintDirty();
    }

    @Override
    protected void parentBoneCountChanged() {
        markConstraintDirty();
    }

    private static float atan2(float x, float y) {
        return (float) com.codename1.util.MathUtil.atan2(y, x);
    }

    private static float clamp1(float v) {
        return Math.max(-1.0f, Math.min(1.0f, v));
    }

    private void solve1(BoneChainLink fk1, float tx, float ty) {
        Mat2D iworld = fk1.parentWorldInverse;
        float[] w = fk1.bone.getWorldTransform().v;
        float dx = tx - w[4];
        float dy = ty - w[5];
        float[] i = iworld.v;
        float lx = i[0] * dx + i[2] * dy;
        float ly = i[1] * dx + i[3] * dy;
        float r = atan2(lx, ly);
        constrainRotation(fk1, r);
        fk1.angle = r;
    }

    private void solve2(BoneChainLink fk1, BoneChainLink fk2, float tx, float ty) {
        Bone b1 = fk1.bone;
        Bone b2 = fk2.bone;
        BoneChainLink firstChild = fkChain[fk1.index + 1];
        Mat2D iw = fk1.parentWorldInverse;
        float[] wa = b1.getWorldTransform().v;
        float[] wc = firstChild.bone.getWorldTransform().v;
        float pAx = iw.mapX(wa[4], wa[5]);
        float pAy = iw.mapY(wa[4], wa[5]);
        float pCx = iw.mapX(wc[4], wc[5]);
        float pCy = iw.mapY(wc[4], wc[5]);
        float tipX = b2.getTipWorldX();
        float tipY = b2.getTipWorldY();
        float pBx = iw.mapX(tipX, tipY);
        float pBy = iw.mapY(tipX, tipY);
        float pTx = iw.mapX(tx, ty);
        float pTy = iw.mapY(tx, ty);
        float avx = pBx - pCx;
        float avy = pBy - pCy;
        float bvx = pCx - pAx;
        float bvy = pCy - pAy;
        float cvx = pTx - pAx;
        float cvy = pTy - pAy;
        float a = (float) Math.sqrt(avx * avx + avy * avy);
        float b = (float) Math.sqrt(bvx * bvx + bvy * bvy);
        float c = (float) Math.sqrt(cvx * cvx + cvy * cvy);
        float angA = (float) com.codename1.util.MathUtil.acos(clamp1((-a * a + b * b + c * c) / (2.0f * b * c)));
        float angC = (float) com.codename1.util.MathUtil.acos(clamp1((a * a + b * b - c * c) / (2.0f * a * b)));
        float r1;
        float r2;
        if (b2.getParent() != b1) { // NOPMD CompareObjectsWithEquals - identity is the intent
            BoneChainLink secondChild = fkChain[fk1.index + 2];
            float[] si = secondChild.parentWorldInverse.v;
            float[] wc2 = firstChild.bone.getWorldTransform().v;
            float dx = b2.getTipWorldX() - wc2[4];
            float dy = b2.getTipWorldY() - wc2[5];
            float lx = si[0] * dx + si[2] * dy;
            float ly = si[1] * dx + si[3] * dy;
            float angleCorrection = -atan2(lx, ly);
            if (mInvertDirection) {
                r1 = atan2(cvx, cvy) - angA;
                r2 = -angC + PI + angleCorrection;
            } else {
                r1 = angA + atan2(cvx, cvy);
                r2 = angC - PI + angleCorrection;
            }
        } else if (mInvertDirection) {
            r1 = atan2(cvx, cvy) - angA;
            r2 = -angC + PI;
        } else {
            r1 = angA + atan2(cvx, cvy);
            r2 = angC - PI;
        }
        constrainRotation(fk1, r1);
        constrainRotation(firstChild, r2);
        if (firstChild != fk2) { // NOPMD CompareObjectsWithEquals - identity is the intent
            fk2.bone.composeWorldTransform();
        }
        fk1.angle = r1;
        firstChild.angle = r2;
    }

    private static void constrainRotation(BoneChainLink fk, float rotation) {
        Bone bone = fk.bone;
        Mat2D transform = bone.getTransform();
        float[] c = fk.components;
        transform.compose(c[0], c[1], rotation, c[3], c[4]);
        float skew = c[5];
        if (skew != 0.0f) {
            float[] t = transform.v;
            t[2] = t[0] * skew + t[2];
            t[3] = t[1] * skew + t[3];
        }
        bone.composeWorldTransform();
    }

    private static boolean sameMatrix(Mat2D a, Mat2D b) {
        for (int i = 0; i < 6; i++) {
            if (Float.floatToIntBits(a.v[i]) != Float.floatToIntBits(b.v[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean holdsOurSolve(BoneChainLink link) {
        if (!link.solved) {
            return false;
        }
        scratch.multiply(getParentWorld(link.bone), link.solvedLocal);
        return sameMatrix(scratch, link.bone.getWorldTransform());
    }

    private static void refreshInverse(BoneChainLink link) {
        link.parentWorldInverse.invert(getParentWorld(link.bone));
    }

    @Override
    public void constrain(TransformComponent component) {
        TransformComponent tgt = target;
        if (tgt == null || tgt.isCollapsed()) {
            return;
        }
        float tx = tgt.getWorldTransform().v[4];
        float ty = tgt.getWorldTransform().v[5];
        int count = fkChain.length;
        for (int i = 0; i < count; i++) {
            BoneChainLink item = fkChain[i];
            refreshInverse(item);
            scratch.multiply(item.parentWorldInverse, item.bone.getWorldTransform());
            scratch.decompose(item.components);
            item.ours = holdsOurSolve(item);
            if (item.ours) {
                item.components[2] = item.baseRotation;
            }
            item.baseRotation = item.components[2];
        }
        boolean rebuilt = false;
        for (int i = 0; i < count; i++) {
            BoneChainLink item = fkChain[i];
            rebuilt = rebuilt || item.ours;
            if (rebuilt) {
                refreshInverse(item);
                constrainRotation(item, item.components[2]);
            }
        }
        if (count == 1) {
            solve1(fkChain[0], tx, ty);
        } else if (count == 2) {
            solve2(fkChain[0], fkChain[1], tx, ty);
        } else if (count > 2) {
            int last = count - 1;
            BoneChainLink tip = fkChain[last];
            for (int i = 0; i < last; i++) {
                BoneChainLink item = fkChain[i];
                solve2(item, tip, tx, ty);
                for (int j = item.index + 1; j < count - 1; j++) {
                    refreshInverse(fkChain[j]);
                }
            }
        }
        if (mStrength != 1.0f) {
            for (int i = 0; i < count; i++) {
                BoneChainLink fk = fkChain[i];
                float fromAngle = fk.components[2] % PI2;
                float toAngle = fk.angle % PI2;
                float diff = wrapDiff(toAngle - fromAngle);
                constrainRotation(fk, fromAngle + diff * mStrength);
            }
        }
        for (int i = 0; i < count; i++) {
            BoneChainLink item = fkChain[i];
            item.solvedLocal.set(item.bone.getTransform());
            item.solved = true;
        }
    }
}
