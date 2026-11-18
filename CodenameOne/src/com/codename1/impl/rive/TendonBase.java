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

/// Generated from rive-runtime's `TendonBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `Tendon`.
public abstract class TendonBase extends Component {
    public static final int TYPE_KEY = 44;
    public static final int BONE_ID_PROPERTY_KEY = 95;
    public static final int XX_PROPERTY_KEY = 96;
    public static final int YX_PROPERTY_KEY = 97;
    public static final int XY_PROPERTY_KEY = 98;
    public static final int YY_PROPERTY_KEY = 99;
    public static final int TX_PROPERTY_KEY = 100;
    public static final int TY_PROPERTY_KEY = 101;

    protected int mBoneId = -1;
    protected float mXx = 1.0f;
    protected float mYx = 0.0f;
    protected float mXy = 0.0f;
    protected float mYy = 1.0f;
    protected float mTx = 0.0f;
    protected float mTy = 0.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getBoneId() {
        return mBoneId;
    }

    public void setBoneId(int value) {
        if (mBoneId == value) {
            return;
        }
        mBoneId = value;
        boneIdChanged();
    }

    protected void boneIdChanged() {
    }

    public float getXx() {
        return mXx;
    }

    public void setXx(float value) {
        if (Float.floatToIntBits(mXx) == Float.floatToIntBits(value)) {
            return;
        }
        mXx = value;
        xxChanged();
    }

    protected void xxChanged() {
    }

    public float getYx() {
        return mYx;
    }

    public void setYx(float value) {
        if (Float.floatToIntBits(mYx) == Float.floatToIntBits(value)) {
            return;
        }
        mYx = value;
        yxChanged();
    }

    protected void yxChanged() {
    }

    public float getXy() {
        return mXy;
    }

    public void setXy(float value) {
        if (Float.floatToIntBits(mXy) == Float.floatToIntBits(value)) {
            return;
        }
        mXy = value;
        xyChanged();
    }

    protected void xyChanged() {
    }

    public float getYy() {
        return mYy;
    }

    public void setYy(float value) {
        if (Float.floatToIntBits(mYy) == Float.floatToIntBits(value)) {
            return;
        }
        mYy = value;
        yyChanged();
    }

    protected void yyChanged() {
    }

    public float getTx() {
        return mTx;
    }

    public void setTx(float value) {
        if (Float.floatToIntBits(mTx) == Float.floatToIntBits(value)) {
            return;
        }
        mTx = value;
        txChanged();
    }

    protected void txChanged() {
    }

    public float getTy() {
        return mTy;
    }

    public void setTy(float value) {
        if (Float.floatToIntBits(mTy) == Float.floatToIntBits(value)) {
            return;
        }
        mTy = value;
        tyChanged();
    }

    protected void tyChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case XX_PROPERTY_KEY:
                mXx = value;
                return true;
            case YX_PROPERTY_KEY:
                mYx = value;
                return true;
            case XY_PROPERTY_KEY:
                mXy = value;
                return true;
            case YY_PROPERTY_KEY:
                mYy = value;
                return true;
            case TX_PROPERTY_KEY:
                mTx = value;
                return true;
            case TY_PROPERTY_KEY:
                mTy = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case XX_PROPERTY_KEY:
                setXx(value);
                return true;
            case YX_PROPERTY_KEY:
                setYx(value);
                return true;
            case XY_PROPERTY_KEY:
                setXy(value);
                return true;
            case YY_PROPERTY_KEY:
                setYy(value);
                return true;
            case TX_PROPERTY_KEY:
                setTx(value);
                return true;
            case TY_PROPERTY_KEY:
                setTy(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case XX_PROPERTY_KEY:
                return mXx;
            case YX_PROPERTY_KEY:
                return mYx;
            case XY_PROPERTY_KEY:
                return mXy;
            case YY_PROPERTY_KEY:
                return mYy;
            case TX_PROPERTY_KEY:
                return mTx;
            case TY_PROPERTY_KEY:
                return mTy;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case BONE_ID_PROPERTY_KEY:
                mBoneId = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case BONE_ID_PROPERTY_KEY:
                setBoneId(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case BONE_ID_PROPERTY_KEY:
                return mBoneId;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
