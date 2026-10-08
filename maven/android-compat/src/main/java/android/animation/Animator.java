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

import java.util.ArrayList;

/// Base class of property animations.
public abstract class Animator implements Cloneable {

    public static final long DURATION_INFINITE = -1;

    public interface AnimatorListener {
        void onAnimationStart(Animator animation);

        void onAnimationEnd(Animator animation);

        void onAnimationCancel(Animator animation);

        void onAnimationRepeat(Animator animation);
    }

    public interface AnimatorPauseListener {
        void onAnimationPause(Animator animation);

        void onAnimationResume(Animator animation);
    }

    ArrayList<AnimatorListener> mListeners;
    ArrayList<AnimatorPauseListener> mPauseListeners;
    boolean mPaused;

    public abstract long getStartDelay();

    public abstract void setStartDelay(long startDelay);

    public abstract Animator setDuration(long duration);

    public abstract long getDuration();

    public abstract void setInterpolator(TimeInterpolator value);

    public TimeInterpolator getInterpolator() {
        return null;
    }

    public abstract boolean isRunning();

    public boolean isStarted() {
        return isRunning();
    }

    public long getTotalDuration() {
        long duration = getDuration();
        if (duration == DURATION_INFINITE) {
            return DURATION_INFINITE;
        }
        return getStartDelay() + duration;
    }

    public void start() {
    }

    public void cancel() {
    }

    public void end() {
    }

    public void pause() {
        if (isStarted() && !mPaused) {
            mPaused = true;
            if (mPauseListeners != null) {
                for (AnimatorPauseListener l : new ArrayList<AnimatorPauseListener>(mPauseListeners)) {
                    l.onAnimationPause(this);
                }
            }
        }
    }

    public void resume() {
        if (mPaused) {
            mPaused = false;
            if (mPauseListeners != null) {
                for (AnimatorPauseListener l : new ArrayList<AnimatorPauseListener>(mPauseListeners)) {
                    l.onAnimationResume(this);
                }
            }
        }
    }

    public boolean isPaused() {
        return mPaused;
    }

    public void addListener(AnimatorListener listener) {
        if (mListeners == null) {
            mListeners = new ArrayList<AnimatorListener>();
        }
        mListeners.add(listener);
    }

    public void removeListener(AnimatorListener listener) {
        if (mListeners != null) {
            mListeners.remove(listener);
            if (mListeners.isEmpty()) {
                mListeners = null;
            }
        }
    }

    public ArrayList<AnimatorListener> getListeners() {
        return mListeners;
    }

    public void addPauseListener(AnimatorPauseListener listener) {
        if (mPauseListeners == null) {
            mPauseListeners = new ArrayList<AnimatorPauseListener>();
        }
        mPauseListeners.add(listener);
    }

    public void removePauseListener(AnimatorPauseListener listener) {
        if (mPauseListeners != null) {
            mPauseListeners.remove(listener);
            if (mPauseListeners.isEmpty()) {
                mPauseListeners = null;
            }
        }
    }

    public void removeAllListeners() {
        mListeners = null;
        mPauseListeners = null;
    }

    public void setTarget(Object target) {
    }

    public void setupStartValues() {
    }

    public void setupEndValues() {
    }

    public boolean canReverse() {
        return false;
    }

    /// The listeners at this moment, so one may remove itself while it is
    /// being called.
    ArrayList<AnimatorListener> listenersCopy() {
        return mListeners == null ? null : new ArrayList<AnimatorListener>(mListeners);
    }

    @Override
    /// A copy of this animator. Every framework animator overrides this with
    /// an explicit copy, because Codename One's Object.clone() copies nothing
    /// (it answers null on the iOS VM). An application subclass that does not
    /// override it cannot be copied.
    public Animator clone() {
        throw new UnsupportedOperationException(getClass().getName()
                + " must override clone() to be copied; the Codename One runtime has no Object.clone()");
    }

    /// Copies the listeners every animator has into a new instance.
    void copyAnimatorFrom(Animator o) {
        mListeners = o.mListeners == null ? null : new ArrayList<AnimatorListener>(o.mListeners);
        mPauseListeners = o.mPauseListeners == null ? null : new ArrayList<AnimatorPauseListener>(o.mPauseListeners);
        mPaused = false;
    }
}
