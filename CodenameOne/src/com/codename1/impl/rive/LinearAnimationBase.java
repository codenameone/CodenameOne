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

/// Generated from rive-runtime's `LinearAnimationBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `LinearAnimation`.
public abstract class LinearAnimationBase extends Animation {
    public static final int TYPE_KEY = 31;
    public static final int FPS_PROPERTY_KEY = 56;
    public static final int DURATION_PROPERTY_KEY = 57;
    public static final int SPEED_PROPERTY_KEY = 58;
    public static final int LOOP_VALUE_PROPERTY_KEY = 59;
    public static final int WORK_START_PROPERTY_KEY = 60;
    public static final int WORK_END_PROPERTY_KEY = 61;
    public static final int ENABLE_WORK_AREA_PROPERTY_KEY = 62;
    public static final int QUANTIZE_PROPERTY_KEY = 376;

    protected int mFps = 60;
    protected int mDuration = 60;
    protected float mSpeed = 1.0f;
    protected int mLoopValue = 0;
    protected int mWorkStart = -1;
    protected int mWorkEnd = -1;
    protected boolean mEnableWorkArea = false;
    protected boolean mQuantize = false;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getFps() {
        return mFps;
    }

    public void setFps(int value) {
        if (mFps == value) {
            return;
        }
        mFps = value;
        fpsChanged();
    }

    protected void fpsChanged() {
    }

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int value) {
        if (mDuration == value) {
            return;
        }
        mDuration = value;
        durationChanged();
    }

    protected void durationChanged() {
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

    public int getLoopValue() {
        return mLoopValue;
    }

    public void setLoopValue(int value) {
        if (mLoopValue == value) {
            return;
        }
        mLoopValue = value;
        loopValueChanged();
    }

    protected void loopValueChanged() {
    }

    public int getWorkStart() {
        return mWorkStart;
    }

    public void setWorkStart(int value) {
        if (mWorkStart == value) {
            return;
        }
        mWorkStart = value;
        workStartChanged();
    }

    protected void workStartChanged() {
    }

    public int getWorkEnd() {
        return mWorkEnd;
    }

    public void setWorkEnd(int value) {
        if (mWorkEnd == value) {
            return;
        }
        mWorkEnd = value;
        workEndChanged();
    }

    protected void workEndChanged() {
    }

    public boolean isEnableWorkArea() {
        return mEnableWorkArea;
    }

    public void setEnableWorkArea(boolean value) {
        if (mEnableWorkArea == value) {
            return;
        }
        mEnableWorkArea = value;
        enableWorkAreaChanged();
    }

    protected void enableWorkAreaChanged() {
    }

    public boolean isQuantize() {
        return mQuantize;
    }

    public void setQuantize(boolean value) {
        if (mQuantize == value) {
            return;
        }
        mQuantize = value;
        quantizeChanged();
    }

    protected void quantizeChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case ENABLE_WORK_AREA_PROPERTY_KEY:
                mEnableWorkArea = value;
                return true;
            case QUANTIZE_PROPERTY_KEY:
                mQuantize = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case ENABLE_WORK_AREA_PROPERTY_KEY:
                setEnableWorkArea(value);
                return true;
            case QUANTIZE_PROPERTY_KEY:
                setQuantize(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case ENABLE_WORK_AREA_PROPERTY_KEY:
                return mEnableWorkArea;
            case QUANTIZE_PROPERTY_KEY:
                return mQuantize;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case SPEED_PROPERTY_KEY:
                mSpeed = value;
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
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case SPEED_PROPERTY_KEY:
                return mSpeed;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case FPS_PROPERTY_KEY:
                mFps = value;
                return true;
            case DURATION_PROPERTY_KEY:
                mDuration = value;
                return true;
            case LOOP_VALUE_PROPERTY_KEY:
                mLoopValue = value;
                return true;
            case WORK_START_PROPERTY_KEY:
                mWorkStart = value;
                return true;
            case WORK_END_PROPERTY_KEY:
                mWorkEnd = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case FPS_PROPERTY_KEY:
                setFps(value);
                return true;
            case DURATION_PROPERTY_KEY:
                setDuration(value);
                return true;
            case LOOP_VALUE_PROPERTY_KEY:
                setLoopValue(value);
                return true;
            case WORK_START_PROPERTY_KEY:
                setWorkStart(value);
                return true;
            case WORK_END_PROPERTY_KEY:
                setWorkEnd(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case FPS_PROPERTY_KEY:
                return mFps;
            case DURATION_PROPERTY_KEY:
                return mDuration;
            case LOOP_VALUE_PROPERTY_KEY:
                return mLoopValue;
            case WORK_START_PROPERTY_KEY:
                return mWorkStart;
            case WORK_END_PROPERTY_KEY:
                return mWorkEnd;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
