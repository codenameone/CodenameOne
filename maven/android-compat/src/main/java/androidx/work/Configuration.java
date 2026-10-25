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

import java.util.concurrent.Executor;

public final class Configuration {

    final WorkerFactory workerFactory;
    final Executor executor;

    Configuration(Builder b) {
        workerFactory = b.workerFactory;
        executor = b.executor;
    }

    public WorkerFactory getWorkerFactory() {
        return workerFactory;
    }

    public Executor getExecutor() {
        return executor;
    }

    public interface Provider {
        Configuration getWorkManagerConfiguration();
    }

    public static final class Builder {
        WorkerFactory workerFactory;
        Executor executor;

        public Builder setWorkerFactory(WorkerFactory workerFactory) {
            this.workerFactory = workerFactory;
            return this;
        }

        public Builder setExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public Builder setTaskExecutor(Executor executor) {
            return this;
        }

        public Builder setMinimumLoggingLevel(int level) {
            return this;
        }

        public Builder setJobSchedulerJobIdRange(int min, int max) {
            return this;
        }

        public Builder setMaxSchedulerLimit(int limit) {
            return this;
        }

        public Builder setDefaultProcessName(String name) {
            return this;
        }

        public Configuration build() {
            return new Configuration(this);
        }
    }
}
