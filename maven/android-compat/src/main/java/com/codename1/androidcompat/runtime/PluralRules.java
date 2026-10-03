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
package com.codename1.androidcompat.runtime;

/// CLDC plural categories for `getQuantityString`. Covers the languages whose
/// integer rules differ from "one when n == 1"; everything else uses that.
/// Category codes match the order the build writes plurals in:
/// zero, one, two, few, many, other.
public final class PluralRules {

    public static final int ZERO = 0;
    public static final int ONE = 1;
    public static final int TWO = 2;
    public static final int FEW = 3;
    public static final int MANY = 4;
    public static final int OTHER = 5;

    private PluralRules() {
    }

    public static int select(String lang, int n) {
        int abs = n < 0 ? -n : n;
        int mod10 = abs % 10;
        int mod100 = abs % 100;
        if (lang == null) {
            lang = "en";
        }
        if (lang.equals("ja") || lang.equals("zh") || lang.equals("ko") || lang.equals("vi") || lang.equals("th")
                || lang.equals("id") || lang.equals("ms") || lang.equals("tr") || lang.equals("fa")) {
            return lang.equals("tr") && abs == 1 ? ONE : OTHER;
        }
        if (lang.equals("fr") || lang.equals("pt") || lang.equals("hi") || lang.equals("bn")) {
            return abs == 0 || abs == 1 ? ONE : (abs % 1000000 == 0 && lang.equals("fr") ? MANY : OTHER);
        }
        if (lang.equals("ru") || lang.equals("uk") || lang.equals("be")) {
            if (mod10 == 1 && mod100 != 11) {
                return ONE;
            }
            if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
                return FEW;
            }
            return MANY;
        }
        if (lang.equals("pl")) {
            if (abs == 1) {
                return ONE;
            }
            if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
                return FEW;
            }
            return MANY;
        }
        if (lang.equals("cs") || lang.equals("sk")) {
            if (abs == 1) {
                return ONE;
            }
            if (abs >= 2 && abs <= 4) {
                return FEW;
            }
            return OTHER;
        }
        if (lang.equals("hr") || lang.equals("sr") || lang.equals("bs")) {
            if (mod10 == 1 && mod100 != 11) {
                return ONE;
            }
            if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
                return FEW;
            }
            return OTHER;
        }
        if (lang.equals("ro")) {
            if (abs == 1) {
                return ONE;
            }
            if (abs == 0 || (mod100 >= 2 && mod100 <= 19)) {
                return FEW;
            }
            return OTHER;
        }
        if (lang.equals("lt")) {
            if (mod10 == 1 && (mod100 < 11 || mod100 > 19)) {
                return ONE;
            }
            if (mod10 >= 2 && mod10 <= 9 && (mod100 < 11 || mod100 > 19)) {
                return FEW;
            }
            return OTHER;
        }
        if (lang.equals("lv")) {
            if (mod10 == 0 || (mod100 >= 11 && mod100 <= 19)) {
                return ZERO;
            }
            // CLDR's "n % 10 = 1 and n % 100 != 11": 11 is already ZERO above.
            if (mod10 == 1) {
                return ONE;
            }
            return OTHER;
        }
        if (lang.equals("ar")) {
            if (abs == 0) {
                return ZERO;
            }
            if (abs == 1) {
                return ONE;
            }
            if (abs == 2) {
                return TWO;
            }
            if (mod100 >= 3 && mod100 <= 10) {
                return FEW;
            }
            if (mod100 >= 11) {
                return MANY;
            }
            return OTHER;
        }
        if (lang.equals("he") || lang.equals("iw")) {
            if (abs == 1) {
                return ONE;
            }
            if (abs == 2) {
                return TWO;
            }
            return OTHER;
        }
        if (lang.equals("cy")) {
            switch (abs) {
                case 0: return ZERO;
                case 1: return ONE;
                case 2: return TWO;
                case 3: return FEW;
                case 6: return MANY;
                default: return OTHER;
            }
        }
        if (lang.equals("ga")) {
            if (abs == 1) {
                return ONE;
            }
            if (abs == 2) {
                return TWO;
            }
            if (abs >= 3 && abs <= 6) {
                return FEW;
            }
            if (abs >= 7 && abs <= 10) {
                return MANY;
            }
            return OTHER;
        }
        return abs == 1 ? ONE : OTHER;
    }
}
