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

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AndroidRuntimeException;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.codename1.androidcompat.runtime.AndroidRuntime;
import com.codename1.androidcompat.runtime.FragmentFactory;

/// A piece of an activity's user interface with its own lifecycle, added and
/// removed through a [FragmentManager].
///
/// The lifecycle runs in Android's order and follows the activity's: attach,
/// create, create view, activity created, start, resume; then pause, stop,
/// destroy view, destroy, detach. A fragment removed in a transaction that is
/// on the back stack only loses its view, and gets it back when the
/// transaction is popped.
public class Fragment implements View.OnCreateContextMenuListener {

    static final int INITIALIZING = 0;
    static final int CREATED = 1;
    static final int ACTIVITY_CREATED = 2;
    static final int STARTED = 3;
    static final int RESUMED = 4;

    static final String TARGET_STATE_TAG = "android:target_state";
    static final String TARGET_REQUEST_CODE_STATE_TAG = "android:target_req_state";
    static final String USER_VISIBLE_HINT_TAG = "android:user_visible_hint";
    static final String VIEW_STATE_TAG = "android:view_state";
    static final String CHILD_FRAGMENTS_TAG = "android:fragments";

    int mState = INITIALIZING;
    Bundle mSavedFragmentState;
    android.util.SparseArray<Parcelable> mSavedViewState;
    int mIndex = -1;
    Bundle mArguments;
    Fragment mTarget;
    int mTargetIndex = -1;
    int mTargetRequestCode;
    boolean mAdded;
    boolean mRemoving;
    boolean mFromLayout;
    boolean mInLayout;
    boolean mRestored;
    int mBackStackNesting;

    FragmentManagerImpl mFragmentManager;
    Activity mActivity;
    FragmentManagerImpl mChildFragmentManager;
    Fragment mParentFragment;

    int mFragmentId;
    int mContainerId;
    String mTag;
    boolean mHidden;
    boolean mDetached;
    boolean mRetainInstance;
    boolean mRetaining;
    boolean mHasMenu;
    boolean mMenuVisible = true;
    boolean mCalled;
    boolean mUserVisibleHint = true;

    ViewGroup mContainer;
    /// The container a `FragmentContainerView` supplied while it was being
    /// inflated, before it can be found by id.
    ViewGroup mInflatedContainer;
    View mView;
    LayoutInflater mLayoutInflater;

    /// Lifecycle steps reported to [#onRuntimeLifecycleStep(int)].
    protected static final int RUNTIME_STEP_CREATE = 1;
    protected static final int RUNTIME_STEP_CREATE_VIEW = 2;
    protected static final int RUNTIME_STEP_VIEW_CREATED = 3;
    protected static final int RUNTIME_STEP_VIEW_RESTORED = 4;
    protected static final int RUNTIME_STEP_START = 5;
    protected static final int RUNTIME_STEP_RESUME = 6;
    protected static final int RUNTIME_STEP_PAUSE = 7;
    protected static final int RUNTIME_STEP_STOP = 8;
    protected static final int RUNTIME_STEP_DESTROY_VIEW = 9;
    protected static final int RUNTIME_STEP_DESTROY = 10;

    /// Runtime use, not Android API: called as the fragment moves through its
    /// lifecycle, after the step's callback when moving up and before it when
    /// moving down, as AndroidX dispatches its `Lifecycle` events. The AndroidX
    /// fragment drives its lifecycles from here.
    protected void onRuntimeLifecycleStep(int step) {
    }

    /// State saved with [FragmentManager#saveFragmentInstanceState(Fragment)]
    /// and handed back through [#setInitialSavedState(SavedState)].
    public static class SavedState implements Parcelable {
        final Bundle mState;

        SavedState(Bundle state) {
            mState = state;
        }

