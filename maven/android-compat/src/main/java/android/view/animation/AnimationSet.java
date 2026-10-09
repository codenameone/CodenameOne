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

import java.util.ArrayList;
import java.util.List;

/// Animations played together, their transformations composed. Properties
/// set on the set (duration, fill, repeat mode, start offset and a shared
/// interpolator) are pushed down to the children when it is initialized,
/// as in AOSP.
public class AnimationSet extends Animation {

    private static final int PROPERTY_FILL_AFTER_MASK = 0x1;
    private static final int PROPERTY_FILL_BEFORE_MASK = 0x2;
    private static final int PROPERTY_REPEAT_MODE_MASK = 0x4;
    private static final int PROPERTY_START_OFFSET_MASK = 0x8;
    private static final int PROPERTY_SHARE_INTERPOLATOR_MASK = 0x10;
    private static final int PROPERTY_DURATION_MASK = 0x20;
    private static final int PROPERTY_MORPH_MATRIX_MASK = 0x40;
    private static final int PROPERTY_CHANGE_BOUNDS_MASK = 0x80;

    private int mFlags;
    private boolean mHasAlpha;
    private boolean mDirty;
    private ArrayList<Animation> mAnimations = new ArrayList<Animation>();
    private Transformation mTempTransformation = new Transformation();
    private long mLastEnd;
    private long[] mStoredOffsets;

