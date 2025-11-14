package com.codename1.ui;

import com.codename1.io.NetworkManager;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class URLImageTest extends UITestBase {

    private EncodedImage createPlaceholder() {
        byte[] data = new byte[]{1, 1, (byte) 0xff};
        return EncodedImage.create(data, 1, 1, true);
    }

    @FormTest
    void testCreateToStorageReusesInstance() {
        EncodedImage placeholder = createPlaceholder();
        URLImage first = URLImage.createToStorage(placeholder, "storageKey-1", "http://example.com/a");
        URLImage second = URLImage.createToStorage(placeholder, "storageKey-1", "http://example.com/a");
        URLImage third = URLImage.createToStorage(placeholder, "storageKey-2", "http://example.com/b");

        assertSame(first, second);
        assertNotSame(first, third);
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
