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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/// The executors, futures and concurrent collections, on real threads,
/// printed as one line the JVM and ParparVM must agree on. Every value is
/// either independent of scheduling or gathered in submission order, so the
/// line is the same however the threads interleave.
public class ConcurrentApiApp {
    private static final StringBuilder OUT = new StringBuilder();

    private static void out(Object label, Object value) {
        OUT.append(label).append('=').append(value).append(';');
    }

    public static void fixtureMain(String[] args) throws Exception {
        pools();
        invokeAllAndAny();
        cancellation();
        completableFutures();
        concurrentMaps();
        copyOnWrite();
        latchesAndScheduling();
        System.out.println("RESULT=" + OUT);
    }

    private static void pools() throws Exception {
        ExecutorService fixed = Executors.newFixedThreadPool(4);
        List<Future<Integer>> squares = new ArrayList<Future<Integer>>();
        for (int i = 0; i < 20; i++) {
            final int n = i;
            squares.add(fixed.submit(() -> n * n));
        }
        int sum = 0;
        for (Future<Integer> f : squares) {
            sum += f.get();
        }
        out("fixedSum", sum);
        final AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < 100; i++) {
            fixed.execute(counter::incrementAndGet);
        }
        fixed.shutdown();
        out("terminated", fixed.awaitTermination(30, TimeUnit.SECONDS));
        out("counter", counter.get());
        out("isShutdown", fixed.isShutdown());
        try {
            fixed.execute(() -> { });
            out("rejected", "no");
        } catch (RejectedExecutionException e) {
            out("rejected", "yes");
        }

        ExecutorService single = Executors.newSingleThreadExecutor();
        final StringBuilder order = new StringBuilder();
        List<Future<?>> steps = new ArrayList<Future<?>>();
        for (int i = 0; i < 6; i++) {
            final int n = i;
            steps.add(single.submit(() -> {
                synchronized (order) {
                    order.append(n);
                }
            }));
        }
        for (Future<?> f : steps) {
            f.get();
        }
        synchronized (order) {
            out("singleOrder", order);
        }
        out("resultOf", single.submit(() -> { }, "given").get());
        single.shutdownNow();

