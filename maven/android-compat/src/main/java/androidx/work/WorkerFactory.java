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

/// Creates workers by class name. Return null to let the default creation
/// run, which calls the worker's `(Context, WorkerParameters)` constructor
/// through code the build generated -- there is no reflection on the device.
public abstract class WorkerFactory {

    public abstract ListenableWorker createWorker(Context appContext, String workerClassName,
            WorkerParameters workerParameters);

    public static WorkerFactory getDefaultWorkerFactory() {
        return new WorkerFactory() {
            @Override
            public ListenableWorker createWorker(Context appContext, String workerClassName,
                    WorkerParameters workerParameters) {
                return null;
            }
        };
    }
}
