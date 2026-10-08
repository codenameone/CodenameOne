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
package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import com.codename1.androidcompat.runtime.ControlDrawables;

/// A progress indicator: a horizontal bar (determinate or indeterminate) or
/// the Material circular spinner, depending on the style's
/// `indeterminateOnly` (true for the circular styles). Drawn in code in the
/// theme's activated color; indeterminate animations run while the view is
/// attached and shown.
public class ProgressBar extends View {

    private int mMin;
    private int mMax = 100;
    private int mProgress;
    private int mSecondaryProgress;
    private boolean mIndeterminate;
    private boolean mOnlyIndeterminate;
    private ColorStateList mProgressTint;
    private ColorStateList mProgressBackgroundTint;
    private ColorStateList mSecondaryProgressTint;
    private ColorStateList mIndeterminateTint;
    private Drawable mProgressDrawable;
    private Drawable mIndeterminateDrawable;
    int mMinWidth;
    int mMaxWidth;
    int mMinHeight;
    int mMaxHeight;
    final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    int[] mColors;
    private long mAnimStart = System.currentTimeMillis();

    public ProgressBar(Context context) {
        this(context, null);
    }

    public ProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.progressBarStyle);
    }

    public ProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, defaultStyle(defStyleAttr));
    }

    private static int defaultStyle(int defStyleAttr) {
        if (defStyleAttr == android.R.attr.progressBarStyleHorizontal) {
            return android.R.style.Widget_Material_ProgressBar_Horizontal;
        }
        if (defStyleAttr == android.R.attr.progressBarStyleSmall) {
            return android.R.style.Widget_Material_ProgressBar_Small;
        }
        if (defStyleAttr == android.R.attr.progressBarStyleLarge) {
            return android.R.style.Widget_Material_ProgressBar_Large;
        }
        return android.R.style.Widget_Material_ProgressBar;
    }

    public ProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mColors = ControlDrawables.controlColors(context);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.ProgressBar, defStyleAttr,
                defStyleRes);
        float density = context.getResources().getDisplayMetrics().density;
        mOnlyIndeterminate = a.getBoolean(android.R.styleable.ProgressBar_indeterminateOnly, true);
        mMinWidth = a.getDimensionPixelSize(android.R.styleable.ProgressBar_minWidth, Math.round(24 * density));
        mMaxWidth = a.getDimensionPixelSize(android.R.styleable.ProgressBar_maxWidth, Math.round(48 * density));
        mMinHeight = a.getDimensionPixelSize(android.R.styleable.ProgressBar_minHeight, Math.round(24 * density));
        mMaxHeight = a.getDimensionPixelSize(android.R.styleable.ProgressBar_maxHeight, Math.round(48 * density));
        mMin = a.getInt(android.R.styleable.ProgressBar_min, 0);
        mMax = a.getInt(android.R.styleable.ProgressBar_max, 100);
        mProgress = a.getInt(android.R.styleable.ProgressBar_progress, 0);
        mSecondaryProgress = a.getInt(android.R.styleable.ProgressBar_secondaryProgress, 0);
        mIndeterminate = mOnlyIndeterminate || a.getBoolean(android.R.styleable.ProgressBar_indeterminate, false);
        if (a.hasValue(android.R.styleable.ProgressBar_progressTint)) {
            mProgressTint = a.getColorStateList(android.R.styleable.ProgressBar_progressTint);
        }
        if (a.hasValue(android.R.styleable.ProgressBar_progressBackgroundTint)) {
            mProgressBackgroundTint = a.getColorStateList(android.R.styleable.ProgressBar_progressBackgroundTint);
        }
        if (a.hasValue(android.R.styleable.ProgressBar_secondaryProgressTint)) {
            mSecondaryProgressTint = a.getColorStateList(android.R.styleable.ProgressBar_secondaryProgressTint);
        }
        if (a.hasValue(android.R.styleable.ProgressBar_indeterminateTint)) {
            mIndeterminateTint = a.getColorStateList(android.R.styleable.ProgressBar_indeterminateTint);
        }
        mProgressDrawable = a.getDrawable(android.R.styleable.ProgressBar_progressDrawable);
        mIndeterminateDrawable = a.getDrawable(android.R.styleable.ProgressBar_indeterminateDrawable);
        a.recycle();
        if (mProgressDrawable != null) {
            mProgressDrawable.setCallback(this);
        }
        if (mIndeterminateDrawable != null) {
            mIndeterminateDrawable.setCallback(this);
        }
    }

    @Override
    protected android.os.Parcelable onSaveInstanceState() {
        return new SavedState(super.onSaveInstanceState(), mProgress, mSecondaryProgress);
    }

    @Override
    protected void onRestoreInstanceState(android.os.Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState saved = (SavedState) state;
        super.onRestoreInstanceState(saved.superState);
        setProgress(saved.progress);
        setSecondaryProgress(saved.secondaryProgress);
    }

    /// In-memory state for view recreation, matching the other widgets.
    private static final class SavedState implements android.os.Parcelable {
        final android.os.Parcelable superState;
        final int progress;
        final int secondaryProgress;

        SavedState(android.os.Parcelable superState, int progress, int secondaryProgress) {
            this.superState = superState;
            this.progress = progress;
            this.secondaryProgress = secondaryProgress;
        }

        @Override
        public int describeContents() { return 0; }

        @Override
        public void writeToParcel(android.os.Parcel dest, int flags) { }
    }

    /// True for the circular spinner styles.
    boolean isCircular() {
        return mOnlyIndeterminate;
    }

    float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    // ------------------------------------------------------------ model

    public int getMin() {
        return mMin;
    }

    public void setMin(int min) {
        mMin = min;
        if (mMax < min) {
            mMax = min;
        }
        setProgress(mProgress);
        clampToRange();
    }

    public int getMax() {
        return mMax;
    }

    public void setMax(int max) {
        if (max < mMin) {
            max = mMin;
        }
        mMax = max;
        clampToRange();
    }

    /// Pulls both progress values back inside the range after an endpoint
    /// moved. The secondary value is clamped too, or a shrunk range left it
    /// past the end and the bar drew it beyond the track; and the primary is
    /// clamped directly because `setProgress` ignores an indeterminate bar.
    private void clampToRange() {
        mProgress = Math.max(mMin, Math.min(mMax, mProgress));
        mSecondaryProgress = Math.max(mMin, Math.min(mMax, mSecondaryProgress));
        invalidate();
    }

    public int getProgress() {
        return mIndeterminate ? 0 : mProgress;
    }

    public void setProgress(int progress) {
        setProgressInternal(progress, false);
    }

    public void setProgress(int progress, boolean animate) {
        setProgressInternal(progress, false);
    }

    boolean setProgressInternal(int progress, boolean fromUser) {
        if (mIndeterminate) {
            return false;
        }
        progress = Math.max(mMin, Math.min(mMax, progress));
        if (progress == mProgress) {
            return false;
        }
        mProgress = progress;
        onProgressRefresh(scale(progress), fromUser, progress);
        invalidate();
        return true;
    }

    void onProgressRefresh(float scale, boolean fromUser, int progress) {
    }

    float scale(int progress) {
        int range = mMax - mMin;
        return range > 0 ? (progress - mMin) / (float) range : 0;
    }

    public int getSecondaryProgress() {
        return mIndeterminate ? 0 : mSecondaryProgress;
    }

    public void setSecondaryProgress(int secondaryProgress) {
        mSecondaryProgress = Math.max(mMin, Math.min(mMax, secondaryProgress));
        invalidate();
    }

    public final void incrementProgressBy(int diff) {
        setProgress(mProgress + diff);
    }

    public final void incrementSecondaryProgressBy(int diff) {
        setSecondaryProgress(mSecondaryProgress + diff);
    }

    public boolean isIndeterminate() {
        return mIndeterminate;
    }

    public void setIndeterminate(boolean indeterminate) {
        if (mOnlyIndeterminate && !indeterminate) {
            return;
        }
        if (indeterminate != mIndeterminate) {
            mIndeterminate = indeterminate;
            mAnimStart = System.currentTimeMillis();
            invalidate();
        }
    }

    public void setProgressTintList(ColorStateList tint) {
        mProgressTint = tint;
        invalidate();
    }

    public ColorStateList getProgressTintList() {
        return mProgressTint;
    }

    public void setProgressBackgroundTintList(ColorStateList tint) {
        mProgressBackgroundTint = tint;
        invalidate();
    }

    public void setSecondaryProgressTintList(ColorStateList tint) {
        mSecondaryProgressTint = tint;
        invalidate();
    }

    public void setIndeterminateTintList(ColorStateList tint) {
        mIndeterminateTint = tint;
        invalidate();
    }

    public ColorStateList getIndeterminateTintList() {
        return mIndeterminateTint;
    }

    public Drawable getProgressDrawable() {
        return mProgressDrawable;
    }

    public void setProgressDrawable(Drawable d) {
        if (mProgressDrawable != null && mProgressDrawable != d && mProgressDrawable != mIndeterminateDrawable) {
            mProgressDrawable.setCallback(null);
        }
        mProgressDrawable = d;
        if (d != null) {
            d.setCallback(this);
        }
        invalidate();
    }

    public Drawable getIndeterminateDrawable() {
        return mIndeterminateDrawable;
    }

    public void setIndeterminateDrawable(Drawable d) {
        if (mIndeterminateDrawable != null && mIndeterminateDrawable != d && mIndeterminateDrawable != mProgressDrawable) {
            mIndeterminateDrawable.setCallback(null);
        }
        mIndeterminateDrawable = d;
        if (d != null) {
            d.setCallback(this);
        }
        invalidate();
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mProgressDrawable || who == mIndeterminateDrawable || super.verifyDrawable(who);
    }

    // ------------------------------------------------------------ measure & draw

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int dw = isCircular() ? Math.max(mMinWidth, Math.min(mMaxWidth, Math.round(dp(48)))) : mMinWidth;
        int dh = Math.max(mMinHeight, Math.min(mMaxHeight, Math.round(dp(isCircular() ? 48 : 16))));
        dw += getPaddingLeft() + getPaddingRight();
        dh += getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSizeAndState(dw, widthMeasureSpec, 0),
                resolveSizeAndState(dh, heightMeasureSpec, 0));
    }

    int color(ColorStateList tint, int def) {
        int c = tint == null ? def : tint.getColorForState(getDrawableState(), def);
        if (!isEnabled()) {
            c = (c & 0xffffff) | ((((c >>> 24) * mColors[2]) / 255) << 24);
        }
        return c;
    }

    static int withAlpha(int color, float alpha) {
        return (color & 0xffffff) | (Math.round((color >>> 24) * alpha) << 24);
    }

    /// The horizontal track's left, right and vertical center.
    float[] trackGeometry() {
        float left = getPaddingLeft();
        float right = getWidth() - getPaddingRight();
        float cy = getPaddingTop() + (getHeight() - getPaddingTop() - getPaddingBottom()) / 2f;
        return new float[] {left, right, cy};
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mColors == null) {
            return;
        }
        if (mIndeterminate && mIndeterminateDrawable != null) {
            drawIndeterminateDrawable(canvas);
        } else if (isCircular()) {
            drawCircular(canvas);
        } else {
            drawHorizontal(canvas);
        }
        if (mIndeterminate && isAttachedToWindow() && isShown()) {
            postInvalidateDelayed(16);
        }
    }

    /// The app's own indeterminate artwork, over the content box. Android
    /// animates a drawable that is not itself animatable by sweeping its level
    /// 0..10000 linearly (default `indeterminateDuration`, 3500ms), which is
    /// what turns a rotate or clip drawable into a spinner; do the same.
    private void drawIndeterminateDrawable(Canvas canvas) {
        Drawable d = mIndeterminateDrawable;
        d.setBounds(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(),
                getHeight() - getPaddingBottom());
        long t = (System.currentTimeMillis() - mAnimStart) % INDETERMINATE_DURATION;
        d.setLevel((int) (t * 10000L / INDETERMINATE_DURATION));
        d.draw(canvas);
    }

    private static final long INDETERMINATE_DURATION = 3500L;

    private void drawHorizontal(Canvas canvas) {
        if (mProgressDrawable != null && !mIndeterminate) {
            mProgressDrawable.setBounds(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(),
                    getHeight() - getPaddingBottom());
            mProgressDrawable.setLevel(Math.round(scale(mProgress) * 10000));
            mProgressDrawable.draw(canvas);
            return;
        }
        float[] g = trackGeometry();
        float h = dp(4);
        float top = g[2] - h / 2;
        int activated = color(mProgressTint, mColors[1]);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(color(mProgressBackgroundTint, withAlpha(mColors[1], 0.3f)));
        canvas.drawRect(g[0], top, g[1], top + h, mPaint);
        float width = g[1] - g[0];
        if (mIndeterminate) {
            mPaint.setColor(color(mIndeterminateTint, mColors[1]));
            float t = ((System.currentTimeMillis() - mAnimStart) % 2000L) / 2000f;
            float start = g[0] + width * (t * 1.5f - 0.5f);
            float end = start + width * 0.5f;
            float l = Math.max(g[0], start);
            float r = Math.min(g[1], end);
            if (r > l) {
                canvas.drawRect(l, top, r, top + h, mPaint);
            }
            return;
        }
        if (mSecondaryProgress > mMin) {
            mPaint.setColor(color(mSecondaryProgressTint, withAlpha(mColors[1], 0.5f)));
            canvas.drawRect(g[0], top, g[0] + width * scale(mSecondaryProgress), top + h, mPaint);
        }
        mPaint.setColor(activated);
        canvas.drawRect(g[0], top, g[0] + width * scale(mProgress), top + h, mPaint);
    }

    private void drawCircular(Canvas canvas) {
        int w = getWidth() - getPaddingLeft() - getPaddingRight();
        int h = getHeight() - getPaddingTop() - getPaddingBottom();
        float size = Math.min(w, h);
        float stroke = Math.max(1, size / 12f);
        float cx = getPaddingLeft() + w / 2f;
        float cy = getPaddingTop() + h / 2f;
        float r = size / 2f - stroke;
        RectF oval = new RectF(cx - r, cy - r, cx + r, cy + r);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(stroke);
        mPaint.setStrokeCap(Paint.Cap.SQUARE);
        mPaint.setColor(color(mIndeterminate ? mIndeterminateTint : mProgressTint, mColors[1]));
        if (!mIndeterminate) {
            canvas.drawArc(oval, -90, 360 * scale(mProgress), false, mPaint);
            return;
        }
        // Material's spinner: the arc rotates steadily while its sweep grows
        // and shrinks between 10 and 270 degrees every 1333ms.
        long t = System.currentTimeMillis() - mAnimStart;
        float cycle = (t % 1333L) / 1333f;
        float rotation = (t % 1568L) / 1568f * 360f;
        float sweep;
        float head;
        if (cycle < 0.5f) {
            sweep = 10 + 260 * ease(cycle * 2);
            head = 0;
        } else {
            sweep = 270 - 260 * ease((cycle - 0.5f) * 2);
            head = 260 * ease((cycle - 0.5f) * 2);
        }
        float cycles = (t / 1333L) % 5;
        float start = rotation + head + cycles * 216 - 90;
        canvas.drawArc(oval, start, sweep, false, mPaint);
    }

    private static float ease(float x) {
        return x < 0.5f ? 2 * x * x : 1 - 2 * (1 - x) * (1 - x);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mIndeterminate) {
            invalidate();
        }
    }

    public CharSequence getAccessibilityClassName() {
        return ProgressBar.class.getName();
    }
}
