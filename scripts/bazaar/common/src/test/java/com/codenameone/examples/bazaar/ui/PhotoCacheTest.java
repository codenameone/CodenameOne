/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/// The offline photo cache's rules, over a map instead of the device's storage.
class PhotoCacheTest {
    private static final class MapStore implements PhotoCache.Store {
        final Map<String, byte[]> entries = new HashMap<String, byte[]>();

        @Override
        public byte[] read(String name) {
            return entries.get(name);
        }

        @Override
        public void write(String name, byte[] data) {
            entries.put(name, data);
        }

        @Override
        public void delete(String name) {
            entries.remove(name);
        }
    }

    @Test
    void keepsPhotosByIdAcrossARestart() {
        MapStore store = new MapStore();
        PhotoCache c = new PhotoCache(store, 100);
        c.put("ph-1", new byte[] {1, 2, 3});
        PhotoCache after = new PhotoCache(store, 100);
        assertArrayEquals(new byte[] {1, 2, 3}, after.get("ph-1"));
        assertEquals(3, after.size());
        assertNull(after.get("ph-2"));
    }

    @Test
    void dropsTheLeastRecentlyUsedPastTheBudget() {
        MapStore store = new MapStore();
        PhotoCache c = new PhotoCache(store, 100);
        c.put("a", new byte[40]);
        c.put("b", new byte[40]);
        c.get("a");
        c.put("c", new byte[40]);
        assertTrue(c.contains("a"));
        assertFalse(c.contains("b"));
        assertTrue(c.contains("c"));
        assertEquals(80, c.size());
        assertNull(store.read("photo-b"));
        c.put("huge", new byte[101]);
        assertFalse(c.contains("huge"));
    }

    @Test
    void forgetsAnEntryThatVanishedAndNamesAreSafe() {
        MapStore store = new MapStore();
        PhotoCache c = new PhotoCache(store, 100);
        c.put("x/y:z", new byte[5]);
        assertTrue(store.entries.containsKey("photo-x_y_z"));
        store.entries.remove("photo-x_y_z");
        assertNull(c.get("x/y:z"));
        assertEquals(0, c.count());
    }
}
