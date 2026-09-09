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
 * The {@code Intl} class from {@code package:intl}. In the gallery it is always
 * reached as {@code intl.Intl.xxx(...)}; the transpiler strips the {@code intl}
 * import prefix and resolves {@code Intl} to this type, invoking every member as
 * a <em>static</em> method. All members are therefore static.
 */
public final class Intl {

    /**
     * The default locale, mirroring {@code Intl.defaultLocale}. {@code null}
     * means "use the runtime/system locale".
     */
    public static String defaultLocale = null;

    // Package-private: the app never instantiates Intl (all members are static),
    // but IntlLib retains a shared instance for the legacy `intl.` prefix path.
    Intl() {
    }

    /**
     * Normalizes a locale identifier. A minimal implementation that maps the
     * common {@code _} separator to {@code -} and returns the value unchanged
     * otherwise.
     */
    public static String canonicalizedLocale(String aLocale) {
        if (aLocale == null || aLocale.length() == 0) {
            return "und";
        }
        return aLocale.replace('_', '-');
    }

    /**
     * English-style plural selection. Returns the branch matching
     * {@code howMany} (one for 1, zero for 0 when supplied), else {@code other}.
     * Extra branches accepted for API shape.
     */
    public static String pluralLogic(Object howManyValue, String locale, String zero, String one,
                                     String two, String few, String many, String other) {
        double howMany = howManyValue instanceof Number ? ((Number) howManyValue).doubleValue() : 0;
        if (howMany == 0 && zero != null) {
            return zero;
        }
        if (howMany == 1 && one != null) {
            return one;
        }
        if (howMany == 2 && two != null) {
            return two;
        }
        if (other != null) {
            return other;
        }
        return one != null ? one : "";
    }

    /**
     * Returns a translated message. Without a message catalog this simply
     * returns the source {@code messageText}, which is the correct behaviour for
     * the base (English) locale.
     */
    public static String message(String messageText, String desc, String locale, String name,
                                 Object args, String meaning) {
        return messageText == null ? "" : messageText;
    }

    /**
     * Plural message selection, delegating to {@link #pluralLogic}. Extra
     * message-metadata parameters ({@code name} / {@code args}) are accepted for
     * API shape.
     */
    public static String plural(Object howMany, String locale, String zero, String one, String two,
                                String few, String many, String other, String name, Object args) {
        return pluralLogic(howMany, locale, zero, one, two, few, many, other);
    }

    /**
     * Selects a branch from {@code cases} by string key, falling back to the
     * {@code "other"} entry. {@code cases} is expected to be a
     * {@code Map<Object,String>}.
     */
    @SuppressWarnings("unchecked")
    public static String select(Object choice, Object cases, String locale, String name, Object args) {
        if (cases instanceof java.util.Map) {
            java.util.Map<Object, Object> m = (java.util.Map<Object, Object>) cases;
            Object v = m.get(choice);
            if (v == null && choice != null) {
                v = m.get(String.valueOf(choice));
            }
            if (v == null) {
                v = m.get("other");
            }
            return v == null ? "" : String.valueOf(v);
        }
        return "";
    }

    /**
     * Gender-based message selection ({@code female} / {@code male} / other).
     */
    public static String gender(String targetGender, String female, String male, String other,
                                String locale, String name, Object args) {
        if ("female".equals(targetGender) && female != null) {
            return female;
        }
        if ("male".equals(targetGender) && male != null) {
            return male;
        }
        return other != null ? other : "";
    }

    /**
     * Runs {@code function} with {@code defaultLocale} temporarily set to
     * {@code locale}. {@code function} is expected to be a zero-arg callable
     * ({@code T Function()}); its result is returned.
     */
    public static Object withLocale(String locale, Object function) {
        String prev = defaultLocale;
        defaultLocale = locale;
        try {
            if (function instanceof dart.runtime.Funcs.Func0) {
                return ((dart.runtime.Funcs.Func0<?>) function).call();
            }
            if (function instanceof dart.runtime.Funcs.VoidFunc0) {
                ((dart.runtime.Funcs.VoidFunc0) function).call();
            }
            return null;
        } finally {
            defaultLocale = prev;
        }
    }

    /** The current locale, i.e. {@link #defaultLocale} or the system default. */
    public static String getCurrentLocale() {
        if (defaultLocale != null) {
            return defaultLocale;
        }
        return "en_US";
    }
}
