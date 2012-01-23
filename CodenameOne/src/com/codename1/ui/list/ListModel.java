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

/**
 * Represents the data structure of the list, thus allowing a list to
 * represent any potential data source by referencing different implementations of this
 * interface. E.g. a list model can be implemented in such a way that it retrieves data
 * directly from storage (although caching would be recommended).
 * <p>It is the responsibility of the list to notify observers (specifically the view 
 * {@link com.codename1.ui.List} of any changes to its state (items removed/added/changed etc.)
 * thus the data would get updated on the view.
 * 
 * @author Chen Fishbein
 */
public interface ListModel {
    
    /**
     * Returns the item at the given offset
     * @param index an index into this list
     * @return the item at the specified index
     */
    public Object getItemAt(int index);
    
    /**
     * Returns the number of items in the list
     * @return the number of items in the list
     */
    public int getSize();
    
    /**
     * Returns the selected list offset
     * 
     * @return the selected list index
     */
    public int getSelectedIndex();

    /**
     * Sets the selected list offset can be set to -1 to clear selection
     * @param index an index into this list
     */
    public void setSelectedIndex(int index);
    
    /**
     * Invoked to indicate interest in future change events
     * @param l a data changed listener
     */
    public void addDataChangedListener(DataChangedListener l);
    
    /**
     * Invoked to indicate no further interest in future change events
     * @param l a data changed listener 
     */
    public void removeDataChangedListener(DataChangedListener l);
    
    /**
     * Invoked to indicate interest in future selection events
     * @param l a selection listener
     */
    public void addSelectionListener(SelectionListener l);
    
    /**
     * Invoked to indicate no further interest in future selection events
     * @param l a selection listener
     */
    public void removeSelectionListener(SelectionListener l);
    
    /**
     * Adds the specified item to the end of this list.
     * An optional operation for mutable lists, it can throw an unsupported operation
     * exception if a list model is not mutable.
     * @param item the item to be added
     */
    public void addItem(Object item);
    
    /**
     * Removes the item at the specified position in this list.
     * @param index the index of the item to removed
     */
    public void removeItem(int index);

}
