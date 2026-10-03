/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

import com.codename1.flutter.foundation.ChangeNotifier;
import com.codename1.flutter.foundation.Listeners;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// This round's runtime fixes: a listener removed during a notification is not
/// called, a disposed notifier stops mid-dispatch, and asset variants come from
/// the build's manifest at any scale.
class Round10RuntimeTest {

    @Test
    void aListenerRemovedDuringDispatchIsNotCalled() {
        final List<Funcs.VoidFunc0> listeners = new ArrayList<Funcs.VoidFunc0>();
        final List<String> calls = new ArrayList<String>();
        final Funcs.VoidFunc0 second = () -> calls.add("second");
        listeners.add(() -> {
            calls.add("first");
            listeners.remove(second);
        });
        listeners.add(second);
        Listeners.notify(listeners);
        assertEquals("[first]", calls.toString());
    }

    /** A notifier model, as an app's class mixing in ChangeNotifier is. */
    static final class Model implements ChangeNotifier {
        void changed() {
            notifyListeners();
        }
    }

    @Test
    void aNotifierDisposedByItsOwnListenerStopsThere() {
        final Model model = new Model();
        final List<String> calls = new ArrayList<String>();
        model.addListener(() -> {
            calls.add("first");
            model.dispose();
        });
        model.addListener(() -> calls.add("second"));
        model.changed();
        assertEquals("[first]", calls.toString());
    }

    @Test
    void theManifestGroupsVariantsAtAnyScale() {
        Map<String, List<String>> m = FlutterAssets.parseManifest(
                "assets/a.png\nassets/2.5x/a.png\nassets/2x/a.png\npackages/p/assets/3.0x/only.png\nassets/notx/b.png\n");
        assertEquals(Arrays.asList("assets/a.png", "assets/2.5x/a.png", "assets/2x/a.png"), m.get("assets/a.png"));
        assertEquals(Arrays.asList("packages/p/assets/3.0x/only.png"), m.get("packages/p/assets/only.png"),
                "an asset bundled only as a variant is still found");
        assertEquals(Arrays.asList("assets/notx/b.png"), m.get("assets/notx/b.png"));
        assertEquals(Arrays.asList("assets/a.png", "assets/2.5x/a.png", "assets/2x/a.png"),
                Arrays.asList(FlutterAssets.variantCandidates("assets/a.png", 2.5, m)));
        assertEquals(2.5, FlutterAssets.ratioOf("assets/2.5x/a.png", "assets/a.png"), 0.0);
        assertEquals(2.0, FlutterAssets.ratioOf("assets/2x/a.png", "assets/a.png"), 0.0);
        assertEquals(1.0, FlutterAssets.ratioOf("assets/a.png", "assets/a.png"), 0.0);
    }

    @Test
    void anAssetTheManifestDoesNotListFallsBackToTheFixedRatios() {
        Map<String, List<String>> m = FlutterAssets.parseManifest("assets/a.png\n");
        String[] c = FlutterAssets.variantCandidates("assets/staged.png", 2.0, m);
        assertTrue(Arrays.asList(c).contains("assets/2.0x/staged.png"));
        assertEquals("assets/staged.png", c[c.length - 1]);
    }

    @Test
    void onlyAPositiveDecimalAndAnXIsARatioDirectory() {
        assertTrue(FlutterAssets.isRatioDir("2x"));
        assertTrue(FlutterAssets.isRatioDir("2.5x"));
        assertTrue(FlutterAssets.isRatioDir("10.0x"));
        assertFalse(FlutterAssets.isRatioDir("x"));
        assertFalse(FlutterAssets.isRatioDir("0x"));
        assertFalse(FlutterAssets.isRatioDir(".5x"));
        assertFalse(FlutterAssets.isRatioDir("2.x"));
        assertFalse(FlutterAssets.isRatioDir("2.5.1x"));
        assertFalse(FlutterAssets.isRatioDir("box"));
        assertNull(FlutterAssets.parseManifest("").get("x"));
    }
}
