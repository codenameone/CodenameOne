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

import java.util.ArrayList;

/// Tree-wide layout and draw callbacks. Global layout listeners run after
/// each layout pass of the activity's root.
public final class ViewTreeObserver {

    public interface OnGlobalLayoutListener {
        void onGlobalLayout();
    }

    public interface OnPreDrawListener {
        boolean onPreDraw();
    }

    public interface OnDrawListener {
        void onDraw();
    }

    public interface OnScrollChangedListener {
        void onScrollChanged();
    }

    public interface OnGlobalFocusChangeListener {
        void onGlobalFocusChanged(View oldFocus, View newFocus);
    }

    public interface OnTouchModeChangeListener {
        void onTouchModeChanged(boolean isInTouchMode);
    }

    public interface OnWindowFocusChangeListener {
        void onWindowFocusChanged(boolean hasFocus);
    }

    private final ArrayList<OnGlobalLayoutListener> layout = new ArrayList<OnGlobalLayoutListener>();
    private final ArrayList<OnPreDrawListener> preDraw = new ArrayList<OnPreDrawListener>();
    private final ArrayList<OnDrawListener> draw = new ArrayList<OnDrawListener>();
    private final ArrayList<OnScrollChangedListener> scroll = new ArrayList<OnScrollChangedListener>();

    public boolean isAlive() {
        return true;
    }

    public void addOnGlobalLayoutListener(OnGlobalLayoutListener l) {
        layout.add(l);
    }

    public void removeOnGlobalLayoutListener(OnGlobalLayoutListener l) {
        layout.remove(l);
    }

    @Deprecated
    public void removeGlobalOnLayoutListener(OnGlobalLayoutListener l) {
        layout.remove(l);
    }

    public void addOnPreDrawListener(OnPreDrawListener l) {
        preDraw.add(l);
    }

    public void removeOnPreDrawListener(OnPreDrawListener l) {
        preDraw.remove(l);
    }

    public void addOnDrawListener(OnDrawListener l) {
        draw.add(l);
    }

    public void removeOnDrawListener(OnDrawListener l) {
        draw.remove(l);
    }

    public void addOnScrollChangedListener(OnScrollChangedListener l) {
        scroll.add(l);
    }

    public void removeOnScrollChangedListener(OnScrollChangedListener l) {
        scroll.remove(l);
    }

    public void addOnGlobalFocusChangeListener(OnGlobalFocusChangeListener l) {
    }

    public void removeOnGlobalFocusChangeListener(OnGlobalFocusChangeListener l) {
    }

    public void addOnTouchModeChangeListener(OnTouchModeChangeListener l) {
    }

    public void removeOnTouchModeChangeListener(OnTouchModeChangeListener l) {
    }

    public void addOnWindowFocusChangeListener(OnWindowFocusChangeListener l) {
    }

    public void removeOnWindowFocusChangeListener(OnWindowFocusChangeListener l) {
    }

    public void dispatchOnGlobalLayout() {
        if (layout.isEmpty()) {
            return;
        }
        for (OnGlobalLayoutListener l : new ArrayList<OnGlobalLayoutListener>(layout)) {
            l.onGlobalLayout();
        }
    }

    public boolean dispatchOnPreDraw() {
        boolean cancel = false;
        if (!preDraw.isEmpty()) {
            for (OnPreDrawListener l : new ArrayList<OnPreDrawListener>(preDraw)) {
                cancel |= !l.onPreDraw();
            }
        }
        return cancel;
    }

    public void dispatchOnDraw() {
        if (!draw.isEmpty()) {
            for (OnDrawListener l : new ArrayList<OnDrawListener>(draw)) {
                l.onDraw();
            }
        }
    }

    public void dispatchOnScrollChanged() {
        if (!scroll.isEmpty()) {
            for (OnScrollChangedListener l : new ArrayList<OnScrollChangedListener>(scroll)) {
                l.onScrollChanged();
            }
        }
    }
}
