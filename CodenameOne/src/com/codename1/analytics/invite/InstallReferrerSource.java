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

/// Reads the referrer the application store recorded when this application
/// was installed. This is the deterministic half of invite attribution: the
/// invite code makes the whole round trip through the store, so no matching
/// or guessing is involved.
///
/// The Codename One build supplies the implementation on platforms that have
/// one and registers it through
/// [Invites#registerInstallReferrerSource] before the application starts.
/// Where none is registered -- the simulator, the desktop build, iOS, and any
/// Android device without store services -- [Invites] behaves exactly as it
/// does on a device that reports no referrer.
///
/// An application does not implement this interface.
public interface InstallReferrerSource {
    /// Whether this source can answer at all on the current device.
    ///
    /// #### Returns
    ///
    /// true when a store client is present
    boolean isSupported();

    /// Asks for the install referrer. The answer arrives on the callback,
    /// possibly asynchronously and possibly on another thread; [Invites]
    /// marshals it back onto the EDT.
    ///
    /// #### Parameters
    ///
    /// - `callback`: receives the answer, never null
    void requestReferrer(InstallReferrerCallback callback);
}
