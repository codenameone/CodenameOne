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

/**
 * Allows mapping an object to JSON/XML for object serialization. Every object that implements 
 * the mappable interface and is registered can be converted back and forth from XML/JSON respectively.
 *
 * @author Shai Almog
 */
interface Mappable {
    /**
     * Property type for getPropertyType
     */
    public static final int PROPERTY_TYPE_STRING = 1;

    /**
     * Property type for getPropertyType
     */
    public static final int PROPERTY_TYPE_INT = 2;

    /**
     * Property type for getPropertyType
     */
    public static final int PROPERTY_TYPE_LONG = 3;

    /**
     * Property type for getPropertyType
     */
    public static final int PROPERTY_TYPE_BOOLEAN = 4;

    /**
     * Property type for getPropertyType
     */
    public static final int PROPERTY_TYPE_DOUBLE = 5;

    /**
     * Property type for getPropertyType
     */
    public static final int PROPERTY_TYPE_FLOAT = 6;

    /**
     * Property type for getPropertyType
     */
    public static final int PROPERTY_TYPE_CHAR = 7;

    /**
     * Property type for getPropertyType
     */
    public static final int PROPERTY_TYPE_BYTE = 8;

    /**
     * Property type for getPropertyType
     */
    public static final int PROPERTY_TYPE_SHORT = 9;

    /**
     * Property type for getPropertyType, maps to a binary byte array
     */
    public static final int PROPERTY_TYPE_BINARY = 10;

    /**
     * Property type for getPropertyType, maps to an array of objects
     */
    public static final int PROPERTY_TYPE_ARRAY = 100;

    /**
     * Property type for getPropertyType, maps to a Map interface
     */
    public static final int PROPERTY_TYPE_MAP = 200;

    /**
     * Property type for getPropertyType, maps to another mappable object
     */
    public static final int PROPERTY_TYPE_OBJECT = 300;
    
    /**
     * Returns an array of the properties supported by this class e.g. for a class with 
     * getX, setX, getY, setY an array of {"x", "y"} would be returned.
     * @return array of properties
     */
    public String[] getSupportedProperties();
    
    /**
     * Returns the type of the property one of the above mentioned types
     * @param offset the offset of the property within the supported properties array.
     * @return one of the PROPERTY_TYPE_* values
     */
    public int getPropertyType(int offset);
    
    /**
     * Returns the property value in the given offset based on the offsets of getSupportedProperties
     * @param property the property
     * @return the value of the property
     */
    public Object getPropertyValue(int property);
    
    /**
     * Sets the property value in the given offset based on the offsets of getSupportedProperties
     * 
     * @param property the property
     * @param value the new value for the property
     */
    public void setPropertyValue(int property, Object value);
    
    /**
     * Returns the type used to identify the object when internalizing/externalizing the object
     * @return type name, this often maps to XML tag or JSON type attribute
     */
    public String getObjectType();
}
