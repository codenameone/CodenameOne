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

/// Generated from rive-runtime's `LayoutComponentStyleBase` by `scripts/rive/gen-rive-schema.py`;
/// do not edit. Behaviour belongs in `LayoutComponentStyle`.
public abstract class LayoutComponentStyleBase extends LayoutSizingStyle {
    public static final int TYPE_KEY = 420;
    public static final int GAP_HORIZONTAL_PROPERTY_KEY = 498;
    public static final int GAP_VERTICAL_PROPERTY_KEY = 499;
    public static final int BORDER_LEFT_PROPERTY_KEY = 504;
    public static final int BORDER_RIGHT_PROPERTY_KEY = 505;
    public static final int BORDER_TOP_PROPERTY_KEY = 506;
    public static final int BORDER_BOTTOM_PROPERTY_KEY = 507;
    public static final int MARGIN_LEFT_PROPERTY_KEY = 508;
    public static final int MARGIN_RIGHT_PROPERTY_KEY = 509;
    public static final int MARGIN_TOP_PROPERTY_KEY = 510;
    public static final int MARGIN_BOTTOM_PROPERTY_KEY = 511;
    public static final int PADDING_LEFT_PROPERTY_KEY = 512;
    public static final int PADDING_RIGHT_PROPERTY_KEY = 513;
    public static final int PADDING_TOP_PROPERTY_KEY = 514;
    public static final int PADDING_BOTTOM_PROPERTY_KEY = 515;
    public static final int POSITION_LEFT_PROPERTY_KEY = 516;
    public static final int POSITION_RIGHT_PROPERTY_KEY = 517;
    public static final int POSITION_TOP_PROPERTY_KEY = 518;
    public static final int POSITION_BOTTOM_PROPERTY_KEY = 519;
    public static final int POSITION_LEFT_UNITS_VALUE_PROPERTY_KEY = 621;
    public static final int POSITION_RIGHT_UNITS_VALUE_PROPERTY_KEY = 622;
    public static final int POSITION_TOP_UNITS_VALUE_PROPERTY_KEY = 623;
    public static final int POSITION_BOTTOM_UNITS_VALUE_PROPERTY_KEY = 624;
    public static final int FLEX_BASIS_PROPERTY_KEY = 523;
    public static final int ASPECT_RATIO_PROPERTY_KEY = 524;
    public static final int ASPECT_RATIO_AXIS_VALUE_PROPERTY_KEY = 546;
    public static final int INTERPOLATOR_ID_PROPERTY_KEY = 591;
    public static final int INTERPOLATION_TIME_PROPERTY_KEY = 592;
    public static final int FLEX_BASIS_UNITS_VALUE_PROPERTY_KEY = 705;
    public static final int LAYOUT_ALIGNMENT_TYPE_PROPERTY_KEY = 632;
    public static final int ANIMATION_STYLE_TYPE_PROPERTY_KEY = 589;
    public static final int INTERPOLATION_TYPE_PROPERTY_KEY = 590;
    public static final int POSITION_TYPE_VALUE_PROPERTY_KEY = 597;
    public static final int FLEX_DIRECTION_VALUE_PROPERTY_KEY = 598;
    public static final int DIRECTION_VALUE_PROPERTY_KEY = 599;
    public static final int FLEX_WRAP_VALUE_PROPERTY_KEY = 604;
    public static final int OVERFLOW_VALUE_PROPERTY_KEY = 605;
    public static final int INTRINSICALLY_SIZED_VALUE_PROPERTY_KEY = 606;
    public static final int BORDER_LEFT_UNITS_VALUE_PROPERTY_KEY = 609;
    public static final int BORDER_RIGHT_UNITS_VALUE_PROPERTY_KEY = 610;
    public static final int BORDER_TOP_UNITS_VALUE_PROPERTY_KEY = 611;
    public static final int BORDER_BOTTOM_UNITS_VALUE_PROPERTY_KEY = 612;
    public static final int MARGIN_LEFT_UNITS_VALUE_PROPERTY_KEY = 613;
    public static final int MARGIN_RIGHT_UNITS_VALUE_PROPERTY_KEY = 614;
    public static final int MARGIN_TOP_UNITS_VALUE_PROPERTY_KEY = 615;
    public static final int MARGIN_BOTTOM_UNITS_VALUE_PROPERTY_KEY = 616;
    public static final int PADDING_LEFT_UNITS_VALUE_PROPERTY_KEY = 617;
    public static final int PADDING_RIGHT_UNITS_VALUE_PROPERTY_KEY = 618;
    public static final int PADDING_TOP_UNITS_VALUE_PROPERTY_KEY = 619;
    public static final int PADDING_BOTTOM_UNITS_VALUE_PROPERTY_KEY = 620;
    public static final int GAP_HORIZONTAL_UNITS_VALUE_PROPERTY_KEY = 625;
    public static final int GAP_VERTICAL_UNITS_VALUE_PROPERTY_KEY = 626;
    public static final int JUSTIFY_ITEMS_VALUE_PROPERTY_KEY = 1045;
    public static final int LAYOUT_TYPE_VALUE_PROPERTY_KEY = 1059;
    public static final int LINK_CORNER_RADIUS_PROPERTY_KEY = 639;
    public static final int CORNER_RADIUS_TL_PROPERTY_KEY = 640;
    public static final int CORNER_RADIUS_TR_PROPERTY_KEY = 641;
    public static final int CORNER_RADIUS_BL_PROPERTY_KEY = 642;
    public static final int CORNER_RADIUS_BR_PROPERTY_KEY = 643;

