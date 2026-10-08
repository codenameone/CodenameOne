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
package com.codename1.flutter.widgets;

import com.codename1.flutter.Locale;

import dart.core.DartIterable;
import dart.core.DartList;

/**
 * The widget-layer localizations surface — Flutter's
 * {@code WidgetsLocalizations}. new_gallery only calls the static
 * {@link #basicLocaleListResolution} helper as the fallback for its
 * {@code localeListResolutionCallback}.
 */
public abstract class WidgetsLocalizations {

    private WidgetsLocalizations() {
    }

    /**
     * Flutter's top-level {@code basicLocaleListResolution}, in Flutter's order. For
     * each preferred locale, best first: an exact match; then language and script; then
     * language and country; then -- if an EARLIER preferred locale matched on language
     * alone -- that match; then a language-only match, returned at once only for the
     * first preferred locale and only when the next one does not share its language
     * (which might match better). A country-only match is remembered as a later
     * fallback. When nothing matched: the language-only match, else the country-only
     * match, else the first supported locale.
     *
     * <p>{@link Locale} carries no script subtag here -- neither the device query nor the
     * Dart surface can produce one -- so every key is formed with a null script, exactly
     * as Flutter forms it for scriptless locales, and the language-and-script tier never
     * applies.</p>
     */
    public static Locale basicLocaleListResolution(DartList<Locale> preferredLocales,
                                                   DartIterable<Locale> supportedLocales) {
        Locale firstSupported = null;
        java.util.Map<String, Locale> all = new java.util.HashMap<String, Locale>();
        java.util.Map<String, Locale> languageAndCountry = new java.util.HashMap<String, Locale>();
        java.util.Map<String, Locale> languages = new java.util.HashMap<String, Locale>();
        java.util.Map<String, Locale> countries = new java.util.HashMap<String, Locale>();
        if (supportedLocales != null) {
            for (Locale s : supportedLocales) {
                if (s == null) {
                    continue;
                }
                if (firstSupported == null) {
                    firstSupported = s;
                }
                // ??= in Flutter: the FIRST supported locale with each key wins.
                putIfAbsent(all, s.languageCode() + "_null_" + s.countryCode(), s);
                putIfAbsent(languageAndCountry, s.languageCode() + "_" + s.countryCode(), s);
                putIfAbsent(languages, String.valueOf(s.languageCode()), s);
                putIfAbsent(countries, String.valueOf(s.countryCode()), s);
            }
        }
        if (preferredLocales == null || preferredLocales.isEmpty()) {
            return firstSupported;
        }
        Locale matchesLanguageCode = null;
        Locale matchesCountryCode = null;
        int n = preferredLocales.size();
        for (int i = 0; i < n; i++) {
            Locale user = preferredLocales.get(i);
            if (user == null) {
                continue;
            }
            if (all.containsKey(user.languageCode() + "_null_" + user.countryCode())) {
                return user;
            }
            if (user.countryCode() != null) {
                Locale match = languageAndCountry.get(user.languageCode() + "_" + user.countryCode());
                if (match != null) {
                    return match;
                }
            }
            if (matchesLanguageCode != null) {
                return matchesLanguageCode;
            }
            Locale match = languages.get(String.valueOf(user.languageCode()));
            if (match != null) {
                matchesLanguageCode = match;
                Locale next = i + 1 < n ? preferredLocales.get(i + 1) : null;
                if (i == 0 && !(next != null && eq(next.languageCode(), user.languageCode()))) {
                    return matchesLanguageCode;
                }
            }
            if (matchesCountryCode == null && user.countryCode() != null) {
                match = countries.get(user.countryCode());
                if (match != null) {
                    matchesCountryCode = match;
                }
            }
        }
        if (matchesLanguageCode != null) {
            return matchesLanguageCode;
        }
        if (matchesCountryCode != null) {
            return matchesCountryCode;
        }
        return firstSupported != null ? firstSupported : preferredLocales.get(0);
    }

    private static void putIfAbsent(java.util.Map<String, Locale> map, String key, Locale value) {
        if (!map.containsKey(key)) {
            map.put(key, value);
        }
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
