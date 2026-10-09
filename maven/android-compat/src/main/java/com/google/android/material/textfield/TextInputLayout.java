/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.google.android.material.textfield;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.internal.MaterialAttrs;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

/// A Material text field: wraps an `EditText` in a filled or outlined box
/// whose hint floats up into a label when the field is focused or has text,
/// with helper or error text and a character counter below it and optional
/// start and end icons (a custom icon, the password toggle, clear text or a
/// dropdown arrow).
///
/// The layout draws the box and the label itself; the field's own hint and
/// background are cleared, as Material Components does.
public class TextInputLayout extends LinearLayout {

    public static final int BOX_BACKGROUND_NONE = 0;
    public static final int BOX_BACKGROUND_FILLED = 1;
    public static final int BOX_BACKGROUND_OUTLINE = 2;
    public static final int END_ICON_CUSTOM = -1;
    public static final int END_ICON_NONE = 0;
    public static final int END_ICON_PASSWORD_TOGGLE = 1;
    public static final int END_ICON_CLEAR_TEXT = 2;
    public static final int END_ICON_DROPDOWN_MENU = 3;

    private static final long LABEL_ANIMATION_MS = 167;

    /// Called when an `EditText` is placed in the layout.
    public interface OnEditTextAttachedListener {
        void onEditTextAttached(TextInputLayout textInputLayout);
    }

    private final FrameLayout mInputFrame;
    private final LinearLayout mIndicatorArea;
    private final TextView mHelperView;
    private final TextView mCounterView;
    private final ImageView mStartIconView;
    private final ImageView mEndIconView;
    private final MaterialShapeDrawable mBox;
    private final Paint mLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private EditText mEditText;
    private CharSequence mHint;
    private boolean mHintEnabled = true;
    private boolean mHintAnimationEnabled = true;
    private int mBoxMode;
    private ColorStateList mBoxStrokeColor;
    private int mBoxStrokeErrorColor;
    private int mBoxStrokeWidth;
    private int mBoxStrokeWidthFocused;
    private ColorStateList mDefaultHintColor;
    private ColorStateList mFocusedHintColor;
    private int mErrorColor;
    private ColorStateList mHelperColor;
    private ColorStateList mCounterColor;
    private ColorStateList mCounterOverflowColor;
    private CharSequence mHelperText;
    private boolean mHelperEnabled;
    private CharSequence mError;
    private boolean mErrorEnabled;
    private Drawable mErrorIcon;
    private ColorStateList mErrorIconTint;
    private boolean mCounterEnabled;
    private int mCounterMaxLength = -1;
    private int mEndIconMode = END_ICON_NONE;
    private Drawable mEndIcon;
    private ColorStateList mEndIconTint;
    private View.OnClickListener mEndIconClick;
    private Drawable mStartIcon;
    private ColorStateList mStartIconTint;
    private CharSequence mPlaceholder;
    private CharSequence mPrefix;
    private CharSequence mSuffix;
    private boolean mPasswordVisible;
    private float mCollapse;
    private ValueAnimator mAnimator;
    private final java.util.List<OnEditTextAttachedListener> mAttachedListeners =
            new java.util.ArrayList<OnEditTextAttachedListener>();

    public TextInputLayout(Context context) {
        this(context, null);
    }

    public TextInputLayout(Context context, AttributeSet attrs) {
        this(context, attrs, MaterialAttrs.defStyleAttr(context, R.attr.textInputStyle));
    }

