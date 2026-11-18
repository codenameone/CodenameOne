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

/// Generated from rive-runtime's `NodeBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `Node`.
public abstract class NodeBase extends TransformComponent {
    public static final int TYPE_KEY = 2;
    public static final int X_PROPERTY_KEY = 13;
    public static final int X_ARTBOARD_PROPERTY_KEY = 9;
    public static final int Y_PROPERTY_KEY = 14;
    public static final int Y_ARTBOARD_PROPERTY_KEY = 10;
    public static final int COMPUTED_LOCAL_X_PROPERTY_KEY = 806;
    public static final int COMPUTED_LOCAL_Y_PROPERTY_KEY = 807;
    public static final int COMPUTED_WORLD_X_PROPERTY_KEY = 808;
    public static final int COMPUTED_WORLD_Y_PROPERTY_KEY = 809;
    public static final int COMPUTED_ROOT_X_PROPERTY_KEY = 864;
    public static final int COMPUTED_ROOT_Y_PROPERTY_KEY = 865;
    public static final int COMPUTED_WIDTH_PROPERTY_KEY = 810;
    public static final int COMPUTED_HEIGHT_PROPERTY_KEY = 811;

    protected float mX = 0.0f;
    protected float mXArtboard = 0f;
    protected float mY = 0.0f;
    protected float mYArtboard = 0f;
    protected float mComputedLocalX = 0f;
    protected float mComputedLocalY = 0f;
    protected float mComputedWorldX = 0f;
    protected float mComputedWorldY = 0f;
    protected float mComputedRootX = 0f;
    protected float mComputedRootY = 0f;
    protected float mComputedWidth = 0f;
    protected float mComputedHeight = 0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    @Override
    public float getX() {
        return mX;
    }

    public void setX(float value) {
        if (Float.floatToIntBits(mX) == Float.floatToIntBits(value)) {
            return;
        }
        mX = value;
        xChanged();
    }

    protected void xChanged() {
    }

    public float getXArtboard() {
        return mXArtboard;
    }

    public void setXArtboard(float value) {
        if (Float.floatToIntBits(mXArtboard) == Float.floatToIntBits(value)) {
            return;
        }
        mXArtboard = value;
        xArtboardChanged();
    }

    protected void xArtboardChanged() {
    }

    @Override
    public float getY() {
        return mY;
    }

    public void setY(float value) {
        if (Float.floatToIntBits(mY) == Float.floatToIntBits(value)) {
            return;
        }
        mY = value;
        yChanged();
    }

    protected void yChanged() {
    }

    public float getYArtboard() {
        return mYArtboard;
    }

    public void setYArtboard(float value) {
        if (Float.floatToIntBits(mYArtboard) == Float.floatToIntBits(value)) {
            return;
        }
        mYArtboard = value;
        yArtboardChanged();
    }

    protected void yArtboardChanged() {
    }

    public float getComputedLocalX() {
        return mComputedLocalX;
    }

    public void setComputedLocalX(float value) {
        if (Float.floatToIntBits(mComputedLocalX) == Float.floatToIntBits(value)) {
            return;
        }
        mComputedLocalX = value;
        computedLocalXChanged();
    }

    protected void computedLocalXChanged() {
    }

    public float getComputedLocalY() {
        return mComputedLocalY;
    }

    public void setComputedLocalY(float value) {
        if (Float.floatToIntBits(mComputedLocalY) == Float.floatToIntBits(value)) {
            return;
        }
        mComputedLocalY = value;
        computedLocalYChanged();
    }

    protected void computedLocalYChanged() {
    }

    public float getComputedWorldX() {
        return mComputedWorldX;
    }

    public void setComputedWorldX(float value) {
        if (Float.floatToIntBits(mComputedWorldX) == Float.floatToIntBits(value)) {
            return;
        }
        mComputedWorldX = value;
        computedWorldXChanged();
    }

    protected void computedWorldXChanged() {
    }

    public float getComputedWorldY() {
        return mComputedWorldY;
    }

    public void setComputedWorldY(float value) {
        if (Float.floatToIntBits(mComputedWorldY) == Float.floatToIntBits(value)) {
            return;
        }
        mComputedWorldY = value;
        computedWorldYChanged();
    }

    protected void computedWorldYChanged() {
    }

    public float getComputedRootX() {
        return mComputedRootX;
    }

    public void setComputedRootX(float value) {
        if (Float.floatToIntBits(mComputedRootX) == Float.floatToIntBits(value)) {
            return;
        }
        mComputedRootX = value;
        computedRootXChanged();
    }

    protected void computedRootXChanged() {
    }

