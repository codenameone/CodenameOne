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

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

public final class WorkerParameters {

    private final UUID id;
    private final Data inputData;
    private final Set<String> tags;
    private final int runAttemptCount;
    private final int generation;
    private final Executor backgroundExecutor;
    final WorkEngine.Progress progress;

    WorkerParameters(UUID id, Data inputData, Set<String> tags, int runAttemptCount, int generation,
            Executor backgroundExecutor, WorkEngine.Progress progress) {
        this.id = id;
        this.inputData = inputData;
        this.tags = tags;
        this.runAttemptCount = runAttemptCount;
        this.generation = generation;
        this.backgroundExecutor = backgroundExecutor;
        this.progress = progress;
    }

    public UUID getId() {
        return id;
    }

    public Data getInputData() {
        return inputData;
    }

    public Set<String> getTags() {
        return tags;
    }

    public int getRunAttemptCount() {
        return runAttemptCount;
    }

    public int getGeneration() {
        return generation;
    }

    public Executor getBackgroundExecutor() {
        return backgroundExecutor;
    }
}
