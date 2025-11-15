package com.codename1.io.services;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class ServicesPackageTest extends UITestBase {

    @FormTest
    void cachedDataExternalizesAndRestoresState() throws Exception {
        CachedData cached = new CachedData();
        cached.setUrl("https://example.com/data");
        cached.setData(new byte[]{1, 2, 3});
        cached.setEtag("etag-value");
        cached.setModified("Wed, 01 Jan 2020 00:00:00 GMT");
        cached.setFetching(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        cached.externalize(new DataOutputStream(out));

        CachedData restored = new CachedData();
        restored.internalize(restored.getVersion(), new DataInputStream(new ByteArrayInputStream(out.toByteArray())));

        assertArrayEquals(new byte[]{1, 2, 3}, restored.getData());
        assertEquals("https://example.com/data", restored.getUrl());
        assertEquals("etag-value", restored.getEtag());
        assertEquals("Wed, 01 Jan 2020 00:00:00 GMT", restored.getModified());
        assertFalse(restored.isFetching());

        restored.setFetching(true);
        assertTrue(restored.isFetching());
    }

    @FormTest
    void imageDownloadServiceStaticConfigurationApplies() throws Exception {
        boolean originalFastScale = ImageDownloadService.isFastScale();
        boolean originalAlwaysRevalidate = ImageDownloadService.isAlwaysRevalidate();
        boolean originalDefaultMaintain = ImageDownloadService.isDefaultMaintainAspectRatio();
        int originalTimeout = ImageDownloadService.getDefaultTimeout();
        try {
            ImageDownloadService.setFastScale(false);
            assertFalse(ImageDownloadService.isFastScale());
            ImageDownloadService.setAlwaysRevalidate(true);
            assertTrue(ImageDownloadService.isAlwaysRevalidate());
            ImageDownloadService.setDefaultMaintainAspectRatio(true);
            assertTrue(ImageDownloadService.isDefaultMaintainAspectRatio());
            ImageDownloadService.setDefaultTimeout(5000);
            assertEquals(5000, ImageDownloadService.getDefaultTimeout());

            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                }
            };
            ImageDownloadService.addErrorListener(listener);
            assertNotNull(getErrorDispatcher());
            ImageDownloadService.removeErrorListener(listener);
            assertNull(getErrorDispatcher());
        } finally {
            ImageDownloadService.setFastScale(originalFastScale);
            ImageDownloadService.setAlwaysRevalidate(originalAlwaysRevalidate);
            ImageDownloadService.setDefaultMaintainAspectRatio(originalDefaultMaintain);
            ImageDownloadService.setDefaultTimeout(originalTimeout);
        }
    }

    private Object getErrorDispatcher() throws Exception {
        Field field = ImageDownloadService.class.getDeclaredField("onErrorListeners");
        field.setAccessible(true);
        return field.get(null);
    }
}
