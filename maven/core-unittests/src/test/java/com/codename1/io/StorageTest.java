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

package com.codename1.io;

import com.codename1.junit.EdtTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

class StorageTest extends UITestBase {
    private Storage storage;

    @BeforeEach
    void setUp() {
        Storage.setStorageInstance(null);
        storage = Storage.getInstance();
        storage.clearStorage();
        storage.clearCache();
        storage.setNormalizeNames(true);
        implementation.resetFlushStorageCacheInvocations();
    }

    @EdtTest
    void writeObjectCachesAndPersistsEntries() {
        Vector<String> payload = new Vector<String>();
        payload.add("alpha");
        payload.add("beta");

        assertTrue(storage.writeObject("vectorEntry", payload));
        assertTrue(storage.exists("vectorEntry"));

        Object firstRead = storage.readObject("vectorEntry");
        assertEquals(payload, firstRead);
        assertSame(firstRead, storage.readObject("vectorEntry"));

        storage.clearCache();
        Object secondRead = storage.readObject("vectorEntry");
        assertEquals(payload, secondRead);
        assertNotSame(firstRead, secondRead);
    }

    @EdtTest
    void createInputStreamThrowsWhenEntryMissing() {
        assertThrows(IOException.class, () -> storage.createInputStream("missing"));
    }

    @EdtTest
    void clearStoragePurgesEntriesAndCache() {
        storage.writeObject("transient", "value");
        assertNotNull(storage.readObject("transient"));

        storage.clearStorage();

        assertFalse(storage.exists("transient"));
        assertNull(storage.readObject("transient"));
    }

    @EdtTest
    void normalizedNamesAreUsedByDefault() {
        String originalKey = "dir/with:illegal*chars";
        storage.writeObject(originalKey, "data");

        assertTrue(storage.exists(originalKey));
        assertTrue(Arrays.asList(storage.listEntries()).contains("dir_with_illegal_chars"));
    }

    @EdtTest
    void disablingNormalizationUsesRawKey() {
        storage.setNormalizeNames(false);
        String rawKey = "raw/name=kept";
        storage.writeObject(rawKey, "v");

        assertTrue(Arrays.asList(storage.listEntries()).contains(rawKey));
    }

    @EdtTest
    void flushStorageCacheDelegatesToImplementation() {
        storage.flushStorageCache();
        assertEquals(1, implementation.getFlushStorageCacheInvocations());
    }

    @EdtTest
    void entrySizeReflectsStoredObjectSize() {
        String key = "sized";
        storage.writeObject(key, "payload");
        int size = storage.entrySize(key);
        assertTrue(size > 0);
    }

    @EdtTest
    void deleteStorageFileRemovesEntryAndCache() {
        String key = "toDelete";
        storage.writeObject(key, "data");
        assertNotNull(storage.readObject(key));

        storage.deleteStorageFile(key);

        assertFalse(storage.exists(key));
        assertNull(storage.readObject(key));
    }

    @EdtTest
    void existsDelegatesToImplementationWithNormalization() {
        String key = "needs?normalization";
        implementation.putStorageEntry("needs_normalization", new byte[]{1});
        assertTrue(storage.exists(key));
    }

    @EdtTest
    void failedWriteLeavesNothingBehindInTheCache() {
        String key = "unwritable";
        assertTrue(storage.writeObject(key, "the value that is really stored"));

        // Object is not one of the supported types, so Util.writeObject throws. The
        // object is cached before the write is attempted, so it has to be dropped
        // again: otherwise reads answer from memory with a value the storage never
        // took, and the write only looks to have failed once the app is restarted.
        Object neverStored = new Object();
        assertFalse(storage.writeObject(key, neverStored, false));

        Object read = storage.readObject(key);
        assertNotSame(neverStored, read);
        assertEquals("the value that is really stored", read);
    }

    @EdtTest
    void writeObjectGoesThroughACustomStoragesOwnStreams() {
        // setStorageInstance exists so an application can wrap the bytes, seamless
        // encryption being the case the API documents. writeObject has always gone
        // through the subclass's createOutputStream, and has to keep doing so:
        // writing past the wrapper leaves bytes the matching reader cannot decode.
        final List<String> wrapped = new ArrayList<String>();
        Storage custom = new Storage() {
            @Override
            public OutputStream createOutputStream(String name) throws IOException {
                wrapped.add(name);
                return new InvertingOutputStream(super.createOutputStream(name));
            }

            @Override
            public InputStream createInputStream(String name) throws IOException {
                return new InvertingInputStream(super.createInputStream(name));
            }
        };
        Storage.setStorageInstance(custom);
        try {
            assertTrue(custom.writeObject("wrappedEntry", "the value"));
            assertTrue(wrapped.contains("wrappedEntry"), "the write bypassed the subclass");

            custom.clearCache();
            // only decodes if the write went through the wrapper too
            assertEquals("the value", custom.readObject("wrappedEntry"));
        } finally {
            Storage.setStorageInstance(null);
        }
    }

    /// Stands in for a Storage that transforms the bytes on their way out.
    private static final class InvertingOutputStream extends OutputStream {
        private final OutputStream out;

        InvertingOutputStream(OutputStream out) {
            this.out = out;
        }

        @Override
        public void write(int b) throws IOException {
            out.write((~b) & 0xff);
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            out.close();
        }
    }

    /// The matching reader, which only makes sense of what the writer above produced.
    private static final class InvertingInputStream extends InputStream {
        private final InputStream in;

        InvertingInputStream(InputStream in) {
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            int b = in.read();
            return b < 0 ? b : (~b) & 0xff;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }

    @EdtTest
    void aFailedWriteLeavesThePreviousValueInPlace() {
        String key = "keeps";
        assertTrue(storage.writeObject(key, "the value that was already stored"));
        storage.clearCache();

        // Object is not a supported type, so serialization fails partway. An
        // implementation that only replaces the entry when the write is closed never
        // touched it, so answering the failure by deleting it would throw away a good
        // value on account of a write that never reached the storage.
        assertFalse(storage.writeObject(key, new Object(), false));

        storage.clearCache();
        assertTrue(storage.exists(key));
        assertEquals("the value that was already stored", storage.readObject(key));
    }

    @EdtTest
    void writeReportsFailureWhenTheEntryCannotBePublished() {
        String key = "unpublishable";
        implementation.setStorageWriteFailsOnClose(true);
        try {
            // an implementation that replaces the entry in one step does the writing
            // as the stream closes, so that is where it can fail. Reporting success
            // for a write that never landed leaves the caller trusting a value the
            // storage does not have.
            assertFalse(storage.writeObject(key, "value"));
        } finally {
            implementation.setStorageWriteFailsOnClose(false);
        }

        assertFalse(storage.exists(key));
        assertNull(storage.readObject(key));
    }
}
