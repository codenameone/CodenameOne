package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class URLImageTest extends UITestBase {

    private EncodedImage createPlaceholder(int size) {
        Image base = Image.createImage(size, size);
        return EncodedImage.createFromImage(base, false);
    }

    @FormTest
    void testCreateToStorageReusesInstance() {
        EncodedImage placeholder = createPlaceholder(8);
        URLImage first = URLImage.createToStorage(placeholder, "storageKey-1", "http://example.com/a");
        URLImage second = URLImage.createToStorage(placeholder, "storageKey-1", "http://example.com/a");
        URLImage third = URLImage.createToStorage(placeholder, "storageKey-2", "http://example.com/b");

        assertSame(first, second);
        assertNotSame(first, third);
    }

    @FormTest
    void testCreateCachedImageRespectsNativeCacheFlag() {
        Image placeholder = Image.createImage(10, 10);
        implementation.setSupportsNativeImageCache(true);
        Image cached = URLImage.createCachedImage("native-cache", "http://example.com/native", placeholder, URLImage.FLAG_RESIZE_SCALE);
        assertNotNull(cached);
        assertFalse(cached instanceof URLImage);

        implementation.setSupportsNativeImageCache(false);
        Image fallback = URLImage.createCachedImage("storage-cache", "http://example.com/storage", placeholder, URLImage.FLAG_RESIZE_SCALE_TO_FILL);
        assertNotNull(fallback);
        assertTrue(fallback instanceof URLImage);
    }

    @FormTest
    void testExceptionHandlerSetter() {
        AtomicBoolean invoked = new AtomicBoolean();
        URLImage.ErrorCallback callback = (image, error) -> invoked.set(true);
        URLImage.setExceptionHandler(callback);
        assertSame(callback, URLImage.getExceptionHandler());
        URLImage.getExceptionHandler().onError(null, new Exception());
        assertTrue(invoked.get());
    }
}
