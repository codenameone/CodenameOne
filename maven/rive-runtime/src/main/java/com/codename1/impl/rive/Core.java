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

/// Root of the Rive object model: every object a `.riv` file holds. The
/// generated `*Base` classes chain the property families below; a key no class
/// claims falls through to these defaults and is ignored.
public abstract class Core {
    /// The Rive type key of this object.
    public abstract int coreType();

    /// Whether this object is of the type `typeKey` or a subtype of it.
    public boolean isTypeOf(int typeKey) {
        return false;
    }

    public boolean deserializeUint(int propertyKey, int value) {
        return false;
    }

    public boolean deserializeDouble(int propertyKey, float value) {
        return false;
    }

    public boolean deserializeColor(int propertyKey, int value) {
        return false;
    }

    public boolean deserializeBool(int propertyKey, boolean value) {
        return false;
    }

    public boolean deserializeString(int propertyKey, String value) {
        return false;
    }

    public boolean deserializeBytes(int propertyKey, byte[] value) {
        return false;
    }

    public boolean setUintProperty(int propertyKey, int value) {
        return false;
    }

    public boolean setDoubleProperty(int propertyKey, float value) {
        return false;
    }

    public boolean setColorProperty(int propertyKey, int value) {
        return false;
    }

    public boolean setBoolProperty(int propertyKey, boolean value) {
        return false;
    }

    public boolean setStringProperty(int propertyKey, String value) {
        return false;
    }

    public int getUintProperty(int propertyKey) {
        return 0;
    }

    public float getDoubleProperty(int propertyKey) {
        return 0f;
    }

    public int getColorProperty(int propertyKey) {
        return 0;
    }

    public boolean getBoolProperty(int propertyKey) {
        return false;
    }

    public String getStringProperty(int propertyKey) {
        return null;
    }
}
