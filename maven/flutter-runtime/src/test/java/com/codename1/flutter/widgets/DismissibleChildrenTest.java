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
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;

import dart.runtime.Funcs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An element that owns children must let the tree walk see them.
 *
 * <p>{@code Element.visitChildren} is empty by default, so owning children and not
 * overriding it owns them invisibly -- and the walk that unmounts a subtree is the one
 * that suffers. A dismissed row was deactivated while its child and both backgrounds
 * stayed mounted with their components still in the scene, so every surviving row drew
 * its new content over its old and the lists inside those rows were never unmounted.</p>
 */
class DismissibleChildrenTest {

    private static List<Element> visited(Dismissible w) {
        RenderHost host = new RenderHost();
        RenderElement e = (RenderElement) FlutterUI.mount(w, host, new BuildOwner());
        e.layout(BoxConstraints.loose(200, 200));
        e.position(0, 0);
        final List<Element> seen = new ArrayList<Element>();
        e.visitChildren(new Funcs.VoidFunc1<Element>() {
            @Override
            public void call(Element c) {
                seen.add(c);
            }
        });
        return seen;
    }

    @Test
    @DisplayName("all three children are visited")
    void visitsChildAndBothBackgrounds() {
        Dismissible w = new Dismissible();
        w.child(new ProbeBox(60, 40));
        w.background(new ProbeBox(60, 40));
        w.secondaryBackground(new ProbeBox(60, 40));
        assertEquals(3, visited(w).size(), "child, background and secondaryBackground");
    }

    @Test
    @DisplayName("only the children that exist are visited")
    void visitsOnlyWhatIsThere() {
        Dismissible w = new Dismissible();
        w.child(new ProbeBox(60, 40));
        assertEquals(1, visited(w).size(), "just the child");
    }
}
