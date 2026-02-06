/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

package com.codename1.properties;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/// Base class for a property as a Map which can contain multiple elements within it
///
/// @author Shai Almog
public class MapProperty<T, J, K> extends PropertyBase<Map.Entry<T, J>, K> implements Iterable<Map.Entry<T, J>> {
    private final LinkedHashMap<T, J> value = new LinkedHashMap<T, J>();
    private Class keyType;
    private Class valueType;

    /// Constructs a property with the given name
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the property
    public MapProperty(String name) {
        super(name);
    }


    /// Constructs a property with the given name
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the property
    ///
    /// - `genericTypeKey`: the generic type of the key
    ///
    /// - `genericTypeValue`: the generic type of the value
    public MapProperty(String name, Class genericTypeKey, Class genericTypeValue) {
        super(name);
        validateCollectionType(genericTypeKey);
        validateCollectionType(genericTypeValue);
        keyType = genericTypeKey;
        valueType = genericTypeValue;
    }

    /// Returns the class for the key element if it's defined or null if it isn't
    ///
    /// #### Returns
    ///
    /// the class matching the map key
    public Class getKeyType() {
        return keyType;
    }

    /// Returns the class for the value element if it's defined or null if it isn't
    ///
    /// #### Returns
    ///
    /// the class matching the map value
    public Class getValueType() {
        return valueType;
    }

    /// Gets the property value
    ///
    /// #### Parameters
    ///
    /// - `key`: the map key
    ///
    /// #### Returns
    ///
    /// the property value
    public J get(T key) {
        internalGet();
        return value.get(key);
    }

    /// The size of the property list
    ///
    /// #### Returns
    ///
    /// the number of elements
    public int size() {
        internalGet();
        return value.size();
    }

    /// Sets the property value and potentially fires a change event
    ///
    /// #### Parameters
    ///
    /// - `key`: the key to set
    ///
    /// - `v`: the new value
    public K set(T key, J v) {
        value.put(key, v);
        firePropertyChanged();
        internalSet();
        return (K) parent.parent;
    }

    /// Same as `java.lang.Object)` here for coding convention convenience
    /// with map code
    ///
    /// #### Parameters
    ///
    /// - `key`: the key to set
    ///
    /// - `v`: the new value
    public K put(T key, J v) {
        return set(key, v);
    }

    /// Removes the item matching the given key
    ///
    /// #### Parameters
    ///
    /// - `key`: the key
    public K remove(T key) {
        value.remove(key);
        internalSet();
        return (K) parent.parent;
    }

    /// Compares this property to another property
    ///
    /// #### Parameters
    ///
    /// - `obj`: the other property
    ///
    /// #### Returns
    ///
    /// true if they are equal in name and value
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        MapProperty other = (MapProperty) obj;
        return other.value.equals(value);
    }

    /// Returns the internal hashcode or 0 for null property
    ///
    /// #### Returns
    ///
    /// the hashcode value
    @Override
    public int hashCode() {
        return value.hashCode();
    }

    /// Iterate over the elements of the property
    ///
    /// #### Returns
    ///
    /// an iterator
    @Override
    public Iterator<Map.Entry<T, J>> iterator() {
        internalGet();
        return value.entrySet().iterator();
    }

    /// Returns the set of keys in the map property
    ///
    /// #### Returns
    ///
    /// the keys
    public Set<T> keySet() {
        return value.keySet();
    }

    /// Returns the set of values in the map property
    ///
    /// #### Returns
    ///
    /// the values
    public Collection<J> valueSet() {
        return value.values();
    }

    /// Returns a copy of the content as a new map
    ///
    /// #### Returns
    ///
    /// a map
    public Map<T, J> asMap() {
        internalGet();
        return new LinkedHashMap<T, J>(value);
    }

    /// Returns a copy of the content as a new map but if the value is a PropertyBusinessObject it will
    /// be converted to a Map
    ///
    /// #### Returns
    ///
    /// a map
    public Map<T, Object> asExplodedMap() {
        Map<T, Object> m = new LinkedHashMap<T, Object>();
        for (Map.Entry<T, J> entry : value.entrySet()) {
            T k = entry.getKey();
            J v = entry.getValue();
            if (v instanceof PropertyBusinessObject) {
                m.put(k, ((PropertyBusinessObject) v).getPropertyIndex().toMapRepresentation());
            } else {
                m.put(k, v);
            }
        }
        internalGet();
        return m;
    }

    /// Sets the entire content of the property
    ///
    /// #### Parameters
    ///
    /// - `t`: the map of elements to set
    ///
    /// #### Returns
    ///
    /// the parent object for chaining
    public K setMap(Map<T, J> t) {
        value.clear();
        value.putAll(t);
        firePropertyChanged();
        internalSet();
        return (K) parent.parent;
    }

    /// Remove all the elements from the map
    public void clear() {
        internalSet();
        value.clear();
    }
}
