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
package androidx.lifecycle;

import com.codename1.androidcompat.runtime.MainThread;

import java.util.ArrayList;
import java.util.List;

/// A value observers see as it changes, delivered only while their
/// lifecycle is at least started: an observer that becomes active again gets
/// the latest value if it missed one, and an observer whose owner is
/// destroyed is removed. Values are set on the UI thread; [#postValue(Object)]
/// sets one from any thread.
public abstract class LiveData<T> {

    static final int START_VERSION = -1;
    private static final Object NOT_SET = new Object();

    private final List<ObserverWrapper> mObservers = new ArrayList<ObserverWrapper>();
    private int mActiveCount;
    private boolean mChangingActiveState;
    private Object mData;
    private int mVersion;
    private boolean mDispatchingValue;
    private boolean mDispatchInvalidated;

    public LiveData(T value) {
        mData = value;
        mVersion = START_VERSION + 1;
    }

    public LiveData() {
        mData = NOT_SET;
        mVersion = START_VERSION;
    }

    // ------------------------------------------------------------ observers

    abstract class ObserverWrapper {
        final Observer<? super T> mObserver;
        boolean mActive;
        int mLastVersion = START_VERSION;

        ObserverWrapper(Observer<? super T> observer) {
            mObserver = observer;
        }

        abstract boolean shouldBeActive();

        boolean isAttachedTo(LifecycleOwner owner) {
            return false;
        }

        void detachObserver() {
        }

        void activeStateChanged(boolean newActive) {
            if (newActive == mActive) {
                return;
            }
            mActive = newActive;
            changeActiveCounter(mActive ? 1 : -1);
            if (mActive) {
                dispatchingValue(this);
            }
        }
    }

    final class LifecycleBoundObserver extends ObserverWrapper implements LifecycleEventObserver {
        final LifecycleOwner mOwner;

        LifecycleBoundObserver(LifecycleOwner owner, Observer<? super T> observer) {
            super(observer);
            mOwner = owner;
        }

        @Override
        boolean shouldBeActive() {
            return mOwner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
        }

        @Override
        public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
            Lifecycle.State current = mOwner.getLifecycle().getCurrentState();
            if (current == Lifecycle.State.DESTROYED) {
                removeObserver(mObserver);
                return;
            }
            activeStateChanged(shouldBeActive());
        }

        @Override
        boolean isAttachedTo(LifecycleOwner owner) {
            return mOwner == owner;
        }

