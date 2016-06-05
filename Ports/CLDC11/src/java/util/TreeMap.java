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
 * TreeMap is an implementation of SortedMap. All optional operations (adding
 * and removing) are supported. The values can be any objects. The keys can be
 * any objects which are comparable to each other either using their natural
 * 
 * @param <K>
 *            type of key
 * @param <V>
 *            type of value
 * 
 * @since 1.2
 */
public class TreeMap<K, V> extends AbstractMap<K, V> implements
        NavigableMap<K, V> {
    private static final long serialVersionUID = 919286545866124006L;

    transient int size;

    transient Node<K, V> root;

    Comparator<? super K> comparator;

    transient int modCount;

    transient Set<Map.Entry<K, V>> entrySet;

    transient NavigableMap<K, V> descendingMap;

    transient NavigableSet<K> navigableKeySet;
    
    static class Node<K, V>  {
        static final int NODE_SIZE = 64;

        Node<K, V> prev, next;

        Node<K, V> parent, left, right;

        V[] values;

        K[] keys;

        int left_idx = 0;

        int right_idx = -1;

        int size = 0;

        boolean color;

        @SuppressWarnings("unchecked")
        public Node() {
            keys = (K[]) new Object[NODE_SIZE];
            values = (V[]) new Object[NODE_SIZE];
        }
    }
    
    /**
     * Entry is an internal class which is used to hold the entries of a
     * TreeMap.
     * 
     * also used to record key, value, and position
     */
    static class Entry<K, V> extends MapEntry<K, V> {
        Entry<K, V> parent, left, right;
        
        Node<K, V> node;

        int index;

        public void setLocation(Node<K, V> node, int index, V value, K key) {
            this.node = node;
            this.index = index;
            this.value = value;
            this.key = key;
        }

        boolean color;

        Entry(K theKey) {
            super(theKey);
        }

        Entry(K theKey, V theValue) {
            super(theKey, theValue);
        }
        
        public V setValue(V object) {
            V result = value;
            value = object;
            this.node.values[index] = value;
            return result;
        }
    }

    private static abstract class AbstractSubMapIterator<K, V> {
        final NavigableSubMap<K, V> subMap;

        int expectedModCount;

        TreeMap.Node<K, V> node;
        
        TreeMap.Node<K, V> lastNode;
        
        TreeMap.Entry<K, V> boundaryPair;
        
        int offset;

        int lastOffset;
        
        boolean getToEnd = false;
        
        AbstractSubMapIterator(final NavigableSubMap<K, V> map) {
            subMap = map;
            expectedModCount = subMap.m.modCount;

            TreeMap.Entry<K, V> entry = map.findStartNode();
            if (entry != null) {
                if (map.toEnd && !map.checkUpperBound(entry.key)) {
                } else {
                    node = entry.node;
                    offset = entry.index;
                    boundaryPair = getBoundaryNode();
                }
            }
        }

        public void remove() {
            if (expectedModCount == subMap.m.modCount) {
                if (expectedModCount == subMap.m.modCount) {
                    K key = (node != null) ? node.keys[offset] : null;
                    if (lastNode != null) {
                        int idx = lastOffset;
                        if (idx == lastNode.left_idx){
                            subMap.m.removeLeftmost(lastNode);
                        } else if (idx == lastNode.right_idx) {
                            subMap.m.removeRightmost(lastNode);
                        } else {
                            int lastRight = lastNode.right_idx;
                            key = subMap.m.removeMiddleElement(lastNode, idx);
                            if (key == null && lastRight > lastNode.right_idx) {
                                // removed from right
                                offset--;
                            }
                        }
                        if (null != key) {
                            // the node has been cleared
                            Entry<K, V> entry = subMap.m.find(key);
                            if (this.subMap.isInRange(key)) {
                                node = entry.node;
                                offset = entry.index;
                                boundaryPair = getBoundaryNode();
                            } else {
                                node = null;
                            }
                        }
                        if (node != null && !this.subMap.isInRange(node.keys[offset])){
                            node = null;
                        }
                        lastNode = null;
                        expectedModCount++;
                    } else {
                        throw new IllegalStateException();
                    }
                }
            } else {
                throw new ConcurrentModificationException();
            }
        }
        
        final void makeNext() {
            if (expectedModCount != subMap.m.modCount) {
                throw new ConcurrentModificationException();
            } else if (node == null) {
                throw new NoSuchElementException();
            }
            lastNode = node;
            lastOffset = offset;
            if (offset != lastNode.right_idx) {
                offset++;
            } else {
                node = node.next;
                if (node != null) {
                    offset = node.left_idx;
                }
            }
            if (boundaryPair.node == lastNode
                    && boundaryPair.index == lastOffset) {
                node = null;
            }
        }

        
        Entry<K, V> createEntry(Node<K,V> node, int index) {
            TreeMap.Entry<K, V> entry = new TreeMap.Entry<K, V>(node.keys[index], node.values[index]);
            entry.node = node;
            entry.index = index;
            return entry; 
        }

        abstract TreeMap.Entry<K, V> getStartNode();

        abstract boolean hasNext();
        
        abstract TreeMap.Entry<K, V> getBoundaryNode();
    }

    private abstract static class AscendingSubMapIterator<K, V> extends
            AbstractSubMapIterator<K, V> {      
        
        AscendingSubMapIterator(NavigableSubMap<K, V> map) {
            super(map);            
        }
        
        final TreeMap.Entry<K, V> getBoundaryNode() {
            TreeMap.Entry<K, V> entry = null;
            if (subMap.toEnd) {
                entry = subMap.hiInclusive ? subMap
                        .smallerOrEqualEntry(subMap.hi) : subMap
                        .smallerEntry(subMap.hi);
            } else {
                entry = subMap.theBiggestEntry();
            }
            if (entry == null){
                entry = subMap.findStartNode();
            }
            return entry;
        }

        @Override
        final TreeMap.Entry<K, V> getStartNode() {
            if (subMap.fromStart) {
                return subMap.loInclusive ? subMap
                        .biggerOrEqualEntry(subMap.lo) : subMap
                        .biggerEntry(subMap.lo);
            }
            return subMap.theSmallestEntry();
        }

        TreeMap.Entry<K, V> getNext() {
            if (expectedModCount != subMap.m.modCount) {
                throw new ConcurrentModificationException();
            } else if (node == null) {
                throw new NoSuchElementException();
            }
            lastNode = node;
            lastOffset = offset;
            if (offset != node.right_idx) {
                offset++;
            } else {
                node = node.next;
                if (node != null) {
                    offset = node.left_idx;
                }
            }
            if (lastNode != null) {
                boundaryPair = getBoundaryNode();
                if (boundaryPair != null && boundaryPair.node == lastNode && boundaryPair.index == lastOffset) {
                    node = null;
                }
                return createEntry(lastNode, lastOffset);
            } 
            return null;
        }
        

        @Override
        public final boolean hasNext() {
            return null!=node;
        }

    }

    static class AscendingSubMapEntryIterator<K, V> extends
            AscendingSubMapIterator<K, V> implements Iterator<Map.Entry<K, V>> {

        AscendingSubMapEntryIterator(NavigableSubMap<K, V> map) {
            super(map);
        }

        public final Map.Entry<K, V> next() {
            return getNext();
        }
    }

    static class AscendingSubMapKeyIterator<K, V> extends
            AscendingSubMapIterator<K, V> implements Iterator<K> {

        AscendingSubMapKeyIterator(NavigableSubMap<K, V> map) {
            super(map);
        }

        public final K next() {
            return getNext().key;
        }
    }

    private abstract static class DescendingSubMapIterator<K, V> extends
            AbstractSubMapIterator<K, V> {

        DescendingSubMapIterator(NavigableSubMap<K, V> map) {
            super(map); 
            TreeMap.Entry<K,V> entry;
            if (map.fromStart){
                entry = map.loInclusive ? map.m.findFloorEntry(map.lo) : map.m.findLowerEntry(map.lo);
            } else {
                entry = map.m.findBiggestEntry();
            }
            if (entry != null) {
                if (!map.isInRange(entry.key)) {
                    node = null;
                    return;
                }
                node = entry.node;
                offset = entry.index;
            } else {
                node = null;
                return;
            }
            boundaryPair = getBoundaryNode();
            if (boundaryPair != null){
                if (map.m.keyCompare(boundaryPair.key,entry.key) > 0){
                    node = null;
                }
            }
            if (map.toEnd && !map.hiInclusive){
                // the last element may be the same with first one but it is not included
                if (map.m.keyCompare(map.hi,entry.key) == 0){
                    node = null;
                }
            }
        }

        @Override
        final TreeMap.Entry<K, V> getStartNode() {
            if (subMap.toEnd) {
                return subMap.hiInclusive ? subMap
                        .smallerOrEqualEntry(subMap.hi) : subMap
                        .smallerEntry(subMap.hi);
            }
            return subMap.theBiggestEntry();
        }
        
        final TreeMap.Entry<K, V> getBoundaryNode() {
            if (subMap.toEnd) {
                return subMap.hiInclusive ? subMap.m.findCeilingEntry(subMap.hi) : subMap.m.findHigherEntry(subMap.hi);
            }
            return subMap.m.findSmallestEntry();
        }
        
        TreeMap.Entry<K, V> getNext() {
            if (node == null) {
                throw new NoSuchElementException();
            }
            if (expectedModCount != subMap.m.modCount) {
                throw new ConcurrentModificationException();
            }
            
            lastNode = node;
            lastOffset = offset;
            if (offset != node.left_idx) {
                offset--;
            } else {
                node = node.prev;
                if (node != null) {
                    offset = node.right_idx;
                }
            }
            boundaryPair = getBoundaryNode();
            if (boundaryPair != null && boundaryPair.node == lastNode && boundaryPair.index == lastOffset) {
                node = null;
            }
            return createEntry(lastNode, lastOffset);
        }

        @Override
        public final boolean hasNext() {
            return node != null;
        }
        
        public final void remove() {
            if (expectedModCount == subMap.m.modCount) {
                if (expectedModCount == subMap.m.modCount) {
                    K key = (node != null) ? node.keys[offset] : null;
                    if (lastNode != null) {
                        int idx = lastOffset;
                        if (idx == lastNode.left_idx){
                            subMap.m.removeLeftmost(lastNode);
                        } else if (idx == lastNode.right_idx) {
                            subMap.m.removeRightmost(lastNode);
                        } else {
                            subMap.m.removeMiddleElement(lastNode, idx);
                        }
                        if (null != key) {
                            // the node has been cleared
                            Entry<K,V> entry = subMap.m.find(key);
                            node = entry.node;
                            offset = entry.index;
                            boundaryPair = getBoundaryNode();
                        } else {
                            node = null;
                        }
                        lastNode = null;
                        expectedModCount++;
                    } else {
                        throw new IllegalStateException();
                    }
                }
            } else {
                throw new ConcurrentModificationException();
            }
        }
    }

    static class DescendingSubMapEntryIterator<K, V> extends
            DescendingSubMapIterator<K, V> implements Iterator<Map.Entry<K, V>> {

        DescendingSubMapEntryIterator(NavigableSubMap<K, V> map) {
            super(map);
        }

        public final Map.Entry<K, V> next() {
            return getNext();
        }
    }

    static class DescendingSubMapKeyIterator<K, V> extends
            DescendingSubMapIterator<K, V> implements Iterator<K> {

        DescendingSubMapKeyIterator(NavigableSubMap<K, V> map) {
            super(map);
        }

        public final K next() {
            return getNext().key;
        }
    }
    
    static final class SubMap<K, V> extends AbstractMap<K, V> implements
            SortedMap<K, V> {
        private TreeMap<K, V> backingMap;

        boolean hasStart, hasEnd;

        K startKey, endKey;

        transient Set<Map.Entry<K, V>> entrySet = null;

        transient int firstKeyModCount = -1;

        transient int lastKeyModCount = -1;

        transient Node<K, V> firstKeyNode;

        transient int firstKeyIndex;

        transient Node<K, V> lastKeyNode;

        transient int lastKeyIndex;

        SubMap(K start, TreeMap<K, V> map) {
            backingMap = map;
            hasStart = true;
            startKey = start;
        }

        SubMap(K start, TreeMap<K, V> map, K end) {
            backingMap = map;
            hasStart = hasEnd = true;
            startKey = start;
            endKey = end;
        }
        
        SubMap(K start, boolean hasStart, TreeMap<K, V> map, K end, boolean hasEnd) {
            backingMap = map;
            this.hasStart = hasStart;
            this.hasEnd = hasEnd;
            startKey = start;
            endKey = end;
        }

        SubMap(TreeMap<K, V> map, K end) {
            backingMap = map;
            hasEnd = true;
            endKey = end;
        }

        private void checkRange(K key) {
            Comparator<? super K> cmp = backingMap.comparator;
            if (cmp == null) {
                java.lang.Comparable<K> object = toComparable(key);
                if (hasStart && object.compareTo(startKey) < 0) {
                    throw new IllegalArgumentException();
                }
                if (hasEnd && object.compareTo(endKey) >= 0) {
                    throw new IllegalArgumentException();
                }
            } else {
                if (hasStart
                        && backingMap.comparator().compare(key, startKey) < 0) {
                    throw new IllegalArgumentException();
                }
                if (hasEnd && backingMap.comparator().compare(key, endKey) >= 0) {
                    throw new IllegalArgumentException();
                }
            }
        }

        private boolean isInRange(K key) {
            Comparator<? super K> cmp = backingMap.comparator;
            if (cmp == null) {
                java.lang.Comparable<K> object = toComparable(key);
                if (hasStart && object.compareTo(startKey) < 0) {
                    return false;
                }
                if (hasEnd && object.compareTo(endKey) >= 0) {
                    return false;
                }
            } else {
                if (hasStart && cmp.compare(key, startKey) < 0) {
                    return false;
                }
                if (hasEnd && cmp.compare(key, endKey) >= 0) {
                    return false;
                }
            }
            return true;
        }

        private boolean checkUpperBound(K key) {
            if (hasEnd) {
                Comparator<? super K> cmp = backingMap.comparator;
                if (cmp == null) {
                    return (toComparable(key).compareTo(endKey) < 0);
                }
                return (cmp.compare(key, endKey) < 0);
            }
            return true;
        }

        private boolean checkLowerBound(K key) {
            if (hasStart && startKey != null) {
                Comparator<? super K> cmp = backingMap.comparator;
                if (cmp == null) {
                    return (toComparable(key).compareTo(startKey) >= 0);
                }
                return (cmp.compare(key, startKey) >= 0);
            }
            return true;
        }

        public Comparator<? super K> comparator() {
            return backingMap.comparator();
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean containsKey(Object key) {
            if (isInRange((K) key)) {
                return backingMap.containsKey(key);
            }
            return false;
        }

        @Override
        public void clear() {
            keySet().clear();
        }

        @Override
        public boolean containsValue(Object value) {
            Iterator<V> it = values().iterator();
            if (value != null) {
                while (it.hasNext()) {
                    if (value.equals(it.next())) {
                        return true;
                    }
                }
            } else {
                while (it.hasNext()) {
                    if (it.next() == null) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            if (entrySet == null) {
                entrySet = new SubMapEntrySet<K, V>(this);
            }
            return entrySet;
        }

        private void setFirstKey() {
            if (firstKeyModCount == backingMap.modCount) {
                return;
            }
            java.lang.Comparable<K> object = backingMap.comparator == null ? toComparable((K) startKey)
                    : null;
            K key = (K) startKey;
            Node<K, V> node = backingMap.root;
            Node<K, V> foundNode = null;
            int foundIndex = -1;
            TOP_LOOP: while (node != null) {
                K[] keys = node.keys;
                int left_idx = node.left_idx;
                int result = backingMap.cmp(object, key, keys[left_idx]);
                if (result < 0) {
                    foundNode = node;
                    foundIndex = node.left_idx;
                    node = node.left;
                } else if (result == 0) {
                    foundNode = node;
                    foundIndex = node.left_idx;
                    break;
                } else {
                    int right_idx = node.right_idx;
                    if (left_idx != right_idx) {
                        result = backingMap.cmp(object, key, keys[right_idx]);
                    }
                    if (result > 0) {
                        node = node.right;
                    } else if (result == 0) {
                        foundNode = node;
                        foundIndex = node.right_idx;
                        break;
                    } else { /* search in node */
                        foundNode = node;
                        foundIndex = node.right_idx;
                        int low = left_idx + 1, mid = 0, high = right_idx - 1;
                        while (low <= high) {
                            mid = (low + high) >>> 1;
                            result = backingMap.cmp(object, key, keys[mid]);
                            if (result > 0) {
                                low = mid + 1;
                            } else if (result == 0) {
                                foundNode = node;
                                foundIndex = mid;
                                break TOP_LOOP;
                            } else {
                                foundNode = node;
                                foundIndex = mid;
                                high = mid - 1;
                            }
                        }
                        break TOP_LOOP;
                    }
                }
            }
            // note, the original subMap is strange as the endKey is always
            // excluded, to improve the performance here, we retain the original
            // subMap, and keep the bound when firstkey = lastkey
            boolean isBounded = true;
            if (hasEnd && foundNode!=null) {
                Comparator<? super K> cmp = backingMap.comparator;
                if (cmp == null) {
                    isBounded =  (toComparable(foundNode.keys[foundIndex]).compareTo(endKey) <= 0);
                } else {
                    isBounded = (cmp.compare(foundNode.keys[foundIndex], endKey) <= 0);
                }
            }
            if (foundNode != null
                    && !isBounded) {
                foundNode = null;
            }
            firstKeyNode = foundNode;
            firstKeyIndex = foundIndex;
            firstKeyModCount = backingMap.modCount;
        }

        public K firstKey() {
            if (backingMap.size > 0 && !(startKey.equals(endKey))) {
                if (!hasStart) {
                    Node<K, V> node = minimum(backingMap.root);
                    if (node != null
                            && checkUpperBound(node.keys[node.left_idx])) {
                        return node.keys[node.left_idx];
                    }
                } else {
                    setFirstKey();
                    if (firstKeyNode != null) {
                        return firstKeyNode.keys[firstKeyIndex];
                    }
                }
            }
            throw new NoSuchElementException();
        }

        @SuppressWarnings("unchecked")
        @Override
        public V get(Object key) {
            if (isInRange((K) key)) {
                return backingMap.get(key);
            }
            return null;
        }

        public SortedMap<K, V> headMap(K endKey) {
            Comparator<? super K> cmp = backingMap.comparator;
            if (cmp == null) {
                java.lang.Comparable<K> object = toComparable(endKey);
                if (hasStart && object.compareTo(startKey) < 0) {
                    throw new IllegalArgumentException();
                }
                if (hasEnd && object.compareTo(this.endKey) > 0) {
                    throw new IllegalArgumentException();
                }
            } else {
                if (hasStart
                        && backingMap.comparator().compare(endKey, startKey) < 0) {
                    throw new IllegalArgumentException();
                }
                if (hasEnd && backingMap.comparator().compare(endKey, this.endKey) >= 0) {
                    throw new IllegalArgumentException();
                }
            }
            if (hasStart) {
                return new SubMap<K, V>(startKey, backingMap, endKey);
            }
            return new SubMap<K, V>(backingMap, endKey);
        }

        @Override
        public boolean isEmpty() {
            Iterator<K> it = this.keySet().iterator();
            if (it.hasNext()) {
                return false;
            }
            return true;
        }

        @Override
        public Set<K> keySet() {
            if (keySet == null) {
                keySet = new SubMapKeySet<K, V>(this);
            }
            return keySet;
        }

        private void setLastKey() {
            if (lastKeyModCount == backingMap.modCount) {
                return;
            }
            java.lang.Comparable<K> object = backingMap.comparator == null ? toComparable((K) endKey)
                    : null;
            K key = (K) endKey;
            Node<K, V> node = backingMap.root;
            Node<K, V> foundNode = null;
            int foundIndex = -1;
            TOP_LOOP: while (node != null) {
                K[] keys = node.keys;
                int left_idx = node.left_idx;
                // to be compatible with RI on null-key comparator
                int result = object != null ? object.compareTo(keys[left_idx]) : -backingMap.comparator.compare(
                        keys[left_idx] , key);
                //int result =  - backingMap.cmp(object, keys[left_idx] , key);
                if (result < 0) {
                    node = node.left;
                } else {
                    int right_idx = node.right_idx;
                    if (left_idx != right_idx) {
                        result = backingMap.cmp(object, key, keys[right_idx]);
                    }
                    if (result > 0) {
                        foundNode = node;
                        foundIndex = right_idx;
                        node = node.right;
                    } else if (result == 0) {
                        if (node.left_idx == node.right_idx) {
                            foundNode = node.prev;
                            if (foundNode != null) {
                                foundIndex = foundNode.right_idx - 1;
                            }
                        } else {
                            foundNode = node;
                            foundIndex = right_idx;
                        }
                        break;
                    } else { /* search in node */
                        foundNode = node;
                        foundIndex = left_idx;
                        int low = left_idx + 1, mid = 0, high = right_idx - 1;
                        while (low <= high) {
                            mid = (low + high) >>> 1;
                            result = backingMap.cmp(object, key, keys[mid]);
                            if (result > 0) {
                                foundNode = node;
                                foundIndex = mid;
                                low = mid + 1;
                            } else if (result == 0) {
                                foundNode = node;
                                foundIndex = mid;
                                break TOP_LOOP;
                            } else {
                                high = mid - 1;
                            }
                        }
                        break TOP_LOOP;
                    }
                }
            }
            if (foundNode != null
                    && !checkLowerBound(foundNode.keys[foundIndex])) {
                foundNode = null;
            }
            lastKeyNode = foundNode;
            lastKeyIndex = foundIndex;
            lastKeyModCount = backingMap.modCount;
        }

        public K lastKey() {
            if (backingMap.size > 0 && !(startKey.equals(endKey))) {
                if (!hasEnd) {
                    Node<K, V> node = maximum(backingMap.root);
                    if (node != null
                            && checkLowerBound(node.keys[node.right_idx])) {
                        return node.keys[node.right_idx];
                    }
                } else {
                    setLastKey();
                    if (lastKeyNode != null) {
                        java.lang.Comparable<K> object = backingMap.comparator == null ? toComparable((K) endKey)
                                : null;
                        if (backingMap.cmp(object,  endKey, lastKeyNode.keys[lastKeyIndex]) != 0) {
                            return lastKeyNode.keys[lastKeyIndex];
                        } else {
						        // according to subMap, it excludes the last element 
                            if (lastKeyIndex != lastKeyNode.left_idx) {
                                object = backingMap.comparator == null ? toComparable((K) startKey)
                                        : null;
							    // check if the element is smaller than the startkey, there's no lastkey
                                if (backingMap.cmp(object,  startKey, lastKeyNode.keys[lastKeyIndex-1]) <= 0){
                                    return lastKeyNode.keys[lastKeyIndex - 1];
                                }
                            } else {
                                Node<K,V> last = lastKeyNode.prev;
                                if (last != null) {
                                    return last.keys[last.right_idx];
                                }
                            }
                        }
                    }
                }
            }
            throw new NoSuchElementException();
        }

        @Override
        public V put(K key, V value) {
            if (isInRange(key)) {
                return backingMap.put(key, value);
            }
            throw new IllegalArgumentException();
        }

        @SuppressWarnings("unchecked")
        @Override
        public V remove(Object key) {
            if (isInRange((K) key)) {
                return backingMap.remove(key);
            }
            return null;
        }

        public SortedMap<K, V> subMap(K startKey, K endKey) {
            checkRange(startKey);
            Comparator<? super K> cmp = backingMap.comparator;
            if (cmp == null) {
                java.lang.Comparable<K> object = toComparable(endKey);
                if (hasStart && object.compareTo(startKey) < 0) {
                    throw new IllegalArgumentException();
                }
                if (hasEnd && object.compareTo(endKey) > 0) {
                    throw new IllegalArgumentException();
                }
            } else {
                if (hasStart
                        && backingMap.comparator().compare(endKey, this.startKey) < 0) {
                    throw new IllegalArgumentException();
                }
                if (hasEnd && backingMap.comparator().compare(endKey,this.endKey) > 0) {
                    throw new IllegalArgumentException();
                }
            }
            Comparator<? super K> c = backingMap.comparator();
            if (c == null) {
                if (toComparable(startKey).compareTo(endKey) <= 0) {
                    return new SubMap<K, V>(startKey, backingMap, endKey);
                }
            } else {
                if (c.compare(startKey, endKey) <= 0) {
                    return new SubMap<K, V>(startKey, backingMap, endKey);
                }
            }
            throw new IllegalArgumentException();
        }

        public SortedMap<K, V> tailMap(K startKey) {
            checkRange(startKey);
            if (hasEnd) {
                return new SubMap<K, V>(startKey, backingMap, endKey);
            }
            return new SubMap<K, V>(startKey, backingMap);
        }

        @Override
        public Collection<V> values() {
            if (valuesCollection == null) {
                valuesCollection = new SubMapValuesCollection<K, V>(this);
            }
            return valuesCollection;
        }

        public int size() {
            Node<K, V> from, to;
            int fromIndex, toIndex;
            if (hasStart) {
                setFirstKey();
                from = firstKeyNode;
                fromIndex = firstKeyIndex;
            } else {
                from = minimum(backingMap.root);
                fromIndex = from == null ? 0 : from.left_idx;
            }
            if (from == null) {
                return 0;
            }
            if (hasEnd) {
                setLastKey();
                to = lastKeyNode;
                toIndex = lastKeyIndex;
                java.lang.Comparable<K> object = backingMap.comparator == null ? toComparable((K) endKey)
                        : null;
                if (to == null){
                    return 0;
                } else  if (backingMap.cmp(object, endKey, to.keys[toIndex]) != 0) {
                    if (toIndex != to.keys.length) {
                        toIndex++;
                    } else {
                        to = to.next;
                        toIndex = to == null ? 0 : to.left_idx;
                    }
                }
            } else {
                to = maximum(backingMap.root);
                toIndex = to == null ? 0 : to.right_idx;
            }
            if (to == null) {
                return 0;
            }
            // the last element of submap if exist should be ignored
            if (from == to) {
                return toIndex - fromIndex + (hasEnd ? 0 : 1);
            }
            int sum = 0;
            while (from != to) {
                sum += (from.right_idx - fromIndex + 1);
                from = from.next;
                fromIndex = from.left_idx;
            }
            return sum + toIndex - fromIndex + (hasEnd ? 0 : 1);
        }
    }
    
    static class SubMapValuesCollection<K, V> extends AbstractCollection<V> {
        SubMap<K, V> subMap;

        public SubMapValuesCollection(SubMap<K, V> subMap) {
            this.subMap = subMap;
        }

        @Override
        public boolean isEmpty() {
            return subMap.isEmpty();
        }

        @Override
        public Iterator<V> iterator() {
            Node<K, V> from;
            int fromIndex;
            if (subMap.hasStart) {
                subMap.setFirstKey();
                from = subMap.firstKeyNode;
                fromIndex = subMap.firstKeyIndex;
            } else {
                from = minimum(subMap.backingMap.root);
                fromIndex = from != null ? from.left_idx : 0;
            }
            if (!subMap.hasEnd) {
                return new UnboundedValueIterator<K, V>(subMap.backingMap,
                        from, from == null ? 0 : fromIndex);
            }
            subMap.setLastKey();
            Node<K, V> to = subMap.lastKeyNode;
            int toIndex = subMap.lastKeyIndex
            + (subMap.lastKeyNode != null
                    && (!subMap.lastKeyNode.keys[subMap.lastKeyIndex].equals(subMap.endKey)) ? 1
                    : 0);
            if (to != null
                    && toIndex > to.right_idx) {
                to = to.next;
                toIndex = to != null ? to.left_idx : 0;
                if  (to == null){
                    // has endkey but it does not exist, thus return UnboundedValueIterator
                    return new UnboundedValueIterator<K, V>(subMap.backingMap,
                            from, from == null ? 0 : fromIndex);
                }
            }
            return new BoundedValueIterator<K, V>(from, from == null ? 0
                    : fromIndex, subMap.backingMap, to,
                    to == null ? 0 : toIndex);
        }

        @Override
        public int size() {
            return subMap.size();
        }
    }
    
    static class BoundedMapIterator<K, V> extends AbstractMapIterator<K, V> {

        Node<K, V> finalNode;

        int finalOffset;

        BoundedMapIterator(Node<K, V> startNode, int startOffset,
                TreeMap<K, V> map, Node<K, V> finalNode, int finalOffset) {
            super(map, startNode, startOffset);
            if (startNode ==null &&  finalNode == null){
                // no elements
                node = null;
                return;
            }
            if (finalNode != null) {
                this.finalNode = finalNode;
                this.finalOffset = finalOffset;
            } else {
                Entry<K, V> entry = map.findBiggestEntry();
                if (entry != null) {
                    this.finalNode = entry.node;
                    this.finalOffset = entry.index;
                } else {
                    node = null;
                    return;
                }
            }
            if (startNode != null) {
                if (node == this.finalNode && offset >= this.finalOffset) {
                    node = null;
                } else if (this.finalOffset < this.finalNode.right_idx) {
                    java.lang.Comparable<K> object = backingMap.comparator == null ? toComparable((K) startNode.keys[startOffset])
                            : null;
                    if (this.backingMap.cmp(object, node.keys[offset],
                            this.finalNode.keys[this.finalOffset]) > 0) {
                        node = null;
                    }
                }
            }
        }

        BoundedMapIterator(Node<K, V> startNode, TreeMap<K, V> map,
                Node<K, V> finalNode, int finalOffset) {
            this(startNode, startNode.left_idx, map, finalNode, finalOffset);
        }

        BoundedMapIterator(Node<K, V> startNode, int startOffset,
                TreeMap<K, V> map, Node<K, V> finalNode) {
            this(startNode, startOffset, map, finalNode, finalNode.right_idx);
        }

        void makeBoundedNext() {
            if (node != null){
                boolean endOfIterator = lastNode == finalNode && lastOffset == finalOffset;
                if (endOfIterator) {
                    node = null;
                } else {
                    makeNext();
                }
            }
        }
        
        public boolean hasNext(){
            if (finalNode == node && finalOffset == offset) {
                node = null;
            }
            return node != null;
        }
    }

    static class BoundedEntryIterator<K, V> extends BoundedMapIterator<K, V>
            implements Iterator<Map.Entry<K, V>> {

        public BoundedEntryIterator(Node<K, V> startNode, int startOffset,
                TreeMap<K, V> map, Node<K, V> finalNode, int finalOffset) {
            super(startNode, startOffset, map, finalNode, finalOffset);
        }

        public Map.Entry<K, V> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            makeBoundedNext();
            int idx = lastOffset;
            return new MapEntry<K, V>(lastNode.keys[idx], lastNode.values[idx]);
        }
    }

    static class BoundedKeyIterator<K, V> extends BoundedMapIterator<K, V>
            implements Iterator<K> {

        public BoundedKeyIterator(Node<K, V> startNode, int startOffset,
                TreeMap<K, V> map, Node<K, V> finalNode, int finalOffset) {
            super(startNode, startOffset, map, finalNode, finalOffset);
        }

        public K next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            makeBoundedNext();
            return lastNode.keys[lastOffset];
        }
    }

    static class BoundedValueIterator<K, V> extends BoundedMapIterator<K, V>
            implements Iterator<V> {

        public BoundedValueIterator(Node<K, V> startNode, int startOffset,
                TreeMap<K, V> map, Node<K, V> finalNode, int finalOffset) {
            super(startNode, startOffset, map, finalNode, finalOffset);
        }

        public V next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            makeBoundedNext();
            return lastNode.values[lastOffset];
        }
    }
    
    static class SubMapKeySet<K, V> extends AbstractSet<K> implements Set<K> {
        SubMap<K, V> subMap;

        SubMapKeySet(SubMap<K, V> map) {
            subMap = map;
        }

        @Override
        public boolean contains(Object object) {
            return subMap.containsKey(object);
        }

        @Override
        public boolean isEmpty() {
            return subMap.isEmpty();
        }

        @Override
        public int size() {
            return subMap.size();
        }

        @Override
        public boolean remove(Object object) {
            return subMap.remove(object) != null;
        }

        public Iterator<K> iterator() {
            Node<K, V> from;
            int fromIndex;
            if (subMap.hasStart) {
                subMap.setFirstKey();
                from = subMap.firstKeyNode;
                fromIndex = subMap.firstKeyIndex;
            } else {
                from = minimum(subMap.backingMap.root);
                fromIndex = from != null ? from.left_idx : 0;
            }
            if (from == null){
                return new BoundedKeyIterator<K, V>(null, 0, subMap.backingMap, null, 0);
            }
            if (!subMap.hasEnd) {
                return new UnboundedKeyIterator<K, V>(subMap.backingMap,
                        from, from == null ? 0 : from.right_idx - fromIndex);
            }
            subMap.setLastKey();
            Node<K, V> to = subMap.lastKeyNode;
            java.lang.Comparable<K> object = subMap.backingMap.comparator == null ? toComparable((K) subMap.endKey)
                    : null;
            int toIndex = subMap.lastKeyIndex
                    + (subMap.lastKeyNode != null
                            && (!subMap.lastKeyNode.keys[subMap.lastKeyIndex].equals(subMap.endKey)) ? 1
                            : 0);
            if (subMap.lastKeyNode != null && toIndex > subMap.lastKeyNode.right_idx){
                to = to.next;
                toIndex = to != null ? to.left_idx : 0;
            }
            // no intial nor end key, return a unbounded iterator
            if (to == null) {
                return new UnboundedKeyIterator(subMap.backingMap, from,fromIndex);
            } else 
            return new BoundedKeyIterator<K, V>(from, from == null ? 0
                    : fromIndex, subMap.backingMap, to,
                    to == null ? 0 : toIndex);
        }
    }


    /*
     * Entry set of sub-maps, must override methods which check the range. add
     * or addAll operations are disabled by default.
     */
    static class SubMapEntrySet<K, V> extends AbstractSet<Map.Entry<K, V>>
            implements Set<Map.Entry<K, V>> {
        SubMap<K, V> subMap;

        SubMapEntrySet(SubMap<K, V> map) {
            subMap = map;
        }

        @Override
        public boolean isEmpty() {
            return subMap.isEmpty();
        }

        public Iterator<Map.Entry<K, V>> iterator() {
            Node<K, V> from;
            int fromIndex;
            if (subMap.hasStart) {
                subMap.setFirstKey();
                from = subMap.firstKeyNode;
                fromIndex = subMap.firstKeyIndex;
            } else {
                from = minimum(subMap.backingMap.root);
                fromIndex = from != null ? from.left_idx : 0;
            }
            if (from == null){
                return new BoundedEntryIterator<K, V>(null, 0, subMap.backingMap, null, 0);
            }
            if (!subMap.hasEnd) {
                return new UnboundedEntryIterator<K, V>(subMap.backingMap,
                        from, from == null ? 0 : from.right_idx - fromIndex);
            }
            subMap.setLastKey();
            Node<K, V> to = subMap.lastKeyNode;
            java.lang.Comparable<K> object = subMap.backingMap.comparator == null ? toComparable((K) subMap.endKey)
                    : null;
            int toIndex = subMap.lastKeyIndex
                    + (subMap.lastKeyNode != null
                            && (!subMap.lastKeyNode.keys[subMap.lastKeyIndex].equals(subMap.endKey)) ? 1
                            : 0);
            if (subMap.lastKeyNode != null && toIndex > subMap.lastKeyNode.right_idx){
                to = to.next;
                toIndex = to != null ? to.left_idx : 0;
            }
            if (to == null) {
                return new UnboundedEntryIterator(subMap.backingMap, from,fromIndex);
            } else 
            return new BoundedEntryIterator<K, V>(from, from == null ? 0
                    : fromIndex, subMap.backingMap, to,
                    to == null ? 0 : toIndex);
        }

        @Override
        public int size() {
            return subMap.size();
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean contains(Object object) {
            if (object instanceof Map.Entry) {
                Map.Entry<K, V> entry = (Map.Entry<K, V>) object;
                K key = entry.getKey();
                if (subMap.isInRange(key)) {
                    V v1 = subMap.get(key), v2 = entry.getValue();
                    return v1 == null ? ( v2 == null && subMap.containsKey(key) ) : v1.equals(v2);
                }
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean remove(Object object) {
            if (contains(object)) {
                Map.Entry<K, V> entry = (Map.Entry<K, V>) object;
                K key = entry.getKey();
                subMap.remove(key);
                return true;
            }
            return false;
        }
    }
    
    static class AscendingSubMapEntrySet<K, V> extends
            AbstractSet<Map.Entry<K, V>> implements NavigableSet<Map.Entry<K, V>> {
        
        boolean hasStart,hasEnd,startInclusive,endInclusive;
        
        java.util.Map.Entry<K,V> startEntry, lastentry; 
        
        NavigableSubMap<K, V> map;

        AscendingSubMapEntrySet(NavigableSubMap<K, V> map) {
            this.map = map;
        }
        
        AscendingSubMapEntrySet(NavigableSubMap<K, V> map,
                java.util.Map.Entry<K,V>  startEntry, boolean startInclusive,
                java.util.Map.Entry<K,V>  endEntry, boolean endInclusive) {
            if (startEntry != null) {
                hasStart = true;
                this.startEntry = startEntry;
                this.startInclusive = startInclusive;
            }
            if (endEntry != null) {
                hasEnd = true;
                this.lastentry = endEntry;
                this.endInclusive = endInclusive;
            }
            if (startEntry != null && endEntry != null) {
                this.map = (NavigableSubMap<K, V>) map.subMap(startEntry
                        .getKey(), startInclusive, endEntry.getKey(),
                        endInclusive);
                return;
            }
            if (startEntry != null) {
                this.map = (NavigableSubMap<K, V>) map.tailMap(startEntry
                        .getKey(), startInclusive);
                return;
            }
            if (endEntry != null) {
                this.map = (NavigableSubMap<K, V>) map.headMap(endEntry
                        .getKey(), endInclusive);
                return;
            }
            this.map = map;
        }

        @Override
        public final Iterator<Map.Entry<K, V>> iterator() {
            return new AscendingSubMapEntryIterator<K, V>(map);
        }

        @Override
        public int size() {
            int size = 0;
            Iterator it = new AscendingSubMapEntryIterator<K, V>(map);
            while (it.hasNext()){
                it.next();
                size ++;
            }
            return size;
        }

        public java.util.Map.Entry<K, V> ceiling(java.util.Map.Entry<K, V> e) {
            Map.Entry<K, V> entry = map.ceilingEntry(e.getKey());
            if (entry != null && map.isInRange(entry.getKey())){
                return entry;
            } else {
                return null;
            }
        }

        public Iterator<java.util.Map.Entry<K, V>> descendingIterator() {
            return new DescendingSubMapEntrySet<K, V>(map.descendingSubMap()).iterator();
        }

        public NavigableSet<java.util.Map.Entry<K, V>> descendingSet() {
            return new DescendingSubMapEntrySet<K, V>(map.descendingSubMap());
        }

        public java.util.Map.Entry<K, V> floor(java.util.Map.Entry<K, V> e) {
            Map.Entry<K, V> entry = map.floorEntry(e.getKey());
            if (entry!= null && map.isInRange(entry.getKey())){
                return entry;
            } else {
                return null;
            }
        }

        public NavigableSet<java.util.Map.Entry<K, V>> headSet(
                java.util.Map.Entry<K, V> end, boolean endInclusive) {
            boolean isInRange = true;
            int result;
            K endKey = end.getKey();
            if (map.toEnd) {
                result = (null != comparator()) ? comparator().compare(endKey,
                        map.hi) : toComparable(endKey).compareTo(map.hi);
                isInRange = (!map.hiInclusive && endInclusive) ? result < 0
                        : result <= 0;
            }
            if (map.fromStart) {
                result = (null != comparator()) ? comparator().compare(endKey,
                        map.lo) : toComparable(endKey).compareTo(map.lo);
                isInRange = isInRange
                        && ((!map.loInclusive && endInclusive) ? result > 0
                                : result >= 0);
            }
            if (isInRange) {
                    return new AscendingSubMapEntrySet<K, V>(map, null, false,
                            end, endInclusive);
            }
            throw new IllegalArgumentException();
        }

        public java.util.Map.Entry<K, V> higher(java.util.Map.Entry<K, V> e) {
            Comparator<? super K> cmp = map.m.comparator;
            if (cmp == null) {
                java.lang.Comparable<K> object = toComparable(e.getKey());
                if (hasStart && object.compareTo(startEntry.getKey()) < 0) {
                    return map.higherEntry(startEntry.getKey());
                }
                if (hasEnd && object.compareTo(lastentry.getKey()) >= 0) {
                    return null;
                }
            } else {
                if (hasStart && cmp.compare(e.getKey(), startEntry.getKey()) < 0) {
                    return map.higherEntry(startEntry.getKey());
                }
                if (hasEnd && cmp.compare(e.getKey(), lastentry.getKey()) >= 0) {
                    return null;
                }
            }
            return map.higherEntry(e.getKey());
        }

        public java.util.Map.Entry<K, V> lower(java.util.Map.Entry<K, V> e) {
            Comparator<? super K> cmp = map.m.comparator;
            if (cmp == null) {
                java.lang.Comparable<K> object = toComparable(e.getKey());
                if (hasStart && object.compareTo(startEntry.getKey()) < 0) {
                    return null;
                }
                if (hasEnd && object.compareTo(lastentry.getKey()) >= 0) {
                    return map.lowerEntry(lastentry.getKey());
                }
            } else {
                if (hasStart && cmp.compare(e.getKey(), startEntry.getKey()) < 0) {
                    return null;
                }
                if (hasEnd && cmp.compare(e.getKey(), lastentry.getKey()) >= 0) {
                    return map.lowerEntry(lastentry.getKey());
                }
            }
            return map.lowerEntry(e.getKey());
        }

        public java.util.Map.Entry<K, V> pollFirst() {
            Map.Entry<K, V> ret = map.firstEntry(); 
            if (ret == null){
                return null;
            }
            map.m.remove(ret.getKey());
            return ret;
        }

        public java.util.Map.Entry<K, V> pollLast() {
            Map.Entry<K, V> ret = map.lastEntry(); 
            if (ret == null){
                return null;
            }
            map.m.remove(ret.getKey());
            return ret;
        }

        public NavigableSet<java.util.Map.Entry<K, V>> subSet(java.util.Map.Entry<K, V> start, boolean startInclusive, java.util.Map.Entry<K, V> end, boolean endInclusive) {
            if (map.m.keyCompare(start.getKey(), end.getKey()) > 0) {
                throw new IllegalArgumentException();
            }
            if (map.fromStart
                    && ((!map.loInclusive && endInclusive) ? map.m.keyCompare(end.getKey(), map.lo) <= 0
                            : map.m.keyCompare(end.getKey(), map.lo) < 0)) {
                throw new IllegalArgumentException();
            }
            if (map.toEnd
                    && ((!map.hiInclusive && startInclusive) ? map.m.keyCompare(start.getKey(), map.hi) >= 0
                            : map.m.keyCompare(start.getKey(), map.hi) > 0)) {
                throw new IllegalArgumentException();
            }
            return new AscendingSubMapEntrySet<K, V>(map, start, startInclusive, end, endInclusive);
        }

        public NavigableSet<java.util.Map.Entry<K, V>> tailSet(java.util.Map.Entry<K, V> start, boolean startInclusive) {
            boolean isInRange = true;
            int result;
            if (map.toEnd) {
                result = (null != comparator()) ? comparator().compare(start.getKey(),
                        map.hi) : toComparable(start.getKey()).compareTo(map.hi);
                isInRange = (map.hiInclusive || !startInclusive) ? result <= 0
                        : result < 0;
            }
            if (map.fromStart) {
                result = (null != comparator()) ? comparator().compare(start.getKey(),
                        map.lo) : toComparable(start.getKey()).compareTo(map.lo);
                isInRange = isInRange
                        && ((map.loInclusive || !startInclusive) ? result >= 0
                                : result > 0);
            }

            if (isInRange) {
                return new AscendingSubMapEntrySet<K, V>(map, start, startInclusive, null, false);
            }
            throw new IllegalArgumentException();
            
        }

        public Comparator comparator() {
            return map.m.comparator;
        }

        public java.util.Map.Entry<K, V> first() {
            if (hasStart){
                if (startInclusive){
                    return startEntry;
                } else {
                    return map.floorEntry(startEntry.getKey());
                }
            }
            java.util.Map.Entry<K, V> ret = map.firstEntry(); 
            if (ret == null){
                throw new NoSuchElementException();
            }
            return ret;
        }

        public SortedSet<java.util.Map.Entry<K, V>> headSet(java.util.Map.Entry<K, V> end) {
            return headSet(end, false);
        }

        public java.util.Map.Entry<K, V> last() {
            if (hasEnd){
                if (endInclusive){
                    return lastentry;
                } else {
                    return map.ceilingEntry(lastentry.getKey());
                }
            }
            java.util.Map.Entry<K, V> ret = map.lastEntry();
            if (ret == null){
                throw new NoSuchElementException();
            }
            return ret;
        }

        public SortedSet<java.util.Map.Entry<K, V>> subSet(java.util.Map.Entry<K, V> start, java.util.Map.Entry<K, V> end) {
            if ((null != comparator()) ? comparator().compare(start.getKey(), end.getKey()) > 0
                    : toComparable(start.getKey()).compareTo(end.getKey()) > 0) {
                throw new IllegalArgumentException();
            }
            if (!map.isInRange(start.getKey())) {
                throw new IllegalArgumentException();
            }
            if (!map.isInRange(end.getKey())) {
                throw new IllegalArgumentException();
            }
            return new AscendingSubMapEntrySet<K, V>(map, start, false, end, false);
        }

        public SortedSet<java.util.Map.Entry<K, V>> tailSet(java.util.Map.Entry<K, V> start) {
            return tailSet(start, false);
        }
    }

    static class DescendingSubMapEntrySet<K, V> extends
            AbstractSet<Map.Entry<K, V>> implements NavigableSet<Map.Entry<K, V>> {
        NavigableSubMap<K, V> map;
        
        DescendingSubMapEntrySet(NavigableSubMap<K, V> map) {
            this.map = map;
        }

        @Override
        public final Iterator<Map.Entry<K, V>> iterator() {
            return new DescendingSubMapEntryIterator<K, V>(map);
        }

        @Override
        public int size() {
            int size = 0;
            Iterator it = new DescendingSubMapEntryIterator<K, V>(map);
            while (it.hasNext()){
                it.next();
                size ++;
            }
            return size;
        }

        public java.util.Map.Entry<K, V> ceiling(java.util.Map.Entry<K, V> e) {
            Entry<K,V> node =  map.m.findFloorEntry(e.getKey());
            if (!map.checkUpperBound(node.key)){
                node = map.findEndNode();
            }
            
            if (!map.checkLowerBound(node.key)){
                java.lang.Comparable<K> object = map.comparator() == null ? toComparable((K) e.getKey())
                        : null;
                TreeMap.Entry<K, V> first = map.loInclusive ? map.m.findFloorEntry(map.lo) :map.m.findLowerEntry(map.lo);
                if (first != null && map.cmp(object, e.getKey(), first.getKey()) <= 0){
                    node = first;
                } else {
                    node = null;
                }
            }
            return node;
        }

        public Iterator<java.util.Map.Entry<K, V>> descendingIterator() {
            return descendingSet().iterator();
        }

        public NavigableSet<java.util.Map.Entry<K, V>> descendingSet() {
            if (map.fromStart && map.toEnd) {
                return new AscendingSubMapEntrySet<K, V>(
                        new AscendingSubMap<K, V>(map.hi, map.hiInclusive,
                                map.m, map.lo, map.loInclusive));
            }
            if (map.fromStart) {
                return new AscendingSubMapEntrySet<K, V>(
                        new AscendingSubMap<K, V>(map.m, map.lo,
                                map.loInclusive));
            }
            if (map.toEnd) {
                return new AscendingSubMapEntrySet<K, V>(
                        new AscendingSubMap<K, V>(map.hi, map.hiInclusive,
                                map.m));
            }
            return new AscendingSubMapEntrySet<K, V>(new AscendingSubMap<K, V>(
                    map.m));
        }

        public java.util.Map.Entry<K, V> floor(java.util.Map.Entry<K, V> e) {
            Entry<K,V> node =  map.m.findCeilingEntry(e.getKey());
            if (!map.checkUpperBound(node.key)){
                node = map.findEndNode();
            }
            
            if (!map.checkLowerBound(node.key)){
                java.lang.Comparable<K> object = map.m.comparator == null ? toComparable((K) e.getKey())
                        : null;
                TreeMap.Entry<K, V> first = map.hiInclusive ? map.m.findCeilingEntry(map.hi) :map.m.findHigherEntry(map.hi);
                if (first != null && map.cmp(object, e.getKey(), first.getKey()) < 0){
                    node = first;
                } else {
                    node = null;
                }
            }
            return node;
        }
        
        void checkInRange(K key, boolean keyInclusive){
            boolean isInRange = true;
            int result = 0;
            if (map.toEnd) {
                result = (null != map.comparator()) ? comparator().compare(
                        key, map.hi) : toComparable(key).compareTo(map.hi);
                isInRange = ((!map.hiInclusive) && keyInclusive) ? result < 0
                        : result <= 0;
            }
            if (map.fromStart) {
                result = (null != comparator()) ? comparator().compare(key,
                        map.lo) : toComparable(key).compareTo(map.lo);
                isInRange = isInRange
                        && (((!map.loInclusive) && keyInclusive) ? result > 0
                                : result >= 0);
            }
            if (!isInRange){
                throw new IllegalArgumentException();
            }
        }
        
        public NavigableSet<java.util.Map.Entry<K, V>> headSet(
                java.util.Map.Entry<K, V> end, boolean endInclusive) {
            boolean outRange = true;
            int result = 0;
            if (map.toEnd) {
                result = (null != map.comparator()) ? comparator().compare(
                        end.getKey(), map.hi) : toComparable(end.getKey()).compareTo(map.hi);
                outRange = ((!map.hiInclusive) && endInclusive) ? result >= 0
                        : result > 0;
                 if (outRange){
                            throw new IllegalArgumentException();
                 }
            }
            if (map.fromStart) {
                result = (null != comparator()) ? comparator().compare(end.getKey(),
                        map.lo) : toComparable(end.getKey()).compareTo(map.lo);
                outRange = (((!map.loInclusive) && endInclusive) ? result <= 0
                                : result < 0);
                if (outRange){
                    throw new IllegalArgumentException();
                }
            }
            
            if (map.fromStart) {
                return new DescendingSubMapEntrySet<K, V>(new DescendingSubMap<K, V>(
                        map.lo, map.loInclusive, map.m, end.getKey(), endInclusive));
            } else {
                return new DescendingSubMapEntrySet<K, V>(new DescendingSubMap<K, V>(
                        map.m, end.getKey(), endInclusive));
            }
        }

        public java.util.Map.Entry<K, V> higher(java.util.Map.Entry<K, V> e) {
            Entry<K,V> node =  map.m.findLowerEntry(e.getKey());
            if (node != null && !map.checkUpperBound(node.key)){
                node =  map.hiInclusive? map.findFloorEntryImpl(map.hi):map.findLowerEntryImpl(map.hi);
                }
            java.lang.Comparable<K> object = map.comparator() == null ? toComparable((K) e.getKey())
                    : null;
            if (node != null && (map.cmp(object, e.getKey(), node.key)) > 0){
                return null;
            } 
            if (node!=null && !map.checkLowerBound(node.key)){
                TreeMap.Entry<K, V> first = map.loInclusive ? map.m.findFloorEntry(map.lo) :map.m.findLowerEntry(map.lo);
                if (first != null && map.cmp(object, e.getKey(), first.getKey()) < 0){
                    node = first;
                } else {
                    node = null;
                }
            }
            return node;
        }

        public java.util.Map.Entry<K, V> lower(java.util.Map.Entry<K, V> e) {
            Entry<K,V> node =  map.m.findHigherEntry(e.getKey());
            if (node != null && !map.checkUpperBound(node.key)){
                node =  map.loInclusive? map.findCeilingEntryImpl(map.hi):map.findHigherEntryImpl(map.hi);
            }
            java.lang.Comparable<K> object = map.m.comparator == null ? toComparable((K) e.getKey())
                    : null;
            if (node != null && (map.cmp(object, e.getKey(), node.key)) >= 0){
                return null;
            } 
            if (node!=null &&!map.checkLowerBound(node.key)){
                Map.Entry<K, V> first = map.firstEntry();
                if (first != null && map.cmp(object, e.getKey(),first.getKey()) < 0){
                    node = map.findStartNode();
                } else {
                    node = null;
                }
            }
            return node;
        }

        public java.util.Map.Entry<K, V> pollFirst() {
            Map.Entry<K, V> ret = map.lastEntry();
            if (ret == null){
                return null;
            }
            map.m.remove(ret.getKey());
            return ret;
        }

        public java.util.Map.Entry<K, V> pollLast() {
            Map.Entry<K, V> ret = map.firstEntry();
            if (ret == null){
                return null;
            }
            map.m.remove(ret.getKey());
            return ret;
        }

        public NavigableSet<java.util.Map.Entry<K, V>> subSet(
                java.util.Map.Entry<K, V> start, boolean startInclusive,
                java.util.Map.Entry<K, V> end, boolean endInclusive) {
            java.lang.Comparable<K> startobject = map.comparator() == null ? toComparable((K) start
                    .getKey())
                    : null;
            java.lang.Comparable<K> endobject = map.comparator() == null ? toComparable((K) end
                    .getKey())
                    : null;
            if (map.fromStart
                    && ((!map.loInclusive && startInclusive) ? map.cmp(
                            startobject, start.getKey(), map.lo) <= 0 : map
                            .cmp(startobject, start.getKey(), map.lo) < 0)
                    || (map.toEnd && ((!map.hiInclusive && endInclusive) ? map
                            .cmp(endobject, end.getKey(), map.hi) >= 0 : map
                            .cmp(endobject, end.getKey(), map.hi) > 0))) {
                throw new IllegalArgumentException();
            }
            if (map.cmp(startobject, start.getKey(), end.getKey()) > 0) {
                throw new IllegalArgumentException();
            }

            return new DescendingSubMapEntrySet<K, V>(
                    new DescendingSubMap<K, V>(start.getKey(), startInclusive,
                            map.m, end.getKey(), endInclusive));
        }

        public NavigableSet<java.util.Map.Entry<K, V>> tailSet(
                java.util.Map.Entry<K, V> start, boolean startInclusive) {
            if (map.toEnd) {
                return new DescendingSubMapEntrySet<K, V>(
                        new DescendingSubMap<K, V>(start.getKey(),
                                startInclusive, map.m, map.hi, map.hiInclusive));
            } else {
                return new DescendingSubMapEntrySet<K, V>(
                        new DescendingSubMap<K, V>(start.getKey(),
                                startInclusive, map.m));
            }
        }

        public Comparator comparator() {
            return map.comparator();
        }

        public java.util.Map.Entry<K, V> first() {
            java.util.Map.Entry<K, V> ret = map.lastEntry();
            if (ret == null){
                throw new NoSuchElementException();
            }
            return ret;
        }

        public SortedSet<java.util.Map.Entry<K, V>> headSet(java.util.Map.Entry<K, V> end) {
            return headSet(end,false);
        }

        public java.util.Map.Entry<K, V> last() {
            java.util.Map.Entry<K, V> ret = map.firstEntry();
            if (ret == null){
                throw new NoSuchElementException();
            }
            return ret;
        }

        public SortedSet<java.util.Map.Entry<K, V>> subSet(
                java.util.Map.Entry<K, V> start, java.util.Map.Entry<K, V> end) {
                return subSet(start, true, end, false);
        }
        
        int keyCompare(K left, K right) {
            return (null != map.comparator()) ? map.comparator().compare(left,
                    right) : toComparable(left).compareTo(right);
        }

        public SortedSet<java.util.Map.Entry<K, V>> tailSet(java.util.Map.Entry<K, V> start) {
            return tailSet(start, true);
        }
    }

    static class AscendingSubMapKeySet<K,V> extends AbstractSet<K> implements NavigableSet<K>  {
        NavigableSubMap<K, V> map;
        
        AscendingSubMapKeySet(NavigableSubMap<K, V> map) {
            this.map = map;
        }

        @Override
        public final Iterator<K> iterator() {
            return new AscendingSubMapKeyIterator<K, V>(map);
        }

        public final Iterator<K> descendingIterator() {
            return new DescendingSubMapKeyIterator<K, V>(map.descendingSubMap());
        }

        @Override
        public int size() {
            int size = 0;
            Iterator it = new AscendingSubMapEntryIterator<K, V>(map);
            while (it.hasNext()){
                it.next();
                size ++;
            }
            return size;
        }

        public K ceiling(K e) {
            Entry<K,V> ret = map.findFloorEntry(e);
            if (ret != null && map.isInRange(ret.key)){
                return ret.key;
            } else {
                return null;
            }
        }

        public NavigableSet<K> descendingSet() {
            return new DescendingSubMapKeySet<K, V>(map.descendingSubMap());
        }

        public K floor(K e) {
            Entry<K,V> ret = map.findFloorEntry(e);
            if (ret != null && map.isInRange(ret.key)){
                return ret.key;
            } else {
                return null;
            }
        }

        public NavigableSet<K> headSet(K end, boolean endInclusive) {
            boolean isInRange = true;
                int result;
            if (map.toEnd) {
                result = (null != comparator()) ? comparator().compare(end,
                        map.hi) : toComparable(end).compareTo(map.hi);
                isInRange = (map.hiInclusive || !endInclusive) ? result <= 0
                        : result < 0;
            }
            if (map.fromStart) {
                result = (null != comparator()) ? comparator().compare(end,
                        map.lo) : toComparable(end).compareTo(map.lo);
                isInRange = isInRange
                        && ((map.loInclusive || !endInclusive) ? result >= 0
                                : result > 0);
            }
            if (isInRange) {
                if (map.fromStart) {
                    return new AscendingSubMapKeySet<K, V>(
                            new AscendingSubMap<K, V>(map.lo, map.loInclusive,
                                    map.m, end, endInclusive));
                } else {
                    return new AscendingSubMapKeySet<K, V>(
                            new AscendingSubMap<K, V>(map.m, end, endInclusive));
                }
            }
            throw new IllegalArgumentException();
        }

        public K higher(K e) {
            K ret = map.m.higherKey(e);
            if (ret != null && map.isInRange(ret)){
                return ret;
            } else {
                return null;
            }
        }

        public K lower(K e) {
            K ret =map.m.lowerKey(e);
            if (ret != null && map.isInRange(ret)){
                return ret;
            } else {
                return null;
            }
        }
        
        public K pollFirst() {
            Map.Entry<K, V> ret = map.firstEntry();
            if (ret == null){
                return null;
            }
            map.m.remove(ret.getKey());
            return ret.getKey();
        }

        public K pollLast() {
            Map.Entry<K, V> ret = map.lastEntry();
            if (ret == null){
                return null;
            }
            map.m.remove(ret.getKey());
            return ret.getKey();
        }

        public NavigableSet<K> subSet(K start, boolean startInclusive, K end, boolean endInclusive) {
            if (map.fromStart
                    && ((!map.loInclusive && startInclusive) ? map.m.keyCompare(start,
                            map.lo) <= 0 : map.m.keyCompare(start, map.lo) < 0)
                    || (map.toEnd && ((!map.hiInclusive && (endInclusive || (startInclusive && start.equals(end)))) ? map.m.keyCompare(
                            end, map.hi) >= 0
                            : map.m.keyCompare(end, map.hi) > 0))) {
                throw new IllegalArgumentException();
            }
            if (map.m.keyCompare(start, end) > 0){
                throw new IllegalArgumentException();
            }
            return new AscendingSubMapKeySet<K,V>(new AscendingSubMap<K, V>(start,
                    startInclusive, map.m, end, endInclusive));
        }

        public NavigableSet<K> tailSet(K start, boolean startInclusive) {
            boolean isInRange = true;
            int result;
            if (map.toEnd) {
                result = (null != comparator()) ? comparator().compare(start,
                        map.hi) : toComparable(start).compareTo(map.hi);
                isInRange = (map.hiInclusive || !startInclusive) ? result <= 0
                        : result < 0;
            }
            if (map.fromStart) {
                result = (null != comparator()) ? comparator().compare(start,
                        map.lo) : toComparable(start).compareTo(map.lo);
                isInRange = isInRange
                        && ((map.loInclusive || !startInclusive) ? result >= 0
                                : result > 0);
            }

            if (isInRange) {
                if (map.toEnd) {
                    return new AscendingSubMapKeySet<K, V>(
                            new AscendingSubMap<K, V>(start, startInclusive,
                                    map.m, map.hi, map.hiInclusive));
                } else {
                    return new AscendingSubMapKeySet<K, V>(
                            new AscendingSubMap<K, V>(start, startInclusive,
                                    map.m));
                }
            }
            throw new IllegalArgumentException();
        }

        public Comparator<? super K> comparator() {
            return map.m.comparator;
        }

        public K first() {
            return map.firstKey();
        }

        public SortedSet<K> headSet(K end) {
            return headSet(end, false);
        }

        public K last() {
            return map.lastKey();
        }

        public SortedSet<K> subSet(K start, K end) {
            return subSet(start,true, end,false);
        }

        public SortedSet<K> tailSet(K start) {
            return tailSet(start,true);
        }
        
        @Override
        public boolean contains(Object object) {
            return map.containsKey(object);
        }
        
        @Override
        public boolean remove(Object object) {
            return this.map.remove(object) != null;
        }
    }

    static class DescendingSubMapKeySet<K,V> extends AbstractSet<K> implements NavigableSet<K>  {
        NavigableSubMap<K, V> map;
        
        DescendingSubMapKeySet(NavigableSubMap<K, V> map) {
            this.map = map;
        }

        @Override
        public final Iterator<K> iterator() {
            return new DescendingSubMapKeyIterator<K, V>(map);
        }

        public final Iterator<K> descendingIterator() {
            if (map.fromStart && map.toEnd) {
                return new AscendingSubMapKeyIterator<K, V>(
                        new AscendingSubMap<K, V>(map.hi, map.hiInclusive,
                                map.m, map.lo, map.loInclusive));
            }
            if (map.toEnd) {
                return new AscendingSubMapKeyIterator<K, V>(
                        new AscendingSubMap<K, V>(map.hi, map.hiInclusive,
                                map.m));
            }
            if (map.fromStart) {
                return new AscendingSubMapKeyIterator<K, V>(
                        new AscendingSubMap<K, V>(map.m, map.lo,
                                map.loInclusive));
            }
            return new AscendingSubMapKeyIterator<K, V>(
                    new AscendingSubMap<K, V>(map.m));
        }

        @Override
        public int size() {
            int size = 0;
            Iterator it = new DescendingSubMapEntryIterator<K, V>(map);
            while (it.hasNext()){
                it.next();
                size ++;
            }
            return size;
        }

        public NavigableSet<K> descendingSet() {
            if (map.fromStart && map.toEnd) {
                return new AscendingSubMapKeySet<K, V>(
                        new AscendingSubMap<K, V>(map.hi, map.hiInclusive,
                                map.m, map.lo, map.loInclusive));
            }
            if (map.toEnd) {
                return new AscendingSubMapKeySet<K, V>(
                        new AscendingSubMap<K, V>(map.hi, map.hiInclusive,
                                map.m));
            }
            if (map.fromStart) {
                return new AscendingSubMapKeySet<K, V>(
                        new AscendingSubMap<K, V>(map.m, map.lo,
                                map.loInclusive));
            }
            return new AscendingSubMapKeySet<K, V>(
                    new AscendingSubMap<K, V>(map.m));
        }

        public K ceiling(K e) {
            java.lang.Comparable<K> object = map.comparator() == null ? toComparable((K) e)
                    : null;
            Entry<K,V> node =  map.m.findFloorEntry(e);
            if (node!= null && !map.checkUpperBound(node.key)){
                return null;
            }
            
            if (node!= null && !map.checkLowerBound(node.key)){
                Entry<K,V> first = map.loInclusive?map.m.findFloorEntry(map.lo):map.m.findLowerEntry(map.lo);
                if (first!= null  && map.cmp(object, e, first.key) <= 0 && map.checkUpperBound(first.key)){
                    node = first;
                } else {
                    node = null;
                }
            }
            return node ==null ? null : node.key;
        }

        public K floor(K e) {
            Entry<K,V> node =  map.m.findCeilingEntry(e);
            if (node!= null && !map.checkUpperBound(node.key)){
                node = map.hiInclusive?map.m.findCeilingEntry(map.hi):map.m.findHigherEntry(map.hi);
            }
            
            if (node!= null && !map.checkLowerBound(node.key)){
                java.lang.Comparable<K> object = map.comparator() == null ? toComparable((K) e)
                        : null;
                Entry<K,V> first = map.loInclusive?map.m.findFloorEntry(map.lo):map.m.findLowerEntry(map.lo);
                if (first != null && map.cmp(object, e, first.key) > 0 && map.checkUpperBound(first.key)){
                    node = first;
                } else {
                    node = null;
                }
            }
            return node ==null ? null :  node.key;
        }

        public NavigableSet<K> headSet(K end, boolean endInclusive) {
            checkInRange(end,endInclusive);
            if (map.fromStart) {
                return new DescendingSubMapKeySet<K, V>(
                        new DescendingSubMap<K, V>(map.lo, map.loInclusive,
                                map.m, end, endInclusive));
            } else {
                return new DescendingSubMapKeySet<K, V>(
                        new DescendingSubMap<K, V>(map.m, end, endInclusive));
            }
        }
        
        public K higher(K e) {
            java.lang.Comparable<K> object = map.comparator() == null ? toComparable((K) e)
                    : null;
            Entry<K,V> node =  map.m.findLowerEntry(e);
            if (node != null && !map.checkUpperBound(node.key)){
                    return null;
            }
            
            if (node != null && !map.checkLowerBound(node.key)){
                Entry<K,V> first = map.loInclusive?map.m.findFloorEntry(map.lo):map.m.findLowerEntry(map.lo);
                if (first!= null && map.cmp(object, e, first.key) < 0 && map.checkUpperBound(first.key)){
                    node = first;
                } else {
                    node = null;
                }
            }
            return node ==null ? null : node.key;
        }

        public K lower(K e) {
            Entry<K,V> node =  map.m.findHigherEntry(e);
            if (node != null && !map.checkUpperBound(node.key)){
                node = map.hiInclusive ? map.m.findCeilingEntry(map.hi) : map.m.findHigherEntry(map.hi);
            }
            
            if (node != null && !map.checkLowerBound(node.key)){
                java.lang.Comparable<K> object = map.comparator() == null ? toComparable((K) e)
                        : null;
                Entry<K,V> first = map.loInclusive?map.m.findFloorEntry(map.lo):map.m.findLowerEntry(map.lo);
                if (first!= null && map.cmp(object, e, first.key) > 0 && map.checkUpperBound(first.key)){
                    node = first;
                } else {
                    node = null;
                }
            }
            return node == null ? null : node.key;
        }
        
        public K pollFirst() {
            Map.Entry<K, V> ret = map.firstEntry();
            if (ret == null){
                return null;
            }
            map.m.remove(ret.getKey());
            return ret.getKey();
        }

        public K pollLast() {
            Map.Entry<K, V> ret = map.lastEntry();
            if (ret == null){
                return null;
            }
            map.m.remove(ret.getKey());
            return ret.getKey();
        }

        public NavigableSet<K> subSet(K start, boolean startInclusive, K end, boolean endInclusive) {
            checkInRange(start,startInclusive);
            checkInRange(end,endInclusive);
            if ((null != map.comparator()) ? map.comparator().compare(
                    start, end)>0 : toComparable(start).compareTo(end) > 0){
                throw new IllegalArgumentException();
            }
            return new DescendingSubMapKeySet<K,V>(new DescendingSubMap<K, V>(
                    start, startInclusive, map.m, end, endInclusive));
        }

        public NavigableSet<K> tailSet(K start, boolean startInclusive) {
            checkInRange(start,startInclusive);
            if (map.toEnd) {
                return new DescendingSubMapKeySet<K, V>(
                        new DescendingSubMap<K, V>(start, startInclusive,
                                map.m, map.hi, map.hiInclusive));
            } else {
                return new DescendingSubMapKeySet<K, V>(
                        new DescendingSubMap<K, V>(start, startInclusive, map.m));
            }
        }
        
        void checkInRange(K key, boolean keyInclusive){
            boolean isInRange = true;
            int result = 0;
            if (map.toEnd) {
                result = (null != map.comparator()) ? map.comparator().compare(
                        key, map.hi) : toComparable(key).compareTo(map.hi);
                isInRange = ((!map.hiInclusive) && keyInclusive) ? result < 0
                        : result <= 0;
            }
            if (map.fromStart) {
                result = (null != comparator()) ? comparator().compare(key,
                        map.lo) : toComparable(key).compareTo(map.lo);
                isInRange = isInRange
                        && (((!map.loInclusive) && keyInclusive) ? result > 0
                                : result >= 0);
            }
            if (!isInRange){
                throw new IllegalArgumentException();
            }
        }

        public Comparator<? super K> comparator() {
            return map.comparator();
        }

        public K first() {
            return map.firstKey();
        }

        public SortedSet<K> headSet(K end) {
            return headSet(end, false);
        }

        public K last() {
            return map.lastKey();
        }

        public SortedSet<K> subSet(K start, K end) {
            return subSet(start,true, end,false);
        }

        public SortedSet<K> tailSet(K start) {
            return tailSet(start,true);
        }
    }

    static abstract class NavigableSubMap<K, V> extends AbstractMap<K, V>
            implements NavigableMap<K, V> {
        
        final TreeMap<K, V> m;

        final K lo, hi;

        final boolean fromStart, toEnd;

        final boolean loInclusive, hiInclusive;

        NavigableSubMap(final K start, final boolean startKeyInclusive,
                final TreeMap<K, V> map, final K end,
                final boolean endKeyInclusive) {
            m = map;
            fromStart = toEnd = true;
            lo = start;
            hi = end;
            loInclusive = startKeyInclusive;
            hiInclusive = endKeyInclusive;
        }

        NavigableSubMap(final K start, final boolean startKeyInclusive,
                final TreeMap<K, V> map) {
            m = map;
            fromStart = true;
            toEnd = false;
            lo = start;
            hi = null;
            loInclusive = startKeyInclusive;
            hiInclusive = false;
        }
        
        NavigableSubMap(final TreeMap<K, V> map, final K end,
                final boolean endKeyInclusive) {
            m = map;
            fromStart = false;
            toEnd = true;
            lo = null;
            hi = end;
            loInclusive = false;
            hiInclusive = endKeyInclusive;
        }

        // the whole TreeMap
        NavigableSubMap(final TreeMap<K, V> map) {
            m = map;
            fromStart = toEnd = false;
            lo = hi = null;
            loInclusive = hiInclusive = false;
        }
        
        Node findNode(K key){
            return m.findNode(key);
        }

        /*
         * The basic public methods.
         */

        public Comparator<? super K> comparator() {
            return m.comparator();
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean containsKey(Object key) {
            checkNull(key);
            if (isInRange((K) key)) {
                return m.containsKey(key);
            }
            return false;
        }
        
        private void checkNull (Object key) {
            if (null == key && null ==comparator()){
                throw new NullPointerException();
            }
        }

        @Override
        public boolean isEmpty() {
            Iterator<K> it = this.keySet().iterator();
            if (it.hasNext()) {
                return false;
            }
            return true;
        }

        @Override
        public int size() {
            return entrySet().size();
        }

        @Override
        public V put(K key, V value) {
            checkNull(key);
            if (isInRange(key)) {
                return m.put(key, value);
            }
            throw new IllegalArgumentException();
        }

        @SuppressWarnings("unchecked")
        @Override
        public V get(Object key) {
            checkNull(key);
            if (isInRange((K) key)) {
                return m.get(key);
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public V remove(Object key) {
            checkNull(key);
            if (isInRange((K) key)) {
                return m.remove(key);
            }
            return null;
        }

        /*
         * The navigable methods.
         */

        public abstract Map.Entry<K, V> firstEntry();

        public abstract Map.Entry<K, V> lastEntry();

        public abstract Map.Entry<K, V> pollFirstEntry();

        public abstract Map.Entry<K, V> pollLastEntry();

        public abstract Map.Entry<K, V> higherEntry(K key);

        public abstract Map.Entry<K, V> lowerEntry(K key);

        public abstract Map.Entry<K, V> ceilingEntry(K key);

        public abstract Map.Entry<K, V> floorEntry(K key);
        
        abstract NavigableSubMap<K, V>descendingSubMap();

        public K firstKey() {
            Map.Entry<K, V> node = firstEntry();
            if (node != null) {
                return node.getKey();
            }
            throw new NoSuchElementException();
        }

        public K lastKey() {
            Map.Entry<K, V> node = lastEntry();
            if (node != null) {
                return node.getKey();
            }
            throw new NoSuchElementException();
        }

        public K higherKey(K key) {
            Map.Entry<K, V> entry = higherEntry(key);
            return (null == entry) ? null : entry.getKey();
        }

        public K lowerKey(K key) {
            Map.Entry<K, V> entry = lowerEntry(key);
            return (null == entry) ? null : entry.getKey();
        }

        public K ceilingKey(K key) {
            Map.Entry<K, V> entry = ceilingEntry(key);
            return (null == entry) ? null : entry.getKey();
        }

        public K floorKey(K key) {
            Map.Entry<K, V> entry = floorEntry(key);
            return (null == entry) ? null : entry.getKey();
        }

        /*
         * The sub-collection methods.
         */

        public abstract NavigableSet<K> navigableKeySet();

        @Override
        public abstract Set<Map.Entry<K, V>> entrySet();

        @Override
        public Set<K> keySet() {
            return navigableKeySet();
        }

        public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        public SortedMap<K, V> subMap(K start, K end) {
            // the exception check is special here, the end should not if equal
            // to endkey unless start is equal to end
            if (!checkLowerBound(start) || !checkUpperBound(start)){
                throw new IllegalArgumentException();
            }
            int result = -1;
            if (toEnd) {
                result = (null != comparator()) ? comparator().compare(end, hi)
                        : toComparable(end).compareTo(hi);
            }
            if (((!hiInclusive && start.equals(end)) ? result < 0 : result <= 0)){
                if ((null != comparator()) ? comparator().compare(start, end) > 0
                        : toComparable(start).compareTo(end) > 0) {
                    throw new IllegalArgumentException();
                }
                return new AscendingSubMap<K, V>(start, true, m, end,
                        false);
            }
            throw new IllegalArgumentException();
        }

        public SortedMap<K, V> headMap(K end) {
            if (toEnd) {
                int result = (null != comparator()) ? comparator().compare(end, hi)
                        : toComparable(end).compareTo(hi);
                if (result > 0){
                    throw new IllegalArgumentException();
                }
            }
            if (fromStart) {
                int result = -((null != comparator()) ? comparator().compare(
                        lo, end) : toComparable(lo).compareTo(end));
                if (result < 0) {
                    throw new IllegalArgumentException();
                }
            }
            return headMap(end, false);
        }

        public SortedMap<K, V> tailMap(K start) {
            if (fromStart) {
                int result = -((null != comparator()) ? comparator().compare(
                        lo, start) : toComparable(lo).compareTo(start));
                if (loInclusive ? result < 0 : result <=0) {
                    throw new IllegalArgumentException();
                }
            }
            if (toEnd) {
                int result = (null != comparator()) ? comparator().compare(start, hi)
                        : toComparable(start).compareTo(hi);
                if (hiInclusive ? result > 0 : result >= 0){
                    throw new IllegalArgumentException();
                }
            }
            return tailMap(start, true);
        }

        public abstract NavigableMap<K, V> subMap(K start,
                boolean startKeyInclusive, K end, boolean endKeyInclusive);

        public abstract NavigableMap<K, V> headMap(K end, boolean inclusive);

        public abstract NavigableMap<K, V> tailMap(K start, boolean inclusive);

        /**
         * 
         * @param key
         * @return false if the key bigger than the end key (if any)
         */
        final boolean checkUpperBound(K key) {
            if (toEnd) {
                int result = (null != comparator()) ? comparator().compare(key, hi)
                        : toComparable(key).compareTo(hi);
                return hiInclusive ? result <= 0 : result < 0;
            }
            return true;
        }

        /**
         * 
         * @param key
         * @return false if the key smaller than the start key (if any)
         */
        final boolean checkLowerBound(K key) {
            if (fromStart) {
                int result = - ((null != comparator()) ? comparator().compare(lo,key)
                        : toComparable(lo).compareTo(key));
                return loInclusive ? result >= 0 : result > 0;
            }
            return true;
        }

        final boolean isInRange(K key) {
            return checkUpperBound(key) && checkLowerBound(key);
        }

        final TreeMap.Entry<K, V> theSmallestEntry() {
            TreeMap.Entry<K, V> result = null;
            if (!fromStart) {
                result = m.findSmallestEntry();
            } else {
                result = loInclusive ? m.findCeilingEntry(lo)
                        : m.findHigherEntry(lo);
            }
            return (null != result && (!toEnd || checkUpperBound(result.getKey()))) ? result
                    : null;
        }

        final TreeMap.Entry<K, V> theBiggestEntry() {
            TreeMap.Entry<K, V> result = null;
            if (!toEnd) {
                result = m.findBiggestEntry();
            } else {
                result = hiInclusive ? m.findFloorEntry(hi)
                        : m.findLowerEntry(hi);
            }
            return (null != result && (!fromStart || checkLowerBound(result.getKey()))) ? result
                    : null;
        }
        
        final TreeMap.Entry<K, V> smallerOrEqualEntry(K key) {
            TreeMap.Entry<K, V> result = findFloorEntry(key);
            return (null != result && (!fromStart || checkLowerBound(result.getKey()))) ? result
                    : null;
        }
        
        private TreeMap.Entry<K, V> findFloorEntry(K key) {
            TreeMap.Entry<K, V> node = findFloorEntryImpl(key);
            
            if (node == null){
                return null;
            }
            
            if (!checkUpperBound(node.key)){
                node = findEndNode();
            }
            
            if (node != null && fromStart && !checkLowerBound(node.key)){
                java.lang.Comparable<K> object = m.comparator == null ? toComparable((K) key)
                        : null;
                if (cmp(object, key, this.lo) > 0){
                    node = findStartNode();
                    if (node == null || cmp(object, key, node.key) < 0){
                        return null;
                    }
                } else {
                    node = null;
                }
            }
            return node;
        }
        
        private int cmp(java.lang.Comparable<K> object, K key1, K key2) {
            return object != null ? object.compareTo(key2) : comparator().compare(
                    key1, key2);
        }
        
        private TreeMap.Entry<K, V> findFloorEntryImpl(K key) {
            java.lang.Comparable<K> object = comparator() == null ? toComparable((K) key)
                    : null;
            K keyK = (K) key;
            Node<K, V> node = this.m.root;
            Node<K, V> foundNode = null;
            int foundIndex = 0;
            while (node != null) {
                K[] keys = node.keys;
                int left_idx = node.left_idx;
                int result = object != null ? object.compareTo(keys[left_idx]) : - comparator().compare(
                        keys[left_idx] ,keyK);
                if (result < 0) {
                    node = node.left;
                } else {
                    foundNode = node;
                    foundIndex = left_idx;
                    if (result == 0){
                        break;
                    }
                    int right_idx = node.right_idx;
                    if (left_idx != right_idx) {
                        result = cmp(object, key, keys[right_idx]);
                    }
                    if (result >= 0) {
                        foundNode = node;
                        foundIndex = right_idx;
                        if (result == 0){
                            break;
                        }
                        node = node.right;
                    } else { /* search in node */
                        int low = left_idx + 1, mid = 0, high = right_idx - 1;
                        while (low <= high && result != 0) {
                            mid = (low + high) >> 1;
                            result = cmp(object, key, keys[mid]);
                            if (result >= 0) {
                                foundNode = node;
                                foundIndex = mid;
                                low = mid + 1;
                            } else {
                                high = mid;
                            }
                            if (low == high && high == mid){
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            if (foundNode != null
                    && cmp(object,keyK, foundNode.keys[foundIndex]) < 0) {
                foundNode = null;
            }
            if (foundNode != null){
                return createEntry(foundNode, foundIndex);
            }
            return null;
        }
        
        TreeMap.Entry<K, V> createEntry(Node<K,V> node, int index) {
            TreeMap.Entry<K, V> entry = new TreeMap.Entry<K, V>(node.keys[index], node.values[index]);
            entry.node = node;
            entry.index = index;
            return entry; 
        }

        final TreeMap.Entry<K, V> biggerOrEqualEntry(K key) {
            TreeMap.Entry<K, V> result = findCeilingEntry(key);
            return (null != result && (!toEnd || checkUpperBound(result.getKey()))) ? result
                    : null;
        }
        
        // find the node whose key equals startKey if any, or the next bigger
        // one than startKey if start exclusive
        private TreeMap.Entry<K, V> findStartNode() {
            if (fromStart) {
                if (loInclusive) {
                    return m.findCeilingEntry(lo);
                } else {
                    return m.findHigherEntry(lo);
                }
            } else {
                return theSmallestEntry();
            }
        }

        // find the node whose key equals endKey if any, or the next smaller
        // one than endKey if end exclusive
        private TreeMap.Entry<K, V> findEndNode(){
            if (hiInclusive){
                return findFloorEntryImpl(hi);
            } else {
                return findLowerEntryImpl(hi);
            }
        }
        
        private TreeMap.Entry<K, V> findCeilingEntry(K key) {
            TreeMap.Entry<K, V> node = findCeilingEntryImpl(key);
            
            if (null == node){
                return null;
            }
            
            if (toEnd && !checkUpperBound(node.key)){
                java.lang.Comparable<K> object = m.comparator == null ? toComparable((K) key)
                        : null;
                if (cmp(object, key, this.hi) < 0){
                    node = findEndNode();
                    if (node!= null && cmp(object, key, node.key) > 0){
                        return null;
                    }
                } else {
                    return null;
                }
            }
            
            if (node != null && !checkLowerBound(node.key)){
                node = findStartNode();
            }
           
            return node;
        }
        
        private TreeMap.Entry<K, V> findLowerEntryImpl(K key) {
            java.lang.Comparable<K> object = comparator() == null ? toComparable((K) key)
                    : null;
            K keyK = (K) key;
            Node<K, V> node = m.root;
            Node<K, V> foundNode = null;
            int foundIndex = 0;
            while (node != null) {
                K[] keys = node.keys;
                int left_idx = node.left_idx;
                int result = object != null ? object.compareTo(keys[left_idx]) : - comparator().compare(
                        keys[left_idx] ,keyK);
                if (result <= 0) {
                    node = node.left;
                } else {
                    foundNode = node;
                    foundIndex = left_idx;
                    int right_idx = node.right_idx;
                    if (left_idx != right_idx) {
                        result = cmp(object, key, keys[right_idx]);
                    }
                    if (result > 0) {
                        foundNode = node;
                        foundIndex = right_idx;
                        node = node.right;
                    } else { /* search in node */
                        int low = left_idx + 1, mid = 0, high = right_idx - 1;
                        while (low <= high) {
                            mid = (low + high) >> 1;
                            result = cmp(object, key, keys[mid]);
                            if (result > 0) {
                                foundNode = node;
                                foundIndex = mid;
                                low = mid + 1;
                            } else {
                                high = mid;
                            }
                            if (low == high && high == mid){
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            if (foundNode != null
                    && cmp(object, keyK, foundNode.keys[foundIndex]) <= 0) {
                foundNode = null;
            }
            if (foundNode != null) {
                return createEntry(foundNode, foundIndex);
            }
            return null;
        }
        
        private TreeMap.Entry<K, V> findCeilingEntryImpl(K key) {
            java.lang.Comparable<K> object = comparator() == null ? toComparable((K) key)
                    : null;
            K keyK = (K) key;
            Node<K, V> node = m.root;
            Node<K, V> foundNode = null;
            int foundIndex = 0;
            while (node != null) {
                K[] keys = node.keys;
                int left_idx = node.left_idx;
                int right_idx = node.right_idx;
                int result = cmp(object, keyK, keys[left_idx]);
                if (result < 0) {
                    foundNode = node;
                    foundIndex = left_idx;
                    node = node.left;
                } else if (result == 0) {
                    foundNode = node;
                    foundIndex = left_idx;
                    break;
                } else {
                    if (left_idx != right_idx) {
                        result = cmp(object, key, keys[right_idx]);
                    }
                    if (result > 0) {
                        node = node.right;
                    } else { /* search in node */
                        foundNode = node;
                        foundIndex = right_idx;
                        if (result == 0) {
                            break;
                        }
                        int low = left_idx + 1, mid = 0, high = right_idx - 1;
                        while (low <= high && result != 0) {
                            mid = (low + high) >> 1;
                            result = cmp(object, key, keys[mid]);
                            if (result <= 0) {
                                foundNode = node;
                                foundIndex = mid;
                                high = mid - 1;    
                            }else {
                                low = mid + 1;          
                            }
                            if (result == 0 || (low == high && high == mid)){
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            if (foundNode != null
                    && cmp(object,keyK, foundNode.keys[foundIndex]) > 0) {
                foundNode = null;
            }
            if (foundNode != null){
                return createEntry(foundNode, foundIndex);
            }
            return null;
        }

        final TreeMap.Entry<K, V> smallerEntry(K key) {
            TreeMap.Entry<K, V> result = findLowerEntry(key);
            return (null != result && (!fromStart || checkLowerBound(result.getKey()))) ? result
                    : null;
        }
        
        private TreeMap.Entry<K, V> findLowerEntry(K key) {
            TreeMap.Entry<K, V> node = findLowerEntryImpl(key);

            if (null == node){
                return null;
            }
            
            if (!checkUpperBound(node.key)){
                node = findEndNode();
            }
            
            if (fromStart && !checkLowerBound(node.key)){
                java.lang.Comparable<K> object = m.comparator == null ? toComparable((K) key)
                        : null;
                if (cmp(object, key, this.lo) > 0){
                    node = findStartNode();
                    if (node == null || cmp(object, key, node.key) <= 0){
                        return null;
                    }
                } else {
                    node = null;
                }
            }

            return node;
        }

        final TreeMap.Entry<K, V> biggerEntry(K key) {
            TreeMap.Entry<K, V> result = findHigherEntry(key);
            return (null != result && (!toEnd || checkUpperBound(result.getKey()))) ? result
                    : null;
        }
        
        private TreeMap.Entry<K, V> findHigherEntry(K key) {
            TreeMap.Entry<K, V> node = findHigherEntryImpl(key);
            
            if (node == null){
                return null;
            }
            
            if (toEnd && !checkUpperBound(node.key)){
                java.lang.Comparable<K> object = m.comparator == null ? toComparable((K) key)
                        : null;
                if (cmp(object, key, this.hi) < 0){
                    node = findEndNode();
                    if (node != null && cmp(object, key, node.key) >= 0){
                        return null;
                    }
                } else {
                    return null;
                }
            }
            
            if (node != null && !checkLowerBound(node.key)){
                node = findStartNode();
            }

            return node;
        }
        
        TreeMap.Entry<K, V> findHigherEntryImpl(K key) {
            java.lang.Comparable<K> object = m.comparator == null ? toComparable((K) key)
                    : null;
            K keyK = (K) key;
            Node<K, V> node = m.root;
            Node<K, V> foundNode = null;
            int foundIndex = 0;
            while (node != null) {
                K[] keys = node.keys;
                int right_idx = node.right_idx;
                int result = cmp(object, keyK, keys[right_idx]);
                if (result >= 0) {
                    node = node.right;
                } else {
                    foundNode = node;
                    foundIndex = right_idx;
                    int left_idx = node.left_idx;
                    if (left_idx != right_idx) {
                        result = cmp(object, key, keys[left_idx]);
                    }
                    if (result < 0) {
                        foundNode = node;
                        foundIndex = left_idx;
                        node = node.left;
                    } else { /* search in node */
                        foundNode = node;
                        foundIndex = right_idx;
                        int low = left_idx + 1, mid = 0, high = right_idx - 1;
                        while (low <= high) {
                            mid = (low + high) >> 1;
                            result = cmp(object, key, keys[mid]);
                            if (result < 0) {
                                foundNode = node;
                                foundIndex = mid;
                                high = mid - 1;     
                            }else {
                                low = mid + 1;            
                            }
                            if (low == high && high == mid){
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            if (foundNode != null
                    && cmp(object,keyK, foundNode.keys[foundIndex]) >= 0) {
                foundNode = null;
            }
            if (foundNode != null){
                return createEntry(foundNode, foundIndex);
            }
            return null;
        }
        
        public Collection<V> values() {
            if(valuesCollection==null) {
                if (!this.toEnd && !this.fromStart){
                    valuesCollection = super.values();
                } else {
                    Map.Entry<K, V> startEntry;
                    if (loInclusive){
                        startEntry = fromStart ? m.ceilingEntry(this.lo) : theSmallestEntry();
                    } else {
                        startEntry = fromStart ? m.findHigherEntry(this.lo): theSmallestEntry();
                    }
                    if (startEntry == null){
                        K key = m.isEmpty() ? this.lo : m.firstKey();
                        valuesCollection = new SubMapValuesCollection<K, V>(
                                new SubMap<K, V>(key, true, this.m, key, true));
                        return valuesCollection;
                    }
                    // for submap, the lastKey is always exclusive, so should take care
                    Map.Entry<K, V> lastEntry;
                    lastEntry = toEnd ? m.ceilingEntry(this.hi) : null;
                    if (lastEntry != null) {
                        if (hiInclusive && lastEntry.getKey().equals(this.hi)) {
                            lastEntry = m.higherEntry(this.hi);
                        }
                    }
                    
                    K startK = startEntry == null ? null: startEntry.getKey();
                    K lastK = lastEntry == null ? null: lastEntry.getKey();
                    // submap always exclude the highest entry
                    valuesCollection = new SubMapValuesCollection<K, V>(
                                new SubMap<K, V>(startK, true, this.m, lastK, lastK == null ? false :toEnd));
                }
            }
            return valuesCollection;
        }
    }
    
    static class NullSubMapValuesCollection<K, V> extends SubMapValuesCollection<K, V> {
        SubMap<K, V> subMap;

        public NullSubMapValuesCollection(SubMap<K, V> subMap) {
            super(subMap);
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Iterator<V> iterator() {
            return new Iterator<V>(){

                public boolean hasNext() {
                    return false;
                }

                public V next() {
                    throw new NoSuchElementException();
                }

                public void remove() {
                    throw new IllegalStateException();
                }
            };
        }

        @Override
        public int size() {
            return 0;
        }
    }
    
    static class AscendingSubMap<K, V> extends NavigableSubMap<K, V> {

        AscendingSubMap(K start, boolean startKeyInclusive, TreeMap<K, V> map,
                K end, boolean endKeyInclusive) {
            super(start, startKeyInclusive, map, end, endKeyInclusive);
        }

        AscendingSubMap(TreeMap<K, V> map, K end, boolean endKeyInclusive) {
            super(map, end, endKeyInclusive);
        }

        AscendingSubMap(K start, boolean startKeyInclusive, TreeMap<K, V> map) {
            super(start, startKeyInclusive, map);
        }

        AscendingSubMap(TreeMap<K, V> map) {
            super(map);
        }

        @Override
        public Map.Entry<K, V> firstEntry() {
            TreeMap.Entry<K, V> ret = theSmallestEntry();
            if (ret != null){
                return m.newImmutableEntry(ret);
            } else {
                return null;
            }
        }

        @Override
        public Map.Entry<K, V> lastEntry() {
            TreeMap.Entry<K, V> ret = theBiggestEntry();
            if (ret != null){
                return m.newImmutableEntry(ret);
            } else {
                return null;
            }
        }

        @Override
        public Map.Entry<K, V> pollFirstEntry() {
            TreeMap.Entry<K, V> node = theSmallestEntry();
            SimpleImmutableEntry<K, V> result = m
                    .newImmutableEntry(node);
            if (null != node) {
                m.remove(node.key);
            }
            return result;
        }

        @Override
        public Map.Entry<K, V> pollLastEntry() {
            TreeMap.Entry<K, V> node = theBiggestEntry();
            SimpleImmutableEntry<K, V> result = m
                    .newImmutableEntry(node);
            if (null != node) {
                m.remove(node.key);
            }
            return result;
        }

        @Override
        public Map.Entry<K, V> higherEntry(K key) {
            TreeMap.Entry<K, V> entry = super.findHigherEntry(key);
            if (null != entry && isInRange(entry.key)){
                return m.newImmutableEntry(entry);
            } else {
                return null;
            }
        }

        @Override
        public Map.Entry<K, V> lowerEntry(K key) {
            TreeMap.Entry<K, V> entry = super.findLowerEntry(key);
            if (null != entry && isInRange(entry.key)){
                return m.newImmutableEntry(entry);
            } else {
                return null;
            }
        }

        @Override
        public Map.Entry<K, V> ceilingEntry(K key) {
            TreeMap.Entry<K, V> entry = super.findCeilingEntry(key);
            if (null != entry && isInRange(entry.key)){
                return m.newImmutableEntry(entry);
            } else {
                return null;
            }
        }

        @Override
        public Map.Entry<K, V> floorEntry(K key) {
            TreeMap.Entry<K, V> entry = super.findFloorEntry(key);
            if (null != entry && isInRange(entry.key)){
                return m.newImmutableEntry(entry);
            } else {
                return null;
            }
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return new AscendingSubMapEntrySet<K, V>(this);
        }

        @SuppressWarnings("unchecked")
        @Override
        public NavigableSet<K> navigableKeySet() {
            return (NavigableSet<K>) new AscendingSubMapKeySet(this);
        }

        public NavigableMap<K, V> descendingMap() {
            if (fromStart && toEnd) {
                return new DescendingSubMap<K, V>( hi, hiInclusive,
                        m,lo, loInclusive);
            }
            if (fromStart) {
                return new DescendingSubMap<K, V>(m,lo, loInclusive);
            }
            if (toEnd) {
                return new DescendingSubMap<K, V>(hi, hiInclusive,m);
            }
            return new DescendingSubMap<K, V>(m);
        }
        
        NavigableSubMap<K, V> descendingSubMap() {
            if (fromStart && toEnd) {
                return new DescendingSubMap<K, V>( hi, hiInclusive,
                        m,lo, loInclusive);
            }
            if (fromStart) {
                return new DescendingSubMap<K, V>(m,lo, loInclusive);
            }
            if (toEnd) {
                return new DescendingSubMap<K, V>(hi, hiInclusive,m);
            }
            return new DescendingSubMap<K, V>(m);
        }

        @Override
        public NavigableMap<K, V> subMap(K start, boolean startKeyInclusive,
                K end, boolean endKeyInclusive) {
            if (fromStart
                    && ((!loInclusive && startKeyInclusive) ? m.keyCompare(start,
                            lo) <= 0 : m.keyCompare(start, lo) < 0)
                    || (toEnd && ((!hiInclusive && (endKeyInclusive || (startKeyInclusive && start.equals(end)))) ? m.keyCompare(
                            end, hi) >= 0
                            : m.keyCompare(end, hi) > 0))) {
                throw new IllegalArgumentException();
            }
            if (m.keyCompare(start, end) > 0){
                throw new IllegalArgumentException();
            }
            return new AscendingSubMap<K, V>(start, startKeyInclusive, m, end,
                    endKeyInclusive);
        }

        @Override
        public NavigableMap<K, V> headMap(K end, boolean inclusive) {
            if (fromStart && ((!loInclusive&&inclusive) ? m.keyCompare(end, lo) <= 0 : m.keyCompare(end, lo) < 0)) {
                throw new IllegalArgumentException();
            }
            if (toEnd && ((!hiInclusive&& inclusive) ? m.keyCompare(end, hi) >= 0 : m.keyCompare(end, hi) > 0)) {
                throw new IllegalArgumentException();
            }
            if (checkUpperBound(end)){
                if (this.fromStart) {
                    return new AscendingSubMap<K, V>(this.lo,
                            this.loInclusive, m, end, inclusive);
                }
                return new AscendingSubMap<K, V>(m, end, inclusive);
            } else {
                return this;
            }
        }

        @Override
        public NavigableMap<K, V> tailMap(K start, boolean inclusive) {
            if (fromStart && ((!loInclusive&& inclusive) ? m.keyCompare(start, lo) <= 0 : m.keyCompare(start, lo) < 0)) {
                throw new IllegalArgumentException();
            }
            if (toEnd && ((!hiInclusive&& inclusive) ? m.keyCompare(start, hi) >= 0 : m.keyCompare(start, hi) > 0)) {
                throw new IllegalArgumentException();
            }
            if ( checkLowerBound(start)) {
                if (this.toEnd) {
                    return new AscendingSubMap<K, V>(start, inclusive,
                            m, this.hi, this.hiInclusive);
                }
                return new AscendingSubMap<K, V>(start, inclusive, m);
            } else {
                return this;
            }
        }
    }

    static class DescendingSubMap<K, V> extends NavigableSubMap<K, V> {
        private final Comparator<? super K> reverseComparator = Collections.reverseOrder(m.comparator);

        DescendingSubMap(K start, boolean startKeyInclusive, TreeMap<K, V> map,
                K end, boolean endKeyInclusive) {
            super(start, startKeyInclusive, map, end, endKeyInclusive);
        }

        DescendingSubMap(K start, boolean startKeyInclusive, TreeMap<K, V> map) {
            super(start, startKeyInclusive, map);
        }

        DescendingSubMap(TreeMap<K, V> map, K end, boolean endKeyInclusive) {
            super(map, end, endKeyInclusive);
        }

        DescendingSubMap(TreeMap<K, V> map) {
            super(map);
        }

        @Override
        public Comparator<? super K> comparator() {
            return reverseComparator;
        }
        
        public SortedMap<K, V> subMap(K start, K end) {
            // the exception check is special here, the end should not if equal
            // to endkey unless start is equal to end
            if (!checkLowerBound(start) || !checkUpperBound(start)){
                throw new IllegalArgumentException();
            }
            int result = -1;
            if (toEnd) {
                result = (null != comparator()) ? comparator().compare(end, hi)
                        : toComparable(end).compareTo(hi);
            }
            if (((!hiInclusive && start.equals(end)) ? result < 0 : result <= 0)){
                if ((null != comparator()) ? comparator().compare(start, end) > 0
                        : toComparable(start).compareTo(end) > 0) {
                    throw new IllegalArgumentException();
                }
                return new DescendingSubMap<K, V>(start, true, m, end,
                        false);
            }
            throw new IllegalArgumentException();
        }

        @Override
        public Map.Entry<K, V> firstEntry() {
            TreeMap.Entry<K,V> result;
            if (!fromStart) {
                result = m.findBiggestEntry();
            } else {
                result = loInclusive ? m.findFloorEntry(lo)
                        : m.findLowerEntry(lo);
            }
            if (result ==null || !isInRange(result.key)){
                return null;
            }
            return m.newImmutableEntry(result);
        }

        @Override
        public Map.Entry<K, V> lastEntry() {
            TreeMap.Entry<K,V> result;
            if (!toEnd) {
                result = m.findSmallestEntry();
            } else {
                result = hiInclusive ? m.findCeilingEntry(hi) : m
                        .findHigherEntry(hi);
            }
            if (result!= null && !isInRange(result.key)){
                return null;
            }
            return m.newImmutableEntry(result);
        }

        @Override
        public Map.Entry<K, V> pollFirstEntry() {
            TreeMap.Entry<K, V> node = null;
            if (fromStart) {
                node = loInclusive ? this.m.findFloorEntry(lo) : this.m
                        .findLowerEntry(lo);
            } else {
                node = this.m.findBiggestEntry();
            }
            if (node != null && fromStart && (loInclusive ? this.m.keyCompare(lo, node.key) < 0 : this.m.keyCompare(lo, node.key) <= 0)) {
                node = null;
            }
            if (node != null && toEnd && (hiInclusive ? this.m.keyCompare(hi, node.key) > 0 : this.m.keyCompare(hi, node.key) >= 0)) {
                node = null;
            }
            SimpleImmutableEntry<K, V> result = m
                    .newImmutableEntry(node);
            if (null != node) {
                m.remove(node.key);
            }
            return result;
        }

        @Override
        public Map.Entry<K, V> pollLastEntry() {
            TreeMap.Entry<K, V> node = null;
            if (toEnd){
                node = hiInclusive ? this.m.findCeilingEntry(hi) : this.m.findHigherEntry(hi);
            } else {
                node = this.m.findSmallestEntry();
            }
            if (node != null && fromStart && (loInclusive ? this.m.keyCompare(lo, node.key) < 0 : this.m.keyCompare(lo, node.key) <= 0)) {
                node = null;
            }
            if (node != null && toEnd && (hiInclusive ? this.m.keyCompare(hi, node.key) > 0 : this.m.keyCompare(hi, node.key) >= 0)) {
                node = null;
            }
            SimpleImmutableEntry<K, V> result = m
                    .newImmutableEntry(node);
            if (null != node) {
                m.remove(node.key);
            }
            return result;
        }

        @Override
        public Map.Entry<K, V> higherEntry(K key) {
            TreeMap.Entry<K, V> entry = this.m.findLowerEntry(key);
            if  (null != entry && (fromStart && !checkLowerBound(entry.getKey()))){
                entry = loInclusive ? this.m.findFloorEntry(this.lo) : this.m.findLowerEntry(this.lo);
            }
            if  (null != entry && (!isInRange(entry.getKey()))){
                entry = null;
            }
            return m.newImmutableEntry(entry);
        }

        @Override
        public Map.Entry<K, V> lowerEntry(K key) {
            TreeMap.Entry<K, V> entry = this.m.findHigherEntry(key);
            if  (null != entry && (toEnd && !checkUpperBound(entry.getKey()))){
                entry = hiInclusive ? this.m.findCeilingEntry(this.hi): this.m.findHigherEntry(this.hi);
            }
            if  (null != entry && (!isInRange(entry.getKey()))){
                entry = null;
            }
            return m.newImmutableEntry(entry);
        }

        @Override
        public Map.Entry<K, V> ceilingEntry(K key) {
            java.lang.Comparable<K> object = m.comparator == null ? toComparable((K) key)
                    : null;
            TreeMap.Entry<K, V> entry = null;
            if (toEnd && m.cmp(object, key, lo) >= 0){
                entry = loInclusive? this.m.findFloorEntry(lo) : this.m.findLowerEntry(lo);
            } else {
                entry = this.m.findFloorEntry(key);
            }
            if  (null != entry && (toEnd && !checkUpperBound(entry.getKey()))){
                entry = null;
            }
            return m.newImmutableEntry(entry);
        }

        @Override
        public Map.Entry<K, V> floorEntry(K key) {
            java.lang.Comparable<K> object = m.comparator == null ? toComparable((K) key)
                    : null;
            TreeMap.Entry<K, V> entry = null;
            if (fromStart && m.cmp(object, key, hi) <= 0){
                entry = hiInclusive? this.m.findCeilingEntry(hi) : this.m.findHigherEntry(hi);
            } else {
                entry = this.m.findCeilingEntry(key);
            }
            if  (null != entry && (fromStart && !checkLowerBound(entry.getKey()))){
                entry = null;
            }
            return m.newImmutableEntry(entry);
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            return new DescendingSubMapEntrySet<K, V>(this);
        }

        @SuppressWarnings("unchecked")
        @Override
        public NavigableSet<K> navigableKeySet() {
            return (NavigableSet<K>) new DescendingSubMapKeySet(this);
        }

        public NavigableMap<K, V> descendingMap() {
            if (fromStart && toEnd) {
                return new AscendingSubMap<K, V>( hi, hiInclusive,
                        m,lo, loInclusive);
            }
            if (fromStart) {
                return new AscendingSubMap<K, V>(m, lo, loInclusive);
            }
            if (toEnd) {
                return new AscendingSubMap<K, V>(hi, hiInclusive,
                        m);
            }
            return new AscendingSubMap<K, V>(m);
        }

        int keyCompare(K left, K right) {
            return (null != reverseComparator) ? reverseComparator.compare(
                    left,right) : toComparable(left).compareTo(right);
        }

        @Override
        public NavigableMap<K, V> subMap(K start, boolean startKeyInclusive,
                K end, boolean endKeyInclusive) {
            // special judgement, the same reason as subMap(K,K)
            if (!checkUpperBound(start)){
                throw new IllegalArgumentException();
            }
            if (fromStart
                    && ((!loInclusive && (startKeyInclusive || (endKeyInclusive && start.equals(end)))) ? keyCompare(start,
                            lo) <= 0 : keyCompare(start, lo) < 0)
                    || (toEnd && ((!hiInclusive && (endKeyInclusive)) ? keyCompare(
                            end, hi) >= 0
                            : keyCompare(end, hi) > 0))) {
                throw new IllegalArgumentException();
            }
            if (keyCompare(start, end) > 0) {
                throw new IllegalArgumentException();
            }
            return new DescendingSubMap<K, V>(start, startKeyInclusive, m, end,
                    endKeyInclusive);
        }

        @Override
        public NavigableMap<K, V> headMap(K end, boolean inclusive) {
            // check for error
            this.keyCompare(end, end);
            K inclusiveEnd = end; //inclusive ? end : m.higherKey(end);
            boolean isInRange = true;
            if (null != inclusiveEnd) {
                int result;
                if (toEnd) {
                    result = (null != comparator()) ? comparator().compare(
                            inclusiveEnd, hi) : toComparable(inclusiveEnd)
                            .compareTo(hi);
                    isInRange = (hiInclusive || !inclusive) ? result <= 0 : result < 0;
                }
                if (fromStart) {
                    result = (null != comparator()) ? comparator().compare(
                            inclusiveEnd, lo) : toComparable(inclusiveEnd)
                            .compareTo(lo);
                    isInRange = isInRange
                            && ((loInclusive || !inclusive) ? result >= 0 : result > 0);
                }
            }
            if (isInRange) {
                if (this.fromStart) {
                    return new DescendingSubMap<K, V>(this.lo,this.loInclusive,
                            m, end, inclusive);
                }
                return new DescendingSubMap<K, V>(m, end, inclusive);
            }
            throw new IllegalArgumentException();
        }

        @Override
        public NavigableMap<K, V> tailMap(K start, boolean inclusive) {
            // check for error
            this.keyCompare(start, start);
            K inclusiveStart = start; // inclusive ? start : m.lowerKey(start);
            boolean isInRange = true;
            int result;
            if (null != inclusiveStart) {
                if (toEnd) {
                    result = (null != comparator()) ? comparator().compare(
                            inclusiveStart, hi) : toComparable(inclusiveStart)
                            .compareTo(hi);
                    isInRange = (hiInclusive || !inclusive) ? result <= 0 : result < 0;
                }
                if (fromStart) {
                    result = (null != comparator()) ? comparator().compare(
                            inclusiveStart, lo) : toComparable(inclusiveStart)
                            .compareTo(lo);
                    isInRange = isInRange
                            && ((loInclusive || !inclusive)  ? result >= 0 : result > 0);
                }
            }
            if (isInRange) {
                if (this.toEnd) {
                    return new DescendingSubMap<K, V>(start, inclusive, m, this.hi, this.hiInclusive);
                }
                return new DescendingSubMap<K, V>(start, inclusive,m);

            }
            throw new IllegalArgumentException();
        }
        
        public Collection<V> values() {
            if(valuesCollection==null) {            
                if (fromStart || toEnd){
                    return valuesCollection = new DescendingSubMapValuesCollection<K, V>(this);
                }
                valuesCollection = super.values();
            }
            return valuesCollection;
        }
        static class DescendingSubMapValuesCollection<K, V> extends AbstractCollection<V> {
            DescendingSubMap<K, V> subMap;

            public DescendingSubMapValuesCollection(DescendingSubMap<K, V> subMap) {
                this.subMap = subMap;
            }

            @Override
            public boolean isEmpty() {
                return subMap.isEmpty();
            }

            @Override
            public Iterator<V> iterator() {
                TreeMap.Entry<K, V> from = subMap.m.find(subMap.firstKey());
                TreeMap.Entry<K, V> to =  subMap.m.findLowerEntry(subMap.lastKey());
                return new DescendingValueIterator<K, V>(from.node,
                        from == null ? 0 : from.index, subMap, to == null ? null : to.node,
                        to == null ? 0 : to.index);
            }
            
            static class DescendingValueIterator<K, V> extends BoundedMapIterator<K, V>
            implements Iterator<V> {

        public DescendingValueIterator(Node<K, V> startNode, int startOffset,
                DescendingSubMap<K, V> map, Node<K, V> finalNode, int finalOffset) {
            super(startNode, startOffset, map.m, finalNode, finalOffset);
            node = startNode;
            offset = startOffset;
        }

        public V next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            if (node != null){
                boolean endOfIterator = lastNode == finalNode && lastOffset == finalOffset;
                if (endOfIterator) {
                    node = null;
                } else {
                    if (expectedModCount != backingMap.modCount) {
                        throw new ConcurrentModificationException();
                    } else if (node == null) {
                        throw new NoSuchElementException();
                    }
                    lastNode = node;
                    lastOffset = offset;
                    if (offset != node.left_idx) {
                        offset --;
                    } else {
                        node = node.prev;
                        if (node != null) {
                            offset = node.right_idx;
                        }
                    }
                }
            }
            return lastNode.values[lastOffset];
        }
    }

            @Override
            public int size() {
                return subMap.size();
            }
        }

        @Override
        NavigableSubMap<K, V> descendingSubMap() {
            if (fromStart && toEnd) {
                return new AscendingSubMap<K, V>( hi, hiInclusive,
                        m,lo, loInclusive);
            }
            if (fromStart) {
                return new AscendingSubMap<K, V>(m, hi, hiInclusive);
            }
            if (toEnd) {
                return new AscendingSubMap<K, V>(lo, loInclusive,
                        m);
            }
            return new AscendingSubMap<K, V>(m);
        }
    }

    /**
     * Constructs a new empty {@code TreeMap} instance.
     */
    public TreeMap() {
        super();
    }

    /**
     * Constructs a new empty {@code TreeMap} instance with the specified
     * comparator.
     *
     * @param comparator
     *            the comparator to compare keys with.
     */
    public TreeMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    /**
     * Constructs a new {@code TreeMap} instance containing the mappings from
     * the specified map and using natural ordering.
     *
     * @param map
     *            the mappings to add.
     * @throws ClassCastException
     *             if a key in the specified map does not implement the
     *             Comparable interface, or if the keys in the map cannot be
     *             compared.
     */
    public TreeMap(Map<? extends K, ? extends V> map) {
        this();
        putAll(map);
    }

    /**
     * Constructs a new {@code TreeMap} instance containing the mappings from
     * the specified SortedMap and using the same comparator.
     *
     * @param map
     *            the mappings to add.
     */
    public TreeMap(SortedMap<K, ? extends V> map) {
        this(map.comparator());
        Node<K, V> lastNode = null;
        Iterator<? extends Map.Entry<K, ? extends V>> it = map.entrySet()
                .iterator();
        while (it.hasNext()) {
            Map.Entry<K, ? extends V> entry = it.next();
            lastNode = addToLast(lastNode, entry.getKey(), entry.getValue());
        }
    }
    
    Node<K, V> addToLast(Node<K, V> last, K key, V value) {
        if (last == null) {
            root = last = createNode(key, value);
            size = 1;
        } else if (last.size == Node.NODE_SIZE) {
            Node<K, V> newNode = createNode(key, value);
            attachToRight(last, newNode);
            balance(newNode);
            size++;
            last = newNode;
        } else {
            appendFromRight(last, key, value);
            size++;
        }
        return last;
    }

    /**
     * Removes all mappings from this TreeMap, leaving it empty.
     *
     * @see Map#isEmpty()
     * @see #size()
     */
    @Override
    public void clear() {
        root = null;
        size = 0;
        modCount++;
    }

    /**
     * Returns the comparator used to compare elements in this map.
     *
     * @return the comparator or {@code null} if the natural ordering is used.
     */
    public Comparator<? super K> comparator() {
        return comparator;
    }

    /**
     * Returns whether this map contains the specified key.
     *
     * @param key
     *            the key to search for.
     * @return {@code true} if this map contains the specified key,
     *         {@code false} otherwise.
     * @throws ClassCastException
     *             if the specified key cannot be compared with the keys in this
     *             map.
     * @throws NullPointerException
     *             if the specified key is {@code null} and the comparator
     *             cannot handle {@code null} keys.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean containsKey(Object key) {
        java.lang.Comparable<K> object = comparator == null ? toComparable((K) key)
                : null;
        K keyK = (K) key;
        Node<K, V> node = root;
        while (node != null) {
            K[] keys = node.keys;
            int left_idx = node.left_idx;
            int result = object != null ? object.compareTo(keys[left_idx])
                    : -comparator.compare(keys[left_idx], keyK);
            if (result < 0) {
                node = node.left;
            } else if (result == 0) {
                return true;
            } else {
                int right_idx = node.right_idx;
                if (left_idx != right_idx) {
                    result = cmp(object, keyK, keys[right_idx]);
                }
                if (result > 0) {
                    node = node.right;
                } else if (result == 0) {
                    return true;
                } else { /* search in node */
                    int low = left_idx + 1, mid = 0, high = right_idx - 1;
                    while (low <= high) {
                        mid = (low + high) >>> 1;
                        result = cmp(object, keyK, keys[mid]);
                        if (result > 0) {
                            low = mid + 1;
                        } else if (result == 0) {
                            return true;
                        } else {
                            high = mid - 1;
                        }
                    }
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Returns whether this map contains the specified value.
     *
     * @param value
     *            the value to search for.
     * @return {@code true} if this map contains the specified value,
     *         {@code false} otherwise.
     */
    @Override
    public boolean containsValue(Object value) {
        if (root == null) {
            return false;
        }
        Node<K, V> node = minimum(root);
        if (value != null) {
            while (node != null) {
                int to = node.right_idx;
                V[] values = node.values;
                for (int i = node.left_idx; i <= to; i++) {
                    if (value.equals(values[i])) {
                        return true;
                    }
                }
                node = node.next;
            }
        } else {
            while (node != null) {
                int to = node.right_idx;
                V[] values = node.values;
                for (int i = node.left_idx; i <= to; i++) {
                    if (values[i] == null) {
                        return true;
                    }
                }
                node = node.next;
            }
        }
        return false;
    }

    private boolean containsValue(Entry<K, V> node, Object value) {
        if (value == null ? node.value == null : value.equals(node.value)) {
            return true;
        }
        if (node.left != null) {
            if (containsValue(node.left, value)) {
                return true;
            }
        }
        if (node.right != null) {
            if (containsValue(node.right, value)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    Entry<K, V> find(Object keyObj) {
        java.lang.Comparable<K> object = comparator == null ? toComparable((K) keyObj)
                : null;
        K keyK = (K) keyObj;
        Node<K, V> node = root;
        while (node != null) {
            K[] keys = node.keys;
            int left_idx = node.left_idx;
            int result = cmp(object, keyK, keys[left_idx]);
            if (result < 0) {
                node = node.left;
            } else if (result == 0) {
                return createEntry(node,left_idx);
            } else {
                int right_idx = node.right_idx;
                if (left_idx != right_idx) {
                    result = cmp(object, keyK, keys[right_idx]);
                }
                if (result > 0) {
                    node = node.right;
                } else if (result == 0) {
                    return createEntry(node,right_idx);
                } else { /* search in node */
                    int low = left_idx + 1, mid = 0, high = right_idx - 1;
                    while (low <= high) {
                        mid = (low + high) >> 1;
                        result = cmp(object, keyK, keys[mid]);
                        if (result > 0) {
                            low = mid + 1;
                        } else if (result == 0) {
                            return createEntry(node,mid);
                        } else {
                            high = mid - 1;
                        }
                    }
                    return null;
                }
            }
        }
        return null;
    }
    
    Entry<K, V> createEntry(Node<K,V> node, int index) {
        TreeMap.Entry<K, V> entry = new TreeMap.Entry<K, V>(node.keys[index], node.values[index]);
        entry.node = node;
        entry.index = index;
        return entry; 
    }

    TreeMap.Entry<K, V> findSmallestEntry() {
        if (null != root){
            Node<K, V> node = minimum(root);
            TreeMap.Entry<K, V> ret = new TreeMap.Entry<K, V>(node.keys[node.left_idx], node.values[node.left_idx]);
            ret.node = node;
            ret.index = node.left_idx;
            return ret;
        }
        return null;
    }

    TreeMap.Entry<K, V> findBiggestEntry() {
        if (null != root){
            Node<K, V> node = maximum(root);
            TreeMap.Entry<K, V> ret = new TreeMap.Entry<K, V>(node.keys[node.right_idx], node.values[node.right_idx]);
            ret.node = node;
            ret.index = node.right_idx;
            return ret;
        }
        return null;
    }

    TreeMap.Entry<K, V> findCeilingEntry(K key) {
        if (root == null) {
            return null;
        }
        java.lang.Comparable<K> object = comparator == null ? toComparable((K) key)
                : null;
        K keyK = (K) key;
        Node<K, V> node = root;
        Node<K, V> foundNode = null;
        int foundIndex = 0;
        while (node != null) {
            K[] keys = node.keys;
            int left_idx = node.left_idx;
            int right_idx = node.right_idx;
            int result = cmp(object, keyK, keys[left_idx]);
            if (result < 0) {
                foundNode = node;
                foundIndex = left_idx;
                node = node.left;
            } else if (result == 0) {
                foundNode = node;
                foundIndex = left_idx;
                break;
            } else {
                if (left_idx != right_idx) {
                    result = cmp(object, key, keys[right_idx]);
                }
                if (result > 0) {
                    node = node.right;
                } else { /* search in node */
                    foundNode = node;
                    foundIndex = right_idx;
                    if (result == 0) {
                        break;
                    }
                    int low = left_idx + 1, mid = 0, high = right_idx - 1;
                    while (low <= high && result != 0) {
                        mid = (low + high) >> 1;
                        result = cmp(object, key, keys[mid]);
                        if (result <= 0) {
                            foundNode = node;
                            foundIndex = mid;
                            high = mid - 1;    
                        }else {
                            low = mid + 1;          
                        }
                        if (result == 0 || (low == high && high == mid)){
                            break;
                        }
                    }
                    break;
                }
            }
        }
        if (foundNode != null
                && cmp(object,keyK, foundNode.keys[foundIndex]) > 0) {
            foundNode = null;
        }
        if (foundNode != null){
            return createEntry(foundNode, foundIndex);
        }
        return null;
    }

    TreeMap.Entry<K, V> findFloorEntry(K key) {
        if (root == null) {
            return null;
        }
        java.lang.Comparable<K> object = comparator == null ? toComparable((K) key)
                : null;
        K keyK = (K) key;
        Node<K, V> node = root;
        Node<K, V> foundNode = null;
        int foundIndex = 0;
        while (node != null) {
            K[] keys = node.keys;
            int left_idx = node.left_idx;
            int result = cmp(object, keyK, keys[left_idx]);
            if (result < 0) {
                node = node.left;
            } else {
                foundNode = node;
                foundIndex = left_idx;
                if (result == 0){
                    break;
                }
                int right_idx = node.right_idx;
                if (left_idx != right_idx) {
                    result = cmp(object, key, keys[right_idx]);
                }
                if (result >= 0) {
                    foundNode = node;
                    foundIndex = right_idx;
                    if (result == 0){
                        break;
                    }
                    node = node.right;
                } else { /* search in node */
                    int low = left_idx + 1, mid = 0, high = right_idx - 1;
                    while (low <= high && result != 0) {
                        mid = (low + high) >> 1;
                        result = cmp(object, key, keys[mid]);
                        if (result >= 0) {
                            foundNode = node;
                            foundIndex = mid;
                            low = mid + 1;
                        } else {
                            high = mid;
                        }
                        if (result == 0 || (low == high && high == mid)){
                            break;
                        }
                    }
                    break;
                }
            }
        }
        if (foundNode != null
                && cmp(object,keyK, foundNode.keys[foundIndex]) < 0) {
            foundNode = null;
        }
        if (foundNode != null){
            return createEntry(foundNode, foundIndex);
        }
        return null;
    }

    TreeMap.Entry<K, V> findLowerEntry(K key) {
        if (root == null) {
            return null;
        }
        java.lang.Comparable<K> object = comparator == null ? toComparable((K) key)
                : null;
        K keyK = (K) key;
        Node<K, V> node = root;
        Node<K, V> foundNode = null;
        int foundIndex = 0;
        while (node != null) {
            K[] keys = node.keys;
            int left_idx = node.left_idx;
            int result = cmp(object, keyK, keys[left_idx]);
            if (result <= 0) {
                node = node.left;
            } else {
                foundNode = node;
                foundIndex = left_idx;
                int right_idx = node.right_idx;
                if (left_idx != right_idx) {
                    result = cmp(object, key, keys[right_idx]);
                }
                if (result > 0) {
                    foundNode = node;
                    foundIndex = right_idx;
                    node = node.right;
                } else { /* search in node */
                    int low = left_idx + 1, mid = 0, high = right_idx - 1;
                    while (low <= high) {
                        mid = (low + high) >> 1;
                        result = cmp(object, key, keys[mid]);
                        if (result > 0) {
                            foundNode = node;
                            foundIndex = mid;
                            low = mid + 1;
                        } else {
                            high = mid;
                        }
                        if (low == high && high == mid){
                            break;
                        }
                    }
                    break;
                }
            }
        }
        if (foundNode != null
                && cmp(object,keyK, foundNode.keys[foundIndex]) <= 0) {
            foundNode = null;
        }
        if (foundNode != null){
            return createEntry(foundNode, foundIndex);
        }
        return null;
    }

    TreeMap.Entry<K, V> findHigherEntry(K key) {
        if (root == null) {
            return null;
        }
        java.lang.Comparable<K> object = comparator == null ? toComparable((K) key)
                : null;
        K keyK = (K) key;
        Node<K, V> node = root;
        Node<K, V> foundNode = null;
        int foundIndex = 0;
        while (node != null) {
            K[] keys = node.keys;
            int right_idx = node.right_idx;
            int result = cmp(object, keyK, keys[right_idx]);
            if (result >= 0) {
                node = node.right;
            } else {
                foundNode = node;
                foundIndex = right_idx;
                int left_idx = node.left_idx;
                if (left_idx != right_idx) {
                    result = cmp(object, key, keys[left_idx]);
                }
                if (result < 0) {
                    foundNode = node;
                    foundIndex = left_idx;
                    node = node.left;
                } else { /* search in node */
                    foundNode = node;
                    foundIndex = right_idx;
                    int low = left_idx + 1, mid = 0, high = right_idx - 1;
                    while (low <= high) {
                        mid = (low + high) >> 1;
                        result = cmp(object, key, keys[mid]);
                        if (result < 0) {
                            foundNode = node;
                            foundIndex = mid;
                            high = mid - 1;     
                        }else {
                            low = mid + 1;            
                        }
                        if (low == high && high == mid){
                            break;
                        }
                    }
                    break;
                }
            }
        }
        if (foundNode != null
                && cmp(object,keyK, foundNode.keys[foundIndex]) >= 0) {
            foundNode = null;
        }
        if (foundNode != null){
            return createEntry(foundNode, foundIndex);
        }
        return null;
    }

    /**
     * Returns the first key in this map.
     *
     * @return the first key in this map.
     * @throws NoSuchElementException
     *                if this map is empty.
     */
    public K firstKey() {
        if (root != null) {
            Node<K, V> node = minimum(root);
            return node.keys[node.left_idx];
        }
        throw new NoSuchElementException();
    }

    Node<K, V> findNode(K key) {
		java.lang.Comparable<K> object = comparator == null ? toComparable((K) key)
				: null;
		K keyK = (K) key;
		Node<K, V> node = root;
		while (node != null) {
			K[] keys = node.keys;
			int left_idx = node.left_idx;
			int result = cmp(object, keyK, keys[left_idx]);
			if (result < 0) {
				node = node.left;
			} else if (result == 0) {
				return node;
			} else {
				int right_idx = node.right_idx;
				if (left_idx != right_idx) {
					result = cmp(object, keyK, keys[right_idx]);
				}
				if (result > 0) {
					node = node.right;
				} else {
					return node;
				}
			}
		}
		return null;
	}
    
    /**
     * Returns the value of the mapping with the specified key.
     *
     * @param key
     *            the key.
     * @return the value of the mapping with the specified key.
     * @throws ClassCastException
     *             if the key cannot be compared with the keys in this map.
     * @throws NullPointerException
     *             if the key is {@code null} and the comparator cannot handle
     *             {@code null}.
     */
    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        java.lang.Comparable<K> object = comparator == null ? toComparable((K) key)
                : null;
        K keyK = (K) key;
        Node<K, V> node = root;
        while (node != null) {
            K[] keys = node.keys;
            int left_idx = node.left_idx;
            int result = cmp(object, keyK, keys[left_idx]);
            if (result < 0) {
                node = node.left;
            } else if (result == 0) {
                return node.values[left_idx];
            } else {
                int right_idx = node.right_idx;
                if (left_idx != right_idx) {
                    result = cmp(object, keyK, keys[right_idx]);
                }
                if (result > 0) {
                    node = node.right;
                } else if (result == 0) {
                    return node.values[right_idx];
                } else { /* search in node */
                    int low = left_idx + 1, mid = 0, high = right_idx - 1;
                    while (low <= high) {
                        mid = (low + high) >>> 1;
                        result = cmp(object, keyK, keys[mid]);
                        if (result > 0) {
                            low = mid + 1;
                        } else if (result == 0) {
                            return node.values[mid];
                        } else {
                            high = mid - 1;
                        }
                    }
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Returns a set of the keys contained in this map. The set is backed by
     * this map so changes to one are reflected by the other. The set does not
     * support adding.
     *
     * @return a set of the keys.
     */
    @Override
    public Set<K> keySet() {
        if (keySet == null) {
            keySet = new AbstractSet<K>() {
                @Override
                public boolean contains(Object object) {
                    return containsKey(object);
                }

                @Override
                public int size() {
                    return size;
                }

                @Override
                public void clear() {
                    TreeMap.this.clear();
                }

                @Override
                public Iterator<K> iterator() {
                    return new UnboundedKeyIterator<K,V> (TreeMap.this);
                }
            };
        }
        return keySet;
    }

    /**
     * Returns the last key in this map.
     *
     * @return the last key in this map.
     * @throws NoSuchElementException
     *             if this map is empty.
     */
    public K lastKey() {
        if (root != null) {
            Node<K, V> node = maximum(root);
            return node.keys[node.right_idx];
        }
        throw new NoSuchElementException();
    }

    static <K, V> Entry<K, V> maximum(Entry<K, V> x) {
        while (x.right != null) {
            x = x.right;
        }
        return x;
    }

    static <K, V> Entry<K, V> minimum(Entry<K, V> x) {
        while (x.left != null) {
            x = x.left;
        }
        return x;
    }

    static <K, V> Entry<K, V> predecessor(Entry<K, V> x) {
        if (x.left != null) {
            return maximum(x.left);
        }
        Entry<K, V> y = x.parent;
        while (y != null && x == y.left) {
            x = y;
            y = y.parent;
        }
        return y;
    }

    static private <K, V> Node<K, V> successor(Node<K, V> x) {
        if (x.right != null) {
            return minimum(x.right);
        }
        Node<K, V> y = x.parent;
        while (y != null && x == y.right) {
            x = y;
            y = y.parent;
        }
        return y;
    }
    
    private int cmp(java.lang.Comparable<K> object, K key1, K key2) {
        return object != null ? object.compareTo(key2) : comparator.compare(
                key1, key2);
    }

    /**
     * Maps the specified key to the specified value.
     *
     * @param key
     *            the key.
     * @param value
     *            the value.
     * @return the value of any previous mapping with the specified key or
     *         {@code null} if there was no mapping.
     * @throws ClassCastException
     *             if the specified key cannot be compared with the keys in this
     *             map.
     * @throws NullPointerException
     *             if the specified key is {@code null} and the comparator
     *             cannot handle {@code null} keys.
     */
    @Override
    public V put(K key, V value) {
        return putImpl(key, value);
    }
    
    private V putImpl(K key, V value) {
        if (root == null) {
            root = createNode(key, value);
            size = 1;
            modCount++;
            return null;
        }
        java.lang.Comparable<K> object = comparator == null ? toComparable((K) key)
                : null;
        K keyK = (K) key;
        Node<K, V> node = root;
        Node<K, V> prevNode = null;
        int result = 0;
        while (node != null) {
            prevNode = node;
            K[] keys = node.keys;
            int left_idx = node.left_idx;
            result = cmp(object, keyK, keys[left_idx]);
            if (result < 0) {
                node = node.left;
            } else if (result == 0) {
                V res = node.values[left_idx];
                node.values[left_idx] = value;
                return res;
            } else {
                int right_idx = node.right_idx;
                if (left_idx != right_idx) {
                    result = cmp(object, keyK, keys[right_idx]);
                }
                if (result > 0) {
                    node = node.right;
                } else if (result == 0) {
                    V res = node.values[right_idx];
                    node.values[right_idx] = value;
                    return res;
                } else { /* search in node */
                    int low = left_idx + 1, mid = 0, high = right_idx - 1;
                    while (low <= high) {
                        mid = (low + high) >>> 1;
                        result = cmp(object, keyK, keys[mid]);
                        if (result > 0) {
                            low = mid + 1;
                        } else if (result == 0) {
                            V res = node.values[mid];
                            node.values[mid] = value;
                            return res;
                        } else {
                            high = mid - 1;
                        }
                    }
                    result = low;
                    break;
                }
            }
        } /* while */
        /*
         * if(node == null) { if(prevNode==null) { - case of empty Tree } else {
         * result < 0 - prevNode.left==null - attach here result > 0 -
         * prevNode.right==null - attach here } } else { insert into node.
         * result - index where it should be inserted. }
         */
        size++;
        modCount++;
        if (node == null) {
            if (prevNode == null) {
                // case of empty Tree
                root = createNode(key, value);
            } else if (prevNode.size < Node.NODE_SIZE) {
                // there is a place for insert
                if (result < 0) {
                    appendFromLeft(prevNode, key, value);
                } else {
                    appendFromRight(prevNode, key, value);
                }
            } else {
                // create and link
                Node<K, V> newNode = createNode(key, value);
                if (result < 0) {
                    attachToLeft(prevNode, newNode);
                } else {
                    attachToRight(prevNode, newNode);
                }
                balance(newNode);
            }
        } else {
            // insert into node.
            // result - index where it should be inserted.
            if (node.size < Node.NODE_SIZE) { // insert and ok
                int left_idx = node.left_idx;
                int right_idx = node.right_idx;
                if (left_idx == 0
                        || ((right_idx != Node.NODE_SIZE - 1) && (right_idx
                                - result <= result - left_idx))) {
                    int right_idxPlus1 = right_idx + 1;
                    System.arraycopy(node.keys, result, node.keys, result + 1,
                            right_idxPlus1 - result);
                    System.arraycopy(node.values, result, node.values,
                            result + 1, right_idxPlus1 - result);
                    node.right_idx = right_idxPlus1;
                    node.keys[result] = key;
                    node.values[result] = value;
                } else {
                    int left_idxMinus1 = left_idx - 1;
                    System.arraycopy(node.keys, left_idx, node.keys,
                            left_idxMinus1, result - left_idx);
                    System.arraycopy(node.values, left_idx, node.values,
                            left_idxMinus1, result - left_idx);
                    node.left_idx = left_idxMinus1;
                    node.keys[result - 1] = key;
                    node.values[result - 1] = value;
                }
                node.size++;
            } else {
                // there are no place here
                // insert and push old pair
                Node<K, V> previous = node.prev;
                Node<K, V> nextNode = node.next;
                boolean removeFromStart;
                boolean attachFromLeft = false;
                Node<K, V> attachHere = null;
                if (previous == null) {
                    if (nextNode != null && nextNode.size < Node.NODE_SIZE) {
                        // move last pair to next
                        removeFromStart = false;
                    } else {
                        // next node doesn't exist or full
                        // left==null
                        // drop first pair to new node from left
                        removeFromStart = true;
                        attachFromLeft = true;
                        attachHere = node;
                    }
                } else if (nextNode == null) {
                    if (previous.size < Node.NODE_SIZE) {
                        // move first pair to prev
                        removeFromStart = true;
                    } else {
                        // right == null;
                        // drop last pair to new node from right
                        removeFromStart = false;
                        attachFromLeft = false;
                        attachHere = node;
                    }
                } else {
                    if (previous.size < Node.NODE_SIZE) {
                        if (nextNode.size < Node.NODE_SIZE) {
                            // choose prev or next for moving
                            removeFromStart = previous.size < nextNode.size;
                        } else {
                            // move first pair to prev
                            removeFromStart = true;
                        }
                    } else {
                        if (nextNode.size < Node.NODE_SIZE) {
                            // move last pair to next
                            removeFromStart = false;
                        } else {
                            // prev & next are full
                            // if node.right!=null then node.next.left==null
                            // if node.left!=null then node.prev.right==null
                            if (node.right == null) {
                                attachHere = node;
                                attachFromLeft = false;
                                removeFromStart = false;
                            } else {
                                attachHere = nextNode;
                                attachFromLeft = true;
                                removeFromStart = false;
                            }
                        }
                    }
                }
                K movedKey;
                V movedValue;
                if (removeFromStart) {
                    // node.left_idx == 0
                    movedKey = node.keys[0];
                    movedValue = node.values[0];
                    int resMunus1 = result - 1;
                    System.arraycopy(node.keys, 1, node.keys, 0, resMunus1);
                    System.arraycopy(node.values, 1, node.values, 0, resMunus1);
                    node.keys[resMunus1] = key;
                    node.values[resMunus1] = value;
                } else {
                    // node.right_idx == Node.NODE_SIZE - 1
                    movedKey = node.keys[Node.NODE_SIZE - 1];
                    movedValue = node.values[Node.NODE_SIZE - 1];
                    System.arraycopy(node.keys, result, node.keys, result + 1,
                            Node.NODE_SIZE - 1 - result);
                    System.arraycopy(node.values, result, node.values,
                            result + 1, Node.NODE_SIZE - 1 - result);
                    node.keys[result] = key;
                    node.values[result] = value;
                }
                if (attachHere == null) {
                    if (removeFromStart) {
                        appendFromRight(previous, movedKey, movedValue);
                    } else {
                        appendFromLeft(nextNode, movedKey, movedValue);
                    }
                } else {
                    Node<K, V> newNode = createNode(movedKey, movedValue);
                    if (attachFromLeft) {
                        attachToLeft(attachHere, newNode);
                    } else {
                        attachToRight(attachHere, newNode);
                    }
                    balance(newNode);
                }
            }
        }
        return null;
    }

    private void appendFromLeft(Node<K, V> node, K keyObj, V value) {
        if (node.left_idx == 0) {
            int new_right = node.right_idx + 1;
            System.arraycopy(node.keys, 0, node.keys, 1, new_right);
            System.arraycopy(node.values, 0, node.values, 1, new_right);
            node.right_idx = new_right;
        } else {
            node.left_idx--;
        }
        node.size++;
        node.keys[node.left_idx] = keyObj;
        node.values[node.left_idx] = value;
    }

    private void attachToLeft(Node<K, V> node, Node<K, V> newNode) {
        newNode.parent = node;
        // node.left==null - attach here
        node.left = newNode;
        Node<K, V> predecessor = node.prev;
        newNode.prev = predecessor;
        newNode.next = node;
        if (predecessor != null) {
            predecessor.next = newNode;
        }
        node.prev = newNode;
    }

    /*
     * add pair into node; existence free room in the node should be checked
     * before call
     */
    private void appendFromRight(Node<K, V> node, K keyObj, V value) {
        if (node.right_idx == Node.NODE_SIZE - 1) {
            int left_idx = node.left_idx;
            int left_idxMinus1 = left_idx - 1;
            System.arraycopy(node.keys, left_idx, node.keys, left_idxMinus1,
                    Node.NODE_SIZE - left_idx);
            System.arraycopy(node.values, left_idx, node.values,
                    left_idxMinus1, Node.NODE_SIZE - left_idx);
            node.left_idx = left_idxMinus1;
        } else {
            node.right_idx++;
        }
        node.size++;
        node.keys[node.right_idx] = keyObj;
        node.values[node.right_idx] = value;
    }

    private void attachToRight(Node<K, V> node, Node<K, V> newNode) {
        newNode.parent = node;
        // - node.right==null - attach here
        node.right = newNode;
        newNode.prev = node;
        Node<K, V> successor = node.next;
        newNode.next = successor;
        if (successor != null) {
            successor.prev = newNode;
        }
        node.next = newNode;
    }

    private Node<K, V> createNode(K keyObj, V value) {
        Node<K, V> node = new Node<K, V>();
        node.keys[0] = keyObj;
        node.values[0] = value;
        node.left_idx = 0;
        node.right_idx = 0;
        node.size = 1;
        return node;
    }

    void balance(Node<K, V> x) {
        Node<K, V> y;
        x.color = true;
        while (x != root && x.parent.color) {
            if (x.parent == x.parent.parent.left) {
                y = x.parent.parent.right;
                if (y != null && y.color) {
                    x.parent.color = false;
                    y.color = false;
                    x.parent.parent.color = true;
                    x = x.parent.parent;
                } else {
                    if (x == x.parent.right) {
                        x = x.parent;
                        leftRotate(x);
                    }
                    x.parent.color = false;
                    x.parent.parent.color = true;
                    rightRotate(x.parent.parent);
                }
            } else {
                y = x.parent.parent.left;
                if (y != null && y.color) {
                    x.parent.color = false;
                    y.color = false;
                    x.parent.parent.color = true;
                    x = x.parent.parent;
                } else {
                    if (x == x.parent.left) {
                        x = x.parent;
                        rightRotate(x);
                    }
                    x.parent.color = false;
                    x.parent.parent.color = true;
                    leftRotate(x.parent.parent);
                }
            }
        }
        root.color = false;
    }

    private void rightRotate(Node<K, V> x) {
        Node<K, V> y = x.left;
        x.left = y.right;
        if (y.right != null) {
            y.right.parent = x;
        }
        y.parent = x.parent;
        if (x.parent == null) {
            root = y;
        } else {
            if (x == x.parent.right) {
                x.parent.right = y;
            } else {
                x.parent.left = y;
            }
        }
        y.right = x;
        x.parent = y;
    }

    private void leftRotate(Node<K, V> x) {
        Node<K, V> y = x.right;
        x.right = y.left;
        if (y.left != null) {
            y.left.parent = x;
        }
        y.parent = x.parent;
        if (x.parent == null) {
            root = y;
        } else {
            if (x == x.parent.left) {
                x.parent.left = y;
            } else {
                x.parent.right = y;
            }
        }
        y.left = x;
        x.parent = y;
    }

    /**
     * Copies all the mappings in the given map to this map. These mappings will
     * replace all mappings that this map had for any of the keys currently in
     * the given map.
     *
     * @param map
     *            the map to copy mappings from.
     * @throws ClassCastException
     *             if a key in the specified map cannot be compared with the
     *             keys in this map.
     * @throws NullPointerException
     *             if a key in the specified map is {@code null} and the
     *             comparator cannot handle {@code null} keys.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        Iterator it = map.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<? extends K, ? extends V> entry = (Map.Entry<? extends K, ? extends V>)it.next();
            this.putImpl(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Removes the mapping with the specified key from this map.
     *
     * @param key
     *            the key of the mapping to remove.
     * @return the value of the removed mapping or {@code null} if no mapping
     *         for the specified key was found.
     * @throws ClassCastException
     *             if the specified key cannot be compared with the keys in this
     *             map.
     * @throws NullPointerException
     *             if the specified key is {@code null} and the comparator
     *             cannot handle {@code null} keys.
     */
    @SuppressWarnings("unchecked")
    @Override
    public V remove(Object key) {
        java.lang.Comparable<K> object = comparator == null ? toComparable((K) key) : null;
        if (size == 0) {
            return null;
        }
        K keyK = (K) key;
        Node<K, V> node = root;
        while (node != null) {
            K[] keys = node.keys;
            int left_idx = node.left_idx;
            int result = cmp(object, keyK, keys[left_idx]);
            if (result < 0) {
                node = node.left;
            } else if (result == 0) {
                V value = node.values[left_idx];
                removeLeftmost(node);
                return value;
            } else {
                int right_idx = node.right_idx;
                if (left_idx != right_idx) {
                    result = cmp(object, keyK, keys[right_idx]);
                }
                if (result > 0) {
                    node = node.right;
                } else if (result == 0) {
                    V value = node.values[right_idx];
                    removeRightmost(node);
                    return value;
                } else { /*search in node*/
                    int low = left_idx + 1, mid = 0, high = right_idx - 1;
                    while (low <= high) {
                        mid = (low + high) >>> 1;
                        result = cmp(object, keyK, keys[mid]);
                        if (result > 0) {
                            low = mid + 1;
                        } else if (result == 0) {
                            V value = node.values[mid];
                            removeMiddleElement(node, mid);
                            return value;
                        } else {
                            high = mid - 1;
                        }
                    }
                    return null;
                }
            }
        }
        return null;
    }

    K removeLeftmost(Node<K, V> node) {
        int index = node.left_idx;
        // record next key and record if the node is removed
        K key = (index + 1 <= node.right_idx) ? node.keys[index + 1] : null;
        if (node.size == 1) {
            deleteNode(node);
        } else if (node.prev != null && (Node.NODE_SIZE - 1 - node.prev.right_idx) > node.size) {
            // move all to prev node and kill it
            Node<K, V> prev = node.prev;
            int size = node.right_idx - index;
            System.arraycopy(node.keys,   index + 1, prev.keys,   prev.right_idx + 1, size);
            System.arraycopy(node.values, index + 1, prev.values, prev.right_idx + 1, size);
            prev.right_idx += size;
            prev.size += size;
            deleteNode(node);
        } else if (node.next != null && (node.next.left_idx) > node.size) {
            // move all to next node and kill it
            Node<K, V> next = node.next;
            int size = node.right_idx - index;
            int next_new_left = next.left_idx - size;
            next.left_idx = next_new_left;
            System.arraycopy(node.keys,   index + 1, next.keys,   next_new_left, size);
            System.arraycopy(node.values, index + 1, next.values, next_new_left, size);
            next.size += size;
            deleteNode(node);
        } else {
            node.keys[index] = null;
            node.values[index] = null;
            node.left_idx++;
            node.size--;
            Node<K, V> prev = node.prev;
            key = null;
            if (prev != null && prev.size == 1) {
                node.size++;
                node.left_idx--;
                node.keys  [node.left_idx] = prev.keys  [prev.left_idx];
                node.values[node.left_idx] = prev.values[prev.left_idx];
                deleteNode(prev);
            }
        }
        modCount++;
        size--;
        return key;
    }

    K removeRightmost(Node<K, V> node) {
        int index = node.right_idx;
        // store the next key, return if the node is deleted
        K key = (node != null && node.next != null) ? node.next.keys[node.next.left_idx] : null;
        if (node.size == 1) {
            deleteNode(node);
        } else if (node.prev != null && (Node.NODE_SIZE - 1 - node.prev.right_idx) > node.size) {
            // move all to prev node and kill it
            Node<K, V> prev = node.prev;
            int left_idx = node.left_idx;
            int size = index - left_idx;
            System.arraycopy(node.keys,   left_idx, prev.keys,   prev.right_idx + 1, size);
            System.arraycopy(node.values, left_idx, prev.values, prev.right_idx + 1, size);
            prev.right_idx += size;
            prev.size += size;
            deleteNode(node);
        } else if (node.next != null && (node.next.left_idx) > node.size) {
            // move all to next node and kill it
            Node<K, V> next = node.next;
            int left_idx = node.left_idx;
            int size = index - left_idx;
            int next_new_left = next.left_idx - size;
            next.left_idx = next_new_left;
            System.arraycopy(node.keys,   left_idx, next.keys,   next_new_left, size);
            System.arraycopy(node.values, left_idx, next.values, next_new_left, size);
            next.size += size;
            deleteNode(node);
        } else {
            node.keys[index] = null;
            node.values[index] = null;
            node.right_idx--;
            node.size--;
            Node<K, V> next = node.next;
            key = null;
            if (next != null && next.size == 1) {
                node.size++;
                node.right_idx++;
                node.keys[node.right_idx]   = next.keys[next.left_idx];
                node.values[node.right_idx] = next.values[next.left_idx];
                deleteNode(next);
            }
        }
        modCount++;
        size--;
        return key;
    }

    K removeMiddleElement(Node<K, V> node, int index) {
        // this function is called iff index if some middle element;
        // so node.left_idx < index < node.right_idx
        // condition above assume that node.size > 1
        K ret = null;
        if (node.prev != null && (Node.NODE_SIZE - 1 - node.prev.right_idx) > node.size) {
            // move all to prev node and kill it
            Node<K, V> prev = node.prev;
            int left_idx = node.left_idx;
            int size = index - left_idx;
            System.arraycopy(node.keys,   left_idx, prev.keys,   prev.right_idx + 1, size);
            System.arraycopy(node.values, left_idx, prev.values, prev.right_idx + 1, size);
            prev.right_idx += size;
            size = node.right_idx - index;
            System.arraycopy(node.keys,   index + 1, prev.keys,   prev.right_idx + 1, size);
            System.arraycopy(node.values, index + 1, prev.values, prev.right_idx + 1, size);
            ret = prev.keys[prev.right_idx + 1];
            prev.right_idx += size;
            prev.size += (node.size - 1);
            deleteNode(node);
        } else if (node.next != null && (node.next.left_idx) > node.size) {
            // move all to next node and kill it
            Node<K, V> next = node.next;
            int left_idx = node.left_idx;
            int next_new_left = next.left_idx - node.size + 1;
            next.left_idx = next_new_left;
            int size = index - left_idx;
            System.arraycopy(node.keys,   left_idx, next.keys,   next_new_left, size);
            System.arraycopy(node.values, left_idx, next.values, next_new_left, size);
            next_new_left += size;
            size = node.right_idx - index;
            System.arraycopy(node.keys,   index + 1, next.keys,   next_new_left, size);
            System.arraycopy(node.values, index + 1, next.values, next_new_left, size);
            ret = next.keys[next_new_left];
            next.size += (node.size - 1);
            deleteNode(node);
        } else {
            int moveFromRight = node.right_idx - index;
            int left_idx = node.left_idx;
            int moveFromLeft = index - left_idx ;
            if (moveFromRight <= moveFromLeft) {
                System.arraycopy(node.keys,   index + 1, node.keys,   index, moveFromRight);
                System.arraycopy(node.values, index + 1, node.values, index, moveFromRight);
                Node<K, V> next = node.next;
                if (next != null && next.size == 1) {
                    node.keys  [node.right_idx] = next.keys  [next.left_idx];
                    node.values[node.right_idx] = next.values[next.left_idx];
                    ret = node.keys[index];
                    deleteNode(next);
                } else {
                    node.keys  [node.right_idx] = null;
                    node.values[node.right_idx] = null;
                    node.right_idx--;
                    node.size--;
                }
            } else {
                System.arraycopy(node.keys,   left_idx , node.keys,   left_idx  + 1, moveFromLeft);
                System.arraycopy(node.values, left_idx , node.values, left_idx + 1, moveFromLeft);
                Node<K, V> prev = node.prev;
                if (prev != null && prev.size == 1) {
                    node.keys  [left_idx ] = prev.keys  [prev.left_idx];
                    node.values[left_idx ] = prev.values[prev.left_idx];
                    ret = node.keys[index + 1];
                    deleteNode(prev);
                } else {
                    node.keys  [left_idx ] = null;
                    node.values[left_idx ] = null;
                    node.left_idx++;
                    node.size--;
                }
            }
        }
        modCount++;
        size--;
        return ret;
    }

    private void deleteNode(Node<K, V> node) {
        if (node.right == null) {
            if (node.left != null) {
                attachToParent(node, node.left);
           } else {
                attachNullToParent(node);
            }
            fixNextChain(node);
        } else if(node.left == null) { // node.right != null
            attachToParent(node, node.right);
            fixNextChain(node);
        } else {
            // Here node.left!=nul && node.right!=null
            // node.next should replace node in tree
            // node.next!=null by tree logic.
            // node.next.left==null by tree logic.
            // node.next.right may be null or non-null
            Node<K, V> toMoveUp = node.next;
            fixNextChain(node);
            if(toMoveUp.right==null){
                attachNullToParent(toMoveUp);
            } else {
                attachToParent(toMoveUp, toMoveUp.right);
            }
            // Here toMoveUp is ready to replace node
            toMoveUp.left = node.left;
            if (node.left != null) {
                node.left.parent = toMoveUp;
            }
            toMoveUp.right = node.right;
            if (node.right != null) {
                node.right.parent = toMoveUp;
            }
            attachToParentNoFixup(node,toMoveUp);
            toMoveUp.color = node.color;
        }
    }

    private void attachToParentNoFixup(Node<K, V> toDelete, Node<K, V> toConnect) {
        // assert toConnect!=null
        Node<K,V> parent = toDelete.parent;
        toConnect.parent = parent;
        if (parent == null) {
            root = toConnect;
        } else if (toDelete == parent.left) {
            parent.left = toConnect;
        } else {
            parent.right = toConnect;
        }
    }

    private void attachToParent(Node<K, V> toDelete, Node<K, V> toConnect) {
        // assert toConnect!=null
        attachToParentNoFixup(toDelete,toConnect);
        if (!toDelete.color) {
            fixup(toConnect);
        }
    }

    private void attachNullToParent(Node<K, V> toDelete) {
        Node<K, V> parent = toDelete.parent;
        if (parent == null) {
            root = null;
        } else {
            if (toDelete == parent.left) {
                parent.left = null;
            } else {
                parent.right = null;
            }
            if (!toDelete.color) {
                fixup(parent);
            }
        }
    }

    private void fixNextChain(Node<K, V> node) {
        if (node.prev != null) {
            node.prev.next = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        }
    }

    private void fixup(Node<K, V> x) {
        Node<K, V> w;
        while (x != root && !x.color) {
            if (x == x.parent.left) {
                w = x.parent.right;
                if (w == null) {
                    x = x.parent;
                    continue;
                }
                if (w.color) {
                    w.color = false;
                    x.parent.color = true;
                    leftRotate(x.parent);
                    w = x.parent.right;
                    if (w == null) {
                        x = x.parent;
                        continue;
                    }
                }
                if ((w.left == null || !w.left.color)
                    && (w.right == null || !w.right.color)) {
                    w.color = true;
                    x = x.parent;
                } else {
                    if (w.right == null || !w.right.color) {
                        w.left.color = false;
                        w.color = true;
                        rightRotate(w);
                        w = x.parent.right;
                    }
                    w.color = x.parent.color;
                    x.parent.color = false;
                    w.right.color = false;
                    leftRotate(x.parent);
                    x = root;
                }
            } else {
                w = x.parent.left;
                if (w == null) {
                    x = x.parent;
                    continue;
                }
                if (w.color) {
                    w.color = false;
                    x.parent.color = true;
                    rightRotate(x.parent);
                    w = x.parent.left;
                    if (w == null) {
                        x = x.parent;
                        continue;
                    }
                }
                if ((w.left == null || !w.left.color)
                    && (w.right == null || !w.right.color)) {
                    w.color = true;
                    x = x.parent;
                } else {
                    if (w.left == null || !w.left.color) {
                        w.right.color = false;
                        w.color = true;
                        leftRotate(w);
                        w = x.parent.left;
                    }
                    w.color = x.parent.color;
                    x.parent.color = false;
                    w.left.color = false;
                    rightRotate(x.parent);
                    x = root;
                }
            }
        }
        x.color = false;
    }

    /**
     * Returns the number of mappings in this map.
     *
     * @return the number of mappings in this map.
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Returns a collection of the values contained in this map. The collection
     * is backed by this map so changes to one are reflected by the other. The
     * collection supports remove, removeAll, retainAll and clear operations,
     * and it does not support add or addAll operations.
     * <p>
     * This method returns a collection which is the subclass of
     * AbstractCollection. The iterator method of this subclass returns a
     * "wrapper object" over the iterator of map's entrySet(). The {@code size}
     * method wraps the map's size method and the {@code contains} method wraps
     * the map's containsValue method.
     * <p>
     * The collection is created when this method is called for the first time
     * and returned in response to all subsequent calls. This method may return
     * different collections when multiple concurrent calls occur, since no
     * synchronization is performed.
     *
     * @return a collection of the values contained in this map.
     */
    @Override
    public Collection<V> values() {
        if (valuesCollection == null) {
            valuesCollection = new AbstractCollection<V>() {
                @Override
                public boolean contains(Object object) {
                    return containsValue(object);
                }

                @Override
                public int size() {
                    return size;
                }

                @Override
                public void clear() {
                    TreeMap.this.clear();
                }

                @Override
                public Iterator<V> iterator() {
                    return new UnboundedValueIterator<K,V> (TreeMap.this);
                }
            };
        }
        return valuesCollection;
    }    

    /**
     * {@inheritDoc}
     * 
     * @see java.util.NavigableMap#firstEntry()
     * @since 1.6
     */
    public Map.Entry<K, V> firstEntry() {
        return newImmutableEntry(findSmallestEntry());
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.NavigableMap#lastEntry()
     * @since 1.6
     */
    public Map.Entry<K, V> lastEntry() {
        return newImmutableEntry(findBiggestEntry());
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.NavigableMap#pollFirstEntry()
     * @since 1.6
     */
    public Map.Entry<K, V> pollFirstEntry() {
        Entry<K, V> node = findSmallestEntry();
        SimpleImmutableEntry<K, V> result = newImmutableEntry(node);
        if (null != node) {
            remove(node.key);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.NavigableMap#pollLastEntry()
     * @since 1.6
     */
    public Map.Entry<K, V> pollLastEntry() {
        Entry<K, V> node = findBiggestEntry();
        SimpleImmutableEntry<K, V> result = newImmutableEntry(node);
        if (null != node) {
            remove(node.key);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.NavigableMap#higherEntry(Object)
     * @since 1.6
     */
    public Map.Entry<K, V> higherEntry(K key) {
        return newImmutableEntry(findHigherEntry(key));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.NavigableMap#higherKey(Object)
     * @since 1.6
     */
    public K higherKey(K key) {
        Map.Entry<K, V> entry = higherEntry(key);
        return (null == entry) ? null : entry.getKey();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.NavigableMap#lowerEntry(Object)
     * @since 1.6
     */
    public Map.Entry<K, V> lowerEntry(K key) {
        return newImmutableEntry(findLowerEntry(key));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.NavigableMap#lowerKey(Object)
     * @since 1.6
     */
    public K lowerKey(K key) {
        Map.Entry<K, V> entry = lowerEntry(key);
        return (null == entry) ? null : entry.getKey();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.NavigableMap#ceilingEntry(java.lang.Object)
     * @since 1.6
     */
    public Map.Entry<K, V> ceilingEntry(K key) {
        return newImmutableEntry(findCeilingEntry(key));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.NavigableMap#ceilingKey(java.lang.Object)
     * @since 1.6
     */
    public K ceilingKey(K key) {
        Map.Entry<K, V> entry = ceilingEntry(key);
        return (null == entry) ? null : entry.getKey();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.NavigableMap#floorEntry(Object)
     * @since 1.6
     */
    public Map.Entry<K, V> floorEntry(K key) {
        return newImmutableEntry(findFloorEntry(key));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.NavigableMap#floorKey(Object)
     * @since 1.6
     */
    public K floorKey(K key) {
        Map.Entry<K, V> entry = floorEntry(key);
        return (null == entry) ? null : entry.getKey();
    }

    final AbstractMap.SimpleImmutableEntry<K, V> newImmutableEntry(
            TreeMap.Entry<K, V> entry) {
        return (null == entry) ? null : new SimpleImmutableEntry<K, V>(entry);
    }

    @SuppressWarnings("unchecked")
    private static <T> java.lang.Comparable<T> toComparable(T obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        return (java.lang.Comparable<T>) obj;
    }

    int keyCompare(K left, K right) {
        return (null != comparator()) ? comparator().compare(left, right)
                : toComparable(left).compareTo(right);
    }
    
    static <K, V> Node<K, V> minimum(Node<K, V> x) {
        if (x == null) {
            return null;
        }
        while (x.left != null) {
            x = x.left;
        }
        return x;
    }

    static <K, V> Node<K, V> maximum(Node<K, V> x) {
        if (x == null) {
            return null;
        }
        while (x.right != null) {
            x = x.right;
        }
        return x;
    }

    /**
     * Returns a set containing all of the mappings in this map. Each mapping is
     * an instance of {@link Map.Entry}. As the set is backed by this map,
     * changes in one will be reflected in the other. It does not support adding
     * operations.
     *
     * @return a set of the mappings.
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        if (entrySet == null) {
            entrySet = new AbstractSet<Map.Entry<K, V>>() {
                @Override
                public int size() {
                    return size;
                }

                @Override
                public void clear() {
                    TreeMap.this.clear();
                }

                @SuppressWarnings("unchecked")
                @Override
                public boolean contains(Object object) {
                    if (object instanceof Map.Entry) {
                        Map.Entry<K, V> entry = (Map.Entry<K, V>) object;
                        K key = entry.getKey();
                        Object v1 = get(key), v2 = entry.getValue();
                        return v1 == null ? ( v2 == null && TreeMap.this.containsKey(key) ) : v1.equals(v2);
                    }
                    return false;
                }

                @Override
                public Iterator<Map.Entry<K, V>> iterator() {
                    return new UnboundedEntryIterator<K, V>(TreeMap.this);
                }
            };
        }
        return entrySet;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.NavigableMap#navigableKeySet()
     * @since 1.6
     */
    @SuppressWarnings("unchecked")
    public NavigableSet<K> navigableKeySet() {
        return (null != navigableKeySet) ? navigableKeySet
                : (navigableKeySet = (new AscendingSubMap<K, V>(this))
                        .navigableKeySet());
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.NavigableMap#descendingKeySet()
     * @since 1.6
     */
    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.NavigableMap#descendingMap()
     * @since 1.6
     */
    public NavigableMap<K, V> descendingMap() {
        return (null != descendingMap) ? descendingMap
                : (descendingMap = new DescendingSubMap<K, V>(this));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.NavigableMap#subMap(Object, boolean, Object, boolean)
     * @since 1.6
     */
    public NavigableMap<K, V> subMap(K start, boolean startInclusive, K end,
            boolean endInclusive) {
        if (keyCompare(start, end) <= 0) {
            return new AscendingSubMap<K, V>(start, startInclusive, this, end,
                    endInclusive);
        }
        throw new IllegalArgumentException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.NavigableMap#headMap(Object, boolean)
     * @since 1.6
     */
    public NavigableMap<K, V> headMap(K end, boolean inclusive) {
        // check for error
        keyCompare(end, end);
        return new AscendingSubMap<K, V>(this, end, inclusive);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.util.NavigableMap#tailMap(Object, boolean)
     * @since 1.6
     */
    public NavigableMap<K, V> tailMap(K start, boolean inclusive) {
        // check for error
        keyCompare(start, start);
        return new AscendingSubMap<K, V>(start, inclusive, this);
    }

    /**
     * Returns a sorted map over a range of this sorted map with all keys
     * greater than or equal to the specified {@code startKey} and less than the
     * specified {@code endKey}. Changes to the returned sorted map are
     * reflected in this sorted map and vice versa.
     * <p>
     * Note: The returned map will not allow an insertion of a key outside the
     * specified range.
     *
     * @param startKey
     *            the low boundary of the range (inclusive).
     * @param endKey
     *            the high boundary of the range (exclusive),
     * @return a sorted map with the key from the specified range.
     * @throws ClassCastException
     *             if the start or end key cannot be compared with the keys in
     *             this map.
     * @throws NullPointerException
     *             if the start or end key is {@code null} and the comparator
     *             cannot handle {@code null} keys.
     * @throws IllegalArgumentException
     *             if the start key is greater than the end key, or if this map
     *             is itself a sorted map over a range of another sorted map and
     *             the specified range is outside of its range.
     */
    public SortedMap<K, V> subMap(K startKey, K endKey) {
        if (comparator == null) {
            if (toComparable(startKey).compareTo(endKey) <= 0) {
                return new SubMap<K, V>(startKey, this, endKey);
            }
        } else {
            if (comparator.compare(startKey, endKey) <= 0) {
                return new SubMap<K, V>(startKey, this, endKey);
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns a sorted map over a range of this sorted map with all keys that
     * are less than the specified {@code endKey}. Changes to the returned
     * sorted map are reflected in this sorted map and vice versa.
     * <p>
     * Note: The returned map will not allow an insertion of a key outside the
     * specified range.
     *
     * @param endKey
     *            the high boundary of the range specified.
     * @return a sorted map where the keys are less than {@code endKey}.
     * @throws ClassCastException
     *             if the specified key cannot be compared with the keys in this
     *             map.
     * @throws NullPointerException
     *             if the specified key is {@code null} and the comparator
     *             cannot handle {@code null} keys.
     * @throws IllegalArgumentException
     *             if this map is itself a sorted map over a range of another
     *             map and the specified key is outside of its range.
     */
    public SortedMap<K, V> headMap(K endKey) {
        return headMap(endKey, false);
    }

    /**
     * Returns a sorted map over a range of this sorted map with all keys that
     * are greater than or equal to the specified {@code startKey}. Changes to
     * the returned sorted map are reflected in this sorted map and vice versa.
     * <p>
     * Note: The returned map will not allow an insertion of a key outside the
     * specified range.
     *
     * @param startKey
     *            the low boundary of the range specified.
     * @return a sorted map where the keys are greater or equal to
     *         {@code startKey}.
     * @throws ClassCastException
     *             if the specified key cannot be compared with the keys in this
     *             map.
     * @throws NullPointerException
     *             if the specified key is {@code null} and the comparator
     *             cannot handle {@code null} keys.
     * @throws IllegalArgumentException
     *             if this map itself a sorted map over a range of another map
     *             and the specified key is outside of its range.
     */
    public SortedMap<K, V> tailMap(K startKey) {
        return tailMap(startKey, true);
    }

    
    static class AbstractMapIterator<K, V> {
        TreeMap<K, V> backingMap;

        int expectedModCount;

        Node<K, V> node;

        Node<K, V> lastNode;

        int offset;

        int lastOffset;

        AbstractMapIterator(TreeMap<K, V> map, Node<K, V> startNode,
                int startOffset) {
            backingMap = map;
            expectedModCount = map.modCount;
            if (startNode != null) {
                node = startNode;
                offset = startOffset;
            } else {
                Entry<K,V> entry = map.findSmallestEntry();
                if (entry != null){
                    node = map.findSmallestEntry().node;
                    offset = node.left_idx;
                }
            }
        }

        AbstractMapIterator(TreeMap<K, V> map, Node<K, V> startNode) {
            this(map, startNode, startNode == null ? 0 : startNode.left_idx);
        }

        AbstractMapIterator(TreeMap<K, V> map) {
            this(map, minimum(map.root));
        }

        public boolean hasNext() {
            return node != null;
        }

        final void makeNext() {
            if (expectedModCount != backingMap.modCount) {
                throw new ConcurrentModificationException();
            } else if (node == null) {
                throw new NoSuchElementException();
            }
            lastNode = node;
            lastOffset = offset;
            if (offset != node.right_idx) {
                offset ++;
            } else {
                node = node.next;
                if (node != null) {
                    offset = node.left_idx;
                }
            }
        }

        final public void remove() {
            if (expectedModCount == backingMap.modCount) {
                if (lastNode != null) {
                    int idx = lastOffset;
                    K key = null;
                    if (idx == lastNode.left_idx){
                        key = backingMap.removeLeftmost(lastNode);
                    } else if (idx == lastNode.right_idx) {
                        key = backingMap.removeRightmost(lastNode);
                    } else {
                        int lastRight = lastNode.right_idx;
                        key = backingMap.removeMiddleElement(node, idx);
                        if (null == key && lastRight > lastNode.right_idx) {
                                // removed from right
                                offset--;
                            }
                    }
                    if (null != key) {
                        // the node has been cleared, need to find new node
                        Entry<K,V> entry = backingMap.find(key);
                        node = entry.node;
                        offset = entry.index;
                    }
                    lastNode = null;
                    expectedModCount++;
                } else {
                    throw new IllegalStateException();
                }
            } else {
                throw new ConcurrentModificationException();
            }
        }
    }
    
    static class TreeMapEntry<K,V> extends MapEntry<K,V>{
        Node<K, V> node;
        int index;
        
        TreeMapEntry(K theKey, V theValue, Node<K,V> node, int index) {
            super(theKey, theValue);
            this.node = node;
            this.index = index;
        }    
        
        // overwrite 
        public V setValue(V object) {
            V result = value;
            value = object;
            // set back to TreeMap
            node.values[index] = object;
            return result;
        }
    }
    
    static class UnboundedEntryIterator<K, V> extends AbstractMapIterator<K, V>
            implements Iterator<Map.Entry<K, V>> {

        UnboundedEntryIterator(TreeMap<K, V> map, Node<K, V> startNode,
                int startOffset) {
            super(map, startNode, startOffset);
        }

        UnboundedEntryIterator(TreeMap<K, V> map) {
            super(map);
        }

        public Map.Entry<K, V> next() {
            makeNext();
            int idx = lastOffset;
            return new TreeMapEntry<K, V>(lastNode.keys[idx], lastNode.values[idx], lastNode, idx);
        }
    }

    static class UnboundedKeyIterator<K, V> extends AbstractMapIterator<K, V>
            implements Iterator<K> {

        UnboundedKeyIterator(TreeMap<K, V> map, Node<K, V> startNode,
                int startOffset) {
            super(map, startNode, startOffset);
        }

        UnboundedKeyIterator(TreeMap<K, V> map) {
            super(map);
        }

        public K next() {
            makeNext();
            return lastNode.keys[lastOffset];
        }
    }

    static class UnboundedValueIterator<K, V> extends AbstractMapIterator<K, V>
            implements Iterator<V> {

        UnboundedValueIterator(TreeMap<K, V> map, Node<K, V> startNode,
                int startOffset) {
            super(map, startNode, startOffset);
        }

        UnboundedValueIterator(TreeMap<K, V> map) {
            super(map);
        }

        public V next() {
            makeNext();
            return lastNode.values[lastOffset];
        }
    }
}
