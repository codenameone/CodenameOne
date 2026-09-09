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
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import dart.core.DartList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Widgets whose job is to NOT show something must not show it.
 *
 * <p>Both of these drew their child anyway. Crane's back layer is an
 * {@code IndexedStack} over three forms, so the study opened with all three
 * printed on top of one another — no error, just an unreadable screen.</p>
 */
class HiddenChildrenTest {

    private static RenderElement laidOut(Widget root, double w, double h) {
        Element e = FlutterUI.mount(root, new RenderHost(), new BuildOwner());
        RenderElement r = RenderElement.findRenderElement(e);
        r.layout(BoxConstraints.loose(w, h));
        r.position(0, 0);
        return r;
    }

    /** Every ProbeBox in the tree, with the size it was laid out at. */
    private static List<RenderElement> probes(RenderElement from) {
        List<RenderElement> out = new ArrayList<RenderElement>();
        collect(from, out);
        return out;
    }

    private static void collect(RenderElement from, List<RenderElement> out) {
        from.visitChildren(child -> {
            RenderElement r = RenderElement.findRenderElement(child);
            if (r != null) {
                if (r.widget() instanceof ProbeBox) {
                    out.add(r);
                }
                collect(r, out);
            }
        });
    }

    @Test
    @DisplayName("an IndexedStack draws only the selected child")
    void indexedStackShowsOne() {
        IndexedStack stack = new IndexedStack();
        stack.index(1);
        stack.children(DartList.<Widget>of(
                new ProbeBox(100, 40), new ProbeBox(200, 60), new ProbeBox(300, 80)));

        RenderElement r = laidOut(stack, 400, 400);

        assertEquals(200, r.size().width(), 1e-9, "sized to the SELECTED child");
        assertEquals(60, r.size().height(), 1e-9);

        List<RenderElement> kids = probes(r);
        assertEquals(3, kids.size(), "the others stay in the tree, keeping their state");
        assertEquals(0, kids.get(0).size().width(), 1e-9, "but take no space");
        assertEquals(200, kids.get(1).size().width(), 1e-9);
        assertEquals(0, kids.get(2).size().width(), 1e-9);
    }

    @Test
    @DisplayName("visible: false shows nothing")
    void invisibleChildIsNotDrawn() {
        Visibility v = new Visibility();
        v.visible(false);
        v.child(new ProbeBox(120, 40));

        RenderElement r = laidOut(v, 400, 400);

        assertEquals(0, r.size().width(), 1e-9);
        assertEquals(0, r.size().height(), 1e-9);
        assertEquals(0, probes(r).size(), "the child is not in the tree at all");
    }

    @Test
    @DisplayName("visible: true is unchanged")
    void visibleChildIsDrawn() {
        Visibility v = new Visibility();
        v.child(new ProbeBox(120, 40));

        RenderElement r = laidOut(v, 400, 400);

        assertEquals(120, r.size().width(), 1e-9);
        assertEquals(1, probes(r).size());
    }

    @Test
    @DisplayName("visible: false with a replacement shows the replacement")
    void replacementIsDrawn() {
        Visibility v = new Visibility();
        v.visible(false);
        v.child(new ProbeBox(120, 40));
        v.replacement(new ProbeBox(10, 10));

        RenderElement r = laidOut(v, 400, 400);

        assertEquals(10, r.size().width(), 1e-9);
    }
}
