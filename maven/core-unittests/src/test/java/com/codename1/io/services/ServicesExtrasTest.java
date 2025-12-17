package com.codename1.io.services;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

class ServicesExtrasTest extends UITestBase {

    @FormTest
    void testImageDownloadService() {
        // ImageDownloadService.createImageToStorage(url, callback, cacheId)
        ImageDownloadService.createImageToStorage("http://example.com/img.png", (evt) -> {}, "cache_id");
    }
}
