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
import android.view.ViewGroup;

import androidx.lifecycle.Lifecycle;

/// A set of fragment operations applied together, and optionally reversed
/// by the back stack. Every method answers the AndroidX transaction, so
/// calls chain as in AndroidX.
public abstract class FragmentTransaction extends android.app.FragmentTransaction {

    FragmentTransaction() {
    }

    @Override
    public abstract FragmentTransaction add(android.app.Fragment fragment, String tag);

    @Override
    public abstract FragmentTransaction add(int containerViewId, android.app.Fragment fragment);

    @Override
    public abstract FragmentTransaction add(int containerViewId, android.app.Fragment fragment, String tag);

    /// Adds `fragment` with its view in `container`.
    public abstract FragmentTransaction add(ViewGroup container, android.app.Fragment fragment, String tag);

    public abstract FragmentTransaction add(Class<? extends Fragment> fragmentClass, Bundle args, String tag);

    public abstract FragmentTransaction add(int containerViewId, Class<? extends Fragment> fragmentClass, Bundle args);

    public abstract FragmentTransaction add(int containerViewId, Class<? extends Fragment> fragmentClass, Bundle args,
                                            String tag);

    @Override
    public abstract FragmentTransaction replace(int containerViewId, android.app.Fragment fragment);

    @Override
    public abstract FragmentTransaction replace(int containerViewId, android.app.Fragment fragment, String tag);

    public abstract FragmentTransaction replace(int containerViewId, Class<? extends Fragment> fragmentClass,
                                                Bundle args);

    public abstract FragmentTransaction replace(int containerViewId, Class<? extends Fragment> fragmentClass,
                                                Bundle args, String tag);

    @Override
    public abstract FragmentTransaction remove(android.app.Fragment fragment);

    @Override
    public abstract FragmentTransaction hide(android.app.Fragment fragment);

    @Override
    public abstract FragmentTransaction show(android.app.Fragment fragment);

    @Override
    public abstract FragmentTransaction detach(android.app.Fragment fragment);

    @Override
    public abstract FragmentTransaction attach(android.app.Fragment fragment);

    @Override
    public abstract FragmentTransaction setPrimaryNavigationFragment(android.app.Fragment fragment);

    /// Caps the lifecycle state of `fragment`. Recorded; the fragment follows
    /// its manager's state.
    public abstract FragmentTransaction setMaxLifecycle(android.app.Fragment fragment, Lifecycle.State state);

    @Override
    public abstract FragmentTransaction setCustomAnimations(int enter, int exit);

    @Override
    public abstract FragmentTransaction setCustomAnimations(int enter, int exit, int popEnter, int popExit);

    @Override
    public abstract FragmentTransaction addSharedElement(View sharedElement, String name);

    @Override
    public abstract FragmentTransaction setTransition(int transit);

    @Override
    public abstract FragmentTransaction setTransitionStyle(int styleRes);

    @Override
    public abstract FragmentTransaction addToBackStack(String name);

    @Override
    public abstract FragmentTransaction disallowAddToBackStack();

    @Override
    public abstract FragmentTransaction setBreadCrumbTitle(int res);

    @Override
    public abstract FragmentTransaction setBreadCrumbTitle(CharSequence text);

    @Override
    public abstract FragmentTransaction setBreadCrumbShortTitle(int res);

    @Override
    public abstract FragmentTransaction setBreadCrumbShortTitle(CharSequence text);

    @Override
    public abstract FragmentTransaction setReorderingAllowed(boolean reorderingAllowed);

    /// Deprecated in AndroidX; the same as [#setReorderingAllowed(boolean)].
    public FragmentTransaction setAllowOptimization(boolean allowOptimization) {
        return setReorderingAllowed(allowOptimization);
    }

    @Override
    public abstract FragmentTransaction runOnCommit(Runnable runnable);
}
