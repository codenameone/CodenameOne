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
import java.util.Arrays;
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

        // Object is not one of the supported types, so Util.writeObject throws and
        // the entry is removed. The cached copy has to go with it, otherwise reads
        // keep answering from memory until the app is restarted.
        assertFalse(storage.writeObject(key, new Object(), false));

        assertFalse(storage.exists(key));
        assertNull(storage.readObject(key));
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
