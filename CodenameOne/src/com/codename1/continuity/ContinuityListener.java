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

/// Notified when a state arrives from somewhere other than this device's own storage: one of the
/// user's other devices handed off what they were doing, or a `StateRelay` produced something
/// newer than what is here.
///
/// Registered with `Continuity.addContinuationListener(ContinuityListener)`. Called on the event
/// dispatch thread, and never for this device's own echo.
public interface ContinuityListener {
    /// A state arrived. Return true to let the framework restore it, false to ignore it.
    ///
    /// Returning false is the hook for the decisions only the app can make -- that the user is
    /// midway through a payment and must not be moved, that the state is older than what is on
    /// screen, that it belongs to a different account than the one signed in here. A listener
    /// that returns false has consumed the state: nothing is restored and no other listener is
    /// asked.
    ///
    /// Doing the work yourself and returning false is a supported pattern, and is how an app
    /// prompts before jumping: keep the state, return false, and call
    /// `Continuity.restore(AppState)` when the user accepts.
    ///
    /// If you handle it yourself and never call `restore`, call
    /// `Continuity.acknowledge(AppState)` instead. Restoring records that the state was acted on
    /// so it is not offered again after a relaunch; handling it silently does not, and without
    /// the acknowledgement the relay's unchanged document is accepted on the next launch and your
    /// side effects run a second time. It is not inferred from the false return, because false
    /// also means "I am going to prompt" -- and marking that handled before the user answers
    /// would lose the state if the process died first.
    ///
    /// #### Parameters
    ///
    /// - `state`: the state that arrived
    ///
    /// #### Returns
    ///
    /// true to restore it now
    boolean stateReceived(AppState state);
}
