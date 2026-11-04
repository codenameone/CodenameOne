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
package java.util.concurrent;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/// `java.util.concurrent.ConcurrentLinkedQueue`: an unbounded thread safe
/// FIFO queue. It is lock based rather than lock free, like
/// `LinkedBlockingQueue` here: every operation holds the list's monitor.
/// Iteration walks a snapshot taken when the iterator was created, so it
/// never throws `ConcurrentModificationException`; `remove` on the iterator
/// removes that element from the queue.
public class ConcurrentLinkedQueue<E> extends AbstractQueue<E> implements java.io.Serializable {

    private final LinkedList<E> list = new LinkedList<E>();

    public ConcurrentLinkedQueue() {
    }

    public ConcurrentLinkedQueue(Collection<? extends E> c) {
        for (E e : c) {
            offer(e);
        }
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        synchronized (list) {
            list.addLast(e);
        }
        return true;
    }

    @Override
    public boolean add(E e) {
        return offer(e);
    }

    @Override
    public E poll() {
        synchronized (list) {
            return list.isEmpty() ? null : list.removeFirst();
        }
    }

    @Override
    public E peek() {
        synchronized (list) {
            return list.isEmpty() ? null : list.getFirst();
        }
    }

    @Override
    public int size() {
        synchronized (list) {
            return list.size();
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (list) {
            return list.isEmpty();
        }
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        synchronized (list) {
            return list.contains(o);
        }
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }
        synchronized (list) {
            return list.remove(o);
        }
    }

    @Override
    public void clear() {
        synchronized (list) {
            list.clear();
        }
    }

    @Override
    public Object[] toArray() {
        synchronized (list) {
            return list.toArray();
        }
    }

    @Override
    public <T> T[] toArray(T[] a) {
        synchronized (list) {
            return list.toArray(a);
        }
    }

    @Override
    public Iterator<E> iterator() {
        final ArrayList<E> snapshot;
        synchronized (list) {
            snapshot = new ArrayList<E>(list);
        }
        return new Iterator<E>() {
            private int next;
            private E last;

            @Override
            public boolean hasNext() {
                return next < snapshot.size();
            }

            @Override
            public E next() {
                if (next >= snapshot.size()) {
                    throw new NoSuchElementException();
                }
                last = snapshot.get(next++);
                return last;
            }

            @Override
            public void remove() {
                if (last == null) {
                    throw new IllegalStateException();
                }
                synchronized (list) {
                    // This exact element, not one equal to it.
                    Iterator<E> it = list.iterator();
                    while (it.hasNext()) {
                        if (it.next() == last) {
                            it.remove();
                            break;
                        }
                    }
                }
                last = null;
            }
        };
    }
}
