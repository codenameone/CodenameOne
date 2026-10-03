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
package com.google.android.material.bottomsheet;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

/// The state of a bottom sheet: collapsed to its peek height, expanded, or
/// hidden, and the callbacks that observe it. On Android the behavior is
/// attached to a child of a `CoordinatorLayout`; here it drives the sheet
/// of a [BottomSheetDialog], and [#from] answers for that sheet only.
public class BottomSheetBehavior<V extends View> {

    public static final int STATE_DRAGGING = 1;
    public static final int STATE_SETTLING = 2;
    public static final int STATE_EXPANDED = 3;
    public static final int STATE_COLLAPSED = 4;
    public static final int STATE_HIDDEN = 5;
    public static final int STATE_HALF_EXPANDED = 6;
    public static final int PEEK_HEIGHT_AUTO = -1;

    /// Observes the sheet's state and its position while it moves.
    public abstract static class BottomSheetCallback {
        public abstract void onStateChanged(View bottomSheet, int newState);

        public abstract void onSlide(View bottomSheet, float slideOffset);
    }

    /// What a host does when the behavior is asked to change state.
    interface Host {
        void settleTo(int state);
    }

    private final List<BottomSheetCallback> mCallbacks = new ArrayList<BottomSheetCallback>();
    private int mState = STATE_COLLAPSED;
    private int mPeekHeight = PEEK_HEIGHT_AUTO;
    private boolean mHideable;
    private boolean mSkipCollapsed;
    private boolean mDraggable = true;
    private boolean mFitToContents = true;
    private View mView;
    private Host mHost;

    public BottomSheetBehavior() {
    }

    /// The behavior of a [BottomSheetDialog]'s sheet. Any other view has
    /// none, because nothing here hosts a `CoordinatorLayout`.
    @SuppressWarnings("unchecked")
    public static <V extends View> BottomSheetBehavior<V> from(V view) {
        if (view instanceof BottomSheetDialog.SheetFrame) {
            return (BottomSheetBehavior<V>) ((BottomSheetDialog.SheetFrame) view).behavior();
        }
        throw new IllegalArgumentException("The view is not associated with BottomSheetBehavior");
    }

    void attach(View view, Host host) {
        mView = view;
        mHost = host;
    }

    public int getState() {
        return mState;
    }

    /// Moves the sheet to `state`, animated. Asking a sheet that is not
    /// hideable to hide is ignored, as on Android.
    public void setState(int state) {
        if (state == STATE_DRAGGING || state == STATE_SETTLING) {
            throw new IllegalArgumentException("STATE_" + (state == STATE_DRAGGING ? "DRAGGING" : "SETTLING")
                    + " should not be set externally.");
        }
        if (state == STATE_HIDDEN && !mHideable) {
            return;
        }
        if (state == STATE_HALF_EXPANDED) {
            state = STATE_EXPANDED;
        }
        if (mHost != null && mView != null) {
            mHost.settleTo(state);
        } else {
            mState = state;
        }
    }

    void dispatchState(int state) {
        if (mState == state) {
            return;
        }
        mState = state;
        if (mView != null) {
            for (BottomSheetCallback c : new ArrayList<BottomSheetCallback>(mCallbacks)) {
                c.onStateChanged(mView, state);
            }
        }
    }

    void dispatchSlide(float offset) {
        if (mView != null) {
            for (BottomSheetCallback c : new ArrayList<BottomSheetCallback>(mCallbacks)) {
                c.onSlide(mView, offset);
            }
        }
    }

    public void addBottomSheetCallback(BottomSheetCallback callback) {
        if (!mCallbacks.contains(callback)) {
            mCallbacks.add(callback);
        }
    }

    public void removeBottomSheetCallback(BottomSheetCallback callback) {
        mCallbacks.remove(callback);
    }

    /// Replaces every callback with `callback`.
    @Deprecated
    public void setBottomSheetCallback(BottomSheetCallback callback) {
        mCallbacks.clear();
        if (callback != null) {
            mCallbacks.add(callback);
        }
    }

    public void setPeekHeight(int peekHeight) {
        mPeekHeight = peekHeight < 0 ? PEEK_HEIGHT_AUTO : peekHeight;
        if (mView != null) {
            mView.requestLayout();
        }
    }

    public int getPeekHeight() {
        return mPeekHeight;
    }

    public void setHideable(boolean hideable) {
        mHideable = hideable;
    }

    public boolean isHideable() {
        return mHideable;
    }

    public void setSkipCollapsed(boolean skipCollapsed) {
        mSkipCollapsed = skipCollapsed;
    }

    public boolean getSkipCollapsed() {
        return mSkipCollapsed;
    }

    public void setDraggable(boolean draggable) {
        mDraggable = draggable;
    }

    public boolean isDraggable() {
        return mDraggable;
    }

    public void setFitToContents(boolean fitToContents) {
        mFitToContents = fitToContents;
    }

    public boolean isFitToContents() {
        return mFitToContents;
    }
}
