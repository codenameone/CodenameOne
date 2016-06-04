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
 * The Boolean class wraps a value of the primitive type boolean in an object. An object of type Boolean contains a single field whose type is boolean.
 * Since: JDK1.0, CLDC 1.0
 */
public final class Boolean{
    /**
     * The Boolean object corresponding to the primitive value false.
     */
    public static final java.lang.Boolean FALSE=null;

    /**
     * The Boolean object corresponding to the primitive value true.
     */
    public static final java.lang.Boolean TRUE=null;

    /**
     * Allocates a Boolean object representing the value argument.
     * value - the value of the Boolean.
     */
    public Boolean(boolean value){
         //TODO codavaj!!
    }

    /**
     * Returns the value of this Boolean object as a boolean primitive.
     */
    public boolean booleanValue(){
        return false; //TODO codavaj!!
    }

    /**
     * Returns true if and only if the argument is not null and is a Boolean object that represents the same boolean value as this object.
     */
    public boolean equals(java.lang.Object obj){
        return false; //TODO codavaj!!
    }

    /**
     * Returns a hash code for this Boolean object.
     */
    public int hashCode(){
        return 0; //TODO codavaj!!
    }

    /**
     * Returns a String object representing this Boolean's value. If this object represents the value true, a string equal to "true" is returned. Otherwise, a string equal to "false" is returned.
     */
    public java.lang.String toString(){
        return null; //TODO codavaj!!
    }

    /**
     * Returns the object instance of i
     * @param b the primitive
     * @return object instance
     */
    public static Boolean valueOf(final boolean b) {
            return b ? Boolean.TRUE : Boolean.FALSE;
    }

    public static Boolean valueOf(final String b) {
            return valueOf(parseBoolean(b));
    }

    public static boolean parseBoolean(final String s) {
            return (s != null) && s.equalsIgnoreCase("true");
    }

    public int compareTo(final Boolean b2) {
            return 0;
    }
}
