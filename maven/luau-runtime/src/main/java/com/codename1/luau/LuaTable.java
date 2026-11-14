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
package com.codename1.luau;

import java.util.HashMap;

/// A Luau table: an array part for the keys 1..n and an insertion-ordered
/// hash part for everything else, so `next` walks in a stable order.
public final class LuaTable {
    private Object[] arr = new Object[0];
    private int n;
    private Object[] hkeys = new Object[0];
    private Object[] hvals = new Object[0];
    private int hcount;
    private int hlive;
    private HashMap<Object, Integer> index;
    private LuaTable metatable;
    private boolean frozen;

    /// An empty table.
    public LuaTable() {
    }

    /// An empty table sized for `narr` array entries.
    public LuaTable(int narr) {
        arr = new Object[Math.max(0, narr)];
    }

    /// The metatable, or null.
    public LuaTable getMetatable() {
        return metatable;
    }

    /// Replaces the metatable.
    public void setMetatable(LuaTable mt) {
        metatable = mt;
    }

    /// True after `table.freeze`.
    public boolean isFrozen() {
        return frozen;
    }

    /// Makes the table read only.
    public void freeze() {
        frozen = true;
    }

    static Object normKey(Object k) {
        if (k instanceof Double) {
            double d = ((Double) k).doubleValue();
            if (d == 0) {
                return Interp.ZERO;
            }
        }
        return k;
    }

    private static int arrayIndex(Object k) {
        if (k instanceof Double) {
            double d = ((Double) k).doubleValue();
            int i = (int) d;
            if (i == d && i >= 1) {
                return i;
            }
        }
        return 0;
    }

    /// Raw read, no metamethods.
    public Object rawget(Object k) {
        int i = arrayIndex(k);
        if (i > 0 && i <= n) {
            return arr[i - 1];
        }
        if (k == null || hlive == 0) {
            return null;
        }
        Integer slot = index.get(normKey(k));
        return slot == null ? null : hvals[slot.intValue()];
    }

    /// Raw read of an integer key.
    public Object rawgeti(int i) {
        if (i >= 1 && i <= n) {
            return arr[i - 1];
        }
        return rawget(Interp.num(i));
    }

    /// Raw read of a string key.
    public Object rawget(String k) {
        if (hlive == 0) {
            return null;
        }
        Integer slot = index.get(k);
        return slot == null ? null : hvals[slot.intValue()];
    }

    /// Raw write, no metamethods. Errors on a nil or NaN key.
    public void rawset(Object k, Object v) {
        if (frozen) {
            throw LuaError.rt("attempt to modify a readonly table");
        }
        int i = arrayIndex(k);
        if (i > 0) {
            if (i <= n) {
                arr[i - 1] = v;
                if (v == null && i == n) {
                    shrink();
                }
                return;
            }
            if (i == n + 1) {
                if (v == null) {
                    hashSet(normKey(k), null);
                    return;
                }
                if (n == arr.length) {
                    Object[] na = new Object[Math.max(4, n * 2)];
                    System.arraycopy(arr, 0, na, 0, n);
                    arr = na;
                }
                arr[n++] = v;
                if (hlive > 0) {
                    hashSet(normKey(k), null);
                    migrate();
                }
                return;
            }
        }
        if (k == null) {
            throw LuaError.rt("table index is nil");
        }
        if (k instanceof Double && ((Double) k).isNaN()) {
            throw LuaError.rt("table index is NaN");
        }
        hashSet(normKey(k), v);
    }

    /// Raw write of an integer key.
    public void rawseti(int i, Object v) {
        rawset(Interp.num(i), v);
    }

    private void shrink() {
        while (n > 0 && arr[n - 1] == null) {
            n--;
        }
    }

    private void migrate() {
        while (hlive > 0) {
            Object key = Interp.num(n + 1);
            Integer slot = index.get(key);
            if (slot == null || hvals[slot.intValue()] == null) {
                return;
            }
            Object v = hvals[slot.intValue()];
            hvals[slot.intValue()] = null;
            hlive--;
            if (n == arr.length) {
                Object[] na = new Object[Math.max(4, n * 2)];
                System.arraycopy(arr, 0, na, 0, n);
                arr = na;
            }
            arr[n++] = v;
        }
    }

