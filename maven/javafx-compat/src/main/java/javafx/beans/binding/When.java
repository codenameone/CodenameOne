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

import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableNumberValue;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableStringValue;

/// The start of a ternary binding:
/// `new When(condition).then(a).otherwise(b)` holds `a` while the condition
/// is true and `b` otherwise. Either branch may be an observable value or a
/// constant.
public class When {

    private final ObservableBooleanValue condition;

    /// Starts a ternary binding on a condition.
    public When(final ObservableBooleanValue condition) {
        if (condition == null) {
            throw new NullPointerException("Condition must be specified.");
        }
        this.condition = condition;
    }

    private static <V> V required(V value) {
        if (value == null) {
            throw new NullPointerException("Value needs to be specified");
        }
        return value;
    }

    private static BooleanBinding booleans(final ObservableBooleanValue condition, final ObservableBooleanValue yes,
            final boolean yesConstant, final ObservableBooleanValue no, final boolean noConstant) {
        return new Fn.BooleanFn(() -> {
            if (condition.get()) {
                return yes == null ? yesConstant : yes.get();
            }
            return no == null ? noConstant : no.get();
        }, Bindings.deps(condition, yes, no));
    }

    private static <T> ObjectBinding<T> objects(final ObservableBooleanValue condition,
            final ObservableObjectValue<T> yes, final T yesConstant, final ObservableObjectValue<T> no,
            final T noConstant) {
        return new Fn.ObjectFn<T>(() -> {
            if (condition.get()) {
                return yes == null ? yesConstant : yes.get();
            }
            return no == null ? noConstant : no.get();
        }, Bindings.deps(condition, yes, no));
    }

    private static StringBinding strings(final ObservableBooleanValue condition, final ObservableStringValue yes,
            final String yesConstant, final ObservableStringValue no, final String noConstant) {
        return new Fn.StringFn(() -> {
            if (condition.get()) {
                return yes == null ? yesConstant : yes.get();
            }
            return no == null ? noConstant : no.get();
        }, Bindings.deps(condition, yes, no));
    }

    /// Chooses a number for the true branch.
    public NumberConditionBuilder then(final ObservableNumberValue thenValue) {
        return new NumberConditionBuilder(required(thenValue));
    }

    /// Chooses a constant for the true branch.
    public NumberConditionBuilder then(double thenValue) {
        return new NumberConditionBuilder(Const.of(thenValue));
    }

    /// Chooses a constant for the true branch.
    public NumberConditionBuilder then(float thenValue) {
        return new NumberConditionBuilder(Const.of(thenValue));
    }

    /// Chooses a constant for the true branch.
    public NumberConditionBuilder then(long thenValue) {
        return new NumberConditionBuilder(Const.of(thenValue));
    }

    /// Chooses a constant for the true branch.
    public NumberConditionBuilder then(int thenValue) {
        return new NumberConditionBuilder(Const.of(thenValue));
    }

    /// Chooses a boolean for the true branch.
    public BooleanConditionBuilder then(final ObservableBooleanValue thenValue) {
        return new BooleanConditionBuilder(required(thenValue), false);
    }

    /// Chooses a constant for the true branch.
    public BooleanConditionBuilder then(final boolean thenValue) {
        return new BooleanConditionBuilder(null, thenValue);
    }

    /// Chooses a string for the true branch.
    public StringConditionBuilder then(final ObservableStringValue thenValue) {
        return new StringConditionBuilder(required(thenValue), null);
    }

    /// Chooses a constant for the true branch.
    public StringConditionBuilder then(final String thenValue) {
        return new StringConditionBuilder(null, thenValue);
    }

    /// Chooses an object for the true branch.
    public <T> ObjectConditionBuilder<T> then(final ObservableObjectValue<T> thenValue) {
        return new ObjectConditionBuilder<T>(required(thenValue), null);
    }

