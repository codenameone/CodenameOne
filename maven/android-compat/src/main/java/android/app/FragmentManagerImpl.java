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
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// The fragment manager of an activity, or of a fragment's children: moves
/// each fragment through its lifecycle to the state its host is in, runs
/// transactions, keeps the back stack, and saves and restores all of it
/// when the activity is recreated.
///
/// It is also the inflater factory for `<fragment>` layout tags. Everything
/// runs on the event dispatch thread.
///
/// Public for the runtime only, not Android API: the AndroidX fragment
/// library runs on this same engine (its fragments extend the platform's),
/// with a second instance attached to a `FragmentActivity`.
public final class FragmentManagerImpl extends FragmentManager implements LayoutInflater.Factory2 {

    static final String TAG_ACTIVE_COUNT = "active";
    static final String TAG_ADDED = "added";
    static final String TAG_BACK_STACK = "backStack";
    static final String TAG_NEXT_INDEX = "nextIndex";

    /// Commits arriving while transactions run are executed in the same pass.
    private final ArrayList<Runnable> mPendingActions = new ArrayList<Runnable>();
    private boolean mExecCommitScheduled;
    private boolean mExecutingActions;

    final ArrayList<Fragment> mActive = new ArrayList<Fragment>();
    final ArrayList<Fragment> mAdded = new ArrayList<Fragment>();
    final ArrayList<BackStackRecord> mBackStack = new ArrayList<BackStackRecord>();
    private final ArrayList<BackStackRecord> mBackStackIndices = new ArrayList<BackStackRecord>();
    private ArrayList<OnBackStackChangedListener> mBackStackChangeListeners;
    private final ArrayList<Object[]> mLifecycleCallbacks = new ArrayList<Object[]>();

    int mCurState = Fragment.INITIALIZING;
    Activity mActivity;
    final Fragment mParent;
    private Fragment mPrimaryNav;
    private boolean mNeedMenuInvalidate;
    private boolean mStateSaved;
    private boolean mDestroyed;

    /// Where [Activity#onSaveInstanceState(Bundle)] keeps this manager's state.
    String mSaveKey = Activity.FRAGMENTS_TAG;
    private Object mFacade;
    private Instantiator mInstantiator;

    /// Runtime use: creates fragments by class name for this manager (the
    /// AndroidX `FragmentFactory` an application installed).
    public interface Instantiator {
        Fragment instantiate(String className);
    }

    public void setInstantiator(Instantiator instantiator) {
        mInstantiator = instantiator;
    }

    /// The fragment `className` names, through the installed instantiator,
    /// then this manager's parent's, then the generated factory.
    Fragment instantiate(Context context, String className, Bundle args) {
        for (FragmentManagerImpl fm = this; fm != null;
             fm = fm.mParent == null ? null : fm.mParent.mFragmentManager) {
            if (fm.mInstantiator != null) {
                Fragment f = fm.mInstantiator.instantiate(className);
                if (f != null) {
                    if (args != null) {
                        f.mArguments = args;
                    }
                    return f;
                }
            }
        }
        return Fragment.instantiate(context, className, args);
    }

    /// Runtime use (AndroidX `FragmentTransaction.add(ViewGroup, ...)`): the
    /// container `f`'s view goes into, before it can be found by id.
    public static void setInflatedContainer(Fragment f, ViewGroup container) {
        f.mInflatedContainer = container;
    }

    FragmentManagerImpl(Activity activity, Fragment parent) {
        mActivity = activity;
        mParent = parent;
    }

    /// Runtime use: a second top-level manager for `activity`, saved under
    /// `saveKey` and moved through the activity's lifecycle with its own
    /// (the AndroidX `FragmentActivity`'s support fragment manager).
    public static FragmentManagerImpl attachHost(Activity activity, String saveKey) {
        FragmentManagerImpl fm = new FragmentManagerImpl(activity, null);
        fm.mSaveKey = saveKey;
        activity.mFragmentHosts.add(fm);
        return fm;
    }

    /// Runtime use: the object presenting this manager to another API (the
    /// AndroidX `FragmentManager` wrapping it).
    public Object getFacade() {
        return mFacade;
    }

    public void setFacade(Object facade) {
        mFacade = facade;
    }

    /// Runtime use: the fragment whose children this manager holds, or null
    /// for an activity's manager.
    public Fragment getParentFragment() {
        return mParent;
    }

    /// Runtime use: whether transactions are running right now.
    public boolean isExecutingActions() {
        return mExecutingActions;
    }

    /// Runtime use: the fragment's position among the managers of its
    /// activity, stable when the activity is recreated from saved state (the
    /// key AndroidX keeps a fragment's view models under).
    public static String keyOf(Fragment f) {
        if (f == null || f.mIndex < 0 || f.mFragmentManager == null) {
            return null;
        }
        if (f.mParentFragment != null) {
            String parent = keyOf(f.mParentFragment);
            return parent == null ? null : parent + "/" + f.mIndex;
        }
        return f.mFragmentManager.mSaveKey + ":" + f.mIndex;
    }

    /// Runtime use: every active fragment of this manager and of its
    /// fragments' child managers, added or on the back stack.
    public void collectActive(List<Fragment> out) {
        for (int i = 0; i < mActive.size(); i++) {
            Fragment f = mActive.get(i);
            if (f != null) {
                out.add(f);
                if (f.mChildFragmentManager != null) {
                    f.mChildFragmentManager.collectActive(out);
                }
            }
        }
    }