    public AnimationSet(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Animation's constructor went through the overridden setters, which
        // mark every property as set on the set; only the ones the XML names
        // are, so start again from none (AOSP gets this from a field
        // initializer, which runs after the super constructor).
        mFlags = 0;
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.AnimationSet);
        setFlag(PROPERTY_SHARE_INTERPOLATOR_MASK,
                a.getBoolean(android.R.styleable.AnimationSet_shareInterpolator, true));
        init();
        if (a.hasValue(android.R.styleable.AnimationSet_duration)) {
            mFlags |= PROPERTY_DURATION_MASK;
        }
        if (a.hasValue(android.R.styleable.AnimationSet_fillBefore)) {
            mFlags |= PROPERTY_FILL_BEFORE_MASK;
        }
        if (a.hasValue(android.R.styleable.AnimationSet_fillAfter)) {
            mFlags |= PROPERTY_FILL_AFTER_MASK;
        }
        if (a.hasValue(android.R.styleable.AnimationSet_repeatMode)) {
            mFlags |= PROPERTY_REPEAT_MODE_MASK;
        }
        if (a.hasValue(android.R.styleable.AnimationSet_startOffset)) {
            mFlags |= PROPERTY_START_OFFSET_MASK;
        }
        a.recycle();
    }

    public AnimationSet(boolean shareInterpolator) {
        setFlag(PROPERTY_SHARE_INTERPOLATOR_MASK, shareInterpolator);
        init();
    }

    @Override
    protected AnimationSet clone() {
        AnimationSet animation = new AnimationSet(false);
        animation.copyAnimationFrom(this);
        animation.mFlags = mFlags;
        animation.mHasAlpha = mHasAlpha;
        animation.mDirty = mDirty;
        animation.mLastEnd = mLastEnd;
        animation.mStoredOffsets = mStoredOffsets == null ? null : copyOf(mStoredOffsets);
        animation.mTempTransformation = new Transformation();
        animation.mAnimations = new ArrayList<Animation>();
        for (Animation a : mAnimations) {
            animation.mAnimations.add(a.clone());
        }
        return animation;
    }

    private static long[] copyOf(long[] a) {
        long[] c = new long[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    private void setFlag(int mask, boolean value) {
        if (value) {
            mFlags |= mask;
        } else {
            mFlags &= ~mask;
        }
    }

    private void init() {
        mStartTime = 0;
    }

    @Override
    public void setFillAfter(boolean fillAfter) {
        mFlags |= PROPERTY_FILL_AFTER_MASK;
        super.setFillAfter(fillAfter);
    }

    @Override
    public void setFillBefore(boolean fillBefore) {
        mFlags |= PROPERTY_FILL_BEFORE_MASK;
        super.setFillBefore(fillBefore);
    }

    @Override
    public void setRepeatMode(int repeatMode) {
        mFlags |= PROPERTY_REPEAT_MODE_MASK;
        super.setRepeatMode(repeatMode);
    }

    @Override
    public void setStartOffset(long startOffset) {
        mFlags |= PROPERTY_START_OFFSET_MASK;
        super.setStartOffset(startOffset);
    }

    @Override
    public boolean hasAlpha() {
        if (mDirty) {
            mDirty = false;
            mHasAlpha = false;
            for (Animation a : mAnimations) {
                if (a.hasAlpha()) {
                    mHasAlpha = true;
                    break;
                }
            }
        }
        return mHasAlpha;
    }

    @Override
    public void setDuration(long durationMillis) {
        mFlags |= PROPERTY_DURATION_MASK;
        super.setDuration(durationMillis);
        mLastEnd = mStartOffset + mDuration;
    }

    public void addAnimation(Animation a) {
        mAnimations.add(a);
        boolean noMatrix = (mFlags & PROPERTY_MORPH_MATRIX_MASK) == 0;
        if (noMatrix && a.willChangeTransformationMatrix()) {
            mFlags |= PROPERTY_MORPH_MATRIX_MASK;
        }
        boolean changeBounds = (mFlags & PROPERTY_CHANGE_BOUNDS_MASK) == 0;
        if (changeBounds && a.willChangeBounds()) {
            mFlags |= PROPERTY_CHANGE_BOUNDS_MASK;
        }
        if ((mFlags & PROPERTY_DURATION_MASK) == PROPERTY_DURATION_MASK) {
            mLastEnd = mStartOffset + mDuration;
        } else {
            if (mAnimations.size() == 1) {
                mDuration = a.getStartOffset() + a.getDuration();
                mLastEnd = mStartOffset + mDuration;
            } else {
                mLastEnd = Math.max(mLastEnd, mStartOffset + a.getStartOffset() + a.getDuration());
                mDuration = mLastEnd - mStartOffset;
            }
        }
        mDirty = true;
    }

    @Override
    public void setStartTime(long startTimeMillis) {
        super.setStartTime(startTimeMillis);
        for (Animation a : mAnimations) {
            a.setStartTime(startTimeMillis);
        }
    }

    @Override
    public long getStartTime() {
        long startTime = Long.MAX_VALUE;
        for (Animation a : mAnimations) {
            startTime = Math.min(startTime, a.getStartTime());
        }
        return startTime;
    }

    @Override
    public void restrictDuration(long durationMillis) {
        super.restrictDuration(durationMillis);
        for (Animation a : mAnimations) {
            a.restrictDuration(durationMillis);
        }
    }

    @Override
    public long getDuration() {
        boolean durationSet = (mFlags & PROPERTY_DURATION_MASK) == PROPERTY_DURATION_MASK;
        if (durationSet) {
            return mDuration;
        }
        long duration = 0;
        for (Animation a : mAnimations) {
            duration = Math.max(duration, a.getDuration());
        }
        return duration;
    }

    @Override
    public long computeDurationHint() {
        long duration = 0;
        for (int i = mAnimations.size() - 1; i >= 0; --i) {
            long d = mAnimations.get(i).computeDurationHint();
            if (d > duration) {
                duration = d;
            }
        }
        return duration;
    }

    @Override
    public boolean getTransformation(long currentTime, Transformation t) {
        boolean more = false;
        boolean started = false;
        boolean ended = true;
        t.clear();
        for (int i = mAnimations.size() - 1; i >= 0; --i) {
            Animation a = mAnimations.get(i);
            mTempTransformation.clear();
            more = a.getTransformation(currentTime, mTempTransformation, getScaleFactor()) || more;
            t.compose(mTempTransformation);
            started = started || a.hasStarted();
            ended = a.hasEnded() && ended;
        }
        if (started && !mStarted) {
            if (mListener != null) {
                mListener.onAnimationStart(this);
            }
            mStarted = true;
        }
        if (ended != mEnded) {
            if (mListener != null) {
                mListener.onAnimationEnd(this);
            }
            mEnded = ended;
        }
        return more;
    }

    @Override
    public void scaleCurrentDuration(float scale) {
        for (Animation a : mAnimations) {
            a.scaleCurrentDuration(scale);
        }
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
        boolean durationSet = (mFlags & PROPERTY_DURATION_MASK) == PROPERTY_DURATION_MASK;
        boolean fillAfterSet = (mFlags & PROPERTY_FILL_AFTER_MASK) == PROPERTY_FILL_AFTER_MASK;
        boolean fillBeforeSet = (mFlags & PROPERTY_FILL_BEFORE_MASK) == PROPERTY_FILL_BEFORE_MASK;
        boolean repeatModeSet = (mFlags & PROPERTY_REPEAT_MODE_MASK) == PROPERTY_REPEAT_MODE_MASK;
        boolean shareInterpolator = (mFlags & PROPERTY_SHARE_INTERPOLATOR_MASK) == PROPERTY_SHARE_INTERPOLATOR_MASK;
        boolean startOffsetSet = (mFlags & PROPERTY_START_OFFSET_MASK) == PROPERTY_START_OFFSET_MASK;
        if (shareInterpolator) {
            ensureInterpolator();
        }
        int count = mAnimations.size();
        long[] storedOffsets = mStoredOffsets;
        if (startOffsetSet) {
            if (storedOffsets == null || storedOffsets.length != count) {
                storedOffsets = new long[count];
                mStoredOffsets = storedOffsets;
            }
        } else if (storedOffsets != null) {
            storedOffsets = null;
            mStoredOffsets = null;
        }
        for (int i = 0; i < count; i++) {
            Animation a = mAnimations.get(i);
            if (durationSet) {
                a.setDuration(mDuration);
            }
            if (fillAfterSet) {
                a.setFillAfter(mFillAfter);
            }
            if (fillBeforeSet) {
                a.setFillBefore(mFillBefore);
            }
            if (repeatModeSet) {
                a.setRepeatMode(mRepeatMode);
            }
            if (shareInterpolator) {
                a.setInterpolator(mInterpolator);
            }
            if (startOffsetSet && storedOffsets != null) {
                long offset = a.getStartOffset();
                a.setStartOffset(offset + mStartOffset);
                storedOffsets[i] = offset;
            }
            a.initialize(width, height, parentWidth, parentHeight);
        }
    }

    @Override
    public void reset() {
        super.reset();
        restoreChildrenStartOffset();
    }

    void restoreChildrenStartOffset() {
        long[] offsets = mStoredOffsets;
        if (offsets == null) {
            return;
        }
        for (int i = 0; i < mAnimations.size() && i < offsets.length; i++) {
            mAnimations.get(i).setStartOffset(offsets[i]);
        }
    }

    public List<Animation> getAnimations() {
        return mAnimations;
    }

    @Override
    public boolean willChangeTransformationMatrix() {
        return (mFlags & PROPERTY_MORPH_MATRIX_MASK) == PROPERTY_MORPH_MATRIX_MASK;
    }

    @Override
    public boolean willChangeBounds() {
        return (mFlags & PROPERTY_CHANGE_BOUNDS_MASK) == PROPERTY_CHANGE_BOUNDS_MASK;
    }
}
