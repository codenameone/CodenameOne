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

/// LinkedList is an implementation of List, backed by a linked list. All
/// optional operations are supported, adding, removing and replacing. The
/// elements can be any objects.
///
/// #### Since
///
/// 1.2
public class LinkedList<E> extends AbstractSequentialList<E> implements
		List<E>, Deque<E> {


    transient int size = 0;

    transient Link<E> voidLink;

    private static final class Link<ET> {
        ET data;

        Link<ET> previous, next;

        Link(ET o, Link<ET> p, Link<ET> n) {
            data = o;
            previous = p;
            next = n;
        }
    }

    private static final class LinkIterator<ET> implements ListIterator<ET> {
        int pos, expectedModCount;

        final LinkedList<ET> list;

        Link<ET> link, lastLink;

        LinkIterator(LinkedList<ET> object, int location) {
            list = object;
            expectedModCount = list.modCount;
            if (0 <= location && location <= list.size) {
                // pos ends up as -1 if list is empty, it ranges from -1 to
                // list.size - 1
                // if link == voidLink then pos must == -1
                link = list.voidLink;
                if (location < list.size / 2) {
                    for (pos = -1; pos + 1 < location; pos++) {
                        link = link.next;
                    }
                } else {
                    for (pos = list.size; pos >= location; pos--) {
                        link = link.previous;
                    }
                }
            } else {
                throw new IndexOutOfBoundsException();
            }
        }

        public void add(ET object) {
            if (expectedModCount == list.modCount) {
                Link<ET> next = link.next;
                Link<ET> newLink = new Link<ET>(object, link, next);
                link.next = newLink;
                next.previous = newLink;
                link = newLink;
                lastLink = null;
                pos++;
                expectedModCount++;
                list.size++;
                list.modCount++;
            } else {
                throw new ConcurrentModificationException();
            }
        }

        public boolean hasNext() {
            return link.next != list.voidLink;
        }

        public boolean hasPrevious() {
            return link != list.voidLink;
        }

        public ET next() {
            if (expectedModCount == list.modCount) {
                LinkedList.Link<ET> next = link.next;
                if (next != list.voidLink) {
                    lastLink = link = next;
                    pos++;
                    return link.data;
                }
                throw new NoSuchElementException();
            }
            throw new ConcurrentModificationException();
        }

        public int nextIndex() {
            return pos + 1;
        }

        public ET previous() {
            if (expectedModCount == list.modCount) {
                if (link != list.voidLink) {
                    lastLink = link;
                    link = link.previous;
                    pos--;
                    return lastLink.data;
                }
                throw new NoSuchElementException();
            }
            throw new ConcurrentModificationException();
        }

        public int previousIndex() {
            return pos;
        }

		public void remove() {
			if (expectedModCount == list.modCount) {
				if (lastLink != null) {
					Link<ET> next = lastLink.next;
					Link<ET> previous = lastLink.previous;
					next.previous = previous;
					previous.next = next;
					if (lastLink == link) {
						pos--;
					}
					link = previous;
					lastLink = null;
					expectedModCount++;
					list.size--;
					list.modCount++;
					return;
				}
				throw new IllegalStateException();
			}
			throw new ConcurrentModificationException();
		}

		public void set(ET object) {
			if (expectedModCount == list.modCount) {
				if (lastLink != null) {
                    lastLink.data = object;
                } else {
                    throw new IllegalStateException();
                }
			} else {
                throw new ConcurrentModificationException();
            }
		}
	}

	/*
	 * NOTES:descendingIterator is not fail-fast, according to the documentation
	 * and test case.
	 */
	private class ReverseLinkIterator<ET> implements Iterator<ET> {
		private int expectedModCount;

		private final LinkedList<ET> list;

		private Link<ET> link;

		private boolean canRemove;

		ReverseLinkIterator(LinkedList<ET> linkedList) {
			super();
			list = linkedList;
			expectedModCount = list.modCount;
			link = list.voidLink;
			canRemove = false;
		}

		public boolean hasNext() {
			return link.previous != list.voidLink;
		}

		public ET next() {
			if (expectedModCount == list.modCount) {
				if (hasNext()) {
					link = link.previous;
					canRemove = true;
					return link.data;
				}
				throw new NoSuchElementException();
			}
			throw new ConcurrentModificationException();

		}

		public void remove() {
			if (expectedModCount == list.modCount) {
				if (canRemove) {
					Link<ET> next = link.previous;
					Link<ET> previous = link.next;
					next.next = previous;
					previous.previous = next;
					link = previous;
					list.size--;
					list.modCount++;
					expectedModCount++;
					canRemove = false;
					return;
				}
				throw new IllegalStateException();
			}
			throw new ConcurrentModificationException();
		}
	}

    /// Constructs a new empty instance of `LinkedList`.
    public LinkedList() {
        voidLink = new Link<E>(null, null, null);
        voidLink.previous = voidLink;
        voidLink.next = voidLink;
    }

    /// Constructs a new instance of `LinkedList` that holds all of the
    /// elements contained in the specified `collection`. The order of the
    /// elements in this new `LinkedList` will be determined by the
    /// iteration order of `collection`.
    ///
    /// #### Parameters
    ///
    /// - `collection`: the collection of elements to add.
    public LinkedList(Collection<? extends E> collection) {
        this();
        addAll(collection);
    }

    /// Inserts the specified object into this `LinkedList` at the
    /// specified location. The object is inserted before any previous element at
    /// the specified location. If the location is equal to the size of this
    /// `LinkedList`, the object is added at the end.
    ///
    /// #### Parameters
    ///
    /// - `location`: the index at which to insert.
    ///
    /// - `object`: the object to add.
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: if `location = size()`
    @Override
    public void add(int location, E object) {
        if (0 <= location && location <= size) {
            Link<E> link = voidLink;
            if (location < (size / 2)) {
                for (int i = 0; i <= location; i++) {
                    link = link.next;
                }
            } else {
                for (int i = size; i > location; i--) {
                    link = link.previous;
                }
            }
            Link<E> previous = link.previous;
            Link<E> newLink = new Link<E>(object, previous, link);
            previous.next = newLink;
            link.previous = newLink;
            size++;
            modCount++;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    /// Adds the specified object at the end of this `LinkedList`.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to add.
    ///
    /// #### Returns
    ///
    /// always true
    @Override
    public boolean add(E object) {
        return addLastImpl(object);
    }

    private boolean addLastImpl(E object) {
        Link<E> oldLast = voidLink.previous;
        Link<E> newLink = new Link<E>(object, oldLast, voidLink);
        voidLink.previous = newLink;
        oldLast.next = newLink;
        size++;
        modCount++;
        return true;
    }

    /// Inserts the objects in the specified collection at the specified location
    /// in this `LinkedList`. The objects are added in the order they are
    /// returned from the collection's iterator.
    ///
    /// #### Parameters
    ///
    /// - `location`: the index at which to insert.
    ///
    /// - `collection`: the collection of objects
    ///
    /// #### Returns
    ///
    /// @return `true` if this `LinkedList` is modified,
    /// `false` otherwise.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: if the class of an object is inappropriate for this list.
    ///
    /// - `IllegalArgumentException`: if an object cannot be added to this list.
    ///
    /// - `IndexOutOfBoundsException`: if `location  size()`
    @Override
    public boolean addAll(int location, Collection<? extends E> collection) {
        if (location < 0 || location > size) {
            throw new IndexOutOfBoundsException();
        }
        int adding = collection.size();
        if (adding == 0) {
            return false;
        }
        Collection<? extends E> elements = (collection == this) ?
                new ArrayList<E>(collection) : collection;

        Link<E> previous = voidLink;
        if (location < (size / 2)) {
            for (int i = 0; i < location; i++) {
                previous = previous.next;
            }
        } else {
            for (int i = size; i >= location; i--) {
                previous = previous.previous;
            }
        }
        Link<E> next = previous.next;
        Iterator it = elements.iterator();
        while(it.hasNext()) {
            Link<E> newLink = new Link<E>((E)it.next(), previous, null);
            previous.next = newLink;
            previous = newLink;
        }
        previous.next = next;
        next.previous = previous;
        size += adding;
        modCount++;
        return true;
    }

    /// Adds the objects in the specified Collection to this `LinkedList`.
    ///
    /// #### Parameters
    ///
    /// - `collection`: the collection of objects.
    ///
    /// #### Returns
    ///
    /// @return `true` if this `LinkedList` is modified,
    /// `false` otherwise.
    @Override
    public boolean addAll(Collection<? extends E> collection) {
        int adding = collection.size();
        if (adding == 0) {
            return false;
        }
        Collection<? extends E> elements = (collection == this) ?
                new ArrayList<E>(collection) : collection;

        Link<E> previous = voidLink.previous;
        Iterator it = elements.iterator();
        while(it.hasNext()) {
            Link<E> newLink = new Link<E>((E)it.next(), previous, null);
            previous.next = newLink;
            previous = newLink;
        }
        previous.next = voidLink;
        voidLink.previous = previous;
        size += adding;
        modCount++;
        return true;
    }

    /// Adds the specified object at the beginning of this `LinkedList`.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to add.
	public void addFirst(E object) {
		addFirstImpl(object);
	}

	private boolean addFirstImpl(E object) {
		Link<E> oldFirst = voidLink.next;
		Link<E> newLink = new Link<E>(object, voidLink, oldFirst);
		voidLink.next = newLink;
		oldFirst.previous = newLink;
		size++;
		modCount++;
		return true;
	}

    /// Adds the specified object at the end of this `LinkedList`.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to add.
	public void addLast(E object) {
		addLastImpl(object);
	}

    /// Removes all elements from this `LinkedList`, leaving it empty.
    ///
    /// #### See also
    ///
    /// - List#isEmpty
    ///
    /// - #size
    @Override
    public void clear() {
        if (size > 0) {
            size = 0;
            voidLink.next = voidLink;
            voidLink.previous = voidLink;
            modCount++;
        }
    }

    /// Searches this `LinkedList` for the specified object.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to search for.
    ///
    /// #### Returns
    ///
    /// @return `true` if `object` is an element of this
    /// `LinkedList`, `false` otherwise
    @Override
    public boolean contains(Object object) {
        Link<E> link = voidLink.next;
        if (object != null) {
            while (link != voidLink) {
                if (object.equals(link.data)) {
                    return true;
                }
                link = link.next;
            }
        } else {
            while (link != voidLink) {
                if (link.data == null) {
                    return true;
                }
                link = link.next;
            }
        }
        return false;
    }

    @Override
    public E get(int location) {
        if (0 <= location && location < size) {
            Link<E> link = voidLink;
            if (location < (size / 2)) {
                for (int i = 0; i <= location; i++) {
                    link = link.next;
                }
            } else {
                for (int i = size; i > location; i--) {
                    link = link.previous;
                }
            }
            return link.data;
        }
        throw new IndexOutOfBoundsException();
    }

    /// Returns the first element in this `LinkedList`.
    ///
    /// #### Returns
    ///
    /// the first element.
    ///
    /// #### Throws
    ///
    /// - `NoSuchElementException`: if this `LinkedList` is empty.
    public E getFirst() {
		return getFirstImpl();
	}

	private E getFirstImpl() {
		Link<E> first = voidLink.next;
		if (first != voidLink) {
            return first.data;
        }
		throw new NoSuchElementException();
	}

    /// Returns the last element in this `LinkedList`.
    ///
    /// #### Returns
    ///
    /// the last element
    ///
    /// #### Throws
    ///
    /// - `NoSuchElementException`: if this `LinkedList` is empty
    public E getLast() {
        Link<E> last = voidLink.previous;
        if (last != voidLink) {
            return last.data;
        }
        throw new NoSuchElementException();
    }

    @Override
    public int indexOf(Object object) {
        int pos = 0;
        Link<E> link = voidLink.next;
        if (object != null) {
            while (link != voidLink) {
                if (object.equals(link.data)) {
                    return pos;
                }
                link = link.next;
                pos++;
            }
        } else {
            while (link != voidLink) {
                if (link.data == null) {
                    return pos;
                }
                link = link.next;
                pos++;
            }
        }
        return -1;
    }

    /// Searches this `LinkedList` for the specified object and returns the
    /// index of the last occurrence.
    ///
    /// #### Parameters
    ///
    /// - `object`: the object to search for
    ///
    /// #### Returns
    ///
    /// @return the index of the last occurrence of the object, or -1 if it was
    /// not found.
    @Override
    public int lastIndexOf(Object object) {
        int pos = size;
        Link<E> link = voidLink.previous;
        if (object != null) {
            while (link != voidLink) {
                pos--;
                if (object.equals(link.data)) {
                    return pos;
                }
                link = link.previous;
            }
        } else {
            while (link != voidLink) {
                pos--;
                if (link.data == null) {
                    return pos;
                }
                link = link.previous;
            }
        }
        return -1;
    }

    /// Returns a ListIterator on the elements of this `LinkedList`. The
    /// elements are iterated in the same order that they occur in the
    /// `LinkedList`. The iteration starts at the specified location.
    ///
    /// #### Parameters
    ///
    /// - `location`: the index at which to start the iteration
    ///
    /// #### Returns
    ///
    /// a ListIterator on the elements of this `LinkedList`
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: if `location = size()`
    ///
    /// #### See also
    ///
    /// - ListIterator
    @Override
    public ListIterator<E> listIterator(int location) {
        return new LinkIterator<E>(this, location);
    }

    /// Removes the object at the specified location from this `LinkedList`.
    ///
    /// #### Parameters
    ///
    /// - `location`: the index of the object to remove
    ///
    /// #### Returns
    ///
    /// the removed object
    ///
    /// #### Throws
    ///
    /// - `IndexOutOfBoundsException`: if `location = size()`
    @Override
    public E remove(int location) {
        if (0 <= location && location < size) {
            Link<E> link = voidLink;
            if (location < (size / 2)) {
                for (int i = 0; i <= location; i++) {
                    link = link.next;
                }
            } else {
                for (int i = size; i > location; i--) {
                    link = link.previous;
                }
            }
            Link<E> previous = link.previous;
            Link<E> next = link.next;
            previous.next = next;
            next.previous = previous;
            size--;
            modCount++;
            return link.data;
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public boolean remove(Object object) {
		return removeFirstOccurrenceImpl(object);
	}

    /// Removes the first object from this `LinkedList`.
    ///
    /// #### Returns
    ///
    /// the removed object.
    ///
    /// #### Throws
    ///
    /// - `NoSuchElementException`: if this `LinkedList` is empty.
    public E removeFirst() {
        return removeFirstImpl();
    }

    private E removeFirstImpl() {
        Link<E> first = voidLink.next;
        if (first != voidLink) {
            Link<E> next = first.next;
            voidLink.next = next;
            next.previous = voidLink;
            size--;
            modCount++;
            return first.data;
        }
        throw new NoSuchElementException();
    }

    /// Removes the last object from this `LinkedList`.
    ///
    /// #### Returns
    ///
    /// the removed object.
    ///
    /// #### Throws
    ///
    /// - `NoSuchElementException`: if this `LinkedList` is empty.
    public E removeLast() {
        return removeLastImpl();
    }

    private E removeLastImpl() {
        Link<E> last = voidLink.previous;
        if (last != voidLink) {
            Link<E> previous = last.previous;
            voidLink.previous = previous;
            previous.next = voidLink;
            size--;
            modCount++;
            return last.data;
        }
        throw new NoSuchElementException();
    }

	/// {@inheritDoc}
	///
	/// #### Since
	///
	/// 1.6
	///
	/// #### See also
	///
	/// - java.util.Deque#descendingIterator()
	public Iterator<E> descendingIterator() {
		return new ReverseLinkIterator<E>(this);
	}

	/// {@inheritDoc}
	///
	/// #### Since
	///
	/// 1.6
	///
	/// #### See also
	///
	/// - java.util.Deque#offerFirst(java.lang.Object)
	public boolean offerFirst(E e) {
		return addFirstImpl(e);
	}

	/// {@inheritDoc}
	///
	/// #### Since
	///
	/// 1.6
	///
	/// #### See also
	///
	/// - java.util.Deque#offerLast(java.lang.Object)
	public boolean offerLast(E e) {
		return addLastImpl(e);
	}

	/// {@inheritDoc}
	///
	/// #### Since
	///
	/// 1.6
	///
	/// #### See also
	///
	/// - java.util.Deque#peekFirst()
	public E peekFirst() {
		return peekFirstImpl();
	}

	/// {@inheritDoc}
	///
	/// #### Since
	///
	/// 1.6
	///
	/// #### See also
	///
	/// - java.util.Deque#peekLast()
	public E peekLast() {
		Link<E> last = voidLink.previous;
		return (last == voidLink) ? null : last.data;
	}

	/// {@inheritDoc}
	///
	/// #### Since
	///
	/// 1.6
	///
	/// #### See also
	///
	/// - java.util.Deque#pollFirst()
	public E pollFirst() {
		return (size == 0) ? null : removeFirstImpl();
	}

	/// {@inheritDoc}
	///
	/// #### Since
	///
	/// 1.6
	///
	/// #### See also
	///
	/// - java.util.Deque#pollLast()
	public E pollLast() {
		return (size == 0) ? null : removeLastImpl();
	}

	/// {@inheritDoc}
	///
	/// #### Since
	///
	/// 1.6
	///
	/// #### See also
	///
	/// - java.util.Deque#pop()
	public E pop() {
		return removeFirstImpl();
	}

	/// {@inheritDoc}
	///
	/// #### Since
	///
	/// 1.6
	///
	/// #### See also
	///
	/// - java.util.Deque#push(java.lang.Object)
	public void push(E e) {
		addFirstImpl(e);
	}

	/// {@inheritDoc}
	///
	/// #### Since
	///
	/// 1.6
	///
	/// #### See also
	///
	/// - java.util.Deque#removeFirstOccurrence(java.lang.Object)
	public boolean removeFirstOccurrence(Object o) {
		return removeFirstOccurrenceImpl(o);
	}

	/// {@inheritDoc}
	///
	/// #### Since
	///
	/// 1.6
	///
	/// #### See also
	///
	/// - java.util.Deque#removeLastOccurrence(java.lang.Object)
	public boolean removeLastOccurrence(Object o) {
		Iterator<E> iter = new ReverseLinkIterator<E>(this);
		return removeOneOccurrence(o, iter);
	}

	private boolean removeFirstOccurrenceImpl(Object o) {
		Iterator<E> iter = new LinkIterator<E>(this, 0);
		return removeOneOccurrence(o, iter);
	}

	private boolean removeOneOccurrence(Object o, Iterator<E> iter) {
		while (iter.hasNext()) {
			E element = iter.next();
			if (o == null ? element == null : o.equals(element)) {
				iter.remove();
				return true;
			}
		}
		return false;
	}

    /// Replaces the element at the specified location in this `LinkedList`
    /// with the specified object.
    ///
    /// #### Parameters
    ///
    /// - `location`: the index at which to put the specified object.
    ///
    /// - `object`: the object to add.
    ///
    /// #### Returns
    ///
    /// the previous element at the index.
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: if the class of an object is inappropriate for this list.
    ///
    /// - `IllegalArgumentException`: if an object cannot be added to this list.
    ///
    /// - `IndexOutOfBoundsException`: if `location = size()`
    @Override
    public E set(int location, E object) {
        if (0 <= location && location < size) {
            Link<E> link = voidLink;
            if (location < (size / 2)) {
                for (int i = 0; i <= location; i++) {
                    link = link.next;
                }
            } else {
                for (int i = size; i > location; i--) {
                    link = link.previous;
                }
            }
            E result = link.data;
            link.data = object;
            return result;
        }
        throw new IndexOutOfBoundsException();
    }

    /// Returns the number of elements in this `LinkedList`.
    ///
    /// #### Returns
    ///
    /// the number of elements in this `LinkedList`.
    @Override
    public int size() {
        return size;
    }

    public boolean offer(E o) {
		return addLastImpl(o);
    }

    public E poll() {
        return size == 0 ? null : removeFirst();
    }

    public E remove() {
		return removeFirstImpl();
    }

    public E peek() {
		return peekFirstImpl();
	}

	private E peekFirstImpl() {
        Link<E> first = voidLink.next;
        return first == voidLink ? null : first.data;
    }

    public E element() {
		return getFirstImpl();
    }

    /// Returns a new array containing all elements contained in this
    /// `LinkedList`.
    ///
    /// #### Returns
    ///
    /// an array of the elements from this `LinkedList`.
    @Override
    public Object[] toArray() {
        int index = 0;
        Object[] contents = new Object[size];
        Link<E> link = voidLink.next;
        while (link != voidLink) {
            contents[index++] = link.data;
            link = link.next;
        }
        return contents;
    }

    /// Returns an array containing all elements contained in this
    /// `LinkedList`. If the specified array is large enough to hold the
    /// elements, the specified array is used, otherwise an array of the same
    /// type is created. If the specified array is used and is larger than this
    /// `LinkedList`, the array element following the collection elements
    /// is set to null.
    ///
    /// #### Parameters
    ///
    /// - `contents`: the array.
    ///
    /// #### Returns
    ///
    /// an array of the elements from this `LinkedList`.
    ///
    /// #### Throws
    ///
    /// - `ArrayStoreException`: @throws ArrayStoreException
    /// if the type of an element in this `LinkedList` cannot
    /// be stored in the type of the specified array.
    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] contents) {
        int index = 0;
        if (size > contents.length) {
            return null;
        }
        Link<E> link = voidLink.next;
        while (link != voidLink) {
            contents[index++] = (T) link.data;
            link = link.next;
        }
        if (index < contents.length) {
            contents[index] = null;
        }
        return contents;
    }
}
