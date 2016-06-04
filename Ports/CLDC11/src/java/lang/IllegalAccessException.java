/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package java.lang;
/**
 * Thrown when an application tries to load in a class, but the currently executing method does not have access to the definition of the specified class, because the class is not public and in another package.
 * An instance of this class can also be thrown when an application tries to create an instance of a class using the newInstance method in class Class, but the current method does not have access to the appropriate zero-argument constructor.
 * Since: JDK1.0, CLDC 1.0 See Also:Class.forName(java.lang.String), Class.newInstance()
 */
public class IllegalAccessException extends java.lang.Exception{
    /**
     * Constructs an IllegalAccessException without a detail message.
     */
    public IllegalAccessException(){
         //TODO codavaj!!
    }

    /**
     * Constructs an IllegalAccessException with a detail message.
     * s - the detail message.
     */
    public IllegalAccessException(java.lang.String s){
         //TODO codavaj!!
    }

}
