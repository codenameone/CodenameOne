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
package com.codename1.background;

import com.codename1.util.Callback;
import java.util.Map;

/// Implemented by an application class to perform constraint-aware background work
/// scheduled through `BackgroundWork#schedule(WorkRequest)`.
///
/// The implementing class MUST have a public no-argument constructor: the platform may
/// reconstruct a fresh instance after the app process has been killed and cold launched
/// to run the work, so no state from the foreground app is available. Pass any required
/// state through the work request input data.
///
/// #### See also
///
/// - BackgroundWork
///
/// - WorkRequest
public interface BackgroundWorker {

    /// Performs the background work. The implementation should call `onComplete` with
    /// `Boolean.TRUE` on success or `Boolean.FALSE` to request that the platform retry
    /// the work later. The work must finish before `deadline`; if it does not, the
    /// platform may terminate it.
    ///
    /// #### Parameters
    ///
    /// - `workId`: the id of the work request being executed
    ///
    /// - `inputData`: the immutable input data supplied to the work request
    ///
    /// - `deadline`: the time in milliseconds since the epoch by which the work must finish
    ///
    /// - `onComplete`: callback invoked with the outcome (true for success, false to retry)
    void performWork(String workId, Map<String, String> inputData, long deadline, Callback<Boolean> onComplete);
}
