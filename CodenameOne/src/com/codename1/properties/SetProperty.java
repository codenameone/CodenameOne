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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Base class for a property as a set which can contain multiple elements within
 */
public class SetProperty<T, K> extends CollectionProperty<T, K> {

	private Set<T> value = new HashSet<T>();
    
    /**
     * Constructs a property with the given name and value
     * @param name the name of the property
     * @param values default values for the property
     */
    public SetProperty(String name, T... values) {
        this(name, null, values);
    }
    
    /**
     * Constructs a property with the given name and values by specifying the
     * type of the elements explicitly. The element type needs to be specified
     * if the set should contain {@link PropertyBusinessObject}s and needs
     * to get deserialized properly!
     * @param name the name of the property
     * @param elementType subclass of {@link PropertyBusinessObject}
     * @param values default values for the property
     */
    public SetProperty(String name, Class<T> elementType, T... values) {
        super(name, elementType);
        for(T t : values) {
            value.add(t);
        }
        if(elementType == null || !PropertyBusinessObject.class.isAssignableFrom(elementType)) 
            throw new IllegalArgumentException(
                    "the element type class needs to be a subclass of PropertyBusinessObject");
    }
    
    /**
     * Constructs a property with null value
     * @param name the name of the property
     */
    public SetProperty(String name) {
        super(name);
    }
          
    /**
     * The size of the property set
     * @return the number of elements
     */
    public int size() {
        return value.size();
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
        return (K)parent.parent;
    } 
    
    /**
     * Adds a property value to the set and fires a change event if it changed the set
     * @param v the new value
     */
    public K add(T v) {
        if (value.add(v)) {
        	firePropertyChanged();
        }
        return (K)parent.parent;
    }

    /**
     * Adds a collection of values to the set and fires a change event if it changed the set
     * @param v the new value
     */
    public K addAll(Collection<? extends T> v) {
        if (value.addAll(v)) {
        	firePropertyChanged();
        }
        return (K)parent.parent;
    }
        
    /**
     * Removes the given item from the set and fires a change event if this item has been sucessfully removed 
     * @param the item to remove
     */
    public K remove(T v) {
        if (value.remove(v)) {
        	firePropertyChanged();
        }
        return (K)parent.parent;
    }
        
    /**
     * Removes from the set all values from the given collection and fires a change event if the set has changed
     * @param the item to remove
     */
    public K removeAll(Collection<? extends T>  v) {
        if (value.removeAll(v)) {
        	firePropertyChanged();
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
        SetProperty other = (SetProperty) obj;
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
        return value.iterator();
    }
    
    /**
     * Returns a copy of the content as a new list
     * @return a list
     */
    public List<T> asList() {
        return new ArrayList<T>(value);
    }
    
    /**
     * Returns a copy of the content as a new list but if the value is a PropertyBusinessObject it will 
     * be converted to a Map 
     * @return a list
     */
    public List<Object> asExplodedList() {
        ArrayList<Object> aa = new ArrayList<Object>();
        for(T t : value) {
            if(t instanceof PropertyBusinessObject) {
                aa.add(((PropertyBusinessObject)t).getPropertyIndex().toMapRepresentation());
            } else {
                aa.add(t);
            }
        }
        return aa;
    }

    /**
     * Remove all the elements from the set and fires a change event if the set wasn't empty
     */
    public void clear() {
        if(value.size() > 0) {
            value.clear();
            firePropertyChanged();
        }
    }

    /**
     * Returns true if the given element is contained in the set property  
     * @param element the element
     * @return true if the given element is contained in the set property  
     */
    public boolean contains(T element) {
        return value.contains(element);
    }


}