    /// Runtime use (AndroidX `FragmentContainerView`): adds the fragment
    /// `fname` names to `container` while the container is being inflated,
    /// unless one was restored there, and answers it.
    public Fragment addToInflatingContainer(ViewGroup container, String fname, String tag, Context context,
                                            AttributeSet attrs) {
        int id = container.getId();
        if (id == View.NO_ID) {
            throw new IllegalStateException("FragmentContainerView must have an android:id to add Fragment " + fname);
        }
        Fragment fragment = findFragmentById(id);
        if (fragment != null) {
            fragment.mInflatedContainer = container;
            return fragment;
        }
        fragment = instantiate(context, fname, null);
        fragment.mFragmentId = id;
        fragment.mContainerId = id;
        fragment.mTag = tag;
        fragment.mInflatedContainer = container;
        fragment.mFragmentManager = this;
        fragment.mActivity = mActivity;
        fragment.onInflate(context, attrs, null);
        addFragment(fragment, true);
        return fragment;
    }

    @Override
    public String toString() {
        return "FragmentManager{" + Integer.toHexString(System.identityHashCode(this)) + " in "
                + (mParent != null ? mParent.toString() : String.valueOf(mActivity)) + "}";
    }

    // ------------------------------------------------------------ public API

    @Override
    public FragmentTransaction beginTransaction() {
        return new BackStackRecord(this);
    }

    @Override
    public boolean executePendingTransactions() {
        return execPendingActions();
    }

    @Override
    public void popBackStack() {
        enqueueAction(new PopAction(null, -1, 0), false);
    }

    @Override
    public boolean popBackStackImmediate() {
        checkStateLoss();
        execPendingActions();
        return popBackStackState(null, -1, 0);
    }

    @Override
    public void popBackStack(String name, int flags) {
        enqueueAction(new PopAction(name, -1, flags), false);
    }

    @Override
    public boolean popBackStackImmediate(String name, int flags) {
        checkStateLoss();
        execPendingActions();
        return popBackStackState(name, -1, flags);
    }

    @Override
    public void popBackStack(int id, int flags) {
        if (id < 0) {
            throw new IllegalArgumentException("Bad id: " + id);
        }
        enqueueAction(new PopAction(null, id, flags), false);
    }

    @Override
    public boolean popBackStackImmediate(int id, int flags) {
        checkStateLoss();
        execPendingActions();
        if (id < 0) {
            throw new IllegalArgumentException("Bad id: " + id);
        }
        return popBackStackState(null, id, flags);
    }

    @Override
    public int getBackStackEntryCount() {
        return mBackStack.size();
    }

    @Override
    public BackStackEntry getBackStackEntryAt(int index) {
        return mBackStack.get(index);
    }

    @Override
    public void addOnBackStackChangedListener(OnBackStackChangedListener listener) {
        if (mBackStackChangeListeners == null) {
            mBackStackChangeListeners = new ArrayList<OnBackStackChangedListener>();
        }
        mBackStackChangeListeners.add(listener);
    }

    @Override
    public void removeOnBackStackChangedListener(OnBackStackChangedListener listener) {
        if (mBackStackChangeListeners != null) {
            mBackStackChangeListeners.remove(listener);
        }
    }

    @Override
    public void putFragment(Bundle bundle, String key, Fragment fragment) {
        if (fragment.mIndex < 0) {
            throw new IllegalStateException("Fragment " + fragment + " is not currently in the FragmentManager");
        }
        bundle.putInt(key, fragment.mIndex);
    }

    @Override
    public Fragment getFragment(Bundle bundle, String key) {
        int index = bundle.getInt(key, -1);
        if (index == -1) {
            return null;
        }
        Fragment f = index < mActive.size() ? mActive.get(index) : null;
        if (f == null) {
            throw new IllegalStateException("Fragment no longer exists for key " + key + ": index " + index);
        }
        return f;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <F extends Fragment> List<F> getFragments() {
        List<Fragment> copy = Collections.unmodifiableList(new ArrayList<Fragment>(mAdded));
        return (List<F>) (List<?>) copy;
    }

    @Override
    public Fragment.SavedState saveFragmentInstanceState(Fragment f) {
        if (f.mIndex < 0) {
            throw new IllegalStateException("Fragment " + f + " is not currently in the FragmentManager");
        }
        if (f.mState > Fragment.INITIALIZING) {
            Bundle result = saveFragmentBasicState(f);
            return result != null ? new Fragment.SavedState(result) : null;
        }
        return null;
    }

    @Override
    public boolean isDestroyed() {
        return mDestroyed;
    }

    @Override
    public boolean isStateSaved() {
        return mStateSaved;
    }

    @Override
    public Fragment getPrimaryNavigationFragment() {
        return mPrimaryNav;
    }

    void setPrimaryNavigationFragment(Fragment f) {
        mPrimaryNav = f;
    }

    @Override
    public void registerFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb, boolean recursive) {
        mLifecycleCallbacks.add(new Object[] {cb, Boolean.valueOf(recursive)});
    }