        /// A copy of `other`, for the AndroidX fragment's own saved state.
        protected SavedState(SavedState other) {
            mState = other == null ? null : other.mState;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeBundle(mState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in.readBundle());
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    /// Thrown when a fragment cannot be created by name.
    public static class InstantiationException extends AndroidRuntimeException {
        public InstantiationException(String msg, Exception cause) {
            super(msg, cause);
        }
    }

    public Fragment() {
    }

    /// Creates the fragment class `fname` names, through the factory the
    /// build generates from the application's compiled classes (there is no
    /// reflection): the class must be public and have a public constructor
    /// taking no arguments, as on Android.
    public static Fragment instantiate(Context context, String fname) {
        return instantiate(context, fname, null);
    }

    public static Fragment instantiate(Context context, String fname, Bundle args) {
        AndroidRuntime rt = AndroidRuntime.getInstance();
        Object o = rt != null && rt.getApp() != null ? rt.getApp().instantiateFragment(fname)
                : FragmentFactory.instantiate(fname);
        if (!(o instanceof Fragment)) {
            throw new InstantiationException("Unable to instantiate fragment " + fname
                    + ": make sure class name exists, is public, and has an empty constructor that is public", null);
        }
        Fragment f = (Fragment) o;
        if (args != null) {
            f.mArguments = args;
        }
        return f;
    }

    final void setIndex(int index) {
        mIndex = index;
    }

    final boolean isInBackStack() {
        return mBackStackNesting > 0;
    }

    /// Back to a fresh instance's state, when the manager forgets the
    /// fragment; the application may add it again.
    final void initState() {
        mSavedViewState = null;
        mIndex = -1;
        mAdded = false;
        mRemoving = false;
        mFromLayout = false;
        mInLayout = false;
        mRestored = false;
        mBackStackNesting = 0;
        mFragmentManager = null;
        mChildFragmentManager = null;
        mActivity = null;
        mParentFragment = null;
        mFragmentId = 0;
        mContainerId = 0;
        mInflatedContainer = null;
        mTag = null;
        mHidden = false;
        mDetached = false;
        mRetaining = false;
    }

    @Override
    public final boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        String name = getClass().getName();
        int dot = name.lastIndexOf('.');
        sb.append(dot < 0 ? name : name.substring(dot + 1));
        sb.append('{').append(Integer.toHexString(System.identityHashCode(this)));
        if (mIndex >= 0) {
            sb.append(" #").append(mIndex);
        }
        if (mFragmentId != 0) {
            sb.append(" id=0x").append(Integer.toHexString(mFragmentId));
        }
        if (mTag != null) {
            sb.append(' ').append(mTag);
        }
        sb.append('}');
        return sb.toString();
    }

    // ------------------------------------------------------------ identity

    public final int getId() {
        return mFragmentId;
    }

    public final String getTag() {
        return mTag;
    }

    /// The arguments this fragment was created with. Only settable before the
    /// fragment is added (or while its state is not saved), as on Android.
    public void setArguments(Bundle args) {
        if (mIndex >= 0 && isStateSaved()) {
            throw new IllegalStateException("Fragment already active");
        }
        mArguments = args;
    }

    public final Bundle getArguments() {
        return mArguments;
    }

    public final boolean isStateSaved() {
        return mFragmentManager != null && mFragmentManager.isStateSaved();
    }

    public void setInitialSavedState(SavedState state) {
        if (mIndex >= 0) {
            throw new IllegalStateException("Fragment already active");
        }
        mSavedFragmentState = state != null && state.mState != null ? state.mState : null;
    }

    public void setTargetFragment(Fragment fragment, int requestCode) {
        mTarget = fragment;
        mTargetRequestCode = requestCode;
    }

    public Fragment getTargetFragment() {
        return mTarget;
    }

    public final int getTargetRequestCode() {
        return mTargetRequestCode;
    }

    // ------------------------------------------------------------ host

    public Context getContext() {
        return mActivity;
    }

    public Activity getActivity() {
        return mActivity;
    }

    public final Object getHost() {
        return mActivity;
    }

    private Activity requireHost() {
        if (mActivity == null) {
            throw new IllegalStateException("Fragment " + this + " not attached to Activity");
        }
        return mActivity;
    }

    public final Resources getResources() {
        return requireHost().getResources();
    }

    public final CharSequence getText(int resId) {
        return getResources().getText(resId);
    }

    public final String getString(int resId) {
        return getResources().getString(resId);
    }

    public final String getString(int resId, Object... formatArgs) {
        return getResources().getString(resId, formatArgs);
    }

    public FragmentManager getFragmentManager() {
        return mFragmentManager;
    }

