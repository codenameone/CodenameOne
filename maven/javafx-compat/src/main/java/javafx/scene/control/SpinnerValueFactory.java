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
package javafx.scene.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

/// What a [Spinner] steps through: it holds the value, moves it by a
/// number of steps either way, and turns it into the text of the editor
/// and back.
///
/// The three factories of JavaFX for whole numbers, decimal numbers and
/// the items of a list are nested here. The ones for dates and times are
/// not provided.
public abstract class SpinnerValueFactory<T> {

    private final ObjectProperty<T> value = new SimpleObjectProperty<T>(this, "value");
    private final ObjectProperty<StringConverter<T>> converter = new SimpleObjectProperty<StringConverter<T>>(this,
            "converter");
    private final BooleanProperty wrapAround = new SimpleBooleanProperty(this, "wrapAround", false);

    /// Creates a factory with no value.
    public SpinnerValueFactory() {
    }

    /// Moves the value down by a number of steps.
    public abstract void decrement(int steps);

    /// Moves the value up by a number of steps.
    public abstract void increment(int steps);

    /// Returns the value.
    public final T getValue() {
        return value.get();
    }

    /// Sets the value.
    public final void setValue(T newValue) {
        value.set(newValue);
    }

    /// The value.
    public final ObjectProperty<T> valueProperty() {
        return value;
    }

    /// Returns what turns a value into the text of the editor and back.
    public final StringConverter<T> getConverter() {
        return converter.get();
    }

    /// Sets what turns a value into the text of the editor and back.
    public final void setConverter(StringConverter<T> newValue) {
        converter.set(newValue);
    }

    /// The converter.
    public final ObjectProperty<StringConverter<T>> converterProperty() {
        return converter;
    }

    /// Returns whether a step past one end arrives at the other.
    public final boolean isWrapAround() {
        return wrapAround.get();
    }

    /// Sets whether a step past one end arrives at the other.
    public final void setWrapAround(boolean newValue) {
        wrapAround.set(newValue);
    }

    /// Whether a step past one end arrives at the other.
    public final BooleanProperty wrapAroundProperty() {
        return wrapAround;
    }

    /// Whole numbers between a minimum and a maximum.
    public static class IntegerSpinnerValueFactory extends SpinnerValueFactory<Integer> {

        private final IntegerProperty min = new SimpleIntegerProperty(this, "min");
        private final IntegerProperty max = new SimpleIntegerProperty(this, "max");
        private final IntegerProperty amountToStepBy = new SimpleIntegerProperty(this, "amountToStepBy", 1);

        /// Creates a factory that starts at the minimum and steps by one.
        public IntegerSpinnerValueFactory(int min, int max) {
            this(min, max, min);
        }

        /// Creates a factory that steps by one.
        public IntegerSpinnerValueFactory(int min, int max, int initialValue) {
            this(min, max, initialValue, 1);
        }

        /// Creates a factory. An initial value outside the range is the
        /// minimum.
        public IntegerSpinnerValueFactory(int min, int max, int initialValue, int amountToStepBy) {
            this.min.set(min);
            this.max.set(max);
            this.amountToStepBy.set(amountToStepBy);
            setConverter(new IntegerStringConverter());
            valueProperty().addListener((observable, was, now) -> {
                if (now == null) {
                    return;
                }
                if (now.intValue() < getMin()) {
                    setValue(Integer.valueOf(getMin()));
                } else if (now.intValue() > getMax()) {
                    setValue(Integer.valueOf(getMax()));
                }
            });
            setValue(Integer.valueOf(initialValue >= min && initialValue <= max ? initialValue : min));
        }

        @Override
        public void decrement(int steps) {
            step(-steps);
        }

        @Override
        public void increment(int steps) {
            step(steps);
        }

        private void step(int steps) {
            int low = getMin();
            int high = getMax();
            Integer now = getValue();
            long next = (now == null ? low : now.intValue()) + (long) steps * getAmountToStepBy();
            if (next < low || next > high) {
                long span = (long) high - low + 1;
                if (isWrapAround() && span > 0) {
                    next = low + (((next - low) % span) + span) % span;
                } else {
                    next = next < low ? low : high;
                }
            }
            setValue(Integer.valueOf((int) next));
        }

        /// Returns the least value.
        public final int getMin() {
            return min.get();
        }

        /// Sets the least value.
        public final void setMin(int value) {
            min.set(value);
        }

        /// The least value.
        public final IntegerProperty minProperty() {
            return min;
        }

        /// Returns the greatest value.
        public final int getMax() {
            return max.get();
        }

        /// Sets the greatest value.
        public final void setMax(int value) {
            max.set(value);
        }

        /// The greatest value.
        public final IntegerProperty maxProperty() {
            return max;
        }

        /// Returns the size of one step.
        public final int getAmountToStepBy() {
            return amountToStepBy.get();
        }

        /// Sets the size of one step.
        public final void setAmountToStepBy(int value) {
            amountToStepBy.set(value);
        }

