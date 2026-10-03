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
package androidx.lifecycle;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.codename1.androidcompat.runtime.AndroidRuntime;

/// The lifecycle of the whole application: started while any activity is
/// started, resumed while one is resumed, never destroyed. It follows the
/// activities from the moment it is first asked for, starting from the
/// state of the activity on screen then.
public final class ProcessLifecycleOwner implements LifecycleOwner {

    private static ProcessLifecycleOwner sInstance;

    private final LifecycleRegistry mRegistry = new LifecycleRegistry(this);
    private int mStartedCounter;
    private int mResumedCounter;

    private ProcessLifecycleOwner() {
    }

    public static LifecycleOwner get() {
        if (sInstance == null) {
            sInstance = new ProcessLifecycleOwner();
            sInstance.attach();
        }
        return sInstance;
    }

    @Override
    public Lifecycle getLifecycle() {
        return mRegistry;
    }

    private void attach() {
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        AndroidRuntime rt = AndroidRuntime.getInstance();
        Application app = rt == null ? null : rt.getApplication();
        if (app == null) {
            return;
        }
        Activity top = android.app.ActivityThread.getTopActivity();
        if (top != null && !top.isFinishing()) {
            mStartedCounter = 1;
            mResumedCounter = 1;
            mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
            mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        }
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
                mStartedCounter++;
                if (mStartedCounter == 1) {
                    mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {
                mResumedCounter++;
                if (mResumedCounter == 1) {
                    mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {
                if (mResumedCounter > 0) {
                    mResumedCounter--;
                }
                if (mResumedCounter == 0 && mRegistry.getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
                if (mStartedCounter > 0) {
                    mStartedCounter--;
                }
                if (mStartedCounter == 0 && mRegistry.getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                    mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }
}
