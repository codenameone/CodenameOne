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

/// What came of a commissioning attempt.
///
/// #### Success does not mean you got a device
///
/// This is the type that has to carry the least comfortable fact in the whole
/// API. Commissioning adds an accessory to the **user's ecosystem**, and
/// whether your app can then see or control it is a separate question with a
/// different answer per backend:
///
/// - **iOS, MatterSupport** -- the accessory joins the user's HomeKit home and
///   appears in your graph on the next refresh, where you can control it like
///   any other. **This result carries no id**: Apple's sheet reports that the
///   flow finished and does not say what was added or which home it went to,
///   and the user is free to pick a different home than the one you asked
///   for. Call `com.codename1.home.SmartHome#refresh()` and look at what is
///   new. [#wasCommissionedToThisApp()] is `false` unless the build asked for
///   a fabric of its own -- see
///   [CommissioningRequest#setCommissionToThisApp(boolean)] -- in which case a
///   successful flow means the accessory joined it, because the extension's
///   commissioning step failing is what would have failed the flow.
/// - **Android, Google Home APIs** -- the accessory joins the user's Google
///   Home and, if the user granted your app access to that structure, appears
///   in your graph. You get an id and it works.
/// - **Android, Play services commissioning alone** -- the accessory joins the
///   user's Google Home and **your app is told nothing more.** There is no id,
///   and there is no graph to look it up in; see
///   `com.codename1.home.HomeAvailability#COMMISSIONING_ONLY`.
///
/// [#wasCommissionedToThisApp()] is that distinction, made explicit so it
/// cannot be missed. An app that assumes it got a device shows a "your new
/// device" screen with nothing on it -- and on two of these three backends
/// that is what it would get.
public final class CommissioningResult {

    private final String accessoryId;
    private final String accessoryName;
    private final String structureId;
    private final boolean commissionedToThisApp;

    /// Creates a result. Called by the ports.
    ///
    /// #### Parameters
    ///
    /// - `accessoryId`: the new accessory, or `null` when the backend did not
    ///   say
    ///
    /// - `accessoryName`: the name the accessory ended up with, or `null`
    ///
    /// - `structureId`: the home it joined, or `null`
    ///
    /// - `commissionedToThisApp`: whether this app can address the accessory
    public CommissioningResult(String accessoryId, String accessoryName,
            String structureId, boolean commissionedToThisApp) {
        this.accessoryId = accessoryId;
        this.accessoryName = accessoryName == null ? "" : accessoryName;
        this.structureId = structureId;
        this.commissionedToThisApp = commissionedToThisApp;
    }

    /// Whether this app can address the new accessory.
    ///
    /// When `false`, the accessory was added to the user's home successfully
    /// and there is nothing more you can do with it from here -- tell the user
    /// it worked and send them to their ecosystem app, rather than showing an
    /// empty device screen. See the class note.
    ///
    /// #### Returns
    ///
    /// `true` when [#getAccessoryId()] is usable
    public boolean wasCommissionedToThisApp() {
        return commissionedToThisApp;
    }

    /// The new accessory's identifier.
    ///
    /// #### Returns
    ///
    /// the identifier, or `null` when
    /// [#wasCommissionedToThisApp()] is `false`
    public String getAccessoryId() {
        return accessoryId;
    }

    /// The name the accessory ended up with, which is the user's choice rather
    /// than the one suggested.
    ///
    /// #### Returns
    ///
    /// the name, never `null`; empty when the backend did not say
    public String getAccessoryName() {
        return accessoryName;
    }

    /// The home the accessory joined.
    ///
    /// The user can pick a different home than the one requested, so this is
    /// worth reading rather than assuming.
    ///
    /// #### Returns
    ///
    /// the structure identifier, or `null` when the backend did not say
    public String getStructureId() {
        return structureId;
    }

    @Override
    public String toString() {
        return "CommissioningResult["
                + (commissionedToThisApp ? "mine " + accessoryId : "external")
                + (accessoryName.length() > 0 ? " " + accessoryName : "")
                + "]";
    }
}
