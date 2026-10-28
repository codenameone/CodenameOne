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

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.EncodedImage;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// An empty cache entry -- a download another request is still writing, or
/// one that failed -- must not be handed back as the image.
class ImageCacheEmptyEntryTest extends UITestBase {
    @FormTest
    void anEmptyStorageEntryIsDownloadedAgain() throws Exception {
        String url = "http://example.com/empty-entry.png";
        String key = "cn1_image_cache_test_empty";
        OutputStream out = Storage.getInstance().createOutputStream(key);
        out.close();
        byte[] body = {1, 2, 3, 4, 5, 6, 7, 8};
        TestCodenameOneImplementation.getInstance().addNetworkMockResponse(url, 200, "OK", body);
        final AtomicReference<Object> result = new AtomicReference<Object>();
        try {
            Util.downloadImageToStorage(url, key, img -> result.set(img), (sender, err, code, msg) -> result.set(err));
            long start = System.currentTimeMillis();
            while (result.get() == null && System.currentTimeMillis() - start < 5000) {
                com.codename1.ui.DisplayTest.flushEdt();
                Thread.sleep(10);
            }
            Object got = result.get();
            assertTrue(got instanceof EncodedImage, String.valueOf(got));
            assertEquals(body.length, ((EncodedImage) got).getImageData().length);
        } finally {
            Storage.getInstance().deleteStorageFile(key);
            implementation.clearNetworkMocks();
        }
    }
}
