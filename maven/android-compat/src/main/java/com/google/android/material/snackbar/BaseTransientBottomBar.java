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
package com.google.android.material.snackbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.material.internal.MaterialAttrs;

import java.util.ArrayList;
import java.util.List;

/// A bar that slides up from the bottom of the screen for a while: the base
/// of [Snackbar]. One bar shows at a time; showing another dismisses the
/// current one.
public abstract class BaseTransientBottomBar<B extends BaseTransientBottomBar<B>> {

    public static final int LENGTH_INDEFINITE = -2;
    public static final int LENGTH_SHORT = -1;
    public static final int LENGTH_LONG = 0;
    public static final int ANIMATION_MODE_SLIDE = 0;
    public static final int ANIMATION_MODE_FADE = 1;

    private static final long SHORT_DURATION_MS = 1500;
    private static final long LONG_DURATION_MS = 2750;
    private static final long ANIMATION_MS = 250;

    /// Told when the bar appears and when it goes away, and why.
    public abstract static class BaseCallback<B> {
        public static final int DISMISS_EVENT_SWIPE = 0;
        public static final int DISMISS_EVENT_ACTION = 1;
        public static final int DISMISS_EVENT_TIMEOUT = 2;
        public static final int DISMISS_EVENT_MANUAL = 3;
        public static final int DISMISS_EVENT_CONSECUTIVE = 4;

        public void onDismissed(B transientBottomBar, int event) {
        }

        public void onShown(B transientBottomBar) {
        }
    }

    /// The bar on screen, which a newly shown bar dismisses, as Material's
    /// `SnackbarManager` does: one bar at a time across the application.
    private static final class Manager {
        static BaseTransientBottomBar<?> current;

        private Manager() {
        }

        static void replace(BaseTransientBottomBar<?> bar) {
            BaseTransientBottomBar<?> previous = current;
            current = bar;
            if (previous != null) {
                previous.dispatchDismiss(BaseCallback.DISMISS_EVENT_CONSECUTIVE);
            }
        }

        static void clear() {
            current = null;
        }
    }

    private final ViewGroup mTargetParent;
    private final FrameLayout mView;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final List<BaseCallback<B>> mCallbacks = new ArrayList<BaseCallback<B>>();
    private int mDuration;
    private View mAnchor;
    private int mAnimationMode = ANIMATION_MODE_SLIDE;
    private boolean mShown;
    private boolean mDismissing;
    private final Runnable mTimeout = new Runnable() {
        @Override
        public void run() {
            dispatchDismiss(BaseCallback.DISMISS_EVENT_TIMEOUT);
        }
    };

    protected BaseTransientBottomBar(ViewGroup parent, FrameLayout view) {
        mTargetParent = parent;
        mView = view;
    }

    @SuppressWarnings("unchecked")
    private B self() {
        return (B) this;
    }

    public Context getContext() {
        return mView.getContext();
    }

    public View getView() {
        return mView;
    }

    public B setDuration(int duration) {
        mDuration = duration;
        return self();
    }

    public int getDuration() {
        return mDuration;
    }

    public B setAnchorView(View anchor) {
        mAnchor = anchor;
        return self();
    }

    public B setAnchorView(int anchorViewId) {
        mAnchor = mTargetParent.getRootView().findViewById(anchorViewId);
        return self();
    }

    public View getAnchorView() {
        return mAnchor;
    }

    public B setAnimationMode(int animationMode) {
        mAnimationMode = animationMode;
        return self();
    }

    public int getAnimationMode() {
        return mAnimationMode;
    }

    public B addCallback(BaseCallback<B> callback) {
        if (callback != null) {
            mCallbacks.add(callback);
        }
        return self();
    }

    public B removeCallback(BaseCallback<B> callback) {
        mCallbacks.remove(callback);
        return self();
    }

    public boolean isShown() {
        return mShown && !mDismissing;
    }

    public boolean isShownOrQueued() {
        return mShown;
    }

    public void show() {
        if (Manager.current == this) {
            restartTimeout();
            return;
        }
        Manager.replace(this);
        showView();
    }

    public void dismiss() {
        dispatchDismiss(BaseCallback.DISMISS_EVENT_MANUAL);
    }

    private void restartTimeout() {
        mHandler.removeCallbacks(mTimeout);
        if (mDuration != LENGTH_INDEFINITE) {
            long ms = mDuration == LENGTH_SHORT ? SHORT_DURATION_MS
                    : mDuration == LENGTH_LONG ? LONG_DURATION_MS : mDuration;
            mHandler.postDelayed(mTimeout, ms);
        }
    }

    /// The bar is a floating card 8dp above the bottom of its parent, or
    /// above its anchor view.
    private void showView() {
        if (mView.getParent() instanceof ViewGroup) {
            ((ViewGroup) mView.getParent()).removeView(mView);
        }
        int margin = MaterialAttrs.dpi(getContext(), 8);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.setMargins(margin, margin, margin, margin + anchorOffset());
        mShown = true;
        mDismissing = false;
        if (mTargetParent instanceof FrameLayout) {
            mTargetParent.addView(mView, lp);
        } else {
            mTargetParent.addView(mView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        mView.setAlpha(0f);
        mView.post(new Runnable() {
            @Override
            public void run() {
                if (mAnimationMode == ANIMATION_MODE_SLIDE) {
                    mView.setAlpha(1f);
                    mView.setTranslationY(mView.getHeight() + MaterialAttrs.dp(getContext(), 8));
                    mView.animate().translationY(0f).setDuration(ANIMATION_MS).setListener(null).start();
                } else {
                    mView.animate().alpha(1f).setDuration(ANIMATION_MS).setListener(null).start();
                }
                for (BaseCallback<B> c : new ArrayList<BaseCallback<B>>(mCallbacks)) {
                    c.onShown(self());
                }
                restartTimeout();
            }
        });
    }

    private int anchorOffset() {
        if (mAnchor == null || mAnchor.getVisibility() != View.VISIBLE) {
            return 0;
        }
        int[] anchor = new int[2];
        int[] parent = new int[2];
        mAnchor.getLocationOnScreen(anchor);
        mTargetParent.getLocationOnScreen(parent);
        int parentBottom = parent[1] + mTargetParent.getHeight();
        return Math.max(0, parentBottom - anchor[1]);
    }

    void dispatchDismiss(final int event) {
        if (!mShown || mDismissing) {
            return;
        }
        mDismissing = true;
        mHandler.removeCallbacks(mTimeout);
        if (Manager.current == this) {
            Manager.clear();
        }
        Animator.AnimatorListener done = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mView.getParent() instanceof ViewGroup) {
                    ((ViewGroup) mView.getParent()).removeView(mView);
                }
                mShown = false;
                mDismissing = false;
                for (BaseCallback<B> c : new ArrayList<BaseCallback<B>>(mCallbacks)) {
                    c.onDismissed(self(), event);
                }
            }
        };
        if (mAnimationMode == ANIMATION_MODE_SLIDE) {
            mView.animate().translationY(mView.getHeight() + MaterialAttrs.dp(getContext(), 8))
                    .setDuration(ANIMATION_MS).setListener(done).start();
        } else {
            mView.animate().alpha(0f).setDuration(ANIMATION_MS).setListener(done).start();
        }
    }
}
