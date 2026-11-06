/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.python.runtime;

import com.codename1.util.BigInteger;

/// The format-spec mini-language (`format()`, f-strings, `str.format`) and
/// printf-style `%` formatting.
///
/// Float conversions are computed from the exact binary value of the double,
/// so `f"{2.675:.2f}"` gives `2.67` exactly as CPython does; nothing here
/// depends on `String.format`, which the device class library lacks.
final class Formatting {

    private Formatting() {
    }

    /// A parsed `[[fill]align][sign][z][#][0][width][grouping][.precision][type]`.
    static final class Spec {
        char fill = ' ';
        char align;
        char sign = '-';
        boolean alt;
        boolean zero;
        int width = -1;
        char grouping;
        int precision = -1;
        char type;

        static Spec parse(String s) {
            Spec p = new Spec();
            int i = 0;
            int n = s.length();
            if (n >= 2 && "<>=^".indexOf(s.charAt(1)) >= 0) {
                p.fill = s.charAt(0);
                p.align = s.charAt(1);
                i = 2;
            } else if (n >= 1 && "<>=^".indexOf(s.charAt(0)) >= 0) {
                p.align = s.charAt(0);
                i = 1;
            }
            if (i < n && "+- ".indexOf(s.charAt(i)) >= 0) {
                p.sign = s.charAt(i++);
            }
            if (i < n && s.charAt(i) == 'z') {
                i++;
            }
            if (i < n && s.charAt(i) == '#') {
                p.alt = true;
                i++;
            }
            if (i < n && s.charAt(i) == '0') {
                p.zero = true;
                i++;
            }
            int w = 0;
            boolean hasW = false;
            while (i < n && Character.isDigit(s.charAt(i))) {
                w = w * 10 + (s.charAt(i++) - '0');
                hasW = true;
            }
            if (hasW) {
                p.width = w;
            }
            if (i < n && (s.charAt(i) == ',' || s.charAt(i) == '_')) {
                p.grouping = s.charAt(i++);
            }
            if (i < n && s.charAt(i) == '.') {
                i++;
                int pr = 0;
                boolean hasP = false;
                while (i < n && Character.isDigit(s.charAt(i))) {
                    pr = pr * 10 + (s.charAt(i++) - '0');
                    hasP = true;
                }
                if (!hasP) {
                    throw Py.valueError("Format specifier missing precision");
                }
                p.precision = pr;
            }
            if (i < n) {
                p.type = s.charAt(i++);
            }
            if (i != n) {
                throw Py.valueError("Invalid format specifier '" + s + "'");
            }
            if (p.zero && p.align == 0) {
                p.fill = '0';
                p.align = '=';
            }
            return p;
        }
    }

    private static PyException unknownCode(Spec p, Object v) {
        return Py.valueError("Unknown format code '" + p.type + "' for object of type '" + Py.typeOf(v).name + "'");
    }

    static String format(Object v, String spec) {
        if (v instanceof PyInstance) {
            Object m = Py.typeOf(v).lookup("__format__");
            if (m != null) {
                return Py.strValue(Py.obj(m).call(new Object[] {v, Py.str(spec)}, Py.NO_KW), "__format__");
            }
            if (spec.length() == 0) {
                return Py.toStr(v);
            }
            if (((PyInstance) v).base != null) {
                // A subclass of a builtin formats as its value.
                v = ((PyInstance) v).base;
            }
        }
        if (spec.length() == 0) {
            return Py.toStr(v);
        }
        Spec p = Spec.parse(spec);
        if (Py.isInt(v) && !(v instanceof PyBool && p.type == 0)) {
            if (p.type == 0 || "dboxXcn".indexOf(p.type) >= 0) {
                return formatInt(Py.intView(v), p);
            }
            if ("eEfFgG%".indexOf(p.type) < 0) {
                throw unknownCode(p, v);
            }
            return formatFloat(Py.asDouble(v), p);
        }
        if (Py.isFloat(v)) {
            if (p.type != 0 && "eEfFgGn%".indexOf(p.type) < 0) {
                throw unknownCode(p, v);
            }
            return formatFloat(Py.asDouble(v), p);
        }
        String s = Py.toStr(v);
        if (p.type != 0 && p.type != 's') {
            throw unknownCode(p, v);
        }
        if (p.precision >= 0 && s.length() > p.precision) {
            s = s.substring(0, p.precision);
        }
        return pad("", s, p, '<');
    }

