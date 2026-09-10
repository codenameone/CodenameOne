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
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.plaf.UIManager;
import org.junit.jupiter.api.Test;

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RadioButtonTest extends UITestBase {

    @Test
    void testDisablingToggleRestoresTheOriginalUiid() {
        RadioButton radio = new RadioButton("Choice");
        assertEquals("RadioButton", radio.getUIID());

        radio.setToggle(true);
        assertEquals("ToggleButton", radio.getUIID());

        radio.setToggle(false);
        assertFalse(radio.isToggle());
        assertEquals("RadioButton", radio.getUIID(),
                "leaving toggle mode has to put the UIID back, or the control keeps "
                        + "painting as a toggle while drawing its radio glyph again");
    }

    @Test
    void testDisablingToggleOnARadioUiidDoesNotMakeItAToggleUiid() {
        // Regression: the condition read (toggle && isCheckBox) || isRadioButton,
        // because && binds tighter than ||, so leaving toggle mode assigned
        // ToggleButton whenever the UIID was RadioButton at that moment.
        //
        // Reaching it needs toggle mode on with a RadioButton UIID, which is what
        // an application does when it sets its own UIID on a toggle: the early
        // return in setToggle means a control that was never a toggle cannot get
        // there, which is why asserting on a fresh RadioButton proves nothing.
        RadioButton radio = new RadioButton("Choice");
        radio.setToggle(true);
        radio.setUIID("RadioButton");

        radio.setToggle(false);

        assertFalse(radio.isToggle());
        assertEquals("RadioButton", radio.getUIID());
    }

    @Test
    void testCreateToggleAddsToGroupAndSetsToggleUiid() {
        ButtonGroup group = new ButtonGroup();
        RadioButton radio = RadioButton.createToggle("Choice", group);

        assertEquals(1, group.getButtonCount());
        assertTrue(radio.isToggle());
        assertEquals("ToggleButton", radio.getUIID());
    }

    @FormTest
    void testSelectionChangeNotifiesListeners() {
        RadioButton radio = new RadioButton("Pick me");
        AtomicInteger changes = new AtomicInteger();
        ActionListener listener = evt -> changes.incrementAndGet();
        radio.addChangeListener(listener);

        radio.setSelected(true);
        radio.setSelected(true);
        radio.setSelected(false);

        assertEquals(2, changes.get());
        radio.removeChangeListeners(listener);
        radio.setSelected(true);
        assertEquals(2, changes.get());
    }

    @Test
    void testReleasedHonorsUnselectAllowedFlag() {
        implementation.setBuiltinSoundsEnabled(false);
        RadioButton radio = new RadioButton("Option");
        radio.setSelected(true);
        radio.setUnselectAllowed(false);

        radio.released(0, 0);
        assertTrue(radio.isSelected());

        radio.setUnselectAllowed(true);
        radio.released(0, 0);
        assertFalse(radio.isSelected());
    }

    @Test
    void testThemeConstantControlsOppositeSide() {
        Hashtable<String, Object> theme = new Hashtable<String, Object>();
        theme.put("@radioOppositeSideBool", "true");
        UIManager.getInstance().setThemeProps(theme);

        RadioButton radio = new RadioButton("Side");
        radio.refreshTheme(false);

        assertTrue(radio.isOppositeSide());
    }
}
