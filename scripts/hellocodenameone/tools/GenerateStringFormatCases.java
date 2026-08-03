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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generates the expected-value table baked into StringFormatTest, so the device-side
 * expectations are demonstrably what a real Java SE java.util.Formatter produces rather
 * than something hand-written.
 *
 * <pre>
 *   java scripts/hellocodenameone/tools/GenerateStringFormatCases.java
 * </pre>
 *
 * Paste the emitted block over the corresponding block in StringFormatTest.java.
 *
 * Cases are deliberately restricted to behaviour that is identical on every runtime the
 * suite runs on -- JDK 8 through 25, Android's libcore, and ParparVM. Excluded on purpose:
 *
 * <ul>
 *   <li>{@code %a} and {@code %t}: ParparVM does not implement them.</li>
 *   <li>{@code %0$s}: accepted before JDK 16, rejected from JDK 16 on.</li>
 *   <li>{@code %n}: the JDK emits the platform line separator, so it is "\r\n" on a
 *       Windows JVM and "\n" everywhere else. StringFormatTest asserts it is one of the
 *       two rather than pinning a value.</li>
 * </ul>
 *
 * Everything is generated with Locale.ROOT and written with '.' as the decimal separator
 * and ',' as the grouping separator; StringFormatTest re-localises before comparing,
 * because on JavaSE and Android the platform formatter follows the default locale.
 *
 * <p>The exception <em>type</em> recorded for a malformed format is what Java SE raises.
 * StringFormatTest only requires that some IllegalArgumentException is raised, because
 * Android's libcore does not always agree with OpenJDK on the subtype -- {@code "%.s"}
 * raises IllegalFormatPrecisionException there and UnknownFormatConversionException on
 * OpenJDK. ParparVM's exact subtypes are pinned against the JDK in vm/tests instead.</p>
 */
public class GenerateStringFormatCases {

    private static final List<String> LINES = new ArrayList<String>();

    private static void c(String label, String format, Object... args) {
        String value = String.format(Locale.ROOT, format, args);
        LINES.add("            check(\"" + label + "\", " + literal(value)
                + ", String.format(" + literal(format) + argList(args) + "));");
    }

    /** A case whose format string is malformed and must be rejected on every port. */
    private static void bad(String label, String format, Object... args) {
        String type;
        try {
            String.format(Locale.ROOT, format, args);
            throw new IllegalStateException("expected " + label + " to throw");
        } catch (IllegalArgumentException e) {
            type = e.getClass().getName();
        }
        LINES.add("            checkThrows(\"" + label + "\", \"" + type + "\", () -> String.format("
                + literal(format) + argList(args) + "));");
    }

    private static String argList(Object[] args) {
        StringBuilder sb = new StringBuilder();
        for (Object a : args) {
            sb.append(", ").append(argLiteral(a));
        }
        return sb.toString();
    }

    private static String argLiteral(Object a) {
        if (a == null) {
            return "(Object) null";
        }
        if (a instanceof String) {
            return literal((String) a);
        }
        if (a instanceof Integer) {
            return "Integer.valueOf(" + a + ")";
        }
        if (a instanceof Long) {
            return "Long.valueOf(" + a + "L)";
        }
        if (a instanceof Short) {
            return "Short.valueOf((short) " + a + ")";
        }
        if (a instanceof Byte) {
            return "Byte.valueOf((byte) " + a + ")";
        }
        if (a instanceof Character) {
            return "Character.valueOf('" + a + "')";
        }
        if (a instanceof Boolean) {
            return "Boolean.valueOf(" + a + ")";
        }
        if (a instanceof Float) {
            return "Float.valueOf(" + floatLiteral(((Float) a).floatValue()) + "f)";
        }
        if (a instanceof Double) {
            return "Double.valueOf(" + doubleLiteral(((Double) a).doubleValue()) + ")";
        }
        throw new IllegalArgumentException("unsupported argument type " + a.getClass());
    }

