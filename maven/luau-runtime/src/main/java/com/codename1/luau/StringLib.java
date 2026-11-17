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
package com.codename1.luau;



/// The `string` library, also reachable as methods on every string.
/// Case mapping is ASCII only: Luau strings are bytes, and folding by the
/// device locale would turn a Turkish `I` into a dotless i.
final class StringLib {
    private StringLib() {
    }

    private static final Object[] NONE = new Object[0];

    /// Longest string `string.rep` may build: 16 M characters. Scripts run
    /// sandboxed on a shared server, where 100 M (up to 200 MB of chars) per
    /// call was enough for a few calls to exhaust the heap.
    static final long MAX_STRING = 16L * 1024 * 1024;

    private static Object[] ret(Object v) {
        return new Object[] {v};
    }

    private static String str(Object[] a, int i, String fn) {
        return Stdlib.string(a, i, fn);
    }

    /// Converts a 1-based, possibly negative Luau index to a 0-based start.
    private static int startIndex(int i, int len) {
        if (i < 0) {
            i = len + i + 1;
        }
        if (i < 1) {
            i = 1;
        }
        return i - 1;
    }

    static void install(Interp in, LuaTable g) {
        LuaTable s = new LuaTable();
        g.rawset("string", s);
        LuaTable meta = new LuaTable();
        meta.rawset("__index", s);
        in.setStringMeta(meta);
        Stdlib.reg(s, new JavaFunction("string.len") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return ret(Interp.num(str(a, 0, "len").length()));
            }
        });
        Stdlib.reg(s, new JavaFunction("string.sub") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                String x = str(a, 0, "sub");
                int len = x.length();
                int from = Stdlib.optInteger(a, 1, "sub", 1);
                int to = Stdlib.optInteger(a, 2, "sub", -1);
                if (from < 0) {
                    from = Math.max(len + from + 1, 1);
                } else if (from == 0) {
                    from = 1;
                }
                if (to < 0) {
                    to = len + to + 1;
                } else if (to > len) {
                    to = len;
                }
                if (from > to) {
                    return ret("");
                }
                return ret(x.substring(from - 1, to));
            }
        });
        Stdlib.reg(s, new JavaFunction("string.upper") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                String x = str(a, 0, "upper");
                StringBuilder b = new StringBuilder(x.length());
                for (int k = 0; k < x.length(); k++) {
                    char c = x.charAt(k);
                    b.append(c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
                }
                return ret(b.toString());
            }
        });
        Stdlib.reg(s, new JavaFunction("string.lower") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                String x = str(a, 0, "lower");
                StringBuilder b = new StringBuilder(x.length());
                for (int k = 0; k < x.length(); k++) {
                    char c = x.charAt(k);
                    b.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
                }
                return ret(b.toString());
            }
        });
        Stdlib.reg(s, new JavaFunction("string.rep") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                String x = str(a, 0, "rep");
                int n = Stdlib.integer(a, 1, "rep");
                String sep = a.length > 2 && a[2] != null ? str(a, 2, "rep") : "";
                long total = n <= 0 ? 0 : (long) x.length() * n + (long) sep.length() * (n - 1);
                if (total == 0) {
                    // Also covers "" with "" for any n: nothing to build, so
                    // never spin through n empty appends.
                    return ret("");
                }
                if (total > MAX_STRING) {
                    throw LuaError.rt("string length overflow");
                }
                // One step per 64 characters produced, so a repeat costs the
                // budget what the equivalent Lua loop would.
                i.charge(n + total / 64);
                StringBuilder b = new StringBuilder((int) total);
                for (int k = 0; k < n; k++) {
                    if (k > 0) {
                        b.append(sep);
                    }
                    b.append(x);
                }
                return ret(b.toString());
            }
        });
        Stdlib.reg(s, new JavaFunction("string.reverse") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return ret(new StringBuilder(str(a, 0, "reverse")).reverse().toString());
            }
        });
        Stdlib.reg(s, new JavaFunction("string.byte") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                String x = str(a, 0, "byte");
                int from = Stdlib.optInteger(a, 1, "byte", 1);
                int to = Stdlib.optInteger(a, 2, "byte", from);
                int len = x.length();
                if (from < 0) {
                    from = len + from + 1;
                }
                if (to < 0) {
                    to = len + to + 1;
                }
                from = Math.max(from, 1);
                to = Math.min(to, len);
                if (from > to) {
                    return NONE;
                }
                Object[] r = new Object[to - from + 1];
                for (int k = from; k <= to; k++) {
                    r[k - from] = Interp.num(x.charAt(k - 1) & 0xFF);
                }
                return r;
            }
        });
        Stdlib.reg(s, new JavaFunction("string.char") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                StringBuilder b = new StringBuilder(a.length);
                for (int k = 0; k < a.length; k++) {
                    int c = Stdlib.integer(a, k, "char");
                    if (c < 0 || c > 255) {
                        throw Stdlib.badArg(k, "char", "value out of range");
                    }
                    b.append((char) c);
                }
                return ret(b.toString());
            }
        });
        Stdlib.reg(s, new JavaFunction("string.format") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return ret(format(i, a));
            }
        });
        Stdlib.reg(s, new JavaFunction("string.find") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return find(i, a, true);
            }
        });
        Stdlib.reg(s, new JavaFunction("string.match") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return find(i, a, false);
            }
        });
        Stdlib.reg(s, new JavaFunction("string.gmatch") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                final String x = str(a, 0, "gmatch");
                final String p = str(a, 1, "gmatch");
                final int[] pos = {0};
                final int[] lastEnd = {-1};
                return ret(new JavaFunction("gmatch_iter") {
                    @Override
                    public Object[] call(Interp i2, Object[] a2) {
                        boolean anchor = p.startsWith("^");
                        int pi = anchor ? 1 : 0;
                        while (pos[0] <= x.length()) {
                            LuaPattern m = new LuaPattern(i2, x, p);
                            int e = m.match(pos[0], pi);
                            if (e >= 0 && e != lastEnd[0]) {
                                int st = pos[0];
                                pos[0] = e > st ? e : st + 1;
                                lastEnd[0] = e;
                                return m.captures(st, e, true);
                            }
                            if (anchor) {
                                break;
                            }
                            pos[0]++;
                        }
                        pos[0] = x.length() + 1;
                        return ret(null);
                    }
                });
            }
        });
        Stdlib.reg(s, new JavaFunction("string.gsub") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                return gsub(i, a);
            }
        });
        Stdlib.reg(s, new JavaFunction("string.split") {
            @Override
            public Object[] call(Interp i, Object[] a) {
                String x = str(a, 0, "split");
                String sep = a.length > 1 && a[1] != null ? str(a, 1, "split") : ",";
                LuaTable t = new LuaTable();
                int n = 1;
                if (sep.length() == 0) {
                    for (int k = 0; k < x.length(); k++) {
                        t.rawseti(n++, x.substring(k, k + 1));
                    }
                    return ret(t);
                }
                int start = 0;
                while (true) {
                    int idx = x.indexOf(sep, start);
                    if (idx < 0) {
                        t.rawseti(n, x.substring(start));
                        return ret(t);
                    }
                    t.rawseti(n++, x.substring(start, idx));
                    start = idx + sep.length();
                }
            }
        });
    }

    private static boolean noSpecials(String p) {
        for (int i = 0; i < p.length(); i++) {
            if ("^$*+?.([%-".indexOf(p.charAt(i)) >= 0) {
                return false;
            }
        }
        return true;
    }

    static Object[] find(Interp in, Object[] a, boolean find) {
        String fn = find ? "find" : "match";
        String x = str(a, 0, fn);
        String p = str(a, 1, fn);
        int init = startIndex(Stdlib.optInteger(a, 2, fn, 1), x.length());
        if (init > x.length()) {
            return ret(null);
        }
        boolean plain = find && a.length > 3 && Interp.truthy(a[3]);
        if (find && (plain || noSpecials(p))) {
            int idx = x.indexOf(p, init);
            if (idx < 0) {
                return ret(null);
            }
            return new Object[] {Interp.num(idx + 1), Interp.num(idx + p.length())};
        }
        boolean anchor = p.startsWith("^");
        int pi = anchor ? 1 : 0;
        int st = init;
        do {
            LuaPattern m = new LuaPattern(in, x, p);
            int e = m.match(st, pi);
            if (e >= 0) {
                if (find) {
                    Object[] caps = m.captures(st, e, false);
                    Object[] r = new Object[caps.length + 2];
                    r[0] = Interp.num(st + 1);
                    r[1] = Interp.num(e);
                    System.arraycopy(caps, 0, r, 2, caps.length);
                    return r;
                }
                return m.captures(st, e, true);
            }
            st++;
        } while (st <= x.length() && !anchor);
        return ret(null);
    }

    static Object[] gsub(Interp in, Object[] a) {
        String x = str(a, 0, "gsub");
        String p = str(a, 1, "gsub");
        Object repl = Stdlib.arg(a, 2);
        if (!(repl instanceof String || repl instanceof Double || repl instanceof LuaTable || repl instanceof LuaFunction)) {
            throw Stdlib.badArg(2, "gsub", "string/function/table expected");
        }
        int max = a.length > 3 && a[3] != null ? Stdlib.integer(a, 3, "gsub") : Integer.MAX_VALUE;
        boolean anchor = p.startsWith("^");
        int pi = anchor ? 1 : 0;
        StringBuilder b = new StringBuilder();
        int s = 0;
        int n = 0;
        while (n < max) {
            LuaPattern m = new LuaPattern(in, x, p);
            int e = m.match(s, pi);
            if (e >= 0) {
                n++;
                Object whole = x.substring(s, e);
                Object val;
                if (repl instanceof String || repl instanceof Double) {
                    String r = Interp.rawToString(repl);
                    StringBuilder rb = new StringBuilder();
                    for (int k = 0; k < r.length(); k++) {
                        char c = r.charAt(k);
                        if (c == '%' && k + 1 < r.length()) {
                            char d = r.charAt(++k);
                            if (d == '%') {
                                rb.append('%');
                            } else if (d >= '0' && d <= '9') {
                                Object cv = d == '0' ? whole : m.capture(d - '1', s, e);
                                rb.append(Interp.rawToString(cv));
                            } else {
                                throw LuaError.rt("invalid use of '%' in replacement string");
                            }
                        } else {
                            rb.append(c);
                        }
                    }
                    val = rb.toString();
                } else {
                    Object key = m.capture(0, s, e);
                    if (repl instanceof LuaTable) {
                        val = in.index(repl, key);
                    } else {
                        val = in.call1(repl, (Object[]) m.captures(s, e, true));
                    }
                    if (!Interp.truthy(val)) {
                        val = whole;
                    } else if (!(val instanceof String) && !(val instanceof Double)) {
                        throw LuaError.rt("invalid replacement value (a " + Interp.typeName(val) + ")");
                    }
                    val = Interp.rawToString(val);
                }
                b.append((String) val);
            }
            if (e >= 0 && e > s) {
                s = e;
            } else {
                if (s < x.length()) {
                    b.append(x.charAt(s));
                }
                s++;
            }
            if (s > x.length() || anchor) {
                break;
            }
        }
        if (s < x.length()) {
            b.append(x.substring(s));
        }
        return new Object[] {b.toString(), Interp.num(n)};
    }

    // ---------------------------------------------------------------- format

    static String format(Interp in, Object[] a) {
        String f = str(a, 0, "format");
        StringBuilder b = new StringBuilder();
        int arg = 1;
        for (int i = 0; i < f.length(); i++) {
            char c = f.charAt(i);
            if (c != '%') {
                b.append(c);
                continue;
            }
            if (++i >= f.length()) { // NOPMD AssignmentInOperand - format scan loop
                throw LuaError.rt("invalid option '%' to 'format'");
            }
            if (f.charAt(i) == '%') {
                b.append('%');
                continue;
            }
            StringBuilder flags = new StringBuilder();
            while (i < f.length() && "-+ #0".indexOf(f.charAt(i)) >= 0) {
                flags.append(f.charAt(i++));
            }
            // Lua allows at most two digits of width and of precision (99);
            // a third digit is "invalid conversion". Unbounded, "%999999999d"
            // padded to a gigabyte and a longer run overflowed int.
            int width = 0;
            int digits = 0;
            while (i < f.length() && Character.isDigit(f.charAt(i))) {
                if (++digits > 2) { // NOPMD AssignmentInOperand - format scan loop
                    throw LuaError.rt("invalid conversion to 'format'");
                }
                width = width * 10 + (f.charAt(i++) - '0');
            }
            int prec = -1;
            if (i < f.length() && f.charAt(i) == '.') {
                i++;
                prec = 0;
                digits = 0;
                while (i < f.length() && Character.isDigit(f.charAt(i))) {
                    if (++digits > 2) { // NOPMD AssignmentInOperand - format scan loop
                        throw LuaError.rt("invalid conversion to 'format'");
                    }
                    prec = prec * 10 + (f.charAt(i++) - '0');
                }
            }
            if (i >= f.length()) {
                throw LuaError.rt("invalid conversion to 'format'");
            }
            char conv = f.charAt(i);
            String fl = flags.toString();
            String body;
            switch (conv) {
                case 'd':
                case 'i': {
                    double d = Stdlib.number(a, arg++, "format");
                    if (d != Math.floor(d)) {
                        throw Stdlib.badArg(arg - 1, "format", "number has no integer representation");
                    }
                    long v = (long) d;
                    body = Long.toString(Math.abs(v));
                    if (prec >= 0) {
                        body = zeroPad(body, prec);
                    }
                    body = sign(v < 0, fl) + body;
                    break;
                }
                case 'u': {
                    long v = (long) Stdlib.number(a, arg++, "format");
                    body = Long.toString(v);
                    break;
                }
                case 'c':
                    body = String.valueOf((char) (Stdlib.integer(a, arg++, "format") & 0xFF));
                    break;
                case 'x':
                case 'X':
                case 'o': {
                    long v = (long) Stdlib.number(a, arg++, "format");
                    body = Long.toString(v & 0xFFFFFFFFFFFFFFFFL, conv == 'o' ? 8 : 16);
                    if (v < 0) {
                        body = conv == 'o' ? Long.toOctalString(v) : Long.toHexString(v);
                    }
                    if (conv == 'X') {
                        body = asciiUpper(body);
                    }
                    if (prec >= 0) {
                        body = zeroPad(body, prec);
                    }
                    if (fl.indexOf('#') >= 0 && v != 0) {
                        body = (conv == 'o' ? "0" : conv == 'x' ? "0x" : "0X") + body;
                    }
                    break;
                }
                case 'f':
                case 'F':
                case 'e':
                case 'E':
                case 'g':
                case 'G': {
                    double d = Stdlib.number(a, arg++, "format");
                    body = formatFloat(d, conv, prec < 0 ? 6 : prec, fl);
                    break;
                }
                case 'a':
                case 'A':
                    body = Double.toString(Stdlib.number(a, arg++, "format"));
                    break;
                case 'q':
                    body = quote(Stdlib.arg(a, arg++));
                    break;
                case 's': {
                    Object v = Stdlib.arg(a, arg);
                    if (arg >= a.length) {
                        throw Stdlib.badArg(arg, "format", "string expected, got no value");
                    }
                    arg++;
                    body = in.tostring(v);
                    if (prec >= 0 && body.length() > prec) {
                        body = body.substring(0, prec);
                    }
                    break;
                }
                case '*': {
                    body = in.tostring(Stdlib.arg(a, arg++));
                    break;
                }
                default:
                    throw LuaError.rt("invalid option '%" + conv + "' to 'format'");
            }
            if (body.length() < width) {
                boolean left = fl.indexOf('-') >= 0;
                boolean zero = fl.indexOf('0') >= 0 && !left && "dioxXeEfFgG".indexOf(conv) >= 0;
                StringBuilder pad = new StringBuilder();
                for (int k = body.length(); k < width; k++) {
                    pad.append(zero ? '0' : ' ');
                }
                if (left) {
                    body = body + pad;
                } else if (zero && body.length() > 0 && (body.charAt(0) == '-' || body.charAt(0) == '+'
                        || body.charAt(0) == ' ')) {
                    body = body.charAt(0) + pad.toString() + body.substring(1);
                } else {
                    body = pad + body;
                }
            }
            // "%s%s%s..." over one long argument repeats it per conversion.
            in.chargeString((long) b.length() + body.length());
            b.append(body);
        }
        return b.toString();
    }

    private static String asciiUpper(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            b.append(c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
        }
        return b.toString();
    }

    private static String sign(boolean neg, String fl) {
        if (neg) {
            return "-";
        }
        if (fl.indexOf('+') >= 0) {
            return "+";
        }
        if (fl.indexOf(' ') >= 0) {
            return " ";
        }
        return "";
    }

    private static String zeroPad(String s, int n) {
        StringBuilder b = new StringBuilder();
        for (int k = s.length(); k < n; k++) {
            b.append('0');
        }
        return b.append(s).toString();
    }

    /// Formats `d` with `%f`, `%e` or `%g` semantics.
    static String formatFloat(double d, char conv, int prec, String fl) {
        boolean neg = d < 0 || d == 0 && 1 / d < 0;
        double v = Math.abs(d);
        String body;
        boolean upper = conv >= 'A' && conv <= 'Z';
        if (Double.isNaN(d)) {
            body = upper ? "NAN" : "nan";
            neg = false;
        } else if (Double.isInfinite(d)) {
            body = upper ? "INF" : "inf";
        } else {
            char lc = upper ? (char) (conv + 32) : conv;
            if (lc == 'f') {
                body = fixed(v, prec);
            } else if (lc == 'e') {
                body = exp(v, prec, upper);
            } else {
                int p = prec == 0 ? 1 : prec;
                int x = v == 0 ? 0 : exponentOf(v, p);
                if (x < -4 || x >= p) {
                    body = exp(v, p - 1, upper);
                    if (fl.indexOf('#') < 0) {
                        int e = body.indexOf(upper ? 'E' : 'e');
                        body = stripZeros(body.substring(0, e)) + body.substring(e);
                    }
                } else {
                    body = fixed(v, p - 1 - x);
                    if (fl.indexOf('#') < 0) {
                        body = stripZeros(body);
                    }
                }
            }
        }
        return sign(neg, fl) + body;
    }

    private static String stripZeros(String s) {
        if (s.indexOf('.') < 0) {
            return s;
        }
        int e = s.length();
        while (e > 0 && s.charAt(e - 1) == '0') {
            e--;
        }
        if (e > 0 && s.charAt(e - 1) == '.') {
            e--;
        }
        return s.substring(0, e);
    }

    /// The decimal exponent of `v` once rounded to `p` significant digits.
    private static int exponentOf(double v, int p) {
        String e = exp(v, p - 1, false);
        int i = e.indexOf('e');
        return Integer.parseInt(e.substring(i + 1).replace("+", ""));
    }

    /// Exact decimal digits of a non-negative double, rounded half-even at
    /// `frac` places, using the shortest representation Java prints.
    static String fixed(double v, int frac) {
        String digits = decimalDigits(v);
        int point = digits.indexOf('.');
        String ip = digits.substring(0, point);
        String fp = digits.substring(point + 1);
        StringBuilder all = new StringBuilder(ip).append(fp);
        int keep = ip.length() + frac;
        while (all.length() < keep + 1) {
            all.append('0');
        }
        boolean roundUp = all.charAt(keep) > '5' || all.charAt(keep) == '5' && (hasNonZero(all, keep + 1)
                || keep > 0 && (all.charAt(keep - 1) - '0') % 2 != 0);
        StringBuilder r = new StringBuilder();
        r.append(all.substring(0, keep));
        if (roundUp) {
            int k = r.length() - 1;
            while (k >= 0) {
                char c = r.charAt(k);
                if (c == '9') {
                    r.setCharAt(k, '0');
                    k--;
                } else {
                    r.setCharAt(k, (char) (c + 1));
                    break;
                }
            }
            if (k < 0) {
                r.insert(0, '1');
            }
        }
        int ipLen = r.length() - frac;
        String ipart = ipLen <= 0 ? "0" : r.substring(0, ipLen);
        while (ipart.length() > 1 && ipart.charAt(0) == '0') {
            ipart = ipart.substring(1);
        }
        if (frac == 0) {
            return ipart;
        }
        String fpart = r.substring(Math.max(0, ipLen));
        while (fpart.length() < frac) {
            fpart = "0" + fpart;
        }
        return ipart + "." + fpart;
    }

    private static boolean hasNonZero(StringBuilder b, int from) {
        for (int i = from; i < b.length(); i++) {
            if (b.charAt(i) != '0') {
                return true;
            }
        }
        return false;
    }

    /// Plain decimal form of `v` (no exponent), e.g. 1.5E-7 -> 0.00000015.
    private static String decimalDigits(double v) {
        String s = Double.toString(v);
        int e = s.indexOf('E');
        if (e < 0) {
            return s.indexOf('.') < 0 ? s + ".0" : s;
        }
        String mant = s.substring(0, e);
        int exp = Integer.parseInt(s.substring(e + 1));
        int dot = mant.indexOf('.');
        String digits = mant.substring(0, dot) + mant.substring(dot + 1);
        int point = dot + exp;
        StringBuilder b = new StringBuilder();
        if (point <= 0) {
            b.append("0.");
            for (int i = 0; i < -point; i++) {
                b.append('0');
            }
            b.append(digits);
        } else if (point >= digits.length()) {
            b.append(digits);
            for (int i = digits.length(); i < point; i++) {
                b.append('0');
            }
            b.append(".0");
        } else {
            b.append(digits.substring(0, point)).append('.').append(digits.substring(point));
        }
        return b.toString();
    }

    static String exp(double v, int prec, boolean upper) {
        int e = 0;
        if (v != 0) {
            String plain = decimalDigits(v);
            int dot = plain.indexOf('.');
            String digits = plain.substring(0, dot) + plain.substring(dot + 1);
            int first = 0;
            while (first < digits.length() && digits.charAt(first) == '0') {
                first++;
            }
            e = dot - first - 1;
        }
        double scaled = v == 0 ? 0 : v / Interp.pow(10, e);
        String m = fixed(scaled, prec);
        if (m.startsWith("10")) {
            e++;
            m = fixed(v / Interp.pow(10, e), prec);
        }
        String es = Integer.toString(Math.abs(e));
        if (es.length() < 2) {
            es = "0" + es;
        }
        return m + (upper ? "E" : "e") + (e < 0 ? "-" : "+") + es;
    }

    private static String quote(Object v) {
        if (v instanceof Double || v == null || v instanceof Boolean) {
            return Interp.rawToString(v);
        }
        if (!(v instanceof String)) {
            throw LuaError.rt("bad argument to 'format' (value has no literal form)");
        }
        String s = (String) v;
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                b.append('\\').append(c);
            } else if (c == '\n') {
                b.append("\\n");
            } else if (c == '\r') {
                b.append("\\r");
            } else if (c == 0) {
                b.append("\\0");
            } else if (c < 32 || c == 127) {
                b.append('\\').append((int) c);
            } else {
                b.append(c);
            }
        }
        return b.append('"').toString();
    }

}
