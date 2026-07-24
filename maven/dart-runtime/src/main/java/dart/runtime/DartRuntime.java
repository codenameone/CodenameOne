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
     * Dart's {@code x!} null-check operator.
     */
    public static <T> T nn(T v) {
        if (v == null) {
            throw new TypeError("Null check operator used on a null value");
        }
        return v;
    }

    /**
     * Dart's {@code ==} between reference values: null-safe, delegates to
     * equals (user {@code operator ==} overrides equals).
     */
    public static boolean eq(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
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
        String s = Double.toString(d);
        int e = s.indexOf('E');
        if (e < 0) {
            return s;
        }
        // Normalize Java's "1.0E21" to Dart's "1e+21" style.
        String mantissa = s.substring(0, e);
        String exp = s.substring(e + 1);
        if (mantissa.endsWith(".0")) {
            mantissa = mantissa.substring(0, mantissa.length() - 2);
        }
        if (!exp.startsWith("-")) {
            exp = "+" + exp;
        }
        return mantissa + "e" + exp;
    }

    /**
     * Dart's {@code num.toStringAsFixed(fractionDigits)} — a fixed-point decimal
     * string with exactly {@code fractionDigits} digits after the point, rounding
     * half away from zero. Implemented with integer scaling only (no String.format
     * / BigDecimal / Math.rint), which the ParparVM minimal JavaAPI lacks.
     */
    public static String toStringAsFixed(double d, long fractionDigits) {
        if (Double.isNaN(d)) {
            return "NaN";
        }
        if (Double.isInfinite(d)) {
            return d > 0 ? "Infinity" : "-Infinity";
        }
        int n = (int) fractionDigits;
        if (n < 0) {
            n = 0;
        }
        boolean neg = d < 0;
        double abs = Math.abs(d);
        double pow = 1;
        for (int i = 0; i < n; i++) {
            pow *= 10;
        }
        long scaled = (long) Math.floor(abs * pow + 0.5);
        String digits = Long.toString(scaled);
        StringBuilder sb = new StringBuilder();
        if (neg && scaled != 0) {
            sb.append('-');
        }
        if (n == 0) {
            sb.append(digits);
            return sb.toString();
        }
        while (digits.length() <= n) {
            digits = "0" + digits;
        }
        int split = digits.length() - n;
        sb.append(digits.substring(0, split)).append('.').append(digits.substring(split));
        return sb.toString();
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
        if (thrown instanceof Throwable t) {
            return new RuntimeException(t);
        }
        return new dart.core.DartException(str(thrown));
    }

    /** Marker for switch arms `dart analyze` proved unreachable. */
    public static RuntimeException unreachable() {
        return new IllegalStateException("unreachable code reached — transpiler/analyzer mismatch");
    }
}
