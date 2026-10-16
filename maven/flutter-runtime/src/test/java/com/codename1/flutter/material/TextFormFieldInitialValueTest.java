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
package com.codename1.flutter.material;

import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.RenderHost;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A {@code TextFormField} with no {@code controller} must still show {@code
 * initialValue} -- Flutter creates an internal {@code TextEditingController(text:
 * initialValue)}, kept alive by the field's own {@code State} across rebuilds (not
 * rebuilt from a fresh widget instance every time, the way a plain {@code
 * StatelessWidget} field would be). Consequences pinned here, all from Flutter's own
 * {@code _TextFormFieldState}: user edits are never reset by a rebuild, a LATER change
 * of {@code initialValue} does not overwrite what is already showing, and switching to
 * an external controller mid-life works.
 */
class TextFormFieldInitialValueTest {

    private static Element mount(Widget w) {
        RenderHost host = new RenderHost();
        return FlutterUI.mount(w, host, new BuildOwner());
    }

    /** The {@link TextField} this {@code TextFormField} rendered, and its controller. */
    private static TextEditingController boundController(Element formFieldElement) {
        RenderElement field = RenderElement.findRenderElement(formFieldElement);
        return ((TextField) field.widget()).getController();
    }

    @Test
    void initialValueShowsWithNoController() {
        TextFormField f = new TextFormField();
        f.initialValue("hello");

        Element e = mount(f);
        TextEditingController ctl = boundController(e);

        assertNotNull(ctl, "a field with no controller must still get one internally");
        assertEquals("hello", ctl.text());
    }

    @Test
    void aRebuildWithADifferentInitialValueDoesNotOverwriteUserEdits() {
        TextFormField f = new TextFormField();
        f.initialValue("hello");
        Element e = mount(f);

        // The user types, directly into the controller this field created for itself --
        // exactly what a real keystroke does through TextFieldRenderElement.userEdited.
        boundController(e).setText("edited by the user");

        // A rebuild is a FRESH TextFormField instance, as the app's own build() method
        // hands down every time -- with a DIFFERENT initialValue, as a rebuild that
        // recomputed it from some other state might.
        TextFormField rebuilt = new TextFormField();
        rebuilt.initialValue("something else entirely");
        e.update(rebuilt);

        assertEquals("edited by the user", boundController(e).text(),
                "a later initialValue must not overwrite what the controller already holds");
    }

    @Test
    void switchingToAnExternalControllerWorks() {
        TextFormField f = new TextFormField();
        f.initialValue("hello");
        Element e = mount(f);

        TextEditingController external = new TextEditingController();
        external.setText("from the app");
        TextFormField rebuilt = new TextFormField();
        rebuilt.controller(external);
        e.update(rebuilt);

        assertSame(external, boundController(e), "the external controller must now be in use");
        assertEquals("from the app", boundController(e).text());
    }
}