    /// Chooses a constant for the true branch.
    public <T> ObjectConditionBuilder<T> then(final T thenValue) {
        return new ObjectConditionBuilder<T>(null, thenValue);
    }

    /// A ternary binding with a numeric true branch, waiting for the other
    /// one. The result has the wider of the two branch types.
    public class NumberConditionBuilder {

        private final ObservableNumberValue thenValue;

        private NumberConditionBuilder(final ObservableNumberValue thenValue) {
            this.thenValue = thenValue;
        }

        /// Completes the binding with a number for the false branch.
        public NumberBinding otherwise(final ObservableNumberValue otherwiseValue) {
            return Bindings.choose(condition, thenValue, required(otherwiseValue));
        }

        /// Completes the binding with a constant for the false branch.
        public DoubleBinding otherwise(double otherwiseValue) {
            return (DoubleBinding) Bindings.choose(condition, thenValue, Const.of(otherwiseValue));
        }

        /// Completes the binding with a constant for the false branch.
        public NumberBinding otherwise(float otherwiseValue) {
            return Bindings.choose(condition, thenValue, Const.of(otherwiseValue));
        }

        /// Completes the binding with a constant for the false branch.
        public NumberBinding otherwise(long otherwiseValue) {
            return Bindings.choose(condition, thenValue, Const.of(otherwiseValue));
        }

        /// Completes the binding with a constant for the false branch.
        public NumberBinding otherwise(int otherwiseValue) {
            return Bindings.choose(condition, thenValue, Const.of(otherwiseValue));
        }
    }

    /// A ternary binding with a boolean true branch, waiting for the other
    /// one.
    public class BooleanConditionBuilder {

        private final ObservableBooleanValue thenValue;
        private final boolean thenConstant;

        private BooleanConditionBuilder(final ObservableBooleanValue thenValue, boolean thenConstant) {
            this.thenValue = thenValue;
            this.thenConstant = thenConstant;
        }

        /// Completes the binding with a boolean for the false branch.
        public BooleanBinding otherwise(final ObservableBooleanValue otherwiseValue) {
            return booleans(condition, thenValue, thenConstant, required(otherwiseValue), false);
        }

        /// Completes the binding with a constant for the false branch.
        public BooleanBinding otherwise(final boolean otherwiseValue) {
            return booleans(condition, thenValue, thenConstant, null, otherwiseValue);
        }
    }

    /// A ternary binding with a string true branch, waiting for the other
    /// one.
    public class StringConditionBuilder {

        private final ObservableStringValue thenValue;
        private final String thenConstant;

        private StringConditionBuilder(final ObservableStringValue thenValue, String thenConstant) {
            this.thenValue = thenValue;
            this.thenConstant = thenConstant;
        }

        /// Completes the binding with a string for the false branch.
        public StringBinding otherwise(final ObservableStringValue otherwiseValue) {
            return strings(condition, thenValue, thenConstant, required(otherwiseValue), null);
        }

        /// Completes the binding with a constant for the false branch.
        public StringBinding otherwise(final String otherwiseValue) {
            return strings(condition, thenValue, thenConstant, null, otherwiseValue);
        }
    }

    /// A ternary binding with an object true branch, waiting for the other
    /// one.
    public class ObjectConditionBuilder<T> {

        private final ObservableObjectValue<T> thenValue;
        private final T thenConstant;

        private ObjectConditionBuilder(final ObservableObjectValue<T> thenValue, T thenConstant) {
            this.thenValue = thenValue;
            this.thenConstant = thenConstant;
        }

        /// Completes the binding with an object for the false branch.
        public ObjectBinding<T> otherwise(final ObservableObjectValue<T> otherwiseValue) {
            return objects(condition, thenValue, thenConstant, required(otherwiseValue), null);
        }

        /// Completes the binding with a constant for the false branch.
        public ObjectBinding<T> otherwise(final T otherwiseValue) {
            return objects(condition, thenValue, thenConstant, null, otherwiseValue);
        }
    }
}
