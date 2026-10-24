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

/// The `LiveData` a DAO query returns. It recomputes on the thread that
/// reports the change: on the main thread the value is set at once, from
/// any other it is posted.
final class RoomTrackingLiveData<T> extends LiveData<T> {

    private final RoomDatabase mDatabase;
    private final boolean mInTransaction;
    private final Callable<T> mCompute;
    private final InvalidationTracker.Observer mObserver;
    private boolean mRegistered;

    RoomTrackingLiveData(RoomDatabase database, String[] tables, boolean inTransaction, Callable<T> compute) {
        mDatabase = database;
        mInTransaction = inTransaction;
        mCompute = compute;
        mObserver = new InvalidationTracker.Observer(tables) {
            @Override
            public void onInvalidated(Set<String> changed) {
                if (hasActiveObservers()) {
                    refresh();
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
        refresh();
    }

    @Override
    protected void onInactive() {
        if (mRegistered) {
            mDatabase.getInvalidationTracker().removeObserver(mObserver);
            mRegistered = false;
        }
    }

    void refresh() {
        boolean main = MainThread.isMainThread();
        T value;
        if (main) {
            mDatabase.mLiveQueryDepth++;
        }
        try {
            value = compute();
        } finally {
            if (main) {
                mDatabase.mLiveQueryDepth--;
            }
        }
        if (main) {
            setValue(value);
        } else {
            postValue(value);
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
