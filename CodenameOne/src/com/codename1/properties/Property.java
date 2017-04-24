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

/**
 * Base class for a property, it can store a generic value of any type and broadcast change events to 
 * external listeners
 *
 * @author Shai Almog
 */
public class Property<T, K> extends PropertyBase<T, K> {
    private T value;
    
    /**
     * Constructs a property with the given name and value
     * @param name the name of the property
     * @param value the default value for the property
     */
    public Property(String name, T value) {
        super(name);
        this.value = value;
    }
    
    /**
     * Constructs a property with the given name and value
     * @param name the name of the property
     * @param genericType the type of the property
     * @param value the default value for the property
     */
    public Property(String name, Class genericType, T value) {
        super(name, genericType);
        this.value = value;
    }

    /**
     * Constructs a property with null value
     * @param name the name of the property
     */
    public Property(String name) {
        super(name);
    }
    
    /**
     * Constructs a property with null value
     * @param genericType the type of the property
     * @param name the name of the property
     */
    public Property(String name, Class genericType) {
        super(name, genericType);
    }

    /**
     * Gets the property value
     * @return the property value
     */
    public T get() {
        return value;
    }
    
    /**
     * Sets the property value and potentially fires a change event
     * @param value the new value
     */
    public K set(T value) {
        if(this.value != value) { 
            this.value = value;
            firePropertyChanged();
        }
        if(parent == null) {
            // allows properties to work even if they aren't registered in the index
            return null;
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
        Property other = (Property)obj;
        Object otherval = other.get();
        if(otherval == value) {
            return true;
        }
        return otherval != null && otherval.equals(value);
    }

    /**
     * Returns the internal hashcode or 0 for null property
     * @return the hashcode value
     */
    @Override
    public int hashCode() {
        if(value == null) return 0;
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "" + value;
    }

    @Override
    void setImpl(Object val) {
        set((T)val);
    }
}
