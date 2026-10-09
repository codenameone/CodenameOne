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

/// Estimates pointer velocity from recent movement: a least-squares line
/// through the samples of the last 100ms, which follows a finger the way
/// Android's estimator does for the single-pointer case.
public final class VelocityTracker {

    private static final int HISTORY = 20;
    private static final long HORIZON_MS = 100;

    private final float[] xs = new float[HISTORY];
    private final float[] ys = new float[HISTORY];
    private final long[] ts = new long[HISTORY];
    private int count;
    private int head;
    private float xVelocity;
    private float yVelocity;

    private VelocityTracker() {
    }

    public static VelocityTracker obtain() {
        return new VelocityTracker();
    }

    public void recycle() {
        clear();
    }

    public void clear() {
        count = 0;
        head = 0;
        xVelocity = 0;
        yVelocity = 0;
    }

    public void addMovement(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            clear();
        }
        xs[head] = event.getRawX();
        ys[head] = event.getRawY();
        ts[head] = event.getEventTime();
        head = (head + 1) % HISTORY;
        if (count < HISTORY) {
            count++;
        }
    }

    public void computeCurrentVelocity(int units) {
        computeCurrentVelocity(units, Float.MAX_VALUE);
    }

    public void computeCurrentVelocity(int units, float maxVelocity) {
        xVelocity = 0;
        yVelocity = 0;
        if (count < 2) {
            return;
        }
        int newest = (head - 1 + HISTORY) % HISTORY;
        long end = ts[newest];
        int n = 0;
        double st = 0;
        double sx = 0;
        double sy = 0;
        double stt = 0;
        double stx = 0;
        double sty = 0;
        for (int i = 0; i < count; i++) {
            int idx = (newest - i + HISTORY) % HISTORY;
            long age = end - ts[idx];
            if (age > HORIZON_MS) {
                break;
            }
            double t = -age;
            st += t;
            sx += xs[idx];
            sy += ys[idx];
            stt += t * t;
            stx += t * xs[idx];
            sty += t * ys[idx];
            n++;
        }
        if (n < 2) {
            return;
        }
        double denom = n * stt - st * st;
        if (denom == 0) {
            return;
        }
        double vx = (n * stx - st * sx) / denom;
        double vy = (n * sty - st * sy) / denom;
        xVelocity = clamp((float) (vx * units), maxVelocity);
        yVelocity = clamp((float) (vy * units), maxVelocity);
    }

    private static float clamp(float v, float max) {
        return v > max ? max : v < -max ? -max : v;
    }

    public float getXVelocity() {
        return xVelocity;
    }

    public float getYVelocity() {
        return yVelocity;
    }

    public float getXVelocity(int id) {
        return xVelocity;
    }

    public float getYVelocity(int id) {
        return yVelocity;
    }
}
