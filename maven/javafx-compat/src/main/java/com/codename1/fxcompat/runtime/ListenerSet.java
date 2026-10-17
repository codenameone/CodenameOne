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
package com.codename1.fxcompat.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakListener;

/// The listeners of one observable: its invalidation listeners and its
/// change listeners of type `L`.
///
/// Both lists are replaced, never edited, when a listener is added or
/// removed, so a notification in progress keeps walking the list it started
/// with and a listener may add or remove listeners freely. An owner with no
/// listeners holds `null` instead of an empty set, which is why every
/// operation is static and returns the set to keep.
public final class ListenerSet<L> {

    private List<InvalidationListener> invalidation = Collections.emptyList();
    private List<L> change = Collections.emptyList();

    private ListenerSet() {
    }

    /// Returns a copy with a listener appended and without the weak
    /// listeners whose target is gone.
    static <X> List<X> plus(List<X> source, X listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        List<X> result = new ArrayList<X>(source.size() + 1);
        for (int i = 0; i < source.size(); i++) {
            X existing = source.get(i);
            if (!(existing instanceof WeakListener) || !((WeakListener) existing).wasGarbageCollected()) {
                result.add(existing);
            }
        }
        result.add(listener);
        return result;
    }

    /// Returns a copy without the first listener equal to the given one, or
    /// the list itself when there is none.
    static <X> List<X> minus(List<X> source, Object listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        int index = source.indexOf(listener);
        if (index < 0) {
            return source;
        }
        List<X> result = new ArrayList<X>(source);
        result.remove(index);
        return result;
    }

    /// Adds an invalidation listener.
    public static <L> ListenerSet<L> addInvalidation(ListenerSet<L> set, InvalidationListener listener) {
        ListenerSet<L> result = set == null ? new ListenerSet<L>() : set;
        result.invalidation = plus(result.invalidation, listener);
        return result;
    }

    /// Removes an invalidation listener; returns `null` once nothing is left.
    public static <L> ListenerSet<L> removeInvalidation(ListenerSet<L> set, InvalidationListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        if (set == null) {
            return null;
        }
        set.invalidation = minus(set.invalidation, listener);
        return set.invalidation.isEmpty() && set.change.isEmpty() ? null : set;
    }

    /// Adds a change listener.
    public static <L> ListenerSet<L> addChange(ListenerSet<L> set, L listener) {
        ListenerSet<L> result = set == null ? new ListenerSet<L>() : set;
        result.change = plus(result.change, listener);
        return result;
    }

    /// Removes a change listener; returns `null` once nothing is left.
    public static <L> ListenerSet<L> removeChange(ListenerSet<L> set, Object listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        if (set == null) {
            return null;
        }
        set.change = minus(set.change, listener);
        return set.invalidation.isEmpty() && set.change.isEmpty() ? null : set;
    }

    /// Tells every invalidation listener that the observable is invalid.
    public static void fireInvalidation(ListenerSet<?> set, Observable observable) {
        if (set != null) {
            List<InvalidationListener> listeners = set.invalidation;
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).invalidated(observable);
            }
        }
    }

    /// Returns the change listeners as they are now; the result is never
    /// modified afterwards and must not be modified by the caller.
    public static <L> List<L> changeListeners(ListenerSet<L> set) {
        if (set == null) {
            return Collections.emptyList();
        }
        return set.change;
    }

    /// Returns whether the set holds any listener at all.
    public static boolean hasListeners(ListenerSet<?> set) {
        return set != null;
    }
}
