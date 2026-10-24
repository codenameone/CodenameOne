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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.codename1.androidcompat.runtime.CompatReport;
import com.codename1.androidcompat.runtime.MainThread;
import com.codename1.androidcompat.runtime.WorkerInstantiator;
import com.codename1.background.BackgroundWork;
import com.codename1.ui.Display;
import com.codename1.util.Callback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.Timer;
import java.util.TimerTask;

/// The scheduler behind [WorkManager].
///
/// Every piece of state is owned by the main thread: an enqueue, a cancel or
/// a finished worker is handed to it, so nothing here needs a lock. Workers
/// run on the configured executor.
///
/// Work runs in this process while the application is running, as soon as
/// its delay has passed and its prerequisites succeeded. What has not
/// finished is saved by [WorkStore] and picked up again the next time the
/// application asks for its `WorkManager`. A request with a constraint only
/// the platform can judge -- charging, battery, idle, storage -- is handed to
/// Codename One's `BackgroundWork` where that is supported, and runs when the
/// platform says so (through [com.codename1.androidcompat.work.CompatBackgroundWorker]);
/// elsewhere those constraints are not checked. A network constraint is
/// treated as met: there is no portable way to wait for connectivity, and a
/// worker without it fails and asks to be retried, as it would on Android.
final class WorkEngine {

    static final String PLATFORM_PREFIX = "cn1compat.work:";
    static final String PLATFORM_KEY_ID = "id";

    private static WorkEngine instance;

    private final Map<String, WorkSpec> specs = new LinkedHashMap<String, WorkSpec>();
    private final Map<String, ListenableWorker> running = new HashMap<String, ListenableWorker>();
    private final Map<String, Query> queries = new LinkedHashMap<String, Query>();
    private final Map<String, List<Callback<Boolean>>> platformWaiters = new HashMap<String, List<Callback<Boolean>>>();
    private final Progress progress = new Progress();
    private Context appContext;
    private WorkerFactory factory;
    private Executor executor;
    private Timer timer;
    private TimerTask wake;
    private long wakeAt = Long.MAX_VALUE;

    private WorkEngine() {
    }

    static WorkEngine get() {
        if (instance == null) {
            instance = new WorkEngine();
        }
        return instance;
    }

    /// Forgets everything in memory; for tests, which start from a clean store.
    static void resetForTesting() {
        if (instance != null && instance.wake != null) {
            instance.wake.cancel();
        }
        if (instance != null && instance.timer != null) {
            instance.timer.cancel();
        }
        instance = null;
    }

    // ---------------------------------------------------------------- setup

    void initialize(Context context, Configuration config) {
        appContext = context.getApplicationContext() == null ? context : context.getApplicationContext();
        if (config != null) {
            factory = config.workerFactory;
            executor = config.executor;
        }
        if (executor == null) {
            // AndroidX's default pool is sized between two and four threads.
            executor = Executors.newFixedThreadPool(4);
        }
        onMain(new Runnable() {
            @Override
            public void run() {
                List<WorkSpec> saved = WorkStore.load();
                for (int i = 0; i < saved.size(); i++) {
                    WorkSpec s = saved.get(i);
                    if (specs.containsKey(s.id)) {
                        continue;
                    }
                    if (s.state == WorkInfo.State.RUNNING) {
                        // The process ended while it ran; Android runs it again.
                        s.state = WorkInfo.State.ENQUEUED;
                    }
                    specs.put(s.id, s);
                }
                changed();
            }
        });
    }

    boolean isInitialized() {
        return appContext != null;
    }

    // ------------------------------------------------------------ requests

    void enqueue(final WorkContinuation c) {
        onMain(new Runnable() {
            @Override
            public void run() {
                insertContinuation(c);
            }
        });
    }

