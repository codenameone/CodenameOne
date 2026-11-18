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

/// Generated from rive-runtime's `DrawTargetBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `DrawTarget`.
public abstract class DrawTargetBase extends Component {
    public static final int TYPE_KEY = 48;
    public static final int DRAWABLE_ID_PROPERTY_KEY = 119;
    public static final int PLACEMENT_VALUE_PROPERTY_KEY = 120;

    protected int mDrawableId = -1;
    protected int mPlacementValue = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getDrawableId() {
        return mDrawableId;
    }

    public void setDrawableId(int value) {
        if (mDrawableId == value) {
            return;
        }
        mDrawableId = value;
        drawableIdChanged();
    }

    protected void drawableIdChanged() {
    }

    public int getPlacementValue() {
        return mPlacementValue;
    }

    public void setPlacementValue(int value) {
        if (mPlacementValue == value) {
            return;
        }
        mPlacementValue = value;
        placementValueChanged();
    }

    protected void placementValueChanged() {
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case DRAWABLE_ID_PROPERTY_KEY:
                mDrawableId = value;
                return true;
            case PLACEMENT_VALUE_PROPERTY_KEY:
                mPlacementValue = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case DRAWABLE_ID_PROPERTY_KEY:
                setDrawableId(value);
                return true;
            case PLACEMENT_VALUE_PROPERTY_KEY:
                setPlacementValue(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case DRAWABLE_ID_PROPERTY_KEY:
                return mDrawableId;
            case PLACEMENT_VALUE_PROPERTY_KEY:
                return mPlacementValue;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
