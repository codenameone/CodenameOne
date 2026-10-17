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
package javafx.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.codename1.fxcompat.runtime.ArrayObservableList;
import com.codename1.fxcompat.runtime.ConstantObservableList;
import com.codename1.fxcompat.runtime.ObservableMapWrapper;
import com.codename1.fxcompat.runtime.ObservableSetWrapper;
import com.codename1.fxcompat.runtime.UnmodifiableViews;

import javafx.beans.Observable;
import javafx.util.Callback;

/// Creates observable collections and applies the bulk operations of
/// `java.util.Collections` to them so that each is reported as one change.
public class FXCollections {

    private FXCollections() {
    }

    /// Wraps a list; changes made through the wrapper are reported, changes
    /// made to the list directly are not.
    public static <E> ObservableList<E> observableList(List<E> list) {
        if (list == null) {
            throw new NullPointerException();
        }
        return new ArrayObservableList<E>(list, null);
    }

    /// Wraps a list and also reports, as an update, a change of any
    /// observable the extractor returns for an element.
    public static <E> ObservableList<E> observableList(List<E> list, Callback<E, Observable[]> extractor) {
        if (list == null || extractor == null) {
            throw new NullPointerException();
        }
        return new ArrayObservableList<E>(list, extractor);
    }

    /// Creates an empty observable list backed by an array list.
    public static <E> ObservableList<E> observableArrayList() {
        return observableList(new ArrayList<E>());
    }

    /// Creates an empty observable list that also reports, as an update, a
    /// change of any observable the extractor returns for an element.
    public static <E> ObservableList<E> observableArrayList(Callback<E, Observable[]> extractor) {
        return observableList(new ArrayList<E>(), extractor);
    }

    /// Creates an observable list holding the given elements.
    public static <E> ObservableList<E> observableArrayList(E... items) {
        ObservableList<E> list = observableArrayList();
        list.addAll(items);
        return list;
    }

    /// Creates an observable list holding a copy of a collection.
    public static <E> ObservableList<E> observableArrayList(Collection<? extends E> col) {
        ObservableList<E> list = observableArrayList();
        list.addAll(col);
        return list;
    }

    /// Wraps a map; changes made through the wrapper are reported.
    public static <K, V> ObservableMap<K, V> observableMap(Map<K, V> map) {
        if (map == null) {
            throw new NullPointerException();
        }
        return new ObservableMapWrapper<K, V>(map);
    }

    /// Creates an empty observable map backed by a hash map.
    public static <K, V> ObservableMap<K, V> observableHashMap() {
        return observableMap(new HashMap<K, V>());
    }

    /// Wraps a set; changes made through the wrapper are reported.
    public static <E> ObservableSet<E> observableSet(Set<E> set) {
        if (set == null) {
            throw new NullPointerException();
        }
        return new ObservableSetWrapper<E>(set);
    }

    /// Creates an observable set holding the given elements.
    public static <E> ObservableSet<E> observableSet(E... elements) {
        if (elements == null) {
            throw new NullPointerException();
        }
        Set<E> set = new HashSet<E>(elements.length);
        Collections.addAll(set, elements);
        return new ObservableSetWrapper<E>(set);
    }

    /// Returns a read-only view of a list that reports its changes.
    public static <E> ObservableList<E> unmodifiableObservableList(ObservableList<E> list) {
        if (list == null) {
            throw new NullPointerException();
        }
        return new UnmodifiableViews.OfList<E>(list);
    }

    /// Returns a read-only view of a map that reports its changes.
    public static <K, V> ObservableMap<K, V> unmodifiableObservableMap(ObservableMap<K, V> map) {
        if (map == null) {
            throw new NullPointerException();
        }
        return new UnmodifiableViews.OfMap<K, V>(map);
    }

    /// Returns a read-only view of a set that reports its changes.
    public static <E> ObservableSet<E> unmodifiableObservableSet(ObservableSet<E> set) {
        if (set == null) {
            throw new NullPointerException();
        }
        return new UnmodifiableViews.OfSet<E>(set);
    }

