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

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/// Animates layout changes of a view group (`animateLayoutChanges`).
/// The configuration is recorded and the listeners are told about each
/// change, but the change itself is applied without an animation.
public class LayoutTransition {

    public static final int CHANGE_APPEARING = 0;
    public static final int CHANGE_DISAPPEARING = 1;
    public static final int APPEARING = 2;
    public static final int DISAPPEARING = 3;
    public static final int CHANGING = 4;

    public interface TransitionListener {
        void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType);

        void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType);
    }

    private final long[] mDurations = {300, 300, 300, 300, 300};
    private final long[] mStartDelays = {0, 300, 300, 0, 0};
    private final long[] mStagger = {0, 0, 0, 0, 0};
    private final TimeInterpolator[] mInterpolators = new TimeInterpolator[5];
    private final Animator[] mAnimators = new Animator[5];
    private final boolean[] mEnabled = {true, true, true, true, false};
    private List<TransitionListener> mListeners;

    public void setDuration(long duration) {
        for (int i = 0; i < mDurations.length; i++) {
            mDurations[i] = duration;
        }
    }

    public void setDuration(int transitionType, long duration) {
        mDurations[transitionType] = duration;
    }

    public long getDuration(int transitionType) {
        return mDurations[transitionType];
    }

    public void enableTransitionType(int transitionType) {
        mEnabled[transitionType] = true;
    }

    public void disableTransitionType(int transitionType) {
        mEnabled[transitionType] = false;
    }

    public boolean isTransitionTypeEnabled(int transitionType) {
        return mEnabled[transitionType];
    }

    public void setStartDelay(int transitionType, long delay) {
        mStartDelays[transitionType] = delay;
    }

    public long getStartDelay(int transitionType) {
        return mStartDelays[transitionType];
    }

    public void setStagger(int transitionType, long duration) {
        mStagger[transitionType] = duration;
    }

    public long getStagger(int transitionType) {
        return mStagger[transitionType];
    }

    public void setInterpolator(int transitionType, TimeInterpolator interpolator) {
        mInterpolators[transitionType] = interpolator;
    }

    public TimeInterpolator getInterpolator(int transitionType) {
        return mInterpolators[transitionType];
    }

    public void setAnimator(int transitionType, Animator animator) {
        mAnimators[transitionType] = animator;
    }

    public Animator getAnimator(int transitionType) {
        return mAnimators[transitionType];
    }

    /// Accepted for compatibility: layout changes are not animated, the
    /// parents' included.
    public void setAnimateParentHierarchy(boolean animateParentHierarchy) {
    }

    public boolean isChangingLayout() {
        return false;
    }

    public boolean isRunning() {
        return false;
    }

    public void addChild(ViewGroup parent, View child) {
        notifyChange(parent, child, APPEARING);
    }

    public void removeChild(ViewGroup parent, View child) {
        notifyChange(parent, child, DISAPPEARING);
    }

    public void showChild(ViewGroup parent, View child, int oldVisibility) {
        notifyChange(parent, child, APPEARING);
    }

    public void hideChild(ViewGroup parent, View child, int newVisibility) {
        notifyChange(parent, child, DISAPPEARING);
    }

    private void notifyChange(ViewGroup parent, View child, int type) {
        if (mListeners == null || !mEnabled[type]) {
            return;
        }
        for (TransitionListener l : new ArrayList<TransitionListener>(mListeners)) {
            l.startTransition(this, parent, child, type);
            l.endTransition(this, parent, child, type);
        }
    }

    public void addTransitionListener(TransitionListener listener) {
        if (mListeners == null) {
            mListeners = new ArrayList<TransitionListener>();
        }
        mListeners.add(listener);
    }

    public void removeTransitionListener(TransitionListener listener) {
        if (mListeners != null) {
            mListeners.remove(listener);
        }
    }

    public List<TransitionListener> getTransitionListeners() {
        return mListeners;
    }
}
