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
/**
 * Exercises java.lang.String.format and prints one line per case so the JDK's
 * output and ParparVM's output can be diffed line by line.
 *
 * Every line is emitted as CASE|&lt;label&gt;|&lt;rendering&gt;. A rendering is either the
 * formatted text (with control characters escaped so a case stays on one line) or
 * EX|&lt;exception class name&gt; when the call threw -- a thrown Java exception is a
 * legitimate outcome that both runtimes must agree on.
 */
public class StringFormatApp {
    private static final StringBuilder OUT = new StringBuilder();

    private static void f(String label, String format, Object... args) {
        String rendering;
        try {
            rendering = escape(String.format(format, args));
        } catch (Throwable t) {
            rendering = "EX|" + t.getClass().getName();
        }
        OUT.append("CASE|").append(label).append('|').append(rendering).append('\n');
    }

    private static String escape(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static void strings() {
        f("s.plain", "%s", "hello");
        f("s.null", "[%s]", new Object[] { null });
        f("s.width", "[%10s]", "abc");
        f("s.left", "[%-10s]", "abc");
        f("s.precision", "[%.3s]", "abcdef");
        f("s.widthPrecision", "[%8.3s]", "abcdef");
        f("s.upper", "%S", "abc");
        f("s.number", "%s", Integer.valueOf(42));
        f("s.double", "%s", Double.valueOf(1.5));
        // %s on a double is Double.toString, which the float conversions build on.
        f("s.doubleThird", "%s", Double.valueOf(1.0 / 3.0));
        f("s.doubleLarge", "%s", Double.valueOf(1e30));
        f("s.doubleTiny", "%s", Double.valueOf(1e-10));
        f("s.doubleNegativeZero", "%s", Double.valueOf(-0.0));
        f("s.float", "%s", Float.valueOf(0.1f));
        f("s.longNegative", "%s", Long.valueOf(-9223372036854775808L));
        f("b.true", "%b", Boolean.TRUE);
        f("b.false", "%b", Boolean.FALSE);
        f("b.null", "%b", new Object[] { null });
        f("b.object", "%b", "text");
        f("b.upper", "%B", Boolean.TRUE);
        f("h.string", "%h", "abc");
        f("h.null", "%h", new Object[] { null });
        f("c.char", "%c", Character.valueOf('x'));
        f("c.upper", "%C", Character.valueOf('x'));
        f("c.int", "%c", Integer.valueOf(65));
        f("c.width", "[%5c]", Character.valueOf('x'));
        f("c.null", "%c", new Object[] { null });
        f("literal.percent", "100%%");
        f("literal.newline", "a%nb");
        f("literal.mixed", "%s-%d-%c%%", "cn1", Integer.valueOf(7), Character.valueOf('A'));
        f("index.explicit", "%2$s %1$s %2$s", "one", "two");
        f("index.previous", "%s %<s %<s", "echo");
        // A null varargs array is not an empty argument list: the JVM feeds every
        // specifier a null instead of reporting a missing argument. "%<" is the one
        // exception, because it reuses an argument that has to have existed.
        f("nullArray.s", "[%s]", (Object[]) null);
        f("nullArray.twoArgs", "[%s %s]", (Object[]) null);
        f("nullArray.d", "[%d]", (Object[]) null);
        f("nullArray.floatWidth", "[%5.2f]", (Object[]) null);
        f("nullArray.upper", "[%S]", (Object[]) null);
        f("nullArray.boolean", "[%b]", (Object[]) null);
        f("nullArray.explicitIndex", "[%2$s]", (Object[]) null);
        f("nullArray.previousAfterArg", "[%s %<s]", (Object[]) null);
        f("nullArray.previousFirst", "%<s", (Object[]) null);
        f("emptyArray.s", "%s", new Object[0]);
    }

    private static void integers() {
        f("d.zero", "%d", Integer.valueOf(0));
        f("d.positive", "%d", Integer.valueOf(42));
        f("d.negative", "%d", Integer.valueOf(-42));
        f("d.intMin", "%d", Integer.valueOf(-2147483648));
        f("d.longMax", "%d", Long.valueOf(9223372036854775807L));
        f("d.longMin", "%d", Long.valueOf(-9223372036854775808L));
        f("d.byte", "%d", Byte.valueOf((byte) -7));
        f("d.short", "%d", Short.valueOf((short) -7));
        f("d.width", "[%8d]", Integer.valueOf(42));
        f("d.left", "[%-8d]", Integer.valueOf(42));
        f("d.zeroPad", "[%08d]", Integer.valueOf(42));
        f("d.zeroPadNegative", "[%08d]", Integer.valueOf(-42));
        f("d.plus", "[%+d]", Integer.valueOf(42));
        f("d.plusNegative", "[%+d]", Integer.valueOf(-42));
        f("d.space", "[% d]", Integer.valueOf(42));
        f("d.paren", "[%(d]", Integer.valueOf(-42));
        f("d.parenPositive", "[%(d]", Integer.valueOf(42));
        f("d.grouped", "%,d", Integer.valueOf(1234567));
        f("d.groupedExact", "%,d", Integer.valueOf(1000));
        f("d.groupedNegative", "%,d", Integer.valueOf(-1234567));
        f("d.groupedParen", "%,(d", Integer.valueOf(-1234567));
        f("d.groupedZeroPad", "[%,012d]", Integer.valueOf(1234));
        f("x.small", "%x", Integer.valueOf(255));
        f("x.negativeInt", "%x", Integer.valueOf(-1));
        f("x.negativeLong", "%x", Long.valueOf(-1L));
        f("x.byte", "%x", Byte.valueOf((byte) -1));
        f("x.short", "%x", Short.valueOf((short) -1));
        f("x.upper", "%X", Integer.valueOf(48879));
        f("x.alt", "%#x", Integer.valueOf(255));
        f("x.altUpper", "%#X", Integer.valueOf(255));
        f("x.zeroPad", "[%08x]", Integer.valueOf(255));
        f("x.width", "[%8x]", Integer.valueOf(255));
        f("o.small", "%o", Integer.valueOf(8));
        f("o.negativeInt", "%o", Integer.valueOf(-1));
        f("o.alt", "%#o", Integer.valueOf(8));
    }

    private static void floats() {
        f("f.zero", "%f", Double.valueOf(0.0));
        f("f.negativeZero", "%f", Double.valueOf(-0.0));
        f("f.one", "%f", Double.valueOf(1.0));
        f("f.simple", "%f", Double.valueOf(1.5));
        f("f.negative", "%f", Double.valueOf(-1.5));
        f("f.third", "%f", Double.valueOf(1.0 / 3.0));
        f("f.large", "%f", Double.valueOf(1e30));
        f("f.tiny", "%f", Double.valueOf(1e-10));
        f("f.float", "%f", Float.valueOf(1.5f));
        f("f.floatImprecise", "%f", Float.valueOf(1.1f));
        // A Float must format at its binary32 value; the divergence only shows past six
        // decimals, which is why the six-decimal default hid it.
        f("f.floatTenDecimals", "%.10f", Float.valueOf(1.1f));
        f("f.floatSeventh", "%.10f", Float.valueOf(1.0f / 7.0f));
        f("f.nan", "%f", Double.valueOf(Double.NaN));
        f("f.infinity", "%f", Double.valueOf(Double.POSITIVE_INFINITY));
        f("f.negativeInfinity", "%f", Double.valueOf(Double.NEGATIVE_INFINITY));
        f("f.parenInfinity", "%(f", Double.valueOf(Double.NEGATIVE_INFINITY));

        // The reproducer from issue 5482.
        f("f.p3", "%.3f", Double.valueOf(0.4567));
        f("f.p3.stats", "size=%d avg=%.3f", Integer.valueOf(12), Double.valueOf(2.0 / 3.0));

        f("f.p0.half", "%.0f", Double.valueOf(0.5));
        f("f.p0.twoHalf", "%.0f", Double.valueOf(2.5));
        f("f.p0.threeHalf", "%.0f", Double.valueOf(3.5));
        f("f.p0.belowHalf", "%.0f", Double.valueOf(0.4));
        f("f.p1.fifteen", "%.1f", Double.valueOf(0.15));
        f("f.p2.oneOhOhFive", "%.2f", Double.valueOf(1.005));
        f("f.p2.roundsUpToTen", "%.2f", Double.valueOf(9.999));
        f("f.p2.small", "%.2f", Double.valueOf(0.005));
        f("f.p2.tooSmall", "%.2f", Double.valueOf(0.0006));
        f("f.p20", "%.20f", Double.valueOf(0.1));
        f("f.p10.third", "%.10f", Double.valueOf(1.0 / 3.0));
        f("f.p15.seventh", "%.15f", Double.valueOf(1.0 / 7.0));
        f("e.p15.third", "%.15e", Double.valueOf(1.0 / 3.0));
        f("f.p6.carry", "%f", Double.valueOf(0.9999999));
        f("f.width", "[%10.2f]", Double.valueOf(3.14159));
        f("f.left", "[%-10.2f]", Double.valueOf(3.14159));
        f("f.zeroPad", "[%010.2f]", Double.valueOf(3.14159));
        f("f.zeroPadNegative", "[%010.2f]", Double.valueOf(-3.14159));
        f("f.plus", "[%+.2f]", Double.valueOf(3.14159));
        f("f.grouped", "%,.2f", Double.valueOf(1234567.891));
        f("f.groupedNegative", "%,.2f", Double.valueOf(-1234567.891));
        f("f.alt", "%#.0f", Double.valueOf(3.0));

        f("e.zero", "%e", Double.valueOf(0.0));
        f("e.simple", "%e", Double.valueOf(12345.6789));
        f("e.negative", "%e", Double.valueOf(-12345.6789));
        f("e.tiny", "%e", Double.valueOf(1e-5));
        f("e.huge", "%e", Double.valueOf(1.23e300));
        f("e.p2", "%.2e", Double.valueOf(12345.6789));
        f("e.p0", "%.0e", Double.valueOf(12345.6789));
        f("e.upper", "%E", Double.valueOf(12345.6789));
        f("e.width", "[%15.3e]", Double.valueOf(12345.6789));
        f("e.carry", "%.2e", Double.valueOf(9.999));

        f("g.zero", "%g", Double.valueOf(0.0));
        f("g.lowBoundary", "%g", Double.valueOf(0.0001));
        f("g.belowBoundary", "%g", Double.valueOf(0.00001));
        f("g.highBoundary", "%g", Double.valueOf(100000.0));
        f("g.aboveBoundary", "%g", Double.valueOf(123456789.0));
        f("g.simple", "%g", Double.valueOf(1.5));
        f("g.p3", "%.3g", Double.valueOf(1234.5678));
        f("g.upper", "%G", Double.valueOf(0.00001));
    }

    private static void failures() {
        f("bad.unknownConversion", "%q", "x");
        // The conversion is validated before an argument is looked for, so a bad
        // conversion with no arguments is an unknown conversion, not a missing argument.
        f("bad.unknownConversionNoArgs", "%q");
        f("bad.groupingOnHexNoArgs", "%,x");
        // Validation order: a character reports an unsupported flag ahead of the missing
        // width, the opposite of every other conversion, and a wrong argument type
        // outranks the deferred sign-flag check on %x and %o.
        f("bad.charFlagBeforeWidth", "%-0c", Character.valueOf('a'));
        f("bad.charAltBeforeWidth", "%-#c", Character.valueOf('a'));
        f("bad.stringWidthBeforeFlag", "%-0s", "a");
        f("bad.hexWrongTypeBeforeFlag", "%+x", "text");
        f("bad.octalWrongTypeBeforeFlag", "%(o", Double.valueOf(1.5));
        f("bad.zeroPadOnStringNoArgs", "%08s");
        f("bad.precisionOnDNoArgs", "%.2d");
        f("bad.trailingPercent", "abc%");
        f("bad.missingArgument", "%s %s", "only");
        f("bad.noArguments", "%s");
        f("bad.wrongTypeForD", "%d", "text");
        f("bad.wrongTypeForF", "%f", "text");
        f("bad.wrongTypeForC", "%c", Double.valueOf(1.0));
        f("bad.precisionOnD", "%.2d", Integer.valueOf(1));
        f("bad.uppercaseD", "%D", Integer.valueOf(1));
        // '<' is a flag like any other, so repeating it is a duplicate flag.
        f("bad.repeatedPrevious", "%s %<<s", "a");
        f("bad.repeatedPreviousAlone", "%<<s", "a");
        f("bad.repeatedMinus", "%--5s", "a");
        f("bad.repeatedComma", "%,,d", Integer.valueOf(1));
        // A '.' with no digits after it is a malformed specifier, not a precision of
        // zero: every supported JDK reports the '.' as an unknown conversion.
        f("bad.emptyPrecision", "%.s", "x");
        f("bad.emptyPrecisionOnD", "%.d", Integer.valueOf(1));
        f("bad.emptyPrecisionOnF", "%.f", Double.valueOf(1.0));
    }

    /**
     * Conversions ParparVM deliberately does not implement. The JDK formats these, so
     * they cannot go through the shared diff -- the test asserts the ParparVM rendering
     * on its own, to pin the behavior as a catchable exception rather than garbage.
     */
    private static void unsupported() {
        g("hexFloat", "%a", Double.valueOf(2.5));
        g("dateTime", "%tY", Long.valueOf(0L));
        // Argument indexes are 1-based. JDK 16 and later reject index zero; JDK 11 still
        // accepts it, so this cannot go through the shared diff.
        g("zeroArgumentIndex", "%0$s", "A");
    }

    private static void g(String label, String format, Object... args) {
        String rendering;
        try {
            rendering = escape(String.format(format, args));
        } catch (Throwable t) {
            rendering = "EX|" + t.getClass().getName();
        }
        OUT.append("CN1ONLY|").append(label).append('|').append(rendering).append('\n');
    }

    public static void main(String[] args) {
        strings();
        integers();
        floats();
        failures();
        unsupported();
        System.out.println(OUT.toString());
        System.out.println("DONE");
    }
}
