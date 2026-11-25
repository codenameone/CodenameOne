/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

import java.util.ArrayList;

/// Port of rive-runtime's `StringTemplateFormat`
/// (`string_template_format.cpp`): parses a string template into literal and
/// placeholder segments, and formats a number for a printf-style spec. The
/// printf conversions are written out by hand, since the device has no
/// `String.format`.
public final class StringTemplateFormat {
    static final int LITERAL = 0;
    static final int NAME = 1;
    static final int POSITION = 2;
    static final int NEXT = 3;

    /// One parsed piece of a template (upstream `StringTemplateSegment`).
    static final class Segment {
        int kind = LITERAL;
        String text = "";
        String name = "";
        int index;
        String spec = "";
    }

    private StringTemplateFormat() {
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isNameStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private static boolean isNameChar(char c) {
        return isNameStart(c) || isDigit(c);
    }

    private static boolean startsWith(String text, int index, String prefix) {
        return index <= text.length() && text.length() - index >= prefix.length()
                && text.regionMatches(index, prefix, 0, prefix.length());
    }

    private static boolean isIntegerConversion(char c) {
        return c == 'd' || c == 'i' || c == 'u' || c == 'x' || c == 'X' || c == 'o';
    }

    private static boolean isFloatConversion(char c) {
        return c == 'f' || c == 'F' || c == 'e' || c == 'E' || c == 'g' || c == 'G';
    }

    public static boolean isValidDelimiter(String delimiter) {
        if (delimiter == null || delimiter.length() == 0 || delimiter.length() > 4) {
            return false;
        }
        for (int i = 0; i < delimiter.length(); i++) {
            char c = delimiter.charAt(i);
            boolean punctuation = (c >= '!' && c <= '/') || (c >= ';' && c <= '@')
                    || (c >= '[' && c <= '`') || (c >= '{' && c <= '~');
            if (!punctuation || c == '\\' || c == '_') {
                return false;
            }
        }
        return true;
    }

    private static void literal(ArrayList<Segment> segments, String text) {
        Segment s = new Segment();
        s.text = text;
        segments.add(s);
    }

    /// Splits `text` into `segments` (upstream `StringTemplateFormat::parse`).
    static void parse(String text, String open, String close, ArrayList<Segment> segments) {
        segments.clear();
        if (!isValidDelimiter(open) || !isValidDelimiter(close)) {
            if (text.length() > 0) {
                literal(segments, text);
            }
            return;
        }
        StringBuilder lit = new StringBuilder();
        int nextIndex = 0;
        int closeMissingFrom = -1;
        int size = text.length();
        int index = 0;
        while (index < size) {
            char c = text.charAt(index);
            if (c == '\\') {
                if (startsWith(text, index + 1, open)) {
                    lit.append(open);
                    index += 1 + open.length();
                    continue;
                }
                if (index + 1 < size && text.charAt(index + 1) == '\\') {
                    lit.append('\\');
                    index += 2;
                    continue;
                }
            }
            if (startsWith(text, index, open)) {
                int keyStart = index + open.length();
                int keyEnd = keyStart;
                int kind = NEXT;
                int position = 0;
                if (keyEnd < size && isNameStart(text.charAt(keyEnd))) {
                    kind = NAME;
                    while (keyEnd < size && isNameChar(text.charAt(keyEnd))) {
                        keyEnd++;
                    }
                } else if (keyEnd < size && isDigit(text.charAt(keyEnd))) {
                    kind = POSITION;
                    while (keyEnd < size && isDigit(text.charAt(keyEnd))) {
                        if (position < 100000000) {
                            position = position * 10 + (text.charAt(keyEnd) - '0');
                        }
                        keyEnd++;
                    }
                }
                int specStart = -1;
                int closeStart = -1;
                if (startsWith(text, keyEnd, close)) {
                    closeStart = keyEnd;
                } else if (keyEnd < size && text.charAt(keyEnd) == ':') {
                    specStart = keyEnd + 1;
                    if (closeMissingFrom == -1 || specStart < closeMissingFrom) {
                        closeStart = text.indexOf(close, specStart);
                        if (closeStart == -1) {
                            closeMissingFrom = specStart;
                        }
                    }
                }
                if (closeStart != -1) {
                    if (lit.length() > 0) {
                        literal(segments, lit.toString());
                        lit.setLength(0);
                    }
                    int end = closeStart + close.length();
                    Segment p = new Segment();
                    p.kind = kind;
                    p.text = text.substring(index, end);
                    if (kind == NAME) {
                        p.name = text.substring(keyStart, keyEnd);
                    } else if (kind == POSITION) {
                        p.index = position;
                    } else {
                        p.index = nextIndex++;
                    }
                    if (specStart != -1) {
                        p.spec = text.substring(specStart, closeStart);
                    }
                    segments.add(p);
                    index = end;
                    continue;
                }
            }
            lit.append(c);
            index++;
        }
        if (lit.length() > 0) {
            literal(segments, lit.toString());
        }
    }

    // ------------------------------------------------------------ numbers

    private static final class Spec {
        boolean leftAlign;
        boolean plus;
        boolean space;
        boolean zeroPad;
        boolean alternate;
        boolean thousands;
        int width = -1;
        int precision = -1;
        char conversion;
    }

    // Reads up to two digits into out[0]; false when a third digit follows.
    private static int readTwoDigits(String spec, int index, int[] out) {
        int start = index;
        int result = 0;
        while (index < spec.length() && isDigit(spec.charAt(index)) && index - start < 2) {
            result = result * 10 + (spec.charAt(index) - '0');
            index++;
        }
        if (index > start) {
            out[0] = result;
        }
        return index < spec.length() && isDigit(spec.charAt(index)) ? -1 : index;
    }

    private static boolean parseSpec(String spec, Spec out) {
        int index = 0;
        if (index < spec.length() && spec.charAt(index) == '%') {
            index++;
        }
        for (; index < spec.length(); index++) {
            char c = spec.charAt(index);
            if (c == '-') {
                out.leftAlign = true;
            } else if (c == '+') {
                out.plus = true;
            } else if (c == ' ') {
                out.space = true;
            } else if (c == '0') {
                out.zeroPad = true;
            } else if (c == '#') {
                out.alternate = true;
            } else if (c == ',') {
                out.thousands = true;
            } else {
                break;
            }
        }
        int[] v = {out.width};
        index = readTwoDigits(spec, index, v);
        if (index < 0) {
            return false;
        }
        out.width = v[0];
        if (index < spec.length() && spec.charAt(index) == '.') {
            index++;
            v[0] = 0;
            index = readTwoDigits(spec, index, v);
            if (index < 0) {
                return false;
            }
            out.precision = v[0];
        }
        if (index + 1 != spec.length()) {
            return false;
        }
        char conversion = spec.charAt(index);
        if (!isIntegerConversion(conversion) && !isFloatConversion(conversion)) {
            return false;
        }
        out.conversion = conversion;
        boolean decimal = conversion == 'd' || conversion == 'i' || conversion == 'u';
        if (out.alternate && decimal) {
            return false;
        }
        return !(out.thousands && (out.zeroPad || !(decimal || conversion == 'f' || conversion == 'F'
                || conversion == 'g' || conversion == 'G')));
    }

    public static boolean isValidNumberSpec(String spec) {
        return parseSpec(spec, new Spec());
    }

    /// `%f` with its zeros removed, the format of a placeholder with no spec.
    static String formatDefault(float value) {
        String out = DataConverterStringRemoveZeros.removeZeros(fixed(Math.abs((double) value), 6, false));
        if (value < 0 && !"0".equals(out)) {
            return "-" + out;
        }
        return out;
    }

    /// `value` formatted for the printf-style `spec`
    /// (upstream `StringTemplateFormat::formatNumber`).
    public static String formatNumber(float value, String spec) {
        if (Float.isNaN(value)) {
            return "nan";
        }
        if (Float.isInfinite(value)) {
            return value < 0 ? "-inf" : "inf";
        }
        Spec s = new Spec();
        if (spec == null || spec.length() == 0 || !parseSpec(spec, s)) {
            return formatDefault(value);
        }
        char conv = s.conversion;
        String sign = "";
        String prefix = "";
        String body;
        boolean zeroPadAllowed = s.zeroPad && !s.leftAlign;
        if (isIntegerConversion(conv)) {
            double limit = 9.0e18;
            double rounded = Math.floor((double) value + 0.5);
            if (value < 0 && rounded - value == 0.5) {
                rounded -= 1; // std::round rounds half away from zero
            }
            rounded = rounded > limit ? limit : (rounded < -limit ? -limit : rounded);
            long n = (long) rounded;
            if (conv == 'd' || conv == 'i') {
                sign = n < 0 ? "-" : s.plus ? "+" : s.space ? " " : "";
                body = Long.toString(Math.abs(n));
            } else {
                long u = n < 0 ? 0 : n;
                int radix = conv == 'o' ? 8 : conv == 'u' ? 10 : 16;
                body = Long.toString(u, radix);
                if (conv == 'X') {
                    StringBuilder up = new StringBuilder(body);
                    for (int i = 0; i < up.length(); i++) {
                        char ch = up.charAt(i);
                        if (ch >= 'a' && ch <= 'f') {
                            up.setCharAt(i, (char) (ch - 'a' + 'A'));
                        }
                    }
                    body = up.toString();
                }
                if (s.alternate && u != 0 && radix == 16) {
                    prefix = conv == 'X' ? "0X" : "0x";
                }
            }
            if (s.precision >= 0) {
                zeroPadAllowed = false;
                if (s.precision == 0 && "0".equals(body)) {
                    body = "";
                }
                body = zeros(s.precision - body.length()) + body;
            }
            if (conv == 'o' && s.alternate && !body.startsWith("0")) {
                body = "0" + body;
            }
        } else {
            double d = value;
            sign = d < 0 || (d == 0 && 1 / d < 0) ? "-" : s.plus ? "+" : s.space ? " " : "";
            body = formatFloat(Math.abs(d), conv, s.precision < 0 ? 6 : s.precision, s.alternate);
        }
        int width = s.width < 0 || s.thousands ? 0 : s.width;
        String out;
        int len = sign.length() + prefix.length() + body.length();
        if (len >= width) {
            out = sign + prefix + body;
        } else if (s.leftAlign) {
            out = sign + prefix + body + spaces(width - len);
        } else if (zeroPadAllowed) {
            out = sign + prefix + zeros(width - len) + body;
        } else {
            out = spaces(width - len) + sign + prefix + body;
        }
        if (s.thousands) {
            out = formatThousands(out);
            int w = s.width < 0 ? 0 : s.width;
            if (out.length() < w) {
                out = s.leftAlign ? out + spaces(w - out.length()) : spaces(w - out.length()) + out;
            }
        }
        return out;
    }

    private static String formatFloat(double v, char conv, int precision, boolean alternate) {
        boolean upper = conv == 'F' || conv == 'E' || conv == 'G';
        if (conv == 'f' || conv == 'F') {
            return fixed(v, precision, alternate);
        }
        if (conv == 'e' || conv == 'E') {
            return exponent(v, precision, alternate, upper);
        }
        int p = precision == 0 ? 1 : precision;
        int x = decimalExponent(v, p - 1);
        String out;
        if (p > x && x >= -4) {
            out = fixed(v, p - 1 - x, alternate);
        } else {
            out = exponent(v, p - 1, alternate, upper);
        }
        if (!alternate) {
            int e = out.indexOf(upper ? 'E' : 'e');
            String mant = e < 0 ? out : out.substring(0, e);
            String tail = e < 0 ? "" : out.substring(e);
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
            out = mant + tail;
        }
        return out;
    }

    // The decimal exponent of v once rounded to `digits` digits after the
    // leading one.
    private static int decimalExponent(double v, int digits) {
        if (v == 0) {
            return 0;
        }
        int x = 0;
        double m = v;
        while (m >= 10) {
            m /= 10;
            x++;
        }
        while (m < 1) {
            m *= 10;
            x--;
        }
        double scale = 1;
        for (int i = 0; i < digits && i < 17; i++) {
            scale *= 10;
        }
        if (Math.floor(m * scale + 0.5) / scale >= 10) {
            x++;
        }
        return x;
    }

    private static String exponent(double v, int precision, boolean alternate, boolean upper) {
        int x = decimalExponent(v, precision);
        double m = v;
        for (int i = 0; i < x; i++) {
            m /= 10;
        }
        for (int i = 0; i > x; i--) {
            m *= 10;
        }
        StringBuilder sb = new StringBuilder(fixed(m, precision, alternate));
        sb.append(upper ? 'E' : 'e').append(x < 0 ? '-' : '+');
        int ax = Math.abs(x);
        if (ax < 10) {
            sb.append('0');
        }
        sb.append(ax);
        return sb.toString();
    }

    /// `v` (not negative) with exactly `decimals` digits after the point,
    /// rounded half up.
    static String fixed(double v, int decimals, boolean alternate) {
        int d = decimals < 0 ? 0 : decimals;
        int exact = d > 15 ? 15 : d;
        double scale = 1;
        for (int i = 0; i < exact; i++) {
            scale *= 10;
        }
        double scaled = Math.floor(v * scale + 0.5);
        double whole = Math.floor(scaled / scale);
        double frac = scaled - whole * scale;
        StringBuilder sb = new StringBuilder();
        if (whole >= 9.0e18) {
            sb.append(Double.toString(whole));
        } else {
            sb.append((long) whole);
        }
        if (d > 0 || alternate) {
            sb.append('.');
        }
        if (exact > 0) {
            String f = Long.toString((long) frac);
            sb.append(zeros(exact - f.length())).append(f);
        }
        sb.append(zeros(d - exact));
        return sb.toString();
    }

    private static String zeros(int n) {
        return repeat('0', n);
    }

    private static String spaces(int n) {
        return repeat(' ', n);
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    /// Inserts a comma every three digits of the integer part.
    public static String formatThousands(String number) {
        int dot = number.indexOf('.');
        StringBuilder in = new StringBuilder(dot < 0 ? number : number.substring(0, dot));
        String frac = dot < 0 ? "" : number.substring(dot);
        int insert = in.length() - 3;
        while (insert > 0 && isDigit(in.charAt(insert - 1))) {
            in.insert(insert, ',');
            insert -= 3;
        }
        return in.append(frac).toString();
    }
}
