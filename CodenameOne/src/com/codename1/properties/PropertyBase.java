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
import java.util.List;

/**
 * Base class for property types
 *
 * @author Shai Almog
 */
public class PropertyBase<T, K> {
    private final String name;
    private Class genericType;
    private ArrayList<PropertyChangeListener<T, K>> listeners;
    PropertyIndex parent;
    private static PropertyChangeListener lastChangeListener;

    /**
     * Used internally to detect if a property was read by a user
     */
    static PropertyChangeListener onGlobalGetProperty;

    /**
     * Used internally to detect if a property was modified by a user
     */
    static PropertyChangeListener onGlobalSetProperty;

    /**
     * All properties must have a name
     * @param name the name of the property
     */
    protected PropertyBase(String name) {
        this.name = name;
    }

    
    /**
     * Provides the internal list of listeners
     * @return internal list of listeners
     */
    List<PropertyChangeListener<T, K>> getListeners() {
        return listeners;
    }

    void internalGet() {
        if(onGlobalGetProperty != null) {
            onGlobalGetProperty.propertyChanged(this);
        }
    }

    void internalSet() {
        if(onGlobalSetProperty != null) {
            onGlobalSetProperty.propertyChanged(this);
        }
    }


    /**
     * Binds an event callback for set calls and property mutation
     * @param listener will be invoked whenever any mutable property is changed
     * @throws RuntimeException if a set listener is already bound, there can be only one per application
     * @deprecated Usage of this method isn't recommended, it's designed for internal use
     */
    public static void bindGlobalSetListener(PropertyChangeListener listener) {
        if (onGlobalSetProperty != null && listener != null) {
            throw new RuntimeException("Set Listener already bound");
        }
        onGlobalSetProperty = listener;
    }

    /**
     * Binds an event callback for get calls and property reads
     * @param listener will be invoked whenever any property is read
     * @throws RuntimeException if a get listener is already bound, there can be only one per application
     * @deprecated Usage of this method isn't recommended, it's designed for internal use
     */
    public static void bindGlobalGetListener(PropertyChangeListener listener) {
        if (onGlobalGetProperty != null && listener != null) {
            throw new RuntimeException("Get Listener already bound");
        }
        onGlobalGetProperty = listener;
    }

    /**
     * All properties must have a name, a generic type is helpful
     * @param name the name of the property
     * @param genericType the property type to workaround issues with erasure
     */
    protected PropertyBase(String name, Class genericType) {
        this.name = name;
        this.genericType = genericType;
    }
        
    /**
     * The property name is immutable and can't be changed after creation it should match the parent field name by convention
     * @return the property name;
     */
    public String getName() {
        return name;
    }

    /**
     * Delivers the property change event to listeners if applicable
     */
    protected void firePropertyChanged() {
        if(listeners != null) {
            for(PropertyChangeListener pl : listeners) {
                lastChangeListener = pl;
                pl.propertyChanged(this);
                lastChangeListener = null;
            }
        }
    }

    /**
     * This method will work when invoked from a propertyChanged callback and should be similar to 
     * {@code removePropertyChangeListener(this)}. It's useful for lambda's where {@code this} 
     * means the base class and not the listener so {@code removePropertyChangeListener(this)} 
     * won't do what we want unless we convert to an inner class
     */
    public void stopListening() {
        removeChangeListener(lastChangeListener);
    }
    
    /**
     * Fires a notification that a property value changed to the given listener
     * @param pl the listener
     */
    public void addChangeListener(PropertyChangeListener<T, K> pl) {
        if(listeners == null) {
            listeners = new ArrayList<PropertyChangeListener<T, K>>();
        }
        listeners.add(pl);
    }
    
    /**
     * Removes the property change listener from the list of listeners
     * @param pl the change listener
     */
    public void removeChangeListener(PropertyChangeListener<T, K> pl) {
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

    /**
     * Compares this property to another property
     * @param obj the other property
     * @return true if they are equal in name and value
     */
    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj.getClass() == getClass()) {
            PropertyBase other = (PropertyBase)obj;
            if(other.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    T get() {
        return null;
    }
    
    void setImpl(Object val) {
    }
    
    /**
     * Default toString that provides easier debug information
     * @return a formatted representation of the property for debugging
     */
    public String toString() {
        T o = get();
        if(o == null) {
            return getName() + " = null";
        }
        return getName() + " = '" + o + "' : " +o.getClass().getName();
    }
    
    /**
     * Returns the generic type of this property if it is known or null
     * @return the generic type
     */
    public Class getGenericType() {
        return genericType;
    }
    
    /**
     * The label of the property defaults to its name but can be changed to anything, it can be used
     * when binding a property to UI elements
     * @param label the new label value
     */
    public void setLabel(String label) {
        putClientProperty("cn1PropertyLabel", label);
    }
    
    /**
     * The label of the property defaults to its name but can be changed to anything
     * 
     * @return the label for the property
     */
    public String getLabel() {
        String l = (String)getClientProperty("cn1PropertyLabel");
        if(l == null) {
            return getName();
        }
        return l;
    }

    /**
     * Validates that the collection type is valid and throws an exception otherwise
     * @param elementType the generic type of the collection
     */
    protected final void validateCollectionType(Class elementType) {
        if(elementType == null || !PropertyBusinessObject.class.isAssignableFrom(elementType)) {
            if(elementType == String.class || elementType == Integer.class ||
                elementType == Long.class  || elementType == Double.class ||
                elementType == Byte.class  || elementType == Float.class ||
                elementType == Boolean.class  || elementType == Character.class) {
                return;
            }
            throw new IllegalArgumentException(
                    "the element type class needs to be a subclass of PropertyBusinessObject");
        }
    }
}
