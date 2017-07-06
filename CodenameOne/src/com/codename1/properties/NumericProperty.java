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
 * This is the base class to all number properties, it introduces nullability and the ability to convert to all 
 * number types. 
 * 
 *
 * @author Shai Almog
 */
public abstract class NumericProperty<T, K> extends Property<T, K> {
    private boolean nullable;
    
    /**
     * {@inheritDoc}
     */
    public NumericProperty(String name) {
        super(name);
        nullable = true;
    }

    /**
     * {@inheritDoc}
     */
    public NumericProperty(String name, Class genericType) {
        super(name, genericType);
        nullable = true;
    }
    
    /**
     * {@inheritDoc}
     */
    public NumericProperty(String name, T value) {
        super(name, value);
        nullable = value == null;
    }

    /**
     * {@inheritDoc}
     */
    public NumericProperty(String name, Class genericType, T value) {
        super(name, genericType, value);
        nullable = value == null;
    }

    /**
     * If the field is nullable {@code set(null)} will fail
     * @return the nullable
     */
    public boolean isNullable() {
        return nullable;
    }

    /**
     * If the field is nullable {@code set(null)} will fail
     * @param nullable the nullable to set
     */
    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    @Override
    public K set(T value) {
        if(nullable && value == null) {
            throw new NullPointerException(getName() + " can't be null");
        }
        return super.set(value);
    }

    
}
