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
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The AndroidX `FragmentManager` over the platform fragment engine: every
/// call goes to the wrapped manager, and answers are narrowed to the
/// AndroidX types.
final class SupportFragmentManager extends FragmentManager {

    /// The keyed view tag holding the fragment a view belongs to.
    static final int VIEW_FRAGMENT_TAG = 0x0f7e0001;

    final FragmentManagerImpl mImpl;
    private FragmentFactory mFactory;
    private final List<FragmentOnAttachListener> mOnAttachListeners = new ArrayList<FragmentOnAttachListener>();
    private final Map<FragmentLifecycleCallbacks, Adapter> mCallbacks = new HashMap<FragmentLifecycleCallbacks, Adapter>();
    private final Map<String, Bundle> mResults = new HashMap<String, Bundle>();
    private final Map<String, ResultListener> mResultListeners = new HashMap<String, ResultListener>();

    private SupportFragmentManager(FragmentManagerImpl impl) {
        mImpl = impl;
        impl.registerFragmentLifecycleCallbacks(new android.app.FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentAttached(android.app.FragmentManager fm, android.app.Fragment f, Context context) {
                if (f instanceof Fragment && !mOnAttachListeners.isEmpty()) {
                    for (FragmentOnAttachListener l : new ArrayList<FragmentOnAttachListener>(mOnAttachListeners)) {
                        l.onAttachFragment(SupportFragmentManager.this, (Fragment) f);
                    }
                }
            }

            @Override
            public void onFragmentViewCreated(android.app.FragmentManager fm, android.app.Fragment f, View v,
                                              Bundle savedInstanceState) {
                if (v != null) {
                    v.setTag(VIEW_FRAGMENT_TAG, f);
                }
            }
        }, false);
    }

    /// The AndroidX manager presenting `impl`, created once.
    static SupportFragmentManager of(android.app.FragmentManager fm) {
        if (!(fm instanceof FragmentManagerImpl)) {
            return null;
        }
        FragmentManagerImpl impl = (FragmentManagerImpl) fm;
        Object facade = impl.getFacade();
        if (facade instanceof SupportFragmentManager) {
            return (SupportFragmentManager) facade;
        }
        SupportFragmentManager s = new SupportFragmentManager(impl);
        impl.setFacade(s);
        return s;
    }

    private static Fragment narrow(android.app.Fragment f) {
        return f instanceof Fragment ? (Fragment) f : null;
    }

    // ------------------------------------------------------------ transactions

    @Override
    public FragmentTransaction beginTransaction() {
        return new SupportTransaction(this, mImpl.beginTransaction());
    }

    @Override
    public boolean executePendingTransactions() {
        return mImpl.executePendingTransactions();
    }

    @Override
    public boolean isExecutingActions() {
        return mImpl.isExecutingActions();
    }

    // ------------------------------------------------------------ fragments

    @Override
    public Fragment findFragmentById(int id) {
        return narrow(mImpl.findFragmentById(id));
    }

    @Override
    public Fragment findFragmentByTag(String tag) {
        return narrow(mImpl.findFragmentByTag(tag));
    }

    @Override
    public void putFragment(Bundle bundle, String key, android.app.Fragment fragment) {
        mImpl.putFragment(bundle, key, fragment);
    }

    @Override
    public Fragment getFragment(Bundle bundle, String key) {
        return narrow(mImpl.getFragment(bundle, key));
    }

    @Override
    public List<Fragment> getFragments() {
        List<android.app.Fragment> all = mImpl.getFragments();
        List<Fragment> out = new ArrayList<Fragment>(all.size());
        for (android.app.Fragment f : all) {
            if (f instanceof Fragment) {
                out.add((Fragment) f);
            }
        }
        return java.util.Collections.unmodifiableList(out);
    }

    @Override
    public Fragment.SavedState saveFragmentInstanceState(android.app.Fragment f) {
        android.app.Fragment.SavedState s = mImpl.saveFragmentInstanceState(f);
        return s == null ? null : new Fragment.SavedState(s);
    }

    @Override
    public Fragment getPrimaryNavigationFragment() {
        return narrow(mImpl.getPrimaryNavigationFragment());
    }

    @Override
    public boolean isDestroyed() {
        return mImpl.isDestroyed();
    }

    @Override
    public boolean isStateSaved() {
        return mImpl.isStateSaved();
    }

    @Override
    public void invalidateOptionsMenu() {
        mImpl.invalidateOptionsMenu();
    }

    // ------------------------------------------------------------ back stack

    @Override
    public void popBackStack() {
        mImpl.popBackStack();
    }

    @Override
    public boolean popBackStackImmediate() {
        return mImpl.popBackStackImmediate();
    }

    @Override
    public void popBackStack(String name, int flags) {
        mImpl.popBackStack(name, flags);
    }

    @Override
    public boolean popBackStackImmediate(String name, int flags) {
        return mImpl.popBackStackImmediate(name, flags);
    }

    @Override
    public void popBackStack(int id, int flags) {
        mImpl.popBackStack(id, flags);
    }

    @Override
    public boolean popBackStackImmediate(int id, int flags) {
        return mImpl.popBackStackImmediate(id, flags);
    }

    @Override
    public int getBackStackEntryCount() {
        return mImpl.getBackStackEntryCount();
    }

    @Override
    public BackStackEntry getBackStackEntryAt(int index) {
        android.app.FragmentManager.BackStackEntry e = mImpl.getBackStackEntryAt(index);
        return e instanceof BackStackEntry ? (BackStackEntry) e : null;
    }

    @Override
    public void addOnBackStackChangedListener(android.app.FragmentManager.OnBackStackChangedListener listener) {
        mImpl.addOnBackStackChangedListener(listener);
    }

    @Override
    public void removeOnBackStackChangedListener(android.app.FragmentManager.OnBackStackChangedListener listener) {
        mImpl.removeOnBackStackChangedListener(listener);
    }

    // ------------------------------------------------------------ callbacks

    @Override
    public void registerFragmentLifecycleCallbacks(android.app.FragmentManager.FragmentLifecycleCallbacks cb,
                                                   boolean recursive) {
        mImpl.registerFragmentLifecycleCallbacks(cb, recursive);
    }

    @Override
    public void unregisterFragmentLifecycleCallbacks(android.app.FragmentManager.FragmentLifecycleCallbacks cb) {
        mImpl.unregisterFragmentLifecycleCallbacks(cb);
    }

    @Override
    public void registerFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb, boolean recursive) {
        Adapter a = new Adapter(cb);
        mCallbacks.put(cb, a);
        mImpl.registerFragmentLifecycleCallbacks(a, recursive);
    }

    @Override
    public void unregisterFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb) {
        Adapter a = mCallbacks.remove(cb);
        if (a != null) {
            mImpl.unregisterFragmentLifecycleCallbacks(a);
        }
    }

    /// Hands the platform engine's callbacks to an AndroidX callback.
    private static final class Adapter extends android.app.FragmentManager.FragmentLifecycleCallbacks {
        private final FragmentLifecycleCallbacks mCb;

        Adapter(FragmentLifecycleCallbacks cb) {
            mCb = cb;
        }

        private static FragmentManager fm(android.app.FragmentManager fm) {
            return of(fm);
        }

        @Override
        public void onFragmentPreAttached(android.app.FragmentManager m, android.app.Fragment f, Context c) {
            if (f instanceof Fragment) {
                mCb.onFragmentPreAttached(fm(m), (Fragment) f, c);
            }
        }

        @Override
        public void onFragmentAttached(android.app.FragmentManager m, android.app.Fragment f, Context c) {
            if (f instanceof Fragment) {
                mCb.onFragmentAttached(fm(m), (Fragment) f, c);
                mCb.onFragmentPreCreated(fm(m), (Fragment) f, null);
            }
        }

        @Override
        public void onFragmentCreated(android.app.FragmentManager m, android.app.Fragment f, Bundle saved) {
            if (f instanceof Fragment) {
                mCb.onFragmentCreated(fm(m), (Fragment) f, saved);
            }
        }

        @Override
        public void onFragmentActivityCreated(android.app.FragmentManager m, android.app.Fragment f, Bundle saved) {
            if (f instanceof Fragment) {
                mCb.onFragmentActivityCreated(fm(m), (Fragment) f, saved);
            }
        }

        @Override
        public void onFragmentViewCreated(android.app.FragmentManager m, android.app.Fragment f, View v,
                                          Bundle saved) {
            if (f instanceof Fragment) {
                mCb.onFragmentViewCreated(fm(m), (Fragment) f, v, saved);
            }
        }

        @Override
        public void onFragmentStarted(android.app.FragmentManager m, android.app.Fragment f) {
            if (f instanceof Fragment) {
                mCb.onFragmentStarted(fm(m), (Fragment) f);
            }
        }

        @Override
        public void onFragmentResumed(android.app.FragmentManager m, android.app.Fragment f) {
            if (f instanceof Fragment) {
                mCb.onFragmentResumed(fm(m), (Fragment) f);
            }
        }

        @Override
        public void onFragmentPaused(android.app.FragmentManager m, android.app.Fragment f) {
            if (f instanceof Fragment) {
                mCb.onFragmentPaused(fm(m), (Fragment) f);
            }
        }

        @Override
        public void onFragmentStopped(android.app.FragmentManager m, android.app.Fragment f) {
            if (f instanceof Fragment) {
                mCb.onFragmentStopped(fm(m), (Fragment) f);
            }
        }

        @Override
        public void onFragmentSaveInstanceState(android.app.FragmentManager m, android.app.Fragment f, Bundle out) {
            if (f instanceof Fragment) {
                mCb.onFragmentSaveInstanceState(fm(m), (Fragment) f, out);
            }
        }

        @Override
        public void onFragmentViewDestroyed(android.app.FragmentManager m, android.app.Fragment f) {
            if (f instanceof Fragment) {
                mCb.onFragmentViewDestroyed(fm(m), (Fragment) f);
            }
        }

        @Override
        public void onFragmentDestroyed(android.app.FragmentManager m, android.app.Fragment f) {
            if (f instanceof Fragment) {
                mCb.onFragmentDestroyed(fm(m), (Fragment) f);
            }
        }

        @Override
        public void onFragmentDetached(android.app.FragmentManager m, android.app.Fragment f) {
            if (f instanceof Fragment) {
                mCb.onFragmentDetached(fm(m), (Fragment) f);
            }
        }
    }

    @Override
    public void addFragmentOnAttachListener(FragmentOnAttachListener listener) {
        mOnAttachListeners.add(listener);
    }

    @Override
    public void removeFragmentOnAttachListener(FragmentOnAttachListener listener) {
        mOnAttachListeners.remove(listener);
    }

    // ------------------------------------------------------------ factory

    @Override
    public FragmentFactory getFragmentFactory() {
        if (mFactory == null) {
            if (mImpl.getParentFragment() instanceof Fragment) {
                return ((Fragment) mImpl.getParentFragment()).getParentFragmentManager().getFragmentFactory();
            }
            mFactory = new FragmentFactory();
        }
        return mFactory;
    }

    @Override
    public void setFragmentFactory(FragmentFactory fragmentFactory) {
        mFactory = fragmentFactory;
        mImpl.setInstantiator(fragmentFactory == null ? null : new FactoryInstantiator(fragmentFactory));
    }

    /// Creates the engine's fragments through an application's factory.
    private static final class FactoryInstantiator implements FragmentManagerImpl.Instantiator {
        private final FragmentFactory mFactory;

        FactoryInstantiator(FragmentFactory factory) {
            mFactory = factory;
        }

        @Override
        public android.app.Fragment instantiate(String className) {
            return mFactory.instantiate(null, className);
        }
    }

    // ------------------------------------------------------------ results

    /// A listener for one key, with the owner whose lifecycle gates it.
    private final class ResultListener implements LifecycleEventObserver {
        final String mKey;
        final LifecycleOwner mOwner;
        final FragmentResultListener mListener;

        ResultListener(String key, LifecycleOwner owner, FragmentResultListener listener) {
            mKey = key;
            mOwner = owner;
            mListener = listener;
        }

        boolean isAtLeastStarted() {
            return mOwner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
        }

        @Override
        public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_START) {
                Bundle stored = mResults.remove(mKey);
                if (stored != null) {
                    mListener.onFragmentResult(mKey, stored);
                }
            }
            if (event == Lifecycle.Event.ON_DESTROY) {
                mOwner.getLifecycle().removeObserver(this);
                if (mResultListeners.get(mKey) == this) {
                    mResultListeners.remove(mKey);
                }
            }
        }
    }

    @Override
    public void setFragmentResult(String requestKey, Bundle result) {
        ResultListener l = mResultListeners.get(requestKey);
        if (l != null && l.isAtLeastStarted()) {
            l.mListener.onFragmentResult(requestKey, result);
        } else {
            mResults.put(requestKey, result);
        }
    }

    @Override
    public void clearFragmentResult(String requestKey) {
        mResults.remove(requestKey);
    }

    @Override
    public void setFragmentResultListener(String requestKey, LifecycleOwner lifecycleOwner,
                                          FragmentResultListener listener) {
        if (lifecycleOwner.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
            return;
        }
        ResultListener old = mResultListeners.remove(requestKey);
        if (old != null) {
            old.mOwner.getLifecycle().removeObserver(old);
        }
        ResultListener l = new ResultListener(requestKey, lifecycleOwner, listener);
        mResultListeners.put(requestKey, l);
        lifecycleOwner.getLifecycle().addObserver(l);
    }

    @Override
    public void clearFragmentResultListener(String requestKey) {
        ResultListener old = mResultListeners.remove(requestKey);
        if (old != null) {
            old.mOwner.getLifecycle().removeObserver(old);
        }
    }

    @Override
    public String toString() {
        return mImpl.toString();
    }
}