    /// Returns an empty list that can never change.
    public static <E> ObservableList<E> emptyObservableList() {
        return new ConstantObservableList<E>(Collections.<E>emptyList());
    }

    /// Returns an empty map that can never change.
    public static <K, V> ObservableMap<K, V> emptyObservableMap() {
        return new UnmodifiableViews.OfMap<K, V>(FXCollections.<K, V>observableHashMap());
    }

    /// Returns an empty set that can never change.
    public static <E> ObservableSet<E> emptyObservableSet() {
        return new UnmodifiableViews.OfSet<E>(observableSet(new HashSet<E>()));
    }

    /// Returns a list of one element that can never change.
    public static <E> ObservableList<E> singletonObservableList(E e) {
        return new ConstantObservableList<E>(Collections.singletonList(e));
    }

    /// Creates an observable list holding the content of several lists in
    /// a row. It is a copy and does not follow them.
    public static <E> ObservableList<E> concat(ObservableList<E>... lists) {
        ObservableList<E> result = observableArrayList();
        if (lists != null) {
            for (ObservableList<E> list : lists) {
                result.addAll(list);
            }
        }
        return result;
    }

    /// Sorts a list into its natural order. A list created by this class
    /// reports a permutation, any other a replacement of its content.
    public static <T extends Comparable<? super T>> void sort(ObservableList<T> list) {
        sort(list, null);
    }

    /// Sorts a list with a comparator. A list created by this class reports
    /// a permutation, any other a replacement of its content.
    public static <T> void sort(ObservableList<T> list, Comparator<? super T> c) {
        if (list instanceof ArrayObservableList) {
            list.sort(c);
        } else {
            List<T> copy = new ArrayList<T>(list);
            Collections.sort(copy, c);
            list.setAll(copy);
        }
    }

    /// Reverses a list, reported as a replacement of its content.
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void reverse(ObservableList list) {
        List copy = new ArrayList(list);
        Collections.reverse(copy);
        list.setAll(copy);
    }

    /// Shuffles a list with the given source of randomness, reported as a
    /// replacement of its content.
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void shuffle(ObservableList list, Random rnd) {
        List copy = new ArrayList(list);
        for (int i = copy.size() - 1; i > 0; i--) {
            int other = rnd.nextInt(i + 1);
            Object swapped = copy.get(i);
            copy.set(i, copy.get(other));
            copy.set(other, swapped);
        }
        list.setAll(copy);
    }

    /// Sets every element of a list to one object, reported as one change.
    public static <T> void fill(ObservableList<? super T> list, T obj) {
        List<T> copy = new ArrayList<T>(list.size());
        for (int i = list.size(); i > 0; i--) {
            copy.add(obj);
        }
        list.setAll(copy);
    }

    /// Copies a list over the start of another, which must be at least as
    /// long; reported as one change.
    public static <T> void copy(ObservableList<? super T> dest, List<? extends T> src) {
        final int srcSize = src.size();
        if (srcSize > dest.size()) {
            throw new IndexOutOfBoundsException("Source does not fit in dest");
        }
        List<Object> copy = new ArrayList<Object>(dest);
        for (int i = 0; i < srcSize; i++) {
            copy.set(i, src.get(i));
        }
        replaceContent(dest, copy);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void replaceContent(ObservableList dest, List content) {
        dest.setAll(content);
    }

    /// Replaces every occurrence of a value in a list; returns whether
    /// there was one.
    public static <T> boolean replaceAll(ObservableList<T> list, T oldVal, T newVal) {
        List<T> copy = new ArrayList<T>(list);
        boolean modified = false;
        for (int i = 0; i < copy.size(); i++) {
            T current = copy.get(i);
            if (current == null ? oldVal == null : current.equals(oldVal)) {
                copy.set(i, newVal);
                modified = true;
            }
        }
        if (modified) {
            list.setAll(copy);
        }
        return modified;
    }

    /// Rotates a list by a distance, reported as a replacement of its
    /// content.
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void rotate(ObservableList list, int distance) {
        List copy = new ArrayList(list);
        Collections.rotate(copy, distance);
        list.setAll(copy);
    }
}
