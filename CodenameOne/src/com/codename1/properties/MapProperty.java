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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base class for a property as a Map which can contain multiple elements within it
 *
 * @author Shai Almog
 */
public class MapProperty<T1, T2, K> extends PropertyBase<Map.Entry<T1, T2>, K> implements Iterable<Map.Entry<T1, T2>> {
    private LinkedHashMap<T1, T2> value = new LinkedHashMap<T1, T2>();
    
    /**
     * Constructs a property with the given name 
     * @param name the name of the property
     */
    public MapProperty(String name) {
        super(name);
    }
    
    /**
     * Gets the property value
     * @param key the map key
     * @return the property value
     */
    public T2 get(T1 key) {
        return value.get(key);
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
     * @param key the key to set
     * @param v the new value
     */
    public K set(T1 key, T2 v) {
        value.put(key, v);
        firePropertyChanged();
        return (K)parent.parent;
    }


    /**
     * Removes the item matching the given key
     * @param key the key
     */
    public K remove(T1 key) {
        value.remove(key);
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
        MapProperty other = (MapProperty)obj;
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
    public Iterator<Map.Entry<T1, T2>> iterator() {
        return value.entrySet().iterator();
    }
    
    /**
     * Returns the set of keys in the map property
     * @return the keys
     */
    public Set<T1> keySet() {
        return value.keySet();
    }

    /**
     * Returns the set of values in the map property
     * @return the values
     */
    public Collection<T2> valueSet() {
        return value.values();
    }
    
    /**
     * Returns a copy of the content as a new list
     * @return a list
     */
    public Map<T1, T2> asMap() {
        return new LinkedHashMap<T1, T2>(value);
    }

    /**
     * Sets the entire content of the property
     * @param t the map of elements to set
     * @return the parent object for chaining
     */
    public K setMap(Map<T1, T2> t) {
        value.clear();
        value.putAll(t);
        firePropertyChanged();        
        return (K)parent.parent;
    } 
}
