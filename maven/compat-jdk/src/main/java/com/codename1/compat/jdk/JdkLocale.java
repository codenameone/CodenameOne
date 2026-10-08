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
package com.codename1.compat.jdk;

import java.util.Locale;

/// What `java.util.Locale` has on a desktop and the device's `Locale` does
/// not: the constants, language tags, and value semantics for `equals`,
/// `hashCode` and `toString`.
///
/// The device class stays the application's `Locale` -- every API of the
/// device that takes a locale takes that one. The build's remap step
/// redirects only what is missing: a read of `Locale.FRANCE` becomes a read
/// of [#FRANCE], `Locale.forLanguageTag(t)` a call of
/// [#forLanguageTag(String)], and `locale.equals(o)`, written against a
/// variable of type `Locale`, a call of [#equals(Locale, Object)].
///
/// #### What that cannot reach
///
/// The device's `Locale` compares by identity, and only calls the compiler
/// bound to `Locale` are redirected. A locale used as a `HashMap` key, held
/// in a variable of type `Object`, or appended to a string is still the
/// device's: two equal locales are then different keys, and the text is not
/// `en_US`. Key such a map by [#toString(Locale)] instead.
///
/// A device locale is a language and a country. Scripts, variants and
/// extensions are dropped by [#forLanguageTag(String)], and
/// [#getVariant(Locale)] is always empty. The display names are the codes
/// themselves: the device has no table of language names to translate them.
public final class JdkLocale {

    public static final Locale ENGLISH = new Locale("en", "");
    public static final Locale FRENCH = new Locale("fr", "");
    public static final Locale GERMAN = new Locale("de", "");
    public static final Locale ITALIAN = new Locale("it", "");
    public static final Locale JAPANESE = new Locale("ja", "");
    public static final Locale KOREAN = new Locale("ko", "");
    public static final Locale CHINESE = new Locale("zh", "");
    public static final Locale SIMPLIFIED_CHINESE = new Locale("zh", "CN");
    public static final Locale TRADITIONAL_CHINESE = new Locale("zh", "TW");
    public static final Locale FRANCE = new Locale("fr", "FR");
    public static final Locale GERMANY = new Locale("de", "DE");
    public static final Locale ITALY = new Locale("it", "IT");
    public static final Locale JAPAN = new Locale("ja", "JP");
    public static final Locale KOREA = new Locale("ko", "KR");
    public static final Locale CHINA = SIMPLIFIED_CHINESE;
    public static final Locale PRC = SIMPLIFIED_CHINESE;
    public static final Locale TAIWAN = TRADITIONAL_CHINESE;
    public static final Locale UK = new Locale("en", "GB");
    public static final Locale US = new Locale("en", "US");
    public static final Locale CANADA = new Locale("en", "CA");
    public static final Locale CANADA_FRENCH = new Locale("fr", "CA");
    public static final Locale ROOT = new Locale("", "");

    private JdkLocale() {
    }

    /// The locale an IETF language tag names: `fr`, `en-US`, `zh-Hans-CN`.
    /// The first subtag is the language, the first later subtag that is two
    /// letters or three digits the country; anything else is dropped. `und`
    /// and the empty tag are the root locale.
    public static Locale forLanguageTag(String languageTag) {
        if (languageTag == null) {
            throw new NullPointerException();
        }
        String language = "";
        String country = "";
        int n = languageTag.length();
        int start = 0;
        int index = 0;
        for (int i = 0; i <= n; i++) {
            if (i < n && languageTag.charAt(i) != '-' && languageTag.charAt(i) != '_') {
                continue;
            }
            String part = languageTag.substring(start, i);
            start = i + 1;
            if (index == 0) {
                language = isLetters(part) && part.length() >= 2 && part.length() <= 8
                        && !"und".equals(asciiLower(part)) ? asciiLower(part) : "";
            } else if (part.length() == 1) {
                // A singleton starts the extensions; nothing after it is a region.
                break;
            } else if (country.length() == 0
                    && ((part.length() == 2 && isLetters(part)) || (part.length() == 3 && isDigits(part)))) {
                country = asciiUpper(part);
            }
            index++;
        }
        return new Locale(language, country);
    }

    /// The IETF tag of `locale`: `en-US`, `fr`, or `und` for the root.
    public static String toLanguageTag(Locale locale) {
        String language = language(locale);
        String country = country(locale);
        if (language.length() == 0) {
            return country.length() == 0 ? "und" : "und-" + country;
        }
        return country.length() == 0 ? language : language + "-" + country;
    }

    /// Whether `other` is a locale of the same language and country.
    public static boolean equals(Locale locale, Object other) {
        if (locale == null) {
            throw new NullPointerException();
        }
        if (locale == other) {
            return true;
        }
        if (!(other instanceof Locale)) {
            return false;
        }
        Locale o = (Locale) other;
        return language(locale).equals(language(o)) && country(locale).equals(country(o));
    }

    /// A hash consistent with [#equals(Locale, Object)].
    public static int hashCode(Locale locale) {
        return language(locale).hashCode() * 31 + country(locale).hashCode();
    }

    /// `en_US`, `fr`, or the empty string for the root locale.
    public static String toString(Locale locale) {
        String language = language(locale);
        String country = country(locale);
        return country.length() == 0 ? language : language + "_" + country;
    }

    /// Always empty: a device locale has no variant.
    public static String getVariant(Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        return "";
    }

    /// Always empty: a device locale has no script.
    public static String getScript(Locale locale) {
        return getVariant(locale);
    }

    /// The language code; see the class description.
    public static String getDisplayLanguage(Locale locale) {
        return language(locale);
    }

    /// The country code; see the class description.
    public static String getDisplayCountry(Locale locale) {
        return country(locale);
    }

    /// `en (US)`, or the language code alone.
    public static String getDisplayName(Locale locale) {
        String language = language(locale);
        String country = country(locale);
        if (country.length() == 0) {
            return language;
        }
        return language.length() == 0 ? country : language + " (" + country + ")";
    }

    /// The locales this class has constants for, and the default locale.
    public static Locale[] getAvailableLocales() {
        return new Locale[] {
            Locale.getDefault(), ENGLISH, FRENCH, GERMAN, ITALIAN, JAPANESE, KOREAN, CHINESE,
            SIMPLIFIED_CHINESE, TRADITIONAL_CHINESE, FRANCE, GERMANY, ITALY, JAPAN, KOREA, UK, US, CANADA,
            CANADA_FRENCH,
        };
    }

    private static String language(Locale locale) {
        String language = locale.getLanguage();
        return language == null ? "" : asciiLower(language);
    }

    private static String country(Locale locale) {
        String country = locale.getCountry();
        return country == null ? "" : asciiUpper(country);
    }

    private static boolean isLetters(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
                return false;
            }
        }
        return s.length() > 0;
    }

    private static boolean isDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return s.length() > 0;
    }

    /// Language and country codes are ASCII, and `String.toLowerCase` folds
    /// by the device's locale: under Turkish `"IT"` would not become `"it"`.
    private static String asciiLower(String s) {
        StringBuilder out = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (out == null) {
                    out = new StringBuilder(s);
                }
                out.setCharAt(i, (char) (c + ('a' - 'A')));
            }
        }
        return out == null ? s : out.toString();
    }

    private static String asciiUpper(String s) {
        StringBuilder out = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'z') {
                if (out == null) {
                    out = new StringBuilder(s);
                }
                out.setCharAt(i, (char) (c - ('a' - 'A')));
            }
        }
        return out == null ? s : out.toString();
    }
}
