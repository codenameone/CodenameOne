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
package com.codename1.impl.ios;

import com.codename1.analytics.invite.AppClipHandoffCallback;
import com.codename1.analytics.invite.AppClipHandoffSource;
import com.codename1.analytics.invite.Invites;
import com.codename1.io.Log;

/// Reads the invite code an App Clip left in the shared app group container.
///
/// This is the deterministic half of invite attribution on iOS. The App Clip
/// is launched by the invite link itself and receives that link exactly, so it
/// writes the code into the group container before offering the App Store.
/// The code made the whole trip through the store, so nothing here is matched
/// or guessed and nothing about the visitor is collected.
///
/// Only an iOS build that generated an App Clip registers this, and
/// `IPhoneBuilder` splices that registration into the generated stub under
/// exactly the condition that produced the clip. Nothing else in the port
/// references this class, so a build without invites strips it along with the
/// invite package -- which is deliberate, and is why it names the app group at
/// construction rather than reading a build hint of its own.
public class IOSAppClipHandoff implements AppClipHandoffSource {
    private final String appGroup;

    /// Creates a source reading the named app group.
    ///
    /// #### Parameters
    ///
    /// - `appGroup`: the `group.` identifier the clip and the application both
    ///   carry in their entitlements
    public IOSAppClipHandoff(String appGroup) {
        this.appGroup = appGroup;
    }

    public boolean isSupported() {
        if (appGroup == null || appGroup.length() == 0) {
            return false;
        }
        try {
            return IOSImplementation.nativeInstance
                    .isAppClipHandoffSupported(appGroup);
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    public void requestHandoff(AppClipHandoffCallback callback) {
        String handoff;
        try {
            handoff = IOSImplementation.nativeInstance
                    .readAppClipInviteHandoff(appGroup);
        } catch (Throwable t) {
            // An unreachable container reads as "no clip ran", never as a
            // crash: the application works, it simply has no invite behind it.
            Log.e(t);
            callback.onUnavailable(Invites.REASON_UNSUPPORTED);
            return;
        }
        if (handoff == null || handoff.length() == 0) {
            callback.onUnavailable(Invites.REASON_NO_MATCH);
            return;
        }
        // "<code>\n<clickedSeconds>". Two values in one string because the
        // container is read once and emptied once: splitting the call would
        // mean deciding which half clears it.
        String code = handoff;
        long clicked = 0;
        int nl = handoff.indexOf('\n');
        if (nl >= 0) {
            code = handoff.substring(0, nl);
            clicked = parseSeconds(handoff.substring(nl + 1));
        }
        code = code.trim();
        if (code.length() == 0) {
            callback.onUnavailable(Invites.REASON_NO_MATCH);
            return;
        }
        callback.onHandoff(code, clicked);
    }

    /// Empties the shared container, once the framework has the code stored
    /// somewhere that survives this process.
    ///
    /// The read deliberately leaves it alone. This container is the only
    /// durable copy of an exact App Clip code until the framework writes its
    /// own record, so clearing on read destroyed it whenever that write failed
    /// or the process exited in between -- and the next launch, finding no
    /// handoff, settled an invited install as no_match for ever. Nothing
    /// reports that: the clip ran, the store carried the person across, and
    /// the install simply looks organic.
    public boolean discardHandoff() {
        if (appGroup == null || appGroup.length() == 0) {
            // No group, so no container, so nothing is holding a code.
            return true;
        }
        try {
            return IOSImplementation.nativeInstance.clearAppClipInviteHandoff(appGroup);
        } catch (Throwable t) {
            // Reported as NOT discarded, because the caller may be an erasure.
            // A container that could not be emptied still holds an exact code
            // naming an inviter, and it is read on the next launch -- so the
            // honest answer is that the handoff is still there, whatever the
            // reason.
            Log.e(t);
            return false;
        }
    }

    /// A timestamp that will not parse is not worth losing an attribution
    /// over: the code is what the claim is made with, and the click time is
    /// only reported alongside it.
    private static long parseSeconds(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException err) {
            return 0;
        }
    }
}
