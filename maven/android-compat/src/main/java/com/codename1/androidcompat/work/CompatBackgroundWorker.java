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
package com.codename1.androidcompat.work;

import com.codename1.background.BackgroundWorker;
import com.codename1.util.Callback;

import java.util.Map;

/// The Codename One `BackgroundWorker` that runs a WorkManager request the
/// platform scheduler released, once its constraints hold. It only names the
/// request; the request itself, and whether it still needs running, comes
/// from WorkManager's saved state, so a request the application already ran
/// in the foreground is not run again.
public class CompatBackgroundWorker implements BackgroundWorker {

    public CompatBackgroundWorker() {
    }

    @Override
    public void performWork(String workId, Map<String, String> inputData, long deadline, Callback<Boolean> onComplete) {
        String id = inputData == null ? null : inputData.get("id");
        if (id == null) {
            onComplete.onSucess(Boolean.TRUE);
            return;
        }
        androidx.work.WorkManagerBridge.run(id, onComplete);
    }
}
