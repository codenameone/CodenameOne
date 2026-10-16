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
package com.codename1.flutter;

/**
 * A Unicode locale identifier: a required language code and an optional country
 * code (Flutter's {@code Locale(languageCode, [countryCode])}).
 */
public class Locale {

    private final String languageCode;
    private final String countryCode;

    public Locale(String languageCode) {
        this(languageCode, null);
    }

    public Locale(String languageCode, String countryCode) {
        this.languageCode = languageCode;
        this.countryCode = countryCode;
    }

    public String languageCode() {
        return languageCode;
    }

    public String countryCode() {
        return countryCode;
    }

    @Override
    public String toString() {
        return countryCode == null ? languageCode : languageCode + "_" + countryCode;
    }

    /// Value equality over the codes, as Flutter's Locale has. Inheriting
    /// identity made two separately built Locale("en", "US") values unequal, so a
    /// check against Localizations.localeOf(context) failed and locale-keyed maps
    /// and sets missed, although the codes matched.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Locale)) {
            return false;
        }
        Locale other = (Locale) o;
        return same(languageCode, other.languageCode) && same(countryCode, other.countryCode);
    }

    @Override
    public int hashCode() {
        int h = languageCode == null ? 0 : languageCode.hashCode();
        return 31 * h + (countryCode == null ? 0 : countryCode.hashCode());
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
