package com.codename1.components;

import com.codename1.io.Storage;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestUtils;
import com.codename1.ui.Image;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StorageImageAsyncTest extends UITestBase {
    private Storage originalStorage;

    @BeforeEach
    void configureMocks() {
        originalStorage = Storage.getInstance();
    }

    @AfterEach
    void restoreStorage() {
        Storage.setStorageInstance(originalStorage);
    }

    @FormTest
    void testGetInternalReturnsPlaceholderUntilDataLoaded() {
        InMemoryStorage storage = new InMemoryStorage();
        byte[] encoded = new byte[]{1, 2, 3};
        storage.put("test", encoded);
        Storage.setStorageInstance(storage);

        TestImage placeholder = new TestImage(10, 10);
        StorageImageAsync image = StorageImageAsync.create("test", placeholder);
        Image internal = image.getInternal();
        assertSame(placeholder, internal, "Placeholder should be returned before data is loaded");
    }

    @FormTest
    void testBackgroundLoadPopulatesImageData() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        byte[] encoded = new byte[]{10, 20, 30, 40};
        storage.put("async", encoded);
        Storage.setStorageInstance(storage);

        TestImage placeholder = new TestImage(8, 8);
        StorageImageAsync image = StorageImageAsync.create("async", placeholder);
        image.getInternal();

        while(image.getInternal() == placeholder) {
            TestUtils.waitFor(10);
        }

        assertArrayEquals(encoded, image.getImageData());

        Image loaded = image.getInternal();
        assertNotSame(placeholder, loaded, "Loaded image should replace placeholder once data is available");
        assertTrue(isImageCreated(image));
    }

    @FormTest
    void testAnimateLifecycle() throws Exception {
        TestImage placeholder = new TestImage(5, 5);
        StorageImageAsync image = StorageImageAsync.create("ignored", placeholder);

        setBooleanField(image, "changePending", true);
        setBooleanField(image, "imageCreated", false);
        assertTrue(image.animate(), "changePending should cause animate to return true");
        assertTrue(getBooleanField(image, "changePending"));

        setBooleanField(image, "imageCreated", true);
        assertTrue(image.animate());
        assertFalse(getBooleanField(image, "changePending"));
    }

    @FormTest
    void testIsAnimationAlwaysTrue() {
        TestImage placeholder = new TestImage(6, 6);
        StorageImageAsync image = StorageImageAsync.create("anything", placeholder);
        assertTrue(image.isAnimation());
    }

    private boolean isImageCreated(StorageImageAsync image) throws Exception {
        Field field = StorageImageAsync.class.getDeclaredField("imageCreated");
        field.setAccessible(true);
        return field.getBoolean(image);
    }

    private void setBooleanField(StorageImageAsync image, String name, boolean value) throws Exception {
        Field field = StorageImageAsync.class.getDeclaredField(name);
        field.setAccessible(true);
        field.setBoolean(image, value);
    }

    private boolean getBooleanField(StorageImageAsync image, String name) throws Exception {
        Field field = StorageImageAsync.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getBoolean(image);
    }

    private static class InMemoryStorage extends Storage {
        private final Map<String, Object> values = new HashMap<>();

        void put(String name, Object value) {
            values.put(name, value);
        }

        @Override
        public Object readObject(String name) {
            return values.get(name);
        }
    }

    private static class TestImage extends Image {
        private final int width;
        private final int height;

        TestImage(int width, int height) {
            super(new Object());
            this.width = width;
            this.height = height;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public Image scaled(int width, int height) {
            return new TestImage(width, height);
        }

        @Override
        public Image scaledSmallerRatio(int width, int height) {
            return new TestImage(width, height);
        }

        @Override
        public boolean animate() {
            return false;
        }
    }
}
