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
import com.codename1.flutter.testsupport.RasterDisplay;
import com.codename1.ui.Component;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A nested {@code IgnorePointer} must stand on its own decision: an outer one is a
 * local hitTest guard, not a blanket write over whatever is beneath it.
 *
 * <p>Before this fix, an outer {@code IgnorePointer(ignoring: false)} re-walked the
 * WHOLE render subtree -- including a nested {@code IgnorePointer(ignoring: true)}'s own
 * -- and stamped {@code false} over every component in it, undoing the inner one's
 * deafness. Flutter's own {@code RenderIgnorePointer.hitTest} is {@code !ignoring &&
 * super.hitTest(...)}: an ignoring ancestor's subtree is unreachable no matter what is
 * nested inside it, so the effective state is the OR of every ancestor's flag -- a
 * {@code false} wrapper may never clear a nested {@code true}, while a {@code true}
 * wrapper still dominates a nested {@code false}.</p>
 *
 * <p>Needs a REAL {@link Component} to assert on ({@link RasterDisplay} installed): a
 * headless {@code createComponent()} returns null for every element here, which would
 * make the walk this test depends on vacuous.</p>
 */
class IgnorePointerNestingTest {

    @BeforeEach
    void install() {
        RasterDisplay.install();
    }

    @AfterEach
    void uninstall() {
        RasterDisplay.uninstall();
    }

    private static RenderElement mount(IgnorePointer w) {
        RenderHost host = new RenderHost();
        RenderElement e = (RenderElement) FlutterUI.mount(w, host, new BuildOwner());
        e.layout(BoxConstraints.loose(200, 200));
        e.position(0, 0);
        return e;
    }

    /** The first component found under {@code e}, by descending through pass-throughs. */
    private static Component firstComponent(RenderElement e) {
        for (RenderElement child : e.renderChildren()) {
            Component c = child.component();
            if (c != null) {
                return c;
            }
            Component deeper = firstComponent(child);
            if (deeper != null) {
                return deeper;
            }
        }
        return null;
    }

    @Test
    @DisplayName("outer ignoring:false must not re-enable a nested ignoring:true")
    void outerFalseDoesNotReenableNestedTrue() {
        ColoredBox leaf = new ColoredBox();

        IgnorePointer inner = new IgnorePointer();
        inner.ignoring(Boolean.TRUE);
        inner.child(leaf);

        IgnorePointer outer = new IgnorePointer();
        outer.ignoring(Boolean.FALSE);
        outer.child(inner);

        RenderElement e = mount(outer);
        Component c = firstComponent(e);
        assertTrue(c != null, "the leaf must actually be mounted with a real component");
        assertTrue(c.isIgnorePointerEvents(),
                "the nested ignoring:true must survive the outer ignoring:false");
    }

    @Test
    @DisplayName("outer ignoring:true dominates a nested ignoring:false")
    void outerTrueOverridesNestedFalse() {
        ColoredBox leaf = new ColoredBox();

        IgnorePointer inner = new IgnorePointer();
        inner.ignoring(Boolean.FALSE);
        inner.child(leaf);

        IgnorePointer outer = new IgnorePointer();
        outer.ignoring(Boolean.TRUE);
        outer.child(inner);

        RenderElement e = mount(outer);
        Component c = firstComponent(e);
        assertTrue(c != null, "the leaf must actually be mounted with a real component");
        assertTrue(c.isIgnorePointerEvents(),
                "an ignoring ancestor makes its whole subtree unreachable, regardless of "
                        + "what is nested inside it -- Flutter's hitTest never even calls "
                        + "the nested one's");
    }
}
