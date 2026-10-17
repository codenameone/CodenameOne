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
package java.text;

import com.codename1.impl.JdkHost;

import java.util.Locale;

/// The characters a [DecimalFormat] writes and reads -- decimal and
/// grouping separators, minus sign, percent, currency symbol and so on.
///
/// #### Where the symbols come from
///
/// The device library carries no locale data of its own. What it knows about
/// the user's locale -- the decimal and grouping separators and the currency
/// symbol -- it is told by the platform, which on a device reads them off
/// numbers Codename One's `L10NManager` formatted. Everything the platform
/// cannot tell (percent, per mille, the exponent separator, the infinity and
/// not-a-number texts) is as in the US English locale.
///
/// A request that names `en_US` gets the US English symbols outright. Any
/// other locale gets the device's symbols, since the device cannot format for
/// a locale other than its own. Before Codename One is initialized, and where
/// the platform answers nothing usable, the symbols are US English.
public class DecimalFormatSymbols implements Cloneable, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private char zeroDigit = '0';
    private char groupingSeparator = ',';
    private char decimalSeparator = '.';
    private char perMill = (char) 0x2030;
    private char percent = '%';
    private char digit = '#';
    private char patternSeparator = ';';
    private String infinity = String.valueOf((char) 0x221E);
    private String nan = String.valueOf((char) 0xFFFD);
    private char minusSign = '-';
    private String currencySymbol = "$";
    private String intlCurrencySymbol = "USD";
    private char monetarySeparator = '.';
    private String exponentSeparator = "E";

    public DecimalFormatSymbols() {
        this(Locale.getDefault());
    }

    public DecimalFormatSymbols(Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        if (!isUsEnglish(locale)) {
            readDeviceSymbols();
        }
    }

    public static final DecimalFormatSymbols getInstance() {
        return new DecimalFormatSymbols();
    }

    public static final DecimalFormatSymbols getInstance(Locale locale) {
        return new DecimalFormatSymbols(locale);
    }

    private static boolean isUsEnglish(Locale locale) {
        return "en".equals(locale.getLanguage()) && "US".equals(locale.getCountry());
    }

    private void readDeviceSymbols() {
        String[] device;
        try {
            device = JdkHost.get().localeNumberSymbols();
        } catch (RuntimeException e) {
            // A port whose localization cannot answer leaves the US English
            // symbols in place, which is the documented fallback.
            return;
        }
        if (device == null || device.length < 3) {
            return;
        }
        String decimal = device[0];
        if (decimal != null && decimal.length() == 1) {
            decimalSeparator = decimal.charAt(0);
            monetarySeparator = decimalSeparator;
        }
        String grouping = device[1];
        if (grouping != null && grouping.length() == 1 && grouping.charAt(0) != decimalSeparator) {
            groupingSeparator = grouping.charAt(0);
        } else if (decimalSeparator == ',') {
            // A locale that writes a decimal comma and no grouping on this
            // platform still must not group with the same character.
            groupingSeparator = '.';
        }
        String currency = device[2];
        if (currency != null && currency.length() > 0) {
            currencySymbol = currency;
            intlCurrencySymbol = currency;
        }
    }

    public char getZeroDigit() {
        return zeroDigit;
    }

    public void setZeroDigit(char zeroDigit) {
        this.zeroDigit = zeroDigit;
    }

    public char getGroupingSeparator() {
        return groupingSeparator;
    }

    public void setGroupingSeparator(char groupingSeparator) {
        this.groupingSeparator = groupingSeparator;
    }

    public char getDecimalSeparator() {
        return decimalSeparator;
    }

    public void setDecimalSeparator(char decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
    }

    public char getPerMill() {
        return perMill;
    }

    public void setPerMill(char perMill) {
        this.perMill = perMill;
    }

    public char getPercent() {
        return percent;
    }

    public void setPercent(char percent) {
        this.percent = percent;
    }

    public char getDigit() {
        return digit;
    }

    public void setDigit(char digit) {
        this.digit = digit;
    }

    public char getPatternSeparator() {
        return patternSeparator;
    }

    public void setPatternSeparator(char patternSeparator) {
        this.patternSeparator = patternSeparator;
    }

    public String getInfinity() {
        return infinity;
    }

    public void setInfinity(String infinity) {
        this.infinity = infinity;
    }

    public String getNaN() {
        return nan;
    }

    public void setNaN(String nan) {
        this.nan = nan;
    }

    public char getMinusSign() {
        return minusSign;
    }

    public void setMinusSign(char minusSign) {
        this.minusSign = minusSign;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public void setCurrencySymbol(String currency) {
        currencySymbol = currency;
    }

    public String getInternationalCurrencySymbol() {
        return intlCurrencySymbol;
    }

    public void setInternationalCurrencySymbol(String currencyCode) {
        intlCurrencySymbol = currencyCode;
    }

    public char getMonetaryDecimalSeparator() {
        return monetarySeparator;
    }

    public void setMonetaryDecimalSeparator(char sep) {
        monetarySeparator = sep;
    }

    public String getExponentSeparator() {
        return exponentSeparator;
    }

    public void setExponentSeparator(String exp) {
        if (exp == null) {
            throw new NullPointerException();
        }
        exponentSeparator = exp;
    }

    /// A copy of these symbols. Written out field by field: `Object.clone()`
    /// answers `null` on the device for anything but an array.
    @Override
    public Object clone() {
        DecimalFormatSymbols copy = new DecimalFormatSymbols(new Locale("en", "US"));
        copy.zeroDigit = zeroDigit;
        copy.groupingSeparator = groupingSeparator;
        copy.decimalSeparator = decimalSeparator;
        copy.perMill = perMill;
        copy.percent = percent;
        copy.digit = digit;
        copy.patternSeparator = patternSeparator;
        copy.infinity = infinity;
        copy.nan = nan;
        copy.minusSign = minusSign;
        copy.currencySymbol = currencySymbol;
        copy.intlCurrencySymbol = intlCurrencySymbol;
        copy.monetarySeparator = monetarySeparator;
        copy.exponentSeparator = exponentSeparator;
        return copy;
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DecimalFormatSymbols)) {
            return false;
        }
        DecimalFormatSymbols other = (DecimalFormatSymbols) obj;
        return zeroDigit == other.zeroDigit
                && groupingSeparator == other.groupingSeparator
                && decimalSeparator == other.decimalSeparator
                && percent == other.percent
                && perMill == other.perMill
                && digit == other.digit
                && minusSign == other.minusSign
                && patternSeparator == other.patternSeparator
                && monetarySeparator == other.monetarySeparator
                && same(infinity, other.infinity)
                && same(nan, other.nan)
                && same(currencySymbol, other.currencySymbol)
                && same(intlCurrencySymbol, other.intlCurrencySymbol)
                && same(exponentSeparator, other.exponentSeparator);
    }

    @Override
    public int hashCode() {
        int result = zeroDigit;
        result = result * 37 + groupingSeparator;
        result = result * 37 + decimalSeparator;
        return result;
    }
}
