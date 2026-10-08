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

import android.content.Context;
import android.content.SharedPreferences;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// Committing the value a key already holds changes nothing and, as on
/// Android, notifies no listener; a listener that writes normalized values
/// back would otherwise loop.
public class SharedPreferencesUnchangedValueTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void rewritingTheSameValuesNotifiesNothing() {
        SharedPreferences prefs = AndroidTestSupport.context()
                .getSharedPreferences("unchanged-listener", Context.MODE_PRIVATE);
        prefs.edit().clear().putString("s", "x").putInt("i", 3).putLong("l", 4L).putFloat("f", 1.5f)
                .putBoolean("b", true).putStringSet("set", new HashSet<String>(Arrays.asList("p", "q"))).commit();
        final List<String> seen = new ArrayList<String>();
        SharedPreferences.OnSharedPreferenceChangeListener l = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences p, String key) {
                seen.add(key);
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(l);
        try {
            prefs.edit().putString("s", "x").putInt("i", 3).putLong("l", 4L).putFloat("f", 1.5f)
                    .putBoolean("b", true).putStringSet("set", new HashSet<String>(Arrays.asList("q", "p"))).commit();
            assertEquals("[]", seen.toString());
            prefs.edit().putString("s", "y").putInt("i", 3).commit();
            assertEquals("[s]", seen.toString());
        } finally {
            prefs.unregisterOnSharedPreferenceChangeListener(l);
        }
    }
}
