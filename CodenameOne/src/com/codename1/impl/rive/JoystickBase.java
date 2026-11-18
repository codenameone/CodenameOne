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

/// Generated from rive-runtime's `JoystickBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `Joystick`.
public abstract class JoystickBase extends Component {
    public static final int TYPE_KEY = 148;
    public static final int X_PROPERTY_KEY = 299;
    public static final int Y_PROPERTY_KEY = 300;
    public static final int POS_X_PROPERTY_KEY = 303;
    public static final int POS_Y_PROPERTY_KEY = 304;
    public static final int ORIGIN_X_PROPERTY_KEY = 307;
    public static final int ORIGIN_Y_PROPERTY_KEY = 308;
    public static final int WIDTH_PROPERTY_KEY = 305;
    public static final int HEIGHT_PROPERTY_KEY = 306;
    public static final int X_ID_PROPERTY_KEY = 301;
    public static final int Y_ID_PROPERTY_KEY = 302;
    public static final int JOYSTICK_FLAGS_PROPERTY_KEY = 312;
    public static final int HANDLE_SOURCE_ID_PROPERTY_KEY = 313;

    protected float mX = 0.0f;
    protected float mY = 0.0f;
    protected float mPosX = 0.0f;
    protected float mPosY = 0.0f;
    protected float mOriginX = 0.5f;
    protected float mOriginY = 0.5f;
    protected float mWidth = 100.0f;
    protected float mHeight = 100.0f;
    protected int mXId = -1;
    protected int mYId = -1;
    protected int mJoystickFlags = 0;
    protected int mHandleSourceId = -1;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getX() {
        return mX;
    }

    public void setX(float value) {
        if (Float.floatToIntBits(mX) == Float.floatToIntBits(value)) {
            return;
        }
        mX = value;
        xChanged();
    }

    protected void xChanged() {
    }

    public float getY() {
        return mY;
    }

    public void setY(float value) {
        if (Float.floatToIntBits(mY) == Float.floatToIntBits(value)) {
            return;
        }
        mY = value;
        yChanged();
    }

    protected void yChanged() {
    }

    public float getPosX() {
        return mPosX;
    }

    public void setPosX(float value) {
        if (Float.floatToIntBits(mPosX) == Float.floatToIntBits(value)) {
            return;
        }
        mPosX = value;
        posXChanged();
    }

    protected void posXChanged() {
    }

    public float getPosY() {
        return mPosY;
    }

    public void setPosY(float value) {
        if (Float.floatToIntBits(mPosY) == Float.floatToIntBits(value)) {
            return;
        }
        mPosY = value;
        posYChanged();
    }

    protected void posYChanged() {
    }

    public float getOriginX() {
        return mOriginX;
    }

    public void setOriginX(float value) {
        if (Float.floatToIntBits(mOriginX) == Float.floatToIntBits(value)) {
            return;
        }
        mOriginX = value;
        originXChanged();
    }

    protected void originXChanged() {
    }

    public float getOriginY() {
        return mOriginY;
    }

    public void setOriginY(float value) {
        if (Float.floatToIntBits(mOriginY) == Float.floatToIntBits(value)) {
            return;
        }
        mOriginY = value;
        originYChanged();
    }

    protected void originYChanged() {
    }

    public float getWidth() {
        return mWidth;
    }

    public void setWidth(float value) {
        if (Float.floatToIntBits(mWidth) == Float.floatToIntBits(value)) {
            return;
        }
        mWidth = value;
        widthChanged();
    }

    protected void widthChanged() {
    }

    public float getHeight() {
        return mHeight;
    }

    public void setHeight(float value) {
        if (Float.floatToIntBits(mHeight) == Float.floatToIntBits(value)) {
            return;
        }
        mHeight = value;
        heightChanged();
    }

    protected void heightChanged() {
    }

    public int getXId() {
        return mXId;
    }

