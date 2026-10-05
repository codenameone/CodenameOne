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
package android.animation;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;

import com.codename1.androidcompat.runtime.FrameDriver;

import java.util.ArrayList;
import java.util.HashMap;

/// Animates values over time on the frame clock, calling update listeners
/// each frame. Iterations, repeat modes, seeking and reversing follow
/// AOSP's arithmetic (`animateBasedOnTime`, `getCurrentIterationFraction`).
public class ValueAnimator extends Animator {

    public static final int RESTART = 1;
    public static final int REVERSE = 2;
    public static final int INFINITE = -1;

    private static final TimeInterpolator DEFAULT_INTERPOLATOR = new AccelerateDecelerateInterpolator();
    private static long sFrameDelay = 10;

    public interface AnimatorUpdateListener {
        void onAnimationUpdate(ValueAnimator animation);
    }

    PropertyValuesHolder[] mValues;
    HashMap<String, PropertyValuesHolder> mValuesMap;
    boolean mInitialized;
    private long mDuration = 300;
    private long mStartDelay;
    private TimeInterpolator mInterpolator = DEFAULT_INTERPOLATOR;
    private int mRepeatCount;
    private int mRepeatMode = RESTART;
    private ArrayList<AnimatorUpdateListener> mUpdateListeners;

    private boolean mStarted;
    private boolean mRunning;
    private boolean mStartListenersCalled;
    private boolean mReversing;
    private long mStartTime;
    private long mDelayStartTime;
    private long mPauseTime;
    private float mSeekFraction = -1;
    private float mOverallFraction;
    private float mCurrentFraction;
    private boolean mAnimationEndRequested;
    private Frame mFrame;

    public ValueAnimator() {
    }

    public static void setFrameDelay(long frameDelay) {
        sFrameDelay = frameDelay;
    }

    public static long getFrameDelay() {
        return sFrameDelay;
    }

    public static boolean areAnimatorsEnabled() {
        return true;
    }

    public static float getDurationScale() {
        return 1f;
    }

    public static ValueAnimator ofInt(int... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setIntValues(values);
        return anim;
    }

    public static ValueAnimator ofArgb(int... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setIntValues(values);
        anim.setEvaluator(ArgbEvaluator.getInstance());
        return anim;
    }

    public static ValueAnimator ofFloat(float... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setFloatValues(values);
        return anim;
    }

    public static ValueAnimator ofPropertyValuesHolder(PropertyValuesHolder... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setValues(values);
        return anim;
    }

    public static ValueAnimator ofObject(TypeEvaluator evaluator, Object... values) {
        ValueAnimator anim = new ValueAnimator();
        anim.setObjectValues(values);
        anim.setEvaluator(evaluator);
        return anim;
    }

    // ------------------------------------------------------------ values

    public void setIntValues(int... values) {
        if (values == null || values.length == 0) {
            return;
        }
        if (mValues == null || mValues.length == 0) {
            setValues(PropertyValuesHolder.ofInt("", values));
        } else {
            mValues[0].setIntValues(values);
        }
        mInitialized = false;
    }

    public void setFloatValues(float... values) {
        if (values == null || values.length == 0) {
            return;
        }
        if (mValues == null || mValues.length == 0) {
            setValues(PropertyValuesHolder.ofFloat("", values));
        } else {
            mValues[0].setFloatValues(values);
        }
        mInitialized = false;
    }

    public void setObjectValues(Object... values) {
        if (values == null || values.length == 0) {
            return;
        }
        if (mValues == null || mValues.length == 0) {
            setValues(PropertyValuesHolder.ofObject("", null, values));
        } else {
            mValues[0].setObjectValues(values);
        }
        mInitialized = false;
    }

    public void setValues(PropertyValuesHolder... values) {
        mValues = values;
        mValuesMap = new HashMap<String, PropertyValuesHolder>();
        for (PropertyValuesHolder v : values) {
            mValuesMap.put(v.getPropertyName(), v);
        }
        mInitialized = false;
    }

    public PropertyValuesHolder[] getValues() {
        return mValues;
    }

    void initAnimation() {
        if (!mInitialized) {
            if (mValues != null) {
                for (PropertyValuesHolder v : mValues) {
                    v.init();
                }
            }
            mInitialized = true;
        }
    }

