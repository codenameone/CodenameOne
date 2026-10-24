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
import com.codename1.compat.testing.MainThreadRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// `edit().clear()` reports every key it removed to the change listeners;
/// it used to empty the file silently, leaving listener-driven state stale.
public class SharedPreferencesClearListenerTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void clearReportsEachRemovedKeyOnce() {
        SharedPreferences prefs = AndroidTestSupport.context()
                .getSharedPreferences("clear-listener", Context.MODE_PRIVATE);
        prefs.edit().clear().putString("a", "1").putString("b", "2").putString("c", "3").commit();
        final List<String> seen = new ArrayList<String>();
        SharedPreferences.OnSharedPreferenceChangeListener l = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences p, String key) {
                seen.add(key);
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(l);
        try {
            // "b" is put back by the same edit and "c" removed explicitly:
            // each key is still reported exactly once.
            prefs.edit().clear().putString("b", "new").remove("c").commit();
            Collections.sort(seen);
            assertEquals("[a, b, c]", seen.toString());
            assertEquals(1, prefs.getAll().size());
        } finally {
            prefs.unregisterOnSharedPreferenceChangeListener(l);
        }
    }
}
