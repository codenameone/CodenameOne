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

import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;

/// A value that can be written, observed and bound to other values.
///
/// A property bound with [#bind(ObservableValue)] follows its source and
/// rejects every attempt to set it. Two properties bound with
/// [#bindBidirectional(Property)] follow each other and both stay writable.
public interface Property<T> extends ReadOnlyProperty<T>, WritableValue<T> {

    /// Makes this property follow another value.
    void bind(ObservableValue<? extends T> observable);

    /// Stops following the bound value; does nothing when not bound.
    void unbind();

    /// Returns whether the property currently follows another value.
    boolean isBound();

    /// Keeps this property and another one equal, starting with the value
    /// of the other.
    void bindBidirectional(Property<T> other);

    /// Removes a bidirectional binding with another property.
    void unbindBidirectional(Property<T> other);
}
