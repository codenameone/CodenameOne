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
package com.codename1.continuity;

import java.io.IOException;

/// Carries state between devices the platform will not carry it between -- an iPhone and an
/// Android tablet, two devices that are never in the same room, a phone and the web build.
///
/// Codename One ships no server for this. A relay is the application's own endpoint, which is
/// also the only honest arrangement: the relay has to know which states belong to the same
/// *person*, and that is the app's account system, not the framework's. `RestStateRelay` covers
/// the common case over HTTPS; implement this interface directly for anything else.
///
/// Both methods are called from a background thread and may block. Neither is called on the event
/// dispatch thread, so ordinary blocking `com.codename1.io` code is correct here.
public interface StateRelay {
    /// Sends a state. Called after each checkpoint, so implementations that talk to a slow
    /// endpoint should coalesce rather than send every one.
    ///
    /// #### Parameters
    ///
    /// - `state`: the state to send
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`: when the send failed; the framework logs it and keeps the state
    ///   for the next attempt
    void publish(AppState state) throws IOException;

    /// Asks for the newest state this user has on any device. Returning this device's own most
    /// recent state is fine and expected -- the framework recognizes its own echo by device id and
    /// sequence, and ignores it.
    ///
    /// #### Returns
    ///
    /// the state, or null when the endpoint has nothing
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`: when the fetch failed
    AppState fetch() throws IOException;
}
