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
public final class Short extends Number implements Comparable<Short> {

    /**
     * The class object for the primitive type this class wraps.
     */
    public static final Class<Short> TYPE = Class.getPrimitiveClass(Class.CN1_PRIM_SHORT);

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
        // instanceof rather than a null check plus getClass(): Short is final, so it is
        // exact, and it needs no header -- which is what a tagged immediate has none of.
        // The missing null guard here was a real defect: equals(null) is specified to return
        // false, and dereferencing obj was a hard SIGSEGV on targets that install no signal
        // handler. Every other wrapper in this package already guarded it.
        return (obj instanceof Short) && ((Short)obj).cn1Value() == cn1Value();
    }

    /**
     * Returns a hashcode for this Short.
     */
    public int hashCode(){
        return cn1Value();
    }

    /**
     * Returns this Short's value, transparently handling both heap-allocated and
     * tagged-immediate representations. All value reads route here -- Short is final, so a
     * plain `return value;` getter would be inlined into a raw field load off a tagged
     * pointer, which has no fields.
     */
    private native short cn1Value();

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
        return cn1Value();
    }

    /**
     * Returns a String object representing this Short's value.
     */
    public java.lang.String toString(){
        return Integer.toString(cn1Value());
    }

    /**
     * Returns the object instance of i
     * @param i the primitive
     * @return object instance
     */
    // Native so the tagged build can return an immediate without allocating. The off path
    // (and 32-bit-pointer targets) calls valueOfHeap, preserving the cache.
    public static native Short valueOf(short i);

    static Short valueOfHeap(short i) {
        if (i >= -128 && i <= 127) {
            return ShortCache.cache[i + 128];
        }
        return new Short(i);
    }

    /** Cache of boxed values for -128..127, mirroring the JDK's ShortCache. */
    private static final class ShortCache {
        static final Short[] cache = new Short[256];
        static {
            for (int j = 0; j < 256; j++) {
                cache[j] = new Short((short) (j - 128));
            }
        }
        private ShortCache() {}
    }

    @Override
    public int intValue() {
        return cn1Value();
    }

    @Override
    public long longValue() {
        return cn1Value();
    }

    @Override
    public float floatValue() {
        return cn1Value();
    }

    @Override
    public double doubleValue() {
        return cn1Value();
    }

    public static int compare(short f1, short f2) {
        return f1 - f2;
    }

    public int compareTo(Short another) {
        // The JDK specifies Short.compareTo as Short.compare(a, b), and Short.compare is the
        // DIFFERENCE, not the sign -- unlike Integer.compare and Long.compare, which really
        // do return -1/0/1. This returned the sign, so it disagreed with the JDK and with
        // this class's own compare(short, short) directly above.
        return cn1Value() - another.cn1Value();
    }
}
