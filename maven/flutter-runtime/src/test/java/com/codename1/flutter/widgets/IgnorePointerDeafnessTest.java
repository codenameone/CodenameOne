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
package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.ui.Component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An IgnorePointer has to make its subtree DEAF, not merely render it.
 *
 * <p>It exists so that something above it receives a touch that lands on top of its
 * child, and it used to be a structural pass-through: the child rendered and stayed
 * live, so the child answered first and the handler above never ran. The gallery's
 * splash screen is built on exactly that -- a strip of the home page wrapped in an
 * IgnorePointer inside a detector whose tap dismisses the splash -- so the screen could
 * be entered and not left.</p>
 *
 * <p>Codename One's hit test walks UP from the component it lands on for as long as each
 * one ignores pointer events, so the flag has to reach the whole subtree: a touch landing
 * on a nested child would otherwise stop there and consume the press.</p>
 */
class IgnorePointerDeafnessTest {

    private static RenderElement mount(IgnorePointer w) {
        RenderHost host = new RenderHost();
        RenderElement e = (RenderElement) FlutterUI.mount(w, host, new BuildOwner());
        e.layout(BoxConstraints.loose(200, 200));
        e.position(0, 0);
        return e;
    }

    private static void assertDeaf(RenderElement e, boolean deaf, String where) {
        for (RenderElement child : e.renderChildren()) {
            Component c = child.component();
            if (c != null) {
                assertTrue(deaf == c.isIgnorePointerEvents(),
                        where + ": " + c.getClass().getSimpleName());
            } else {
                assertDeaf(child, deaf, where);
            }
        }
    }

    @Test
    @DisplayName("the subtree is deaf to touch by default")
    void ignoresByDefault() {
        IgnorePointer w = new IgnorePointer();
        w.child(new ProbeBox(50, 50));
        assertDeaf(mount(w), true, "default");
    }

    @Test
    @DisplayName("ignoring: false leaves the subtree live")
    void honoursIgnoringFalse() {
        IgnorePointer w = new IgnorePointer();
        w.ignoring(Boolean.FALSE);
        w.child(new ProbeBox(50, 50));
        assertDeaf(mount(w), false, "ignoring false");
    }

    @Test
    @DisplayName("deafness reaches a NESTED child, not just the top one")
    void reachesNestedChildren() {
        Column inner = new Column();
        inner.children(dart.core.DartList.of(new ProbeBox(20, 20), new ProbeBox(20, 20)));
        Padding pad = new Padding();
        pad.child(inner);
        IgnorePointer w = new IgnorePointer();
        w.child(pad);
        RenderElement e = mount(w);
        assertDeaf(e, true, "nested");
        assertFalse(e.renderChildren().isEmpty(), "the child must actually be mounted");
    }
}