        /// The size of one step.
        public final IntegerProperty amountToStepByProperty() {
            return amountToStepBy;
        }
    }

    /// Decimal numbers between a minimum and a maximum.
    public static class DoubleSpinnerValueFactory extends SpinnerValueFactory<Double> {

        private final DoubleProperty min = new SimpleDoubleProperty(this, "min");
        private final DoubleProperty max = new SimpleDoubleProperty(this, "max");
        private final DoubleProperty amountToStepBy = new SimpleDoubleProperty(this, "amountToStepBy", 1);

        /// Creates a factory that starts at the minimum and steps by one.
        public DoubleSpinnerValueFactory(double min, double max) {
            this(min, max, min);
        }

        /// Creates a factory that steps by one.
        public DoubleSpinnerValueFactory(double min, double max, double initialValue) {
            this(min, max, initialValue, 1);
        }

        /// Creates a factory. An initial value outside the range is the
        /// minimum.
        public DoubleSpinnerValueFactory(double min, double max, double initialValue, double amountToStepBy) {
            this.min.set(min);
            this.max.set(max);
            this.amountToStepBy.set(amountToStepBy);
            setConverter(new DoubleStringConverter());
            valueProperty().addListener((observable, was, now) -> {
                if (now == null) {
                    return;
                }
                if (now.doubleValue() < getMin()) {
                    setValue(Double.valueOf(getMin()));
                } else if (now.doubleValue() > getMax()) {
                    setValue(Double.valueOf(getMax()));
                }
            });
            setValue(Double.valueOf(initialValue >= min && initialValue <= max ? initialValue : min));
        }

        @Override
        public void decrement(int steps) {
            step(-steps);
        }

        @Override
        public void increment(int steps) {
            step(steps);
        }

        private void step(int steps) {
            double low = getMin();
            double high = getMax();
            Double now = getValue();
            double next = (now == null ? low : now.doubleValue()) + steps * getAmountToStepBy();
            if (next < low) {
                next = isWrapAround() ? high : low;
            } else if (next > high) {
                next = isWrapAround() ? low : high;
            }
            setValue(Double.valueOf(next));
        }

        /// Returns the least value.
        public final double getMin() {
            return min.get();
        }

        /// Sets the least value.
        public final void setMin(double value) {
            min.set(value);
        }

        /// The least value.
        public final DoubleProperty minProperty() {
            return min;
        }

        /// Returns the greatest value.
        public final double getMax() {
            return max.get();
        }

        /// Sets the greatest value.
        public final void setMax(double value) {
            max.set(value);
        }

        /// The greatest value.
        public final DoubleProperty maxProperty() {
            return max;
        }

        /// Returns the size of one step.
        public final double getAmountToStepBy() {
            return amountToStepBy.get();
        }

        /// Sets the size of one step.
        public final void setAmountToStepBy(double value) {
            amountToStepBy.set(value);
        }

        /// The size of one step.
        public final DoubleProperty amountToStepByProperty() {
            return amountToStepBy;
        }
    }

    /// The items of a list, in its order.
    public static class ListSpinnerValueFactory<T> extends SpinnerValueFactory<T> {

        private final ObjectProperty<ObservableList<T>> items = new SimpleObjectProperty<ObservableList<T>>(this,
                "items");

        /// Creates a factory that starts at the first item of a list.
        public ListSpinnerValueFactory(ObservableList<T> items) {
            setConverter(new StringConverter<T>() {
                @Override
                public String toString(T object) {
                    return object == null ? "" : object.toString();
                }

                @Override
                public T fromString(String string) {
                    ObservableList<T> all = getItems();
                    for (int i = 0; all != null && i < all.size(); i++) {
                        T item = all.get(i);
                        if (item != null && item.toString().equals(string)) {
                            return item;
                        }
                    }
                    return getValue();
                }
            });
            this.items.set(items == null ? FXCollections.<T>observableArrayList() : items);
            ObservableList<T> all = this.items.get();
            setValue(all.isEmpty() ? null : all.get(0));
        }

        @Override
        public void decrement(int steps) {
            step(-steps);
        }

        @Override
        public void increment(int steps) {
            step(steps);
        }

        private void step(int steps) {
            ObservableList<T> all = getItems();
            int n = all == null ? 0 : all.size();
            if (n == 0) {
                return;
            }
            int at = Math.max(0, all.indexOf(getValue())) + steps;
            if (at < 0 || at >= n) {
                at = isWrapAround() ? ((at % n) + n) % n : (at < 0 ? 0 : n - 1);
            }
            setValue(all.get(at));
        }

        /// Returns the items.
        public final ObservableList<T> getItems() {
            return items.get();
        }

        /// Sets the items.
        public final void setItems(ObservableList<T> value) {
            items.set(value);
        }

        /// The items.
        public final ObjectProperty<ObservableList<T>> itemsProperty() {
            return items;
        }
    }
}
