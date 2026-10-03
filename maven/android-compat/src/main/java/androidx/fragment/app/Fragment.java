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

import android.app.Activity;
import android.app.FragmentManagerImpl;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.lifecycle.HasDefaultViewModelProviderFactory;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryOwner;

/// The AndroidX fragment: a `Lifecycle` and a separate one for its view,
/// view models, fragment results and the Activity Result API.
///
/// It extends the platform's `android.app.Fragment` and runs on the same
/// fragment engine; the methods that answer a host, a manager or another
/// fragment answer the AndroidX types.
public class Fragment extends android.app.Fragment
        implements LifecycleOwner, ViewModelStoreOwner, HasDefaultViewModelProviderFactory,
        SavedStateRegistryOwner, ActivityResultCaller {

    /// State saved with `FragmentManager.saveFragmentInstanceState`.
    public static class SavedState extends android.app.Fragment.SavedState {
        SavedState(android.app.Fragment.SavedState other) {
            super(other);
        }
    }

    private final LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);
    private ViewLifecycleOwner mViewLifecycleOwner;
    private final MutableLiveData<LifecycleOwner> mViewLifecycleOwnerLiveData = new MutableLiveData<LifecycleOwner>();
    private final SavedStateRegistry mSavedStateRegistry = new SavedStateRegistry();
    private ViewModelStore mViewModelStore;
    private ViewModelProvider.Factory mDefaultFactory;
    private final int mContentLayoutId;

    public Fragment() {
        mContentLayoutId = 0;
    }

    /// A fragment whose view is the layout `contentLayoutId`.
    public Fragment(int contentLayoutId) {
        mContentLayoutId = contentLayoutId;
    }

    /// Deprecated in AndroidX in favour of the fragment factory.
    public static Fragment instantiate(Context context, String fname) {
        return instantiate(context, fname, null);
    }

    public static Fragment instantiate(Context context, String fname, Bundle args) {
        android.app.Fragment f = android.app.Fragment.instantiate(context, fname, args);
        if (!(f instanceof Fragment)) {
            throw new InstantiationException("Unable to instantiate fragment " + fname
                    + ": it is not an androidx.fragment.app.Fragment", null);
        }
        return (Fragment) f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mContentLayoutId != 0) {
            return inflater.inflate(mContentLayoutId, container, false);
        }
        return null;
    }

    // ------------------------------------------------------------ host

    @Override
    public FragmentActivity getActivity() {
        Activity a = super.getActivity();
        return a instanceof FragmentActivity ? (FragmentActivity) a : null;
    }

    public final FragmentActivity requireActivity() {
        FragmentActivity a = getActivity();
        if (a == null) {
            throw new IllegalStateException("Fragment " + this + " not attached to an activity.");
        }
        return a;
    }

    public final Context requireContext() {
        Context c = getContext();
        if (c == null) {
            throw new IllegalStateException("Fragment " + this + " not attached to a context.");
        }
        return c;
    }

    public final Object requireHost() {
        Object host = getHost();
        if (host == null) {
            throw new IllegalStateException("Fragment " + this + " not attached to a host.");
        }
        return host;
    }

    public final Bundle requireArguments() {
        Bundle args = getArguments();
        if (args == null) {
            throw new IllegalStateException("Fragment " + this + " does not have any arguments.");
        }
        return args;
    }

    public final View requireView() {
        View v = getView();
        if (v == null) {
            throw new IllegalStateException("Fragment " + this + " did not return a View from onCreateView() or this"
                    + " was called before onCreateView().");
        }
        return v;
    }

    // ------------------------------------------------------------ managers

    /// Deprecated in AndroidX; see [#getParentFragmentManager()].
    @Override
    public FragmentManager getFragmentManager() {
        return SupportFragmentManager.of(super.getFragmentManager());
    }

    public final FragmentManager getParentFragmentManager() {
        FragmentManager fm = getFragmentManager();
        if (fm == null) {
            throw new IllegalStateException("Fragment " + this + " not associated with a fragment manager.");
        }
        return fm;
    }

    /// Deprecated in AndroidX; see [#getParentFragmentManager()].
    public final FragmentManager requireFragmentManager() {
        return getParentFragmentManager();
    }

    @Override
    public FragmentManager getChildFragmentManager() {
        return SupportFragmentManager.of(super.getChildFragmentManager());
    }

    @Override
    public Fragment getParentFragment() {
        android.app.Fragment p = super.getParentFragment();
        return p instanceof Fragment ? (Fragment) p : null;
    }

    public final Fragment requireParentFragment() {
        Fragment p = getParentFragment();
        if (p == null) {
            Context c = getContext();
            throw new IllegalStateException("Fragment " + this + (c == null ? " is not attached to any Fragment or "
                    + "host" : " is not a child Fragment, it is directly attached to " + c));
        }
        return p;
    }

    @Override
    public Fragment getTargetFragment() {
        android.app.Fragment t = super.getTargetFragment();
        return t instanceof Fragment ? (Fragment) t : null;
    }

    // ------------------------------------------------------------ results

    public final void setFragmentResult(String requestKey, Bundle result) {
        getParentFragmentManager().setFragmentResult(requestKey, result);
    }

    public final void clearFragmentResult(String requestKey) {
        getParentFragmentManager().clearFragmentResult(requestKey);
    }

    public final void setFragmentResultListener(String requestKey, FragmentResultListener listener) {
        getParentFragmentManager().setFragmentResultListener(requestKey, this, listener);
    }

    public final void clearFragmentResultListener(String requestKey) {
        getParentFragmentManager().clearFragmentResultListener(requestKey);
    }

    /// Registers for an activity result. As in AndroidX, register while the
    /// fragment is created (a field initializer or `onCreate`) and launch
    /// once it is attached; the result arrives through the activity.
    @Override
    public final <I, O> ActivityResultLauncher<I> registerForActivityResult(
            final ActivityResultContract<I, O> contract, final ActivityResultCallback<O> callback) {
        if (mLifecycleRegistry.getCurrentState().isAtLeast(Lifecycle.State.CREATED)) {
            throw new IllegalStateException("Fragment " + this + " is attempting to registerForActivityResult after "
                    + "being created. Fragments must call registerForActivityResult() before they are created (i.e. "
                    + "initialization, onAttach(), or onCreate()).");
        }
        return new ActivityResultLauncher<I>() {
            private ActivityResultLauncher<I> mDelegate;

            @Override
            public void launch(I input, Object options) {
                if (mDelegate == null) {
                    Activity a = Fragment.super.getActivity();
                    if (!(a instanceof ComponentActivity)) {
                        throw new IllegalStateException("Fragment " + Fragment.this + " is not attached to a "
                                + "ComponentActivity; it cannot launch for a result");
                    }
                    mDelegate = ((ComponentActivity) a).registerForActivityResult(contract, callback);
                }
                mDelegate.launch(input, options);
            }

            @Override
            public void unregister() {
                if (mDelegate != null) {
                    mDelegate.unregister();
                    mDelegate = null;
                }
            }

            @Override
            public ActivityResultContract<I, ?> getContract() {
                return contract;
            }
        };
    }

    // ------------------------------------------------------------ lifecycles

    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    /// The lifecycle of the fragment's view: from `onCreateView` (once a view
    /// was returned) to `onDestroyView`.
    public LifecycleOwner getViewLifecycleOwner() {
        if (mViewLifecycleOwner == null) {
            throw new IllegalStateException("Can't access the Fragment View's LifecycleOwner for " + this
                    + " when getView() is null i.e., before onCreateView() or after onDestroyView()");
        }
        return mViewLifecycleOwner;
    }

    public LiveData<LifecycleOwner> getViewLifecycleOwnerLiveData() {
        return mViewLifecycleOwnerLiveData;
    }

    private static final class ViewLifecycleOwner implements LifecycleOwner {
        final LifecycleRegistry mRegistry = new LifecycleRegistry(this);

        @Override
        public Lifecycle getLifecycle() {
            return mRegistry;
        }

        void event(Lifecycle.Event e) {
            Lifecycle.State target = e.getTargetState();
            Lifecycle.State current = mRegistry.getCurrentState();
            if (current == Lifecycle.State.INITIALIZED && target == Lifecycle.State.DESTROYED) {
                return;
            }
            mRegistry.handleLifecycleEvent(e);
        }
    }

    @Override
    protected void onRuntimeLifecycleStep(int step) {
        switch (step) {
            case RUNTIME_STEP_CREATE:
                mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
                break;
            case RUNTIME_STEP_CREATE_VIEW:
                mViewLifecycleOwner = new ViewLifecycleOwner();
                break;
            case RUNTIME_STEP_VIEW_CREATED:
                if (getView() == null) {
                    mViewLifecycleOwner = null;
                } else {
                    mViewLifecycleOwnerLiveData.setValue(mViewLifecycleOwner);
                }
                break;
            case RUNTIME_STEP_VIEW_RESTORED:
                if (mViewLifecycleOwner != null) {
                    mViewLifecycleOwner.event(Lifecycle.Event.ON_CREATE);
                }
                break;
            case RUNTIME_STEP_START:
                mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
                if (mViewLifecycleOwner != null) {
                    mViewLifecycleOwner.event(Lifecycle.Event.ON_START);
                }
                break;
            case RUNTIME_STEP_RESUME:
                mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
                if (mViewLifecycleOwner != null) {
                    mViewLifecycleOwner.event(Lifecycle.Event.ON_RESUME);
                }
                break;
            case RUNTIME_STEP_PAUSE:
                if (mViewLifecycleOwner != null) {
                    mViewLifecycleOwner.event(Lifecycle.Event.ON_PAUSE);
                }
                mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
                break;
            case RUNTIME_STEP_STOP:
                if (mViewLifecycleOwner != null) {
                    mViewLifecycleOwner.event(Lifecycle.Event.ON_STOP);
                }
                mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
                break;
            case RUNTIME_STEP_DESTROY_VIEW:
                if (mViewLifecycleOwner != null) {
                    mViewLifecycleOwner.event(Lifecycle.Event.ON_DESTROY);
                    mViewLifecycleOwner = null;
                    mViewLifecycleOwnerLiveData.setValue(null);
                }
                break;
            case RUNTIME_STEP_DESTROY:
                if (mLifecycleRegistry.getCurrentState() != Lifecycle.State.INITIALIZED) {
                    mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
                }
                clearViewModelsIfGone();
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------ view models

    /// This fragment's view models. They survive the activity being
    /// recreated for a configuration change: they are kept, by the
    /// fragment's position in its activity, inside a view model of the
    /// activity's own store.
    @Override
    public ViewModelStore getViewModelStore() {
        if (mViewModelStore == null) {
            Activity a = super.getActivity();
            String key = FragmentManagerImpl.keyOf(this);
            if (!(a instanceof ComponentActivity) || key == null) {
                throw new IllegalStateException("Can't access ViewModels from detached fragment");
            }
            mViewModelStore = FragmentViewModelStores.of((ComponentActivity) a).storeFor(key);
        }
        return mViewModelStore;
    }

    private void clearViewModelsIfGone() {
        Activity a = super.getActivity();
        if (a != null && a.isChangingConfigurations()) {
            return;
        }
        String key = FragmentManagerImpl.keyOf(this);
        if (mViewModelStore != null || (a instanceof ComponentActivity && key != null)) {
            if (a instanceof ComponentActivity && key != null) {
                FragmentViewModelStores.of((ComponentActivity) a).clear(key);
            } else if (mViewModelStore != null) {
                mViewModelStore.clear();
            }
        }
        mViewModelStore = null;
    }

    @Override
    public ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
        if (mDefaultFactory == null) {
            Activity a = super.getActivity();
            mDefaultFactory = new SavedStateViewModelFactory(a == null ? null : a.getApplication(), this,
                    getArguments());
        }
        return mDefaultFactory;
    }

    @Override
    public SavedStateRegistry getSavedStateRegistry() {
        return mSavedStateRegistry;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (!mSavedStateRegistry.isRestored()) {
            mSavedStateRegistry.performRestore(savedInstanceState);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mSavedStateRegistry.performSave(outState);
    }
}
