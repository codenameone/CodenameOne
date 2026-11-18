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

/// Generated from rive-runtime's `GamepadInputBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `GamepadInput`.
public abstract class GamepadInputBase extends UserInput {
    public static final int TYPE_KEY = 974;
    public static final int KIND_PROPERTY_KEY = 1021;
    public static final int MAPPING_PROPERTY_KEY = 1018;
    public static final int INPUT_INDEX_PROPERTY_KEY = 1019;
    public static final int BUTTON_PHASE_PROPERTY_KEY = 1020;

    protected int mKind = 0;
    protected int mMapping = 0;
    protected int mInputIndex = 0;
    protected int mButtonPhase = 1;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getKind() {
        return mKind;
    }

    public void setKind(int value) {
        if (mKind == value) {
            return;
        }
        mKind = value;
        kindChanged();
    }

    protected void kindChanged() {
    }

    public int getMapping() {
        return mMapping;
    }

    public void setMapping(int value) {
        if (mMapping == value) {
            return;
        }
        mMapping = value;
        mappingChanged();
    }

    protected void mappingChanged() {
    }

    public int getInputIndex() {
        return mInputIndex;
    }

    public void setInputIndex(int value) {
        if (mInputIndex == value) {
            return;
        }
        mInputIndex = value;
        inputIndexChanged();
    }

    protected void inputIndexChanged() {
    }

    public int getButtonPhase() {
        return mButtonPhase;
    }

    public void setButtonPhase(int value) {
        if (mButtonPhase == value) {
            return;
        }
        mButtonPhase = value;
        buttonPhaseChanged();
    }

    protected void buttonPhaseChanged() {
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case KIND_PROPERTY_KEY:
                mKind = value;
                return true;
            case MAPPING_PROPERTY_KEY:
                mMapping = value;
                return true;
            case INPUT_INDEX_PROPERTY_KEY:
                mInputIndex = value;
                return true;
            case BUTTON_PHASE_PROPERTY_KEY:
                mButtonPhase = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case KIND_PROPERTY_KEY:
                setKind(value);
                return true;
            case MAPPING_PROPERTY_KEY:
                setMapping(value);
                return true;
            case INPUT_INDEX_PROPERTY_KEY:
                setInputIndex(value);
                return true;
            case BUTTON_PHASE_PROPERTY_KEY:
                setButtonPhase(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case KIND_PROPERTY_KEY:
                return mKind;
            case MAPPING_PROPERTY_KEY:
                return mMapping;
            case INPUT_INDEX_PROPERTY_KEY:
                return mInputIndex;
            case BUTTON_PHASE_PROPERTY_KEY:
                return mButtonPhase;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
