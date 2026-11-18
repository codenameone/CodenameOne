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

/// Generated from rive-runtime's `GridTrackBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `GridTrack`.
public abstract class GridTrackBase extends Component {
    public static final int TYPE_KEY = 1058;
    public static final int TRACK_VALUE_PROPERTY_KEY = 1063;
    public static final int TRACK_MAX_VALUE_PROPERTY_KEY = 1065;
    public static final int COLLECTION_PROPERTY_KEY = 1061;
    public static final int TRACK_TYPE_PROPERTY_KEY = 1062;
    public static final int TRACK_MAX_TYPE_PROPERTY_KEY = 1064;

    protected float mTrackValue = 0.0f;
    protected float mTrackMaxValue = 0.0f;
    protected int mCollection = 0;
    protected int mTrackType = 0;
    protected int mTrackMaxType = 0;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getTrackValue() {
        return mTrackValue;
    }

    public void setTrackValue(float value) {
        if (Float.floatToIntBits(mTrackValue) == Float.floatToIntBits(value)) {
            return;
        }
        mTrackValue = value;
        trackValueChanged();
    }

    protected void trackValueChanged() {
    }

    public float getTrackMaxValue() {
        return mTrackMaxValue;
    }

    public void setTrackMaxValue(float value) {
        if (Float.floatToIntBits(mTrackMaxValue) == Float.floatToIntBits(value)) {
            return;
        }
        mTrackMaxValue = value;
        trackMaxValueChanged();
    }

    protected void trackMaxValueChanged() {
    }

    public int getCollection() {
        return mCollection;
    }

    public void setCollection(int value) {
        if (mCollection == value) {
            return;
        }
        mCollection = value;
        collectionChanged();
    }

    protected void collectionChanged() {
    }

    public int getTrackType() {
        return mTrackType;
    }

    public void setTrackType(int value) {
        if (mTrackType == value) {
            return;
        }
        mTrackType = value;
        trackTypeChanged();
    }

    protected void trackTypeChanged() {
    }

    public int getTrackMaxType() {
        return mTrackMaxType;
    }

    public void setTrackMaxType(int value) {
        if (mTrackMaxType == value) {
            return;
        }
        mTrackMaxType = value;
        trackMaxTypeChanged();
    }

    protected void trackMaxTypeChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case TRACK_VALUE_PROPERTY_KEY:
                mTrackValue = value;
                return true;
            case TRACK_MAX_VALUE_PROPERTY_KEY:
                mTrackMaxValue = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case TRACK_VALUE_PROPERTY_KEY:
                setTrackValue(value);
                return true;
            case TRACK_MAX_VALUE_PROPERTY_KEY:
                setTrackMaxValue(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case TRACK_VALUE_PROPERTY_KEY:
                return mTrackValue;
            case TRACK_MAX_VALUE_PROPERTY_KEY:
                return mTrackMaxValue;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case COLLECTION_PROPERTY_KEY:
                mCollection = value;
                return true;
            case TRACK_TYPE_PROPERTY_KEY:
                mTrackType = value;
                return true;
            case TRACK_MAX_TYPE_PROPERTY_KEY:
                mTrackMaxType = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case COLLECTION_PROPERTY_KEY:
                setCollection(value);
                return true;
            case TRACK_TYPE_PROPERTY_KEY:
                setTrackType(value);
                return true;
            case TRACK_MAX_TYPE_PROPERTY_KEY:
                setTrackMaxType(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case COLLECTION_PROPERTY_KEY:
                return mCollection;
            case TRACK_TYPE_PROPERTY_KEY:
                return mTrackType;
            case TRACK_MAX_TYPE_PROPERTY_KEY:
                return mTrackMaxType;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
