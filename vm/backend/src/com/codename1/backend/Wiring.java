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
package com.codename1.backend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// What the build-generated wiring calls: configuration placeholders, the
/// conversions a `@Value` needs, and the conditions of conditional beans.
///
/// Everything here runs once, at start-up, from straight-line code the build
/// wrote. None of it looks up a bean: the build already decided which bean goes
/// where.
public final class Wiring {
    private Wiring() {
    }

    /// Where a request- or session-scoped bean's current instance comes from, for
    /// the generated class that stands in for it in a singleton. `slot` is
    /// the number the build gave the bean.
    public interface Scope {
        Object get(int slot);
    }

    /// Resolves every `${key`} and `${key:fallback`} in
    /// `expression` against the configuration, keeping the text around
    /// them.
    ///
    /// #### Parameters
    ///
    /// - `where`: the injection point, for the refusal when a key has no value
    public static String value(Config config, String expression, String where) {
        if (expression == null || expression.indexOf("${") < 0) {
            return expression;
        }
        StringBuilder out = new StringBuilder();
        int at = 0;
        while (at < expression.length()) {
            int open = expression.indexOf("${", at);
            if (open < 0) {
                out.append(expression.substring(at));
                break;
            }
            int close = expression.indexOf('}', open + 2);
            if (close < 0) {
                throw new IllegalStateException(where + ": unterminated ${ in \"" + expression
                        + "\"");
            }
            out.append(expression.substring(at, open));
            String inner = expression.substring(open + 2, close);
            int colon = inner.indexOf(':');
            String key = colon < 0 ? inner : inner.substring(0, colon);
            String resolved;
            try {
                resolved = config.get(key.trim());
            } catch (IOException err) {
                throw new IllegalStateException(where + ": " + err.getMessage(), err);
            }
            if (resolved == null) {
                if (colon < 0) {
                    throw new IllegalStateException(where + " needs the setting \"" + key.trim()
                            + "\", and nothing sets it: add it to application.properties, "
                            + "set it in the environment, or give the expression a "
                            + "fallback with ${" + key.trim() + ":...}");
                }
                resolved = inner.substring(colon + 1);
            }
            out.append(resolved);
            at = close + 1;
        }
        return out.toString();
    }

    /// The first of the keys that is set, or null. For configuration-property binding.
    public static String property(Config config, String key, String relaxed) {
        try {
            String value = config.get(key);
            if (value == null && relaxed != null) {
                value = config.get(relaxed);
            }
            return value;
        } catch (IOException err) {
            throw new IllegalStateException(key + ": " + err.getMessage(), err);
        }
    }

    /// Whether any of the profiles, each optionally negated with `!`, is active.
    public static boolean profiles(Config config, String[] profiles) {
        String active = config.getProfile();
        for (String element : profiles) {
            String p = element.trim();
            if (p.startsWith("!")) {
                if (!p.substring(1).trim().equalsIgnoreCase(active)) {
                    return true;
                }
            } else if (p.equalsIgnoreCase(active)) {
                return true;
            }
        }
        return false;
    }

    /// `@ConditionalOnProperty`: whether the key's value matches.
    public static boolean propertyMatches(Config config, String key, String havingValue,
                                          boolean matchIfMissing) {
        String value;
        try {
            value = config.get(key);
        } catch (IOException err) {
            throw new IllegalStateException(key + ": " + err.getMessage(), err);
        }
        if (value == null) {
            return matchIfMissing;
        }
        if (havingValue == null || havingValue.length() == 0) {
            return !"false".equalsIgnoreCase(value.trim());
        }
        return havingValue.equalsIgnoreCase(value.trim());
    }

    /// What a `@Bean` method returned, refused when it is null: a factory that
    /// produces nothing is a bug in the factory, and handing the null to what
    /// injects it would only move the failure to its first use.
    public static Object produced(Object bean, String factory) {
        if (bean == null) {
            throw new IllegalStateException(factory + " returned null; a @Bean method must "
                    + "return the bean, or be made conditional so that it is not built");
        }
        return bean;
    }

