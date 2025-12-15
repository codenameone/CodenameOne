package com.codename1.ui;

import com.codename1.io.NetworkManager;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import org.junit.jupiter.api.Assertions;

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

    @FormTest
    public void testDownloadCompleted() {
        // Create a valid placeholder image using raw data (1x1 GIF)
        byte[] data = new byte[] {
            (byte)0x47, (byte)0x49, (byte)0x46, (byte)0x38, (byte)0x39, (byte)0x61, (byte)0x01, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x80, (byte)0x00, (byte)0x00, (byte)0xff, (byte)0xff, (byte)0xff,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x2c, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x02, (byte)0x44,
            (byte)0x01, (byte)0x00, (byte)0x3b
        };
        EncodedImage placeholder = EncodedImage.create(data);

        String url = "http://example.com/image.png";

        // Setup mock network response
        TestCodenameOneImplementation.getInstance().addNetworkMockResponse(url, 200, "OK", data);

        URLImage img = URLImage.createToStorage(placeholder, "storageKey", url, URLImage.RESIZE_SCALE);

        // This should trigger download
        img.fetch();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}

        // Mock a different response (another valid image, same 1x1 GIF for simplicity)
        TestCodenameOneImplementation.getInstance().addNetworkMockResponse(url, 200, "OK", data);

        URLImage img2 = URLImage.createToStorage(placeholder, "storageKey2", url, URLImage.RESIZE_SCALE);
        img2.fetch();

        com.codename1.ui.DisplayTest.flushEdt();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {}
        com.codename1.ui.DisplayTest.flushEdt();
    }

    @FormTest
    public void testCachedImage() {
        EncodedImage placeholder = createPlaceholder();
        String url = "http://example.com/cached.png";

        // Mock response
        byte[] data = new byte[] {
            (byte)0x47, (byte)0x49, (byte)0x46, (byte)0x38, (byte)0x39, (byte)0x61, (byte)0x01, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x80, (byte)0x00, (byte)0x00, (byte)0xff, (byte)0xff, (byte)0xff,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x2c, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x02, (byte)0x44,
            (byte)0x01, (byte)0x00, (byte)0x3b
        };
        TestCodenameOneImplementation.getInstance().addNetworkMockResponse(url, 200, "OK", data);

        try {
            com.codename1.io.FileSystemStorage.getInstance().openOutputStream("cachedKey").write(data);
            com.codename1.io.FileSystemStorage.getInstance().openOutputStream("cachedKey").close();
        } catch(java.io.IOException err) {}

        URLImage img = URLImage.createToStorage(placeholder, "cachedKey", url, URLImage.RESIZE_SCALE);

        Assertions.assertNotNull(img.getImage());

        Image buffer = Image.createImage(10, 10);
        buffer.getGraphics().drawImage(img, 0, 0);
    }
}
