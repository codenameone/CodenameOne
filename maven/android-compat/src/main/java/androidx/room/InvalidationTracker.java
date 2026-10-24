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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/// Tells observers which tables changed. Room on Android learns this from
/// triggers; here the generated DAOs report every table a write names, and a
/// change made inside a transaction is reported once the outermost
/// transaction ends. A statement the application runs itself through
/// `getOpenHelper()` is not seen, as no trigger watches it.
public class InvalidationTracker {

    private final RoomDatabase mDatabase;
    private final String[] mTableNames;
    private final List<Observer> mObservers = new ArrayList<Observer>();
    private final Set<String> mPending = new HashSet<String>();

    /// An observer of a set of tables, named case-insensitively.
    public abstract static class Observer {
        final String[] mTables;

        protected Observer(String firstTable, String... rest) {
            this(join(firstTable, rest));
        }

        public Observer(String[] tables) {
            mTables = new String[tables.length];
            for (int i = 0; i < tables.length; i++) {
                mTables[i] = asciiLower(tables[i]);
            }
        }

        public abstract void onInvalidated(Set<String> tables);

        boolean isRemote() {
            return false;
        }

        private static String[] join(String first, String[] rest) {
            String[] all = new String[rest.length + 1];
            all[0] = first;
            System.arraycopy(rest, 0, all, 1, rest.length);
            return all;
        }
    }

    public InvalidationTracker(RoomDatabase database, String... tableNames) {
        mDatabase = database;
        mTableNames = new String[tableNames.length];
        for (int i = 0; i < tableNames.length; i++) {
            mTableNames[i] = asciiLower(tableNames[i]);
        }
    }

    public void addObserver(Observer observer) {
        for (String t : observer.mTables) {
            if (!knows(t)) {
                throw new IllegalArgumentException("There is no table with name " + t);
            }
        }
        if (!mObservers.contains(observer)) {
            mObservers.add(observer);
        }
    }

    public void removeObserver(Observer observer) {
        mObservers.remove(observer);
    }

    /// Delivers what is pending. Room schedules this after a write; here a
    /// write outside a transaction is delivered at once.
    public void refreshVersionsAsync() {
        flush();
    }

    public void refreshVersionsSync() {
        flush();
    }

    /// Records that the named tables changed. Generated DAOs call it after
    /// every write.
    public void notifyTablesChanged(String... tables) {
        for (String t : tables) {
            mPending.add(asciiLower(t));
        }
        if (!mDatabase.inTransaction()) {
            flush();
        }
    }

    void flush() {
        if (mPending.isEmpty()) {
            return;
        }
        Set<String> changed = new HashSet<String>(mPending);
        mPending.clear();
        List<Observer> copy = new ArrayList<Observer>(mObservers);
        for (Observer o : copy) {
            Set<String> hit = null;
            for (String t : o.mTables) {
                if (changed.contains(t)) {
                    if (hit == null) {
                        hit = new HashSet<String>();
                    }
                    hit.add(t);
                }
            }
            if (hit != null) {
                o.onInvalidated(Collections.unmodifiableSet(hit));
            }
        }
    }

    /// A live query: computed when it gains an active observer and again
    /// after every change to `tableNames` while it has one.
    public <T> LiveData<T> createLiveData(String[] tableNames, boolean inTransaction, Callable<T> computeFunction) {
        return new RoomTrackingLiveData<T>(mDatabase, tableNames, inTransaction, computeFunction);
    }

    public <T> LiveData<T> createLiveData(String[] tableNames, Callable<T> computeFunction) {
        return createLiveData(tableNames, false, computeFunction);
    }

    private boolean knows(String table) {
        for (String t : mTableNames) {
            if (t.equals(table)) {
                return true;
            }
        }
        return false;
    }

    /// ASCII-only lower case: `toLowerCase()` follows the device locale.
    static String asciiLower(String s) {
        char[] out = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (out == null) {
                    out = s.toCharArray();
                }
                out[i] = (char) (c + ('a' - 'A'));
            }
        }
        return out == null ? s : new String(out);
    }
}
