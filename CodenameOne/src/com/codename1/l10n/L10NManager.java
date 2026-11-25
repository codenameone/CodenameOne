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
package com.codename1.l10n;

import com.codename1.io.Util;
import com.codename1.ui.Display;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

/// The localization manager allows adapting values for display in different locales thru parsing and formatting
/// capabilities (similar to JavaSE's DateFormat/NumberFormat). It also includes language/locale/currency
/// related API's similar to Locale/currency API's from JavaSE.
///
/// The sample code below just lists the various capabilities of the API:
///
/// ```java
/// Form hi = new Form("L10N", new TableLayout(16, 2));
/// L10NManager l10n = L10NManager.getInstance();
/// hi.add("format(double)").add(l10n.format(11.11)).
///     add("format(int)").add(l10n.format(33)).
///     add("formatCurrency").add(l10n.formatCurrency(53.267)).
///     add("formatDateLongStyle").add(l10n.formatDateLongStyle(new Date())).
///     add("formatDateShortStyle").add(l10n.formatDateShortStyle(new Date())).
///     add("formatDateTime").add(l10n.formatDateTime(new Date())).
///     add("formatDateTimeMedium").add(l10n.formatDateTimeMedium(new Date())).
///     add("formatDateTimeShort").add(l10n.formatDateTimeShort(new Date())).
///     add("getCurrencySymbol").add(l10n.getCurrencySymbol()).
///     add("getLanguage").add(l10n.getLanguage()).
///     add("getLocale").add(l10n.getLocale()).
///     add("isRTLLocale").add("" + l10n.isRTLLocale()).
///     add("parseCurrency").add(l10n.formatCurrency(l10n.parseCurrency("33.77$"))).
///     add("parseDouble").add(l10n.format(l10n.parseDouble("34.35"))).
///     add("parseInt").add(l10n.format(l10n.parseInt("56"))).
///     add("parseLong").add("" + l10n.parseLong("4444444"));
/// hi.show();
/// ```
///
/// #### Currencies
///
/// [#getCurrencyCode()] is the ISO 4217 code of the user's currency, and
/// [#formatCurrency(double, String)] formats an amount in any currency the way
/// the user's locale writes it: a price in euros shown to a user in the US
/// reads `\u20ac12.50` there and `12,50 \u20ac` in France. Use the standard
/// `java.util.Currency` for the facts about a currency itself -- its fraction
/// digits, numeric code and symbol -- which are the same on every platform.
///
/// ```java
/// L10NManager l10n = L10NManager.getInstance();
/// String mine = l10n.getCurrencyCode();               // "USD" for a user in the US
/// String price = l10n.formatCurrency(1250, "JPY");    // "\u00a51,250"
/// int digits = Currency.getInstance("JPY").getDefaultFractionDigits(); // 0
/// ```
public class L10NManager {
    private String language;
    private String locale;
    private DateFormatSymbols symbols;

    /// Instances of this class should be received via the Display class
    protected L10NManager(String language, String locale) {
        this.language = language;
        this.locale = locale;
    }

    /// Convenience method that invokes Display.getLocalizationManager()
    ///
    /// #### Returns
    ///
    /// the L10NManager instance
    public static L10NManager getInstance() {
        return Display.getInstance().getLocalizationManager();
    }

    private DateFormatSymbols getSymbols() {
        if (symbols == null) {
            symbols = new DateFormatSymbols();
        }
        return symbols;
    }

    /// Returns the current locale language as an ISO 639 two letter code
    ///
    /// #### Returns
    ///
    /// iso language string
    public String getLanguage() {
        return language;
    }

    /// Forces the locale/language
    ///
    /// #### Parameters
    ///
    /// - `locale`: the new locale
    ///
    /// - `language`: the language to use
    public void setLocale(String locale, String language) {
        this.language = language;
        this.locale = locale;
    }

    /// Format an integer number for this locale
    ///
    /// #### Parameters
    ///
    /// - `number`: the number to format
    ///
    /// #### Returns
    ///
    /// a string representation of a number
    public String format(int number) {
        return "" + number;
    }

