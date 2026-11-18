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

/// Generated from rive-runtime's `NestedSimpleAnimationBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `NestedSimpleAnimation`.
public abstract class NestedSimpleAnimationBase extends NestedLinearAnimation {
    public static final int TYPE_KEY = 96;
    public static final int SPEED_PROPERTY_KEY = 199;
    public static final int IS_PLAYING_PROPERTY_KEY = 201;

    protected float mSpeed = 1.0f;
    protected boolean mIsPlaying = false;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
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

    public boolean isPlaying() {
        return mIsPlaying;
    }

    public void setIsPlaying(boolean value) {
        if (mIsPlaying == value) {
            return;
        }
        mIsPlaying = value;
        isPlayingChanged();
    }

    protected void isPlayingChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case IS_PLAYING_PROPERTY_KEY:
                mIsPlaying = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case IS_PLAYING_PROPERTY_KEY:
                setIsPlaying(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case IS_PLAYING_PROPERTY_KEY:
                return mIsPlaying;
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
}