    private void insertContinuation(WorkContinuation c) {
        List<WorkSpec> added = c.specs;
        String name = c.uniqueName;
        if (name != null) {
            List<WorkSpec> existing = named(name);
            boolean anyUnfinished = false;
            for (int i = 0; i < existing.size(); i++) {
                anyUnfinished |= !existing.get(i).state.isFinished();
            }
            ExistingWorkPolicy policy = c.policy;
            List<String> leaves = leaves(existing);
            if (policy == ExistingWorkPolicy.APPEND_OR_REPLACE) {
                boolean broken = false;
                for (int i = 0; i < leaves.size(); i++) {
                    WorkInfo.State st = specs.get(leaves.get(i)).state;
                    broken |= st == WorkInfo.State.FAILED || st == WorkInfo.State.CANCELLED;
                }
                policy = broken ? ExistingWorkPolicy.REPLACE : ExistingWorkPolicy.APPEND;
            }
            if (policy == ExistingWorkPolicy.KEEP && anyUnfinished) {
                return;
            }
            if (policy == ExistingWorkPolicy.REPLACE) {
                for (int i = 0; i < existing.size(); i++) {
                    cancel(existing.get(i));
                }
                // The replaced work is gone from the name, finished or not.
                for (int i = 0; i < existing.size(); i++) {
                    existing.get(i).uniqueName = null;
                }
            } else if (!anyUnfinished && policy != ExistingWorkPolicy.APPEND) {
                // KEEP with nothing pending: the finished work leaves the name,
                // so a later APPEND does not wait on it.
                for (int i = 0; i < existing.size(); i++) {
                    existing.get(i).uniqueName = null;
                }
            } else if (policy == ExistingWorkPolicy.APPEND) {
                // Finished leaves stay prerequisites: a failed or cancelled
                // one fails or cancels what is appended, as on Android.
                for (int i = 0; i < added.size(); i++) {
                    WorkSpec s = added.get(i);
                    if (s.prerequisites.isEmpty()) {
                        s.prerequisites.addAll(leaves);
                    }
                }
            }
            for (int i = 0; i < added.size(); i++) {
                added.get(i).uniqueName = name;
            }
        }
        long now = System.currentTimeMillis();
        for (int i = 0; i < added.size(); i++) {
            WorkSpec s = added.get(i);
            s.state = s.prerequisites.isEmpty() ? WorkInfo.State.ENQUEUED : WorkInfo.State.BLOCKED;
            s.nextRunAt = now + s.initialDelayMillis;
            specs.put(s.id, s);
        }
        for (int i = 0; i < added.size(); i++) {
            WorkSpec s = added.get(i);
            if (s.state == WorkInfo.State.BLOCKED) {
                resolveBlocked(s, now);
            } else {
                platformSchedule(s);
            }
        }
        changed();
    }

    void enqueueUniquePeriodic(final String name, final ExistingPeriodicWorkPolicy policy, final WorkSpec spec) {
        onMain(new Runnable() {
            @Override
            public void run() {
                List<WorkSpec> existing = named(name);
                WorkSpec live = null;
                for (int i = 0; i < existing.size(); i++) {
                    if (!existing.get(i).state.isFinished()) {
                        live = existing.get(i);
                    }
                }
                if (live != null && policy == ExistingPeriodicWorkPolicy.KEEP) {
                    return;
                }
                if (live != null && policy == ExistingPeriodicWorkPolicy.UPDATE) {
                    // The request keeps its id, state and schedule; what it
                    // runs and how changes, as a new generation.
                    live.workerClassName = spec.workerClassName;
                    live.tags.clear();
                    live.tags.addAll(spec.tags);
                    live.input = spec.input;
                    live.constraints = spec.constraints;
                    live.intervalMillis = spec.intervalMillis;
                    live.flexMillis = spec.flexMillis;
                    live.backoffPolicy = spec.backoffPolicy;
                    live.backoffDelayMillis = spec.backoffDelayMillis;
                    live.generation++;
                    platformSchedule(live);
                    changed();
                    return;
                }
                for (int i = 0; i < existing.size(); i++) {
                    cancel(existing.get(i));
                    existing.get(i).uniqueName = null;
                }
                List<WorkSpec> one = new ArrayList<WorkSpec>();
                one.add(spec);
                WorkContinuation c = new WorkContinuation(null, one, one, name, ExistingWorkPolicy.KEEP);
                insertContinuation(c);
            }
        });
    }

