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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/// The offline photo cache's rules, over a map instead of the device's storage.
class PhotoCacheTest {
    private static final class MapStore implements PhotoCache.Store {
        final Map<String, byte[]> entries = new HashMap<String, byte[]>();
        int indexWrites;

        @Override
        public byte[] read(String name) {
            return entries.get(name);
        }

        @Override
        public void write(String name, byte[] data) {
            if ("photo-cache-index".equals(name)) {
                indexWrites++;
            }
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
        assertNull(store.read("photo-" + PhotoCache.safe("b")));
        c.put("huge", new byte[101]);
        assertFalse(c.contains("huge"));
    }

    @Test
    void forgetsAnEntryThatVanishedAndNamesAreSafe() {
        MapStore store = new MapStore();
        PhotoCache c = new PhotoCache(store, 100);
        c.put("x/y:z", new byte[5]);
        assertTrue(store.entries.containsKey("photo-782f793a7a"));
        store.entries.remove("photo-782f793a7a");
        assertNull(c.get("x/y:z"));
        assertEquals(0, c.count());
    }

    @Test
    void idsThatLookAlikeNeverShareAName() {
        assertEquals("612e62", PhotoCache.safe("a.b"));
        assertEquals("615f62", PhotoCache.safe("a_b"));
        MapStore store = new MapStore();
        PhotoCache c = new PhotoCache(store, 100);
        c.put("a.b", new byte[] {1});
        c.put("a_b", new byte[] {2});
        assertArrayEquals(new byte[] {1}, c.get("a.b"));
        assertArrayEquals(new byte[] {2}, c.get("a_b"));
    }

    @Test
    void hitsWriteTheIndexOnceAndLater() {
        MapStore store = new MapStore();
        final List<Runnable> queued = new ArrayList<Runnable>();
        PhotoCache c = new PhotoCache(store, 100, queued::add);
        c.put("a", new byte[10]);
        c.put("b", new byte[10]);
        c.put("c", new byte[10]);
        int afterPuts = store.indexWrites;
        for (int iter = 0; iter < 20; iter++) {
            c.get("a");
            c.get("b");
        }
        assertEquals(afterPuts, store.indexWrites, "a hit never writes on the spot");
        assertEquals(1, queued.size(), "one deferred write, however many hits");
        queued.get(0).run();
        assertEquals(afterPuts + 1, store.indexWrites);
        c.get("a");
        assertEquals(2, queued.size(), "a later hit queues again");
        c.flush();
        int flushed = store.indexWrites;
        queued.get(1).run();
        assertEquals(flushed, store.indexWrites, "nothing left to write");
    }

    @Test
    void aPendingIndexIsWrittenByTheNextPut() {
        MapStore store = new MapStore();
        PhotoCache c = new PhotoCache(store, 30);
        c.put("a", new byte[10]);
        c.put("b", new byte[10]);
        c.put("c", new byte[10]);
        c.get("a");
        c.put("d", new byte[10]);
        PhotoCache after = new PhotoCache(store, 30);
        assertTrue(after.contains("a"), "the hit's order was saved with the put");
        assertFalse(after.contains("b"), "the least recently used went");
    }
}
