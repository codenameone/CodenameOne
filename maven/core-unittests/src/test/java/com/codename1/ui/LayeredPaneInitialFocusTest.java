/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.TextModeLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asking a form for its layered pane before showing it used to leave the first text field
 * focused and blinking a caret, while asking after showing it did not (issue #2710).
 *
 * <p>{@code wrapInLayeredPane()} initialized the wrapper it splices around the content pane
 * unconditionally, so on a form that had never been shown it ran {@code initComponent()} over
 * the whole tree - which is what makes an {@link InputComponent} build its editor. The editor
 * then existed when {@code show()} chose the initial focus, and the field the user had never
 * touched came up focused and blinking.</p>
 */
class LayeredPaneInitialFocusTest extends UITestBase {

    private static TextComponent addField(Form f) {
        TextComponent tc = new TextComponent().label("TestField");
        f.add(tc);
        return tc;
    }

    @FormTest
    void askingForTheLayeredPaneBeforeShowingDoesNotInitializeTheForm() {
        Form f = new Form("Test", new TextModeLayout(1, 1));
        TextComponent tc = addField(f);

        f.getLayeredPane();

        assertFalse(f.getContentPane().isInitialized(),
                "a form that was never shown must not be initialized by getLayeredPane()");
        assertEquals(0, tc.getComponentCount(),
                "the input component must not build its editor before the form is shown");
    }

    @FormTest
    void theLayeredPaneStillWorksAndIsInitializedOnceTheFormIsShown() {
        Form f = new Form("Test", new TextModeLayout(1, 1));
        addField(f);
        Container pane = f.getLayeredPane();
        Label marker = new Label("marker");
        pane.add(marker);

        f.show();

        assertTrue(f.getContentPane().isInitialized(), "showing the form initializes the content pane");
        assertTrue(pane.isInitialized(), "and the layered pane that was created before it");
        assertTrue(marker.isInitialized(), "along with everything already inside the layered pane");
        assertSame(pane, f.getLayeredPane(), "the pane created before the show is the one still in use");
    }

    @FormTest
    void gettingTheLayeredPaneBeforeShowingLeavesTheFieldUnfocused() {
        Form f = new Form("Test", new TextModeLayout(1, 1));
        TextComponent tc = addField(f);
        f.getLayeredPane();
        f.show();

        assertNull(f.getFocused(), "no component asked for the focus, so nothing may hold it");
        assertFalse(tc.getField().hasFocus(), "the text field must not be focused");
    }

    @FormTest
    void gettingTheLayeredPaneAfterShowingBehavesTheSameWay() {
        Form f = new Form("Test", new TextModeLayout(1, 1));
        TextComponent tc = addField(f);
        f.show();
        f.getLayeredPane();

        assertNull(f.getFocused(), "the order of the two calls must not change the focus");
        assertFalse(tc.getField().hasFocus(), "the text field must not be focused");
    }

}