    // -------------------------------------------------------------- cancel

    void cancelWhere(final Matcher m) {
        onMain(new Runnable() {
            @Override
            public void run() {
                List<WorkSpec> all = new ArrayList<WorkSpec>(specs.values());
                for (int i = 0; i < all.size(); i++) {
                    if (m.matches(all.get(i))) {
                        cancel(all.get(i));
                    }
                }
                changed();
            }
        });
    }

    void prune() {
        onMain(new Runnable() {
            @Override
            public void run() {
                for (Iterator<WorkSpec> it = specs.values().iterator(); it.hasNext();) {
                    WorkSpec s = it.next();
                    if (s.state.isFinished() && !hasUnfinishedDependent(s.id)) {
                        it.remove();
                    }
                }
                changed();
            }
        });
    }

    private void cancel(WorkSpec s) {
        if (s.state.isFinished()) {
            return;
        }
        ListenableWorker w = running.remove(s.id);
        if (w != null) {
            w.stop();
        }
        s.state = WorkInfo.State.CANCELLED;
        platformCancel(s);
        settleWaiters(s.id, true);
        cascade(s.id, WorkInfo.State.CANCELLED);
    }

    private void cascade(String id, WorkInfo.State st) {
        List<WorkSpec> all = new ArrayList<WorkSpec>(specs.values());
        for (int i = 0; i < all.size(); i++) {
            WorkSpec d = all.get(i);
            if (!d.state.isFinished() && d.prerequisites.contains(id)) {
                d.state = st;
                platformCancel(d);
                cascade(d.id, st);
            }
        }
    }

    // --------------------------------------------------------------- queries

    interface Matcher {
        boolean matches(WorkSpec s);
    }

    static Matcher byId(final String id) {
        return new Matcher() {
            @Override
            public boolean matches(WorkSpec s) {
                return s.id.equals(id);
            }
        };
    }

    static Matcher byTag(final String tag) {
        return new Matcher() {
            @Override
            public boolean matches(WorkSpec s) {
                return s.tags.contains(tag);
            }
        };
    }

    static Matcher byName(final String name) {
        return new Matcher() {
            @Override
            public boolean matches(WorkSpec s) {
                return name.equals(s.uniqueName);
            }
        };
    }

    static Matcher all() {
        return new Matcher() {
            @Override
            public boolean matches(WorkSpec s) {
                return true;
            }
        };
    }

    List<WorkInfo> infos(Matcher m) {
        List<WorkInfo> out = new ArrayList<WorkInfo>();
        for (WorkSpec s : specs.values()) {
            if (m.matches(s)) {
                out.add(s.toInfo());
            }
        }
        return out;
    }

    WorkInfo info(String id) {
        WorkSpec s = specs.get(id);
        return s == null ? null : s.toInfo();
    }

    Future<List<WorkInfo>> infosLater(final Matcher m) {
        return later(new Callable<List<WorkInfo>>() {
            @Override
            public List<WorkInfo> call() {
                return infos(m);
            }
        });
    }

    Future<WorkInfo> infoLater(final String id) {
        return later(new Callable<WorkInfo>() {
            @Override
            public WorkInfo call() {
                return info(id);
            }
        });
    }

    @SuppressWarnings("unchecked")
    LiveData<List<WorkInfo>> liveInfos(String key, Matcher m) {
        Query q = queries.get(key);
        if (q == null) {
            q = new Query(m, false);
            queries.put(key, q);
            refresh(q);
        }
        return (LiveData<List<WorkInfo>>) (LiveData<?>) q.live;
    }

    @SuppressWarnings("unchecked")
    LiveData<WorkInfo> liveInfo(String id) {
        String key = "id:" + id;
        Query q = queries.get(key);
        if (q == null) {
            q = new Query(byId(id), true);
            queries.put(key, q);
            refresh(q);
        }
        return (LiveData<WorkInfo>) (LiveData<?>) q.live;
    }

    static final class Query {
        final Matcher matcher;
        final boolean single;
        final MutableLiveData<Object> live = new MutableLiveData<Object>();
        Object last;
        boolean published;

