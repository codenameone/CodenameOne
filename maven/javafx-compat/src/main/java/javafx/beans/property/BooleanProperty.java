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

import com.codename1.fxcompat.runtime.BidirectionalBinding;
import com.codename1.fxcompat.runtime.PropertyText;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.beans.value.WritableBooleanValue;

/// A property holding a boolean that can be read, written, observed and bound.
public abstract class BooleanProperty extends ReadOnlyBooleanProperty implements Property<Boolean>, WritableBooleanValue {

    /// Creates the property.
    public BooleanProperty() {
    }

    @Override
    public void setValue(Boolean v) {
        if (v == null) {
            set(false);
        } else {
            set(v.booleanValue());
        }
    }

    @Override
    public void bindBidirectional(Property<Boolean> other) {
        Bindings.bindBidirectional(this, other);
    }

    @Override
    public void unbindBidirectional(Property<Boolean> other) {
        Bindings.unbindBidirectional(this, other);
    }

    /// Returns a boolean property kept in sync, in both directions, with a boxed
    /// property. Neither keeps the other alive.
    public static BooleanProperty booleanProperty(final Property<Boolean> property) {
        if (property == null) {
            throw new NullPointerException("Property cannot be null");
        }
        BooleanPropertyBase result = new BooleanPropertyBase() {
            @Override
            public Object getBean() {
                return null;
            }

            @Override
            public String getName() {
                return property.getName();
            }
        };
        BidirectionalBinding.bind(result, property, new Function<Boolean, Boolean>() {
            @Override
            public Boolean apply(Boolean v) {
                return v;
            }
        }, new Function<Boolean, Boolean>() {
            @Override
            public Boolean apply(Boolean v) {
                return Boolean.valueOf(v != null && v.booleanValue());
            }
        });
        return result;
    }

    /// Returns an object property holding the boxed value, kept in sync with
    /// this one in both directions.
    @Override
    public ObjectProperty<Boolean> asObject() {
        ObjectPropertyBase<Boolean> result = new ObjectPropertyBase<Boolean>() {
            @Override
            public Object getBean() {
                return null;
            }

            @Override
            public String getName() {
                return BooleanProperty.this.getName();
            }
        };
        Function<Boolean, Boolean> box = v -> Boolean.valueOf(v != null && v.booleanValue());
        Function<Boolean, Boolean> unbox = v -> v;
        BidirectionalBinding.bind(result, this, box, unbox);
        return result;
    }

    @Override
    public String toString() {
        return PropertyText.describe("BooleanProperty", this, "value: " + get());
    }
}
