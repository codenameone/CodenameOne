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

/// A set of fragment operations applied together: committed, it runs on the
/// event dispatch thread after the current event, and added to the back
/// stack, the back key reverses it.
public abstract class FragmentTransaction {

    public static final int TRANSIT_ENTER_MASK = 0x1000;
    public static final int TRANSIT_EXIT_MASK = 0x2000;
    public static final int TRANSIT_UNSET = -1;
    public static final int TRANSIT_NONE = 0;
    public static final int TRANSIT_FRAGMENT_OPEN = 1 | TRANSIT_ENTER_MASK;
    public static final int TRANSIT_FRAGMENT_CLOSE = 2 | TRANSIT_EXIT_MASK;
    public static final int TRANSIT_FRAGMENT_FADE = 3 | TRANSIT_ENTER_MASK;

    public abstract FragmentTransaction add(Fragment fragment, String tag);

    public abstract FragmentTransaction add(int containerViewId, Fragment fragment);

    public abstract FragmentTransaction add(int containerViewId, Fragment fragment, String tag);

    public abstract FragmentTransaction replace(int containerViewId, Fragment fragment);

    public abstract FragmentTransaction replace(int containerViewId, Fragment fragment, String tag);

    public abstract FragmentTransaction remove(Fragment fragment);

    public abstract FragmentTransaction hide(Fragment fragment);

    public abstract FragmentTransaction show(Fragment fragment);

    public abstract FragmentTransaction detach(Fragment fragment);

    public abstract FragmentTransaction attach(Fragment fragment);

    public abstract FragmentTransaction setPrimaryNavigationFragment(Fragment fragment);

    public abstract boolean isEmpty();

    /// Recorded; fragment changes are not animated.
    public abstract FragmentTransaction setCustomAnimations(int enter, int exit);

    /// Recorded; fragment changes are not animated.
    public abstract FragmentTransaction setCustomAnimations(int enter, int exit, int popEnter, int popExit);

    public abstract FragmentTransaction addSharedElement(android.view.View sharedElement, String name);

    public abstract FragmentTransaction setTransition(int transit);

    public abstract FragmentTransaction setTransitionStyle(int styleRes);

    public abstract FragmentTransaction addToBackStack(String name);

    public abstract boolean isAddToBackStackAllowed();

    public abstract FragmentTransaction disallowAddToBackStack();

    public abstract FragmentTransaction setBreadCrumbTitle(int res);

    public abstract FragmentTransaction setBreadCrumbTitle(CharSequence text);

    public abstract FragmentTransaction setBreadCrumbShortTitle(int res);

    public abstract FragmentTransaction setBreadCrumbShortTitle(CharSequence text);

    public abstract FragmentTransaction setReorderingAllowed(boolean reorderingAllowed);

    public abstract FragmentTransaction runOnCommit(Runnable runnable);

    public abstract int commit();

    public abstract int commitAllowingStateLoss();

    public abstract void commitNow();

    public abstract void commitNowAllowingStateLoss();
}
