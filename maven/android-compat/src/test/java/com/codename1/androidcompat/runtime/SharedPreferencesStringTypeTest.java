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

import org.junit.Rule;
import org.junit.Test;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/// As on Android, and as the numeric and boolean getters already did, reading
/// a present value of another type as a string (or string set) throws
/// instead of answering the default, which made a schema mistake look like
/// a missing key.
public class SharedPreferencesStringTypeTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void aWrongTypedStringReadThrows() {
        SharedPreferences prefs = AndroidTestSupport.context()
                .getSharedPreferences("string-type", Context.MODE_PRIVATE);
        prefs.edit().putInt("n", 3).putString("s", "x").commit();
        try {
            prefs.getString("n", "def");
            fail("an int read as a String must throw");
        } catch (ClassCastException expected) {
            assertEquals("Key n is not a String", expected.getMessage());
        }
        try {
            prefs.getStringSet("s", new HashSet<String>());
            fail("a String read as a string set must throw");
        } catch (ClassCastException expected) {
            assertEquals("Key s is not a string set", expected.getMessage());
        }
        assertEquals("def", prefs.getString("missing", "def"));
        assertEquals("x", prefs.getString("s", "def"));
    }
}
