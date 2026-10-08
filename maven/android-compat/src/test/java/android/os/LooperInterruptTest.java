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
package android.os;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

public class LooperInterruptTest {
    @Test public void interruptDoesNotAbandonAcceptedMessages() throws Exception {
        HandlerThread thread = new HandlerThread("interrupt-test");
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        try {
            long deadline = System.currentTimeMillis() + 3000;
            while (thread.getState() != Thread.State.TIMED_WAITING && System.currentTimeMillis() < deadline) {
                Thread.yield();
            }
            assertEquals(Thread.State.TIMED_WAITING, thread.getState());
            thread.interrupt();
            CountDownLatch delivered = new CountDownLatch(1);
            assertTrue(handler.postDelayed(() -> delivered.countDown(), 100));
            assertTrue("accepted message was abandoned", delivered.await(3, TimeUnit.SECONDS));
        } finally {
            thread.quit();
            thread.join(3000);
        }
        assertFalse(thread.isAlive());
    }
}
