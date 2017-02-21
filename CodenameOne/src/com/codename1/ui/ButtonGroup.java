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

/**
 * <p>This class is used to create a multiple-exclusion scope for 
 * {@link com.codename1.ui.RadioButton}.
 * Creating a set of {@link com.codename1.ui.RadioButton} components with the same {@code ButtonGroup} object
 * means that only one {@link com.codename1.ui.RadioButton} can be selected among those within 
 * the specific {@code ButtonGroup}.</p>
 * 
 * <script src="https://gist.github.com/codenameone/dc7fccf13dc102bc5ea0.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-radiobutton-checkbox.png" alt="Sample usage of CheckBox/RadioButton/ButtonGroup" />
 * 
 * 
 * @author Nir Shabi
 */
public class ButtonGroup {
    
    
    private java.util.Vector buttons = new java.util.Vector();
    private int selectedIndex=-1;
    
    /** 
     * Creates a new instance of ButtonsGroup 
     */
    public ButtonGroup() {
    }

    /**
     * Adds all the radio buttons to the group
     * @param rb 
     */
    public ButtonGroup(RadioButton... rb) {
        addAll(rb);
    }
    
    /**
     * Adds the RadioButtons to the group
     * 
     * @param rb a RadioButtons to add
     */
    public void addAll(RadioButton... rb){
        for(RadioButton r : rb) {
            add(r);
        }
    }
    
    /**
     * Adds a RadioButton to the group
     * 
     * @param rb a RadioButton to add
     */
    public void add(RadioButton rb){
        if(rb==null)
            return;
        if(!buttons.contains(rb)) {
            buttons.addElement(rb);
            if(rb.isSelected()) {
                setSelected(buttons.indexOf(rb));
            }
            rb.setButtonGroup(this);
        }
    }

    /**
     * removes a RadioButton from the group
     * 
     * @param rb a RadioButton to remove
     */
    public void remove(RadioButton rb){
        if(rb==null)
            return;
        buttons.removeElement(rb);
        if(rb.isSelected())
            clearSelection();
        rb.setButtonGroup(null);
    }
    
    /**
     * Clears the selection such that none of the buttons in the ButtonGroup are selected.
     */
    public void clearSelection() {
        if(selectedIndex!=-1) {
            if(selectedIndex < buttons.size()) {
                ((RadioButton)buttons.elementAt(selectedIndex)).setSelected(false);
            }
            selectedIndex=-1;
        }
        
    }
    
    /**
     * Returns the number of buttons in the group.
     * 
     * @return number of radio buttons in the group
     */
    public int getButtonCount() {
        return buttons.size();
    }
    
    /**
     * Returns whether a radio button in the group is selected.
     * 
     * @return true if a selection was made in the radio button group
     */
    public boolean isSelected() {
        if(selectedIndex!= -1)
            return true;
        return false;
    }
    
    /**
     * Return the index of the selected button within the group
     * 
     * @return the index of the selected button within the group
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }
    
    /**
     * Returns the radio button at the given group index
     * 
     * @param index offset within the group starting with 0 and no larger than getButtonCount()
     * @return the radio button instance
     */
    public RadioButton getRadioButton(int index) {
        if(index >=0 && index < getButtonCount())
            return ((RadioButton)buttons.elementAt(index));
        return null;
    }

    /**
     * Selects the given radio button
     * 
     * @param rb the radio button to set as selected
     */
    public void setSelected(RadioButton rb) {
        if (rb != null) {
            int index = buttons.indexOf(rb);
            if(index < 0) {
                add(rb);
                index = buttons.indexOf(rb);
            }
            setSelected(index);
        } else {
            clearSelection();
        }
    }
    
    /**
     * Sets the selected Radio button by index
     * 
     * @param index the index of the radio button to mark as selected
     */
    public void setSelected(int index) {
        if(index < 0  ||  index >= getButtonCount() )
            throw new IllegalArgumentException("Index out of bounds");

        if(selectedIndex == index) {
            return;
        }

        if(selectedIndex!=-1) {
            //unselect last selected Radio button
            ((RadioButton)buttons.elementAt(selectedIndex)).setSelectedImpl(false);
        }
        ((RadioButton)buttons.elementAt(index)).setSelectedImpl(true);
        selectedIndex=index;
    }
}
