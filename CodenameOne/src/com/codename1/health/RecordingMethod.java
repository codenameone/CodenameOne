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

/// How a sample came to exist. Health Connect records this explicitly;
/// HealthKit infers it from sample metadata. Worth setting on writes --
/// downstream apps use it to decide whether to trust a value.
public enum RecordingMethod {

    /// The platform did not say.
    UNKNOWN,

    /// A human typed it in.
    MANUAL_ENTRY,

    /// Recorded passively in the background by a device or app, without
    /// the user starting anything.
    AUTOMATIC,

    /// Recorded while the user was deliberately tracking -- during a
    /// workout they started, or a measurement they triggered.
    ACTIVELY_RECORDED
}