    @Override
    public void unregisterFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb) {
        for (int i = mLifecycleCallbacks.size() - 1; i >= 0; i--) {
            if (mLifecycleCallbacks.get(i)[0] == cb) {
                mLifecycleCallbacks.remove(i);
            }
        }
    }

    @Override
    public void invalidateOptionsMenu() {
        if (mActivity != null && mCurState == Fragment.RESUMED) {
            mActivity.invalidateOptionsMenu();
        } else {
            mNeedMenuInvalidate = true;
        }
    }

    @Override
    public Fragment findFragmentById(int id) {
        for (int i = mAdded.size() - 1; i >= 0; i--) {
            Fragment f = mAdded.get(i);
            if (f != null && f.mFragmentId == id) {
                return f;
            }
        }
        for (int i = mActive.size() - 1; i >= 0; i--) {
            Fragment f = mActive.get(i);
            if (f != null && f.mFragmentId == id) {
                return f;
            }
        }
        return null;
    }

    @Override
    public Fragment findFragmentByTag(String tag) {
        if (tag == null) {
            return null;
        }
        for (int i = mAdded.size() - 1; i >= 0; i--) {
            Fragment f = mAdded.get(i);
            if (f != null && tag.equals(f.mTag)) {
                return f;
            }
        }
        for (int i = mActive.size() - 1; i >= 0; i--) {
            Fragment f = mActive.get(i);
            if (f != null && tag.equals(f.mTag)) {
                return f;
            }
        }
        return null;
    }

    // ------------------------------------------------------------ host state

    boolean isStateAtLeast(int state) {
        return mCurState >= state;
    }

    void noteStateNotSaved() {
        mStateSaved = false;
    }

    void dispatchCreate() {
        mStateSaved = false;
        dispatchMoveToState(Fragment.CREATED);
    }

    void dispatchActivityCreated() {
        mStateSaved = false;
        dispatchMoveToState(Fragment.ACTIVITY_CREATED);
    }

    void dispatchStart() {
        mStateSaved = false;
        dispatchMoveToState(Fragment.STARTED);
    }

    void dispatchResume() {
        mStateSaved = false;
        dispatchMoveToState(Fragment.RESUMED);
    }

    void dispatchPause() {
        dispatchMoveToState(Fragment.STARTED);
    }

    void dispatchStop() {
        dispatchMoveToState(Fragment.ACTIVITY_CREATED);
    }

    void dispatchDestroyView() {
        dispatchMoveToState(Fragment.CREATED);
    }

    void dispatchDestroy() {
        mDestroyed = true;
        execPendingActions();
        dispatchMoveToState(Fragment.INITIALIZING);
        mActivity = null;
    }

    private void dispatchMoveToState(int state) {
        mExecutingActions = true;
        try {
            moveToState(state, false);
        } finally {
            mExecutingActions = false;
        }
        execPendingActions();
    }

    void dispatchConfigurationChanged(Configuration newConfig) {
        for (int i = 0; i < mAdded.size(); i++) {
            mAdded.get(i).performConfigurationChanged(newConfig);
        }
    }

    void dispatchLowMemory() {
        for (int i = 0; i < mAdded.size(); i++) {
            mAdded.get(i).performLowMemory();
        }
    }

    boolean dispatchCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        boolean show = false;
        for (int i = 0; i < mAdded.size(); i++) {
            show |= mAdded.get(i).performCreateOptionsMenu(menu, inflater);
        }
        return show;
    }

    boolean dispatchPrepareOptionsMenu(Menu menu) {
        boolean show = false;
        for (int i = 0; i < mAdded.size(); i++) {
            show |= mAdded.get(i).performPrepareOptionsMenu(menu);
        }
        return show;
    }

    boolean dispatchOptionsItemSelected(MenuItem item) {
        for (int i = 0; i < mAdded.size(); i++) {
            if (mAdded.get(i).performOptionsItemSelected(item)) {
                return true;
            }
        }
        return false;
    }

    boolean dispatchContextItemSelected(MenuItem item) {
        for (int i = 0; i < mAdded.size(); i++) {
            if (mAdded.get(i).performContextItemSelected(item)) {
                return true;
            }
        }
        return false;
    }

    void dispatchOptionsMenuClosed(Menu menu) {
        for (int i = 0; i < mAdded.size(); i++) {
            mAdded.get(i).performOptionsMenuClosed(menu);
        }
    }

    // ------------------------------------------------------------ the state machine

    private Context context() {
        return mActivity;
    }

    void moveToState(int newState, boolean always) {
        if (mActivity == null && newState != Fragment.INITIALIZING) {
            throw new IllegalStateException("No activity");
        }
        if (!always && newState == mCurState) {
            return;
        }
        mCurState = newState;
        // Added fragments first, in order, then the rest of the active ones,
        // so containers fill in the order fragments were added.
        for (int i = 0; i < mAdded.size(); i++) {
            moveFragmentToExpectedState(mAdded.get(i));
        }
        for (int i = 0; i < mActive.size(); i++) {
            Fragment f = mActive.get(i);
            if (f != null && (f.mRemoving || f.mDetached) && !f.mAdded) {
                moveFragmentToExpectedState(f);
            }
        }
        if (mNeedMenuInvalidate && mActivity != null && mCurState == Fragment.RESUMED) {
            mActivity.invalidateOptionsMenu();
            mNeedMenuInvalidate = false;
        }
    }

    void moveFragmentToExpectedState(Fragment f) {
        if (f == null) {
            return;
        }
        int next = mCurState;
        if (f.mRemoving) {
            next = f.isInBackStack() ? Math.min(next, Fragment.CREATED) : Math.min(next, Fragment.INITIALIZING);
        }
        moveToState(f, next, false);
    }

    void moveToState(Fragment f) {
        moveToState(f, mCurState, false);
    }

    @SuppressWarnings("fallthrough")
    void moveToState(Fragment f, int newState, boolean keepActive) {
        // A fragment that is not added (or is detached) never gets past created.
        if ((!f.mAdded || f.mDetached) && newState > Fragment.CREATED) {
            newState = Fragment.CREATED;
        }
        if (f.mRemoving && newState > f.mState) {
            newState = f.mState == Fragment.INITIALIZING && f.isInBackStack() ? Fragment.CREATED : f.mState;
        }
        if (f.mState <= newState) {
            // A restored layout fragment waits until its layout is inflated again.
            if (f.mFromLayout && !f.mInLayout) {
                return;
            }
            switch (f.mState) {
                case Fragment.INITIALIZING:
                    if (newState > Fragment.INITIALIZING) {
                        performAttach(f);
                        if (f.mFromLayout) {
                            createLayoutView(f);
                        }
                    }
                    // fall through
                case Fragment.CREATED:
                    if (newState > Fragment.CREATED) {
                        if (!f.mFromLayout) {
                            createView(f);
                        }
                        f.performActivityCreated(f.mSavedFragmentState);
                        callback(f, "activityCreated");
                        if (f.mView != null) {
                            f.restoreViewState(f.mSavedFragmentState);
                        }
                        f.mSavedFragmentState = null;
                    }
                    // fall through
                case Fragment.ACTIVITY_CREATED:
                    if (newState > Fragment.ACTIVITY_CREATED) {
                        f.performStart();
                        callback(f, "started");
                    }
                    // fall through
                case Fragment.STARTED:
                    if (newState > Fragment.STARTED) {
                        f.performResume();
                        callback(f, "resumed");
                        f.mSavedFragmentState = null;
                    }
                    // fall through
                default:
                    break;
            }
        } else if (f.mState > newState) {
            switch (f.mState) {
                case Fragment.RESUMED:
                    if (newState < Fragment.RESUMED) {
                        f.performPause();
                        callback(f, "paused");
                    }
                    // fall through
                case Fragment.STARTED:
                    if (newState < Fragment.STARTED) {
                        f.performStop();
                        callback(f, "stopped");
                    }
                    // fall through
                case Fragment.ACTIVITY_CREATED:
                    if (newState < Fragment.ACTIVITY_CREATED) {
                        f.performDestroyView();
                        callback(f, "viewDestroyed");
                        if (f.mView != null && f.mContainer != null) {
                            f.mContainer.removeView(f.mView);
                        }
                        f.mContainer = null;
                        f.mView = null;
                        f.mInLayout = false;
                    }
                    // fall through
                case Fragment.CREATED:
                    if (newState < Fragment.CREATED) {
                        if (!f.mRetaining) {
                            f.performDestroy();
                            callback(f, "destroyed");
                        } else {
                            f.mState = Fragment.INITIALIZING;
                        }
                        f.performDetach();
                        callback(f, "detached");
                        if (!keepActive) {
                            if (!f.mRetaining) {
                                makeInactive(f);
                            } else {
                                f.mActivity = null;
                                f.mParentFragment = null;
                                f.mFragmentManager = null;
                            }
                        }
                    }
                    // fall through
                default:
                    break;
            }
        }
        if (f.mState != newState) {
            f.mState = newState;
        }
    }

    private void performAttach(Fragment f) {
        Bundle saved = f.mSavedFragmentState;
        if (saved != null) {
            int target = saved.getInt(Fragment.TARGET_STATE_TAG, -1);
            if (target >= 0 && target < mActive.size()) {
                f.mTarget = mActive.get(target);
                f.mTargetRequestCode = saved.getInt(Fragment.TARGET_REQUEST_CODE_STATE_TAG, 0);
            }
            f.mUserVisibleHint = saved.getBoolean(Fragment.USER_VISIBLE_HINT_TAG, true);
        }
        f.mActivity = mActivity;
        f.mParentFragment = mParent;
        f.mFragmentManager = this;
        callback(f, "preAttached");
        f.mCalled = false;
        f.onAttach(context());
        if (!f.mCalled) {
            throw new SuperNotCalledException("Fragment " + f + " did not call through to super.onAttach()");
        }
        if (mParent == null) {
            mActivity.onAttachFragment(f);
        } else {
            mParent.onAttachFragment(f);
        }
        callback(f, "attached");
        if (!f.mRetaining) {
            f.performCreate(saved);
            callback(f, "created");
        } else {
            f.restoreChildFragmentState(saved);
            f.mState = Fragment.CREATED;
        }
        f.mRetaining = false;
    }

    private void createLayoutView(Fragment f) {
        Bundle saved = f.mSavedFragmentState;
        f.mView = f.performCreateView(f.performGetLayoutInflater(saved), null, saved);
        if (f.mView != null) {
            if (f.mHidden) {
                f.mView.setVisibility(View.GONE);
            }
            f.onViewCreated(f.mView, saved);
            callback(f, "viewCreated");
        }
    }

    private void createView(Fragment f) {
        Bundle saved = f.mSavedFragmentState;
        ViewGroup container = null;
        if (f.mContainerId != 0) {
            if (f.mContainerId == View.NO_ID) {
                throw new IllegalArgumentException("Cannot create fragment " + f + " for a container view with no id");
            }
            View v = f.mInflatedContainer != null && f.mInflatedContainer.getId() == f.mContainerId
                    ? f.mInflatedContainer : findContainer(f.mContainerId);
            if (v instanceof ViewGroup) {
                container = (ViewGroup) v;
            } else if (!f.mRestored) {
                String resName;
                try {
                    resName = f.getResources().getResourceName(f.mContainerId);
                } catch (RuntimeException e) {
                    resName = "unknown";
                }
                throw new IllegalArgumentException("No view found for id 0x" + Integer.toHexString(f.mContainerId)
                        + " (" + resName + ") for fragment " + f);
            }
        }
        f.mContainer = container;
        f.mView = f.performCreateView(f.performGetLayoutInflater(saved), container, saved);
        if (f.mView != null) {
            if (container != null) {
                container.addView(f.mView);
            }
            if (f.mHidden) {
                f.mView.setVisibility(View.GONE);
            }
            f.onViewCreated(f.mView, saved);
            callback(f, "viewCreated");
        }
    }

    private View findContainer(int id) {
        if (mParent != null) {
            return mParent.mView == null ? null : mParent.mView.findViewById(id);
        }
        return mActivity == null ? null : mActivity.findViewById(id);
    }

    private void callback(Fragment f, String event) {
        FragmentManagerImpl fm = this;
        boolean direct = true;
        while (fm != null) {
            for (int i = 0; i < fm.mLifecycleCallbacks.size(); i++) {
                Object[] e = fm.mLifecycleCallbacks.get(i);
                if (direct || Boolean.TRUE.equals(e[1])) {
                    fire((FragmentLifecycleCallbacks) e[0], f, event);
                }
            }
            fm = fm.mParent == null ? null : fm.mParent.mFragmentManager;
            direct = false;
        }
    }

    private void fire(FragmentLifecycleCallbacks cb, Fragment f, String event) {
        Bundle saved = f.mSavedFragmentState;
        if (event.equals("preAttached")) {
            cb.onFragmentPreAttached(this, f, mActivity);
        } else if (event.equals("attached")) {
            cb.onFragmentAttached(this, f, mActivity);
        } else if (event.equals("created")) {
            cb.onFragmentCreated(this, f, saved);
        } else if (event.equals("activityCreated")) {
            cb.onFragmentActivityCreated(this, f, saved);
        } else if (event.equals("viewCreated")) {
            cb.onFragmentViewCreated(this, f, f.mView, saved);
        } else if (event.equals("started")) {
            cb.onFragmentStarted(this, f);
        } else if (event.equals("resumed")) {
            cb.onFragmentResumed(this, f);
        } else if (event.equals("paused")) {
            cb.onFragmentPaused(this, f);
        } else if (event.equals("stopped")) {
            cb.onFragmentStopped(this, f);
        } else if (event.equals("viewDestroyed")) {
            cb.onFragmentViewDestroyed(this, f);
        } else if (event.equals("destroyed")) {
            cb.onFragmentDestroyed(this, f);
        } else if (event.equals("detached")) {
            cb.onFragmentDetached(this, f);
        }
    }

    // ------------------------------------------------------------ membership

    void makeActive(Fragment f) {
        if (f.mIndex >= 0) {
            return;
        }
        int index = mActive.indexOf(null);
        if (index < 0) {
            index = mActive.size();
            mActive.add(f);
        } else {
            mActive.set(index, f);
        }
        f.setIndex(index);
    }

    void makeInactive(Fragment f) {
        if (f.mIndex < 0) {
            return;
        }
        if (f.mIndex < mActive.size() && mActive.get(f.mIndex) == f) {
            mActive.set(f.mIndex, null);
        }
        f.initState();
    }

    void addFragment(Fragment f, boolean moveToStateNow) {
        makeActive(f);
        if (!f.mDetached) {
            if (mAdded.contains(f)) {
                throw new IllegalStateException("Fragment already added: " + f);
            }
            mAdded.add(f);
            f.mAdded = true;
            f.mRemoving = false;
            if (f.mView == null) {
                f.mHidden = false;
            }
            if (f.mHasMenu && f.mMenuVisible) {
                mNeedMenuInvalidate = true;
            }
            if (moveToStateNow) {
                moveToState(f);
            }
        }
    }

    void removeFragment(Fragment f) {
        boolean inactive = !f.isInBackStack();
        if (!f.mDetached || inactive) {
            mAdded.remove(f);
            if (f.mHasMenu && f.mMenuVisible) {
                mNeedMenuInvalidate = true;
            }
            f.mAdded = false;
            f.mRemoving = true;
            moveToState(f, inactive ? Fragment.INITIALIZING : Fragment.CREATED, false);
        }
    }

    void hideFragment(Fragment f) {
        if (!f.mHidden) {
            f.mHidden = true;
            if (f.mView != null) {
                f.mView.setVisibility(View.GONE);
            }
            if (f.mAdded && f.mHasMenu && f.mMenuVisible) {
                mNeedMenuInvalidate = true;
            }
            f.onHiddenChanged(true);
        }
    }

    void showFragment(Fragment f) {
        if (f.mHidden) {
            f.mHidden = false;
            if (f.mView != null) {
                f.mView.setVisibility(View.VISIBLE);
            }
            if (f.mAdded && f.mHasMenu && f.mMenuVisible) {
                mNeedMenuInvalidate = true;
            }
            f.onHiddenChanged(false);
        }
    }

    void detachFragment(Fragment f) {
        if (!f.mDetached) {
            f.mDetached = true;
            if (f.mAdded) {
                mAdded.remove(f);
                if (f.mHasMenu && f.mMenuVisible) {
                    mNeedMenuInvalidate = true;
                }
                f.mAdded = false;
                moveToState(f, Fragment.CREATED, false);
            }
        }
    }

    void attachFragment(Fragment f) {
        if (f.mDetached) {
            f.mDetached = false;
            if (!f.mAdded) {
                if (mAdded.contains(f)) {
                    throw new IllegalStateException("Fragment already added: " + f);
                }
                mAdded.add(f);
                f.mAdded = true;
                if (f.mHasMenu && f.mMenuVisible) {
                    mNeedMenuInvalidate = true;
                }
                moveToState(f);
            }
        }
    }

    // ------------------------------------------------------------ transactions

    void checkStateLoss() {
        if (mStateSaved) {
            throw new IllegalStateException("Can not perform this action after onSaveInstanceState");
        }
    }

    void enqueueAction(Runnable action, boolean allowStateLoss) {
        if (!allowStateLoss) {
            checkStateLoss();
        }
        if (mDestroyed || mActivity == null) {
            if (allowStateLoss) {
                return;
            }
            throw new IllegalStateException("Activity has been destroyed");
        }
        mPendingActions.add(action);
        scheduleCommit();
    }

    private void scheduleCommit() {
        if (mExecCommitScheduled || mExecutingActions) {
            return;
        }
        mExecCommitScheduled = true;
        // After the current event, as Android posts to the main looper.
        // (Before Codename One is running this runs at once.)
        com.codename1.ui.CN.callSerially(new Runnable() {
            @Override
            public void run() {
                mExecCommitScheduled = false;
                execPendingActions();
            }
        });
    }

    /// Runs a transaction now, without going through the queue.
    void execSingleAction(Runnable action, boolean allowStateLoss) {
        if (!allowStateLoss) {
            checkStateLoss();
        }
        if (mExecutingActions) {
            throw new IllegalStateException("FragmentManager is already executing transactions");
        }
        execPendingActions();
        mExecutingActions = true;
        try {
            action.run();
        } finally {
            mExecutingActions = false;
        }
        afterExecution(true);
    }

    boolean execPendingActions() {
        if (mExecutingActions) {
            throw new IllegalStateException("FragmentManager is already executing transactions");
        }
        boolean didSomething = false;
        while (!mPendingActions.isEmpty()) {
            ArrayList<Runnable> batch = new ArrayList<Runnable>(mPendingActions);
            mPendingActions.clear();
            mExecutingActions = true;
            try {
                for (int i = 0; i < batch.size(); i++) {
                    batch.get(i).run();
                }
            } finally {
                mExecutingActions = false;
            }
            didSomething = true;
        }
        afterExecution(didSomething);
        return didSomething;
    }

    private void afterExecution(boolean didSomething) {
        if (didSomething && mNeedMenuInvalidate && mActivity != null && mCurState == Fragment.RESUMED) {
            mNeedMenuInvalidate = false;
            mActivity.invalidateOptionsMenu();
        }
    }

    int allocBackStackIndex(BackStackRecord bse) {
        int index = mBackStackIndices.indexOf(null);
        if (index < 0) {
            index = mBackStackIndices.size();
            mBackStackIndices.add(bse);
        } else {
            mBackStackIndices.set(index, bse);
        }
        return index;
    }

    void freeBackStackIndex(int index) {
        if (index >= 0 && index < mBackStackIndices.size()) {
            mBackStackIndices.set(index, null);
        }
    }

    void addBackStackState(BackStackRecord state) {
        mBackStack.add(state);
        reportBackStackChanged();
    }

    void reportBackStackChanged() {
        if (mBackStackChangeListeners != null) {
            ArrayList<OnBackStackChangedListener> copy =
                    new ArrayList<OnBackStackChangedListener>(mBackStackChangeListeners);
            for (int i = 0; i < copy.size(); i++) {
                copy.get(i).onBackStackChanged();
            }
        }
    }

    /// Pops back stack entries the way Android does: the last one, the ones
    /// above the entry `name` or `id` names, or (inclusive) that entry too.
    boolean popBackStackState(String name, int id, int flags) {
        if (mBackStack.isEmpty()) {
            return false;
        }
        int index;
        if (name == null && id < 0 && (flags & POP_BACK_STACK_INCLUSIVE) == 0) {
            index = mBackStack.size() - 2;
        } else {
            index = -1;
            if (name != null || id >= 0) {
                index = mBackStack.size() - 1;
                while (index >= 0) {
                    BackStackRecord bss = mBackStack.get(index);
                    if ((name != null && name.equals(bss.getName())) || (id >= 0 && id == bss.mIndex)) {
                        break;
                    }
                    index--;
                }
                if (index < 0) {
                    return false;
                }
                if ((flags & POP_BACK_STACK_INCLUSIVE) != 0) {
                    index--;
                    while (index >= 0) {
                        BackStackRecord bss = mBackStack.get(index);
                        if ((name != null && name.equals(bss.getName())) || (id >= 0 && id == bss.mIndex)) {
                            index--;
                            continue;
                        }
                        break;
                    }
                }
            }
            if (index == mBackStack.size() - 1) {
                return false;
            }
        }
        boolean was = mExecutingActions;
        mExecutingActions = true;
        try {
            for (int i = mBackStack.size() - 1; i > index; i--) {
                BackStackRecord rec = mBackStack.remove(i);
                rec.popFromBackStack(i == index + 1);
            }
        } finally {
            mExecutingActions = was;
        }
        reportBackStackChanged();
        afterExecution(true);
        return true;
    }

    private final class PopAction implements Runnable {
        private final String name;
        private final int id;
        private final int flags;

        PopAction(String name, int id, int flags) {
            this.name = name;
            this.id = id;
            this.flags = flags;
        }

        @Override
        public void run() {
            popBackStackState(name, id, flags);
        }
    }

    // ------------------------------------------------------------ <fragment> tags

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return null;
    }

    /// The AndroidX fragment container's tag.
    static final String CONTAINER_VIEW = "androidx.fragment.app.FragmentContainerView";

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        if (CONTAINER_VIEW.equals(name)) {
            return new androidx.fragment.app.FragmentContainerView(context, attrs, this);
        }
        if (!"fragment".equals(name)) {
            return null;
        }
        String fname = attrs.getAttributeValue("", "class");
        TypedArray a = context.obtainStyledAttributes(attrs, android.R.styleable.Fragment);
        if (fname == null) {
            fname = a.getString(android.R.styleable.Fragment_name);
        }
        int id = a.getResourceId(android.R.styleable.Fragment_id, View.NO_ID);
        String tag = a.getString(android.R.styleable.Fragment_tag);
        a.recycle();
        int containerId = parent != null ? parent.getId() : 0;
        if (containerId == View.NO_ID && id == View.NO_ID && tag == null) {
            throw new IllegalArgumentException(attrs.getPositionDescription()
                    + ": Must specify unique android:id, android:tag, or have a parent with an id for " + fname);
        }
        Fragment fragment = id != View.NO_ID ? findFragmentById(id) : null;
        if (fragment == null && tag != null) {
            fragment = findFragmentByTag(tag);
        }
        if (fragment == null && containerId != View.NO_ID) {
            fragment = findFragmentById(containerId);
        }
        if (fragment == null) {
            fragment = instantiate(context, fname, null);
            fragment.mFromLayout = true;
            fragment.mFragmentId = id != View.NO_ID ? id : containerId;
            fragment.mContainerId = containerId;
            fragment.mTag = tag;
            fragment.mInLayout = true;
            fragment.mFragmentManager = this;
            fragment.mActivity = mActivity;
            fragment.onInflate(mActivity, attrs, fragment.mSavedFragmentState);
            addFragment(fragment, true);
        } else if (fragment.mInLayout) {
            throw new IllegalArgumentException(attrs.getPositionDescription() + ": Duplicate id 0x"
                    + Integer.toHexString(id) + ", tag " + tag + ", or parent id 0x" + Integer.toHexString(containerId)
                    + " with another fragment for " + fname);
        } else {
            // Restored from saved state: the layout is claiming it back.
            fragment.mInLayout = true;
            fragment.mActivity = mActivity;
            if (!fragment.mRetaining) {
                fragment.onInflate(mActivity, attrs, fragment.mSavedFragmentState);
            }
        }
        if (mCurState < Fragment.CREATED && fragment.mFromLayout) {
            moveToState(fragment, Fragment.CREATED, false);
        } else {
            moveToState(fragment);
        }
        if (fragment.mView == null) {
            throw new IllegalStateException("Fragment " + fname + " did not create a view.");
        }
        if (id != View.NO_ID) {
            fragment.mView.setId(id);
        }
        if (fragment.mView.getTag() == null) {
            fragment.mView.setTag(tag);
        }
        return fragment.mView;
    }

    // ------------------------------------------------------------ saving

    Bundle saveFragmentBasicState(Fragment f) {
        Bundle result = f.performSaveInstanceState();
        callbackSave(f, result);
        if (f.mTarget != null && f.mTarget.mIndex >= 0) {
            result.putInt(Fragment.TARGET_STATE_TAG, f.mTarget.mIndex);
            if (f.mTargetRequestCode != 0) {
                result.putInt(Fragment.TARGET_REQUEST_CODE_STATE_TAG, f.mTargetRequestCode);
            }
        }
        if (!f.mUserVisibleHint) {
            result.putBoolean(Fragment.USER_VISIBLE_HINT_TAG, false);
        }
        return result;
    }

    private void callbackSave(Fragment f, Bundle out) {
        for (int i = 0; i < mLifecycleCallbacks.size(); i++) {
            ((FragmentLifecycleCallbacks) mLifecycleCallbacks.get(i)[0]).onFragmentSaveInstanceState(this, f, out);
        }
    }

    /// Everything needed to rebuild this manager after the activity is
    /// recreated: the active fragments (by class name, arguments and saved
    /// state), which are added, and the back stack.
    Bundle saveAllState() {
        execPendingActions();
        mStateSaved = true;
        Bundle state = new Bundle();
        state.putInt(TAG_ACTIVE_COUNT, mActive.size());
        for (int i = 0; i < mActive.size(); i++) {
            Fragment f = mActive.get(i);
            if (f == null) {
                continue;
            }
            Bundle fs = new Bundle();
            fs.putString("class", f.getClass().getName());
            fs.putBoolean("fromLayout", f.mFromLayout);
            fs.putInt("id", f.mFragmentId);
            fs.putInt("container", f.mContainerId);
            fs.putString("tag", f.mTag);
            fs.putBoolean("retain", f.mRetainInstance);
            fs.putBoolean("removing", f.mRemoving);
            fs.putBoolean("detached", f.mDetached);
            fs.putBoolean("hidden", f.mHidden);
            fs.putBundle("arguments", f.mArguments);
            if (f.mState > Fragment.INITIALIZING) {
                fs.putBundle("saved", saveFragmentBasicState(f));
            } else if (f.mSavedFragmentState != null) {
                fs.putBundle("saved", f.mSavedFragmentState);
            }
            state.putBundle("f" + i, fs);
        }
        int[] added = new int[mAdded.size()];
        for (int i = 0; i < added.length; i++) {
            added[i] = mAdded.get(i).mIndex;
        }
        state.putIntArray(TAG_ADDED, added);
        state.putInt(TAG_BACK_STACK, mBackStack.size());
        for (int i = 0; i < mBackStack.size(); i++) {
            state.putBundle("b" + i, mBackStack.get(i).saveState());
        }
        state.putInt(TAG_NEXT_INDEX, mBackStackIndices.size());
        if (mPrimaryNav != null && mPrimaryNav.mIndex >= 0) {
            state.putInt("primary", mPrimaryNav.mIndex);
        }
        return state;
    }

    /// Marks the fragments that keep their instance across recreation and
    /// returns them, so the next activity's manager reuses them.
    ArrayList<Fragment> retainNonConfig() {
        ArrayList<Fragment> out = null;
        for (int i = 0; i < mActive.size(); i++) {
            Fragment f = mActive.get(i);
            if (f != null && f.mRetainInstance) {
                if (out == null) {
                    out = new ArrayList<Fragment>();
                }
                out.add(f);
                f.mRetaining = true;
                f.mTargetIndex = f.mTarget != null ? f.mTarget.mIndex : -1;
            }
        }
        return out;
    }

    void restoreAllState(Bundle state, List<Fragment> retained) {
        if (state == null) {
            return;
        }
        int count = state.getInt(TAG_ACTIVE_COUNT, 0);
        mActive.clear();
        mAdded.clear();
        for (int i = 0; i < count; i++) {
            Bundle fs = state.getBundle("f" + i);
            if (fs == null) {
                mActive.add(null);
                continue;
            }
            Fragment f = null;
            if (retained != null) {
                for (int r = 0; r < retained.size(); r++) {
                    Fragment candidate = retained.get(r);
                    if (candidate.mIndex == i) {
                        f = candidate;
                        break;
                    }
                }
            }
            if (f == null) {
                f = instantiate(mActivity, fs.getString("class"), fs.getBundle("arguments"));
            } else {
                f.mRetaining = true;
                f.mBackStackNesting = 0;
                f.mInLayout = false;
                f.mAdded = false;
                f.mTarget = null;
            }
            f.setIndex(i);
            f.mFromLayout = fs.getBoolean("fromLayout");
            f.mRestored = true;
            f.mFragmentId = fs.getInt("id");
            f.mContainerId = fs.getInt("container");
            f.mTag = fs.getString("tag");
            f.mRetainInstance = fs.getBoolean("retain");
            f.mRemoving = fs.getBoolean("removing");
            f.mDetached = fs.getBoolean("detached");
            f.mHidden = fs.getBoolean("hidden");
            f.mFragmentManager = this;
            Bundle saved = fs.getBundle("saved");
            f.mSavedFragmentState = saved;
            mActive.add(f);
        }
        if (retained != null) {
            for (int r = 0; r < retained.size(); r++) {
                Fragment f = retained.get(r);
                if (f.mTargetIndex >= 0 && f.mTargetIndex < mActive.size()) {
                    f.mTarget = mActive.get(f.mTargetIndex);
                }
            }
        }
        int[] added = state.getIntArray(TAG_ADDED);
        if (added != null) {
            for (int idx : added) {
                Fragment f = idx >= 0 && idx < mActive.size() ? mActive.get(idx) : null;
                if (f == null) {
                    throw new IllegalStateException("No instantiated fragment for index #" + idx);
                }
                f.mAdded = true;
                if (mAdded.contains(f)) {
                    throw new IllegalStateException("Already added!");
                }
                mAdded.add(f);
            }
        }
        mBackStack.clear();
        mBackStackIndices.clear();
        int next = state.getInt(TAG_NEXT_INDEX, 0);
        for (int i = 0; i < next; i++) {
            mBackStackIndices.add(null);
        }
        int backStack = state.getInt(TAG_BACK_STACK, 0);
        for (int i = 0; i < backStack; i++) {
            BackStackRecord rec = BackStackRecord.restore(this, state.getBundle("b" + i));
            mBackStack.add(rec);
            if (rec.mIndex >= 0) {
                while (mBackStackIndices.size() <= rec.mIndex) {
                    mBackStackIndices.add(null);
                }
                mBackStackIndices.set(rec.mIndex, rec);
            }
        }
        int primary = state.getInt("primary", -1);
        mPrimaryNav = primary >= 0 && primary < mActive.size() ? mActive.get(primary) : null;
    }
}
