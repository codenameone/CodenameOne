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

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Instances of this class adapt an arbitrary object type so a property can 
 * appear differently when it's placed into a Map or initialized from a Map.
 * This impacts JSON generation too as it relies heavily on the process of 
 * transforming elements to/from maps.</p>
 * <p>To use this class create a new subclass of it and override the appropriate
 * methods.</p>
 * 
 * @author Shai Almog
 */
public abstract class MapAdapter {
    private final Class type;
    private final static Map<String, MapAdapter> lookup = new HashMap<String, MapAdapter>();
    
    /**
     * By default subclasses should target a specific type but this can be
     * further narrowed by overriding {@link #useAdapterFor(com.codename1.properties.PropertyBase)}.
     * @param type the type to which the map adapter is bound
     */
    protected MapAdapter(Class type) {
        this.type = type;
        lookup.put(type.getName(), this);
    }
    
    static MapAdapter checkInstance(PropertyBase b) {
        if(b.getGenericType() != null) {
            MapAdapter a = lookup.get(b.getGenericType().getName());
            if(a != null && a.useAdapterFor(b)) {
                return a;
            }
        }
        return null;
    }
    
    /**
     * Returns true if the adapter should be used for this property
     * 
     * @param b the property
     * @return true if this adapter should be used for this property
     */
    public boolean useAdapterFor(PropertyBase b) {
        return b.getGenericType() == type;
    }
    
    /**
     * Places the given property into the given map
     * @param b the property object
     * @param m the map instance
     */
    public void placeInMap(PropertyBase b, Map m) {
        m.put(b.getName(), b.get());
    }
    
    /**
     * Sets the value of the property from the map object
     * @param b the property object
     * @param m the map instance
     */
    public void setFromMap(PropertyBase b, Map m){
        b.setImpl(m.get(b.getName()));
    }
}
