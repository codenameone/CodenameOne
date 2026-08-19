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
package com.codename1.home;

/// Whether the user has granted this app access to their home.
///
/// Unlike the health API, this question has an honest answer on every
/// backend: HomeKit publishes `HMHomeManagerAuthorizationStatus` and the
/// Google Home APIs publish the account and structure grant. There is no
/// read/write split -- a home is authorized as a whole, and what an app may do
/// to a particular accessory is a property of the accessory, not of the grant.
public enum HomeAuthorizationStatus {

    /// The user has not been asked yet. Call
    /// [SmartHome#requestAuthorization()].
    NOT_DETERMINED,

    /// Access granted.
    AUTHORIZED,

    /// Blocked by parental controls or device management. Asking again will
    /// not help.
    RESTRICTED,

    /// The user was asked and declined. Recoverable only through the system
    /// settings; see [SmartHome#openHomeSettings()].
    DENIED,

    /// This port cannot answer the question because it has no backend to ask.
    /// Returned by the inert fallback and by the local simulated home.
    UNKNOWN
}