    public void setEvaluator(TypeEvaluator value) {
        if (value != null && mValues != null && mValues.length > 0) {
            mValues[0].setEvaluator(value);
        }
    }

    public Object getAnimatedValue() {
        if (mValues != null && mValues.length > 0) {
            return mValues[0].getAnimatedValue();
        }
        return null;
    }

    public Object getAnimatedValue(String propertyName) {
        PropertyValuesHolder v = mValuesMap == null ? null : mValuesMap.get(propertyName);
        return v == null ? null : v.getAnimatedValue();
    }

    public float getAnimatedFraction() {
        return mCurrentFraction;
    }

    // ------------------------------------------------------------ timing

    @Override
    public ValueAnimator setDuration(long duration) {
        if (duration < 0) {
            throw new IllegalArgumentException("Animators cannot have negative duration: " + duration);
        }
        mDuration = duration;
        return this;
    }

    @Override
    public long getDuration() {
        return mDuration;
    }

    @Override
    public long getTotalDuration() {
        if (mRepeatCount == INFINITE) {
            return DURATION_INFINITE;
        }
        return mStartDelay + (mDuration * (mRepeatCount + 1));
    }

    @Override
    public long getStartDelay() {
        return mStartDelay;
    }

    @Override
    public void setStartDelay(long startDelay) {
        mStartDelay = Math.max(0, startDelay);
    }

    @Override
    public void setInterpolator(TimeInterpolator value) {
        mInterpolator = value != null ? value : new LinearInterpolator();
    }

    @Override
    public TimeInterpolator getInterpolator() {
        return mInterpolator;
    }

    public void setRepeatCount(int value) {
        mRepeatCount = value;
    }

    public int getRepeatCount() {
        return mRepeatCount;
    }

    public void setRepeatMode(int value) {
        mRepeatMode = value;
    }

    public int getRepeatMode() {
        return mRepeatMode;
    }

    public void setCurrentPlayTime(long playTime) {
        float fraction = mDuration > 0 ? (float) playTime / mDuration : 1;
        setCurrentFraction(fraction);
    }

    public void setCurrentFraction(float fraction) {
        initAnimation();
        fraction = clampFraction(fraction);
        if (mRunning) {
            mStartTime = now() - (long) (mDuration * fraction);
        } else {
            mSeekFraction = fraction;
        }
        mOverallFraction = fraction;
        animateValue(getCurrentIterationFraction(fraction, mReversing));
    }

    public long getCurrentPlayTime() {
        if (!mInitialized || (!mStarted && mSeekFraction < 0)) {
            return 0;
        }
        if (mSeekFraction >= 0) {
            return (long) (mDuration * mSeekFraction);
        }
        return now() - mStartTime;
    }

    private static long now() {
        return AnimationUtils.currentAnimationTimeMillis();
    }

    private float clampFraction(float fraction) {
        if (fraction < 0) {
            fraction = 0;
        } else if (mRepeatCount != INFINITE) {
            fraction = Math.min(fraction, mRepeatCount + 1);
        }
        return fraction;
    }

    private int getCurrentIteration(float fraction) {
        fraction = clampFraction(fraction);
        double iteration = Math.floor(fraction);
        if (fraction == iteration && fraction > 0) {
            iteration--;
        }
        return (int) iteration;
    }

    private boolean shouldPlayBackward(int iteration, boolean inReverse) {
        if (iteration > 0 && mRepeatMode == REVERSE && (iteration < (mRepeatCount + 1) || mRepeatCount == INFINITE)) {
            if (inReverse) {
                return (iteration % 2) == 0;
            }
            return (iteration % 2) != 0;
        }
        return inReverse;
    }

    private float getCurrentIterationFraction(float fraction, boolean inReverse) {
        fraction = clampFraction(fraction);
        int iteration = getCurrentIteration(fraction);
        float currentFraction = fraction - iteration;
        return shouldPlayBackward(iteration, inReverse) ? 1f - currentFraction : currentFraction;
    }

    // ------------------------------------------------------------ listeners

    public void addUpdateListener(AnimatorUpdateListener listener) {
        if (mUpdateListeners == null) {
            mUpdateListeners = new ArrayList<AnimatorUpdateListener>();
        }
        mUpdateListeners.add(listener);
    }

