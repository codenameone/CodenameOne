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
 * Localized strings for Material widgets, mirroring Flutter's
 * {@code MaterialLocalizations}. Tables for English, Spanish and Hebrew;
 * {@link #of(BuildContext)} picks one by the ambient locale's language code and
 * falls back to English. The date formats are fixed per language rather than
 * driven by CLDR patterns.
 */
public class MaterialLocalizations {

    private static final int K_OKBUTTONLABEL = 0;
    private static final int K_CANCELBUTTONLABEL = 1;
    private static final int K_CLOSEBUTTONLABEL = 2;
    private static final int K_CONTINUEBUTTONLABEL = 3;
    private static final int K_COPYBUTTONLABEL = 4;
    private static final int K_CUTBUTTONLABEL = 5;
    private static final int K_PASTEBUTTONLABEL = 6;
    private static final int K_SELECTALLBUTTONLABEL = 7;
    private static final int K_SEARCHFIELDLABEL = 8;
    private static final int K_BACKBUTTONTOOLTIP = 9;
    private static final int K_CLOSEBUTTONTOOLTIP = 10;
    private static final int K_DELETEBUTTONTOOLTIP = 11;
    private static final int K_MOREBUTTONTOOLTIP = 12;
    private static final int K_NEXTPAGETOOLTIP = 13;
    private static final int K_PREVIOUSPAGETOOLTIP = 14;
    private static final int K_OPENAPPDRAWERTOOLTIP = 15;
    private static final int K_SHOWMENUTOOLTIP = 16;
    private static final int K_DRAWERLABEL = 17;
    private static final int K_POPUPMENULABEL = 18;
    private static final int K_DIALOGLABEL = 19;
    private static final int K_ALERTDIALOGLABEL = 20;
    private static final int K_DATEPICKERHELPTEXT = 21;
    private static final int K_DATEHELPTEXT = 22;
    private static final int K_TIMEPICKERDIALHELPTEXT = 23;
    private static final int K_REFRESHINDICATORSEMANTICLABEL = 24;
    private static final int K_MODALBARRIERDISMISSLABEL = 25;
    private static final int K_VIEWLICENSESBUTTONLABEL = 26;
    private static final int K_LICENSESPAGETITLE = 27;

    private static final String[][] TABLES = {
        // en
        {
            "OK",
            "Cancel",
            "Close",
            "Continue",
            "Copy",
            "Cut",
            "Paste",
            "Select all",
            "Search",
            "Back",
            "Close",
            "Delete",
            "More",
            "Next page",
            "Previous page",
            "Open navigation menu",
            "Show menu",
            "Navigation menu",
            "Popup menu",
            "Dialog",
            "Alert",
            "Select date",
            "mm/dd/yyyy",
            "Select time",
            "Refresh",
            "Dismiss",
            "View licenses",
            "Licenses"
        },
        // es
        {
            "ACEPTAR",
            "Cancelar",
            "Cerrar",
            "Continuar",
            "Copiar",
            "Cortar",
            "Pegar",
            "Seleccionar todo",
            "Buscar",
            "Atr\u00e1s",
            "Cerrar",
            "Eliminar",
            "M\u00e1s",
            "P\u00e1gina siguiente",
            "P\u00e1gina anterior",
            "Abrir el men\u00fa de navegaci\u00f3n",
            "Mostrar men\u00fa",
            "Men\u00fa de navegaci\u00f3n",
            "Men\u00fa emergente",
            "Cuadro de di\u00e1logo",
            "Alerta",
            "Seleccionar fecha",
            "dd/mm/aaaa",
            "Seleccionar hora",
            "Actualizar",
            "Cerrar",
            "Ver licencias",
            "Licencias"
        },
        // he
        {
            "\u05d0\u05d9\u05e9\u05d5\u05e8",
            "\u05d1\u05d9\u05d8\u05d5\u05dc",
            "\u05e1\u05d2\u05d9\u05e8\u05d4",
            "\u05d4\u05de\u05e9\u05da",
            "\u05d4\u05e2\u05ea\u05e7\u05d4",
            "\u05d2\u05d6\u05d9\u05e8\u05d4",
            "\u05d4\u05d3\u05d1\u05e7\u05d4",
            "\u05d1\u05d7\u05d9\u05e8\u05ea \u05d4\u05db\u05d5\u05dc",
            "\u05d7\u05d9\u05e4\u05d5\u05e9",
            "\u05d4\u05e7\u05d5\u05d3\u05dd",
            "\u05e1\u05d2\u05d9\u05e8\u05d4",
            "\u05de\u05d7\u05d9\u05e7\u05d4",
            "\u05e2\u05d5\u05d3",
            "\u05d4\u05d3\u05e3 \u05d4\u05d1\u05d0",
            "\u05d4\u05d3\u05e3 \u05d4\u05e7\u05d5\u05d3\u05dd",
            "\u05e4\u05ea\u05d9\u05d7\u05d4 \u05e9\u05dc \u05ea\u05e4\u05e8\u05d9\u05d8 \u05d4\u05e0\u05d9\u05d5\u05d5\u05d8",
            "\u05d4\u05e6\u05d2\u05ea \u05d4\u05ea\u05e4\u05e8\u05d9\u05d8",
            "\u05ea\u05e4\u05e8\u05d9\u05d8 \u05e0\u05d9\u05d5\u05d5\u05d8",
            "\u05ea\u05e4\u05e8\u05d9\u05d8 \u05e7\u05d5\u05e4\u05e5",
            "\u05ea\u05d9\u05d1\u05ea \u05d3\u05d5-\u05e9\u05d9\u05d7",
            "\u05d4\u05ea\u05e8\u05d0\u05d4",
            "\u05d1\u05d7\u05d9\u05e8\u05ea \u05ea\u05d0\u05e8\u05d9\u05da",
            "dd.mm.yyyy",
            "\u05d1\u05d7\u05d9\u05e8\u05ea \u05e9\u05e2\u05d4",
            "\u05e8\u05e2\u05e0\u05d5\u05df",
            "\u05e1\u05d2\u05d9\u05e8\u05d4",
            "\u05d4\u05e6\u05d2\u05ea \u05d4\u05e8\u05d9\u05e9\u05d9\u05d5\u05e0\u05d5\u05ea",
            "\u05e8\u05d9\u05e9\u05d9\u05d5\u05e0\u05d5\u05ea"
        },
    };

    private static final MaterialLocalizations[] INSTANCES = {
        new MaterialLocalizations(0), new MaterialLocalizations(1), new MaterialLocalizations(2)
    };

    /** {@code MaterialLocalizations.delegate} -- a static getter, hence a field. Loads the table for a locale. */
    public static final LocalizationsDelegate<MaterialLocalizations> delegate = new LocalizationsDelegate<MaterialLocalizations>() {
        @Override
        public dart.async.Future<MaterialLocalizations> load(Locale locale) {
            return dart.async.Future.value(forLocale(locale));
        }
    };

    /** Index into the tables: 0 en, 1 es, 2 he. */
    final int lang;

    MaterialLocalizations(int lang) {
        this.lang = lang;
    }

    /**
     * The strings of the ambient locale's language
     * ({@code Localizations.localeOf(context)}); English for any language
     * without a table here.
     */
    public static MaterialLocalizations of(BuildContext context) {
        return forLocale(com.codename1.flutter.widgets.Localizations.localeOf(context));
    }

    /** The instance for {@code locale}'s language code; English when there is no table for it. */
    public static MaterialLocalizations forLocale(Locale locale) {
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

    public String okButtonLabel() {
        return s(K_OKBUTTONLABEL);
    }

    public String cancelButtonLabel() {
        return s(K_CANCELBUTTONLABEL);
    }

    public String closeButtonLabel() {
        return s(K_CLOSEBUTTONLABEL);
    }

    public String continueButtonLabel() {
        return s(K_CONTINUEBUTTONLABEL);
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

    public String searchFieldLabel() {
        return s(K_SEARCHFIELDLABEL);
    }

    public String backButtonTooltip() {
        return s(K_BACKBUTTONTOOLTIP);
    }

    public String closeButtonTooltip() {
        return s(K_CLOSEBUTTONTOOLTIP);
    }

    public String deleteButtonTooltip() {
        return s(K_DELETEBUTTONTOOLTIP);
    }

    public String moreButtonTooltip() {
        return s(K_MOREBUTTONTOOLTIP);
    }

    public String nextPageTooltip() {
        return s(K_NEXTPAGETOOLTIP);
    }

    public String previousPageTooltip() {
        return s(K_PREVIOUSPAGETOOLTIP);
    }

    public String openAppDrawerTooltip() {
        return s(K_OPENAPPDRAWERTOOLTIP);
    }

    public String showMenuTooltip() {
        return s(K_SHOWMENUTOOLTIP);
    }

    public String drawerLabel() {
        return s(K_DRAWERLABEL);
    }

    public String popupMenuLabel() {
        return s(K_POPUPMENULABEL);
    }

    public String dialogLabel() {
        return s(K_DIALOGLABEL);
    }

    public String alertDialogLabel() {
        return s(K_ALERTDIALOGLABEL);
    }

    public String datePickerHelpText() {
        return s(K_DATEPICKERHELPTEXT);
    }

    public String dateHelpText() {
        return s(K_DATEHELPTEXT);
    }

    public String timePickerDialHelpText() {
        return s(K_TIMEPICKERDIALHELPTEXT);
    }

    public String refreshIndicatorSemanticLabel() {
        return s(K_REFRESHINDICATORSEMANTICLABEL);
    }

    public String modalBarrierDismissLabel() {
        return s(K_MODALBARRIERDISMISSLABEL);
    }

    public String viewLicensesButtonLabel() {
        return s(K_VIEWLICENSESBUTTONLABEL);
    }

    public String licensesPageTitle() {
        return s(K_LICENSESPAGETITLE);
    }

    private static final String[][] MONTHS = {{"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"}, {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"}, {"\u05d9\u05e0\u05d5\u05d0\u05e8", "\u05e4\u05d1\u05e8\u05d5\u05d0\u05e8", "\u05de\u05e8\u05e5", "\u05d0\u05e4\u05e8\u05d9\u05dc", "\u05de\u05d0\u05d9", "\u05d9\u05d5\u05e0\u05d9", "\u05d9\u05d5\u05dc\u05d9", "\u05d0\u05d5\u05d2\u05d5\u05e1\u05d8", "\u05e1\u05e4\u05d8\u05de\u05d1\u05e8", "\u05d0\u05d5\u05e7\u05d8\u05d5\u05d1\u05e8", "\u05e0\u05d5\u05d1\u05de\u05d1\u05e8", "\u05d3\u05e6\u05de\u05d1\u05e8"}};
    private static final String[][] SHORT_MONTHS = {{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"}, {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic"}, {"\u05d9\u05e0\u05d5\u05d0\u05e8", "\u05e4\u05d1\u05e8\u05d5\u05d0\u05e8", "\u05de\u05e8\u05e5", "\u05d0\u05e4\u05e8\u05d9\u05dc", "\u05de\u05d0\u05d9", "\u05d9\u05d5\u05e0\u05d9", "\u05d9\u05d5\u05dc\u05d9", "\u05d0\u05d5\u05d2\u05d5\u05e1\u05d8", "\u05e1\u05e4\u05d8\u05de\u05d1\u05e8", "\u05d0\u05d5\u05e7\u05d8\u05d5\u05d1\u05e8", "\u05e0\u05d5\u05d1\u05de\u05d1\u05e8", "\u05d3\u05e6\u05de\u05d1\u05e8"}};
    /** Sunday first. */
    private static final String[][] WEEKDAYS = {{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"}, {"domingo", "lunes", "martes", "mi\u00e9rcoles", "jueves", "viernes", "s\u00e1bado"}, {"\u05d9\u05d5\u05dd \u05e8\u05d0\u05e9\u05d5\u05df", "\u05d9\u05d5\u05dd \u05e9\u05e0\u05d9", "\u05d9\u05d5\u05dd \u05e9\u05dc\u05d9\u05e9\u05d9", "\u05d9\u05d5\u05dd \u05e8\u05d1\u05d9\u05e2\u05d9", "\u05d9\u05d5\u05dd \u05d7\u05de\u05d9\u05e9\u05d9", "\u05d9\u05d5\u05dd \u05e9\u05d9\u05e9\u05d9", "\u05d9\u05d5\u05dd \u05e9\u05d1\u05ea"}};
    private static final String[][] SHORT_WEEKDAYS = {{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"}, {"dom", "lun", "mar", "mi\u00e9", "jue", "vie", "s\u00e1b"}, {"\u05d9\u05d5\u05dd \u05d0\u05f3", "\u05d9\u05d5\u05dd \u05d1\u05f3", "\u05d9\u05d5\u05dd \u05d2\u05f3", "\u05d9\u05d5\u05dd \u05d3\u05f3", "\u05d9\u05d5\u05dd \u05d4\u05f3", "\u05d9\u05d5\u05dd \u05d5\u05f3", "\u05e9\u05d1\u05ea"}};
    private static final String[][] NARROW_WEEKDAYS = {{"S", "M", "T", "W", "T", "F", "S"}, {"D", "L", "M", "X", "J", "V", "S"}, {"\u05d0", "\u05d1", "\u05d2", "\u05d3", "\u05d4", "\u05d5", "\u05e9"}};
    private static final long[] FIRST_DAY = {0, 1, 0};

    /** Dart's weekday runs 1 (Monday) to 7 (Sunday); the tables start on Sunday. */
    private static int sundayFirst(dart.core.DateTime d) {
        return (int) (d.weekday() % 7);
    }

    private static int month0(dart.core.DateTime d) {
        return (int) d.month() - 1;
    }

    /** Flutter's {@code formatMediumDate}: weekday, month and day without the year. */
    public String formatMediumDate(dart.core.DateTime d) {
        String wd = SHORT_WEEKDAYS[lang][sundayFirst(d)];
        String m = SHORT_MONTHS[lang][month0(d)];
        switch (lang) {
            case 1:
                return wd + ", " + d.day() + " " + m;
            case 2:
                return wd + ", " + d.day() + " \u05d1" + m;
            default:
                return wd + ", " + m + " " + d.day();
        }
    }

    /** Flutter's {@code formatFullDate}: weekday, day, month and year in full. */
    public String formatFullDate(dart.core.DateTime d) {
        String wd = WEEKDAYS[lang][sundayFirst(d)];
        String m = MONTHS[lang][month0(d)];
        switch (lang) {
            case 1:
                return wd + ", " + d.day() + " de " + m + " de " + d.year();
            case 2:
                return wd + ", " + d.day() + " \u05d1" + m + " " + d.year();
            default:
                return wd + ", " + m + " " + d.day() + ", " + d.year();
        }
    }

    /** Flutter's {@code formatMonthYear}. */
    public String formatMonthYear(dart.core.DateTime d) {
        String m = MONTHS[lang][month0(d)];
        return lang == 1 ? m + " de " + d.year() : m + " " + d.year();
    }

    /** Flutter's {@code narrowWeekdays}: one-letter names, Sunday first whatever the locale. */
    public dart.core.DartList<String> narrowWeekdays() {
        dart.core.DartList<String> out = new dart.core.DartList<String>();
        for (String n : NARROW_WEEKDAYS[lang]) {
            out.add(n);
        }
        return out;
    }

    /** Flutter's {@code firstDayOfWeekIndex}: 0 is Sunday. */
    public long firstDayOfWeekIndex() {
        return FIRST_DAY[lang];
    }
}
