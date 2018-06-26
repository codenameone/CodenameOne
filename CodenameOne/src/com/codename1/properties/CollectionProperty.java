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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Base class for a property as a collection which can contain multiple elements within
 */
public abstract class CollectionProperty<T, K> extends PropertyBase<T, K> implements Iterable<T> {
    
      
    public CollectionProperty(String name) {
        super(name);
    }
      
    
    public CollectionProperty(String name, Class genericType) {
        super(name, genericType);
        validateCollectionType(genericType);
    }
    
    
    /**
     * The size of the property collection
     * @return the number of elements
     */
    public abstract int size();
      

    /**
     * Sets the entire content of the property
     * @param t the collection of elements to set
     * @return the parent object for chaining
     */
    public abstract  K set(Collection<T> t);
   

    /**
     * Adds a property value to the collection and fires a change event if collection has changed
     * @param v the new value
     */
    public abstract K add(T v);


    /**
     * Adds a collection of values to the collection and fires a change event if the collection has changed
     * @param v the collection of values to add
     */
    public abstract K addAll(Collection<? extends T> v);
    
    /**
     * Removes the given item 
     * @param v the item to remove
     */
    public abstract K remove(T v);
    
    /**
     * Removes from this collection all of its elements that are contained in the specified collection
     * and fires a change event if the collection has changed
     * @param v the collection of values to remove
     */
    public abstract K removeAll(Collection<? extends T> v);
    
    /**
     * Iterate over the elements of the property
     * @return an iterator
     */
    public abstract Iterator<T> iterator();
    
    /**
     * Returns a copy of the content as a new list
     * @return a list
     */
    public abstract List<T> asList();
    
    /**
     * Returns a copy of the content as a new list but if the value is a PropertyBusinessObject it will 
     * be converted to a Map 
     * @return a list
     */
    public abstract List<Object> asExplodedList();

    /**
     * Remove all the elements from the collection and fires a change event
     */
    public abstract void clear();

    /**
     * Returns true if the given element is contained in the collection property  
     * @param element the element
     * @return true if the given element is contained in the collection property  
     */
    public abstract boolean contains(T element);

}
