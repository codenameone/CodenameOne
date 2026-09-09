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
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.IconButton;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import dart.runtime.Funcs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A GestureDetector must not shadow the controls INSIDE it.
 *
 * <p>The overlay that makes a wrapped subtree tappable covers that whole subtree and grabs
 * pointer events, so a wrapper around a button used to swallow the button's presses —
 * Flutter's hit test gives the innermost target priority. The symptom is a control that
 * draws correctly and does nothing, which is why the app bar's overflow button looked
 * finished while being inert.</p>
 *
 * <p>Headless, so no CN1 components exist; what is asserted here is the STRUCTURE the
 * dispatch relies on — that the gesture element exposes the content subtree the overlay
 * consults, and that the button inside it is a distinct render element with its own
 * handler.</p>
 */
class GestureHitTestTest {

    private static GestureDetector detectorAround(Widget child, Funcs.VoidFunc0 onTap) {
        GestureDetector g = new GestureDetector();
        g.child(child);
        g.onTap(onTap);
        return g;
    }

    @Test
    @DisplayName("the detector exposes its content subtree, not just its overlay")
    void theContentSubtreeIsReachable() {
        GestureDetector g = detectorAround(new ProbeBox(50, 50), null);
        GestureRenderElement e = (GestureRenderElement)
                FlutterUI.mount(g, new RenderHost(), new BuildOwner());

        assertNotNull(e.contentElement(),
                "the overlay resolves its own subtree through this");
    }

    @Test
    @DisplayName("a button inside a detector keeps its own press handler")
    void anInnerButtonKeepsItsHandler() {
        final int[] pressed = {0};
        IconButton inner = new IconButton();
        inner.icon(new Icon(com.codename1.flutter.Icons.search));
        inner.onPressed(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                pressed[0]++;
            }
        });

        GestureRenderElement e = (GestureRenderElement) FlutterUI.mount(
                detectorAround(inner, null), new RenderHost(), new BuildOwner());
        e.layout(BoxConstraints.tight(200, 100));
        e.position(0, 0);

        Element content = e.contentElement();
        RenderElement button = RenderElement.findRenderElement(content);
        assertNotNull(button, "the button must be a render element of the content subtree");
        assertSame(inner, button.widget(),
                "and it must be the button itself, not the detector's overlay");
    }

    @Test
    @DisplayName("the overlay is still a separate element, so bare content stays tappable")
    void theOverlayStillExists() {
        final int[] taps = {0};
        GestureDetector g = detectorAround(new ProbeBox(50, 50), new Funcs.VoidFunc0() {
            @Override
            public void call() {
                taps[0]++;
            }
        });
        GestureRenderElement e = (GestureRenderElement)
                FlutterUI.mount(g, new RenderHost(), new BuildOwner());
        e.layout(BoxConstraints.tight(50, 50));

        final int[] children = {0};
        e.visitChildren(new Funcs.VoidFunc1<Element>() {
            @Override
            public void call(Element c) {
                children[0]++;
            }
        });
        // content + overlay: losing the overlay would make every wrapped label dead.
        org.junit.jupiter.api.Assertions.assertEquals(2, children[0]);
    }
}
