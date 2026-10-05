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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;

/// Displays a drawable, scaled into the view by its scale type.
public class ImageView extends android.view.View {

    public enum ScaleType {
        MATRIX(0), FIT_XY(1), FIT_START(2), FIT_CENTER(3), FIT_END(4), CENTER(5), CENTER_CROP(6), CENTER_INSIDE(7);

        final int nativeInt;

        ScaleType(int ni) {
            nativeInt = ni;
        }
    }

    private static final ScaleType[] SCALE_TYPES = {ScaleType.MATRIX, ScaleType.FIT_XY, ScaleType.FIT_START,
        ScaleType.FIT_CENTER, ScaleType.FIT_END, ScaleType.CENTER, ScaleType.CENTER_CROP, ScaleType.CENTER_INSIDE};

    private Drawable mDrawable;
    private int mResource;
    private ScaleType mScaleType = ScaleType.FIT_CENTER;
    private boolean mAdjustViewBounds;
    private int mMaxWidth = Integer.MAX_VALUE;
    private int mMaxHeight = Integer.MAX_VALUE;
    private Matrix mMatrix = new Matrix();
    private Matrix mDrawMatrix;
    private ColorStateList mTint;
    private PorterDuff.Mode mTintMode = PorterDuff.Mode.SRC_IN;
    private ColorFilter mColorFilter;
    /// Whether this view has ever set a tint / color filter on its drawable.
    /// Once it has, the current value (null included) is pushed to every
    /// drawable, so clearing one really clears it; until then the drawable's
    /// own tint and filter are left alone, as on Android.
    private boolean mHasTint;
    private boolean mHasTintMode;
    private boolean mHasColorFilter;
    private int mAlpha = 255;
    private boolean mCropToPadding;
    private int mLevel;

    public ImageView(Context context) {
        super(context);
    }

