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

/// Generated from rive-runtime's `BitmapCacheBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `BitmapCache`.
public abstract class BitmapCacheBase extends Component {
    public static final int TYPE_KEY = 136;
    public static final int RESOLUTION_PROPERTY_KEY = 417;
    public static final int CACHE_FLAGS_PROPERTY_KEY = 418;
    public static final int CACHE_ENABLED_PROPERTY_KEY = 419;
    public static final int DITHER_PROPERTY_KEY = 420;

    protected float mResolution = 1.0f;
    protected int mCacheFlags = 1;
    protected boolean mCacheEnabled = false;
    protected boolean mDither = false;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getResolution() {
        return mResolution;
    }

    public void setResolution(float value) {
        if (Float.floatToIntBits(mResolution) == Float.floatToIntBits(value)) {
            return;
        }
        mResolution = value;
        resolutionChanged();
    }

    protected void resolutionChanged() {
    }

    public int getCacheFlags() {
        return mCacheFlags;
    }

    public void setCacheFlags(int value) {
        if (mCacheFlags == value) {
            return;
        }
        mCacheFlags = value;
        cacheFlagsChanged();
    }

    protected void cacheFlagsChanged() {
    }

    public boolean isCacheEnabled() {
        return mCacheEnabled;
    }

    public void setCacheEnabled(boolean value) {
        if (mCacheEnabled == value) {
            return;
        }
        mCacheEnabled = value;
        cacheEnabledChanged();
    }

    protected void cacheEnabledChanged() {
    }

    public boolean isDither() {
        return mDither;
    }

    public void setDither(boolean value) {
        if (mDither == value) {
            return;
        }
        mDither = value;
        ditherChanged();
    }

    protected void ditherChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case CACHE_ENABLED_PROPERTY_KEY:
                mCacheEnabled = value;
                return true;
            case DITHER_PROPERTY_KEY:
                mDither = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case CACHE_ENABLED_PROPERTY_KEY:
                setCacheEnabled(value);
                return true;
            case DITHER_PROPERTY_KEY:
                setDither(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case CACHE_ENABLED_PROPERTY_KEY:
                return mCacheEnabled;
            case DITHER_PROPERTY_KEY:
                return mDither;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case RESOLUTION_PROPERTY_KEY:
                mResolution = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case RESOLUTION_PROPERTY_KEY:
                setResolution(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case RESOLUTION_PROPERTY_KEY:
                return mResolution;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case CACHE_FLAGS_PROPERTY_KEY:
                mCacheFlags = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case CACHE_FLAGS_PROPERTY_KEY:
                setCacheFlags(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case CACHE_FLAGS_PROPERTY_KEY:
                return mCacheFlags;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
