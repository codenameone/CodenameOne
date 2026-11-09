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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/// The list a record's to-many relationship holds: read from the session the
/// first time anything touches it, and changed in place like any list.
///
/// Internal ORM runtime; not an application API.
/// @hidden
@com.codename1.impl.SharedWithBackend
public final class LazyList<E> extends AbstractList<E> {
    final Link link = new Link();
    private final List<E> items = new ArrayList<E>();

    LazyList() {
    }

    void fill(Collection<? extends E> values) {
        if (values != this) { //NOPMD CompareObjectsWithEquals - identity is the point
            List<E> copy = values == null ? new ArrayList<E>() : new ArrayList<E>(values);
            items.clear();
            items.addAll(copy);
        }
        link.loaded = true;
    }

    @Override
    public E get(int index) {
        link.load();
        return items.get(index);
    }

    @Override
    public int size() {
        link.load();
        return items.size();
    }

    @Override
    public E set(int index, E element) {
        link.load();
        return items.set(index, element);
    }

    @Override
    public void add(int index, E element) {
        link.load();
        items.add(index, element);
    }

    @Override
    public E remove(int index) {
        link.load();
        return items.remove(index);
    }
}
