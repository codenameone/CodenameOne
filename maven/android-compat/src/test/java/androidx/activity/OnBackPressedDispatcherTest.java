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
package androidx.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/// The most recently added enabled callback handles back; with none enabled
/// the fallback runs, and a removed callback is no longer asked.
public class OnBackPressedDispatcherTest {

    private static OnBackPressedCallback callback(final String name, final List<String> log, boolean enabled) {
        return new OnBackPressedCallback(enabled) {
            @Override
            public void handleOnBackPressed() {
                log.add(name);
            }
        };
    }

    @Test
    public void mostRecentEnabledCallbackFirstThenFallback() {
        final List<String> log = new ArrayList<String>();
        OnBackPressedDispatcher d = new OnBackPressedDispatcher(new Runnable() {
            @Override
            public void run() {
                log.add("fallback");
            }
        });
        OnBackPressedCallback a = callback("a", log, true);
        OnBackPressedCallback b = callback("b", log, false);
        d.addCallback(a);
        d.addCallback(b);
        assertTrue(d.hasEnabledCallbacks());
        d.onBackPressed();
        b.setEnabled(true);
        d.onBackPressed();
        b.remove();
        a.setEnabled(false);
        assertFalse(d.hasEnabledCallbacks());
        d.onBackPressed();
        assertEquals("[a, b, fallback]", log.toString());
    }
}
