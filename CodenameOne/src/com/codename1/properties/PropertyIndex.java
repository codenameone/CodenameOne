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

import com.codename1.processing.Result;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Maps the properties that are in a class/object and provides access to them so tools such as ORM
 * can implicitly access them for us. This class also holds the class level meta-data for a specific property
 * or class. It also provides utility level tools e.g. toString implementation etc.
 *
 * @author Shai Almog
 */
public class PropertyIndex implements Iterable<PropertyBase>{
    private final PropertyBase[] properties;
    private static HashMap<String, HashMap<String, Object>> metadata = new HashMap<String, HashMap<String, Object>>();
    PropertyBusinessObject parent;
    private final String name;
    
    /**
     * The constructor is essential for a proper property business object
     * 
     * @param parent the parent object instance
     * @param name the name of the parent class
     * @param properties the list of properties in the object
     */
    public PropertyIndex(PropertyBusinessObject parent, String name, PropertyBase... properties) {
        this.properties = properties;
        this.parent = parent;
        this.name = name;
        for(PropertyBase p : properties) {
            p.parent = this;
        }
    }
    
    /**
     * The name of the parent business object
     * @return a unique name for the parent
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns a property by its name
     * @param name the name of the property (case sensitive)
     * @return the property or null
     */
    public PropertyBase get(String name) {
        for(PropertyBase p : properties) {
            if(p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Returns a property by its name regardless of case sensitivity for the name
     * @param name the name of the property (case insensitive)
     * @return the property or null
     */
    public PropertyBase getIgnoreCase(String name) {
        for(PropertyBase p : properties) {
            if(p.getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }
    
    /**
     * Allows us to get an individual property within the object instance
     * @param i the index of the property
     * @return the property instance
     */
    public PropertyBase get(int i) {
        return properties[i];
    }
    
    /**
     * The number of properties in the class
     * @return number of properties in the class
     */
    public int getSize() {
        return properties.length;
    }

    /**
     * Allows us to traverse the properties with a for-each statement
     * @return an iterator instance
     */
    public Iterator<PropertyBase> iterator() {
        return new Iterator<PropertyBase>() {
            int off = 0;
            public boolean hasNext() {
                return off < properties.length;
            }

            public void remove() {
            }
            
            public PropertyBase next() {
                int i = off;
                off++;
                return properties[i];
            }
        };
    }
    

    private HashMap<String, Object> getProps() {
        HashMap<String,Object> m = metadata.get(parent.getClass().getName());
        if(m == null) {
            m = new HashMap<String, Object>();
            metadata.put(parent.getClass().getName(), m);
        }
        return m;
    }
    
    /**
     * Allows us to fetch class meta data not to be confused with standard properties
     * @param meta the meta data unique name
     * @return the object instance
     */
    public Object getMetaDataOfClass(String meta) {
        return getProps().get(meta);
    }

    /**
     * Sets class specific metadata
     * 
     * @param meta the name of the meta data
     * @param o object value for the meta data
     */
    public void putMetaDataOfClass(String meta, Object o) {
        if(o == null) {
            getProps().remove(meta);
        } else {
            getProps().put(meta, o);
        }
    }
    
    /**
     * Returns a user readable printout of the property values which is useful for debugging
     * @return user readable printout of the property values which is useful for debugging
     */
    public String toString() {
        StringBuilder b = new StringBuilder(name);
        b.append(" : {\n");
        for(PropertyBase p : this) {
            b.append(p.getName());
            b.append(" = ");
            b.append(p.toString());
            b.append("\n");
        }
        b.append("}");
        return b.toString();
    }
    
    /**
     * This is useful for JSON parsing, it allows converting JSON map data to objects
     * @param m the map
     */
    public void populateFromMap(Map<String, Object> m) {
        for(PropertyBase p : this) {
            Object val = m.get(p.getName());
            if(val != null) {
                p.setImpl(val);
            }
        }
    }
    
    /**
     * This is useful in converting a property object to JSON
     * @return a map representation of the properties
     */
    public Map<String, Object> toMapRepresentation() {
        HashMap<String, Object> m = new HashMap<String, Object>();
        for(PropertyBase p : this) {
            if(p instanceof Property) {
                Property pp = (Property)p;
                if(pp.get() != null) {
                    m.put(p.getName(), pp.get());
                }
            }
        }
        return m;
    }
    
    /**
     * Converts the object to a JSON representation
     * @return a JSON String
     */
    public String toJSON() {
        return Result.fromContent(toMapRepresentation()).toString();
    }
    
    /**
     * Returns true if the given object equals the property index
     * @param o the object
     * @return true if equals
     */
    public boolean equals(Object o) {
        if(o instanceof PropertyIndex) {
            PropertyIndex other = (PropertyIndex)o;
            if(parent == other.parent) {
                return true;
            }
            if(parent.getClass() != other.parent.getClass()) {
                return false;
            }
            if(properties.length == other.properties.length) {
                for(int iter = 0 ; iter < properties.length ; iter++) {
                    if(!properties[iter].equals(other.properties[iter])) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * The hashcode of the object
     * @return a composite of the hashcodes of the properties
     */
    @Override
    public int hashCode() {
        int value = 0;
        for(int iter = 0 ; iter < properties.length ; iter++) {
            if(properties[iter] instanceof Property) {
                Object v = ((Property)properties[iter]).get();
                if(v != null) {
                    int b = v.hashCode();
                    value = 31 * value + (int) (b ^ (b >>> 32));
                }
            }
        }
        return value;
    }
    
    
}
