/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui;

import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;

/// A component group is a container that applies the given UIID to a set of components within it
/// and gives the same UIID with "First"/"Last" prepended to the first and last components. E.g.
/// by default the  GroupElement UIID is applied so the first and last elements would have the
/// GroupElementFirst/GroupElementLast UIID's applied to them. If a group has only one element
/// the word "Only" is appended to the element UIID as in GroupElementOnly.
///
/// **Important!!!** A component group does nothing by default unless explicitly activated by
/// the theme by enabling the ComponentGroupBool constant (by default, this can be customized via the groupFlag property).
/// This allows logical grouping without changing the UI for themes that don't need grouping.
///
/// This container uses box X/Y layout (defaults to Y), other layout managers shouldn't be used
/// since this container relies on the specific behavior of the box layout.
///
/// Check out this sample code:
///
/// ```java
/// hi.add("Three Labels").
///         add(ComponentGroup.enclose(new Label("GroupElementFirst UIID"), new Label("GroupElement UIID"), new Label("GroupElementLast UIID"))).
///         add("One Label").
///         add(ComponentGroup.enclose(new Label("GroupElementOnly UIID"))).
///         add("Three Buttons").
///         add(ComponentGroup.enclose(new Button("ButtonGroupFirst UIID"), new Button("ButtonGroup UIID"), new Button("ButtonGroupLast UIID"))).
///         add("One Button").
///         add(ComponentGroup.enclose(new Button("ButtonGroupOnly UIID")));
/// ```
///
/// @author Shai Almog
public class ComponentGroup extends Container {
    private String elementUIID = "GroupElement";
    private String buttonUIID = "ButtonGroup";
    private String groupFlag = "ComponentGroupBool";
    private boolean uiidsDirty;
    private boolean forceGroup;

    /// Default constructor
    public ComponentGroup() {
        super(new BoxLayout(BoxLayout.Y_AXIS));
        setUIIDFinal("ComponentGroup");
    }

    /// Shorthand method for wrapping the given components in a vertical component group
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the components to add into a newly created group
    ///
    /// #### Returns
    ///
    /// the newly created group
    public static ComponentGroup enclose(Component... cmp) {
        ComponentGroup c = new ComponentGroup();
        for (Component cc : cmp) {
            c.add(cc);
        }
        return c;
    }

    /// Shorthand method for wrapping the given components in a horizontal component group
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the components to add into a newly created group
    ///
    /// #### Returns
    ///
    /// the newly created group
    public static ComponentGroup encloseHorizontal(Component... cmp) {
        ComponentGroup c = new ComponentGroup();
        c.setHorizontal(true);
        for (Component cc : cmp) {
            c.add(cc);
        }
        return c;
    }

    private void reverseRadio(Component cmp) {
        if (cmp instanceof ComboBox) {
            ((ComboBox) cmp).setActAsSpinnerDialog(uiidsDirty);
        }
    }

    @Override
    void insertComponentAt(int index, Object con, Component cmp) {
        super.insertComponentAt(index, con, cmp);
        updateUIIDs();
    }

    /// {@inheritDoc}
    @Override
    public void refreshTheme(boolean merge) {
        super.refreshTheme(merge);
        if (isGroupingActive()) {
            updateUIIDs();
        } else {
            restoreAllUIIDs();
        }
    }

    @Override
    void removeComponentImpl(Component cmp) {
        super.removeComponentImpl(cmp);

        // restore original UIID
        Object o = cmp.getClientProperty("$origUIID");
        if (o != null) {
            cmp.setUIID((String) o);
        }
        updateUIIDs();
    }

    private String elementPrefix(Component c) {
        if (c.getClass() == Button.class) {
            return buttonUIID;
        } else {
            return elementUIID;
        }
    }

    private void updateUIIDs() {
        if (!isGroupingActive()) {
            return;
        }
        int count = getComponentCount();
        if (count > 0) {
            uiidsDirty = true;
            if (count == 1) {
                Component c = getComponentAt(0);
                updateUIID(elementPrefix(c) + "Only", c);
            } else {
                Component c = getComponentAt(0);
                updateUIID(elementPrefix(c) + "First", c);
                c = getComponentAt(count - 1);
                updateUIID(elementPrefix(c) + "Last", c);
                for (int iter = 1; iter < count - 1; iter++) {
                    c = getComponentAt(iter);
                    updateUIID(elementPrefix(c), c);
                }
            }
        }
    }

