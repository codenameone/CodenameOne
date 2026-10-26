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

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

/// The base of every worker. [#startWork()] is called on a background thread
/// and its future is waited for there.
///
/// AndroidX returns Guava's `ListenableFuture` from `startWork`,
/// `setProgressAsync` and `setForegroundAsync`. Guava is not part of this
/// runtime, so they return `java.util.concurrent.Future`: a [Worker] is
/// unaffected, and a direct subclass must return a plain future.
public abstract class ListenableWorker {

    private final Context appContext;
    private final WorkerParameters params;
    private boolean stopped;
    private boolean used;

    public ListenableWorker(Context appContext, WorkerParameters workerParams) {
        if (appContext == null) {
            throw new IllegalArgumentException("Application Context is null");
        }
        if (workerParams == null) {
            throw new IllegalArgumentException("WorkerParameters is null");
        }
        this.appContext = appContext;
        this.params = workerParams;
    }

    public final Context getApplicationContext() {
        return appContext;
    }

    public final UUID getId() {
        return params.getId();
    }

    public final Data getInputData() {
        return params.getInputData();
    }

    public final Set<String> getTags() {
        return params.getTags();
    }

    public final int getRunAttemptCount() {
        return params.getRunAttemptCount();
    }

    public abstract Future<Result> startWork();

    public final boolean isStopped() {
        return stopped;
    }

    public void onStopped() {
    }

    public Future<Void> setProgressAsync(Data data) {
        params.progress.set(getId(), data);
        return WorkEngine.done(null);
    }

    /// Runs the rest of this work under a foreground notification, which on
    /// this runtime is the notification itself: posted through
    /// `NotificationManager`, so a long worker is visible as on Android.
    public final Future<Void> setForegroundAsync(ForegroundInfo info) {
        android.app.NotificationManager.getInstance().notify(info.getNotificationId(), info.getNotification());
        return WorkEngine.done(null);
    }

    final void stop() {
        stopped = true;
        onStopped();
    }

    final boolean markUsed() {
        boolean was = used;
        used = true;
        return was;
    }

    public abstract static class Result {

        Result() {
        }

        public static Result success() {
            return new Success(Data.EMPTY);
        }

        public static Result success(Data outputData) {
            return new Success(outputData);
        }

        public static Result retry() {
            return new Retry();
        }

        public static Result failure() {
            return new Failure(Data.EMPTY);
        }

        public static Result failure(Data outputData) {
            return new Failure(outputData);
        }

        public abstract Data getOutputData();

        public static final class Success extends Result {
            private final Data output;

            public Success() {
                this(Data.EMPTY);
            }

            public Success(Data output) {
                this.output = output == null ? Data.EMPTY : output;
            }

            @Override
            public Data getOutputData() {
                return output;
            }

            @Override
            public String toString() {
                return "Success {mOutputData=" + output + "}";
            }
        }

        public static final class Failure extends Result {
            private final Data output;

            public Failure() {
                this(Data.EMPTY);
            }

            public Failure(Data output) {
                this.output = output == null ? Data.EMPTY : output;
            }

            @Override
            public Data getOutputData() {
                return output;
            }

            @Override
            public String toString() {
                return "Failure {mOutputData=" + output + "}";
            }
        }

        public static final class Retry extends Result {
            public Retry() {
            }

            @Override
            public Data getOutputData() {
                return Data.EMPTY;
            }

            @Override
            public String toString() {
                return "Retry";
            }
        }
    }
}
