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
 * NavigableSet is a SortedSet with navigation methods answering the closest
 * matches for specified item.
 * 
 * @param <E>
 *            the type of element
 * @since 1.6
 */
public interface NavigableSet<E> extends SortedSet<E> {

    /**
     * Deletes and answers the smallest element, or null if the set is empty.
     * 
     * @return the smallest element, or null if the set is empty
     */
    E pollFirst();

    /**
     * Deletes and answers the biggest element, or null if the set is empty.
     * 
     * @return the biggest element, or null if the set is empty
     */
    E pollLast();

    /**
     * Answers the smallest element bigger than the specified one, or null if no
     * such element.
     * 
     * @param e
     *            the specified element
     * @return the smallest element bigger than the specified one, or null if no
     *         such element
     * @throws ClassCastException
     *             if the element cannot be compared with the ones in the set
     * @throws NullPointerException
     *             if the element is null and the set can not contain null
     */
    E higher(E e);

    /**
     * Answers the smallest element bigger than or equal to the specified one,
     * or null if no such element.
     * 
     * @param e
     *            the specified element
     * @return the smallest element bigger than or equal to the specified one,
     *         or null if no such element
     * @throws ClassCastException
     *             if the element cannot be compared with the ones in the set
     * @throws NullPointerException
     *             if the element is null and the set can not contain null
     */
    E ceiling(E e);

    /**
     * Answers the biggest element less than the specified one, or null if no
     * such element.
     * 
     * @param e
     *            the specified element
     * @return the biggest element less than the specified one, or null if no
     *         such element
     * @throws ClassCastException
     *             if the element cannot be compared with the ones in the set
     * @throws NullPointerException
     *             if the element is null and the set can not contain null
     */
    E lower(E e);

    /**
     * Answers the biggest element less than or equal to the specified one, or
     * null if no such element.
     * 
     * @param e
     *            the specified element
     * @return the biggest element less than or equal to the specified one, or
     *         null if no such element
     * @throws ClassCastException
     *             if the element cannot be compared with the ones in the set
     * @throws NullPointerException
     *             if the element is null and the set can not contain null
     */
    E floor(E e);

    /**
     * Answers a descending iterator of this set.
     * 
     * @return the descending iterator
     */
    Iterator<E> descendingIterator();

    /**
     * Answers a reverse order view of this set.
     * 
     * @return the reverse order view
     */
    NavigableSet<E> descendingSet();

    /**
     * Answers a NavigableSet of the specified portion of this set which
     * contains elements greater (or equal to, depends on startInclusive) the
     * start element but less than (or equal to, depends on endInclusive) the
     * end element. The returned NavigableSet is backed by this set so changes
     * to one are reflected by the other.
     * 
     * @param start
     *            the start element
     * @param startInclusive
     *            true if the start element is in the returned set
     * @param end
     *            the end element
     * @param endInclusive
     *            true if the end element is in the returned set
     * @return the subset
     * 
     * @throws ClassCastException
     *             when the start or end object cannot be compared with the
     *             elements in this set
     * @throws NullPointerException
     *             when the start or end object is null and the set cannot
     *             contain null
     * @throws IllegalArgumentException
     *             when the start is bigger than end; or start or end is out of
     *             range and the set has a range
     */
    NavigableSet<E> subSet(E start, boolean startInclusive, E end,
            boolean endInclusive);

    /**
     * Answers a NavigableSet of the specified portion of this set which
     * contains elements less than (or equal to, depends on endInclusive) the
     * end element. The returned NavigableSet is backed by this set so changes
     * to one are reflected by the other.
     * 
     * @param end
     *            the end element
     * @param endInclusive
     *            true if the end element is in the returned set
     * @return the subset
     * 
     * @throws ClassCastException
     *             when the end object cannot be compared with the elements in
     *             this set
     * @throws NullPointerException
     *             when the end object is null and the set cannot contain handle
     *             null
     * @throws IllegalArgumentException
     *             when end is out of range and the set has a range
     */
    NavigableSet<E> headSet(E end, boolean endInclusive);

    /**
     * Answers a NavigableSet of the specified portion of this set which
     * contains elements greater (or equal to, depends on startInclusive) the
     * start element. The returned NavigableSet is backed by this set so changes
     * to one are reflected by the other.
     * 
     * @param start
     *            the start element
     * @param startInclusive
     *            true if the start element is in the returned set
     * @return the subset
     * 
     * @throws ClassCastException
     *             when the start object cannot be compared with the elements in
     *             this set
     * @throws NullPointerException
     *             when the start object is null and the set cannot contain null
     * @throws IllegalArgumentException
     *             when start is out of range and the set has a range
     */
    NavigableSet<E> tailSet(E start, boolean startInclusive);
}
