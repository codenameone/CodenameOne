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
    void testDisablingToggleRestoresTheUiidInsideAComponentGroup() {
        // A horizontal ComponentGroup renames its members to ToggleButtonFirst,
        // ToggleButton and ToggleButtonLast, and records what it replaced in
        // $origUIID so it can put it back on removal. A restore that only matched
        // the bare name skipped all of those, and the group then reinstated
        // ToggleButton on a control that was no longer a toggle.
        ComponentGroup group = new ComponentGroup();
        group.setForceGroup(true);
        group.setHorizontal(true);
        RadioButton first = new RadioButton("One");
        RadioButton last = new RadioButton("Two");
        first.setToggle(true);
        last.setToggle(true);
        group.addComponent(first);
        group.addComponent(last);
        assertEquals("ToggleButtonFirst", first.getUIID());

        first.setToggle(false);

        assertFalse(first.isToggle());
        assertEquals("ToggleButtonFirst", first.getUIID(),
                "the live UIID belongs to the group while the control is in it: the "
                        + "group renames every member for the segmented edges and does "
                        + "not re-apply that when the UIID changes under it");

        group.removeComponent(first);
        assertEquals("RadioButton", first.getUIID(),
                "leaving the group must not reinstate the toggle UIID");
    }

    @Test
    void testDisablingToggleRestoresTheUiidInsideAVerticalComponentGroup() {
        // A vertical group keeps the default prefix, so its members are renamed
        // GroupElementFirst and the like -- names with nothing toggle-ish about
        // them -- while $origUIID still holds ToggleButton. Matching on the live
        // UIID alone therefore missed this case entirely, and removing the control
        // put ToggleButton back on a plain radio.
        ComponentGroup group = new ComponentGroup();
        group.setForceGroup(true);
        RadioButton first = new RadioButton("One");
        RadioButton last = new RadioButton("Two");
        first.setToggle(true);
        last.setToggle(true);
        group.addComponent(first);
        group.addComponent(last);
        assertEquals("GroupElementFirst", first.getUIID());

        first.setToggle(false);
        group.removeComponent(first);

        assertFalse(first.isToggle());
        assertEquals("RadioButton", first.getUIID(),
                "leaving a vertical group must not reinstate the toggle UIID");
    }

    @Test
    void testTogglingOffAndOnInsideAGroupLeavesTheGroupHoldingTheToggleUiid() {
        // The group saves what it replaced and restores it on removal, so both
        // directions of setToggle have to keep that saved name in step. Only the
        // disable direction did: re-enabling saw a group alias as the live UIID,
        // recognised nothing, and left the group holding RadioButton for a control
        // that was a toggle again.
        ComponentGroup group = new ComponentGroup();
        group.setForceGroup(true);
        group.setHorizontal(true);
        RadioButton first = new RadioButton("One");
        RadioButton last = new RadioButton("Two");
        first.setToggle(true);
        last.setToggle(true);
        group.addComponent(first);
        group.addComponent(last);

        first.setToggle(false);
        first.setToggle(true);

        assertTrue(first.isToggle());
        group.removeComponent(first);
        assertEquals("ToggleButton", first.getUIID(),
                "a control that is a toggle again must not leave the group as a radio");
    }

    @Test
    void testTogglingInsideAnInactiveGroupStillRestoresTheUiid() {
        // ComponentGroupBool is off by default -- Android Material never sets it --
        // and an unforced group then returns from updateUIIDs without renaming
        // anything. The live UIID is still the control's own in that case, so
        // skipping the restore merely because the parent is a group left a plain
        // radio wearing the toggle style.
        ComponentGroup group = new ComponentGroup();
        RadioButton radio = new RadioButton("Choice");
        radio.setToggle(true);
        group.addComponent(radio);
        assertEquals("ToggleButton", radio.getUIID());

        radio.setToggle(false);

        assertEquals("RadioButton", radio.getUIID());
    }

    @Test
    void testTogglingInsideAGroupKeepsAnApplicationsOwnUiid() {
        // setToggle has only ever converted the two default UIIDs; a UIID the
        // application chose is left alone. The grouped path has to honour that too,
        // or the group hands back ToggleButton instead of the custom name.
        ComponentGroup group = new ComponentGroup();
        group.setForceGroup(true);
        group.setHorizontal(true);
        RadioButton custom = new RadioButton("Choice");
        custom.setUIID("MyChoice");
        RadioButton other = new RadioButton("Other");
        group.addComponent(custom);
        group.addComponent(other);

        custom.setToggle(true);
        group.removeComponent(custom);

        assertEquals("MyChoice", custom.getUIID(),
                "a UIID the application set must survive toggle mode");
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