    private static String formatInt(PyInt v, Spec p) {
        BigInteger b = v.toBig();
        boolean neg = b.signum() < 0;
        BigInteger a = neg ? b.negate() : b;
        String digits;
        String prefix = "";
        switch (p.type) {
            case 'b':
                digits = a.toString(2);
                prefix = p.alt ? "0b" : "";
                break;
            case 'o':
                digits = a.toString(8);
                prefix = p.alt ? "0o" : "";
                break;
            case 'x':
                digits = a.toString(16);
                prefix = p.alt ? "0x" : "";
                break;
            case 'X':
                digits = upper(a.toString(16));
                prefix = p.alt ? "0X" : "";
                break;
            case 'c':
                return pad("", new String(Character.toChars((int) v.asLong())), p, '<');
            default:
                digits = a.toString();
                break;
        }
        if (p.grouping != 0) {
            digits = group(digits, p.grouping, p.type == 'b' || p.type == 'o' || p.type == 'x' || p.type == 'X' ? 4 : 3);
        }
        return pad(signOf(neg, p) + prefix, digits, p, '>');
    }

    static String upper(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
        }
        return sb.toString();
    }

    private static String signOf(boolean neg, Spec p) {
        if (neg) {
            return "-";
        }
        return p.sign == '+' ? "+" : (p.sign == ' ' ? " " : "");
    }

    private static String group(String digits, char sep, int every) {
        int dot = digits.indexOf('.');
        String intPart = dot >= 0 ? digits.substring(0, dot) : digits;
        String rest = dot >= 0 ? digits.substring(dot) : "";
        StringBuilder sb = new StringBuilder();
        int n = intPart.length();
        for (int i = 0; i < n; i++) {
            if (i > 0 && (n - i) % every == 0) {
                sb.append(sep);
            }
            sb.append(intPart.charAt(i));
        }
        return sb.append(rest).toString();
    }

    private static String pad(String sign, String body, Spec p, char defaultAlign) {
        int len = sign.length() + body.length();
        if (p.width <= len) {
            return sign + body;
        }
        int fillN = p.width - len;
        StringBuilder sb = new StringBuilder(p.width);
        char align = p.align == 0 ? defaultAlign : p.align;
        switch (align) {
            case '<':
                sb.append(sign).append(body);
                repeat(sb, p.fill, fillN);
                break;
            case '^':
                repeat(sb, p.fill, fillN / 2);
                sb.append(sign).append(body);
                repeat(sb, p.fill, fillN - fillN / 2);
                break;
            case '=':
                sb.append(sign);
                repeat(sb, p.fill, fillN);
                sb.append(body);
                break;
            default:
                repeat(sb, p.fill, fillN);
                sb.append(sign).append(body);
                break;
        }
        return sb.toString();
    }

    private static void repeat(StringBuilder sb, char c, int n) {
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
    }

    // ---- floats ----------------------------------------------------------

    private static String formatFloat(double d, Spec p) {
        boolean neg = d < 0 || d == 0 && 1 / d < 0;
        double a = Math.abs(d);
        String body;
        char type = p.type;
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            body = Double.isNaN(d) ? "nan" : "inf";
            if (type == 'F' || type == 'E' || type == 'G') {
                body = upper(body);
            }
            return pad(signOf(neg && !Double.isNaN(d), p), body, p, '>');
        }
        switch (type) {
            case 'f':
            case 'F':
                body = fixed(a, p.precision < 0 ? 6 : p.precision);
                if (p.alt && body.indexOf('.') < 0) {
                    body += ".";
                }
                break;
            case 'e':
            case 'E':
                body = exponent(a, p.precision < 0 ? 6 : p.precision, p.alt);
                if (type == 'E') {
                    body = upper(body);
                }
                break;
            case '%':
                body = fixed(a * 100, p.precision < 0 ? 6 : p.precision) + "%";
                break;
            case 'g':
            case 'G':
            case 'n':
                body = general(a, p.precision < 0 ? 6 : Math.max(1, p.precision), p.alt);
                if (type == 'G') {
                    body = upper(body);
                }
                break;
            default:
                if (p.precision >= 0) {
                    body = general(a, Math.max(1, p.precision), true);
                    body = trimGeneral(body, true);
                } else {
                    body = PyFloat.repr(a);
                }
                break;
        }
        if (p.grouping != 0) {
            int e = body.indexOf('e');
            body = e >= 0 ? group(body.substring(0, e), p.grouping, 3) + body.substring(e)
                    : group(body, p.grouping, 3);
        }
        return pad(signOf(neg, p), body, p, '>');
    }

    /// The exact decimal value of a non-negative finite double, as digits and
    /// the power of ten of the LAST digit.
    private static Object[] exact(double a) {
        if (a == 0) {
            return new Object[] {"0", Integer.valueOf(0)};
        }
        long bits = Double.doubleToLongBits(a);
        int exp = (int) ((bits >> 52) & 0x7ff);
        long mant = bits & 0xfffffffffffffL;
        if (exp == 0) {
            exp = 1;
        } else {
            mant |= 1L << 52;
        }
        exp -= 1075;
        BigInteger m = BigInteger.valueOf(mant);
        if (exp >= 0) {
            return new Object[] {m.shiftLeft(exp).toString(), Integer.valueOf(0)};
        }
        // m * 2^exp = m * 5^-exp / 10^-exp
        BigInteger scaled = m.multiply(BigInteger.valueOf(5).pow(-exp));
        return new Object[] {scaled.toString(), Integer.valueOf(exp)};
    }

    /// Rounds the digit string so its last digit has power `targetExp`
    /// (half to even on the exact value). Returns `{digits, lastExp}`.
    private static Object[] roundTo(String digits, int lastExp, int targetExp) {
        if (targetExp <= lastExp) {
            StringBuilder sb = new StringBuilder(digits);
            for (int i = targetExp; i < lastExp; i++) {
                sb.append('0');
            }
            return new Object[] {sb.toString(), Integer.valueOf(targetExp)};
        }
        int drop = targetExp - lastExp;
        if (drop > digits.length()) {
            return new Object[] {"0", Integer.valueOf(targetExp)};
        }
        String keep = digits.substring(0, digits.length() - drop);
        String gone = digits.substring(digits.length() - drop);
        boolean up;
        char first = gone.charAt(0);
        if (first > '5') {
            up = true;
        } else if (first < '5') {
            up = false;
        } else {
            boolean rest = false;
            for (int i = 1; i < gone.length(); i++) {
                if (gone.charAt(i) != '0') {
                    rest = true;
                    break;
                }
            }
            if (rest) {
                up = true;
            } else {
                int lastKeep = keep.length() == 0 ? 0 : keep.charAt(keep.length() - 1) - '0';
                up = (lastKeep & 1) == 1;
            }
        }
        if (keep.length() == 0) {
            keep = "0";
        }
        if (up) {
            keep = new BigInteger(keep).add(BigInteger.ONE).toString();
        }
        return new Object[] {keep, Integer.valueOf(targetExp)};
    }

    static String fixed(double a, int prec) {
        Object[] ex = exact(a);
        Object[] r = roundTo((String) ex[0], ((Integer) ex[1]).intValue(), -prec);
        String digits = (String) r[0];
        if (prec == 0) {
            return digits;
        }
        while (digits.length() <= prec) {
            digits = "0" + digits;
        }
        return digits.substring(0, digits.length() - prec) + "." + digits.substring(digits.length() - prec);
    }

    /// `{mantissaDigits (prec+1 of them), decimalExponent}` for `e` formatting.
    private static Object[] sci(double a, int prec) {
        if (a == 0) {
            StringBuilder z = new StringBuilder("0");
            for (int i = 0; i < prec; i++) {
                z.append('0');
            }
            return new Object[] {z.toString(), Integer.valueOf(0)};
        }
        Object[] ex = exact(a);
        String digits = (String) ex[0];
        int lastExp = ((Integer) ex[1]).intValue();
        int sigLen = digits.length();
        int lead = 0;
        while (lead < sigLen - 1 && digits.charAt(lead) == '0') {
            lead++;
        }
        digits = digits.substring(lead);
        int topExp = lastExp + digits.length() - 1;
        Object[] r = roundTo(digits, lastExp, topExp - prec);
        String d = (String) r[0];
        if (d.length() > prec + 1) {
            topExp++;
            d = d.substring(0, prec + 1);
        }
        return new Object[] {d, Integer.valueOf(topExp)};
    }

    static String exponent(double a, int prec, boolean alt) {
        Object[] s = sci(a, prec);
        String d = (String) s[0];
        int e = ((Integer) s[1]).intValue();
        StringBuilder sb = new StringBuilder();
        sb.append(d.charAt(0));
        if (prec > 0 || alt) {
            sb.append('.');
        }
        sb.append(d, 1, d.length());
        sb.append('e').append(e < 0 ? '-' : '+');
        int ae = Math.abs(e);
        if (ae < 10) {
            sb.append('0');
        }
        return sb.append(ae).toString();
    }

    static String general(double a, int prec, boolean alt) {
        int e;
        if (a == 0) {
            e = 0;
        } else {
            e = ((Integer) sci(a, prec - 1)[1]).intValue();
        }
        String body;
        if (e >= -4 && e < prec) {
            body = fixed(a, prec - 1 - e);
        } else {
            body = exponent(a, prec - 1, alt);
        }
        return trimGeneral(body, alt);
    }

    private static String trimGeneral(String body, boolean alt) {
        if (alt) {
            return body;
        }
        int e = body.indexOf('e');
        String mant = e >= 0 ? body.substring(0, e) : body;
        String tail = e >= 0 ? body.substring(e) : "";
        if (mant.indexOf('.') >= 0) {
            int end = mant.length();
            while (mant.charAt(end - 1) == '0') {
                end--;
            }
            if (mant.charAt(end - 1) == '.') {
                end--;
            }
            mant = mant.substring(0, end);
        }
        return mant + tail;
    }

    // ---- printf style ----------------------------------------------------

    static String percent(String fmt, Object args) {
        Object[] items;
        PyDict mapping = null;
        if (args instanceof PyTuple) {
            items = ((PyTuple) args).items;
        } else {
            items = new Object[] {args};
            if (args instanceof PyDict) {
                mapping = (PyDict) args;
            }
        }
        StringBuilder sb = new StringBuilder();
        int ai = 0;
        int n = fmt.length();
        for (int i = 0; i < n; i++) {
            char c = fmt.charAt(i);
            if (c != '%') {
                sb.append(c);
                continue;
            }
            i++;
            if (i >= n) {
                throw Py.valueError("incomplete format");
            }
            Object value = null;
            if (fmt.charAt(i) == '(') {
                int close = fmt.indexOf(')', i);
                String key = fmt.substring(i + 1, close);
                if (mapping == null) {
                    throw Py.typeError("format requires a mapping");
                }
                value = mapping.getItem(Py.str(key));
                i = close + 1;
            }
            StringBuilder spec = new StringBuilder();
            boolean left = false;
            while (i < n && "-+ #0".indexOf(fmt.charAt(i)) >= 0) {
                char f = fmt.charAt(i++);
                if (f == '-') {
                    left = true;
                } else {
                    spec.append(f == '0' && left ? "" : String.valueOf(f));
                }
            }
            StringBuilder width = new StringBuilder();
            while (i < n && (Character.isDigit(fmt.charAt(i)) || fmt.charAt(i) == '*')) {
                if (fmt.charAt(i) == '*') {
                    width.append(Py.asLong(items[ai++]));
                    i++;
                } else {
                    width.append(fmt.charAt(i++));
                }
            }
            String prec = "";
            if (i < n && fmt.charAt(i) == '.') {
                i++;
                StringBuilder pr = new StringBuilder(".");
                if (i < n && fmt.charAt(i) == '*') {
                    // `%.*f`: the precision is the next argument.
                    pr.append(Math.max(0, Py.asLong(items[ai++])));
                    i++;
                }
                while (i < n && Character.isDigit(fmt.charAt(i))) {
                    pr.append(fmt.charAt(i++));
                }
                if (pr.length() == 1) {
                    pr.append('0');
                }
                prec = pr.toString();
            }
            char conv = fmt.charAt(i);
            if (conv == '%') {
                sb.append('%');
                continue;
            }
            if (value == null) {
                if (ai >= items.length) {
                    throw Py.typeError("not enough arguments for format string");
                }
                value = items[ai++];
            }
            if (conv != 's' && conv != 'r' && conv != 'a' && value instanceof PyInstance
                    && ((PyInstance) value).base != null) {
                // A number conversion formats a subclass's value (an IntEnum).
                value = ((PyInstance) value).base;
            }
            String flags = spec.toString();
            String align = left ? "<" : "";
            // printf-style padding right-justifies text too, unlike format().
            String textAlign = left ? "<" : ">";
            switch (conv) {
                case 's':
                    sb.append(format(Py.str(Py.toStr(value)), textAlign + width + prec));
                    break;
                case 'r':
                case 'a':
                    sb.append(format(Py.str(Py.toRepr(value)), textAlign + width + prec));
                    break;
                case 'd':
                case 'i':
                case 'u': {
                    Object iv = Py.isFloat(value) ? PyInt.of((long) Py.asDouble(value)) : value;
                    if (!(Py.isInt(iv))) {
                        throw Py.typeError("%" + conv + " format: a real number is required, not " + Py.typeOf(value).name);
                    }
                    sb.append(format(iv, align + flags.replace("#", "") + width + "d"));
                    break;
                }
                case 'c':
                    sb.append(value instanceof String ? Py.toStr(value) : new String(Character.toChars((int) Py.asLong(value))));
                    break;
                default:
                    if ("xXo".indexOf(conv) >= 0) {
                        sb.append(format(value, align + flags + width + conv));
                    } else if ("eEfFgG".indexOf(conv) >= 0) {
                        sb.append(format(Py.ofFloat(Py.asDouble(value)), align + flags + width + prec + conv));
                    } else {
                        throw Py.valueError("unsupported format character '" + conv + "'");
                    }
                    break;
            }
        }
        if (ai < items.length && mapping == null) {
            throw Py.typeError("not all arguments converted during string formatting");
        }
        return sb.toString();
    }
}
