/*
 * Copyright (c) 2021, Codename One and/or its affiliates. All rights reserved.
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
package java.util;

/// A language, a country and a variant, with the JDK's value semantics:
/// two locales of the same codes are equal, hash alike and print alike
/// (`en_US`). The codes are normalized as the JDK normalizes them -- the
/// language to lower case, the country to upper case, with an ASCII fold, so
/// a device set to Turkish cannot turn `IT` into a dotless `it`.
///
/// This runtime carries no locale data beyond the English names of the
/// common languages and countries, so the `getDisplay*` methods answer in
/// English whatever locale they are asked for, and fall back to the code.
/// A script and extensions are not kept: `getScript` is always empty.
public class Locale {
    /// English.
    public static final Locale ENGLISH = new Locale("en", "");
    /// French.
    public static final Locale FRENCH = new Locale("fr", "");
    /// German.
    public static final Locale GERMAN = new Locale("de", "");
    /// Italian.
    public static final Locale ITALIAN = new Locale("it", "");
    /// Japanese.
    public static final Locale JAPANESE = new Locale("ja", "");
    /// Korean.
    public static final Locale KOREAN = new Locale("ko", "");
    /// Chinese.
    public static final Locale CHINESE = new Locale("zh", "");
    /// Chinese as written in China.
    public static final Locale SIMPLIFIED_CHINESE = new Locale("zh", "CN");
    /// Chinese as written in Taiwan.
    public static final Locale TRADITIONAL_CHINESE = new Locale("zh", "TW");
    /// French in France.
    public static final Locale FRANCE = new Locale("fr", "FR");
    /// German in Germany.
    public static final Locale GERMANY = new Locale("de", "DE");
    /// Italian in Italy.
    public static final Locale ITALY = new Locale("it", "IT");
    /// Japanese in Japan.
    public static final Locale JAPAN = new Locale("ja", "JP");
    /// Korean in Korea.
    public static final Locale KOREA = new Locale("ko", "KR");
    /// Chinese in China; the same locale as `SIMPLIFIED_CHINESE`.
    public static final Locale CHINA = SIMPLIFIED_CHINESE;
    /// Chinese in China; the same locale as `SIMPLIFIED_CHINESE`.
    public static final Locale PRC = SIMPLIFIED_CHINESE;
    /// Chinese in Taiwan; the same locale as `TRADITIONAL_CHINESE`.
    public static final Locale TAIWAN = TRADITIONAL_CHINESE;
    /// English in the United Kingdom.
    public static final Locale UK = new Locale("en", "GB");
    /// English in the United States.
    public static final Locale US = new Locale("en", "US");
    /// English in Canada.
    public static final Locale CANADA = new Locale("en", "CA");
    /// French in Canada.
    public static final Locale CANADA_FRENCH = new Locale("fr", "CA");
    /// The root locale: no language, country or variant.
    public static final Locale ROOT = new Locale("", "");

    private static Locale defaultLocale;
    private String language;
    private String country;
    private String variant;

    /// A locale of a language alone.
    public Locale(String language) {
        this(language, "", "");
    }

    /// A locale of a language and a country.
    public Locale(String language, String country) {
        this(language, country, "");
    }

    /// A locale of a language, a country and a variant.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if any of them is null
    public Locale(String language, String country, String variant) {
        if (language == null || country == null || variant == null) {
            throw new NullPointerException();
        }
        this.language = normalizeLanguage(language);
        this.country = asciiUpper(country);
        this.variant = variant;
    }

    Locale() {
        language = "en";
        int pos;
        if (language != null && (pos = language.indexOf('-')) != -1) {
            country = language.substring(pos+1);
            language = language.substring(0, pos);
        }
        if (language != null && (pos = language.indexOf('_')) != -1) {
            country = language.substring(pos+1);
            language = language.substring(0, pos);
        }
        if (country == null) {
            country = "US";//getOSCountry();
        }
        language = language == null ? "" : normalizeLanguage(language);
        country = asciiUpper(country);
        variant = "";
    }

    /// `new Locale(language)`.
    public static Locale of(String language) {
        return new Locale(language);
    }

    /// `new Locale(language, country)`.
    public static Locale of(String language, String country) {
        return new Locale(language, country);
    }

    /// `new Locale(language, country, variant)`.
    public static Locale of(String language, String country, String variant) {
        return new Locale(language, country, variant);
    }

    public static Locale getDefault() {
        if(defaultLocale == null) {
            defaultLocale = new Locale();
        }
        return defaultLocale;
    }

    public static void setDefault(Locale l) {
        if (l == null) {
            throw new NullPointerException("Can't set default locale to NULL");
        }
        defaultLocale = l;
    }

    public String getLanguage() {
        return language;
    }

    public String getCountry() {
        return country;
    }

    /// The variant, or the empty string.
    public String getVariant() {
        return variant;
    }

    /// Always empty: this runtime does not keep a script.
    public String getScript() {
        return "";
    }

    /// Whether the locale carries extensions; never, here.
    public boolean hasExtensions() {
        return false;
    }

    /// The locale without extensions, which is the locale itself here.
    public Locale stripExtensions() {
        return this;
    }

    /// The locale an IETF BCP 47 language tag names: `fr`, `en-US`,
    /// `zh-Hans-CN`. The first subtag is the language, a four letter subtag
    /// after it a script (dropped), the next two letter or three digit subtag
    /// the country and the subtags after it, five to eight characters or a
    /// digit and three more, the variant. A single character subtag starts
    /// the extensions, which are dropped. `und` and the empty tag are the
    /// root locale.
    public static Locale forLanguageTag(String languageTag) {
        if (languageTag == null) {
            throw new NullPointerException();
        }
        String language = "";
        String country = "";
        StringBuilder variant = null;
        int n = languageTag.length();
        int start = 0;
        int index = 0;
        int extlangs = 0;
        int baseLanguageLength = 0;
        for (int i = 0; i <= n; i++) {
            // Only '-' separates subtags: "en_US" is not a language tag, and the
            // JDK reads it as the root locale.
            if (i < n && languageTag.charAt(i) != '-') {
                continue;
            }
            String part = languageTag.substring(start, i);
            start = i + 1;
            if (index == 0) {
                if (!isLetters(part) || part.length() < 2 || part.length() > 8) {
                    break;
                }
                language = "und".equals(asciiLower(part)) ? "" : part;
                baseLanguageLength = part.length();
            } else if (part.length() == 1 || part.length() == 0) {
                // "x-lvariant-" carries a variant that is not well formed BCP 47,
                // which is how toLanguageTag writes one; the JDK reads it back.
                String rest = i < n ? languageTag.substring(i + 1) : "";
                if (part.length() == 1 && (part.charAt(0) == 'x' || part.charAt(0) == 'X')
                        && rest.length() > 9 && rest.regionMatches(true, 0, "lvariant", 0, 8)
                        && (rest.charAt(8) == '-' || rest.charAt(8) == '_')) {
                    String lvariant = rest.substring(9).replace('-', '_');
                    if (variant == null) {
                        variant = new StringBuilder(lvariant);
                    } else {
                        variant.append('_').append(lvariant);
                    }
                }
                break;
            } else if (index == 1 && extlangs < 3 && part.length() == 3 && isLetters(part)
                    && baseLanguageLength >= 2 && baseLanguageLength <= 3) {
                // An extended language subtag ("zh-cmn-Hans-CN"): the JDK takes
                // the first as the language and goes on to the script and
                // region. The index stays put so a script may still follow.
                if (extlangs == 0) {
                    language = part;
                }
                extlangs++;
                continue;
            } else if (index == 1 && part.length() == 4 && isLetters(part)) {
                // a script
                index++;
                continue;
            } else if (country.length() == 0 && variant == null
                    && ((part.length() == 2 && isLetters(part)) || (part.length() == 3 && isDigits(part)))) {
                country = part;
            } else if (part.length() >= 5 && part.length() <= 8 && isAlphanumeric(part)
                    || part.length() == 4 && part.charAt(0) >= '0' && part.charAt(0) <= '9' && isAlphanumeric(part)) {
                if (variant == null) {
                    variant = new StringBuilder(part);
                } else {
                    variant.append('_').append(part);
                }
            } else {
                break;
            }
            index++;
        }
        return new Locale(language, country, variant == null ? "" : variant.toString());
    }

    /// The IETF BCP 47 tag of the locale: `en-US`, `fr`, or `und` for the
    /// root.
    public String toLanguageTag() {
        StringBuilder out = new StringBuilder();
        // An ill-formed language or region (the constructors take anything)
        // is left out of the tag, as the JDK does: "und" for the language,
        // nothing for the region.
        boolean wellFormedLanguage = language.length() >= 2 && language.length() <= 8 && isLetters(language);
        out.append(wellFormedLanguage ? language : "und");
        if (country.length() == 2 && isLetters(country) || country.length() == 3 && isDigits(country)) {
            out.append('-').append(country);
        }
        if (variant.length() > 0) {
            // As JDK 8 writes it: the leading well-formed variant subtags as
            // they are, the rest private-use ("POSIX_AB" is
            // "POSIX-x-lvariant-AB"), stopping at the first part that cannot
            // be a subtag at all ("a b", nine letters), which is left out
            // with everything after it.
            List<String> split = new ArrayList<String>();
            int from = 0;
            for (int j = 0; j <= variant.length(); j++) {
                if (j == variant.length() || variant.charAt(j) == '_' || variant.charAt(j) == '-') {
                    split.add(variant.substring(from, j));
                    from = j + 1;
                }
            }
            String[] parts = split.toArray(new String[split.size()]);
            int i = 0;
            while (i < parts.length && isWellFormedVariant(parts[i])) {
                out.append('-').append(parts[i]);
                i++;
            }
            StringBuilder rest = new StringBuilder();
            while (i < parts.length && parts[i].length() > 0 && isPrivateUseText(parts[i])) {
                rest.append('-').append(parts[i]);
                i++;
            }
            if (rest.length() > 0) {
                out.append("-x-lvariant").append(rest);
            }
        }
        return out.toString();
    }

    /// Whether every `-` separated part is 1 to 8 letters or digits, which
    /// a private-use subtag requires.
    private static boolean isPrivateUseText(String tagged) {
        int start = 0;
        int n = tagged.length();
        for (int i = 0; i <= n; i++) {
            if (i < n && tagged.charAt(i) != '-') {
                continue;
            }
            String part = tagged.substring(start, i);
            start = i + 1;
            if (part.length() < 1 || part.length() > 8 || !isAlphanumeric(part)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWellFormedVariant(String tagged) {
        int start = 0;
        int n = tagged.length();
        for (int i = 0; i <= n; i++) {
            if (i < n && tagged.charAt(i) != '-') {
                continue;
            }
            String part = tagged.substring(start, i);
            start = i + 1;
            boolean ok = part.length() >= 5 && part.length() <= 8 && isAlphanumeric(part)
                    || part.length() == 4 && part.charAt(0) >= '0' && part.charAt(0) <= '9' && isAlphanumeric(part);
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /// Whether `obj` is a locale of the same language, country and variant.
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Locale)) {
            return false;
        }
        Locale other = (Locale) obj;
        return language.equals(other.language) && country.equals(other.country) && variant.equals(other.variant);
    }

    /// A hash of the language, country and variant.
    public int hashCode() {
        return (language.hashCode() * 31 + country.hashCode()) * 31 + variant.hashCode();
    }

    /// `en_US`, `fr`, `en_US_POSIX`, or the empty string for the root locale.
    public String toString() {
        boolean l = language.length() > 0;
        boolean c = country.length() > 0;
        boolean v = variant.length() > 0;
        if (!c && !(l && v)) {
            return language;
        }
        StringBuilder out = new StringBuilder(language);
        out.append('_');
        if (c) {
            out.append(country);
        }
        if (v && (l || c)) {
            out.append('_').append(variant);
        }
        return out.toString();
    }

    /// The English name of the language, or its code when this runtime has
    /// no name for it.
    public String getDisplayLanguage() {
        return displayLanguage(language);
    }

    /// As `getDisplayLanguage()`: names are in English only.
    public String getDisplayLanguage(Locale inLocale) {
        if (inLocale == null) {
            throw new NullPointerException();
        }
        return getDisplayLanguage();
    }

    /// The English name of the country, or its code when this runtime has no
    /// name for it.
    public String getDisplayCountry() {
        return displayCountry(country);
    }

    /// As `getDisplayCountry()`: names are in English only.
    public String getDisplayCountry(Locale inLocale) {
        if (inLocale == null) {
            throw new NullPointerException();
        }
        return getDisplayCountry();
    }

    /// The variant as it is.
    public String getDisplayVariant() {
        return variant;
    }

    /// As `getDisplayVariant()`.
    public String getDisplayVariant(Locale inLocale) {
        if (inLocale == null) {
            throw new NullPointerException();
        }
        return getDisplayVariant();
    }

    /// Always empty: this runtime does not keep a script.
    public String getDisplayScript() {
        return "";
    }

    /// Always empty: this runtime does not keep a script.
    public String getDisplayScript(Locale inLocale) {
        if (inLocale == null) {
            throw new NullPointerException();
        }
        return "";
    }

    /// `English (United States)`: the language, then the country and the
    /// variant in parentheses.
    public String getDisplayName() {
        String lang = getDisplayLanguage();
        StringBuilder qualifiers = new StringBuilder();
        if (country.length() > 0) {
            qualifiers.append(getDisplayCountry());
        }
        if (variant.length() > 0) {
            if (qualifiers.length() > 0) {
                qualifiers.append(", ");
            }
            qualifiers.append(variant);
        }
        if (lang.length() == 0) {
            return qualifiers.toString();
        }
        if (qualifiers.length() == 0) {
            return lang;
        }
        return lang + " (" + qualifiers + ")";
    }

    /// As `getDisplayName()`: names are in English only.
    public String getDisplayName(Locale inLocale) {
        if (inLocale == null) {
            throw new NullPointerException();
        }
        return getDisplayName();
    }

    /// The default locale and the locales this class has constants for.
    public static Locale[] getAvailableLocales() {
        return new Locale[] {
            getDefault(), ENGLISH, FRENCH, GERMAN, ITALIAN, JAPANESE, KOREAN, CHINESE,
            SIMPLIFIED_CHINESE, TRADITIONAL_CHINESE, FRANCE, GERMANY, ITALY, JAPAN, KOREA, UK, US, CANADA,
            CANADA_FRENCH
        };
    }

    /// The English names, in a holder so that the tables are built the
    /// first time a display name is asked for, not by every use of a
    /// `Locale`.
    private static final class Names {
        private Names() {
        }

        private static final String[] LANGUAGE_NAMES = {
            "ar", "Arabic", "bg", "Bulgarian", "ca", "Catalan", "cs", "Czech", "da", "Danish",
            "de", "German", "el", "Greek", "en", "English", "es", "Spanish", "et", "Estonian",
            "fa", "Persian", "fi", "Finnish", "fr", "French", "he", "Hebrew", "hi", "Hindi",
            "hr", "Croatian", "hu", "Hungarian", "id", "Indonesian", "is", "Icelandic", "it", "Italian",
            "ja", "Japanese", "ko", "Korean", "lt", "Lithuanian", "lv", "Latvian", "ms", "Malay",
            "nl", "Dutch", "no", "Norwegian", "pl", "Polish", "pt", "Portuguese", "ro", "Romanian",
            "ru", "Russian", "sk", "Slovak", "sl", "Slovenian", "sr", "Serbian", "sv", "Swedish",
            "th", "Thai", "tr", "Turkish", "uk", "Ukrainian", "vi", "Vietnamese", "zh", "Chinese"
        };

        private static final String[] COUNTRY_NAMES = {
            "AR", "Argentina", "AT", "Austria", "AU", "Australia", "BE", "Belgium", "BR", "Brazil",
            "CA", "Canada", "CH", "Switzerland", "CN", "China", "CZ", "Czechia", "DE", "Germany",
            "DK", "Denmark", "ES", "Spain", "FI", "Finland", "FR", "France", "GB", "United Kingdom",
            "GR", "Greece", "HK", "Hong Kong SAR China", "IE", "Ireland", "IL", "Israel", "IN", "India",
            "IT", "Italy", "JP", "Japan", "KR", "South Korea", "MX", "Mexico", "NL", "Netherlands",
            "NO", "Norway", "NZ", "New Zealand", "PL", "Poland", "PT", "Portugal", "RU", "Russia",
            "SE", "Sweden", "TR", "Turkey", "TW", "Taiwan", "UA", "Ukraine", "US", "United States",
            "ZA", "South Africa"
        };
    }

    private static String displayLanguage(String code) {
        return lookup(Names.LANGUAGE_NAMES, code);
    }

    private static String displayCountry(String code) {
        return lookup(Names.COUNTRY_NAMES, code);
    }

    private static String lookup(String[] table, String code) {
        for (int i = 0; i < table.length; i += 2) {
            if (table[i].equals(code)) {
                return table[i + 1];
            }
        }
        return code;
    }

    private static String normalizeLanguage(String language) {
        String lower = asciiLower(language);
        // The JDK answers the current ISO 639 code for the three it renamed.
        if ("iw".equals(lower)) {
            return "he";
        }
        if ("ji".equals(lower)) {
            return "yi";
        }
        if ("in".equals(lower)) {
            return "id";
        }
        return lower;
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

    private static boolean isAlphanumeric(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'))) {
                return false;
            }
        }
        return s.length() > 0;
    }

    // Language and country codes are ASCII, and String.toLowerCase folds by
    // the device's locale: under Turkish "IT" would not become "it".
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
