/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package com.codename1.impl;

import com.codename1.impl.blackberry.BlackBerryImplementation;

/**
 * Generic class allowing 3rd parties to replace the underlying implementation in
 * LWUIT seamlessly. The factory can be replaced by 3rd parties to install a new
 * underlying implementation using elaborate logic. 
 *
 * @author Shai Almog, Thorsten Schemm
 */
public class ImplementationFactory {
    private static ImplementationFactory instance = new ImplementationFactory();
    
    /**
     * Allows third parties to replace the implementation factory
     */
    protected ImplementationFactory() {
    }
    
    /**
     * Returns the singleton instance of this class
     * 
     * @return instanceof Implementation factory
     */
    public static ImplementationFactory getInstance() {
        return instance;
    }
    
    /**
     * Install a new implementation factory this method is invoked by implementors
     * to replace a factory.
     * 
     * @param i implementation factory instance
     */
    public static void setInstance(ImplementationFactory i) {
        instance = i;
    }
    
    /**
     * Factory method to create the implementation instance
     * 
     * @return a newly created implementation instance
     */
    public Object createImplementation() {
        return new BlackBerryImplementation();
    }
}
