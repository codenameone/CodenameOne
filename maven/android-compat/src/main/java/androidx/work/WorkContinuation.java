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

import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

/// A chain of one-time requests: each stage starts once every request of the
/// stage before it succeeded, and gets their outputs merged into its input.
public class WorkContinuation {

    final WorkManager manager;
    /// Every request of the chain, each a copy owning its prerequisites.
    final List<WorkSpec> specs;
    /// The requests of the last stage, which a `then` waits for.
    final List<WorkSpec> leaves;
    final String uniqueName;
    final ExistingWorkPolicy policy;

    WorkContinuation(WorkManager manager, List<WorkSpec> specs, List<WorkSpec> leaves, String uniqueName,
            ExistingWorkPolicy policy) {
        this.manager = manager;
        this.specs = specs;
        this.leaves = leaves;
        this.uniqueName = uniqueName;
        this.policy = policy;
    }

    static WorkContinuation begin(WorkManager manager, List<OneTimeWorkRequest> work, String uniqueName,
            ExistingWorkPolicy policy) {
        if (work == null || work.isEmpty()) {
            throw new IllegalArgumentException("beginWith needs at least one OneTimeWorkRequest.");
        }
        List<WorkSpec> stage = copies(work);
        return new WorkContinuation(manager, stage, stage, uniqueName, policy);
    }

    private static List<WorkSpec> copies(List<OneTimeWorkRequest> work) {
        List<WorkSpec> out = new ArrayList<WorkSpec>();
        for (int i = 0; i < work.size(); i++) {
            out.add(work.get(i).spec.copy());
        }
        return out;
    }

    public final WorkContinuation then(OneTimeWorkRequest work) {
        return then(Arrays.asList(work));
    }

    public final WorkContinuation then(OneTimeWorkRequest... work) {
        return then(Arrays.asList(work));
    }

    public WorkContinuation then(List<OneTimeWorkRequest> work) {
        if (work.isEmpty()) {
            return this;
        }
        List<WorkSpec> stage = copies(work);
        for (int i = 0; i < stage.size(); i++) {
            for (int j = 0; j < leaves.size(); j++) {
                stage.get(i).prerequisites.add(leaves.get(j).id);
            }
        }
        List<WorkSpec> all = new ArrayList<WorkSpec>(specs);
        all.addAll(stage);
        return new WorkContinuation(manager, all, stage, uniqueName, policy);
    }

    /// A continuation whose next stage waits for every chain in `continuations`.
    public static WorkContinuation combine(List<WorkContinuation> continuations) {
        if (continuations.isEmpty()) {
            throw new IllegalArgumentException("combine needs at least one WorkContinuation.");
        }
        List<WorkSpec> all = new ArrayList<WorkSpec>();
        List<WorkSpec> leaves = new ArrayList<WorkSpec>();
        WorkContinuation first = continuations.get(0);
        for (int i = 0; i < continuations.size(); i++) {
            WorkContinuation c = continuations.get(i);
            for (int j = 0; j < c.specs.size(); j++) {
                if (!all.contains(c.specs.get(j))) {
                    all.add(c.specs.get(j));
                }
            }
            leaves.addAll(c.leaves);
        }
        return new WorkContinuation(first.manager, all, leaves, first.uniqueName, first.policy);
    }

    public Operation enqueue() {
        // Enqueue a fresh copy, so enqueueing the same continuation twice
        // does not share state between the two runs.
        List<WorkSpec> fresh = new ArrayList<WorkSpec>();
        List<WorkSpec> freshLeaves = new ArrayList<WorkSpec>();
        for (int i = 0; i < specs.size(); i++) {
            WorkSpec c = specs.get(i).copy();
            fresh.add(c);
            if (leaves.contains(specs.get(i))) {
                freshLeaves.add(c);
            }
        }
        WorkEngine.get().enqueue(new WorkContinuation(manager, fresh, freshLeaves, uniqueName, policy));
        return WorkManager.completed();
    }

    public LiveData<List<WorkInfo>> getWorkInfosLiveData() {
        return WorkEngine.get().liveInfos("chain:" + ids(), matcher());
    }

    public Future<List<WorkInfo>> getWorkInfos() {
        return WorkEngine.get().infosLater(matcher());
    }

    private String ids() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < specs.size(); i++) {
            sb.append(specs.get(i).id).append(',');
        }
        return sb.toString();
    }

    private WorkEngine.Matcher matcher() {
        final List<String> ids = new ArrayList<String>();
        for (int i = 0; i < specs.size(); i++) {
            ids.add(specs.get(i).id);
        }
        return new IdsMatcher(ids);
    }

    private static final class IdsMatcher implements WorkEngine.Matcher {
        private final List<String> ids;

        IdsMatcher(List<String> ids) {
            this.ids = ids;
        }

        @Override
        public boolean matches(WorkSpec s) {
            return ids.contains(s.id);
        }
    }
}