    public void setXId(int value) {
        if (mXId == value) {
            return;
        }
        mXId = value;
        xIdChanged();
    }

    protected void xIdChanged() {
    }

    public int getYId() {
        return mYId;
    }

    public void setYId(int value) {
        if (mYId == value) {
            return;
        }
        mYId = value;
        yIdChanged();
    }

    protected void yIdChanged() {
    }

    public int getJoystickFlags() {
        return mJoystickFlags;
    }

    public void setJoystickFlags(int value) {
        if (mJoystickFlags == value) {
            return;
        }
        mJoystickFlags = value;
        joystickFlagsChanged();
    }

    protected void joystickFlagsChanged() {
    }

    public int getHandleSourceId() {
        return mHandleSourceId;
    }

    public void setHandleSourceId(int value) {
        if (mHandleSourceId == value) {
            return;
        }
        mHandleSourceId = value;
        handleSourceIdChanged();
    }

    protected void handleSourceIdChanged() {
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case X_PROPERTY_KEY:
                mX = value;
                return true;
            case Y_PROPERTY_KEY:
                mY = value;
                return true;
            case POS_X_PROPERTY_KEY:
                mPosX = value;
                return true;
            case POS_Y_PROPERTY_KEY:
                mPosY = value;
                return true;
            case ORIGIN_X_PROPERTY_KEY:
                mOriginX = value;
                return true;
            case ORIGIN_Y_PROPERTY_KEY:
                mOriginY = value;
                return true;
            case WIDTH_PROPERTY_KEY:
                mWidth = value;
                return true;
            case HEIGHT_PROPERTY_KEY:
                mHeight = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case X_PROPERTY_KEY:
                setX(value);
                return true;
            case Y_PROPERTY_KEY:
                setY(value);
                return true;
            case POS_X_PROPERTY_KEY:
                setPosX(value);
                return true;
            case POS_Y_PROPERTY_KEY:
                setPosY(value);
                return true;
            case ORIGIN_X_PROPERTY_KEY:
                setOriginX(value);
                return true;
            case ORIGIN_Y_PROPERTY_KEY:
                setOriginY(value);
                return true;
            case WIDTH_PROPERTY_KEY:
                setWidth(value);
                return true;
            case HEIGHT_PROPERTY_KEY:
                setHeight(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case X_PROPERTY_KEY:
                return mX;
            case Y_PROPERTY_KEY:
                return mY;
            case POS_X_PROPERTY_KEY:
                return mPosX;
            case POS_Y_PROPERTY_KEY:
                return mPosY;
            case ORIGIN_X_PROPERTY_KEY:
                return mOriginX;
            case ORIGIN_Y_PROPERTY_KEY:
                return mOriginY;
            case WIDTH_PROPERTY_KEY:
                return mWidth;
            case HEIGHT_PROPERTY_KEY:
                return mHeight;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case X_ID_PROPERTY_KEY:
                mXId = value;
                return true;
            case Y_ID_PROPERTY_KEY:
                mYId = value;
                return true;
            case JOYSTICK_FLAGS_PROPERTY_KEY:
                mJoystickFlags = value;
                return true;
            case HANDLE_SOURCE_ID_PROPERTY_KEY:
                mHandleSourceId = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case X_ID_PROPERTY_KEY:
                setXId(value);
                return true;
            case Y_ID_PROPERTY_KEY:
                setYId(value);
                return true;
            case JOYSTICK_FLAGS_PROPERTY_KEY:
                setJoystickFlags(value);
                return true;
            case HANDLE_SOURCE_ID_PROPERTY_KEY:
                setHandleSourceId(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case X_ID_PROPERTY_KEY:
                return mXId;
            case Y_ID_PROPERTY_KEY:
                return mYId;
            case JOYSTICK_FLAGS_PROPERTY_KEY:
                return mJoystickFlags;
            case HANDLE_SOURCE_ID_PROPERTY_KEY:
                return mHandleSourceId;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
