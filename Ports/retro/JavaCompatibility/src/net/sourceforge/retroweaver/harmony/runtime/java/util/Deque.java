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

package net.sourceforge.retroweaver.harmony.runtime.java.util;

/**
 * A kind of collection that can insert or remove element at both ends("double
 * ended queue"). Mostly a deque has no limit of its size.
 * 
 * Extending from Queue, a deque can be used as a Queue which behavior is
 * first-in-first-out. Furthermore, a deque can also be used as a Stack(legacy
 * class) which behavior is last-in-first-out.
 * 
 * A typical deque does not allow null to be inserted as its element, while some
 * implementations allow it. But null should not be inserted even in these
 * implementations, since method poll return null to indicate that there is no
 * element left in the deque.
 * 
 * A deque can also remove interior elements by removeFirstOccurrence and
 * removeLastOccurrence methods. A deque can not access elements by index.
 * 
 * @param <E>
 *            the type of elements in this collection
 * @since 1.6
 */
public interface Deque<E> extends Queue<E> {

    /**
     * Inserts an element at the head of this deque if it dose not violate size
     * limit immediately. It is better to use offerFirst(E) if a deque is
     * size-limited.
     * 
     * @param e
     *            the element
     * @throws IllegalStateException
     *             if it can not add now due to size limit
     * @throws ClassCastException
     *             if the class of element can not be added into this deque
     * @throws NullPointerException
     *             if the element is null and the deque can not contain null
     *             element
     * @throws IllegalArgumentException
     *             if the element can not be added due to some property.
     */
    void addFirst(E e);

    /**
     * Inserts an element at the tail of this deque if it dose not violate size
     * limit immediately. It is better to use offerLast(E) if a deque is
     * size-limited.
     * 
     * @param e
     *            the element
     * @throws IllegalStateException
     *             if it can not add now due to size limit
     * @throws ClassCastException
     *             if the class of element can not be added into this deque
     * @throws NullPointerException
     *             if the element is null and the deque can not contain null
     *             element
     * @throws IllegalArgumentException
     *             if the element can not be added due to some property.
     */
    void addLast(E e);

    /**
     * Inserts an element at the head of this deque unless it would violate size
     * limit. It is better than the addFirst(E) method in a size-limited deque,
     * because the latter one may fail to add the element only by throwing an
     * exception.
     * 
     * @param e
     *            the element
     * @return true if the operation succeeds or false if it fails.
     * @throws ClassCastException
     *             if the class of element can not be added into this deque
     * @throws NullPointerException
     *             if the element is null and the deque can not contain null
     *             element
     * @throws IllegalArgumentException
     *             if the element can not be added due to some property.
     */
    boolean offerFirst(E e);

    /**
     * Inserts an element at the tail of this deque unless it would violate size
     * limit. It is better than the addLast(E) method in a size-limited deque,
     * because the latter one may fail to add the element only by throwing an
     * exception.
     * 
     * @param e
     *            the element
     * @return true if the operation succeeds or false if it fails
     * @throws ClassCastException
     *             if the class of element can not be added into this deque
     * @throws NullPointerException
     *             if the element is null and the deque can not contain null
     *             element
     * @throws IllegalArgumentException
     *             if the element can not be added due to some property
     */
    boolean offerLast(E e);

    /**
     * Gets and removes the head element of this deque. This method throws an
     * exception if the deque is empty.
     * 
     * @return the head element
     * @throws NoSuchElementException
     *             if the deque is empty
     */
    E removeFirst();

    /**
     * Gets and removes the tail element of this deque. This method throws an
     * exception if the deque is empty.
     * 
     * @return the tail element
     * @throws NoSuchElementException
     *             if the deque is empty
     */
    E removeLast();

    /**
     * Gets and removes the head element of this deque. This method returns null
     * if the deque is empty.
     * 
     * @return the head element or null if the deque is empty
     */
    E pollFirst();

    /**
     * Gets and removes the tail element of this deque. This method returns null
     * if the deque is empty.
     * 
     * @return the tail element or null if the deque is empty
     */
    E pollLast();

    /**
     * Gets but not removes the head element of this deque. This method throws
     * an exception if the deque is empty.
     * 
     * @return the head element
     * @throws NoSuchElementException
     *             if the deque is empty
     */
    E getFirst();

    /**
     * Gets but not removes the tail element of this deque. This method throws
     * an exception if the deque is empty.
     * 
     * @return the tail element
     * @throws NoSuchElementException
     *             if the deque is empty
     */
    E getLast();

    /**
     * Gets but not removes the head element of this deque. This method returns
     * null if the deque is empty.
     * 
     * @return the head element or null if the deque is empty
     */
    E peekFirst();

    /**
     * Gets but not removes the tail element of this deque. This method returns
     * null if the deque is empty.
     * 
     * @return the tail element or null if the deque is empty
     */
    E peekLast();

    /**
     * Removes the first equivalent element of the specified object. If the
     * deque does not contain the element, it is unchanged and returns false.
     * 
     * @param o
     *            the element to be removed
     * @return true if the operation succeeds or false if the deque does not
     *         contain the element.
     * @throws ClassCastException
     *             if the class of the element is incompatible with the deque
     * @throws NullPointerException
     *             if the element is null and the deque can not contain null
     *             element
     */
    boolean removeFirstOccurrence(Object o);

    /**
     * Removes the last equivalent element of the specified object. If the deque
     * does not contain the element, it is unchanged and returns false.
     * 
     * @param o
     *            the element to be removed
     * @return true if the operation succeeds or false if the deque does not
     *         contain the element.
     * @throws ClassCastException
     *             if the class of the element is incompatible with the deque
     * @throws NullPointerException
     *             if the element is null and the deque can not contain null
     *             element
     */
    boolean removeLastOccurrence(Object o);

    /**
     * Pushes the element to the deque(at the head of the deque), just same as
     * addFirst(E).
     * 
     * @param e
     *            the element
     * @throws IllegalStateException
     *             if it can not add now due to size limit
     * @throws ClassCastException
     *             if the class of element can not be added into this deque
     * @throws NullPointerException
     *             if the element is null and the deque can not contain null
     *             element
     * @throws IllegalArgumentException
     *             if the element can not be added due to some property.
     */
    void push(E e);

    /**
     * Pops the head element of the deque, just same as removeFirst().
     * 
     * @return the head element
     * @throws NoSuchElementException
     *             if the deque is empty
     */
    E pop();

    /**
     * Returns the iterator in reverse order, from tail to head.
     * 
     * @return the iterator in reverse order
     */
    Iterator<E> descendingIterator();
}
