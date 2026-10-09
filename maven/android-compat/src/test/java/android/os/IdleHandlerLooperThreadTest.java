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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// An idle handler registered on a background looper's queue runs on that
/// looper's thread, when its queue runs out of work. It used to be handed to
/// the event dispatch thread whatever queue it was added to, so it ran
/// concurrently with the looper it was meant to be confined to.
public class IdleHandlerLooperThreadTest {

    @Test
    public void runsOnTheOwningLooperThread() throws Exception {
        // The EDT exists, so a handler wrongly sent there would run there.
        AndroidTestSupport.context();
        HandlerThread t = new HandlerThread("idle-owner");
        t.start();
        try {
            final AtomicReference<Thread> ranOn = new AtomicReference<Thread>();
            final CountDownLatch ran = new CountDownLatch(1);
            t.getLooper().getQueue().addIdleHandler(new MessageQueue.IdleHandler() {
                @Override
                public boolean queueIdle() {
                    ranOn.set(Thread.currentThread());
                    ran.countDown();
                    return false;
                }
            });
            assertTrue("the idle handler ran", ran.await(10, TimeUnit.SECONDS));
            assertSame("ran on the looper's own thread", t, ranOn.get());
        } finally {
            t.quit();
            t.join(10000);
        }
    }

    @Test
    public void keptHandlerRunsAgainAfterTheNextMessageOnly() throws Exception {
        AndroidTestSupport.context();
        HandlerThread t = new HandlerThread("idle-again");
        t.start();
        try {
            final AtomicInteger runs = new AtomicInteger();
            final MessageQueue.IdleHandler h = new MessageQueue.IdleHandler() {
                @Override
                public boolean queueIdle() {
                    runs.incrementAndGet();
                    return true;
                }
            };
            Handler handler = new Handler(t.getLooper());
            final CountDownLatch added = new CountDownLatch(1);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Looper.myQueue().addIdleHandler(h);
                    added.countDown();
                }
            });
            assertTrue(added.await(10, TimeUnit.SECONDS));
            awaitRuns(runs, 1);
            // An idle queue does not spin its idle handlers.
            Thread.sleep(200);
            assertEquals("ran once per idle period", 1, runs.get());
            handler.post(new Runnable() {
                @Override
                public void run() {
                }
            });
            awaitRuns(runs, 2);
            // A second getQueue() call names the same queue, so removal works.
            final CountDownLatch removed = new CountDownLatch(1);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Looper.myLooper().getQueue().removeIdleHandler(h);
                    removed.countDown();
                }
            });
            assertTrue(removed.await(10, TimeUnit.SECONDS));
            int after = runs.get();
            handler.post(new Runnable() {
                @Override
                public void run() {
                }
            });
            Thread.sleep(200);
            assertEquals("a removed handler does not run", after, runs.get());
        } finally {
            t.quit();
            t.join(10000);
            assertFalse(t.isAlive());
        }
    }

    private static void awaitRuns(AtomicInteger runs, int n) throws InterruptedException {
        long end = System.currentTimeMillis() + 10000;
        while (runs.get() < n && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        assertTrue("the idle handler ran " + n + " times", runs.get() >= n);
    }
}
