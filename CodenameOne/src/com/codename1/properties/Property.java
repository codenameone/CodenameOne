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
 * Base class for a property, it can store a generic value of any type and broadcast change events to extrenal
 * listeners
 *
 * @author Shai Almog
 */
public class Property<T> {
    private final String name;
    private T value;
    private ArrayList<PropertyChangeListener> listeners;
    PropertyIndex parent;
    
    /**
     * Constructs a property with the given name and value
     * @param name the name of the property
     * @param value the default value for the property
     */
    public Property(String name, T value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Constructs a property with null value
     * @param name the name of the property
     */
    public Property(String name) {
        this.name = name;
    }
    
    /**
     * The property name is immutable and can't be changed after creation it should match the parent field name by convention
     * @return the property name;
     */
    public String getName() {
        return name;
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
    public void set(T value) {
        if(this.value != value) { 
            this.value = value;
            if(listeners != null) {
                for(PropertyChangeListener pl : listeners) {
                    pl.propertyChanged(this);
                }
            }
        }
    }
    
    /**
     * Fires a notification that a property value changed to the given listener
     * @param pl the listener
     */
    public void addChangeListener(PropertyChangeListener pl) {
        if(listeners == null) {
            listeners = new ArrayList<PropertyChangeListener>();
        }
        listeners.add(pl);
    }
    
    /**
     * Removes the property change listener from the list of listeners
     * @param pl the change listener
     */
    public void removeChangeListener(PropertyChangeListener pl) {
        if(listeners != null) {
            listeners.remove(pl);
            if(listeners.size() == 0) {
                listeners = null;
            }
        }
    }
    
    /**
     * Places a property that will apply statically to all instances of this property
     * @param key the key to put
     * @param o the value object
     */
    public void putClientProperty(String key, Object o) {
        parent.putMetaDataOfClass("cn1$field" + name + "-" + key, o);
    }
    
    /**
     * Returns the client property set to this property name
     * @param key the key of the property
     * @return the value that was previously placed with put client property
     */
    public Object getClientProperty(String key) {
        return parent.getMetaDataOfClass("cn1$field" + name + "-" + key);
    }
}
