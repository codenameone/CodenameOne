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

/// Generated from rive-runtime's `ClippingShapeBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `ClippingShape`.
public abstract class ClippingShapeBase extends Component {
    public static final int TYPE_KEY = 42;
    public static final int SOURCE_ID_PROPERTY_KEY = 92;
    public static final int FILL_RULE_PROPERTY_KEY = 93;
    public static final int IS_VISIBLE_PROPERTY_KEY = 94;

    protected int mSourceId = -1;
    protected int mFillRule = 0;
    protected boolean mIsVisible = true;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getSourceId() {
        return mSourceId;
    }

    public void setSourceId(int value) {
        if (mSourceId == value) {
            return;
        }
        mSourceId = value;
        sourceIdChanged();
    }

    protected void sourceIdChanged() {
    }

    public int getFillRule() {
        return mFillRule;
    }

    public void setFillRule(int value) {
        if (mFillRule == value) {
            return;
        }
        mFillRule = value;
        fillRuleChanged();
    }

    protected void fillRuleChanged() {
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    public void setIsVisible(boolean value) {
        if (mIsVisible == value) {
            return;
        }
        mIsVisible = value;
        isVisibleChanged();
    }

    protected void isVisibleChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case IS_VISIBLE_PROPERTY_KEY:
                mIsVisible = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case IS_VISIBLE_PROPERTY_KEY:
                setIsVisible(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case IS_VISIBLE_PROPERTY_KEY:
                return mIsVisible;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case SOURCE_ID_PROPERTY_KEY:
                mSourceId = value;
                return true;
            case FILL_RULE_PROPERTY_KEY:
                mFillRule = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case SOURCE_ID_PROPERTY_KEY:
                setSourceId(value);
                return true;
            case FILL_RULE_PROPERTY_KEY:
                setFillRule(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case SOURCE_ID_PROPERTY_KEY:
                return mSourceId;
            case FILL_RULE_PROPERTY_KEY:
                return mFillRule;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
