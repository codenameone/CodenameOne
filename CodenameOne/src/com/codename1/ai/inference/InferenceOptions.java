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
package com.codename1.ai.inference;

/// Configures how a reusable {@link InferenceSession} is created.
///
/// Accelerator requests are portable preferences rather than promises.
/// With fallback enabled, a backend may execute on CPU when the requested
/// delegate is unavailable. With fallback disabled, opening the session
/// fails instead of silently changing the execution target. Android's NNAPI
/// runtime can mix NPU and CPU operations without reporting full delegation,
/// and the iOS Core ML delegate can schedule work across the Neural Engine,
/// GPU, and CPU. Both mobile backends therefore reject
/// {@link Accelerator#NPU} when fallback is disabled. iOS also rejects strict
/// {@link Accelerator#CORE_ML} sessions because the delegate does not report
/// whether unsupported model operations remained on LiteRT's CPU path.
public final class InferenceOptions {
    /// Execution targets understood by the portable inference API.
    public enum Accelerator {
        AUTO, CPU, GPU, NPU, CORE_ML
    }

    private Accelerator accelerator = Accelerator.AUTO;
    private int threads;
    private boolean allowFallback = true;

    /// Requests an execution target for the model.
    ///
    /// @param value requested target; {@code null} restores {@link Accelerator#AUTO}
    /// @return this options object
    public InferenceOptions accelerator(Accelerator value) {
        accelerator = value == null ? Accelerator.AUTO : value;
        return this;
    }

    /// Sets the CPU worker count. Non-positive values let the native runtime
    /// choose its default.
    ///
    /// @param value requested worker count
    /// @return this options object
    public InferenceOptions threads(int value) {
        threads = Math.max(0, value);
        return this;
    }

    /// Controls whether opening may fall back from an unavailable accelerator
    /// to CPU execution.
    ///
    /// On Android and iOS, setting this to {@code false} with
    /// {@link Accelerator#NPU} rejects session creation. On iOS it also
    /// rejects {@link Accelerator#CORE_ML}. Neither LiteRT's NNAPI delegate
    /// nor its Core ML delegate can prove that every operation ran on the
    /// requested accelerator instead of CPU or another processor.
    ///
    /// @param value {@code true} to permit CPU fallback
    /// @return this options object
    public InferenceOptions allowFallback(boolean value) {
        allowFallback = value;
        return this;
    }

    /// @return the requested accelerator, never {@code null}
    public Accelerator getAccelerator() {
        return accelerator;
    }

    /// @return the requested CPU worker count, or a non-positive runtime default
    public int getThreads() {
        return threads;
    }

    /// @return whether an unavailable accelerator may fall back to CPU
    public boolean isFallbackAllowed() {
        return allowFallback;
    }

    InferenceOptions snapshot() {
        return new InferenceOptions()
                .accelerator(accelerator)
                .threads(threads)
                .allowFallback(allowFallback);
    }
}
