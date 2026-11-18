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

/// Generated from rive-runtime's `KeyboardInputBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `KeyboardInput`.
public abstract class KeyboardInputBase extends UserInput {
    public static final int TYPE_KEY = 664;
    public static final int KEY_TYPE_PROPERTY_KEY = 971;
    public static final int KEY_PHASE_PROPERTY_KEY = 972;
    public static final int MODIFIERS_PROPERTY_KEY = 973;

    protected int mKeyType = -1;
    protected int mKeyPhase = 0;
    protected int mModifiers = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getKeyType() {
        return mKeyType;
    }

    public void setKeyType(int value) {
        if (mKeyType == value) {
            return;
        }
        mKeyType = value;
        keyTypeChanged();
    }

    protected void keyTypeChanged() {
    }

    public int getKeyPhase() {
        return mKeyPhase;
    }

    public void setKeyPhase(int value) {
        if (mKeyPhase == value) {
            return;
        }
        mKeyPhase = value;
        keyPhaseChanged();
    }

    protected void keyPhaseChanged() {
    }

    public int getModifiers() {
        return mModifiers;
    }

    public void setModifiers(int value) {
        if (mModifiers == value) {
            return;
        }
        mModifiers = value;
        modifiersChanged();
    }

    protected void modifiersChanged() {
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case KEY_TYPE_PROPERTY_KEY:
                mKeyType = value;
                return true;
            case KEY_PHASE_PROPERTY_KEY:
                mKeyPhase = value;
                return true;
            case MODIFIERS_PROPERTY_KEY:
                mModifiers = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case KEY_TYPE_PROPERTY_KEY:
                setKeyType(value);
                return true;
            case KEY_PHASE_PROPERTY_KEY:
                setKeyPhase(value);
                return true;
            case MODIFIERS_PROPERTY_KEY:
                setModifiers(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case KEY_TYPE_PROPERTY_KEY:
                return mKeyType;
            case KEY_PHASE_PROPERTY_KEY:
                return mKeyPhase;
            case MODIFIERS_PROPERTY_KEY:
                return mModifiers;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
