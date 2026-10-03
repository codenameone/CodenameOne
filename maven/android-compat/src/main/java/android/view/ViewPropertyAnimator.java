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
package android.view;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;

/// `view.animate()`: animates alpha, translation, scale and rotation
/// together on one ValueAnimator.
public class ViewPropertyAnimator {

    public interface AnimatorListenerCompat {
    }

    private static final int ALPHA = 0;
    private static final int TX = 1;
    private static final int TY = 2;
    private static final int SX = 3;
    private static final int SY = 4;
    private static final int ROT = 5;
    private static final int COUNT = 6;

    private final View view;
    private long duration = 300;
    private long startDelay;
    private TimeInterpolator interpolator = new AccelerateDecelerateInterpolator();
    private final float[] target = new float[COUNT];
    private final boolean[] set = new boolean[COUNT];
    private final boolean[] relative = new boolean[COUNT];
    private Runnable startAction;
    private Runnable endAction;
    private android.animation.Animator.AnimatorListener listener;
    private android.animation.ValueAnimator.AnimatorUpdateListener updateListener;
    private Running running;
    private boolean pendingStart;

    ViewPropertyAnimator(View view) {
        this.view = view;
    }

    public ViewPropertyAnimator setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    public long getDuration() {
        return duration;
    }

    public ViewPropertyAnimator setStartDelay(long startDelay) {
        this.startDelay = startDelay;
        return this;
    }

    public long getStartDelay() {
        return startDelay;
    }

    public ViewPropertyAnimator setInterpolator(TimeInterpolator interpolator) {
        this.interpolator = interpolator;
        return this;
    }

    public TimeInterpolator getInterpolator() {
        return interpolator;
    }

    public ViewPropertyAnimator setListener(android.animation.Animator.AnimatorListener listener) {
        this.listener = listener;
        return this;
    }

    public ViewPropertyAnimator setUpdateListener(android.animation.ValueAnimator.AnimatorUpdateListener l) {
        this.updateListener = l;
        return this;
    }

    public ViewPropertyAnimator withStartAction(Runnable r) {
        startAction = r;
        return this;
    }

    public ViewPropertyAnimator withEndAction(Runnable r) {
        endAction = r;
        return this;
    }

    public ViewPropertyAnimator withLayer() {
        return this;
    }

    private ViewPropertyAnimator to(int prop, float value, boolean by) {
        target[prop] = value;
        set[prop] = true;
        relative[prop] = by;
        scheduleStart();
        return this;
    }

    public ViewPropertyAnimator alpha(float v) {
        return to(ALPHA, v, false);
    }

    public ViewPropertyAnimator alphaBy(float v) {
        return to(ALPHA, v, true);
    }

    public ViewPropertyAnimator translationX(float v) {
        return to(TX, v, false);
    }

    public ViewPropertyAnimator translationXBy(float v) {
        return to(TX, v, true);
    }

    public ViewPropertyAnimator translationY(float v) {
        return to(TY, v, false);
    }

    public ViewPropertyAnimator translationYBy(float v) {
        return to(TY, v, true);
    }

    public ViewPropertyAnimator translationZ(float v) {
        return this;
    }

    public ViewPropertyAnimator x(float v) {
        return to(TX, v - view.getLeft(), false);
    }

    public ViewPropertyAnimator y(float v) {
        return to(TY, v - view.getTop(), false);
    }

    public ViewPropertyAnimator z(float v) {
        return this;
    }

    public ViewPropertyAnimator scaleX(float v) {
        return to(SX, v, false);
    }

    public ViewPropertyAnimator scaleXBy(float v) {
        return to(SX, v, true);
    }

    public ViewPropertyAnimator scaleY(float v) {
        return to(SY, v, false);
    }

    public ViewPropertyAnimator scaleYBy(float v) {
        return to(SY, v, true);
    }

    public ViewPropertyAnimator rotation(float v) {
        return to(ROT, v, false);
    }

    public ViewPropertyAnimator rotationBy(float v) {
        return to(ROT, v, true);
    }

    public ViewPropertyAnimator rotationX(float v) {
        return this;
    }

