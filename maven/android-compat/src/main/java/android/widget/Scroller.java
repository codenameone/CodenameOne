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

/// The legacy scroller: `startScroll` with the viscous-fluid curve, and
/// spline flings.
public class Scroller {

    private final ScrollerMath.Axis x;
    private final ScrollerMath.Axis y;

    public Scroller(Context context) {
        this(context, null);
    }

    public Scroller(Context context, Interpolator interpolator) {
        this(context, interpolator, true);
    }

    public Scroller(Context context, Interpolator interpolator, boolean flywheel) {
        float density = context == null ? 1f : context.getResources().getDisplayMetrics().density;
        x = new ScrollerMath.Axis(density);
        y = new ScrollerMath.Axis(density);
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

    public final int getDuration() {
        return Math.max(x.duration, y.duration);
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

    public boolean computeScrollOffset() {
        if (isFinished()) {
            return false;
        }
        x.update();
        y.update();
        return true;
    }

    public void startScroll(int startX, int startY, int dx, int dy) {
        startScroll(startX, startY, dx, dy, 250);
    }

    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        x.startScroll(startX, dx, duration);
        y.startScroll(startY, dy, duration);
    }

    public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY) {
        x.fling(startX, velocityX, minX, maxX);
        y.fling(startY, velocityY, minY, maxY);
    }

    public void abortAnimation() {
        x.finish();
        y.finish();
    }

    public void extendDuration(int extend) {
        x.duration = timePassed() + extend;
        y.duration = x.duration;
        x.splineDuration = Math.max(x.splineDuration, x.duration);
        y.splineDuration = Math.max(y.splineDuration, y.duration);
        x.finished = false;
        y.finished = false;
    }

    public int timePassed() {
        return (int) (ScrollerMath.Axis.now() - Math.min(x.startTime, y.startTime));
    }

    public void setFinalX(int newX) {
        x.finalPos = newX;
        x.finished = false;
    }

    public void setFinalY(int newY) {
        y.finalPos = newY;
        y.finished = false;
    }
}
