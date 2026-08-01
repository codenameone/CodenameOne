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

/// How much stage detail a [SleepSample] actually carries.
///
/// A queryable fact rather than something to infer from the data, because
/// the three cases need different UI and the difference is not the app's
/// fault: a phone-inferred session and a watch-recorded one are the same
/// type carrying different fidelity.
public enum SleepStageSupport {

    /// No stage intervals at all -- the session knows when it started and
    /// ended and nothing else.
    NONE,

    /// Asleep and awake spans only, with no breakdown of the asleep time.
    /// A phone-inferred session, or an iOS session recorded before the
    /// stage values arrived in iOS 16.
    ASLEEP_AWAKE,

    /// A real hypnogram: at least one of [SleepStage#LIGHT],
    /// [SleepStage#CORE], [SleepStage#DEEP] or [SleepStage#REM].
    STAGED
}