    protected float mGapHorizontal = 0.0f;
    protected float mGapVertical = 0.0f;
    protected float mBorderLeft = 0.0f;
    protected float mBorderRight = 0.0f;
    protected float mBorderTop = 0.0f;
    protected float mBorderBottom = 0.0f;
    protected float mMarginLeft = 0.0f;
    protected float mMarginRight = 0.0f;
    protected float mMarginTop = 0.0f;
    protected float mMarginBottom = 0.0f;
    protected float mPaddingLeft = 0.0f;
    protected float mPaddingRight = 0.0f;
    protected float mPaddingTop = 0.0f;
    protected float mPaddingBottom = 0.0f;
    protected float mPositionLeft = 0.0f;
    protected float mPositionRight = 0.0f;
    protected float mPositionTop = 0.0f;
    protected float mPositionBottom = 0.0f;
    protected int mPositionLeftUnitsValue = 0;
    protected int mPositionRightUnitsValue = 0;
    protected int mPositionTopUnitsValue = 0;
    protected int mPositionBottomUnitsValue = 0;
    protected float mFlexBasis = 0.0f;
    protected float mAspectRatio = 0.0f;
    protected int mAspectRatioAxisValue = 0;
    protected int mInterpolatorId = -1;
    protected float mInterpolationTime = 0.0f;
    protected int mFlexBasisUnitsValue = 3;
    protected int mLayoutAlignmentType = 0;
    protected int mAnimationStyleType = 0;
    protected int mInterpolationType = 0;
    protected int mPositionTypeValue = 1;
    protected int mFlexDirectionValue = 2;
    protected int mDirectionValue = 0;
    protected int mFlexWrapValue = 0;
    protected int mOverflowValue = 0;
    protected boolean mIntrinsicallySizedValue = false;
    protected int mBorderLeftUnitsValue = 0;
    protected int mBorderRightUnitsValue = 0;
    protected int mBorderTopUnitsValue = 0;
    protected int mBorderBottomUnitsValue = 0;
    protected int mMarginLeftUnitsValue = 0;
    protected int mMarginRightUnitsValue = 0;
    protected int mMarginTopUnitsValue = 0;
    protected int mMarginBottomUnitsValue = 0;
    protected int mPaddingLeftUnitsValue = 0;
    protected int mPaddingRightUnitsValue = 0;
    protected int mPaddingTopUnitsValue = 0;
    protected int mPaddingBottomUnitsValue = 0;
    protected int mGapHorizontalUnitsValue = 0;
    protected int mGapVerticalUnitsValue = 0;
    protected int mJustifyItemsValue = 7;
    protected int mLayoutTypeValue = 0;
    protected boolean mLinkCornerRadius = true;
    protected float mCornerRadiusTL = 0.0f;
    protected float mCornerRadiusTR = 0.0f;
    protected float mCornerRadiusBL = 0.0f;
    protected float mCornerRadiusBR = 0.0f;

    @Override
    public int coreType() {
        return TYPE_KEY;
    }

    @Override
    public boolean isTypeOf(int typeKey) {
        return typeKey == TYPE_KEY || super.isTypeOf(typeKey);
    }

    public float getGapHorizontal() {
        return mGapHorizontal;
    }

    public void setGapHorizontal(float value) {
        if (Float.floatToIntBits(mGapHorizontal) == Float.floatToIntBits(value)) {
            return;
        }
        mGapHorizontal = value;
        gapHorizontalChanged();
    }

    protected void gapHorizontalChanged() {
    }

    public float getGapVertical() {
        return mGapVertical;
    }

    public void setGapVertical(float value) {
        if (Float.floatToIntBits(mGapVertical) == Float.floatToIntBits(value)) {
            return;
        }
        mGapVertical = value;
        gapVerticalChanged();
    }

    protected void gapVerticalChanged() {
    }

    public float getBorderLeft() {
        return mBorderLeft;
    }

    public void setBorderLeft(float value) {
        if (Float.floatToIntBits(mBorderLeft) == Float.floatToIntBits(value)) {
            return;
        }
        mBorderLeft = value;
        borderLeftChanged();
    }

    protected void borderLeftChanged() {
    }

    public float getBorderRight() {
        return mBorderRight;
    }

    public void setBorderRight(float value) {
        if (Float.floatToIntBits(mBorderRight) == Float.floatToIntBits(value)) {
            return;
        }
        mBorderRight = value;
        borderRightChanged();
    }

    protected void borderRightChanged() {
    }

    public float getBorderTop() {
        return mBorderTop;
    }

    public void setBorderTop(float value) {
        if (Float.floatToIntBits(mBorderTop) == Float.floatToIntBits(value)) {
            return;
        }
        mBorderTop = value;
        borderTopChanged();
    }

    protected void borderTopChanged() {
    }

    public float getBorderBottom() {
        return mBorderBottom;
    }

    public void setBorderBottom(float value) {
        if (Float.floatToIntBits(mBorderBottom) == Float.floatToIntBits(value)) {
            return;
        }
        mBorderBottom = value;
        borderBottomChanged();
    }

    protected void borderBottomChanged() {
    }

    public float getMarginLeft() {
        return mMarginLeft;
    }

    public void setMarginLeft(float value) {
        if (Float.floatToIntBits(mMarginLeft) == Float.floatToIntBits(value)) {
            return;
        }
        mMarginLeft = value;
        marginLeftChanged();
    }

    protected void marginLeftChanged() {
    }

    public float getMarginRight() {
        return mMarginRight;
    }

    public void setMarginRight(float value) {
        if (Float.floatToIntBits(mMarginRight) == Float.floatToIntBits(value)) {
            return;
        }
        mMarginRight = value;
        marginRightChanged();
    }

    protected void marginRightChanged() {
    }

    public float getMarginTop() {
        return mMarginTop;
    }

    public void setMarginTop(float value) {
        if (Float.floatToIntBits(mMarginTop) == Float.floatToIntBits(value)) {
            return;
        }
        mMarginTop = value;
        marginTopChanged();
    }

    protected void marginTopChanged() {
    }

    public float getMarginBottom() {
        return mMarginBottom;
    }

    public void setMarginBottom(float value) {
        if (Float.floatToIntBits(mMarginBottom) == Float.floatToIntBits(value)) {
            return;
        }
        mMarginBottom = value;
        marginBottomChanged();
    }

    protected void marginBottomChanged() {
    }

    public float getPaddingLeft() {
        return mPaddingLeft;
    }

    public void setPaddingLeft(float value) {
        if (Float.floatToIntBits(mPaddingLeft) == Float.floatToIntBits(value)) {
            return;
        }
        mPaddingLeft = value;
        paddingLeftChanged();
    }

    protected void paddingLeftChanged() {
    }

    public float getPaddingRight() {
        return mPaddingRight;
    }

    public void setPaddingRight(float value) {
        if (Float.floatToIntBits(mPaddingRight) == Float.floatToIntBits(value)) {
            return;
        }
        mPaddingRight = value;
        paddingRightChanged();
    }

    protected void paddingRightChanged() {
    }

    public float getPaddingTop() {
        return mPaddingTop;
    }

