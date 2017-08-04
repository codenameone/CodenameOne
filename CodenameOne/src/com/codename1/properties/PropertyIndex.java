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

import com.codename1.io.Externalizable;
import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.processing.Result;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
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
public class PropertyIndex implements Iterable<PropertyBase> {
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
        populateFromMap(m, null);
    }
    
    private Object listParse(List l, Class<? extends PropertyBusinessObject>recursiveType) throws InstantiationException, IllegalAccessException {
        ArrayList al = new ArrayList();
        for(Object o : l) {
            if(o instanceof Map) {
                PropertyBusinessObject po = (PropertyBusinessObject)recursiveType.newInstance();
                po.getPropertyIndex().populateFromMap((Map<String, Object>)o, recursiveType);
                al.add(po);
                continue;
            }
            if(o instanceof List) {
                al.add(listParse((List)o,recursiveType));
                continue;
            }
            al.add(o);
        }
        return al;
    }

    /**
     * This is useful for JSON parsing, it allows converting JSON map data to objects
     * @param m the map
     * @param recursiveType when running into map types we create this object type
     */
    public void populateFromMap(Map<String, Object> m, Class<? extends PropertyBusinessObject>recursiveType) {
        try {
            for(PropertyBase p : this) {
                Object val = m.get(p.getName());
                if(val != null) {
                    if(val instanceof List) {
                        if(p instanceof ListProperty) {
                            if(recursiveType != null) {
                                if(((ListProperty)p) != null) {
                                    ((ListProperty)p).clear();
                                } 
                                for(Object e : (Collection)val) {
                                    if(e instanceof Map) {
                                        Class eType = ((ListProperty) p).getGenericType();
                                        // maybe don't use recursiveType here anymore???
                                        // elementType is usually sufficient... 
                                        Class type = (eType == null)? recursiveType : eType; 
                                        PropertyBusinessObject po = (PropertyBusinessObject)type.newInstance();
                                        po.getPropertyIndex().populateFromMap((Map<String, Object>)e, type);
                                        ((ListProperty)p).add(po);
                                        continue;
                                    }
                                    if(e instanceof List) {
                                        ((ListProperty)p).add(listParse((List)e, recursiveType));
                                        continue;
                                    }
                                    ((ListProperty)p).add(e);
                                }
                            } else {
                                ((ListProperty)p).setList((Collection)val);
                            }
                        }
                        continue;
                    } 

                    if(val instanceof Map) {
                        if(p instanceof MapProperty) {
                            ((MapProperty)p).clear();
                            for(Object k : ((Map)val).keySet()) {
                                Object value = ((Map)val).get(k);
                                if(value instanceof Map) {
                                    PropertyBusinessObject po = (PropertyBusinessObject)p.get();
                                    po.getPropertyIndex().populateFromMap((Map<String, Object>)value, recursiveType);
                                    ((MapProperty)p).set(k, po);
                                    continue;
                                }
                                if(value instanceof List) {
                                    ((MapProperty)p).set(k, listParse((List)value, recursiveType));
                                    continue;
                                }
                                ((MapProperty)p).set(k, value);
                            }                        
                            continue;
                        } else {
                            if(p.get() instanceof PropertyBusinessObject) {
                                PropertyBusinessObject po = (PropertyBusinessObject)p.get();
                                po.getPropertyIndex().populateFromMap((Map<String, Object>)val, recursiveType);
                            } else {
                                if(recursiveType != null) {
                                    PropertyBusinessObject po = (PropertyBusinessObject)recursiveType.newInstance();
                                    po.getPropertyIndex().populateFromMap((Map<String, Object>)val, recursiveType);
                                    p.setImpl(po);
                                }
                            }
                        }
                        continue;
                    } 
                    if(p instanceof IntProperty) {
                        p.setImpl(Util.toIntValue(val));
                        continue;
                    } 
                    if(p instanceof LongProperty) {
                        p.setImpl(Util.toLongValue(val));
                        continue;
                    } 
                    if(p instanceof FloatProperty) {
                        p.setImpl(Util.toFloatValue(val));
                        continue;
                    } 
                    if(p instanceof DoubleProperty) {
                        p.setImpl(Util.toDoubleValue(val));
                        continue;
                    } 
                    p.setImpl(val);                
                }
            }
        } catch(InstantiationException err) {
            Log.e(err);
            throw new RuntimeException("Can't create instanceof class: " + err);
        } catch(IllegalAccessException err) {
            Log.e(err);
            throw new RuntimeException("Can't create instanceof class: " + err);
        }            
    }
    
    /**
     * This is useful in converting a property object to JSON
     * @return a map representation of the properties
     */
    public Map<String, Object> toMapRepresentation() {
        return toMapRepresentationImpl("mapExclude");
    }

    /**
     * This is useful in converting a property object to JSON
     * @return a map representation of the properties
     */
    private Map<String, Object> toMapRepresentationImpl(String excludeFlag) {
        HashMap<String, Object> m = new HashMap<String, Object>();
        for(PropertyBase p : this) {
            if(p.getClientProperty(excludeFlag) != null) {
                continue;
            }
            if(p instanceof MapProperty) {
                MapProperty pp = (MapProperty)p;
                m.put(p.getName(), pp.asExplodedMap());
                continue;
            }
            if(p instanceof ListProperty) {
                ListProperty pp = (ListProperty)p;
                m.put(p.getName(), pp.asExplodedList());
                continue;
            }
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
        return Result.fromContent(toMapRepresentationImpl("jsonExclude")).toString();
    }
    
    /**
     * Writes the JSON string to storage, it's a shortcut for writing/generating the JSON
     * @param name the name of the storage file
     */
    public void storeJSON(String name) {
        try {
            OutputStream os = Storage.getInstance().createOutputStream(name);
            os.write(toJSON().getBytes("UTF-8"));
            os.close();
        } catch(IOException err) {
            Log.e(err);
            throw new RuntimeException(err.toString());
        }
    }
    
    /**
     * Loads JSON for the object from storage with the given name
     * @param name the name of the storage
     */
    public void loadJSON(String name) {
        try {
            InputStream is = Storage.getInstance().createInputStream(name);
            JSONParser jp = new JSONParser();
            JSONParser.setUseBoolean(true);
            JSONParser.setUseLongs(true);
            populateFromMap(jp.parseJSON(new InputStreamReader(is, "UTF-8")), parent.getClass());
        } catch(IOException err) {
            Log.e(err);
            throw new RuntimeException(err.toString());
        }
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
    
    /**
     * Allows us to exclude a specific property from the toJSON process
     * @param pb the property
     * @param exclude true to exclude and false to reinclude
     */
    public void setExcludeFromJSON(PropertyBase pb, boolean exclude) {
        if(exclude) {
            pb.putClientProperty("jsonExclude", Boolean.TRUE);
        } else {
            pb.putClientProperty("jsonExclude", null);
        }
    }
    
    /**
     * Indicates whether the given property is excluded from the {@link #toMapRepresentation()} 
     * method output
     * @param pb the property
     * @return true if the property is excluded and false otherwise
     */
    public boolean isExcludeFromMap(PropertyBase pb) {
        return pb.getClientProperty("mapExclude") != null;
    }

    /**
     * Allows us to exclude a specific property from the {@link #toMapRepresentation()} process
     * @param pb the property
     * @param exclude true to exclude and false to reinclude
     */
    public void setExcludeFromMap(PropertyBase pb, boolean exclude) {
        if(exclude) {
            pb.putClientProperty("mapExclude", Boolean.TRUE);
        } else {
            pb.putClientProperty("jsonExclude", null);
        }
    }
    
    /**
     * Indicates whether the given property is excluded from the {@link #toJSON()} method output
     * @param pb the property
     * @return true if the property is excluded and false otherwise
     */
    public boolean isExcludeFromJSON(PropertyBase pb) {
        return pb.getClientProperty("jsonExclude") != null;
    }
    
    /**
     * Invoking this method will allow a property object to be serialized seamlessly
     */
    public void registerExternalizable() {
        Util.register(getName(), parent.getClass());
    }
    
    /**
     * Returns an externalizable object for serialization of this business object, unlike regular
     * externalizables this implementation is robust to changes, additions and removals of 
     * properties
     * @return an externalizable instance
     */
    public Externalizable asExternalizable() {
        return new Externalizable() {
            public int getVersion() {
                return 1;
            }

            public void externalize(DataOutputStream out) throws IOException {
                out.writeInt(getSize());
                for(PropertyBase b : PropertyIndex.this) {
                    out.writeUTF(b.getName());
                    if(b instanceof ListProperty) {
                        out.writeByte(2);
                        Util.writeObject(((ListProperty)b).asList(), out);
                        continue;
                    }
                    if(b instanceof MapProperty) {
                        out.writeByte(3);
                        Util.writeObject(((MapProperty)b).asMap(), out);
                        continue;
                    }
                    if(b instanceof Property) {
                        out.writeByte(1);
                        Util.writeObject(((Property)b).get(), out);
                        continue;
                    }
                }
            }

            public void internalize(int version, DataInputStream in) throws IOException {
                int size = in.readInt();
                for(int iter = 0 ; iter < size ; iter++) {
                    String pname = in.readUTF();
                    int type = in.readByte();
                    Object data = Util.readObject(in);
                    PropertyBase pb = get(pname);
                    switch(type) {
                        case 1: // Property
                            if(pb instanceof Property) {
                                ((Property)pb).set(data);
                            }
                            break;
                            
                        case 2: // ListProperty
                            if(pb instanceof ListProperty) {
                                ((ListProperty)pb).setList((List)data);
                            }
                            break;
                            
                        case 3: // MapProperty
                            if(pb instanceof MapProperty) {
                                ((MapProperty)pb).setMap((Map)data);
                            }
                            break;
                    }
                }
            }

            public String getObjectId() {
                return getName();
            }
        };
    }
}
