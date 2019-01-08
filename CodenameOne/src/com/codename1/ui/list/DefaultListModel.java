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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * <p>Default implementation of the list model based on a {@code List} of elements.
 * The list model is an observable set of objects that {@link com.codename1.ui.List} uses to pull
 * the data to display.</p>
 *
 * <script src="https://gist.github.com/codenameone/b2ab6645db842d7b2750.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/graphics-urlimage-multilist.png" alt="MultiList and model in action" />
 * 
 * @author Chen Fishbein
 */
public class DefaultListModel<T> implements MultipleSelectionListModel<T> {

    
    private boolean multiSelectionMode;
    private java.util.List items;

    private EventDispatcher dataListener = new EventDispatcher();
    private EventDispatcher selectionListener = new EventDispatcher();
        
    private int selectedIndex = 0;
    private Set<Integer> selectedIndices;
    
    /** 
     * Creates a new instance of DefaultListModel 
     */
    public DefaultListModel() {
        this.items = new ArrayList();
    }

    /** 
     * Creates a new instance of DefaultListModel 
     * 
     * @param items the items in the model
     */
    public DefaultListModel(Vector<T> items) {
        this.items = new ArrayList(items);
    }

    /** 
     * Creates a new instance of DefaultListModel 
     * 
     * @param items the items in the model
     */
    public DefaultListModel(Collection<T> items) {
        this.items = new ArrayList(items);
    }

    /** 
     * Creates a new instance of DefaultListModel 
     * 
     * @param items the items in the model
     */
    public DefaultListModel(T... items) {
        this.items = createList(items);
    }

    private static java.util.List createList(Object[] items) {
        if (items == null) {
            items = new Object[] {};
        }
        java.util.List vec = new ArrayList(items.length);
        int ilen = items.length;
        for(int iter = 0 ; iter < ilen ; iter++) {
            vec.add(items[iter]);
        }
        return vec;
    }

