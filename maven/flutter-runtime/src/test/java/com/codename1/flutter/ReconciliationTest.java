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

import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.rendering.Size;
import com.codename1.flutter.testsupport.AltBox;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.testsupport.Toggler;
import com.codename1.flutter.widgets.Column;

import dart.core.DartList;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Element reconciliation driven synchronously via BuildOwner.flushSync (no
 * Display, so nothing schedules on the EDT).
 */
class ReconciliationTest {

    private BuildOwner owner;
    private RenderHost host;

    private Toggler.TogglerState mountToggler(Widget initialChild) {
        owner = new BuildOwner();
        host = new RenderHost();
        Element root = FlutterUI.mount(new Toggler(initialChild), host, owner);
        return (Toggler.TogglerState) ((StatefulElement) root).state();
    }

    @Test
    void sameTypeRebuildReusesTheRenderElement() {
        Toggler.TogglerState state = mountToggler(new ProbeBox(10, 10));
        assertEquals(1, state.initStateCalls);

        RenderElement before = host.rootRenderElement();
        assertTrue(before instanceof ProbeBox.ProbeBoxElement);

        final ProbeBox bigger = new ProbeBox(20, 20);
        state.setState(() -> state.child = bigger);
        owner.flushSync();

        RenderElement after = host.rootRenderElement();
        assertSame(before, after, "same widget type must update the element in place");
        assertSame(bigger, after.widget(), "element must hold the new widget config");
        assertTrue(after.isMounted());

        // The updated config drives layout.
        assertEquals(new Size(20, 20), after.layout(BoxConstraints.loose(100, 100)));
    }

    @Test
    void differentTypeRebuildReplacesTheRenderElement() {
        Toggler.TogglerState state = mountToggler(new ProbeBox(10, 10));
        RenderElement before = host.rootRenderElement();

        state.setState(() -> state.child = new AltBox(5, 5));
        owner.flushSync();

        RenderElement after = host.rootRenderElement();
        assertNotSame(before, after);
        assertTrue(after instanceof AltBox.AltBoxElement);
        assertFalse(before.isMounted(), "the replaced element must be unmounted");
        assertTrue(after.isMounted());
    }

    @Test
    void statefulElementSurvivesParentRebuildAndDisposesOnRemoval() {
        Toggler inner = new Toggler(new ProbeBox(1, 1));
        Toggler.TogglerState outer = mountToggler(inner);

        // Find the inner stateful element.
        StatefulElement rootElement = (StatefulElement) host.rootElement();
        Element innerElement = rootElement.child();
        assertTrue(innerElement instanceof StatefulElement);
        Toggler.TogglerState innerState = (Toggler.TogglerState) ((StatefulElement) innerElement).state();
        assertEquals(1, innerState.initStateCalls);

        // Parent rebuild with a same-type widget keeps the inner state alive.
        state_setChild(outer, new Toggler(new ProbeBox(2, 2)));
        assertSame(innerElement, rootElement.child(), "canUpdate match must reuse the stateful element");
        assertEquals(0, innerState.disposeCalls);

        // Replacing with a different type disposes the inner state.
        state_setChild(outer, new AltBox(3, 3));
        assertEquals(1, innerState.disposeCalls);
        assertFalse(innerElement.isMounted());
    }

    private void state_setChild(final Toggler.TogglerState s, final Widget w) {
        s.setState(() -> s.child = w);
        owner.flushSync();
    }

    @Test
    void multiChildKeyedReconciliationReusesMovedChildren() {
        ProbeBox a = keyed(new ProbeBox(10, 10), "a");
        ProbeBox b = keyed(new ProbeBox(20, 20), "b");
        ProbeBox c = keyed(new ProbeBox(30, 30), "c");

        Column col1 = new Column();
        col1.children(DartList.of((Widget) a, b, c));

        Toggler.TogglerState state = mountToggler(col1);
        RenderElement flexBefore = host.rootRenderElement();
        List<RenderElement> before = flexBefore.renderChildren();
        assertEquals(3, before.size());
        RenderElement elA = before.get(0);
        RenderElement elC = before.get(2);

        // Reorder: c, a — b removed.
        Column col2 = new Column();
        col2.children(DartList.of((Widget) keyed(new ProbeBox(30, 30), "c"), keyed(new ProbeBox(10, 10), "a")));
        state.setState(() -> state.child = col2);
        owner.flushSync();

        RenderElement flexAfter = host.rootRenderElement();
        assertSame(flexBefore, flexAfter);
        List<RenderElement> after = flexAfter.renderChildren();
        assertEquals(2, after.size());
        assertSame(elC, after.get(0), "keyed child c must be reused across the move");
        assertSame(elA, after.get(1), "keyed child a must be reused across the move");
        assertFalse(before.get(1).isMounted(), "removed keyed child b must be unmounted");
    }

    private static ProbeBox keyed(ProbeBox box, String key) {
        box.key(new ValueKey<String>(key));
        return box;
    }
}
