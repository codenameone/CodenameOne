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
package android.view.animation;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;

/// The legacy view animation: a transformation of a view's drawing (alpha
/// and matrix) computed over time, without changing its layout. The timing
/// rules -- start offset, fill before and after, repeat count and mode --
/// follow AOSP's `Animation.getTransformation` exactly.
public abstract class Animation implements Cloneable {

    public static final int INFINITE = -1;
    public static final int RESTART = 1;
    public static final int REVERSE = 2;
    public static final int START_ON_FIRST_FRAME = -1;
    public static final int ABSOLUTE = 0;
    public static final int RELATIVE_TO_SELF = 1;
    public static final int RELATIVE_TO_PARENT = 2;
    public static final int ZORDER_NORMAL = 0;
    public static final int ZORDER_TOP = 1;
    public static final int ZORDER_BOTTOM = -1;

    public interface AnimationListener {
        void onAnimationStart(Animation animation);

        void onAnimationEnd(Animation animation);

        void onAnimationRepeat(Animation animation);
    }

    boolean mEnded;
    boolean mStarted;
    boolean mCycleFlip;
    boolean mInitialized;
    boolean mFillBefore = true;
    boolean mFillAfter;
    boolean mFillEnabled;
    long mStartTime = -1;
    long mStartOffset;
    long mDuration;
    int mRepeatCount;
    int mRepeated;
    int mRepeatMode = RESTART;
    Interpolator mInterpolator;
    AnimationListener mListener;
    private int mZAdjustment;
    private int mBackgroundColor;
    private float mScaleFactor = 1f;
    private boolean mDetachWallpaper;
    private boolean mMore = true;
    private boolean mOneMoreTime = true;

    public Animation() {
        ensureInterpolator();
    }