        @Override
        void detachObserver() {
            mOwner.getLifecycle().removeObserver(this);
        }
    }

    final class AlwaysActiveObserver extends ObserverWrapper {
        AlwaysActiveObserver(Observer<? super T> observer) {
            super(observer);
        }

        @Override
        boolean shouldBeActive() {
            return true;
        }
    }

    private ObserverWrapper find(Observer<? super T> observer) {
        for (int i = 0; i < mObservers.size(); i++) {
            ObserverWrapper w = mObservers.get(i);
            if (w.mObserver == observer) {
                return w;
            }
        }
        return null;
    }

    /// Delivers values to `observer` while `owner` is started or resumed;
    /// removes it when `owner` is destroyed.
    public void observe(LifecycleOwner owner, Observer<? super T> observer) {
        if (owner.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
            return;
        }
        ObserverWrapper existing = find(observer);
        if (existing != null) {
            if (!existing.isAttachedTo(owner)) {
                throw new IllegalArgumentException("Cannot add the same observer with different lifecycles");
            }
            return;
        }
        LifecycleBoundObserver wrapper = new LifecycleBoundObserver(owner, observer);
        mObservers.add(wrapper);
        owner.getLifecycle().addObserver(wrapper);
    }

    /// Delivers every value to `observer` until it is removed.
    public void observeForever(Observer<? super T> observer) {
        ObserverWrapper existing = find(observer);
        if (existing instanceof LiveData.LifecycleBoundObserver) {
            throw new IllegalArgumentException("Cannot add the same observer with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        AlwaysActiveObserver wrapper = new AlwaysActiveObserver(observer);
        mObservers.add(wrapper);
        wrapper.activeStateChanged(true);
    }

    public void removeObserver(Observer<? super T> observer) {
        ObserverWrapper removed = find(observer);
        if (removed == null) {
            return;
        }
        mObservers.remove(removed);
        removed.detachObserver();
        removed.activeStateChanged(false);
    }

    public void removeObservers(LifecycleOwner owner) {
        for (ObserverWrapper w : new ArrayList<ObserverWrapper>(mObservers)) {
            if (w.isAttachedTo(owner)) {
                removeObserver(w.mObserver);
            }
        }
    }

    public boolean hasObservers() {
        return !mObservers.isEmpty();
    }

    public boolean hasActiveObservers() {
        return mActiveCount > 0;
    }

    // ------------------------------------------------------------ values

    private void changeActiveCounter(int change) {
        int previous = mActiveCount;
        mActiveCount += change;
        if (mChangingActiveState) {
            return;
        }
        mChangingActiveState = true;
        try {
            while (previous != mActiveCount) {
                boolean needToCallActive = previous == 0 && mActiveCount > 0;
                boolean needToCallInactive = previous > 0 && mActiveCount == 0;
                previous = mActiveCount;
                if (needToCallActive) {
                    onActive();
                } else if (needToCallInactive) {
                    onInactive();
                }
            }
        } finally {
            mChangingActiveState = false;
        }
    }

    @SuppressWarnings("unchecked")
    private void considerNotify(ObserverWrapper observer) {
        if (!observer.mActive) {
            return;
        }
        if (!observer.shouldBeActive()) {
            observer.activeStateChanged(false);
            return;
        }
        if (observer.mLastVersion >= mVersion) {
            return;
        }
        observer.mLastVersion = mVersion;
        observer.mObserver.onChanged((T) mData);
    }

    void dispatchingValue(ObserverWrapper initiator) {
        if (mDispatchingValue) {
            mDispatchInvalidated = true;
            return;
        }
        mDispatchingValue = true;
        do {
            mDispatchInvalidated = false;
            if (initiator != null) {
                considerNotify(initiator);
                initiator = null;
            } else {
                for (ObserverWrapper w : new ArrayList<ObserverWrapper>(mObservers)) {
                    if (mObservers.contains(w)) {
                        considerNotify(w);
                    }
                    if (mDispatchInvalidated) {
                        break;
                    }
                }
            }
        } while (mDispatchInvalidated);
        mDispatchingValue = false;
    }

    /// Sets the value and delivers it to the active observers. UI thread
    /// only, as on Android.
    protected void setValue(T value) {
        if (!MainThread.isMainThread()) {
            throw new IllegalStateException("Cannot invoke setValue on a background thread");
        }
        mVersion++;
        mData = value;
        dispatchingValue(null);
    }

    /// Sets the value from any thread: it is set on the UI thread later.
    ///
    /// The posting thread hands the value over and touches no field of this
    /// object; every value is then set on the UI thread, in order. AndroidX
    /// instead keeps one pending value behind a lock and drops all but the
    /// last post made before the UI thread runs. Here each one is delivered:
    /// observers end on the same value, possibly passing through the earlier
    /// ones, and no state is shared between threads -- the pending field that
    /// coalesced posts could be read and reset by two threads at once.
    protected void postValue(final T value) {
        MainThread.post(new Runnable() {
            @Override
            public void run() {
                setValue(value);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public T getValue() {
        Object data = mData;
        return data != NOT_SET ? (T) data : null;
    }

    public boolean isInitialized() {
        return mData != NOT_SET;
    }

    int getVersion() {
        return mVersion;
    }

    /// The number of active observers went from none to one.
    protected void onActive() {
    }

    /// The number of active observers went to none.
    protected void onInactive() {
    }
}
