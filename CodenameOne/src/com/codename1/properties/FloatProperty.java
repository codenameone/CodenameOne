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

/**
 * This is the float specific version of numeric property
 *
 * @author Shai Almog
 */
public class FloatProperty<K> extends NumericProperty<Float, K>{

    /**
     * {@inheritDoc}
     */
    public FloatProperty(String name) {
        super(name);
    }

    /**
     * {@inheritDoc}
     */
    public FloatProperty(String name, Float value) {
        super(name, value);
    }    

    
    /**
     * Returns the value as a primitive, if the value is null/nullable this will fail...
     * @return the numeric value
     */
    public float getFloat() {
        return get().floatValue();
    }
}
