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

/**
 * <p>A component group is a container that applies the given UIID to a set of components within it
 * and gives the same UIID with "First"/"Last" prepended to the first and last components. E.g.
 * by default the  GroupElement UIID is applied so the first and last elements would have the
 * GroupElementFirst/GroupElementLast UIID's applied to them. If a group has only one element
 * the word "Only" is appended to the element UIID as in GroupElementOnly.
 * <p><b>Important!!!</b> A component group does nothing by default unless explicitly activated by
 * the theme by enabling the ComponentGroupBool constant (by default, this can be customized via the groupFlag property).
 * This allows logical grouping without changing the UI for themes that don't need grouping.
 * <p>This container uses box X/Y layout (defaults to Y), other layout managers shouldn't be used
 * since this container relies on the specific behavior of the box layout.
 *
 * @author Shai Almog
 */
public class ComponentGroup extends Container {
    private String elementUIID = "GroupElement";
    private String buttonUIID = "ButtonGroup";
    private String groupFlag = "ComponentGroupBool";
    private boolean uiidsDirty;
    private boolean forceGroup;
 
    /**
     * Default constructor
     */
    public ComponentGroup() {
        super(new BoxLayout(BoxLayout.Y_AXIS));
        setUIID("ComponentGroup");
    }

    private void reverseRadio(Component cmp) {
        if(cmp instanceof ComboBox) {
            ((ComboBox)cmp).setActAsSpinnerDialog(uiidsDirty);
        }
    }

    void insertComponentAt(int index, Object con, Component cmp) {
        super.insertComponentAt(index, con, cmp);
        updateUIIDs();
    }

    /**
     * {@inheritDoc}
     */
    public void refreshTheme(boolean merge) {
        super.refreshTheme(merge);
        if(!getUIManager().isThemeConstant(groupFlag, false) && !forceGroup) {
            if(uiidsDirty) {
                uiidsDirty = false;
                int count = getComponentCount();
                for(int iter = 0 ; iter < count ; iter++) {
                    restoreUIID(getComponentAt(iter));
                }
            }
        } else {
            updateUIIDs();
        }
    }

    void removeComponentImpl(Component cmp) {
        super.removeComponentImpl(cmp);
        
        // restore original UIID
        Object o = cmp.getClientProperty("$origUIID");
        if(o != null) {
            cmp.setUIID((String)o);
        }
        updateUIIDs();
    }
    
    private String elementPrefix(Component c) {
        if(c.getClass() == Button.class) {
            return buttonUIID;
        } else {
            return elementUIID;
        }
    }

    private void updateUIIDs() {
        if(!getUIManager().isThemeConstant(groupFlag, false) && !forceGroup) {
            return;
        }
        int count = getComponentCount();
        if(count > 0) {
            uiidsDirty = true;
            if(count == 1) {
                Component c = getComponentAt(0);
                updateUIID(elementPrefix(c) + "Only", c);
            } else {
                Component c = getComponentAt(0);
                updateUIID(elementPrefix(c) + "First", c);
                if(count > 1) {
                    c = getComponentAt(count - 1);
                    updateUIID(elementPrefix(c) + "Last", c);
                    for(int iter = 1 ; iter < count - 1 ; iter++) {
                        c = getComponentAt(iter);
                        updateUIID(elementPrefix(c), c);
                    }
                }
            }
        }
    }

    private void updateUIID(String newUIID, Component c) {
        Object o = c.getClientProperty("$origUIID");
        if(o == null) {
            c.putClientProperty("$origUIID", c.getUIID());
        }
        c.setUIID(newUIID);
        reverseRadio(c);
    }

    private void restoreUIID(Component c) {
        String o = (String)c.getClientProperty("$origUIID");
        if(o != null) {
            c.setUIID(o);
        }
        reverseRadio(c);
    }

    /**
     * Indicates that the component group should be horizontal by using the BoxLayout Y
     * @return the horizontal
     */
    public boolean isHorizontal() {
        return getLayout() instanceof BoxLayout && ((BoxLayout)getLayout()).getAxis() == BoxLayout.X_AXIS;
    }

