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
package com.codename1.flutter.l10n;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Locale;

/**
 * Localized strings for Cupertino widgets, mirroring Flutter's
 * {@code CupertinoLocalizations}: English, Spanish and Hebrew, chosen by the
 * ambient locale's language code, English for any other.
 */
public class CupertinoLocalizations {

    private static final int K_ALERTDIALOGLABEL = 0;
    private static final int K_ANTEMERIDIEMABBREVIATION = 1;
    private static final int K_POSTMERIDIEMABBREVIATION = 2;
    private static final int K_COPYBUTTONLABEL = 3;
    private static final int K_CUTBUTTONLABEL = 4;
    private static final int K_PASTEBUTTONLABEL = 5;
    private static final int K_SELECTALLBUTTONLABEL = 6;
    private static final int K_SEARCHTEXTFIELDPLACEHOLDERLABEL = 7;
    private static final int K_MODALBARRIERDISMISSLABEL = 8;
    private static final int K_TODAYLABEL = 9;
    private static final int K_CANCELBUTTONLABEL = 10;
    private static final int K_BACKBUTTONLABEL = 11;

    private static final String[][] TABLES = {
        // en
        {
            "Alert",
            "AM",
            "PM",
            "Copy",
            "Cut",
            "Paste",
            "Select All",
            "Search",
            "Dismiss",
            "Today",
            "Cancel",
            "Back"
        },
        // es
        {
            "Alerta",
            "a. m.",
            "p. m.",
            "Copiar",
            "Cortar",
            "Pegar",
            "Seleccionar todo",
            "Buscar",
            "Cerrar",
            "Hoy",
            "Cancelar",
            "Atr\u00e1s"
        },
        // he
        {
            "\u05d4\u05ea\u05e8\u05d0\u05d4",
            "AM",
            "PM",
            "\u05d4\u05e2\u05ea\u05e7\u05d4",
            "\u05d2\u05d6\u05d9\u05e8\u05d4",
            "\u05d4\u05d3\u05d1\u05e7\u05d4",
            "\u05d1\u05d7\u05d9\u05e8\u05ea \u05d4\u05db\u05d5\u05dc",
            "\u05d7\u05d9\u05e4\u05d5\u05e9",
            "\u05e1\u05d2\u05d9\u05e8\u05d4",
            "\u05d4\u05d9\u05d5\u05dd",
            "\u05d1\u05d9\u05d8\u05d5\u05dc",
            "\u05d4\u05e7\u05d5\u05d3\u05dd"
        },
    };

    private static final CupertinoLocalizations[] INSTANCES = {
        new CupertinoLocalizations(0), new CupertinoLocalizations(1), new CupertinoLocalizations(2)
    };

    /** {@code CupertinoLocalizations.delegate} -- a static getter, hence a field. Loads the table for a locale. */
    public static final LocalizationsDelegate<CupertinoLocalizations> delegate = new LocalizationsDelegate<CupertinoLocalizations>() {
        @Override
        public dart.async.Future<CupertinoLocalizations> load(Locale locale) {
            return dart.async.Future.value(forLocale(locale));
        }
    };

    /** Index into the tables: 0 en, 1 es, 2 he. */
    final int lang;

    CupertinoLocalizations(int lang) {
        this.lang = lang;
    }

    /**
     * The strings of the ambient locale's language
     * ({@code Localizations.localeOf(context)}); English for any language
     * without a table here.
     */
    public static CupertinoLocalizations of(BuildContext context) {
        return forLocale(com.codename1.flutter.widgets.Localizations.localeOf(context));
    }

    /** The instance for {@code locale}'s language code; English when there is no table for it. */
    public static CupertinoLocalizations forLocale(Locale locale) {
        String lc = locale == null ? null : locale.languageCode();
        if ("es".equals(lc)) {
            return INSTANCES[1];
        }
        if ("he".equals(lc) || "iw".equals(lc)) {
            return INSTANCES[2];
        }
        return INSTANCES[0];
    }

    private String s(int key) {
        return TABLES[lang][key];
    }

    public String alertDialogLabel() {
        return s(K_ALERTDIALOGLABEL);
    }

    public String anteMeridiemAbbreviation() {
        return s(K_ANTEMERIDIEMABBREVIATION);
    }

    public String postMeridiemAbbreviation() {
        return s(K_POSTMERIDIEMABBREVIATION);
    }

    public String copyButtonLabel() {
        return s(K_COPYBUTTONLABEL);
    }

    public String cutButtonLabel() {
        return s(K_CUTBUTTONLABEL);
    }

    public String pasteButtonLabel() {
        return s(K_PASTEBUTTONLABEL);
    }

    public String selectAllButtonLabel() {
        return s(K_SELECTALLBUTTONLABEL);
    }

    public String searchTextFieldPlaceholderLabel() {
        return s(K_SEARCHTEXTFIELDPLACEHOLDERLABEL);
    }

    public String modalBarrierDismissLabel() {
        return s(K_MODALBARRIERDISMISSLABEL);
    }

    public String todayLabel() {
        return s(K_TODAYLABEL);
    }

    public String cancelButtonLabel() {
        return s(K_CANCELBUTTONLABEL);
    }

    public String backButtonLabel() {
        return s(K_BACKBUTTONLABEL);
    }
}
