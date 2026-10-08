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

/// An animator with no values that reports elapsed time every frame.
public class TimeAnimator extends ValueAnimator {

    public interface TimeListener {
        void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime);
    }

    private TimeListener mListener;
    private long mPreviousTime = -1;
    private long mStart = -1;

    @Override
    public void start() {
        mPreviousTime = -1;
        mStart = -1;
        super.start();
    }

    @Override
    boolean doAnimationFrame(long frameTime) {
        if (!isStarted()) {
            return false;
        }
        if (isPaused()) {
            mPreviousTime = -1;
            return true;
        }
        if (mStart < 0) {
            mStart = frameTime;
        }
        if (mListener != null) {
            long totalTime = frameTime - mStart;
            long deltaTime = mPreviousTime < 0 ? 0 : frameTime - mPreviousTime;
            mPreviousTime = frameTime;
            mListener.onTimeUpdate(this, totalTime, deltaTime);
        }
        return isStarted();
    }

    public void setTimeListener(TimeListener listener) {
        mListener = listener;
    }

    @Override
    void animateValue(float fraction) {
    }

    @Override
    void initAnimation() {
    }

    @Override
    public TimeAnimator clone() {
        TimeAnimator anim = new TimeAnimator();
        anim.copyValueAnimatorFrom(this);
        anim.mListener = mListener;
        return anim;
    }
}
