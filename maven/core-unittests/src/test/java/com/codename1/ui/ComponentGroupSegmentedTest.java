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

import com.codename1.junit.UITestBase;
import com.codename1.ui.plaf.UIManager;
import org.junit.jupiter.api.Test;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A horizontal ComponentGroup is a segmented control, and it is the only use of
/// ComponentGroup the current native themes carry any styling for: they style
/// ToggleButton and its First/Last/Only variants in full and define no GroupElement
/// or ButtonGroup rule at all. Neither of them sets ComponentGroupBool, so gating the
/// segmented path on that constant made setHorizontal(true) a no-op everywhere it
/// actually matters.
class ComponentGroupSegmentedTest extends UITestBase {

    private static void activateGrouping(boolean on) {
        Hashtable<String, Object> theme = new Hashtable<String, Object>();
        if (on) {
            theme.put("@ComponentGroupBool", "true");
        }
        UIManager.getInstance().setThemeProps(theme);
    }

    @Test
    void testHorizontalGroupRenamesItsMembersWithoutTheThemeConstant() {
        activateGrouping(false);
        ComponentGroup group = new ComponentGroup();
        group.setHorizontal(true);
        Button first = new Button("One");
        Button last = new Button("Two");
        group.addComponent(first);
        group.addComponent(last);

        assertEquals("ToggleButtonFirst", first.getUIID(),
                "a segmented control has to style its edges, or the theme's "
                        + "ToggleButtonFirst/Last rules are unreachable");
        assertEquals("ToggleButtonLast", last.getUIID());
    }

    @Test
    void testASingleMemberGetsTheOnlyVariant() {
        activateGrouping(false);
        ComponentGroup group = new ComponentGroup();
        group.setHorizontal(true);
        Button only = new Button("One");
        group.addComponent(only);

        assertEquals("ToggleButtonOnly", only.getUIID());
    }

    @Test
    void testVerticalGroupStillHonoursTheThemeConstant() {
        // The constant still governs the vertical GroupElement path. That path
        // flattens Labels, TextFields and Buttons onto one UIID, so a theme that
        // does not style GroupElement must be able to opt out -- which is the
        // reason the flag exists and the reason this change does not remove it.
        activateGrouping(false);
        ComponentGroup group = new ComponentGroup();
        Button b = new Button("One");
        group.addComponent(b);

        assertEquals("Button", b.getUIID(),
                "an inactive vertical group must leave its members alone");
    }

    @Test
    void testHorizontalGroupWithACustomElementUiidStillHonoursTheConstant() {
        // Activation is deliberately narrow: it recognises the ToggleButton UIIDs
        // setHorizontal installs, not "horizontal" on its own. An application that
        // chose its own UIID gets the documented theme-gated behaviour.
        activateGrouping(false);
        ComponentGroup group = new ComponentGroup();
        group.setElementUIID("MyElement");
        group.setHorizontal(true);
        Button b = new Button("One");
        group.addComponent(b);

        assertEquals("Button", b.getUIID(),
                "a custom element UIID is not the framework's segmented control");
    }

    @Test
    void testLeavingHorizontalRestoresTheOriginalUiids() {
        activateGrouping(false);
        ComponentGroup group = new ComponentGroup();
        group.setHorizontal(true);
        Button b = new Button("One");
        group.addComponent(b);
        assertNotEquals("Button", b.getUIID());

        group.setHorizontal(false);

        assertEquals("Button", b.getUIID(),
                "updateUIIDs only renames, so the path out of the segmented state "
                        + "has to restore or the member keeps a name nothing styles");
    }

    @Test
    void testSetForceGroupAppliesWithoutWaitingForAnotherEvent() {
        // setForceGroup only assigned the field, so it took effect if and only if
        // something later happened to call updateUIIDs -- adding a component, or a
        // theme refresh. On a group already populated it did nothing at all.
        activateGrouping(false);
        ComponentGroup group = new ComponentGroup();
        Button b = new Button("One");
        group.addComponent(b);
        assertEquals("Button", b.getUIID());

        group.setForceGroup(true);

        // a Button takes buttonUIID ("ButtonGroup"), not elementUIID -- see elementPrefix
        assertEquals("ButtonGroupOnly", b.getUIID(),
                "the setter has to apply the change it records");
    }

    @Test
    void testClearingForceGroupRestores() {
        activateGrouping(false);
        ComponentGroup group = new ComponentGroup();
        Button b = new Button("One");
        group.addComponent(b);
        group.setForceGroup(true);
        assertEquals("ButtonGroupOnly", b.getUIID());

        group.setForceGroup(false);

        assertEquals("Button", b.getUIID(),
                "turning forcing off has to hand the UIID back");
    }

    @Test
    void testASegmentedGroupStaysActiveWhenForcingIsCleared() {
        // Deliberate behaviour change: a horizontal ToggleButton group is active on its
        // own, so clearing forceGroup no longer deactivates it. Before this, the only
        // way such a group ever grouped was the flag or forcing, which is the defect.
        activateGrouping(false);
        ComponentGroup group = new ComponentGroup();
        group.setForceGroup(true);
        group.setHorizontal(true);
        Button b = new Button("One");
        group.addComponent(b);
        assertEquals("ToggleButtonOnly", b.getUIID());

        group.setForceGroup(false);

        assertEquals("ToggleButtonOnly", b.getUIID(),
                "a segmented control does not stop being one because forcing was cleared");
    }