    public void setPaddingTop(float value) {
        if (Float.floatToIntBits(mPaddingTop) == Float.floatToIntBits(value)) {
            return;
        }
        mPaddingTop = value;
        paddingTopChanged();
    }

    protected void paddingTopChanged() {
    }

    public float getPaddingBottom() {
        return mPaddingBottom;
    }

    public void setPaddingBottom(float value) {
        if (Float.floatToIntBits(mPaddingBottom) == Float.floatToIntBits(value)) {
            return;
        }
        mPaddingBottom = value;
        paddingBottomChanged();
    }

    protected void paddingBottomChanged() {
    }

    public float getPositionLeft() {
        return mPositionLeft;
    }

    public void setPositionLeft(float value) {
        if (Float.floatToIntBits(mPositionLeft) == Float.floatToIntBits(value)) {
            return;
        }
        mPositionLeft = value;
        positionLeftChanged();
    }

    protected void positionLeftChanged() {
    }

    public float getPositionRight() {
        return mPositionRight;
    }

    public void setPositionRight(float value) {
        if (Float.floatToIntBits(mPositionRight) == Float.floatToIntBits(value)) {
            return;
        }
        mPositionRight = value;
        positionRightChanged();
    }

    protected void positionRightChanged() {
    }

    public float getPositionTop() {
        return mPositionTop;
    }

    public void setPositionTop(float value) {
        if (Float.floatToIntBits(mPositionTop) == Float.floatToIntBits(value)) {
            return;
        }
        mPositionTop = value;
        positionTopChanged();
    }

    protected void positionTopChanged() {
    }

    public float getPositionBottom() {
        return mPositionBottom;
    }

    public void setPositionBottom(float value) {
        if (Float.floatToIntBits(mPositionBottom) == Float.floatToIntBits(value)) {
            return;
        }
        mPositionBottom = value;
        positionBottomChanged();
    }

    protected void positionBottomChanged() {
    }

    public int getPositionLeftUnitsValue() {
        return mPositionLeftUnitsValue;
    }

    public void setPositionLeftUnitsValue(int value) {
        if (mPositionLeftUnitsValue == value) {
            return;
        }
        mPositionLeftUnitsValue = value;
        positionLeftUnitsValueChanged();
    }

    protected void positionLeftUnitsValueChanged() {
    }

    public int getPositionRightUnitsValue() {
        return mPositionRightUnitsValue;
    }

    public void setPositionRightUnitsValue(int value) {
        if (mPositionRightUnitsValue == value) {
            return;
        }
        mPositionRightUnitsValue = value;
        positionRightUnitsValueChanged();
    }

    protected void positionRightUnitsValueChanged() {
    }

    public int getPositionTopUnitsValue() {
        return mPositionTopUnitsValue;
    }

    public void setPositionTopUnitsValue(int value) {
        if (mPositionTopUnitsValue == value) {
            return;
        }
        mPositionTopUnitsValue = value;
        positionTopUnitsValueChanged();
    }

    protected void positionTopUnitsValueChanged() {
    }

    public int getPositionBottomUnitsValue() {
        return mPositionBottomUnitsValue;
    }

    public void setPositionBottomUnitsValue(int value) {
        if (mPositionBottomUnitsValue == value) {
            return;
        }
        mPositionBottomUnitsValue = value;
        positionBottomUnitsValueChanged();
    }

    protected void positionBottomUnitsValueChanged() {
    }

    public float getFlexBasis() {
        return mFlexBasis;
    }

    public void setFlexBasis(float value) {
        if (Float.floatToIntBits(mFlexBasis) == Float.floatToIntBits(value)) {
            return;
        }
        mFlexBasis = value;
        flexBasisChanged();
    }

    protected void flexBasisChanged() {
    }

    public float getAspectRatio() {
        return mAspectRatio;
    }

    public void setAspectRatio(float value) {
        if (Float.floatToIntBits(mAspectRatio) == Float.floatToIntBits(value)) {
            return;
        }
        mAspectRatio = value;
        aspectRatioChanged();
    }

    protected void aspectRatioChanged() {
    }

    public int getAspectRatioAxisValue() {
        return mAspectRatioAxisValue;
    }

    public void setAspectRatioAxisValue(int value) {
        if (mAspectRatioAxisValue == value) {
            return;
        }
        mAspectRatioAxisValue = value;
        aspectRatioAxisValueChanged();
    }

    protected void aspectRatioAxisValueChanged() {
    }

    public int getInterpolatorId() {
        return mInterpolatorId;
    }

    public void setInterpolatorId(int value) {
        if (mInterpolatorId == value) {
            return;
        }
        mInterpolatorId = value;
        interpolatorIdChanged();
    }

    protected void interpolatorIdChanged() {
    }

    public float getInterpolationTime() {
        return mInterpolationTime;
    }

    public void setInterpolationTime(float value) {
        if (Float.floatToIntBits(mInterpolationTime) == Float.floatToIntBits(value)) {
            return;
        }
        mInterpolationTime = value;
        interpolationTimeChanged();
    }

    protected void interpolationTimeChanged() {
    }

    public int getFlexBasisUnitsValue() {
        return mFlexBasisUnitsValue;
    }

    public void setFlexBasisUnitsValue(int value) {
        if (mFlexBasisUnitsValue == value) {
            return;
        }
        mFlexBasisUnitsValue = value;
        flexBasisUnitsValueChanged();
    }

    protected void flexBasisUnitsValueChanged() {
    }

    public int getLayoutAlignmentType() {
        return mLayoutAlignmentType;
    }

    public void setLayoutAlignmentType(int value) {
        if (mLayoutAlignmentType == value) {
            return;
        }
        mLayoutAlignmentType = value;
        layoutAlignmentTypeChanged();
    }

    protected void layoutAlignmentTypeChanged() {
    }

    public int getAnimationStyleType() {
        return mAnimationStyleType;
    }

    public void setAnimationStyleType(int value) {
        if (mAnimationStyleType == value) {
            return;
        }
        mAnimationStyleType = value;
        animationStyleTypeChanged();
    }

    protected void animationStyleTypeChanged() {
    }

    public int getInterpolationType() {
        return mInterpolationType;
    }

    public void setInterpolationType(int value) {
        if (mInterpolationType == value) {
            return;
        }
        mInterpolationType = value;
        interpolationTypeChanged();
    }

    protected void interpolationTypeChanged() {
    }

