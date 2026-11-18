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

/// Generated from rive-runtime's `ScrollConstraintBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `ScrollConstraint`.
public abstract class ScrollConstraintBase extends DraggableConstraint {
    public static final int TYPE_KEY = 521;
    public static final int SCROLL_OFFSET_X_PROPERTY_KEY = 759;
    public static final int SCROLL_OFFSET_Y_PROPERTY_KEY = 760;
    public static final int SCROLL_PERCENT_X_PROPERTY_KEY = 761;
    public static final int SCROLL_PERCENT_Y_PROPERTY_KEY = 762;
    public static final int SCROLL_INDEX_PROPERTY_KEY = 763;
    public static final int SNAP_PROPERTY_KEY = 724;
    public static final int PHYSICS_TYPE_VALUE_PROPERTY_KEY = 727;
    public static final int PHYSICS_ID_PROPERTY_KEY = 726;
    public static final int VIRTUALIZE_PROPERTY_KEY = 850;
    public static final int VIRTUALIZE_BUFFER_PROPERTY_KEY = 221;
    public static final int INFINITE_PROPERTY_KEY = 851;
    public static final int INTERACTIVE_PROPERTY_KEY = 891;
    public static final int THRESHOLD_PROPERTY_KEY = 894;
    public static final int VELOCITY_X_PROPERTY_KEY = 1023;
    public static final int VELOCITY_Y_PROPERTY_KEY = 1024;
    public static final int SCROLL_ACTIVE_PROPERTY_KEY = 1025;
    public static final int DRAG_MULTIPLIER_PROPERTY_KEY = 1029;
    public static final int COMPUTED_CONTENT_WIDTH_PROPERTY_KEY = 1069;
    public static final int COMPUTED_CONTENT_HEIGHT_PROPERTY_KEY = 1070;
    public static final int SCROLL_FLAGS_PROPERTY_KEY = 466;
    public static final int WHEEL_INTERACTIVE_PROPERTY_KEY = 467;

    protected float mScrollOffsetX = 0.0f;
    protected float mScrollOffsetY = 0.0f;
    protected float mScrollPercentX = 0f;
    protected float mScrollPercentY = 0f;
    protected float mScrollIndex = 0f;
    protected boolean mSnap = false;
    protected int mPhysicsTypeValue = 0;
    protected int mPhysicsId = -1;
    protected boolean mVirtualize = false;
    protected int mVirtualizeBuffer = 0;
    protected boolean mInfinite = false;
    protected boolean mInteractive = true;
    protected float mThreshold = 0.0f;
    protected float mVelocityX = 0f;
    protected float mVelocityY = 0f;
    protected boolean mScrollActive = false;
    protected float mDragMultiplier = 1.0f;
    protected float mComputedContentWidth = 0f;
    protected float mComputedContentHeight = 0f;
    protected int mScrollFlags = 1;
    protected boolean mWheelInteractive = false;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getScrollOffsetX() {
        return mScrollOffsetX;
    }

    public void setScrollOffsetX(float value) {
        if (Float.floatToIntBits(mScrollOffsetX) == Float.floatToIntBits(value)) {
            return;
        }
        mScrollOffsetX = value;
        scrollOffsetXChanged();
    }

    protected void scrollOffsetXChanged() {
    }

    public float getScrollOffsetY() {
        return mScrollOffsetY;
    }

    public void setScrollOffsetY(float value) {
        if (Float.floatToIntBits(mScrollOffsetY) == Float.floatToIntBits(value)) {
            return;
        }
        mScrollOffsetY = value;
        scrollOffsetYChanged();
    }

    protected void scrollOffsetYChanged() {
    }

    public float getScrollPercentX() {
        return mScrollPercentX;
    }

    public void setScrollPercentX(float value) {
        if (Float.floatToIntBits(mScrollPercentX) == Float.floatToIntBits(value)) {
            return;
        }
        mScrollPercentX = value;
        scrollPercentXChanged();
    }

    protected void scrollPercentXChanged() {
    }