    public void removeUpdateListener(AnimatorUpdateListener listener) {
        if (mUpdateListeners != null) {
            mUpdateListeners.remove(listener);
            if (mUpdateListeners.isEmpty()) {
                mUpdateListeners = null;
            }
        }
    }

    public void removeAllUpdateListeners() {
        mUpdateListeners = null;
    }

    private void notifyStartListeners() {
        if (!mStartListenersCalled) {
            mStartListenersCalled = true;
            ArrayList<AnimatorListener> l = listenersCopy();
            if (l != null) {
                for (AnimatorListener a : l) {
                    a.onAnimationStart(this);
                }
            }
        }
    }

    // ------------------------------------------------------------ running

    @Override
    public boolean isRunning() {
        return mRunning;
    }

    @Override
    public boolean isStarted() {
        return mStarted;
    }

    @Override
    public boolean canReverse() {
        return true;
    }

    @Override
    public void start() {
        start(false);
    }

    private void start(boolean playBackwards) {
        mReversing = playBackwards;
        mStarted = true;
        mPaused = false;
        mRunning = false;
        mAnimationEndRequested = false;
        mStartListenersCalled = false;
        mDelayStartTime = now();
        if (mFrame == null) {
            mFrame = new Frame();
        }
        FrameDriver.add(mFrame);
        if (mStartDelay == 0 || mSeekFraction >= 0 || mReversing) {
            startAnimation(mDelayStartTime);
        }
    }

    /// The delay is over: values start moving and start listeners hear so.
    private void startAnimation(long frameTime) {
        initAnimation();
        mRunning = true;
        if (mSeekFraction >= 0) {
            mOverallFraction = mSeekFraction;
            mStartTime = frameTime - (long) (mDuration * mSeekFraction);
            mSeekFraction = -1;
        } else {
            mOverallFraction = 0f;
            mStartTime = frameTime;
        }
        notifyStartListeners();
        animateValue(getCurrentIterationFraction(mOverallFraction, mReversing));
    }

    /// One frame; answers whether the animator still runs.
    boolean doAnimationFrame(long frameTime) {
        if (mPaused) {
            return true;
        }
        if (!mRunning) {
            if (frameTime < mDelayStartTime + mStartDelay) {
                return true;
            }
            startAnimation(mDelayStartTime + mStartDelay);
        }
        boolean finished = animateBasedOnTime(frameTime);
        if (finished) {
            endAnimation();
        }
        return !finished && mStarted;
    }

    private boolean animateBasedOnTime(long currentTime) {
        boolean done = false;
        if (mRunning) {
            float fraction = mDuration > 0 ? (float) (currentTime - mStartTime) / mDuration : 1f;
            float lastFraction = mOverallFraction;
            boolean newIteration = (int) fraction > (int) lastFraction;
            boolean lastIterationFinished = (fraction >= mRepeatCount + 1) && (mRepeatCount != INFINITE);
            if (mDuration == 0) {
                done = true;
            } else if (newIteration && !lastIterationFinished) {
                ArrayList<AnimatorListener> l = listenersCopy();
                if (l != null) {
                    for (AnimatorListener a : l) {
                        a.onAnimationRepeat(this);
                    }
                }
            } else if (lastIterationFinished) {
                done = true;
            }
            mOverallFraction = clampFraction(fraction);
            animateValue(getCurrentIterationFraction(mOverallFraction, mReversing));
        }
        return done;
    }

    /// Sets every value for `fraction` (before interpolation) and calls the
    /// update listeners.
    void animateValue(float fraction) {
        fraction = mInterpolator.getInterpolation(fraction);
        mCurrentFraction = fraction;
        if (mValues != null) {
            for (PropertyValuesHolder v : mValues) {
                v.calculateValue(fraction);
            }
        }
        if (mUpdateListeners != null) {
            for (AnimatorUpdateListener l : new ArrayList<AnimatorUpdateListener>(mUpdateListeners)) {
                l.onAnimationUpdate(this);
            }
        }
    }

