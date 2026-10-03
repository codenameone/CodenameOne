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
import android.content.res.TypedArray;
import android.util.AttributeSet;

/// A ViewAnimator that flips to the next child on a timer while it is
/// attached and started.
public class ViewFlipper extends ViewAnimator {

    private static final int DEFAULT_INTERVAL = 3000;

    private int mFlipInterval = DEFAULT_INTERVAL;
    private boolean mAutoStart;
    private boolean mRunning;
    private boolean mStarted;
    private boolean mVisible;

    private final Runnable mFlipRunnable = new Runnable() {
        @Override
        public void run() {
            if (mRunning) {
                showNext();
                postDelayed(mFlipRunnable, mFlipInterval);
            }
        }
    };

    public ViewFlipper(Context context) {
        super(context);
    }

    public ViewFlipper(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.ViewFlipper);
        mFlipInterval = a.getInt(android.R.styleable.ViewFlipper_flipInterval, DEFAULT_INTERVAL);
        mAutoStart = a.getBoolean(android.R.styleable.ViewFlipper_autoStart, false);
        a.recycle();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mVisible = true;
        if (mAutoStart) {
            startFlipping();
        } else {
            updateRunning(false);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mVisible = false;
        updateRunning();
    }

    public void setFlipInterval(int milliseconds) {
        mFlipInterval = milliseconds;
    }

    public int getFlipInterval() {
        return mFlipInterval;
    }

    public void startFlipping() {
        mStarted = true;
        updateRunning();
    }

    public void stopFlipping() {
        mStarted = false;
        updateRunning();
    }

    private void updateRunning() {
        updateRunning(true);
    }

    private void updateRunning(boolean flipNow) {
        boolean running = mVisible && mStarted;
        if (running != mRunning) {
            if (running) {
                showOnly(mWhichChild, flipNow);
                postDelayed(mFlipRunnable, mFlipInterval);
            } else {
                removeCallbacks(mFlipRunnable);
            }
            mRunning = running;
        }
    }

    public boolean isFlipping() {
        return mStarted;
    }

    public void setAutoStart(boolean autoStart) {
        mAutoStart = autoStart;
    }

    public boolean isAutoStart() {
        return mAutoStart;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return ViewFlipper.class.getName();
    }
}
