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
package java.lang;

import java.util.DuplicateFormatFlagsException;
import java.util.FormatFlagsConversionMismatchException;
import java.util.IllegalFormatArgumentIndexException;
import java.util.IllegalFormatCodePointException;
import java.util.IllegalFormatConversionException;
import java.util.IllegalFormatFlagsException;
import java.util.IllegalFormatPrecisionException;
import java.util.IllegalFormatWidthException;
import java.util.MissingFormatArgumentException;
import java.util.MissingFormatWidthException;
import java.util.UnknownFormatConversionException;

/**
 * The implementation behind {@link java.lang.String#format(String, Object[])}.
 *
 * <p>This used to be a native method with two wildly divergent implementations: an
 * Objective-C one that fed {@code -[NSString initWithFormat:arguments:]} a hand-rolled
 * argument vector (and then discarded the result and returned {@code [NSString init]},
 * which aborts the process), and a C fallback that ignored width and precision entirely.
 * Formatting is pure string manipulation, so there is no reason for it to be native at
 * all -- a single Java implementation behaves identically on every ParparVM target and
 * can be diffed against the JDK in a normal test.</p>
 *
 * <p>Floating point conversions deliberately round the <em>shortest round-tripping
 * decimal representation</em> of the value (i.e. what {@link Double#toString(double)}
 * produces) using HALF_UP, which is what {@code java.util.Formatter} does. That is not
 * the same as rounding the exact binary value the way C's {@code printf} does: Java
 * renders {@code String.format("%.1f", 0.15)} as {@code 0.2} where {@code printf} says
 * {@code 0.1}.</p>
 *
 * <p>Supported conversions are {@code s S b B h H c C d o x X e E f g G n %}, with the
 * {@code - + ' ' 0 , ( #} flags, width, precision, and the {@code %n$} and {@code %<}
 * argument selectors. Rendering is locale independent: grouping uses {@code ','} in
 * groups of three, the decimal separator is always {@code '.'}, and {@code %n} emits
 * {@code '\n'} (the line separator on every platform ParparVM targets).</p>
 *
 * <p>Two JDK conversions are <em>not</em> implemented: {@code %a} (hexadecimal floating
 * point) and {@code %t} (date and time). Both raise
 * {@link java.util.UnknownFormatConversionException} rather than producing something
 * wrong. Use {@code com.codename1.l10n.SimpleDateFormat} for dates.</p>
 */
final class StringFormatter {
    private static final int FLAG_MINUS = 1;
    private static final int FLAG_PLUS = 2;
    private static final int FLAG_SPACE = 4;
    private static final int FLAG_ZERO = 8;
    private static final int FLAG_COMMA = 16;
    private static final int FLAG_PAREN = 32;
    private static final int FLAG_HASH = 64;
    private static final int FLAG_PREVIOUS = 128;

    private static final char[] DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private StringFormatter() {
    }

    /**
     * A decimal significand plus the position of the decimal point, such that the value
     * being formatted is {@code 0.digits * 10^point}. {@code digits} never has leading or
     * trailing zeros, except for the value zero which is the single digit {@code "0"}.
     */
    private static final class Decimal {
        private String digits;
        private int point;
    }