    private void hashSet(Object k, Object v) {
        if (index == null) {
            if (v == null) {
                return;
            }
            index = new HashMap<Object, Integer>();
        }
        Integer slot = index.get(k);
        if (slot != null) {
            int s = slot.intValue();
            if (hvals[s] == null && v != null) {
                hlive++;
            } else if (hvals[s] != null && v == null) {
                hlive--;
            }
            hvals[s] = v;
            return;
        }
        if (v == null) {
            return;
        }
        if (hcount == hkeys.length) {
            if (hcount > 8 && hlive < hcount / 2) {
                compact();
            }
            if (hcount == hkeys.length) {
                int cap = Math.max(4, hkeys.length * 2);
                Object[] nk = new Object[cap];
                Object[] nv = new Object[cap];
                System.arraycopy(hkeys, 0, nk, 0, hcount);
                System.arraycopy(hvals, 0, nv, 0, hcount);
                hkeys = nk;
                hvals = nv;
            }
        }
        hkeys[hcount] = k;
        hvals[hcount] = v;
        index.put(k, Integer.valueOf(hcount));
        hcount++;
        hlive++;
    }

    private void compact() {
        int j = 0;
        index.clear();
        for (int i = 0; i < hcount; i++) {
            if (hvals[i] != null) {
                hkeys[j] = hkeys[i];
                hvals[j] = hvals[i];
                index.put(hkeys[j], Integer.valueOf(j));
                j++;
            }
        }
        for (int i = j; i < hcount; i++) {
            hkeys[i] = null;
            hvals[i] = null;
        }
        hcount = j;
    }

    /// The border `#t` reports.
    public int length() {
        if (n > 0) {
            return n;
        }
        return 0;
    }

    /// Number of entries in the array part, which `ipairs` walks.
    int arraySize() {
        return n;
    }

    /// The key after `k` in traversal order, written to `out[0]`/`out[1]`;
    /// false at the end. Errors on a key not in the table.
    public boolean next(Object k, Object[] out) {
        int pos;
        if (k == null) {
            pos = 0;
        } else {
            int i = arrayIndex(k);
            if (i > 0 && i <= n) {
                pos = i;
            } else {
                Integer slot = index == null ? null : index.get(normKey(k));
                if (slot == null) {
                    throw LuaError.rt("invalid key to 'next'");
                }
                pos = n + slot.intValue() + 1;
            }
        }
        for (; pos < n; pos++) {
            if (arr[pos] != null) {
                out[0] = Interp.num(pos + 1);
                out[1] = arr[pos];
                return true;
            }
        }
        for (int h = pos - n; h < hcount; h++) {
            if (hvals[h] != null) {
                out[0] = hkeys[h];
                out[1] = hvals[h];
                return true;
            }
        }
        return false;
    }

    /// Inserts at `pos` (1-based), shifting the array part up.
    public void insert(int pos, Object v) {
        if (frozen) {
            throw LuaError.rt("attempt to modify a readonly table");
        }
        int len = length();
        if (pos < 1 || pos > len + 1) {
            throw LuaError.rt("bad argument #2 to 'insert' (position out of bounds)");
        }
        for (int i = len; i >= pos; i--) {
            rawseti(i + 1, rawgeti(i));
        }
        rawseti(pos, v);
    }

    /// Removes the element at `pos`, shifting down, and returns it.
    public Object remove(int pos) {
        if (frozen) {
            throw LuaError.rt("attempt to modify a readonly table");
        }
        int len = length();
        if (len == 0 && (pos == 0 || pos == len)) {
            return rawgeti(pos);
        }
        if (pos < 1 || pos > len + 1) {
            throw LuaError.rt("bad argument #2 to 'remove' (position out of bounds)");
        }
        if (pos == len + 1) {
            Object v = rawgeti(pos);
            rawseti(pos, null);
            return v;
        }
        Object v = arr[pos - 1];
        System.arraycopy(arr, pos, arr, pos - 1, len - pos);
        arr[len - 1] = null;
        n = len - 1;
        shrink();
        return v;
    }

    /// Removes every entry; the metatable stays.
    public void clear() {
        if (frozen) {
            throw LuaError.rt("attempt to modify a readonly table");
        }
        arr = new Object[0];
        n = 0;
        hkeys = new Object[0];
        hvals = new Object[0];
        hcount = 0;
        hlive = 0;
        index = null;
    }

    Object[] arrayPart() {
        return arr;
    }

    /// A shallow copy without the metatable or the frozen flag.
    public LuaTable copy() {
        LuaTable t = new LuaTable(n);
        System.arraycopy(arr, 0, t.arr, 0, n);
        t.n = n;
        for (int i = 0; i < hcount; i++) {
            if (hvals[i] != null) {
                t.hashSet(hkeys[i], hvals[i]);
            }
        }
        return t;
    }

    @Override
    public String toString() {
        return "table: 0x" + Integer.toHexString(System.identityHashCode(this));
    }
}