    @Test
    void testChangingTheElementUiidOffToggleButtonRestores() {
        // updateUIIDs only renames and returns early once the group is inactive, so a
        // setter that deactivates the group and then calls it strands the members on
        // the names they had in the state the group just left.
        activateGrouping(false);
        ComponentGroup group = new ComponentGroup();
        group.setHorizontal(true);
        Button b = new Button("One");
        group.addComponent(b);
        assertEquals("ToggleButtonOnly", b.getUIID());

        group.setElementUIID("MyElement");

        assertEquals("Button", b.getUIID(),
                "a custom element UIID is theme-gated, so leaving the segmented UIID "
                        + "has to hand the member back rather than strand it");
    }

    @Test
    void testChangingTheGroupFlagToOneTheThemeSetsActivates() {
        // setGroupFlag assigned the field and did nothing else, so naming a constant the
        // theme does set never took effect.
        Hashtable<String, Object> theme = new Hashtable<String, Object>();
        theme.put("@MyGroupBool", "true");
        UIManager.getInstance().setThemeProps(theme);

        ComponentGroup group = new ComponentGroup();
        Button b = new Button("One");
        group.addComponent(b);
        assertEquals("Button", b.getUIID());

        group.setGroupFlag("MyGroupBool");

        assertEquals("ButtonGroupOnly", b.getUIID(),
                "naming a constant the theme sets has to activate the group");
    }

    @Test
    void testChangingTheGroupFlagAwayFromOneTheThemeSetsRestores() {
        Hashtable<String, Object> theme = new Hashtable<String, Object>();
        theme.put("@ComponentGroupBool", "true");
        UIManager.getInstance().setThemeProps(theme);

        ComponentGroup group = new ComponentGroup();
        Button b = new Button("One");
        group.addComponent(b);
        assertEquals("ButtonGroupOnly", b.getUIID());

        group.setGroupFlag("SomeConstantNoThemeSets");

        assertEquals("Button", b.getUIID(),
                "pointing at a constant nothing sets deactivates the group");
    }

    @Test
    void testTheGroupFlagPropertySetterUsesItsArgument() {
        // It passed the field instead of the value, so the property assigned the flag
        // to itself and a designer edit was silently discarded.
        Hashtable<String, Object> theme = new Hashtable<String, Object>();
        theme.put("@MyGroupBool", "true");
        UIManager.getInstance().setThemeProps(theme);

        ComponentGroup group = new ComponentGroup();
        Button b = new Button("One");
        group.addComponent(b);

        group.setPropertyValue("groupFlag", "MyGroupBool");

        assertEquals("MyGroupBool", group.getGroupFlag());
        assertEquals("ButtonGroupOnly", b.getUIID(),
                "the property setter has to use the value it was handed");
    }

    @Test
    void testTheForceGroupPropertySetterAppliesImmediately() {
        // It assigned the field directly, so it skipped the apply the real setter does.
        activateGrouping(false);
        ComponentGroup group = new ComponentGroup();
        Button b = new Button("One");
        group.addComponent(b);
        assertEquals("Button", b.getUIID());

        group.setPropertyValue("forceGroup", Boolean.TRUE);

        assertEquals("ButtonGroupOnly", b.getUIID(),
                "the property setter has to go through setForceGroup");
    }

    @Test
    void testTurningHorizontalAppliesWhenTheUiidWasAlreadyToggleButton() {
        // setHorizontal only renamed inside its "GroupElement" branch, so a group that
        // already carried the ToggleButton UIID became segmented on the orientation
        // change and applied nothing: isGroupingActive said the group owned its members'
        // UIIDs while they still wore their own, until an unrelated insert or refresh.
        activateGrouping(false);
        ComponentGroup group = new ComponentGroup();
        Button b = new Button("One");
        group.addComponent(b);
        group.setElementUIID("ToggleButton");
        assertEquals("Button", b.getUIID(), "still vertical, so still inactive");

        group.setHorizontal(true);

        assertEquals("ToggleButtonOnly", b.getUIID(),
                "turning horizontal is what activates this group, so it has to apply");
    }

    @Test
    void testRemovingAComboFromASegmentedGroupRestoresItsPopupMode() {
        // A grouped ComboBox is forced into spinner mode. Removal restored the UIID and
        // nothing else, so the combo kept opening as a spinner after it had left. The
        // self-activating segmented group is what makes this reachable with no theme
        // constant set.
        activateGrouping(false);
        ComponentGroup group = new ComponentGroup();
        group.setHorizontal(true);
        ComboBox<String> combo = new ComboBox<String>("a", "b");
        assertFalse(combo.isActAsSpinnerDialog(), "default popup mode");
        group.addComponent(combo);
        assertTrue(combo.isActAsSpinnerDialog(), "a grouped combo opens as a spinner");

        group.removeComponent(combo);

        assertFalse(combo.isActAsSpinnerDialog(),
                "leaving the group has to hand the popup mode back");
    }

    @Test
    void testLeavingAGroupRestoresAnApplicationsOwnSpinnerChoice() {
        // The restore keyed off the group's dirty flag rather than the value the
        // application chose, so a combo deliberately set to spinner mode came back
        // as a popup.
        activateGrouping(false);
        ComponentGroup group = new ComponentGroup();
        group.setHorizontal(true);
        ComboBox<String> combo = new ComboBox<String>("a", "b");
        combo.setActAsSpinnerDialog(true);
        group.addComponent(combo);

        group.removeComponent(combo);

        assertTrue(combo.isActAsSpinnerDialog(),
                "the application asked for spinner mode, so it survives the group");
    }
}
