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
package com.codename1.backend.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The conversions the build's generated tool and operation adapters use: one
 * argument out of a call's argument map, as the type the method declares.
 *
 * <p>Accepts what an agent or an HTTP client actually sends -- a JSON number or a
 * numeric string, a JSON boolean or "true" -- and refuses anything else with an
 * {@link IllegalArgumentException} naming the argument, which the MCP endpoint
 * reports to the agent as a tool error it can correct.
 */
public final class McpArgs {
    private McpArgs() {
    }

    /** One property of an input schema. */
    public static Map property(String type, String description, String[] values) {
        Map p = new LinkedHashMap();
        if(type != null && type.length() > 0) {
            p.put("type", type);
        }
        if(description != null && description.length() > 0) {
            p.put("description", description);
        }
        if(values != null) {
            List list = new ArrayList();
            for(int iter = 0 ; iter < values.length ; iter++) {
                list.add(values[iter]);
            }
            p.put("enum", list);
        }
        return p;
    }

    /** An object schema. */
    public static Map object(Map properties, String[] required) {
        Map out = new LinkedHashMap();
        out.put("type", "object");
        out.put("properties", properties);
        if(required != null && required.length > 0) {
            List list = new ArrayList();
            for(int iter = 0 ; iter < required.length ; iter++) {
                list.add(required[iter]);
            }
            out.put("required", list);
        }
        return out;
    }

    private static Object raw(Map args, String name, boolean required) {
        Object v = args == null ? null : args.get(name);
        if(v == null && required) {
            throw new IllegalArgumentException("Missing required argument \"" + name + "\"");
        }
        return v;
    }

    public static Object any(Map args, String name, boolean required) {
        return raw(args, name, required);
    }

    public static String string(Map args, String name, boolean required) {
        Object v = raw(args, name, required);
        return v == null ? null : String.valueOf(v);
    }

    public static long longValue(Map args, String name, boolean required) {
        Object v = raw(args, name, required);
        return v == null ? 0L : toLong(v, name);
    }

    public static int intValue(Map args, String name, boolean required) {
        long v = longValue(args, name, required);
        if(v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("\"" + name + "\" is out of range");
        }
        return (int)v;
    }

    public static double doubleValue(Map args, String name, boolean required) {
        Object v = raw(args, name, required);
        return v == null ? 0 : toDouble(v, name);
    }

    public static boolean booleanValue(Map args, String name, boolean required) {
        Object v = raw(args, name, required);
        if(v == null) {
            return false;
        }
        if(v instanceof Boolean) {
            return ((Boolean)v).booleanValue();
        }
        String s = String.valueOf(v);
        if("true".equalsIgnoreCase(s)) {
            return true;
        }
        if("false".equalsIgnoreCase(s)) {
            return false;
        }
        throw new IllegalArgumentException("\"" + name + "\" must be true or false");
    }

    public static char charValue(Map args, String name, boolean required) {
        String s = string(args, name, required);
        if(s == null) {
            return 0;
        }
        if(s.length() != 1) {
            throw new IllegalArgumentException("\"" + name + "\" must be one character");
        }
        return s.charAt(0);
    }

    public static Integer integerObject(Map args, String name, boolean required) {
        return raw(args, name, required) == null ? null
                : new Integer(intValue(args, name, required));
    }

    public static Long longObject(Map args, String name, boolean required) {
        return raw(args, name, required) == null ? null
                : new Long(longValue(args, name, required));
    }

    public static Short shortObject(Map args, String name, boolean required) {
        return raw(args, name, required) == null ? null
                : new Short((short)intValue(args, name, required));
    }

    public static Byte byteObject(Map args, String name, boolean required) {
        return raw(args, name, required) == null ? null
                : new Byte((byte)intValue(args, name, required));
    }

    public static Double doubleObject(Map args, String name, boolean required) {
        return raw(args, name, required) == null ? null
                : new Double(doubleValue(args, name, required));
    }

    public static Float floatObject(Map args, String name, boolean required) {
        return raw(args, name, required) == null ? null
                : new Float((float)doubleValue(args, name, required));
    }

    public static Boolean booleanObject(Map args, String name, boolean required) {
        return raw(args, name, required) == null ? null
                : Boolean.valueOf(booleanValue(args, name, required));
    }

    public static Character characterObject(Map args, String name, boolean required) {
        return raw(args, name, required) == null ? null
                : new Character(charValue(args, name, required));
    }

    public static Map map(Map args, String name, boolean required) {
        Object v = raw(args, name, required);
        if(v == null || v instanceof Map) {
            return (Map)v;
        }
        throw new IllegalArgumentException("\"" + name + "\" must be an object");
    }

    public static List list(Map args, String name, boolean required) {
        Object v = raw(args, name, required);
        if(v == null || v instanceof List) {
            return (List)v;
        }
        throw new IllegalArgumentException("\"" + name + "\" must be an array");
    }

    /** The constant of {@code values} -- an enum's values() -- that the argument names. */
    public static Object enumValue(Object[] values, Map args, String name, boolean required) {
        String s = string(args, name, required);
        if(s == null) {
            return null;
        }
        for(int iter = 0 ; iter < values.length ; iter++) {
            if(String.valueOf(values[iter]).equals(s)) {
                return values[iter];
            }
        }
        StringBuilder allowed = new StringBuilder();
        for(int iter = 0 ; iter < values.length ; iter++) {
            allowed.append(iter == 0 ? "" : ", ").append(values[iter]);
        }
        throw new IllegalArgumentException("\"" + name + "\" must be one of " + allowed);
    }

    /** A boxed number as a double, NaN for null. */
    public static double toDouble(Object value) {
        return value instanceof Number ? ((Number)value).doubleValue() : Double.NaN;
    }

    private static long toLong(Object v, String name) {
        if(v instanceof Number) {
            double d = ((Number)v).doubleValue();
            if(d != Math.floor(d)) {
                throw new IllegalArgumentException("\"" + name + "\" must be a whole number");
            }
            return ((Number)v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (NumberFormatException err) {
            throw new IllegalArgumentException("\"" + name + "\" must be a whole number");
        }
    }

    private static double toDouble(Object v, String name) {
        if(v instanceof Number) {
            return ((Number)v).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(v).trim());
        } catch (NumberFormatException err) {
            throw new IllegalArgumentException("\"" + name + "\" must be a number");
        }
    }
}
