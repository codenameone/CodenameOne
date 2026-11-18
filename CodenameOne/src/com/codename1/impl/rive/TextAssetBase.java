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

/// Generated from rive-runtime's `TextAssetBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `TextAsset`.
public abstract class TextAssetBase extends FileAsset {
    public static final int TYPE_KEY = 971;
    public static final int FOLDER_PATH_PROPERTY_KEY = 926;

    protected String mFolderPath = "";

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public String getFolderPath() {
        return mFolderPath;
    }

    public void setFolderPath(String value) {
        if ((mFolderPath == null ? value == null : mFolderPath.equals(value))) {
            return;
        }
        mFolderPath = value;
        folderPathChanged();
    }

    protected void folderPathChanged() {
    }

    @Override
    public boolean deserializeString(int propertyKey, String value) {
        switch (propertyKey) {
            case FOLDER_PATH_PROPERTY_KEY:
                mFolderPath = value;
                return true;
            default:
                return super.deserializeString(propertyKey, value);
        }
    }

    @Override
    public boolean setStringProperty(int propertyKey, String value) {
        switch (propertyKey) {
            case FOLDER_PATH_PROPERTY_KEY:
                setFolderPath(value);
                return true;
            default:
                return super.setStringProperty(propertyKey, value);
        }
    }

    @Override
    public String getStringProperty(int propertyKey) {
        switch (propertyKey) {
            case FOLDER_PATH_PROPERTY_KEY:
                return mFolderPath;
            default:
                return super.getStringProperty(propertyKey);
        }
    }
}
