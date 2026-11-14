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

/// The inline cache of one `d["key"]` read with a str literal key: the
/// layout ([PyDict.Layout]) last seen there and the slot of the key in it.
/// A dict on that layout answers with one identity compare and an array
/// load; anything else takes [Py#getitemS].
public final class KeySite {
    private final String key;
    private PyDict.Layout layout;
    private int slot;

    public KeySite(String key) {
        this.key = key.intern();
    }

    /// One site per name of `names`, joined by newlines.
    public static KeySite[] table(String names) {
        String[] each = Py.names(names);
        KeySite[] out = new KeySite[each.length];
        for (int i = 0; i < each.length; i++) {
            out[i] = new KeySite(each[i]);
        }
        return out;
    }

    public Object get(Object o) {
        if (o instanceof PyDict) {
            PyDict d = (PyDict) o;
            PyDict.Layout l = d.layout;
            if (l != null) {
                if (l == layout) { //NOPMD CompareObjectsWithEquals - identity is meant
                    return d.values[slot];
                }
                return miss(d, l);
            }
        }
        return Py.getitemS(o, key);
    }

    private Object miss(PyDict d, PyDict.Layout l) {
        int e = l.slotOf(key);
        if (e < 0 || d.type() != Py.DICT_TYPE) { //NOPMD CompareObjectsWithEquals - identity is meant
            return Py.getitemS(d, key);
        }
        layout = l;
        slot = e;
        return d.values[e];
    }
}
