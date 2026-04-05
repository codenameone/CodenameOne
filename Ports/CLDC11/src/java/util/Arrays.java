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

import java.io.Serializable;
import java.lang.reflect.Array;

/// `Arrays` contains static methods which operate on arrays.
///
/// #### Since
///
/// 1.2
public class Arrays {

    /* Specifies when to switch to insertion sort */
    private static final int SIMPLE_LENGTH = 7;

    private static class ArrayList<E> extends AbstractList<E> implements
            List<E>, RandomAccess {


        private final E[] a;

        ArrayList(E[] storage) {
            if (storage == null) {
                throw new NullPointerException();
            }
            a = storage;
        }

        @Override
        public boolean contains(Object object) {
            if (object != null) {
                for (E element : a) {
                    if (object.equals(element)) {
                        return true;
                    }
                }
            } else {
                for (E element : a) {
                    if (element == null) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public E get(int location) {
            try {
                return a[location];
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public int indexOf(Object object) {
            if (object != null) {
                for (int i = 0; i < a.length; i++) {
                    if (object.equals(a[i])) {
                        return i;
                    }
                }
            } else {
                for (int i = 0; i < a.length; i++) {
                    if (a[i] == null) {
                        return i;
                    }
                }
            }
            return -1;
        }

        @Override
        public int lastIndexOf(Object object) {
            if (object != null) {
                for (int i = a.length - 1; i >= 0; i--) {
                    if (object.equals(a[i])) {
                        return i;
                    }
                }
            } else {
                for (int i = a.length - 1; i >= 0; i--) {
                    if (a[i] == null) {
                        return i;
                    }
                }
            }
            return -1;
        }

        @Override
        public E set(int location, E object) {
            try {
                E result = a[location];
                a[location] = object;
                return result;
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IndexOutOfBoundsException();
            } catch (ArrayStoreException e) {
                throw new ClassCastException();
            }
        }

        @Override
        public int size() {
            return a.length;
        }
    }

    private Arrays() {
        /* empty */
    }

    /// Returns a `List` of the objects in the specified array. The size of the
    /// `List` cannot be modified, i.e. adding and removing are unsupported, but
    /// the elements can be set. Setting an element modifies the underlying
    /// array.
    ///
    /// #### Parameters
    ///
    /// - `array`: the array.
    ///
    /// #### Returns
    ///
    /// a `List` of the elements of the specified array.
    public static <T> List<T> asList(T... array) {
        return new ArrayList<T>(array);
    }

    /// Performs a binary search for the specified element in the specified
    /// ascending sorted array. Searching in an unsorted array has an undefined
    /// result. It's also undefined which element is found if there are multiple
    /// occurrences of the same element.
    ///
    /// #### Parameters
    ///
    /// - `array`: the sorted `byte` array to search.
    ///
    /// - `value`: the `byte` element to find.
    ///
    /// #### Returns
    ///
    /// @return the non-negative index of the element, or a negative index which
    /// is `-index - 1` where the element would be inserted.
    public static int binarySearch(byte[] array, byte value) {
        return binarySearch(array, 0, array.length, value);
    }

    /// Performs a binary search for the specified element in the specified
    /// sorted array.
    ///
    /// #### Parameters
    ///
    /// - `array`: the sorted char array to search
    ///
    /// - `value`: the char element to find
    ///
    /// #### Returns
    ///
    /// @return the non-negative index of the element, or a negative index which
    /// is the -index - 1 where the element would be inserted
    public static int binarySearch(char[] array, char value) {
        return binarySearch(array, 0, array.length, value);
    }

    /// Performs a binary search for the specified element in the specified
    /// sorted array.
    ///
    /// #### Parameters
    ///
    /// - `array`: the sorted double array to search
    ///
    /// - `value`: the double element to find
    ///
    /// #### Returns
    ///
    /// @return the non-negative index of the element, or a negative index which
    /// is the -index - 1 where the element would be inserted
    public static int binarySearch(double[] array, double value) {
        return binarySearch(array, 0, array.length, value);
    }

    /// Performs a binary search for the specified element in the specified
    /// sorted array.
    ///
    /// #### Parameters
    ///
    /// - `array`: the sorted float array to search
    ///
    /// - `value`: the float element to find
    ///
    /// #### Returns
    ///
    /// @return the non-negative index of the element, or a negative index which
    /// is the -index - 1 where the element would be inserted
    public static int binarySearch(float[] array, float value) {
        return binarySearch(array, 0, array.length, value);
    }

    /// Performs a binary search for the specified element in the specified
    /// sorted array.
    ///
    /// #### Parameters
    ///
    /// - `array`: the sorted int array to search
    ///
    /// - `value`: the int element to find
    ///
    /// #### Returns
    ///
    /// @return the non-negative index of the element, or a negative index which
    /// is the -index - 1 where the element would be inserted
    public static int binarySearch(int[] array, int value) {
        return binarySearch(array, 0, array.length, value);
    }

    /// Performs a binary search for the specified element in the specified
    /// sorted array.
    ///
    /// #### Parameters
    ///
    /// - `array`: the sorted long array to search
    ///
    /// - `value`: the long element to find
    ///
    /// #### Returns
    ///
    /// @return the non-negative index of the element, or a negative index which
    /// is the -index - 1 where the element would be inserted
    public static int binarySearch(long[] array, long value) {
        return binarySearch(array, 0, array.length, value);
    }

    /// Performs a binary search for the specified element in the specified
    /// sorted array.
    ///
    /// #### Parameters
    ///
    /// - `array`: the sorted Object array to search
    ///
    /// - `object`: the Object element to find
    ///
    /// #### Returns
    ///
    /// @return the non-negative index of the element, or a negative index which
    /// is the -index - 1 where the element would be inserted
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @exception ClassCastException
    /// when an element in the array or the search element does
    /// not implement Comparable, or cannot be compared to each
    /// other
    @SuppressWarnings("unchecked")
    public static int binarySearch(Object[] array, Object object) {
        return binarySearch(array, 0, array.length, object);
    }

    /// Performs a binary search for the specified element in the specified
    /// sorted array using the Comparator to compare elements.
    ///
    /// @param
    /// type of object
    ///
    /// #### Parameters
    ///
    /// - `array`: the sorted Object array to search
    ///
    /// - `object`: the char element to find
    ///
    /// - `comparator`: the Comparator
    ///
    /// #### Returns
    ///
    /// @return the non-negative index of the element, or a negative index which
    /// is the -index - 1 where the element would be inserted
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @exception ClassCastException
    /// when an element in the array and the search element cannot
    /// be compared to each other using the Comparator
    public static <T> int binarySearch(T[] array, T object,
            Comparator<? super T> comparator) {
        return binarySearch(array, 0, array.length, object, comparator);
    }

    /// Performs a binary search for the specified element in the specified
    /// sorted array.
    ///
    /// #### Parameters
    ///
    /// - `array`: the sorted short array to search
    ///
    /// - `value`: the short element to find
    ///
    /// #### Returns
    ///
    /// @return the non-negative index of the element, or a negative index which
    /// is the -index - 1 where the element would be inserted
    public static int binarySearch(short[] array, short value) {
        return binarySearch(array, 0, array.length, value);
    }

    /// Performs a binary search for the specified element in a part of the
    /// specified sorted array.
    ///
    /// #### Parameters
    ///
    /// - `array`: the sorted byte array to search
    ///
    /// - `startIndex`: the inclusive start index
    ///
    /// - `endIndex`: the exclusive end index
    ///
    /// - `value`: the byte element to find
    ///
    /// #### Returns
    ///
    /// @return the non-negative index of the element, or a negative index which
    /// is the -index - 1 where the element would be inserted
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: @throws IllegalArgumentException -
    /// if startIndex is bigger than endIndex
    ///
    /// - `ArrayIndexOutOfBoundsException`: @throws ArrayIndexOutOfBoundsException -
    /// if startIndex is smaller than zero or or endIndex is bigger
    /// than length of array
    ///
    /// #### Since
    ///
    /// 1.6
    public static int binarySearch(byte[] array, int startIndex, int endIndex,
            byte value) {
        checkIndexForBinarySearch(array.length, startIndex, endIndex);
        int low = startIndex, mid = -1, high = endIndex - 1;
        while (low <= high) {
            mid = (low + high) >>> 1;
            if (value > array[mid]) {
                low = mid + 1;
            } else if (value == array[mid]) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        if (mid < 0) {
            int insertPoint = endIndex;
            for (int index = startIndex; index < endIndex; index++) {
                if (value < array[index]) {
                    insertPoint = index;
                }
            }
            return -insertPoint - 1;
        }
        return -mid - (value < array[mid] ? 1 : 2);
    }

    /// Performs a binary search for the specified element in a part of the
    /// specified sorted array.
    ///
    /// #### Parameters
    ///
    /// - `array`: the sorted char array to search
    ///
    /// - `startIndex`: the inclusive start index
    ///
    /// - `endIndex`: the exclusive end index
    ///
    /// - `value`: the char element to find
    ///
    /// #### Returns
    ///
    /// @return the non-negative index of the element, or a negative index which
    /// is the -index - 1 where the element would be inserted
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: @throws IllegalArgumentException -
    /// if startIndex is bigger than endIndex
    ///
    /// - `ArrayIndexOutOfBoundsException`: @throws ArrayIndexOutOfBoundsException -
    /// if startIndex is smaller than zero or or endIndex is bigger
    /// than length of array
    ///
    /// #### Since
    ///
    /// 1.6
    public static int binarySearch(char[] array, int startIndex, int endIndex,
            char value) {
        checkIndexForBinarySearch(array.length, startIndex, endIndex);
        int low = startIndex, mid = -1, high = endIndex - 1;
        while (low <= high) {
            mid = (low + high) >>> 1;
            if (value > array[mid]) {
                low = mid + 1;
            } else if (value == array[mid]) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        if (mid < 0) {
            int insertPoint = endIndex;
            for (int index = startIndex; index < endIndex; index++) {
                if (value < array[index]) {
                    insertPoint = index;
                }
            }
            return -insertPoint - 1;
        }
        return -mid - (value < array[mid] ? 1 : 2);
    }

    /// Performs a binary search for the specified element in a part of the
    /// specified sorted array.
    ///
    /// #### Parameters
    ///
    /// - `array`: the sorted double array to search
    ///
    /// - `startIndex`: the inclusive start index
    ///
    /// - `endIndex`: the exclusive end index
    ///
    /// - `value`: the double element to find
    ///
    /// #### Returns
    ///
    /// @return the non-negative index of the element, or a negative index which
    /// is the -index - 1 where the element would be inserted
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: @throws IllegalArgumentException -
    /// if startIndex is bigger than endIndex
    ///
    /// - `ArrayIndexOutOfBoundsException`: @throws ArrayIndexOutOfBoundsException -
    /// if startIndex is smaller than zero or or endIndex is bigger
    /// than length of array
    ///
    /// #### Since
    ///
    /// 1.6
    public static int binarySearch(double[] array, int startIndex,
            int endIndex, double value) {
        checkIndexForBinarySearch(array.length, startIndex, endIndex);
        long longBits = Double.doubleToLongBits(value);
        int low = startIndex, mid = -1, high = endIndex - 1;
        while (low <= high) {
            mid = (low + high) >>> 1;
            if (lessThan(array[mid], value)) {
                low = mid + 1;
            } else if (longBits == Double.doubleToLongBits(array[mid])) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        if (mid < 0) {
            int insertPoint = endIndex;
            for (int index = startIndex; index < endIndex; index++) {
                if (value < array[index]) {
                    insertPoint = index;
                }
            }
            return -insertPoint - 1;
        }
        return -mid - (lessThan(value, array[mid]) ? 1 : 2);
    }

    /// Performs a binary search for the specified element in a part of the
    /// specified sorted array.
    ///
    /// #### Parameters
    ///
    /// - `array`: the sorted float array to search
    ///
    /// - `startIndex`: the inclusive start index
    ///
    /// - `endIndex`: the exclusive end index
    ///
    /// - `value`: the float element to find
    ///
    /// #### Returns
    ///
    /// @return the non-negative index of the element, or a negative index which
    /// is the -index - 1 where the element would be inserted
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: @throws IllegalArgumentException -
    /// if startIndex is bigger than endIndex
    ///
    /// - `ArrayIndexOutOfBoundsException`: @throws ArrayIndexOutOfBoundsException -
    /// if startIndex is smaller than zero or or endIndex is bigger
    /// than length of array
    ///
    /// #### Since
    ///
    /// 1.6
    public static int binarySearch(float[] array, int startIndex, int endIndex,
            float value) {
        checkIndexForBinarySearch(array.length, startIndex, endIndex);
        int intBits = Float.floatToIntBits(value);
        int low = startIndex, mid = -1, high = endIndex - 1;
        while (low <= high) {
            mid = (low + high) >>> 1;
            if (lessThan(array[mid], value)) {
                low = mid + 1;
            } else if (intBits == Float.floatToIntBits(array[mid])) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        if (mid < 0) {
            int insertPoint = endIndex;
            for (int index = startIndex; index < endIndex; index++) {
                if (value < array[index]) {
                    insertPoint = index;
                }
            }
            return -insertPoint - 1;
        }
        return -mid - (lessThan(value, array[mid]) ? 1 : 2);
    }

    /// Performs a binary search for the specified element in a part of the
    /// specified sorted array.
    ///
    /// #### Parameters
    ///
    /// - `array`: the sorted int array to search
    ///
    /// - `startIndex`: the inclusive start index
    ///
    /// - `endIndex`: the exclusive end index
    ///
    /// - `value`: the int element to find
    ///
    /// #### Returns
    ///
    /// @return the non-negative index of the element, or a negative index which
    /// is the -index - 1 where the element would be inserted
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: @throws IllegalArgumentException -
    /// if startIndex is bigger than endIndex
    ///
    /// - `ArrayIndexOutOfBoundsException`: @throws ArrayIndexOutOfBoundsException -
    /// if startIndex is smaller than zero or or endIndex is bigger
    /// than length of array
    ///
    /// #### Since
    ///
    /// 1.6
    public static int binarySearch(int[] array, int startIndex, int endIndex,
            int value) {
        checkIndexForBinarySearch(array.length, startIndex, endIndex);
        int low = startIndex, mid = -1, high = endIndex - 1;
        while (low <= high) {
            mid = (low + high) >>> 1;
            if (value > array[mid]) {
                low = mid + 1;
            } else if (value == array[mid]) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        if (mid < 0) {
            int insertPoint = endIndex;
            for (int index = startIndex; index < endIndex; index++) {
                if (value < array[index]) {
                    insertPoint = index;
                }
            }
            return -insertPoint - 1;
        }
        return -mid - (value < array[mid] ? 1 : 2);
    }

    /// Performs a binary search for the specified element in a part of the
    /// specified sorted array.
    ///
    /// #### Parameters
    ///
    /// - `array`: the sorted long array to search
    ///
    /// - `startIndex`: the inclusive start index
    ///
    /// - `endIndex`: the exclusive end index
    ///
    /// - `value`: the long element to find
    ///
    /// #### Returns
    ///
    /// @return the non-negative index of the element, or a negative index which
    /// is the -index - 1 where the element would be inserted
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: @throws IllegalArgumentException -
    /// if startIndex is bigger than endIndex
    ///
    /// - `ArrayIndexOutOfBoundsException`: @throws ArrayIndexOutOfBoundsException -
    /// if startIndex is smaller than zero or or endIndex is bigger
    /// than length of array
    ///
    /// #### Since
    ///
    /// 1.6
    public static int binarySearch(long[] array, int startIndex, int endIndex,
            long value) {
        checkIndexForBinarySearch(array.length, startIndex, endIndex);
        int low = startIndex, mid = -1, high = endIndex - 1;
        while (low <= high) {
            mid = (low + high) >>> 1;
            if (value > array[mid]) {
                low = mid + 1;
            } else if (value == array[mid]) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        if (mid < 0) {
            int insertPoint = endIndex;
            for (int index = startIndex; index < endIndex; index++) {
                if (value < array[index]) {
                    insertPoint = index;
                }
            }
            return -insertPoint - 1;
        }
        return -mid - (value < array[mid] ? 1 : 2);
    }

    /// Performs a binary search for the specified element in a part of the
    /// specified sorted array.
    ///
    /// #### Parameters
    ///
    /// - `array`: the sorted Object array to search
    ///
    /// - `startIndex`: the inclusive start index
    ///
    /// - `endIndex`: the exclusive end index
    ///
    /// - `object`: the object element to find
    ///
    /// #### Returns
    ///
    /// @return the non-negative index of the element, or a negative index which
    /// is the -index - 1 where the element would be inserted
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// when an element in the array or the search element does not
    /// implement Comparable, or cannot be compared to each other
    ///
    /// - `IllegalArgumentException`: @throws IllegalArgumentException -
    /// if startIndex is bigger than endIndex
    ///
    /// - `ArrayIndexOutOfBoundsException`: @throws ArrayIndexOutOfBoundsException -
    /// if startIndex is smaller than zero or or endIndex is bigger
    /// than length of array
    ///
    /// #### Since
    ///
    /// 1.6
    @SuppressWarnings("unchecked")
    public static int binarySearch(Object[] array, int startIndex,
            int endIndex, Object object) {
        checkIndexForBinarySearch(array.length, startIndex, endIndex);
        if (array.length == 0) {
            return -1;
        }

        int low = startIndex, mid = -1, high = endIndex - 1, result = 0;
        while (low <= high) {
            mid = (low + high) >>> 1;
            if ((result = ((java.lang.Comparable<Object>)array[mid]).compareTo(object)) < 0){
                low = mid + 1;
            } else if (result == 0) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        if (mid < 0) {
            int insertPoint = endIndex;
            for (int index = startIndex; index < endIndex; index++) {
                if (((java.lang.Comparable<Object>) object).compareTo(array[index]) < 0) {
                    insertPoint = index;
                }
            }
            return -insertPoint - 1;
        }
        return -mid - (result >= 0 ? 1 : 2);
    }

    /// Performs a binary search for the specified element in a part of the
    /// specified sorted array using the Comparator to compare elements.
    ///
    /// @param
    /// type of object
    ///
    /// #### Parameters
    ///
    /// - `array`: the sorted Object array to search
    ///
    /// - `startIndex`: the inclusive start index
    ///
    /// - `endIndex`: the exclusive end index
    ///
    /// - `object`: the value element to find
    ///
    /// - `comparator`: the Comparator
    ///
    /// #### Returns
    ///
    /// @return the non-negative index of the element, or a negative index which
    /// is the -index - 1 where the element would be inserted
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// when an element in the array and the search element cannot be
    /// compared to each other using the Comparator
    ///
    /// - `IllegalArgumentException`: @throws IllegalArgumentException -
    /// if startIndex is bigger than endIndex
    ///
    /// - `ArrayIndexOutOfBoundsException`: @throws ArrayIndexOutOfBoundsException -
    /// if startIndex is smaller than zero or or endIndex is bigger
    /// than length of array
    ///
    /// #### Since
    ///
    /// 1.6
    public static <T> int binarySearch(T[] array, int startIndex, int endIndex,
            T object, Comparator<? super T> comparator) {
        checkIndexForBinarySearch(array.length, startIndex, endIndex);
        if (comparator == null) {
            return binarySearch(array, startIndex, endIndex, object);
        }

        int low = startIndex, mid = -1, high = endIndex - 1, result = 0;
        while (low <= high) {
            mid = (low + high) >>> 1;
            if ((result = comparator.compare(array[mid], object)) < 0) {
                low = mid + 1;
            } else if (result == 0) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        if (mid < 0) {
            int insertPoint = endIndex;
            for (int index = startIndex; index < endIndex; index++) {
                if (comparator.compare(object, array[index]) < 0) {
                    insertPoint = index;
                }
            }
            return -insertPoint - 1;
        }
        return -mid - (result >= 0 ? 1 : 2);
    }

    /// Performs a binary search for the specified element in a part of the
    /// specified sorted array.
    ///
    /// #### Parameters
    ///
    /// - `array`: the sorted short array to search
    ///
    /// - `startIndex`: the inclusive start index
    ///
    /// - `endIndex`: the exclusive end index
    ///
    /// - `value`: the short element to find
    ///
    /// #### Returns
    ///
    /// @return the non-negative index of the element, or a negative index which
    /// is the -index - 1 where the element would be inserted
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: @throws IllegalArgumentException -
    /// if startIndex is bigger than endIndex
    ///
    /// - `ArrayIndexOutOfBoundsException`: @throws ArrayIndexOutOfBoundsException -
    /// if startIndex is smaller than zero or or endIndex is bigger
    /// than length of array
    ///
    /// #### Since
    ///
    /// 1.6
    public static int binarySearch(short[] array, int startIndex, int endIndex,
            short value) {
        checkIndexForBinarySearch(array.length, startIndex, endIndex);
        int low = startIndex, mid = -1, high = endIndex - 1;
        while (low <= high) {
            mid = (low + high) >>> 1;
            if (value > array[mid]) {
                low = mid + 1;
            } else if (value == array[mid]) {
                return mid;
            } else {
                high = mid - 1;
            }
        }
        if (mid < 0) {
            int insertPoint = endIndex;
            for (int index = startIndex; index < endIndex; index++) {
                if (value < array[index]) {
                    insertPoint = index;
                }
            }
            return -insertPoint - 1;
        }
        return -mid - (value < array[mid] ? 1 : 2);
    }

    /// Fills the array with the given value.
    ///
    /// #### Parameters
    ///
    /// - `length`: length of the array
    ///
    /// - `start`: start index
    ///
    /// - `end`: end index
    private static void checkIndexForBinarySearch(int length, int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException();
        }
        if (length < end || 0 > start) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /// Fills the specified array with the specified element.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `byte` array to fill.
    ///
    /// - `value`: the `byte` element.
    public static void fill(byte[] array, byte value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    /// Fills the specified range in the array with the specified element.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `byte` array to fill.
    ///
    /// - `start`: the first index to fill.
    ///
    /// - `end`: the last + 1 index to fill.
    ///
    /// - `value`: the `byte` element.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `start > end`.
    ///
    /// - `ArrayIndexOutOfBoundsException`: if `start  array.length`.
    public static void fill(byte[] array, int start, int end, byte value) {
        checkBounds(array.length, start, end);
        for (int i = start; i < end; i++) {
            array[i] = value;
        }
    }

    /// Fills the specified array with the specified element.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `short` array to fill.
    ///
    /// - `value`: the `short` element.
    public static void fill(short[] array, short value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    /// Fills the specified range in the array with the specified element.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `short` array to fill.
    ///
    /// - `start`: the first index to fill.
    ///
    /// - `end`: the last + 1 index to fill.
    ///
    /// - `value`: the `short` element.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `start > end`.
    ///
    /// - `ArrayIndexOutOfBoundsException`: if `start  array.length`.
    public static void fill(short[] array, int start, int end, short value) {
        checkBounds(array.length, start, end);
        for (int i = start; i < end; i++) {
            array[i] = value;
        }
    }

    /// Fills the specified array with the specified element.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `char` array to fill.
    ///
    /// - `value`: the `char` element.
    public static void fill(char[] array, char value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    /// Fills the specified range in the array with the specified element.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `char` array to fill.
    ///
    /// - `start`: the first index to fill.
    ///
    /// - `end`: the last + 1 index to fill.
    ///
    /// - `value`: the `char` element.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `start > end`.
    ///
    /// - `ArrayIndexOutOfBoundsException`: if `start  array.length`.
    public static void fill(char[] array, int start, int end, char value) {
        checkBounds(array.length, start, end);
        for (int i = start; i < end; i++) {
            array[i] = value;
        }
    }

    /// Fills the specified array with the specified element.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `int` array to fill.
    ///
    /// - `value`: the `int` element.
    public static void fill(int[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    /// Fills the specified range in the array with the specified element.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `int` array to fill.
    ///
    /// - `start`: the first index to fill.
    ///
    /// - `end`: the last + 1 index to fill.
    ///
    /// - `value`: the `int` element.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `start > end`.
    ///
    /// - `ArrayIndexOutOfBoundsException`: if `start  array.length`.
    public static void fill(int[] array, int start, int end, int value) {
        checkBounds(array.length, start, end);
        for (int i = start; i < end; i++) {
            array[i] = value;
        }
    }

    /// Fills the specified array with the specified element.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `long` array to fill.
    ///
    /// - `value`: the `long` element.
    public static void fill(long[] array, long value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    /// Fills the specified range in the array with the specified element.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `long` array to fill.
    ///
    /// - `start`: the first index to fill.
    ///
    /// - `end`: the last + 1 index to fill.
    ///
    /// - `value`: the `long` element.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `start > end`.
    ///
    /// - `ArrayIndexOutOfBoundsException`: if `start  array.length`.
    public static void fill(long[] array, int start, int end, long value) {
        checkBounds(array.length, start, end);
        for (int i = start; i < end; i++) {
            array[i] = value;
        }
    }

    /// Fills the specified array with the specified element.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `float` array to fill.
    ///
    /// - `value`: the `float` element.
    public static void fill(float[] array, float value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    /// Fills the specified range in the array with the specified element.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `float` array to fill.
    ///
    /// - `start`: the first index to fill.
    ///
    /// - `end`: the last + 1 index to fill.
    ///
    /// - `value`: the `float` element.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `start > end`.
    ///
    /// - `ArrayIndexOutOfBoundsException`: if `start  array.length`.
    public static void fill(float[] array, int start, int end, float value) {
        checkBounds(array.length, start, end);
        for (int i = start; i < end; i++) {
            array[i] = value;
        }
    }

    /// Fills the specified array with the specified element.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `double` array to fill.
    ///
    /// - `value`: the `double` element.
    public static void fill(double[] array, double value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    /// Fills the specified range in the array with the specified element.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `double` array to fill.
    ///
    /// - `start`: the first index to fill.
    ///
    /// - `end`: the last + 1 index to fill.
    ///
    /// - `value`: the `double` element.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `start > end`.
    ///
    /// - `ArrayIndexOutOfBoundsException`: if `start  array.length`.
    public static void fill(double[] array, int start, int end, double value) {
        checkBounds(array.length, start, end);
        for (int i = start; i < end; i++) {
            array[i] = value;
        }
    }

    /// Fills the specified array with the specified element.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `boolean` array to fill.
    ///
    /// - `value`: the `boolean` element.
    public static void fill(boolean[] array, boolean value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    /// Fills the specified range in the array with the specified element.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `boolean` array to fill.
    ///
    /// - `start`: the first index to fill.
    ///
    /// - `end`: the last + 1 index to fill.
    ///
    /// - `value`: the `boolean` element.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `start > end`.
    ///
    /// - `ArrayIndexOutOfBoundsException`: if `start  array.length`.
    public static void fill(boolean[] array, int start, int end, boolean value) {
        checkBounds(array.length, start, end);
        for (int i = start; i < end; i++) {
            array[i] = value;
        }
    }

    /// Fills the specified array with the specified element.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `Object` array to fill.
    ///
    /// - `value`: the `Object` element.
    public static void fill(Object[] array, Object value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    /// Fills the specified range in the array with the specified element.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `Object` array to fill.
    ///
    /// - `start`: the first index to fill.
    ///
    /// - `end`: the last + 1 index to fill.
    ///
    /// - `value`: the `Object` element.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `start > end`.
    ///
    /// - `ArrayIndexOutOfBoundsException`: if `start  array.length`.
    public static void fill(Object[] array, int start, int end, Object value) {
        checkBounds(array.length, start, end);
        for (int i = start; i < end; i++) {
            array[i] = value;
        }
    }

    /// Returns a hash code based on the contents of the given array. For any two
    /// `boolean` arrays `a` and `b`, if
    /// `Arrays.equals(a, b)` returns `true`, it means
    /// that the return value of `Arrays.hashCode(a)` equals `Arrays.hashCode(b)`.
    ///
    /// The value returned by this method is the same value as the
    /// `List#hashCode()`} method which is invoked on a `List`}
    /// containing a sequence of `Boolean`} instances representing the
    /// elements of array in the same order. If the array is `null`, the return
    /// value is 0.
    ///
    /// #### Parameters
    ///
    /// - `array`: the array whose hash code to compute.
    ///
    /// #### Returns
    ///
    /// the hash code for `array`.
    public static int hashCode(boolean[] array) {
        if (array == null) {
            return 0;
        }
        int hashCode = 1;
        for (boolean element : array) {
            // 1231, 1237 are hash code values for boolean value
            hashCode = 31 * hashCode + (element ? 1231 : 1237);
        }
        return hashCode;
    }

    /// Returns a hash code based on the contents of the given array. For any two
    /// not-null `int` arrays `a` and `b`, if
    /// `Arrays.equals(a, b)` returns `true`, it means
    /// that the return value of `Arrays.hashCode(a)` equals `Arrays.hashCode(b)`.
    ///
    /// The value returned by this method is the same value as the
    /// `List#hashCode()`} method which is invoked on a `List`}
    /// containing a sequence of `Integer`} instances representing the
    /// elements of array in the same order. If the array is `null`, the return
    /// value is 0.
    ///
    /// #### Parameters
    ///
    /// - `array`: the array whose hash code to compute.
    ///
    /// #### Returns
    ///
    /// the hash code for `array`.
    public static int hashCode(int[] array) {
        if (array == null) {
            return 0;
        }
        int hashCode = 1;
        for (int element : array) {
            // the hash code value for integer value is integer value itself
            hashCode = 31 * hashCode + element;
        }
        return hashCode;
    }

    /// Returns a hash code based on the contents of the given array. For any two
    /// `short` arrays `a` and `b`, if
    /// `Arrays.equals(a, b)` returns `true`, it means
    /// that the return value of `Arrays.hashCode(a)` equals `Arrays.hashCode(b)`.
    ///
    /// The value returned by this method is the same value as the
    /// `List#hashCode()`} method which is invoked on a `List`}
    /// containing a sequence of `Short`} instances representing the
    /// elements of array in the same order. If the array is `null`, the return
    /// value is 0.
    ///
    /// #### Parameters
    ///
    /// - `array`: the array whose hash code to compute.
    ///
    /// #### Returns
    ///
    /// the hash code for `array`.
    public static int hashCode(short[] array) {
        if (array == null) {
            return 0;
        }
        int hashCode = 1;
        for (short element : array) {
            // the hash code value for short value is its integer value
            hashCode = 31 * hashCode + element;
        }
        return hashCode;
    }

    /// Returns a hash code based on the contents of the given array. For any two
    /// `char` arrays `a` and `b`, if
    /// `Arrays.equals(a, b)` returns `true`, it means
    /// that the return value of `Arrays.hashCode(a)` equals `Arrays.hashCode(b)`.
    ///
    /// The value returned by this method is the same value as the
    /// `List#hashCode()`} method which is invoked on a `List`}
    /// containing a sequence of `Character`} instances representing the
    /// elements of array in the same order. If the array is `null`, the return
    /// value is 0.
    ///
    /// #### Parameters
    ///
    /// - `array`: the array whose hash code to compute.
    ///
    /// #### Returns
    ///
    /// the hash code for `array`.
    public static int hashCode(char[] array) {
        if (array == null) {
            return 0;
        }
        int hashCode = 1;
        for (char element : array) {
            // the hash code value for char value is its integer value
            hashCode = 31 * hashCode + element;
        }
        return hashCode;
    }

    /// Returns a hash code based on the contents of the given array. For any two
    /// `byte` arrays `a` and `b`, if
    /// `Arrays.equals(a, b)` returns `true`, it means
    /// that the return value of `Arrays.hashCode(a)` equals `Arrays.hashCode(b)`.
    ///
    /// The value returned by this method is the same value as the
    /// `List#hashCode()`} method which is invoked on a `List`}
    /// containing a sequence of `Byte`} instances representing the
    /// elements of array in the same order. If the array is `null`, the return
    /// value is 0.
    ///
    /// #### Parameters
    ///
    /// - `array`: the array whose hash code to compute.
    ///
    /// #### Returns
    ///
    /// the hash code for `array`.
    public static int hashCode(byte[] array) {
        if (array == null) {
            return 0;
        }
        int hashCode = 1;
        for (byte element : array) {
            // the hash code value for byte value is its integer value
            hashCode = 31 * hashCode + element;
        }
        return hashCode;
    }

    /// Returns a hash code based on the contents of the given array. For any two
    /// `long` arrays `a` and `b`, if
    /// `Arrays.equals(a, b)` returns `true`, it means
    /// that the return value of `Arrays.hashCode(a)` equals `Arrays.hashCode(b)`.
    ///
    /// The value returned by this method is the same value as the
    /// `List#hashCode()`} method which is invoked on a `List`}
    /// containing a sequence of `Long`} instances representing the
    /// elements of array in the same order. If the array is `null`, the return
    /// value is 0.
    ///
    /// #### Parameters
    ///
    /// - `array`: the array whose hash code to compute.
    ///
    /// #### Returns
    ///
    /// the hash code for `array`.
    public static int hashCode(long[] array) {
        if (array == null) {
            return 0;
        }
        int hashCode = 1;
        for (long elementValue : array) {
            /*
             * the hash code value for long value is (int) (value ^ (value >>>
             * 32))
             */
            hashCode = 31 * hashCode
                    + (int) (elementValue ^ (elementValue >>> 32));
        }
        return hashCode;
    }

    /// Returns a hash code based on the contents of the given array. For any two
    /// `float` arrays `a` and `b`, if
    /// `Arrays.equals(a, b)` returns `true`, it means
    /// that the return value of `Arrays.hashCode(a)` equals `Arrays.hashCode(b)`.
    ///
    /// The value returned by this method is the same value as the
    /// `List#hashCode()`} method which is invoked on a `List`}
    /// containing a sequence of `Float`} instances representing the
    /// elements of array in the same order. If the array is `null`, the return
    /// value is 0.
    ///
    /// #### Parameters
    ///
    /// - `array`: the array whose hash code to compute.
    ///
    /// #### Returns
    ///
    /// the hash code for `array`.
    public static int hashCode(float[] array) {
        if (array == null) {
            return 0;
        }
        int hashCode = 1;
        for (float element : array) {
            /*
             * the hash code value for float value is
             * Float.floatToIntBits(value)
             */
            hashCode = 31 * hashCode + Float.floatToIntBits(element);
        }
        return hashCode;
    }

    /// Returns a hash code based on the contents of the given array. For any two
    /// `double` arrays `a` and `b`, if
    /// `Arrays.equals(a, b)` returns `true`, it means
    /// that the return value of `Arrays.hashCode(a)` equals `Arrays.hashCode(b)`.
    ///
    /// The value returned by this method is the same value as the
    /// `List#hashCode()`} method which is invoked on a `List`}
    /// containing a sequence of `Double`} instances representing the
    /// elements of array in the same order. If the array is `null`, the return
    /// value is 0.
    ///
    /// #### Parameters
    ///
    /// - `array`: the array whose hash code to compute.
    ///
    /// #### Returns
    ///
    /// the hash code for `array`.
    public static int hashCode(double[] array) {
        if (array == null) {
            return 0;
        }
        int hashCode = 1;

        for (double element : array) {
            long v = Double.doubleToLongBits(element);
            /*
             * the hash code value for double value is (int) (v ^ (v >>> 32))
             * where v = Double.doubleToLongBits(value)
             */
            hashCode = 31 * hashCode + (int) (v ^ (v >>> 32));
        }
        return hashCode;
    }

    /// Returns a hash code based on the contents of the given array. If the
    /// array contains other arrays as its elements, the hash code is based on
    /// their identities not their contents. So it is acceptable to invoke this
    /// method on an array that contains itself as an element, either directly or
    /// indirectly.
    ///
    /// For any two arrays `a` and `b`, if
    /// `Arrays.equals(a, b)` returns `true`, it means
    /// that the return value of `Arrays.hashCode(a)` equals
    /// `Arrays.hashCode(b)`.
    ///
    /// The value returned by this method is the same value as the method
    /// Arrays.asList(array).hashCode(). If the array is `null`, the return value
    /// is 0.
    ///
    /// #### Parameters
    ///
    /// - `array`: the array whose hash code to compute.
    ///
    /// #### Returns
    ///
    /// the hash code for `array`.
    public static int hashCode(Object[] array) {
        if (array == null) {
            return 0;
        }
        int hashCode = 1;
        for (Object element : array) {
            int elementHashCode;

            if (element == null) {
                elementHashCode = 0;
            } else {
                elementHashCode = (element).hashCode();
            }
            hashCode = 31 * hashCode + elementHashCode;
        }
        return hashCode;
    }

    /// Returns a hash code based on the "deep contents" of the given array. If
    /// the array contains other arrays as its elements, the hash code is based
    /// on their contents not their identities. So it is not acceptable to invoke
    /// this method on an array that contains itself as an element, either
    /// directly or indirectly.
    ///
    /// For any two arrays `a` and `b`, if
    /// `Arrays.deepEquals(a, b)` returns `true`, it
    /// means that the return value of `Arrays.deepHashCode(a)` equals
    /// `Arrays.deepHashCode(b)`.
    ///
    /// The computation of the value returned by this method is similar to that
    /// of the value returned by `List#hashCode()`} invoked on a
    /// `List`} containing a sequence of instances representing the
    /// elements of array in the same order. The difference is: If an element e
    /// of array is itself an array, its hash code is computed by calling the
    /// appropriate overloading of `Arrays.hashCode(e)` if e is an array of a
    /// primitive type, or by calling `Arrays.deepHashCode(e)` recursively if e is
    /// an array of a reference type. The value returned by this method is the
    /// same value as the method `Arrays.asList(array).hashCode()`. If the array is
    /// `null`, the return value is 0.
    ///
    /// #### Parameters
    ///
    /// - `array`: the array whose hash code to compute.
    ///
    /// #### Returns
    ///
    /// the hash code for `array`.
    public static int deepHashCode(Object[] array) {
        if (array == null) {
            return 0;
        }
        int hashCode = 1;
        for (Object element : array) {
            int elementHashCode = deepHashCodeElement(element);
            hashCode = 31 * hashCode + elementHashCode;
        }
        return hashCode;
    }

    private static int deepHashCodeElement(Object element) {
        Class<?> cl;
        if (element == null) {
            return 0;
        }

        return element.hashCode();
    }

    /// Compares the two arrays.
    ///
    /// #### Parameters
    ///
    /// - `array1`: the first `byte` array.
    ///
    /// - `array2`: the second `byte` array.
    ///
    /// #### Returns
    ///
    /// @return `true` if both arrays are `null` or if the arrays have the
    /// same length and the elements at each index in the two arrays are
    /// equal, `false` otherwise.
    public static boolean equals(byte[] array1, byte[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    /// Compares the two arrays.
    ///
    /// #### Parameters
    ///
    /// - `array1`: the first `short` array.
    ///
    /// - `array2`: the second `short` array.
    ///
    /// #### Returns
    ///
    /// @return `true` if both arrays are `null` or if the arrays have the
    /// same length and the elements at each index in the two arrays are
    /// equal, `false` otherwise.
    public static boolean equals(short[] array1, short[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    /// Compares the two arrays.
    ///
    /// #### Parameters
    ///
    /// - `array1`: the first `char` array.
    ///
    /// - `array2`: the second `char` array.
    ///
    /// #### Returns
    ///
    /// @return `true` if both arrays are `null` or if the arrays have the
    /// same length and the elements at each index in the two arrays are
    /// equal, `false` otherwise.
    public static boolean equals(char[] array1, char[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    /// Compares the two arrays.
    ///
    /// #### Parameters
    ///
    /// - `array1`: the first `int` array.
    ///
    /// - `array2`: the second `int` array.
    ///
    /// #### Returns
    ///
    /// @return `true` if both arrays are `null` or if the arrays have the
    /// same length and the elements at each index in the two arrays are
    /// equal, `false` otherwise.
    public static boolean equals(int[] array1, int[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    /// Compares the two arrays.
    ///
    /// #### Parameters
    ///
    /// - `array1`: the first `long` array.
    ///
    /// - `array2`: the second `long` array.
    ///
    /// #### Returns
    ///
    /// @return `true` if both arrays are `null` or if the arrays have the
    /// same length and the elements at each index in the two arrays are
    /// equal, `false` otherwise.
    public static boolean equals(long[] array1, long[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    /// Compares the two arrays. The values are compared in the same manner as
    /// `Float.equals()`.
    ///
    /// #### Parameters
    ///
    /// - `array1`: the first `float` array.
    ///
    /// - `array2`: the second `float` array.
    ///
    /// #### Returns
    ///
    /// @return `true` if both arrays are `null` or if the arrays have the
    /// same length and the elements at each index in the two arrays are
    /// equal, `false` otherwise.
    ///
    /// #### See also
    ///
    /// - Float#equals(Object)
    public static boolean equals(float[] array1, float[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (Float.floatToIntBits(array1[i]) != Float
                    .floatToIntBits(array2[i])) {
                return false;
            }
        }
        return true;
    }

    /// Compares the two arrays. The values are compared in the same manner as
    /// `Double.equals()`.
    ///
    /// #### Parameters
    ///
    /// - `array1`: the first `double` array.
    ///
    /// - `array2`: the second `double` array.
    ///
    /// #### Returns
    ///
    /// @return `true` if both arrays are `null` or if the arrays have the
    /// same length and the elements at each index in the two arrays are
    /// equal, `false` otherwise.
    ///
    /// #### See also
    ///
    /// - Double#equals(Object)
    public static boolean equals(double[] array1, double[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (Double.doubleToLongBits(array1[i]) != Double
                    .doubleToLongBits(array2[i])) {
                return false;
            }
        }
        return true;
    }

    /// Compares the two arrays.
    ///
    /// #### Parameters
    ///
    /// - `array1`: the first `boolean` array.
    ///
    /// - `array2`: the second `boolean` array.
    ///
    /// #### Returns
    ///
    /// @return `true` if both arrays are `null` or if the arrays have the
    /// same length and the elements at each index in the two arrays are
    /// equal, `false` otherwise.
    public static boolean equals(boolean[] array1, boolean[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    /// Compares the two arrays.
    ///
    /// #### Parameters
    ///
    /// - `array1`: the first `Object` array.
    ///
    /// - `array2`: the second `Object` array.
    ///
    /// #### Returns
    ///
    /// @return `true` if both arrays are `null` or if the arrays have the
    /// same length and the elements at each index in the two arrays are
    /// equal according to `equals()`, `false` otherwise.
    public static boolean equals(Object[] array1, Object[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            Object e1 = array1[i], e2 = array2[i];
            if (!(e1 == null ? e2 == null : e1.equals(e2))) {
                return false;
            }
        }
        return true;
    }

    /// Returns `true` if the two given arrays are deeply equal to one another.
    /// Unlike the method `equals(Object[] array1, Object[] array2)`, this method
    /// is appropriate for use for nested arrays of arbitrary depth.
    ///
    /// Two array references are considered deeply equal if they are both `null`,
    /// or if they refer to arrays that have the same length and the elements at
    /// each index in the two arrays are equal.
    ///
    /// Two `null` elements `element1` and `element2` are possibly deeply equal if any
    /// of the following conditions satisfied:
    ///
    /// `element1` and `element2` are both arrays of object reference types, and
    /// `Arrays.deepEquals(element1, element2)` would return `true`.
    ///
    /// `element1` and `element2` are arrays of the same primitive type, and the
    /// appropriate overloading of `Arrays.equals(element1, element2)` would return
    /// `true`.
    ///
    /// `element1 == element2`
    ///
    /// `element1.equals(element2)` would return `true`.
    ///
    /// Note that this definition permits `null` elements at any depth.
    ///
    /// If either of the given arrays contain themselves as elements, the
    /// behavior of this method is uncertain.
    ///
    /// #### Parameters
    ///
    /// - `array1`: the first `Object` array.
    ///
    /// - `array2`: the second `Object` array.
    ///
    /// #### Returns
    ///
    /// @return `true` if both arrays are `null` or if the arrays have the
    /// same length and the elements at each index in the two arrays are
    /// equal according to `equals()`, `false` otherwise.
    public static boolean deepEquals(Object[] array1, Object[] array2) {
        if (array1 == array2) {
            return true;
        }
        if (array1 == null || array2 == null || array1.length != array2.length) {
            return false;
        }
        for (int i = 0; i < array1.length; i++) {
            Object e1 = array1[i], e2 = array2[i];

            if (!deepEqualsElements(e1, e2)) {
                return false;
            }
        }
        return true;
    }

    private static boolean deepEqualsElements(Object e1, Object e2) {
        Class<?> cl1, cl2;

        if (e1 == e2) {
            return true;
        }

        if (e1 == null || e2 == null) {
            return false;
        }
        return e1.equals(e2);
    }

    private static boolean isSame(double double1, double double2) {
        // Simple case
        if (double1 == double2 && 0.0d != double1) {
            return true;
        }

        // Deal with NaNs
        if (Double.isNaN(double1)) {
            return Double.isNaN(double2);
        }
        if (Double.isNaN(double2)) {
            return false;
        }

        // Deal with +0.0 and -0.0
        //long d1 = Double.doubleToRawLongBits(double1);
        //long d2 = Double.doubleToRawLongBits(double2);
        //return d1 == d2;
        return false;
    }

    private static boolean lessThan(double double1, double double2) {
        // A slightly specialized version of
        // Double.compare(double1, double2) < 0.

        // Non-zero and non-NaN checking.
        if (double1 < double2) {
            return true;
        }
        if (double1 > double2) {
            return false;
        }
        if (double1 == double2 && 0.0d != double1) {
            return false;
        }

        // NaNs are equal to other NaNs and larger than any other double.
        if (Double.isNaN(double1)) {
            return false;
        } else if (Double.isNaN(double2)) {
            return true;
        }

        // Deal with +0.0 and -0.0.
        //long d1 = Double.doubleToRawLongBits(double1);
        //long d2 = Double.doubleToRawLongBits(double2);
        return double1 < double2;
    }

    private static boolean isSame(float float1, float float2) {
        // Simple case
        if (float1 == float2 && 0.0d != float1) {
            return true;
        }

        // Deal with NaNs
        if (Float.isNaN(float1)) {
            return Float.isNaN(float2);
        }
        if (Float.isNaN(float2)) {
            return false;
        }

        // Deal with +0.0 and -0.0
        //int f1 = Float.floatToRawIntBits(float1);
        //int f2 = Float.floatToRawIntBits(float2);
        return false; //f1 == f2;
    }
    
    private static boolean lessThan(float float1, float float2) {
        // A slightly specialized version of Float.compare(float1, float2) < 0.

        // Non-zero and non-NaN checking.
        if (float1 < float2) {
            return true;
        }
        if (float1 > float2) {
            return false;
        }
        if (float1 == float2 && 0.0f != float1) {
            return false;
        }

        // NaNs are equal to other NaNs and larger than any other float
        if (Float.isNaN(float1)) {
            return false;
        } else if (Float.isNaN(float2)) {
            return true;
        }

        // Deal with +0.0 and -0.0
        //int f1 = Float.floatToRawIntBits(float1);
        //int f2 = Float.floatToRawIntBits(float2);
        return float1 < float2;
    }

    private static int med3(byte[] array, int a, int b, int c) {
        byte x = array[a], y = array[b], z = array[c];
        return x < y ? (y < z ? b : (x < z ? c : a)) : (y > z ? b : (x > z ? c
                : a));
    }

    private static int med3(char[] array, int a, int b, int c) {
        char x = array[a], y = array[b], z = array[c];
        return x < y ? (y < z ? b : (x < z ? c : a)) : (y > z ? b : (x > z ? c
                : a));
    }

    private static int med3(double[] array, int a, int b, int c) {
        double x = array[a], y = array[b], z = array[c];
        return lessThan(x, y) ? (lessThan(y, z) ? b : (lessThan(x, z) ? c : a))
                : (lessThan(z, y) ? b : (lessThan(z, x) ? c : a));
    }

    private static int med3(float[] array, int a, int b, int c) {
        float x = array[a], y = array[b], z = array[c];
        return lessThan(x, y) ? (lessThan(y, z) ? b : (lessThan(x, z) ? c : a))
                : (lessThan(z, y) ? b : (lessThan(z, x) ? c : a));
    }

    private static int med3(int[] array, int a, int b, int c) {
        int x = array[a], y = array[b], z = array[c];
        return x < y ? (y < z ? b : (x < z ? c : a)) : (y > z ? b : (x > z ? c
                : a));
    }

    private static int med3(long[] array, int a, int b, int c) {
        long x = array[a], y = array[b], z = array[c];
        return x < y ? (y < z ? b : (x < z ? c : a)) : (y > z ? b : (x > z ? c
                : a));
    }

    private static int med3(short[] array, int a, int b, int c) {
        short x = array[a], y = array[b], z = array[c];
        return x < y ? (y < z ? b : (x < z ? c : a)) : (y > z ? b : (x > z ? c
                : a));
    }

    /// Sorts the specified array in ascending numerical order.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `byte` array to be sorted.
    public static void sort(byte[] array) {
        sort(0, array.length, array);
    }

    /// Sorts the specified range in the array in ascending numerical order.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `byte` array to be sorted.
    ///
    /// - `start`: the start index to sort.
    ///
    /// - `end`: the last + 1 index to sort.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `start > end`.
    ///
    /// - `ArrayIndexOutOfBoundsException`: if `start  array.length`.
    public static void sort(byte[] array, int start, int end) {
        if (array == null) {
            throw new NullPointerException();
        }
        checkBounds(array.length, start, end);
        sort(start, end, array);
    }

    private static void checkBounds(int arrLength, int start, int end) {
        if (start > end) {
            // luni.35=Start index ({0}) is greater than end index ({1})
            throw new IndexOutOfBoundsException("" + start + " out of: " + end);
        }
        if (start < 0) {
            // luni.36=Array index out of range\: {0}
            throw new IndexOutOfBoundsException("" + start);
        }
        if (end > arrLength) {
            // luni.36=Array index out of range\: {0}
            throw new IndexOutOfBoundsException("" + end);
        }
    }

    private static void sort(int start, int end, byte[] array) {
        byte temp;
        int length = end - start;
        if (length < 7) {
            for (int i = start + 1; i < end; i++) {
                for (int j = i; j > start && array[j - 1] > array[j]; j--) {
                    temp = array[j];
                    array[j] = array[j - 1];
                    array[j - 1] = temp;
                }
            }
            return;
        }
        int middle = (start + end) / 2;
        if (length > 7) {
            int bottom = start;
            int top = end - 1;
            if (length > 40) {
                length /= 8;
                bottom = med3(array, bottom, bottom + length, bottom
                        + (2 * length));
                middle = med3(array, middle - length, middle, middle + length);
                top = med3(array, top - (2 * length), top - length, top);
            }
            middle = med3(array, bottom, middle, top);
        }
        byte partionValue = array[middle];
        int a, b, c, d;
        a = b = start;
        c = d = end - 1;
        while (true) {
            while (b <= c && array[b] <= partionValue) {
                if (array[b] == partionValue) {
                    temp = array[a];
                    array[a++] = array[b];
                    array[b] = temp;
                }
                b++;
            }
            while (c >= b && array[c] >= partionValue) {
                if (array[c] == partionValue) {
                    temp = array[c];
                    array[c] = array[d];
                    array[d--] = temp;
                }
                c--;
            }
            if (b > c) {
                break;
            }
            temp = array[b];
            array[b++] = array[c];
            array[c--] = temp;
        }
        length = a - start < b - a ? a - start : b - a;
        int l = start;
        int h = b - length;
        while (length-- > 0) {
            temp = array[l];
            array[l++] = array[h];
            array[h++] = temp;
        }
        length = d - c < end - 1 - d ? d - c : end - 1 - d;
        l = b;
        h = end - length;
        while (length-- > 0) {
            temp = array[l];
            array[l++] = array[h];
            array[h++] = temp;
        }
        if ((length = b - a) > 0) {
            sort(start, start + length, array);
        }
        if ((length = d - c) > 0) {
            sort(end - length, end, array);
        }
    }

    /// Sorts the specified array in ascending numerical order.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `char` array to be sorted.
    public static void sort(char[] array) {
        sort(0, array.length, array);
    }

    /// Sorts the specified range in the array in ascending numerical order.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `char` array to be sorted.
    ///
    /// - `start`: the start index to sort.
    ///
    /// - `end`: the last + 1 index to sort.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `start > end`.
    ///
    /// - `ArrayIndexOutOfBoundsException`: if `start  array.length`.
    public static void sort(char[] array, int start, int end) {
        if (array == null) {
            throw new NullPointerException();
        }
        checkBounds(array.length, start, end);
        sort(start, end, array);
    }

    private static void sort(int start, int end, char[] array) {
        char temp;
        int length = end - start;
        if (length < 7) {
            for (int i = start + 1; i < end; i++) {
                for (int j = i; j > start && array[j - 1] > array[j]; j--) {
                    temp = array[j];
                    array[j] = array[j - 1];
                    array[j - 1] = temp;
                }
            }
            return;
        }
        int middle = (start + end) / 2;
        if (length > 7) {
            int bottom = start;
            int top = end - 1;
            if (length > 40) {
                length /= 8;
                bottom = med3(array, bottom, bottom + length, bottom
                        + (2 * length));
                middle = med3(array, middle - length, middle, middle + length);
                top = med3(array, top - (2 * length), top - length, top);
            }
            middle = med3(array, bottom, middle, top);
        }
        char partionValue = array[middle];
        int a, b, c, d;
        a = b = start;
        c = d = end - 1;
        while (true) {
            while (b <= c && array[b] <= partionValue) {
                if (array[b] == partionValue) {
                    temp = array[a];
                    array[a++] = array[b];
                    array[b] = temp;
                }
                b++;
            }
            while (c >= b && array[c] >= partionValue) {
                if (array[c] == partionValue) {
                    temp = array[c];
                    array[c] = array[d];
                    array[d--] = temp;
                }
                c--;
            }
            if (b > c) {
                break;
            }
            temp = array[b];
            array[b++] = array[c];
            array[c--] = temp;
        }
        length = a - start < b - a ? a - start : b - a;
        int l = start;
        int h = b - length;
        while (length-- > 0) {
            temp = array[l];
            array[l++] = array[h];
            array[h++] = temp;
        }
        length = d - c < end - 1 - d ? d - c : end - 1 - d;
        l = b;
        h = end - length;
        while (length-- > 0) {
            temp = array[l];
            array[l++] = array[h];
            array[h++] = temp;
        }
        if ((length = b - a) > 0) {
            sort(start, start + length, array);
        }
        if ((length = d - c) > 0) {
            sort(end - length, end, array);
        }
    }

    /// Sorts the specified array in ascending numerical order.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `double` array to be sorted.
    ///
    /// #### See also
    ///
    /// - #sort(double[], int, int)
    public static void sort(double[] array) {
        sort(0, array.length, array);
    }

    /// Sorts the specified range in the array in ascending numerical order. The
    /// values are sorted according to the order imposed by `Double.compareTo()`.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `double` array to be sorted.
    ///
    /// - `start`: the start index to sort.
    ///
    /// - `end`: the last + 1 index to sort.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `start > end`.
    ///
    /// - `ArrayIndexOutOfBoundsException`: if `start  array.length`.
    ///
    /// #### See also
    ///
    /// - Double#compareTo(Double)
    public static void sort(double[] array, int start, int end) {
        if (array == null) {
            throw new NullPointerException();
        }
        checkBounds(array.length, start, end);
        sort(start, end, array);
    }

    private static void sort(int start, int end, double[] array) {
        double temp;
        int length = end - start;
        if (length < 7) {
            for (int i = start + 1; i < end; i++) {
                for (int j = i; j > start && lessThan(array[j], array[j - 1]); j--) {
                    temp = array[j];
                    array[j] = array[j - 1];
                    array[j - 1] = temp;
                }
            }
            return;
        }
        int middle = (start + end) / 2;
        if (length > 7) {
            int bottom = start;
            int top = end - 1;
            if (length > 40) {
                length /= 8;
                bottom = med3(array, bottom, bottom + length, bottom
                        + (2 * length));
                middle = med3(array, middle - length, middle, middle + length);
                top = med3(array, top - (2 * length), top - length, top);
            }
            middle = med3(array, bottom, middle, top);
        }
        double partionValue = array[middle];
        int a, b, c, d;
        a = b = start;
        c = d = end - 1;
        while (true) {
            while (b <= c && !lessThan(partionValue, array[b])) {
                if (isSame(array[b], partionValue)) {
                    temp = array[a];
                    array[a++] = array[b];
                    array[b] = temp;
                }
                b++;
            }
            while (c >= b && !lessThan(array[c], partionValue)) {
                if (isSame(array[c], partionValue)) {
                    temp = array[c];
                    array[c] = array[d];
                    array[d--] = temp;
                }
                c--;
            }
            if (b > c) {
                break;
            }
            temp = array[b];
            array[b++] = array[c];
            array[c--] = temp;
        }
        length = a - start < b - a ? a - start : b - a;
        int l = start;
        int h = b - length;
        while (length-- > 0) {
            temp = array[l];
            array[l++] = array[h];
            array[h++] = temp;
        }
        length = d - c < end - 1 - d ? d - c : end - 1 - d;
        l = b;
        h = end - length;
        while (length-- > 0) {
            temp = array[l];
            array[l++] = array[h];
            array[h++] = temp;
        }
        if ((length = b - a) > 0) {
            sort(start, start + length, array);
        }
        if ((length = d - c) > 0) {
            sort(end - length, end, array);
        }
    }

    /// Sorts the specified array in ascending numerical order.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `float` array to be sorted.
    ///
    /// #### See also
    ///
    /// - #sort(float[], int, int)
    public static void sort(float[] array) {
        sort(0, array.length, array);
    }

    /// Sorts the specified range in the array in ascending numerical order. The
    /// values are sorted according to the order imposed by `Float.compareTo()`.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `float` array to be sorted.
    ///
    /// - `start`: the start index to sort.
    ///
    /// - `end`: the last + 1 index to sort.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `start > end`.
    ///
    /// - `ArrayIndexOutOfBoundsException`: if `start  array.length`.
    ///
    /// #### See also
    ///
    /// - Float#compareTo(Float)
    public static void sort(float[] array, int start, int end) {
        if (array == null) {
            throw new NullPointerException();
        }
        checkBounds(array.length, start, end);
        sort(start, end, array);
    }

    private static void sort(int start, int end, float[] array) {
        float temp;
        int length = end - start;
        if (length < 7) {
            for (int i = start + 1; i < end; i++) {
                for (int j = i; j > start && lessThan(array[j], array[j - 1]); j--) {
                    temp = array[j];
                    array[j] = array[j - 1];
                    array[j - 1] = temp;
                }
            }
            return;
        }
        int middle = (start + end) / 2;
        if (length > 7) {
            int bottom = start;
            int top = end - 1;
            if (length > 40) {
                length /= 8;
                bottom = med3(array, bottom, bottom + length, bottom
                        + (2 * length));
                middle = med3(array, middle - length, middle, middle + length);
                top = med3(array, top - (2 * length), top - length, top);
            }
            middle = med3(array, bottom, middle, top);
        }
        float partionValue = array[middle];
        int a, b, c, d;
        a = b = start;
        c = d = end - 1;
        while (true) {
            while (b <= c && !lessThan(partionValue, array[b])) {
                if (isSame(array[b], partionValue)) {
                    temp = array[a];
                    array[a++] = array[b];
                    array[b] = temp;
                }
                b++;
            }
            while (c >= b && !lessThan(array[c], partionValue)) {
                if (isSame(array[c], partionValue)) {
                    temp = array[c];
                    array[c] = array[d];
                    array[d--] = temp;
                }
                c--;
            }
            if (b > c) {
                break;
            }
            temp = array[b];
            array[b++] = array[c];
            array[c--] = temp;
        }
        length = a - start < b - a ? a - start : b - a;
        int l = start;
        int h = b - length;
        while (length-- > 0) {
            temp = array[l];
            array[l++] = array[h];
            array[h++] = temp;
        }
        length = d - c < end - 1 - d ? d - c : end - 1 - d;
        l = b;
        h = end - length;
        while (length-- > 0) {
            temp = array[l];
            array[l++] = array[h];
            array[h++] = temp;
        }
        if ((length = b - a) > 0) {
            sort(start, start + length, array);
        }
        if ((length = d - c) > 0) {
            sort(end - length, end, array);
        }
    }

    /// Sorts the specified array in ascending numerical order.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `int` array to be sorted.
    public static void sort(int[] array) {
        sort(0, array.length, array);
    }

    /// Sorts the specified range in the array in ascending numerical order.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `int` array to be sorted.
    ///
    /// - `start`: the start index to sort.
    ///
    /// - `end`: the last + 1 index to sort.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `start > end`.
    ///
    /// - `ArrayIndexOutOfBoundsException`: if `start  array.length`.
    public static void sort(int[] array, int start, int end) {
        if (array == null) {
            throw new NullPointerException();
        }
        checkBounds(array.length, start, end);
        sort(start, end, array);
    }

    private static void sort(int start, int end, int[] array) {
        int temp;
        int length = end - start;
        if (length < 7) {
            for (int i = start + 1; i < end; i++) {
                for (int j = i; j > start && array[j - 1] > array[j]; j--) {
                    temp = array[j];
                    array[j] = array[j - 1];
                    array[j - 1] = temp;
                }
            }
            return;
        }
        int middle = (start + end) / 2;
        if (length > 7) {
            int bottom = start;
            int top = end - 1;
            if (length > 40) {
                length /= 8;
                bottom = med3(array, bottom, bottom + length, bottom
                        + (2 * length));
                middle = med3(array, middle - length, middle, middle + length);
                top = med3(array, top - (2 * length), top - length, top);
            }
            middle = med3(array, bottom, middle, top);
        }
        int partionValue = array[middle];
        int a, b, c, d;
        a = b = start;
        c = d = end - 1;
        while (true) {
            while (b <= c && array[b] <= partionValue) {
                if (array[b] == partionValue) {
                    temp = array[a];
                    array[a++] = array[b];
                    array[b] = temp;
                }
                b++;
            }
            while (c >= b && array[c] >= partionValue) {
                if (array[c] == partionValue) {
                    temp = array[c];
                    array[c] = array[d];
                    array[d--] = temp;
                }
                c--;
            }
            if (b > c) {
                break;
            }
            temp = array[b];
            array[b++] = array[c];
            array[c--] = temp;
        }
        length = a - start < b - a ? a - start : b - a;
        int l = start;
        int h = b - length;
        while (length-- > 0) {
            temp = array[l];
            array[l++] = array[h];
            array[h++] = temp;
        }
        length = d - c < end - 1 - d ? d - c : end - 1 - d;
        l = b;
        h = end - length;
        while (length-- > 0) {
            temp = array[l];
            array[l++] = array[h];
            array[h++] = temp;
        }
        if ((length = b - a) > 0) {
            sort(start, start + length, array);
        }
        if ((length = d - c) > 0) {
            sort(end - length, end, array);
        }
    }

    /// Sorts the specified array in ascending numerical order.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `long` array to be sorted.
    public static void sort(long[] array) {
        sort(0, array.length, array);
    }

    /// Sorts the specified range in the array in ascending numerical order.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `long` array to be sorted.
    ///
    /// - `start`: the start index to sort.
    ///
    /// - `end`: the last + 1 index to sort.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `start > end`.
    ///
    /// - `ArrayIndexOutOfBoundsException`: if `start  array.length`.
    public static void sort(long[] array, int start, int end) {
        if (array == null) {
            throw new NullPointerException();
        }
        checkBounds(array.length, start, end);
        sort(start, end, array);
    }

    private static void sort(int start, int end, long[] array) {
        long temp;
        int length = end - start;
        if (length < 7) {
            for (int i = start + 1; i < end; i++) {
                for (int j = i; j > start && array[j - 1] > array[j]; j--) {
                    temp = array[j];
                    array[j] = array[j - 1];
                    array[j - 1] = temp;
                }
            }
            return;
        }
        int middle = (start + end) / 2;
        if (length > 7) {
            int bottom = start;
            int top = end - 1;
            if (length > 40) {
                length /= 8;
                bottom = med3(array, bottom, bottom + length, bottom
                        + (2 * length));
                middle = med3(array, middle - length, middle, middle + length);
                top = med3(array, top - (2 * length), top - length, top);
            }
            middle = med3(array, bottom, middle, top);
        }
        long partionValue = array[middle];
        int a, b, c, d;
        a = b = start;
        c = d = end - 1;
        while (true) {
            while (b <= c && array[b] <= partionValue) {
                if (array[b] == partionValue) {
                    temp = array[a];
                    array[a++] = array[b];
                    array[b] = temp;
                }
                b++;
            }
            while (c >= b && array[c] >= partionValue) {
                if (array[c] == partionValue) {
                    temp = array[c];
                    array[c] = array[d];
                    array[d--] = temp;
                }
                c--;
            }
            if (b > c) {
                break;
            }
            temp = array[b];
            array[b++] = array[c];
            array[c--] = temp;
        }
        length = a - start < b - a ? a - start : b - a;
        int l = start;
        int h = b - length;
        while (length-- > 0) {
            temp = array[l];
            array[l++] = array[h];
            array[h++] = temp;
        }
        length = d - c < end - 1 - d ? d - c : end - 1 - d;
        l = b;
        h = end - length;
        while (length-- > 0) {
            temp = array[l];
            array[l++] = array[h];
            array[h++] = temp;
        }
        if ((length = b - a) > 0) {
            sort(start, start + length, array);
        }
        if ((length = d - c) > 0) {
            sort(end - length, end, array);
        }
    }

    /// Sorts the specified array in ascending natural order.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `Object` array to be sorted.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// if an element in the array does not implement `Comparable`
    /// or if some elements cannot be compared to each other.
    ///
    /// #### See also
    ///
    /// - #sort(Object[], int, int)
    public static void sort(Object[] array) {
        sort(0, array.length, array);
    }

    /// Sorts the specified range in the array in ascending natural order. All
    /// elements must implement the `Comparable` interface and must be
    /// comparable to each other without a `ClassCastException` being
    /// thrown.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `Object` array to be sorted.
    ///
    /// - `start`: the start index to sort.
    ///
    /// - `end`: the last + 1 index to sort.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// if an element in the array does not implement `Comparable`
    /// or some elements cannot be compared to each other.
    ///
    /// - `IllegalArgumentException`: if `start > end`.
    ///
    /// - `ArrayIndexOutOfBoundsException`: if `start  array.length`.
    public static void sort(Object[] array, int start, int end) {
        if (array == null) {
            throw new NullPointerException();
        }
        checkBounds(array.length, start, end);
        sort(start, end, array);
    }

    private static void sort(int start, int end, Object[] array) {
        int length = end - start;
        if (length <= 0) {
            return;
        }
        if (array instanceof String[]) {
            stableStringSort((String[]) array, start, end);
        } else {
            Object[] out = new Object[end];
            System.arraycopy(array, start, out, start, length);
            mergeSort(out, array, start, end);
        }
    }

    /// Swaps the elements at the given indices in the array.
    ///
    /// #### Parameters
    ///
    /// - `a`: @param a -
    /// the index of one element to be swapped.
    ///
    /// - `b`: @param b -
    /// the index of the other element to be swapped.
    ///
    /// - `arr`: @param arr -
    /// the array in which to swap elements.
    private static void swap(int a, int b, Object[] arr) {
        Object tmp = arr[a];
        arr[a] = arr[b];
        arr[b] = tmp;
    }

    /// Performs a sort on the section of the array between the given indices
    /// using a mergesort with exponential search algorithm (in which the merge
    /// is performed by exponential search). n*log(n) performance is guaranteed
    /// and in the average case it will be faster then any mergesort in which the
    /// merge is performed by linear search.
    ///
    /// #### Parameters
    ///
    /// - `in`: @param in -
    /// the array for sorting.
    ///
    /// - `out`: @param out -
    /// the result, sorted array.
    ///
    /// - `start`: the start index
    ///
    /// - `end`: the end index + 1
    @SuppressWarnings("unchecked")
    private static void mergeSort(Object[] in, Object[] out, int start,
            int end) {
        int len = end - start;
        // use insertion sort for small arrays
        if (len <= SIMPLE_LENGTH) {
            for (int i = start + 1; i < end; i++) {
                java.lang.Comparable<Object> current = 
                            (java.lang.Comparable<Object>) out[i];
                Object prev = out[i - 1];
                if (current.compareTo(prev) < 0) {
                    int j = i;
                    do {
                        out[j--] = prev;
                    } while (j > start
                            && current.compareTo(prev = out[j - 1]) < 0);
                    out[j] = current;
                }
            }
            return;
        }
        int med = (end + start) >>> 1;
        mergeSort(out, in, start, med);
        mergeSort(out, in, med, end);

        // merging

        // if arrays are already sorted - no merge
        if (((java.lang.Comparable<Object>) in[med - 1]).compareTo(in[med]) <= 0) {
            System.arraycopy(in, start, out, start, len);
            return;
        }
        int r = med, i = start;

        // use merging with exponential search
        do {
            java.lang.Comparable<Object> fromVal = (java.lang.Comparable<Object>) in[start];
            java.lang.Comparable<Object> rVal = (java.lang.Comparable<Object>) in[r];
            if (fromVal.compareTo(rVal) <= 0) {
                int l_1 = find(in, rVal, -1, start + 1, med - 1);
                int toCopy = l_1 - start + 1;
                System.arraycopy(in, start, out, i, toCopy);
                i += toCopy;
                out[i++] = rVal;
                r++;
                start = l_1 + 1;
            } else {
                int r_1 = find(in, fromVal, 0, r + 1, end - 1);
                int toCopy = r_1 - r + 1;
                System.arraycopy(in, r, out, i, toCopy);
                i += toCopy;
                out[i++] = fromVal;
                start++;
                r = r_1 + 1;
            }
        } while ((end - r) > 0 && (med - start) > 0);

        // copy rest of array
        if ((end - r) <= 0) {
            System.arraycopy(in, start, out, i, med - start);
        } else {
            System.arraycopy(in, r, out, i, end - r);
        }
    }

    /// Performs a sort on the section of the array between the given indices
    /// using a mergesort with exponential search algorithm (in which the merge
    /// is performed by exponential search). n*log(n) performance is guaranteed
    /// and in the average case it will be faster then any mergesort in which the
    /// merge is performed by linear search.
    ///
    /// #### Parameters
    ///
    /// - `in`: @param in -
    /// the array for sorting.
    ///
    /// - `out`: @param out -
    /// the result, sorted array.
    ///
    /// - `start`: the start index
    ///
    /// - `end`: the end index + 1
    ///
    /// - `c`: @param c -
    /// the comparator to determine the order of the array.
    @SuppressWarnings("unchecked")
    private static void mergeSort(Object[] in, Object[] out, int start,
            int end, Comparator c) {
        int len = end - start;
        // use insertion sort for small arrays
        if (len <= SIMPLE_LENGTH) {
            for (int i = start + 1; i < end; i++) {
                Object current = out[i];
                Object prev = out[i - 1];
                if (c.compare(prev, current) > 0) {
                    int j = i;
                    do {
                        out[j--] = prev;
                    } while (j > start
                            && (c.compare(prev = out[j - 1], current) > 0));
                    out[j] = current;
                }
            }
            return;
        }
        int med = (end + start) >>> 1;
        mergeSort(out, in, start, med, c);
        mergeSort(out, in, med, end, c);

        // merging

        // if arrays are already sorted - no merge
        if (c.compare(in[med - 1],in[med] ) <= 0) {
            System.arraycopy(in, start, out, start, len);
            return;
        }
        int r = med, i = start;

        // use merging with exponential search
        do {
            Object fromVal = in[start];
            Object rVal = in[r];
            if (c.compare(fromVal, rVal) <= 0) {
                int l_1 = find(in, rVal, -1, start + 1, med - 1, c);
                int toCopy = l_1 - start + 1;
                System.arraycopy(in, start, out, i, toCopy);
                i += toCopy;
                out[i++] = rVal;
                r++;
                start = l_1 + 1;
            } else {
                int r_1 = find(in, fromVal, 0, r + 1, end - 1, c);
                int toCopy = r_1 - r + 1;
                System.arraycopy(in, r, out, i, toCopy);
                i += toCopy;
                out[i++] = fromVal;
                start++;
                r = r_1 + 1;
            }
        } while ((end - r) > 0 && (med - start) > 0);

        // copy rest of array
        if ((end - r) <= 0) {
            System.arraycopy(in, start, out, i, med - start);
        } else {
            System.arraycopy(in, r, out, i, end - r);
        }
    }

    /// Finds the place in the given range of specified sorted array, where the
    /// element should be inserted for getting sorted array. Uses exponential
    /// search algorithm.
    ///
    /// #### Parameters
    ///
    /// - `arr`: @param arr -
    /// the array with already sorted range
    ///
    /// - `val`: @param val -
    /// object to be inserted
    ///
    /// - `l`: @param l -
    /// the start index
    ///
    /// - `r`: @param r -
    /// the end index
    ///
    /// - `bnd`: @param bnd -
    /// possible values 0,-1. "-1" - val is located at index more then
    /// elements equals to val. "0" - val is located at index less
    /// then elements equals to val.
    @SuppressWarnings("unchecked")
    private static int find(Object[] arr, java.lang.Comparable val, int bnd, int l, int r) {
        int m = l;
        int d = 1;
        while (m <= r) {
            if (val.compareTo(arr[m]) > bnd) {
                l = m + 1;
            } else {
                r = m - 1;
                break;
            }
            m += d;
            d <<= 1;
        }
        while (l <= r) {
            m = (l + r) >>> 1;
            if (val.compareTo(arr[m]) > bnd) {
                l = m + 1;
            } else {
                r = m - 1;
            }
        }
        return l - 1;
    }

    /// Finds the place of specified range of specified sorted array, where the
    /// element should be inserted for getting sorted array. Uses exponential
    /// search algorithm.
    ///
    /// #### Parameters
    ///
    /// - `arr`: @param arr -
    /// the array with already sorted range
    ///
    /// - `val`: @param val -
    /// object to be inserted
    ///
    /// - `l`: @param l -
    /// the start index
    ///
    /// - `r`: @param r -
    /// the end index
    ///
    /// - `bnd`: @param bnd -
    /// possible values 0,-1. "-1" - val is located at index more then
    /// elements equals to val. "0" - val is located at index less
    /// then elements equals to val.
    ///
    /// - `c`: @param c -
    /// the comparator used to compare Objects
    @SuppressWarnings("unchecked")
    private static int find(Object[] arr, Object val, int bnd, int l, int r,
            Comparator c) {
        int m = l;
        int d = 1;
        while (m <= r) {
            if (c.compare(val, arr[m]) > bnd) {
                l = m + 1;
            } else {
                r = m - 1;
                break;
            }
            m += d;
            d <<= 1;
        }
        while (l <= r) {
            m = (l + r) >>> 1;
            if (c.compare(val, arr[m]) > bnd) {
                l = m + 1;
            } else {
                r = m - 1;
            }
        }
        return l - 1;
    }

    /*
     * returns the median index.
     */
    private static int medChar(int a, int b, int c, String[] arr, int id) {
        int ac = charAt(arr[a], id);
        int bc = charAt(arr[b], id);
        int cc = charAt(arr[c], id);
        return ac < bc ? (bc < cc ? b : (ac < cc ? c : a))
                : (bc < cc ? (ac < cc ? a : c) : b);

    }

    /*
     * Returns the char value at the specified index of string or -1 if the
     * index more than the length of this string.
     */
    private static int charAt(String str, int i) {
        if (i >= str.length()) {
            return -1;
        }
        return str.charAt(i);
    }

    /// Copies object from one array to another array with reverse of objects
    /// order. Source and destination arrays may be the same.
    ///
    /// #### Parameters
    ///
    /// - `src`: @param src -
    /// the source array.
    ///
    /// - `from`: @param from -
    /// starting position in the source array.
    ///
    /// - `dst`: @param dst -
    /// the destination array.
    ///
    /// - `to`: @param to -
    /// starting position in the destination array.
    ///
    /// - `len`: @param len -
    /// the number of array elements to be copied.
    private static void copySwap(Object[] src, int from, Object[] dst, int to,
            int len) {
        if (src == dst && from + len > to) {
            int new_to = to + len - 1;
            for (; from < to; from++, new_to--, len--) {
                dst[new_to] = src[from];
            }
            for (; len > 1; from++, new_to--, len -= 2) {
                swap(from, new_to, dst);
            }

        } else {
            to = to + len - 1;
            for (; len > 0; from++, to--, len--) {
                dst[to] = src[from];
            }
        }
    }

    /// Performs a sort on the given String array. Elements will be re-ordered into
    /// ascending order.
    ///
    /// #### Parameters
    ///
    /// - `arr`: @param arr -
    /// the array to sort
    ///
    /// - `start`: @param start -
    /// the start index
    ///
    /// - `end`: @param end -
    /// the end index + 1
    private static void stableStringSort(String[] arr, int start,
            int end) {
        stableStringSort(arr, arr, new String[end], start, end, 0);
    }

    /// Performs a sort on the given String array. Elements will be re-ordered into
    /// ascending order. Uses a stable ternary quick sort algorithm.
    ///
    /// #### Parameters
    ///
    /// - `arr`: @param arr -
    /// the array to sort
    ///
    /// - `src`: @param src -
    /// auxiliary array
    ///
    /// - `dst`: @param dst -
    /// auxiliary array
    ///
    /// - `start`: @param start -
    /// the start index
    ///
    /// - `end`: @param end -
    /// the end index + 1
    ///
    /// - `chId`: @param chId -
    /// index of char for current sorting
    private static void stableStringSort(String[] arr, String[] src,
            String[] dst, int start, int end, int chId) {
        int length = end - start;
        // use insertion sort for small arrays
        if (length < SIMPLE_LENGTH) {
            if (src == arr) {
                for (int i = start + 1; i < end; i++) {
                    String current = arr[i];
                    String prev = arr[i - 1];
                    if (current.compareTo(prev) < 0) {
                        int j = i;
                        do {
                            arr[j--] = prev;
                        } while (j > start
                                && current.compareTo(prev = arr[j - 1]) < 0);
                        arr[j] = current;
                    }
                }
            } else {
                int actualEnd = end - 1;
                dst[start] = src[actualEnd--];
                for (int i = start + 1; i < end; i++, actualEnd--) {
                    String current = src[actualEnd];
                    String prev;
                    int j = i;
                    while (j > start
                            && current.compareTo(prev = dst[j - 1]) < 0) {
                        dst[j--] = prev;
                    }
                    dst[j] = current;
                }
            }
            return;
        }
        // Approximate median
        int s;
        int mid = start + length / 2;
        int lo = start;
        int hi = end - 1;
        if (length > 40) {
            s = length / 8;
            lo = medChar(lo, lo + s, lo + s * 2, src, chId);
            mid = medChar(mid - s, mid, mid + s, src, chId);
            hi = medChar(hi, hi - s, hi - s * 2, src, chId);
        }
        mid = medChar(lo, mid, hi, src, chId);
        // median found
        // create 4 pointers <a (in star of src) ,
        // =b(in start of dst), >c (in end of dst)
        // i - current element;
        int midVal = charAt(src[mid], chId);
        int a, b, c;
        a = b = start;
        c = end - 1;
        int cmp;

        for (int i = start; i < end; i++) {
            String el = src[i];
            cmp = charAt(el, chId) - midVal;
            if (cmp < 0) {
                src[a] = el;
                a++;
            } else if (cmp > 0) {
                dst[c] = el;
                c--;
            } else {
                dst[b] = el;
                b++;
            }
        }

        s = b - start;
        if (s > 0) {
            if (arr == src) {
                System.arraycopy(dst, start, arr, a, s);
            } else {
                copySwap(dst, start, arr, a, s);
            }

            if (b >= end && midVal == -1) {
                return;
            }
            stableStringSort(arr, arr, arr == dst ? src : dst, a, a + s,
                    chId + 1);
        }

        s = a - start;
        if (s > 0) {
            stableStringSort(arr, src, dst, start, a, chId);
        }

        c++;
        s = end - c;
        if (s > 0) {
            stableStringSort(arr, dst, src, c, end, chId);
        }
    }

    /// Sorts the specified range in the array using the specified `Comparator`.
    /// All elements must be comparable to each other without a
    /// `ClassCastException` being thrown.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `Object` array to be sorted.
    ///
    /// - `start`: the start index to sort.
    ///
    /// - `end`: the last + 1 index to sort.
    ///
    /// - `comparator`: the `Comparator`.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// if elements in the array cannot be compared to each other
    /// using the `Comparator`.
    ///
    /// - `IllegalArgumentException`: if `start > end`.
    ///
    /// - `ArrayIndexOutOfBoundsException`: if `start  array.length`.
    public static <T> void sort(T[] array, int start, int end,
            Comparator<? super T> comparator) {
        if (array == null) {
            throw new NullPointerException();
        }
        checkBounds(array.length, start, end);
        sort(start, end, array, comparator);
    }

    private static <T> void sort(int start, int end, T[] array,
            Comparator<? super T> comparator) {
        if (comparator == null) {
            sort(start, end, array);
        } else {
            int length = end - start;
            Object[] out = new Object[end];
            System.arraycopy(array, start, out, start, length);
            mergeSort(out, array, start, end, comparator);
        }
    }

    /// Sorts the specified array using the specified `Comparator`. All elements
    /// must be comparable to each other without a `ClassCastException` being thrown.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `Object` array to be sorted.
    ///
    /// - `comparator`: the `Comparator`.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// if elements in the array cannot be compared to each other
    /// using the `Comparator`.
    public static <T> void sort(T[] array, Comparator<? super T> comparator) {
        sort(0, array.length, array, comparator);
    }

    /// Sorts the specified array in ascending numerical order.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `short` array to be sorted.
    public static void sort(short[] array) {
        sort(0, array.length, array);
    }

    /// Sorts the specified range in the array in ascending numerical order.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `short` array to be sorted.
    ///
    /// - `start`: the start index to sort.
    ///
    /// - `end`: the last + 1 index to sort.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if `start > end`.
    ///
    /// - `ArrayIndexOutOfBoundsException`: if `start  array.length`.
    public static void sort(short[] array, int start, int end) {
        if (array == null) {
            throw new NullPointerException();
        }
        checkBounds(array.length, start, end);
        sort(start, end, array);
    }

    private static void sort(int start, int end, short[] array) {
        short temp;
        int length = end - start;
        if (length < 7) {
            for (int i = start + 1; i < end; i++) {
                for (int j = i; j > start && array[j - 1] > array[j]; j--) {
                    temp = array[j];
                    array[j] = array[j - 1];
                    array[j - 1] = temp;
                }
            }
            return;
        }
        int middle = (start + end) / 2;
        if (length > 7) {
            int bottom = start;
            int top = end - 1;
            if (length > 40) {
                length /= 8;
                bottom = med3(array, bottom, bottom + length, bottom
                        + (2 * length));
                middle = med3(array, middle - length, middle, middle + length);
                top = med3(array, top - (2 * length), top - length, top);
            }
            middle = med3(array, bottom, middle, top);
        }
        short partionValue = array[middle];
        int a, b, c, d;
        a = b = start;
        c = d = end - 1;
        while (true) {
            while (b <= c && array[b] <= partionValue) {
                if (array[b] == partionValue) {
                    temp = array[a];
                    array[a++] = array[b];
                    array[b] = temp;
                }
                b++;
            }
            while (c >= b && array[c] >= partionValue) {
                if (array[c] == partionValue) {
                    temp = array[c];
                    array[c] = array[d];
                    array[d--] = temp;
                }
                c--;
            }
            if (b > c) {
                break;
            }
            temp = array[b];
            array[b++] = array[c];
            array[c--] = temp;
        }
        length = a - start < b - a ? a - start : b - a;
        int l = start;
        int h = b - length;
        while (length-- > 0) {
            temp = array[l];
            array[l++] = array[h];
            array[h++] = temp;
        }
        length = d - c < end - 1 - d ? d - c : end - 1 - d;
        l = b;
        h = end - length;
        while (length-- > 0) {
            temp = array[l];
            array[l++] = array[h];
            array[h++] = temp;
        }
        if ((length = b - a) > 0) {
            sort(start, start + length, array);
        }
        if ((length = d - c) > 0) {
            sort(end - length, end, array);
        }
    }

    /// Creates a `String` representation of the `boolean[]` passed.
    /// The result is surrounded by brackets (`"[]"`), each
    /// element is converted to a `String` via the
    /// `String#valueOf(boolean)` and separated by `", "`.
    /// If the array is `null`, then `"null"` is returned.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `boolean` array to convert.
    ///
    /// #### Returns
    ///
    /// the `String` representation of `array`.
    ///
    /// #### Since
    ///
    /// 1.5
    public static String toString(boolean[] array) {
        if (array == null) {
            return "null"; //$NON-NLS-1$
        }
        if (array.length == 0) {
            return "[]"; //$NON-NLS-1$
        }
        StringBuffer sb = new StringBuffer(2 + array.length * 5);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", "); //$NON-NLS-1$
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /// Creates a `String` representation of the `byte[]` passed. The
    /// result is surrounded by brackets (`"[]"`), each element
    /// is converted to a `String` via the `String#valueOf(int)` and
    /// separated by `", "`. If the array is `null`, then
    /// `"null"` is returned.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `byte` array to convert.
    ///
    /// #### Returns
    ///
    /// the `String` representation of `array`.
    ///
    /// #### Since
    ///
    /// 1.5
    public static String toString(byte[] array) {
        if (array == null) {
            return "null"; //$NON-NLS-1$
        }
        if (array.length == 0) {
            return "[]"; //$NON-NLS-1$
        }
        StringBuffer sb = new StringBuffer(2 + array.length * 3);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", "); //$NON-NLS-1$
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /// Creates a `String` representation of the `char[]` passed. The
    /// result is surrounded by brackets (`"[]"`), each element
    /// is converted to a `String` via the `String#valueOf(char)` and
    /// separated by `", "`. If the array is `null`, then
    /// `"null"` is returned.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `char` array to convert.
    ///
    /// #### Returns
    ///
    /// the `String` representation of `array`.
    ///
    /// #### Since
    ///
    /// 1.5
    public static String toString(char[] array) {
        if (array == null) {
            return "null"; //$NON-NLS-1$
        }
        if (array.length == 0) {
            return "[]"; //$NON-NLS-1$
        }
        StringBuffer sb = new StringBuffer(2 + array.length * 2);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", "); //$NON-NLS-1$
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /// Creates a `String` representation of the `double[]` passed.
    /// The result is surrounded by brackets (`"[]"`), each
    /// element is converted to a `String` via the
    /// `String#valueOf(double)` and separated by `", "`.
    /// If the array is `null`, then `"null"` is returned.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `double` array to convert.
    ///
    /// #### Returns
    ///
    /// the `String` representation of `array`.
    ///
    /// #### Since
    ///
    /// 1.5
    public static String toString(double[] array) {
        if (array == null) {
            return "null"; //$NON-NLS-1$
        }
        if (array.length == 0) {
            return "[]"; //$NON-NLS-1$
        }
        StringBuffer sb = new StringBuffer(2 + array.length * 5);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", "); //$NON-NLS-1$
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /// Creates a `String` representation of the `float[]` passed.
    /// The result is surrounded by brackets (`"[]"`), each
    /// element is converted to a `String` via the
    /// `String#valueOf(float)` and separated by `", "`.
    /// If the array is `null`, then `"null"` is returned.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `float` array to convert.
    ///
    /// #### Returns
    ///
    /// the `String` representation of `array`.
    ///
    /// #### Since
    ///
    /// 1.5
    public static String toString(float[] array) {
        if (array == null) {
            return "null"; //$NON-NLS-1$
        }
        if (array.length == 0) {
            return "[]"; //$NON-NLS-1$
        }
        StringBuffer sb = new StringBuffer(2 + array.length * 5);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", "); //$NON-NLS-1$
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /// Creates a `String` representation of the `int[]` passed. The
    /// result is surrounded by brackets (`"[]"`), each element
    /// is converted to a `String` via the `String#valueOf(int)` and
    /// separated by `", "`. If the array is `null`, then
    /// `"null"` is returned.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `int` array to convert.
    ///
    /// #### Returns
    ///
    /// the `String` representation of `array`.
    ///
    /// #### Since
    ///
    /// 1.5
    public static String toString(int[] array) {
        if (array == null) {
            return "null"; //$NON-NLS-1$
        }
        if (array.length == 0) {
            return "[]"; //$NON-NLS-1$
        }
        StringBuffer sb = new StringBuffer(2 + array.length * 4);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", "); //$NON-NLS-1$
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /// Creates a `String` representation of the `long[]` passed. The
    /// result is surrounded by brackets (`"[]"`), each element
    /// is converted to a `String` via the `String#valueOf(long)` and
    /// separated by `", "`. If the array is `null`, then
    /// `"null"` is returned.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `long` array to convert.
    ///
    /// #### Returns
    ///
    /// the `String` representation of `array`.
    ///
    /// #### Since
    ///
    /// 1.5
    public static String toString(long[] array) {
        if (array == null) {
            return "null"; //$NON-NLS-1$
        }
        if (array.length == 0) {
            return "[]"; //$NON-NLS-1$
        }
        StringBuffer sb = new StringBuffer(2 + array.length * 4);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", "); //$NON-NLS-1$
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /// Creates a `String` representation of the `short[]` passed.
    /// The result is surrounded by brackets (`"[]"`), each
    /// element is converted to a `String` via the
    /// `String#valueOf(int)` and separated by `", "`. If
    /// the array is `null`, then `"null"` is returned.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `short` array to convert.
    ///
    /// #### Returns
    ///
    /// the `String` representation of `array`.
    ///
    /// #### Since
    ///
    /// 1.5
    public static String toString(short[] array) {
        if (array == null) {
            return "null"; //$NON-NLS-1$
        }
        if (array.length == 0) {
            return "[]"; //$NON-NLS-1$
        }
        StringBuffer sb = new StringBuffer(2 + array.length * 4);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", "); //$NON-NLS-1$
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /// Creates a `String` representation of the `Object[]` passed.
    /// The result is surrounded by brackets (`"[]"`), each
    /// element is converted to a `String` via the
    /// `String#valueOf(Object)` and separated by `", "`.
    /// If the array is `null`, then `"null"` is returned.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `Object` array to convert.
    ///
    /// #### Returns
    ///
    /// the `String` representation of `array`.
    ///
    /// #### Since
    ///
    /// 1.5
    public static String toString(Object[] array) {
        if (array == null) {
            return "null"; //$NON-NLS-1$
        }
        if (array.length == 0) {
            return "[]"; //$NON-NLS-1$
        }
        StringBuffer sb = new StringBuffer(2 + array.length * 5);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(", "); //$NON-NLS-1$
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /// Creates a *"deep"* `String` representation of the
    /// `Object[]` passed, such that if the array contains other arrays,
    /// the `String` representation of those arrays is generated as well.
    ///
    /// If any of the elements are primitive arrays, the generation is delegated
    /// to the other `toString` methods in this class. If any element
    /// contains a reference to the original array, then it will be represented
    /// as `"[...]"`. If an element is an `Object[]`, then its
    /// representation is generated by a recursive call to this method. All other
    /// elements are converted via the `String#valueOf(Object)` method.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `Object` array to convert.
    ///
    /// #### Returns
    ///
    /// the `String` representation of `array`.
    ///
    /// #### Since
    ///
    /// 1.5
    public static String deepToString(Object[] array) {
        // delegate this to the recursive method
        return deepToStringImpl(array, new Object[] { array }, null);
    }

    /// Implementation method used by `#deepToString(Object[])`.
    ///
    /// #### Parameters
    ///
    /// - `array`: the `Object[]` to dive into.
    ///
    /// - `origArrays`: @param origArrays
    /// the original `Object[]`; used to test for self
    /// references.
    ///
    /// - `sb`: @param sb
    /// the `StringBuilder` instance to append to or
    /// `null` one hasn't been created yet.
    ///
    /// #### Returns
    ///
    /// the result.
    ///
    /// #### See also
    ///
    /// - #deepToString(Object[])
    private static String deepToStringImpl(Object[] array, Object[] origArrays,
            StringBuffer sb) {
        if (array == null) {
            return "null"; //$NON-NLS-1$
        }
        if (array.length == 0) {
            return "[]"; //$NON-NLS-1$
        }

        if (sb == null) {
            sb = new StringBuffer(2 + array.length * 5);
        }
        sb.append('[');

        for (int i = 0; i < array.length; i++) {
            if (i != 0) {
                sb.append(", "); //$NON-NLS-1$
            }
            // establish current element
            Object elem = array[i];
            if (elem == null) {
                // element is null
                sb.append("null"); //$NON-NLS-1$
            } else {
                // get the Class of the current element
                Class<?> elemClass = elem.getClass();
                if (elemClass.isArray()) {
                    // element is an array type

                    // get the declared Class of the array (element)
                    //Class<?> elemElemClass = elemClass.getComponentType();
                    // element is an Object[], so we assert that
                    if (deepToStringImplContains(origArrays, elem)) {
                        sb.append("[...]"); //$NON-NLS-1$
                    } else {
                        Object[] newArray = (Object[]) elem;
                        Object[] newOrigArrays = new Object[origArrays.length + 1];
                        System.arraycopy(origArrays, 0, newOrigArrays, 0,
                                origArrays.length);
                        newOrigArrays[origArrays.length] = newArray;
                        // make the recursive call to this method
                        deepToStringImpl(newArray, newOrigArrays, sb);
                    }
                } else { // element is NOT an array, just an Object
                    sb.append(array[i]);
                }
            }
        }
        sb.append(']');
        return sb.toString();
    }

    /// Utility method used to assist the implementation of
    /// `#deepToString(Object[])`.
    ///
    /// #### Parameters
    ///
    /// - `origArrays`: An array of Object[] references.
    ///
    /// - `array`: An Object[] reference to look for in `origArrays`.
    ///
    /// #### Returns
    ///
    /// @return A value of `true` if `array` is an
    /// element in `origArrays`.
    private static boolean deepToStringImplContains(Object[] origArrays,
            Object array) {
        if (origArrays == null || origArrays.length == 0) {
            return false;
        }
        for (Object element : origArrays) {
            if (element == array) {
                return true;
            }
        }
        return false;
    }


    /// Copies elements in original array to a new array, from index
    /// start(inclusive) to end(exclusive). The first element (if any) in the new
    /// array is original[from], and other elements in the new array are in the
    /// original order. The padding value whose index is bigger than or equal to
    /// original.length - start is false.
    ///
    /// #### Parameters
    ///
    /// - `original`: the original array
    ///
    /// - `start`: the start index, inclusive
    ///
    /// - `end`: the end index, exclusive, may bigger than length of the array
    ///
    /// #### Returns
    ///
    /// the new copied array
    ///
    /// #### Throws
    ///
    /// - `ArrayIndexOutOfBoundsException`: if start is smaller than 0 or bigger than original.length
    ///
    /// - `IllegalArgumentException`: if start is bigger than end
    ///
    /// - `NullPointerException`: if original is null
    ///
    /// #### Since
    ///
    /// 1.6
    public static boolean[] copyOfRange(boolean[] original, int start, int end) {
        if (start <= end) {
            if (original.length >= start && 0 <= start) {
                int length = end - start;
                int copyLength = Math.min(length, original.length - start);
                boolean[] copy = new boolean[length];
                System.arraycopy(original, start, copy, 0, copyLength);
                return copy;
            }
            throw new ArrayIndexOutOfBoundsException();
        }
        throw new IllegalArgumentException();
    }

    /// Copies elements in original array to a new array, from index
    /// start(inclusive) to end(exclusive). The first element (if any) in the new
    /// array is original[from], and other elements in the new array are in the
    /// original order. The padding value whose index is bigger than or equal to
    /// original.length - start is (byte)0.
    ///
    /// #### Parameters
    ///
    /// - `original`: the original array
    ///
    /// - `start`: the start index, inclusive
    ///
    /// - `end`: the end index, exclusive, may bigger than length of the array
    ///
    /// #### Returns
    ///
    /// the new copied array
    ///
    /// #### Throws
    ///
    /// - `ArrayIndexOutOfBoundsException`: if start is smaller than 0 or bigger than original.length
    ///
    /// - `IllegalArgumentException`: if start is bigger than end
    ///
    /// - `NullPointerException`: if original is null
    ///
    /// #### Since
    ///
    /// 1.6
    public static byte[] copyOfRange(byte[] original, int start, int end) {
        if (start <= end) {
            if (original.length >= start && 0 <= start) {
                int length = end - start;
                int copyLength = Math.min(length, original.length - start);
                byte[] copy = new byte[length];
                System.arraycopy(original, start, copy, 0, copyLength);
                return copy;
            }
            throw new ArrayIndexOutOfBoundsException();
        }
        throw new IllegalArgumentException();
    }

    /// Copies elements in original array to a new array, from index
    /// start(inclusive) to end(exclusive). The first element (if any) in the new
    /// array is original[from], and other elements in the new array are in the
    /// original order. The padding value whose index is bigger than or equal to
    /// original.length - start is '\\u000'.
    ///
    /// #### Parameters
    ///
    /// - `original`: the original array
    ///
    /// - `start`: the start index, inclusive
    ///
    /// - `end`: the end index, exclusive, may bigger than length of the array
    ///
    /// #### Returns
    ///
    /// the new copied array
    ///
    /// #### Throws
    ///
    /// - `ArrayIndexOutOfBoundsException`: if start is smaller than 0 or bigger than original.length
    ///
    /// - `IllegalArgumentException`: if start is bigger than end
    ///
    /// - `NullPointerException`: if original is null
    ///
    /// #### Since
    ///
    /// 1.6
    public static char[] copyOfRange(char[] original, int start, int end) {
        if (start <= end) {
            if (original.length >= start && 0 <= start) {
                int length = end - start;
                int copyLength = Math.min(length, original.length - start);
                char[] copy = new char[length];
                System.arraycopy(original, start, copy, 0, copyLength);
                return copy;
            }
            throw new ArrayIndexOutOfBoundsException();
        }
        throw new IllegalArgumentException();
    }

    /// Copies elements in original array to a new array, from index
    /// start(inclusive) to end(exclusive). The first element (if any) in the new
    /// array is original[from], and other elements in the new array are in the
    /// original order. The padding value whose index is bigger than or equal to
    /// original.length - start is 0d.
    ///
    /// #### Parameters
    ///
    /// - `original`: the original array
    ///
    /// - `start`: the start index, inclusive
    ///
    /// - `end`: the end index, exclusive, may bigger than length of the array
    ///
    /// #### Returns
    ///
    /// the new copied array
    ///
    /// #### Throws
    ///
    /// - `ArrayIndexOutOfBoundsException`: if start is smaller than 0 or bigger than original.length
    ///
    /// - `IllegalArgumentException`: if start is bigger than end
    ///
    /// - `NullPointerException`: if original is null
    ///
    /// #### Since
    ///
    /// 1.6
    public static double[] copyOfRange(double[] original, int start, int end) {
        if (start <= end) {
            if (original.length >= start && 0 <= start) {
                int length = end - start;
                int copyLength = Math.min(length, original.length - start);
                double[] copy = new double[length];
                System.arraycopy(original, start, copy, 0, copyLength);
                return copy;
            }
            throw new ArrayIndexOutOfBoundsException();
        }
        throw new IllegalArgumentException();
    }

    /// Copies elements in original array to a new array, from index
    /// start(inclusive) to end(exclusive). The first element (if any) in the new
    /// array is original[from], and other elements in the new array are in the
    /// original order. The padding value whose index is bigger than or equal to
    /// original.length - start is 0f.
    ///
    /// #### Parameters
    ///
    /// - `original`: the original array
    ///
    /// - `start`: the start index, inclusive
    ///
    /// - `end`: the end index, exclusive, may bigger than length of the array
    ///
    /// #### Returns
    ///
    /// the new copied array
    ///
    /// #### Throws
    ///
    /// - `ArrayIndexOutOfBoundsException`: if start is smaller than 0 or bigger than original.length
    ///
    /// - `IllegalArgumentException`: if start is bigger than end
    ///
    /// - `NullPointerException`: if original is null
    ///
    /// #### Since
    ///
    /// 1.6
    public static float[] copyOfRange(float[] original, int start, int end) {
        if (start <= end) {
            if (original.length >= start && 0 <= start) {
                int length = end - start;
                int copyLength = Math.min(length, original.length - start);
                float[] copy = new float[length];
                System.arraycopy(original, start, copy, 0, copyLength);
                return copy;
            }
            throw new ArrayIndexOutOfBoundsException();
        }
        throw new IllegalArgumentException();
    }

    /// Copies elements in original array to a new array, from index
    /// start(inclusive) to end(exclusive). The first element (if any) in the new
    /// array is original[from], and other elements in the new array are in the
    /// original order. The padding value whose index is bigger than or equal to
    /// original.length - start is 0.
    ///
    /// #### Parameters
    ///
    /// - `original`: the original array
    ///
    /// - `start`: the start index, inclusive
    ///
    /// - `end`: the end index, exclusive, may bigger than length of the array
    ///
    /// #### Returns
    ///
    /// the new copied array
    ///
    /// #### Throws
    ///
    /// - `ArrayIndexOutOfBoundsException`: if start is smaller than 0 or bigger than original.length
    ///
    /// - `IllegalArgumentException`: if start is bigger than end
    ///
    /// - `NullPointerException`: if original is null
    ///
    /// #### Since
    ///
    /// 1.6
    public static int[] copyOfRange(int[] original, int start, int end) {
        if (start <= end) {
            if (original.length >= start && 0 <= start) {
                int length = end - start;
                int copyLength = Math.min(length, original.length - start);
                int[] copy = new int[length];
                System.arraycopy(original, start, copy, 0, copyLength);
                return copy;
            }
            throw new ArrayIndexOutOfBoundsException();
        }
        throw new IllegalArgumentException();
    }

    /// Copies elements in original array to a new array, from index
    /// start(inclusive) to end(exclusive). The first element (if any) in the new
    /// array is original[from], and other elements in the new array are in the
    /// original order. The padding value whose index is bigger than or equal to
    /// original.length - start is 0L.
    ///
    /// #### Parameters
    ///
    /// - `original`: the original array
    ///
    /// - `start`: the start index, inclusive
    ///
    /// - `end`: the end index, exclusive, may bigger than length of the array
    ///
    /// #### Returns
    ///
    /// the new copied array
    ///
    /// #### Throws
    ///
    /// - `ArrayIndexOutOfBoundsException`: if start is smaller than 0 or bigger than original.length
    ///
    /// - `IllegalArgumentException`: if start is bigger than end
    ///
    /// - `NullPointerException`: if original is null
    ///
    /// #### Since
    ///
    /// 1.6
    public static long[] copyOfRange(long[] original, int start, int end) {
        if (start <= end) {
            if (original.length >= start && 0 <= start) {
                int length = end - start;
                int copyLength = Math.min(length, original.length - start);
                long[] copy = new long[length];
                System.arraycopy(original, start, copy, 0, copyLength);
                return copy;
            }
            throw new ArrayIndexOutOfBoundsException();
        }
        throw new IllegalArgumentException();
    }

    /// Copies elements in original array to a new array, from index
    /// start(inclusive) to end(exclusive). The first element (if any) in the new
    /// array is original[from], and other elements in the new array are in the
    /// original order. The padding value whose index is bigger than or equal to
    /// original.length - start is (short)0.
    ///
    /// #### Parameters
    ///
    /// - `original`: the original array
    ///
    /// - `start`: the start index, inclusive
    ///
    /// - `end`: the end index, exclusive, may bigger than length of the array
    ///
    /// #### Returns
    ///
    /// the new copied array
    ///
    /// #### Throws
    ///
    /// - `ArrayIndexOutOfBoundsException`: if start is smaller than 0 or bigger than original.length
    ///
    /// - `IllegalArgumentException`: if start is bigger than end
    ///
    /// - `NullPointerException`: if original is null
    ///
    /// #### Since
    ///
    /// 1.6
    public static short[] copyOfRange(short[] original, int start, int end) {
        if (start <= end) {
            if (original.length >= start && 0 <= start) {
                int length = end - start;
                int copyLength = Math.min(length, original.length - start);
                short[] copy = new short[length];
                System.arraycopy(original, start, copy, 0, copyLength);
                return copy;
            }
            throw new ArrayIndexOutOfBoundsException();
        }
        throw new IllegalArgumentException();
    }
    
    public static <T,U> T[] copyOfRange(U[] original,
                    int from,
                    int to,
                    Class<? extends T[]> newType) {
        return null;
    }
    
    public static <T> T[] copyOfRange(T[] original,
                  int from,
                  int to) {
        return null;
    }
    
    
    
    public static <T> T[] copyOf(T[] original, int newLength,  Class<? extends T[]> newType) {
        T[] arr = (T[])Array.newInstance(newType.getComponentType(), newLength);
        int len = Math.min(original.length, newLength);
        System.arraycopy(original, 0, arr, 0, len);
        return arr;
    }
    
    public static <T> T[] copyOf(T[] original, int newLength) {
        return copyOf(original, newLength, (Class<T[]>)original.getClass());
    }
    
    static boolean[] copyOf(boolean[] original) {
        return copyOfRange(original, 0, original.length);
    }
    
    public static boolean[] copyOf(boolean[] original, int newlen) {
        return copyOfRange(original, 0, newlen);
    }
    
    static char[] copyOf(char[] original) {
        return copyOfRange(original, 0, original.length);
    }
    
    public static char[] copyOf(char[] original, int newlen) {
        return copyOfRange(original, 0, newlen);
    }
    
    static double[] copyOf(double[] original) {
        return copyOfRange(original, 0, original.length);
    }
    public static double[] copyOf(double[] original, int newlen) {
        return copyOfRange(original, 0, newlen);
    }
    
    static float[] copyOf(float[] original) {
        return copyOfRange(original, 0, original.length);
    }
    public static float[] copyOf(float[] original, int newlen) {
        return copyOfRange(original, 0, newlen);
    }
    
    static long[] copyOf(long[] original) {
        return copyOfRange(original, 0, original.length);
    }
    
    public static long[] copyOf(long[] original, int newlen) {
        return copyOfRange(original, 0, newlen);
    }
    
    static int[] copyOf(int[] original) {
        return copyOfRange(original, 0, original.length);
    }
    
    public static int[] copyOf(int[] original, int newlen) {
        return copyOfRange(original, 0, newlen);
    }
    
    static byte[] copyOf(byte[] original) {
        return copyOfRange(original, 0, original.length);
    }
    
    public static byte[] copyOf(byte[] original, int newlen) {
        return copyOfRange(original, 0, newlen);
    }
    
    static short[] copyOf(short[] original) {
        return copyOfRange(original, 0, original.length);
    }
    
    public static short[] copyOf(short[] original, int len) {
        return copyOfRange(original, 0, len);
    }
   
}
