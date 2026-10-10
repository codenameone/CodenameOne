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

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// Bindings whose computation is a function object. Every operator of
/// [Bindings] and of the fluent expressions is one of these.
final class Fn {

    private Fn() {
    }

    static ObservableList<?> dependencies(Observable[] deps) {
        if (deps == null || deps.length == 0) {
            return FXCollections.emptyObservableList();
        }
        if (deps.length == 1) {
            return FXCollections.singletonObservableList(deps[0]);
        }
        return FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(deps));
    }

    /// Computes a boolean.
    interface ToBoolean {
        boolean get();
    }

    /// A binding computing a boolean from a function.
    static final class BooleanFn extends BooleanBinding {
        private final ToBoolean fn;
        private final Observable[] deps;

        BooleanFn(ToBoolean fn, Observable... deps) {
            this.fn = fn;
            this.deps = deps;
            bind(deps);
        }

        @Override
        protected boolean computeValue() {
            return fn.get();
        }

        @Override
        public void dispose() {
            unbind(deps);
        }

        @Override
        public ObservableList<?> getDependencies() {
            return dependencies(deps);
        }
    }

    /// Computes a `int`.
    interface ToInteger {
        int get();
    }

    /// A binding computing a `int` from a function.
    static final class IntegerFn extends IntegerBinding {
        private final ToInteger fn;
        private final Observable[] deps;

        IntegerFn(ToInteger fn, Observable... deps) {
            this.fn = fn;
            this.deps = deps;
            bind(deps);
        }

        @Override
        protected int computeValue() {
            return fn.get();
        }

        @Override
        public void dispose() {
            unbind(deps);
        }

        @Override
        public ObservableList<?> getDependencies() {
            return dependencies(deps);
        }
    }

    /// Computes a `long`.
    interface ToLong {
        long get();
    }

    /// A binding computing a `long` from a function.
    static final class LongFn extends LongBinding {
        private final ToLong fn;
        private final Observable[] deps;

        LongFn(ToLong fn, Observable... deps) {
            this.fn = fn;
            this.deps = deps;
            bind(deps);
        }

        @Override
        protected long computeValue() {
            return fn.get();
        }

        @Override
        public void dispose() {
            unbind(deps);
        }

        @Override
        public ObservableList<?> getDependencies() {
            return dependencies(deps);
        }
    }

    /// Computes a `float`.
    interface ToFloat {
        float get();
    }

    /// A binding computing a `float` from a function.
    static final class FloatFn extends FloatBinding {
        private final ToFloat fn;
        private final Observable[] deps;

        FloatFn(ToFloat fn, Observable... deps) {
            this.fn = fn;
            this.deps = deps;
            bind(deps);
        }

        @Override
        protected float computeValue() {
            return fn.get();
        }

        @Override
        public void dispose() {
            unbind(deps);
        }

        @Override
        public ObservableList<?> getDependencies() {
            return dependencies(deps);
        }
    }

    /// Computes a `double`.
    interface ToDouble {
        double get();
    }

    /// A binding computing a `double` from a function.
    static final class DoubleFn extends DoubleBinding {
        private final ToDouble fn;
        private final Observable[] deps;

        DoubleFn(ToDouble fn, Observable... deps) {
            this.fn = fn;
            this.deps = deps;
            bind(deps);
        }

        @Override
        protected double computeValue() {
            return fn.get();
        }

        @Override
        public void dispose() {
            unbind(deps);
        }

        @Override
        public ObservableList<?> getDependencies() {
            return dependencies(deps);
        }
    }

    /// Computes a string.
    interface ToString {
        String get();
    }

    /// A binding computing a string from a function.
    static final class StringFn extends StringBinding {
        private final ToString fn;
        private final Observable[] deps;

        StringFn(ToString fn, Observable... deps) {
            this.fn = fn;
            this.deps = deps;
            bind(deps);
        }

        @Override
        protected String computeValue() {
            return fn.get();
        }

        @Override
        public void dispose() {
            unbind(deps);
        }

        @Override
        public ObservableList<?> getDependencies() {
            return dependencies(deps);
        }
    }

    /// A binding computing an object from a function.
    static final class ObjectFn<T> extends ObjectBinding<T> {
        private final java.util.function.Supplier<T> fn;
        private final Observable[] deps;

        ObjectFn(java.util.function.Supplier<T> fn, Observable... deps) {
            this.fn = fn;
            this.deps = deps;
            bind(deps);
        }

        @Override
        protected T computeValue() {
            return fn.get();
        }

        @Override
        public void dispose() {
            unbind(deps);
        }

        @Override
        public ObservableList<?> getDependencies() {
            return dependencies(deps);
        }
    }
}
