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

/// Whether presenting the authorization sheet would actually show the user
/// anything. Maps `HKHealthStore.getRequestStatusForAuthorization` on iOS
/// and a permission-set comparison on Android.
///
/// This is the **only** read-related signal iOS offers, and it is one-way:
/// [#UNNECESSARY] means "the user has already been asked", never "the user
/// said yes". Use it to decide whether to show a pre-permission explainer
/// screen, not to decide whether reads will return data.
public enum HealthRequestStatus {

    /// At least one requested access has never been presented; showing the
    /// sheet will prompt the user.
    SHOULD_REQUEST,

    /// Every requested access has already been presented. The user may
    /// have granted or denied any of them; this value does not say which,
    /// and showing the sheet again would display nothing.
    UNNECESSARY,

    /// The platform could not determine the status.
    UNKNOWN
}