    /// The first of `candidates` -- a conditional @Primary -- when it exists,
    /// otherwise the one of the rest that does, as [#single] picks it.
    public static Object preferred(Object[] candidates, boolean required, String what) {
        if (candidates.length > 0 && candidates[0] != null) {
            return candidates[0];
        }
        Object[] rest = new Object[Math.max(0, candidates.length - 1)];
        System.arraycopy(candidates, 1, rest, 0, rest.length);
        return single(rest, required, what);
    }

    /// The one bean of `candidates` that exists, when all of them are
    /// conditional. More than one existing is ambiguous; none is a missing
    /// dependency when `required`.
    public static Object single(Object[] candidates, boolean required, String what) {
        Object found = null;
        for (Object element : candidates) {
            if (element != null) {
                if (found != null) {
                    throw new IllegalStateException(what + ": more than one conditional bean "
                            + "is active for it; mark one @Primary or use @Qualifier");
                }
                found = element;
            }
        }
        if (found == null && required) {
            throw new IllegalStateException(what + ": no bean for it is active under this "
                    + "configuration -- every candidate is conditional");
        }
        return found;
    }

    /// The beans of `items` that exist, for a `List` injection point.
    public static List list(Object[] items) {
        List out = new ArrayList(items.length);
        for (Object element : items) {
            if (element != null) {
                out.add(element);
            }
        }
        return out;
    }

    public static int toInt(String value, String where) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException err) {
            throw bad(value, where, "a whole number", err);
        }
    }

    public static long toLong(String value, String where) {
        try {
            return Long.parseLong(value.trim());
        } catch (RuntimeException err) {
            throw bad(value, where, "a whole number", err);
        }
    }

    public static short toShort(String value, String where) {
        int v = toInt(value, where);
        if (v < Short.MIN_VALUE || v > Short.MAX_VALUE) {
            throw bad(value, where, "a short");
        }
        return (short) v;
    }

    public static byte toByte(String value, String where) {
        int v = toInt(value, where);
        if (v < Byte.MIN_VALUE || v > Byte.MAX_VALUE) {
            throw bad(value, where, "a byte");
        }
        return (byte) v;
    }

    public static double toDouble(String value, String where) {
        try {
            return Double.parseDouble(value.trim());
        } catch (RuntimeException err) {
            throw bad(value, where, "a number", err);
        }
    }

    public static float toFloat(String value, String where) {
        return (float) toDouble(value, where);
    }

    public static char toChar(String value, String where) {
        if (value.length() != 1) {
            throw bad(value, where, "one character");
        }
        return value.charAt(0);
    }

    public static boolean toBoolean(String value, String where) {
        String v = value.trim();
        if ("true".equalsIgnoreCase(v) || "yes".equalsIgnoreCase(v) || "on".equalsIgnoreCase(v)
                || "1".equals(v)) {
            return true;
        }
        if ("false".equalsIgnoreCase(v) || "no".equalsIgnoreCase(v)
                || "off".equalsIgnoreCase(v) || "0".equals(v)) {
            return false;
        }
        throw bad(value, where, "true or false");
    }

    /// The constant of an enum by name; `values` is the enum's values().
    public static Object toEnum(Object[] values, String value, String where) {
        String v = value.trim();
        for (Object element : values) {
            if (((Enum) element).name().equals(v)) {
                return element;
            }
        }
        for (Object element : values) {
            if (((Enum) element).name().equalsIgnoreCase(v)) {
                return element;
            }
        }
        throw bad(value, where, "one of the enum's constants");
    }

    private static IllegalStateException bad(String value, String where, String what) {
        return bad(value, where, what, null);
    }

    private static IllegalStateException bad(String value, String where, String what,
            Throwable cause) {
        return new IllegalStateException(where + " is \"" + value + "\", which is not " + what,
                cause);
    }

    /// Reports a destroy method that failed; the others still run.
    public static void destroyFailed(String bean, Throwable error) {
        System.err.println("Destroying bean " + bean + " failed: " + error);
    }
}
