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

import com.codename1.androidcompat.testing.AndroidTestSupport;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// A background looper's queue is not idle while a message is due on it. It
/// used to answer idle whatever it held. A message posted for later does not
/// make it busy, as on Android.
public class MessageQueueIsIdleTest {

    @Test
    public void dueMessageMakesTheQueueBusy() throws Exception {
        AndroidTestSupport.context();
        HandlerThread t = new HandlerThread("is-idle");
        t.start();
        try {
            final Handler h = new Handler(t.getLooper());
            final List<Boolean> seen = new ArrayList<Boolean>();
            final CountDownLatch done = new CountDownLatch(1);
            final Runnable noop = new Runnable() {
                @Override
                public void run() {
                }
            };
            h.post(new Runnable() {
                @Override
                public void run() {
                    MessageQueue q = Looper.myQueue();
                    seen.add(Boolean.valueOf(q.isIdle()));
                    h.postDelayed(noop, 60000);
                    seen.add(Boolean.valueOf(q.isIdle()));
                    h.post(noop);
                    seen.add(Boolean.valueOf(q.isIdle()));
                    h.removeCallbacks(noop);
                    seen.add(Boolean.valueOf(q.isIdle()));
                    done.countDown();
                }
            });
            assertTrue("the message ran", done.await(10, TimeUnit.SECONDS));
            assertEquals("[true, true, false, true]", seen.toString());
        } finally {
            t.quit();
            t.join(10000);
        }
    }
}