    public ImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.ImageView, defStyleAttr, defStyleRes);
        Drawable d = a.getDrawable(android.R.styleable.ImageView_src);
        mAdjustViewBounds = a.getBoolean(android.R.styleable.ImageView_adjustViewBounds, false);
        mMaxWidth = a.getDimensionPixelSize(android.R.styleable.ImageView_maxWidth, Integer.MAX_VALUE);
        mMaxHeight = a.getDimensionPixelSize(android.R.styleable.ImageView_maxHeight, Integer.MAX_VALUE);
        int st = a.getInt(android.R.styleable.ImageView_scaleType, -1);
        if (st >= 0 && st < SCALE_TYPES.length) {
            mScaleType = SCALE_TYPES[st];
        }
        if (a.hasValue(android.R.styleable.ImageView_tint)) {
            mTint = a.getColorStateList(android.R.styleable.ImageView_tint);
            mHasTint = true;
            mTintMode = PorterDuff.intToMode(a.getInt(android.R.styleable.ImageView_tintMode, 5));
            mHasTintMode = true;
        }
        mCropToPadding = a.getBoolean(android.R.styleable.ImageView_cropToPadding, false);
        a.recycle();
        if (d != null) {
            setImageDrawable(d);
        }
    }

    // ------------------------------------------------------------ content

    public void setImageResource(int resId) {
        if (mResource == resId && mDrawable != null) {
            return;
        }
        mResource = resId;
        updateDrawable(resId == 0 ? null : getContext().getDrawable(resId));
    }

    public void setImageDrawable(Drawable drawable) {
        if (mDrawable == drawable) {
            return;
        }
        mResource = 0;
        updateDrawable(drawable);
    }

    public void setImageBitmap(Bitmap bm) {
        setImageDrawable(bm == null ? null : new BitmapDrawable(getResources(), bm));
    }

    public void setImageURI(Uri uri) {
        if (uri == null) {
            setImageDrawable(null);
            return;
        }
        try {
            java.io.InputStream in = getContext().getContentResolver().openInputStream(uri);
            try {
                setImageBitmap(android.graphics.BitmapFactory.decodeStream(in));
            } finally {
                in.close();
            }
        } catch (java.io.IOException e) {
            setImageDrawable(null);
        }
    }

    public void setImageIcon(Object icon) {
    }

    public Drawable getDrawable() {
        return mDrawable;
    }

    private void updateDrawable(Drawable d) {
        int oldW = mDrawable == null ? -1 : mDrawable.getIntrinsicWidth();
        int oldH = mDrawable == null ? -1 : mDrawable.getIntrinsicHeight();
        if (mDrawable != null) {
            mDrawable.setCallback(null);
        }
        mDrawable = d;
        if (d != null) {
            d.setCallback(this);
            if (d.isStateful()) {
                d.setState(getDrawableState());
            }
            d.setLevel(mLevel);
            applyColorMod();
        }
        if (d == null || d.getIntrinsicWidth() != oldW || d.getIntrinsicHeight() != oldH) {
            requestLayout();
        }
        configureBounds();
        invalidate();
    }

    public void setImageLevel(int level) {
        mLevel = level;
        if (mDrawable != null) {
            mDrawable.setLevel(level);
        }
    }

    public void setImageState(int[] state, boolean merge) {
    }

    public void setImageTintList(ColorStateList tint) {
        mTint = tint;
        mHasTint = true;
        applyColorMod();
        invalidate();
    }

    public ColorStateList getImageTintList() {
        return mTint;
    }

    public void setImageTintMode(PorterDuff.Mode mode) {
        mTintMode = mode;
        mHasTintMode = true;
        applyColorMod();
        invalidate();
    }

    public PorterDuff.Mode getImageTintMode() {
        return mTintMode;
    }

    public final void setColorFilter(int color, PorterDuff.Mode mode) {
        setColorFilter(new PorterDuffColorFilter(color, mode));
    }

    public final void setColorFilter(int color) {
        setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    public void setColorFilter(ColorFilter cf) {
        mColorFilter = cf;
        mHasColorFilter = true;
        applyColorMod();
        invalidate();
    }

    public final void clearColorFilter() {
        setColorFilter(null);
    }

    public ColorFilter getColorFilter() {
        return mColorFilter;
    }

    public void setImageAlpha(int alpha) {
        mAlpha = alpha & 0xff;
        applyColorMod();
        invalidate();
    }

    public int getImageAlpha() {
        return mAlpha;
    }

    @Deprecated
    public void setAlpha(int alpha) {
        setImageAlpha(alpha);
    }

    private void applyColorMod() {
        if (mDrawable == null) {
            return;
        }
        // A color filter takes precedence over the tint inside the drawable,
        // so both are pushed independently.
        if (mHasTint) {
            mDrawable.setTintList(mTint);
        }
        if (mHasTint || mHasTintMode) {
            mDrawable.setTintMode(mTintMode);
        }
        if (mHasColorFilter) {
            mDrawable.setColorFilter(mColorFilter);
        }
        mDrawable.setAlpha(mAlpha);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mDrawable != null && mDrawable.isStateful() && mDrawable.setState(getDrawableState())) {
            invalidate();
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable dr) {
        return mDrawable == dr || super.verifyDrawable(dr);
    }

    // ------------------------------------------------------------ scaling

    public void setScaleType(ScaleType scaleType) {
        if (scaleType == null) {
            throw new NullPointerException();
        }
        if (mScaleType != scaleType) {
            mScaleType = scaleType;
            requestLayout();
            configureBounds();
            invalidate();
        }
    }

    public ScaleType getScaleType() {
        return mScaleType;
    }

    public void setImageMatrix(Matrix matrix) {
        mMatrix = matrix == null ? new Matrix() : new Matrix(matrix);
        configureBounds();
        invalidate();
    }

    public Matrix getImageMatrix() {
        return mDrawMatrix == null ? new Matrix() : mDrawMatrix;
    }

    public void setAdjustViewBounds(boolean adjustViewBounds) {
        mAdjustViewBounds = adjustViewBounds;
        if (adjustViewBounds) {
            setScaleType(ScaleType.FIT_CENTER);
        }
    }

    public boolean getAdjustViewBounds() {
        return mAdjustViewBounds;
    }

    public void setMaxWidth(int maxWidth) {
        mMaxWidth = maxWidth;
    }

    public int getMaxWidth() {
        return mMaxWidth;
    }

    public void setMaxHeight(int maxHeight) {
        mMaxHeight = maxHeight;
    }

    public int getMaxHeight() {
        return mMaxHeight;
    }

    public void setCropToPadding(boolean cropToPadding) {
        mCropToPadding = cropToPadding;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w;
        int h;
        float desiredAspect = 0.0f;
        boolean resizeWidth = false;
        boolean resizeHeight = false;
        final int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        if (mDrawable == null) {
            w = h = 0;
        } else {
            w = Math.max(1, mDrawable.getIntrinsicWidth());
            h = Math.max(1, mDrawable.getIntrinsicHeight());
            if (mAdjustViewBounds) {
                resizeWidth = widthSpecMode != MeasureSpec.EXACTLY;
                resizeHeight = heightSpecMode != MeasureSpec.EXACTLY;
                desiredAspect = (float) w / (float) h;
            }
        }
        final int pleft = mPaddingLeft;
        final int pright = mPaddingRight;
        final int ptop = mPaddingTop;
        final int pbottom = mPaddingBottom;
        int widthSize;
        int heightSize;
        if (resizeWidth || resizeHeight) {
            widthSize = resizeAdjust(w + pleft + pright, mMaxWidth, widthMeasureSpec);
            heightSize = resizeAdjust(h + ptop + pbottom, mMaxHeight, heightMeasureSpec);
            if (desiredAspect != 0.0f) {
                final float actualAspect = (float) (widthSize - pleft - pright) / (heightSize - ptop - pbottom);
                if (Math.abs(actualAspect - desiredAspect) > 0.0000001) {
                    boolean done = false;
                    if (resizeWidth) {
                        int newWidth = (int) (desiredAspect * (heightSize - ptop - pbottom)) + pleft + pright;
                        if (!resizeHeight) {
                            widthSize = resizeAdjust(newWidth, mMaxWidth, widthMeasureSpec);
                        }
                        if (newWidth <= widthSize) {
                            widthSize = newWidth;
                            done = true;
                        }
                    }
                    if (!done && resizeHeight) {
                        int newHeight = (int) ((widthSize - pleft - pright) / desiredAspect) + ptop + pbottom;
                        if (!resizeWidth) {
                            heightSize = resizeAdjust(newHeight, mMaxHeight, heightMeasureSpec);
                        }
                        if (newHeight <= heightSize) {
                            heightSize = newHeight;
                        }
                    }
                }
            }
        } else {
            w += pleft + pright;
            h += ptop + pbottom;
            w = Math.max(w, getSuggestedMinimumWidth());
            h = Math.max(h, getSuggestedMinimumHeight());
            widthSize = resolveSizeAndState(w, widthMeasureSpec, 0);
            heightSize = resolveSizeAndState(h, heightMeasureSpec, 0);
        }
        setMeasuredDimension(widthSize, heightSize);
    }

    private int resizeAdjust(int desiredSize, int maxSize, int measureSpec) {
        int result = desiredSize;
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize = MeasureSpec.getSize(measureSpec);
        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                result = Math.min(desiredSize, maxSize);
                break;
            case MeasureSpec.AT_MOST:
                result = Math.min(Math.min(desiredSize, specSize), maxSize);
                break;
            default:
                result = specSize;
                break;
        }
        return result;
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        boolean changed = super.setFrame(l, t, r, b);
        configureBounds();
        return changed;
    }

    private void configureBounds() {
        if (mDrawable == null) {
            return;
        }
        final int dwidth = mDrawable.getIntrinsicWidth();
        final int dheight = mDrawable.getIntrinsicHeight();
        final int vwidth = getWidth() - mPaddingLeft - mPaddingRight;
        final int vheight = getHeight() - mPaddingTop - mPaddingBottom;
        final boolean fits = (dwidth < 0 || vwidth == dwidth) && (dheight < 0 || vheight == dheight);
        if (dwidth <= 0 || dheight <= 0 || ScaleType.FIT_XY == mScaleType) {
            mDrawable.setBounds(0, 0, vwidth, vheight);
            mDrawMatrix = null;
            return;
        }
        mDrawable.setBounds(0, 0, dwidth, dheight);
        if (ScaleType.MATRIX == mScaleType) {
            mDrawMatrix = mMatrix.isIdentity() ? null : mMatrix;
        } else if (fits) {
            mDrawMatrix = null;
        } else if (ScaleType.CENTER == mScaleType) {
            mDrawMatrix = new Matrix();
            mDrawMatrix.setTranslate(Math.round((vwidth - dwidth) * 0.5f), Math.round((vheight - dheight) * 0.5f));
        } else if (ScaleType.CENTER_CROP == mScaleType) {
            mDrawMatrix = new Matrix();
            float scale;
            float dx = 0;
            float dy = 0;
            if (dwidth * vheight > vwidth * dheight) {
                scale = (float) vheight / (float) dheight;
                dx = (vwidth - dwidth * scale) * 0.5f;
            } else {
                scale = (float) vwidth / (float) dwidth;
                dy = (vheight - dheight * scale) * 0.5f;
            }
            mDrawMatrix.setScale(scale, scale);
            mDrawMatrix.postTranslate(Math.round(dx), Math.round(dy));
        } else if (ScaleType.CENTER_INSIDE == mScaleType) {
            mDrawMatrix = new Matrix();
            float scale;
            if (dwidth <= vwidth && dheight <= vheight) {
                scale = 1.0f;
            } else {
                scale = Math.min((float) vwidth / (float) dwidth, (float) vheight / (float) dheight);
            }
            float dx = Math.round((vwidth - dwidth * scale) * 0.5f);
            float dy = Math.round((vheight - dheight * scale) * 0.5f);
            mDrawMatrix.setScale(scale, scale);
            mDrawMatrix.postTranslate(dx, dy);
        } else {
            RectF src = new RectF(0, 0, dwidth, dheight);
            RectF dst = new RectF(0, 0, vwidth, vheight);
            mDrawMatrix = new Matrix();
            Matrix.ScaleToFit stf = mScaleType == ScaleType.FIT_START ? Matrix.ScaleToFit.START
                    : mScaleType == ScaleType.FIT_END ? Matrix.ScaleToFit.END : Matrix.ScaleToFit.CENTER;
            mDrawMatrix.setRectToRect(src, dst, stf);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDrawable == null || mDrawable.getBounds().isEmpty() && !(mDrawable instanceof ColorDrawable)) {
            return;
        }
        if (mDrawMatrix == null && mPaddingTop == 0 && mPaddingLeft == 0) {
            mDrawable.draw(canvas);
            return;
        }
        int save = canvas.save();
        if (mCropToPadding) {
            canvas.clipRect(mScrollX + mPaddingLeft, mScrollY + mPaddingTop,
                    mScrollX + getWidth() - mPaddingRight, mScrollY + getHeight() - mPaddingBottom);
        }
        canvas.translate(mPaddingLeft, mPaddingTop);
        if (mDrawMatrix != null) {
            canvas.concat(mDrawMatrix);
        }
        mDrawable.draw(canvas);
        canvas.restoreToCount(save);
    }

    @Override
    public int getBaseline() {
        return -1;
    }

    public CharSequence getAccessibilityClassName() {
        return ImageView.class.getName();
    }
}
