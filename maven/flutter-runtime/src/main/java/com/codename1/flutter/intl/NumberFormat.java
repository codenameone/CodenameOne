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
package com.codename1.flutter.intl;

/**
 * A subset of {@code package:intl}'s NumberFormat covering the currency and
 * percent factory constructors used by the gallery. Formatting is a simple
 * fixed-decimal render (no locale grouping); faithful locale output is a
 * later pass.
 */
public final class NumberFormat {

    private final String prefix;
    private final String suffix;
    private final int digits;
    private final boolean percent;
    /** 0: fixed; 1: compact ("1.2K"); 2: compact long ("1.2 thousand"); 3: grouped decimal. */
    private int mode;

    private NumberFormat(String prefix, String suffix, int digits, boolean percent) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.digits = digits;
        this.percent = percent;
    }

    // decimalDigits is boxed so that an omitted argument (null) and an explicit 0
    // stay distinct: as a primitive both arrived as 0, and 0 was read as "not
    // given", so a whole-unit currency printed $12.00 for 12.

    public static NumberFormat currency(String locale, String symbol, Long decimalDigits, String name) {
        return new NumberFormat(symbol != null ? symbol : "$", "",
                decimalDigits != null ? (int) decimalDigits.longValue() : 2, false);
    }

    public static NumberFormat simpleCurrency(String locale, String name, Long decimalDigits) {
        return new NumberFormat("$", "", decimalDigits != null ? (int) decimalDigits.longValue() : 2, false);
    }

    public static NumberFormat decimalPercentPattern(String locale, long decimalDigits) {
        return new NumberFormat("", "%", decimalDigits >= 0 ? (int) decimalDigits : 0, true);
    }

    private static NumberFormat withMode(NumberFormat f, int mode) {
        f.mode = mode;
        return f;
    }

    /** {@code NumberFormat.compact()}: three significant digits and a K/M/B/T suffix. */
    public static NumberFormat compact(String locale, Boolean explicitSign) {
        return withMode(new NumberFormat("", "", -1, false), 1);
    }

    /** {@code NumberFormat.compactLong()}: "1.23 thousand". */
    public static NumberFormat compactLong(String locale, Boolean explicitSign) {
        return withMode(new NumberFormat("", "", -1, false), 2);
    }

    /** {@code NumberFormat.compactCurrency(symbol:)}: the symbol before a compact amount. */
    public static NumberFormat compactCurrency(String locale, String name, String symbol, Long decimalDigits) {
        return withMode(new NumberFormat(symbol != null ? symbol : (name != null ? name : "USD"), "",
                decimalDigits != null ? (int) decimalDigits.longValue() : 2, false), 1);
    }

    /** {@code NumberFormat.compactSimpleCurrency()}: as compactCurrency, with "$". */
    public static NumberFormat compactSimpleCurrency(String locale, String name, Long decimalDigits) {
        return withMode(new NumberFormat("$", "", decimalDigits != null ? (int) decimalDigits.longValue() : 2,
                false), 1);
    }

    /** {@code NumberFormat.decimalPattern()}: grouped thousands, up to three decimals. */
    public static NumberFormat decimalPattern(String locale) {
        return withMode(new NumberFormat("", "", 3, false), 3);
    }

    /** {@code NumberFormat.percentPattern()}: a whole percentage. */
    public static NumberFormat percentPattern(String locale) {
        return new NumberFormat("", "%", 0, true);
    }

    private static final String[] SHORT = {"", "K", "M", "B", "T"};
    private static final String[] LONG = {"", " thousand", " million", " billion", " trillion"};

    /// intl's en_US compact rule: the value scaled to its thousands bucket and shown to
    /// three significant digits with trailing zeros dropped (1234 -> 1.23K, 999999 ->
    /// 1M). Below a thousand a currency keeps up to its decimal digits as three
    /// significant ones (7 -> $7.00, 999 -> $999); a plain number drops the zeros.
    private String compactFormat(double value) {
        boolean neg = value < 0;
        double v = Math.abs(value);
        String sign = neg ? "-" : "";
        if (v < 1000) {
            int intDigits = v < 1 ? 1 : Long.toString((long) Math.floor(v)).length();
            boolean currency = prefix.length() > 0;
            int dec = Math.max(0, Math.min(currency ? digits : 2, 3 - intDigits));
            String s = fixed(v, dec);
            if (!currency) {
                s = trim(s);
            }
            return sign + prefix + s;
        }
        int k = 0;
        double scaled = v;
        while (scaled >= 1000 && k < SHORT.length - 1) {
            scaled /= 1000;
            k++;
        }
        int intDigits = Long.toString((long) Math.floor(scaled)).length();
        String s = trim(fixed(scaled, Math.max(0, 3 - intDigits)));
        if (s.startsWith("1000") && k < SHORT.length - 1) {
            k++;
            s = "1";
        }
        return sign + prefix + s + (mode == 2 ? LONG[k] : SHORT[k]);
    }

    private static String trim(String s) {
        if (s.indexOf('.') < 0) {
            return s;
        }
        int end = s.length();
        while (s.charAt(end - 1) == '0') {
            end--;
        }
        if (s.charAt(end - 1) == '.') {
            end--;
        }
        return s.substring(0, end);
    }

    private static String grouped(String s) {
        int dot = s.indexOf('.');
        String ip = dot < 0 ? s : s.substring(0, dot);
        boolean neg = ip.startsWith("-");
        if (neg) {
            ip = ip.substring(1);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ip.length(); i++) {
            if (i > 0 && (ip.length() - i) % 3 == 0) {
                sb.append(',');
            }
            sb.append(ip.charAt(i));
        }
        return (neg ? "-" : "") + sb + (dot < 0 ? "" : s.substring(dot));
    }

    public String format(Object number) {
        double v = number instanceof Number ? ((Number) number).doubleValue() : 0;
        if (mode == 1 || mode == 2) {
            return compactFormat(v);
        }
        if (mode == 3) {
            return grouped(trim(fixed(v, digits)));
        }
        if (percent) {
            v = v * 100;
        }
        return prefix + fixed(v, digits) + suffix;
    }

    private static String fixed(double value, int digits) {
        boolean neg = value < 0;
        double v = neg ? -value : value;
        long factor = 1;
        for (int i = 0; i < digits; i++) {
            factor *= 10;
        }
        long scaled = Math.round(v * factor);
        long intPart = scaled / factor;
        long fracPart = scaled % factor;
        StringBuilder sb = new StringBuilder();
        if (neg) {
            sb.append('-');
        }
        sb.append(intPart);
        if (digits > 0) {
            sb.append('.');
            String f = Long.toString(fracPart);
            for (int i = f.length(); i < digits; i++) {
                sb.append('0');
            }
            sb.append(f);
        }
        return sb.toString();
    }
}
