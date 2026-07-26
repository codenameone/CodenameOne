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
package com.codename1.ai.vision;

import com.codename1.util.AsyncResource;

/// Reusable, closable on-device analyzer for still images or camera frames.
/// Implementations may retain native detectors and models between calls, so
/// create one analyzer per stream/workflow and close it when finished.
public interface VisionAnalyzer<T> extends AutoCloseable {
    /// Tests the exact feature/backend pair configured for this analyzer.
    /// @return {@code true} when the current target supports it
    boolean isSupported();
    /// Starts one asynchronous analysis without uploading the image.
    /// @param image encoded or raw input
    /// @return asynchronous typed result delivered on the EDT
    AsyncResource<T> process(VisionImage image);
    /// Releases native detector/model resources; further processing fails.
    @Override
    void close();
}
