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
package com.codename1.background;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for {@link ForegroundService} against the default implementation,
 * which runs the task on a background thread without a system notification.
 * Verifies the task executes (receiving the running service handle), the
 * running-state transitions through {@code stop}, the no-op notification
 * update, the unsupported-platform probe, and the null-task path.
 */
class ForegroundServiceTest extends UITestBase {

    @Test
    void startRunsTaskWithTheServiceHandleAndIsRunning() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<ForegroundService> received = new AtomicReference<ForegroundService>();
        ForegroundService svc = ForegroundService.start("downloads", "Title", "Body", null,
                new ForegroundService.Task() {
                    public void run(ForegroundService service) {
                        received.set(service);
                        latch.countDown();
                    }
                });
        assertNotNull(svc);
        assertTrue(svc.isRunning());
        assertTrue(latch.await(3, TimeUnit.SECONDS), "the foreground task should have run");
        assertSame(svc, received.get());
        svc.stop();
    }

    @Test
    void stopMarksTheServiceNotRunning() {
        ForegroundService svc = ForegroundService.start("c", "t", "b", null, null);
        assertTrue(svc.isRunning());
        svc.stop();
        assertFalse(svc.isRunning());
        // A second stop is harmless.
        svc.stop();
        assertFalse(svc.isRunning());
    }

    @Test
    void updateNotificationDoesNotThrowRunningOrStopped() {
        ForegroundService svc = ForegroundService.start("c", "t", "b", null, null);
        svc.updateNotification("new title", "new body");
        svc.stop();
        // After stopping, the update short-circuits and is still safe.
        svc.updateNotification("ignored", "ignored");
    }

    @Test
    void isSupportedIsFalseOnTestPlatform() {
        assertFalse(ForegroundService.isSupported());
    }

    @Test
    void startWithNullTaskStillReturnsARunningService() {
        ForegroundService svc = ForegroundService.start("c", "t", "b", "icon", null);
        assertNotNull(svc);
        assertTrue(svc.isRunning());
        svc.stop();
    }
}
