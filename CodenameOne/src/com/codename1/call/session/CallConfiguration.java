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
package com.codename1.call.session;

import com.codename1.call.CallHandleType;
import com.codename1.impl.call.CallWire;

import java.util.ArrayList;
import java.util.List;

/// The calling identity this app presents to the operating system: the name
/// shown above a call, the icon beside it, the ringtone, and what kinds of
/// address the app understands.
///
/// Hand one to [Calls#configure] before reporting anything. On Android this
/// registers the `PhoneAccount`, and until it has been registered
/// `TelecomManager` **silently ignores** every incoming call the app reports
/// -- no error, no call, nothing in the log. That silence is why configuring
/// is a separate step rather than something inferred from the first call.
///
/// #### What cannot be set here
///
/// On iOS the provider's name, icon and ringtone are also needed **before any
/// of this app's code runs**, because a call arriving as a VoIP push is
/// reported to the system by native code during launch. Those come from build
/// hints baked into the app -- `ios.call.providerName`, `ios.call.icon`,
/// `ios.call.ringtone` -- and what is set here refines them once the app is
/// up. Setting only this and expecting a pushed call to ring with the right
/// name is the mistake the guide warns about.
public final class CallConfiguration {
    private String displayName;
    private boolean videoSupported;
    private boolean includesCallsInRecents = true;
    private int maximumCallGroups = 1;
    private int maximumCallsPerGroup = 1;
    private final List<CallHandleType> handleTypes =
            new ArrayList<CallHandleType>();

    /// Creates a configuration with the defaults: audio only, calls in the
    /// system call log, no conferencing, phone-number and generic handles.
    public CallConfiguration() {
        handleTypes.add(CallHandleType.GENERIC);
        handleTypes.add(CallHandleType.PHONE_NUMBER);
    }

    /// The name shown above a call. Defaults to the application name.
    public CallConfiguration displayName(String name) {
        this.displayName = name;
        return this;
    }

    /// Whether the app offers video calls.
    public CallConfiguration videoSupported(boolean value) {
        this.videoSupported = value;
        return this;
    }

    /// Whether calls appear in the system call log. Turning this off also
    /// stops the user calling back from Recents, which is usually not what
    /// is wanted.
    public CallConfiguration includesCallsInRecents(boolean value) {
        this.includesCallsInRecents = value;
        return this;
    }

    /// How many separate conferences may exist at once.
    public CallConfiguration maximumCallGroups(int value) {
        this.maximumCallGroups = value < 1 ? 1 : value;
        return this;
    }

    /// How many calls may be in one conference.
    public CallConfiguration maximumCallsPerGroup(int value) {
        this.maximumCallsPerGroup = value < 1 ? 1 : value;
        return this;
    }

    /// Replaces the set of address kinds the app understands.
    public CallConfiguration handleTypes(CallHandleType[] types) {
        handleTypes.clear();
        if (types != null) {
            for (CallHandleType type : types) {
                if (type != null && !handleTypes.contains(type)) {
                    handleTypes.add(type);
                }
            }
        }
        return this;
    }

    /// The display name, or null for the application's own.
    public String getDisplayName() {
        return displayName;
    }

    /// Whether video is offered.
    public boolean isVideoSupported() {
        return videoSupported;
    }

    /// Whether calls appear in the system call log.
    public boolean isIncludesCallsInRecents() {
        return includesCallsInRecents;
    }

    /// How many conferences may exist at once.
    public int getMaximumCallGroups() {
        return maximumCallGroups;
    }

    /// How many calls may be in one conference.
    public int getMaximumCallsPerGroup() {
        return maximumCallsPerGroup;
    }

    /// The address kinds the app understands.
    public CallHandleType[] getHandleTypes() {
        CallHandleType[] out = new CallHandleType[handleTypes.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = handleTypes.get(i);
        }
        return out;
    }

    /// Encodes this for the bridge.
    ///
    /// @hidden not part of the public API.
    public String toWire() {
        StringBuilder types = new StringBuilder();
        for (int i = 0; i < handleTypes.size(); i++) {
            if (i > 0) {
                types.append(',');
            }
            types.append(handleTypes.get(i).ordinal());
        }
        return CallWire.join(new String[]{
            displayName == null ? "" : displayName,
            CallWire.flagOf(videoSupported),
            CallWire.flagOf(includesCallsInRecents),
            String.valueOf(maximumCallGroups),
            String.valueOf(maximumCallsPerGroup),
            types.toString()
        });
    }
}
