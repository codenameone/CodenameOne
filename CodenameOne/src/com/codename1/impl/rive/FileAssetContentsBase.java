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

/// Generated from rive-runtime's `FileAssetContentsBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `FileAssetContents`.
public abstract class FileAssetContentsBase extends Core {
    public static final int TYPE_KEY = 106;
    public static final int BYTES_PROPERTY_KEY = 212;
    public static final int SIGNATURE_PROPERTY_KEY = 911;

    protected byte[] mBytes = null;
    protected byte[] mSignature = null;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public byte[] getBytes() {
        return mBytes;
    }

    protected void decodeBytes(byte[] value) {
        mBytes = value;
    }

    public byte[] getSignature() {
        return mSignature;
    }

    protected void decodeSignature(byte[] value) {
        mSignature = value;
    }

    @Override
    public boolean deserializeBytes(int propertyKey, byte[] value) {
        switch (propertyKey) {
            case BYTES_PROPERTY_KEY:
                decodeBytes(value);
                return true;
            case SIGNATURE_PROPERTY_KEY:
                decodeSignature(value);
                return true;
            default:
                return super.deserializeBytes(propertyKey, value);
        }
    }
}
