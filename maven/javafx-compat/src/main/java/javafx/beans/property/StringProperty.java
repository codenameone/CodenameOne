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

import com.codename1.fxcompat.runtime.PropertyText;
import javafx.beans.binding.Bindings;
import javafx.beans.value.WritableStringValue;
import javafx.util.StringConverter;

/// A property holding a string that can be read, written, observed and bound.
public abstract class StringProperty extends ReadOnlyStringProperty implements Property<String>, WritableStringValue {

    /// Creates the property.
    public StringProperty() {
    }

    @Override
    public void setValue(String v) {
        set(v);
    }

    @Override
    public void bindBidirectional(Property<String> other) {
        Bindings.bindBidirectional(this, other);
    }

    @Override
    public void unbindBidirectional(Property<String> other) {
        Bindings.unbindBidirectional(this, other);
    }

    /// Keeps this property and another one of any type in sync through a
    /// converter: this side holds the text, the other the converted value.
    public <T> void bindBidirectional(Property<T> other, StringConverter<T> converter) {
        Bindings.bindBidirectional(this, other, converter);
    }

    /// Removes a bidirectional binding to any object, converter based or not.
    public void unbindBidirectional(Object other) {
        Bindings.unbindBidirectional(this, other);
    }

    @Override
    public String toString() {
        return PropertyText.describe("StringProperty", this, "value: " + get());
    }
}
