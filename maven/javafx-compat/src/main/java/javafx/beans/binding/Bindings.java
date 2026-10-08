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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.codename1.fxcompat.runtime.BidirectionalBinding;
import com.codename1.fxcompat.runtime.ContentBindings;
import com.codename1.fxcompat.runtime.Diagnostics;

import javafx.beans.Observable;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableFloatValue;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableLongValue;
import javafx.beans.value.ObservableNumberValue;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.util.StringConverter;

/// The operators of the binding API: every method returns a binding that
/// follows its operands and recomputes lazily.
///
/// An arithmetic result has the wider of its two operand types -- `double`
/// over `float` over `long` over `int` -- as the same expression would in
/// Java. An integer division by zero throws `ArithmeticException` when the
/// binding is read.
public final class Bindings {

    static final int ADD = 0;
    static final int SUB = 1;
    static final int MUL = 2;
    static final int DIV = 3;
    static final int MIN = 4;
    static final int MAX = 5;
    static final int NEG = 6;

    static final int EQ = 0;
    static final int NE = 1;
    static final int GT = 2;
    static final int LT = 3;
    static final int GE = 4;
    static final int LE = 5;
    static final int EQ_IC = 6;
    static final int NE_IC = 7;

    private Bindings() {
    }

    // ---- shared machinery

    /// Returns the candidates a binding has to observe: the ones that are
    /// there and can change.
    static Observable[] deps(Observable... candidates) {
        int count = 0;
        for (Observable candidate : candidates) {
            if (candidate != null && !Const.isConstant(candidate)) {
                count++;
            }
        }
        Observable[] result = new Observable[count];
        int next = 0;
        for (Observable candidate : candidates) {
            if (candidate != null && !Const.isConstant(candidate)) {
                result[next++] = candidate;
            }
        }
        return result;
    }

    private static int rank(ObservableNumberValue value) {
        if (value instanceof ObservableDoubleValue) {
            return 3;
        }
        if (value instanceof ObservableFloatValue) {
            return 2;
        }
        if (value instanceof ObservableLongValue) {
            return 1;
        }
        return 0;
    }

    private static void operands(Object first, Object second) {
        if (first == null || second == null) {
            throw new NullPointerException("Operands cannot be null.");
        }
    }

    static NumberBinding arith(final int op, final ObservableNumberValue a, final ObservableNumberValue b) {
        operands(a, b);
        Observable[] deps = deps(a, b);
        switch (Math.max(rank(a), rank(b))) {
            case 3:
                return new Fn.DoubleFn(() -> apply(op, a.doubleValue(), b.doubleValue()), deps);
            case 2:
                return new Fn.FloatFn(() -> apply(op, a.floatValue(), b.floatValue()), deps);
            case 1:
                return new Fn.LongFn(() -> apply(op, a.longValue(), b.longValue()), deps);
            default:
                return new Fn.IntegerFn(() -> apply(op, a.intValue(), b.intValue()), deps);
        }
    }

    static NumberBinding choose(final ObservableBooleanValue condition, final ObservableNumberValue a,
            final ObservableNumberValue b) {
        Observable[] deps = deps(condition, a, b);
        switch (Math.max(rank(a), rank(b))) {
            case 3:
                return new Fn.DoubleFn(() -> condition.get() ? a.doubleValue() : b.doubleValue(), deps);
            case 2:
                return new Fn.FloatFn(() -> condition.get() ? a.floatValue() : b.floatValue(), deps);
            case 1:
                return new Fn.LongFn(() -> condition.get() ? a.longValue() : b.longValue(), deps);
            default:
                return new Fn.IntegerFn(() -> condition.get() ? a.intValue() : b.intValue(), deps);
        }
    }

    private static double apply(int op, double x, double y) {
        switch (op) {
            case ADD:
                return x + y;
            case SUB:
                return x - y;
            case MUL:
                return x * y;
            case DIV:
                return x / y;
            case MIN:
                return Math.min(x, y);
            case MAX:
                return Math.max(x, y);
            default:
                return -x;
        }
    }

    private static float apply(int op, float x, float y) {
        switch (op) {
            case ADD:
                return x + y;
            case SUB:
                return x - y;
            case MUL:
                return x * y;
            case DIV:
                return x / y;
            case MIN:
                return Math.min(x, y);
            case MAX:
                return Math.max(x, y);
            default:
                return -x;
        }
    }

    private static long apply(int op, long x, long y) {
        switch (op) {
            case ADD:
                return x + y;
            case SUB:
                return x - y;
            case MUL:
                return x * y;
            case DIV:
                // The device VM answers 0 for an integer division by zero,
                // so the exception Java promises is raised by hand.
                if (y == 0) {
                    throw new ArithmeticException("/ by zero");
                }
                return x / y;
            case MIN:
                return Math.min(x, y);
            case MAX:
                return Math.max(x, y);
            default:
                return -x;
        }
    }

    private static int apply(int op, int x, int y) {
        switch (op) {
            case ADD:
                return x + y;
            case SUB:
                return x - y;
            case MUL:
                return x * y;
            case DIV:
                if (y == 0) {
                    throw new ArithmeticException("/ by zero");
                }
                return x / y;
            case MIN:
                return Math.min(x, y);
            case MAX:
                return Math.max(x, y);
            default:
                return -x;
        }
    }

    static BooleanBinding compare(final int op, final ObservableNumberValue a, final ObservableNumberValue b,
            final double epsilon) {
        operands(a, b);
        Observable[] deps = deps(a, b);
        switch (Math.max(rank(a), rank(b))) {
            case 3:
                return new Fn.BooleanFn(() -> {
                    double x = a.doubleValue();
                    double y = b.doubleValue();
                    return pick(op, Math.abs(x - y) <= epsilon, x > y, x >= y, x < y, x <= y);
                }, deps);
            case 2:
                return new Fn.BooleanFn(() -> {
                    float x = a.floatValue();
                    float y = b.floatValue();
                    return pick(op, Math.abs(x - y) <= epsilon, x > y, x >= y, x < y, x <= y);
                }, deps);
            case 1:
                return new Fn.BooleanFn(() -> {
                    long x = a.longValue();
                    long y = b.longValue();
                    return pick(op, Math.abs(x - y) <= epsilon, x > y, x >= y, x < y, x <= y);
                }, deps);
            default:
                return new Fn.BooleanFn(() -> {
                    int x = a.intValue();
                    int y = b.intValue();
                    return pick(op, Math.abs(x - y) <= epsilon, x > y, x >= y, x < y, x <= y);
                }, deps);
        }
    }

    private static boolean pick(int op, boolean eq, boolean gt, boolean ge, boolean lt, boolean le) {
        switch (op) {
            case EQ:
                return eq;
            case NE:
                return !eq;
            case GT:
                return gt;
            case GE:
                return ge;
            case LT:
                return lt;
            default:
                return le;
        }
    }

