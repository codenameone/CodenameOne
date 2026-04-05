/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.util;

/// NavigableMap is a SortedMap with navigation methods answering the closest
/// matches for specified item.
///
/// Type parameter `K`: the type of key
/// Type parameter `V`: the type of value
///
/// #### Since
///
/// 1.6
public interface NavigableMap<K, V> extends SortedMap<K, V> {
    /// Answers the entry with the smallest key, or null if the map is empty.
    ///
    /// #### Returns
    ///
    /// the entry with the smallest key, or null if the map is empty
    Map.Entry<K, V> firstEntry();

    /// Answers the entry with the biggest key, or null if the map is empty.
    ///
    /// #### Returns
    ///
    /// the entry with the biggest key, or null if the map is empty
    Map.Entry<K, V> lastEntry();

    /// Deletes and answers the entry with the smallest key, or null if the map
    /// is empty.
    ///
    /// #### Returns
    ///
    /// the entry with the smallest key, or null if the map is empty
    Map.Entry<K, V> pollFirstEntry();

    /// Deletes and answers the entry with the biggest key, or null if the map is
    /// empty.
    ///
    /// #### Returns
    ///
    /// the entry with the biggest key, or null if the map is empty
    Map.Entry<K, V> pollLastEntry();

    /// Answers an entry related with the smallest key greater than or equal to
    /// the specified key, or null if no such key.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    ///
    /// #### Returns
    ///
    /// the entry, or null if no such key
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: if the key cannot be compared with the keys in the map
    ///
    /// - `NullPointerException`: if the key is null and the map can not contain null key
    Map.Entry<K, V> ceilingEntry(K key);

    /// Answers the smallest key greater than or equal to the specified key, or
    /// null if no such key.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    ///
    /// #### Returns
    ///
    /// @return the smallest key greater than or equal to key, or null if no such
    /// key
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: if the key cannot be compared with the keys in the map
    ///
    /// - `NullPointerException`: if the key is null and the map can not contain null key
    K ceilingKey(K key);

    /// Answers an entry related with the smallest key greater than the specified
    /// key, or null if no such key.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    ///
    /// #### Returns
    ///
    /// the entry, or null if no such key
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: if the key cannot be compared with the keys in the map
    ///
    /// - `NullPointerException`: if the key is null and the map can not contain null key
    Map.Entry<K, V> higherEntry(K key);

    /// Answers the smallest key greater than the specified key, or null if no
    /// such key.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    ///
    /// #### Returns
    ///
    /// the smallest key greater than key, or null if no such key
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: if the key cannot be compared with the keys in the map
    ///
    /// - `NullPointerException`: if the key is null and the map can not contain null key
    K higherKey(K key);

    /// Answers an entry related with the biggest key less than or equal to the
    /// specified key, or null if no such key.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    ///
    /// #### Returns
    ///
    /// the entry, or null if no such key
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: if the key cannot be compared with the keys in the map
    ///
    /// - `NullPointerException`: if the key is null and the map can not contain null key
    Map.Entry<K, V> floorEntry(K key);

    /// Answers the biggest key less than or equal to the specified key, or null
    /// if no such key.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    ///
    /// #### Returns
    ///
    /// the biggest key less than or equal to key, or null if no such key
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: if the key cannot be compared with the keys in the map
    ///
    /// - `NullPointerException`: if the key is null and the map can not contain null key
    K floorKey(K key);

    /// Answers an entry related with the biggest key less than the specified
    /// key, or null if no such key.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    ///
    /// #### Returns
    ///
    /// the entry, or null if no such key
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: if the key cannot be compared with the keys in the map
    ///
    /// - `NullPointerException`: if the key is null and the map can not contain null key
    Map.Entry<K, V> lowerEntry(K key);

    /// Answers the biggest key less than the specified key, or null if no such
    /// key.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    ///
    /// #### Returns
    ///
    /// the biggest key less than key, or null if no such key
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: if the key cannot be compared with the keys in the map
    ///
    /// - `NullPointerException`: if the key is null and the map can not contain null key
    K lowerKey(K key);

    /// Answers a NavigableSet view of the keys in ascending order.
    ///
    /// #### Returns
    ///
    /// the navigable set view
    NavigableSet<K> navigableKeySet();

    /// Answers a reverse order view of the map.
    ///
    /// #### Returns
    ///
    /// the reverse order view of the map
    NavigableMap<K, V> descendingMap();

    /// Answers a NavigableSet view of the keys in descending order.
    ///
    /// #### Returns
    ///
    /// the navigable set view
    NavigableSet<K> descendingKeySet();

    /// Answers a view of part of the map whose keys is from startKey to endKey.
    ///
    /// #### Parameters
    ///
    /// - `startKey`: the start key
    ///
    /// - `startInclusive`: true if the start key is in the returned map
    ///
    /// - `endKey`: the end key
    ///
    /// - `endInclusive`: true if the end key is in the returned map
    ///
    /// #### Returns
    ///
    /// the sub-map view
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @exception ClassCastException
    /// when the class of the start or end key is inappropriate
    /// for this SubMap
    ///
    /// - `NullPointerException`: @exception NullPointerException
    /// when the start or end key is null and this SortedMap does
    /// not support null keys
    ///
    /// - `IllegalArgumentException`: when the start key is greater than the end key
    NavigableMap<K, V> subMap(K startKey, boolean startInclusive, K endKey,
            boolean endInclusive);

    /// Answers a view of the head of the map whose keys are smaller than (or
    /// equal to, depends on inclusive argument) endKey.
    ///
    /// #### Parameters
    ///
    /// - `endKey`: the end key
    ///
    /// - `inclusive`: true if the end key is in the returned map
    ///
    /// #### Returns
    ///
    /// the head-map view
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @exception ClassCastException
    /// when the class of the end key is inappropriate for this
    /// SubMap
    ///
    /// - `NullPointerException`: @exception NullPointerException
    /// when the end key is null and this SortedMap does not
    /// support null keys
    ///
    /// - `IllegalArgumentException`: @exception IllegalArgumentException
    /// when the map is range-limited and end key is out of the
    /// range of the map
    NavigableMap<K, V> headMap(K endKey, boolean inclusive);

    /// Answers a view of the tail of the map whose keys are bigger than (or
    /// equal to, depends on inclusive argument) startKey.
    ///
    /// #### Parameters
    ///
    /// - `startKey`: the start key
    ///
    /// - `inclusive`: true if the start key is in the returned map
    ///
    /// #### Returns
    ///
    /// the tail-map view
    ///
    /// #### Throws
    ///
    /// - `ClassCastException`: @exception ClassCastException
    /// when the class of the start key is inappropriate for this
    /// SubMap
    ///
    /// - `NullPointerException`: @exception NullPointerException
    /// when the start key is null and this SortedMap does not
    /// support null keys
    ///
    /// - `IllegalArgumentException`: @exception IllegalArgumentException
    /// when the map is range-limited and start key is out of the
    /// range of the map
    NavigableMap<K, V> tailMap(K startKey, boolean inclusive);
}