    /// The manager of this fragment's own child fragments.
    public FragmentManager getChildFragmentManager() {
        if (mChildFragmentManager == null) {
            instantiateChildFragmentManager();
            if (mState >= RESUMED) {
                mChildFragmentManager.dispatchResume();
            } else if (mState >= STARTED) {
                mChildFragmentManager.dispatchStart();
            } else if (mState >= ACTIVITY_CREATED) {
                mChildFragmentManager.dispatchActivityCreated();
            } else if (mState >= CREATED) {
                mChildFragmentManager.dispatchCreate();
            }
        }
        return mChildFragmentManager;
    }

    final void instantiateChildFragmentManager() {
        mChildFragmentManager = new FragmentManagerImpl(mActivity, this);
    }

    public Fragment getParentFragment() {
        return mParentFragment;
    }

    public final boolean isAdded() {
        return mActivity != null && mAdded;
    }

    public final boolean isDetached() {
        return mDetached;
    }

    public final boolean isRemoving() {
        return mRemoving;
    }

    public final boolean isInLayout() {
        return mInLayout;
    }

    public final boolean isResumed() {
        return mState >= RESUMED;
    }

    public final boolean isVisible() {
        return isAdded() && !isHidden() && mView != null && mView.isAttachedToWindow()
                && mView.getVisibility() == View.VISIBLE;
    }

    public final boolean isHidden() {
        return mHidden;
    }

    public void onHiddenChanged(boolean hidden) {
    }

    /// Keeps this instance across the activity being recreated for a
    /// configuration change: it is detached and attached again, but neither
    /// destroyed nor created a second time.
    public void setRetainInstance(boolean retain) {
        mRetainInstance = retain;
    }

    public final boolean getRetainInstance() {
        return mRetainInstance;
    }

    public void setHasOptionsMenu(boolean hasMenu) {
        if (mHasMenu != hasMenu) {
            mHasMenu = hasMenu;
            if (isAdded() && !isHidden()) {
                mActivity.invalidateOptionsMenu();
            }
        }
    }

    public void setMenuVisibility(boolean menuVisible) {
        if (mMenuVisible != menuVisible) {
            mMenuVisible = menuVisible;
            if (mHasMenu && isAdded() && !isHidden()) {
                mActivity.invalidateOptionsMenu();
            }
        }
    }

    public void setUserVisibleHint(boolean isVisibleToUser) {
        mUserVisibleHint = isVisibleToUser;
    }

    public boolean getUserVisibleHint() {
        return mUserVisibleHint;
    }

    // ------------------------------------------------------------ activities

    public void startActivity(Intent intent) {
        startActivity(intent, null);
    }

    public void startActivity(Intent intent, Bundle options) {
        requireHost().startActivityFromFragment(this, intent, -1, options);
    }

    public void startActivityForResult(Intent intent, int requestCode) {
        startActivityForResult(intent, requestCode, null);
    }

    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        requireHost().startActivityFromFragment(this, intent, requestCode, options);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    public final void requestPermissions(final String[] permissions, final int requestCode) {
        requireHost();
        com.codename1.ui.CN.callSerially(new Runnable() {
            @Override
            public void run() {
                onRequestPermissionsResult(requestCode, permissions, new int[permissions.length]);
            }
        });
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    }

    public boolean shouldShowRequestPermissionRationale(String permission) {
        return false;
    }

    // ------------------------------------------------------------ inflation

    /// The inflater for this fragment's views: the activity's, with this
    /// fragment's child manager handling `<fragment>` tags in them.
    public LayoutInflater onGetLayoutInflater(Bundle savedInstanceState) {
        LayoutInflater result = requireHost().getLayoutInflater().cloneInContext(mActivity);
        getChildFragmentManager();
        result.setPrivateFactory(mChildFragmentManager);
        return result;
    }

    public final LayoutInflater getLayoutInflater() {
        if (mLayoutInflater == null) {
            return performGetLayoutInflater(null);
        }
        return mLayoutInflater;
    }

    final LayoutInflater performGetLayoutInflater(Bundle savedInstanceState) {
        mLayoutInflater = onGetLayoutInflater(savedInstanceState);
        return mLayoutInflater;
    }

    /// Called when the fragment is created from a `<fragment>` tag, with the
    /// tag's attributes.
    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        onInflate(attrs, savedInstanceState);
        mCalled = true;
        if (context instanceof Activity) {
            mCalled = false;
            onInflate((Activity) context, attrs, savedInstanceState);
        }
    }