    private void updateUIID(String newUIID, Component c) {
        Object o = c.getClientProperty("$origUIID");
        if (o == null) {
            c.putClientProperty("$origUIID", c.getUIID());
        }
        c.setUIID(newUIID);
        reverseRadio(c);
    }

    /// Whether this group is currently renaming its members' UIIDs. updateUIIDs()
    /// does nothing unless the theme constant is on or grouping is forced, and
    /// Button needs the same answer to know whether a member's live UIID belongs
    /// to the group or to itself. $origUIID cannot answer it: restoreUIID leaves
    /// that set, so it says "was renamed once", not "is renamed now".
    ///
    /// #### Returns
    ///
    /// true when this group assigns its members' UIIDs
    boolean isGroupingActive() {
        return getUIManager().isThemeConstant(groupFlag, false) || forceGroup || isSegmented();
    }

    /// True when this group is a segmented control: horizontal, and still using the
    /// ToggleButton UIIDs that setHorizontal installs. Such a group renames its members
    /// whether or not the theme sets the group flag.
    ///
    /// The flag exists so a theme that doesn't want the grouped-row look isn't forced
    /// into it, and it still governs the vertical GroupElement path. It cannot govern
    /// this one: neither modern native theme sets the flag, both style
    /// ToggleButton/First/Last/Only in full, and a segmented control is the only use of
    /// ComponentGroup those themes have any styling for -- so honouring the flag here
    /// means setHorizontal(true) silently does nothing on the themes every new app uses.
    ///
    /// Renaming to ToggleButton without asking the theme first is what Button.setToggle
    /// already does, ungated, so an app that uses toggle buttons at all already depends
    /// on that UIID resolving.
    ///
    /// #### Returns
    ///
    /// true when this group is a horizontal ToggleButton segmented control
    private boolean isSegmented() {
        return isHorizontal() && "ToggleButton".equals(elementUIID);
    }

    /// Applies or undoes this group's renaming to match its current state. Every mutator
    /// that can change the answer to isGroupingActive has to go through here: updateUIIDs
    /// only renames and returns early when the group is inactive, so a setter that
    /// deactivates a group and then calls it leaves the members wearing names from the
    /// state the group just left.
    private void applyOrRestore() {
        if (isGroupingActive()) {
            updateUIIDs();
        } else {
            restoreAllUIIDs();
        }
    }

    /// Puts every member back to the UIID it had before this group renamed it, if this
    /// group ever did. updateUIIDs only renames, so every path that can deactivate a
    /// group needs this as its other half.
    private void restoreAllUIIDs() {
        if (!uiidsDirty) {
            return;
        }
        uiidsDirty = false;
        int count = getComponentCount();
        for (int iter = 0; iter < count; iter++) {
            restoreUIID(getComponentAt(iter));
        }
    }

    private void restoreUIID(Component c) {
        String o = (String) c.getClientProperty("$origUIID");
        if (o != null) {
            c.setUIID(o);
        }
        reverseRadio(c);
    }

    /// Indicates that the component group should be horizontal by using the BoxLayout Y
    ///
    /// #### Returns
    ///
    /// the horizontal
    public boolean isHorizontal() {
        Layout l = getLayout();
        if (l instanceof BoxLayout) {
            return ((BoxLayout) l).getAxis() == BoxLayout.X_AXIS;
        }
        return false;
    }

    /// Indicates that the component group should be horizontal by using the BoxLayout Y
    ///
    /// #### Parameters
    ///
    /// - `horizontal`: the horizontal to set
    public void setHorizontal(boolean horizontal) {
        if (horizontal != isHorizontal()) {
            if (horizontal) {
                setLayout(new BoxLayout(BoxLayout.X_AXIS));
                if ("GroupElement".equals(elementUIID)) {
                    elementUIID = "ToggleButton";
                    buttonUIID = "ToggleButton";
                    updateUIIDs();
                }
            } else {
                setLayout(new BoxLayout(BoxLayout.Y_AXIS));
                if ("ToggleButton".equals(elementUIID)) {
                    elementUIID = "GroupElement";
                    buttonUIID = "ButtonGroup";
                    // Leaving the segmented state can deactivate the group, and updateUIIDs
                    // only ever renames -- it has no path back. Restore here, or the members
                    // keep the ToggleButton names a vertical group no longer justifies.
                    applyOrRestore();
                }
            }
        }
    }