    static BooleanBinding strings(final int op, final ObservableStringValue a, final ObservableStringValue b) {
        operands(a, b);
        return new Fn.BooleanFn(() -> {
            String x = a.get();
            String y = b.get();
            if (x == null) {
                x = "";
            }
            if (y == null) {
                y = "";
            }
            switch (op) {
                case EQ:
                    return x.equals(y);
                case NE:
                    return !x.equals(y);
                case EQ_IC:
                    return x.equalsIgnoreCase(y);
                case NE_IC:
                    return !x.equalsIgnoreCase(y);
                case GT:
                    return x.compareTo(y) > 0;
                case GE:
                    return x.compareTo(y) >= 0;
                case LT:
                    return x.compareTo(y) < 0;
                default:
                    return x.compareTo(y) <= 0;
            }
        }, deps(a, b));
    }

    /// The text of any observable value; `"null"` for none.
    static StringBinding text(final ObservableValue<?> value) {
        if (value == null) {
            throw new NullPointerException("ObservableValue must be specified");
        }
        return new Fn.StringFn(() -> String.valueOf(value.getValue()), value);
    }

    private static Observable[] observables(Object[] args) {
        int count = 0;
        for (Object arg : args) {
            if (arg instanceof ObservableValue) {
                count++;
            }
        }
        Observable[] result = new Observable[count];
        int next = 0;
        for (Object arg : args) {
            if (arg instanceof ObservableValue) {
                result[next++] = (ObservableValue<?>) arg;
            }
        }
        return result;
    }

    static StringBinding formatted(final String format, final Object... args) {
        if (format == null) {
            throw new NullPointerException("Format cannot be null.");
        }
        final Object[] source = args == null ? new Object[0] : args.clone();
        return new Fn.StringFn(() -> {
            Object[] values = new Object[source.length];
            for (int i = 0; i < values.length; i++) {
                Object arg = source[i];
                values[i] = arg instanceof ObservableValue ? ((ObservableValue<?>) arg).getValue() : arg;
            }
            return String.format(format, values);
        }, observables(source));
    }

    // ---- custom bindings

    /// Returns a boolean binding computed by a function; a function that
    /// throws or returns `null` yields `false`.
    public static BooleanBinding createBooleanBinding(final Callable<Boolean> func, final Observable... dependencies) {
        return new Fn.BooleanFn(() -> {
            try {
                Boolean value = func.call();
                if (value != null) {
                    return value.booleanValue();
                }
            } catch (Exception failure) {
                Diagnostics.report(failure);
            }
            return false;
        }, dependencies);
    }

    /// Returns a `double` binding computed by a function; a function that
    /// throws or returns `null` yields zero.
    public static DoubleBinding createDoubleBinding(final Callable<Double> func, final Observable... dependencies) {
        return new Fn.DoubleFn(() -> {
            try {
                Double value = func.call();
                if (value != null) {
                    return value.doubleValue();
                }
            } catch (Exception failure) {
                Diagnostics.report(failure);
            }
            return 0.0;
        }, dependencies);
    }

    /// Returns a `float` binding computed by a function; a function that
    /// throws or returns `null` yields zero.
    public static FloatBinding createFloatBinding(final Callable<Float> func, final Observable... dependencies) {
        return new Fn.FloatFn(() -> {
            try {
                Float value = func.call();
                if (value != null) {
                    return value.floatValue();
                }
            } catch (Exception failure) {
                Diagnostics.report(failure);
            }
            return 0.0f;
        }, dependencies);
    }

    /// Returns an `int` binding computed by a function; a function that
    /// throws or returns `null` yields zero.
    public static IntegerBinding createIntegerBinding(final Callable<Integer> func, final Observable... dependencies) {
        return new Fn.IntegerFn(() -> {
            try {
                Integer value = func.call();
                if (value != null) {
                    return value.intValue();
                }
            } catch (Exception failure) {
                Diagnostics.report(failure);
            }
            return 0;
        }, dependencies);
    }

    /// Returns a `long` binding computed by a function; a function that
    /// throws or returns `null` yields zero.
    public static LongBinding createLongBinding(final Callable<Long> func, final Observable... dependencies) {
        return new Fn.LongFn(() -> {
            try {
                Long value = func.call();
                if (value != null) {
                    return value.longValue();
                }
            } catch (Exception failure) {
                Diagnostics.report(failure);
            }
            return 0L;
        }, dependencies);
    }

    /// Returns an object binding computed by a function; a function that
    /// throws yields `null`.
    public static <T> ObjectBinding<T> createObjectBinding(final Callable<T> func, final Observable... dependencies) {
        return new Fn.ObjectFn<T>(() -> {
            try {
                return func.call();
            } catch (Exception failure) {
                Diagnostics.report(failure);
                return null;
            }
        }, dependencies);
    }

    /// Returns a string binding computed by a function; a function that
    /// throws yields the empty string.
    public static StringBinding createStringBinding(final Callable<String> func, final Observable... dependencies) {
        return new Fn.StringFn(() -> {
            try {
                return func.call();
            } catch (Exception failure) {
                Diagnostics.report(failure);
                return "";
            }
        }, dependencies);
    }

    // ---- numbers

    /// Returns a binding computing the negation of a number.
    public static NumberBinding negate(final ObservableNumberValue value) {
        if (value == null) {
            throw new NullPointerException("Operand cannot be null.");
        }
        return arith(NEG, value, Const.of(0));
    }

    /// Returns a binding computing the sum of two numbers, typed by the wider operand.
    public static NumberBinding add(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return arith(ADD, op1, op2);
    }

    /// Returns a binding computing the sum of a number and a constant.
    public static DoubleBinding add(final ObservableNumberValue op1, final double op2) {
        return (DoubleBinding) arith(ADD, op1, Const.of(op2));
    }

    /// Returns a binding computing the sum of a number and a constant.
    public static DoubleBinding add(final double op1, final ObservableNumberValue op2) {
        return (DoubleBinding) arith(ADD, Const.of(op1), op2);
    }

    /// Returns a binding computing the sum of a number and a constant.
    public static NumberBinding add(final ObservableNumberValue op1, final float op2) {
        return arith(ADD, op1, Const.of(op2));
    }

    /// Returns a binding computing the sum of a number and a constant.
    public static NumberBinding add(final float op1, final ObservableNumberValue op2) {
        return arith(ADD, Const.of(op1), op2);
    }

    /// Returns a binding computing the sum of a number and a constant.
    public static NumberBinding add(final ObservableNumberValue op1, final long op2) {
        return arith(ADD, op1, Const.of(op2));
    }

