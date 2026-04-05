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

/// **Note: Do not use this class since it is obsolete. Please use the
/// `Map` interface for new implementations.**
///
/// Dictionary is an abstract class which is the superclass of all classes that
/// associate keys with values, such as `Hashtable`.
///
/// #### Since
///
/// 1.0
///
/// #### See also
///
/// - Hashtable
public abstract class Dictionary<K, V> {
    /// Constructs a new instance of this class.
    public Dictionary() {
        super();
    }

    /// Returns an enumeration on the elements of this dictionary.
    ///
    /// #### Returns
    ///
    /// an enumeration of the values of this dictionary.
    ///
    /// #### See also
    ///
    /// - #keys
    ///
    /// - #size
    ///
    /// - Enumeration
    public abstract java.util.Enumeration<V> elements();

    /// Returns the value which is associated with `key`.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key of the value returned.
    ///
    /// #### Returns
    ///
    /// @return the value associated with `key`, or `null` if the
    /// specified key does not exist.
    ///
    /// #### See also
    ///
    /// - #put
    public abstract V get(Object key);

    /// Returns true if this dictionary has no key/value pairs.
    ///
    /// #### Returns
    ///
    /// @return `true` if this dictionary has no key/value pairs,
    /// `false` otherwise.
    ///
    /// #### See also
    ///
    /// - #size
    public abstract boolean isEmpty();

    /// Returns an enumeration on the keys of this dictionary.
    ///
    /// #### Returns
    ///
    /// an enumeration of the keys of this dictionary.
    ///
    /// #### See also
    ///
    /// - #elements
    ///
    /// - #size
    ///
    /// - Enumeration
    public abstract java.util.Enumeration<K> keys();

    /// Associate `key` with `value` in this dictionary. If `key` exists in the dictionary before this call, the old value in the
    /// dictionary is replaced by `value`.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key to add.
    ///
    /// - `value`: the value to add.
    ///
    /// #### Returns
    ///
    /// the old value previously associated with `key` or `null` if `key` is new to the dictionary.
    ///
    /// #### See also
    ///
    /// - #elements
    ///
    /// - #get
    ///
    /// - #keys
    public abstract V put(K key, V value);

    /// Removes the key/value pair with the specified `key` from this
    /// dictionary.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key to remove.
    ///
    /// #### Returns
    ///
    /// @return the associated value before the deletion or `null` if
    /// `key` was not known to this dictionary.
    ///
    /// #### See also
    ///
    /// - #get
    ///
    /// - #put
    public abstract V remove(Object key);

    /// Returns the number of key/value pairs in this dictionary.
    ///
    /// #### Returns
    ///
    /// the number of key/value pairs in this dictionary.
    ///
    /// #### See also
    ///
    /// - #elements
    ///
    /// - #keys
    public abstract int size();
}
