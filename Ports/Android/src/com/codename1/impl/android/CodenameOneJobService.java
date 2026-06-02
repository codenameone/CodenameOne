/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.android;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.PersistableBundle;
import android.util.Log;
import com.codename1.background.BackgroundWorker;
import com.codename1.ui.Display;
import java.util.HashMap;
import java.util.Map;

/// JobScheduler entry point for constraint-aware background work scheduled through
/// `com.codename1.background.BackgroundWork`. A fresh CN1 context is started if the app
/// process was not already running, the worker class is reconstructed reflectively and
/// run, and the job is then finished (rescheduled when the worker requests a retry).
///
/// One-shot processing tasks scheduled via `com.codename1.background.BackgroundTask` carry
/// a live Runnable in a static registry; if the process was killed and cold launched the
/// runnable is gone and the job is a no-op (use a BackgroundWorker for cold-launch safe
/// work).
public class CodenameOneJobService extends JobService {

    static final String EXTRA_WORKER_CLASS = "cn1.workerClass";
    static final String EXTRA_WORK_ID = "cn1.workId";
    static final String EXTRA_PROCESSING_ID = "cn1.processingId";
    static final String INPUT_PREFIX = "cn1.in.";

    private static final Map<String, Runnable> PROCESSING_RUNNABLES = new HashMap<String, Runnable>();

    static void registerProcessingRunnable(String id, Runnable r) {
        synchronized (PROCESSING_RUNNABLES) {
            PROCESSING_RUNNABLES.put(id, r);
        }
    }

    static void unregisterProcessingRunnable(String id) {
        synchronized (PROCESSING_RUNNABLES) {
            PROCESSING_RUNNABLES.remove(id);
        }
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        final PersistableBundle extras = params.getExtras();
        final String processingId = extras == null ? null : extras.getString(EXTRA_PROCESSING_ID);
        if (processingId != null) {
            Runnable r;
            synchronized (PROCESSING_RUNNABLES) {
                r = PROCESSING_RUNNABLES.remove(processingId);
            }
            if (r == null) {
                Log.d("CN1", "Background processing task '" + processingId + "' has no live runnable (process was relaunched); skipping");
                return false;
            }
            final Runnable task = r;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        task.run();
                    } catch (Throwable t) {
                        Log.e("CN1", "background processing error", t);
                    } finally {
                        jobFinished(params, false);
                    }
                }
            }, "cn1-background-processing").start();
            return true;
        }

        final String workerClass = extras == null ? null : extras.getString(EXTRA_WORKER_CLASS);
        final String workId = extras == null ? null : extras.getString(EXTRA_WORK_ID);
        if (workerClass == null) {
            return false;
        }
        final Map<String, String> input = new HashMap<String, String>();
        if (extras != null) {
            for (String key : extras.keySet()) {
                if (key.startsWith(INPUT_PREFIX)) {
                    input.put(key.substring(INPUT_PREFIX.length()), extras.getString(key));
                }
            }
        }

        new Thread(new Runnable() {
            public void run() {
                boolean startedContext = false;
                if (!Display.isInitialized()) {
                    startedContext = true;
                    AndroidImplementation.startContext(CodenameOneJobService.this);
                }
                final boolean fStarted = startedContext;
                final boolean[] retry = new boolean[]{false};
                final Object lock = new Object();
                final boolean[] done = new boolean[]{false};
                try {
                    Class<?> cls = Class.forName(workerClass);
                    BackgroundWorker worker = (BackgroundWorker) cls.newInstance();
                    long deadline = System.currentTimeMillis() + 9 * 60 * 1000L;
                    worker.performWork(workId, input, deadline, new com.codename1.util.Callback<Boolean>() {
                        public void onSucess(Boolean value) {
                            synchronized (lock) {
                                retry[0] = (value != null && !value.booleanValue());
                                done[0] = true;
                                lock.notifyAll();
                            }
                        }
                        public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                            Log.e("CN1", "background worker error", err);
                            synchronized (lock) {
                                retry[0] = true;
                                done[0] = true;
                                lock.notifyAll();
                            }
                        }
                    });
                    synchronized (lock) {
                        long waitUntil = System.currentTimeMillis() + 9 * 60 * 1000L;
                        while (!done[0] && System.currentTimeMillis() < waitUntil) {
                            try {
                                lock.wait(waitUntil - System.currentTimeMillis());
                            } catch (InterruptedException ie) {
                                break;
                            }
                        }
                    }
                } catch (Throwable t) {
                    Log.e("CN1", "Failed to run background worker " + workerClass, t);
                } finally {
                    if (fStarted) {
                        AndroidImplementation.stopContext(CodenameOneJobService.this);
                    }
                    jobFinished(params, retry[0]);
                }
            }
        }, "cn1-background-work").start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // request a reschedule if the system stopped us early
        return true;
    }
}
