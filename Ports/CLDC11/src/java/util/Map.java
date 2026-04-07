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


/// A `Map` is a data structure consisting of a set of keys and values
/// in which each key is mapped to a single value.  The class of the objects
/// used as keys is declared when the `Map` is declared, as is the
/// class of the corresponding values.
///
/// A `Map` provides helper methods to iterate through all of the
/// keys contained in it, as well as various methods to access and update
/// the key/value pairs.
public interface Map<K,V> {

    /// `Map.Entry` is a key/value mapping contained in a `Map`.
    public static interface Entry<K,V> {
        /// Compares the specified object to this `Map.Entry` and returns if they
        /// are equal. To be equal, the object must be an instance of `Map.Entry` and have the
        /// same key and value.
        ///
        /// #### Parameters
        ///
        /// - `object`: the `Object` to compare with this `Object`.
        ///
        /// #### Returns
        ///
        /// @return `true` if the specified `Object` is equal to this
        /// `Map.Entry`, `false` otherwise.
        ///
        /// #### See also
        ///
        /// - #hashCode()
        public boolean equals(Object object);

        /// Returns the key.
        ///
        /// #### Returns
        ///
        /// the key
        public K getKey();

        /// Returns the value.
        ///
        /// #### Returns
        ///
        /// the value
        public V getValue();

        /// Returns an integer hash code for the receiver. `Object` which are
        /// equal return the same value for this method.
        ///
        /// #### Returns
        ///
        /// the receiver's hash code.
        ///
        /// #### See also
        ///
        /// - #equals(Object)
        public int hashCode();

        /// Sets the value of this entry to the specified value, replacing any
        /// existing value.
        ///
        /// #### Parameters
        ///
        /// - `object`: the new value to set.
        ///
        /// #### Returns
        ///
        /// object the replaced value of this entry.
        public V setValue(V object);
    };

    /// Removes all elements from this `Map`, leaving it empty.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if removing elements from this `Map` is not supported.
    ///
    /// #### See also
    ///
    /// - #isEmpty()
    ///
    /// - #size()
    public void clear();

    /// Returns whether this `Map` contains the specified key.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key to search for.
    ///
    /// #### Returns
    ///
    /// @return `true` if this map contains the specified key,
    /// `false` otherwise.
    public boolean containsKey(Object key);

    /// Returns whether this `Map` contains the specified value.
    ///
    /// #### Parameters
    ///
    /// - `value`: the value to search for.
    ///
    /// #### Returns
    ///
    /// @return `true` if this map contains the specified value,
    /// `false` otherwise.
    public boolean containsValue(Object value);

    /// Returns a `Set` containing all of the mappings in this `Map`. Each mapping is
    /// an instance of `Map.Entry`. As the `Set` is backed by this `Map`,
    /// changes in one will be reflected in the other.
    ///
    /// #### Returns
    ///
    /// a set of the mappings
    public Set<Map.Entry<K,V>> entrySet();

    /// Compares the argument to the receiver, and returns `true` if the
    /// specified object is a `Map` and both `Map`s contain the same mappings.
    ///
    /// #### Parameters
    ///
    /// - `object`: the `Object` to compare with this `Object`.
    ///
    /// #### Returns
    ///
    /// @return boolean `true` if the `Object` is the same as this `Object`
    /// `false` if it is different from this `Object`.
    ///
    /// #### See also
    ///
    /// - #hashCode()
    ///
    /// - #entrySet()
    public boolean equals(Object object);

    /// Returns the value of the mapping with the specified key.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key.
    ///
    /// #### Returns
    ///
    /// @return the value of the mapping with the specified key, or `null`
    /// if no mapping for the specified key is found.
    public V get(Object key);

    /// Returns an integer hash code for the receiver. `Object`s which are equal
    /// return the same value for this method.
    ///
    /// #### Returns
    ///
    /// the receiver's hash.
    ///
    /// #### See also
    ///
    /// - #equals(Object)
    public int hashCode();

    /// Returns whether this map is empty.
    ///
    /// #### Returns
    ///
    /// @return `true` if this map has no elements, `false`
    /// otherwise.
    ///
    /// #### See also
    ///
    /// - #size()
    public boolean isEmpty();

    /// Returns a set of the keys contained in this `Map`. The `Set` is backed by
    /// this `Map` so changes to one are reflected by the other. The `Set` does not
    /// support adding.
    ///
    /// #### Returns
    ///
    /// a set of the keys.
    public Set<K> keySet();

    /// Maps the specified key to the specified value.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key.
    ///
    /// - `value`: the value.
    ///
    /// #### Returns
    ///
    /// @return the value of any previous mapping with the specified key or
    /// `null` if there was no mapping.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if adding to this `Map` is not supported.
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// if the class of the key or value is inappropriate for
    /// this `Map`.
    ///
    /// - `IllegalArgumentException`: if the key or value cannot be added to this `Map`.
    ///
    /// - `NullPointerException`: @throws NullPointerException
    /// if the key or value is `null` and this `Map` does
    /// not support `null` keys or values.
    public V put(K key, V value);

    /// Copies every mapping in the specified `Map` to this `Map`.
    ///
    /// #### Parameters
    ///
    /// - `map`: the `Map` to copy mappings from.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if adding to this `Map` is not supported.
    ///
    /// - `ClassCastException`: @throws ClassCastException
    /// if the class of a key or a value of the specified `Map` is
    /// inappropriate for this `Map`.
    ///
    /// - `IllegalArgumentException`: if a key or value cannot be added to this `Map`.
    ///
    /// - `NullPointerException`: @throws NullPointerException
    /// if a key or value is `null` and this `Map` does not
    /// support `null` keys or values.
    public void putAll(Map<? extends K,? extends V> map);

    /// Removes a mapping with the specified key from this `Map`.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key of the mapping to remove.
    ///
    /// #### Returns
    ///
    /// @return the value of the removed mapping or `null` if no mapping
    /// for the specified key was found.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if removing from this `Map` is not supported.
    public V remove(Object key);

    /// Returns the number of mappings in this `Map`.
    ///
    /// #### Returns
    ///
    /// the number of mappings in this `Map`.
    public int size();

    /// Returns a `Collection` of the values contained in this `Map`. The `Collection`
    /// is backed by this `Map` so changes to one are reflected by the other. The
    /// `Collection` supports `Collection#remove`, `Collection#removeAll`,
    /// `Collection#retainAll`, and `Collection#clear` operations,
    /// and it does not support `Collection#add` or `Collection#addAll` operations.
    ///
    /// This method returns a `Collection` which is the subclass of
    /// `AbstractCollection`. The `AbstractCollection#iterator` method of this subclass returns a
    /// "wrapper object" over the iterator of this `Map`'s `#entrySet()`. The `AbstractCollection#size` method
    /// wraps this `Map`'s `#size` method and the `AbstractCollection#contains` method wraps this `Map`'s
    /// `#containsValue` method.
    ///
    /// The collection is created when this method is called at first time and
    /// returned in response to all subsequent calls. This method may return
    /// different Collection when multiple calls to this method, since it has no
    /// synchronization performed.
    ///
    /// #### Returns
    ///
    /// a collection of the values contained in this map.
    public Collection<V> values();
}
