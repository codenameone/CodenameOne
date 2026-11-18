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

/// Generated from rive-runtime's `ViewModelInstanceListItemBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `ViewModelInstanceListItem`.
public abstract class ViewModelInstanceListItemBase extends Core {
    public static final int TYPE_KEY = 427;
    public static final int VIEW_MODEL_ID_PROPERTY_KEY = 549;
    public static final int VIEW_MODEL_INSTANCE_ID_PROPERTY_KEY = 550;

    protected int mViewModelId = -1;
    protected int mViewModelInstanceId = -1;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getViewModelId() {
        return mViewModelId;
    }

    public void setViewModelId(int value) {
        if (mViewModelId == value) {
            return;
        }
        mViewModelId = value;
        viewModelIdChanged();
    }

    protected void viewModelIdChanged() {
    }

    public int getViewModelInstanceId() {
        return mViewModelInstanceId;
    }

    public void setViewModelInstanceId(int value) {
        if (mViewModelInstanceId == value) {
            return;
        }
        mViewModelInstanceId = value;
        viewModelInstanceIdChanged();
    }

    protected void viewModelInstanceIdChanged() {
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case VIEW_MODEL_ID_PROPERTY_KEY:
                mViewModelId = value;
                return true;
            case VIEW_MODEL_INSTANCE_ID_PROPERTY_KEY:
                mViewModelInstanceId = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case VIEW_MODEL_ID_PROPERTY_KEY:
                setViewModelId(value);
                return true;
            case VIEW_MODEL_INSTANCE_ID_PROPERTY_KEY:
                setViewModelInstanceId(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case VIEW_MODEL_ID_PROPERTY_KEY:
                return mViewModelId;
            case VIEW_MODEL_INSTANCE_ID_PROPERTY_KEY:
                return mViewModelInstanceId;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
