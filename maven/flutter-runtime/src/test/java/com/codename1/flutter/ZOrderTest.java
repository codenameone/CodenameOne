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
import com.codename1.flutter.testsupport.AltMarkerBox;
import com.codename1.flutter.testsupport.MarkerBox;
import com.codename1.flutter.testsupport.Toggler;
import com.codename1.flutter.widgets.Column;

import dart.core.DartList;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The flat container's child order (RenderHost attach order) must match
 * element-tree order — in particular a render element REPLACED mid-life must
 * insert its component at the tree-order index instead of appending at the
 * end.
 */
class ZOrderTest {

    private BuildOwner owner;
    private RenderHost host;

    private Toggler.TogglerState mount(Widget middle) {
        owner = new BuildOwner();
        host = new RenderHost();
        Column col = new Column();
        col.children(DartList.of((Widget) new MarkerBox("a"), new Toggler(middle), new MarkerBox("c")));
        Element root = FlutterUI.mount(col, host, owner);
        RenderElement flex = host.rootRenderElement();
        Element togglerElement = childAt(flex, 1);
        return (Toggler.TogglerState) ((StatefulElement) togglerElement).state();
    }

    private static Element childAt(Element parent, int index) {
        final List<Element> kids = new ArrayList<Element>();
        parent.visitChildren(new dart.runtime.Funcs.VoidFunc1<Element>() {
            @Override
            public void call(Element c) {
                kids.add(c);
            }
        });
        return kids.get(index);
    }

    private List<String> attachTags() {
        List<String> tags = new ArrayList<String>();
        for (RenderElement r : host.attachOrder()) {
            Widget w = r.widget();
            if (w instanceof MarkerBox) {
                tags.add(((MarkerBox) w).tag());
            } else if (w instanceof AltMarkerBox) {
                tags.add(((AltMarkerBox) w).tag());
            } else {
                tags.add("?");
            }
        }
        return tags;
    }

    @Test
    void initialMountAttachesInTreeOrder() {
        mount(new MarkerBox("b"));
        assertEquals(List.of("a", "b", "c"), attachTags());
    }

    @Test
    void midLifeReplacementInsertsAtTreeOrderIndexNotAtTheEnd() {
        Toggler.TogglerState state = mount(new MarkerBox("b"));

        state.setState(() -> state.child = new AltMarkerBox("b2"));
        owner.flushSync();

        // Without index-aware attach the replacement would land at the end
        // ("a", "c", "b2"), drifting the z-order.
        assertEquals(List.of("a", "b2", "c"), attachTags());
    }

    @Test
    void repeatedReplacementKeepsTheOrderStable() {
        Toggler.TogglerState state = mount(new MarkerBox("b"));

        state.setState(() -> state.child = new AltMarkerBox("b2"));
        owner.flushSync();
        state.setState(() -> state.child = new MarkerBox("b3"));
        owner.flushSync();

        assertEquals(List.of("a", "b3", "c"), attachTags());
    }

    @Test
    void replacementOfTheFirstChildInsertsAtTheFront() {
        owner = new BuildOwner();
        host = new RenderHost();
        Column col = new Column();
        Toggler first = new Toggler(new MarkerBox("a"));
        col.children(DartList.of((Widget) first, new MarkerBox("b"), new MarkerBox("c")));
        FlutterUI.mount(col, host, owner);
        Toggler.TogglerState state = (Toggler.TogglerState)
                ((StatefulElement) childAt(host.rootRenderElement(), 0)).state();
        assertEquals(List.of("a", "b", "c"), attachTags());

        state.setState(() -> state.child = new AltMarkerBox("a2"));
        owner.flushSync();
        assertEquals(List.of("a2", "b", "c"), attachTags());
    }
}
