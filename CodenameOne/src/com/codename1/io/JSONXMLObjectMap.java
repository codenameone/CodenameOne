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

package com.codename1.io;

import com.codename1.util.Base64;
import com.codename1.xml.Element;
import com.codename1.xml.XMLParser;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps objects back and forth to JSON/XML for usage with webservices, requires that
 * all mappable objects implement the Mappable interface and be registered with this
 * class
 *
 * @author Shai Almog
 */
class JSONXMLObjectMap {
    private static HashMap<String, Class> objectMap;
    private static HashMap<String, String[]> objectToAttributes;
    
    /**
     * Registers a class implementing the Mappable interface so it can be serialized to/from JSON
     * @param type the object type
     * @param cls the class
     */
    public static void register(String type, Class cls) {
        if(objectMap == null) {
            objectMap = new HashMap<String, Class>();
        }
        objectMap.put(type, cls);
    }
    
    private static void initObjectAttributes() {
        if(objectToAttributes == null && objectMap != null) {
            objectToAttributes = new HashMap<String, String[]>();
            for(String k : objectMap.keySet()) {
                Class val = objectMap.get(k);
                try {
                    String[] s = ((Mappable)val.newInstance()).getSupportedProperties();
                    objectToAttributes.put(k, s);
                } catch(Throwable t) {
                    t.printStackTrace();
                    throw new RuntimeException(t.toString());
                }
            }
        }
    }
    
    /**
     * Parses XML and returns an object hierarchy based on mappable objects registered to this class
     * @param i the reader stream
     * @return a mappable object hierarchy
     * @throws IOException if thrown by the stream or the parsing
     */
    /*public static Object parseXML(Reader i) throws IOException {
        XMLParser p = new XMLParser();
        Element elem = p.parse(i);
    }*/
    
    /**
     * Parses JSON into mappable objects when applicable
     * @param i the reader to load from
     * @param typeAttribute the type attribute is an attribute within the JSON that
     * should map to the registered type of a mappable. If its available an object instance
     * will map to the type e.g. if JSON such as { "type": "MyObject", "value": "MyValue" } exists
     * and the MyObject type is registered that object will be instantiated. However, if this
     * isn't the case the object instances will be scanned and the best match will be picked
     * based on the number of supported attributes.
     * @return a mappable object or hierarchy of such
     * @throws IOException 
     */
    public static Object parseJSON(Reader i, String typeAttribute) throws IOException {
        JSONParser jp = new JSONParser();
        Map<String, Object> m = jp.parseJSON(i);
        return mapToObject(m, typeAttribute);
    }
    
    /**
     * Generates a JSON string from an object that must be either a Mappable object or an array of objects
     * @param o a Mappable object or array of objects
     * @return JSON String
     */
    public static String generateJSON(Object o, String typeAttribute) {
        StringBuilder bld = new StringBuilder();
        if(o instanceof Mappable) {
            writeObject(bld, (Mappable)o, typeAttribute);
        } else {
            writeArray(bld, (Object[])o, typeAttribute);
        }
        return bld.toString();
    }

    private static void appendObject(StringBuilder b, Object o, String typeAttribute) {
        if(Util.instanceofObjArray(o)) {
            writeArray(b, (Object[])o, typeAttribute);
        } else {
            if(o instanceof Mappable) {
                writeObject(b, (Mappable)o, typeAttribute);
            } else {
                if(o instanceof String) {
                    b.append('"');
                    jsonQuote(o, b);
                    b.append('"');
                } else {
                    b.append(o);
                }
            }
        }
    }
    
     private static void jsonQuote(Object str, StringBuilder sb) {
         if (str == null || !(str instanceof String) || ((String)str).length() == 0 ) {
             return;
         }

         String string = (String)str;
         char         b;
         char         c = 0;
         int          i;
         int          len = string.length();
         String       t;

         for (i = 0; i < len; i += 1) {
             b = c;
             c = string.charAt(i);
             switch (c) {
             case '\\':
             case '"':
                 sb.append('\\');
                 sb.append(c);
                 break;
             case '/':
                 if (b == '<') {
                     sb.append('\\');
                 }
                 sb.append(c);
                 break;
             case '\b':
                 sb.append("\\b");
                 break;
             case '\t':
                 sb.append("\\t");
                 break;
             case '\n':
                 sb.append("\\n");
                 break;
             case '\f':
                 sb.append("\\f");
                 break;
             case '\r':
                 sb.append("\\r");
                 break;
             default:
                 if (c < ' ') {
                     t = "000" + Integer.toHexString(c);
                     sb.append("\\u" + t.substring(t.length() - 4));
                 } else {
                     sb.append(c);
                 }
             }
         }
     }
    
    
    private static void writeArray(StringBuilder b, Object[] arr, String typeAttribute) {
        if(arr != null) {
            b.append('[');
            if(arr.length > 0) {
                appendObject(b, arr[0], typeAttribute);
                for(int iter = 1 ; iter < arr.length ; iter++) {
                    b.append(',');
                    appendObject(b, arr[iter], typeAttribute);
                }
            }
            b.append(']');        
        } else {
            b.append("null");
        }
    }
    
