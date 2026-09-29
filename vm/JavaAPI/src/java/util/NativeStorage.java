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
package java.util;

/** VM-private buffers. Owners register their fields with the translator's storage descriptor. */
final class NativeStorage {
    private NativeStorage() { }

    static long references(int capacity) {
        if (capacity < 0) throw new OutOfMemoryError();
        long block = allocateReferences(capacity);
        if (capacity != 0 && block == 0) throw new OutOfMemoryError();
        return block;
    }

    static long integers(int capacity) {
        if (capacity < 0) throw new OutOfMemoryError();
        long block = allocateIntegers(capacity);
        if (capacity != 0 && block == 0) throw new OutOfMemoryError();
        return block;
    }

    static long table(int capacity, boolean ordered) {
        if (capacity < 0) throw new OutOfMemoryError();
        long block = allocateTable(capacity, ordered);
        if (capacity != 0 && block == 0) throw new OutOfMemoryError();
        return block;
    }

    private static native long allocateTable(int capacity, boolean ordered);
    // Parts 1..4 borrow storage from part 0; never free or retire them separately.
    static native long part(long table, int part);
    private static native long allocateReferences(int capacity);
    private static native long allocateIntegers(int capacity);
    static native int capacity(long block);
    static native void free(long block);
    static native void retire(long block);
    static native <T> T get(long block, int index);
    static native void set(long block, int index, Object value);
    // The store every collection makes. `owner` is the object whose mark function traces
    // `block`: the barrier treats the store as a field store into the owner, so the
    // generational collector remembers the OWNER, and only when it is old. The
    // owner-less set() above has to remember the block itself, whatever its owner's age,
    // which made every young collection a remembered-set root of the next minor.
    static native void setOwned(Object owner, long block, int index, Object value);
    static native int getInt(long block, int index);
    static native void setInt(long block, int index, int value);
    // Rebuild fresh hash buffers, optionally following and rebuilding an ordering chain.
    // Returns the new tail; the first stored hash determines the new head.
    static native int rehash(long keys, long values, long metadata, long links, int head,
            long newKeys, long newValues, long newMetadata, long newPrev, long newNext);
    static native int nextOccupied(long block, int from, int capacity);
    static native void clearMap(long keys, long values, long metadata, int capacity);
    static native void move(long block, int from, int to, int count);
    static native void clear(long block, int from, int count);
    // Fresh destination only: all copied references remain rooted by the old owner.
    static native void copy(long source, int from, long destination, int to, int count);
}
