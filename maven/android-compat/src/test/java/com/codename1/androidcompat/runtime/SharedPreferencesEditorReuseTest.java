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

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// A commit consumes the editor's batch, as on Android: an editor reused
/// afterwards neither re-applies its earlier values nor repeats an earlier
/// `clear()` over what another editor wrote in between.
public class SharedPreferencesEditorReuseTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static SharedPreferences prefs(String name) {
        SharedPreferences p = AndroidTestSupport.context().getSharedPreferences(name, Context.MODE_PRIVATE);
        p.edit().clear().commit();
        return p;
    }

    @Test
    public void reusedEditorDoesNotReapplyOldValues() {
        SharedPreferences p = prefs("editor-reuse-values");
        SharedPreferences.Editor e = p.edit();
        e.putString("x", "1").commit();
        p.edit().putString("x", "2").commit();
        e.putString("y", "3").commit();
        assertEquals("2", p.getString("x", null));
        assertEquals("3", p.getString("y", null));
    }

    @Test
    public void reusedEditorDoesNotRepeatClear() {
        SharedPreferences p = prefs("editor-reuse-clear");
        SharedPreferences.Editor e = p.edit();
        e.clear().apply();
        p.edit().putString("b", "2").commit();
        e.putString("a", "1").commit();
        assertEquals("2", p.getString("b", null));
        assertEquals("1", p.getString("a", null));
    }
}
