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

import android.app.FragmentManagerImpl;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.lifecycle.Lifecycle;

/// The AndroidX transaction over the platform engine's transaction.
final class SupportTransaction extends FragmentTransaction {

    private final SupportFragmentManager mManager;
    private final android.app.FragmentTransaction mBase;

    SupportTransaction(SupportFragmentManager manager, android.app.FragmentTransaction base) {
        mManager = manager;
        mBase = base;
    }

    private Fragment create(Class<? extends Fragment> fragmentClass, Bundle args) {
        Fragment f = mManager.getFragmentFactory().instantiate(null, fragmentClass.getName());
        if (args != null) {
            f.setArguments(args);
        }
        return f;
    }

    @Override
    public FragmentTransaction add(android.app.Fragment fragment, String tag) {
        mBase.add(fragment, tag);
        return this;
    }

    @Override
    public FragmentTransaction add(int containerViewId, android.app.Fragment fragment) {
        mBase.add(containerViewId, fragment);
        return this;
    }

    @Override
    public FragmentTransaction add(int containerViewId, android.app.Fragment fragment, String tag) {
        mBase.add(containerViewId, fragment, tag);
        return this;
    }

    @Override
    public FragmentTransaction add(ViewGroup container, android.app.Fragment fragment, String tag) {
        FragmentManagerImpl.setInflatedContainer(fragment, container);
        mBase.add(container.getId(), fragment, tag);
        return this;
    }

    @Override
    public FragmentTransaction add(Class<? extends Fragment> fragmentClass, Bundle args, String tag) {
        return add(create(fragmentClass, args), tag);
    }

    @Override
    public FragmentTransaction add(int containerViewId, Class<? extends Fragment> fragmentClass, Bundle args) {
        return add(containerViewId, create(fragmentClass, args));
    }

    @Override
    public FragmentTransaction add(int containerViewId, Class<? extends Fragment> fragmentClass, Bundle args,
                                   String tag) {
        return add(containerViewId, create(fragmentClass, args), tag);
    }

    @Override
    public FragmentTransaction replace(int containerViewId, android.app.Fragment fragment) {
        mBase.replace(containerViewId, fragment);
        return this;
    }

    @Override
    public FragmentTransaction replace(int containerViewId, android.app.Fragment fragment, String tag) {
        mBase.replace(containerViewId, fragment, tag);
        return this;
    }

    @Override
    public FragmentTransaction replace(int containerViewId, Class<? extends Fragment> fragmentClass, Bundle args) {
        return replace(containerViewId, create(fragmentClass, args));
    }

    @Override
    public FragmentTransaction replace(int containerViewId, Class<? extends Fragment> fragmentClass, Bundle args,
                                       String tag) {
        return replace(containerViewId, create(fragmentClass, args), tag);
    }

    @Override
    public FragmentTransaction remove(android.app.Fragment fragment) {
        mBase.remove(fragment);
        return this;
    }

    @Override
    public FragmentTransaction hide(android.app.Fragment fragment) {
        mBase.hide(fragment);
        return this;
    }

    @Override
    public FragmentTransaction show(android.app.Fragment fragment) {
        mBase.show(fragment);
        return this;
    }

    @Override
    public FragmentTransaction detach(android.app.Fragment fragment) {
        mBase.detach(fragment);
        return this;
    }

    @Override
    public FragmentTransaction attach(android.app.Fragment fragment) {
        mBase.attach(fragment);
        return this;
    }

    @Override
    public FragmentTransaction setPrimaryNavigationFragment(android.app.Fragment fragment) {
        mBase.setPrimaryNavigationFragment(fragment);
        return this;
    }

    @Override
    public FragmentTransaction setMaxLifecycle(android.app.Fragment fragment, Lifecycle.State state) {
        return this;
    }

    @Override
    public boolean isEmpty() {
        return mBase.isEmpty();
    }

    @Override
    public FragmentTransaction setCustomAnimations(int enter, int exit) {
        mBase.setCustomAnimations(enter, exit);
        return this;
    }

    @Override
    public FragmentTransaction setCustomAnimations(int enter, int exit, int popEnter, int popExit) {
        mBase.setCustomAnimations(enter, exit, popEnter, popExit);
        return this;
    }

    @Override
    public FragmentTransaction addSharedElement(View sharedElement, String name) {
        // Shared element transitions are not played.
        return this;
    }

    @Override
    public FragmentTransaction setTransition(int transit) {
        mBase.setTransition(transit);
        return this;
    }

    @Override
    public FragmentTransaction setTransitionStyle(int styleRes) {
        mBase.setTransitionStyle(styleRes);
        return this;
    }

    @Override
    public FragmentTransaction addToBackStack(String name) {
        mBase.addToBackStack(name);
        return this;
    }

    @Override
    public boolean isAddToBackStackAllowed() {
        return mBase.isAddToBackStackAllowed();
    }

    @Override
    public FragmentTransaction disallowAddToBackStack() {
        mBase.disallowAddToBackStack();
        return this;
    }

    @Override
    public FragmentTransaction setBreadCrumbTitle(int res) {
        mBase.setBreadCrumbTitle(res);
        return this;
    }

    @Override
    public FragmentTransaction setBreadCrumbTitle(CharSequence text) {
        mBase.setBreadCrumbTitle(text);
        return this;
    }

    @Override
    public FragmentTransaction setBreadCrumbShortTitle(int res) {
        mBase.setBreadCrumbShortTitle(res);
        return this;
    }

    @Override
    public FragmentTransaction setBreadCrumbShortTitle(CharSequence text) {
        mBase.setBreadCrumbShortTitle(text);
        return this;
    }

    @Override
    public FragmentTransaction setReorderingAllowed(boolean reorderingAllowed) {
        // The engine applies operations in order either way.
        return this;
    }

    @Override
    public FragmentTransaction runOnCommit(Runnable runnable) {
        mBase.runOnCommit(runnable);
        return this;
    }

    @Override
    public int commit() {
        return mBase.commit();
    }

    @Override
    public int commitAllowingStateLoss() {
        return mBase.commitAllowingStateLoss();
    }

    @Override
    public void commitNow() {
        mBase.commitNow();
    }

    @Override
    public void commitNowAllowingStateLoss() {
        mBase.commitNowAllowingStateLoss();
    }
}