    public float getScrollPercentY() {
        return mScrollPercentY;
    }

    public void setScrollPercentY(float value) {
        if (Float.floatToIntBits(mScrollPercentY) == Float.floatToIntBits(value)) {
            return;
        }
        mScrollPercentY = value;
        scrollPercentYChanged();
    }

    protected void scrollPercentYChanged() {
    }

    public float getScrollIndex() {
        return mScrollIndex;
    }

    public void setScrollIndex(float value) {
        if (Float.floatToIntBits(mScrollIndex) == Float.floatToIntBits(value)) {
            return;
        }
        mScrollIndex = value;
        scrollIndexChanged();
    }

    protected void scrollIndexChanged() {
    }

    public boolean isSnap() {
        return mSnap;
    }

    public void setSnap(boolean value) {
        if (mSnap == value) {
            return;
        }
        mSnap = value;
        snapChanged();
    }

    protected void snapChanged() {
    }

    public int getPhysicsTypeValue() {
        return mPhysicsTypeValue;
    }

    public void setPhysicsTypeValue(int value) {
        if (mPhysicsTypeValue == value) {
            return;
        }
        mPhysicsTypeValue = value;
        physicsTypeValueChanged();
    }

    protected void physicsTypeValueChanged() {
    }

    public int getPhysicsId() {
        return mPhysicsId;
    }

    public void setPhysicsId(int value) {
        if (mPhysicsId == value) {
            return;
        }
        mPhysicsId = value;
        physicsIdChanged();
    }

    protected void physicsIdChanged() {
    }

    public boolean isVirtualize() {
        return mVirtualize;
    }

    public void setVirtualize(boolean value) {
        if (mVirtualize == value) {
            return;
        }
        mVirtualize = value;
        virtualizeChanged();
    }

    protected void virtualizeChanged() {
    }

    public int getVirtualizeBuffer() {
        return mVirtualizeBuffer;
    }

    public void setVirtualizeBuffer(int value) {
        if (mVirtualizeBuffer == value) {
            return;
        }
        mVirtualizeBuffer = value;
        virtualizeBufferChanged();
    }

    protected void virtualizeBufferChanged() {
    }

    public boolean isInfinite() {
        return mInfinite;
    }

    public void setInfinite(boolean value) {
        if (mInfinite == value) {
            return;
        }
        mInfinite = value;
        infiniteChanged();
    }

    protected void infiniteChanged() {
    }

    public boolean isInteractive() {
        return mInteractive;
    }

    public void setInteractive(boolean value) {
        if (mInteractive == value) {
            return;
        }
        mInteractive = value;
        interactiveChanged();
    }

    protected void interactiveChanged() {
    }

    public float getThreshold() {
        return mThreshold;
    }

    public void setThreshold(float value) {
        if (Float.floatToIntBits(mThreshold) == Float.floatToIntBits(value)) {
            return;
        }
        mThreshold = value;
        thresholdChanged();
    }

    protected void thresholdChanged() {
    }

    public float getVelocityX() {
        return mVelocityX;
    }

    public void setVelocityX(float value) {
        if (Float.floatToIntBits(mVelocityX) == Float.floatToIntBits(value)) {
            return;
        }
        mVelocityX = value;
        velocityXChanged();
    }

    protected void velocityXChanged() {
    }

    public float getVelocityY() {
        return mVelocityY;
    }

    public void setVelocityY(float value) {
        if (Float.floatToIntBits(mVelocityY) == Float.floatToIntBits(value)) {
            return;
        }
        mVelocityY = value;
        velocityYChanged();
    }

    protected void velocityYChanged() {
    }

    public boolean isScrollActive() {
        return mScrollActive;
    }

    public void setScrollActive(boolean value) {
        if (mScrollActive == value) {
            return;
        }
        mScrollActive = value;
        scrollActiveChanged();
    }

    protected void scrollActiveChanged() {
    }

    public float getDragMultiplier() {
        return mDragMultiplier;
    }

