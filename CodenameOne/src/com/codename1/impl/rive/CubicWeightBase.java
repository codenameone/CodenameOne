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

/// Generated from rive-runtime's `CubicWeightBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `CubicWeight`.
public abstract class CubicWeightBase extends Weight {
    public static final int TYPE_KEY = 46;
    public static final int IN_VALUES_PROPERTY_KEY = 110;
    public static final int IN_INDICES_PROPERTY_KEY = 111;
    public static final int OUT_VALUES_PROPERTY_KEY = 112;
    public static final int OUT_INDICES_PROPERTY_KEY = 113;

    protected int mInValues = 255;
    protected int mInIndices = 1;
    protected int mOutValues = 255;
    protected int mOutIndices = 1;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getInValues() {
        return mInValues;
    }

    public void setInValues(int value) {
        if (mInValues == value) {
            return;
        }
        mInValues = value;
        inValuesChanged();
    }

    protected void inValuesChanged() {
    }

    public int getInIndices() {
        return mInIndices;
    }

    public void setInIndices(int value) {
        if (mInIndices == value) {
            return;
        }
        mInIndices = value;
        inIndicesChanged();
    }

    protected void inIndicesChanged() {
    }

    public int getOutValues() {
        return mOutValues;
    }

    public void setOutValues(int value) {
        if (mOutValues == value) {
            return;
        }
        mOutValues = value;
        outValuesChanged();
    }

    protected void outValuesChanged() {
    }

    public int getOutIndices() {
        return mOutIndices;
    }

    public void setOutIndices(int value) {
        if (mOutIndices == value) {
            return;
        }
        mOutIndices = value;
        outIndicesChanged();
    }

    protected void outIndicesChanged() {
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case IN_VALUES_PROPERTY_KEY:
                mInValues = value;
                return true;
            case IN_INDICES_PROPERTY_KEY:
                mInIndices = value;
                return true;
            case OUT_VALUES_PROPERTY_KEY:
                mOutValues = value;
                return true;
            case OUT_INDICES_PROPERTY_KEY:
                mOutIndices = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case IN_VALUES_PROPERTY_KEY:
                setInValues(value);
                return true;
            case IN_INDICES_PROPERTY_KEY:
                setInIndices(value);
                return true;
            case OUT_VALUES_PROPERTY_KEY:
                setOutValues(value);
                return true;
            case OUT_INDICES_PROPERTY_KEY:
                setOutIndices(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case IN_VALUES_PROPERTY_KEY:
                return mInValues;
            case IN_INDICES_PROPERTY_KEY:
                return mInIndices;
            case OUT_VALUES_PROPERTY_KEY:
                return mOutValues;
            case OUT_INDICES_PROPERTY_KEY:
                return mOutIndices;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
