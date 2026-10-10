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
package com.codenameone.examples.wayline.ui;

import com.codename1.l10n.L10NManager;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codenameone.examples.wayline.Prefs;
import java.util.HashMap;
import java.util.Map;

/// The language the app speaks.
///
/// The translations are the `Bundle_<code>.properties` files in
/// `src/main/l10n`, which the build packs into the theme. One of them at a
/// time is installed as the `UIManager`'s bundle, and from then on every
/// label, button, title and hint looks its text up there as it is set: the
/// English text in the source is the key, and a text with no entry is shown as
/// it is. So a screen written in English is translated without saying so.
///
/// What a component cannot do by itself is a sentence with something put into
/// it -- "Pick up Riley". Those go through [#tr(String, Object...)], with the
/// whole sentence as the key and `{0}` where the name goes, because where it
/// goes is the translator's decision.
///
/// To add a string: write it in English in the code, and add a line for it to
/// each of the four translated files. To add a language: add its file, and its
/// code to [#CODES].
public final class Lang {
    /// The languages there is a translation for. English is the source.
    public static final String[] CODES = {"en", "es", "fr", "de", "he"};

    private static final String BUNDLE = "Bundle";
    /// The entry each file names its own language in, in that language.
    private static final String NAME_KEY = "language.name";

    private Lang() {
    }

    /// The language in use: the one chosen in Settings, else the device's when
    /// there is a translation for it, else English.
    public static String current() {
        String chosen = known(Prefs.language());
        if (chosen != null) {
            return chosen;
        }
        String device = known(L10NManager.getInstance().getLanguage());
        return device == null ? CODES[0] : device;
    }

    /// Installs the translation for [#current()]. A screen built after this is
    /// in the new language, and laid out right to left when the language is
    /// written that way; one already built keeps the words it was built with.
    public static void apply() {
        Map<String, String> bundle = new HashMap<String, String>();
        // Always said, one way or the other: a bundle that is silent about the
        // direction leaves the one before it in force.
        bundle.put("@rtl", "false");
        Map<String, String> translated = table(current());
        if (translated != null) {
            bundle.putAll(translated);
        }
        UIManager.getInstance().setBundle(bundle);
    }

    /// What `code`'s language calls itself.
    public static String name(String code) {
        Map<String, String> translated = table(code);
        String name = translated == null ? null : translated.get(NAME_KEY);
        return name == null || name.length() == 0 ? code : name;
    }

    public static String tr(String text) {
        return UIManager.getInstance().localize(text, text);
    }

    /// Translates `pattern` and puts `values` where it says `{0}`, `{1}`...
    public static String tr(String pattern, Object... values) {
        String text = tr(pattern);
        for (int iter = 0; iter < values.length; iter++) {
            String mark = "{" + iter + "}";
            String value = String.valueOf(values[iter]);
            int at = text.indexOf(mark);
            while (at >= 0) {
                text = text.substring(0, at) + value + text.substring(at + mark.length());
                at = text.indexOf(mark, at + value.length());
            }
        }
        return text;
    }

    /// The code among [#CODES] that `language` is, or null. A device reports
    /// its language with or without a country (`es`, `es_MX`) and Hebrew under
    /// either of two codes; this is compared as the ASCII it is, since folding
    /// the case would follow the very language being worked out.
    private static String known(String language) {
        if (language == null || language.length() < 2) {
            return null;
        }
        if (language.regionMatches(true, 0, "iw", 0, 2)) {
            return "he";
        }
        for (String code : CODES) {
            if (language.regionMatches(true, 0, code, 0, 2)) {
                return code;
            }
        }
        return null;
    }

    private static Map<String, String> table(String code) {
        Resources theme = Resources.getGlobalResources();
        return theme == null ? null : theme.getL10N(BUNDLE, code);
    }
}
