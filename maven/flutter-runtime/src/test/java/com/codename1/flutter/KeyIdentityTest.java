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

import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.widgets.SizedBox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Keys and locales compare the way Flutter compares them, and a GlobalKey
/// reaches the element its widget is mounted as.
public class KeyIdentityTest {

    static final class Probe extends StatefulWidget {
        @Override
        public State<? extends StatefulWidget> createState() {
            return new ProbeState();
        }
    }

    static final class ProbeState extends State<Probe> {
        @Override
        public Widget build(BuildContext context) {
            return new SizedBox();
        }
    }

    @Test
    public void separateUniqueKeysAreUnequal() {
        UniqueKey a = new UniqueKey();
        UniqueKey b = new UniqueKey();
        assertNotEquals(a, b, "a fresh UniqueKey exists to force a new element");
        assertEquals(a, a);
        Widget w1 = new SizedBox();
        w1.key(a);
        Widget w2 = new SizedBox();
        w2.key(b);
        assertFalse(Widget.canUpdate(w1, w2), "different unique keys must not reuse an element");
    }

    @Test
    public void separateGlobalKeysAreUnequal() {
        assertNotEquals(new GlobalKey<Object>(), new GlobalKey<Object>());
    }

    @Test
    public void valueKeysStillCompareByValue() {
        assertEquals(new ValueKey<String>("a"), new ValueKey<String>("a"));
    }

    @Test
    public void aGlobalKeyReachesItsMountedState() {
        GlobalKey<ProbeState> key = new GlobalKey<ProbeState>();
        Probe probe = new Probe();
        probe.key(key);
        Element root = FlutterUI.mount(probe, new RenderHost(), new BuildOwner());

        assertTrue(key.currentState() instanceof ProbeState, "currentState is the mounted State");
        assertSame(root, key.currentContext());
        assertSame(probe, key.currentWidget());

        FlutterUI.unmountTree(root);
        assertNull(key.currentState(), "unmounting releases it");
    }

    @Test
    public void localesCompareByValue() {
        assertEquals(new Locale("en", "US"), new Locale("en", "US"));
        assertEquals(new Locale("en", "US").hashCode(), new Locale("en", "US").hashCode());
        assertNotEquals(new Locale("en", "US"), new Locale("en", "GB"));
        assertEquals(new Locale("en"), new Locale("en"));
    }
}
