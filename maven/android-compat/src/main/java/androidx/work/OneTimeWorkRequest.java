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

import java.util.ArrayList;
import java.util.List;

public final class OneTimeWorkRequest extends WorkRequest {

    OneTimeWorkRequest(WorkSpec spec) {
        super(spec);
    }

    public static OneTimeWorkRequest from(Class<? extends ListenableWorker> workerClass) {
        return new Builder(workerClass).build();
    }

    public static List<OneTimeWorkRequest> from(List<Class<? extends ListenableWorker>> workerClasses) {
        List<OneTimeWorkRequest> out = new ArrayList<OneTimeWorkRequest>();
        for (int i = 0; i < workerClasses.size(); i++) {
            out.add(from(workerClasses.get(i)));
        }
        return out;
    }

    public static final class Builder extends WorkRequest.Builder<Builder, OneTimeWorkRequest> {

        public Builder(Class<? extends ListenableWorker> workerClass) {
            super(workerClass);
        }

        /// Every merger here overwrites, like Android's default
        /// `OverwritingInputMerger`.
        public Builder setInputMerger(Class<?> inputMerger) {
            return this;
        }

        @Override
        Builder thisObject() {
            return this;
        }

        @Override
        OneTimeWorkRequest buildInternal() {
            return new OneTimeWorkRequest(spec.copy());
        }
    }
}
