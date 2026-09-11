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

    /// Told that the referrer handed over is now stored somewhere that
    /// survives the process, so a source holding a one-shot flag may burn it.
    ///
    /// The Android source may only ask Play once: the API answers a given
    /// install once, and the port records that it has asked so a later launch
    /// does not throw the answer away by asking again. Burning that flag when
    /// the value was merely HANDED OVER loses the exact code whenever the
    /// process dies first -- the framework marshals onto the EDT, so the
    /// persist is queued rather than done -- and the next launch then settles
    /// an invited install as no-match, permanently, on the one platform whose
    /// answer is exact.
    ///
    /// Called once per accepted referrer, and never when the write failed: the
    /// source should keep its flag unburnt so the next launch can ask again.
    /// A source with no such flag does nothing here.
    void referrerPersisted();
}
