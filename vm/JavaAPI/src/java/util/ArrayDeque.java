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
 * An implementation of Deque, backed by an array.
 * 
 * ArrayDeques have no size limit, can not contain null element, and they are
 * not thread-safe.
 * 
 * All optional operations are supported, and the elements can be any objects.
 * 
 * @param <E>
 *            the type of elements in this collection
 * 
 * @since 1.6
 */
public class ArrayDeque<E> extends AbstractCollection<E> implements Deque<E> {

    private static final int DEFAULT_SIZE = 16;

    private enum DequeStatus {
        Empty, Normal, Full;
    }

    private transient DequeStatus status;

    private transient int modCount;

    // the pointer of the head element
    private transient int front;

    // the pointer of the "next" position of the tail element
    private transient int rear;

    private transient E[] elements;

    @SuppressWarnings("hiding")
    private class ArrayDequeIterator<E> implements Iterator<E> {
        private int pos;

        private final int expectedModCount;

        private boolean canRemove;

        @SuppressWarnings("synthetic-access")
        ArrayDequeIterator() {
            super();
            pos = front;
            expectedModCount = modCount;
            canRemove = false;
        }

        @SuppressWarnings("synthetic-access")
        public boolean hasNext() {
            if (expectedModCount != modCount) {
                return false;
            }
            return hasNextInternal();
        }

        private boolean hasNextInternal() {
            // canRemove means "next" method is called, and the Full
            // status can ensure that this method is not called just
            // after "remove" method is call.(so, canRemove can keep
            // true after "next" method called)
            return (pos != rear)
                    || ((status == DequeStatus.Full) && !canRemove);
        }

        @SuppressWarnings( { "synthetic-access", "unchecked" })
        public E next() {
            if (hasNextInternal()) {
                E result = (E) elements[pos];
                if (expectedModCount == modCount && null != result) {
                    canRemove = true;
                    pos = circularBiggerPos(pos);
                    return result;
                }
                throw new ConcurrentModificationException();
            }
            throw new NoSuchElementException();
        }

        @SuppressWarnings("synthetic-access")
        public void remove() {
            if (canRemove) {
                int removedPos = circularSmallerPos(pos);
                if (expectedModCount == modCount
                        && null != elements[removedPos]) {
                    removeInternal(removedPos, true);
                    canRemove = false;
                    return;
                }
                throw new ConcurrentModificationException();
            }
            throw new IllegalStateException();
        }
    }

    /*
     * NOTES:descendingIterator is not fail-fast, according to the documentation
     * and test case.
     */
    @SuppressWarnings("hiding")
    private class ReverseArrayDequeIterator<E> implements Iterator<E> {
        private int pos;

        private final int expectedModCount;

        private boolean canRemove;

        @SuppressWarnings("synthetic-access")
        ReverseArrayDequeIterator() {
            super();
            expectedModCount = modCount;
            pos = circularSmallerPos(rear);
            canRemove = false;
        }

        @SuppressWarnings("synthetic-access")
        public boolean hasNext() {
            if (expectedModCount != modCount) {
                return false;
            }
            return hasNextInternal();
        }

        private boolean hasNextInternal() {
            // canRemove means "next" method is called, and the Full
            // status can ensure that this method is not called just
            // after "remove" method is call.(so, canRemove can keep
            // true after "next" method called)
            return (circularBiggerPos(pos) != front)
                    || ((status == DequeStatus.Full) && !canRemove);
        }

        @SuppressWarnings( { "synthetic-access", "unchecked" })
        public E next() {
            if (hasNextInternal()) {
                E result = (E) elements[pos];
                canRemove = true;
                pos = circularSmallerPos(pos);
                return result;
            }
            throw new NoSuchElementException();
        }

        @SuppressWarnings("synthetic-access")
        public void remove() {
            if (canRemove) {
                removeInternal(circularBiggerPos(pos), false);
                canRemove = false;
                return;
            }
            throw new IllegalStateException();
        }
    }

    /**
     * Constructs a new empty instance of ArrayDeque big enough for 16 elements.
     */
    public ArrayDeque() {
        this(DEFAULT_SIZE);
    }

    /**
     * Constructs a new empty instance of ArrayDeque big enough for specified
     * number of elements.
     * 
     * @param minSize
     *            the smallest size of the ArrayDeque
     */
    @SuppressWarnings("unchecked")
    public ArrayDeque(final int minSize) {
        int size = countInitSize(minSize);
        elements = (E[]) new Object[size];
        front = rear = 0;
        status = DequeStatus.Empty;
        modCount = 0;
    }

