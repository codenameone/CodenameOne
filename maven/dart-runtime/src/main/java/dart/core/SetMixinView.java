/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.core;

import dart.collection.SetMixin;

/// A live {@code Set} view of a program object that applies dart:collection's
/// {@code SetMixin} (or extends {@code SetBase}), made where the object is passed to a
/// parameter or variable declared {@code Set}. Like {@link ListMixinView} it holds no
/// storage: every membership test, addition, removal and iteration goes to the object's
/// own {@code SetMixin} members, so writes through the view reach the object.
/// {@link #source()} answers the object, for identity and for a cast back to its class.
public final class SetMixinView<E> extends DartSet<E> implements MixinView {

    private final SetMixin<E> src;

    public SetMixinView(SetMixin<E> src) {
        this.src = src;
    }

    @Override
    public Object source() {
        return src;
    }

    /// The object's own toString, as Dart prints the object, not a re-rendering.
    @Override
    public String toString() {
        return dart.runtime.DartRuntime.str(src);
    }

    @Override
    public boolean add(E e) {
        boolean added = src.add(e);
        if (added) {
            structuralChange();
        }
        return added;
    }

    @Override
    public boolean contains(Object e) {
        return src.contains(e);
    }

    @Override
    public E lookup(Object object) {
        return src.lookup(object);
    }

    @Override
    public boolean remove(Object e) {
        boolean removed = src.remove(e);
        if (removed) {
            structuralChange();
        }
        return removed;
    }

    @Override
    public int size() {
        return (int) src.length();
    }

    @Override
    public boolean isEmpty() {
        return src.length() == 0;
    }

    @Override
    public void clear() {
        if (src.length() != 0) {
            structuralChange();
        }
        src.clear();
    }

    @Override
    public DartSet<E> toSet() {
        return src.toSet();
    }

    @Override
    public java.util.Iterator<E> iterator() {
        // Snapshot-free: steps the object's own iterator. remove() goes through the
        // object's remove, after the step that produced the element.
        final dart.collection.Iterator<E> it = src.iterator();
        return checkedIterator(new java.util.Iterator<E>() {
            private boolean ready;
            private boolean has;
            private E last;
            private boolean canRemove;

            @Override
            public boolean hasNext() {
                if (!ready) {
                    has = it.moveNext();
                    ready = true;
                }
                return has;
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new java.util.NoSuchElementException();
                }
                ready = false;
                last = it.current();
                canRemove = true;
                return last;
            }

            @Override
            public void remove() {
                if (!canRemove) {
                    throw new IllegalStateException();
                }
                canRemove = false;
                src.remove(last);
            }
        });
    }
}
