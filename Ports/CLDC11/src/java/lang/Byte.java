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
 * The Byte class is the standard wrapper for byte values.
 * Since: JDK1.1, CLDC 1.0
 */
public final class Byte{
    /**
     * The maximum value a Byte can have.
     * See Also:Constant Field Values
     */
    public static final byte MAX_VALUE=127;

    /**
     * The minimum value a Byte can have.
     * See Also:Constant Field Values
     */
    public static final byte MIN_VALUE=-128;

    /**
     * Constructs a Byte object initialized to the specified byte value.
     * value - the initial value of the Byte
     */
    public Byte(byte value){
         //TODO codavaj!!
    }

    /**
     * Returns the value of this Byte as a byte.
     */
    public byte byteValue(){
        return 0; //TODO codavaj!!
    }

    /**
     * Compares this object to the specified object.
     */
    public boolean equals(java.lang.Object obj){
        return false; //TODO codavaj!!
    }

    /**
     * Returns a hashcode for this Byte.
     */
    public int hashCode(){
        return 0; //TODO codavaj!!
    }

    /**
     * Assuming the specified String represents a byte, returns that byte's value. Throws an exception if the String cannot be parsed as a byte. The radix is assumed to be 10.
     */
    public static byte parseByte(java.lang.String s) throws java.lang.NumberFormatException{
        return 0; //TODO codavaj!!
    }

    /**
     * Assuming the specified String represents a byte, returns that byte's value. Throws an exception if the String cannot be parsed as a byte.
     */
    public static byte parseByte(java.lang.String s, int radix) throws java.lang.NumberFormatException{
        return 0; //TODO codavaj!!
    }

    /**
     * Returns a String object representing this Byte's value.
     */
    public java.lang.String toString(){
        return null; //TODO codavaj!!
    }

    /**
     * Returns the object instance of i
     * @param i the primitive
     * @return object instance
     */
    public static Byte valueOf(byte i) {
        return null;
    }
}
