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

import com.codename1.io.Preferences;

/**
 * Binds an object to the {@link com.codename1.io.Preferences} API for automatic persistent storage. 
 * You can use this API like the builder pattern by using the create method and chaining it with setters until 
 * bind is invoked.
 *
 * @author Shai Almog
 */
public class PreferencesObject {
    private PropertyBusinessObject bo;
    private String prefix;
    private boolean bound;
    
    private PreferencesObject() {}
    
    /**
     * Creates a binding object, this method doesn't do anything until bind is invoked
     * @param bo the business object this binding relates to
     * @return the object controlling the binding
     */
    public static PreferencesObject create(PropertyBusinessObject bo) {
        PreferencesObject po = new PreferencesObject();
        po.bo = bo;
        po.prefix = bo.getPropertyIndex().getName() + ".";
        return po;
    }
    
    /**
     * Binds the object so it's seamlessly stored in preferences 
     * @return this to enable builder pattern binding
     */
    public PreferencesObject bind() {
        for(PropertyBase pb : bo.getPropertyIndex()) {
            String name = (String)pb.getClientProperty("cn1-po-name");
            if(name == null) {
                name = pb.getName();
            }
            Class type = pb.getGenericType();
            String n = prefix + name;
            if(type == String.class || type == null) {
                ((Property)pb).set(Preferences.get(n, (String)((Property)pb).get()));
                bindChangeListener((Property)pb, n, type);
                continue;
            }
            if(type == Boolean.class) {
                ((Property)pb).set(Preferences.get(n, (Boolean)((Property)pb).get()));
                bindChangeListener((Property)pb, n, type);
                continue;
            }
            if(type == Double.class) {
                ((Property)pb).set(Preferences.get(n, (Double)((Property)pb).get()));
                bindChangeListener((Property)pb, n, type);
                continue;
            }
            if(type == Float.class) {
                ((Property)pb).set(Preferences.get(n, (Float)((Property)pb).get()));
                bindChangeListener((Property)pb, n, type);
                continue;
            }
            if(type == Integer.class) {
                ((Property)pb).set(Preferences.get(n, (Integer)((Property)pb).get()));
                bindChangeListener((Property)pb, n, type);
                continue;
            }
            if(type == Long.class) {
                ((Property)pb).set(Preferences.get(n, (Long)((Property)pb).get()));
                bindChangeListener((Property)pb, n, type);
                continue;
            }
            throw new IllegalStateException("Unsupported property type in preferences: " + type.getName());
        }
        bound = true;
        return this;
    }
    
    private void bindChangeListener(final Property pb, final String n, final Class type) {
        pb.addChangeListener(new PropertyChangeListener() {
            public void propertyChanged(PropertyBase p) {
                if(type == String.class || type == null) {
                    Preferences.set(n, (String)((Property)pb).get());
                    return;
                }
                if(type == Boolean.class) {
                    Preferences.set(n, (Boolean)((Property)pb).get());
                    return;
                }
                if(type == Double.class) {
                    Preferences.set(n, (Double)((Property)pb).get());
                    return;
                }
                if(type == Float.class) {
                    Preferences.set(n, (Float)((Property)pb).get());
                    return;
                }
                if(type == Integer.class) {
                    Preferences.set(n, (Integer)((Property)pb).get());
                    return;
                }
                if(type == Long.class) {
                    Preferences.set(n, (Long)((Property)pb).get());
                }
            }
        });
    }
    
    private void checkBind() {
        if(bound) {
            throw new IllegalStateException("Method can't be invoked after binding");
        }
    }
    
    /**
     * Sets the prefix for the binding, by default the object name with a "." is the common prefix
     * @param prefix a string that will prefix the name
     * @return this to enable builder pattern binding
     */
    public PreferencesObject setPrefix(String prefix) {
        checkBind();
        
        // intern is used to trigger a null pointer exception if null is used
        this.prefix = prefix.intern();
        return this;
    }

    /**
     * Sets the name of the specific field not including the prefix, by default the property name is used
     * @param pb the property
     * @param name the name for the property
     * @return this to enable builder pattern binding
     */
    public PreferencesObject setName(PropertyBase pb, String name) {
        checkBind();
        
        // intern is used to trigger a null pointer exception if null is used
        pb.putClientProperty("cn1-po-name", name);
        return this;
    }
}
