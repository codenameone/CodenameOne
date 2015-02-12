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

package com.codename1.ui.util;

import com.codename1.ui.Display;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Helper weak hash map substitute 
 * @author Shai Almog
 */
public class WeakHashMap<K, V> implements Map<K, V> {
    private final HashMap<K, Object> map = new HashMap<K, Object>();

    /**
     * @inheritDoc
     */
    @Override
    public int size() {
        return map.size();
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    /**
     * This method is unsupported in the weak hash map
     */
    @Override
    public boolean containsValue(Object value) {
        throw new RuntimeException();
    }

    /**
     * @inheritDoc
     */
    @Override
    public V get(Object key) {
        Object o = map.get(key);
        if(o == null) {
            return null;
        }
        return (V) Display.getInstance().extractHardRef(o);
    }

    /**
     * @inheritDoc
     */
    @Override
    public V put(K key, V value) {
        map.put(key, Display.getInstance().createSoftWeakRef(value));
        return value;
    }

    /**
     * @inheritDoc
     */
    @Override
    public V remove(Object key) {
        Object o = map.remove(key);
        if(o == null) {
            return null;
        }
        return (V) Display.getInstance().extractHardRef(o);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for(Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public void clear() {
        map.clear();
    }

    /**
     * @inheritDoc
     */
    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    /**
     * Unsupported operation
     */
    @Override
    public Collection<V> values() {
        throw new RuntimeException();
    }

    /**
     * Unsupported operation
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new RuntimeException();
    }    
}
