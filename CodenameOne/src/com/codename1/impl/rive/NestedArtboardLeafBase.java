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

/// Generated from rive-runtime's `NestedArtboardLeafBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `NestedArtboardLeaf`.
public abstract class NestedArtboardLeafBase extends NestedArtboard {
    public static final int TYPE_KEY = 451;
    public static final int FIT_PROPERTY_KEY = 538;
    public static final int FIT_TO_LAYOUT_PARENT_PROPERTY_KEY = 1098;
    public static final int ALIGNMENT_X_PROPERTY_KEY = 644;
    public static final int ALIGNMENT_Y_PROPERTY_KEY = 645;

    protected int mFit = 0;
    protected boolean mFitToLayoutParent = false;
    protected float mAlignmentX = 0.0f;
    protected float mAlignmentY = 0.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getFit() {
        return mFit;
    }

    public void setFit(int value) {
        if (mFit == value) {
            return;
        }
        mFit = value;
        fitChanged();
    }

    protected void fitChanged() {
    }

    public boolean isFitToLayoutParent() {
        return mFitToLayoutParent;
    }

    public void setFitToLayoutParent(boolean value) {
        if (mFitToLayoutParent == value) {
            return;
        }
        mFitToLayoutParent = value;
        fitToLayoutParentChanged();
    }

    protected void fitToLayoutParentChanged() {
    }

    public float getAlignmentX() {
        return mAlignmentX;
    }

    public void setAlignmentX(float value) {
        if (Float.floatToIntBits(mAlignmentX) == Float.floatToIntBits(value)) {
            return;
        }
        mAlignmentX = value;
        alignmentXChanged();
    }

    protected void alignmentXChanged() {
    }

    public float getAlignmentY() {
        return mAlignmentY;
    }

    public void setAlignmentY(float value) {
        if (Float.floatToIntBits(mAlignmentY) == Float.floatToIntBits(value)) {
            return;
        }
        mAlignmentY = value;
        alignmentYChanged();
    }

    protected void alignmentYChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case FIT_TO_LAYOUT_PARENT_PROPERTY_KEY:
                mFitToLayoutParent = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case FIT_TO_LAYOUT_PARENT_PROPERTY_KEY:
                setFitToLayoutParent(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case FIT_TO_LAYOUT_PARENT_PROPERTY_KEY:
                return mFitToLayoutParent;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case ALIGNMENT_X_PROPERTY_KEY:
                mAlignmentX = value;
                return true;
            case ALIGNMENT_Y_PROPERTY_KEY:
                mAlignmentY = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case ALIGNMENT_X_PROPERTY_KEY:
                setAlignmentX(value);
                return true;
            case ALIGNMENT_Y_PROPERTY_KEY:
                setAlignmentY(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case ALIGNMENT_X_PROPERTY_KEY:
                return mAlignmentX;
            case ALIGNMENT_Y_PROPERTY_KEY:
                return mAlignmentY;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case FIT_PROPERTY_KEY:
                mFit = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case FIT_PROPERTY_KEY:
                setFit(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case FIT_PROPERTY_KEY:
                return mFit;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
