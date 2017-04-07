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
public class ListProperty<T, K> extends PropertyBase<T, K> implements Iterable<T> {
    private ArrayList<T> value = new ArrayList<T>();
    
    public final Class<T> elementType;
    
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
        super(name);
        for(T t : values) {
            value.add(t);
        }
        if(elementType == null || !PropertyBusinessObject.class.isAssignableFrom(elementType)) 
            throw new IllegalArgumentException(
                    "the element type class needs to be a subclass of PropertyBusinessObject");
        
        this.elementType = elementType;
    }
    
    /**
     * Constructs a property with null value
     * @param name the name of the property
     */
    public ListProperty(String name) {
        super(name);
        this.elementType = null;
    }
    
    /**
     * Gets the property value
     * @param offset the offset within the list
     * @return the property value
     */
    public T get(int offset) {
        return value.get(offset);
    }
    
    /**
     * The size of the property list
     * @return the number of elements
     */
    public int size() {
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
        return (K)parent.parent;
    }

    /**
     * Sets the entire content of the property
     * @param t the collection of elements to set
     * @return the parent object for chaining
     */
    public K setList(Collection<T> t) {
        value.clear();
        value.addAll(t);
        firePropertyChanged();        
        return (K)parent.parent;
    } 
    
    /**
     * Adds a property value and fires a change event
     * @param offset the position for the new value
     * @param v the new value
     */
    public K add(int offset, T v) {
        value.add(offset, v);
        firePropertyChanged();
        return (K)parent.parent;
    }


    /**
     * Adds a property value to the end of the list and fires a change event
     * @param v the new value
     */
    public K add(T v) {
        value.add(v);
        firePropertyChanged();
        return (K)parent.parent;
    }


    /**
     * Removes the item at the given offset
     * @param offset the offset
     */
    public K remove(int offset) {
        value.remove(offset);
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
     * Remove all the elements from the list
     */
    public void clear() {
        if(value.size() > 0) {
            value.clear();
            firePropertyChanged();
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
