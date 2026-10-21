/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.unitycompat.system.collections.generic;

/// `System.Collections.Generic.IDictionary<TKey, TValue>`, as far as a
/// script reaches a dictionary through it: the members below, the count
/// of [ICollection_1], and the constructor of [Dictionary_2] that copies
/// one. `Keys` and `Values` are read from the dictionary itself.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public interface IDictionary_2 extends ICollection_1 {
    void Add(Object key, Object value);

    boolean ContainsKey(Object key);

    Object get_Item(Object key);

    void set_Item(Object key, Object value);

    /// `out TValue` is an array and an index.
    boolean TryGetValue(Object key, Object[] value, int at);

    boolean Remove(Object key);
}
