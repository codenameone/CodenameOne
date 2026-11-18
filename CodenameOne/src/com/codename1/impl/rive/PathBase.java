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

/// Generated from rive-runtime's `PathBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `Path`.
public abstract class PathBase extends Node {
    public static final int TYPE_KEY = 12;
    public static final int PATH_FLAGS_PROPERTY_KEY = 128;
    public static final int IS_HOLE_PROPERTY_KEY = 770;

    protected int mPathFlags = 0;
    protected boolean mIsHole = false;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public int getPathFlags() {
        return mPathFlags;
    }

    public void setPathFlags(int value) {
        if (mPathFlags == value) {
            return;
        }
        mPathFlags = value;
        pathFlagsChanged();
    }

    protected void pathFlagsChanged() {
    }

    public boolean isHole() {
        return mIsHole;
    }

    public void setIsHole(boolean value) {
        if (mIsHole == value) {
            return;
        }
        mIsHole = value;
        isHoleChanged();
    }

    protected void isHoleChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case IS_HOLE_PROPERTY_KEY:
                mIsHole = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case IS_HOLE_PROPERTY_KEY:
                setIsHole(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case IS_HOLE_PROPERTY_KEY:
                return mIsHole;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case PATH_FLAGS_PROPERTY_KEY:
                mPathFlags = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case PATH_FLAGS_PROPERTY_KEY:
                setPathFlags(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case PATH_FLAGS_PROPERTY_KEY:
                return mPathFlags;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