    public void setDragMultiplier(float value) {
        if (Float.floatToIntBits(mDragMultiplier) == Float.floatToIntBits(value)) {
            return;
        }
        mDragMultiplier = value;
        dragMultiplierChanged();
    }

    protected void dragMultiplierChanged() {
    }

    public float getComputedContentWidth() {
        return mComputedContentWidth;
    }

    public void setComputedContentWidth(float value) {
        if (Float.floatToIntBits(mComputedContentWidth) == Float.floatToIntBits(value)) {
            return;
        }
        mComputedContentWidth = value;
        computedContentWidthChanged();
    }

    protected void computedContentWidthChanged() {
    }

    public float getComputedContentHeight() {
        return mComputedContentHeight;
    }

    public void setComputedContentHeight(float value) {
        if (Float.floatToIntBits(mComputedContentHeight) == Float.floatToIntBits(value)) {
            return;
        }
        mComputedContentHeight = value;
        computedContentHeightChanged();
    }

    protected void computedContentHeightChanged() {
    }

    public int getScrollFlags() {
        return mScrollFlags;
    }

    public void setScrollFlags(int value) {
        if (mScrollFlags == value) {
            return;
        }
        mScrollFlags = value;
        scrollFlagsChanged();
    }

    protected void scrollFlagsChanged() {
    }

    public boolean isWheelInteractive() {
        return mWheelInteractive;
    }

    public void setWheelInteractive(boolean value) {
        if (mWheelInteractive == value) {
            return;
        }
        mWheelInteractive = value;
        wheelInteractiveChanged();
    }