    static String format(String format, Object[] args) {
        if (format == null) {
            throw new NullPointerException();
        }
        int len = format.length();
        StringBuilder out = new StringBuilder(len + 16);
        int pos = 0;
        int nextArg = 0;
        int lastArg = -1;
        while (pos < len) {
            char c = format.charAt(pos);
            if (c != '%') {
                out.append(c);
                pos++;
                continue;
            }
            int specStart = pos;
            pos++;
            if (pos >= len) {
                throw new UnknownFormatConversionException("%");
            }

            // An explicit argument index is a run of digits terminated by '$'. If the '$'
            // is not there the same digits are a width, so rewind.
            int argIndex = -1;
            int digitsEnd = pos;
            while (digitsEnd < len && isDigit(format.charAt(digitsEnd))) {
                digitsEnd++;
            }
            if (digitsEnd > pos && digitsEnd < len && format.charAt(digitsEnd) == '$') {
                argIndex = parseNumber(format, pos, digitsEnd);
                if (argIndex < 0) {
                    // Saturated: the index did not fit in an int.
                    throw new IllegalFormatArgumentIndexException(argIndex);
                }
                if (argIndex == 0) {
                    // Argument indexes are 1-based; "%0$s" has no argument to select.
                    throw new IllegalFormatArgumentIndexException(argIndex);
                }
                pos = digitsEnd + 1;
            }

            int flags = 0;
            while (pos < len) {
                char f = format.charAt(pos);
                int flag;
                if (f == '-') {
                    flag = FLAG_MINUS;
                } else if (f == '+') {
                    flag = FLAG_PLUS;
                } else if (f == ' ') {
                    flag = FLAG_SPACE;
                } else if (f == '0') {
                    flag = FLAG_ZERO;
                } else if (f == ',') {
                    flag = FLAG_COMMA;
                } else if (f == '(') {
                    flag = FLAG_PAREN;
                } else if (f == '#') {
                    flag = FLAG_HASH;
                } else if (f == '<') {
                    flag = FLAG_PREVIOUS;
                } else {
                    break;
                }
                if ((flags & flag) != 0) {
                    throw new DuplicateFormatFlagsException(String.valueOf(f));
                }
                flags |= flag;
                pos++;
            }
            boolean previous = (flags & FLAG_PREVIOUS) != 0;

            int width = -1;
            int widthStart = pos;
            while (pos < len && isDigit(format.charAt(pos))) {
                pos++;
            }
            if (pos > widthStart) {
                width = parseNumber(format, widthStart, pos);
                if (width < 0) {
                    throw new IllegalFormatWidthException(width);
                }
            }

            int precision = -1;
            if (pos < len && format.charAt(pos) == '.') {
                pos++;
                int precisionStart = pos;
                while (pos < len && isDigit(format.charAt(pos))) {
                    pos++;
                }
                if (pos == precisionStart) {
                    // "%.s" is malformed; the JVM reports the '.' as the conversion.
                    throw new UnknownFormatConversionException(".");
                }
                precision = parseNumber(format, precisionStart, pos);
                if (precision < 0) {
                    throw new IllegalFormatPrecisionException(precision);
                }
            }

            if (pos >= len) {
                throw new UnknownFormatConversionException(format.substring(specStart + 1));
            }
            char conversion = format.charAt(pos);
            pos++;

            if (conversion == '%' || conversion == 'n') {
                checkTextFlags(conversion, flags, width, precision);
                out.append(conversion == '%' ? pad("%", width, flags) : "\n");
                continue;
            }

            // format(fmt, (Object[]) null) is not an empty argument list: the JVM skips
            // the bounds checks and hands every specifier a null. "%<" is the exception
            // -- it reuses the previous argument, so there must have been one either way.
            // The JVM validates the specifier before it looks for an argument: "%q" with
            // no arguments is an unknown conversion, not a missing argument.
            boolean upper = conversion >= 'A' && conversion <= 'Z';
            char lower = validateSpecifier(conversion, upper, flags, width, precision);

            Object arg;
            if (previous) {
                if (lastArg < 0 || (args != null && lastArg >= args.length)) {
                    throw new MissingFormatArgumentException(format.substring(specStart, pos));
                }
                arg = args == null ? null : args[lastArg];
            } else if (argIndex > 0) {
                if (args != null && argIndex > args.length) {
                    throw new MissingFormatArgumentException(format.substring(specStart, pos));
                }
                lastArg = argIndex - 1;
                arg = args == null ? null : args[lastArg];
            } else {
                if (args != null && nextArg >= args.length) {
                    throw new MissingFormatArgumentException(format.substring(specStart, pos));
                }
                lastArg = nextArg;
                arg = args == null ? null : args[nextArg];
                nextArg++;
            }
            out.append(convert(conversion, lower, upper, arg, flags, width, precision));
        }
        return out.toString();
    }

