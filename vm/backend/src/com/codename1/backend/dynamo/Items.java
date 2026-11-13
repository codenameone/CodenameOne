/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.dynamo;

import com.codename1.backend.Base64;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Converts between DynamoDB attribute values and plain Java values.
///
/// | Java | DynamoDB |
/// |---|---|
/// | `null` | `NULL` |
/// | `String` | `S` |
/// | `Number` | `N` |
/// | `Boolean` | `BOOL` |
/// | `byte[]` | `B` |
/// | `List` | `L` |
/// | `Map` with string keys | `M` |
///
/// Sets are read as lists (`SS`, `NS` and `BS` all decode to a `List`). A number
/// is read back as a `Long` when it is integral and fits, and as a `Double`
/// otherwise: a DynamoDB number has 38 digits of precision and this runtime has
/// no `BigDecimal`, so a caller that needs more than a double holds reads the
/// raw attribute map instead.
public final class Items {
    private Items() {
    }

    /// The attribute map of an item made of plain values.
    public static Map toAttributes(Map item) {
        Map out = new LinkedHashMap();
        if (item == null) {
            return out;
        }
        Iterator it = item.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            out.put(String.valueOf(e.getKey()), toAttribute(e.getValue()));
        }
        return out;
    }

    /// One plain value as an attribute value.
    public static Map toAttribute(Object value) {
        Map av = new LinkedHashMap();
        if (value == null) {
            av.put("NULL", Boolean.TRUE);
        } else if (value instanceof String) {
            av.put("S", value);
        } else if (value instanceof Boolean) {
            av.put("BOOL", value);
        } else if (value instanceof Number) {
            av.put("N", numberText((Number) value));
        } else if (value instanceof byte[]) {
            av.put("B", Base64.encode((byte[]) value));
        } else if (value instanceof List) {
            List list = (List) value;
            List out = new ArrayList(list.size());
            for (int i = 0; i < list.size(); i++) {
                out.add(toAttribute(list.get(i)));
            }
            av.put("L", out);
        } else if (value instanceof Map) {
            av.put("M", toAttributes((Map) value));
        } else {
            throw new IllegalArgumentException("DynamoDB cannot store a "
                    + value.getClass().getName()
                    + "; use a String, Number, Boolean, byte[], List or Map");
        }
        return av;
    }

    private static String numberText(Number n) {
        if (n instanceof Double || n instanceof Float) {
            double d = n.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("DynamoDB has no NaN or infinity");
            }
            if (d == Math.floor(d) && Math.abs(d) < 9.0E15) {
                return Long.toString((long) d);
            }
        }
        return n.toString();
    }

    /// The plain values of an attribute map; null for null.
    public static Map fromAttributes(Map attributes) {
        if (attributes == null) {
            return null;
        }
        Map out = new LinkedHashMap();
        Iterator it = attributes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            Object v = e.getValue();
            out.put(e.getKey(), v instanceof Map ? fromAttribute((Map) v) : null);
        }
        return out;
    }

    /// One attribute value as a plain value.
    public static Object fromAttribute(Map av) {
        if (av.containsKey("S")) {
            return av.get("S");
        }
        if (av.containsKey("N")) {
            return number(String.valueOf(av.get("N")));
        }
        if (av.containsKey("BOOL")) {
            return Boolean.valueOf(Boolean.TRUE.equals(av.get("BOOL")));
        }
        if (av.containsKey("B")) {
            return Base64.decode(String.valueOf(av.get("B")));
        }
        if (av.containsKey("M")) {
            Object m = av.get("M");
            return m instanceof Map ? fromAttributes((Map) m) : null;
        }
        if (av.containsKey("L")) {
            return list(av.get("L"), null);
        }
        if (av.containsKey("SS")) {
            return list(av.get("SS"), "S");
        }
        if (av.containsKey("NS")) {
            return list(av.get("NS"), "N");
        }
        if (av.containsKey("BS")) {
            return list(av.get("BS"), "B");
        }
        return null;
    }

    private static List list(Object raw, String scalar) {
        List out = new ArrayList();
        if (!(raw instanceof List)) {
            return out;
        }
        List in = (List) raw;
        for (int i = 0; i < in.size(); i++) {
            Object v = in.get(i);
            if (scalar == null) {
                out.add(v instanceof Map ? fromAttribute((Map) v) : null);
            } else if ("N".equals(scalar)) {
                out.add(number(String.valueOf(v)));
            } else if ("B".equals(scalar)) {
                out.add(Base64.decode(String.valueOf(v)));
            } else {
                out.add(v);
            }
        }
        return out;
    }

    static Object number(String text) {
        boolean integral = text.length() > 0 && text.length() <= 18;
        for (int i = 0; integral && i < text.length(); i++) {
            char c = text.charAt(i);
            integral = (c >= '0' && c <= '9') || (i == 0 && c == '-' && text.length() > 1);
        }
        if (integral) {
            return Long.valueOf(Long.parseLong(text));
        }
        return Double.valueOf(Double.parseDouble(text));
    }
}
