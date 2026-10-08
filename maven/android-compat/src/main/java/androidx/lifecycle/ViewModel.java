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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// State a screen keeps across configuration changes: an activity or
/// fragment gets the same instance back after it is recreated, until it is
/// finished or removed for good, when [#onCleared()] runs.
public abstract class ViewModel {

    private final Map<String, Object> mBag = new HashMap<String, Object>();
    private final List<AutoCloseable> mCloseables = new ArrayList<AutoCloseable>();
    private boolean mCleared;

    public ViewModel() {
    }

    public ViewModel(AutoCloseable... closeables) {
        for (AutoCloseable c : closeables) {
            mCloseables.add(c);
        }
    }

    /// Called once, when the owner is gone for good.
    protected void onCleared() {
    }

    /// Closed (after [#onCleared()]) when the view model is cleared; closed
    /// at once if it already was.
    public void addCloseable(AutoCloseable closeable) {
        if (mCleared) {
            closeQuietly(closeable);
            return;
        }
        mCloseables.add(closeable);
    }

    public void addCloseable(String key, AutoCloseable closeable) {
        if (mCleared) {
            closeQuietly(closeable);
            return;
        }
        Object old = mBag.put(key, closeable);
        if (old instanceof AutoCloseable && old != closeable) {
            closeQuietly((AutoCloseable) old);
        }
    }

    public AutoCloseable getCloseable(String key) {
        Object o = mBag.get(key);
        return o instanceof AutoCloseable ? (AutoCloseable) o : null;
    }

    final void clear() {
        mCleared = true;
        for (Object o : mBag.values()) {
            if (o instanceof AutoCloseable) {
                closeQuietly((AutoCloseable) o);
            }
        }
        for (AutoCloseable c : mCloseables) {
            closeQuietly(c);
        }
        mCloseables.clear();
        onCleared();
    }

    private static void closeQuietly(AutoCloseable c) {
        try {
            c.close();
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }
    }
}
