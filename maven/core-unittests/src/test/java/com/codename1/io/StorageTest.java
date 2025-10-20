package com.codename1.io;

import com.codename1.impl.CodenameOneImplementation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorageTest {
    private CodenameOneImplementation implementation;
    private Storage storage;

    @BeforeEach
    void setUp() {
        implementation = TestImplementationProvider.installImplementation(true);
        storage = Storage.getInstance();
        storage.clearStorage();
        storage.clearCache();
        storage.setNormalizeNames(true);
    }

    @Test
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

    @Test
    void createInputStreamThrowsWhenEntryMissing() {
        assertThrows(IOException.class, () -> storage.createInputStream("missing"));
    }

    @Test
    void clearStoragePurgesEntriesAndCache() {
        storage.writeObject("transient", "value");
        assertNotNull(storage.readObject("transient"));

        storage.clearStorage();

        assertFalse(storage.exists("transient"));
        assertNull(storage.readObject("transient"));
    }

    @Test
    void normalizedNamesAreUsedByDefault() {
        String originalKey = "dir/with:illegal*chars";
        storage.writeObject(originalKey, "data");

        assertTrue(storage.exists(originalKey));
        assertTrue(Arrays.asList(storage.listEntries()).contains("dir_with_illegal_chars"));
    }

    @Test
    void disablingNormalizationUsesRawKey() {
        storage.setNormalizeNames(false);
        String rawKey = "raw/name=kept";
        storage.writeObject(rawKey, "v");

        assertTrue(Arrays.asList(storage.listEntries()).contains(rawKey));
    }

    @Test
    void flushStorageCacheDelegatesToImplementation() {
        storage.flushStorageCache();
        verify(implementation).flushStorageCache();
    }

    @Test
    void entrySizeReflectsStoredObjectSize() {
        String key = "sized";
        storage.writeObject(key, "payload");
        int size = storage.entrySize(key);
        assertTrue(size > 0);
    }

    @Test
    void deleteStorageFileRemovesEntryAndCache() {
        String key = "toDelete";
        storage.writeObject(key, "data");
        assertNotNull(storage.readObject(key));

        storage.deleteStorageFile(key);

        assertFalse(storage.exists(key));
        assertNull(storage.readObject(key));
    }

    @Test
    void existsDelegatesToImplementationWithNormalization() {
        String key = "needs?normalization";
        when(implementation.storageFileExists("needs_normalization")).thenReturn(true);
        assertTrue(storage.exists(key));
    }
}
