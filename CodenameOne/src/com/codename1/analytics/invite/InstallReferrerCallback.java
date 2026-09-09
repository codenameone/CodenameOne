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
package com.codename1.analytics.invite;

/// Receives the answer from an [InstallReferrerSource].
///
/// Implemented by the framework; an application never implements this.
public interface InstallReferrerCallback {
    /// Called with the raw referrer query string the store recorded at
    /// install time.
    ///
    /// #### Parameters
    ///
    /// - `rawReferrer`: the undecoded referrer query string, may be empty
    ///
    /// - `referrerClickSeconds`: when the link was clicked, in seconds since
    ///   the epoch, or 0 when the store did not say
    ///
    /// - `installBeginSeconds`: when the install began, in seconds since the
    ///   epoch, or 0 when the store did not say
    public void onReferrer(String rawReferrer, long referrerClickSeconds,
            long installBeginSeconds);

    /// Called when no referrer can be obtained. This is the normal answer on
    /// a device with no store client -- a sideload, an emulator without store
    /// services, or a non-store distribution -- and is not an error.
    ///
    /// #### Parameters
    ///
    /// - `reason`: one of the `REASON_` constants on [Invites]
    public void onUnavailable(String reason);
}