    /// Returns a binding computing the sum of a number and a constant.
    public static NumberBinding add(final long op1, final ObservableNumberValue op2) {
        return arith(ADD, Const.of(op1), op2);
    }

    /// Returns a binding computing the sum of a number and a constant.
    public static NumberBinding add(final ObservableNumberValue op1, final int op2) {
        return arith(ADD, op1, Const.of(op2));
    }

    /// Returns a binding computing the sum of a number and a constant.
    public static NumberBinding add(final int op1, final ObservableNumberValue op2) {
        return arith(ADD, Const.of(op1), op2);
    }

    /// Returns a binding computing the difference of two numbers, typed by the wider operand.
    public static NumberBinding subtract(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return arith(SUB, op1, op2);
    }

    /// Returns a binding computing the difference of a number and a constant.
    public static DoubleBinding subtract(final ObservableNumberValue op1, final double op2) {
        return (DoubleBinding) arith(SUB, op1, Const.of(op2));
    }

    /// Returns a binding computing the difference of a number and a constant.
    public static DoubleBinding subtract(final double op1, final ObservableNumberValue op2) {
        return (DoubleBinding) arith(SUB, Const.of(op1), op2);
    }

    /// Returns a binding computing the difference of a number and a constant.
    public static NumberBinding subtract(final ObservableNumberValue op1, final float op2) {
        return arith(SUB, op1, Const.of(op2));
    }

    /// Returns a binding computing the difference of a number and a constant.
    public static NumberBinding subtract(final float op1, final ObservableNumberValue op2) {
        return arith(SUB, Const.of(op1), op2);
    }

    /// Returns a binding computing the difference of a number and a constant.
    public static NumberBinding subtract(final ObservableNumberValue op1, final long op2) {
        return arith(SUB, op1, Const.of(op2));
    }

    /// Returns a binding computing the difference of a number and a constant.
    public static NumberBinding subtract(final long op1, final ObservableNumberValue op2) {
        return arith(SUB, Const.of(op1), op2);
    }

    /// Returns a binding computing the difference of a number and a constant.
    public static NumberBinding subtract(final ObservableNumberValue op1, final int op2) {
        return arith(SUB, op1, Const.of(op2));
    }

    /// Returns a binding computing the difference of a number and a constant.
    public static NumberBinding subtract(final int op1, final ObservableNumberValue op2) {
        return arith(SUB, Const.of(op1), op2);
    }

    /// Returns a binding computing the product of two numbers, typed by the wider operand.
    public static NumberBinding multiply(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return arith(MUL, op1, op2);
    }

    /// Returns a binding computing the product of a number and a constant.
    public static DoubleBinding multiply(final ObservableNumberValue op1, final double op2) {
        return (DoubleBinding) arith(MUL, op1, Const.of(op2));
    }

    /// Returns a binding computing the product of a number and a constant.
    public static DoubleBinding multiply(final double op1, final ObservableNumberValue op2) {
        return (DoubleBinding) arith(MUL, Const.of(op1), op2);
    }

    /// Returns a binding computing the product of a number and a constant.
    public static NumberBinding multiply(final ObservableNumberValue op1, final float op2) {
        return arith(MUL, op1, Const.of(op2));
    }

    /// Returns a binding computing the product of a number and a constant.
    public static NumberBinding multiply(final float op1, final ObservableNumberValue op2) {
        return arith(MUL, Const.of(op1), op2);
    }

    /// Returns a binding computing the product of a number and a constant.
    public static NumberBinding multiply(final ObservableNumberValue op1, final long op2) {
        return arith(MUL, op1, Const.of(op2));
    }

    /// Returns a binding computing the product of a number and a constant.
    public static NumberBinding multiply(final long op1, final ObservableNumberValue op2) {
        return arith(MUL, Const.of(op1), op2);
    }

    /// Returns a binding computing the product of a number and a constant.
    public static NumberBinding multiply(final ObservableNumberValue op1, final int op2) {
        return arith(MUL, op1, Const.of(op2));
    }

    /// Returns a binding computing the product of a number and a constant.
    public static NumberBinding multiply(final int op1, final ObservableNumberValue op2) {
        return arith(MUL, Const.of(op1), op2);
    }

    /// Returns a binding computing the quotient of two numbers, typed by the wider operand.
    public static NumberBinding divide(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return arith(DIV, op1, op2);
    }

    /// Returns a binding computing the quotient of a number and a constant.
    public static DoubleBinding divide(final ObservableNumberValue op1, final double op2) {
        return (DoubleBinding) arith(DIV, op1, Const.of(op2));
    }

    /// Returns a binding computing the quotient of a number and a constant.
    public static DoubleBinding divide(final double op1, final ObservableNumberValue op2) {
        return (DoubleBinding) arith(DIV, Const.of(op1), op2);
    }

    /// Returns a binding computing the quotient of a number and a constant.
    public static NumberBinding divide(final ObservableNumberValue op1, final float op2) {
        return arith(DIV, op1, Const.of(op2));
    }

    /// Returns a binding computing the quotient of a number and a constant.
    public static NumberBinding divide(final float op1, final ObservableNumberValue op2) {
        return arith(DIV, Const.of(op1), op2);
    }

    /// Returns a binding computing the quotient of a number and a constant.
    public static NumberBinding divide(final ObservableNumberValue op1, final long op2) {
        return arith(DIV, op1, Const.of(op2));
    }

    /// Returns a binding computing the quotient of a number and a constant.
    public static NumberBinding divide(final long op1, final ObservableNumberValue op2) {
        return arith(DIV, Const.of(op1), op2);
    }

    /// Returns a binding computing the quotient of a number and a constant.
    public static NumberBinding divide(final ObservableNumberValue op1, final int op2) {
        return arith(DIV, op1, Const.of(op2));
    }

    /// Returns a binding computing the quotient of a number and a constant.
    public static NumberBinding divide(final int op1, final ObservableNumberValue op2) {
        return arith(DIV, Const.of(op1), op2);
    }

    /// Returns a binding computing the smaller of two numbers, typed by the wider operand.
    public static NumberBinding min(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return arith(MIN, op1, op2);
    }

    /// Returns a binding computing the smaller of a number and a constant.
    public static DoubleBinding min(final ObservableNumberValue op1, final double op2) {
        return (DoubleBinding) arith(MIN, op1, Const.of(op2));
    }

    /// Returns a binding computing the smaller of a number and a constant.
    public static DoubleBinding min(final double op1, final ObservableNumberValue op2) {
        return (DoubleBinding) arith(MIN, Const.of(op1), op2);
    }

    /// Returns a binding computing the smaller of a number and a constant.
    public static NumberBinding min(final ObservableNumberValue op1, final float op2) {
        return arith(MIN, op1, Const.of(op2));
    }

