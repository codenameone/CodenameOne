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

/// Port of `Tendon`: binds a [Skin] to a bone with the bone's inverse bind
/// transform.
public class Tendon extends TendonBase {
    private final Mat2D inverseBind = new Mat2D();
    private Bone bone;

    public Bone getBone() {
        return bone;
    }

    public Mat2D getInverseBind() {
        return inverseBind;
    }

    @Override
    public int onAddedDirty(Artboard context) {
        Mat2D bind = new Mat2D().set(mXx, mXy, mYx, mYy, mTx, mTy);
        inverseBind.invert(bind);
        int code = super.onAddedDirty(context);
        if (code != STATUS_OK) {
            return code;
        }
        Core o = context.resolve(mBoneId);
        if (!(o instanceof Bone)) {
            return STATUS_INVALID;
        }
        bone = (Bone) o;
        return STATUS_OK;
    }

    @Override
    public int onAddedClean(Artboard context) {
        if (!(parent instanceof Skin)) {
            return STATUS_MISSING;
        }
        ((Skin) parent).addTendon(this);
        return STATUS_OK;
    }
}
