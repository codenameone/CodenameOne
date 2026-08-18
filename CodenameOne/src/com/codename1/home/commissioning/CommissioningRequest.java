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
    private boolean commissionToThisApp;

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
    /// Ask the BUILD for a Matter fabric of this app's own, so a commissioned
    /// accessory can be reached directly rather than only through the user's
    /// home.
    ///
    /// #### This is a build-wide capability, not a per-accessory switch
    ///
    /// The machinery lives in an operating-system app extension that runs
    /// outside your process, and it is generated when the build is made: one
    /// `setCommissionToThisApp(true)` anywhere in your app turns it on for
    /// every accessory that build commissions, whatever a particular request
    /// says. A `false` request in such a build does not turn it back off --
    /// nothing at run time can reach into the extension to say so.
    ///
    /// It is a request method rather than a build hint because a call is
    /// something the build can SEE: the scanner reads this one and generates
    /// the extension accordingly, so an app that asks for the capability gets
    /// it without also having to remember a hint. Where the call is invisible
    /// -- behind reflection, say -- `ios.home.commissioning.fabric=true` says
    /// the same thing.
    ///
    /// [CommissioningResult#wasCommissionedToThisApp()] reports what actually
    /// happened for each accessory.
    ///
    /// #### What this costs, and what it does not do
    ///
    /// Commissioning to the user's ecosystem -- their HomeKit or Google home
    /// -- is what the flow does by default, and it is what makes the
    /// accessory usable at all. This asks for a *second* administrator: the
    /// accessory is additionally commissioned onto a fabric belonging to this
    /// app, which is the only way an app can talk to a Matter accessory
    /// without going through the ecosystem.
    ///
    /// It is not free. On iOS the build ships an operating-system Matter
    /// controller inside the generated commissioning extension, and the app
    /// carries the key material for its fabric.
    ///
    /// **Codename One does not yet expose an API for talking to an accessory
    /// over that fabric.** What asking for it buys today is that the
    /// accessory is already commissioned when such an API arrives, and that
    /// [CommissioningResult#wasCommissionedToThisApp()] can be true. Reading
    /// and writing traits still goes through the ecosystem.
    ///
    /// Where the platform cannot do it -- Android's Play Services
    /// commissioning, every desktop -- this is ignored and
    /// [CommissioningResult#wasCommissionedToThisApp()] stays `false`.
    ///
    /// #### Parameters
    ///
    /// - `commissionToThisApp`: `true` to ask for the second fabric
    ///
    /// #### Returns
    ///
    /// this request, for chaining
    public CommissioningRequest setCommissionToThisApp(
            boolean commissionToThisApp) {
        this.commissionToThisApp = commissionToThisApp;
        return this;
    }

    /// Whether this request asks for the accessory to join a fabric of this
    /// app's.
    ///
    /// #### Returns
    ///
    /// `true` when [#setCommissionToThisApp(boolean)] asked for it
    public boolean isCommissionToThisApp() {
        return commissionToThisApp;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }
}
