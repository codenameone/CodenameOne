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
package com.codename1.compat.jdk;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// The executor services, on real Codename One background threads.
public class ExecutorsTest {

    @BeforeClass
    public static void startCodenameOne() {
        HeadlessImplementation.install();
    }

    private static void terminate(ExecutorService pool) throws Exception {
        pool.shutdown();
        assertTrue(pool.isShutdown());
        assertTrue("the workers did not stop", pool.awaitTermination(10, TimeUnit.SECONDS));
        assertTrue(pool.isTerminated());
    }

    @Test
    public void aSingleThreadRunsTasksInOrderOffTheEventDispatchThread() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        final List<Integer> order = Collections.synchronizedList(new ArrayList<Integer>());
        final AtomicBoolean onEdt = new AtomicBoolean();
        final Thread caller = Thread.currentThread();
        final AtomicBoolean onCaller = new AtomicBoolean();
        List<Future<Integer>> futures = new ArrayList<Future<Integer>>();
        for (int i = 0; i < 50; i++) {
            final int n = i;
            futures.add(pool.submit(new Callable<Integer>() {
                @Override
                public Integer call() {
                    order.add(Integer.valueOf(n));
                    onEdt.compareAndSet(false, Display.getInstance().isEdt());
                    onCaller.compareAndSet(false, Thread.currentThread() == caller);
                    return Integer.valueOf(n * 2);
                }
            }));
        }
        for (int i = 0; i < 50; i++) {
            assertEquals(Integer.valueOf(i * 2), futures.get(i).get(10, TimeUnit.SECONDS));
            assertTrue(futures.get(i).isDone());
            assertFalse(futures.get(i).isCancelled());
        }
        List<Integer> expected = new ArrayList<Integer>();
        for (int i = 0; i < 50; i++) {
            expected.add(Integer.valueOf(i));
        }
        assertEquals(expected, order);
        assertFalse("a task ran on the event dispatch thread", onEdt.get());
        assertFalse("a task ran on the submitting thread", onCaller.get());
        assertFalse(pool.isTerminated());
        terminate(pool);
    }

    @Test
    public void aFailingTaskSurfacesAsAnExecutionException() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        final IllegalStateException boom = new IllegalStateException("boom");
        Future<String> failed = pool.submit(new Callable<String>() {
            @Override
            public String call() {
                throw boom;
            }
        });
        try {
            failed.get(10, TimeUnit.SECONDS);
            fail("The task threw");
        } catch (ExecutionException expected) {
            assertSame(boom, expected.getCause());
        }
        assertTrue(failed.isDone());
        // The worker survives a task that throws.
        assertEquals("after", pool.submit(Executors.callable(new Runnable() {
            @Override
            public void run() {
            }
        }, "after")).get(10, TimeUnit.SECONDS));
        terminate(pool);
    }

    @Test
    public void aPoolRunsEveryTaskAndInvokeAllWaitsForThem() throws Exception {
        ExecutorService pool = Executors.newCachedThreadPool();
        final AtomicInteger ran = new AtomicInteger();
        List<Callable<Integer>> tasks = new ArrayList<Callable<Integer>>();
        for (int i = 0; i < 40; i++) {
            final int n = i;
            tasks.add(new Callable<Integer>() {
                @Override
                public Integer call() {
                    ran.incrementAndGet();
                    if (n == 13) {
                        throw new IllegalArgumentException("unlucky");
                    }
                    return Integer.valueOf(n);
                }
            });
        }
        List<Future<Integer>> futures = pool.invokeAll(tasks);
        assertEquals(40, ran.get());
        assertEquals(40, futures.size());
        for (int i = 0; i < 40; i++) {
            assertTrue(futures.get(i).isDone());
            if (i == 13) {
                try {
                    futures.get(i).get();
                    fail();
                } catch (ExecutionException expected) {
                    assertEquals("unlucky", expected.getCause().getMessage());
                }
            } else {
                assertEquals(Integer.valueOf(i), futures.get(i).get());
            }
        }
        final AtomicBoolean plain = new AtomicBoolean();
        Future<?> done = pool.submit(new Runnable() {
            @Override
            public void run() {
                plain.set(true);
            }
        });
        done.get(10, TimeUnit.SECONDS);
        assertTrue(plain.get());
        terminate(pool);
    }

    @Test
    public void shutdownRefusesNewTasksButFinishesAcceptedOnes() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        final AtomicInteger ran = new AtomicInteger();
        for (int i = 0; i < 20; i++) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    ran.incrementAndGet();
                }
            });
        }
        pool.shutdown();
        try {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    ran.addAndGet(1000);
                }
            });
            fail("A shut down executor accepts nothing");
        } catch (RejectedExecutionException expected) {
            assertNotNull(expected);
        }
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(20, ran.get());
    }

    @Test
    public void shutdownNowHandsBackWhatNeverStartedAndCancelsItsFutures() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        final AtomicBoolean release = new AtomicBoolean();
        final AtomicBoolean started = new AtomicBoolean();
        Future<String> running = pool.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                started.set(true);
                while (!release.get()) {
                    Thread.sleep(2);
                }
                return "finished";
            }
        });
        while (!started.get()) {
            Thread.sleep(2);
        }
        final AtomicBoolean queuedRan = new AtomicBoolean();
        Future<String> queued = pool.submit(new Callable<String>() {
            @Override
            public String call() {
                queuedRan.set(true);
                return "queued";
            }
        });
        try {
            queued.get(50, TimeUnit.MILLISECONDS);
            fail("The worker is busy with the first task");
        } catch (TimeoutException expected) {
            assertNotNull(expected);
        }
        assertFalse("A started task cannot be cancelled", running.cancel(true));

        List<Runnable> dropped = pool.shutdownNow();
        assertEquals(1, dropped.size());
        assertTrue(queued.isCancelled());
        try {
            queued.get();
            fail("A dropped task is a cancelled one");
        } catch (CancellationException expected) {
            assertNotNull(expected);
        }
        release.set(true);
        assertEquals("finished", running.get(10, TimeUnit.SECONDS));
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertFalse(queuedRan.get());
    }

    @Test
    public void theArgumentsAreChecked() {
        for (int bad : Arrays.asList(Integer.valueOf(0), Integer.valueOf(-1))) {
            try {
                Executors.newFixedThreadPool(bad);
                fail();
            } catch (IllegalArgumentException expected) {
                assertNotNull(expected);
            }
        }
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            pool.execute(null);
            fail();
        } catch (NullPointerException expected) {
            assertNotNull(expected);
        }
        pool.shutdown();
    }

    private static Callable<String> answering(final String value, final AtomicBoolean ran) {
        return new Callable<String>() {
            @Override
            public String call() {
                ran.set(true);
                return value;
            }
        };
    }

    private static final Runnable NOTHING = new Runnable() {
        @Override
        public void run() {
        }
    };

    /// What a JDK scheduled executor does by default: delayed work that was
    /// accepted still runs after `shutdown()`, repeating work is cancelled.
    @Test
    public void anOrderlyShutdownStillRunsDelayedWorkAndCancelsWhatRepeats() throws Exception {
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
        AtomicBoolean ran = new AtomicBoolean();
        ScheduledFuture<String> once = pool.schedule(answering("ran", ran), 300, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> repeating = pool.scheduleAtFixedRate(NOTHING, 50, 50, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> spaced = pool.scheduleWithFixedDelay(NOTHING, 50, 50, TimeUnit.MILLISECONDS);
        pool.shutdown();
        assertTrue(pool.isShutdown());
        assertFalse("Accepted work is still due", pool.isTerminated());
        assertTrue(repeating.isCancelled());
        assertTrue(spaced.isCancelled());
        assertFalse(once.isDone());
        try {
            pool.schedule(answering("late", new AtomicBoolean()), 1, TimeUnit.MILLISECONDS);
            fail("Nothing new is accepted");
        } catch (RejectedExecutionException expected) {
            assertNotNull(expected);
        }
        try {
            pool.execute(NOTHING);
            fail("Nothing new is accepted");
        } catch (RejectedExecutionException expected) {
            assertNotNull(expected);
        }
        try {
            pool.submit(NOTHING);
            fail("Nothing new is accepted");
        } catch (RejectedExecutionException expected) {
            assertNotNull(expected);
        }
        assertEquals("ran", once.get(10, TimeUnit.SECONDS));
        assertTrue(ran.get());
        assertTrue("the executor did not stop after its last task", pool.awaitTermination(10, TimeUnit.SECONDS));
        assertTrue(pool.isTerminated());

        // With nothing due, it stops at once.
        ScheduledExecutorService idle = Executors.newSingleThreadScheduledExecutor();
        idle.scheduleAtFixedRate(NOTHING, 50, 50, TimeUnit.MILLISECONDS);
        terminate(idle);

        // Work cancelled before the shutdown is not waited for.
        ScheduledExecutorService cancelled = Executors.newSingleThreadScheduledExecutor();
        assertTrue(cancelled.schedule(answering("never", new AtomicBoolean()), 1, TimeUnit.HOURS).cancel(false));
        terminate(cancelled);
    }

    @Test
    public void shutdownNowDropsDelayedWork() throws Exception {
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
        AtomicBoolean ran = new AtomicBoolean();
        ScheduledFuture<String> once = pool.schedule(answering("ran", ran), 150, TimeUnit.MILLISECONDS);
        pool.shutdownNow();
        assertTrue(pool.isShutdown());
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        Thread.sleep(400);
        assertFalse(ran.get());
        // As the JDK leaves the future of a dropped task.
        assertFalse(once.isDone());
        assertFalse(once.isCancelled());
    }

    private static boolean collected(java.lang.ref.WeakReference<?> ref) throws Exception {
        for (int i = 0; i < 100 && ref.get() != null; i++) {
            System.gc();
            Thread.sleep(20);
        }
        return ref.get() == null;
    }

    /// A timeout set for a future that completed in time is cancelled, and
    /// holds on to nothing: the timer keeps a cancelled task until its time.
    @Test
    public void aTimeoutNoLongerNeededLetsGoOfItsFuture() throws Exception {
        CompletableFuture<String> failing = new CompletableFuture<String>();
        assertSame(failing, failing.orTimeout(1, TimeUnit.HOURS));
        assertTrue(failing.complete("in time"));
        java.lang.ref.WeakReference<Object> first = new java.lang.ref.WeakReference<Object>(failing);
        failing = null;
        assertTrue("orTimeout still holds a future that completed", collected(first));

        CompletableFuture<String> defaulting = new CompletableFuture<String>();
        defaulting.completeOnTimeout("late", 1, TimeUnit.HOURS);
        assertTrue(defaulting.cancel(false));
        java.lang.ref.WeakReference<Object> second = new java.lang.ref.WeakReference<Object>(defaulting);
        defaulting = null;
        assertTrue("completeOnTimeout still holds a future that completed", collected(second));

        // And one that is needed still fires.
        assertEquals("late", new CompletableFuture<String>().completeOnTimeout("late", 30, TimeUnit.MILLISECONDS)
                .get(10, TimeUnit.SECONDS));
        try {
            new CompletableFuture<String>().orTimeout(30, TimeUnit.MILLISECONDS).get(10, TimeUnit.SECONDS);
            fail("The time passed");
        } catch (ExecutionException expected) {
            assertTrue(String.valueOf(expected.getCause()), expected.getCause() instanceof TimeoutException);
        }
    }
}
