/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.runtime;

import dart.core.TypeError;

/**
 * Static helpers the transpiler-generated Java code calls into for Dart
 * operator and core-language semantics that have no direct Java equivalent.
 *
 * <p>Signatures here are a stable contract with the code generator — do not
 * change them without updating {@code JavaEmitter} in the dart-transpiler
 * module and its golden tests.</p>
 */
public final class DartRuntime {

    private DartRuntime() {
    }

    /** Pluggable sink for Dart's top-level print(); tests capture output here. */
    private static Funcs.VoidFunc1<String> printSink;

    /**
     * What the app was doing when a failure happens, appended to runtime
     * errors. A UI framework built on this runtime sets it around a build;
     * without it a {@code !} failure names no location at all, because the
     * transpiled call sites are inlined into their caller's frame and the
     * stack trace shows only the framework's own recursion.
     */
    private static Object diagnosticContext;

    /**
     * Sets (or clears, with null) the context appended to runtime errors.
     *
     * <p>Takes an OBJECT, not a formatted string. The context is set on every
     * widget build and read only when something actually fails, so formatting
     * it eagerly allocated one String per widget per frame to describe an
     * error that almost never happens. A non-String value is described by its
     * class when — and only when — a failure asks for it.</p>
     */
    public static void diagnosticContext(Object context) {
        diagnosticContext = context;
    }

    public static String diagnosticContext() {
        Object c = diagnosticContext;
        if (c == null) {
            return null;
        }
        if (c instanceof String) {
            return (String) c;
        }
        return "building " + c.getClass().getName();
    }

    /** The raw context value, for a caller that only wants to save and restore it. */
    public static Object diagnosticContextValue() {
        return diagnosticContext;
    }

    /**
     * Dart's {@code x!} null-check operator.
     */
    public static <T> T nn(T v) {
        if (v == null) {
            String where = diagnosticContext();
            throw new TypeError("Null check operator used on a null value"
                    + (where == null ? "" : " (while " + where + ")"));
        }
        return v;
    }

    /**
     * Dart's {@code ==} between reference values: null-safe, delegates to
     * equals (user {@code operator ==} overrides equals).
     */
    /**
     * Dart's {@code Object.hashAll(objects)}: a hash of the ELEMENTS, in order, combined
     * as java.util.Objects.hash combines its arguments. Passing the iterable to
     * Objects.hash hashed the collection object itself -- by identity, for a Dart list --
     * so two equal value objects hashing {@code [a, b]} got different hashes and never
     * found each other's map or set entries.
     */
    public static long hashAll(Iterable<?> objects) {
        int h = 1;
        if (objects != null) {
            for (Object o : objects) {
                h = 31 * h + (o == null ? 0 : o.hashCode());
            }
        }
        return h;
    }

    /**
     * Whether {@code t} is what Dart calls an Error, for {@code on Error}. This runtime's
     * Dart errors -- StateError, ArgumentError, RangeError, TypeError, UnsupportedError,
     * LateInitializationError and the rest -- are RuntimeExceptions, so the Java
     * {@code catch (Error e)} that {@code on Error} used to become caught none of them.
     * Everything thrown is an Error except the Dart Exceptions (DartException,
     * FormatException) and Java's checked exceptions.
     */
    public static boolean isDartError(Throwable t) {
        if (t instanceof Error) {
            return true;
        }
        return t instanceof RuntimeException && !(t instanceof dart.core.DartException)
                && !(t instanceof dart.core.FormatException) && !(t instanceof dart.core.DartThrown);
    }

