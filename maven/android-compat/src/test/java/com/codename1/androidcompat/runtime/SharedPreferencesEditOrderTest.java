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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/// An editor applies the last call made for each key, as on Android: removals
/// were applied before puts whatever the order, so put-then-remove kept the
/// value.
public class SharedPreferencesEditOrderTest {

    @Test
    public void theLastEditOfAKeyWins() {
        SharedPreferences prefs = AndroidTestSupport.context()
                .getSharedPreferences("edit-order", Context.MODE_PRIVATE);
        prefs.edit().putString("a", "old").putString("b", "old").commit();
        prefs.edit().putString("a", "new").remove("a").remove("b").putString("b", "new").commit();
        assertFalse(prefs.contains("a"));
        assertEquals("new", prefs.getString("b", null));
    }
}
