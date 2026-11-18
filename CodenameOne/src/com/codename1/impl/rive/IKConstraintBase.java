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

/// Generated from rive-runtime's `IKConstraintBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `IKConstraint`.
public abstract class IKConstraintBase extends TargetedConstraint {
    public static final int TYPE_KEY = 81;
    public static final int INVERT_DIRECTION_PROPERTY_KEY = 174;
    public static final int PARENT_BONE_COUNT_PROPERTY_KEY = 175;

    protected boolean mInvertDirection = false;
    protected int mParentBoneCount = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public boolean isInvertDirection() {
        return mInvertDirection;
    }

    public void setInvertDirection(boolean value) {
        if (mInvertDirection == value) {
            return;
        }
        mInvertDirection = value;
        invertDirectionChanged();
    }

    protected void invertDirectionChanged() {
    }

    public int getParentBoneCount() {
        return mParentBoneCount;
    }

    public void setParentBoneCount(int value) {
        if (mParentBoneCount == value) {
            return;
        }
        mParentBoneCount = value;
        parentBoneCountChanged();
    }

    protected void parentBoneCountChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case INVERT_DIRECTION_PROPERTY_KEY:
                mInvertDirection = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case INVERT_DIRECTION_PROPERTY_KEY:
                setInvertDirection(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case INVERT_DIRECTION_PROPERTY_KEY:
                return mInvertDirection;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case PARENT_BONE_COUNT_PROPERTY_KEY:
                mParentBoneCount = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case PARENT_BONE_COUNT_PROPERTY_KEY:
                setParentBoneCount(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case PARENT_BONE_COUNT_PROPERTY_KEY:
                return mParentBoneCount;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
