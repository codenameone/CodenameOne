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

/// Generated from rive-runtime's `StateMachineListenerBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `StateMachineListener`.
public abstract class StateMachineListenerBase extends StateMachineComponent {
    public static final int TYPE_KEY = 654;
    public static final int TARGET_ID_PROPERTY_KEY = 224;
    public static final int PHASE_VALUE_PROPERTY_KEY = 535;
    public static final int LISTENER_FLAGS_PROPERTY_KEY = 1137;

    protected int mTargetId = -1;
    protected int mPhaseValue = 0;
    protected int mListenerFlags = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getTargetId() {
        return mTargetId;
    }

    public void setTargetId(int value) {
        if (mTargetId == value) {
            return;
        }
        mTargetId = value;
        targetIdChanged();
    }

    protected void targetIdChanged() {
    }

    public int getPhaseValue() {
        return mPhaseValue;
    }

    public void setPhaseValue(int value) {
        if (mPhaseValue == value) {
            return;
        }
        mPhaseValue = value;
        phaseValueChanged();
    }

    protected void phaseValueChanged() {
    }

    public int getListenerFlags() {
        return mListenerFlags;
    }

    public void setListenerFlags(int value) {
        if (mListenerFlags == value) {
            return;
        }
        mListenerFlags = value;
        listenerFlagsChanged();
    }

    protected void listenerFlagsChanged() {
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case TARGET_ID_PROPERTY_KEY:
                mTargetId = value;
                return true;
            case PHASE_VALUE_PROPERTY_KEY:
                mPhaseValue = value;
                return true;
            case LISTENER_FLAGS_PROPERTY_KEY:
                mListenerFlags = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case TARGET_ID_PROPERTY_KEY:
                setTargetId(value);
                return true;
            case PHASE_VALUE_PROPERTY_KEY:
                setPhaseValue(value);
                return true;
            case LISTENER_FLAGS_PROPERTY_KEY:
                setListenerFlags(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case TARGET_ID_PROPERTY_KEY:
                return mTargetId;
            case PHASE_VALUE_PROPERTY_KEY:
                return mPhaseValue;
            case LISTENER_FLAGS_PROPERTY_KEY:
                return mListenerFlags;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
