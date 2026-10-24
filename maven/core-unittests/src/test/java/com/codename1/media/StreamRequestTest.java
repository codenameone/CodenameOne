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
package com.codename1.media;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class StreamRequestTest extends UITestBase {
    private Media media;

    @BeforeEach
    void configureImplementation() {
        media = mock(Media.class);
        implementation.setMedia(media);
    }

    @FormTest
    void unprotectedStreamPlaysThroughCreateMediaAndSeeks() throws IOException {
        StreamRequest req = new StreamRequest("https://example.com/v/master.m3u8").setStartPosition(42000);
        assertTrue(req.isSupported());
        assertSame(media, req.create());
        verify(media).setTime(42000);
    }

    @FormTest
    void zeroStartPositionDoesNotSeek() throws IOException {
        new StreamRequest("https://example.com/v.mp4").setStartPosition(-5).create();
        verify(media, never()).setTime(0);
    }

    @FormTest
    void defaultImplementationSupportsOnlyAes128() {
        // AES-128 HLS only needs the key URL in the playlist, which any HLS player reads.
        for (DrmScheme s : DrmScheme.values()) {
            assertEquals(s == DrmScheme.AES_128, MediaManager.isDrmSupported(s), s.name());
        }
        StreamRequest req = new StreamRequest("https://example.com/v.m3u8")
                .setDrm(new DrmConfig(DrmScheme.WIDEVINE, "https://example.com/license"));
        assertFalse(req.isSupported());
        IOException e = assertThrows(IOException.class, req::create);
        assertTrue(e.getMessage().contains("WIDEVINE"));
        assertThrows(IllegalArgumentException.class,
                () -> new DrmConfig(DrmScheme.CLEARKEY, "https://example.com/license").addLicenseHeader("X", "a\nb"));
        assertThrows(IllegalArgumentException.class, () -> req.addHeader("X\r", "v"));
    }

    @FormTest
    void headersNeedPlatformSupport() {
        assertFalse(MediaManager.isStreamHeadersSupported());
        StreamRequest req = new StreamRequest("https://example.com/v.m3u8").addHeader("Authorization", "Bearer x");
        assertFalse(req.isSupported());
        assertThrows(IOException.class, req::create);
    }

    @FormTest
    void drmConfigValidatesAndCopies() {
        assertThrows(IllegalArgumentException.class, () -> new DrmConfig(null, "u"));
        assertThrows(IllegalArgumentException.class, () -> new DrmConfig(DrmScheme.FAIRPLAY, null));
        assertNull(new DrmConfig(DrmScheme.AES_128, null).getLicenseUrl());
        DrmConfig c = new DrmConfig(DrmScheme.FAIRPLAY, "https://l").setCertificateUrl("https://c")
                .setContentId("title-7").addLicenseHeader("X-Token", "t");
        assertEquals("https://c", c.getCertificateUrl());
        assertEquals("title-7", c.getContentId());
        c.getLicenseHeaders().clear();
        assertEquals("t", c.getLicenseHeaders().get("X-Token"));
        assertThrows(IllegalArgumentException.class, () -> new StreamRequest(""));
    }
}
