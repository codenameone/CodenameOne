/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.security.shield;

/// Callback for shield state changes. Register with [AppShield#addListener(ShieldListener)].
///
/// All callbacks are delivered on the EDT, so they may touch the UI directly. The framework itself
/// never shows a dialog or terminates the app over a shield event -- what the user sees is entirely
/// the app's decision, made here.
public interface ShieldListener {

    /// The token status changed, for example from [ShieldStatus#OK] to [ShieldStatus#REJECTED].
    ///
    /// Branch on [ShieldStatus#isTransient()] before reacting. A transient status means the
    /// service was unreachable and will likely be reachable again shortly; reacting to it the same
    /// way as [ShieldStatus#REJECTED] is how an app ends up locking out users on a bad connection.
    void statusChanged(ShieldStatus status);

    /// A new runtime self-protection observation was recorded. Informational: the attestation
    /// service decides what a signal means for token issuance, and it may reach a different
    /// conclusion than the device would.
    void signalRaised(ShieldSignal signal);
}
