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

/// Generated from rive-runtime's `ScrollBarConstraintBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `ScrollBarConstraint`.
public abstract class ScrollBarConstraintBase extends DraggableConstraint {
    public static final int TYPE_KEY = 522;
    public static final int SCROLL_CONSTRAINT_ID_PROPERTY_KEY = 725;
    public static final int AUTO_SIZE_PROPERTY_KEY = 734;

    protected int mScrollConstraintId = -1;
    protected boolean mAutoSize = true;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getScrollConstraintId() {
        return mScrollConstraintId;
    }

    public void setScrollConstraintId(int value) {
        if (mScrollConstraintId == value) {
            return;
        }
        mScrollConstraintId = value;
        scrollConstraintIdChanged();
    }

    protected void scrollConstraintIdChanged() {
    }

    public boolean isAutoSize() {
        return mAutoSize;
    }

    public void setAutoSize(boolean value) {
        if (mAutoSize == value) {
            return;
        }
        mAutoSize = value;
        autoSizeChanged();
    }

    protected void autoSizeChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case AUTO_SIZE_PROPERTY_KEY:
                mAutoSize = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case AUTO_SIZE_PROPERTY_KEY:
                setAutoSize(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case AUTO_SIZE_PROPERTY_KEY:
                return mAutoSize;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case SCROLL_CONSTRAINT_ID_PROPERTY_KEY:
                mScrollConstraintId = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case SCROLL_CONSTRAINT_ID_PROPERTY_KEY:
                setScrollConstraintId(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case SCROLL_CONSTRAINT_ID_PROPERTY_KEY:
                return mScrollConstraintId;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
