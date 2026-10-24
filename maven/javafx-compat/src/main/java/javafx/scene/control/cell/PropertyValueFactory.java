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
package javafx.scene.control.cell;

import com.codename1.fxcompat.runtime.PropertyAccess;

import javafx.beans.NamedArg;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.util.Callback;

/// A cell value factory that takes the value of a column from a property
/// of the row's item, named as a string.
///
/// For the name `firstName` it answers what the item's
/// `firstNameProperty()` returns, so the cell follows the property. An
/// item without that method is asked `getFirstName()`, then
/// `isFirstName()`, and the answer is wrapped in a value that does not
/// change.
///
/// #### No reflection
///
/// JavaFX finds those methods by reflection. Here the build generates the
/// calls, for every public class of the application that has at least one
/// public `xxxProperty()` method, and for a public class with an accessor
/// for a name that is handed to this constructor as a constant. The
/// methods have to be public and take no argument.
///
/// A row whose class or property the build generated nothing for is an
/// `IllegalStateException` that names both; JavaFX logs a warning and
/// shows an empty cell. A lambda -- `c -> c.getValue().firstNameProperty()`
/// -- needs none of this and is the way around it.
public class PropertyValueFactory<S, T> implements Callback<CellDataFeatures<S, T>, ObservableValue<T>> {

    private final String property;

    /// Creates a factory for the property of a name.
    public PropertyValueFactory(@NamedArg("property") String property) {
        this.property = property;
    }

    /// Returns the name of the property.
    public final String getProperty() {
        return property;
    }

    /// Returns the value of the property for the item of a row, or `null`
    /// for a row without an item.
    @Override
    @SuppressWarnings("unchecked")
    public ObservableValue<T> call(CellDataFeatures<S, T> param) {
        return cn1ValueOf(param == null ? null : param.getValue(), property);
    }

    /// The value of a property of a bean, as [#call(CellDataFeatures)]
    /// answers it.
    @SuppressWarnings("unchecked")
    static <T> ObservableValue<T> cn1ValueOf(Object bean, String property) {
        if (bean == null || property == null || property.length() == 0) {
            return null;
        }
        PropertyAccess access = PropertyAccess.current();
        Object observable = access.call(bean, property + "Property");
        if (observable instanceof ObservableValue) {
            return (ObservableValue<T>) observable;
        }
        // As JavaFX does: the first letter in upper case, the rest as given.
        char first = property.charAt(0);
        String capital = (first >= 'a' && first <= 'z' ? (char) (first - 'a' + 'A') : first) + property.substring(1);
        Object value = access.call(bean, "get" + capital);
        if (value == PropertyAccess.NONE) {
            value = access.call(bean, "is" + capital);
        }
        if (value == PropertyAccess.NONE) {
            String type = bean.getClass().getName();
            throw new IllegalStateException(access.knows(bean)
                    ? "PropertyValueFactory: " + type + " has no public " + property + "Property(), get" + capital
                            + "() or is" + capital + "() method without arguments"
                    : "PropertyValueFactory: no accessors were generated for " + type + " (property \"" + property
                            + "\"). The class must be public and have a public xxxProperty() method, or the"
                            + " property name must be a constant where the PropertyValueFactory is created;"
                            + " otherwise use a lambda as the cell value factory");
        }
        return new ReadOnlyObjectWrapper<T>((T) value);
    }
}
