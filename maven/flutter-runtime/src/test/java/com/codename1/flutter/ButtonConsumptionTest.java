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

import com.codename1.flutter.material.ButtonRenderElement;
import com.codename1.flutter.material.ElevatedButton;
import com.codename1.flutter.material.IconButton;
import com.codename1.flutter.material.OutlinedButton;
import com.codename1.flutter.material.TextButton;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.Icon;
import com.codename1.flutter.widgets.Text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Buttons consume a Text child as their label and an Icon child as their
 * material glyph (headless — the CN1 Button itself only exists with a
 * Display).
 */
class ButtonConsumptionTest {

    private ButtonRenderElement mount(Widget w) {
        BuildOwner owner = new BuildOwner();
        RenderHost host = new RenderHost();
        FlutterUI.mount(w, host, owner);
        return (ButtonRenderElement) host.rootRenderElement();
    }

    @Test
    void textChildBecomesTheLabel() {
        ElevatedButton b = new ElevatedButton();
        b.onPressed(() -> {
        });
        b.child(new Text("Save"));
        ButtonRenderElement el = mount(b);
        assertEquals("Save", el.consumedLabel());
        assertEquals(0, el.consumedIconChar());
    }

    @Test
    void iconChildBecomesTheMaterialGlyph() {
        TextButton b = new TextButton();
        b.child(new Icon(Icons.add));
        ButtonRenderElement el = mount(b);
        assertNull(el.consumedLabel());
        assertEquals(Icons.add.codePoint(), el.consumedIconChar());
    }

    @Test
    void outlinedButtonConsumesLikeTheOthers() {
        OutlinedButton b = new OutlinedButton();
        b.child(new Text("Cancel"));
        ButtonRenderElement el = mount(b);
        assertEquals("Cancel", el.consumedLabel());
    }

    @Test
    void iconButtonConsumesItsIconParameter() {
        IconButton b = new IconButton();
        b.icon(new Icon(Icons.settings));
        b.iconSize(32.0);
        ButtonRenderElement el = mount(b);
        assertNull(el.consumedLabel());
        assertEquals(Icons.settings.codePoint(), el.consumedIconChar());
    }

    /**
     * Unresolvable content yields NO label — it used to yield the widget's toString().
     *
     * <p>This test asserted that fallback, and the fallback was the bug: a button whose
     * content was neither a Text nor an Icon drew its Java class name, which is how the
     * gallery's back button came out as "com.codename1.flutter.material.BackButtonIcon@…"
     * across the app bar. A class name is never a label anyone meant to show.</p>
     */
    @Test
    void unsupportedChildYieldsNoLabelRatherThanAClassName() {
        ElevatedButton b = new ElevatedButton();
        b.child(new ProbeBox(1, 1));
        ButtonRenderElement el = mount(b);
        assertNull(el.consumedLabel(), "no label beats a class name");
    }

    @Test
    void missingChildYieldsNoLabelAndNoIcon() {
        TextButton b = new TextButton();
        ButtonRenderElement el = mount(b);
        assertNull(el.consumedLabel());
        assertEquals(0, el.consumedIconChar());
    }
}
