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

import dart.runtime.Funcs;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/// Live views that retype the elements of a container built by generic code.
///
/// A generic body (`Map<String, List<T>> byKey()` on a `Box<int>`) builds its lists boxed,
/// as `DartList<Long>`, because it cannot know T is int. The caller's static type
/// `Map<String, List<int>>` holds every `List<int>` as a `DartLongList`, so wherever a
/// substituted type argument puts `List<int>` (or `List<double>`) inside another type -- a
/// map value, a list element, an iterable, a future or a stream -- the value is seen
/// through one of these views. Each reads the same container and converts an element as
/// it is read (`DartLongList.viewLongs`, itself live), so a write through either side
/// reaches the same object. A converted element is a subtype of what the container holds
/// once erased (a `DartLongList` is a `DartList<Long>`, a nested view is a `DartMap` or a
/// `DartList`), so writes store the value as given.
public final class ElementViews {

    private ElementViews() {
    }

    /// `list`, its elements read through `read`.
    public static <S, T> DartList<T> list(DartList<S> list, Funcs.Func1<S, T> read) {
        return list == null ? null : new ListView<S, T>(list, read);
    }

    /// `map`, its values read through `read`. Keys are left as they are: a lookup hashes
    /// the key, and a retyped key would not find the entry it names.
    public static <K, S, T> DartMap<K, T> map(DartMap<K, S> map, Funcs.Func1<S, T> read) {
        return map == null ? null : new MapView<K, S, T>(map, read);
    }

    /// `iterable`, lazily mapped: an Iterable has no writes to forward. The Dart object is
    /// the source, so the view answers the source's element type.
    public static <S, T> DartIterable<T> iterable(Iterable<S> iterable, Funcs.Func1<S, T> read) {
        return iterable == null ? null
                : DartIterable.typed(DartIterable.wrap(iterable).map(read), DartIterable.witnessOf(iterable));
    }

    /// Dart's `list.cast<T>()`: a live view whose reads are checked against {@code target}
    /// and whose reified element type is {@code target} (null: a type nothing can name,
    /// left unchecked and unknown).
    public static <S, T> DartList<T> castList(DartList<S> list, dart.runtime.TypeWitness target) {
        return list == null ? null : new ListView<S, T>(list, ElementViews.<S, T>checker(target), target);
    }

    /// Dart's `map.cast<K, V>()`: values read through the check on {@code value}; the view
    /// records both target arguments. Keys are not rewritten (a lookup hashes them).
    public static <K, S, K2, T> DartMap<K2, T> castMap(DartMap<K, S> map, dart.runtime.TypeWitness key,
            dart.runtime.TypeWitness value) {
        if (map == null) {
            return null;
        }
        MapView<K, S, T> v = new MapView<K, S, T>(map, ElementViews.<S, T>checker(value));
        v.keyTarget = key;
        v.valueTarget = value;
        @SuppressWarnings("unchecked")
        DartMap<K2, T> r = (DartMap<K2, T>) (DartMap<?, ?>) v;
        return r;
    }

    /// Dart's `set.cast<T>()`: a live view, reads checked against {@code target}.
    public static <S, T> DartSet<T> castSet(DartSet<S> set, dart.runtime.TypeWitness target) {
        return set == null ? null : new SetView<S, T>(set, ElementViews.<S, T>checker(target), target);
    }

    /// Dart's `iterable.cast<T>()`: lazily checked, recording {@code target}.
    public static <S, T> DartIterable<T> castIterable(Iterable<S> it, dart.runtime.TypeWitness target) {
        return it == null ? null : DartIterable.typed(DartIterable.wrap(it).map(ElementViews.<S, T>checker(target)),
                target);
    }

    @SuppressWarnings("unchecked")
    private static <S, T> Funcs.Func1<S, T> checker(final dart.runtime.TypeWitness target) {
        return new Funcs.Func1<S, T>() {
            @Override
            public T call(S value) {
                if (target != null && !target.test(value)) {
                    throw new TypeError("type '" + dart.runtime.DartRuntime.str(dart.runtime.TypeWitness.runtimeType(value))
                            + "' is not a subtype of type '" + target + "' in type cast");
                }
                return (T) value;
            }
        };
    }

    private static final class SetView<S, T> extends DartSet<T> implements MixinView {
        private final DartSet<S> src;
        private final Funcs.Func1<S, T> read;
        private final dart.runtime.TypeWitness target;

        SetView(DartSet<S> src, Funcs.Func1<S, T> read, dart.runtime.TypeWitness target) {
            this.src = src;
            this.read = read;
            this.target = target;
        }

        @Override
        public Object source() {
            return src;
        }

        @Override
        public dart.runtime.TypeWitness elementWitness() {
            return target != null ? target : src.elementWitness();
        }

        @Override
        public Object $runtimeType() {
            return dart.runtime.TypeWitness.of(DartSet.class, false, elementWitness());
        }

        @Override
        public int size() {
            return src.size();
        }

