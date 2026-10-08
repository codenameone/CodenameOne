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
import android.view.animation.Interpolator;

/// Scroll and fling animations for scrolling views, with Android's spline
/// deceleration. Overscroll past the bounds is not drawn: flings stop at the
/// edge, and `springBack` returns a position that is out of range.
public class OverScroller {

    private static final int DEFAULT_DURATION = 250;

    private final ScrollerMath.Axis x;
    private final ScrollerMath.Axis y;

    public OverScroller(Context context) {
        this(context, null);
    }

    public OverScroller(Context context, Interpolator interpolator) {
        float density = context == null ? 1f : context.getResources().getDisplayMetrics().density;
        x = new ScrollerMath.Axis(density);
        y = new ScrollerMath.Axis(density);
        setInterpolator(interpolator);
    }

    public OverScroller(Context context, Interpolator interpolator, float bounceCoefficientX,
                        float bounceCoefficientY) {
        this(context, interpolator);
    }

    public OverScroller(Context context, Interpolator interpolator, float bounceCoefficientX,
                        float bounceCoefficientY, boolean flywheel) {
        this(context, interpolator);
    }

    void setInterpolator(Interpolator interpolator) {
        x.interpolator = interpolator;
        y.interpolator = interpolator;
    }

    public final void setFriction(float friction) {
        x.friction = friction;
        y.friction = friction;
    }

    public final boolean isFinished() {
        return x.finished && y.finished;
    }

    public final void forceFinished(boolean finished) {
        x.finished = finished;
        y.finished = finished;
    }

    public final int getCurrX() {
        return x.current;
    }

    public final int getCurrY() {
        return y.current;
    }

    public float getCurrVelocity() {
        return (float) Math.sqrt(x.currVelocity * x.currVelocity + y.currVelocity * y.currVelocity);
    }

    public final int getStartX() {
        return x.start;
    }

    public final int getStartY() {
        return y.start;
    }

    public final int getFinalX() {
        return x.finalPos;
    }

    public final int getFinalY() {
        return y.finalPos;
    }

    public final int getDuration() {
        return Math.max(x.duration, y.duration);
    }

    public boolean computeScrollOffset() {
        if (isFinished()) {
            return false;
        }
        x.update();
        y.update();
        return true;
    }

    public void startScroll(int startX, int startY, int dx, int dy) {
        startScroll(startX, startY, dx, dy, DEFAULT_DURATION);
    }

    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        x.startScroll(startX, dx, duration);
        y.startScroll(startY, dy, duration);
    }

    public boolean springBack(int startX, int startY, int minX, int maxX, int minY, int maxY) {
        boolean sx = x.springBack(startX, minX, maxX);
        boolean sy = y.springBack(startY, minY, maxY);
        return sx || sy;
    }

    public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY,
                      int maxY) {
        fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0);
    }

    public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY,
                      int maxY, int overX, int overY) {
        x.fling(startX, velocityX, minX, maxX);
        y.fling(startY, velocityY, minY, maxY);
    }

    public void notifyHorizontalEdgeReached(int startX, int finalX, int overX) {
    }

    public void notifyVerticalEdgeReached(int startY, int finalY, int overY) {
    }

    public boolean isOverScrolled() {
        return (!x.finished && x.mode == ScrollerMath.Axis.SCROLL && (x.current < x.min || x.current > x.max))
                || (!y.finished && y.mode == ScrollerMath.Axis.SCROLL && (y.current < y.min || y.current > y.max));
    }

    public void abortAnimation() {
        x.finish();
        y.finish();
    }

    public int timePassed() {
        final long time = ScrollerMath.Axis.now();
        final long startTime = Math.min(x.startTime, y.startTime);
        return (int) (time - startTime);
    }

    public boolean isScrollingInDirection(float xvel, float yvel) {
        final int dx = x.finalPos - x.start;
        final int dy = y.finalPos - y.start;
        return !isFinished() && sign(xvel) == sign(dx) && sign(yvel) == sign(dy);
    }

    private static int sign(float f) {
        return f > 0 ? 1 : f < 0 ? -1 : 0;
    }
}
