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

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

import javafx.beans.InvalidationListener;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

/// The observable set `FXCollections` hands out: a `java.util.Set` whose
/// modifications are reported one element at a time.
public final class ObservableSetWrapper<E> extends AbstractSet<E> implements ObservableSet<E> {

    private final Set<E> backing;
    private ListenerSet<SetChangeListener<? super E>> listeners;

    /// Wraps a set.
    public ObservableSetWrapper(Set<E> backing) {
        this.backing = backing;
    }

    @Override
    public void addListener(InvalidationListener listener) {
        listeners = ListenerSet.addInvalidation(listeners, listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        listeners = ListenerSet.removeInvalidation(listeners, listener);
    }

    @Override
    public void addListener(SetChangeListener<? super E> listener) {
        listeners = ListenerSet.addChange(listeners, listener);
    }

    @Override
    public void removeListener(SetChangeListener<? super E> listener) {
        listeners = ListenerSet.removeChange(listeners, listener);
    }

    private void fire(E element, boolean added) {
        if (ListenerSet.hasListeners(listeners)) {
            Changes.fire(listeners, this, new Changes.OfSet<E>(this, element, added));
        }
    }

    @Override
    public int size() {
        return backing.size();
    }

    @Override
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return backing.contains(o);
    }

    @Override
    public boolean add(E element) {
        if (backing.add(element)) {
            fire(element, true);
            return true;
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object o) {
        if (backing.remove(o)) {
            fire((E) o, false);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        for (Iterator<E> it = backing.iterator(); it.hasNext();) {
            E element = it.next();
            it.remove();
            fire(element, false);
        }
    }

    @Override
    public Iterator<E> iterator() {
        final Iterator<E> source = backing.iterator();
        return new Iterator<E>() {
            private E last;

            @Override
            public boolean hasNext() {
                return source.hasNext();
            }

            @Override
            public E next() {
                last = source.next();
                return last;
            }

            @Override
            public void remove() {
                source.remove();
                fire(last, false);
            }
        };
    }
}
