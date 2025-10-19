package com.codename1.components;

import com.codename1.io.Storage;
import com.codename1.test.UITestBase;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class StorageImageAsyncTest extends UITestBase {
    private Storage originalStorage;

    @BeforeEach
    void configureMocks() {
        when(implementation.createImage(any(byte[].class), anyInt(), anyInt())).thenReturn(new Object());
        when(implementation.createImage(any(int[].class), anyInt(), anyInt())).thenReturn(new Object());
        when(implementation.isAnimation(any())).thenReturn(false);
        when(implementation.animateImage(any(), anyLong())).thenReturn(false);
        originalStorage = Storage.getInstance();
    }

    @AfterEach
    void restoreStorage() {
        Storage.setStorageInstance(originalStorage);
    }

    @Test
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

    @Test
    void testBackgroundLoadPopulatesImageData() throws Exception {
        InMemoryStorage storage = new InMemoryStorage();
        byte[] encoded = new byte[]{10, 20, 30, 40};
        storage.put("async", encoded);
        Storage.setStorageInstance(storage);

        TestImage placeholder = new TestImage(8, 8);
        StorageImageAsync image = StorageImageAsync.create("async", placeholder);
        image.getInternal();

        waitForImageData(image);
        assertArrayEquals(encoded, image.getImageData());

        Image loaded = image.getInternal();
        assertNotSame(placeholder, loaded, "Loaded image should replace placeholder once data is available");
        assertTrue(isImageCreated(image));
    }

    @Test
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

    @Test
    void testIsAnimationAlwaysTrue() {
        TestImage placeholder = new TestImage(6, 6);
        StorageImageAsync image = StorageImageAsync.create("anything", placeholder);
        assertTrue(image.isAnimation());
    }

    private void waitForImageData(StorageImageAsync image) throws Exception {
        Method processSerialCalls = Display.class.getDeclaredMethod("processSerialCalls");
        processSerialCalls.setAccessible(true);
        long start = System.currentTimeMillis();
        while (getImageDataField(image) == null) {
            processSerialCalls.invoke(Display.getInstance());
            if (System.currentTimeMillis() - start > 2000) {
                fail("Timed out waiting for image data to load");
            }
            Thread.sleep(10);
        }
    }

    private byte[] getImageDataField(StorageImageAsync image) throws Exception {
        Field field = StorageImageAsync.class.getDeclaredField("imageData");
        field.setAccessible(true);
        return (byte[]) field.get(image);
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
