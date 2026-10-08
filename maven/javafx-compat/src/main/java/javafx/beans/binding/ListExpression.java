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
package javafx.beans.binding;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableListValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// The fluent operations of an observable reference to an observable list.
///
/// The expression is itself a list: every list operation goes to the list
/// it currently holds. While it holds `null` it reads as an empty list and
/// cannot be modified.
public abstract class ListExpression<E> implements ObservableListValue<E> {

    /// Creates the expression.
    public ListExpression() {
    }

    private ObservableList<E> content() {
        ObservableList<E> list = get();
        return list == null ? FXCollections.<E>emptyObservableList() : list;
    }

    @Override
    public ObservableList<E> getValue() {
        return get();
    }

    /// Returns the number of elements.
    public int getSize() {
        return size();
    }

    /// Returns a property holding the number of elements.
    public abstract ReadOnlyIntegerProperty sizeProperty();

    /// Returns a property telling whether the list is empty.
    public abstract ReadOnlyBooleanProperty emptyProperty();

    /// Returns a binding holding the element at a position; `null` when the
    /// position is outside the list.
    public ObjectBinding<E> valueAt(int index) {
        return Bindings.valueAt(this, index);
    }

    /// Returns a binding holding the element at an observable position.
    public ObjectBinding<E> valueAt(ObservableIntegerValue index) {
        return Bindings.valueAt(this, index);
    }

    /// Returns a binding telling whether this list and another hold equal
    /// elements in the same order.
    public BooleanBinding isEqualTo(final ObservableList<?> other) {
        return new Fn.BooleanFn(() -> sameContent(other), this, other);
    }

    /// Returns a binding telling whether this list and another differ.
    public BooleanBinding isNotEqualTo(final ObservableList<?> other) {
        return new Fn.BooleanFn(() -> !sameContent(other), this, other);
    }

    private boolean sameContent(ObservableList<?> other) {
        ObservableList<E> list = get();
        if (list == null) {
            return other == null;
        }
        return other != null && list.equals(other);
    }

    /// Returns a binding telling whether no list is held.
    public BooleanBinding isNull() {
        return new Fn.BooleanFn(() -> get() == null, this);
    }

    /// Returns a binding telling whether a list is held.
    public BooleanBinding isNotNull() {
        return new Fn.BooleanFn(() -> get() != null, this);
    }

    /// Returns a binding holding the text of the list.
    public StringBinding asString() {
        return Bindings.text(this);
    }

    @Override
    public int size() {
        return content().size();
    }

    @Override
    public boolean isEmpty() {
        return content().isEmpty();
    }

    @Override
    public boolean contains(Object obj) {
        return content().contains(obj);
    }

    @Override
    public Iterator<E> iterator() {
        return content().iterator();
    }

    @Override
    public Object[] toArray() {
        return content().toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        return content().toArray(array);
    }

    @Override
    public boolean add(E element) {
        return content().add(element);
    }

    @Override
    public boolean remove(Object obj) {
        return content().remove(obj);
    }

    @Override
    public boolean containsAll(Collection<?> objects) {
        return content().containsAll(objects);
    }

    @Override
    public boolean addAll(Collection<? extends E> elements) {
        return content().addAll(elements);
    }

    @Override
    public boolean addAll(int i, Collection<? extends E> elements) {
        return content().addAll(i, elements);
    }

    @Override
    public boolean removeAll(Collection<?> objects) {
        return content().removeAll(objects);
    }

    @Override
    public boolean retainAll(Collection<?> objects) {
        return content().retainAll(objects);
    }

    @Override
    public void clear() {
        content().clear();
    }

    @Override
    public E get(int i) {
        return content().get(i);
    }

    @Override
    public E set(int i, E element) {
        return content().set(i, element);
    }

    @Override
    public void add(int i, E element) {
        content().add(i, element);
    }

    @Override
    public E remove(int i) {
        return content().remove(i);
    }

    @Override
    public int indexOf(Object obj) {
        return content().indexOf(obj);
    }

    @Override
    public int lastIndexOf(Object obj) {
        return content().lastIndexOf(obj);
    }

    @Override
    public ListIterator<E> listIterator() {
        return content().listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int i) {
        return content().listIterator(i);
    }

    @Override
    public List<E> subList(int from, int to) {
        return content().subList(from, to);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean addAll(E... elements) {
        return content().addAll(elements);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setAll(E... elements) {
        return content().setAll(elements);
    }

    @Override
    public boolean setAll(Collection<? extends E> elements) {
        return content().setAll(elements);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean removeAll(E... elements) {
        return content().removeAll(elements);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean retainAll(E... elements) {
        return content().retainAll(elements);
    }

    @Override
    public void remove(int from, int to) {
        content().remove(from, to);
    }
}