    /**
     * Everything the JVM rejects before it selects an argument. Returns the lowercase
     * conversion so the caller does not recompute it.
     */
    private static char validateSpecifier(char conversion, boolean upper, int flags,
                                          int width, int precision) {
        if (upper && "SBHCXEG".indexOf(conversion) < 0) {
            // 'D' and 'O' have no uppercase form in java.util.Formatter.
            throw new UnknownFormatConversionException(String.valueOf(conversion));
        }
        char lower = upper ? (char) (conversion + ('a' - 'A')) : conversion;
        if ("sbhcdoxefg".indexOf(lower) < 0) {
            // %a (hexadecimal float) and %t (date and time) land here: unimplemented
            // rather than silently wrong. See the class javadoc.
            throw new UnknownFormatConversionException(String.valueOf(conversion));
        }
        checkFlags(conversion, lower, flags, width, precision);
        return lower;
    }

    private static String convert(char conversion, char lower, boolean upper, Object arg,
                                  int flags, int width, int precision) {
        switch (lower) {
            case 's':
                // '#' on a string is a print-time check on the JVM, so a missing argument
                // outranks it while a null argument does not.
                if ((flags & FLAG_HASH) != 0) {
                    throw new FormatFlagsConversionMismatchException("#", conversion);
                }
                return text(arg == null ? "null" : arg.toString(), upper, flags, width, precision);
            case 'b': {
                String value;
                if (arg == null) {
                    value = "false";
                } else if (arg instanceof Boolean) {
                    value = ((Boolean) arg).booleanValue() ? "true" : "false";
                } else {
                    value = "true";
                }
                return text(value, upper, flags, width, precision);
            }
            case 'h':
                return text(arg == null ? "null" : Integer.toHexString(arg.hashCode()),
                        upper, flags, width, precision);
            case 'c': {
                String value;
                if (arg == null) {
                    value = "null";
                } else if (arg instanceof Character) {
                    value = String.valueOf(((Character) arg).charValue());
                } else if (arg instanceof Integer || arg instanceof Short || arg instanceof Byte) {
                    int codePoint = ((Number) arg).intValue();
                    if (!Character.isValidCodePoint(codePoint)) {
                        throw new IllegalFormatCodePointException(codePoint);
                    }
                    value = new String(Character.toChars(codePoint));
                } else {
                    throw new IllegalFormatConversionException(conversion, arg.getClass());
                }
                return text(value, upper, flags, width, -1);
            }
            case 'd':
                return decimal(arg, conversion, upper, flags, width, precision);
            case 'o':
            case 'x':
                return radix(arg, conversion, lower == 'o' ? 3 : 4, upper, flags, width, precision);
            case 'e':
            case 'f':
            case 'g':
                return floatingPoint(arg, conversion, lower, upper, flags, width, precision);
            default:
                throw new UnknownFormatConversionException(String.valueOf(conversion));
        }
    }

    /**
     * {@code %%} takes at most a left-justification flag with a width, {@code %n} takes
     * nothing at all.
     */
    private static void checkTextFlags(char conversion, int flags, int width, int precision) {
        if (precision >= 0) {
            throw new IllegalFormatPrecisionException(precision);
        }
        if (conversion == 'n') {
            if (width >= 0) {
                throw new IllegalFormatWidthException(width);
            }
            if (flags != 0) {
                throw new IllegalFormatFlagsException(flagString(flags));
            }
            return;
        }
        if ((flags & ~FLAG_MINUS) != 0) {
            throw new IllegalFormatFlagsException(flagString(flags));
        }
        if (width < 0 && (flags & FLAG_MINUS) != 0) {
            throw new MissingFormatWidthException("%" + flagString(flags) + conversion);
        }
    }

