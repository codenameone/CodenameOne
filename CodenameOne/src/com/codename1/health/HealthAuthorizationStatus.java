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

/// The app's authorization for one data type and one direction.
///
/// #### The asymmetry you must handle
///
/// Write authorization is truthfully reportable on both platforms. **Read
/// authorization is not.** HealthKit deliberately refuses to disclose it,
/// because telling an app "the user denied you access to pregnancy data"
/// leaks the very thing the user was hiding. So
/// [HealthStore#getReadAuthorizationStatus(HealthDataType)] returns
/// [#UNKNOWN] on iOS regardless of what the user actually chose, and any
/// code that branches on it must handle that value.
public enum HealthAuthorizationStatus {

    /// The user has not been asked yet.
    NOT_DETERMINED,

    /// The app holds the access.
    AUTHORIZED,

    /// The user explicitly refused the access.
    DENIED,

    /// Access is blocked by something outside the user's control at this
    /// moment -- a device management policy, or a data type the platform
    /// does not permit this app to touch.
    RESTRICTED,

    /// The platform will not say. Returned for **read** access on iOS in
    /// every case, by design; it means neither "granted" nor "denied" and
    /// must not be rendered to the user as either.
    ///
    /// The only honest way to find out whether reads work is to run a
    /// query -- see [HealthStore#hasAnyData(HealthDataType,HealthTimeRange)].
    UNKNOWN,

    /// This platform has no health store, so the question does not apply.
    NOT_SUPPORTED
}