    /// Returns a binding computing the smaller of a number and a constant.
    public static NumberBinding min(final float op1, final ObservableNumberValue op2) {
        return arith(MIN, Const.of(op1), op2);
    }

    /// Returns a binding computing the smaller of a number and a constant.
    public static NumberBinding min(final ObservableNumberValue op1, final long op2) {
        return arith(MIN, op1, Const.of(op2));
    }

    /// Returns a binding computing the smaller of a number and a constant.
    public static NumberBinding min(final long op1, final ObservableNumberValue op2) {
        return arith(MIN, Const.of(op1), op2);
    }

    /// Returns a binding computing the smaller of a number and a constant.
    public static NumberBinding min(final ObservableNumberValue op1, final int op2) {
        return arith(MIN, op1, Const.of(op2));
    }

    /// Returns a binding computing the smaller of a number and a constant.
    public static NumberBinding min(final int op1, final ObservableNumberValue op2) {
        return arith(MIN, Const.of(op1), op2);
    }

    /// Returns a binding computing the larger of two numbers, typed by the wider operand.
    public static NumberBinding max(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return arith(MAX, op1, op2);
    }

    /// Returns a binding computing the larger of a number and a constant.
    public static DoubleBinding max(final ObservableNumberValue op1, final double op2) {
        return (DoubleBinding) arith(MAX, op1, Const.of(op2));
    }

    /// Returns a binding computing the larger of a number and a constant.
    public static DoubleBinding max(final double op1, final ObservableNumberValue op2) {
        return (DoubleBinding) arith(MAX, Const.of(op1), op2);
    }

    /// Returns a binding computing the larger of a number and a constant.
    public static NumberBinding max(final ObservableNumberValue op1, final float op2) {
        return arith(MAX, op1, Const.of(op2));
    }

    /// Returns a binding computing the larger of a number and a constant.
    public static NumberBinding max(final float op1, final ObservableNumberValue op2) {
        return arith(MAX, Const.of(op1), op2);
    }

    /// Returns a binding computing the larger of a number and a constant.
    public static NumberBinding max(final ObservableNumberValue op1, final long op2) {
        return arith(MAX, op1, Const.of(op2));
    }

    /// Returns a binding computing the larger of a number and a constant.
    public static NumberBinding max(final long op1, final ObservableNumberValue op2) {
        return arith(MAX, Const.of(op1), op2);
    }

    /// Returns a binding computing the larger of a number and a constant.
    public static NumberBinding max(final ObservableNumberValue op1, final int op2) {
        return arith(MAX, op1, Const.of(op2));
    }

    /// Returns a binding computing the larger of a number and a constant.
    public static NumberBinding max(final int op1, final ObservableNumberValue op2) {
        return arith(MAX, Const.of(op1), op2);
    }

    /// Returns a binding telling whether two numbers are equal, within a tolerance.
    public static BooleanBinding equal(final ObservableNumberValue op1, final ObservableNumberValue op2, final double epsilon) {
        return compare(EQ, op1, op2, epsilon);
    }

    /// Returns a binding telling whether two numbers are equal.
    public static BooleanBinding equal(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return compare(EQ, op1, op2, 0.0);
    }

    /// Returns a binding telling whether a number and a constant are equal, within a tolerance.
    public static BooleanBinding equal(final ObservableNumberValue op1, final double op2, final double epsilon) {
        return compare(EQ, op1, Const.of(op2), epsilon);
    }

    /// Returns a binding telling whether a number and a constant are equal, within a tolerance.
    public static BooleanBinding equal(final double op1, final ObservableNumberValue op2, final double epsilon) {
        return compare(EQ, Const.of(op1), op2, epsilon);
    }

    /// Returns a binding telling whether a number and a constant are equal, within a tolerance.
    public static BooleanBinding equal(final ObservableNumberValue op1, final float op2, final double epsilon) {
        return compare(EQ, op1, Const.of(op2), epsilon);
    }

    /// Returns a binding telling whether a number and a constant are equal, within a tolerance.
    public static BooleanBinding equal(final float op1, final ObservableNumberValue op2, final double epsilon) {
        return compare(EQ, Const.of(op1), op2, epsilon);
    }

    /// Returns a binding telling whether a number and a constant are equal, within a tolerance.
    public static BooleanBinding equal(final ObservableNumberValue op1, final long op2, final double epsilon) {
        return compare(EQ, op1, Const.of(op2), epsilon);
    }

    /// Returns a binding telling whether a number and a constant are equal, within a tolerance.
    public static BooleanBinding equal(final long op1, final ObservableNumberValue op2, final double epsilon) {
        return compare(EQ, Const.of(op1), op2, epsilon);
    }

