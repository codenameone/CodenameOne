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

import com.codename1.io.Storage;
import com.codename1.io.Util;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/// Product photos kept on the device, so a register that lost the network
/// (or a shopper on a train) still shows the pictures it has seen.
///
/// Each photo is one storage entry keyed by its photo id, as downloaded. An
/// index entry lists them oldest-used first with their sizes; past the byte
/// budget the oldest go. A photo id never changes its picture (a new photo
/// is a new id), so nothing here ever needs to be revalidated.
public final class PhotoCache {
    /// Where the bytes live: [Storage] on a device, a map in the tests.
    public interface Store {
        /// @return the bytes of `name`, or null when there are none
        byte[] read(String name);

        void write(String name, byte[] data);

        void delete(String name);
    }

    /// Twelve megabytes: a few hundred catalog photos at register size.
    public static final long DEFAULT_BUDGET = 12L * 1024 * 1024;
    private static final String PREFIX = "photo-";
    private static final String INDEX = "photo-cache-index";

    private static PhotoCache shared;

    private final Store store;
    private final long budget;
    /// Photo ids, least recently used first, and their sizes in bytes.
    private final List<String> ids = new ArrayList<String>();
    private final List<Long> sizes = new ArrayList<Long>();
    private long total;

    public PhotoCache(Store store, long budget) {
        this.store = store;
        this.budget = budget;
        load();
    }

    /// The app's cache, over [Storage].
    public static PhotoCache shared() {
        if (shared == null) {
            shared = new PhotoCache(new StorageStore(), DEFAULT_BUDGET);
        }
        return shared;
    }

    /// The photo `id` as it was downloaded, or null when it is not here.
    public byte[] get(String id) {
        int at = ids.indexOf(id);
        if (at < 0) {
            return null;
        }
        byte[] data = store.read(PREFIX + safe(id));
        if (data == null) {
            // The entry went missing behind our back: forget it.
            total -= sizes.remove(at).longValue();
            ids.remove(at);
            save();
            return null;
        }
        if (at != ids.size() - 1) {
            ids.add(ids.remove(at));
            sizes.add(sizes.remove(at));
            save();
        }
        return data;
    }

    /// Keeps `data` as the photo `id`, dropping the least recently used
    /// photos until the cache is within its budget. A photo larger than the
    /// whole budget is not kept.
    public void put(String id, byte[] data) {
        if (id == null || id.length() == 0 || data == null || data.length > budget) {
            return;
        }
        int at = ids.indexOf(id);
        if (at >= 0) {
            total -= sizes.remove(at).longValue();
            ids.remove(at);
        }
        store.write(PREFIX + safe(id), data);
        ids.add(id);
        sizes.add(Long.valueOf(data.length));
        total += data.length;
        while (total > budget && ids.size() > 1) {
            String old = ids.remove(0);
            total -= sizes.remove(0).longValue();
            store.delete(PREFIX + safe(old));
        }
        save();
    }

    public boolean contains(String id) {
        return ids.contains(id);
    }

    /// Bytes held.
    public long size() {
        return total;
    }

    public int count() {
        return ids.size();
    }

    /// An id as a storage name: letters, digits, `-` and `_` only.
    static String safe(String id) {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < id.length(); iter++) {
            char c = id.charAt(iter);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '-' || c == '_';
            b.append(ok ? c : '_');
        }
        return b.toString();
    }

    private void load() {
        byte[] raw = store.read(INDEX);
        if (raw == null) {
            return;
        }
        String text;
        try {
            text = new String(raw, "UTF-8");
        } catch (IOException bad) {
            return;
        }
        int from = 0;
        while (from < text.length()) {
            int end = text.indexOf('\n', from);
            if (end < 0) {
                end = text.length();
            }
            String line = text.substring(from, end);
            from = end + 1;
            int space = line.indexOf(' ');
            if (space <= 0) {
                continue;
            }
            long n;
            try {
                n = Long.parseLong(line.substring(space + 1));
            } catch (NumberFormatException bad) {
                continue;
            }
            String id = line.substring(0, space);
            if (!ids.contains(id)) {
                ids.add(id);
                sizes.add(Long.valueOf(n));
                total += n;
            }
        }
    }

    private void save() {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < ids.size(); iter++) {
            b.append(ids.get(iter)).append(' ').append(sizes.get(iter).longValue()).append('\n');
        }
        try {
            store.write(INDEX, b.toString().getBytes("UTF-8"));
        } catch (IOException bad) {
            // UTF-8 is always there.
        }
    }

    /// [Storage], one entry per photo.
    private static final class StorageStore implements Store {
        @Override
        public byte[] read(String name) {
            Storage s = Storage.getInstance();
            if (!s.exists(name)) {
                return null;
            }
            InputStream in = null;
            try {
                in = s.createInputStream(name);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Util.copy(in, out);
                return out.toByteArray();
            } catch (IOException failed) {
                return null;
            } finally {
                Util.cleanup(in);
            }
        }

        @Override
        public void write(String name, byte[] data) {
            OutputStream out = null;
            try {
                out = Storage.getInstance().createOutputStream(name);
                out.write(data);
            } catch (IOException failed) {
                // A full device keeps working, only without this photo.
            } finally {
                Util.cleanup(out);
            }
        }

        @Override
        public void delete(String name) {
            Storage.getInstance().deleteStorageFile(name);
        }
    }
}
