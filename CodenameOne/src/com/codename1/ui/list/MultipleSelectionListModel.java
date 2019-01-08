/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui.list;

/**
 * Events {@link ListModel} to support multiple selection.
 * @author Steve Hannah
 * @since 6.0
 */
public interface MultipleSelectionListModel<T> extends ListModel<T> {
    /**
     * Adds indices to set of selected indices.
     * @param indices Indices to add to selected indices.
     */
    public void addSelectedIndices(int... indices);
    
    /**
     * Removes indices from the set of selected indices.
     * @param indices Indices to remove from selected indices.
     */
    public void removeSelectedIndices(int... indices);
    
    /**
     * Sets the selected indices in this model.
     * @param indices 
     */
    public void setSelectedIndices(int... indices);
    
    /**
     * Gets the selected indices in this model.  Indices should be returned
     * in increasing order with no duplicates.
     * @return Selected indices in increasing order with no duplicates.  If there
     * are no selected indices, then this will return a zero-length array.
     */
    public int[] getSelectedIndices();
    
}
