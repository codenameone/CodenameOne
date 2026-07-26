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
package com.codename1.health;

/// Thrown when the app is missing build configuration that the health APIs
/// cannot work without -- an absent `ios.NSHealthShareUsageDescription`
/// build hint, a missing Health Connect privacy-policy declaration.
///
/// #### Why this is unchecked, when everything else is an AsyncResource failure
///
/// An unsupported platform is a runtime fact that a correct app must
/// handle gracefully, so it arrives as a [HealthException] through an
/// `AsyncResource`. A missing usage description is a **developer bug**
/// that gets the app rejected from the App Store, and it must therefore be
/// impossible to swallow into an error callback nobody logs. It follows
/// the precedent set by `LocationManager` on iOS, which throws with the
/// exact name of the build hint to add.
///
/// [Health#getConfigurationProblems()] returns the same diagnostics
/// without throwing, so a diagnostics screen or a unit test can assert on
/// them.
public class HealthConfigurationException extends RuntimeException {

    /// Creates an exception whose message should name the exact build hint
    /// to add and describe what to put in it.
    public HealthConfigurationException(String message) {
        super(message);
    }
}
