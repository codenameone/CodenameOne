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
package com.codename1.security;

/// What the framework should do about a touch that arrives while another application's window is
/// drawn over this app. See [DeviceIntegrity#setTapjackingProtection(TapjackingPolicy)].
///
/// The distinction that matters here is between *fully* and *partially* obscured. Android reports
/// them as two different flags, and they carry very different signal-to-noise:
///
/// - **Fully obscured** means another window sits directly over the point that was touched. That is
///   the tapjacking attack itself, and it is rare enough in normal use that blocking on it is safe.
/// - **Partially obscured** means some other window covers *part* of this app's window, anywhere.
///   Benign system UI sets it routinely, so treating it as an attack will drop legitimate taps.
///
/// That is why [#BLOCK] stops at the first and [#STRICT] is a deliberate opt-in to the second.
public enum TapjackingPolicy {

    /// No detection and no reporting. This is the default: the check costs nothing but an app that
    /// never asked for it should not start seeing signals, and blocking touches is not a behaviour
    /// to switch on behind a developer's back.
    OFF,

    /// Observe and report, but never change event delivery. Touches are dispatched exactly as they
    /// would be with [#OFF], while [DeviceIntegrity#isScreenObscured()], the tapjacking listeners
    /// and the [com.codename1.security.shield.ShieldSignal#TAPJACK] signal all become live.
    ///
    /// Use this to measure how often obscuring actually happens in your user base before you commit
    /// to dropping input.
    REPORT,

    /// Report, and drop any gesture that begins on a fully obscured window. **The recommended
    /// setting for a sensitive app.**
    ///
    /// The whole gesture is dropped, not the individual event: swallowing a press while letting the
    /// matching release through would leave the framework holding half a gesture.
    BLOCK,

    /// As [#BLOCK], and additionally drop gestures that arrive while the window is only *partially*
    /// obscured.
    ///
    /// Understand the cost before choosing this. The partial flag is set by ordinary system UI, so
    /// on some devices this will discard taps the user meant, and the app will read as unresponsive
    /// with nothing in the logs to explain it. Reach for it only where a missed tap is clearly
    /// preferable to a hijacked one.
    STRICT;

    /// Whether this policy wants the platform to look at the obscured flags at all. False only for
    /// [#OFF], which is what lets a port skip the check entirely on the hot input path.
    public boolean isDetecting() {
        return this != OFF;
    }

    /// Whether a gesture carrying these obscured flags should be dropped rather than delivered.
    ///
    /// This is the whole blocking decision, kept here as pure logic so it is identical on every
    /// port and can be tested without a device.
    ///
    /// #### Parameters
    ///
    /// - `obscured`: another window sits directly over the touched point
    /// - `partiallyObscured`: another window covers some part of this app's window
    ///
    /// #### Returns
    ///
    /// true if the gesture must not be delivered to the application
    public boolean blocks(boolean obscured, boolean partiallyObscured) {
        if (this == BLOCK) {
            return obscured;
        }
        if (this == STRICT) {
            return obscured || partiallyObscured;
        }
        // OFF and REPORT never change delivery.
        return false;
    }
}
