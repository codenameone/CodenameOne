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
package com.codename1.desktopcompat.javax.swing.text;

import java.text.NumberFormat;
import java.text.ParseException;

/// Formats numbers through a `NumberFormat`, keeps them inside an optional
/// range, and reads them back as the value class.
///
/// On the desktop this class sits two levels further down, under a
/// formatter for any `java.text.Format`; here it holds the number format
/// itself.
public class NumberFormatter extends DefaultFormatter {

    private NumberFormat numberFormat;
    private Comparable<?> min;
    private Comparable<?> max;

    public NumberFormatter() {
        this(NumberFormat.getNumberInstance());
    }

    public NumberFormatter(NumberFormat format) {
        numberFormat = format;
        setOverwriteMode(false);
    }

    public void setMinimum(Comparable minimum) {
        min = minimum;
    }

    public Comparable getMinimum() {
        return min;
    }

    public void setMaximum(Comparable max) {
        this.max = max;
    }

    public Comparable getMaximum() {
        return max;
    }

    @Override
    public Object stringToValue(String text) throws ParseException {
        if (text == null || text.length() == 0) {
            return null;
        }
        NumberFormat f = numberFormat;
        Object parsed = f == null ? (Object) text : f.parse(text);
        Object value = parsed instanceof Number ? convert((Number) parsed) : parsed;
        if (!inRange(value)) {
            throw new ParseException("Value not within min/max range", 0);
        }
        return value;
    }

    @Override
    public String valueToString(Object value) throws ParseException {
        if (value == null) {
            return "";
        }
        NumberFormat f = numberFormat;
        if (f == null || !(value instanceof Number)) {
            return value.toString();
        }
        if (value instanceof Double || value instanceof Float) {
            return f.format(((Number) value).doubleValue());
        }
        return f.format(((Number) value).longValue());
    }

    private Object convert(Number n) {
        Class<?> vc = getValueClass();
        if (vc == null && getFormattedTextField() != null) {
            Object current = getFormattedTextField().getValue();
            if (current != null) {
                vc = current.getClass();
            }
        }
        if (vc == Integer.class) {
            return Integer.valueOf(n.intValue());
        } else if (vc == Long.class) {
            return Long.valueOf(n.longValue());
        } else if (vc == Double.class) {
            return Double.valueOf(n.doubleValue());
        } else if (vc == Float.class) {
            return Float.valueOf(n.floatValue());
        } else if (vc == Short.class) {
            return Short.valueOf(n.shortValue());
        } else if (vc == Byte.class) {
            return Byte.valueOf(n.byteValue());
        }
        return n;
    }

    private static int compare(Object value, Object bound) {
        if (value instanceof Number && bound instanceof Number) {
            double a = ((Number) value).doubleValue();
            double b = ((Number) bound).doubleValue();
            return a < b ? -1 : a > b ? 1 : 0;
        }
        return 0;
    }

    private boolean inRange(Object value) {
        if (value == null) {
            return true;
        }
        return (min == null || compare(value, min) >= 0) && (max == null || compare(value, max) <= 0);
    }
}