    /**
     * Rejects the flag combinations that {@code java.util.Formatter} rejects, so that a
     * bogus format string fails the same way here as it does on the JVM.
     */
    private static void checkFlags(char conversion, char lower, int flags, int width, int precision) {
        if (lower == 's' || lower == 'b' || lower == 'h') {
            // '#' on a boolean or hash code is reported ahead of the width check; on a
            // string it is deferred to print time (see convert).
            if ((flags & FLAG_HASH) != 0 && lower != 's') {
                throw new FormatFlagsConversionMismatchException("#", conversion);
            }
            failMissingWidth(conversion, flags, width, FLAG_MINUS);
            failMismatch(conversion, flags,
                    FLAG_PLUS | FLAG_SPACE | FLAG_ZERO | FLAG_COMMA | FLAG_PAREN);
            return;
        }
        if (lower == 'c') {
            if (precision >= 0) {
                throw new IllegalFormatPrecisionException(precision);
            }
            // Unlike every other conversion, the JVM reports an unsupported flag on a
            // character ahead of the missing width: "%-0c" is a flag mismatch, not a
            // missing width, even though '-' has no width to justify against.
            failMismatch(conversion, flags,
                    FLAG_PLUS | FLAG_SPACE | FLAG_ZERO | FLAG_COMMA | FLAG_PAREN | FLAG_HASH);
            failMissingWidth(conversion, flags, width, FLAG_MINUS);
            return;
        }
        // Numeric conversions: zero padding is meaningful, so it needs a width too.
        failMissingWidth(conversion, flags, width, FLAG_MINUS | FLAG_ZERO);
        if ((flags & FLAG_PLUS) != 0 && (flags & FLAG_SPACE) != 0) {
            throw new IllegalFormatFlagsException(flagString(flags));
        }
        if ((flags & FLAG_MINUS) != 0 && (flags & FLAG_ZERO) != 0) {
            throw new IllegalFormatFlagsException(flagString(flags));
        }
        if (lower == 'd' || lower == 'o' || lower == 'x') {
            if (precision >= 0) {
                throw new IllegalFormatPrecisionException(precision);
            }
            failMismatch(conversion, flags, lower == 'd' ? FLAG_HASH : FLAG_COMMA);
        } else if (lower == 'e') {
            failMismatch(conversion, flags, FLAG_COMMA);
        } else if (lower == 'g') {
            failMismatch(conversion, flags, FLAG_HASH);
        }
    }

    private static void failMissingWidth(char conversion, int flags, int width, int needsWidth) {
        if (width < 0 && (flags & needsWidth) != 0) {
            throw new MissingFormatWidthException("%" + flagString(flags) + conversion);
        }
    }

    private static void failMismatch(char conversion, int flags, int illegal) {
        int mismatch = flags & illegal;
        if (mismatch != 0) {
            throw new FormatFlagsConversionMismatchException(flagString(mismatch), conversion);
        }
    }

    private static String flagString(int flags) {
        StringBuilder sb = new StringBuilder();
        if ((flags & FLAG_MINUS) != 0) {
            sb.append('-');
        }
        if ((flags & FLAG_HASH) != 0) {
            sb.append('#');
        }
        if ((flags & FLAG_PLUS) != 0) {
            sb.append('+');
        }
        if ((flags & FLAG_SPACE) != 0) {
            sb.append(' ');
        }
        if ((flags & FLAG_ZERO) != 0) {
            sb.append('0');
        }
        if ((flags & FLAG_COMMA) != 0) {
            sb.append(',');
        }
        if ((flags & FLAG_PAREN) != 0) {
            sb.append('(');
        }
        if ((flags & FLAG_PREVIOUS) != 0) {
            sb.append('<');
        }
        return sb.toString();
    }

    private static String text(String value, boolean upper, int flags, int width, int precision) {
        if (precision >= 0 && precision < value.length()) {
            value = value.substring(0, precision);
        }
        if (upper) {
            value = value.toUpperCase();
        }
        return pad(value, width, flags);
    }

    private static String decimal(Object arg, char conversion, boolean upper, int flags,
                                  int width, int precision) {
        if (arg == null) {
            return text("null", upper, flags, width, precision);
        }
        long value = longValue(arg, conversion);
        boolean negative = value < 0;
        // Long.MIN_VALUE has no positive counterpart, so strip the sign textually.
        String digits = negative ? Long.toString(value).substring(1) : Long.toString(value);
        return padNumeric(signPrefix(negative, flags), digits, signSuffix(negative, flags),
                flags, width);
    }