    @Deprecated
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        mCalled = true;
    }

    @Deprecated
    public void onInflate(AttributeSet attrs, Bundle savedInstanceState) {
        mCalled = true;
    }

    // ------------------------------------------------------------ lifecycle

    public void onAttachFragment(Fragment childFragment) {
    }

    public void onAttach(Context context) {
        mCalled = true;
        if (mActivity != null) {
            mCalled = false;
            onAttach(mActivity);
        }
    }

    @Deprecated
    public void onAttach(Activity activity) {
        mCalled = true;
    }

    /// Restores the child fragments saved with this fragment, so they come
    /// back with it; subclasses must call through.
    public void onCreate(Bundle savedInstanceState) {
        mCalled = true;
        restoreChildFragmentState(savedInstanceState);
        if (mChildFragmentManager != null && !mChildFragmentManager.isStateAtLeast(CREATED)) {
            mChildFragmentManager.dispatchCreate();
        }
    }

    final void restoreChildFragmentState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        Bundle p = savedInstanceState.getBundle(CHILD_FRAGMENTS_TAG);
        if (p != null) {
            if (mChildFragmentManager == null) {
                instantiateChildFragmentManager();
            }
            mChildFragmentManager.restoreAllState(p, null);
            mChildFragmentManager.dispatchCreate();
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return null;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
    }

    public View getView() {
        return mView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        mCalled = true;
    }

    public void onViewStateRestored(Bundle savedInstanceState) {
        mCalled = true;
    }

    public void onStart() {
        mCalled = true;
    }

    public void onResume() {
        mCalled = true;
    }

    public void onSaveInstanceState(Bundle outState) {
    }

    public void onConfigurationChanged(Configuration newConfig) {
        mCalled = true;
    }

    public void onPause() {
        mCalled = true;
    }

    public void onStop() {
        mCalled = true;
    }

    public void onLowMemory() {
        mCalled = true;
    }

    public void onTrimMemory(int level) {
        mCalled = true;
    }

    public void onDestroyView() {
        mCalled = true;
    }

    public void onDestroy() {
        mCalled = true;
    }

    public void onDetach() {
        mCalled = true;
    }

    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
    }

    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
    }

    // ------------------------------------------------------------ menus

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    }

    public void onPrepareOptionsMenu(Menu menu) {
    }

    public void onDestroyOptionsMenu() {
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    public void onOptionsMenuClosed(Menu menu) {
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        requireHost().onCreateContextMenu(menu, v, menuInfo);
    }

    public void registerForContextMenu(View view) {
        view.setOnCreateContextMenuListener(this);
    }

    public void unregisterForContextMenu(View view) {
        view.setOnCreateContextMenuListener(null);
    }

    public boolean onContextItemSelected(MenuItem item) {
        return false;
    }

    // ------------------------------------------------------------ driven by the manager

    private void check(String what) {
        if (!mCalled) {
            throw new SuperNotCalledException("Fragment " + this + " did not call through to super." + what + "()");
        }
    }

    final void performCreate(Bundle savedInstanceState) {
        mState = CREATED;
        mCalled = false;
        onCreate(savedInstanceState);
        check("onCreate");
        onRuntimeLifecycleStep(RUNTIME_STEP_CREATE);
    }

    final View performCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        onRuntimeLifecycleStep(RUNTIME_STEP_CREATE_VIEW);
        View v = onCreateView(inflater, container, savedInstanceState);
        mView = v;
        onRuntimeLifecycleStep(RUNTIME_STEP_VIEW_CREATED);
        return v;
    }

    final void performActivityCreated(Bundle savedInstanceState) {
        mState = ACTIVITY_CREATED;
        mCalled = false;
        onActivityCreated(savedInstanceState);
        check("onActivityCreated");
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchActivityCreated();
        }
    }

    final void restoreViewState(Bundle savedInstanceState) {
        if (mSavedViewState != null) {
            mView.restoreHierarchyState(mSavedViewState);
            mSavedViewState = null;
        }
        mCalled = false;
        onViewStateRestored(savedInstanceState);
        check("onViewStateRestored");
        onRuntimeLifecycleStep(RUNTIME_STEP_VIEW_RESTORED);
    }

    final void performStart() {
        if (mChildFragmentManager != null) {
            mChildFragmentManager.noteStateNotSaved();
            mChildFragmentManager.execPendingActions();
        }
        mState = STARTED;
        mCalled = false;
        onStart();
        check("onStart");
        onRuntimeLifecycleStep(RUNTIME_STEP_START);
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchStart();
        }
    }

    final void performResume() {
        if (mChildFragmentManager != null) {
            mChildFragmentManager.noteStateNotSaved();
            mChildFragmentManager.execPendingActions();
        }
        mState = RESUMED;
        mCalled = false;
        onResume();
        check("onResume");
        onRuntimeLifecycleStep(RUNTIME_STEP_RESUME);
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchResume();
            mChildFragmentManager.execPendingActions();
        }
    }

    final void performPause() {
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchPause();
        }
        onRuntimeLifecycleStep(RUNTIME_STEP_PAUSE);
        mState = STARTED;
        mCalled = false;
        onPause();
        check("onPause");
    }

    final void performStop() {
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchStop();
        }
        onRuntimeLifecycleStep(RUNTIME_STEP_STOP);
        mState = ACTIVITY_CREATED;
        mCalled = false;
        onStop();
        check("onStop");
    }

    final void performDestroyView() {
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchDestroyView();
        }
        onRuntimeLifecycleStep(RUNTIME_STEP_DESTROY_VIEW);
        mState = CREATED;
        mCalled = false;
        onDestroyView();
        check("onDestroyView");
    }

    final void performDestroy() {
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchDestroy();
        }
        onRuntimeLifecycleStep(RUNTIME_STEP_DESTROY);
        mState = INITIALIZING;
        mCalled = false;
        onDestroy();
        check("onDestroy");
        mChildFragmentManager = null;
    }

    final void performDetach() {
        mCalled = false;
        onDetach();
        mLayoutInflater = null;
        check("onDetach");
        if (mChildFragmentManager != null) {
            if (!mRetaining) {
                throw new IllegalStateException("Child FragmentManager of " + this + " was not destroyed and this "
                        + "fragment is not retaining instance");
            }
            mChildFragmentManager.dispatchDestroy();
            mChildFragmentManager = null;
        }
    }

    final void performConfigurationChanged(Configuration newConfig) {
        onConfigurationChanged(newConfig);
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchConfigurationChanged(newConfig);
        }
    }

    final void performLowMemory() {
        onLowMemory();
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchLowMemory();
        }
    }

    final boolean performCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        boolean show = false;
        if (!mHidden) {
            if (mHasMenu && mMenuVisible) {
                show = true;
                onCreateOptionsMenu(menu, inflater);
            }
            if (mChildFragmentManager != null) {
                show |= mChildFragmentManager.dispatchCreateOptionsMenu(menu, inflater);
            }
        }
        return show;
    }

    final boolean performPrepareOptionsMenu(Menu menu) {
        boolean show = false;
        if (!mHidden) {
            if (mHasMenu && mMenuVisible) {
                show = true;
                onPrepareOptionsMenu(menu);
            }
            if (mChildFragmentManager != null) {
                show |= mChildFragmentManager.dispatchPrepareOptionsMenu(menu);
            }
        }
        return show;
    }

    final boolean performOptionsItemSelected(MenuItem item) {
        if (!mHidden) {
            if (mHasMenu && mMenuVisible && onOptionsItemSelected(item)) {
                return true;
            }
            if (mChildFragmentManager != null && mChildFragmentManager.dispatchOptionsItemSelected(item)) {
                return true;
            }
        }
        return false;
    }

    final boolean performContextItemSelected(MenuItem item) {
        if (!mHidden) {
            if (onContextItemSelected(item)) {
                return true;
            }
            if (mChildFragmentManager != null && mChildFragmentManager.dispatchContextItemSelected(item)) {
                return true;
            }
        }
        return false;
    }

    final void performOptionsMenuClosed(Menu menu) {
        if (!mHidden) {
            if (mHasMenu && mMenuVisible) {
                onOptionsMenuClosed(menu);
            }
            if (mChildFragmentManager != null) {
                mChildFragmentManager.dispatchOptionsMenuClosed(menu);
            }
        }
    }

    final Bundle performSaveInstanceState() {
        Bundle out = new Bundle();
        onSaveInstanceState(out);
        if (mChildFragmentManager != null) {
            Bundle p = mChildFragmentManager.saveAllState();
            if (p != null) {
                out.putBundle(CHILD_FRAGMENTS_TAG, p);
            }
        }
        return out;
    }
}