        ExecutorService cached = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        Future<String> boom = cached.submit(() -> {
            throw new IllegalStateException("boom");
        });
        try {
            boom.get();
            out("execException", "none");
        } catch (ExecutionException e) {
            out("execException", e.getCause().getClass().getName() + ":" + e.getCause().getMessage());
        }
        out("boomDone", boom.isDone());
        cached.shutdown();
        out("cachedTerminated", cached.awaitTermination(30, TimeUnit.SECONDS));
        out("callable", Executors.callable(() -> { }, 7).call());
    }

    private static void invokeAllAndAny() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        List<Callable<String>> tasks = new ArrayList<Callable<String>>();
        for (int i = 0; i < 5; i++) {
            final int n = i;
            tasks.add(() -> "t" + n);
        }
        StringBuilder all = new StringBuilder();
        for (Future<String> f : pool.invokeAll(tasks)) {
            all.append(f.isDone()).append(':').append(f.get()).append(',');
        }
        out("invokeAll", all);
        List<Callable<String>> one = new ArrayList<Callable<String>>();
        one.add(() -> {
            throw new IllegalArgumentException("first");
        });
        one.add(() -> "second");
        out("invokeAny", pool.invokeAny(one));
        List<Callable<String>> slow = new ArrayList<Callable<String>>();
        slow.add(() -> {
            // Polled for the same reason as in cancellation(): the cancel
            // that the timeout makes must end this promptly on both VMs.
            long end = System.currentTimeMillis() + 10000;
            while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < end) {
                Thread.sleep(5);
            }
            return "late";
        });
        slow.add(() -> "quick");
        List<Future<String>> timed = pool.invokeAll(slow, 200, TimeUnit.MILLISECONDS);
        out("timedFirstCancelled", timed.get(0).isCancelled());
        out("timedSecond", timed.get(1).get());
        pool.shutdownNow();
        out("poolTerminated", pool.awaitTermination(30, TimeUnit.SECONDS));
    }

    private static void cancellation() throws Exception {
        FutureTask<String> task = new FutureTask<String>(() -> "ran");
        out("cancelUnstarted", task.cancel(false));
        task.run();
        out("cancelled", task.isCancelled() + "/" + task.isDone());
        try {
            task.get();
            out("getCancelled", "none");
        } catch (CancellationException e) {
            out("getCancelled", "ce");
        }
        FutureTask<String> done = new FutureTask<String>(() -> "value");
        done.run();
        out("cancelDone", done.cancel(true));
        out("doneValue", done.get());

        final StringBuilder hook = new StringBuilder();
        FutureTask<Integer> withHook = new FutureTask<Integer>(() -> 42) {
            @Override
            protected void done() {
                hook.append("done:").append(isCancelled());
            }
        };
        withHook.run();
        out("doneHook", hook);

        ExecutorService pool = Executors.newSingleThreadExecutor();
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch interrupted = new CountDownLatch(1);
        // Polls the flag rather than sleeping it out: ParparVM's interrupt()
        // sets the flag but does not wake a Thread.sleep (see
        // java_lang_Thread_sleepImpl in nativeMethods.m), so the JVM sees the
        // exception and ParparVM the flag -- either is the runner interrupted.
        Future<?> sleeper = pool.submit(() -> {
            started.countDown();
            long end = System.currentTimeMillis() + 30000;
            try {
                while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < end) {
                    Thread.sleep(5);
                }
                if (Thread.currentThread().isInterrupted()) {
                    interrupted.countDown();
                }
            } catch (InterruptedException e) {
                interrupted.countDown();
            }
        });
        started.await();
        out("cancelRunning", sleeper.cancel(true));
        out("runnerInterrupted", interrupted.await(30, TimeUnit.SECONDS));
        out("nextNotInterrupted", pool.submit(() -> Thread.currentThread().isInterrupted()).get());
        try {
            pool.submit(() -> "x").get(1, TimeUnit.NANOSECONDS);
            out("timeout", "maybe");
        } catch (TimeoutException e) {
            out("timeout", "maybe");
        }
        pool.shutdown();
        out("singleTerminated", pool.awaitTermination(30, TimeUnit.SECONDS));
    }

    private static void completableFutures() throws Exception {
        CompletableFuture<Integer> a = CompletableFuture.supplyAsync(() -> 6);
        CompletableFuture<Integer> b = CompletableFuture.supplyAsync(() -> 7);
        out("combine", a.thenCombine(b, (x, y) -> x * y).get());
        out("chain", CompletableFuture.completedFuture("a")
                .thenApply(s -> s + "b")
                .thenCompose(s -> CompletableFuture.supplyAsync(() -> s + "c"))
                .join());
        CompletableFuture<String> failed = CompletableFuture.supplyAsync(() -> {
            throw new IllegalStateException("bad");
        });
        try {
            failed.join();
            out("join", "none");
        } catch (CompletionException e) {
            out("join", e.getCause().getClass().getName());
        }
        try {
            failed.get();
            out("getFailed", "none");
        } catch (ExecutionException e) {
            out("getFailed", e.getCause().getMessage());
        }
        out("exceptionally", failed.exceptionally(t -> "recovered").join());
        out("handle", failed.handle((v, t) -> v == null ? "handled" : v).join());
        out("isCompletedExceptionally", failed.isCompletedExceptionally());

        List<CompletableFuture<Integer>> parts = new ArrayList<CompletableFuture<Integer>>();
        for (int i = 1; i <= 10; i++) {
            final int n = i;
            parts.add(CompletableFuture.supplyAsync(() -> n));
        }
        CompletableFuture.allOf(parts.toArray(new CompletableFuture[0])).join();
        int total = 0;
        for (CompletableFuture<Integer> p : parts) {
            total += p.join();
        }
        out("allOf", total);
        CompletableFuture<String> manual = new CompletableFuture<String>();
        CompletableFuture<Object> any = CompletableFuture.anyOf(manual, CompletableFuture.completedFuture("ready"));
        out("anyOf", any.join());
        out("completeOnce", manual.complete("x") + "/" + manual.complete("y") + "/" + manual.join());
        CompletableFuture<String> cancelled = new CompletableFuture<String>();
        out("cfCancel", cancelled.cancel(true) + "/" + cancelled.isCancelled());
        final AtomicReference<String> seen = new AtomicReference<String>();
        CompletableFuture.supplyAsync(() -> "async").thenAccept(seen::set).join();
        out("thenAccept", seen.get());
    }

    private static void concurrentMaps() throws Exception {
        final ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<String, Integer>();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<?>> work = new ArrayList<Future<?>>();
        for (int t = 0; t < 4; t++) {
            work.add(pool.submit(() -> {
                for (int i = 0; i < 250; i++) {
                    map.merge("k" + (i % 10), 1, Integer::sum);
                    map.compute("total", (k, v) -> v == null ? 1 : v + 1);
                }
            }));
        }
        for (Future<?> f : work) {
            f.get();
        }
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
        out("chm", new TreeMap<String, Integer>(map));
        out("putIfAbsent", map.putIfAbsent("k0", -1) + "/" + map.putIfAbsent("new", 5));
        out("replace", map.replace("new", 5, 6) + "/" + map.get("new"));
        out("removeKV", map.remove("new", 0) + "/" + map.remove("new", 6));
        out("computeIfAbsent", map.computeIfAbsent("abs", k -> k.length()));
        out("getOrDefault", map.getOrDefault("missing", -9));
        try {
            map.put(null, 1);
            out("nullKey", "accepted");
        } catch (NullPointerException e) {
            out("nullKey", "npe");
        }
        ConcurrentMap<String, Integer> iface = map;
        out("ifaceSize", iface.size());
    }

    private static void copyOnWrite() throws Exception {
        CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<Integer>(Arrays.asList(1, 2, 3));
        StringBuilder seen = new StringBuilder();
        for (Integer i : list) {
            list.add(i * 10);
            seen.append(i);
        }
        out("cowSnapshot", seen + "/" + list);
        out("addIfAbsent", list.addIfAbsent(1) + "/" + list.addIfAbsent(4));
        Iterator<Integer> it = list.iterator();
        it.next();
        try {
            it.remove();
            out("iterRemove", "allowed");
        } catch (UnsupportedOperationException e) {
            out("iterRemove", "uoe");
        }
        list.removeIf(x -> x > 9);
        out("removeIf", list);
        CopyOnWriteArraySet<String> set = new CopyOnWriteArraySet<String>();
        out("setAdd", set.add("b") + "/" + set.add("a") + "/" + set.add("b"));
        out("set", set);

        final CopyOnWriteArrayList<Integer> shared = new CopyOnWriteArrayList<Integer>();
        Thread[] threads = new Thread[4];
        for (int t = 0; t < threads.length; t++) {
            final int base = t * 100;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    shared.add(base + i);
                }
            });
            threads[t].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        List<Integer> sortedShared = new ArrayList<Integer>(shared);
        Collections.sort(sortedShared);
        out("cowThreads", shared.size() + "/" + sortedShared.get(0) + "/" + sortedShared.get(399));
    }

    private static void latchesAndScheduling() throws Exception {
        final CountDownLatch latch = new CountDownLatch(3);
        for (int i = 0; i < 3; i++) {
            new Thread(latch::countDown).start();
        }
        out("latch", latch.await(30, TimeUnit.SECONDS) + "/" + latch.getCount());
        out("latchTimeout", new CountDownLatch(1).await(10, TimeUnit.MILLISECONDS));

        ScheduledExecutorService sched = Executors.newScheduledThreadPool(1);
        long start = System.currentTimeMillis();
        ScheduledFuture<String> later = sched.schedule(() -> "scheduled", 50, TimeUnit.MILLISECONDS);
        out("scheduled", later.get());
        out("waited", System.currentTimeMillis() - start >= 40);
        final AtomicInteger ticks = new AtomicInteger();
        final CountDownLatch three = new CountDownLatch(3);
        ScheduledFuture<?> rate = sched.scheduleAtFixedRate(() -> {
            ticks.incrementAndGet();
            three.countDown();
        }, 0, 10, TimeUnit.MILLISECONDS);
        out("ticked", three.await(30, TimeUnit.SECONDS));
        out("rateCancel", rate.cancel(false) + "/" + rate.isCancelled());
        ScheduledFuture<?> far = sched.schedule(() -> { }, 1, TimeUnit.HOURS);
        out("delayPositive", far.getDelay(TimeUnit.MINUTES) > 50);
        far.cancel(false);
        sched.shutdown();
        out("schedTerminated", sched.awaitTermination(30, TimeUnit.SECONDS));
    }
}
