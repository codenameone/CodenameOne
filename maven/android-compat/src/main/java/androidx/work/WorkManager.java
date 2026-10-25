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
import com.codename1.androidcompat.runtime.AndroidRuntime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

/// AndroidX WorkManager over Codename One. See the engine notes in this
/// package for what runs where; in short, work runs in this process while
/// the application runs, is saved until it finishes, and work with a
/// charging, battery, idle or storage constraint is handed to Codename One's
/// `BackgroundWork` where the platform supports it.
///
/// The work saved by an earlier run is resumed the first time the
/// application asks for its `WorkManager`, which AndroidX does at start-up
/// on its own. Code that only reads work it enqueued in an earlier run should
/// call [#getInstance(Context)] from `Application.onCreate`.
public abstract class WorkManager {

    private static WorkManager instance;

    WorkManager() {
    }

    public static WorkManager getInstance(Context context) {
        if (instance == null) {
            Configuration config = null;
            Context app = context.getApplicationContext() == null ? context : context.getApplicationContext();
            if (app instanceof Configuration.Provider) {
                config = ((Configuration.Provider) app).getWorkManagerConfiguration();
            }
            initialize(context, config == null ? new Configuration.Builder().build() : config);
        }
        return instance;
    }

    @Deprecated
    public static WorkManager getInstance() {
        if (instance == null) {
            AndroidRuntime rt = AndroidRuntime.getInstance();
            if (rt == null) {
                throw new IllegalStateException("WorkManager is not initialized properly.");
            }
            return getInstance(rt.getApplication());
        }
        return instance;
    }

    public static void initialize(Context context, Configuration configuration) {
        if (instance != null) {
            throw new IllegalStateException("WorkManager is already initialized.");
        }
        instance = new Impl();
        WorkEngine.get().initialize(context, configuration);
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    /// Forgets the instance and the work in memory; for tests.
    static void resetForTesting() {
        instance = null;
        WorkEngine.resetForTesting();
    }

    static Operation completed() {
        return new Operation() {
            @Override
            public LiveData<State> getState() {
                return new MutableLiveData<State>(Operation.SUCCESS);
            }

            @Override
            public Future<State.SUCCESS> getResult() {
                return WorkEngine.done(Operation.SUCCESS);
            }
        };
    }

    // ------------------------------------------------------------- enqueue

    public final Operation enqueue(WorkRequest request) {
        return enqueue(Arrays.asList(request));
    }

    public Operation enqueue(List<? extends WorkRequest> requests) {
        for (int i = 0; i < requests.size(); i++) {
            List<WorkSpec> one = new ArrayList<WorkSpec>();
            one.add(requests.get(i).spec.copy());
            WorkEngine.get().enqueue(new WorkContinuation(this, one, one, null, null));
        }
        return completed();
    }

    public final WorkContinuation beginWith(OneTimeWorkRequest request) {
        return beginWith(Arrays.asList(request));
    }

    public WorkContinuation beginWith(List<OneTimeWorkRequest> requests) {
        return WorkContinuation.begin(this, requests, null, null);
    }

    public final WorkContinuation beginUniqueWork(String uniqueWorkName, ExistingWorkPolicy policy,
            OneTimeWorkRequest request) {
        return beginUniqueWork(uniqueWorkName, policy, Arrays.asList(request));
    }

    public WorkContinuation beginUniqueWork(String uniqueWorkName, ExistingWorkPolicy policy,
            List<OneTimeWorkRequest> requests) {
        return WorkContinuation.begin(this, requests, uniqueWorkName, policy);
    }

    public Operation enqueueUniqueWork(String uniqueWorkName, ExistingWorkPolicy policy, OneTimeWorkRequest request) {
        return enqueueUniqueWork(uniqueWorkName, policy, Arrays.asList(request));
    }

    public Operation enqueueUniqueWork(String uniqueWorkName, ExistingWorkPolicy policy,
            List<OneTimeWorkRequest> requests) {
        return beginUniqueWork(uniqueWorkName, policy, requests).enqueue();
    }

    public Operation enqueueUniquePeriodicWork(String uniqueWorkName, ExistingPeriodicWorkPolicy policy,
            PeriodicWorkRequest request) {
        WorkEngine.get().enqueueUniquePeriodic(uniqueWorkName, policy, request.spec.copy());
        return completed();
    }

    // -------------------------------------------------------------- cancel

    public Operation cancelWorkById(UUID id) {
        WorkEngine.get().cancelWhere(WorkEngine.byId(id.toString()));
        return completed();
    }

    public Operation cancelAllWorkByTag(String tag) {
        WorkEngine.get().cancelWhere(WorkEngine.byTag(tag));
        return completed();
    }

    public Operation cancelUniqueWork(String uniqueWorkName) {
        WorkEngine.get().cancelWhere(WorkEngine.byName(uniqueWorkName));
        return completed();
    }

    public Operation cancelAllWork() {
        WorkEngine.get().cancelWhere(WorkEngine.all());
        return completed();
    }

    public Operation pruneWork() {
        WorkEngine.get().prune();
        return completed();
    }

    // -------------------------------------------------------------- query

    public Future<WorkInfo> getWorkInfoById(UUID id) {
        return WorkEngine.get().infoLater(id.toString());
    }

    public LiveData<WorkInfo> getWorkInfoByIdLiveData(UUID id) {
        return WorkEngine.get().liveInfo(id.toString());
    }

    public Future<List<WorkInfo>> getWorkInfosByTag(String tag) {
        return WorkEngine.get().infosLater(WorkEngine.byTag(tag));
    }

    public LiveData<List<WorkInfo>> getWorkInfosByTagLiveData(String tag) {
        return WorkEngine.get().liveInfos("tag:" + tag, WorkEngine.byTag(tag));
    }

    public Future<List<WorkInfo>> getWorkInfosForUniqueWork(String uniqueWorkName) {
        return WorkEngine.get().infosLater(WorkEngine.byName(uniqueWorkName));
    }

    public LiveData<List<WorkInfo>> getWorkInfosForUniqueWorkLiveData(String uniqueWorkName) {
        return WorkEngine.get().liveInfos("name:" + uniqueWorkName, WorkEngine.byName(uniqueWorkName));
    }

    static final class Impl extends WorkManager {
    }
}