    public TextInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        setWillNotDraw(false);
        setAddStatesFromChildren(true);
        mInputFrame = new FrameLayout(context);
        mInputFrame.setAddStatesFromChildren(true);
        super.addView(mInputFrame, -1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        mStartIconView = iconView(context, Gravity.START | Gravity.CENTER_VERTICAL);
        mEndIconView = iconView(context, Gravity.END | Gravity.CENTER_VERTICAL);
        mIndicatorArea = new LinearLayout(context);
        mIndicatorArea.setOrientation(HORIZONTAL);
        int pad = MaterialAttrs.dpi(context, 16);
        mIndicatorArea.setPadding(pad, MaterialAttrs.dpi(context, 4), pad, 0);
        mHelperView = new TextView(context);
        mHelperView.setTextSize(12);
        mCounterView = new TextView(context);
        mCounterView.setTextSize(12);
        mIndicatorArea.addView(mHelperView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        mIndicatorArea.addView(mCounterView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        super.addView(mIndicatorArea, -1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        mIndicatorArea.setVisibility(View.GONE);

        int defStyleRes = MaterialAttrs.isMaterial3(context) ? R.style.Widget_Material3_TextInputLayout_OutlinedBox
                : R.style.Widget_MaterialComponents_TextInputLayout_FilledBox;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TextInputLayout, defStyleAttr,
                defStyleAttr == 0 ? defStyleRes : 0);
        try {
            mHintEnabled = a.getBoolean(R.styleable.TextInputLayout_hintEnabled, true);
            mHintAnimationEnabled = a.getBoolean(R.styleable.TextInputLayout_hintAnimationEnabled, true);
            mHint = a.getText(R.styleable.TextInputLayout_android_hint);
            int onSurfaceVariant = MaterialColors.getColor(context, R.attr.colorOnSurfaceVariant, 0x99000000);
            int primary = MaterialColors.getColor(context, R.attr.colorPrimary, 0xff6750a4);
            mErrorColor = MaterialColors.getColor(context, R.attr.colorError, 0xffb3261e);
            mDefaultHintColor = colorOr(a, R.styleable.TextInputLayout_android_textColorHint, onSurfaceVariant);
            mFocusedHintColor = colorOr(a, R.styleable.TextInputLayout_hintTextColor, primary);
            mBoxMode = a.getInt(R.styleable.TextInputLayout_boxBackgroundMode, BOX_BACKGROUND_NONE);
            mBoxStrokeColor = colorOr(a, R.styleable.TextInputLayout_boxStrokeColor, onSurfaceVariant);
            ColorStateList errStroke = a.getColorStateList(R.styleable.TextInputLayout_boxStrokeErrorColor);
            mBoxStrokeErrorColor = errStroke == null ? mErrorColor : errStroke.getDefaultColor();
            mBoxStrokeWidth = a.getDimensionPixelSize(R.styleable.TextInputLayout_boxStrokeWidth,
                    MaterialAttrs.dpi(context, 1));
            mBoxStrokeWidthFocused = a.getDimensionPixelSize(R.styleable.TextInputLayout_boxStrokeWidthFocused,
                    MaterialAttrs.dpi(context, 2));
            float corner = MaterialAttrs.dp(context, 4);
            ShapeAppearanceModel shape = ShapeAppearanceModel.builder()
                    .setTopLeftCornerSize(a.getDimension(R.styleable.TextInputLayout_boxCornerRadiusTopStart, corner))
                    .setTopRightCornerSize(a.getDimension(R.styleable.TextInputLayout_boxCornerRadiusTopEnd, corner))
                    .setBottomRightCornerSize(a.getDimension(R.styleable.TextInputLayout_boxCornerRadiusBottomEnd,
                            mBoxMode == BOX_BACKGROUND_FILLED ? 0f : corner))
                    .setBottomLeftCornerSize(a.getDimension(R.styleable.TextInputLayout_boxCornerRadiusBottomStart,
                            mBoxMode == BOX_BACKGROUND_FILLED ? 0f : corner))
                    .build();
            mBox = new MaterialShapeDrawable(shape);
            mBox.setFillColor(a.getColorStateList(R.styleable.TextInputLayout_boxBackgroundColor));
            mHelperColor = colorOr(a, R.styleable.TextInputLayout_helperTextTextColor, onSurfaceVariant);
            mCounterColor = colorOr(a, R.styleable.TextInputLayout_counterTextColor, onSurfaceVariant);
            mCounterOverflowColor = colorOr(a, R.styleable.TextInputLayout_counterOverflowTextColor, mErrorColor);
            ColorStateList errText = a.getColorStateList(R.styleable.TextInputLayout_errorTextColor);
            if (errText != null) {
                mErrorColor = errText.getDefaultColor();
            }
            mHelperText = a.getText(R.styleable.TextInputLayout_helperText);
            mHelperEnabled = a.getBoolean(R.styleable.TextInputLayout_helperTextEnabled, mHelperText != null);
            mErrorEnabled = a.getBoolean(R.styleable.TextInputLayout_errorEnabled, false);
            mErrorIcon = a.getDrawable(R.styleable.TextInputLayout_errorIconDrawable);
            if (mErrorIcon == null) {
                mErrorIcon = context.getDrawable(R.drawable.mtrl_ic_error);
            }
            mErrorIconTint = colorOr(a, R.styleable.TextInputLayout_errorIconTint, mErrorColor);
            mCounterEnabled = a.getBoolean(R.styleable.TextInputLayout_counterEnabled, false);
            mCounterMaxLength = a.getInt(R.styleable.TextInputLayout_counterMaxLength, -1);
            mStartIcon = a.getDrawable(R.styleable.TextInputLayout_startIconDrawable);
            mStartIconTint = colorOr(a, R.styleable.TextInputLayout_startIconTint, onSurfaceVariant);
            mEndIconTint = colorOr(a, R.styleable.TextInputLayout_endIconTint, onSurfaceVariant);
            int endMode = a.getInt(R.styleable.TextInputLayout_endIconMode, END_ICON_NONE);
            if (a.getBoolean(R.styleable.TextInputLayout_passwordToggleEnabled, false)) {
                endMode = END_ICON_PASSWORD_TOGGLE;
            }
            mEndIcon = a.getDrawable(R.styleable.TextInputLayout_endIconDrawable);
            mPlaceholder = a.getText(R.styleable.TextInputLayout_placeholderText);
            mPrefix = a.getText(R.styleable.TextInputLayout_prefixText);
            mSuffix = a.getText(R.styleable.TextInputLayout_suffixText);
            setEndIconMode(endMode);
        } finally {
            a.recycle();
        }
        if (mPrefix != null || mSuffix != null) {
            com.codename1.androidcompat.runtime.CompatReport.unsupported("TextInputLayout", "prefixText and suffixText");
        }
        updateStartIcon();
        updateIndicators();
    }

    private static ColorStateList colorOr(TypedArray a, int index, int fallback) {
        ColorStateList c = a.getColorStateList(index);
        return c != null ? c : ColorStateList.valueOf(fallback);
    }

    private ImageView iconView(Context context, int gravity) {
        ImageView v = new ImageView(context);
        int pad = MaterialAttrs.dpi(context, 12);
        v.setPadding(pad, pad, pad, pad);
        v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        v.setVisibility(View.GONE);
        int size = MaterialAttrs.dpi(context, 48);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size, gravity);
        int margin = MaterialAttrs.dpi(context, 4) - pad + MaterialAttrs.dpi(context, 8);
        lp.setMarginStart(Math.max(0, margin));
        lp.setMarginEnd(Math.max(0, margin));
        addToFrame(v, lp);
        return v;
    }

    private void addToFrame(View v, FrameLayout.LayoutParams lp) {
        mInputFrame.addView(v, lp);
    }

    // ------------------------------------------------------------ the field

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (child instanceof EditText) {
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL);
            mInputFrame.addView(child, 0, lp);
            setEditText((EditText) child);
            return;
        }
        super.addView(child, index, params);
    }

    private void setEditText(EditText editText) {
        if (mEditText != null) {
            throw new IllegalArgumentException("We already have an EditText, can only have one");
        }
        mEditText = editText;
        if (mHintEnabled) {
            if (mHint == null) {
                mHint = editText.getHint();
            }
            editText.setHint(null);
        }
        if (mBoxMode != BOX_BACKGROUND_NONE) {
            editText.setBackground(null);
        }
        applyFieldPadding();
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateCounter();
                onEditTextStateChanged(true);
            }
        });
        if (!(editText instanceof TextInputEditText)) {
            final View.OnFocusChangeListener previous = editText.getOnFocusChangeListener();
            editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (previous != null) {
                        previous.onFocusChange(v, hasFocus);
                    }
                    onEditTextStateChanged(true);
                }
            });
        }
        mCollapse = shouldCollapse() ? 1f : 0f;
        updateCounter();
        updateEndIcon();
        for (OnEditTextAttachedListener l : mAttachedListeners) {
            l.onEditTextAttached(this);
        }
    }

    /// The field's padding in each box mode, as the Material text field
    /// styles give it: 16dp around in an outlined box, the text pushed down
    /// under the label in a filled one, and room for the icons.
    private void applyFieldPadding() {
        if (mEditText == null) {
            return;
        }
        Context c = getContext();
        int start = MaterialAttrs.dpi(c, mStartIcon != null ? 52 : 16);
        int end = MaterialAttrs.dpi(c, mEndIconView.getVisibility() == View.VISIBLE || errorShowing() ? 52 : 16);
        int top;
        int bottom;
        if (mBoxMode == BOX_BACKGROUND_FILLED) {
            top = MaterialAttrs.dpi(c, mHintEnabled && mHint != null ? 24 : 16);
            bottom = MaterialAttrs.dpi(c, mHintEnabled && mHint != null ? 8 : 16);
        } else if (mBoxMode == BOX_BACKGROUND_OUTLINE) {
            top = MaterialAttrs.dpi(c, 16);
            bottom = top;
        } else {
            top = mEditText.getPaddingTop();
            bottom = mEditText.getPaddingBottom();
            start = mStartIcon != null ? start : mEditText.getPaddingStart();
        }
        mEditText.setPaddingRelative(start, top, end, bottom);
    }

    public EditText getEditText() {
        return mEditText;
    }

    public void addOnEditTextAttachedListener(OnEditTextAttachedListener listener) {
        mAttachedListeners.add(listener);
        if (mEditText != null) {
            listener.onEditTextAttached(this);
        }
    }

    public void removeOnEditTextAttachedListener(OnEditTextAttachedListener listener) {
        mAttachedListeners.remove(listener);
    }

    // ------------------------------------------------------------ label state

    private boolean fieldFocused() {
        return mEditText != null && mEditText.isFocused();
    }

    private boolean shouldCollapse() {
        if (mEditText == null) {
            return false;
        }
        return fieldFocused() || mEditText.getText().length() > 0 || mPrefix != null;
    }

    /// Re-reads the field's focus and text and animates the label to match.
    void onEditTextStateChanged(boolean animate) {
        float target = shouldCollapse() ? 1f : 0f;
        if (mEditText != null && mPlaceholder != null) {
            boolean show = fieldFocused() && mEditText.getText().length() == 0;
            CharSequence want = show ? mPlaceholder : null;
            CharSequence have = mEditText.getHint();
            if (mEditText instanceof TextInputEditText) {
                have = null;
            }
            if (want != have) {
                mEditText.setHint(want);
            }
        }
        updateEndIcon();
        if (Float.compare(target, mCollapse) != 0 && (mAnimator == null || !mAnimator.isRunning())) {
            if (animate && mHintAnimationEnabled && isAttachedToWindow()) {
                mAnimator = ValueAnimator.ofFloat(mCollapse, target);
                mAnimator.setDuration(LABEL_ANIMATION_MS);
                mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        mCollapse = ((Float) animation.getAnimatedValue()).floatValue();
                        invalidate();
                    }
                });
                mAnimator.start();
            } else {
                mCollapse = target;
            }
        }
        invalidate();
    }

    // ------------------------------------------------------------ measure and draw

    private float collapsedTextSize() {
        return MaterialAttrs.dp(getContext(), 12) * getResources().getDisplayMetrics().scaledDensity
                / getResources().getDisplayMetrics().density;
    }

    private float expandedTextSize() {
        return mEditText != null ? mEditText.getTextSize() : collapsedTextSize() * 4f / 3f;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mInputFrame.getLayoutParams();
        int top = 0;
        if (mBoxMode == BOX_BACKGROUND_OUTLINE && mHintEnabled && mHint != null) {
            mLabelPaint.setTextSize(collapsedTextSize());
            Paint.FontMetrics fm = mLabelPaint.getFontMetrics();
            top = Math.round((fm.descent - fm.ascent) / 2f);
        }
        if (lp.topMargin != top) {
            lp.topMargin = top;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private boolean errorShowing() {
        return mErrorEnabled && mError != null && mError.length() > 0;
    }

    private boolean counterOverflowed() {
        return mCounterEnabled && mCounterMaxLength > 0 && mEditText != null
                && mEditText.getText().length() > mCounterMaxLength;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBoxMode == BOX_BACKGROUND_NONE) {
            return;
        }
        int[] state = getDrawableState();
        boolean focused = fieldFocused();
        boolean error = errorShowing() || counterOverflowed();
        Rect f = new Rect(mInputFrame.getLeft(), mInputFrame.getTop(), mInputFrame.getRight(), mInputFrame.getBottom());
        mBox.setState(state);
        mBox.setBounds(f);
        int strokeColor = error ? mBoxStrokeErrorColor
                : focused ? mFocusedHintColor.getColorForState(state, mFocusedHintColor.getDefaultColor())
                : mBoxStrokeColor.getColorForState(state, mBoxStrokeColor.getDefaultColor());
        int strokeWidth = focused || error ? mBoxStrokeWidthFocused : mBoxStrokeWidth;
        if (!isEnabled()) {
            strokeColor = MaterialColors.withAlpha(MaterialAttrs.onSurface(getContext()), 0.12f);
            strokeWidth = mBoxStrokeWidth;
        }
        if (mBoxMode == BOX_BACKGROUND_OUTLINE) {
            mBox.setStroke(strokeWidth, strokeColor);
            if (mHintEnabled && mHint != null && mCollapse > 0f) {
                mLabelPaint.setTextSize(collapsedTextSize());
                float labelWidth = mLabelPaint.measureText(mHint.toString()) * mCollapse;
                float start = collapsedLabelX() - f.left - MaterialAttrs.dp(getContext(), 4);
                mBox.setTopNotch(start, start + labelWidth + MaterialAttrs.dp(getContext(), 8) * mCollapse);
            } else {
                mBox.setTopNotch(0, 0);
            }
            mBox.draw(canvas);
        } else {
            mBox.setStrokeWidth(0);
            mBox.setTopNotch(0, 0);
            mBox.draw(canvas);
            Paint line = new Paint();
            line.setColor(strokeColor);
            canvas.drawRect(f.left, f.bottom - strokeWidth, f.right, f.bottom, line);
        }
    }

    private float collapsedLabelX() {
        return mInputFrame.getLeft() + MaterialAttrs.dp(getContext(), mBoxMode == BOX_BACKGROUND_NONE ? 4 : 16);
    }

    /// The floating label, above the field.
    @Override
    public void onDrawForeground(Canvas canvas) {
        super.onDrawForeground(canvas);
        if (!mHintEnabled || mHint == null || mEditText == null) {
            return;
        }
        float t = mCollapse;
        Rect f = new Rect(mInputFrame.getLeft(), mInputFrame.getTop(), mInputFrame.getRight(), mInputFrame.getBottom());
        float expandedSize = expandedTextSize();
        float collapsedSize = collapsedTextSize();
        float size = expandedSize + (collapsedSize - expandedSize) * t;
        float expandedX = f.left + mEditText.getLeft() + mEditText.getPaddingLeft();
        float collapsedX = mBoxMode == BOX_BACKGROUND_FILLED ? expandedX : collapsedLabelX();
        float x = expandedX + (collapsedX - expandedX) * t;
        float expandedCenter = mBoxMode == BOX_BACKGROUND_FILLED && mEditText.getHeight() > 0
                ? f.top + mEditText.getTop() + (mEditText.getHeight()) / 2f
                : f.top + f.height() / 2f;
        mLabelPaint.setTextSize(collapsedSize);
        Paint.FontMetrics cfm = mLabelPaint.getFontMetrics();
        float collapsedCenter;
        if (mBoxMode == BOX_BACKGROUND_OUTLINE) {
            collapsedCenter = f.top;
        } else if (mBoxMode == BOX_BACKGROUND_FILLED) {
            collapsedCenter = f.top + MaterialAttrs.dp(getContext(), 8) + (cfm.descent - cfm.ascent) / 2f;
        } else {
            collapsedCenter = f.top - (cfm.descent - cfm.ascent) / 2f;
        }
        float center = expandedCenter + (collapsedCenter - expandedCenter) * t;
        int[] state = getDrawableState();
        int color;
        if (!isEnabled()) {
            color = MaterialColors.withAlpha(MaterialAttrs.onSurface(getContext()), 0.38f);
        } else if (errorShowing() && t > 0f) {
            color = mErrorColor;
        } else if (fieldFocused()) {
            color = mFocusedHintColor.getColorForState(state, mFocusedHintColor.getDefaultColor());
        } else {
            color = mDefaultHintColor.getColorForState(state, mDefaultHintColor.getDefaultColor());
        }
        mLabelPaint.setTextSize(size);
        if (mEditText.getTypeface() != null) {
            mLabelPaint.setTypeface(mEditText.getTypeface());
        }
        mLabelPaint.setColor(color);
        Paint.FontMetrics fm = mLabelPaint.getFontMetrics();
        float baseline = center - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(mHint.toString(), x, baseline, mLabelPaint);
    }

    // ------------------------------------------------------------ hint

    public void setHint(CharSequence hint) {
        if (mHintEnabled) {
            mHint = hint;
            if (mEditText != null) {
                mEditText.setHint(null);
            }
            requestLayout();
            invalidate();
        } else if (mEditText != null) {
            mEditText.setHint(hint);
        }
    }

    public void setHint(int resId) {
        setHint(getResources().getText(resId));
    }

    public CharSequence getHint() {
        return mHintEnabled ? mHint : null;
    }

    public void setHintEnabled(boolean enabled) {
        if (enabled == mHintEnabled) {
            return;
        }
        mHintEnabled = enabled;
        if (!enabled && mEditText != null && mHint != null) {
            mEditText.setHint(mHint);
        }
        applyFieldPadding();
        requestLayout();
    }

    public boolean isHintEnabled() {
        return mHintEnabled;
    }

    public void setHintAnimationEnabled(boolean enabled) {
        mHintAnimationEnabled = enabled;
    }

    public boolean isHintAnimationEnabled() {
        return mHintAnimationEnabled;
    }

    public void setHintTextColor(ColorStateList color) {
        mFocusedHintColor = color;
        invalidate();
    }

    public ColorStateList getHintTextColor() {
        return mFocusedHintColor;
    }

    public void setDefaultHintTextColor(ColorStateList color) {
        mDefaultHintColor = color;
        invalidate();
    }

    public ColorStateList getDefaultHintTextColor() {
        return mDefaultHintColor;
    }

    public void setPlaceholderText(CharSequence placeholder) {
        mPlaceholder = placeholder;
        onEditTextStateChanged(false);
    }

    public CharSequence getPlaceholderText() {
        return mPlaceholder;
    }

    public void setPrefixText(CharSequence prefix) {
        mPrefix = prefix;
        onEditTextStateChanged(false);
    }

    public CharSequence getPrefixText() {
        return mPrefix;
    }

    public void setSuffixText(CharSequence suffix) {
        mSuffix = suffix;
    }

    public CharSequence getSuffixText() {
        return mSuffix;
    }

    // ------------------------------------------------------------ box

    public void setBoxBackgroundMode(int mode) {
        mBoxMode = mode;
        if (mode != BOX_BACKGROUND_NONE && mEditText != null) {
            mEditText.setBackground(null);
        }
        applyFieldPadding();
        requestLayout();
        invalidate();
    }

    public int getBoxBackgroundMode() {
        return mBoxMode;
    }

    public void setBoxStrokeColor(int color) {
        mBoxStrokeColor = ColorStateList.valueOf(color);
        invalidate();
    }

    public int getBoxStrokeColor() {
        return mBoxStrokeColor.getDefaultColor();
    }

    public void setBoxStrokeColorStateList(ColorStateList color) {
        mBoxStrokeColor = color;
        invalidate();
    }

    public void setBoxStrokeErrorColor(ColorStateList color) {
        mBoxStrokeErrorColor = color.getDefaultColor();
        invalidate();
    }

    public void setBoxStrokeWidth(int width) {
        mBoxStrokeWidth = width;
        invalidate();
    }

    public void setBoxStrokeWidthFocused(int width) {
        mBoxStrokeWidthFocused = width;
        invalidate();
    }

    public void setBoxBackgroundColor(int color) {
        mBox.setFillColor(ColorStateList.valueOf(color));
        invalidate();
    }

    public void setBoxBackgroundColorStateList(ColorStateList color) {
        mBox.setFillColor(color);
        invalidate();
    }

    public int getBoxBackgroundColor() {
        ColorStateList c = mBox.getFillColor();
        return c == null ? 0 : c.getDefaultColor();
    }

    public void setBoxCornerRadii(float topStart, float topEnd, float bottomStart, float bottomEnd) {
        mBox.setShapeAppearanceModel(ShapeAppearanceModel.builder().setTopLeftCornerSize(topStart)
                .setTopRightCornerSize(topEnd).setBottomLeftCornerSize(bottomStart)
                .setBottomRightCornerSize(bottomEnd).build());
        invalidate();
    }

    // ------------------------------------------------------------ helper, error, counter

    public void setHelperText(CharSequence helperText) {
        mHelperText = helperText;
        if (helperText != null && helperText.length() > 0) {
            mHelperEnabled = true;
        }
        updateIndicators();
    }

    public CharSequence getHelperText() {
        return mHelperEnabled ? mHelperText : null;
    }

    public void setHelperTextEnabled(boolean enabled) {
        mHelperEnabled = enabled;
        updateIndicators();
    }

    public boolean isHelperTextEnabled() {
        return mHelperEnabled;
    }

    public void setHelperTextColor(ColorStateList color) {
        mHelperColor = color;
        updateIndicators();
    }

    /// Shows `error` below the field, colors the box and label with the
    /// error color and shows the error icon; null clears it.
    public void setError(CharSequence error) {
        if (error != null && error.length() > 0 && !mErrorEnabled) {
            mErrorEnabled = true;
        }
        mError = error;
        updateIndicators();
        updateEndIcon();
        invalidate();
    }

    public CharSequence getError() {
        return mErrorEnabled ? mError : null;
    }

    public void setErrorEnabled(boolean enabled) {
        mErrorEnabled = enabled;
        if (!enabled) {
            mError = null;
        }
        updateIndicators();
        updateEndIcon();
        invalidate();
    }

    public boolean isErrorEnabled() {
        return mErrorEnabled;
    }

    public void setErrorTextColor(ColorStateList color) {
        mErrorColor = color.getDefaultColor();
        updateIndicators();
    }

    public void setErrorIconDrawable(Drawable icon) {
        mErrorIcon = icon;
        updateEndIcon();
    }

    public void setErrorIconDrawable(int resId) {
        setErrorIconDrawable(resId == 0 ? null : getContext().getDrawable(resId));
    }

    public void setCounterEnabled(boolean enabled) {
        mCounterEnabled = enabled;
        updateCounter();
    }

    public boolean isCounterEnabled() {
        return mCounterEnabled;
    }

    public void setCounterMaxLength(int maxLength) {
        mCounterMaxLength = maxLength;
        updateCounter();
    }

    public int getCounterMaxLength() {
        return mCounterMaxLength;
    }

    private void updateCounter() {
        if (!mCounterEnabled) {
            mCounterView.setVisibility(View.GONE);
            updateIndicators();
            return;
        }
        int length = mEditText == null ? 0 : mEditText.getText().length();
        mCounterView.setVisibility(View.VISIBLE);
        mCounterView.setText(mCounterMaxLength > 0 ? length + "/" + mCounterMaxLength : String.valueOf(length));
        ColorStateList c = counterOverflowed() ? mCounterOverflowColor : mCounterColor;
        mCounterView.setTextColor(c);
        updateIndicators();
        invalidate();
    }

    private void updateIndicators() {
        boolean error = errorShowing();
        CharSequence text = error ? mError : mHelperEnabled ? mHelperText : null;
        mHelperView.setText(text == null ? "" : text);
        if (error) {
            mHelperView.setTextColor(mErrorColor);
        } else if (mHelperColor != null) {
            mHelperView.setTextColor(mHelperColor);
        }
        boolean show = (text != null && text.length() > 0) || mCounterEnabled || mErrorEnabled;
        mIndicatorArea.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // ------------------------------------------------------------ icons

    public void setStartIconDrawable(Drawable icon) {
        mStartIcon = icon;
        updateStartIcon();
    }

    public void setStartIconDrawable(int resId) {
        setStartIconDrawable(resId == 0 ? null : getContext().getDrawable(resId));
    }

    public Drawable getStartIconDrawable() {
        return mStartIcon;
    }

    public void setStartIconOnClickListener(View.OnClickListener listener) {
        mStartIconView.setOnClickListener(listener);
    }

    public void setStartIconTintList(ColorStateList tint) {
        mStartIconTint = tint;
        updateStartIcon();
    }

    private void updateStartIcon() {
        if (mStartIcon == null) {
            mStartIconView.setVisibility(View.GONE);
        } else {
            mStartIconView.setImageDrawable(MaterialAttrs.tinted(mStartIcon, mStartIconTint));
            mStartIconView.setVisibility(View.VISIBLE);
        }
        applyFieldPadding();
    }

    public void setEndIconMode(int mode) {
        mEndIconMode = mode;
        if (mode == END_ICON_PASSWORD_TOGGLE) {
            mPasswordVisible = false;
        }
        updateEndIcon();
    }

    public int getEndIconMode() {
        return mEndIconMode;
    }

    public void setEndIconDrawable(Drawable icon) {
        mEndIcon = icon;
        updateEndIcon();
    }

    public void setEndIconDrawable(int resId) {
        setEndIconDrawable(resId == 0 ? null : getContext().getDrawable(resId));
    }

    public Drawable getEndIconDrawable() {
        return mEndIconView.getDrawable();
    }

    public void setEndIconOnClickListener(View.OnClickListener listener) {
        mEndIconClick = listener;
        updateEndIcon();
    }

    public void setEndIconTintList(ColorStateList tint) {
        mEndIconTint = tint;
        updateEndIcon();
    }

    public void setEndIconVisible(boolean visible) {
        mEndIconView.setVisibility(visible ? View.VISIBLE : View.GONE);
        applyFieldPadding();
    }

    public boolean isEndIconVisible() {
        return mEndIconView.getVisibility() == View.VISIBLE;
    }

    /// The end slot shows the error icon while there is an error, else the
    /// end icon of the mode: the password toggle's eye, the clear-text cross
    /// while the field has text and focus, the dropdown arrow, or a custom
    /// icon.
    private void updateEndIcon() {
        Drawable icon = null;
        ColorStateList tint = mEndIconTint;
        View.OnClickListener click = mEndIconClick;
        Context c = getContext();
        if (errorShowing() && mErrorIcon != null) {
            icon = mErrorIcon;
            tint = mErrorIconTint;
            click = null;
        } else if (mEndIconMode == END_ICON_PASSWORD_TOGGLE) {
            icon = mEndIcon != null ? mEndIcon : c.getDrawable(mPasswordVisible ? R.drawable.design_ic_visibility_off
                    : R.drawable.design_ic_visibility);
            if (click == null) {
                click = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        togglePassword();
                    }
                };
            }
        } else if (mEndIconMode == END_ICON_CLEAR_TEXT) {
            boolean show = mEditText != null && mEditText.getText().length() > 0 && fieldFocused();
            icon = show ? (mEndIcon != null ? mEndIcon : c.getDrawable(R.drawable.mtrl_ic_close)) : null;
            if (click == null) {
                click = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mEditText != null) {
                            mEditText.setText("");
                        }
                    }
                };
            }
        } else if (mEndIconMode == END_ICON_DROPDOWN_MENU) {
            icon = mEndIcon != null ? mEndIcon : c.getDrawable(R.drawable.mtrl_dropdown_arrow);
            if (click == null) {
                click = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mEditText instanceof AutoCompleteTextView) {
                            ((AutoCompleteTextView) mEditText).showDropDown();
                        }
                    }
                };
            }
        } else if (mEndIconMode == END_ICON_CUSTOM) {
            icon = mEndIcon;
        }
        boolean was = mEndIconView.getVisibility() == View.VISIBLE;
        if (icon == null) {
            mEndIconView.setVisibility(View.GONE);
        } else {
            mEndIconView.setImageDrawable(MaterialAttrs.tinted(icon, tint));
            mEndIconView.setOnClickListener(click);
            mEndIconView.setVisibility(View.VISIBLE);
        }
        if (was != (icon != null)) {
            applyFieldPadding();
        }
    }

    private void togglePassword() {
        if (mEditText == null) {
            return;
        }
        mPasswordVisible = !mPasswordVisible;
        int selection = mEditText.getSelectionEnd();
        mEditText.setInputType(InputType.TYPE_CLASS_TEXT | (mPasswordVisible
                ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD));
        if (selection >= 0) {
            mEditText.setSelection(Math.min(selection, mEditText.getText().length()));
        }
        updateEndIcon();
    }

    public void setPasswordVisibilityToggleEnabled(boolean enabled) {
        setEndIconMode(enabled ? END_ICON_PASSWORD_TOGGLE : END_ICON_NONE);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (mEditText != null) {
            mEditText.setEnabled(enabled);
        }
        invalidate();
    }
}