        @Override
        public boolean isEmpty() {
            return src.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return src.contains(o);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean add(T e) {
            return src.add((S) e);
        }

        @Override
        public boolean remove(Object o) {
            return src.remove(o);
        }

        @Override
        public void clear() {
            src.clear();
        }

        @Override
        public java.util.Iterator<T> iterator() {
            final java.util.Iterator<S> it = src.iterator();
            return new java.util.Iterator<T>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public T next() {
                    return read.call(it.next());
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }
    }

    /// `future`, its value converted when it completes.
    public static <S, T> dart.async.Future<T> future(dart.async.Future<S> future, final Funcs.Func1<S, T> read) {
        if (future == null) {
            return null;
        }
        Funcs.Func1<S, Object> f = new Funcs.Func1<S, Object>() {
            @Override
            public Object call(S value) {
                return read.call(value);
            }
        };
        return future.<T>then(f);
    }

    /// `stream`, every event converted.
    public static <S, T> dart.async.Stream<T> stream(dart.async.Stream<S> stream, Funcs.Func1<S, T> read) {
        return stream == null ? null : stream.map(read);
    }

    private static final class ListView<S, T> extends DartList<T> implements MixinView {
        private final DartList<S> src;
        private final Funcs.Func1<S, T> read;

        /// The view's reified element type: a cast's target, else the source's own (a
        /// primitive-list conversion views the same Dart object).
        private final dart.runtime.TypeWitness target;

        ListView(DartList<S> src, Funcs.Func1<S, T> read) {
            this(src, read, null);
        }

        ListView(DartList<S> src, Funcs.Func1<S, T> read, dart.runtime.TypeWitness target) {
            super(true, false);
            this.src = src;
            this.read = read;
            this.target = target;
        }

        @Override
        public dart.runtime.TypeWitness elementWitness() {
            return target != null ? target : src.elementWitness();
        }

        @Override
        public Object source() {
            return src;
        }

        @Override
        public T get(int index) {
            return read.call(src.get(index));
        }

        @Override
        @SuppressWarnings("unchecked")
        public T set(int index, T element) {
            return read.call(src.set(index, (S) element));
        }

        @Override
        public int size() {
            return src.size();
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean add(T e) {
            modCount++;
            return src.add((S) e);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void add(int index, T element) {
            modCount++;
            src.add(index, (S) element);
        }

        @Override
        public T remove(int index) {
            modCount++;
            return read.call(src.remove(index));
        }

        @Override
        void setLengthUnchecked(int n) {
            modCount++;
            src.length(n);
        }
    }

    private static final class MapView<K, S, T> extends DartMap<K, T> implements MixinView {
        private final DartMap<K, S> src;
        private final Funcs.Func1<S, T> read;

        private dart.runtime.TypeWitness keyTarget;
        private dart.runtime.TypeWitness valueTarget;

        MapView(DartMap<K, S> src, Funcs.Func1<S, T> read) {
            this.src = src;
            this.read = read;
        }

        @Override
        public dart.runtime.TypeWitness keyWitness() {
            return keyTarget != null ? keyTarget : src.keyWitness();
        }

        @Override
        public dart.runtime.TypeWitness valueWitness() {
            return valueTarget != null ? valueTarget : src.valueWitness();
        }

        @Override
        public Object $runtimeType() {
            return dart.runtime.TypeWitness.of(DartMap.class, false, keyWitness(), valueWitness());
        }

        @Override
        public Object source() {
            return src;
        }

        private T in(S value) {
            return value == null ? null : read.call(value);
        }

        @Override
        public T get(Object key) {
            return in(src.get(key));
        }

        @Override
        public boolean containsKey(Object key) {
            return src.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return values().contains(value);
        }

        @Override
        @SuppressWarnings("unchecked")
        public T put(K key, T value) {
            structuralChange();
            return in(src.put(key, (S) value));
        }

        @Override
        public T remove(Object key) {
            structuralChange();
            return in(src.remove(key));
        }

        @Override
        public void clear() {
            structuralChange();
            src.clear();
        }

        @Override
        public int size() {
            return src.size();
        }

        @Override
        public boolean isEmpty() {
            return src.isEmpty();
        }

        @Override
        public Set<K> keySet() {
            return src.keySet();
        }

        @Override
        public Collection<T> values() {
            return new AbstractCollection<T>() {
                @Override
                public Iterator<T> iterator() {
                    final Iterator<S> it = src.values().iterator();
                    return new Iterator<T>() {
                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        @Override
                        public T next() {
                            return in(it.next());
                        }

                        @Override
                        public void remove() {
                            it.remove();
                        }
                    };
                }

                @Override
                public int size() {
                    return src.size();
                }
            };
        }

        @Override
        public Set<Map.Entry<K, T>> entrySet() {
            return new AbstractSet<Map.Entry<K, T>>() {
                @Override
                public Iterator<Map.Entry<K, T>> iterator() {
                    final Iterator<Map.Entry<K, S>> it = src.entrySet().iterator();
                    return new Iterator<Map.Entry<K, T>>() {
                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        @Override
                        public Map.Entry<K, T> next() {
                            final Map.Entry<K, S> e = it.next();
                            return new AbstractMap.SimpleEntry<K, T>(e.getKey(), in(e.getValue())) {
                                @Override
                                @SuppressWarnings("unchecked")
                                public T setValue(T value) {
                                    super.setValue(value);
                                    return in(e.setValue((S) value));
                                }
                            };
                        }

                        @Override
                        public void remove() {
                            it.remove();
                        }
                    };
                }

                @Override
                public int size() {
                    return src.size();
                }
            };
        }
    }
}
