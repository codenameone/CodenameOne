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
 * ArrayList is an implementation of {@link List}, backed by an array. All
 * optional operations adding, removing, and replacing are supported. The
 * elements can be any objects.
 * 
 * @since 1.2
 */
public class ArrayList<E> extends AbstractList<E> implements List<E>, RandomAccess {

    private static final long serialVersionUID = 8683452581122892189L;

    private transient int firstIndex;

    private transient int size;

    private transient E[] array;

    /**
     * Constructs a new instance of {@code ArrayList} with ten capacity.
     */
    public ArrayList() {
        this(10);
    }

    /**
     * Constructs a new instance of {@code ArrayList} with the specified
     * capacity.
     * 
     * @param capacity
     *            the initial capacity of this {@code ArrayList}.
     */
    public ArrayList(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException();
        }
        firstIndex = size = 0;
        array = newElementArray(capacity);
    }

    /**
     * Constructs a new instance of {@code ArrayList} containing the elements of
     * the specified collection. The initial size of the {@code ArrayList} will
     * be 10% larger than the size of the specified collection.
     * 
     * @param collection
     *            the collection of elements to add.
     */
    public ArrayList(Collection<? extends E> collection) {
        firstIndex = 0;
        Object[] objects = toObjectArray(collection);
        size = objects.length;

        // REVIEW: Created 2 array copies of the original collection here
        //         Could be better to use the collection iterator and
        //         copy once?
        array = newElementArray(size + (size / 10));
        System.arraycopy(objects, 0, array, 0, size);
        modCount = 1;
    }

    static void toObjectArray(Object[] objects, Collection collection) {
        Iterator i = collection.iterator();
        for(int iter = 0 ; iter < objects.length ; iter++) {
            objects[iter] = i.next();
        }
    }
    
    static Object[] toObjectArray(Collection collection) {
        Object[] objects = new Object[collection.size()];
        toObjectArray(objects, collection);
        return objects;
    }
    
    @SuppressWarnings("unchecked")
    private E[] newElementArray(int size) {
        return (E[]) new Object[size];
    }

    /**
     * Inserts the specified object into this {@code ArrayList} at the specified
     * location. The object is inserted before any previous element at the
     * specified location. If the location is equal to the size of this
     * {@code ArrayList}, the object is added at the end.
     * 
     * @param location
     *            the index at which to insert the object.
     * @param object
     *            the object to add.
     * @throws IndexOutOfBoundsException
     *             when {@code location < 0 || > size()}
     */
    @Override
    public void add(int location, E object) {
        if (location < 0 || location > size) {
            throw new IndexOutOfBoundsException("" + location + " out of: " + size);
        }
        if (location == 0) {
            if (firstIndex == 0) {
                growAtFront(1);
            }
            array[--firstIndex] = object;
        } else if (location == size) {
            if (firstIndex + size == array.length) {
                growAtEnd(1);
            }
            array[firstIndex + size] = object;
        } else { // must be case: (0 < location && location < size)
            if (size == array.length) {
                growForInsert(location, 1);
            } else if (firstIndex + size == array.length
                    || (firstIndex > 0 && location < size / 2)) {
                System.arraycopy(array, firstIndex, array, --firstIndex,
                        location);
            } else {
                int index = location + firstIndex;
                System.arraycopy(array, index, array, index + 1, size
                        - location);
            }
            array[location + firstIndex] = object;
        }

        size++;
        modCount++;
    }

    /**
     * Adds the specified object at the end of this {@code ArrayList}.
     * 
     * @param object
     *            the object to add.
     * @return always true
     */
    @Override
    public boolean add(E object) {
        if (firstIndex + size == array.length) {
            growAtEnd(1);
        }
        array[firstIndex + size] = object;
        size++;
        modCount++;
        return true;
    }

    /**
     * Inserts the objects in the specified collection at the specified location
     * in this List. The objects are added in the order they are returned from
     * the collection's iterator.
     * 
     * @param location
     *            the index at which to insert.
     * @param collection
     *            the collection of objects.
     * @return {@code true} if this {@code ArrayList} is modified, {@code false}
     *         otherwise.
     * @throws IndexOutOfBoundsException
     *             when {@code location < 0 || > size()}
     */
    @Override
    public boolean addAll(int location, Collection<? extends E> collection) {
        if (location < 0 || location > size) {
            throw new IndexOutOfBoundsException("" + location + " out of: " + size);
        }

        Object[] dumparray = toObjectArray(collection);
        int growSize = dumparray.length;
        // REVIEW: Why do this check here rather than check
        //         collection.size() earlier? RI behaviour?
        if (growSize == 0) {
            return false;
        }

        if (location == 0) {
            growAtFront(growSize);
            firstIndex -= growSize;
        } else if (location == size) {
            if (firstIndex + size > array.length - growSize) {
                growAtEnd(growSize);
            }
        } else { // must be case: (0 < location && location < size)
            if (array.length - size < growSize) {
                growForInsert(location, growSize);
            } else if (firstIndex + size > array.length - growSize
                       || (firstIndex > 0 && location < size / 2)) {
                int newFirst = firstIndex - growSize;
                if (newFirst < 0) {
                    int index = location + firstIndex;
                    System.arraycopy(array, index, array, index - newFirst,
                            size - location);
                    newFirst = 0;
                }
                System.arraycopy(array, firstIndex, array, newFirst, location);
                firstIndex = newFirst;
            } else {
                int index = location + firstIndex;
                System.arraycopy(array, index, array, index + growSize, size
                        - location);
            }
        }

        System.arraycopy(dumparray, 0, this.array, location + firstIndex,
                growSize);
        size += growSize;
        modCount++;
        return true;
    }

    /**
     * Adds the objects in the specified collection to this {@code ArrayList}.
     * 
     * @param collection
     *            the collection of objects.
     * @return {@code true} if this {@code ArrayList} is modified, {@code false}
     *         otherwise.
     */
    @Override
    public boolean addAll(Collection<? extends E> collection) {
        Object[] dumpArray = toObjectArray(collection);
        if (dumpArray.length == 0) {
            return false;
        }
        if (dumpArray.length > array.length - (firstIndex + size)) {
            growAtEnd(dumpArray.length);
        }
        System.arraycopy(dumpArray, 0, this.array, firstIndex + size,
                         dumpArray.length);
        size += dumpArray.length;
        modCount++;
        return true;
    }

    /**
     * Removes all elements from this {@code ArrayList}, leaving it empty.
     * 
     * @see #isEmpty
     * @see #size
     */
    @Override
    public void clear() {
        if (size != 0) {
            // REVIEW: Should we use Arrays.fill() instead of just
            //         allocating a new array?  Should we use the same
            //         sized array?
            Arrays.fill(array, firstIndex, firstIndex + size, null);
            // REVIEW: Should the indexes point into the middle of the
            //         array rather than 0?
            firstIndex = size = 0;
            modCount++;
        }
    }

    /**
     * Searches this {@code ArrayList} for the specified object.
     * 
     * @param object
     *            the object to search for.
     * @return {@code true} if {@code object} is an element of this
     *         {@code ArrayList}, {@code false} otherwise
     */
    @Override
    public boolean contains(Object object) {
        int lastIndex = firstIndex + size;
        if (object != null) {
            for (int i = firstIndex; i < lastIndex; i++) {
                if (object.equals(array[i])) {
                    return true;
                }
            }
        } else {
            for (int i = firstIndex; i < lastIndex; i++) {
                if (array[i] == null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Ensures that after this operation the {@code ArrayList} can hold the
     * specified number of elements without further growing.
     * 
     * @param minimumCapacity
     *            the minimum capacity asked for.
     */
    public void ensureCapacity(int minimumCapacity) {
        int required = minimumCapacity - array.length;
        if (required > 0) {
            // REVIEW: Why do we check the firstIndex first? Growing
            //         the end makes more sense
            if (firstIndex > 0) {
                growAtFront(required);
            } else {
                growAtEnd(required);
            }
        }
    }

    @Override
    public E get(int location) {
        if (location < 0 || location >= size) {
            throw new IndexOutOfBoundsException("" + location + " out of: " + size);
        }
        return array[firstIndex + location];
    }

    private void growAtEnd(int required) {
        if (array.length - size >= required) {
            // REVIEW: as growAtEnd, why not move size == 0 out as
            //         special case
            if (size != 0) {
                System.arraycopy(array, firstIndex, array, 0, size);
                int start = size < firstIndex ? firstIndex : size;
                // REVIEW: I think we null too much
                //         array.length should be lastIndex ?
                Arrays.fill(array, start, array.length, null);
            }
            firstIndex = 0;
        } else {
            // REVIEW: If size is 0?
            //         Does size/2 seems a little high!
            int increment = size / 2;
            if (required > increment) {
                increment = required;
            }
            if (increment < 12) {
                increment = 12;
            }
            E[] newArray = newElementArray(size + increment);
            if (size != 0) {
                System.arraycopy(array, firstIndex, newArray, 0, size);
                firstIndex = 0;
            }
            array = newArray;
        }
    }

    private void growAtFront(int required) {
        if (array.length - size >= required) {
            int newFirst = array.length - size;
            // REVIEW: as growAtEnd, why not move size == 0 out as
            //         special case
            if (size != 0) {
                System.arraycopy(array, firstIndex, array, newFirst, size);
                int lastIndex = firstIndex + size;
                int length = lastIndex > newFirst ? newFirst : lastIndex;
                Arrays.fill(array, firstIndex, length, null);
            }
            firstIndex = newFirst;
        } else {
            int increment = size / 2;
            if (required > increment) {
                increment = required;
            }
            if (increment < 12) {
                increment = 12;
            }
            E[] newArray = newElementArray(size + increment);
            if (size != 0) {
                System.arraycopy(array, firstIndex, newArray, increment, size);
            }
            firstIndex = newArray.length - size;
            array = newArray;
        }
    }

    private void growForInsert(int location, int required) {
        // REVIEW: we grow too quickly because we are called with the
        //         size of the new collection to add without taking in
        //         to account the free space we already have
        int increment = size / 2;
        if (required > increment) {
            increment = required;
        }
        if (increment < 12) {
            increment = 12;
        }
        E[] newArray = newElementArray(size + increment);
        // REVIEW: biased towards leaving space at the beginning?
        //         perhaps newFirst should be (increment-required)/2?
        int newFirst = increment - required;
        // Copy elements after location to the new array skipping inserted
        // elements
        System.arraycopy(array, location + firstIndex, newArray, newFirst
                + location + required, size - location);
        // Copy elements before location to the new array from firstIndex
        System.arraycopy(array, firstIndex, newArray, newFirst, location);
        firstIndex = newFirst;
        array = newArray;
    }

    @Override
    public int indexOf(Object object) {
        // REVIEW: should contains call this method?
        int lastIndex = firstIndex + size;
        if (object != null) {
            for (int i = firstIndex; i < lastIndex; i++) {
                if (object.equals(array[i])) {
                    return i - firstIndex;
                }
            }
        } else {
            for (int i = firstIndex; i < lastIndex; i++) {
                if (array[i] == null) {
                    return i - firstIndex;
                }
            }
        }
        return -1;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int lastIndexOf(Object object) {
        int lastIndex = firstIndex + size;
        if (object != null) {
            for (int i = lastIndex - 1; i >= firstIndex; i--) {
                if (object.equals(array[i])) {
                    return i - firstIndex;
                }
            }
        } else {
            for (int i = lastIndex - 1; i >= firstIndex; i--) {
                if (array[i] == null) {
                    return i - firstIndex;
                }
            }
        }
        return -1;
    }

    /**
     * Removes the object at the specified location from this list.
     * 
     * @param location
     *            the index of the object to remove.
     * @return the removed object.
     * @throws IndexOutOfBoundsException
     *             when {@code location < 0 || >= size()}
     */
    @Override
    public E remove(int location) {
        E result;
        if (location < 0 || location >= size) {
            throw new IndexOutOfBoundsException("" + location + " out of: " + size);
        }
        if (location == 0) {
            result = array[firstIndex];
            array[firstIndex++] = null;
        } else if (location == size - 1) {
            int lastIndex = firstIndex + size - 1;
            result = array[lastIndex];
            array[lastIndex] = null;
        } else {
            int elementIndex = firstIndex + location;
            result = array[elementIndex];
            if (location < size / 2) {
                System.arraycopy(array, firstIndex, array, firstIndex + 1,
                                 location);
                array[firstIndex++] = null;
            } else {
                System.arraycopy(array, elementIndex + 1, array,
                                 elementIndex, size - location - 1);
                array[firstIndex+size-1] = null;
            }
        }
        size--;

        // REVIEW: we can move this to the first if case since it
        //         can only occur when size==1
        if (size == 0) {
            firstIndex = 0;
        }

        modCount++;
        return result;
    }

    @Override
    public boolean remove(Object object) {
        int location = indexOf(object);
        if (location >= 0) {
            remove(location);
            return true;
        }
        return false;
    }

    /**
     * Removes the objects in the specified range from the start to the end, but
     * not including the end index.
     * 
     * @param start
     *            the index at which to start removing.
     * @param end
     *            the index one after the end of the range to remove.
     * @throws IndexOutOfBoundsException
     *             when {@code start < 0, start > end} or {@code end > size()}
     */
    @Override
    protected void removeRange(int start, int end) {
        // REVIEW: does RI call this from remove(location)
        if (start < 0) {
            throw new IndexOutOfBoundsException("" + start);
        } else if (end > size) {
            throw new IndexOutOfBoundsException("" + end + " out of: " + size);
        } else if (start > end) {
            throw new IndexOutOfBoundsException("" + start + " out of: " + end);
        }

        if (start == end) {
            return;
        }
        if (end == size) {
            Arrays.fill(array, firstIndex + start, firstIndex + size, null);
        } else if (start == 0) {
            Arrays.fill(array, firstIndex, firstIndex + end, null);
            firstIndex += end;
        } else {
            // REVIEW: should this optimize to do the smallest copy?
            System.arraycopy(array, firstIndex + end, array, firstIndex
                             + start, size - end);
            int lastIndex = firstIndex + size;
            int newLast = lastIndex + start - end;
            Arrays.fill(array, newLast, lastIndex, null);
        }
        size -= end - start;
        modCount++;
    }

    /**
     * Replaces the element at the specified location in this {@code ArrayList}
     * with the specified object.
     * 
     * @param location
     *            the index at which to put the specified object.
     * @param object
     *            the object to add.
     * @return the previous element at the index.
     * @throws IndexOutOfBoundsException
     *             when {@code location < 0 || >= size()}
     */
    @Override
    public E set(int location, E object) {
        if (location < 0 || location >= size) {
            throw new IndexOutOfBoundsException("" + location + " out of: " + size);
        }
        E result = array[firstIndex + location];
        array[firstIndex + location] = object;
        return result;
    }

    /**
     * Returns the number of elements in this {@code ArrayList}.
     * 
     * @return the number of elements in this {@code ArrayList}.
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Returns a new array containing all elements contained in this
     * {@code ArrayList}.
     * 
     * @return an array of the elements from this {@code ArrayList}
     */
    @Override
    public Object[] toArray() {
        Object[] result = new Object[size];
        System.arraycopy(array, firstIndex, result, 0, size);
        return result;
    }

    /**
     * Returns an array containing all elements contained in this
     * {@code ArrayList}. If the specified array is large enough to hold the
     * elements, the specified array is used, otherwise an array of the same
     * type is created. If the specified array is used and is larger than this
     * {@code ArrayList}, the array element following the collection elements
     * is set to null.
     * 
     * @param contents
     *            the array.
     * @return an array of the elements from this {@code ArrayList}.
     * @throws ArrayStoreException
     *             when the type of an element in this {@code ArrayList} cannot
     *             be stored in the type of the specified array.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] contents) {
        Object[] arr = contents;
        if (size > arr.length) {
            arr = new Object[size];
        }
        System.arraycopy(array, firstIndex, arr, 0, size);
        if (size < arr.length) {
            // REVIEW: do we use this incorrectly - i.e. do we null
            //         the rest out?
            arr[size] = null;
        }
        return (T[])arr;
    }

    /**
     * Sets the capacity of this {@code ArrayList} to be the same as the current
     * size.
     * 
     * @see #size
     */
    public void trimToSize() {
        E[] newArray = newElementArray(size);
        System.arraycopy(array, firstIndex, newArray, 0, size);
        array = newArray;
        firstIndex = 0;
        modCount = 0;
    }
}