    public int getPositionTypeValue() {
        return mPositionTypeValue;
    }

    public void setPositionTypeValue(int value) {
        if (mPositionTypeValue == value) {
            return;
        }
        mPositionTypeValue = value;
        positionTypeValueChanged();
    }

    protected void positionTypeValueChanged() {
    }

    public int getFlexDirectionValue() {
        return mFlexDirectionValue;
    }

    public void setFlexDirectionValue(int value) {
        if (mFlexDirectionValue == value) {
            return;
        }
        mFlexDirectionValue = value;
        flexDirectionValueChanged();
    }

    protected void flexDirectionValueChanged() {
    }

    public int getDirectionValue() {
        return mDirectionValue;
    }

    public void setDirectionValue(int value) {
        if (mDirectionValue == value) {
            return;
        }
        mDirectionValue = value;
        directionValueChanged();
    }

    protected void directionValueChanged() {
    }

    public int getFlexWrapValue() {
        return mFlexWrapValue;
    }

    public void setFlexWrapValue(int value) {
        if (mFlexWrapValue == value) {
            return;
        }
        mFlexWrapValue = value;
        flexWrapValueChanged();
    }

    protected void flexWrapValueChanged() {
    }

    public int getOverflowValue() {
        return mOverflowValue;
    }

    public void setOverflowValue(int value) {
        if (mOverflowValue == value) {
            return;
        }
        mOverflowValue = value;
        overflowValueChanged();
    }

    protected void overflowValueChanged() {
    }

    public boolean isIntrinsicallySizedValue() {
        return mIntrinsicallySizedValue;
    }

    public void setIntrinsicallySizedValue(boolean value) {
        if (mIntrinsicallySizedValue == value) {
            return;
        }
        mIntrinsicallySizedValue = value;
        intrinsicallySizedValueChanged();
    }

    protected void intrinsicallySizedValueChanged() {
    }

    public int getBorderLeftUnitsValue() {
        return mBorderLeftUnitsValue;
    }

    public void setBorderLeftUnitsValue(int value) {
        if (mBorderLeftUnitsValue == value) {
            return;
        }
        mBorderLeftUnitsValue = value;
        borderLeftUnitsValueChanged();
    }

    protected void borderLeftUnitsValueChanged() {
    }

    public int getBorderRightUnitsValue() {
        return mBorderRightUnitsValue;
    }

    public void setBorderRightUnitsValue(int value) {
        if (mBorderRightUnitsValue == value) {
            return;
        }
        mBorderRightUnitsValue = value;
        borderRightUnitsValueChanged();
    }

    protected void borderRightUnitsValueChanged() {
    }

    public int getBorderTopUnitsValue() {
        return mBorderTopUnitsValue;
    }

    public void setBorderTopUnitsValue(int value) {
        if (mBorderTopUnitsValue == value) {
            return;
        }
        mBorderTopUnitsValue = value;
        borderTopUnitsValueChanged();
    }

    protected void borderTopUnitsValueChanged() {
    }

    public int getBorderBottomUnitsValue() {
        return mBorderBottomUnitsValue;
    }

    public void setBorderBottomUnitsValue(int value) {
        if (mBorderBottomUnitsValue == value) {
            return;
        }
        mBorderBottomUnitsValue = value;
        borderBottomUnitsValueChanged();
    }

    protected void borderBottomUnitsValueChanged() {
    }

    public int getMarginLeftUnitsValue() {
        return mMarginLeftUnitsValue;
    }

    public void setMarginLeftUnitsValue(int value) {
        if (mMarginLeftUnitsValue == value) {
            return;
        }
        mMarginLeftUnitsValue = value;
        marginLeftUnitsValueChanged();
    }

    protected void marginLeftUnitsValueChanged() {
    }

    public int getMarginRightUnitsValue() {
        return mMarginRightUnitsValue;
    }

    public void setMarginRightUnitsValue(int value) {
        if (mMarginRightUnitsValue == value) {
            return;
        }
        mMarginRightUnitsValue = value;
        marginRightUnitsValueChanged();
    }

    protected void marginRightUnitsValueChanged() {
    }

    public int getMarginTopUnitsValue() {
        return mMarginTopUnitsValue;
    }

    public void setMarginTopUnitsValue(int value) {
        if (mMarginTopUnitsValue == value) {
            return;
        }
        mMarginTopUnitsValue = value;
        marginTopUnitsValueChanged();
    }

    protected void marginTopUnitsValueChanged() {
    }

    public int getMarginBottomUnitsValue() {
        return mMarginBottomUnitsValue;
    }

    public void setMarginBottomUnitsValue(int value) {
        if (mMarginBottomUnitsValue == value) {
            return;
        }
        mMarginBottomUnitsValue = value;
        marginBottomUnitsValueChanged();
    }

    protected void marginBottomUnitsValueChanged() {
    }

    public int getPaddingLeftUnitsValue() {
        return mPaddingLeftUnitsValue;
    }

    public void setPaddingLeftUnitsValue(int value) {
        if (mPaddingLeftUnitsValue == value) {
            return;
        }
        mPaddingLeftUnitsValue = value;
        paddingLeftUnitsValueChanged();
    }

    protected void paddingLeftUnitsValueChanged() {
    }

    public int getPaddingRightUnitsValue() {
        return mPaddingRightUnitsValue;
    }

    public void setPaddingRightUnitsValue(int value) {
        if (mPaddingRightUnitsValue == value) {
            return;
        }
        mPaddingRightUnitsValue = value;
        paddingRightUnitsValueChanged();
    }

    protected void paddingRightUnitsValueChanged() {
    }

    public int getPaddingTopUnitsValue() {
        return mPaddingTopUnitsValue;
    }

    public void setPaddingTopUnitsValue(int value) {
        if (mPaddingTopUnitsValue == value) {
            return;
        }
        mPaddingTopUnitsValue = value;
        paddingTopUnitsValueChanged();
    }

    protected void paddingTopUnitsValueChanged() {
    }

    public int getPaddingBottomUnitsValue() {
        return mPaddingBottomUnitsValue;
    }

    public void setPaddingBottomUnitsValue(int value) {
        if (mPaddingBottomUnitsValue == value) {
            return;
        }
        mPaddingBottomUnitsValue = value;
        paddingBottomUnitsValueChanged();
    }

    protected void paddingBottomUnitsValueChanged() {
    }

