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

/// Generated from rive-runtime's `StateTransitionBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `StateTransition`.
public abstract class StateTransitionBase extends StateMachineLayerComponent {
    public static final int TYPE_KEY = 65;
    public static final int STATE_TO_ID_PROPERTY_KEY = 151;
    public static final int FLAGS_PROPERTY_KEY = 152;
    public static final int DURATION_PROPERTY_KEY = 158;
    public static final int EXIT_TIME_PROPERTY_KEY = 160;
    public static final int INTERPOLATION_TYPE_PROPERTY_KEY = 349;
    public static final int INTERPOLATOR_ID_PROPERTY_KEY = 350;
    public static final int RANDOM_WEIGHT_PROPERTY_KEY = 537;

    protected int mStateToId = -1;
    protected int mFlags = 0;
    protected int mDuration = 0;
    protected int mExitTime = 0;
    protected int mInterpolationType = 1;
    protected int mInterpolatorId = -1;
    protected int mRandomWeight = 1;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getStateToId() {
        return mStateToId;
    }

    public void setStateToId(int value) {
        if (mStateToId == value) {
            return;
        }
        mStateToId = value;
        stateToIdChanged();
    }

    protected void stateToIdChanged() {
    }

    public int getFlags() {
        return mFlags;
    }

    public void setFlags(int value) {
        if (mFlags == value) {
            return;
        }
        mFlags = value;
        flagsChanged();
    }

    protected void flagsChanged() {
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

    public int getExitTime() {
        return mExitTime;
    }

    public void setExitTime(int value) {
        if (mExitTime == value) {
            return;
        }
        mExitTime = value;
        exitTimeChanged();
    }

    protected void exitTimeChanged() {
    }

    public int getInterpolationType() {
        return mInterpolationType;
    }

    public void setInterpolationType(int value) {
        if (mInterpolationType == value) {
            return;
        }
        mInterpolationType = value;
        interpolationTypeChanged();
    }

    protected void interpolationTypeChanged() {
    }

    public int getInterpolatorId() {
        return mInterpolatorId;
    }

    public void setInterpolatorId(int value) {
        if (mInterpolatorId == value) {
            return;
        }
        mInterpolatorId = value;
        interpolatorIdChanged();
    }

    protected void interpolatorIdChanged() {
    }

    public int getRandomWeight() {
        return mRandomWeight;
    }

    public void setRandomWeight(int value) {
        if (mRandomWeight == value) {
            return;
        }
        mRandomWeight = value;
        randomWeightChanged();
    }

    protected void randomWeightChanged() {
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case STATE_TO_ID_PROPERTY_KEY:
                mStateToId = value;
                return true;
            case FLAGS_PROPERTY_KEY:
                mFlags = value;
                return true;
            case DURATION_PROPERTY_KEY:
                mDuration = value;
                return true;
            case EXIT_TIME_PROPERTY_KEY:
                mExitTime = value;
                return true;
            case INTERPOLATION_TYPE_PROPERTY_KEY:
                mInterpolationType = value;
                return true;
            case INTERPOLATOR_ID_PROPERTY_KEY:
                mInterpolatorId = value;
                return true;
            case RANDOM_WEIGHT_PROPERTY_KEY:
                mRandomWeight = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case STATE_TO_ID_PROPERTY_KEY:
                setStateToId(value);
                return true;
            case FLAGS_PROPERTY_KEY:
                setFlags(value);
                return true;
            case DURATION_PROPERTY_KEY:
                setDuration(value);
                return true;
            case EXIT_TIME_PROPERTY_KEY:
                setExitTime(value);
                return true;
            case INTERPOLATION_TYPE_PROPERTY_KEY:
                setInterpolationType(value);
                return true;
            case INTERPOLATOR_ID_PROPERTY_KEY:
                setInterpolatorId(value);
                return true;
            case RANDOM_WEIGHT_PROPERTY_KEY:
                setRandomWeight(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case STATE_TO_ID_PROPERTY_KEY:
                return mStateToId;
            case FLAGS_PROPERTY_KEY:
                return mFlags;
            case DURATION_PROPERTY_KEY:
                return mDuration;
            case EXIT_TIME_PROPERTY_KEY:
                return mExitTime;
            case INTERPOLATION_TYPE_PROPERTY_KEY:
                return mInterpolationType;
            case INTERPOLATOR_ID_PROPERTY_KEY:
                return mInterpolatorId;
            case RANDOM_WEIGHT_PROPERTY_KEY:
                return mRandomWeight;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
