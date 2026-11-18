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

/// Generated from rive-runtime's `MeshVertexBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `MeshVertex`.
public abstract class MeshVertexBase extends Vertex {
    public static final int TYPE_KEY = 108;
    public static final int U_PROPERTY_KEY = 215;
    public static final int V_PROPERTY_KEY = 216;

    protected float mU = 0.0f;
    protected float mV = 0.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getU() {
        return mU;
    }

    public void setU(float value) {
        if (Float.floatToIntBits(mU) == Float.floatToIntBits(value)) {
            return;
        }
        mU = value;
        uChanged();
    }

    protected void uChanged() {
    }

    public float getV() {
        return mV;
    }

    public void setV(float value) {
        if (Float.floatToIntBits(mV) == Float.floatToIntBits(value)) {
            return;
        }
        mV = value;
        vChanged();
    }

    protected void vChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case U_PROPERTY_KEY:
                mU = value;
                return true;
            case V_PROPERTY_KEY:
                mV = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case U_PROPERTY_KEY:
                setU(value);
                return true;
            case V_PROPERTY_KEY:
                setV(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case U_PROPERTY_KEY:
                return mU;
            case V_PROPERTY_KEY:
                return mV;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }
}
