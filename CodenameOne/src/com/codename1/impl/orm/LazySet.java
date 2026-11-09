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
package com.codename1.impl.orm;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/// The set a record's to-many relationship holds: read from the session the
/// first time anything touches it, and changed in place like any set.
///
/// Internal ORM runtime; not an application API.
/// @hidden
@com.codename1.impl.SharedWithBackend
public final class LazySet<E> extends AbstractSet<E> {
    final Link link = new Link();
    private final Set<E> items = new LinkedHashSet<E>();

    LazySet() {
    }

    void fill(Collection<? extends E> values) {
        if (values != this) { //NOPMD CompareObjectsWithEquals - identity is the point
            Set<E> copy = values == null ? new LinkedHashSet<E>() : new LinkedHashSet<E>(values);
            items.clear();
            items.addAll(copy);
        }
        link.loaded = true;
    }

    @Override
    public Iterator<E> iterator() {
        link.load();
        return items.iterator();
    }

    @Override
    public int size() {
        link.load();
        return items.size();
    }

    @Override
    public boolean add(E element) {
        link.load();
        return items.add(element);
    }

    @Override
    public boolean contains(Object element) {
        link.load();
        return items.contains(element);
    }

    @Override
    public boolean remove(Object element) {
        link.load();
        return items.remove(element);
    }
}
