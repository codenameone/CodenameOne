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
 *
 * This code was adapted from TeaVM's class library. 
 */

package java.util;


/**
 * This is a compatibility class which supports the java.util.Objects API.  On platforms that don't support this class (e.g. Android)
 * the build server will automatically remap all uses of java.util.Objects to use this implementation instead.
 * 
 * This class consists of static utility methods for operating on objects. These utilities include null-safe or null-tolerant methods for computing the hash code of an object, returning a string for an object, and comparing two objects.
 * @author shannah
 */
public final class Objects {
    /**
     * Returns true if the arguments are equal to each other and false otherwise. Consequently, if both arguments are null, true is returned and if exactly one argument is null, false is returned. Otherwise, equality is determined by using the equals method of the first argument.
     * @param a
     * @param b
     * @return 
     */
    public static boolean equals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        return a == null ? b == null : a.equals(b);
    }

    /**
     * Returns the hash code of a non-null argument and 0 for a null argument.
     * @param o
     * @return 
     */
    public static int hashCode(Object o) {
        return o == null ? 0 : o.hashCode();
    }

    public static String toString(Object o) {
        return toString(o, "null");
    }

    public static String toString(Object o, String nullDefault) {
        return o != null ? o.toString() : nullDefault;
    }

    public static <T> int compare(T a, T b, Comparator<? super T> c) {
        return a == null && b == null ? 0 : c.compare(a, b);
    }

    public static <T> T requireNonNull(T obj) {
        return requireNonNull(obj, "");
    }

    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null) {
            throw new NullPointerException(message);
        }
        return obj;
    }

    public static boolean nonNull(Object obj) {
        return obj != null;
    }

    public static boolean deepEquals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null) {
            return b == null;
        }
        if (a instanceof boolean[]) {
            return b instanceof boolean[] && Arrays.equals((boolean[]) a, (boolean[]) b);
        } else if (b instanceof boolean[]) {
            return false;
        } else if (a instanceof byte[]) {
            return b instanceof byte[] && Arrays.equals((byte[]) a, (byte[]) b);
        } else if (b instanceof byte[]) {
            return false;
        } else if (a instanceof short[]) {
            return b instanceof short[] && Arrays.equals((short[]) a, (short[]) b);
        } else if (b instanceof short[]) {
            return false;
        } else if (a instanceof int[]) {
            return b instanceof int[] && Arrays.equals((int[]) a, (int[]) b);
        } else if (b instanceof int[]) {
            return false;
        } else if (a instanceof char[]) {
            return b instanceof char[] && Arrays.equals((char[]) a, (char[]) b);
        } else if (b instanceof char[]) {
            return false;
        } else if (a instanceof float[]) {
            return b instanceof float[] && Arrays.equals((float[]) a, (float[]) b);
        } else if (b instanceof float[]) {
            return false;
        } else if (a instanceof double[]) {
            return b instanceof double[] && Arrays.equals((double[]) a, (double[]) b);
        } else if (b instanceof double[]) {
            return false;
        } else if (a instanceof Object[]) {
            return b instanceof Object[] && Arrays.deepEquals((Object[]) a, (Object[]) b);
        } else if (b instanceof Object[]) {
            return false;
        } else {
            return a.equals(b);
        }
    }

    public static int hash(Object... values) {
        return Arrays.hashCode(values);
    }
}