    /// Format a double number for this locale
    ///
    /// #### Parameters
    ///
    /// - `number`: the number to format
    ///
    /// #### Returns
    ///
    /// a string representation of a number
    public String format(double number) {
        return "" + number;
    }

    /// Gets the short month name in the current locale for the given date.
    ///
    /// #### Parameters
    ///
    /// - `date`: The date.
    ///
    /// #### Returns
    ///
    /// Short month name.  E.g. Jan, Feb, etc..
    ///
    public String getShortMonthName(Date date) {
        return limitLength(getLongMonthName(date), 3);
    }

    /// Gets the long month name in the current locale for the given date.
    ///
    /// #### Parameters
    ///
    /// - `date`: The date.
    ///
    /// #### Returns
    ///
    /// Long month name.  E.g. January, February, etc..
    ///
    public String getLongMonthName(Date date) {
        String fmt = formatDateLongStyle(date);
        try {
            return extractMonthName(fmt, formatDateLongStyle(neighbourInMonth(date)));
        } catch (ParseException ex) {
            // the month of the date asked about, not of today
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.setTime(date);
            int v = c.get(java.util.Calendar.MONTH) - java.util.Calendar.JANUARY;

            return getSymbols().getMonths()[v];
        }
    }

    private String limitLength(String s, int len) {
        if (s.length() > len) {
            return s.substring(0, len);
        }
        return s;
    }

