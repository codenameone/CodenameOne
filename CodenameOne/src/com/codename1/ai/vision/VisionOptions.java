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

/// Common analyzer configuration. An analyzer captures the supplied options
/// when constructed; reuse it for frames needing the same backend, confidence
/// threshold, and result limit.
public class VisionOptions {
    private VisionBackend backend = VisionBackends.auto();
    private float minimumConfidence;
    private int maximumResults;

    /// @param value selector, or {@code null} to restore automatic selection
    /// @return this options object
    public VisionOptions backend(VisionBackend value) {
        backend = value == null ? VisionBackends.auto() : value;
        return this;
    }

    /// Sets the confidence threshold, clamped to 0..1. NaN has no meaningful
    /// ordering and is rejected instead of being passed to platform detectors.
    /// @param value requested threshold
    /// @return this options object
    /// @throws IllegalArgumentException if {@code value} is NaN
    public VisionOptions minimumConfidence(float value) {
        if (Float.isNaN(value)) {
            throw new IllegalArgumentException(
                    "minimum confidence must be a number");
        }
        minimumConfidence = Math.max(0, Math.min(1, value));
        return this;
    }

    /// Sets a non-negative result limit; zero means backend default/unlimited.
    /// @param value requested limit
    /// @return this options object
    public VisionOptions maximumResults(int value) {
        maximumResults = Math.max(0, value);
        return this;
    }

    /// @return selected backend, never {@code null}
    public VisionBackend getBackend() {
        return backend;
    }

    /// @return confidence threshold in the range 0..1
    public float getMinimumConfidence() {
        return minimumConfidence;
    }

    /// @return non-negative result limit; zero means backend default/unlimited
    public int getMaximumResults() {
        return maximumResults;
    }

    VisionOptions snapshot() {
        return new VisionOptions().backend(backend)
                .minimumConfidence(minimumConfidence)
                .maximumResults(maximumResults);
    }
}
