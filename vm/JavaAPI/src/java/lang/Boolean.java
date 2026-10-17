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
 * The Boolean class wraps a value of the primitive type boolean in an object. An object of type Boolean contains a single field whose type is boolean.
 * Since: JDK1.0, CLDC 1.0
 */
public final class Boolean implements Comparable<Boolean> {

    /**
     * The class object for the primitive type this class wraps.
     *
     * Null on every ParparVM target, and declared only because ASM's compiled
     * bytecode reads it: org.objectweb.asm.Type compares against Short.TYPE,
     * Float.TYPE and Boolean.TYPE, so the self-hosted translator does not link
     * without the three fields existing. ASM is a jar we cannot edit, which is
     * the one case where JavaAPI grows to meet a dependency rather than the
     * dependency being removed.
     *
     * It cannot be given a real value here. javac lowers a primitive class
     * literal to a read of the boxed type's own TYPE field, so the obvious
     * initializer compiles to "getstatic TYPE; putstatic TYPE" -- it reads the
     * field it is initializing and stores the null straight back. The six
     * wrappers that already declare TYPE are null for exactly that reason.
     * Giving all nine real values needs VM-side primitive class objects; that
     * work is not part of this change, and nothing in the translator depends on
     * it now that the C-type tables are keyed on the PrimitiveType enum.
     */
    public static final Class<Boolean> TYPE = null;

    /**
     * The Boolean object corresponding to the primitive value false.
     */
    public static final java.lang.Boolean FALSE = new Boolean(false);

    /**
     * The Boolean object corresponding to the primitive value true.
     */
    public static final java.lang.Boolean TRUE = new Boolean(true);

    private boolean value;
    
    /**
     * Allocates a Boolean object representing the value argument.
     * value - the value of the Boolean.
     */
    public Boolean(boolean value){
         this.value = value;
    }

    /**
     * Returns the value of this Boolean object as a boolean primitive.
     */
    public boolean booleanValue(){
        return value;
    }

    /**
     * Returns true if and only if the argument is not null and is a Boolean object that represents the same boolean value as this object.
     */
    public boolean equals(java.lang.Object obj){
        return obj != null && obj.getClass() == getClass() && ((Boolean)obj).value == value;
    }

    /**
     * Returns a hash code for this Boolean object.
     */
    public int hashCode(){
        return 0;
    }

    /**
     * Returns a String object representing this Boolean's value. If this object represents the value true, a string equal to "true" is returned. Otherwise, a string equal to "false" is returned.
     */
    public java.lang.String toString(){
        if(value) {
            return "true";
        }
        return "false"; 
    }

    /**
     * Returns the object instance of i
     * @param i the primitive
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
        if(b2.value == value) {
            return 0;
        }
        if(b2.value) {
            return -1;
        }
        return 1;
    }

    // ---------------------------------------------------------------------
    // Members of the JDK's class this one did not have, in plain Java.
    // ---------------------------------------------------------------------

    /// A Boolean of `parseBoolean(s)`.
    @Deprecated
    public Boolean(String s) {
        this(parseBoolean(s));
    }

    /// `"true"` or `"false"`.
    public static String toString(boolean b) {
        return b ? "true" : "false";
    }

    /// 1231 for true, 1237 for false, as the JDK hashes them.
    public static int hashCode(boolean value) {
        return value ? 1231 : 1237;
    }

    /// 0 when equal, otherwise positive when `x` is true.
    public static int compare(boolean x, boolean y) {
        return x == y ? 0 : (x ? 1 : -1);
    }

    /// `a && b`.
    public static boolean logicalAnd(boolean a, boolean b) {
        return a && b;
    }

    /// `a || b`.
    public static boolean logicalOr(boolean a, boolean b) {
        return a || b;
    }

    /// `a ^ b`.
    public static boolean logicalXor(boolean a, boolean b) {
        return a ^ b;
    }

    /// Whether the system property `name` is `"true"`, ignoring case.
    public static boolean getBoolean(String name) {
        if (name == null || name.length() == 0) {
            return false;
        }
        String value = System.getProperty(name);
        return value != null && value.equalsIgnoreCase("true");
    }
}