    /// The UIID to apply to the elements within this container
    ///
    /// #### Returns
    ///
    /// the elementUIID
    public String getElementUIID() {
        return elementUIID;
    }

    /// The UIID to apply to the elements within this container
    ///
    /// #### Parameters
    ///
    /// - `elementUIID`: the elementUIID to set
    public void setElementUIID(String elementUIID) {
        this.elementUIID = elementUIID;
        buttonUIID = elementUIID;
        applyOrRestore();
    }

    /// {@inheritDoc}
    @Override
    public String[] getPropertyNames() {
        return new String[]{"elementUIID", "displayName", "horizontal", "groupFlag", "forceGroup"};
    }

    /// {@inheritDoc}
    @Override
    public Class[] getPropertyTypes() {
        return new Class[]{String.class, String.class, Boolean.class, String.class, Boolean.class};
    }

    /// {@inheritDoc}
    @Override
    public Object getPropertyValue(String name) {
        if ("elementUIID".equals(name)) {
            return getElementUIID();
        }
        if ("horizontal".equals(name)) {
            if (isHorizontal()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if ("groupFlag".equals(name)) {
            return groupFlag;
        }
        if ("forceGroup".equals(name)) {
            return forceGroup ? Boolean.TRUE : Boolean.FALSE;
        }
        return null;
    }

    /// {@inheritDoc}
    @Override
    public String setPropertyValue(String name, Object value) {
        if ("elementUIID".equals(name)) {
            setElementUIID((String) value);
            return null;
        }
        if ("horizontal".equals(name)) {
            setHorizontal(((Boolean) value).booleanValue());
            return null;
        }
        if ("groupFlag".equals(name)) {
            // Passed the field rather than the value, so setting this property
            // assigned the flag to itself and the designer's edit was discarded.
            setGroupFlag((String) value);
            return null;
        }
        if ("forceGroup".equals(name)) {
            // Assigning the field skips the apply/restore the setter does.
            setForceGroup(((Boolean) value).booleanValue());
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    /// The group flag allows changing the flag that activates this group, from ComponentGroupBool to any
    /// arbitrary flag. This allows a developer/designer to enable grouping for a specific type of components
    /// (e.g. for horizontal Toggle Buttons) yet disable it for vertical lists of components.
    ///
    /// #### Returns
    ///
    /// the groupFlag
    public String getGroupFlag() {
        return groupFlag;
    }

    /// The group flag allows changing the flag that activates this group, from ComponentGroupBool to any
    /// arbitrary flag. This allows a developer/designer to enable grouping for a specific type of components
    /// (e.g. for horizontal Toggle Buttons) yet disable it for vertical lists of components.
    ///
    /// #### Parameters
    ///
    /// - `groupFlag`: the groupFlag to set
    public void setGroupFlag(String groupFlag) {
        if (this.groupFlag == null ? groupFlag == null : this.groupFlag.equals(groupFlag)) {
            return;
        }
        this.groupFlag = groupFlag;
        // Naming a different constant can activate or deactivate the group, and
        // assigning the field was the whole method.
        applyOrRestore();
    }

    /// Component grouping can be an element from the theme but can be forced manually
    /// for a specific group
    ///
    /// #### Returns
    ///
    /// the forceGroup
    public boolean isForceGroup() {
        return forceGroup;
    }

    /// Component grouping can be an element from the theme but can be forced manually
    /// for a specific group
    ///
    /// #### Parameters
    ///
    /// - `forceGroup`: the forceGroup to set
    public void setForceGroup(boolean forceGroup) {
        if (this.forceGroup == forceGroup) {
            return;
        }
        this.forceGroup = forceGroup;
        // Assigning the field was the whole method, so the setter only ever took effect
        // if something else happened to call updateUIIDs afterwards -- adding a component,
        // or a theme refresh. Turning it off never restored anything at all.
        applyOrRestore();
    }
}
