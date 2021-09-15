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

package com.codename1.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Base class for a property as a list which can contain multiple elements within
 *
 * @author Shai Almog
 */
public class ListProperty<T, K> extends CollectionProperty<T, K> {
    private ArrayList<T> value = new ArrayList<T>();
    
    /**
     * Constructs a property with the given name and value
     * @param name the name of the property
     * @param values default values for the property
     */
    public ListProperty(String name, T... values) {
        this(name, null, values);
    }
    
    /**
     * Constructs a property with the given name and values by specifying the
     * type of the elements explicitly. The element type needs to be specified
     * if the list should contain {@link PropertyBusinessObject}s and needs
     * to get deserialized properly!
     * @param name the name of the property
     * @param elementType subclass of {@link PropertyBusinessObject}
     * @param values default values for the property
     */
    public ListProperty(String name, Class<T> elementType, T... values) {
        super(name, elementType);
        for(T t : values) {
            value.add(t);
        }
    }
    
    /**
     * Constructs a property with null value
     * @param name the name of the property
     */
    public ListProperty(String name) {
        super(name);
    }
    
    /**
     * Gets the property value
     * @param offset the offset within the list
     * @return the property value
     */
    public T get(int offset) {
        internalGet();
        return value.get(offset);
    }
    
    /**
     * The size of the property list
     * @return the number of elements
     */
    public int size() {
        internalGet();
        return value.size();
    }
    
    /**
     * Sets the property value and potentially fires a change event
     * @param offset the position for the new value
     * @param v the new value
     * @return the parent object for chaining
     */
    public K set(int offset, T v) {
        value.set(offset, v);
        firePropertyChanged();
        internalSet();
        return (K)parent.parent;
    }

    /**
     * Sets the entire content of the property
     * @param t the collection of elements to set
     * @return the parent object for chaining
     */
    public K set(Collection<T> t) {
        value.clear();
        value.addAll(t);
        firePropertyChanged();
        internalSet();
        return (K)parent.parent;
    }
    
    /**
     * Historical alias of set(Collection<T> t)
     * Sets the entire content of the property
     * @param t the collection of elements to set
     * @return the parent object for chaining
     */
    public K setList(Collection<T> t) {
        return set(t);
    }
        
    /**
     * Adds a property value and fires a change event
     * @param offset the position for the new value
     * @param v the new value
     */
    public K add(int offset, T v) {
        value.add(offset, v);
        firePropertyChanged();
        internalSet();
        return (K)parent.parent;
    }

    /**
     * Adds a property value to the end of the list and fires a change event
     * @param v the new value
     */
    public K add(T v) {
        value.add(v);
        firePropertyChanged();
        internalSet();
        return (K)parent.parent;
    }
    
    /**
     * Adds a all properties value to the list and fires a change event
     * @param v the collection of values to add
     */
    public K addAll(Collection<? extends T> v) {
        if (value.addAll(v)) {
            firePropertyChanged();
            internalSet();
        }
        return (K)parent.parent;
    }
    
    /**
     * Removes the item at the given offset
     * @param offset the offset
     */
    public K remove(int offset) {
        value.remove(offset);
        firePropertyChanged();
        internalSet();
        return (K)parent.parent;
    }
    
    /**
     * Removes the item with this value
     * @param v the value object
     */
    public K remove(T v) {
    	if (value.remove(v)) {
    		firePropertyChanged();
            internalSet();
    	}
        return (K)parent.parent;
    }
    
    /**
     * Removes from the list all values from the given collection and fires a change event if the list has changed
     * @param the item to remove
     */
    public K removeAll(Collection<? extends T>  v) {
        if (value.removeAll(v)) {
        	firePropertyChanged();
            internalSet();
        }
        return (K)parent.parent;
    }
        
    /**
     * Compares this property to another property
     * @param obj the other property
     * @return true if they are equal in name and value
     */
    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) {
            return false;
        }
        ListProperty other = (ListProperty)obj;
        return other.value.equals(value);
    }

    /**
     * Returns the internal hashcode or 0 for null property
     * @return the hashcode value
     */
    @Override
    public int hashCode() {
        return value.hashCode();
    }

    /**
     * Iterate over the elements of the property
     * @return an iterator
     */
    public Iterator<T> iterator() {
        internalGet();
        return value.iterator();
    }
    
    /**
     * Returns a copy of the content as a new list
     * @return a list
     */
    public List<T> asList() {
        internalGet();
        return new ArrayList<T>(value);
    }
    
    /**
     * Returns a copy of the content as a new list but if the value is a PropertyBusinessObject it will 
     * be converted to a Map 
     * @return a list
     */
    public List<Object> asExplodedList() {
        return asExplodedList(value);
    }

    /**
     * Remove all the elements from the list
     */
    public void clear() {
        if(value.size() > 0) {
            value.clear();
            firePropertyChanged();
            internalSet();
        }
    }

    /**
     * Returns true if the given element is contained in the list property  
     * @param element the element
     * @return true if the given element is contained in the list property  
     */
    public boolean contains(T element) {
        return value.contains(element);
    }


    /**
     * Returns the index of the given element in the list property  
     * @param element the element
     * @return the index of the given element in the list property  
     */
    public int indexOf(T element) {
        return value.indexOf(element);
    }
}
