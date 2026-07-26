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

import com.codename1.ai.vision.VisionAnalyzer;
import com.codename1.ai.vision.VisionImage;
import com.codename1.ai.vision.VisionPipeline;
import com.codename1.ai.vision.VisionPipelineListener;
import com.codename1.junit.UITestBase;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class VisionPipelineTest extends UITestBase {
    private RecordingCameraImpl implementationBackend;
    private CameraSession session;
    private VisionPipeline<String> pipeline;

    @BeforeEach
    void openSession() {
        implementationBackend = new RecordingCameraImpl();
        implementation.setCameraImpl(implementationBackend);
        session = Camera.open(new CameraInfo("back", CameraFacing.BACK,
                null, null, true, true), new CameraSessionOptions());
    }

    @AfterEach
    void closeSession() {
        if (pipeline != null) {
            pipeline.close();
        }
        if (session != null) {
            session.close();
        }
    }

    @Test
    void listenerFailureDoesNotStrandPendingFrame() {
        final RecordingAnalyzer analyzer = new RecordingAnalyzer();
        final int[] notifications = new int[1];
        pipeline = new VisionPipeline<String>(session, analyzer,
                new VisionPipelineListener<String>() {
                    public void result(String value, VisionImage image) {
                        notifications[0]++;
                        if (notifications[0] == 1) {
                            throw new IllegalStateException(
                                    "Application listener failed");
                        }
                    }

                    public void error(Throwable error) {
                    }
                });

        implementationBackend.lastFrameListener.onFrame(frame(1));
        implementationBackend.lastFrameListener.onFrame(frame(2));
        assertEquals(1, analyzer.operations.size());

        analyzer.operations.get(0).complete("first");
        try {
            flushSerialCalls();
        } catch (IllegalStateException expected) {
            // The application exception may propagate from the EDT harness,
            // but the pending operation must already have started.
        }
        assertEquals(2, analyzer.operations.size());

        analyzer.operations.get(1).complete("second");
        flushSerialCalls();
        assertEquals(2, notifications[0]);
        assertFalse(pipeline.isBusy());
    }

    private static CameraFrame frame(int value) {
        return new CameraFrame(new byte[] {(byte) value}, null,
                1, 1, 0, value, FrameFormat.JPEG);
    }

    private static final class RecordingAnalyzer
            implements VisionAnalyzer<String> {
        final List<AsyncResource<String>> operations =
                new ArrayList<AsyncResource<String>>();

        public boolean isSupported() {
            return true;
        }

        public AsyncResource<String> process(VisionImage image) {
            AsyncResource<String> operation = new AsyncResource<String>();
            operations.add(operation);
            return operation;
        }

        public void close() {
        }
    }
}
