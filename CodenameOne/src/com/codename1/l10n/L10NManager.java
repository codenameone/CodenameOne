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

import java.util.Date;

/**
 * The localization manager allows adapting values for display in different locales
 *
 * @author Shai Almog
 */
public class L10NManager {
    private String language;
    private String locale;
    
    /**
     * Instances of this class should be received via the Display class
     */
    protected L10NManager(String language, String locale) {
        this.language = language;
        this.locale = locale;
    }
    
    /**
     * Returns the current locale language
     * 
     * @return iso language string
     */
    public String getLanguage() {
        return language;
    }
    
    /**
     * Forces the locale/language
     * 
     * @param locale the new locale
     * @param language the language to use
     */
    public void setLocale(String locale, String language) {
        this.language = language;
        this.locale = locale;
    }
    
    
    /**
     * Format an integer number for this locale
     * 
     * @param number the number to format
     * @return a string representation of a number
     */
    public String format(int number) {
        return "" + number;
    }

    /**
     * Format a double number for this locale
     * 
     * @param number the number to format
     * @return a string representation of a number
     */
    public String format(double number) {
        return "" + number;
    }

    /**
     * Format a currency value
     * 
     * @param currency the monetary value
     * @return a string representation of a number
     */
    public String formatCurrency(double currency) {
        return "" + currency;
    }

    /**
     * Returns the currency symbol for this locale
     * 
     * @return currency symbol
     */
    public String getCurrencySymbol() {
        return "$";
    }
    
    /**
     * Formats a date in a long form e.g. Sunday January 1st 2001
     * @param d the date
     * @return the long form string
     */
    public String formatDateLongStyle(Date d) {
        return d.toString();
    }

    /**
     * Formats a date in a short form e.g. 1/1/2011
     * @param d the date
     * @return the short form string
     */
    public String formatDateShortStyle(Date d) {
        return d.toString();
    }

    /**
     * Formats a date and a time in a default form e.g. 1/1/2011 10:00AM
     * @param d the date
     * @return the date and time
     */
    public String formatDateTime(Date d) {
        return d.toString();
    }

    /**
     * Formats a date and a time in a default form e.g. 1/1/2011 10:00AM
     * @param d the date
     * @return the date and time
     */
    public String formatDateTimeMedium(Date d) {
        return d.toString();
    }

    /**
     * Formats a date and a time in a default form e.g. 1/1/2011 10:00AM
     * @param d the date
     * @return the date and time
     */
    public String formatDateTimeShort(Date d) {
        return d.toString();
    }

    /**
     * Indicates whether the language is a right to left language
     * 
     * @return true for bidi/rtl languages
     */
    public boolean isRTLLocale() {
        return "iw".equals(language) || "ar".equals(language);
    }

    /**
     * Determines the locale (location)
     * 
     * @return the locale
     */
    public String getLocale() {
        return locale;
    }
}