    /**
     * {@inheritDoc}
     */
    public T getItemAt(int index) {
        if(index < getSize() && index >= 0){
            return (T)items.get(index);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        return items.size();
    }

    /**
     * {@inheritDoc}
     */
    public int getSelectedIndex() {
        if (isMultiSelectionMode()) {
            int[] selected = getSelectedIndices();
            if (selected.length == 0) {
                return -1;
            } else {
                return selected[0];
            }
        }
        return selectedIndex;
    }
    
    /**
     * {@inheritDoc}
     */
    public void addItem(T item){
        items.add(item);
        fireDataChangedEvent(DataChangedListener.ADDED, items.size()-1);
    }
    
    /**
     * Change the item at the given index
     * 
     * @param index the offset for the item
     * @param item the value to set
     */
    public void setItem(int index, T item){
        items.set(index, item);
        fireDataChangedEvent(DataChangedListener.CHANGED, index);
    }

    /**
     * Adding an item to list at given index
     * @param item - the item to add
     * @param index - the index position in the list
     */
    public void addItemAtIndex(T item, int index){
        if (index <= items.size()) {
            items.add(index, item);
            fireDataChangedEvent(DataChangedListener.ADDED, index);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void removeItem(int index){
        if(index < getSize() && index >= 0){
            items.remove(index);
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
     * {@inheritDoc}
     */
    public void setSelectedIndex(int index) {
        if (isMultiSelectionMode()) {
            setSelectedIndices(index);
        } else {
            int oldIndex = selectedIndex;
            this.selectedIndex = index;
            selectionListener.fireSelectionEvent(oldIndex, selectedIndex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addDataChangedListener(DataChangedListener l) {
        dataListener.addListener(l);
    }

    /**
     * {@inheritDoc}
     */
    public void removeDataChangedListener(DataChangedListener l) {
        dataListener.removeListener(l);
    }
    
    /**
     * Broadcast a change event to all listeners
     * @param status the status of the event
     * @param index the index changed
     */
    protected void fireDataChangedEvent(final int status, final int index){
        dataListener.fireDataChangeEvent(index, status);
    }

    /**
     * {@inheritDoc}
     */
    public void addSelectionListener(SelectionListener l) {
        selectionListener.addListener(l);
    }

    /**
     * {@inheritDoc}
     */
    public void removeSelectionListener(SelectionListener l) {
        selectionListener.removeListener(l);
    }
    
    /**
     * Returns the internal list of items which makes traversal using iterators easier.
     * 
     * @return the list, notice that you shouldn't modify it
     */
    public java.util.List<T> getList() {
        return items;
    }
    
    // Multi-selection Mode methods.
    
    private java.util.List<Integer> toList(int[] ints) {
        int len = ints.length;
        ArrayList<Integer> out = new ArrayList<Integer>(len);
        
        for (int i=0; i<len; i++) {
            out.add(ints[i]);
        }
        return out;
    }
  

    /**
     * For use with multi-selection mode.  Sets the selected indices in this model.
     * 
     * <p>Note:  This may fire multiple selectionChange events.  For each "deselected" index,
     * it will fire an event with the (oldIndex, newIndex) being (index, -1) (i.e. selected index
     * changes from the index to -1.  And for each newly selected index, it will fire 
     * the event with (oldIndex, newIndex) being (-1, index).</p>
     * 
     * 
     * 
     * @param indices The indices to select.
     * @throws IllegalArgumentException If {@link #isMultiSelectionMode() } is false, and {@literal indices} length is greater than 1.
     * 
     * @see #setMultiSelectionMode(boolean) 
     * @see #isMultiSelectionMode() 
     * @since 6.0
     */
    @Override
    public void setSelectedIndices(int... indices) {
        if (isMultiSelectionMode()) {
            if (selectedIndices == null) {
                selectedIndices = new HashSet<Integer>();
            }
            java.util.Set newSelections = new HashSet(toList(indices));
            if (selectedIndices.size() != indices.length || !selectedIndices.containsAll(newSelections)) {
                HashSet toRemove = new HashSet(selectedIndices);
                toRemove.removeAll(newSelections);
                HashSet toAdd = new HashSet(newSelections);
                toAdd.removeAll(selectedIndices);
                selectedIndices.clear();
                selectedIndices.addAll(newSelections);
                
                for (Integer i : (Set<Integer>)toRemove) {
                    selectionListener.fireSelectionEvent(i, -1);
                }
                for (Integer i : (Set<Integer>) toAdd) {
                    selectionListener.fireSelectionEvent(-1, i);
                }
            }
        } else {
            if (indices.length == 1) {
                setSelectedIndex(indices[0]);
            } else if (indices.length == 0) {
                setSelectedIndex(-1);
            } else {
                throw new IllegalArgumentException("setSelectedIndices can only include 0 or 1 index in multiselection mode, but received "+indices.length);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addSelectedIndices(int... indices) {
        if (isMultiSelectionMode()) {
            if (selectedIndices == null) {
                selectedIndices = new HashSet<Integer>();
            }
            for (int index : indices) {
                if (!selectedIndices.contains((Integer)index)) {
                    selectedIndices.add(index);
                    selectionListener.fireSelectionEvent(-1, index);
                }
            }
        } else {
            throw new IllegalArgumentException("addSelectedIndices only supported if isMultiSelectionMode() is on");
        }
    }

    /**
     * {@inheritDoc}
     * @since 6.0
     */
    @Override
    public void removeSelectedIndices(int... indices) {
        if (isMultiSelectionMode()) {
            if (selectedIndices == null) {
                return;
            }
            for (int index : indices) {
                if (selectedIndices.contains((Integer)index)) {
                    selectedIndices.remove((Integer)index);
                    selectionListener.fireSelectionEvent(index, -1);
                }
            }
        } else {
            throw new IllegalArgumentException("removeSelectedIndices only supported if isMultiSelectionMode() is on");
        }
    }
    

    

    /**
     * {@inheritDoc}
     * @since 6.0
     */
    @Override
    public int[] getSelectedIndices() {
        
        if (isMultiSelectionMode()) {
            if (selectedIndices == null) {
                return new int[0];
            }
            int[] out = new int[selectedIndices.size()];
            int index=0;
            for (Integer i : selectedIndices) {
                out[index++] = i;
            }
            Arrays.sort(out);
            return out;
        } else {
            int selectedIndex = getSelectedIndex();
            if (selectedIndex >= 0) {
                return new int[]{selectedIndex};
            } else {
                return new int[0];
            }
        }
    }
    
    /**
     * Checks to see if this list model is in multi-selection mode.
     * @return the multiSelectionMode
     * @since 6.0
     */
    public boolean isMultiSelectionMode() {
        return multiSelectionMode;
    }

    /**
     * Enables or disables multi-selection mode.
     * @param multiSelectionMode the multiSelectionMode to set
     * @since 6.0
     */
    public void setMultiSelectionMode(boolean multiSelectionMode) {
        this.multiSelectionMode = multiSelectionMode;
    }
}
