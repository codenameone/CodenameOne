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
package com.codename1.home.commissioning;

import com.codename1.home.HomeRoom;
import com.codename1.home.HomeStructure;

/// What to add, and where the user would like it to land.
///
/// Every field is optional. An empty request is valid and means "open the
/// platform's add-accessory UI and let the user do everything there", which is
/// the right call for a plain "add a device" button.
///
/// ```java
/// CommissioningRequest req = new CommissioningRequest()
///         .setSetupPayload(SetupPayload.parse(scanned))
///         .setStructure(home)
///         .setRoom(kitchen)
///         .setSuggestedName("Kettle");
/// ```
///
/// #### Preferences, not instructions
///
/// The structure, room and name are passed to an operating-system flow that
/// owns the interaction, and the user can overrule any of them. Do not assume
/// the accessory ended up where you asked; re-read the graph afterwards.
public final class CommissioningRequest {

    private SetupPayload setupPayload;
    private String rawSetupPayload;
    private String structureId;
    private String roomId;
    private String suggestedName;
    private int timeoutMillis;

    /// The accessory's onboarding payload, from a scanned QR code or a typed
    /// manual code.
    ///
    /// Leave it unset to have the platform's own UI scan one. Setting it skips
    /// that step, which is what you want when your app already runs a scanner.
    ///
    /// #### Parameters
    ///
    /// - `setupPayload`: the parsed payload, or `null` to clear it
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    public CommissioningRequest setSetupPayload(SetupPayload setupPayload) {
        this.setupPayload = setupPayload;
        // Cleared together, because commission() forwards the raw form.
        // Leaving the old code behind means clearing the payload silently
        // commissions with it on the next use, instead of opening the
        // platform's scanner as asked.
        this.rawSetupPayload =
                setupPayload == null ? null : setupPayload.getRaw();
        return this;
    }

    /// The parsed payload.
    ///
    /// #### Returns
    ///
    /// the payload, or `null` when none was set or it was set raw
    public SetupPayload getSetupPayload() {
        return setupPayload;
    }

    /// An onboarding payload to pass through without parsing it.
    ///
    /// For the vendor-extended payloads
    /// [SetupPayload#parse(java.lang.String)] refuses: the platform's own
    /// commissioning UI understands them, and refusing to forward one would
    /// make an accessory uncommissionable through this API for no reason
    /// beyond our parser's scope.
    ///
    /// The cost is that nothing is validated, so a mistyped code fails in the
    /// OS sheet rather than in your app. Prefer
    /// [#setSetupPayload(SetupPayload)] and fall back to this only when
    /// parsing threw.
    ///
    /// #### Parameters
    ///
    /// - `rawSetupPayload`: the code exactly as scanned, or `null` to clear it
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    public CommissioningRequest setRawSetupPayload(String rawSetupPayload) {
        this.rawSetupPayload = rawSetupPayload;
        this.setupPayload = null;
        return this;
    }

    /// The onboarding payload as it will be handed to the platform.
    ///
    /// #### Returns
    ///
    /// the code, or `null` when none was set
    public String getRawSetupPayload() {
        return rawSetupPayload;
    }

    /// The home to add the accessory to.
    ///
    /// #### Parameters
    ///
    /// - `structure`: the home, or `null` for the platform's default
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    public CommissioningRequest setStructure(HomeStructure structure) {
        this.structureId = structure == null ? null : structure.getId();
        return this;
    }

    /// The home to add the accessory to, by identifier.
    ///
    /// #### Parameters
    ///
    /// - `structureId`: the home, or `null` for the platform's default
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    public CommissioningRequest setStructureId(String structureId) {
        this.structureId = structureId;
        return this;
    }

    /// The home to add the accessory to.
    ///
    /// #### Returns
    ///
    /// the structure identifier, or `null`
    public String getStructureId() {
        return structureId;
    }

    /// The room to put the accessory in.
    ///
    /// #### Parameters
    ///
    /// - `room`: the room, or `null` to let the user choose
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    public CommissioningRequest setRoom(HomeRoom room) {
        this.roomId = room == null ? null : room.getId();
        return this;
    }

    /// The room to put the accessory in, by identifier.
    ///
    /// #### Parameters
    ///
    /// - `roomId`: the room, or `null` to let the user choose
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    public CommissioningRequest setRoomId(String roomId) {
        this.roomId = roomId;
        return this;
    }

    /// The room to put the accessory in.
    ///
    /// #### Returns
    ///
    /// the room identifier, or `null`
    public String getRoomId() {
        return roomId;
    }

    /// A name to offer the user for the new accessory.
    ///
    /// #### Parameters
    ///
    /// - `suggestedName`: the name, or `null` for none
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    public CommissioningRequest setSuggestedName(String suggestedName) {
        this.suggestedName = suggestedName;
        return this;
    }

    /// The name offered to the user.
    ///
    /// #### Returns
    ///
    /// the name, or `null`
    public String getSuggestedName() {
        return suggestedName;
    }

    /// How long to allow the whole flow, in milliseconds.
    ///
    /// Zero, the default, means the platform's own limit, and that is almost
    /// always what you want: **a user commissioning an accessory may be up a
    /// ladder.** They have to power it on, hold a button, sometimes join it to
    /// Wi-Fi. A timeout tuned to how long a network call takes will abandon a
    /// flow that was going fine.
    ///
    /// #### Parameters
    ///
    /// - `timeoutMillis`: the limit, or zero for the platform default
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the value is negative
    public CommissioningRequest setTimeoutMillis(int timeoutMillis) {
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException(
                    "the timeout cannot be negative, got " + timeoutMillis);
        }
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    /// How long the flow is allowed, in milliseconds.
    ///
    /// #### Returns
    ///
    /// the limit, or zero for the platform default
    public int getTimeoutMillis() {
        return timeoutMillis;
    }
}
