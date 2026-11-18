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

/// Generated from rive-runtime's `ListFollowPathConstraintBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `ListFollowPathConstraint`.
public abstract class ListFollowPathConstraintBase extends FollowPathConstraint {
    public static final int TYPE_KEY = 625;
    public static final int DISTANCE_END_PROPERTY_KEY = 888;
    public static final int DISTANCE_OFFSET_PROPERTY_KEY = 889;

    protected float mDistanceEnd = 1.0f;
    protected float mDistanceOffset = 0.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getDistanceEnd() {
        return mDistanceEnd;
    }

    public void setDistanceEnd(float value) {
        if (Float.floatToIntBits(mDistanceEnd) == Float.floatToIntBits(value)) {
            return;
        }
        mDistanceEnd = value;
        distanceEndChanged();
    }

    protected void distanceEndChanged() {
    }

    public float getDistanceOffset() {
        return mDistanceOffset;
    }

    public void setDistanceOffset(float value) {
        if (Float.floatToIntBits(mDistanceOffset) == Float.floatToIntBits(value)) {
            return;
        }
        mDistanceOffset = value;
        distanceOffsetChanged();
    }

    protected void distanceOffsetChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case DISTANCE_END_PROPERTY_KEY:
                mDistanceEnd = value;
                return true;
            case DISTANCE_OFFSET_PROPERTY_KEY:
                mDistanceOffset = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case DISTANCE_END_PROPERTY_KEY:
                setDistanceEnd(value);
                return true;
            case DISTANCE_OFFSET_PROPERTY_KEY:
                setDistanceOffset(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case DISTANCE_END_PROPERTY_KEY:
                return mDistanceEnd;
            case DISTANCE_OFFSET_PROPERTY_KEY:
                return mDistanceOffset;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }
}
