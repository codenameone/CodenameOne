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
package androidx.room;

import androidx.lifecycle.LiveData;
import com.codename1.androidcompat.runtime.MainThread;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/// The `LiveData` a DAO query returns, refreshed as AndroidX's is: an
/// invalidation is noted on the main thread, the query runs on the
/// database's query executor, and the value is posted back. One refresh
/// runs at a time, and an invalidation that arrives while one runs makes it
/// go round again, so the last value delivered is never older than the last
/// write.
final class RoomTrackingLiveData<T> extends LiveData<T> {

    private final RoomDatabase mDatabase;
    private final boolean mInTransaction;
    private final Callable<T> mCompute;
    private final InvalidationTracker.Observer mObserver;
    private final AtomicBoolean mInvalid = new AtomicBoolean(true);
    private final AtomicBoolean mComputing = new AtomicBoolean(false);
    private boolean mRegistered;

    private final Runnable mRefresh = new Runnable() {
        @Override
        public void run() {
            boolean computed;
            do {
                computed = false;
                if (mComputing.compareAndSet(false, true)) {
                    try {
                        T value = null;
                        while (mInvalid.compareAndSet(true, false)) {
                            computed = true;
                            value = compute();
                        }
                        if (computed) {
                            postValue(value);
                        }
                    } finally {
                        mComputing.set(false);
                    }
                }
            } while (computed && mInvalid.get());
        }
    };

    private final Runnable mInvalidated = new Runnable() {
        @Override
        public void run() {
            boolean active = hasActiveObservers();
            if (mInvalid.compareAndSet(false, true) && active) {
                mDatabase.getQueryExecutor().execute(mRefresh);
            }
        }
    };

    RoomTrackingLiveData(RoomDatabase database, String[] tables, boolean inTransaction, Callable<T> compute) {
        mDatabase = database;
        mInTransaction = inTransaction;
        mCompute = compute;
        mObserver = new InvalidationTracker.Observer(tables) {
            @Override
            public void onInvalidated(Set<String> changed) {
                if (MainThread.isMainThread()) {
                    mInvalidated.run();
                } else {
                    MainThread.post(mInvalidated);
                }
            }
        };
    }

    @Override
    protected void onActive() {
        if (!mRegistered) {
            mDatabase.getInvalidationTracker().addObserver(mObserver);
            mRegistered = true;
        }
        mDatabase.getQueryExecutor().execute(mRefresh);
    }

    @Override
    protected void onInactive() {
        if (mRegistered) {
            mDatabase.getInvalidationTracker().removeObserver(mObserver);
            mRegistered = false;
        }
    }

    private T compute() {
        if (!mInTransaction) {
            return call();
        }
        mDatabase.beginTransaction();
        try {
            T value = call();
            mDatabase.setTransactionSuccessful();
            return value;
        } finally {
            mDatabase.endTransaction();
        }
    }

    private T call() {
        try {
            return mCompute.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Exception while computing database live data.", e);
        }
    }
}