        Query(Matcher matcher, boolean single) {
            this.matcher = matcher;
            this.single = single;
        }
    }

    private void refresh(Query q) {
        List<WorkInfo> now = infos(q.matcher);
        Object v = q.single ? (now.isEmpty() ? null : now.get(0)) : now;
        if (q.published && (q.last == null ? v == null : q.last.equals(v))) {
            return;
        }
        q.published = true;
        q.last = v;
        q.live.setValue(v);
    }

    // ----------------------------------------------------------- scheduling

    /// Everything after a change: persist, tell observers, run what is due.
    private void changed() {
        persist();
        for (Query q : queries.values()) {
            refresh(q);
        }
        pump();
    }

    private void pump() {
        long now = System.currentTimeMillis();
        long earliest = Long.MAX_VALUE;
        List<WorkSpec> all = new ArrayList<WorkSpec>(specs.values());
        boolean ran = false;
        for (int i = 0; i < all.size(); i++) {
            WorkSpec s = all.get(i);
            if (s.state != WorkInfo.State.ENQUEUED || running.containsKey(s.id) || deferToPlatform(s)) {
                continue;
            }
            if (s.nextRunAt <= now) {
                start(s, null);
                ran = true;
            } else {
                earliest = Math.min(earliest, s.nextRunAt);
            }
        }
        if (ran) {
            persist();
            for (Query q : queries.values()) {
                refresh(q);
            }
        }
        wakeAt(earliest);
    }

