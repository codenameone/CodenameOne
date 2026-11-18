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

/// Generated from rive-runtime's `NestedArtboardBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `NestedArtboard`.
public abstract class NestedArtboardBase extends Drawable {
    public static final int TYPE_KEY = 92;
    public static final int ARTBOARD_ID_PROPERTY_KEY = 197;
    public static final int DATA_BIND_PATH_IDS_PROPERTY_KEY = 582;
    public static final int IS_PAUSED_PROPERTY_KEY = 895;
    public static final int SPEED_PROPERTY_KEY = 907;
    public static final int QUANTIZE_PROPERTY_KEY = 908;
    public static final int IS_STATEFUL_PROPERTY_KEY = 1014;

    protected int mArtboardId = -1;
    protected byte[] mDataBindPathIds = null;
    protected boolean mIsPaused = false;
    protected float mSpeed = 1.0f;
    protected float mQuantize = -1.0f;
    protected boolean mIsStateful = false;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getArtboardId() {
        return mArtboardId;
    }

    public void setArtboardId(int value) {
        if (mArtboardId == value) {
            return;
        }
        mArtboardId = value;
        artboardIdChanged();
    }

    protected void artboardIdChanged() {
    }

    public byte[] getDataBindPathIds() {
        return mDataBindPathIds;
    }

    protected void decodeDataBindPathIds(byte[] value) {
        mDataBindPathIds = value;
    }

    public boolean isPaused() {
        return mIsPaused;
    }

    public void setIsPaused(boolean value) {
        if (mIsPaused == value) {
            return;
        }
        mIsPaused = value;
        isPausedChanged();
    }

    protected void isPausedChanged() {
    }

    public float getSpeed() {
        return mSpeed;
    }

    public void setSpeed(float value) {
        if (Float.floatToIntBits(mSpeed) == Float.floatToIntBits(value)) {
            return;
        }
        mSpeed = value;
        speedChanged();
    }

    protected void speedChanged() {
    }

    public float getQuantize() {
        return mQuantize;
    }

    public void setQuantize(float value) {
        if (Float.floatToIntBits(mQuantize) == Float.floatToIntBits(value)) {
            return;
        }
        mQuantize = value;
        quantizeChanged();
    }

    protected void quantizeChanged() {
    }

    public boolean isStateful() {
        return mIsStateful;
    }

    public void setIsStateful(boolean value) {
        if (mIsStateful == value) {
            return;
        }
        mIsStateful = value;
        isStatefulChanged();
    }

    protected void isStatefulChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case IS_PAUSED_PROPERTY_KEY:
                mIsPaused = value;
                return true;
            case IS_STATEFUL_PROPERTY_KEY:
                mIsStateful = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case IS_PAUSED_PROPERTY_KEY:
                setIsPaused(value);
                return true;
            case IS_STATEFUL_PROPERTY_KEY:
                setIsStateful(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case IS_PAUSED_PROPERTY_KEY:
                return mIsPaused;
            case IS_STATEFUL_PROPERTY_KEY:
                return mIsStateful;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeBytes(int propertyKey, byte[] value) {
        switch (propertyKey) {
            case DATA_BIND_PATH_IDS_PROPERTY_KEY:
                decodeDataBindPathIds(value);
                return true;
            default:
                return super.deserializeBytes(propertyKey, value);
        }
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case SPEED_PROPERTY_KEY:
                mSpeed = value;
                return true;
            case QUANTIZE_PROPERTY_KEY:
                mQuantize = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case SPEED_PROPERTY_KEY:
                setSpeed(value);
                return true;
            case QUANTIZE_PROPERTY_KEY:
                setQuantize(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case SPEED_PROPERTY_KEY:
                return mSpeed;
            case QUANTIZE_PROPERTY_KEY:
                return mQuantize;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case ARTBOARD_ID_PROPERTY_KEY:
                mArtboardId = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case ARTBOARD_ID_PROPERTY_KEY:
                setArtboardId(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case ARTBOARD_ID_PROPERTY_KEY:
                return mArtboardId;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
