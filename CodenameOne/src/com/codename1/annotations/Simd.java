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
package com.codename1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Helper annotations for SIMD/vectorization hints.
///
/// These are intentionally hints only: runtimes/translators may ignore them,
/// and code should remain correct and performant without relying on them.
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class Simd {

    /// Prohibited default constructor.
    private Simd() {
        throw new AssertionError("Simd should not be instantiated");
    }

    /// Marks a method as a SIMD vectorization candidate.
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
    public @interface Candidate {
    }

    /// Marks a method as likely containing a reduction pattern
    /// (e.g. sum/min/max over an array).
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
    public @interface Reduction {
    }

    /// Optional preferred SIMD lane count for vectorized code generation.
    ///
    /// This is a hint only; translators may pick a different width based on
    /// target architecture and ABI constraints.
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
    public @interface WidthHint {
        int value();
    }
}