    /** Throws {@code t} on unchanged; declared to return so a caller can write {@code throw rethrow(t)}. */
    public static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }

    /** Collections whose toString is running, innermost last; compared by identity. */
    private static final java.util.ArrayList<Object> FORMATTING = new java.util.ArrayList<Object>();

    /**
     * Starts formatting {@code collection}, or answers false when it is already being
     * formatted further up -- a list that contains itself, directly or through another
     * collection. Dart prints such a reference as {@code [...]} (or {@code {...}},
     * {@code (...)}); formatting it again recursed until the stack overflowed.
     * Pair every true answer with {@link #endFormat}.
     */
    public static boolean beginFormat(Object collection) {
        for (int i = 0; i < FORMATTING.size(); i++) {
            if (FORMATTING.get(i) == collection) {
                return false;
            }
        }
        FORMATTING.add(collection);
        return true;
    }

    public static void endFormat(Object collection) {
        for (int i = FORMATTING.size() - 1; i >= 0; i--) {
            if (FORMATTING.get(i) == collection) {
                FORMATTING.remove(i);
                return;
            }
        }
    }

    /**
     * Dart's {@code identical(a, b)}: the same object -- except that numbers and booleans
     * are values, not objects, in Dart, so two boxes of the same int (or of the same
     * double bits, or the same bool) are identical even when Java allocated them apart.
     */
    public static boolean identical(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a instanceof Long && b instanceof Long) {
            return ((Long) a).longValue() == ((Long) b).longValue();
        }
        if (a instanceof Double && b instanceof Double) {
            return Double.doubleToLongBits(((Double) a).doubleValue())
                    == Double.doubleToLongBits(((Double) b).doubleValue());
        }
        if (a instanceof Boolean && b instanceof Boolean) {
            return ((Boolean) a).booleanValue() == ((Boolean) b).booleanValue();
        }
        return false;
    }

    public static boolean eq(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            return numEq((Number) a, (Number) b);
        }
        return a == null ? b == null : a.equals(b);
    }

    /**
     * Dart's {@code ==} between two boxed numbers, which compares VALUES across
     * int and double: {@code 1 == 1.0} is true. Java's wrapper equals is false
     * for a Long against a Double, so a value that reached eq through dynamic,
     * Object or a boxed collection element -- {@code List<num>.contains(1.0)}
     * holding the int 1 -- compared unequal. Double's equals is also wrong for
     * Dart in both directions: NaN is never == itself and 0.0 == -0.0, which
     * the primitive comparison below gets right.
     *
     * An int against a double is compared after converting the int to double --
     * measured on the Dart 3.9 VM, int.parse('9007199254740993') == 9007199254740992.0
     * is true, and so is the largest int against 2^63. This used to compare exactly
     * and answered false for both, which no Dart program observes. (compareTo IS
     * exact on the VM; see DartComparable.compareIntDouble. ==, <, >, min and max
     * are not, and neither is this.)
     */
    private static boolean numEq(Number a, Number b) {
        boolean ai = isIntegral(a);
        boolean bi = isIntegral(b);
        if (ai && bi) {
            return a.longValue() == b.longValue();
        }
        if (!ai && !bi) {
            return a.doubleValue() == b.doubleValue();
        }
        long l = ai ? a.longValue() : b.longValue();
        double d = ai ? b.doubleValue() : a.doubleValue();
        return (double) l == d;
    }

    private static boolean isIntegral(Number n) {
        return n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte;
    }

    // --- Operators on dynamic operands ---------------------------------------
    //
    // The transpiler emits Java operators when it knows the operand types. When
    // an operand is `dynamic`, Dart picks the operator at run time from the
    // value; these do the same for the built-in operand types. A user class's
    // operator cannot be reached from here (there is no reflection on every
    // port), so it fails with an error that says so rather than as invalid Java.

    /**
     * Dart's binary {@code op} on values of unknown static type: arithmetic,
     * {@code ~/} and {@code %}, the bitwise and shift operators, String
     * {@code +} and {@code *}, and List {@code +}. int op int stays int, any
     * double makes the result double, and {@code /} is always double.
     */
    public static Object dynBinary(String op, Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            Number x = (Number) a;
            Number y = (Number) b;
            boolean ints = isIntegral(x) && isIntegral(y);
            if (op.equals("/")) {
                return Double.valueOf(x.doubleValue() / y.doubleValue());
            }
            if (op.equals("~/")) {
                return Long.valueOf(ints ? tdiv(x.longValue(), y.longValue())
                        : tdiv(x.doubleValue(), y.doubleValue()));
            }
            if (op.equals("%")) {
                return ints ? (Object) Long.valueOf(mod(x.longValue(), y.longValue()))
                        : (Object) Double.valueOf(mod(x.doubleValue(), y.doubleValue()));
            }
            if (op.equals("+") || op.equals("-") || op.equals("*")) {
                if (ints) {
                    long l = x.longValue();
                    long r = y.longValue();
                    return Long.valueOf(op.equals("+") ? l + r : op.equals("-") ? l - r : l * r);
                }
                double l = x.doubleValue();
                double r = y.doubleValue();
                return Double.valueOf(op.equals("+") ? l + r : op.equals("-") ? l - r : l * r);
            }
            if (ints) {
                long l = x.longValue();
                long r = y.longValue();
                if (op.equals("&")) {
                    return Long.valueOf(l & r);
                }
                if (op.equals("|")) {
                    return Long.valueOf(l | r);
                }
                if (op.equals("^")) {
                    return Long.valueOf(l ^ r);
                }
                // Dart's shift-count rules, as the typed operators get -- changing
                // only an operand's static type must not change the result.
                if (op.equals("<<")) {
                    return Long.valueOf(shl(l, r));
                }
                if (op.equals(">>")) {
                    return Long.valueOf(shr(l, r));
                }
                if (op.equals(">>>")) {
                    return Long.valueOf(ushr(l, r));
                }
            }
        }
        if (a instanceof String) {
            if (op.equals("+")) {
                if (!(b instanceof String)) {
                    throw new TypeError("type '" + typeName(b) + "' is not a subtype of type 'String'");
                }
                return (String) a + (String) b;
            }
            if (op.equals("*") && b instanceof Number && isIntegral((Number) b)) {
                return dart.core.DString.repeat((String) a, ((Number) b).longValue());
            }
        }
        if (op.equals("+") && a instanceof java.util.List && b instanceof java.util.List) {
            return dart.core.DartList.concat((java.util.List<?>) a, (java.util.List<?>) b);
        }
        throw noOperator(op, a, b);
    }

    /** Dart's relational {@code op} ({@code < > <= >=}) on values of unknown static type. */
    public static boolean dynCompare(String op, Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            Number x = (Number) a;
            Number y = (Number) b;
            int c;
            if (isIntegral(x) && isIntegral(y)) {
                long l = x.longValue();
                long r = y.longValue();
                c = l < r ? -1 : l > r ? 1 : 0;
            } else {
                // An int against a double is compared as doubles, deliberately: that is
                // what Dart's relational operators do. Measured on the Dart 3.9 VM,
                // 9007199254740993 > 9007199254740992.0 is FALSE (statically, as num, and
                // as dynamic), because the int is converted first. Only compareTo is exact.
                double l = x.doubleValue();
                double r = y.doubleValue();
                if (Double.isNaN(l) || Double.isNaN(r)) {
                    return false;   // every comparison with NaN is false
                }
                c = l < r ? -1 : l > r ? 1 : 0;
            }
            if (op.equals("<")) {
                return c < 0;
            }
            if (op.equals(">")) {
                return c > 0;
            }
            if (op.equals("<=")) {
                return c <= 0;
            }
            if (op.equals(">=")) {
                return c >= 0;
            }
        }
        throw noOperator(op, a, b);
    }

    /**
     * Answers {@code rest} once {@code evaluated} has been evaluated. The transpiler
     * chains it to evaluate call arguments in Dart's source order when that differs
     * from the parameter order -- {@code seq($t1 = b(), seq($t2 = a(), true))} runs b
     * before a, because Java evaluates arguments left to right. Not generic, so it
     * never takes part in the call's type inference.
     */
    public static boolean seq(Object evaluated, boolean rest) {
        return rest;
    }

    // --- int shifts and double rounding, with Dart's rules ----------------------
    //
    // Java masks a long shift count to its low six bits, so 1 >> 64 was 1 and
    // 1 << -1 a value, where Dart answers 0 and throws. Java's Math.round rounds a
    // half toward positive infinity, so (-1.5).round() was -1 where Dart rounds half
    // away from zero to -2. The emitter keeps a native shift for a constant count
    // of 0..63, where the two agree.

    private static long checkShift(long count) {
        if (count < 0) {
            throw new dart.core.ArgumentError("Invalid argument: negative shift count " + count);
        }
        return count;
    }

    /** Dart's {@code a << b}. */
    public static long shl(long a, long b) {
        return checkShift(b) >= 64 ? 0L : a << b;
    }

    /** Dart's {@code a >> b}: arithmetic, so a negative value shifts to -1. */
    public static long shr(long a, long b) {
        return checkShift(b) >= 64 ? (a < 0 ? -1L : 0L) : a >> b;
    }

    /** Dart's {@code a >>> b}. */
    public static long ushr(long a, long b) {
        return checkShift(b) >= 64 ? 0L : a >>> b;
    }

    /**
     * Dart's {@code double.roundToDouble()}: half away from zero, keeping the sign
     * of a zero result. Computed from the floor rather than as floor(d + 0.5),
     * which rounds 0.49999999999999994 up to 1.
     */
    public static double roundToDouble(double d) {
        if (d != d || Double.isInfinite(d)) {
            return d;
        }
        if (d < 0) {
            return -roundHalfUpNonNegative(-d);
        }
        return roundHalfUpNonNegative(d);
    }

    private static double roundHalfUpNonNegative(double d) {
        double f = Math.floor(d);
        return d - f >= 0.5 ? f + 1 : f;
    }

    /** Dart's {@code double.round()}: as {@link #roundToDouble}, as an int. */
    public static long round(double d) {
        if (d != d || Double.isInfinite(d)) {
            throw new dart.core.UnsupportedError("Unsupported operation: " + doubleStr(d) + ".round()");
        }
        return (long) roundToDouble(d);
    }

    /**
     * Dart's {@code double.toInt()} (and truncate): toward zero, but refusing NaN and
     * the infinities with UnsupportedError. A Java (long) cast made NaN 0 and saturated
     * the infinities, so validation around a bad value went quietly through.
     */
    public static long toInt(double d) {
        checkFinite(d, "toInt");
        return (long) d;
    }

    /** Dart's {@code double.floor()}: an int, refusing NaN and the infinities. */
    public static long floor(double d) {
        checkFinite(d, "floor");
        return (long) Math.floor(d);
    }

    /** Dart's {@code double.ceil()}: an int, refusing NaN and the infinities. */
    public static long ceil(double d) {
        checkFinite(d, "ceil");
        return (long) Math.ceil(d);
    }

    private static void checkFinite(double d, String op) {
        if (d != d || Double.isInfinite(d)) {
            throw new dart.core.UnsupportedError("Unsupported operation: " + doubleStr(d) + "." + op + "()");
        }
    }

    /**
     * Dart's {@code int.clamp(lower, upper)}: ArgumentError when lower is above upper.
     * Nested Math.min/max answered a plausible value instead -- 5.clamp(10, 0) was 0.
     */
    public static long clamp(long v, long lower, long upper) {
        if (lower > upper) {
            throw new dart.core.ArgumentError("Invalid argument(s): " + upper + " < " + lower);
        }
        return v < lower ? lower : v > upper ? upper : v;
    }

    /** Dart's {@code double.clamp}, ordered by num.compareTo (NaN compares greatest). */
    public static double clamp(double v, double lower, double upper) {
        if (Double.compare(lower, upper) > 0) {
            throw new dart.core.ArgumentError("Invalid argument(s): " + doubleStr(upper) + " < " + doubleStr(lower));
        }
        if (Double.compare(v, lower) < 0) {
            return lower;
        }
        return Double.compare(v, upper) > 0 ? upper : v;
    }

    /** Dart's unary minus on a value of unknown static type. */
    public static Object dynNegate(Object a) {
        if (a instanceof Number) {
            Number x = (Number) a;
            return isIntegral(x) ? (Object) Long.valueOf(-x.longValue()) : (Object) Double.valueOf(-x.doubleValue());
        }
        throw noOperator("unary-", a, null);
    }

    /** Dart's {@code ~} on a value of unknown static type. */
    public static Object dynBitNot(Object a) {
        if (a instanceof Number && isIntegral((Number) a)) {
            return Long.valueOf(~((Number) a).longValue());
        }
        throw noOperator("~", a, null);
    }

    /** A dynamic value used where Dart requires a bool: {@code !x}, a condition. */
    public static boolean dynBool(Object a) {
        if (a instanceof Boolean) {
            return ((Boolean) a).booleanValue();
        }
        throw new TypeError("type '" + typeName(a) + "' is not a subtype of type 'bool'");
    }

    /**
     * A postfix {@code x++} on a dynamic {@code x}: the transpiler emits
     * {@code dynPostfix(x, x = dynBinary("+", x, 1))}, and Java evaluates the
     * arguments left to right, so this answers the value from before the write.
     */
    public static Object dynPostfix(Object before, Object after) {
        return before;
    }

    private static RuntimeException noOperator(String op, Object a, Object b) {
        if (a == null) {
            return new TypeError("Null check operator used on a null value (operator " + op + ")");
        }
        return new dart.core.UnsupportedError("Operator '" + op + "' on a dynamic value of type "
                + typeName(a) + (b == null ? "" : " and " + typeName(b))
                + " is not supported: the transpiler only dispatches the built-in number, String and"
                + " List operators at run time. Give the operand a static type.");
    }

    private static String typeName(Object o) {
        if (o == null) {
            return "Null";
        }
        if (o instanceof Long || o instanceof Integer) {
            return "int";
        }
        if (o instanceof Double) {
            return "double";
        }
        if (o instanceof String) {
            return "String";
        }
        if (o instanceof Boolean) {
            return "bool";
        }
        return o.getClass().getName();
    }

    /**
     * Dart's truncating division {@code ~/} — truncates toward zero, which
     * matches Java integer division.
     */
    public static long tdiv(long a, long b) {
        if (b == 0) {
            throw new UnsupportedOperationException("Result of truncating division is not representable: " + a + " ~/ 0");
        }
        return a / b;
    }

    public static long tdiv(double a, double b) {
        double r = a / b;
        if (Double.isNaN(r) || Double.isInfinite(r)) {
            throw new UnsupportedOperationException("Result of truncating division is not representable: " + a + " ~/ " + b);
        }
        return (long) r;
    }

    /**
     * Dart's euclidean-style {@code %}: the result is always non-negative
     * when the divisor is non-zero, unlike Java's remainder.
     */
    public static long mod(long a, long b) {
        long r = a % b;
        return r < 0 ? r + Math.abs(b) : r;
    }

    public static double mod(double a, double b) {
        double r = a % b;
        return r < 0 ? r + Math.abs(b) : r;
    }

    /**
     * Dart string conversion used by string interpolation and print:
     * null prints as "null", doubles use Dart's formatting.
     */
    public static String str(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Double d) {
            return doubleStr(d);
        }
        if (v instanceof Float f) {
            return doubleStr(f);
        }
        return v.toString();
    }

    public static String str(long v) {
        return Long.toString(v);
    }

    public static String str(double v) {
        return doubleStr(v);
    }

    public static String str(boolean v) {
        return Boolean.toString(v);
    }

    /**
     * Dart's double.toString(): integral values keep a trailing ".0"
     * (Dart prints 1.0, Java prints 1.0 too via Double.toString, but Java
     * switches to scientific notation at different magnitudes). Values that
     * are mathematically integral and within the safe range render as
     * "&lt;digits&gt;.0"; everything else falls back to Java's shortest
     * representation, with exponent formatting normalized to Dart's ("e+21"
     * instead of "E21").
     */
    public static String doubleStr(double d) {
        if (Double.isNaN(d)) {
            return "NaN";
        }
        if (Double.isInfinite(d)) {
            return d > 0 ? "Infinity" : "-Infinity";
        }
        // Integral test without Math.rint (absent from the ParparVM minimal JavaAPI):
        // inside the |d| < 1e16 (< 2^53) guard the long truncation is exact, so an
        // integral double round-trips through (long) unchanged.
        if (Math.abs(d) < 1e16 && d == (double) (long) d) {
            long l = (long) d;
            // Negative-zero detection without doubleToRawLongBits (also absent): the
            // non-raw doubleToLongBits (a supported native) yields the same bit pattern
            // for -0.0 as raw, and NaN was already handled above.
            if (l == 0 && Double.doubleToLongBits(d) != 0L) {
                return "-0.0";
            }
            return l + ".0";
        }
        return dartNotation(d);
    }

    /**
     * The layout Dart gives a double's shortest digits -- the JavaScript
     * Number-to-String rule: plain notation for magnitudes from 1e-7 up to 1e21,
     * exponent notation outside it. Java's own thresholds are 1e-3 and 1e7, so its
     * text was wrong in both bands: 0.0001 printed as 1e-4 and 12345678.9 as
     * 1.23456789e+7. Java's digits are reused -- they are the shortest that round
     * trip -- and only the point moves.
     */
    private static String dartNotation(double d) {
        String s = Double.toString(Math.abs(d));
        int ePos = s.indexOf('E');
        String mantissa = ePos < 0 ? s : s.substring(0, ePos);
        int exp10 = ePos < 0 ? 0 : Integer.parseInt(s.substring(ePos + 1));
        int dot = mantissa.indexOf('.');
        String digits = dot < 0 ? mantissa : mantissa.substring(0, dot) + mantissa.substring(dot + 1);
        // n: where the decimal point falls, counted in digits from the left.
        int n = (dot < 0 ? mantissa.length() : dot) + exp10;
        int lead = 0;
        while (lead < digits.length() - 1 && digits.charAt(lead) == '0') {
            lead++;
        }
        digits = digits.substring(lead);
        n -= lead;
        int end = digits.length();
        while (end > 1 && digits.charAt(end - 1) == '0') {
            end--;
        }
        digits = digits.substring(0, end);
        int k = digits.length();
        StringBuilder out = new StringBuilder();
        if (d < 0) {
            out.append('-');
        }
        if (k <= n && n <= 21) {
            out.append(digits);
            for (int i = k; i < n; i++) {
                out.append('0');
            }
            return out.append(".0").toString();
        }
        if (0 < n && n <= 21) {
            return out.append(digits, 0, n).append('.').append(digits, n, k).toString();
        }
        if (-6 < n && n <= 0) {
            out.append("0.");
            for (int i = n; i < 0; i++) {
                out.append('0');
            }
            return out.append(digits).toString();
        }
        int e = n - 1;
        out.append(digits.charAt(0));
        if (k > 1) {
            out.append('.').append(digits, 1, k);
        }
        return out.append('e').append(e < 0 ? '-' : '+').append(Math.abs(e)).toString();
    }

    /**
     * Dart's {@code int.toRadixString}: a radix outside 2..36 is a RangeError.
     * Java's Long.toString(v, radix) silently falls back to base 10 there, so
     * 255.toRadixString(1) printed "255"; and the radix is checked as the Dart
     * int it is, before the narrowing to a Java int could wrap it into range.
     */
    public static String toRadixString(long v, long radix) {
        dart.core.RangeError.checkValueInInterval(radix, 2, 36, "radix");
        return Long.toString(v, (int) radix);
    }

    /**
     * Dart's {@code num.toStringAsFixed(fractionDigits)}: exactly
     * {@code fractionDigits} digits after the point, for 0 through 20. The
     * digits are those of the double's EXACT binary value rounded half up, as
     * Dart (and JavaScript's toFixed) define it -- so 1.005, which is really
     * 1.00499999999999989..., gives 1.00 -- and a magnitude of 1e21 or more
     * falls back to toString, as Dart does.
     *
     * The value is m * 2^e with an integer m, so value * 10^n is m * 10^n *
     * 2^e, computed in the small multi-word integer below; ParparVM's minimal
     * JavaAPI has no BigInteger or BigDecimal. The previous version scaled in a
     * double and a long, which failed twice over: 1.0.toStringAsFixed(20)
     * scaled to 1e20, past Long.MAX_VALUE, and printed 0.0922... instead of
     * 1.000...; and scaling in floating point rounded where exact arithmetic
     * would not.
     */
    public static String toStringAsFixed(double d, long fractionDigits) {
        if (fractionDigits < 0 || fractionDigits > 20) {
            throw new dart.core.RangeError("Invalid value: fractionDigits must be in 0..20: "
                    + fractionDigits);
        }
        if (Double.isNaN(d)) {
            return "NaN";
        }
        if (Double.isInfinite(d)) {
            return d > 0 ? "Infinity" : "-Infinity";
        }
        if (Math.abs(d) >= 1e21) {
            return str(d);
        }
        int n = (int) fractionDigits;
        long bits = Double.doubleToLongBits(d);
        int biased = (int) ((bits >>> 52) & 0x7ff);
        long mantissa = bits & 0xfffffffffffffL;
        int exp;
        if (biased == 0) {
            exp = -1074;
        } else {
            mantissa |= 1L << 52;
            exp = biased - 1075;
        }
        int[] v = fromLong(mantissa);
        for (int i = 0; i < n; i++) {
            v = mulSmall(v, 10);
        }
        if (exp >= 0) {
            v = shiftLeft(v, exp);
        } else {
            // Round half up: add the bit just below the cut, then drop the rest.
            boolean half = testBit(v, -exp - 1);
            v = shiftRight(v, -exp);
            if (half) {
                v = addOne(v);
            }
        }
        String digits = toDecimal(v);
        StringBuilder sb = new StringBuilder();
        // Dart keeps the sign of a negative value that rounds to zero:
        // (-0.001).toStringAsFixed(2) is -0.00.
        if (d < 0) {
            sb.append('-');
        }
        if (n == 0) {
            return sb.append(digits).toString();
        }
        StringBuilder padded = new StringBuilder();
        for (int i = digits.length(); i <= n; i++) {
            padded.append('0');
        }
        digits = padded.append(digits).toString();
        int split = digits.length() - n;
        return sb.append(digits, 0, split).append('.').append(digits, split, digits.length()).toString();
    }

    // Minimal unsigned multi-word integers for toStringAsFixed: little-endian
    // 32-bit words in an int[], no leading-zero words except for zero itself.

    private static int[] fromLong(long x) {
        return trim(new int[] {(int) x, (int) (x >>> 32)});
    }

    private static int[] trim(int[] w) {
        int len = w.length;
        while (len > 1 && w[len - 1] == 0) {
            len--;
        }
        if (len == w.length) {
            return w;
        }
        int[] out = new int[len];
        System.arraycopy(w, 0, out, 0, len);
        return out;
    }

    private static int[] mulSmall(int[] w, int m) {
        int[] out = new int[w.length + 1];
        long carry = 0;
        for (int i = 0; i < w.length; i++) {
            long p = (w[i] & 0xffffffffL) * m + carry;
            out[i] = (int) p;
            carry = p >>> 32;
        }
        out[w.length] = (int) carry;
        return trim(out);
    }

    private static int[] shiftLeft(int[] w, int k) {
        int words = k >>> 5;
        int bits = k & 31;
        int[] out = new int[w.length + words + 1];
        for (int i = 0; i < w.length; i++) {
            long x = (w[i] & 0xffffffffL) << bits;
            out[i + words] |= (int) x;
            out[i + words + 1] |= (int) (x >>> 32);
        }
        return trim(out);
    }

    private static int[] shiftRight(int[] w, int k) {
        int words = k >>> 5;
        int bits = k & 31;
        if (words >= w.length) {
            return new int[] {0};
        }
        int[] out = new int[w.length - words];
        for (int i = 0; i < out.length; i++) {
            long lo = w[i + words] & 0xffffffffL;
            long hi = i + words + 1 < w.length ? w[i + words + 1] & 0xffffffffL : 0L;
            out[i] = (int) (((hi << 32) | lo) >>> bits);
        }
        return trim(out);
    }

    private static boolean testBit(int[] w, int k) {
        int word = k >>> 5;
        return word < w.length && ((w[word] >>> (k & 31)) & 1) != 0;
    }

    private static int[] addOne(int[] w) {
        int[] out = new int[w.length + 1];
        long carry = 1;
        for (int i = 0; i < w.length; i++) {
            long s = (w[i] & 0xffffffffL) + carry;
            out[i] = (int) s;
            carry = s >>> 32;
        }
        out[w.length] = (int) carry;
        return trim(out);
    }

    private static String toDecimal(int[] w) {
        if (w.length == 1) {
            return Long.toString(w[0] & 0xffffffffL);
        }
        // Peel off nine decimal digits at a time.
        StringBuilder chunks = new StringBuilder();
        int[] cur = w;
        while (cur.length > 1 || cur[0] != 0) {
            int[] q = new int[cur.length];
            long rem = 0;
            for (int i = cur.length - 1; i >= 0; i--) {
                long x = (rem << 32) | (cur[i] & 0xffffffffL);
                q[i] = (int) (x / 1000000000L);
                rem = x % 1000000000L;
            }
            cur = trim(q);
            String part = Long.toString(rem);
            boolean more = cur.length > 1 || cur[0] != 0;
            if (more) {
                for (int i = part.length(); i < 9; i++) {
                    part = "0" + part;
                }
            }
            chunks.insert(0, part);
        }
        return chunks.toString();
    }

    /**
     * Dart's top-level print(). Routed through a pluggable sink so
     * behavioral tests can capture output deterministically.
     */
    public static void print(Object v) {
        String s = str(v);
        Funcs.VoidFunc1<String> sink = printSink;
        if (sink != null) {
            sink.call(s);
        } else {
            System.out.println(s);
        }
    }

    public static void print(long v) {
        print((Object) Long.toString(v));
    }

    public static void print(double v) {
        print((Object) doubleStr(v));
    }

    public static void print(boolean v) {
        print((Object) Boolean.toString(v));
    }

    /** Install a print sink (tests); pass null to restore System.out. */
    public static void setPrintSink(Funcs.VoidFunc1<String> sink) {
        printSink = sink;
    }

    /**
     * Dart's `throw expr` accepts any object; wrap non-throwables so the
     * JVM can propagate them while toString stays Dart-like.
     */
    public static RuntimeException asError(Object thrown) {
        if (thrown instanceof RuntimeException re) {
            return re;
        }
        if (thrown instanceof Error err) {
            throw err;   // thrown as itself: an Error cannot be returned as a RuntimeException
        }
        if (thrown instanceof Throwable t) {
            return new RuntimeException(t);
        }
        // Any other object is carried, not converted, so a catch gets it back.
        return new dart.core.DartThrown(thrown);
    }

    /**
     * What a Dart catch clause binds for {@code t}: the thrown object itself when Dart
     * code threw a non-exception value ({@link dart.core.DartThrown}), else {@code t}.
     */
    public static Object caught(Throwable t) {
        return t instanceof dart.core.DartThrown ? ((dart.core.DartThrown) t).value() : t;
    }

    /**
     * Whether {@code t} is what Dart calls an Exception, for {@code on Exception}: not a
     * Dart error and not a thrown non-exception value -- a thrown String is neither.
     */
    public static boolean isDartException(Throwable t) {
        return !isDartError(t) && !(t instanceof dart.core.DartThrown);
    }

    /** Marker for switch arms `dart analyze` proved unreachable. */
    public static RuntimeException unreachable() {
        return new IllegalStateException("unreachable code reached — transpiler/analyzer mismatch");
    }
}
