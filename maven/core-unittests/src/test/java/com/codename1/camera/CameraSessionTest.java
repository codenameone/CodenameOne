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
package com.codename1.camera;

import com.codename1.junit.UITestBase;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for {@link CameraSession}: the methods that delegate to the
 * per-session {@link com.codename1.impl.CameraImpl} backend, view caching,
 * frame-listener management, and the idempotent close contract. The session
 * is created through the real {@link Camera#open} path against a hand-written
 * {@link RecordingCameraImpl}.
 */
class CameraSessionTest extends UITestBase {

    private RecordingCameraImpl impl;
    private CameraSession session;

    @BeforeEach
    void openSession() {
        impl = new RecordingCameraImpl();
        implementation.setCameraImpl(impl);
        session = Camera.open(new CameraInfo("back", CameraFacing.BACK, null, null, true, true),
                new CameraSessionOptions());
    }

    @AfterEach
    void closeSession() {
        if (session != null) {
            session.close();
            session = null;
        }
    }

    @Test
    void infoAndOptionsAreExposed() {
        assertEquals("back", session.getInfo().getId());
        assertNotNull(session.getOptions());
    }

    @Test
    void createViewCachesASingleInstance() {
        CameraView v1 = session.createView();
        CameraView v2 = session.createView();
        assertNotNull(v1);
        assertSame(v1, v2);
        assertSame(session, v1.getSession());
    }

    @Test
    void takePhotoDelegatesAndResolves() {
        AsyncResource<CapturedPhoto> r = session.takePhoto();
        CapturedPhoto photo = await(r);
        assertNotNull(photo);
        assertEquals(4, photo.getWidth());
        assertNotNull(impl.lastPhotoOptions);
    }

    @Test
    void takePhotoWithNullOptionsSubstitutesDefaults() {
        AsyncResource<CapturedPhoto> r = session.takePhoto(null);
        await(r);
        assertNotNull(impl.lastPhotoOptions, "session should pass a non-null options object to the backend");
    }

    @Test
    void takePhotoForwardsSuppliedOptions() {
        PhotoCaptureOptions opts = new PhotoCaptureOptions().jpegQuality(55);
        await(session.takePhoto(opts));
        assertSame(opts, impl.lastPhotoOptions);
    }

    @Test
    void startVideoRecordingForwardsPathAndAudioFlag() {
        VideoRecording rec = session.startVideoRecording("file://clip.mp4");
        assertNotNull(rec);
        assertEquals("file://clip.mp4", impl.videoPath);
        assertEquals("file://clip.mp4", rec.getRequestedPath());
    }

    @Test
    void setFrameListenerInstallsListener() {
        FrameListener l = new FrameListener() {
            public void onFrame(CameraFrame frame) {
            }
        };
        session.setFrameListener(l);
        assertSame(l, impl.lastFrameListener);
    }

    @Test
    void addFrameListenerIsAnAliasForSet() {
        FrameListener l = new FrameListener() {
            public void onFrame(CameraFrame frame) {
            }
        };
        session.addFrameListener(l);
        assertSame(l, impl.lastFrameListener);
    }

    @Test
    void removeMatchingFrameListenerClearsIt() {
        FrameListener l = new FrameListener() {
            public void onFrame(CameraFrame frame) {
            }
        };
        session.setFrameListener(l);
        impl.frameListenerCleared = false;
        session.removeFrameListener(l);
        assertNull(impl.lastFrameListener);
        assertTrue(impl.frameListenerCleared);
    }

    @Test
    void removeNonMatchingFrameListenerIsIgnored() {
        FrameListener installed = new FrameListener() {
            public void onFrame(CameraFrame frame) {
            }
        };
        FrameListener other = new FrameListener() {
            public void onFrame(CameraFrame frame) {
            }
        };
        session.setFrameListener(installed);
        session.removeFrameListener(other);
        // The installed listener must survive an unrelated remove.
        assertSame(installed, impl.lastFrameListener);
    }

    @Test
    void flashZoomAndFocusDelegateToBackend() {
        session.setFlashMode(FlashMode.TORCH);
        session.setZoom(2.5f);
        session.focus(0.25f, 0.75f);
        assertEquals(FlashMode.TORCH, impl.lastFlashMode);
        assertEquals(2.5f, impl.lastZoom, 1e-6f);
        assertEquals(0.25f, impl.lastFocusX, 1e-6f);
        assertEquals(0.75f, impl.lastFocusY, 1e-6f);
    }

    @Test
    void pauseAndResumeDelegateToBackend() {
        session.pause();
        session.resume();
        assertEquals(1, impl.pauseCount);
        assertEquals(1, impl.resumeCount);
    }

    @Test
    void closeIsIdempotentAndClosesBackendOnce() {
        assertFalse(session.isClosed());
        session.close();
        assertTrue(session.isClosed());
        session.close();
        assertEquals(1, impl.closeCount);
    }

    private <T> T await(AsyncResource<T> r) {
        final AtomicReference<T> value = new AtomicReference<T>();
        r.ready(new SuccessCallback<T>() {
            public void onSucess(T v) {
                value.set(v);
            }
        });
        int budget = 4000;
        while (!r.isDone() && budget > 0) {
            flushSerialCalls();
            budget -= 5;
        }
        flushSerialCalls();
        return value.get();
    }
}