    public Animation(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.Animation);
        setDuration(a.getInt(android.R.styleable.Animation_duration, 0));
        setStartOffset(a.getInt(android.R.styleable.Animation_startOffset, 0));
        setFillEnabled(a.getBoolean(android.R.styleable.Animation_fillEnabled, mFillEnabled));
        setFillBefore(a.getBoolean(android.R.styleable.Animation_fillBefore, mFillBefore));
        setFillAfter(a.getBoolean(android.R.styleable.Animation_fillAfter, mFillAfter));
        setRepeatCount(a.getInt(android.R.styleable.Animation_repeatCount, mRepeatCount));
        setRepeatMode(a.getInt(android.R.styleable.Animation_repeatMode, RESTART));
        setZAdjustment(a.getInt(android.R.styleable.Animation_zAdjustment, ZORDER_NORMAL));
        setDetachWallpaper(a.getBoolean(android.R.styleable.Animation_detachWallpaper, false));
        int resId = a.getResourceId(android.R.styleable.Animation_interpolator, 0);
        a.recycle();
        if (resId > 0) {
            setInterpolator(context, resId);
        }
        ensureInterpolator();
    }

    /// A copy of this animation. Every framework animation overrides this
    /// with an explicit copy, because Codename One's Object.clone() copies
    /// nothing (it answers null on the iOS VM). An application subclass that
    /// does not override it cannot be copied.
    @Override
    protected Animation clone() {
        throw new UnsupportedOperationException(getClass().getName()
                + " must override clone() to be copied; the Codename One runtime has no Object.clone()");
    }

    /// Copies the state every animation has; a subclass's clone() calls it
    /// on its new instance and then copies its own fields.
    protected final void copyAnimationFrom(Animation o) {
        mEnded = o.mEnded;
        mStarted = o.mStarted;
        mCycleFlip = o.mCycleFlip;
        mInitialized = o.mInitialized;
        mFillBefore = o.mFillBefore;
        mFillAfter = o.mFillAfter;
        mFillEnabled = o.mFillEnabled;
        mStartTime = o.mStartTime;
        mStartOffset = o.mStartOffset;
        mDuration = o.mDuration;
        mRepeatCount = o.mRepeatCount;
        mRepeated = o.mRepeated;
        mRepeatMode = o.mRepeatMode;
        mInterpolator = o.mInterpolator;
        mListener = o.mListener;
        mZAdjustment = o.mZAdjustment;
        mBackgroundColor = o.mBackgroundColor;
        mScaleFactor = o.mScaleFactor;
        mDetachWallpaper = o.mDetachWallpaper;
        mMore = o.mMore;
        mOneMoreTime = o.mOneMoreTime;
    }

    public void reset() {
        mInitialized = false;
        mCycleFlip = false;
        mRepeated = 0;
        mMore = true;
        mOneMoreTime = true;
    }

    public void cancel() {
        if (mStarted && !mEnded) {
            fireAnimationEnd();
            mEnded = true;
        }
        // Long.MIN_VALUE marks the animation canceled; see isCanceled.
        mStartTime = Long.MIN_VALUE;
        mMore = false;
        mOneMoreTime = false;
    }

    public void detach() {
        if (mStarted && !mEnded) {
            mEnded = true;
            fireAnimationEnd();
        }
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    /// Called with the size of the view and its parent before the first
    /// frame; subclasses resolve relative values here.
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        reset();
        mInitialized = true;
    }

    public void setInterpolator(Context context, int resID) {
        setInterpolator(AnimationUtils.loadInterpolator(context, resID));
    }

    public void setInterpolator(Interpolator i) {
        mInterpolator = i;
    }

    public void setStartOffset(long startOffset) {
        mStartOffset = startOffset;
    }

    public void setDuration(long durationMillis) {
        if (durationMillis < 0) {
            throw new IllegalArgumentException("Animation duration cannot be negative");
        }
        mDuration = durationMillis;
    }

    public void restrictDuration(long durationMillis) {
        if (mStartOffset > durationMillis) {
            mStartOffset = durationMillis;
            mDuration = 0;
            mRepeatCount = 0;
            return;
        }
        long dur = mDuration + mStartOffset;
        if (dur > durationMillis) {
            mDuration = durationMillis - mStartOffset;
            dur = durationMillis;
        }
        if (mDuration <= 0) {
            mDuration = 0;
            mRepeatCount = 0;
            return;
        }
        if (mRepeatCount < 0 || mRepeatCount > durationMillis || (dur * mRepeatCount) > durationMillis) {
            mRepeatCount = (int) (durationMillis / dur) - 1;
            if (mRepeatCount < 0) {
                mRepeatCount = 0;
            }
        }
    }

    public void scaleCurrentDuration(float scale) {
        mDuration = (long) (mDuration * scale);
        mStartOffset = (long) (mStartOffset * scale);
    }

    public void setStartTime(long startTimeMillis) {
        mStartTime = startTimeMillis;
        mStarted = false;
        mEnded = false;
        mCycleFlip = false;
        mRepeated = 0;
        mMore = true;
    }

    public void start() {
        setStartTime(-1);
    }

    public void startNow() {
        setStartTime(AnimationUtils.currentAnimationTimeMillis());
    }

    public void setRepeatMode(int repeatMode) {
        mRepeatMode = repeatMode;
    }

    public void setRepeatCount(int repeatCount) {
        if (repeatCount < 0) {
            repeatCount = INFINITE;
        }
        mRepeatCount = repeatCount;
    }

    public boolean isFillEnabled() {
        return mFillEnabled;
    }

    public void setFillEnabled(boolean fillEnabled) {
        mFillEnabled = fillEnabled;
    }

    public void setFillBefore(boolean fillBefore) {
        mFillBefore = fillBefore;
    }

    public void setFillAfter(boolean fillAfter) {
        mFillAfter = fillAfter;
    }

    /// Recorded; animated views keep their drawing order.
    public void setZAdjustment(int zAdjustment) {
        mZAdjustment = zAdjustment;
    }

    public void setBackgroundColor(int bg) {
        mBackgroundColor = bg;
    }

    protected float getScaleFactor() {
        return mScaleFactor;
    }

    public void setDetachWallpaper(boolean detachWallpaper) {
        mDetachWallpaper = detachWallpaper;
    }

    public Interpolator getInterpolator() {
        return mInterpolator;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public long getDuration() {
        return mDuration;
    }

    public long getStartOffset() {
        return mStartOffset;
    }

    public int getRepeatMode() {
        return mRepeatMode;
    }

    public int getRepeatCount() {
        return mRepeatCount;
    }

    public boolean getFillBefore() {
        return mFillBefore;
    }

    public boolean getFillAfter() {
        return mFillAfter;
    }

    public int getZAdjustment() {
        return mZAdjustment;
    }

    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    public boolean getDetachWallpaper() {
        return mDetachWallpaper;
    }

    public boolean willChangeTransformationMatrix() {
        return true;
    }

    public boolean willChangeBounds() {
        return true;
    }

    public void setAnimationListener(AnimationListener listener) {
        mListener = listener;
    }

    protected void ensureInterpolator() {
        if (mInterpolator == null) {
            mInterpolator = new AccelerateDecelerateInterpolator();
        }
    }

    public long computeDurationHint() {
        return (getStartOffset() + getDuration()) * (getRepeatCount() + 1);
    }

    /// Fills `outTransformation` for `currentTime` and answers whether the
    /// animation still runs; the first call fixes the start time when it is
    /// [#START_ON_FIRST_FRAME].
    public boolean getTransformation(long currentTime, Transformation outTransformation) {
        if (mStartTime == -1) {
            mStartTime = currentTime;
        }
        long startOffset = getStartOffset();
        long duration = mDuration;
        float normalizedTime;
        if (duration != 0) {
            normalizedTime = ((float) (currentTime - (mStartTime + startOffset))) / (float) duration;
        } else {
            normalizedTime = currentTime < mStartTime ? 0.0f : 1.0f;
        }
        boolean expired = normalizedTime >= 1.0f || isCanceled();
        mMore = !expired;
        if (!mFillEnabled) {
            normalizedTime = Math.max(Math.min(normalizedTime, 1.0f), 0.0f);
        }
        if ((normalizedTime >= 0.0f || mFillBefore) && (normalizedTime <= 1.0f || mFillAfter)) {
            if (!mStarted) {
                fireAnimationStart();
                mStarted = true;
            }
            if (mFillEnabled) {
                normalizedTime = Math.max(Math.min(normalizedTime, 1.0f), 0.0f);
            }
            if (mCycleFlip) {
                normalizedTime = 1.0f - normalizedTime;
            }
            float interpolatedTime = mInterpolator.getInterpolation(normalizedTime);
            applyTransformation(interpolatedTime, outTransformation);
        }
        if (expired) {
            if (mRepeatCount == mRepeated || isCanceled()) {
                if (!mEnded) {
                    mEnded = true;
                    fireAnimationEnd();
                }
            } else {
                if (mRepeatCount > 0) {
                    mRepeated++;
                }
                if (mRepeatMode == REVERSE) {
                    mCycleFlip = !mCycleFlip;
                }
                mStartTime = -1;
                mMore = true;
                fireAnimationRepeat();
            }
        }
        if (!mMore && mOneMoreTime) {
            mOneMoreTime = false;
            return true;
        }
        return mMore;
    }

    public boolean getTransformation(long currentTime, Transformation outTransformation, float scale) {
        mScaleFactor = scale;
        return getTransformation(currentTime, outTransformation);
    }

    private boolean isCanceled() {
        return mStartTime == Long.MIN_VALUE;
    }

    private void fireAnimationStart() {
        if (mListener != null) {
            mListener.onAnimationStart(this);
        }
    }

    private void fireAnimationRepeat() {
        if (mListener != null) {
            mListener.onAnimationRepeat(this);
        }
    }

    private void fireAnimationEnd() {
        if (mListener != null) {
            mListener.onAnimationEnd(this);
        }
    }

    public boolean hasStarted() {
        return mStarted;
    }

    public boolean hasEnded() {
        return mEnded;
    }

    protected void applyTransformation(float interpolatedTime, Transformation t) {
    }

    protected float resolveSize(int type, float value, int size, int parentSize) {
        switch (type) {
            case ABSOLUTE:
                return value;
            case RELATIVE_TO_SELF:
                return size * value;
            case RELATIVE_TO_PARENT:
                return parentSize * value;
            default:
                return value;
        }
    }

    public boolean hasAlpha() {
        return false;
    }

    /// A value from an animation attribute: absolute, or a fraction of the
    /// view (`50%`) or of its parent (`50%p`).
    protected static class Description {
        public int type;
        public float value;

        static Description parseValue(TypedValue value, Context context) {
            Description d = new Description();
            if (value == null) {
                d.type = ABSOLUTE;
                d.value = 0;
            } else if (value.type == TypedValue.TYPE_FRACTION) {
                d.type = (value.data & TypedValue.COMPLEX_UNIT_MASK) == TypedValue.COMPLEX_UNIT_FRACTION_PARENT
                        ? RELATIVE_TO_PARENT : RELATIVE_TO_SELF;
                d.value = TypedValue.complexToFloat(value.data);
            } else if (value.type == TypedValue.TYPE_FLOAT) {
                d.type = ABSOLUTE;
                d.value = value.getFloat();
            } else if (value.type >= TypedValue.TYPE_FIRST_INT && value.type <= TypedValue.TYPE_LAST_INT) {
                d.type = ABSOLUTE;
                d.value = value.data;
            } else if (value.type == TypedValue.TYPE_DIMENSION) {
                d.type = ABSOLUTE;
                d.value = TypedValue.complexToDimension(value.data, context.getResources().getDisplayMetrics());
            } else {
                d.type = ABSOLUTE;
                d.value = 0;
            }
            return d;
        }
    }
}
