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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/// Preference listeners are held weakly, as on Android. The application-
/// lifetime preferences used to hold every registered listener strongly, so
/// an activity that registered itself and was destroyed without
/// unregistering was never collected.
public class SharedPreferencesWeakListenerTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    static final class Listener implements SharedPreferences.OnSharedPreferenceChangeListener {
        final List<String> seen = new ArrayList<String>();

        @Override
        public void onSharedPreferenceChanged(SharedPreferences p, String key) {
            seen.add(key);
        }
    }

    private static WeakReference<Listener> registerAndForget(SharedPreferences prefs) {
        Listener l = new Listener();
        prefs.registerOnSharedPreferenceChangeListener(l);
        return new WeakReference<Listener>(l);
    }

    @Test
    public void anUnreferencedListenerIsCollected() {
        SharedPreferences prefs = AndroidTestSupport.context()
                .getSharedPreferences("weak-listener", Context.MODE_PRIVATE);
        WeakReference<Listener> ref = registerAndForget(prefs);
        for (int i = 0; i < 50 && ref.get() != null; i++) {
            System.gc();
            byte[] pressure = new byte[1 << 20];
            pressure[0] = 1;
        }
        assertNull("the preferences kept the listener alive", ref.get());
        // Changes after the collection reach nobody and do not fail.
        prefs.edit().putString("k", "v").commit();
    }

    @Test
    public void aReferencedListenerStillHearsUntilUnregistered() {
        SharedPreferences prefs = AndroidTestSupport.context()
                .getSharedPreferences("weak-listener-kept", Context.MODE_PRIVATE);
        Listener l = new Listener();
        prefs.registerOnSharedPreferenceChangeListener(l);
        prefs.registerOnSharedPreferenceChangeListener(l);
        for (int i = 0; i < 3; i++) {
            System.gc();
        }
        prefs.edit().putString("a", String.valueOf(System.nanoTime())).commit();
        assertEquals("[a]", l.seen.toString());
        prefs.unregisterOnSharedPreferenceChangeListener(l);
        prefs.edit().putString("a", String.valueOf(System.nanoTime())).commit();
        assertEquals("[a]", l.seen.toString());
    }
}
