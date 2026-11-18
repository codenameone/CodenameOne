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

/// Generated from rive-runtime's `StateMachineListenerSingleBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `StateMachineListenerSingle`.
public abstract class StateMachineListenerSingleBase extends StateMachineListener {
    public static final int TYPE_KEY = 114;
    public static final int LISTENER_TYPE_VALUE_PROPERTY_KEY = 225;
    public static final int EVENT_ID_PROPERTY_KEY = 399;
    public static final int VIEW_MODEL_PATH_IDS_PROPERTY_KEY = 868;

    protected int mListenerTypeValue = 0;
    protected int mEventId = -1;
    protected byte[] mViewModelPathIds = null;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getListenerTypeValue() {
        return mListenerTypeValue;
    }

    public void setListenerTypeValue(int value) {
        if (mListenerTypeValue == value) {
            return;
        }
        mListenerTypeValue = value;
        listenerTypeValueChanged();
    }

    protected void listenerTypeValueChanged() {
    }

    public int getEventId() {
        return mEventId;
    }

    public void setEventId(int value) {
        if (mEventId == value) {
            return;
        }
        mEventId = value;
        eventIdChanged();
    }

    protected void eventIdChanged() {
    }

    public byte[] getViewModelPathIds() {
        return mViewModelPathIds;
    }

    protected void decodeViewModelPathIds(byte[] value) {
        mViewModelPathIds = value;
    }

    @Override
    public boolean deserializeBytes(int propertyKey, byte[] value) {
        switch (propertyKey) {
            case VIEW_MODEL_PATH_IDS_PROPERTY_KEY:
                decodeViewModelPathIds(value);
                return true;
            default:
                return super.deserializeBytes(propertyKey, value);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case LISTENER_TYPE_VALUE_PROPERTY_KEY:
                mListenerTypeValue = value;
                return true;
            case EVENT_ID_PROPERTY_KEY:
                mEventId = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case LISTENER_TYPE_VALUE_PROPERTY_KEY:
                setListenerTypeValue(value);
                return true;
            case EVENT_ID_PROPERTY_KEY:
                setEventId(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case LISTENER_TYPE_VALUE_PROPERTY_KEY:
                return mListenerTypeValue;
            case EVENT_ID_PROPERTY_KEY:
                return mEventId;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
