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
import com.codename1.io.Storage;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// `commit()` answers whether the edit reached storage, so a caller that
/// needs a value to survive a restart learns when it will not.
public class SharedPreferencesCommitFailureTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void commitReportsAFailedWrite() {
        SharedPreferences prefs = AndroidTestSupport.context()
                .getSharedPreferences("commit-failure", Context.MODE_PRIVATE);
        Storage real = Storage.getInstance();
        // The headless port has no storage stream, so both outcomes are
        // stubbed.
        final boolean[] succeed = {true};
        Storage.setStorageInstance(new Storage() {
            @Override
            public boolean writeObject(String name, Object o) {
                return succeed[0];
            }
        });
        try {
            assertTrue(prefs.edit().putString("k", "ok").commit());
            succeed[0] = false;
            assertFalse(prefs.edit().putString("k", "lost").commit());
            assertEquals("lost", prefs.getString("k", null));
            succeed[0] = true;
            assertTrue(prefs.edit().putString("k", "ok").commit());
        } finally {
            Storage.setStorageInstance(real);
        }
    }
}