    private static String radix(Object arg, char conversion, int shift, boolean upper,
                                int flags, int width, int precision) {
        if (arg == null) {
            return text("null", upper, flags, width, precision);
        }
        long value;
        if (arg instanceof Long) {
            value = ((Long) arg).longValue();
        } else if (arg instanceof Integer) {
            value = ((Integer) arg).intValue() & 0xffffffffL;
        } else if (arg instanceof Short) {
            value = ((Short) arg).shortValue() & 0xffffL;
        } else if (arg instanceof Byte) {
            value = ((Byte) arg).byteValue() & 0xffL;
        } else {
            throw new IllegalFormatConversionException(conversion, arg.getClass());
        }
        // The JVM defers these to print time, after it has accepted the argument's type,
        // so both a null and a wrong-typed argument outrank them.
        int printTimeIllegal = flags & (FLAG_PAREN | FLAG_SPACE | FLAG_PLUS);
        if (printTimeIllegal != 0) {
            throw new FormatFlagsConversionMismatchException(flagString(printTimeIllegal), conversion);
        }
        String digits = unsigned(value, shift);
        String prefix = "";
        if ((flags & FLAG_HASH) != 0) {
            prefix = shift == 3 ? "0" : "0x";
        }
        if (upper) {
            digits = digits.toUpperCase();
            prefix = prefix.toUpperCase();
        }
        // Grouping is not defined for these conversions.
        return padNumeric(prefix, digits, "", flags & ~FLAG_COMMA, width);
    }

    private static String floatingPoint(Object arg, char conversion, char lower, boolean upper,
                                        int flags, int width, int precision) {
        if (arg == null) {
            return text("null", upper, flags, width, precision);
        }
        double value;
        if (arg instanceof Double) {
            value = ((Double) arg).doubleValue();
        } else if (arg instanceof Float) {
            // The JavaScript backend treats D2F as a no-op, so a float there is still an
            // unrounded double until it goes through the bit conversions. On every other
            // target this round trip is the identity.
            value = Float.intBitsToFloat(Float.floatToIntBits(((Float) arg).floatValue()));
        } else {
            throw new IllegalFormatConversionException(conversion, arg.getClass());
        }
        if (Double.isNaN(value)) {
            return pad(upper ? "NAN" : "NaN", width, flags & ~FLAG_ZERO);
        }
        boolean negative = value < 0 || (value == 0.0 && 1.0 / value < 0);
        if (Double.isInfinite(value)) {
            // Neither zero padding nor grouping applies to a non-numeric rendering.
            String body = upper ? "INFINITY" : "Infinity";
            return padNumeric(signPrefix(negative, flags), body, signSuffix(negative, flags),
                    flags & ~(FLAG_ZERO | FLAG_COMMA), width);
        }

        Decimal d = decompose(negative ? -value : value);
        String body;
        boolean scientificForm = lower == 'e';
        if (lower == 'f') {
            int scale = precision < 0 ? 6 : precision;
            roundTo(d, d.point + scale);
            body = fixed(d, scale, (flags & FLAG_HASH) != 0);
        } else if (lower == 'e') {
            int scale = precision < 0 ? 6 : precision;
            roundTo(d, scale + 1);
            body = scientific(d, scale, value == 0.0, upper, (flags & FLAG_HASH) != 0);
        } else {
            int significant = precision < 0 ? 6 : (precision == 0 ? 1 : precision);
            if (value == 0.0) {
                body = fixed(d, significant - 1, false);
            } else {
                roundTo(d, significant);
                if (d.point >= -3 && d.point <= significant) {
                    body = fixed(d, significant - d.point, false);
                } else {
                    body = scientific(d, significant - 1, false, upper, false);
                    scientificForm = true;
                }
            }
        }
        if (upper) {
            body = body.toUpperCase();
        }
        // The exponent must never be group-separated: "%,g" of 1e-5 is "1e-05".
        int effectiveFlags = scientificForm ? flags & ~FLAG_COMMA : flags;
        return padNumeric(signPrefix(negative, flags), body, signSuffix(negative, flags),
                effectiveFlags, width);
    }

