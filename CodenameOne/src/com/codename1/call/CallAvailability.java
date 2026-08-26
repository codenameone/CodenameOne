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
package com.codename1.call;

/// Whether the app may ring a call *right now*. Distinct from
/// `Calls.isSupported()`, which asks whether the platform has the machinery
/// at all: an app can be perfectly supported and still be unable to ring,
/// because the user is on an emergency call or another app holds one.
///
/// Check this before reporting an incoming call so the far end can be told
/// to stop trying, rather than discovering the refusal from a failed report.
///
/// The ordinals cross the SPI boundary, so **existing constants must not be
/// reordered**.
public enum CallAvailability {
    /// A call can be reported now.
    AVAILABLE,

    /// An emergency call is in progress. Nothing else may ring.
    EMERGENCY_CALL_IN_PROGRESS,

    /// Another application holds a call the system will not interrupt.
    OTHER_APP_IN_CALL,

    /// The user has switched this app's calling off, or the required
    /// permission or role is missing.
    NOT_PERMITTED,

    /// The platform has no system call integration at all.
    UNSUPPORTED
}
