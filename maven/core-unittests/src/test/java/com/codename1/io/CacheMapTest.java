package com.codename1.io;

import com.codename1.junit.EdtTest;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Display;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

class CacheMapTest {
    private TestCodenameOneImplementation implementation;

    @BeforeEach
    void setUp() {
        implementation = new TestCodenameOneImplementation(true);
        Util.setImplementation(implementation);
        Storage.setStorageInstance(null);
    }

    @AfterEach
    void tearDown() {
        Storage.setStorageInstance(null);
        Util.setImplementation(null);
    }

    @Test
    void defaultConstructor() {
        CacheMap cache = new CacheMap();
        assertNotNull(cache);
        assertEquals(10, cache.getCacheSize());
    }

    @Test
    void constructWithPrefix() {
        CacheMap cache = new CacheMap("test_");
        assertEquals("test_", cache.getCachePrefix());
    }

    @Test
    void setAndGetCacheSize() {
        CacheMap cache = new CacheMap();
        cache.setCacheSize(20);
        assertEquals(20, cache.getCacheSize());
    }

    @Test
    void putAndGet() {
        CacheMap cache = new CacheMap();
        cache.put("key1", "value1");

        assertEquals("value1", cache.get("key1"));
    }

    @Test
    void putMultipleItems() {
        CacheMap cache = new CacheMap();
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");

        assertEquals("value1", cache.get("key1"));
        assertEquals("value2", cache.get("key2"));
        assertEquals("value3", cache.get("key3"));
    }

    @Test
    void getNonExistentKey() {
        CacheMap cache = new CacheMap();
        assertNull(cache.get("nonexistent"));
    }

    @EdtTest
    void putExceedingCacheSizeEvictsOldest() throws InterruptedException {
        CacheMap cache = new CacheMap();
        cache.setCacheSize(3);

        cache.put("key1", "value1");
        Thread.sleep(10);
        cache.put("key2", "value2");
        Thread.sleep(10);
        cache.put("key3", "value3");
        Thread.sleep(10);
        cache.put("key4", "value4");

        assertNotNull(cache.get("key2"));
        assertNotNull(cache.get("key3"));
        assertNotNull(cache.get("key4"));
    }

    @Test
    void delete() {
        CacheMap cache = new CacheMap();
        cache.put("key1", "value1");
        cache.delete("key1");

        assertNull(cache.get("key1"));
    }

    @Test
    void deleteNonExistentKey() {
        CacheMap cache = new CacheMap();
        cache.delete("nonexistent");
    }

    @Test
    void clearMemoryCache() {
        CacheMap cache = new CacheMap();
        cache.put("key1", "value1");
        cache.put("key2", "value2");

        cache.clearMemoryCache();

        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }

    @Test
    void clearAllCache() {
        CacheMap cache = new CacheMap();
        cache.put("key1", "value1");
        cache.put("key2", "value2");

        cache.clearAllCache();

        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }

    @Test
    void getKeysInCache() {
        CacheMap cache = new CacheMap();
        cache.put("key1", "value1");
        cache.put("key2", "value2");

        Vector keys = cache.getKeysInCache();

        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
    }

    @Test
    void getKeysInEmptyCache() {
        CacheMap cache = new CacheMap();
        Vector keys = cache.getKeysInCache();

        assertEquals(0, keys.size());
    }

    @Test
    void setAndGetCachePrefix() {
        CacheMap cache = new CacheMap();
        cache.setCachePrefix("prefix_");

        assertEquals("prefix_", cache.getCachePrefix());
    }

    @Test
    void setAndGetStorageCacheSize() {
        CacheMap cache = new CacheMap();
        cache.setStorageCacheSize(5);

        assertEquals(5, cache.getStorageCacheSize());
    }

    @Test
    void setStorageCacheSizeToZeroDisablesAlwaysStore() {
        CacheMap cache = new CacheMap();
        cache.setAlwaysStore(true);
        cache.setStorageCacheSize(0);

        assertFalse(cache.isAlwaysStore());
    }

    @Test
    void setAndGetAlwaysStore() {
        CacheMap cache = new CacheMap();
        cache.setAlwaysStore(true);

        assertTrue(cache.isAlwaysStore());
    }

    @Test
    void putWithStorageCache() {
        CacheMap cache = new CacheMap("storage_");
        cache.setStorageCacheSize(5);
        cache.put("key1", "value1");

        assertEquals("value1", cache.get("key1"));
    }

    @EdtTest
    void evictionToStorageCache() throws InterruptedException {
        CacheMap cache = new CacheMap("evict_");
        cache.setCacheSize(2);
        cache.setStorageCacheSize(5);

        cache.put("key1", "value1");
        Thread.sleep(10);
        cache.put("key2", "value2");
        Thread.sleep(10);
        cache.put("key3", "value3");

        assertEquals("value3", cache.get("key3"));
    }

    @Test
    void alwaysStoreWritesToStorage() {
        CacheMap cache = new CacheMap("always_");
        cache.setStorageCacheSize(5);
        cache.setAlwaysStore(true);

        cache.put("key1", "value1");

        assertEquals("value1", cache.get("key1"));
    }

    @Test
    void clearStorageCache() {
        CacheMap cache = new CacheMap("clear_");
        cache.setStorageCacheSize(5);
        cache.put("key1", "value1");

        cache.clearStorageCache();
    }

    @Test
    void clearStorageCacheWithZeroSize() {
        CacheMap cache = new CacheMap("zero_");
        cache.setStorageCacheSize(0);

        cache.clearStorageCache();
    }

    @Test
    void updateExistingKey() {
        CacheMap cache = new CacheMap();
        cache.put("key1", "value1");
        cache.put("key1", "value2");

        assertEquals("value2", cache.get("key1"));
    }

    @EdtTest
    void cachePromotionOnAccess() throws InterruptedException {
        CacheMap cache = new CacheMap();
        cache.setCacheSize(3);

        cache.put("key1", "value1");
        Thread.sleep(10);
        cache.put("key2", "value2");
        Thread.sleep(10);
        cache.put("key3", "value3");

        cache.get("key1");
        Thread.sleep(10);

        cache.put("key4", "value4");

        assertNotNull(cache.get("key1"));
    }

    @Test
    void differentPrefixesIsolateData() {
        CacheMap cache1 = new CacheMap("prefix1_");
        CacheMap cache2 = new CacheMap("prefix2_");

        cache1.put("key", "value1");
        cache2.put("key", "value2");

        assertEquals("value1", cache1.get("key"));
        assertEquals("value2", cache2.get("key"));
    }
}