    /**
     * Splits {@code Double.toString(abs)} into a significand and a decimal exponent.
     */
    private static Decimal decompose(double abs) {
        String s = Double.toString(abs);
        String mantissa = s;
        int exponent = 0;
        int e = s.indexOf('E');
        if (e < 0) {
            e = s.indexOf('e');
        }
        if (e >= 0) {
            mantissa = s.substring(0, e);
            exponent = Integer.parseInt(s.substring(e + 1));
        }
        int dot = mantissa.indexOf('.');
        String intPart = dot < 0 ? mantissa : mantissa.substring(0, dot);
        String fracPart = dot < 0 ? "" : mantissa.substring(dot + 1);
        String digits = intPart + fracPart;
        int point = intPart.length() + exponent;
        int start = 0;
        while (start < digits.length() - 1 && digits.charAt(start) == '0') {
            start++;
            point--;
        }
        digits = digits.substring(start);
        int end = digits.length();
        while (end > 1 && digits.charAt(end - 1) == '0') {
            end--;
        }
        digits = digits.substring(0, end);
        Decimal d = new Decimal();
        if (digits.equals("0")) {
            d.digits = "0";
            d.point = 0;
        } else {
            d.digits = digits;
            d.point = point;
        }
        return d;
    }

    /**
     * Rounds the significand to {@code keep} significant digits, HALF_UP. A {@code keep}
     * at or below zero still rounds: it decides whether the value survives at all.
     */
    private static void roundTo(Decimal d, int keep) {
        if (keep < 0) {
            keep = 0;
        }
        if (keep >= d.digits.length()) {
            return;
        }
        // The extra leading slot absorbs a carry out of the most significant digit.
        char[] buf = new char[keep + 1];
        buf[0] = '0';
        for (int i = 0; i < keep; i++) {
            buf[i + 1] = d.digits.charAt(i);
        }
        if (d.digits.charAt(keep) >= '5') {
            int i = keep;
            while (i >= 0) {
                if (buf[i] == '9') {
                    buf[i] = '0';
                    i--;
                } else {
                    buf[i] = (char) (buf[i] + 1);
                    break;
                }
            }
        }
        if (buf[0] != '0') {
            d.point++;
            d.digits = new String(buf);
        } else {
            d.digits = keep == 0 ? "0" : new String(buf, 1, keep);
        }
    }

    private static String fixed(Decimal d, int scale, boolean alternate) {
        StringBuilder sb = new StringBuilder();
        if (d.point <= 0) {
            sb.append('0');
        } else {
            for (int i = 0; i < d.point; i++) {
                sb.append(i < d.digits.length() ? d.digits.charAt(i) : '0');
            }
        }
        if (scale > 0 || alternate) {
            sb.append('.');
        }
        for (int i = 0; i < scale; i++) {
            int idx = d.point + i;
            sb.append(idx >= 0 && idx < d.digits.length() ? d.digits.charAt(idx) : '0');
        }
        return sb.toString();
    }

    private static String scientific(Decimal d, int scale, boolean zero, boolean upper, boolean alternate) {
        StringBuilder sb = new StringBuilder();
        sb.append(d.digits.charAt(0));
        if (scale > 0 || alternate) {
            sb.append('.');
        }
        for (int i = 1; i <= scale; i++) {
            sb.append(i < d.digits.length() ? d.digits.charAt(i) : '0');
        }
        sb.append(upper ? 'E' : 'e');
        int exponent = zero ? 0 : d.point - 1;
        sb.append(exponent < 0 ? '-' : '+');
        int magnitude = exponent < 0 ? -exponent : exponent;
        String exponentDigits = Integer.toString(magnitude);
        if (exponentDigits.length() < 2) {
            sb.append('0');
        }
        sb.append(exponentDigits);
        return sb.toString();
    }

