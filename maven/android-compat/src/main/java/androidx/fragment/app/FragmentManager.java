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
package androidx.fragment.app;

import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.LifecycleOwner;

import java.util.List;

/// The AndroidX fragment manager: the fragments of a `FragmentActivity`, or
/// a fragment's children. It runs on the same engine as the platform's
/// `android.app.FragmentManager`, which it extends; every method answers the
/// AndroidX types.
public abstract class FragmentManager extends android.app.FragmentManager implements FragmentResultOwner {

    /// A back stack entry.
    public interface BackStackEntry extends android.app.FragmentManager.BackStackEntry {
    }

    /// Told when the back stack changes.
    public interface OnBackStackChangedListener extends android.app.FragmentManager.OnBackStackChangedListener {
    }

    /// Told as the manager's fragments move through their lifecycle.
    public abstract static class FragmentLifecycleCallbacks {
        public void onFragmentPreAttached(FragmentManager fm, Fragment f, android.content.Context context) {
        }

        public void onFragmentAttached(FragmentManager fm, Fragment f, android.content.Context context) {
        }

        public void onFragmentPreCreated(FragmentManager fm, Fragment f, Bundle savedInstanceState) {
        }

        public void onFragmentCreated(FragmentManager fm, Fragment f, Bundle savedInstanceState) {
        }

        public void onFragmentActivityCreated(FragmentManager fm, Fragment f, Bundle savedInstanceState) {
        }

        public void onFragmentViewCreated(FragmentManager fm, Fragment f, View v, Bundle savedInstanceState) {
        }

        public void onFragmentStarted(FragmentManager fm, Fragment f) {
        }

        public void onFragmentResumed(FragmentManager fm, Fragment f) {
        }

        public void onFragmentPaused(FragmentManager fm, Fragment f) {
        }

        public void onFragmentStopped(FragmentManager fm, Fragment f) {
        }

        public void onFragmentSaveInstanceState(FragmentManager fm, Fragment f, Bundle outState) {
        }

        public void onFragmentViewDestroyed(FragmentManager fm, Fragment f) {
        }

        public void onFragmentDestroyed(FragmentManager fm, Fragment f) {
        }

        public void onFragmentDetached(FragmentManager fm, Fragment f) {
        }
    }

    FragmentManager() {
    }

    @Override
    public abstract FragmentTransaction beginTransaction();

    @Override
    public abstract Fragment findFragmentById(int id);

    @Override
    public abstract Fragment findFragmentByTag(String tag);

    @Override
    public abstract BackStackEntry getBackStackEntryAt(int index);

    @Override
    public abstract Fragment getFragment(Bundle bundle, String key);

    /// The fragments added to this manager.
    @Override
    @SuppressWarnings("unchecked")
    public abstract List<Fragment> getFragments();

    @Override
    public abstract Fragment.SavedState saveFragmentInstanceState(android.app.Fragment f);

    @Override
    public abstract Fragment getPrimaryNavigationFragment();

    public abstract void registerFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb, boolean recursive);

    public abstract void unregisterFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb);

    public abstract FragmentFactory getFragmentFactory();

    public abstract void setFragmentFactory(FragmentFactory fragmentFactory);

    public abstract void addFragmentOnAttachListener(FragmentOnAttachListener listener);

    public abstract void removeFragmentOnAttachListener(FragmentOnAttachListener listener);

    public abstract boolean isExecutingActions();

    @Override
    public abstract void setFragmentResultListener(String requestKey, LifecycleOwner lifecycleOwner,
                                                   FragmentResultListener listener);

    /// The fragment whose view contains `view`.
    @SuppressWarnings("unchecked")
    public static <F extends Fragment> F findFragment(View view) {
        for (View v = view; v != null; ) {
            Object tag = v.getTag(SupportFragmentManager.VIEW_FRAGMENT_TAG);
            if (tag instanceof Fragment) {
                return (F) tag;
            }
            Object parent = v.getParent();
            v = parent instanceof View ? (View) parent : null;
        }
        throw new IllegalStateException("View " + view + " does not have a Fragment set");
    }
}