    public int getGapHorizontalUnitsValue() {
        return mGapHorizontalUnitsValue;
    }

    public void setGapHorizontalUnitsValue(int value) {
        if (mGapHorizontalUnitsValue == value) {
            return;
        }
        mGapHorizontalUnitsValue = value;
        gapHorizontalUnitsValueChanged();
    }

    protected void gapHorizontalUnitsValueChanged() {
    }

    public int getGapVerticalUnitsValue() {
        return mGapVerticalUnitsValue;
    }

    public void setGapVerticalUnitsValue(int value) {
        if (mGapVerticalUnitsValue == value) {
            return;
        }
        mGapVerticalUnitsValue = value;
        gapVerticalUnitsValueChanged();
    }

    protected void gapVerticalUnitsValueChanged() {
    }

    public int getJustifyItemsValue() {
        return mJustifyItemsValue;
    }

    public void setJustifyItemsValue(int value) {
        if (mJustifyItemsValue == value) {
            return;
        }
        mJustifyItemsValue = value;
        justifyItemsValueChanged();
    }

    protected void justifyItemsValueChanged() {
    }

    public int getLayoutTypeValue() {
        return mLayoutTypeValue;
    }

    public void setLayoutTypeValue(int value) {
        if (mLayoutTypeValue == value) {
            return;
        }
        mLayoutTypeValue = value;
        layoutTypeValueChanged();
    }

    protected void layoutTypeValueChanged() {
    }

    public boolean isLinkCornerRadius() {
        return mLinkCornerRadius;
    }

    public void setLinkCornerRadius(boolean value) {
        if (mLinkCornerRadius == value) {
            return;
        }
        mLinkCornerRadius = value;
        linkCornerRadiusChanged();
    }

    protected void linkCornerRadiusChanged() {
    }

    public float getCornerRadiusTL() {
        return mCornerRadiusTL;
    }

    public void setCornerRadiusTL(float value) {
        if (Float.floatToIntBits(mCornerRadiusTL) == Float.floatToIntBits(value)) {
            return;
        }
        mCornerRadiusTL = value;
        cornerRadiusTLChanged();
    }

    protected void cornerRadiusTLChanged() {
    }

    public float getCornerRadiusTR() {
        return mCornerRadiusTR;
    }

    public void setCornerRadiusTR(float value) {
        if (Float.floatToIntBits(mCornerRadiusTR) == Float.floatToIntBits(value)) {
            return;
        }
        mCornerRadiusTR = value;
        cornerRadiusTRChanged();
    }

    protected void cornerRadiusTRChanged() {
    }

    public float getCornerRadiusBL() {
        return mCornerRadiusBL;
    }

    public void setCornerRadiusBL(float value) {
        if (Float.floatToIntBits(mCornerRadiusBL) == Float.floatToIntBits(value)) {
            return;
        }
        mCornerRadiusBL = value;
        cornerRadiusBLChanged();
    }

    protected void cornerRadiusBLChanged() {
    }

    public float getCornerRadiusBR() {
        return mCornerRadiusBR;
    }

    public void setCornerRadiusBR(float value) {
        if (Float.floatToIntBits(mCornerRadiusBR) == Float.floatToIntBits(value)) {
            return;
        }
        mCornerRadiusBR = value;
        cornerRadiusBRChanged();
    }

    protected void cornerRadiusBRChanged() {
    }

    @Override
    public boolean deserializeBool(int propertyKey, boolean value) {
        switch (propertyKey) {
            case INTRINSICALLY_SIZED_VALUE_PROPERTY_KEY:
                mIntrinsicallySizedValue = value;
                return true;
            case LINK_CORNER_RADIUS_PROPERTY_KEY:
                mLinkCornerRadius = value;
                return true;
            default:
                return super.deserializeBool(propertyKey, value);
        }
    }

    @Override
    public boolean setBoolProperty(int propertyKey, boolean value) {
        switch (propertyKey) {
            case INTRINSICALLY_SIZED_VALUE_PROPERTY_KEY:
                setIntrinsicallySizedValue(value);
                return true;
            case LINK_CORNER_RADIUS_PROPERTY_KEY:
                setLinkCornerRadius(value);
                return true;
            default:
                return super.setBoolProperty(propertyKey, value);
        }
    }

