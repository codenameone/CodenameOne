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
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.BooleanExpression;

/// A property holding a boolean that can be read and observed but not written.
public abstract class ReadOnlyBooleanProperty extends BooleanExpression implements ReadOnlyProperty<Boolean> {

    /// Creates the property.
    public ReadOnlyBooleanProperty() {
    }

    /// Returns a read-only view of a boxed property as a boolean property. A `null`
    /// value reads as the default value.
    public static ReadOnlyBooleanProperty readOnlyBooleanProperty(final ReadOnlyProperty<Boolean> property) {
        if (property == null) {
            throw new NullPointerException("Property cannot be null");
        }
        if (property instanceof ReadOnlyBooleanProperty) {
            return (ReadOnlyBooleanProperty) property;
        }
        return new ReadOnlyBooleanPropertyBase() {
            private final InvalidationListener listener = new InvalidationListener() {
                @Override
                public void invalidated(Observable observable) {
                    fireValueChangedEvent();
                }
            };

            {
                property.addListener(new WeakInvalidationListener(listener));
            }

            @Override
            public boolean get() {
                Boolean v = property.getValue();
                return v != null && v.booleanValue();
            }

            @Override
            public Object getBean() {
                return null;
            }

            @Override
            public String getName() {
                return property.getName();
            }
        };
    }

    /// Returns a read-only object property holding the boxed value of this one.
    @Override
    public ReadOnlyObjectProperty<Boolean> asObject() {
        return new ReadOnlyObjectPropertyBase<Boolean>() {
            private final InvalidationListener listener = new InvalidationListener() {
                @Override
                public void invalidated(Observable observable) {
                    fireValueChangedEvent();
                }
            };

            {
                ReadOnlyBooleanProperty.this.addListener(new WeakInvalidationListener(listener));
            }

            @Override
            public Boolean get() {
                return ReadOnlyBooleanProperty.this.getValue();
            }

            @Override
            public Object getBean() {
                return null;
            }

            @Override
            public String getName() {
                return ReadOnlyBooleanProperty.this.getName();
            }
        };
    }

    @Override
    public String toString() {
        return PropertyText.describe("ReadOnlyBooleanProperty", this, "value: " + get());
    }
}