    private static void writeObject(StringBuilder b, Mappable obj, String typeAttribute) {
        b.append("{");
        boolean first = true;
        if(typeAttribute != null) {
            b.append("\"");
            b.append(typeAttribute);
            b.append("\":\"");
            b.append(obj.getObjectType());
            b.append("\"");
            first = false;
        }
        String[] arr = obj.getSupportedProperties();
        for(int iter = 0 ; iter < arr.length ; iter++) {
            if(!first) {
                b.append(',');
            }
            b.append('"');
            b.append(arr[iter]);
            b.append("\":");
            switch(obj.getPropertyType(iter)) {
                case Mappable.PROPERTY_TYPE_BINARY: 
                    byte[] bytes = (byte[])obj.getPropertyValue(iter);
                    b.append('"');
                    b.append(Base64.encodeNoNewline(bytes));
                    b.append('"');
                    break;
                case Mappable.PROPERTY_TYPE_ARRAY:
                    writeArray(b, (Object[])obj.getPropertyValue(iter), typeAttribute);
                    break;
                case Mappable.PROPERTY_TYPE_OBJECT:
                    writeObject(b, (Mappable)obj.getPropertyValue(iter), typeAttribute);
                    break;
                case Mappable.PROPERTY_TYPE_STRING:
                    b.append('"');
                    jsonQuote(obj.getPropertyValue(iter), b);
                    b.append('"');
                    break;
                default:
                    b.append(obj.getPropertyValue(iter));
                    break;
            }
        }
        b.append("}");
    }
    
    private static Object initClass(Map m, Class cls, String typeAttribute) {
        try {
            Mappable ma = (Mappable)cls.newInstance();
            String[] st = ma.getSupportedProperties();
            for(int iter = 0 ; iter < st.length ; iter++) {
                Object val = m.get(st[iter]);
                if(val != null) {
                    switch(ma.getPropertyType(iter)) {
                        case Mappable.PROPERTY_TYPE_BINARY: 
                            byte[] v = ((String)val).getBytes();
                            ma.setPropertyValue(iter, Base64.encodeNoNewline(v));
                            break;
                        case Mappable.PROPERTY_TYPE_ARRAY:
                            ma.setPropertyValue(iter, mapCollection((List)val, typeAttribute));
                            break;
                        case Mappable.PROPERTY_TYPE_OBJECT:
                            ma.setPropertyValue(iter, mapToObject((Map)val, typeAttribute));
                            break;
                        default:
                            ma.setPropertyValue(iter, val);
                            break;
                    }
                }
            }
            return ma;
        } catch(Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t.toString());
        }
    }
    
    /*private static Object initClass(Element m, Class cls) {
        try {
            Mappable ma = (Mappable)cls.newInstance();
            String[] st = ma.getSupportedProperties();
            for(int iter = 0 ; iter < st.length ; iter++) {
                Object val = m.getAttribute(st[iter]);
                if(val != null) {
                    switch(ma.getPropertyType(iter)) {
                        case Mappable.PROPERTY_TYPE_BINARY: 
                            byte[] v = ((String)val).getBytes();
                            ma.setPropertyValue(iter, Base64.encodeNoNewline(v));
                            break;
                        case Mappable.PROPERTY_TYPE_ARRAY:
                            ma.setPropertyValue(iter, mapCollection((List)val, typeAttribute));
                            break;
                        case Mappable.PROPERTY_TYPE_OBJECT:
                            ma.setPropertyValue(iter, mapToObject((Map)val, typeAttribute));
                            break;
                        default:
                            ma.setPropertyValue(iter, val);
                            break;
                    }
                }
            }
            return ma;
        } catch(Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }*/
    

    private static Object[] mapCollection(List val, String typeAttribute) {
        Object[] arr = new Object[val.size()];
        for(int iter = 0 ; iter < arr.length ; iter++) {
            arr[iter] = val.get(iter);
            if(arr[iter] instanceof List) {
                arr[iter] = mapCollection(val, typeAttribute);
                continue;
            }
            if(arr[iter] instanceof Map) {
                arr[iter] = mapToObject((Map)arr[iter], typeAttribute);
            }
        }
        return arr;
    }
    
    private static Object mapToObject(Map m, String typeAttribute) {
        if(typeAttribute != null) {
            Object val = m.get(typeAttribute);
            if(val != null) {
                Class cls = objectMap.get((String)val);
                if(cls != null) {
                    return initClass(m, cls, typeAttribute);
                }
            }
        }
        
        initObjectAttributes();
        // fallback to trying to detect the object automatically
        String best = null;
        int bestRank = -1;
        for(String current : objectToAttributes.keySet()) {
            String[] props = objectToAttributes.get(current);
            int rank = rank(props, m);
            if(rank > bestRank) {
                best = current;
                bestRank = rank;
            }
        }
        return initClass(m, objectMap.get(best), typeAttribute);
    }

    
    /*private static Object mapToObject(Element e) {
        Class cls = objectMap.get(e.getTagName());
        if(cls != null) {
            return initClass(m, cls, typeAttribute);
        }
        
        initObjectAttributes();
        // fallback to trying to detect the object automatically
        String best = null;
        int bestRank = -1;
        for(String current : objectToAttributes.keySet()) {
            String[] props = objectToAttributes.get(current);
            int rank = rank(props, e);
            if(rank > bestRank) {
                best = current;
                bestRank = rank;
            }
        }
        return initClass(m, objectMap.get(best), typeAttribute);
    }*/
    
    private static int rank(String[] properties, Map m) {
        int r = 0;
        for(String s : properties) {
            if(m.containsKey(s)) {
                r++;
            }
        }
        return r;
    }

    private static int rank(String[] properties, Element e) {
        int r = 0;
        for(String s : properties) {
            if(e.getAttribute(s) != null) {
                r++;
            }
        }
        return r;
    }
}