    /**
     * Indicates that the component group should be horizontal by using the BoxLayout Y
     * @param horizontal the horizontal to set
     */
    public void setHorizontal(boolean horizontal) {
        if(horizontal != isHorizontal()) {
            if(horizontal) {
                setLayout(new BoxLayout(BoxLayout.X_AXIS));
                if("GroupElement".equals(elementUIID)) {
                    elementUIID = "ToggleButton";
                    buttonUIID = "ToggleButton";
                    updateUIIDs();
                }
            } else {
                setLayout(new BoxLayout(BoxLayout.Y_AXIS));
                if("ToggleButton".equals(elementUIID)) {
                    elementUIID = "GroupElement";
                    buttonUIID = "ButtonGroup";
                    updateUIIDs();
                }
            }
        }
    }

    /**
     * The UIID to apply to the elements within this container
     *
     * @return the elementUIID
     */
    public String getElementUIID() {
        return elementUIID;
    }

    /**
     * The UIID to apply to the elements within this container
     * @param elementUIID the elementUIID to set
     */
    public void setElementUIID(String elementUIID) {
        this.elementUIID = elementUIID;
        buttonUIID = elementUIID;
        updateUIIDs();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPropertyNames() {
        return new String[] {"elementUIID", "displayName", "horizontal", "groupFlag", "forceGroup"};
    }

    /**
     * {@inheritDoc}
     */
    public Class[] getPropertyTypes() {
       return new Class[] {String.class, String.class, Boolean.class, String.class, Boolean.class};
    }

    /**
     * {@inheritDoc}
     */
    public Object getPropertyValue(String name) {
        if(name.equals("elementUIID")) {
            return getElementUIID();
        }
        if(name.equals("horizontal")) {
            if(isHorizontal()) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        if(name.equals("groupFlag")) {
            return groupFlag;
        }
        if(name.equals("forceGroup")) {
            if(forceGroup) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("elementUIID")) {
            setElementUIID((String)value);
            return null;
        }
        if(name.equals("horizontal")) {
            setHorizontal(((Boolean)value).booleanValue());
            return null;
        }
        if(name.equals("groupFlag")) {
            setGroupFlag(groupFlag);
            return null;
        }
        if(name.equals("forceGroup")) {
            forceGroup = ((Boolean)value).booleanValue();
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    /**
     * The group flag allows changing the flag that activates this group, from ComponentGroupBool to any
     * arbitrary flag. This allows a developer/designer to enable grouping for a specific type of components
     * (e.g. for horizontal Toggle Buttons) yet disable it for vertical lists of components.
     *
     * @return the groupFlag
     */
    public String getGroupFlag() {
        return groupFlag;
    }

    /**
     * The group flag allows changing the flag that activates this group, from ComponentGroupBool to any
     * arbitrary flag. This allows a developer/designer to enable grouping for a specific type of components
     * (e.g. for horizontal Toggle Buttons) yet disable it for vertical lists of components.
     * 
     * @param groupFlag the groupFlag to set
     */
    public void setGroupFlag(String groupFlag) {
        this.groupFlag = groupFlag;
    }

    /**
     * Component grouping can be an element from the theme but can be forced manually
     * for a specific group
     * 
     * @return the forceGroup
     */
    public boolean isForceGroup() {
        return forceGroup;
    }

    /**
     * Component grouping can be an element from the theme but can be forced manually
     * for a specific group
     * 
     * @param forceGroup the forceGroup to set
     */
    public void setForceGroup(boolean forceGroup) {
        this.forceGroup = forceGroup;
    }
    
    /**
     * Shorthand method for wrapping the given components in a vertical component group
     * @param cmp the components to add into a newly created group
     * @return the newly created group
     */
    public static ComponentGroup enclose(Component... cmp) {
        ComponentGroup c = new ComponentGroup();
        for(Component cc : cmp) {
            c.add(cc);
        }
        return c;
    }

    
    /**
     * Shorthand method for wrapping the given components in a horizontal component group
     * @param cmp the components to add into a newly created group
     * @return the newly created group
     */
    public static ComponentGroup encloseHorizontal(Component... cmp) {
        ComponentGroup c = new ComponentGroup();
        c.setHorizontal(true);
        for(Component cc : cmp) {
            c.add(cc);
        }
        return c;
    }
}
