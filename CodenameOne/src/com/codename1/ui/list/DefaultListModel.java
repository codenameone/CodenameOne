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
package com.codename1.ui.list;

import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.util.EventDispatcher;
import java.util.Vector;

/**
 * Default implementation of the list model based on a vector of elements
 *
 * @author Chen Fishbein
 */
public class DefaultListModel implements ListModel{
    
    private Vector items;

    private EventDispatcher dataListener = new EventDispatcher();
    private EventDispatcher selectionListener = new EventDispatcher();
        
    private int selectedIndex = 0;
    
    /** 
     * Creates a new instance of DefaultListModel 
     */
    public DefaultListModel() {
        this.items = new Vector();
    }

    /** 
     * Creates a new instance of DefaultListModel 
     * 
     * @param items the items in the model
     */
    public DefaultListModel(Vector items) {
        this.items = items;
    }

    /** 
     * Creates a new instance of DefaultListModel 
     * 
     * @param items the items in the model
     */
    public DefaultListModel(Object[] items) {
        this.items = createVector(items);
    }

    private static Vector createVector(Object[] items) {
        if (items == null) {
            items = new Object[] {};
        }
        Vector vec = new Vector(items.length);
        for(int iter = 0 ; iter < items.length ; iter++) {
            vec.addElement(items[iter]);
        }
        return vec;
    }

    /**
     * @inheritDoc
     */
    public Object getItemAt(int index) {
        if(index < getSize() && index >= 0){
            return items.elementAt(index);
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public int getSize() {
        return items.size();
    }

    /**
     * @inheritDoc
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }
    
    /**
     * @inheritDoc
     */
    public void addItem(Object item){
        items.addElement(item);
        fireDataChangedEvent(DataChangedListener.ADDED, items.size());
    }
    
    /**
     * Change the item at the given index
     * 
     * @param index the offset for the item
     * @param item the value to set
     */
    public void setItem(int index, Object item){
        items.setElementAt(item, index);
        fireDataChangedEvent(DataChangedListener.CHANGED, index);
    }

    /**
     * Adding an item to list at given index
     * @param item - the item to add
     * @param index - the index position in the list
     */
    public void addItemAtIndex(Object item, int index){
        if (index <= items.size()) {
            items.insertElementAt(item, index);
            fireDataChangedEvent(DataChangedListener.ADDED, index);
        }
    }
    
    /**
     * @inheritDoc
     */
    public void removeItem(int index){
        if(index < getSize() && index >= 0){
            items.removeElementAt(index);
            if(index != 0){
                setSelectedIndex(index - 1);
            }
            fireDataChangedEvent(DataChangedListener.REMOVED, index);
        }
    }
    
    /**
     * Removes all elements from the model
     */
    public void removeAll(){
        while(getSize() > 0) {
            removeItem(0);
        }
    }
    
    /**
     * @inheritDoc
     */
    public void setSelectedIndex(int index) {
        int oldIndex = selectedIndex;
        this.selectedIndex = index;
        selectionListener.fireSelectionEvent(oldIndex, selectedIndex);
    }

    /**
     * @inheritDoc
     */
    public void addDataChangedListener(DataChangedListener l) {
        dataListener.addListener(l);
    }

    /**
     * @inheritDoc
     */
    public void removeDataChangedListener(DataChangedListener l) {
        dataListener.removeListener(l);
    }
    
    private void fireDataChangedEvent(final int status, final int index){
        dataListener.fireDataChangeEvent(index, status);
    }

    /**
     * @inheritDoc
     */
    public void addSelectionListener(SelectionListener l) {
        selectionListener.addListener(l);
    }

    /**
     * @inheritDoc
     */
    public void removeSelectionListener(SelectionListener l) {
        selectionListener.removeListener(l);
    }
}
