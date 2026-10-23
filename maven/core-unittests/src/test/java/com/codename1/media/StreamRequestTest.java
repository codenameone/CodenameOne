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
    void defaultImplementationSupportsNoDrmScheme() {
        for (DrmScheme s : DrmScheme.values()) {
            assertFalse(MediaManager.isDrmSupported(s), s.name());
        }
        StreamRequest req = new StreamRequest("https://example.com/v.mpd")
                .setDrm(new DrmConfig(DrmScheme.WIDEVINE, "https://example.com/license"));
        assertFalse(req.isSupported());
        IOException e = assertThrows(IOException.class, req::create);
        assertTrue(e.getMessage().contains("WIDEVINE"));
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
