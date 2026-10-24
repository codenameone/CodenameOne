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
package androidx.work;

import android.content.Context;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.io.Storage;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WorkManagerTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    /// What each worker saw, in the order they ran.
    static final List<String> RAN = Collections.synchronizedList(new ArrayList<String>());

    public static final class Echo extends Worker {
        public Echo(Context c, WorkerParameters p) {
            super(c, p);
        }

        @Override
        public Result doWork() {
            String in = getInputData().getString("in");
            RAN.add("echo:" + in);
            if ("fail".equals(in)) {
                return Result.failure();
            }
            if ("retry".equals(in)) {
                return Result.retry();
            }
            return Result.success(new Data.Builder().putString("in", in + "+").putInt("n", 7).build());
        }
    }

    private Context context;

    @Before
    public void setUp() {
        context = AndroidTestSupport.context();
        WorkManager.resetForTesting();
        Storage.getInstance().deleteStorageFile(WorkStore.FILE);
        Storage.getInstance().deleteStorageFile(WorkStore.TEMP);
        RAN.clear();
    }

    @After
    public void tearDown() {
        WorkManager.resetForTesting();
        Storage.getInstance().deleteStorageFile(WorkStore.FILE);
        Storage.getInstance().deleteStorageFile(WorkStore.TEMP);
    }

    private WorkManager manager() {
        WorkerFactory f = new WorkerFactory() {
            @Override
            public ListenableWorker createWorker(Context c, String name, WorkerParameters p) {
                return Echo.class.getName().equals(name) ? new Echo(c, p) : null;
            }
        };
        Executor inline = new Executor() {
            @Override
            public void execute(Runnable r) {
                r.run();
            }
        };
        WorkManager.initialize(context, new Configuration.Builder().setWorkerFactory(f).setExecutor(inline).build());
        return WorkManager.getInstance(context);
    }

    private static OneTimeWorkRequest echo(String in) {
        return new OneTimeWorkRequest.Builder(Echo.class).setInputData(new Data.Builder().putString("in", in).build())
                .addTag("t").build();
    }

    private static WorkInfo.State state(WorkManager wm, OneTimeWorkRequest r) throws Exception {
        MainThreadRule.drain();
        MainThreadRule.drain();
        return wm.getWorkInfoById(r.getId()).get().getState();
    }

    @Test
    public void oneTimeWorkRunsAndReportsItsOutput() throws Exception {
        WorkManager wm = manager();
        OneTimeWorkRequest r = echo("a");
        wm.enqueue(r);
        assertEquals(WorkInfo.State.SUCCEEDED, state(wm, r));
        WorkInfo info = wm.getWorkInfoById(r.getId()).get();
        assertEquals("a+", info.getOutputData().getString("in"));
        assertEquals(7, info.getOutputData().getInt("n", 0));
        assertTrue(info.getTags().contains(Echo.class.getName()));
        assertEquals(1, wm.getWorkInfosByTag("t").get().size());
        assertEquals(WorkInfo.State.SUCCEEDED, wm.getWorkInfoByIdLiveData(r.getId()).getValue().getState());
    }

    @Test
    public void aChainMergesOutputsAndAFailureCascades() throws Exception {
        WorkManager wm = manager();
        OneTimeWorkRequest a = echo("a");
        OneTimeWorkRequest b = new OneTimeWorkRequest.Builder(Echo.class).build();
        wm.beginWith(a).then(b).enqueue();
        assertEquals(WorkInfo.State.SUCCEEDED, state(wm, b));
        // b had no input of its own: it ran with a's output.
        assertEquals("a+", RAN.get(1).substring("echo:".length()));

        OneTimeWorkRequest f = echo("fail");
        OneTimeWorkRequest after = echo("never");
        wm.beginWith(f).then(after).enqueue();
        assertEquals(WorkInfo.State.FAILED, state(wm, f));
        assertEquals(WorkInfo.State.FAILED, state(wm, after));
        assertTrue(!RAN.contains("echo:never"));
    }

    @Test
    public void retryBacksOffAndCountsTheAttempt() throws Exception {
        WorkManager wm = manager();
        OneTimeWorkRequest r = new OneTimeWorkRequest.Builder(Echo.class)
                .setInputData(new Data.Builder().putString("in", "retry").build())
                .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.HOURS).build();
        long before = System.currentTimeMillis();
        wm.enqueue(r);
        assertEquals(WorkInfo.State.ENQUEUED, state(wm, r));
        WorkInfo info = wm.getWorkInfoById(r.getId()).get();
        assertEquals(1, info.getRunAttemptCount());
        assertTrue(info.getNextScheduleTimeMillis() >= before + TimeUnit.HOURS.toMillis(1));
        wm.cancelWorkById(r.getId());
        assertEquals(WorkInfo.State.CANCELLED, state(wm, r));
    }

    @Test
    public void uniqueWorkPolicies() throws Exception {
        WorkManager wm = manager();
        OneTimeWorkRequest delayed = new OneTimeWorkRequest.Builder(Echo.class)
                .setInitialDelay(1, TimeUnit.HOURS).build();
        wm.enqueueUniqueWork("u", ExistingWorkPolicy.KEEP, delayed);
        OneTimeWorkRequest kept = echo("kept");
        wm.enqueueUniqueWork("u", ExistingWorkPolicy.KEEP, kept);
        MainThreadRule.drain();
        assertEquals(null, wm.getWorkInfoById(kept.getId()).get());

        OneTimeWorkRequest appended = echo("appended");
        wm.enqueueUniqueWork("u", ExistingWorkPolicy.APPEND, appended);
        assertEquals(WorkInfo.State.BLOCKED, state(wm, appended));

        OneTimeWorkRequest replacement = echo("replacement");
        wm.enqueueUniqueWork("u", ExistingWorkPolicy.REPLACE, replacement);
        assertEquals(WorkInfo.State.SUCCEEDED, state(wm, replacement));
        assertEquals(WorkInfo.State.CANCELLED, state(wm, delayed));
        assertEquals(WorkInfo.State.CANCELLED, state(wm, appended));
        assertEquals(1, wm.getWorkInfosForUniqueWork("u").get().size());
    }

    @Test
    public void unfinishedWorkSurvivesARestart() throws Exception {
        WorkManager wm = manager();
        OneTimeWorkRequest later = new OneTimeWorkRequest.Builder(Echo.class).addTag("persist")
                .setInputData(new Data.Builder().putString("in", "x").putIntArray("arr", new int[] {1, 2}).build())
                .setInitialDelay(1, TimeUnit.HOURS).build();
        wm.enqueue(later);
        assertEquals(WorkInfo.State.ENQUEUED, state(wm, later));

        WorkManager.resetForTesting();
        WorkManager again = manager();
        List<WorkInfo> infos = again.getWorkInfosByTag("persist").get();
        assertEquals(1, infos.size());
        assertEquals(later.getId(), infos.get(0).getId());
        assertEquals(WorkInfo.State.ENQUEUED, infos.get(0).getState());
        assertTrue(RAN.isEmpty());
    }

    /// A save cut short while the main entry was being replaced leaves it
    /// truncated; the queue is read from the second entry, which was
    /// written whole first, rather than lost.
    @Test
    public void aTruncatedSaveFallsBackToTheCompleteCopy() throws Exception {
        WorkManager wm = manager();
        OneTimeWorkRequest later = new OneTimeWorkRequest.Builder(Echo.class).addTag("persist")
                .setInitialDelay(1, TimeUnit.HOURS).build();
        wm.enqueue(later);
        assertEquals(WorkInfo.State.ENQUEUED, state(wm, later));
        WorkManager.resetForTesting();

        Storage st = Storage.getInstance();
        byte[] whole;
        java.io.InputStream in = st.createInputStream(WorkStore.FILE);
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[512];
        int n;
        while ((n = in.read(buf)) > 0) {
            bo.write(buf, 0, n);
        }
        in.close();
        whole = bo.toByteArray();
        java.io.OutputStream o = st.createOutputStream(WorkStore.TEMP);
        o.write(whole);
        o.close();
        o = st.createOutputStream(WorkStore.FILE);
        o.write(whole, 0, whole.length / 2);
        o.close();
        st.clearCache();

        List<WorkInfo> infos = manager().getWorkInfosByTag("persist").get();
        assertEquals(1, infos.size());
        assertEquals(later.getId(), infos.get(0).getId());
    }

    /// Initialized off the main thread, the saved queue is merged before the
    /// first enqueue made on the main thread, which used to save over it.
    @Test
    public void anEnqueueBeforeTheLoadDoesNotOverwriteTheSavedQueue() throws Exception {
        WorkManager wm = manager();
        OneTimeWorkRequest first = new OneTimeWorkRequest.Builder(Echo.class).addTag("persist")
                .setInitialDelay(1, TimeUnit.HOURS).build();
        wm.enqueue(first);
        assertEquals(WorkInfo.State.ENQUEUED, state(wm, first));
        WorkManager.resetForTesting();

        final WorkManager[] off = new WorkManager[1];
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                off[0] = manager();
            }
        });
        t.start();
        t.join();
        OneTimeWorkRequest second = new OneTimeWorkRequest.Builder(Echo.class).addTag("persist")
                .setInitialDelay(1, TimeUnit.HOURS).build();
        off[0].enqueue(second);
        List<WorkInfo> infos = off[0].getWorkInfosByTag("persist").get();
        assertEquals(2, infos.size());
    }

    @Test
    public void aShortDelayRunsWithoutAnotherTrigger() throws Exception {
        WorkManager wm = manager();
        OneTimeWorkRequest r = new OneTimeWorkRequest.Builder(Echo.class)
                .setInitialDelay(30, TimeUnit.MILLISECONDS).build();
        wm.enqueue(r);
        long until = System.currentTimeMillis() + 5000;
        while (state(wm, r) != WorkInfo.State.SUCCEEDED && System.currentTimeMillis() < until) {
            Thread.sleep(10);
        }
        assertEquals(WorkInfo.State.SUCCEEDED, state(wm, r));
    }

    @Test
    public void dataRoundTripsAndEnforcesItsLimit() throws Exception {
        Data d = new Data.Builder().putBoolean("b", true).putLong("l", 5L).putDouble("d", 1.5)
                .putStringArray("s", new String[] {"x", null}).putByteArray("y", new byte[] {1, 2}).build();
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        WorkStore.writeData(new java.io.DataOutputStream(bo), d);
        Data back = WorkStore.readData(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bo.toByteArray())));
        assertEquals(d, back);
        try {
            new Data.Builder().putByteArray("big", new byte[Data.MAX_DATA_BYTES + 1]).build();
            throw new AssertionError("oversized data accepted");
        } catch (IllegalStateException expected) {
            // as on Android
        }
    }
}
