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

/// Receives the invite that caused this install, if there was one.
///
/// Register with [Invites#setInviteListener] before calling
/// [Invites#checkForInvite]. Exactly one of the two methods is called per
/// install, on the EDT, and neither is called again on later launches -- the
/// answer is remembered.
///
/// ```java
/// Invites.setInviteListener(new InviteListener() {
///     public void inviteReceived(InviteAttribution attribution) {
///         Dialog.show("Welcome", "Invited by " + attribution.getCampaign(), "OK", null);
///     }
///
///     public void attributionUnavailable(String reason) {
///     }
/// });
/// ```
public interface InviteListener {
    /// Called when this install is attributed to an invite.
    ///
    /// #### Parameters
    ///
    /// - `attribution`: the resolved attribution, never null
    void inviteReceived(InviteAttribution attribution);

    /// Called when no invite will be attributed to this install. This is the
    /// ordinary outcome -- most installs are not invited -- so treat it as
    /// information rather than as a failure.
    ///
    /// #### Parameters
    ///
    /// - `reason`: one of the `REASON_` constants on [Invites]
    void attributionUnavailable(String reason);
}
