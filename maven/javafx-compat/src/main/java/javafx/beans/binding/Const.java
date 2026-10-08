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

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableFloatValue;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableLongValue;
import javafx.beans.value.ObservableNumberValue;
import javafx.beans.value.ObservableStringValue;

/// A constant dressed as an observable number, so an operator taking a
/// constant is the same code as the one taking two observables. It never
/// changes and therefore keeps no listeners.
abstract class Const implements ObservableNumberValue {

    private final Number value;

    Const(Number value) {
        this.value = value;
    }

    static Const of(double value) {
        return new OfDouble(value);
    }

    static Const of(float value) {
        return new OfFloat(value);
    }

    static Const of(long value) {
        return new OfLong(value);
    }

    static Const of(int value) {
        return new OfInt(value);
    }

    static boolean isConstant(Object candidate) {
        return candidate instanceof Const || candidate instanceof Text;
    }

    @Override
    public void addListener(InvalidationListener listener) {
    }

    @Override
    public void removeListener(InvalidationListener listener) {
    }

    @Override
    public void addListener(ChangeListener<? super Number> listener) {
    }

    @Override
    public void removeListener(ChangeListener<? super Number> listener) {
    }

    @Override
    public Number getValue() {
        return value;
    }

    @Override
    public int intValue() {
        return value.intValue();
    }

    @Override
    public long longValue() {
        return value.longValue();
    }

    @Override
    public float floatValue() {
        return value.floatValue();
    }

    @Override
    public double doubleValue() {
        return value.doubleValue();
    }

    static final class OfDouble extends Const implements ObservableDoubleValue {
        OfDouble(double value) {
            super(Double.valueOf(value));
        }

        @Override
        public double get() {
            return doubleValue();
        }
    }

    static final class OfFloat extends Const implements ObservableFloatValue {
        OfFloat(float value) {
            super(Float.valueOf(value));
        }

        @Override
        public float get() {
            return floatValue();
        }
    }

    static final class OfLong extends Const implements ObservableLongValue {
        OfLong(long value) {
            super(Long.valueOf(value));
        }

        @Override
        public long get() {
            return longValue();
        }
    }

    static final class OfInt extends Const implements ObservableIntegerValue {
        OfInt(int value) {
            super(Integer.valueOf(value));
        }

        @Override
        public int get() {
            return intValue();
        }
    }

    /// A constant string.
    static final class Text implements ObservableStringValue {
        private final String value;

        Text(String value) {
            this.value = value;
        }

        @Override
        public void addListener(InvalidationListener listener) {
        }

        @Override
        public void removeListener(InvalidationListener listener) {
        }

        @Override
        public void addListener(ChangeListener<? super String> listener) {
        }

        @Override
        public void removeListener(ChangeListener<? super String> listener) {
        }

        @Override
        public String get() {
            return value;
        }

        @Override
        public String getValue() {
            return value;
        }
    }
}