    private void wakeAt(long at) {
        if (at == wakeAt) {
            return;
        }
        if (wake != null) {
            wake.cancel();
            wake = null;
        }
        wakeAt = at;
        if (at == Long.MAX_VALUE) {
            return;
        }
        if (timer == null) {
            timer = new Timer();
        }
        wake = new TimerTask() {
            @Override
            public void run() {
                MainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        wakeAt = Long.MAX_VALUE;
                        wake = null;
                        pump();
                    }
                });
            }
        };
        timer.schedule(wake, Math.max(0, at - System.currentTimeMillis()));
    }

    /// Starts `s`. Answers false when no worker could be made, in which
    /// case the request has failed.
    private boolean start(final WorkSpec s, Callback<Boolean> platform) {
        if (platform != null) {
            addWaiter(s.id, platform);
        }
        WorkerParameters params = new WorkerParameters(UUID.fromString(s.id), s.input,
                new java.util.LinkedHashSet<String>(s.tags), s.runAttemptCount, s.generation, executor, progress);
        final ListenableWorker w = createWorker(s.workerClassName, params);
        if (w == null || w.markUsed()) {
            CompatReport.unsupported("WorkManager", s.workerClassName
                    + " (no public (Context, WorkerParameters) constructor, and no WorkerFactory made one)");
            finish(s, new ListenableWorker.Result.Failure());
            return false;
        }
        s.state = WorkInfo.State.RUNNING;
        running.put(s.id, w);
        final String id = s.id;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Object got = null;
                try {
                    // Read as Object: the generic get() casts to Result, and a
                    // cast never fails on ParparVM, so the type is checked
                    // with instanceof below instead.
                    Future<?> f = w.startWork();
                    got = f == null ? null : f.get();
                } catch (Throwable t) {
                    com.codename1.io.Log.e(t);
                }
                ListenableWorker.Result r = got instanceof ListenableWorker.Result
                        ? (ListenableWorker.Result) got : null;
                final ListenableWorker.Result result = r == null ? new ListenableWorker.Result.Failure() : r;
                MainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        completed(id, w, result);
                    }
                });
            }
        });
        return true;
    }

    private ListenableWorker createWorker(String className, WorkerParameters params) {
        if (factory != null) {
            ListenableWorker w = factory.createWorker(appContext, className, params);
            if (w != null) {
                return w;
            }
        }
        Object o = WorkerInstantiator.create(className, appContext, params);
        return o instanceof ListenableWorker ? (ListenableWorker) o : null;
    }

    private void completed(String id, ListenableWorker w, ListenableWorker.Result r) {
        if (running.get(id) != w) {
            // Stopped by a cancel or a replace; its result means nothing.
            return;
        }
        running.remove(id);
        WorkSpec s = specs.get(id);
        if (s == null || s.state != WorkInfo.State.RUNNING) {
            return;
        }
        finish(s, r);
        changed();
    }

    private void finish(WorkSpec s, ListenableWorker.Result r) {
        long now = System.currentTimeMillis();
        s.progress = Data.EMPTY;
        if (r instanceof ListenableWorker.Result.Retry) {
            s.runAttemptCount++;
            s.state = WorkInfo.State.ENQUEUED;
            s.nextRunAt = now + s.backoffMillis();
            settleWaiters(s.id, false);
            return;
        }
        settleWaiters(s.id, true);
        if (s.isPeriodic()) {
            // A periodic request never finishes: success or failure, it waits
            // for its next period.
            s.runAttemptCount = 0;
            s.state = WorkInfo.State.ENQUEUED;
            s.nextRunAt = now + s.intervalMillis;
            return;
        }
        platformCancel(s);
        if (r instanceof ListenableWorker.Result.Success) {
            s.state = WorkInfo.State.SUCCEEDED;
            s.output = r.getOutputData();
            List<WorkSpec> all = new ArrayList<WorkSpec>(specs.values());
            for (int i = 0; i < all.size(); i++) {
                WorkSpec d = all.get(i);
                if (d.state == WorkInfo.State.BLOCKED && d.prerequisites.contains(s.id)) {
                    resolveBlocked(d, now);
                }
            }
        } else {
            s.state = WorkInfo.State.FAILED;
            s.output = r.getOutputData();
            cascade(s.id, WorkInfo.State.FAILED);
        }
    }

    /// Unblocks `d` once every prerequisite succeeded, merging their outputs
    /// over its own input as AndroidX's `OverwritingInputMerger` does; fails
    /// or cancels it as soon as one did.
    private void resolveBlocked(WorkSpec d, long now) {
        Map<String, Object> merged = new HashMap<String, Object>(d.input.values);
        for (int i = 0; i < d.prerequisites.size(); i++) {
            WorkSpec p = specs.get(d.prerequisites.get(i));
            if (p == null) {
                // Pruned, which only happens to finished work.
                continue;
            }
            if (p.state == WorkInfo.State.FAILED || p.state == WorkInfo.State.CANCELLED) {
                d.state = p.state;
                cascade(d.id, p.state);
                return;
            }
            if (p.state != WorkInfo.State.SUCCEEDED) {
                return;
            }
            merged.putAll(p.output.values);
        }
        d.input = new Data(merged);
        d.state = WorkInfo.State.ENQUEUED;
        d.nextRunAt = now + d.initialDelayMillis;
        platformSchedule(d);
    }

    // ------------------------------------------------------------- platform

    private static boolean platformAvailable() {
        return Display.isInitialized() && BackgroundWork.isSupported();
    }

    private static boolean deferToPlatform(WorkSpec s) {
        return s.constraints.needsPlatformScheduler() && platformAvailable();
    }

    private void platformSchedule(WorkSpec s) {
        if (!deferToPlatform(s) || s.state != WorkInfo.State.ENQUEUED) {
            return;
        }
        Constraints c = s.constraints;
        com.codename1.background.WorkRequest.Builder b = com.codename1.background.WorkRequest.builder(
                PLATFORM_PREFIX + s.id, com.codename1.androidcompat.work.CompatBackgroundWorker.class)
                .setRequiresNetwork(c.networkType != NetworkType.NOT_REQUIRED)
                .setRequiresUnmeteredNetwork(c.networkType == NetworkType.UNMETERED)
                .setRequiresCharging(c.requiresCharging)
                .setRequiresIdle(c.requiresDeviceIdle)
                .setRequiresBatteryNotLow(c.requiresBatteryNotLow)
                .setInitialDelay(Math.max(0, s.nextRunAt - System.currentTimeMillis()))
                .putInputData(PLATFORM_KEY_ID, s.id);
        if (s.isPeriodic()) {
            b.setPeriodic(s.intervalMillis);
        }
        BackgroundWork.schedule(b.build());
    }

    private static void platformCancel(WorkSpec s) {
        if (s.constraints.needsPlatformScheduler() && platformAvailable()) {
            BackgroundWork.cancel(PLATFORM_PREFIX + s.id);
        }
    }

    /// Runs `id` because the platform scheduler says its constraints hold.
    /// `done` hears true once the attempt finished and false when it should
    /// be retried. Work that is already running is not started twice.
    void runForPlatform(final String id, final Callback<Boolean> done) {
        onMain(new Runnable() {
            @Override
            public void run() {
                WorkSpec s = specs.get(id);
                if (s == null || s.state.isFinished()) {
                    done.onSucess(Boolean.TRUE);
                    return;
                }
                if (s.state == WorkInfo.State.RUNNING) {
                    addWaiter(id, done);
                    return;
                }
                if (s.state != WorkInfo.State.ENQUEUED) {
                    done.onSucess(Boolean.FALSE);
                    return;
                }
                start(s, done);
                changed();
            }
        });
    }

    private void addWaiter(String id, Callback<Boolean> cb) {
        List<Callback<Boolean>> l = platformWaiters.get(id);
        if (l == null) {
            l = new ArrayList<Callback<Boolean>>();
            platformWaiters.put(id, l);
        }
        l.add(cb);
    }

    private void settleWaiters(String id, boolean finished) {
        List<Callback<Boolean>> l = platformWaiters.remove(id);
        if (l != null) {
            for (int i = 0; i < l.size(); i++) {
                l.get(i).onSucess(Boolean.valueOf(finished));
            }
        }
    }

    // ---------------------------------------------------------------- store

    private void persist() {
        List<WorkSpec> keep = new ArrayList<WorkSpec>();
        for (WorkSpec s : specs.values()) {
            if (!s.state.isFinished() || hasUnfinishedDependent(s.id)) {
                keep.add(s);
            }
        }
        WorkStore.save(keep);
    }

    private boolean hasUnfinishedDependent(String id) {
        for (WorkSpec d : specs.values()) {
            if (!d.state.isFinished() && d.prerequisites.contains(id)) {
                return true;
            }
        }
        return false;
    }

    private List<WorkSpec> named(String name) {
        List<WorkSpec> out = new ArrayList<WorkSpec>();
        for (WorkSpec s : specs.values()) {
            if (name.equals(s.uniqueName)) {
                out.add(s);
            }
        }
        return out;
    }

    /// The members of a unique chain nothing else in it waits for.
    private static List<String> leaves(List<WorkSpec> chain) {
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < chain.size(); i++) {
            String id = chain.get(i).id;
            boolean waitedFor = false;
            for (int j = 0; j < chain.size() && !waitedFor; j++) {
                waitedFor = chain.get(j).prerequisites.contains(id);
            }
            if (!waitedFor) {
                out.add(id);
            }
        }
        return out;
    }

    // -------------------------------------------------------------- threads

    static void onMain(Runnable r) {
        if (MainThread.isMainThread()) {
            r.run();
        } else {
            MainThread.post(r);
        }
    }

    static <T> Future<T> later(Callable<T> c) {
        FutureTask<T> t = new FutureTask<T>(c);
        onMain(t);
        return t;
    }

    static <T> Future<T> done(final T value) {
        FutureTask<T> t = new FutureTask<T>(new Callable<T>() {
            @Override
            public T call() {
                return value;
            }
        });
        t.run();
        return t;
    }

    /// Where `setProgressAsync` reports to, from the worker's thread.
    final class Progress {
        void set(final UUID id, final Data data) {
            onMain(new Runnable() {
                @Override
                public void run() {
                    WorkSpec s = specs.get(id.toString());
                    if (s != null && s.state == WorkInfo.State.RUNNING) {
                        s.progress = data == null ? Data.EMPTY : data;
                        for (Query q : queries.values()) {
                            refresh(q);
                        }
                    }
                }
            });
        }
    }
}
