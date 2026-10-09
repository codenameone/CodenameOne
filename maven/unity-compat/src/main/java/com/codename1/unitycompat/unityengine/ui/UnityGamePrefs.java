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
package com.codename1.unitycompat.unityengine.ui;

import com.codename1.io.Preferences;
import com.codename1.unitycompat.unityengine.PlayerPrefsStore;

/// Keeps `PlayerPrefs` in Codename One's preferences, which each port
/// stores where its platform keeps such things, so a high score outlives
/// the application.
///
/// Unity remembers whether a key was set as an int, a float or a string,
/// and Codename One's preferences answer whatever type is asked for. So
/// each value is kept as a string whose first character says which of the
/// three it is; a float is kept as its bits, because printing one and
/// reading it back is not promised to give the same float everywhere.
///
/// The names of the keys are kept as well, in one more entry, since
/// `DeleteAll` has to know them and must leave the application's other
/// preferences alone.
final class UnityGamePrefs implements PlayerPrefsStore {
    private static final String PREFIX = "unity.playerprefs.";
    private static final String INDEX = "unity.playerprefs";

    @Override
    public Object get(String key) {
        String kept = Preferences.get(PREFIX + key, (String) null);
        if (kept == null || kept.length() == 0) {
            return null;
        }
        String rest = kept.substring(1);
        char kind = kept.charAt(0);
        if (kind == 's') {
            return rest;
        }
        try {
            if (kind == 'i') {
                return Integer.valueOf(Integer.parseInt(rest));
            }
            if (kind == 'f') {
                return Float.valueOf(Float.intBitsToFloat(Integer.parseInt(rest)));
            }
        } catch (NumberFormatException e) {
            // Not something this class wrote: as good as absent.
            return null;
        }
        return null;
    }

    @Override
    public void set(String key, Object value) {
        String kept;
        if (value instanceof Integer) {
            kept = "i" + value;
        } else if (value instanceof Float) {
            kept = "f" + Float.floatToIntBits(((Float) value).floatValue());
        } else {
            kept = "s" + value;
        }
        if (Preferences.get(PREFIX + key, (String) null) == null) {
            // Each name is written behind its length, so that no
            // character a key may hold can be taken for a separator.
            Preferences.set(INDEX, Preferences.get(INDEX, "") + key.length() + ":" + key);
        }
        Preferences.set(PREFIX + key, kept);
    }

    @Override
    public void delete(String key) {
        Preferences.delete(PREFIX + key);
    }

    @Override
    public void deleteAll() {
        String index = Preferences.get(INDEX, "");
        int at = 0;
        while (at < index.length()) {
            int colon = index.indexOf(':', at);
            if (colon < 0) {
                break;
            }
            int length;
            try {
                length = Integer.parseInt(index.substring(at, colon));
            } catch (NumberFormatException e) {
                break;
            }
            int end = colon + 1 + length;
            if (length < 0 || end > index.length()) {
                break;
            }
            Preferences.delete(PREFIX + index.substring(colon + 1, end));
            at = end;
        }
        Preferences.delete(INDEX);
    }

    @Override
    public void save() {
        // Codename One writes each change as it is made.
    }
}