    public float getComputedRootY() {
        return mComputedRootY;
    }

    public void setComputedRootY(float value) {
        if (Float.floatToIntBits(mComputedRootY) == Float.floatToIntBits(value)) {
            return;
        }
        mComputedRootY = value;
        computedRootYChanged();
    }

    protected void computedRootYChanged() {
    }

    public float getComputedWidth() {
        return mComputedWidth;
    }

    public void setComputedWidth(float value) {
        if (Float.floatToIntBits(mComputedWidth) == Float.floatToIntBits(value)) {
            return;
        }
        mComputedWidth = value;
        computedWidthChanged();
    }

    protected void computedWidthChanged() {
    }

    public float getComputedHeight() {
        return mComputedHeight;
    }

    public void setComputedHeight(float value) {
        if (Float.floatToIntBits(mComputedHeight) == Float.floatToIntBits(value)) {
            return;
        }
        mComputedHeight = value;
        computedHeightChanged();
    }

    protected void computedHeightChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case X_PROPERTY_KEY:
                mX = value;
                return true;
            case X_ARTBOARD_PROPERTY_KEY:
                mXArtboard = value;
                return true;
            case Y_PROPERTY_KEY:
                mY = value;
                return true;
            case Y_ARTBOARD_PROPERTY_KEY:
                mYArtboard = value;
                return true;
            case COMPUTED_LOCAL_X_PROPERTY_KEY:
                mComputedLocalX = value;
                return true;
            case COMPUTED_LOCAL_Y_PROPERTY_KEY:
                mComputedLocalY = value;
                return true;
            case COMPUTED_WORLD_X_PROPERTY_KEY:
                mComputedWorldX = value;
                return true;
            case COMPUTED_WORLD_Y_PROPERTY_KEY:
                mComputedWorldY = value;
                return true;
            case COMPUTED_ROOT_X_PROPERTY_KEY:
                mComputedRootX = value;
                return true;
            case COMPUTED_ROOT_Y_PROPERTY_KEY:
                mComputedRootY = value;
                return true;
            case COMPUTED_WIDTH_PROPERTY_KEY:
                mComputedWidth = value;
                return true;
            case COMPUTED_HEIGHT_PROPERTY_KEY:
                mComputedHeight = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case X_PROPERTY_KEY:
                setX(value);
                return true;
            case X_ARTBOARD_PROPERTY_KEY:
                setXArtboard(value);
                return true;
            case Y_PROPERTY_KEY:
                setY(value);
                return true;
            case Y_ARTBOARD_PROPERTY_KEY:
                setYArtboard(value);
                return true;
            case COMPUTED_LOCAL_X_PROPERTY_KEY:
                setComputedLocalX(value);
                return true;
            case COMPUTED_LOCAL_Y_PROPERTY_KEY:
                setComputedLocalY(value);
                return true;
            case COMPUTED_WORLD_X_PROPERTY_KEY:
                setComputedWorldX(value);
                return true;
            case COMPUTED_WORLD_Y_PROPERTY_KEY:
                setComputedWorldY(value);
                return true;
            case COMPUTED_ROOT_X_PROPERTY_KEY:
                setComputedRootX(value);
                return true;
            case COMPUTED_ROOT_Y_PROPERTY_KEY:
                setComputedRootY(value);
                return true;
            case COMPUTED_WIDTH_PROPERTY_KEY:
                setComputedWidth(value);
                return true;
            case COMPUTED_HEIGHT_PROPERTY_KEY:
                setComputedHeight(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case X_PROPERTY_KEY:
                return mX;
            case X_ARTBOARD_PROPERTY_KEY:
                return mXArtboard;
            case Y_PROPERTY_KEY:
                return mY;
            case Y_ARTBOARD_PROPERTY_KEY:
                return mYArtboard;
            case COMPUTED_LOCAL_X_PROPERTY_KEY:
                return mComputedLocalX;
            case COMPUTED_LOCAL_Y_PROPERTY_KEY:
                return mComputedLocalY;
            case COMPUTED_WORLD_X_PROPERTY_KEY:
                return mComputedWorldX;
            case COMPUTED_WORLD_Y_PROPERTY_KEY:
                return mComputedWorldY;
            case COMPUTED_ROOT_X_PROPERTY_KEY:
                return mComputedRootX;
            case COMPUTED_ROOT_Y_PROPERTY_KEY:
                return mComputedRootY;
            case COMPUTED_WIDTH_PROPERTY_KEY:
                return mComputedWidth;
            case COMPUTED_HEIGHT_PROPERTY_KEY:
                return mComputedHeight;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }
}
