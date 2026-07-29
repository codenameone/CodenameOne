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
package com.codename1.ai.language;

/// Reusable options shared by language identification, translation, and
/// Smart Reply operations.
public final class LanguageOptions {
    private LanguageBackend backend = LanguageBackends.auto();
    private float minimumConfidence;

    /// @param value backend selector; {@code null} restores automatic selection
    /// @return this options object
    public LanguageOptions backend(LanguageBackend value) {
        backend = value == null ? LanguageBackends.auto() : value;
        return this;
    }

    /// Sets the minimum language-identification confidence, clamped to 0..1.
    /// NaN has no meaningful ordering and is rejected instead of being passed
    /// to a platform language identifier.
    /// @param value requested threshold
    /// @return this options object
    /// @throws IllegalArgumentException if {@code value} is NaN
    public LanguageOptions minimumConfidence(float value) {
        if (Float.isNaN(value)) {
            throw new IllegalArgumentException(
                    "minimum confidence must be a number");
        }
        minimumConfidence = Math.max(0, Math.min(1, value));
        return this;
    }

    /// @return selected backend, never {@code null}
    public LanguageBackend getBackend() {
        return backend;
    }

    /// @return language-identification threshold in the range 0..1
    public float getMinimumConfidence() {
        return minimumConfidence;
    }

    LanguageOptions snapshot() {
        return new LanguageOptions()
                .backend(backend)
                .minimumConfidence(minimumConfidence);
    }
}
