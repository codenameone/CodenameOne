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

/// Generated from rive-runtime's `ElasticScrollPhysicsBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `ElasticScrollPhysics`.
public abstract class ElasticScrollPhysicsBase extends ScrollPhysics {
    public static final int TYPE_KEY = 525;
    public static final int FRICTION_PROPERTY_KEY = 728;
    public static final int SPEED_MULTIPLIER_PROPERTY_KEY = 729;
    public static final int ELASTIC_FACTOR_PROPERTY_KEY = 730;

    protected float mFriction = 8.0f;
    protected float mSpeedMultiplier = 1.0f;
    protected float mElasticFactor = 0.66f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getFriction() {
        return mFriction;
    }

    public void setFriction(float value) {
        if (Float.floatToIntBits(mFriction) == Float.floatToIntBits(value)) {
            return;
        }
        mFriction = value;
        frictionChanged();
    }

    protected void frictionChanged() {
    }

    public float getSpeedMultiplier() {
        return mSpeedMultiplier;
    }

    public void setSpeedMultiplier(float value) {
        if (Float.floatToIntBits(mSpeedMultiplier) == Float.floatToIntBits(value)) {
            return;
        }
        mSpeedMultiplier = value;
        speedMultiplierChanged();
    }

    protected void speedMultiplierChanged() {
    }

    public float getElasticFactor() {
        return mElasticFactor;
    }

    public void setElasticFactor(float value) {
        if (Float.floatToIntBits(mElasticFactor) == Float.floatToIntBits(value)) {
            return;
        }
        mElasticFactor = value;
        elasticFactorChanged();
    }

    protected void elasticFactorChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case FRICTION_PROPERTY_KEY:
                mFriction = value;
                return true;
            case SPEED_MULTIPLIER_PROPERTY_KEY:
                mSpeedMultiplier = value;
                return true;
            case ELASTIC_FACTOR_PROPERTY_KEY:
                mElasticFactor = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case FRICTION_PROPERTY_KEY:
                setFriction(value);
                return true;
            case SPEED_MULTIPLIER_PROPERTY_KEY:
                setSpeedMultiplier(value);
                return true;
            case ELASTIC_FACTOR_PROPERTY_KEY:
                setElasticFactor(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case FRICTION_PROPERTY_KEY:
                return mFriction;
            case SPEED_MULTIPLIER_PROPERTY_KEY:
                return mSpeedMultiplier;
            case ELASTIC_FACTOR_PROPERTY_KEY:
                return mElasticFactor;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }
}