    protected void wheelInteractiveChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case SNAP_PROPERTY_KEY:
                mSnap = value;
                return true;
            case VIRTUALIZE_PROPERTY_KEY:
                mVirtualize = value;
                return true;
            case INFINITE_PROPERTY_KEY:
                mInfinite = value;
                return true;
            case INTERACTIVE_PROPERTY_KEY:
                mInteractive = value;
                return true;
            case SCROLL_ACTIVE_PROPERTY_KEY:
                mScrollActive = value;
                return true;
            case WHEEL_INTERACTIVE_PROPERTY_KEY:
                mWheelInteractive = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case SNAP_PROPERTY_KEY:
                setSnap(value);
                return true;
            case VIRTUALIZE_PROPERTY_KEY:
                setVirtualize(value);
                return true;
            case INFINITE_PROPERTY_KEY:
                setInfinite(value);
                return true;
            case INTERACTIVE_PROPERTY_KEY:
                setInteractive(value);
                return true;
            case SCROLL_ACTIVE_PROPERTY_KEY:
                setScrollActive(value);
                return true;
            case WHEEL_INTERACTIVE_PROPERTY_KEY:
                setWheelInteractive(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case SNAP_PROPERTY_KEY:
                return mSnap;
            case VIRTUALIZE_PROPERTY_KEY:
                return mVirtualize;
            case INFINITE_PROPERTY_KEY:
                return mInfinite;
            case INTERACTIVE_PROPERTY_KEY:
                return mInteractive;
            case SCROLL_ACTIVE_PROPERTY_KEY:
                return mScrollActive;
            case WHEEL_INTERACTIVE_PROPERTY_KEY:
                return mWheelInteractive;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case SCROLL_OFFSET_X_PROPERTY_KEY:
                mScrollOffsetX = value;
                return true;
            case SCROLL_OFFSET_Y_PROPERTY_KEY:
                mScrollOffsetY = value;
                return true;
            case SCROLL_PERCENT_X_PROPERTY_KEY:
                mScrollPercentX = value;
                return true;
            case SCROLL_PERCENT_Y_PROPERTY_KEY:
                mScrollPercentY = value;
                return true;
            case SCROLL_INDEX_PROPERTY_KEY:
                mScrollIndex = value;
                return true;
            case THRESHOLD_PROPERTY_KEY:
                mThreshold = value;
                return true;
            case VELOCITY_X_PROPERTY_KEY:
                mVelocityX = value;
                return true;
            case VELOCITY_Y_PROPERTY_KEY:
                mVelocityY = value;
                return true;
            case DRAG_MULTIPLIER_PROPERTY_KEY:
                mDragMultiplier = value;
                return true;
            case COMPUTED_CONTENT_WIDTH_PROPERTY_KEY:
                mComputedContentWidth = value;
                return true;
            case COMPUTED_CONTENT_HEIGHT_PROPERTY_KEY:
                mComputedContentHeight = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case SCROLL_OFFSET_X_PROPERTY_KEY:
                setScrollOffsetX(value);
                return true;
            case SCROLL_OFFSET_Y_PROPERTY_KEY:
                setScrollOffsetY(value);
                return true;
            case SCROLL_PERCENT_X_PROPERTY_KEY:
                setScrollPercentX(value);
                return true;
            case SCROLL_PERCENT_Y_PROPERTY_KEY:
                setScrollPercentY(value);
                return true;
            case SCROLL_INDEX_PROPERTY_KEY:
                setScrollIndex(value);
                return true;
            case THRESHOLD_PROPERTY_KEY:
                setThreshold(value);
                return true;
            case VELOCITY_X_PROPERTY_KEY:
                setVelocityX(value);
                return true;
            case VELOCITY_Y_PROPERTY_KEY:
                setVelocityY(value);
                return true;
            case DRAG_MULTIPLIER_PROPERTY_KEY:
                setDragMultiplier(value);
                return true;
            case COMPUTED_CONTENT_WIDTH_PROPERTY_KEY:
                setComputedContentWidth(value);
                return true;
            case COMPUTED_CONTENT_HEIGHT_PROPERTY_KEY:
                setComputedContentHeight(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case SCROLL_OFFSET_X_PROPERTY_KEY:
                return mScrollOffsetX;
            case SCROLL_OFFSET_Y_PROPERTY_KEY:
                return mScrollOffsetY;
            case SCROLL_PERCENT_X_PROPERTY_KEY:
                return mScrollPercentX;
            case SCROLL_PERCENT_Y_PROPERTY_KEY:
                return mScrollPercentY;
            case SCROLL_INDEX_PROPERTY_KEY:
                return mScrollIndex;
            case THRESHOLD_PROPERTY_KEY:
                return mThreshold;
            case VELOCITY_X_PROPERTY_KEY:
                return mVelocityX;
            case VELOCITY_Y_PROPERTY_KEY:
                return mVelocityY;
            case DRAG_MULTIPLIER_PROPERTY_KEY:
                return mDragMultiplier;
            case COMPUTED_CONTENT_WIDTH_PROPERTY_KEY:
                return mComputedContentWidth;
            case COMPUTED_CONTENT_HEIGHT_PROPERTY_KEY:
                return mComputedContentHeight;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case PHYSICS_TYPE_VALUE_PROPERTY_KEY:
                mPhysicsTypeValue = value;
                return true;
            case PHYSICS_ID_PROPERTY_KEY:
                mPhysicsId = value;
                return true;
            case VIRTUALIZE_BUFFER_PROPERTY_KEY:
                mVirtualizeBuffer = value;
                return true;
            case SCROLL_FLAGS_PROPERTY_KEY:
                mScrollFlags = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case PHYSICS_TYPE_VALUE_PROPERTY_KEY:
                setPhysicsTypeValue(value);
                return true;
            case PHYSICS_ID_PROPERTY_KEY:
                setPhysicsId(value);
                return true;
            case VIRTUALIZE_BUFFER_PROPERTY_KEY:
                setVirtualizeBuffer(value);
                return true;
            case SCROLL_FLAGS_PROPERTY_KEY:
                setScrollFlags(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case PHYSICS_TYPE_VALUE_PROPERTY_KEY:
                return mPhysicsTypeValue;
            case PHYSICS_ID_PROPERTY_KEY:
                return mPhysicsId;
            case VIRTUALIZE_BUFFER_PROPERTY_KEY:
                return mVirtualizeBuffer;
            case SCROLL_FLAGS_PROPERTY_KEY:
                return mScrollFlags;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