    /// Returns a binding telling whether a number and a constant are equal.
    public static BooleanBinding equal(final ObservableNumberValue op1, final long op2) {
        return compare(EQ, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether a number and a constant are equal.
    public static BooleanBinding equal(final long op1, final ObservableNumberValue op2) {
        return compare(EQ, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether a number and a constant are equal, within a tolerance.
    public static BooleanBinding equal(final ObservableNumberValue op1, final int op2, final double epsilon) {
        return compare(EQ, op1, Const.of(op2), epsilon);
    }

    /// Returns a binding telling whether a number and a constant are equal, within a tolerance.
    public static BooleanBinding equal(final int op1, final ObservableNumberValue op2, final double epsilon) {
        return compare(EQ, Const.of(op1), op2, epsilon);
    }

    /// Returns a binding telling whether a number and a constant are equal.
    public static BooleanBinding equal(final ObservableNumberValue op1, final int op2) {
        return compare(EQ, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether a number and a constant are equal.
    public static BooleanBinding equal(final int op1, final ObservableNumberValue op2) {
        return compare(EQ, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether two numbers are not equal, within a tolerance.
    public static BooleanBinding notEqual(final ObservableNumberValue op1, final ObservableNumberValue op2, final double epsilon) {
        return compare(NE, op1, op2, epsilon);
    }

    /// Returns a binding telling whether two numbers are not equal.
    public static BooleanBinding notEqual(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return compare(NE, op1, op2, 0.0);
    }

    /// Returns a binding telling whether a number and a constant are not equal, within a tolerance.
    public static BooleanBinding notEqual(final ObservableNumberValue op1, final double op2, final double epsilon) {
        return compare(NE, op1, Const.of(op2), epsilon);
    }

    /// Returns a binding telling whether a number and a constant are not equal, within a tolerance.
    public static BooleanBinding notEqual(final double op1, final ObservableNumberValue op2, final double epsilon) {
        return compare(NE, Const.of(op1), op2, epsilon);
    }

    /// Returns a binding telling whether a number and a constant are not equal, within a tolerance.
    public static BooleanBinding notEqual(final ObservableNumberValue op1, final float op2, final double epsilon) {
        return compare(NE, op1, Const.of(op2), epsilon);
    }

    /// Returns a binding telling whether a number and a constant are not equal, within a tolerance.
    public static BooleanBinding notEqual(final float op1, final ObservableNumberValue op2, final double epsilon) {
        return compare(NE, Const.of(op1), op2, epsilon);
    }

    /// Returns a binding telling whether a number and a constant are not equal, within a tolerance.
    public static BooleanBinding notEqual(final ObservableNumberValue op1, final long op2, final double epsilon) {
        return compare(NE, op1, Const.of(op2), epsilon);
    }

    /// Returns a binding telling whether a number and a constant are not equal, within a tolerance.
    public static BooleanBinding notEqual(final long op1, final ObservableNumberValue op2, final double epsilon) {
        return compare(NE, Const.of(op1), op2, epsilon);
    }

    /// Returns a binding telling whether a number and a constant are not equal.
    public static BooleanBinding notEqual(final ObservableNumberValue op1, final long op2) {
        return compare(NE, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether a number and a constant are not equal.
    public static BooleanBinding notEqual(final long op1, final ObservableNumberValue op2) {
        return compare(NE, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether a number and a constant are not equal, within a tolerance.
    public static BooleanBinding notEqual(final ObservableNumberValue op1, final int op2, final double epsilon) {
        return compare(NE, op1, Const.of(op2), epsilon);
    }

    /// Returns a binding telling whether a number and a constant are not equal, within a tolerance.
    public static BooleanBinding notEqual(final int op1, final ObservableNumberValue op2, final double epsilon) {
        return compare(NE, Const.of(op1), op2, epsilon);
    }

    /// Returns a binding telling whether a number and a constant are not equal.
    public static BooleanBinding notEqual(final ObservableNumberValue op1, final int op2) {
        return compare(NE, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether a number and a constant are not equal.
    public static BooleanBinding notEqual(final int op1, final ObservableNumberValue op2) {
        return compare(NE, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether the first number is greater than the second.
    public static BooleanBinding greaterThan(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return compare(GT, op1, op2, 0.0);
    }

    /// Returns a binding telling whether the first operand is greater than the second.
    public static BooleanBinding greaterThan(final ObservableNumberValue op1, final double op2) {
        return compare(GT, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether the first operand is greater than the second.
    public static BooleanBinding greaterThan(final double op1, final ObservableNumberValue op2) {
        return compare(GT, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether the first operand is greater than the second.
    public static BooleanBinding greaterThan(final ObservableNumberValue op1, final float op2) {
        return compare(GT, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether the first operand is greater than the second.
    public static BooleanBinding greaterThan(final float op1, final ObservableNumberValue op2) {
        return compare(GT, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether the first operand is greater than the second.
    public static BooleanBinding greaterThan(final ObservableNumberValue op1, final long op2) {
        return compare(GT, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether the first operand is greater than the second.
    public static BooleanBinding greaterThan(final long op1, final ObservableNumberValue op2) {
        return compare(GT, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether the first operand is greater than the second.
    public static BooleanBinding greaterThan(final ObservableNumberValue op1, final int op2) {
        return compare(GT, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether the first operand is greater than the second.
    public static BooleanBinding greaterThan(final int op1, final ObservableNumberValue op2) {
        return compare(GT, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether the first number is less than the second.
    public static BooleanBinding lessThan(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return compare(LT, op1, op2, 0.0);
    }

    /// Returns a binding telling whether the first operand is less than the second.
    public static BooleanBinding lessThan(final ObservableNumberValue op1, final double op2) {
        return compare(LT, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether the first operand is less than the second.
    public static BooleanBinding lessThan(final double op1, final ObservableNumberValue op2) {
        return compare(LT, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether the first operand is less than the second.
    public static BooleanBinding lessThan(final ObservableNumberValue op1, final float op2) {
        return compare(LT, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether the first operand is less than the second.
    public static BooleanBinding lessThan(final float op1, final ObservableNumberValue op2) {
        return compare(LT, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether the first operand is less than the second.
    public static BooleanBinding lessThan(final ObservableNumberValue op1, final long op2) {
        return compare(LT, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether the first operand is less than the second.
    public static BooleanBinding lessThan(final long op1, final ObservableNumberValue op2) {
        return compare(LT, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether the first operand is less than the second.
    public static BooleanBinding lessThan(final ObservableNumberValue op1, final int op2) {
        return compare(LT, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether the first operand is less than the second.
    public static BooleanBinding lessThan(final int op1, final ObservableNumberValue op2) {
        return compare(LT, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether the first number is greater than or equal to the second.
    public static BooleanBinding greaterThanOrEqual(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return compare(GE, op1, op2, 0.0);
    }

    /// Returns a binding telling whether the first operand is greater than or equal to the second.
    public static BooleanBinding greaterThanOrEqual(final ObservableNumberValue op1, final double op2) {
        return compare(GE, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether the first operand is greater than or equal to the second.
    public static BooleanBinding greaterThanOrEqual(final double op1, final ObservableNumberValue op2) {
        return compare(GE, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether the first operand is greater than or equal to the second.
    public static BooleanBinding greaterThanOrEqual(final ObservableNumberValue op1, final float op2) {
        return compare(GE, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether the first operand is greater than or equal to the second.
    public static BooleanBinding greaterThanOrEqual(final float op1, final ObservableNumberValue op2) {
        return compare(GE, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether the first operand is greater than or equal to the second.
    public static BooleanBinding greaterThanOrEqual(final ObservableNumberValue op1, final long op2) {
        return compare(GE, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether the first operand is greater than or equal to the second.
    public static BooleanBinding greaterThanOrEqual(final long op1, final ObservableNumberValue op2) {
        return compare(GE, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether the first operand is greater than or equal to the second.
    public static BooleanBinding greaterThanOrEqual(final ObservableNumberValue op1, final int op2) {
        return compare(GE, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether the first operand is greater than or equal to the second.
    public static BooleanBinding greaterThanOrEqual(final int op1, final ObservableNumberValue op2) {
        return compare(GE, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether the first number is less than or equal to the second.
    public static BooleanBinding lessThanOrEqual(final ObservableNumberValue op1, final ObservableNumberValue op2) {
        return compare(LE, op1, op2, 0.0);
    }

    /// Returns a binding telling whether the first operand is less than or equal to the second.
    public static BooleanBinding lessThanOrEqual(final ObservableNumberValue op1, final double op2) {
        return compare(LE, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether the first operand is less than or equal to the second.
    public static BooleanBinding lessThanOrEqual(final double op1, final ObservableNumberValue op2) {
        return compare(LE, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether the first operand is less than or equal to the second.
    public static BooleanBinding lessThanOrEqual(final ObservableNumberValue op1, final float op2) {
        return compare(LE, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether the first operand is less than or equal to the second.
    public static BooleanBinding lessThanOrEqual(final float op1, final ObservableNumberValue op2) {
        return compare(LE, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether the first operand is less than or equal to the second.
    public static BooleanBinding lessThanOrEqual(final ObservableNumberValue op1, final long op2) {
        return compare(LE, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether the first operand is less than or equal to the second.
    public static BooleanBinding lessThanOrEqual(final long op1, final ObservableNumberValue op2) {
        return compare(LE, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether the first operand is less than or equal to the second.
    public static BooleanBinding lessThanOrEqual(final ObservableNumberValue op1, final int op2) {
        return compare(LE, op1, Const.of(op2), 0.0);
    }

    /// Returns a binding telling whether the first operand is less than or equal to the second.
    public static BooleanBinding lessThanOrEqual(final int op1, final ObservableNumberValue op2) {
        return compare(LE, Const.of(op1), op2, 0.0);
    }

    /// Returns a binding telling whether two strings are equal; `null` counts as empty.
    public static BooleanBinding equal(final ObservableStringValue op1, final ObservableStringValue op2) {
        return strings(EQ, op1, op2);
    }

    /// Returns a binding telling whether two strings are equal; `null` counts as empty.
    public static BooleanBinding equal(final ObservableStringValue op1, final String op2) {
        return strings(EQ, op1, new Const.Text(op2));
    }

    /// Returns a binding telling whether two strings are equal; `null` counts as empty.
    public static BooleanBinding equal(final String op1, final ObservableStringValue op2) {
        return strings(EQ, new Const.Text(op1), op2);
    }

    /// Returns a binding telling whether two strings differ; `null` counts as empty.
    public static BooleanBinding notEqual(final ObservableStringValue op1, final ObservableStringValue op2) {
        return strings(NE, op1, op2);
    }

    /// Returns a binding telling whether two strings differ; `null` counts as empty.
    public static BooleanBinding notEqual(final ObservableStringValue op1, final String op2) {
        return strings(NE, op1, new Const.Text(op2));
    }

    /// Returns a binding telling whether two strings differ; `null` counts as empty.
    public static BooleanBinding notEqual(final String op1, final ObservableStringValue op2) {
        return strings(NE, new Const.Text(op1), op2);
    }

    /// Returns a binding telling whether two strings are equal ignoring case; `null` counts as empty.
    public static BooleanBinding equalIgnoreCase(final ObservableStringValue op1, final ObservableStringValue op2) {
        return strings(EQ_IC, op1, op2);
    }

    /// Returns a binding telling whether two strings are equal ignoring case; `null` counts as empty.
    public static BooleanBinding equalIgnoreCase(final ObservableStringValue op1, final String op2) {
        return strings(EQ_IC, op1, new Const.Text(op2));
    }

    /// Returns a binding telling whether two strings are equal ignoring case; `null` counts as empty.
    public static BooleanBinding equalIgnoreCase(final String op1, final ObservableStringValue op2) {
        return strings(EQ_IC, new Const.Text(op1), op2);
    }

    /// Returns a binding telling whether two strings differ even ignoring case; `null` counts as empty.
    public static BooleanBinding notEqualIgnoreCase(final ObservableStringValue op1, final ObservableStringValue op2) {
        return strings(NE_IC, op1, op2);
    }

    /// Returns a binding telling whether two strings differ even ignoring case; `null` counts as empty.
    public static BooleanBinding notEqualIgnoreCase(final ObservableStringValue op1, final String op2) {
        return strings(NE_IC, op1, new Const.Text(op2));
    }

    /// Returns a binding telling whether two strings differ even ignoring case; `null` counts as empty.
    public static BooleanBinding notEqualIgnoreCase(final String op1, final ObservableStringValue op2) {
        return strings(NE_IC, new Const.Text(op1), op2);
    }

    /// Returns a binding telling whether two strings are in descending order; `null` counts as empty.
    public static BooleanBinding greaterThan(final ObservableStringValue op1, final ObservableStringValue op2) {
        return strings(GT, op1, op2);
    }

    /// Returns a binding telling whether two strings are in descending order; `null` counts as empty.
    public static BooleanBinding greaterThan(final ObservableStringValue op1, final String op2) {
        return strings(GT, op1, new Const.Text(op2));
    }

    /// Returns a binding telling whether two strings are in descending order; `null` counts as empty.
    public static BooleanBinding greaterThan(final String op1, final ObservableStringValue op2) {
        return strings(GT, new Const.Text(op1), op2);
    }

    /// Returns a binding telling whether two strings are in ascending order; `null` counts as empty.
    public static BooleanBinding lessThan(final ObservableStringValue op1, final ObservableStringValue op2) {
        return strings(LT, op1, op2);
    }

    /// Returns a binding telling whether two strings are in ascending order; `null` counts as empty.
    public static BooleanBinding lessThan(final ObservableStringValue op1, final String op2) {
        return strings(LT, op1, new Const.Text(op2));
    }

    /// Returns a binding telling whether two strings are in ascending order; `null` counts as empty.
    public static BooleanBinding lessThan(final String op1, final ObservableStringValue op2) {
        return strings(LT, new Const.Text(op1), op2);
    }

    /// Returns a binding telling whether two strings are equal or in descending order; `null` counts as empty.
    public static BooleanBinding greaterThanOrEqual(final ObservableStringValue op1, final ObservableStringValue op2) {
        return strings(GE, op1, op2);
    }

    /// Returns a binding telling whether two strings are equal or in descending order; `null` counts as empty.
    public static BooleanBinding greaterThanOrEqual(final ObservableStringValue op1, final String op2) {
        return strings(GE, op1, new Const.Text(op2));
    }

    /// Returns a binding telling whether two strings are equal or in descending order; `null` counts as empty.
    public static BooleanBinding greaterThanOrEqual(final String op1, final ObservableStringValue op2) {
        return strings(GE, new Const.Text(op1), op2);
    }

    /// Returns a binding telling whether two strings are equal or in ascending order; `null` counts as empty.
    public static BooleanBinding lessThanOrEqual(final ObservableStringValue op1, final ObservableStringValue op2) {
        return strings(LE, op1, op2);
    }

    /// Returns a binding telling whether two strings are equal or in ascending order; `null` counts as empty.
    public static BooleanBinding lessThanOrEqual(final ObservableStringValue op1, final String op2) {
        return strings(LE, op1, new Const.Text(op2));
    }

    /// Returns a binding telling whether two strings are equal or in ascending order; `null` counts as empty.
    public static BooleanBinding lessThanOrEqual(final String op1, final ObservableStringValue op2) {
        return strings(LE, new Const.Text(op1), op2);
    }

    // ---- booleans

    /// Returns a binding that is true while both operands are.
    public static BooleanBinding and(final ObservableBooleanValue op1, final ObservableBooleanValue op2) {
        operands(op1, op2);
        return new Fn.BooleanFn(() -> op1.get() && op2.get(), op1, op2);
    }

    /// Returns a binding that is true while either operand is.
    public static BooleanBinding or(final ObservableBooleanValue op1, final ObservableBooleanValue op2) {
        operands(op1, op2);
        return new Fn.BooleanFn(() -> op1.get() || op2.get(), op1, op2);
    }

    /// Returns a binding holding the opposite of a boolean.
    public static BooleanBinding not(final ObservableBooleanValue op) {
        if (op == null) {
            throw new NullPointerException("Operand cannot be null.");
        }
        return new Fn.BooleanFn(() -> !op.get(), op);
    }

    /// Returns a binding telling whether two booleans are equal.
    public static BooleanBinding equal(final ObservableBooleanValue op1, final ObservableBooleanValue op2) {
        operands(op1, op2);
        return new Fn.BooleanFn(() -> op1.get() == op2.get(), op1, op2);
    }

    /// Returns a binding telling whether two booleans differ.
    public static BooleanBinding notEqual(final ObservableBooleanValue op1, final ObservableBooleanValue op2) {
        operands(op1, op2);
        return new Fn.BooleanFn(() -> op1.get() != op2.get(), op1, op2);
    }

    // ---- objects

    private static boolean same(Object x, Object y) {
        return x == null ? y == null : x.equals(y);
    }

    /// Returns a binding telling whether two objects are equal.
    public static BooleanBinding equal(final ObservableObjectValue<?> op1, final ObservableObjectValue<?> op2) {
        operands(op1, op2);
        return new Fn.BooleanFn(() -> same(op1.get(), op2.get()), op1, op2);
    }

    /// Returns a binding telling whether an object equals a constant.
    public static BooleanBinding equal(final ObservableObjectValue<?> op1, Object op2) {
        if (op1 == null) {
            throw new NullPointerException("Operands cannot be null.");
        }
        return new Fn.BooleanFn(() -> same(op1.get(), op2), op1);
    }

    /// Returns a binding telling whether a constant equals an object.
    public static BooleanBinding equal(Object op1, final ObservableObjectValue<?> op2) {
        if (op2 == null) {
            throw new NullPointerException("Operands cannot be null.");
        }
        return new Fn.BooleanFn(() -> same(op1, op2.get()), op2);
    }

    /// Returns a binding telling whether two objects differ.
    public static BooleanBinding notEqual(final ObservableObjectValue<?> op1, final ObservableObjectValue<?> op2) {
        operands(op1, op2);
        return new Fn.BooleanFn(() -> !same(op1.get(), op2.get()), op1, op2);
    }

    /// Returns a binding telling whether an object differs from a constant.
    public static BooleanBinding notEqual(final ObservableObjectValue<?> op1, Object op2) {
        if (op1 == null) {
            throw new NullPointerException("Operands cannot be null.");
        }
        return new Fn.BooleanFn(() -> !same(op1.get(), op2), op1);
    }

    /// Returns a binding telling whether a constant differs from an object.
    public static BooleanBinding notEqual(Object op1, final ObservableObjectValue<?> op2) {
        if (op2 == null) {
            throw new NullPointerException("Operands cannot be null.");
        }
        return new Fn.BooleanFn(() -> !same(op1, op2.get()), op2);
    }

    /// Returns a binding telling whether a value is `null`.
    public static BooleanBinding isNull(final ObservableObjectValue<?> op) {
        if (op == null) {
            throw new NullPointerException("Operand cannot be null.");
        }
        return new Fn.BooleanFn(() -> op.get() == null, op);
    }

    /// Returns a binding telling whether a value is not `null`.
    public static BooleanBinding isNotNull(final ObservableObjectValue<?> op) {
        if (op == null) {
            throw new NullPointerException("Operand cannot be null.");
        }
        return new Fn.BooleanFn(() -> op.get() != null, op);
    }

    // ---- strings

    /// Returns a binding holding the length of a string; zero for `null`.
    public static IntegerBinding length(final ObservableStringValue op) {
        if (op == null) {
            throw new NullPointerException("Operand cannot be null");
        }
        return new Fn.IntegerFn(() -> {
            String value = op.get();
            return value == null ? 0 : value.length();
        }, op);
    }

    /// Returns a binding telling whether a string is `null` or empty.
    public static BooleanBinding isEmpty(final ObservableStringValue op) {
        if (op == null) {
            throw new NullPointerException("Operand cannot be null");
        }
        return new Fn.BooleanFn(() -> {
            String value = op.get();
            return value == null || value.length() == 0;
        }, op);
    }

    /// Returns a binding telling whether a string has any character.
    public static BooleanBinding isNotEmpty(final ObservableStringValue op) {
        if (op == null) {
            throw new NullPointerException("Operand cannot be null");
        }
        return new Fn.BooleanFn(() -> {
            String value = op.get();
            return value != null && value.length() > 0;
        }, op);
    }

    /// Returns an expression holding the text of every argument in a row.
    /// An argument that is an observable value contributes its current
    /// value and is observed.
    public static StringExpression concat(Object... args) {
        final Object[] source = args == null ? new Object[0] : args.clone();
        return new Fn.StringFn(() -> {
            StringBuilder result = new StringBuilder();
            for (Object arg : source) {
                result.append(arg instanceof ObservableValue ? ((ObservableValue<?>) arg).getValue() : arg);
            }
            return result.toString();
        }, observables(source));
    }

    /// Returns any observable value as a string expression: the value itself
    /// when it already is one, otherwise a binding holding its text.
    public static StringExpression convert(ObservableValue<?> observableValue) {
        if (observableValue instanceof StringExpression) {
            return (StringExpression) observableValue;
        }
        return text(observableValue);
    }

    /// Returns an expression holding the arguments formatted with a
    /// `String.format` pattern. An argument that is an observable value
    /// contributes its current value and is observed.
    public static StringExpression format(String format, Object... args) {
        return formatted(format, args);
    }

    // ---- collections

    /// Returns a binding holding the size of a list.
    public static <E> IntegerBinding size(final ObservableList<E> op) {
        if (op == null) {
            throw new NullPointerException("List cannot be null.");
        }
        return new Fn.IntegerFn(() -> op.size(), op);
    }

    /// Returns a binding telling whether a list is empty.
    public static <E> BooleanBinding isEmpty(final ObservableList<E> op) {
        if (op == null) {
            throw new NullPointerException("List cannot be null.");
        }
        return new Fn.BooleanFn(() -> op.isEmpty(), op);
    }

    /// Returns a binding telling whether a list has any element.
    public static <E> BooleanBinding isNotEmpty(final ObservableList<E> op) {
        if (op == null) {
            throw new NullPointerException("List cannot be null.");
        }
        return new Fn.BooleanFn(() -> !op.isEmpty(), op);
    }

    /// Returns a binding holding the element at a position of a list, or
    /// `null` while the list is shorter than that.
    public static <E> ObjectBinding<E> valueAt(final ObservableList<E> op, final int index) {
        if (op == null) {
            throw new NullPointerException("List cannot be null.");
        }
        if (index < 0) {
            throw new IllegalArgumentException("Index cannot be negative");
        }
        return new Fn.ObjectFn<E>(() -> index < op.size() ? op.get(index) : null, op);
    }

    /// Returns a binding holding the element at an observed position of a
    /// list, or `null` while the position is outside the list.
    public static <E> ObjectBinding<E> valueAt(final ObservableList<E> op, final ObservableIntegerValue index) {
        return valueAt(op, (ObservableNumberValue) index);
    }

    /// Returns a binding holding the element at an observed position of a
    /// list, or `null` while the position is outside the list.
    public static <E> ObjectBinding<E> valueAt(final ObservableList<E> op, final ObservableNumberValue index) {
        if (op == null || index == null) {
            throw new NullPointerException("Operands cannot be null.");
        }
        return new Fn.ObjectFn<E>(() -> {
            int at = index.intValue();
            return at >= 0 && at < op.size() ? op.get(at) : null;
        }, op, index);
    }

    /// Returns a binding holding the size of a set.
    public static <E> IntegerBinding size(final ObservableSet<E> op) {
        if (op == null) {
            throw new NullPointerException("Set cannot be null.");
        }
        return new Fn.IntegerFn(() -> op.size(), op);
    }

    /// Returns a binding telling whether a set is empty.
    public static <E> BooleanBinding isEmpty(final ObservableSet<E> op) {
        if (op == null) {
            throw new NullPointerException("Set cannot be null.");
        }
        return new Fn.BooleanFn(() -> op.isEmpty(), op);
    }

    /// Returns a binding telling whether a set has any element.
    public static <E> BooleanBinding isNotEmpty(final ObservableSet<E> op) {
        if (op == null) {
            throw new NullPointerException("Set cannot be null.");
        }
        return new Fn.BooleanFn(() -> !op.isEmpty(), op);
    }

    /// Returns a binding holding the size of a map.
    public static <K, V> IntegerBinding size(final ObservableMap<K, V> op) {
        if (op == null) {
            throw new NullPointerException("Map cannot be null.");
        }
        return new Fn.IntegerFn(() -> op.size(), op);
    }

    /// Returns a binding telling whether a map is empty.
    public static <K, V> BooleanBinding isEmpty(final ObservableMap<K, V> op) {
        if (op == null) {
            throw new NullPointerException("Map cannot be null.");
        }
        return new Fn.BooleanFn(() -> op.isEmpty(), op);
    }

    /// Returns a binding telling whether a map has any entry.
    public static <K, V> BooleanBinding isNotEmpty(final ObservableMap<K, V> op) {
        if (op == null) {
            throw new NullPointerException("Map cannot be null.");
        }
        return new Fn.BooleanFn(() -> !op.isEmpty(), op);
    }

    /// Returns a binding holding the value a map has for a key.
    public static <K, V> ObjectBinding<V> valueAt(final ObservableMap<K, V> op, final K key) {
        if (op == null) {
            throw new NullPointerException("Map cannot be null.");
        }
        return new Fn.ObjectFn<V>(() -> op.get(key), op);
    }

    /// Returns a binding holding the value a map has for an observed key.
    public static <K, V> ObjectBinding<V> valueAt(final ObservableMap<K, V> op,
            final ObservableValue<? extends K> key) {
        if (op == null || key == null) {
            throw new NullPointerException("Operands cannot be null.");
        }
        return new Fn.ObjectFn<V>(() -> op.get(key.getValue()), op, key);
    }

    // ---- ternary

    /// Starts a ternary binding: `when(condition).then(a).otherwise(b)`.
    public static When when(final ObservableBooleanValue condition) {
        return new When(condition);
    }

    // ---- bidirectional bindings

    /// Keeps two properties equal, starting with the value of the second.
    /// Neither property keeps the other alive.
    public static <T> void bindBidirectional(Property<T> property1, Property<T> property2) {
        BidirectionalBinding.bind(property1, property2);
    }

    /// Removes a bidirectional binding between two properties.
    public static <T> void unbindBidirectional(Property<T> property1, Property<T> property2) {
        BidirectionalBinding.unbind(property1, property2);
    }

    /// Removes a bidirectional binding between two objects, whatever kind
    /// it was.
    public static void unbindBidirectional(Object property1, Object property2) {
        BidirectionalBinding.unbind(property1, property2);
    }

    /// Keeps a string property and a property of another type in sync
    /// through a converter, starting with the text of the second.
    public static <T> void bindBidirectional(Property<String> stringProperty, Property<T> otherProperty,
            final StringConverter<T> converter) {
        if (converter == null) {
            throw new NullPointerException("Converter cannot be null");
        }
        BidirectionalBinding.bind(stringProperty, otherProperty, value -> converter.toString(value),
                text -> converter.fromString(text));
    }

    // ---- content bindings

    /// Makes a list mirror an observable list: it takes the content now and
    /// follows every later change. The list is held weakly.
    public static <E> void bindContent(List<E> list1, ObservableList<? extends E> list2) {
        ContentBindings.bind(list1, list2);
    }

    /// Makes a set mirror an observable set, held weakly.
    public static <E> void bindContent(Set<E> set1, ObservableSet<? extends E> set2) {
        ContentBindings.bind(set1, set2);
    }

    /// Makes a map mirror an observable map, held weakly.
    public static <K, V> void bindContent(Map<K, V> map1, ObservableMap<? extends K, ? extends V> map2) {
        ContentBindings.bind(map1, map2);
    }

    /// Removes a content binding of the first object to the second.
    public static void unbindContent(Object obj1, Object obj2) {
        ContentBindings.unbind(obj1, obj2);
    }

    /// Keeps two observable lists equal, starting with the content of the
    /// second. Neither list keeps the other alive.
    public static <E> void bindContentBidirectional(ObservableList<E> list1, ObservableList<E> list2) {
        ContentBindings.bindBidirectional(list1, list2);
    }

    /// Removes a bidirectional content binding between two objects.
    public static void unbindContentBidirectional(Object obj1, Object obj2) {
        ContentBindings.unbindBidirectional(obj1, obj2);
    }
}
