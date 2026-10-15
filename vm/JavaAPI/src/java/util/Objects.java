/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package java.util;

/**
 * Utility methods for objects.
 */
public final class Objects {
    private Objects() {
        throw new AssertionError("No java.util.Objects instances for you!");
    }

    public static <T> T requireNonNull(T obj) {
        if (obj == null)
            throw new NullPointerException();
        return obj;
    }

    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null)
            throw new NullPointerException(message);
        return obj;
    }

    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    public static boolean deepEquals(Object a, Object b) {
        if (a == b) {
            return true;
        } else if (a == null || b == null) {
            return false;
        }
        if (a instanceof Object[] && b instanceof Object[]) {
            return Arrays.deepEquals((Object[]) a, (Object[]) b);
        } else if (a instanceof byte[] && b instanceof byte[]) {
            return Arrays.equals((byte[]) a, (byte[]) b);
        } else if (a instanceof short[] && b instanceof short[]) {
            return Arrays.equals((short[]) a, (short[]) b);
        } else if (a instanceof int[] && b instanceof int[]) {
            return Arrays.equals((int[]) a, (int[]) b);
        } else if (a instanceof long[] && b instanceof long[]) {
            return Arrays.equals((long[]) a, (long[]) b);
        } else if (a instanceof char[] && b instanceof char[]) {
            return Arrays.equals((char[]) a, (char[]) b);
        } else if (a instanceof float[] && b instanceof float[]) {
            return Arrays.equals((float[]) a, (float[]) b);
        } else if (a instanceof double[] && b instanceof double[]) {
            return Arrays.equals((double[]) a, (double[]) b);
        } else if (a instanceof boolean[] && b instanceof boolean[]) {
            return Arrays.equals((boolean[]) a, (boolean[]) b);
        }
        return a.equals(b);
    }

    public static int hashCode(Object o) {
        return o != null ? o.hashCode() : 0;
    }

    public static String toString(Object o) {
        return String.valueOf(o);
    }

    public static String toString(Object o, String nullDefault) {
        return (o != null) ? o.toString() : nullDefault;
    }

    public static int compare(Object a, Object b, Comparator c) {
        return (a == b) ? 0 : c.compare(a, b);
    }

    public static boolean isNull(Object obj) {
        return obj == null;
    }

    public static boolean nonNull(Object obj) {
        return obj != null;
    }

    public static int hash(Object... values) {
        if (values == null) {
            return 0;
        }
        int hashCode = 1;
        for (Object element : values) {
            hashCode = 31 * hashCode + element.hashCode();
        }
        return hashCode;
    }

    // ---------------------------------------------------------------------
    // Members of the JDK's class this one did not have, in plain Java.
    // ---------------------------------------------------------------------

    /// `obj`, or a NullPointerException whose message `messageSupplier`
    /// makes, only when it is needed.
    public static <T> T requireNonNull(T obj, java.util.function.Supplier<String> messageSupplier) {
        if (obj == null) {
            throw new NullPointerException(messageSupplier == null ? null : messageSupplier.get());
        }
        return obj;
    }

    /// `obj`, or `defaultObj` when `obj` is null.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if both are null
    public static <T> T requireNonNullElse(T obj, T defaultObj) {
        if (obj != null) {
            return obj;
        }
        if (defaultObj == null) {
            throw new NullPointerException("defaultObj");
        }
        return defaultObj;
    }

    /// `obj`, or what `supplier` gives when `obj` is null.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `obj` is null and the supplier is null or gives null
    public static <T> T requireNonNullElseGet(T obj, java.util.function.Supplier<? extends T> supplier) {
        if (obj != null) {
            return obj;
        }
        if (supplier == null) {
            throw new NullPointerException("supplier");
        }
        T value = supplier.get();
        if (value == null) {
            throw new NullPointerException("supplier.get()");
        }
        return value;
    }

    /// `index`, if it lies in `[0, length)`.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: otherwise
    public static int checkIndex(int index, int length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + length);
        }
        return index;
    }

    /// `index`, if it lies in `[0, length)`.
    public static long checkIndex(long index, long length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + length);
        }
        return index;
    }

    /// `fromIndex`, if `[fromIndex, toIndex)` lies in `[0, length)`.
    public static int checkFromToIndex(int fromIndex, int toIndex, int length) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + toIndex
                    + ") out of bounds for length " + length);
        }
        return fromIndex;
    }

    /// `fromIndex`, if `[fromIndex, toIndex)` lies in `[0, length)`.
    public static long checkFromToIndex(long fromIndex, long toIndex, long length) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + toIndex
                    + ") out of bounds for length " + length);
        }
        return fromIndex;
    }

    /// `fromIndex`, if `[fromIndex, fromIndex + size)` lies in `[0, length)`.
    public static int checkFromIndexSize(int fromIndex, int size, int length) {
        if ((length | fromIndex | size) < 0 || size > length - fromIndex) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + fromIndex + " + " + size
                    + ") out of bounds for length " + length);
        }
        return fromIndex;
    }

    /// `fromIndex`, if `[fromIndex, fromIndex + size)` lies in `[0, length)`.
    public static long checkFromIndexSize(long fromIndex, long size, long length) {
        if ((length | fromIndex | size) < 0 || size > length - fromIndex) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + fromIndex + " + " + size
                    + ") out of bounds for length " + length);
        }
        return fromIndex;
    }

    /// What `Object.toString` would answer for `o`, even when its class
    /// overrides it.
    public static String toIdentityString(Object o) {
        return o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }
}
