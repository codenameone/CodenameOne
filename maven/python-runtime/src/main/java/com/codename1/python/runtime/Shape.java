/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.python.runtime;

/// The layout of an instance's attributes: which name lives in which slot.
///
/// Instances of a class that assign the same attributes in the same order
/// share one shape, so an attribute is an array element at an index every
/// one of them agrees on. A shape is immutable; adding an attribute moves the
/// instance to the shape that extends its own by that name, and the
/// transitions are remembered so the next instance reaches the very same
/// object. That identity is what an `AttrSite` compares.
final class Shape {
    static final Shape EMPTY = new Shape(new String[0]);

    final String[] names;
    private Shape[] next = new Shape[0];

    private Shape(String[] names) {
        this.names = names;
    }

    int size() {
        return names.length;
    }

    /// The slot holding `name`, or -1.
    int indexOf(String name) {
        String[] n = names;
        for (int i = 0; i < n.length; i++) {
            if (n[i] == name) { //NOPMD CompareObjectsWithEquals - identity is meant
                return i;
            }
        }
        for (int i = 0; i < n.length; i++) {
            if (n[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /// This shape extended by `name`.
    Shape with(String name) {
        Shape[] t = next;
        for (Shape e : t) {
            String last = e.names[names.length];
            if (last == name || last.equals(name)) { //NOPMD CompareObjectsWithEquals - identity is meant
                return e;
            }
        }
        String[] n = new String[names.length + 1];
        System.arraycopy(names, 0, n, 0, names.length);
        n[names.length] = name;
        Shape s = new Shape(n);
        Shape[] grown = new Shape[t.length + 1];
        System.arraycopy(t, 0, grown, 0, t.length);
        grown[t.length] = s;
        next = grown;
        return s;
    }
}
