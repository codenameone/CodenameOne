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

/// Reads the invite code an iOS App Clip left behind for the full
/// application.
///
/// This is the iOS half of deterministic attribution, and the counterpart of
/// [InstallReferrerSource] on Android. An App Clip is launched by the invite
/// link itself and receives that link exactly, so it can write the code into
/// the container it shares with the full application before offering the App
/// Store. When the person installs, the application reads it here: the code
/// made the whole trip through the store, so nothing is matched or guessed.
///
/// It replaced a statistical match against a hashed device profile, which
/// existed only because the App Store carries no referrer of its own. Nothing
/// about the visitor is collected any more.
///
/// The Codename One build supplies the implementation on platforms that have
/// one and registers it through [Invites#registerAppClipHandoffSource] before
/// the application starts. Where none is registered -- the simulator, the
/// desktop build, Android, and any iOS application built without an App Clip
/// -- [Invites] behaves exactly as it does when a clip left nothing.
///
/// An application does not implement this interface.
public interface AppClipHandoffSource {
    /// Whether this source can answer at all on the current device.
    ///
    /// #### Returns
    ///
    /// true when a shared container is reachable
    boolean isSupported();

    /// Asks for the code an App Clip left behind. The answer arrives on the
    /// callback, possibly asynchronously and possibly on another thread;
    /// [Invites] marshals it back onto the EDT.
    ///
    /// The handoff is read once and cleared by the implementation, so a code
    /// cannot be claimed twice by two launches.
    ///
    /// #### Parameters
    ///
    /// - `callback`: receives the answer, never null
    void requestHandoff(AppClipHandoffCallback callback);
}
