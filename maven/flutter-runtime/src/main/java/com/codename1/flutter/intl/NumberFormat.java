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

    public String format(Object number) {
        double v = number instanceof Number ? ((Number) number).doubleValue() : 0;
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