    private static long longValue(Object arg, char conversion) {
        if (arg instanceof Long) {
            return ((Long) arg).longValue();
        }
        if (arg instanceof Integer) {
            return ((Integer) arg).intValue();
        }
        if (arg instanceof Short) {
            return ((Short) arg).shortValue();
        }
        if (arg instanceof Byte) {
            return ((Byte) arg).byteValue();
        }
        throw new IllegalFormatConversionException(conversion, arg.getClass());
    }

    private static String unsigned(long value, int shift) {
        if (value == 0) {
            return "0";
        }
        char[] buf = new char[64];
        int pos = buf.length;
        int mask = (1 << shift) - 1;
        long v = value;
        while (v != 0) {
            pos--;
            buf[pos] = DIGITS[(int) (v & mask)];
            v >>>= shift;
        }
        return new String(buf, pos, buf.length - pos);
    }

    private static String signPrefix(boolean negative, int flags) {
        if (negative) {
            return (flags & FLAG_PAREN) != 0 ? "(" : "-";
        }
        if ((flags & FLAG_PLUS) != 0) {
            return "+";
        }
        if ((flags & FLAG_SPACE) != 0) {
            return " ";
        }
        return "";
    }

    private static String signSuffix(boolean negative, int flags) {
        return negative && (flags & FLAG_PAREN) != 0 ? ")" : "";
    }

    private static String padNumeric(String prefix, String digits, String suffix, int flags, int width) {
        String body = (flags & FLAG_COMMA) != 0 ? group(digits) : digits;
        int fixedLength = prefix.length() + suffix.length();
        if (width > fixedLength + body.length()
                && (flags & FLAG_ZERO) != 0 && (flags & FLAG_MINUS) == 0) {
            // The padding zeros are not themselves grouped: "%,012d" of 1234 is
            // "00000001,234", not "0,000,001,234".
            StringBuilder padded = new StringBuilder();
            for (int i = fixedLength + body.length(); i < width; i++) {
                padded.append('0');
            }
            padded.append(body);
            body = padded.toString();
        }
        return pad(prefix + body + suffix, width, flags);
    }

    /**
     * Inserts grouping separators into the integer part of {@code value}, which may carry
     * a fractional tail that must be left alone.
     */
    private static String group(String value) {
        int dot = value.indexOf('.');
        String head = dot < 0 ? value : value.substring(0, dot);
        String tail = dot < 0 ? "" : value.substring(dot);
        if (head.length() <= 3) {
            return value;
        }
        StringBuilder sb = new StringBuilder();
        int first = head.length() % 3;
        if (first == 0) {
            first = 3;
        }
        sb.append(head.substring(0, first));
        for (int i = first; i < head.length(); i += 3) {
            sb.append(',');
            sb.append(head.substring(i, i + 3));
        }
        sb.append(tail);
        return sb.toString();
    }

    private static String pad(String value, int width, int flags) {
        if (width <= value.length()) {
            return value;
        }
        StringBuilder sb = new StringBuilder(width);
        if ((flags & FLAG_MINUS) != 0) {
            sb.append(value);
            while (sb.length() < width) {
                sb.append(' ');
            }
        } else {
            for (int i = value.length(); i < width; i++) {
                sb.append(' ');
            }
            sb.append(value);
        }
        return sb.toString();
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Parses a run of digits the way the JVM does, including its overflow behaviour: a
     * value too large for an int saturates to {@link Integer#MIN_VALUE}, and the caller
     * turns that negative result into the exception its field calls for. A large but
     * representable width is legal -- {@code "%1000001d"} really does produce a million
     * characters -- so there is deliberately no cap here.
     */
    private static int parseNumber(String value, int start, int end) {
        long result = 0;
        for (int i = start; i < end; i++) {
            result = result * 10 + (value.charAt(i) - '0');
            if (result > Integer.MAX_VALUE) {
                return Integer.MIN_VALUE;
            }
        }
        return (int) result;
    }
}
