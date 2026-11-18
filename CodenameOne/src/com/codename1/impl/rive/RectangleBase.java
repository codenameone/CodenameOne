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

/// Generated from rive-runtime's `RectangleBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `Rectangle`.
public abstract class RectangleBase extends ParametricPath {
    public static final int TYPE_KEY = 7;
    public static final int LINK_CORNER_RADIUS_PROPERTY_KEY = 164;
    public static final int CORNER_RADIUS_TL_PROPERTY_KEY = 31;
    public static final int CORNER_RADIUS_TR_PROPERTY_KEY = 161;
    public static final int CORNER_RADIUS_BL_PROPERTY_KEY = 162;
    public static final int CORNER_RADIUS_BR_PROPERTY_KEY = 163;

    protected boolean mLinkCornerRadius = true;
    protected float mCornerRadiusTL = 0.0f;
    protected float mCornerRadiusTR = 0.0f;
    protected float mCornerRadiusBL = 0.0f;
    protected float mCornerRadiusBR = 0.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public boolean isLinkCornerRadius() {
        return mLinkCornerRadius;
    }

    public void setLinkCornerRadius(boolean value) {
        if (mLinkCornerRadius == value) {
            return;
        }
        mLinkCornerRadius = value;
        linkCornerRadiusChanged();
    }

    protected void linkCornerRadiusChanged() {
    }

    public float getCornerRadiusTL() {
        return mCornerRadiusTL;
    }

    public void setCornerRadiusTL(float value) {
        if (Float.floatToIntBits(mCornerRadiusTL) == Float.floatToIntBits(value)) {
            return;
        }
        mCornerRadiusTL = value;
        cornerRadiusTLChanged();
    }

    protected void cornerRadiusTLChanged() {
    }

    public float getCornerRadiusTR() {
        return mCornerRadiusTR;
    }

    public void setCornerRadiusTR(float value) {
        if (Float.floatToIntBits(mCornerRadiusTR) == Float.floatToIntBits(value)) {
            return;
        }
        mCornerRadiusTR = value;
        cornerRadiusTRChanged();
    }

    protected void cornerRadiusTRChanged() {
    }

    public float getCornerRadiusBL() {
        return mCornerRadiusBL;
    }

    public void setCornerRadiusBL(float value) {
        if (Float.floatToIntBits(mCornerRadiusBL) == Float.floatToIntBits(value)) {
            return;
        }
        mCornerRadiusBL = value;
        cornerRadiusBLChanged();
    }

    protected void cornerRadiusBLChanged() {
    }

    public float getCornerRadiusBR() {
        return mCornerRadiusBR;
    }

    public void setCornerRadiusBR(float value) {
        if (Float.floatToIntBits(mCornerRadiusBR) == Float.floatToIntBits(value)) {
            return;
        }
        mCornerRadiusBR = value;
        cornerRadiusBRChanged();
    }

    protected void cornerRadiusBRChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case LINK_CORNER_RADIUS_PROPERTY_KEY:
                mLinkCornerRadius = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case LINK_CORNER_RADIUS_PROPERTY_KEY:
                setLinkCornerRadius(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case LINK_CORNER_RADIUS_PROPERTY_KEY:
                return mLinkCornerRadius;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case CORNER_RADIUS_TL_PROPERTY_KEY:
                mCornerRadiusTL = value;
                return true;
            case CORNER_RADIUS_TR_PROPERTY_KEY:
                mCornerRadiusTR = value;
                return true;
            case CORNER_RADIUS_BL_PROPERTY_KEY:
                mCornerRadiusBL = value;
                return true;
            case CORNER_RADIUS_BR_PROPERTY_KEY:
                mCornerRadiusBR = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case CORNER_RADIUS_TL_PROPERTY_KEY:
                setCornerRadiusTL(value);
                return true;
            case CORNER_RADIUS_TR_PROPERTY_KEY:
                setCornerRadiusTR(value);
                return true;
            case CORNER_RADIUS_BL_PROPERTY_KEY:
                setCornerRadiusBL(value);
                return true;
            case CORNER_RADIUS_BR_PROPERTY_KEY:
                setCornerRadiusBR(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case CORNER_RADIUS_TL_PROPERTY_KEY:
                return mCornerRadiusTL;
            case CORNER_RADIUS_TR_PROPERTY_KEY:
                return mCornerRadiusTR;
            case CORNER_RADIUS_BL_PROPERTY_KEY:
                return mCornerRadiusBL;
            case CORNER_RADIUS_BR_PROPERTY_KEY:
                return mCornerRadiusBR;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }
}
