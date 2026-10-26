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
package com.codename1.desktopcompat.rt;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

/// The elements of a list as the `Enumeration` the older JDK interfaces
/// answer, in the list's order or from its end to its start. The elements
/// are copied when it is made, so a list that changes afterwards does not
/// disturb a walk already under way.
public final class ListEnumeration<T> implements Enumeration<T> {

    private final ArrayList<T> list;
    private final boolean backwards;
    private int done;

    public ListEnumeration(List<? extends T> list, boolean backwards) {
        this.list = new ArrayList<T>(list);
        this.backwards = backwards;
    }

    @Override
    public boolean hasMoreElements() {
        return done < list.size();
    }

    @Override
    public T nextElement() {
        if (done >= list.size()) {
            throw new NoSuchElementException("No more elements");
        }
        int at = backwards ? list.size() - 1 - done : done;
        done++;
        return list.get(at);
    }
}