    private void endAnimation() {
        if (mAnimationEndRequested) {
            return;
        }
        if (mFrame != null) {
            FrameDriver.remove(mFrame);
        }
        mAnimationEndRequested = true;
        mPaused = false;
        boolean notify = (mStarted || mRunning) && mListeners != null;
        if (notify && !mRunning) {
            notifyStartListeners();
        }
        mRunning = false;
        mStarted = false;
        mStartListenersCalled = false;
        mReversing = false;
        mSeekFraction = -1;
        animationEnded();
        if (notify) {
            ArrayList<AnimatorListener> l = listenersCopy();
            for (AnimatorListener a : l) {
                a.onAnimationEnd(this);
            }
        }
    }

    /// Called once the animator has ended, however it ended: naturally,
    /// through end() or through cancel().
    void animationEnded() {
    }

    @Override
    public void cancel() {
        // As AOSP: an animator that never started is still ended, so a
        // pending start delay is dropped; only listeners need it started.
        if (mAnimationEndRequested) {
            return;
        }
        if ((mStarted || mRunning) && mListeners != null) {
            if (!mRunning) {
                notifyStartListeners();
            }
            ArrayList<AnimatorListener> l = listenersCopy();
            for (AnimatorListener a : l) {
                a.onAnimationCancel(this);
            }
        }
        endAnimation();
    }

    @Override
    public void end() {
        if (!mRunning) {
            startAnimation(now());
            mStarted = true;
        } else if (!mInitialized) {
            initAnimation();
        }
        animateValue(shouldPlayBackward(mRepeatCount, mReversing) ? 0f : 1f);
        endAnimation();
    }

    @Override
    public void pause() {
        boolean wasPaused = mPaused;
        super.pause();
        if (!wasPaused && mPaused) {
            mPauseTime = now();
        }
    }

    @Override
    public void resume() {
        if (mPaused) {
            long elapsed = now() - mPauseTime;
            mStartTime += elapsed;
            mDelayStartTime += elapsed;
        }
        super.resume();
    }

    public void reverse() {
        if (mRunning) {
            long currentTime = now();
            long currentPlayTime = currentTime - mStartTime;
            long timeLeft = mDuration - currentPlayTime;
            mStartTime = currentTime - timeLeft;
            mReversing = !mReversing;
        } else if (mStarted) {
            mReversing = !mReversing;
            end();
        } else {
            start(true);
        }
    }

    @Override
    public ValueAnimator clone() {
        ValueAnimator anim = new ValueAnimator();
        anim.copyValueAnimatorFrom(this);
        return anim;
    }

    /// Copies `o`'s configuration into this new animator, which starts out
    /// not running, as Android's clone does.
    void copyValueAnimatorFrom(ValueAnimator o) {
        copyAnimatorFrom(o);
        mDuration = o.mDuration;
        mStartDelay = o.mStartDelay;
        mInterpolator = o.mInterpolator;
        mRepeatCount = o.mRepeatCount;
        mRepeatMode = o.mRepeatMode;
        mStartTime = o.mStartTime;
        mDelayStartTime = o.mDelayStartTime;
        mPauseTime = o.mPauseTime;
        mOverallFraction = o.mOverallFraction;
        mCurrentFraction = o.mCurrentFraction;
        mUpdateListeners = o.mUpdateListeners == null ? null
                : new ArrayList<AnimatorUpdateListener>(o.mUpdateListeners);
        mSeekFraction = -1;
        mReversing = false;
        mInitialized = false;
        mStarted = false;
        mRunning = false;
        mAnimationEndRequested = false;
        mStartListenersCalled = false;
        mFrame = null;
        mValues = null;
        mValuesMap = null;
        if (o.mValues != null) {
            mValues = new PropertyValuesHolder[o.mValues.length];
            mValuesMap = new HashMap<String, PropertyValuesHolder>();
            for (int i = 0; i < o.mValues.length; i++) {
                PropertyValuesHolder h = o.mValues[i].clone();
                mValues[i] = h;
                mValuesMap.put(h.getPropertyName(), h);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ValueAnimator@").append(Integer.toHexString(hashCode()));
        if (mValues != null) {
            for (PropertyValuesHolder v : mValues) {
                sb.append("\n    ").append(v.toString());
            }
        }
        return sb.toString();
    }

    private final class Frame implements FrameDriver.FrameCallback {
        @Override
        public boolean doFrame(long frameTimeMillis) {
            return doAnimationFrame(frameTimeMillis);
        }
    }
}
