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
package androidx.activity;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.List;

/// Routes the back key to the callbacks added to it, most recent first; the
/// fallback runs when none is enabled. An activity's dispatcher is asked
/// before the activity's own `onBackPressed` behaviour, as in AndroidX.
public final class OnBackPressedDispatcher {

    private final Runnable mFallback;
    private final List<OnBackPressedCallback> mCallbacks = new ArrayList<OnBackPressedCallback>();

    public OnBackPressedDispatcher() {
        this(null);
    }

    public OnBackPressedDispatcher(Runnable fallbackOnBackPressed) {
        mFallback = fallbackOnBackPressed;
    }

    public void addCallback(final OnBackPressedCallback callback) {
        mCallbacks.add(callback);
        callback.addRemover(new Runnable() {
            @Override
            public void run() {
                mCallbacks.remove(callback);
            }
        });
    }

    /// Adds `callback` while `owner` is at least started, and removes it for
    /// good when the owner is destroyed.
    public void addCallback(LifecycleOwner owner, final OnBackPressedCallback callback) {
        final Lifecycle lifecycle = owner.getLifecycle();
        if (lifecycle.getCurrentState() == Lifecycle.State.DESTROYED) {
            return;
        }
        final DefaultLifecycleObserver observer = new DefaultLifecycleObserver() {
            @Override
            public void onStart(LifecycleOwner o) {
                if (!mCallbacks.contains(callback)) {
                    mCallbacks.add(callback);
                }
            }

            @Override
            public void onStop(LifecycleOwner o) {
                mCallbacks.remove(callback);
            }

            @Override
            public void onDestroy(LifecycleOwner o) {
                mCallbacks.remove(callback);
                lifecycle.removeObserver(this);
            }
        };
        lifecycle.addObserver(observer);
        callback.addRemover(new Runnable() {
            @Override
            public void run() {
                mCallbacks.remove(callback);
                lifecycle.removeObserver(observer);
            }
        });
    }

    public boolean hasEnabledCallbacks() {
        for (OnBackPressedCallback c : mCallbacks) {
            if (c.isEnabled()) {
                return true;
            }
        }
        return false;
    }

    public void onBackPressed() {
        for (int i = mCallbacks.size() - 1; i >= 0; i--) {
            OnBackPressedCallback c = mCallbacks.get(i);
            if (c.isEnabled()) {
                c.handleOnBackPressed();
                return;
            }
        }
        if (mFallback != null) {
            mFallback.run();
        }
    }
}
