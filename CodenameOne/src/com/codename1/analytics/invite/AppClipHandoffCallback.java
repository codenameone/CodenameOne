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

/// Receives the answer to [AppClipHandoffSource#requestHandoff].
///
/// Exactly one method is called, once.
public interface AppClipHandoffCallback {
    /// Called with the invite code an App Clip recorded.
    ///
    /// #### Parameters
    ///
    /// - `code`: the invite code the clip received, never empty
    ///
    /// - `clickedSeconds`: when the link was tapped, in seconds since the
    ///   epoch, or 0 when the clip did not record it
    void onHandoff(String code, long clickedSeconds);

    /// Called when no clip handoff exists. This is the normal answer for
    /// somebody who installed the application without ever tapping an invite
    /// link, and is not an error.
    ///
    /// #### Parameters
    ///
    /// - `reason`: one of the `REASON_` constants on [Invites]
    void onUnavailable(String reason);
}
