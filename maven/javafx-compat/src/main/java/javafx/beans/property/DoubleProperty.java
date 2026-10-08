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
import javafx.beans.value.WritableDoubleValue;

/// A property holding a `double` that can be read, written, observed and bound.
public abstract class DoubleProperty extends ReadOnlyDoubleProperty implements Property<Number>, WritableDoubleValue {

    /// Creates the property.
    public DoubleProperty() {
    }

    @Override
    public void setValue(Number v) {
        if (v == null) {
            set(0);
        } else {
            set(v.doubleValue());
        }
    }

    @Override
    public void bindBidirectional(Property<Number> other) {
        Bindings.bindBidirectional(this, other);
    }

    @Override
    public void unbindBidirectional(Property<Number> other) {
        Bindings.unbindBidirectional(this, other);
    }

    /// Returns a `double` property kept in sync, in both directions, with a boxed
    /// property. Neither keeps the other alive.
    public static DoubleProperty doubleProperty(final Property<Double> property) {
        if (property == null) {
            throw new NullPointerException("Property cannot be null");
        }
        DoublePropertyBase result = new DoublePropertyBase() {
            @Override
            public Object getBean() {
                return null;
            }

            @Override
            public String getName() {
                return property.getName();
            }
        };
        BidirectionalBinding.bind(result, property, new Function<Double, Number>() {
            @Override
            public Number apply(Double v) {
                return v;
            }
        }, new Function<Number, Double>() {
            @Override
            public Double apply(Number v) {
                return Double.valueOf(v == null ? 0 : v.doubleValue());
            }
        });
        return result;
    }

    /// Returns an object property holding the boxed value, kept in sync with
    /// this one in both directions.
    @Override
    public ObjectProperty<Double> asObject() {
        ObjectPropertyBase<Double> result = new ObjectPropertyBase<Double>() {
            @Override
            public Object getBean() {
                return null;
            }

            @Override
            public String getName() {
                return DoubleProperty.this.getName();
            }
        };
        Function<Number, Double> box = v -> Double.valueOf(v == null ? 0 : v.doubleValue());
        Function<Double, Number> unbox = v -> v;
        BidirectionalBinding.bind(result, this, box, unbox);
        return result;
    }

    @Override
    public String toString() {
        return PropertyText.describe("DoubleProperty", this, "value: " + get());
    }
}
