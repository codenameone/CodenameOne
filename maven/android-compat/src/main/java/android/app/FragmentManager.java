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
package android.app;

import android.os.Bundle;

import java.util.List;

/// Interacts with the fragments of an activity, or the child fragments of a
/// fragment. Changes are made through [FragmentTransaction]s; a committed
/// transaction runs on the event dispatch thread after the current event,
/// or at once through [#executePendingTransactions()].
public abstract class FragmentManager {

    /// Pops every entry up to and including the one named, not only those
    /// above it.
    public static final int POP_BACK_STACK_INCLUSIVE = 1;

    /// One transaction on the back stack.
    public interface BackStackEntry {
        int getId();

        String getName();

        int getBreadCrumbTitleRes();

        int getBreadCrumbShortTitleRes();

        CharSequence getBreadCrumbTitle();

        CharSequence getBreadCrumbShortTitle();
    }

    public interface OnBackStackChangedListener {
        void onBackStackChanged();
    }

    /// Callbacks for every fragment the manager moves through its lifecycle.
    public abstract static class FragmentLifecycleCallbacks {
        public void onFragmentPreAttached(FragmentManager fm, Fragment f, android.content.Context context) {
        }

        public void onFragmentAttached(FragmentManager fm, Fragment f, android.content.Context context) {
        }

        public void onFragmentCreated(FragmentManager fm, Fragment f, Bundle savedInstanceState) {
        }

        public void onFragmentActivityCreated(FragmentManager fm, Fragment f, Bundle savedInstanceState) {
        }

        public void onFragmentViewCreated(FragmentManager fm, Fragment f, android.view.View v,
                                          Bundle savedInstanceState) {
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

    public abstract FragmentTransaction beginTransaction();

    @Deprecated
    public FragmentTransaction openTransaction() {
        return beginTransaction();
    }

    public abstract boolean executePendingTransactions();

    public abstract Fragment findFragmentById(int id);

    public abstract Fragment findFragmentByTag(String tag);

    public abstract void popBackStack();

    public abstract boolean popBackStackImmediate();

    public abstract void popBackStack(String name, int flags);

    public abstract boolean popBackStackImmediate(String name, int flags);

    public abstract void popBackStack(int id, int flags);

    public abstract boolean popBackStackImmediate(int id, int flags);

    public abstract int getBackStackEntryCount();

    public abstract BackStackEntry getBackStackEntryAt(int index);

    public abstract void addOnBackStackChangedListener(OnBackStackChangedListener listener);

    public abstract void removeOnBackStackChangedListener(OnBackStackChangedListener listener);

    /// Stores `fragment` in `bundle` under `key`, to be found again with
    /// [#getFragment(Bundle, String)] after the activity is recreated.
    public abstract void putFragment(Bundle bundle, String key, Fragment fragment);

    public abstract Fragment getFragment(Bundle bundle, String key);

    /// The fragments currently added, in the order they were added.
    /// Generic so the AndroidX manager, which extends this class, can answer
    /// its own fragment type; `List<Fragment> l = getFragments()` compiles
    /// unchanged.
    public abstract <F extends Fragment> List<F> getFragments();

    public abstract Fragment.SavedState saveFragmentInstanceState(Fragment f);

    public abstract boolean isDestroyed();

    public abstract boolean isStateSaved();

    public abstract Fragment getPrimaryNavigationFragment();

    public abstract void registerFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb, boolean recursive);

    public abstract void unregisterFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb);

    public static void enableDebugLogging(boolean enabled) {
    }

    public void invalidateOptionsMenu() {
    }
}