    @Override
    public boolean getBoolProperty(int propertyKey) {
        switch (propertyKey) {
            case INTRINSICALLY_SIZED_VALUE_PROPERTY_KEY:
                return mIntrinsicallySizedValue;
            case LINK_CORNER_RADIUS_PROPERTY_KEY:
                return mLinkCornerRadius;
            default:
                return super.getBoolProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeDouble(int propertyKey, float value) {
        switch (propertyKey) {
            case GAP_HORIZONTAL_PROPERTY_KEY:
                mGapHorizontal = value;
                return true;
            case GAP_VERTICAL_PROPERTY_KEY:
                mGapVertical = value;
                return true;
            case BORDER_LEFT_PROPERTY_KEY:
                mBorderLeft = value;
                return true;
            case BORDER_RIGHT_PROPERTY_KEY:
                mBorderRight = value;
                return true;
            case BORDER_TOP_PROPERTY_KEY:
                mBorderTop = value;
                return true;
            case BORDER_BOTTOM_PROPERTY_KEY:
                mBorderBottom = value;
                return true;
            case MARGIN_LEFT_PROPERTY_KEY:
                mMarginLeft = value;
                return true;
            case MARGIN_RIGHT_PROPERTY_KEY:
                mMarginRight = value;
                return true;
            case MARGIN_TOP_PROPERTY_KEY:
                mMarginTop = value;
                return true;
            case MARGIN_BOTTOM_PROPERTY_KEY:
                mMarginBottom = value;
                return true;
            case PADDING_LEFT_PROPERTY_KEY:
                mPaddingLeft = value;
                return true;
            case PADDING_RIGHT_PROPERTY_KEY:
                mPaddingRight = value;
                return true;
            case PADDING_TOP_PROPERTY_KEY:
                mPaddingTop = value;
                return true;
            case PADDING_BOTTOM_PROPERTY_KEY:
                mPaddingBottom = value;
                return true;
            case POSITION_LEFT_PROPERTY_KEY:
                mPositionLeft = value;
                return true;
            case POSITION_RIGHT_PROPERTY_KEY:
                mPositionRight = value;
                return true;
            case POSITION_TOP_PROPERTY_KEY:
                mPositionTop = value;
                return true;
            case POSITION_BOTTOM_PROPERTY_KEY:
                mPositionBottom = value;
                return true;
            case FLEX_BASIS_PROPERTY_KEY:
                mFlexBasis = value;
                return true;
            case ASPECT_RATIO_PROPERTY_KEY:
                mAspectRatio = value;
                return true;
            case INTERPOLATION_TIME_PROPERTY_KEY:
                mInterpolationTime = value;
                return true;
            case CORNER_RADIUS_TL_PROPERTY_KEY:
                mCornerRadiusTL = value;
                return true;
            case CORNER_RADIUS_TR_PROPERTY_KEY:
                mCornerRadiusTR = value;
                return true;
            case CORNER_RADIUS_BL_PROPERTY_KEY:
                mCornerRadiusBL = value;
                return true;
            case CORNER_RADIUS_BR_PROPERTY_KEY:
                mCornerRadiusBR = value;
                return true;
            default:
                return super.deserializeDouble(propertyKey, value);
        }
    }

    @Override
    public boolean setDoubleProperty(int propertyKey, float value) {
        switch (propertyKey) {
            case GAP_HORIZONTAL_PROPERTY_KEY:
                setGapHorizontal(value);
                return true;
            case GAP_VERTICAL_PROPERTY_KEY:
                setGapVertical(value);
                return true;
            case BORDER_LEFT_PROPERTY_KEY:
                setBorderLeft(value);
                return true;
            case BORDER_RIGHT_PROPERTY_KEY:
                setBorderRight(value);
                return true;
            case BORDER_TOP_PROPERTY_KEY:
                setBorderTop(value);
                return true;
            case BORDER_BOTTOM_PROPERTY_KEY:
                setBorderBottom(value);
                return true;
            case MARGIN_LEFT_PROPERTY_KEY:
                setMarginLeft(value);
                return true;
            case MARGIN_RIGHT_PROPERTY_KEY:
                setMarginRight(value);
                return true;
            case MARGIN_TOP_PROPERTY_KEY:
                setMarginTop(value);
                return true;
            case MARGIN_BOTTOM_PROPERTY_KEY:
                setMarginBottom(value);
                return true;
            case PADDING_LEFT_PROPERTY_KEY:
                setPaddingLeft(value);
                return true;
            case PADDING_RIGHT_PROPERTY_KEY:
                setPaddingRight(value);
                return true;
            case PADDING_TOP_PROPERTY_KEY:
                setPaddingTop(value);
                return true;
            case PADDING_BOTTOM_PROPERTY_KEY:
                setPaddingBottom(value);
                return true;
            case POSITION_LEFT_PROPERTY_KEY:
                setPositionLeft(value);
                return true;
            case POSITION_RIGHT_PROPERTY_KEY:
                setPositionRight(value);
                return true;
            case POSITION_TOP_PROPERTY_KEY:
                setPositionTop(value);
                return true;
            case POSITION_BOTTOM_PROPERTY_KEY:
                setPositionBottom(value);
                return true;
            case FLEX_BASIS_PROPERTY_KEY:
                setFlexBasis(value);
                return true;
            case ASPECT_RATIO_PROPERTY_KEY:
                setAspectRatio(value);
                return true;
            case INTERPOLATION_TIME_PROPERTY_KEY:
                setInterpolationTime(value);
                return true;
            case CORNER_RADIUS_TL_PROPERTY_KEY:
                setCornerRadiusTL(value);
                return true;
            case CORNER_RADIUS_TR_PROPERTY_KEY:
                setCornerRadiusTR(value);
                return true;
            case CORNER_RADIUS_BL_PROPERTY_KEY:
                setCornerRadiusBL(value);
                return true;
            case CORNER_RADIUS_BR_PROPERTY_KEY:
                setCornerRadiusBR(value);
                return true;
            default:
                return super.setDoubleProperty(propertyKey, value);
        }
    }

    @Override
    public float getDoubleProperty(int propertyKey) {
        switch (propertyKey) {
            case GAP_HORIZONTAL_PROPERTY_KEY:
                return mGapHorizontal;
            case GAP_VERTICAL_PROPERTY_KEY:
                return mGapVertical;
            case BORDER_LEFT_PROPERTY_KEY:
                return mBorderLeft;
            case BORDER_RIGHT_PROPERTY_KEY:
                return mBorderRight;
            case BORDER_TOP_PROPERTY_KEY:
                return mBorderTop;
            case BORDER_BOTTOM_PROPERTY_KEY:
                return mBorderBottom;
            case MARGIN_LEFT_PROPERTY_KEY:
                return mMarginLeft;
            case MARGIN_RIGHT_PROPERTY_KEY:
                return mMarginRight;
            case MARGIN_TOP_PROPERTY_KEY:
                return mMarginTop;
            case MARGIN_BOTTOM_PROPERTY_KEY:
                return mMarginBottom;
            case PADDING_LEFT_PROPERTY_KEY:
                return mPaddingLeft;
            case PADDING_RIGHT_PROPERTY_KEY:
                return mPaddingRight;
            case PADDING_TOP_PROPERTY_KEY:
                return mPaddingTop;
            case PADDING_BOTTOM_PROPERTY_KEY:
                return mPaddingBottom;
            case POSITION_LEFT_PROPERTY_KEY:
                return mPositionLeft;
            case POSITION_RIGHT_PROPERTY_KEY:
                return mPositionRight;
            case POSITION_TOP_PROPERTY_KEY:
                return mPositionTop;
            case POSITION_BOTTOM_PROPERTY_KEY:
                return mPositionBottom;
            case FLEX_BASIS_PROPERTY_KEY:
                return mFlexBasis;
            case ASPECT_RATIO_PROPERTY_KEY:
                return mAspectRatio;
            case INTERPOLATION_TIME_PROPERTY_KEY:
                return mInterpolationTime;
            case CORNER_RADIUS_TL_PROPERTY_KEY:
                return mCornerRadiusTL;
            case CORNER_RADIUS_TR_PROPERTY_KEY:
                return mCornerRadiusTR;
            case CORNER_RADIUS_BL_PROPERTY_KEY:
                return mCornerRadiusBL;
            case CORNER_RADIUS_BR_PROPERTY_KEY:
                return mCornerRadiusBR;
            default:
                return super.getDoubleProperty(propertyKey);
        }
    }

    @Override
    public boolean deserializeUint(int propertyKey, int value) {
        switch (propertyKey) {
            case POSITION_LEFT_UNITS_VALUE_PROPERTY_KEY:
                mPositionLeftUnitsValue = value;
                return true;
            case POSITION_RIGHT_UNITS_VALUE_PROPERTY_KEY:
                mPositionRightUnitsValue = value;
                return true;
            case POSITION_TOP_UNITS_VALUE_PROPERTY_KEY:
                mPositionTopUnitsValue = value;
                return true;
            case POSITION_BOTTOM_UNITS_VALUE_PROPERTY_KEY:
                mPositionBottomUnitsValue = value;
                return true;
            case ASPECT_RATIO_AXIS_VALUE_PROPERTY_KEY:
                mAspectRatioAxisValue = value;
                return true;
            case INTERPOLATOR_ID_PROPERTY_KEY:
                mInterpolatorId = value;
                return true;
            case FLEX_BASIS_UNITS_VALUE_PROPERTY_KEY:
                mFlexBasisUnitsValue = value;
                return true;
            case LAYOUT_ALIGNMENT_TYPE_PROPERTY_KEY:
                mLayoutAlignmentType = value;
                return true;
            case ANIMATION_STYLE_TYPE_PROPERTY_KEY:
                mAnimationStyleType = value;
                return true;
            case INTERPOLATION_TYPE_PROPERTY_KEY:
                mInterpolationType = value;
                return true;
            case POSITION_TYPE_VALUE_PROPERTY_KEY:
                mPositionTypeValue = value;
                return true;
            case FLEX_DIRECTION_VALUE_PROPERTY_KEY:
                mFlexDirectionValue = value;
                return true;
            case DIRECTION_VALUE_PROPERTY_KEY:
                mDirectionValue = value;
                return true;
            case FLEX_WRAP_VALUE_PROPERTY_KEY:
                mFlexWrapValue = value;
                return true;
            case OVERFLOW_VALUE_PROPERTY_KEY:
                mOverflowValue = value;
                return true;
            case BORDER_LEFT_UNITS_VALUE_PROPERTY_KEY:
                mBorderLeftUnitsValue = value;
                return true;
            case BORDER_RIGHT_UNITS_VALUE_PROPERTY_KEY:
                mBorderRightUnitsValue = value;
                return true;
            case BORDER_TOP_UNITS_VALUE_PROPERTY_KEY:
                mBorderTopUnitsValue = value;
                return true;
            case BORDER_BOTTOM_UNITS_VALUE_PROPERTY_KEY:
                mBorderBottomUnitsValue = value;
                return true;
            case MARGIN_LEFT_UNITS_VALUE_PROPERTY_KEY:
                mMarginLeftUnitsValue = value;
                return true;
            case MARGIN_RIGHT_UNITS_VALUE_PROPERTY_KEY:
                mMarginRightUnitsValue = value;
                return true;
            case MARGIN_TOP_UNITS_VALUE_PROPERTY_KEY:
                mMarginTopUnitsValue = value;
                return true;
            case MARGIN_BOTTOM_UNITS_VALUE_PROPERTY_KEY:
                mMarginBottomUnitsValue = value;
                return true;
            case PADDING_LEFT_UNITS_VALUE_PROPERTY_KEY:
                mPaddingLeftUnitsValue = value;
                return true;
            case PADDING_RIGHT_UNITS_VALUE_PROPERTY_KEY:
                mPaddingRightUnitsValue = value;
                return true;
            case PADDING_TOP_UNITS_VALUE_PROPERTY_KEY:
                mPaddingTopUnitsValue = value;
                return true;
            case PADDING_BOTTOM_UNITS_VALUE_PROPERTY_KEY:
                mPaddingBottomUnitsValue = value;
                return true;
            case GAP_HORIZONTAL_UNITS_VALUE_PROPERTY_KEY:
                mGapHorizontalUnitsValue = value;
                return true;
            case GAP_VERTICAL_UNITS_VALUE_PROPERTY_KEY:
                mGapVerticalUnitsValue = value;
                return true;
            case JUSTIFY_ITEMS_VALUE_PROPERTY_KEY:
                mJustifyItemsValue = value;
                return true;
            case LAYOUT_TYPE_VALUE_PROPERTY_KEY:
                mLayoutTypeValue = value;
                return true;
            default:
                return super.deserializeUint(propertyKey, value);
        }
    }

    @Override
    public boolean setUintProperty(int propertyKey, int value) {
        switch (propertyKey) {
            case POSITION_LEFT_UNITS_VALUE_PROPERTY_KEY:
                setPositionLeftUnitsValue(value);
                return true;
            case POSITION_RIGHT_UNITS_VALUE_PROPERTY_KEY:
                setPositionRightUnitsValue(value);
                return true;
            case POSITION_TOP_UNITS_VALUE_PROPERTY_KEY:
                setPositionTopUnitsValue(value);
                return true;
            case POSITION_BOTTOM_UNITS_VALUE_PROPERTY_KEY:
                setPositionBottomUnitsValue(value);
                return true;
            case ASPECT_RATIO_AXIS_VALUE_PROPERTY_KEY:
                setAspectRatioAxisValue(value);
                return true;
            case INTERPOLATOR_ID_PROPERTY_KEY:
                setInterpolatorId(value);
                return true;
            case FLEX_BASIS_UNITS_VALUE_PROPERTY_KEY:
                setFlexBasisUnitsValue(value);
                return true;
            case LAYOUT_ALIGNMENT_TYPE_PROPERTY_KEY:
                setLayoutAlignmentType(value);
                return true;
            case ANIMATION_STYLE_TYPE_PROPERTY_KEY:
                setAnimationStyleType(value);
                return true;
            case INTERPOLATION_TYPE_PROPERTY_KEY:
                setInterpolationType(value);
                return true;
            case POSITION_TYPE_VALUE_PROPERTY_KEY:
                setPositionTypeValue(value);
                return true;
            case FLEX_DIRECTION_VALUE_PROPERTY_KEY:
                setFlexDirectionValue(value);
                return true;
            case DIRECTION_VALUE_PROPERTY_KEY:
                setDirectionValue(value);
                return true;
            case FLEX_WRAP_VALUE_PROPERTY_KEY:
                setFlexWrapValue(value);
                return true;
            case OVERFLOW_VALUE_PROPERTY_KEY:
                setOverflowValue(value);
                return true;
            case BORDER_LEFT_UNITS_VALUE_PROPERTY_KEY:
                setBorderLeftUnitsValue(value);
                return true;
            case BORDER_RIGHT_UNITS_VALUE_PROPERTY_KEY:
                setBorderRightUnitsValue(value);
                return true;
            case BORDER_TOP_UNITS_VALUE_PROPERTY_KEY:
                setBorderTopUnitsValue(value);
                return true;
            case BORDER_BOTTOM_UNITS_VALUE_PROPERTY_KEY:
                setBorderBottomUnitsValue(value);
                return true;
            case MARGIN_LEFT_UNITS_VALUE_PROPERTY_KEY:
                setMarginLeftUnitsValue(value);
                return true;
            case MARGIN_RIGHT_UNITS_VALUE_PROPERTY_KEY:
                setMarginRightUnitsValue(value);
                return true;
            case MARGIN_TOP_UNITS_VALUE_PROPERTY_KEY:
                setMarginTopUnitsValue(value);
                return true;
            case MARGIN_BOTTOM_UNITS_VALUE_PROPERTY_KEY:
                setMarginBottomUnitsValue(value);
                return true;
            case PADDING_LEFT_UNITS_VALUE_PROPERTY_KEY:
                setPaddingLeftUnitsValue(value);
                return true;
            case PADDING_RIGHT_UNITS_VALUE_PROPERTY_KEY:
                setPaddingRightUnitsValue(value);
                return true;
            case PADDING_TOP_UNITS_VALUE_PROPERTY_KEY:
                setPaddingTopUnitsValue(value);
                return true;
            case PADDING_BOTTOM_UNITS_VALUE_PROPERTY_KEY:
                setPaddingBottomUnitsValue(value);
                return true;
            case GAP_HORIZONTAL_UNITS_VALUE_PROPERTY_KEY:
                setGapHorizontalUnitsValue(value);
                return true;
            case GAP_VERTICAL_UNITS_VALUE_PROPERTY_KEY:
                setGapVerticalUnitsValue(value);
                return true;
            case JUSTIFY_ITEMS_VALUE_PROPERTY_KEY:
                setJustifyItemsValue(value);
                return true;
            case LAYOUT_TYPE_VALUE_PROPERTY_KEY:
                setLayoutTypeValue(value);
                return true;
            default:
                return super.setUintProperty(propertyKey, value);
        }
    }

    @Override
    public int getUintProperty(int propertyKey) {
        switch (propertyKey) {
            case POSITION_LEFT_UNITS_VALUE_PROPERTY_KEY:
                return mPositionLeftUnitsValue;
            case POSITION_RIGHT_UNITS_VALUE_PROPERTY_KEY:
                return mPositionRightUnitsValue;
            case POSITION_TOP_UNITS_VALUE_PROPERTY_KEY:
                return mPositionTopUnitsValue;
            case POSITION_BOTTOM_UNITS_VALUE_PROPERTY_KEY:
                return mPositionBottomUnitsValue;
            case ASPECT_RATIO_AXIS_VALUE_PROPERTY_KEY:
                return mAspectRatioAxisValue;
            case INTERPOLATOR_ID_PROPERTY_KEY:
                return mInterpolatorId;
            case FLEX_BASIS_UNITS_VALUE_PROPERTY_KEY:
                return mFlexBasisUnitsValue;
            case LAYOUT_ALIGNMENT_TYPE_PROPERTY_KEY:
                return mLayoutAlignmentType;
            case ANIMATION_STYLE_TYPE_PROPERTY_KEY:
                return mAnimationStyleType;
            case INTERPOLATION_TYPE_PROPERTY_KEY:
                return mInterpolationType;
            case POSITION_TYPE_VALUE_PROPERTY_KEY:
                return mPositionTypeValue;
            case FLEX_DIRECTION_VALUE_PROPERTY_KEY:
                return mFlexDirectionValue;
            case DIRECTION_VALUE_PROPERTY_KEY:
                return mDirectionValue;
            case FLEX_WRAP_VALUE_PROPERTY_KEY:
                return mFlexWrapValue;
            case OVERFLOW_VALUE_PROPERTY_KEY:
                return mOverflowValue;
            case BORDER_LEFT_UNITS_VALUE_PROPERTY_KEY:
                return mBorderLeftUnitsValue;
            case BORDER_RIGHT_UNITS_VALUE_PROPERTY_KEY:
                return mBorderRightUnitsValue;
            case BORDER_TOP_UNITS_VALUE_PROPERTY_KEY:
                return mBorderTopUnitsValue;
            case BORDER_BOTTOM_UNITS_VALUE_PROPERTY_KEY:
                return mBorderBottomUnitsValue;
            case MARGIN_LEFT_UNITS_VALUE_PROPERTY_KEY:
                return mMarginLeftUnitsValue;
            case MARGIN_RIGHT_UNITS_VALUE_PROPERTY_KEY:
                return mMarginRightUnitsValue;
            case MARGIN_TOP_UNITS_VALUE_PROPERTY_KEY:
                return mMarginTopUnitsValue;
            case MARGIN_BOTTOM_UNITS_VALUE_PROPERTY_KEY:
                return mMarginBottomUnitsValue;
            case PADDING_LEFT_UNITS_VALUE_PROPERTY_KEY:
                return mPaddingLeftUnitsValue;
            case PADDING_RIGHT_UNITS_VALUE_PROPERTY_KEY:
                return mPaddingRightUnitsValue;
            case PADDING_TOP_UNITS_VALUE_PROPERTY_KEY:
                return mPaddingTopUnitsValue;
            case PADDING_BOTTOM_UNITS_VALUE_PROPERTY_KEY:
                return mPaddingBottomUnitsValue;
            case GAP_HORIZONTAL_UNITS_VALUE_PROPERTY_KEY:
                return mGapHorizontalUnitsValue;
            case GAP_VERTICAL_UNITS_VALUE_PROPERTY_KEY:
                return mGapVerticalUnitsValue;
            case JUSTIFY_ITEMS_VALUE_PROPERTY_KEY:
                return mJustifyItemsValue;
            case LAYOUT_TYPE_VALUE_PROPERTY_KEY:
                return mLayoutTypeValue;
            default:
                return super.getUintProperty(propertyKey);
        }
    }
}
