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
import android.view.View;

import java.util.ArrayList;

/// A committed transaction: its operations, and on the back stack the means
/// to reverse them.
final class BackStackRecord extends FragmentTransaction
        implements androidx.fragment.app.FragmentManager.BackStackEntry, Runnable {

    static final int OP_NULL = 0;
    static final int OP_ADD = 1;
    static final int OP_REPLACE = 2;
    static final int OP_REMOVE = 3;
    static final int OP_HIDE = 4;
    static final int OP_SHOW = 5;
    static final int OP_DETACH = 6;
    static final int OP_ATTACH = 7;
    static final int OP_SET_PRIMARY_NAV = 8;

    static final class Op {
        int cmd;
        Fragment fragment;
        int enterAnim;
        int exitAnim;
        int popEnterAnim;
        int popExitAnim;
        final ArrayList<Fragment> removed = new ArrayList<Fragment>();

        Op(int cmd, Fragment fragment) {
            this.cmd = cmd;
            this.fragment = fragment;
        }
    }

    final FragmentManagerImpl mManager;
    final ArrayList<Op> mOps = new ArrayList<Op>();
    int mEnterAnim;
    int mExitAnim;
    int mPopEnterAnim;
    int mPopExitAnim;
    int mTransition = TRANSIT_NONE;
    int mTransitionStyle;
    boolean mAddToBackStack;
    boolean mAllowAddToBackStack = true;
    String mName;
    boolean mCommitted;
    int mIndex = -1;
    int mBreadCrumbTitleRes;
    CharSequence mBreadCrumbTitleText;
    int mBreadCrumbShortTitleRes;
    CharSequence mBreadCrumbShortTitleText;
    ArrayList<Runnable> mCommitRunnables;

    BackStackRecord(FragmentManagerImpl manager) {
        mManager = manager;
    }

    @Override
    public String toString() {
        return "BackStackEntry{" + Integer.toHexString(System.identityHashCode(this))
                + (mIndex >= 0 ? " #" + mIndex : "") + (mName != null ? " " + mName : "") + "}";
    }

    // ------------------------------------------------------------ BackStackEntry

    @Override
    public int getId() {
        return mIndex;
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public int getBreadCrumbTitleRes() {
        return mBreadCrumbTitleRes;
    }

    @Override
    public int getBreadCrumbShortTitleRes() {
        return mBreadCrumbShortTitleRes;
    }

    @Override
    public CharSequence getBreadCrumbTitle() {
        if (mBreadCrumbTitleRes != 0 && mManager.mActivity != null) {
            return mManager.mActivity.getText(mBreadCrumbTitleRes);
        }
        return mBreadCrumbTitleText;
    }

    @Override
    public CharSequence getBreadCrumbShortTitle() {
        if (mBreadCrumbShortTitleRes != 0 && mManager.mActivity != null) {
            return mManager.mActivity.getText(mBreadCrumbShortTitleRes);
        }
        return mBreadCrumbShortTitleText;
    }

    // ------------------------------------------------------------ building

    private void addOp(Op op) {
        op.enterAnim = mEnterAnim;
        op.exitAnim = mExitAnim;
        op.popEnterAnim = mPopEnterAnim;
        op.popExitAnim = mPopExitAnim;
        mOps.add(op);
    }

    @Override
    public FragmentTransaction add(Fragment fragment, String tag) {
        doAddOp(0, fragment, tag, OP_ADD);
        return this;
    }

    @Override
    public FragmentTransaction add(int containerViewId, Fragment fragment) {
        doAddOp(containerViewId, fragment, null, OP_ADD);
        return this;
    }

    @Override
    public FragmentTransaction add(int containerViewId, Fragment fragment, String tag) {
        doAddOp(containerViewId, fragment, tag, OP_ADD);
        return this;
    }

    private void doAddOp(int containerViewId, Fragment fragment, String tag, int opcmd) {
        if (fragment == null) {
            throw new NullPointerException("fragment");
        }
        fragment.mFragmentManager = mManager;
        if (tag != null) {
            if (fragment.mTag != null && !tag.equals(fragment.mTag)) {
                throw new IllegalStateException("Can't change tag of fragment " + fragment + ": was " + fragment.mTag
                        + " now " + tag);
            }
            fragment.mTag = tag;
        }
        if (containerViewId != 0) {
            if (containerViewId == View.NO_ID) {
                throw new IllegalArgumentException("Can't add fragment " + fragment + " with tag " + tag
                        + " to container view with no id");
            }
            if (fragment.mFragmentId != 0 && fragment.mFragmentId != containerViewId) {
                throw new IllegalStateException("Can't change container ID of fragment " + fragment + ": was "
                        + fragment.mFragmentId + " now " + containerViewId);
            }
            fragment.mContainerId = containerViewId;
            fragment.mFragmentId = containerViewId;
        }
        addOp(new Op(opcmd, fragment));
    }

    @Override
    public FragmentTransaction replace(int containerViewId, Fragment fragment) {
        return replace(containerViewId, fragment, null);
    }

    @Override
    public FragmentTransaction replace(int containerViewId, Fragment fragment, String tag) {
        if (containerViewId == 0) {
            throw new IllegalArgumentException("Must use non-zero containerViewId");
        }
        doAddOp(containerViewId, fragment, tag, OP_REPLACE);
        return this;
    }

    @Override
    public FragmentTransaction remove(Fragment fragment) {
        addOp(new Op(OP_REMOVE, fragment));
        return this;
    }

    @Override
    public FragmentTransaction hide(Fragment fragment) {
        addOp(new Op(OP_HIDE, fragment));
        return this;
    }

    @Override
    public FragmentTransaction show(Fragment fragment) {
        addOp(new Op(OP_SHOW, fragment));
        return this;
    }

    @Override
    public FragmentTransaction detach(Fragment fragment) {
        addOp(new Op(OP_DETACH, fragment));
        return this;
    }

    @Override
    public FragmentTransaction attach(Fragment fragment) {
        addOp(new Op(OP_ATTACH, fragment));
        return this;
    }

    @Override
    public FragmentTransaction setPrimaryNavigationFragment(Fragment fragment) {
        addOp(new Op(OP_SET_PRIMARY_NAV, fragment));
        return this;
    }

    @Override
    public boolean isEmpty() {
        return mOps.isEmpty();
    }

    @Override
    public FragmentTransaction setCustomAnimations(int enter, int exit) {
        return setCustomAnimations(enter, exit, 0, 0);
    }

    @Override
    public FragmentTransaction setCustomAnimations(int enter, int exit, int popEnter, int popExit) {
        mEnterAnim = enter;
        mExitAnim = exit;
        mPopEnterAnim = popEnter;
        mPopExitAnim = popExit;
        return this;
    }

    @Override
    public FragmentTransaction addSharedElement(View sharedElement, String name) {
        return this;
    }

    @Override
    public FragmentTransaction setTransition(int transition) {
        mTransition = transition;
        return this;
    }

    @Override
    public FragmentTransaction setTransitionStyle(int styleRes) {
        mTransitionStyle = styleRes;
        return this;
    }

    @Override
    public FragmentTransaction addToBackStack(String name) {
        if (!mAllowAddToBackStack) {
            throw new IllegalStateException("This FragmentTransaction is not allowed to be added to the back stack.");
        }
        mAddToBackStack = true;
        mName = name;
        return this;
    }

    @Override
    public boolean isAddToBackStackAllowed() {
        return mAllowAddToBackStack;
    }

    @Override
    public FragmentTransaction disallowAddToBackStack() {
        if (mAddToBackStack) {
            throw new IllegalStateException("This transaction is already being added to the back stack");
        }
        mAllowAddToBackStack = false;
        return this;
    }

    @Override
    public FragmentTransaction setBreadCrumbTitle(int res) {
        mBreadCrumbTitleRes = res;
        mBreadCrumbTitleText = null;
        return this;
    }

    @Override
    public FragmentTransaction setBreadCrumbTitle(CharSequence text) {
        mBreadCrumbTitleRes = 0;
        mBreadCrumbTitleText = text;
        return this;
    }

    @Override
    public FragmentTransaction setBreadCrumbShortTitle(int res) {
        mBreadCrumbShortTitleRes = res;
        mBreadCrumbShortTitleText = null;
        return this;
    }

    @Override
    public FragmentTransaction setBreadCrumbShortTitle(CharSequence text) {
        mBreadCrumbShortTitleRes = 0;
        mBreadCrumbShortTitleText = text;
        return this;
    }

    @Override
    public FragmentTransaction setReorderingAllowed(boolean reorderingAllowed) {
        return this;
    }

    @Override
    public FragmentTransaction runOnCommit(Runnable runnable) {
        if (runnable == null) {
            throw new IllegalArgumentException("runnable cannot be null");
        }
        disallowAddToBackStack();
        if (mCommitRunnables == null) {
            mCommitRunnables = new ArrayList<Runnable>();
        }
        mCommitRunnables.add(runnable);
        return this;
    }

    // ------------------------------------------------------------ committing

    @Override
    public int commit() {
        return commitInternal(false);
    }

    @Override
    public int commitAllowingStateLoss() {
        return commitInternal(true);
    }

    @Override
    public void commitNow() {
        disallowAddToBackStack();
        commitNowInternal(false);
    }

    @Override
    public void commitNowAllowingStateLoss() {
        disallowAddToBackStack();
        commitNowInternal(true);
    }

    private void commitNowInternal(boolean allowStateLoss) {
        if (mCommitted) {
            throw new IllegalStateException("commit already called");
        }
        mCommitted = true;
        mManager.execSingleAction(this, allowStateLoss);
    }

    private int commitInternal(boolean allowStateLoss) {
        if (mCommitted) {
            throw new IllegalStateException("commit already called");
        }
        mCommitted = true;
        mIndex = mAddToBackStack ? mManager.allocBackStackIndex(this) : -1;
        mManager.enqueueAction(this, allowStateLoss);
        return mIndex;
    }

    // ------------------------------------------------------------ running

    void bumpBackStackNesting(int amt) {
        if (!mAddToBackStack) {
            return;
        }
        for (int i = 0; i < mOps.size(); i++) {
            Op op = mOps.get(i);
            if (op.fragment != null) {
                op.fragment.mBackStackNesting += amt;
            }
            for (int r = 0; r < op.removed.size(); r++) {
                op.removed.get(r).mBackStackNesting += amt;
            }
        }
    }

    /// Applies the operations, then brings every fragment to the manager's
    /// state.
    @Override
    public void run() {
        bumpBackStackNesting(1);
        for (int i = 0; i < mOps.size(); i++) {
            Op op = mOps.get(i);
            Fragment f = op.fragment;
            switch (op.cmd) {
                case OP_ADD:
                    mManager.addFragment(f, false);
                    break;
                case OP_REPLACE:
                    runReplace(op);
                    break;
                case OP_REMOVE:
                    mManager.removeFragment(f);
                    break;
                case OP_HIDE:
                    mManager.hideFragment(f);
                    break;
                case OP_SHOW:
                    mManager.showFragment(f);
                    break;
                case OP_DETACH:
                    mManager.detachFragment(f);
                    break;
                case OP_ATTACH:
                    mManager.attachFragment(f);
                    break;
                case OP_SET_PRIMARY_NAV:
                    op.removed.clear();
                    if (mManager.getPrimaryNavigationFragment() != null) {
                        Fragment previous = mManager.getPrimaryNavigationFragment();
                        op.removed.add(previous);
                        // The pop uncounts every removed fragment, so count it.
                        if (mAddToBackStack) {
                            previous.mBackStackNesting += 1;
                        }
                    }
                    mManager.setPrimaryNavigationFragment(f);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown cmd: " + op.cmd);
            }
        }
        mManager.moveToState(mManager.mCurState, true);
        if (mAddToBackStack) {
            mManager.addBackStackState(this);
        }
        if (mCommitRunnables != null) {
            for (int i = 0; i < mCommitRunnables.size(); i++) {
                mCommitRunnables.get(i).run();
            }
            mCommitRunnables = null;
        }
    }

    private void runReplace(Op op) {
        Fragment f = op.fragment;
        int containerId = f.mContainerId;
        ArrayList<Fragment> added = new ArrayList<Fragment>(mManager.mAdded);
        for (int i = added.size() - 1; i >= 0; i--) {
            Fragment old = added.get(i);
            if (old.mContainerId != containerId) {
                continue;
            }
            if (old == f) {
                // Replacing a fragment with itself leaves it in place. run()
                // already counted it as on the back stack, and with no
                // fragment left in the op the pop cannot uncount it.
                if (mAddToBackStack) {
                    f.mBackStackNesting -= 1;
                }
                op.fragment = null;
                f = null;
            } else {
                op.removed.add(old);
                if (mAddToBackStack) {
                    old.mBackStackNesting += 1;
                }
                mManager.removeFragment(old);
            }
        }
        if (f != null) {
            mManager.addFragment(f, false);
        }
    }

    /// Reverses the operations, last first.
    void popFromBackStack(boolean doStateMove) {
        bumpBackStackNesting(-1);
        for (int i = mOps.size() - 1; i >= 0; i--) {
            Op op = mOps.get(i);
            Fragment f = op.fragment;
            switch (op.cmd) {
                case OP_ADD:
                    mManager.removeFragment(f);
                    break;
                case OP_REPLACE:
                    if (f != null) {
                        mManager.removeFragment(f);
                    }
                    for (int r = 0; r < op.removed.size(); r++) {
                        mManager.addFragment(op.removed.get(r), false);
                    }
                    break;
                case OP_REMOVE:
                    mManager.addFragment(f, false);
                    break;
                case OP_HIDE:
                    mManager.showFragment(f);
                    break;
                case OP_SHOW:
                    mManager.hideFragment(f);
                    break;
                case OP_DETACH:
                    mManager.attachFragment(f);
                    break;
                case OP_ATTACH:
                    mManager.detachFragment(f);
                    break;
                case OP_SET_PRIMARY_NAV:
                    mManager.setPrimaryNavigationFragment(op.removed.isEmpty() ? null : op.removed.get(0));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown cmd: " + op.cmd);
            }
        }
        if (doStateMove) {
            mManager.moveToState(mManager.mCurState, true);
        }
        if (mIndex >= 0) {
            mManager.freeBackStackIndex(mIndex);
            mIndex = -1;
        }
    }

    // ------------------------------------------------------------ saving

    /// The operations as fragment indexes, so the manager that restores
    /// them can point them at the recreated fragments.
    Bundle saveState() {
        Bundle b = new Bundle();
        // Per op: cmd, fragment, four animations, the removed count, then
        // the removed fragments.
        int size = 0;
        for (int i = 0; i < mOps.size(); i++) {
            size += 7 + mOps.get(i).removed.size();
        }
        int[] ops = new int[size];
        int pos = 0;
        for (int i = 0; i < mOps.size(); i++) {
            Op op = mOps.get(i);
            ops[pos++] = op.cmd;
            ops[pos++] = op.fragment != null ? op.fragment.mIndex : -1;
            ops[pos++] = op.enterAnim;
            ops[pos++] = op.exitAnim;
            ops[pos++] = op.popEnterAnim;
            ops[pos++] = op.popExitAnim;
            ops[pos++] = op.removed.size();
            for (int r = 0; r < op.removed.size(); r++) {
                ops[pos++] = op.removed.get(r).mIndex;
            }
        }
        b.putIntArray("ops", ops);
        b.putInt("transition", mTransition);
        b.putInt("transitionStyle", mTransitionStyle);
        b.putString("name", mName);
        b.putInt("index", mIndex);
        b.putInt("bcTitleRes", mBreadCrumbTitleRes);
        b.putCharSequence("bcTitle", mBreadCrumbTitleText);
        b.putInt("bcShortTitleRes", mBreadCrumbShortTitleRes);
        b.putCharSequence("bcShortTitle", mBreadCrumbShortTitleText);
        return b;
    }

    static BackStackRecord restore(FragmentManagerImpl fm, Bundle b) {
        BackStackRecord rec = new BackStackRecord(fm);
        int[] ops = b.getIntArray("ops");
        int pos = 0;
        while (ops != null && pos < ops.length) {
            int cmd = ops[pos++];
            int index = ops[pos++];
            Op op = new Op(cmd, index >= 0 ? fm.mActive.get(index) : null);
            op.enterAnim = ops[pos++];
            op.exitAnim = ops[pos++];
            op.popEnterAnim = ops[pos++];
            op.popExitAnim = ops[pos++];
            int n = ops[pos++];
            for (int r = 0; r < n; r++) {
                op.removed.add(fm.mActive.get(ops[pos++]));
            }
            rec.mOps.add(op);
        }
        rec.mTransition = b.getInt("transition");
        rec.mTransitionStyle = b.getInt("transitionStyle");
        rec.mName = b.getString("name");
        rec.mIndex = b.getInt("index", -1);
        rec.mAddToBackStack = true;
        rec.mCommitted = true;
        rec.mBreadCrumbTitleRes = b.getInt("bcTitleRes");
        rec.mBreadCrumbTitleText = b.getCharSequence("bcTitle");
        rec.mBreadCrumbShortTitleRes = b.getInt("bcShortTitleRes");
        rec.mBreadCrumbShortTitleText = b.getCharSequence("bcShortTitle");
        rec.bumpBackStackNesting(1);
        return rec;
    }
}
