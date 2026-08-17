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
package com.codename1.impl.home;

import com.codename1.impl.async.EdtResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The in-flight requests of one kind, keyed by the id the native side will
/// answer with.
///
/// #### One map per result type, on purpose
///
/// The obvious design is one map holding every pending request, and it needs a
/// cast at every completion to get the value type back. This codebase has a
/// hard rule against a cast whose failure you expect to handle -- ParparVM
/// does not check `CHECKCAST`, so on iOS a wrong one does not throw, it hands
/// the wrong object to the next instruction. A generic holder per result type
/// costs a field and removes the cast entirely.
///
/// Request ids are allocated from one counter across every kind, so an id
/// lives in exactly one of these and a reply cannot be matched to the wrong
/// operation.
///
/// #### Parameters
///
/// - `<T>`: what the operation resolves with
public final class PendingMap<T> {

    private final Map<Integer, EdtResult<T>> map =
            new HashMap<Integer, EdtResult<T>>();

    /// Registers a request and returns the resource its answer will complete.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id the native side will answer with
    ///
    /// #### Returns
    ///
    /// the resource to hand to the caller
    public synchronized EdtResult<T> open(int requestId) {
        EdtResult<T> result = new EdtResult<T>();
        map.put(Integer.valueOf(requestId), result);
        return result;
    }

    /// Claims a request's resource, removing it.
    ///
    /// Returns `null` for an id that is not in flight, which happens for a
    /// duplicate answer, an answer that arrived after the caller cancelled,
    /// and an answer from a port that got its bookkeeping wrong. All three are
    /// handled the same way -- ignored -- rather than being distinguished,
    /// because none of them has a caller left to tell.
    ///
    /// #### Parameters
    ///
    /// - `requestId`: the id being answered
    ///
    /// #### Returns
    ///
    /// the resource, or `null`
    public synchronized EdtResult<T> take(int requestId) {
        return map.remove(Integer.valueOf(requestId));
    }

    /// Fails and clears everything in flight.
    ///
    /// For a backend that has gone away -- the bridge stopped, the user
    /// revoked access. A pending request whose answer will never come is worse
    /// than a failed one: the caller waits forever with no way to know.
    ///
    /// #### Parameters
    ///
    /// - `failure`: what to fail them with
    public void failAll(Throwable failure) {
        List<EdtResult<T>> doomed;
        synchronized (this) {
            if (map.isEmpty()) {
                return;
            }
            doomed = new ArrayList<EdtResult<T>>(map.values());
            map.clear();
        }
        // Outside the monitor: completing a resource runs its callbacks, and
        // a callback that starts another request would otherwise deadlock
        // against this lock.
        for (EdtResult<T> result : doomed) {
            result.error(failure);
        }
    }

    /// How many requests are in flight.
    ///
    /// #### Returns
    ///
    /// the count
    public synchronized int size() {
        return map.size();
    }
}