    private static String doubleLiteral(double d) {
        if (Double.isNaN(d)) {
            return "Double.NaN";
        }
        if (d == Double.POSITIVE_INFINITY) {
            return "Double.POSITIVE_INFINITY";
        }
        if (d == Double.NEGATIVE_INFINITY) {
            return "Double.NEGATIVE_INFINITY";
        }
        return Double.toString(d);
    }

    private static String floatLiteral(float f) {
        return Float.toString(f);
    }

    private static String literal(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '"' || ch == '\\') {
                sb.append('\\').append(ch);
            } else if (ch == '\n') {
                sb.append("\\n");
            } else if (ch == '\r') {
                sb.append("\\r");
            } else if (ch < 0x20 || ch > 0x7e) {
                sb.append(String.format("\\u%04x", (int) ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.append('"').toString();
    }

    public static void main(String[] args) {
        LINES.add("            // ---- strings and characters ----");
        c("s.plain", "%s", "hello");
        c("s.null", "[%s]", (Object) null);
        c("s.width", "[%10s]", "abc");
        c("s.left", "[%-10s]", "abc");
        c("s.precision", "[%.3s]", "abcdef");
        c("s.widthPrecision", "[%8.3s]", "abcdef");
        c("s.upper", "%S", "abc");
        c("s.boxedInt", "%s", Integer.valueOf(42));
        c("s.boxedLongMin", "%s", Long.valueOf(-9223372036854775808L));
        c("b.true", "%b", Boolean.TRUE);
        c("b.false", "%b", Boolean.FALSE);
        c("b.null", "%b", (Object) null);
        c("b.nonBoolean", "%b", "text");
        c("b.upper", "%B", Boolean.TRUE);
        c("h.string", "%h", "abc");
        c("h.null", "%h", (Object) null);
        c("c.char", "%c", Character.valueOf('x'));
        c("c.upper", "%C", Character.valueOf('x'));
        c("c.codePoint", "%c", Integer.valueOf(65));
        c("c.width", "[%5c]", Character.valueOf('x'));
        c("c.null", "%c", (Object) null);
        c("literal.percent", "100%%");
        c("literal.mixed", "%s-%d-%c%%", "cn1", Integer.valueOf(7), Character.valueOf('A'));
        c("index.explicit", "%2$s %1$s %2$s", "one", "two");
        c("index.previous", "%s %<s %<s", "echo");

        LINES.add("");
        LINES.add("            // ---- integers ----");
        c("d.zero", "%d", Integer.valueOf(0));
        c("d.positive", "%d", Integer.valueOf(42));
        c("d.negative", "%d", Integer.valueOf(-42));
        c("d.intMin", "%d", Integer.valueOf(Integer.MIN_VALUE));
        c("d.longMax", "%d", Long.valueOf(Long.MAX_VALUE));
        c("d.longMin", "%d", Long.valueOf(Long.MIN_VALUE));
        c("d.byte", "%d", Byte.valueOf((byte) -7));
        c("d.short", "%d", Short.valueOf((short) -7));
        c("d.width", "[%8d]", Integer.valueOf(42));
        c("d.left", "[%-8d]", Integer.valueOf(42));
        c("d.zeroPad", "[%08d]", Integer.valueOf(42));
        c("d.zeroPadNegative", "[%08d]", Integer.valueOf(-42));
        c("d.plus", "[%+d]", Integer.valueOf(42));
        c("d.plusNegative", "[%+d]", Integer.valueOf(-42));
        c("d.space", "[% d]", Integer.valueOf(42));
        c("d.paren", "[%(d]", Integer.valueOf(-42));
        c("d.grouped", "%,d", Integer.valueOf(1234567));
        c("d.groupedExact", "%,d", Integer.valueOf(1000));
        c("d.groupedNegative", "%,d", Integer.valueOf(-1234567));
        c("d.groupedParen", "%,(d", Integer.valueOf(-1234567));
        c("d.groupedZeroPad", "[%,012d]", Integer.valueOf(1234));
        c("x.small", "%x", Integer.valueOf(255));
        c("x.negativeInt", "%x", Integer.valueOf(-1));
        c("x.negativeLong", "%x", Long.valueOf(-1L));
        c("x.byte", "%x", Byte.valueOf((byte) -1));
        c("x.short", "%x", Short.valueOf((short) -1));
        c("x.upper", "%X", Integer.valueOf(48879));
        c("x.alt", "%#x", Integer.valueOf(255));
        c("x.altUpper", "%#X", Integer.valueOf(255));
        c("x.zeroPad", "[%08x]", Integer.valueOf(255));
        c("o.small", "%o", Integer.valueOf(8));
        c("o.negativeInt", "%o", Integer.valueOf(-1));
        c("o.alt", "%#o", Integer.valueOf(8));

        LINES.add("");
        LINES.add("            // ---- floating point ----");
        c("f.zero", "%f", Double.valueOf(0.0));
        c("f.negativeZero", "%f", Double.valueOf(-0.0));
        c("f.simple", "%f", Double.valueOf(1.5));
        c("f.negative", "%f", Double.valueOf(-1.5));
        c("f.third", "%f", Double.valueOf(1.0 / 3.0));
        c("f.large", "%f", Double.valueOf(1e30));
        c("f.tiny", "%f", Double.valueOf(1e-10));
        c("f.float", "%f", Float.valueOf(1.5f));
        c("f.floatImprecise", "%f", Float.valueOf(1.1f));
        // A float must format at its binary32 value, which only shows past six decimals.
        c("f.floatTenDecimals", "%.10f", Float.valueOf(1.1f));
        c("f.floatSeventh", "%.10f", Float.valueOf(1.0f / 7.0f));
        c("f.nan", "%f", Double.valueOf(Double.NaN));
        c("f.infinity", "%f", Double.valueOf(Double.POSITIVE_INFINITY));
        c("f.negativeInfinity", "%f", Double.valueOf(Double.NEGATIVE_INFINITY));
        // The reproducer from issue 5482.
        c("f.stats", "size=%d avg=%.3f", Integer.valueOf(12), Double.valueOf(2.0 / 3.0));
        // Java rounds the shortest round-tripping decimal HALF_UP; C printf rounds the
        // exact binary value, and would disagree on every one of these.
        c("f.halfUp.zeroPointFive", "%.0f", Double.valueOf(0.5));
        c("f.halfUp.twoPointFive", "%.0f", Double.valueOf(2.5));
        c("f.halfUp.threePointFive", "%.0f", Double.valueOf(3.5));
        c("f.halfUp.belowHalf", "%.0f", Double.valueOf(0.4));
        c("f.halfUp.fifteenHundredths", "%.1f", Double.valueOf(0.15));
        c("f.halfUp.oneOhOhFive", "%.2f", Double.valueOf(1.005));
        c("f.carryToTen", "%.2f", Double.valueOf(9.999));
        c("f.smallCarry", "%.2f", Double.valueOf(0.005));
        c("f.belowPrecision", "%.2f", Double.valueOf(0.0006));
        c("f.padBeyondShortest", "%.20f", Double.valueOf(0.1));
        c("f.tenDecimalsOfThird", "%.10f", Double.valueOf(1.0 / 3.0));
        c("f.fifteenDecimalsOfSeventh", "%.15f", Double.valueOf(1.0 / 7.0));
        c("f.width", "[%10.2f]", Double.valueOf(3.14159));
        c("f.left", "[%-10.2f]", Double.valueOf(3.14159));
        c("f.zeroPad", "[%010.2f]", Double.valueOf(3.14159));
        c("f.zeroPadNegative", "[%010.2f]", Double.valueOf(-3.14159));
        c("f.plus", "[%+.2f]", Double.valueOf(3.14159));
        c("f.grouped", "%,.2f", Double.valueOf(1234567.891));
        c("f.groupedNegative", "%,.2f", Double.valueOf(-1234567.891));
        c("f.alt", "%#.0f", Double.valueOf(3.0));
        c("e.zero", "%e", Double.valueOf(0.0));
        c("e.simple", "%e", Double.valueOf(12345.6789));
        c("e.negative", "%e", Double.valueOf(-12345.6789));
        c("e.tiny", "%e", Double.valueOf(1e-5));
        c("e.huge", "%e", Double.valueOf(1.23e300));
        c("e.precision2", "%.2e", Double.valueOf(12345.6789));
        c("e.precision0", "%.0e", Double.valueOf(12345.6789));
        c("e.upper", "%E", Double.valueOf(12345.6789));
        c("e.carry", "%.2e", Double.valueOf(9.999));
        c("g.zero", "%g", Double.valueOf(0.0));
        c("g.lowBoundary", "%g", Double.valueOf(0.0001));
        c("g.belowBoundary", "%g", Double.valueOf(0.00001));
        c("g.highBoundary", "%g", Double.valueOf(100000.0));
        c("g.aboveBoundary", "%g", Double.valueOf(123456789.0));
        c("g.precision3", "%.3g", Double.valueOf(1234.5678));
        c("g.upper", "%G", Double.valueOf(0.00001));

        LINES.add("");
        LINES.add("            // ---- a null varargs array supplies a null per specifier ----");
        LINES.add("            check(\"nullArray.s\", " + literal(String.format(Locale.ROOT, "[%s]", (Object[]) null))
                + ", String.format(\"[%s]\", NULL_ARRAY));");
        LINES.add("            check(\"nullArray.twoArgs\", "
                + literal(String.format(Locale.ROOT, "[%s %s]", (Object[]) null))
                + ", String.format(\"[%s %s]\", NULL_ARRAY));");
        LINES.add("            check(\"nullArray.d\", " + literal(String.format(Locale.ROOT, "[%d]", (Object[]) null))
                + ", String.format(\"[%d]\", NULL_ARRAY));");
        LINES.add("            check(\"nullArray.floatWidth\", "
                + literal(String.format(Locale.ROOT, "[%5.2f]", (Object[]) null))
                + ", String.format(\"[%5.2f]\", NULL_ARRAY));");
        LINES.add("            check(\"nullArray.upper\", "
                + literal(String.format(Locale.ROOT, "[%S]", (Object[]) null))
                + ", String.format(\"[%S]\", NULL_ARRAY));");

        LINES.add("");
        LINES.add("            // ---- malformed formats must raise the same exception everywhere ----");
        bad("bad.unknownConversion", "%q", "x");
        // The conversion is validated before an argument is looked for.
        bad("bad.unknownConversionNoArgs", "%q");
        bad("bad.groupingOnHexNoArgs", "%,x");
        // The JVM reports an unsupported flag on a character ahead of the missing width,
        // which is the opposite order from every other conversion.
        bad("bad.charFlagBeforeWidth", "%-0c", Character.valueOf('a'));
        bad("bad.charAltBeforeWidth", "%-#c", Character.valueOf('a'));
        bad("bad.stringWidthBeforeFlag", "%-0s", "a");
        // A wrong argument type outranks the deferred sign-flag check on %x and %o.
        bad("bad.hexWrongTypeBeforeFlag", "%+x", "text");
        bad("bad.octalWrongTypeBeforeFlag", "%(o", Double.valueOf(1.5));
        bad("bad.trailingPercent", "abc%");
        bad("bad.missingArgument", "%s %s", "only");
        bad("bad.noArguments", "%s");
        bad("bad.wrongTypeForD", "%d", "text");
        bad("bad.wrongTypeForF", "%f", "text");
        bad("bad.precisionOnD", "%.2d", Integer.valueOf(1));
        bad("bad.emptyPrecision", "%.s", "x");
        bad("bad.repeatedPrevious", "%s %<<s", "a");
        bad("bad.repeatedMinus", "%--5s", "a");
        bad("bad.groupingOnHex", "%,x", Integer.valueOf(1));
        bad("bad.zeroPadOnString", "%08s", "a");

        System.out.println("// Generated by scripts/hellocodenameone/tools/generate-string-format-cases.java");
        System.out.println("// on " + System.getProperty("java.vendor") + " JDK "
                + System.getProperty("java.version") + " -- do not edit by hand.");
        for (String line : LINES) {
            System.out.println(line);
        }
        System.err.println("generated " + LINES.size() + " lines");
    }
}
