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

/// Generated from rive-runtime's `FocusDataBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `FocusData`.
public abstract class FocusDataBase extends Component {
    public static final int TYPE_KEY = 653;
    public static final int FOCUS_FLAGS_PROPERTY_KEY = 1033;
    public static final int CAN_FOCUS_PROPERTY_KEY = 953;
    public static final int CAN_TOUCH_PROPERTY_KEY = 954;
    public static final int CAN_TRAVERSE_PROPERTY_KEY = 955;
    public static final int EDGE_BEHAVIOR_VALUE_PROPERTY_KEY = 956;

    protected int mFocusFlags = 7;
    protected boolean mCanFocus = false;
    protected boolean mCanTouch = false;
    protected boolean mCanTraverse = false;
    protected int mEdgeBehaviorValue = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getFocusFlags() {
        return mFocusFlags;
    }

    public void setFocusFlags(int value) {
        if (mFocusFlags == value) {
            return;
        }
        mFocusFlags = value;
        focusFlagsChanged();
    }

    protected void focusFlagsChanged() {
    }

    public boolean isCanFocus() {
        return mCanFocus;
    }

    public void setCanFocus(boolean value) {
        if (mCanFocus == value) {
            return;
        }
        mCanFocus = value;
        canFocusChanged();
    }

    protected void canFocusChanged() {
    }

    public boolean isCanTouch() {
        return mCanTouch;
    }

    public void setCanTouch(boolean value) {
        if (mCanTouch == value) {
            return;
        }
        mCanTouch = value;
        canTouchChanged();
    }

    protected void canTouchChanged() {
    }

    public boolean isCanTraverse() {
        return mCanTraverse;
    }

    public void setCanTraverse(boolean value) {
        if (mCanTraverse == value) {
            return;
        }
        mCanTraverse = value;
        canTraverseChanged();
    }

    protected void canTraverseChanged() {
    }

    public int getEdgeBehaviorValue() {
        return mEdgeBehaviorValue;
    }

    public void setEdgeBehaviorValue(int value) {
        if (mEdgeBehaviorValue == value) {
            return;
        }
        mEdgeBehaviorValue = value;
        edgeBehaviorValueChanged();
    }

    protected void edgeBehaviorValueChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case CAN_FOCUS_PROPERTY_KEY:
                mCanFocus = value;
                return true;
            case CAN_TOUCH_PROPERTY_KEY:
                mCanTouch = value;
                return true;
            case CAN_TRAVERSE_PROPERTY_KEY:
                mCanTraverse = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case CAN_FOCUS_PROPERTY_KEY:
                setCanFocus(value);
                return true;
            case CAN_TOUCH_PROPERTY_KEY:
                setCanTouch(value);
                return true;
            case CAN_TRAVERSE_PROPERTY_KEY:
                setCanTraverse(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case CAN_FOCUS_PROPERTY_KEY:
                return mCanFocus;
            case CAN_TOUCH_PROPERTY_KEY:
                return mCanTouch;
            case CAN_TRAVERSE_PROPERTY_KEY:
                return mCanTraverse;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case FOCUS_FLAGS_PROPERTY_KEY:
                mFocusFlags = value;
                return true;
            case EDGE_BEHAVIOR_VALUE_PROPERTY_KEY:
                mEdgeBehaviorValue = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case FOCUS_FLAGS_PROPERTY_KEY:
                setFocusFlags(value);
                return true;
            case EDGE_BEHAVIOR_VALUE_PROPERTY_KEY:
                setEdgeBehaviorValue(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case FOCUS_FLAGS_PROPERTY_KEY:
                return mFocusFlags;
            case EDGE_BEHAVIOR_VALUE_PROPERTY_KEY:
                return mEdgeBehaviorValue;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
