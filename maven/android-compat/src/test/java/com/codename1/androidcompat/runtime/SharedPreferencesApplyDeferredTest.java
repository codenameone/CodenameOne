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
import com.codename1.io.Storage;
import com.codename1.ui.Display;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// `apply()` updates memory at once and writes later, as on Android, rather
/// than waiting on storage like `commit()`. The write goes through the main
/// thread's queue, so a burst of applies costs one write, and a `commit()`
/// in between writes everything and leaves the queued write nothing to do.
public class SharedPreferencesApplyDeferredTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Test
    public void applyWritesOnceLater() {
        final SharedPreferences prefs = AndroidTestSupport.context()
                .getSharedPreferences("apply-deferred", Context.MODE_PRIVATE);
        Storage real = Storage.getInstance();
        final ArrayList<Object> writes = new ArrayList<Object>();
        Storage.setStorageInstance(new Storage() {
            @Override
            public boolean writeObject(String name, Object o) {
                writes.add(o);
                return true;
            }
        });
        try {
            // The applies and the checks between them run on the main thread
            // (the EDT): from any other thread the queued write could run
            // before the next line. Whether the test thread is the EDT
            // differs between runs, so onMainThread handles both.
            final int[] seen = new int[1];
            onMainThread(new Runnable() {
                @Override
                public void run() {
                    prefs.edit().putString("k", "one").apply();
                    prefs.edit().putString("k", "two").apply();
                    seen[0] = writes.size();
                }
            });
            assertEquals("two", prefs.getString("k", null));
            assertEquals("apply does not wait on storage", 0, seen[0]);
            MainThreadRule.drain();
            assertEquals("the applies share one write", 1, writes.size());
            assertEquals("two", ((Map<?, ?>) writes.get(0)).get("k"));

            final boolean[] committed = new boolean[1];
            onMainThread(new Runnable() {
                @Override
                public void run() {
                    prefs.edit().putString("k", "three").apply();
                    committed[0] = prefs.edit().putString("k", "four").commit();
                    seen[0] = writes.size();
                }
            });
            assertTrue(committed[0]);
            assertEquals(2, seen[0]);
            MainThreadRule.drain();
            assertEquals("the commit already wrote the applied value", 2, writes.size());
            assertEquals("four", ((Map<?, ?>) writes.get(1)).get("k"));
        } finally {
            Storage.setStorageInstance(real);
        }
    }

    private static void onMainThread(Runnable r) {
        if (Display.getInstance().isEdt()) {
            r.run();
        } else {
            Display.getInstance().callSeriallyAndWait(r);
        }
    }
}
