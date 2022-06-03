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
import com.codename1.ui.CN;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.util.Base64;
import com.codename1.util.regex.StringReader;
import com.codename1.xml.Element;
import com.codename1.xml.XMLWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
    private static Map<String, HashMap<String, Object>> metadata = new LinkedHashMap<String, HashMap<String, Object>>();
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
        return toString(true);
    }
    
    /**
     * Returns a user readable printout of the property values which is useful for debugging
     * 
     * @param includeNewline true to indicate that newline characters should be included
     * @return user readable printout of the property values which is useful for debugging
     */
    public String toString(boolean includeNewline) {
        StringBuilder b = new StringBuilder(name);
        b.append(" : {");
        if(includeNewline) {
            b.append('\n');
        }
        for(PropertyBase p : this) {
            b.append(p.getName());
            b.append(" = ");
            b.append(p.toString());
            if(includeNewline) {
                b.append('\n');
            }
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
     * Sets one of the builtin simple objects into a property
     * @param p the property base
     * @param val the object value
     * @return true if successful
     */
    public boolean setSimpleObject(PropertyBase p, Object val) {
        if(val == null) {
            p.setImpl(null);
            return true;
        }
        if(p.getGenericType() == null || p.getGenericType() == String.class) {
            p.setImpl(val.toString());
            return true;
        }
        if(p instanceof IntProperty) {
            p.setImpl(Util.toIntValue(val));
            return true;
        } 
        if(p instanceof BooleanProperty) {
            p.setImpl(Util.toBooleanValue(val));
            return true;
        } 
        if(p instanceof LongProperty) {
            p.setImpl(Util.toLongValue(val));
            return true;
        } 
        if(p instanceof FloatProperty) {
            p.setImpl(Util.toFloatValue(val));
            return true;
        } 
        if(p instanceof DoubleProperty) {
            p.setImpl(Util.toDoubleValue(val));
            return true;
        } 
        if(p.getGenericType() == Image.class || p.getGenericType() == EncodedImage.class) {
            if(val instanceof Image) {
                p.setImpl(val);                                    
            } else {
                if(val instanceof byte[]) {
                    p.setImpl(EncodedImage.create((byte[])val));
                } else {
                    p.setImpl(EncodedImage.create(Base64.decode(((String)val).getBytes())));
                }
            }
            return true;
        }
        if(p.getGenericType() == Date.class) {
            p.setImpl(Util.toDateValue(val));
            return true;
        }
        return false;
    }

    /**
     * This is useful for JSON parsing, it allows converting JSON map data to objects
     * @param m the map
     * @param recursiveType when running into map types we create this object type
     */
    public void populateFromMap(Map<String, Object> m, Class<? extends PropertyBusinessObject>recursiveType) {
        try {
            for(PropertyBase p : this) {
                MapAdapter ma = MapAdapter.checkInstance(p);
                if(ma != null) {
                    ma.setFromMap(p, m);
                    continue;
                }
                Object val = m.get(p.getName());
                if(val != null) {
                    if(val instanceof List) {
                        if(p instanceof CollectionProperty) {
                            if(recursiveType != null) {
                                if(((CollectionProperty)p) != null) {
                                    ((CollectionProperty)p).clear();
                                } 
                                for(Object e : (Collection)val) {
                                    if(e instanceof Map) {
                                        Class eType = ((CollectionProperty) p).getGenericType();
                                        // maybe don't use recursiveType here anymore???
                                        // elementType is usually sufficient... 
                                        Class type = (eType == null)? recursiveType : eType; 
                                        PropertyBusinessObject po = (PropertyBusinessObject)type.newInstance();
                                        po.getPropertyIndex().populateFromMap((Map<String, Object>)e, type);
                                        ((CollectionProperty)p).add(po);
                                        continue;
                                    }
                                    if(e instanceof List) {
                                        ((CollectionProperty)p).add(listParse((List)e, recursiveType));
                                        continue;
                                    }
                                    ((CollectionProperty)p).add(e);
                                }
                            } else {
                                List l = (List)val;
                                if(!l.isEmpty()) {
                                    if(l.get(0) instanceof PropertyBusinessObject 
                                            || l.get(0) instanceof String 
                                            || l.get(0) instanceof Character 
                                            || l.get(0) instanceof Boolean 
                                            || l.get(0) instanceof Integer
                                            || l.get(0) instanceof Long
                                            || l.get(0) instanceof Float
                                            || l.get(0) instanceof Double
                                            || l.get(0) instanceof Byte
                                            || l.get(0) instanceof Short
                                            || p.getGenericType() == null) {
                                        ((CollectionProperty)p).set((Collection)val);
                                    } else {
                                        Class eType = p.getGenericType();
                                        for(Object e : l) {
                                            PropertyBusinessObject po = (PropertyBusinessObject)eType.newInstance();
                                            po.getPropertyIndex().populateFromMap((Map<String, Object>)e, eType);
                                            ((CollectionProperty)p).add(po);
                                        }
                                    }
                                } else {
                                    ((CollectionProperty)p).set((Collection)val);                                
                                }
                            }
                        }
                        continue;
                    } 

                    if(val instanceof Map) {
                        if(p instanceof MapProperty) {
                            ((MapProperty)p).clear();
                            for(Object k : ((Map)val).keySet()) {
                                Object value = ((Map)val).get(k);
                                Class keyType = ((MapProperty)p).getKeyType();
                                if(keyType != null && 
                                    PropertyBusinessObject.class.isAssignableFrom(keyType)) {
                                    PropertyBusinessObject po = (PropertyBusinessObject)keyType.newInstance();
                                    po.getPropertyIndex().populateFromMap((Map<String, Object>)val, keyType);
                                    k = po;
                                }
                                Class valueType = ((MapProperty)p).getValueType();
                                if(valueType != null && 
                                    PropertyBusinessObject.class.isAssignableFrom(valueType)) {
                                    Map<String, Object> contentMap = (Map<String, Object>)val;
                                    for(String kk : contentMap.keySet()) {
                                        PropertyBusinessObject po = (PropertyBusinessObject)valueType.newInstance();
                                        Map<String, Object> vv = (Map<String, Object>)contentMap.get(kk);
                                        po.getPropertyIndex().populateFromMap(vv, valueType);
                                        ((MapProperty)p).set(kk, po);
                                    }
                                    continue;
                                } else {
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
                                }
                                ((MapProperty)p).set(k, value);
                            }                        
                            continue;
                        } else {
                            if(p.get() instanceof PropertyBusinessObject) {
                                PropertyBusinessObject po = (PropertyBusinessObject)p.get();
                                po.getPropertyIndex().populateFromMap((Map<String, Object>)val, recursiveType);
                            } else {
                                if(p.getGenericType() != null) {
                                    Object o = p.getGenericType().newInstance();
                                    if(o instanceof PropertyBusinessObject) {
                                        ((PropertyBusinessObject)o).getPropertyIndex().populateFromMap((Map<String, Object>)val);
                                        p.setImpl(o);
                                    }
                                } else {
                                    if(recursiveType != null) {
                                        PropertyBusinessObject po = (PropertyBusinessObject)recursiveType.newInstance();
                                        po.getPropertyIndex().populateFromMap((Map<String, Object>)val, recursiveType);
                                        p.setImpl(po);
                                    }
                                }
                            }
                        }
                        continue;
                    } 
                    if(setSimpleObject(p, val)) {
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
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        for(PropertyBase p : this) {
            if(p.getClientProperty(excludeFlag) != null) {
                continue;
            }
            MapAdapter ma = MapAdapter.checkInstance(p);
            if(ma != null) {
                ma.placeInMap(p, m);
                continue;
            }
            if(p instanceof MapProperty) {
                MapProperty pp = (MapProperty)p;
                m.put(p.getName(), pp.asExplodedMap());
                continue;
            }
            if(p instanceof CollectionProperty) {
            	CollectionProperty pp = (CollectionProperty)p;
                m.put(p.getName(), pp.asExplodedList());
                continue;
            }
            if(p instanceof Property) {
                Property pp = (Property)p;
                if(pp.get() != null) {
                    if(pp.getGenericType() != null && PropertyBusinessObject.class.isAssignableFrom(pp.getGenericType())) {
                        m.put(p.getName(), ((PropertyBusinessObject)pp.get()).getPropertyIndex().toMapRepresentationImpl(excludeFlag));
                    } else {
                        m.put(p.getName(), pp.get());
                    }
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
     * Returns an element object mapping to the current object hierarchy similar
     * to the map object
     * @return an XML parser element
     */
    public Element asElement() {
        return new PropertyXMLElement(this);
    } 
    
    /**
     * Converts the object to an XML representation
     * @return an XML String
     */
    public String toXML() {
        XMLWriter w = new XMLWriter(true);
        return w.toXML(asElement());
    }
    
    /**
     * Toggles whether a given property should act as a text element for this 
     * object
     * @param p the property that should act as a text element
     * @param t true to activate the text element false to remove it
     */
    public void setXmlTextElement(PropertyBase p, boolean t) {
        if(t) {
            p.putClientProperty("xmlTextElement", Boolean.TRUE);
        } else {
            p.putClientProperty("xmlTextElement", null);
        }
    }
    
    /**
     * Toggles whether a given property should act as a text element for this 
     * object
     * @param p the property 
     * @return true if this is a text element
     */
    public boolean isXmlTextElement(PropertyBase p) {
        Boolean b = (Boolean)p.getClientProperty("xmlTextElement");
        return b != null && b.booleanValue();
    }

    /**
     * Returns the property that contains the XML text e.g. {@code <xmltag>property value</xmltag>}
     * @return the property representing text XML or null if no such property was set
     */
    public PropertyBase getXmlTextElement() {
        for(PropertyBase b : this) {
            if(isXmlTextElement(b)) {
                return b;
            }
        }
        return null;
    }
    
    /**
     * Converts the XML element to this object hierarchy
     * @param e the element
     */
    public void fromXml(Element e) {
        Hashtable atts = e.getAttributes();
        if(atts != null) {
            for(Object a : atts.keySet()) {
                PropertyBase pb = get((String)a);
                if(pb != null) {
                    setSimpleObject(pb, atts.get(a));
                }
            }
        }
        int cc = e.getNumChildren();
        for(int iter = 0 ; iter < cc ; iter++) {
            Element chld = e.getChildAt(iter);
            if(chld.isTextElement()) {
                PropertyBase pt = getXmlTextElement();
                if(pt != null) {
                    String t = chld.getText();
                    pt.setImpl(t);
                }
                continue;
            }
            PropertyBase pb = get(chld.getTagName());
            Class cls = pb.getGenericType();
            if(cls.isAssignableFrom(PropertyBusinessObject.class)) {
                try {
                    PropertyBusinessObject business = (PropertyBusinessObject)cls.newInstance();
                    business.getPropertyIndex().fromXml(chld);
                    if(pb instanceof ListProperty) {
                        ((ListProperty)pb).add(business);
                    } else {
                        pb.setImpl(business);
                    }
                } catch(InstantiationException ex) {
                    Log.e(ex);
                } catch(IllegalAccessException ex) {
                    Log.e(ex);
                }
            } 
        }
    }

    /**
     * This method works similarly to a constructor, it accepts the values for the properties in the order
     * they appear within the index
     * @param values values of properties in the order they appear in the index
     */
    public void init(Object... values) {
        int offset = 0;
        for(PropertyBase pb : properties) {
            if(pb instanceof CollectionProperty) {
                if(values[offset] instanceof Object[]) {
                    ((CollectionProperty) pb).addAll(Arrays.asList((Object[])values[offset]));
                } else {
                    ((CollectionProperty) pb).addAll((Collection) values[offset]);
                }
            } else {
                pb.setImpl(values[offset]);
            }
            offset++;
        }
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
     * Writes the JSON string to storage, it's a shortcut for writing/generating the JSON
     * @param name the name of the storage file
     * @param objs a list of business objects
     */
    public static void storeJSONList(String name, List<? extends PropertyBusinessObject> objs) {
        try {
            OutputStream os = Storage.getInstance().createOutputStream(name);
            os.write(toJSONList(objs).getBytes());
            os.close();
        } catch(IOException err) {
            Log.e(err);
            throw new RuntimeException(err.toString());
        }
    }

    /**
     * Creates a JSON string, containing the list of property business objects
     *
     * @param objs a list of business objects
     * @return the JSON string
     */
    public static String toJSONList(List<? extends PropertyBusinessObject> objs) {
        StringBuilder b = new StringBuilder("[");
        boolean first = true;
        for (PropertyBusinessObject pb : objs) {
            if (first) {
                first = false;
            } else {
                b.append(",\n");
            }
            b.append(pb.getPropertyIndex().toJSON());
        }
        b.append("]");
        return b.toString();
    }

    /**
     * Loads JSON containing a list of property objects of this type
     * @param name the name of the storage
     * @return list of property objects matching this type
     */
    public <X extends PropertyBusinessObject> List<X> loadJSONList(String name) {
        try {
            if(Storage.getInstance().exists(name)) {
                return loadJSONList(Storage.getInstance().createInputStream(name));
            }
            InputStream is = CN.getResourceAsStream("/" + name);
            return loadJSONList(is);
        } catch(IOException err) {
            Log.e(err);
            throw new RuntimeException(err.toString());
        }
    }

    /**
     * Loads JSON containing a list of property objects of this type
     * @param stream the input stream
     * @return list of property objects matching this type
     */
    public <X extends PropertyBusinessObject> List<X> loadJSONList(InputStream stream)
        throws IOException {
        JSONParser jp = new JSONParser();
        JSONParser.setUseBoolean(true);
        JSONParser.setUseLongs(true);
        List<X> response = new ArrayList<X>();
        Map<String, Object> result = jp.parseJSON(new InputStreamReader(stream, "UTF-8"));
        List<Map> entries = (List<Map>)result.get("root");
        for(Map m : entries) {
            X pb = (X)newInstance();
            pb.getPropertyIndex().populateFromMap(m, parent.getClass());
            response.add(pb);
        }
        return response;
    }

    /**
     * Creates a new instance of the parent class
     * @return an instance of the parent class or null if this failed
     */
    public PropertyBusinessObject newInstance() {
        try {
            return (PropertyBusinessObject)parent.getClass().newInstance();
        } catch(Exception err) {
            Log.e(err);
            return null;
        }
    }

    /**
     * Populates the object from a JSON string
     * @param jsonString the JSON String
     */
    public void fromJSON(String jsonString) {
        try {
            StringReader r = new StringReader(jsonString);
            JSONParser jp = new JSONParser();
            JSONParser.setUseBoolean(true);
            JSONParser.setUseLongs(true);
            populateFromMap(jp.parseJSON(r), parent.getClass());
        } catch(IOException err) {
            Log.e(err);
            throw new RuntimeException(err.toString());
        }
    }
    
    /**
     * Loads JSON for the object from storage with the given name if it exists. If the storage
     * file doesn't exist getResources() will be used to find a default JSON file in the root
     * of the package
     * @param name the name of the storage
     */
    public void loadJSON(String name) {
        try {
            if(Storage.getInstance().exists(name)) {
                loadJSON(Storage.getInstance().createInputStream(name));
            } else {
                InputStream is = CN.getResourceAsStream("/" + name);
                if(is != null) {
                    loadJSON(is);
                } else {
                    throw new IOException("Storage file not found: " + name);
                }
            }
        } catch(IOException err) {
            Log.e(err);
            throw new RuntimeException(err.toString());
        }
    }

    /**
     * Loads JSON for the object from the given input stream
     * @param stream the input stream containing the JSON file
     */
    public void loadJSON(InputStream stream) throws IOException {
        JSONParser jp = new JSONParser();
        JSONParser.setUseBoolean(true);
        JSONParser.setUseLongs(true);
        populateFromMap(jp.parseJSON(new InputStreamReader(stream, "UTF-8")), parent.getClass());
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
        if(o instanceof PropertyBusinessObject) {
            return equals(((PropertyBusinessObject)o).getPropertyIndex());
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
                    if(b instanceof CollectionProperty) {
                        out.writeByte(2);
                        Util.writeObject(((CollectionProperty)b).asList(), out);
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
                            
                        case 2: // CollectionProperty
                            if(pb instanceof CollectionProperty) {
                                ((CollectionProperty)pb).set((List)data);
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