    public ViewPropertyAnimator rotationY(float v) {
        return this;
    }

    /// Android starts the animation on the next frame unless `start()` is
    /// called first; so do we.
    private void scheduleStart() {
        if (!pendingStart) {
            pendingStart = true;
            view.post(new Runnable() {
                @Override
                public void run() {
                    if (pendingStart) {
                        start();
                    }
                }
            });
        }
    }

    public void start() {
        pendingStart = false;
        if (running != null) {
            running.finish(false);
        }
        float[] from = current();
        float[] to = new float[COUNT];
        boolean[] which = set.clone();
        for (int i = 0; i < COUNT; i++) {
            to[i] = relative[i] ? from[i] + target[i] : target[i];
            set[i] = false;
            relative[i] = false;
        }
        Running r = new Running(from, to, which, duration, startDelay, interpolator, startAction, endAction, listener,
                updateListener);
        startAction = null;
        endAction = null;
        running = r;
        r.begin();
    }

    public void cancel() {
        pendingStart = false;
        if (running != null) {
            running.finish(true);
            running = null;
        }
    }

    private float[] current() {
        return new float[] {view.getAlpha(), view.getTranslationX(), view.getTranslationY(), view.getScaleX(),
                view.getScaleY(), view.getRotation()};
    }

    void apply(float[] values, boolean[] which) {
        if (which[ALPHA]) {
            view.setAlpha(values[ALPHA]);
        }
        if (which[TX]) {
            view.setTranslationX(values[TX]);
        }
        if (which[TY]) {
            view.setTranslationY(values[TY]);
        }
        if (which[SX]) {
            view.setScaleX(values[SX]);
        }
        if (which[SY]) {
            view.setScaleY(values[SY]);
        }
        if (which[ROT]) {
            view.setRotation(values[ROT]);
        }
    }

    /// One started batch, driven by a ValueAnimator from 0 to 1 that the
    /// listeners receive, as AOSP's does.
    private final class Running {
        final float[] from;
        final float[] to;
        final boolean[] which;
        final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        boolean done;

        Running(float[] from, float[] to, boolean[] which, long duration, long delay, TimeInterpolator interp,
                final Runnable startAction, final Runnable endAction,
                final android.animation.Animator.AnimatorListener listener,
                final ValueAnimator.AnimatorUpdateListener update) {
            this.from = from;
            this.to = to;
            this.which = which;
            animator.setDuration(duration);
            animator.setStartDelay(delay);
            if (interp != null) {
                animator.setInterpolator(interp);
            }
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float e = animation.getAnimatedFraction();
                    float[] v = new float[COUNT];
                    for (int i = 0; i < COUNT; i++) {
                        v[i] = Running.this.from[i] + (Running.this.to[i] - Running.this.from[i]) * e;
                    }
                    apply(v, Running.this.which);
                    if (update != null) {
                        update.onAnimationUpdate(animation);
                    }
                }
            });
            animator.addListener(new android.animation.Animator.AnimatorListener() {
                private boolean canceled;

                @Override
                public void onAnimationStart(android.animation.Animator animation) {
                    if (startAction != null) {
                        startAction.run();
                    }
                    if (listener != null) {
                        listener.onAnimationStart(animation);
                    }
                }

                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    done = true;
                    if (running == Running.this) {
                        running = null;
                    }
                    if (listener != null) {
                        listener.onAnimationEnd(animation);
                    }
                    if (!canceled && endAction != null) {
                        endAction.run();
                    }
                }

                @Override
                public void onAnimationCancel(android.animation.Animator animation) {
                    canceled = true;
                    if (listener != null) {
                        listener.onAnimationCancel(animation);
                    }
                }

                @Override
                public void onAnimationRepeat(android.animation.Animator animation) {
                    if (listener != null) {
                        listener.onAnimationRepeat(animation);
                    }
                }
            });
        }

        void begin() {
            animator.start();
        }

        void finish(boolean canceled) {
            if (done) {
                return;
            }
            if (canceled) {
                animator.cancel();
            } else {
                animator.end();
            }
        }
    }
}
