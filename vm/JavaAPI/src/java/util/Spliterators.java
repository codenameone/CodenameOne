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
package java.util;

import java.util.function.Consumer;

/// Ways to make a [Spliterator] of a collection or an iterator, and an
/// iterator of a spliterator.
public final class Spliterators {

    private Spliterators() {
    }

    /// A spliterator of no elements.
    public static <T> Spliterator<T> emptySpliterator() {
        return new IteratorSpliterator<T>(null, Collections.<T>emptyList().iterator(), 0,
                Spliterator.SIZED | Spliterator.SUBSIZED);
    }

    /// The elements of `c`. Its iterator is asked for when the first element
    /// is, not now, so elements added in between are seen, as the JDK's
    /// collections do.
    public static <T> Spliterator<T> spliterator(Collection<? extends T> c, int characteristics) {
        if (c == null) {
            throw new NullPointerException();
        }
        return new IteratorSpliterator<T>(c, null, -1, sized(characteristics));
    }

    /// The `size` elements of `iterator`.
    public static <T> Spliterator<T> spliterator(Iterator<? extends T> iterator, long size, int characteristics) {
        if (iterator == null) {
            throw new NullPointerException();
        }
        return new IteratorSpliterator<T>(null, iterator, size, sized(characteristics));
    }

    /// The elements of `iterator`, however many there are.
    public static <T> Spliterator<T> spliteratorUnknownSize(Iterator<? extends T> iterator, int characteristics) {
        if (iterator == null) {
            throw new NullPointerException();
        }
        return new IteratorSpliterator<T>(null, iterator, Long.MAX_VALUE,
                characteristics & ~(Spliterator.SIZED | Spliterator.SUBSIZED));
    }

    private static int sized(int characteristics) {
        return (characteristics & Spliterator.CONCURRENT) == 0
                ? characteristics | Spliterator.SIZED | Spliterator.SUBSIZED : characteristics;
    }

    /// The elements of `spliterator`, as an iterator.
    public static <T> Iterator<T> iterator(final Spliterator<? extends T> spliterator) {
        if (spliterator == null) {
            throw new NullPointerException();
        }
        return new Unspliterated<T>(spliterator);
    }

    /// What [#iterator(Spliterator)] answers: an element taken ahead to
    /// answer `hasNext`.
    private static final class Unspliterated<T> implements Iterator<T>, Consumer<T> {
        private final Spliterator<? extends T> spliterator;
        private boolean ready;
        private T next;

        Unspliterated(Spliterator<? extends T> spliterator) {
            this.spliterator = spliterator;
        }

        @Override
        public void accept(T t) {
            ready = true;
            next = t;
        }

        @Override
        public boolean hasNext() {
            if (!ready) {
                spliterator.tryAdvance(this);
            }
            return ready;
        }

        @Override
        public T next() {
            if (!ready && !hasNext()) {
                throw new NoSuchElementException();
            }
            ready = false;
            T out = next;
            next = null;
            return out;
        }
    }

    /// A spliterator over an iterator, or over the iterator a collection
    /// gives when it is first read.
    private static final class IteratorSpliterator<T> implements Spliterator<T> {
        private final Collection<? extends T> collection;
        private Iterator<? extends T> it;
        private long size;
        private final int characteristics;

        IteratorSpliterator(Collection<? extends T> collection, Iterator<? extends T> it, long size,
                            int characteristics) {
            this.collection = collection;
            this.it = it;
            this.size = size;
            this.characteristics = characteristics;
        }

        private Iterator<? extends T> open() {
            if (it == null) {
                it = collection.iterator();
                size = collection.size();
            }
            return it;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            Iterator<? extends T> source = open();
            if (!source.hasNext()) {
                return false;
            }
            action.accept(source.next());
            return true;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            Iterator<? extends T> source = open();
            while (source.hasNext()) {
                action.accept(source.next());
            }
        }

        @Override
        public Spliterator<T> trySplit() {
            return null;
        }

        /// The size given, not what is left: as in the JDK's IteratorSpliterator,
        /// handing elements out does not count it down (only a split would), so a
        /// caller that needs what remains has to count it itself.
        @Override
        public long estimateSize() {
            if (it == null) {
                return collection.size();
            }
            return size;
        }

        @Override
        public int characteristics() {
            return characteristics;
        }

        /// Natural order when the caller declared SORTED, as the JDK's does:
        /// an iterator carries no comparator to hand out.
        @Override
        public Comparator<? super T> getComparator() {
            if ((characteristics & SORTED) != 0) {
                return null;
            }
            throw new IllegalStateException();
        }
    }
}
