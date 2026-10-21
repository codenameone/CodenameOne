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

    /// Takes the name out of the index as well. `set` adds a name whose
    /// value is absent, so a name left behind here was added once more by
    /// every `DeleteKey` and `Set` of it, and a game that deleted and set a
    /// key each round grew the stored index for ever. Every copy of it goes,
    /// which also heals an index an earlier version let grow.
    @Override
    public void delete(String key) {
        Preferences.delete(PREFIX + key);
        String index = Preferences.get(INDEX, "");
        if (index.length() == 0) {
            return;
        }
        StringBuilder kept = new StringBuilder(index.length());
        int[] at = {0};
        String name = next(index, at);
        while (name != null) {
            if (!name.equals(key)) {
                kept.append(name.length()).append(':').append(name);
            }
            name = next(index, at);
        }
        // What could not be read is kept as it was, not dropped with the
        // names it may still hold.
        kept.append(index.substring(at[0]));
        if (kept.length() == 0) {
            Preferences.delete(INDEX);
        } else if (kept.length() != index.length()) {
            Preferences.set(INDEX, kept.toString());
        }
    }

    @Override
    public void deleteAll() {
        String index = Preferences.get(INDEX, "");
        int[] at = {0};
        String name = next(index, at);
        while (name != null) {
            Preferences.delete(PREFIX + name);
            name = next(index, at);
        }
        Preferences.delete(INDEX);
    }

    /// The name of the index that starts at `at[0]`, moving `at[0]` past
    /// it; null at the end, or where the index is not one this class wrote.
    private static String next(String index, int[] at) {
        int from = at[0];
        if (from >= index.length()) {
            return null;
        }
        int colon = index.indexOf(':', from);
        if (colon < 0) {
            return null;
        }
        int length;
        try {
            length = Integer.parseInt(index.substring(from, colon));
        } catch (NumberFormatException e) {
            return null;
        }
        int end = colon + 1 + length;
        if (length < 0 || end > index.length()) {
            return null;
        }
        at[0] = end;
        return index.substring(colon + 1, end);
    }

    @Override
    public void save() {
        // Codename One writes each change as it is made.
    }
}
