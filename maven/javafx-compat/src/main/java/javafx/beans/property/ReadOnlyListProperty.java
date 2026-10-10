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
package javafx.beans.property;

import java.util.List;
import java.util.ListIterator;

import com.codename1.fxcompat.runtime.PropertyText;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListExpression;
import javafx.collections.ObservableList;

/// A property holding an observable list that can be read and observed but
/// not replaced.
public abstract class ReadOnlyListProperty<E> extends ListExpression<E>
        implements ReadOnlyProperty<ObservableList<E>> {

    /// Creates the property.
    public ReadOnlyListProperty() {
    }

    /// Keeps the content of this property and of a list equal in both
    /// directions.
    public void bindContentBidirectional(ObservableList<E> list) {
        Bindings.bindContentBidirectional(this, list);
    }

    /// Removes a bidirectional content binding.
    public void unbindContentBidirectional(Object object) {
        Bindings.unbindContentBidirectional(this, object);
    }

    /// Makes the content of this property follow a list.
    public void bindContent(ObservableList<E> list) {
        Bindings.bindContent(this, list);
    }

    /// Removes a content binding.
    public void unbindContent(Object object) {
        Bindings.unbindContent(this, object);
    }

    /// Compares as a list: equal to any list of equal elements in the same
    /// order.
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof List)) {
            return false;
        }
        List<?> other = (List<?>) obj;
        if (size() != other.size()) {
            return false;
        }
        ListIterator<E> mine = listIterator();
        ListIterator<?> theirs = other.listIterator();
        while (mine.hasNext() && theirs.hasNext()) {
            E first = mine.next();
            Object second = theirs.next();
            if (!(first == null ? second == null : first.equals(second))) {
                return false;
            }
        }
        return true;
    }

    /// Returns the hash code of the content, as a list defines it.
    @Override
    public int hashCode() {
        int hashCode = 1;
        for (E element : this) {
            hashCode = 31 * hashCode + (element == null ? 0 : element.hashCode());
        }
        return hashCode;
    }

    @Override
    public String toString() {
        return PropertyText.describe("ReadOnlyListProperty", this, "value: " + get());
    }
}
