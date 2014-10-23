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

package java.lang;
/**
 * The Short class is the standard wrapper for short values.
 * Since: JDK1.1, CLDC 1.0
 */
public final class Short{
    /**
     * The maximum value a Short can have.
     * See Also:Constant Field Values
     */
    public static final short MAX_VALUE=32767;

    /**
     * The minimum value a Short can have.
     * See Also:Constant Field Values
     */
    public static final short MIN_VALUE=-32768;

    private short value;
    
    /**
     * Constructs a Short object initialized to the specified short value.
     * value - the initial value of the Short
     */
    public Short(short value){
         this.value = value;
    }

    /**
     * Compares this object to the specified object.
     */
    public boolean equals(java.lang.Object obj){
        return obj.getClass() == getClass() && ((Short)obj).value == value;
    }

    /**
     * Returns a hashcode for this Short.
     */
    public int hashCode(){
        return value;
    }

    /**
     * Assuming the specified String represents a short, returns that short's value. Throws an exception if the String cannot be parsed as a short. The radix is assumed to be 10.
     */
    public static short parseShort(java.lang.String s) throws java.lang.NumberFormatException{
        return (short)Integer.parseInt(s);
    }

    /**
     * Assuming the specified String represents a short, returns that short's value in the radix specified by the second argument. Throws an exception if the String cannot be parsed as a short.
     */
    public static short parseShort(java.lang.String s, int radix) throws java.lang.NumberFormatException{
        return (short)Integer.parseInt(s, radix);
    }

    /**
     * Returns the value of this Short as a short.
     */
    public short shortValue(){
        return value; //TODO codavaj!!
    }

    /**
     * Returns a String object representing this Short's value.
     */
    public java.lang.String toString(){
        return Integer.toString(value);
    }

    /**
     * Returns the object instance of i
     * @param i the primitive
     * @return object instance
     */
    public static Short valueOf(short i) {
        return new Short(i);
    }
}