    /*
     * count out the size for a new deque, and ensure that size >= minSize
     */
    private int countInitSize(final int minSize) {
        return Math.max(minSize, DEFAULT_SIZE);
    }

    /**
     * Constructs a new instance of ArrayDeque containing the elements of the
     * specified collection, with the order returned by the collection's
     * iterator.
     * 
     * @param c
     *            the source of the elements
     * @throws NullPointerException
     *             if the collection is null
     */
    @SuppressWarnings("unchecked")
    public ArrayDeque(Collection<? extends E> c) {
        elements = (E[]) new Object[countInitSize(c.size())];
        front = rear = 0;
        status = DequeStatus.Empty;
        modCount = 0;
        Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            addLastImpl(it.next());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @param e
     *            the element
     * @throws NullPointerException
     *             if the element is null
     * @see java.util.Deque#addFirst(java.lang.Object)
     */
    public void addFirst(E e) {
        offerFirst(e);
    }

    /**
     * {@inheritDoc}
     * 
     * @param e
     *            the element
     * @throws NullPointerException
     *             if the element is null
     * @see java.util.Deque#addLast(java.lang.Object)
     */
    public void addLast(E e) {
        addLastImpl(e);
    }

    /**
     * {@inheritDoc}
     * 
     * @param e
     *            the element
     * @return true
     * @throws NullPointerException
     *             if the element is null
     * @see java.util.Deque#offerFirst(java.lang.Object)
     */
    public  boolean offerFirst(E e) {
        checkNull(e);
        checkAndExpand();
        front = circularSmallerPos(front);
        elements[front] = e;
        resetStatus(true);
        modCount++;
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @param e
     *            the element
     * @return true if the operation succeeds or false if it fails
     * @throws NullPointerException
     *             if the element is null
     * @see java.util.Deque#offerLast(java.lang.Object)
     */
    public boolean offerLast(E e) {
        return addLastImpl(e);
    }

    /**
     * Inserts the element at the tail of the deque.
     * 
     * @param e
     *            the element
     * @return true if the operation succeeds or false if it fails.
     * @throws NullPointerException
     *             if the element is null
     * @see java.util.Queue#offer(java.lang.Object)
     */
    public boolean offer(E e) {
        return addLastImpl(e);
    }

    /**
     * Inserts the element to the tail of the deque.
     * 
     * @param e
     *            the element
     * @return true
     * @see java.util.AbstractCollection#add(java.lang.Object)
     */
    @Override
    public boolean add(E e) {
        return addLastImpl(e);
    }

    /**
     * {@inheritDoc}
     * 
     * @param e
     *            the element to push
     * @throws NullPointerException
     *             if the element is null
     * @see java.util.Deque#push(java.lang.Object)
     */
    public void push(E e) {
        offerFirst(e);
    }

    /**
     * {@inheritDoc}
     * 
     * @return the head element
     * @throws NoSuchElementException
     *             if the deque is empty
     * @see java.util.Deque#removeFirst()
     */
    public  E removeFirst() {
        checkEmpty();
        return removePollFirstImpl();
    }

    /**
     * Gets and removes the head element of this deque. This method throws an
     * exception if the deque is empty.
     * 
     * @return the head element
     * @throws NoSuchElementException
     *             if the deque is empty
     * @see java.util.Queue#remove()
     */
    public E remove() {
        return removeFirst();
    }

    /**
     * {@inheritDoc}
     * 
     * @return the head element
     * @throws NoSuchElementException
     *             if the deque is empty
     * @see java.util.Deque#pop()
     */
    public E pop() {
        return removeFirst();
    }

    /**
     * {@inheritDoc}
     * 
     * @return the tail element
     * @throws NoSuchElementException
     *             if the deque is empty
     * @see java.util.Deque#removeLast()
     */
    public  E removeLast() {
        checkEmpty();
        return removeLastImpl();
    }

    /**
     * {@inheritDoc}
     * 
     * @return the head element or null if the deque is empty
     * @see java.util.Deque#pollFirst()
     */
    public  E pollFirst() {
        return (status == DequeStatus.Empty) ? null : removePollFirstImpl();
    }

    /**
     * Gets and removes the head element of this deque. This method returns null
     * if the deque is empty.
     * 
     * @return the head element or null if the deque is empty
     * @see java.util.Queue#poll()
     */
    public E poll() {
        return pollFirst();
    }

    /**
     * {@inheritDoc}
     * 
     * @return the tail element or null if the deque is empty
     * @see java.util.Deque#pollLast()
     */
    public  E pollLast() {
        return (status == DequeStatus.Empty) ? null : removeLastImpl();
    }

    /**
     * {@inheritDoc}
     * 
     * @return the head element
     * @throws NoSuchElementException
     *             if the deque is empty
     * @see java.util.Deque#getFirst()
     */
    public  E getFirst() {
        checkEmpty();
        return elements[front];
    }

    /**
     * Gets but does not remove the head element of this deque. It throws an
     * exception if the deque is empty.
     * 
     * @return the head element
     * @throws NoSuchElementException
     *             if the deque is empty
     * @see java.util.Queue#element()
     */
    public E element() {
        return getFirst();
    }

    /**
     * {@inheritDoc}
     * 
     * @return the tail element
     * @throws NoSuchElementException
     *             if the deque is empty
     * @see java.util.Deque#getLast()
     */
    public  E getLast() {
        checkEmpty();
        return elements[circularSmallerPos(rear)];
    }

    /**
     * {@inheritDoc}
     * 
     * @return the head element or null if the deque is empty
     * @see java.util.Deque#peekFirst()
     */
    public  E peekFirst() {
        return (status == DequeStatus.Empty) ? null : elements[front];
    }

    /**
     * Gets but not removes the head element of this deque. This method returns
     * null if the deque is empty.
     * 
     * @return the head element or null if the deque is empty
     * @see java.util.Queue#peek()
     */
    public  E peek() {
        return (status == DequeStatus.Empty) ? null : elements[front];
    }

    /**
     * {@inheritDoc}
     * 
     * @return the tail element or null if the deque is empty
     * @see java.util.Deque#peekLast()
     */
    public  E peekLast() {
        return (status == DequeStatus.Empty) ? null
                : elements[circularSmallerPos(rear)];
    }

    private void checkNull(E e) {
        if (null == e) {
            throw new NullPointerException();
        }
    }

    private void checkEmpty() {
        if (status == DequeStatus.Empty) {
            throw new NoSuchElementException();
        }
    }

    private int circularSmallerPos(int current) {
        return (current - 1 < 0) ? (elements.length - 1) : current - 1;
    }

    private int circularBiggerPos(int current) {
        return (current + 1 >= elements.length) ? 0 : current + 1;
    }

    @SuppressWarnings("unchecked")
    /*
     * If array of elements is full, there will be a new bigger array to store
     * the elements.
     */
    private void checkAndExpand() {
        if (status != DequeStatus.Full) {
            return;
        }
        if (Integer.MAX_VALUE == elements.length) {
            throw new IllegalStateException();
        }
        int length = elements.length;
        int newLength = length << 1;
        // bigger than Integer.MAX_VALUE
        if (newLength < 0) {
            newLength = Integer.MAX_VALUE;
        }
        E[] newElements = (E[]) new Object[newLength];
        System.arraycopy(elements, front, newElements, 0, length - front);
        System.arraycopy(elements, 0, newElements, length - front, front);
        front = 0;
        rear = length;
        status = DequeStatus.Normal;
        elements = newElements;
    }

    /**
     * Resets the status after adding or removing operation.
     * 
     * @param adding
     *            if the method is called after an "adding" operation
     */
    private void resetStatus(boolean adding) {
        if (front == rear) {
            status = adding ? DequeStatus.Full : DequeStatus.Empty;
        } else {
            status = DequeStatus.Normal;
        }
    }

    private  boolean addLastImpl(E e) {
        checkNull(e);
        checkAndExpand();
        elements[rear] = e;
        rear = circularBiggerPos(rear);
        resetStatus(true);
        modCount++;
        return true;
    }

    private E removePollFirstImpl() {
        E element = elements[front];
        elements[front] = null;
        front = circularBiggerPos(front);
        resetStatus(false);
        modCount++;
        return element;
    }

    private E removeLastImpl() {
        int last = circularSmallerPos(rear);
        E element = elements[last];
        elements[last] = null;
        rear = last;
        resetStatus(false);
        modCount++;
        return element;
    }

    /**
     * {@inheritDoc}
     * 
     * @param obj
     *            the element to be removed
     * @return true if the operation succeeds or false if the deque does not
     *         contain the element
     * @see java.util.Deque#removeFirstOccurrence(java.lang.Object)
     */
    public boolean removeFirstOccurrence(Object obj) {
        return removeFirstOccurrenceImpl(obj);
    }

    /**
     * Removes the first equivalent element of the specified object. If the
     * deque does not contain the element, it is unchanged and returns false.
     * 
     * @param obj
     *            the element to be removed
     * @return true if the operation succeeds or false if the deque does not
     *         contain the element
     * @see java.util.AbstractCollection#remove(java.lang.Object)
     */
    @Override
    public boolean remove(Object obj) {
        return removeFirstOccurrenceImpl(obj);
    }

    /**
     * {@inheritDoc}
     * 
     * @param obj
     *            the element to be removed
     * @return true if the operation succeeds or false if the deque does not
     *         contain the element.
     * @see java.util.Deque#removeLastOccurrence(java.lang.Object)
     */
    public  boolean removeLastOccurrence(final Object obj) {
        if (null != obj) {
            Iterator<E> iter = descendingIterator();
            while (iter.hasNext()) {
                if (iter.next().equals(obj)) {
                    iter.remove();
                    return true;
                }
            }
        }
        return false;
    }

    private  boolean removeFirstOccurrenceImpl(final Object obj) {
        if (null != obj) {
            Iterator<E> iter = iterator();
            while (iter.hasNext()) {
                if (iter.next().equals(obj)) {
                    iter.remove();
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * Removes the element in the cursor position and shifts front elements to
     * fill the gap if frontShift is true, shifts rear elements otherwise.
     * 
     */
    private void removeInternal(final int current, final boolean frontShift) {
        int cursor = current;
        if (frontShift) {
            while (cursor != front) {
                int next = circularSmallerPos(cursor);
                elements[cursor] = elements[next];
                cursor = next;
            }
            front = circularBiggerPos(front);
        } else {
            while (cursor != rear) {
                int next = circularBiggerPos(cursor);
                elements[cursor] = elements[next];
                cursor = next;
            }
            rear = circularSmallerPos(rear);
        }
        elements[cursor] = null;
        resetStatus(false);
    }

    /**
     * Returns the size of the deque.
     * 
     * @return the size of the deque
     * @see java.util.AbstractCollection#size()
     */
    @Override
    public  int size() {
        if (status == DequeStatus.Full) {
            return elements.length;
        }
        return (front <= rear) ? (rear - front)
                : (rear + elements.length - front);
    }

    /**
     * Returns true if the deque has no elements.
     * 
     * @return true if the deque has no elements, false otherwise
     * @see java.util.AbstractCollection#isEmpty()
     */
    @Override
    public  boolean isEmpty() {
        return 0 == size();
    }

    /**
     * Returns true if the specified element is in the deque.
     * 
     * @param obj
     *            the element
     * @return true if the element is in the deque, false otherwise
     * @see java.util.AbstractCollection#contains(java.lang.Object)
     */
    @SuppressWarnings("cast")
    @Override
    public  boolean contains(final Object obj) {
        if (null != obj) {
            Iterator<E> it = new ArrayDequeIterator<E>();
            while (it.hasNext()) {
                if (obj.equals((E) it.next())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Empty the deque.
     * 
     * @see java.util.AbstractCollection#clear()
     */
    @SuppressWarnings("cast")
    @Override
    public  void clear() {
        if (status != DequeStatus.Empty) {
            int cursor = front;
            do {
                elements[cursor] = null;
                cursor = circularBiggerPos(cursor);
            } while (cursor != rear);
            status = DequeStatus.Empty;
        }
        front = rear = 0;
        modCount = 0;
    }

    /**
     * Returns the iterator of the deque. The elements will be ordered from head
     * to tail.
     * 
     * @return the iterator
     * @see java.util.AbstractCollection#iterator()
     */
    @SuppressWarnings("synthetic-access")
    @Override
    public Iterator<E> iterator() {
        return new ArrayDequeIterator<E>();
    }

    /**
     * {@inheritDoc}
     * 
     * @return the reverse order Iterator
     * @see java.util.Deque#descendingIterator()
     */
    public Iterator<E> descendingIterator() {
        return new ReverseArrayDequeIterator<E>();
    }

}