    /// The day next to `date` in the same month, at noon so a daylight saving shift cannot
    /// carry it into another day: formatted with the same style it differs from `date` only in
    /// the day number and the weekday name.
    private static Date neighbourInMonth(Date date) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTime(date);
        c.set(java.util.Calendar.HOUR_OF_DAY, 12);
        long noon = c.getTime().getTime();
        long day = 24L * 60 * 60 * 1000;
        return new Date(c.get(java.util.Calendar.DAY_OF_MONTH) == 1 ? noon + day : noon - day);
    }

    /// The device-independent fallback for a style that names no weekday differently on two
    /// neighbouring days (a constant or custom format): the English names that
    /// `Date.toString()` uses, which is the only text that symbol table describes.
    private boolean isWeekdayName(String part) {
        DateFormatSymbols sym = getSymbols();
        return contains(sym.getShortWeekdays(), part) || contains(sym.getWeekdays(), part);
    }

    private static boolean contains(String[] names, String part) {
        for (String n : names) {
            if (n != null && n.length() > 0 && part.equalsIgnoreCase(n)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsWord(String[] words, String part) {
        for (String w : words) {
            if (part.equals(trimPunctuation(w))) {
                return true;
            }
        }
        return false;
    }

    private static String trimPunctuation(String s) {
        int end = s.length();
        // only separators: an abbreviation's own period ("janv.", "Jan.") is part of the name
        while (end > 0 && ",;".indexOf(s.charAt(end - 1)) >= 0) {
            end--;
        }
        return s.substring(0, end);
    }

    /// Picks the month out of a long-style date. The weekday is found structurally: the same
    /// style applied to a neighbouring day of the same month keeps the month word and changes
    /// the weekday word, in whatever language the device formats in ("lundi" / "mardi",
    /// "Montag," / "Dienstag,"), so a word that differs between the two is never the month.
    private String extractMonthName(String dateStr, String neighbour) throws ParseException {
        String[] parts = Util.split(dateStr, " ");
        String[] other = neighbour == null ? new String[0] : Util.split(neighbour, " ");
        boolean first = true;
        for (String raw : parts) {
            // long styles punctuate their words ("lundi," "Montag,"): compare the bare word
            String part = trimPunctuation(raw);
            if (part.length() == 0) {
                continue;
            }
            boolean leading = first;
            first = false;
            // a word that changes on the neighbouring day is the weekday, wherever it sits. The
            // English symbol fallback applies only to the LEADING word (Date.toString() opens
            // with one): a short weekday can equal a short month (Spanish "mar" is both martes
            // and marzo), so a later word is never matched against a name table
            if ("de".equalsIgnoreCase(part) || !containsWord(other, part)
                    || leading && isWeekdayName(part)) {
                continue;
            }
            String firstChar = part.substring(0, 1);
            if (!firstChar.toLowerCase().equals(firstChar.toUpperCase())) {
                return part;
            }
        }
        throw new ParseException("Cannot extract month from string", 0);

    }

    /// Format a currency value in the user's currency ([#getCurrencyCode()]).
    ///
    /// #### Parameters
    ///
    /// - `currency`: the monetary value
    ///
    /// #### Returns
    ///
    /// a string representation of a number
    public String formatCurrency(double currency) {
        String code = getCurrencyCode();
        if (code == null) {
            return "" + currency;
        }
        return formatCurrency(currency, code);
    }

    /// Formats an amount of the given currency the way this locale writes
    /// money: its separators and its placement of the symbol, with the
    /// currency's own number of fraction digits (none for the yen).
    ///
    /// The default implementation uses `java.text.NumberFormat`; the ports
    /// replace it with the platform's own currency formatting.
    ///
    /// #### Parameters
    ///
    /// - `amount`: the monetary value
    ///
    /// - `currencyCode`: an ISO 4217 code such as `"EUR"`
    ///
    /// #### Returns
    ///
    /// the formatted amount
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `currencyCode` is not an ISO 4217 code
    public String formatCurrency(double amount, String currencyCode) {
        Currency c = Currency.getInstance(currencyCode);
        NumberFormat format = NumberFormat.getCurrencyInstance(toJavaLocale());
        format.setCurrency(c);
        int digits = c.getDefaultFractionDigits();
        if (digits >= 0) {
            format.setMinimumFractionDigits(digits);
            format.setMaximumFractionDigits(digits);
        }
        return format.format(amount);
    }

    /// Returns the currency symbol for this locale
    ///
    /// #### Returns
    ///
    /// currency symbol
    public String getCurrencySymbol() {
        String code = getCurrencyCode();
        if (code == null) {
            return "$";
        }
        return getCurrencySymbol(code);
    }

    /// The symbol this locale writes for the given currency: `$` for US
    /// dollars in the US, `US$` elsewhere.
    ///
    /// #### Parameters
    ///
    /// - `currencyCode`: an ISO 4217 code such as `"EUR"`
    ///
    /// #### Returns
    ///
    /// the currency symbol
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `currencyCode` is not an ISO 4217 code
    public String getCurrencySymbol(String currencyCode) {
        return Currency.getInstance(currencyCode).getSymbol(toJavaLocale());
    }

    /// The ISO 4217 code of the user's currency, such as `"USD"`: the one the
    /// platform reports, or by default the currency of the country in
    /// [#getLocale()].
    ///
    /// #### Returns
    ///
    /// the currency code, or null when the locale names no country
    public String getCurrencyCode() {
        String country = getLocale();
        if (country == null || country.length() != 2) {
            return null;
        }
        try {
            Currency c = Currency.getInstance(toJavaLocale());
            return c == null ? null : c.getCurrencyCode();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /// This manager's language and country as a `java.util.Locale`.
    private Locale toJavaLocale() {
        String lang = getLanguage();
        String country = getLocale();
        return new Locale(lang == null ? "" : lang, country == null ? "" : asciiUpper(country));
    }

    /// Upper-cases ASCII letters only: a country code is ASCII by
    /// specification, and `toUpperCase()` depends on the device locale.
    private static String asciiUpper(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            b.append(c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
        }
        return b.toString();
    }

    /// Formats a date in a long form e.g. Sunday January 1st 2001
    ///
    /// #### Parameters
    ///
    /// - `d`: the date
    ///
    /// #### Returns
    ///
    /// the long form string
    public String formatDateLongStyle(Date d) {
        return d.toString();
    }

    /// Formats a date in a short form e.g. 1/1/2011
    ///
    /// #### Parameters
    ///
    /// - `d`: the date
    ///
    /// #### Returns
    ///
    /// the short form string
    public String formatDateShortStyle(Date d) {
        return d.toString();
    }

    /// Formats a date and a time in a default form e.g. 1/1/2011 10:00AM
    ///
    /// #### Parameters
    ///
    /// - `d`: the date
    ///
    /// #### Returns
    ///
    /// the date and time
    public String formatDateTime(Date d) {
        return d.toString();
    }

    /// Formats a date and a time in a default form e.g. 1/1/2011 10:00AM
    ///
    /// #### Parameters
    ///
    /// - `d`: the date
    ///
    /// #### Returns
    ///
    /// the date and time
    public String formatDateTimeMedium(Date d) {
        return d.toString();
    }

    /// Formats the time in a default form e.g. 10:00AM
    ///
    /// #### Parameters
    ///
    /// - `d`: the date time object
    ///
    /// #### Returns
    ///
    /// the time as a String
    public String formatTime(Date d) {
        String s = formatDateTimeMedium(d);
        int pos = s.lastIndexOf(" ");

        // this can be just the AM or PM
        if (s.length() - pos < 4) {
            pos = s.lastIndexOf(" ", pos - 1);
        }
        s = s.substring(pos + 1);
        return s;
    }

    /// Formats a date and a time in a default form e.g. 1/1/2011 10:00AM
    ///
    /// #### Parameters
    ///
    /// - `d`: the date
    ///
    /// #### Returns
    ///
    /// the date and time
    public String formatDateTimeShort(Date d) {
        return d.toString();
    }

    /// Indicates whether the language is a right to left language
    ///
    /// #### Returns
    ///
    /// true for bidi/rtl languages
    public boolean isRTLLocale() {
        return "iw".equals(language) || "ar".equals(language);
    }

    /// Determines the locale (location) as an ISO 3166 country code
    ///
    /// #### Returns
    ///
    /// the locale
    public String getLocale() {
        return locale;
    }

    /// Formats a number as a String with a fixed number of decimal places
    ///
    /// #### Parameters
    ///
    /// - `number`: the number
    ///
    /// - `decimalPlaces`: decimals
    ///
    /// #### Returns
    ///
    /// formatted string
    public String format(double number, int decimalPlaces) {
        if (decimalPlaces == 0) {
            return format((double) ((long) number));
        }

        double pos = 10;
        for (int iter = 1; iter < decimalPlaces; iter++) {
            pos *= 10;
        }
        long ln = Math.round(number * pos);
        number = ((double) ln) / pos;
        return format(number);
    }

    /// Parses a double based on locale conventions
    ///
    /// #### Parameters
    ///
    /// - `localeFormattedDecimal`: the locale formatted number
    ///
    /// #### Returns
    ///
    /// the parsed double
    public double parseDouble(String localeFormattedDecimal) {
        return Double.parseDouble(localeFormattedDecimal);
    }

    /// Parses a long based on locale conventions
    ///
    /// #### Parameters
    ///
    /// - `localeFormattedLong`: the number
    ///
    /// #### Returns
    ///
    /// a long
    public long parseLong(String localeFormattedLong) {
        return Long.parseLong(localeFormattedLong);
    }

    /// Parses an integer based on locale conventions
    ///
    /// #### Parameters
    ///
    /// - `localeFormattedInteger`: the number
    ///
    /// #### Returns
    ///
    /// a parsed number
    public int parseInt(String localeFormattedInteger) {
        return Integer.parseInt(localeFormattedInteger);
    }

    /// Parses the currency value
    ///
    /// #### Parameters
    ///
    /// - `amount`: the amount
    ///
    /// #### Returns
    ///
    /// a numeric value for the currency
    public double parseCurrency(String amount) {
        StringBuilder b = new StringBuilder();
        int l = amount.length();
        for (int iter = 0; iter < l; iter++) {
            char c = amount.charAt(iter);
            if (Character.isDigit(c) || c == '.' || c == ',' || c == '-') {
                b.append(c);
            }
        }
        return parseDouble(b.toString());
    }
}
