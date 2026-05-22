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
package com.codename1.nfc;

/// Application-supplied handler for Host Card Emulation (HCE) -- the mode
/// where the device acts as a contactless smart card and answers APDUs
/// from a nearby reader/terminal.
///
/// Subclass this and register the instance via
/// [Nfc#registerHostCardEmulationService(HostCardEmulationService)] before
/// the OS routes a terminal's APDU to the app.
///
/// #### Lifecycle
///
/// 1. Reader / POS terminal sends an ISO 7816 `SELECT` APDU naming an AID
///    that matches [#getAids()].
/// 2. OS routes that APDU and every subsequent APDU in the same field
///    session to [#processCommand(byte[])].
/// 3. The implementation returns the response (data + 2-byte status word).
///    Use [ApduResponse] helpers to construct typical responses.
/// 4. [#onDeactivated(int)] fires when the reader leaves the field or
///    routes a SELECT for a different AID.
///
/// #### Platform support
///
/// - **Android** -- backed by `android.nfc.cardemulation.HostApduService`.
///   The Codename One Maven plugin and BuildDaemon auto-generate the
///   `AndroidManifest.xml` service entry and the `apduservice.xml`
///   resource from [#getAids()] / [#getServiceDescription()] /
///   [#getCategory()] when an app references this class.
/// - **iOS** -- backed by Core NFC's `CardSession` (iOS 17.4+, EU only as
///   of 2026-05-21) and requires the `com.apple.developer.nfc.hce` /
///   `com.apple.developer.nfc.hce.iso7816.select-identifiers`
///   entitlements. The IPhoneBuilder injects both entitlements when the
///   app references this class.
/// - **JavaSE simulator** -- the Simulate -> NFC menu fires synthetic
///   APDUs at the registered service so the implementation can be
///   exercised without a terminal.
/// - **All other platforms** -- the OS never invokes the service.
public abstract class HostCardEmulationService {

    /// Categories accepted by Android's `HostApduService` -- "payment" is
    /// reserved for EMV-conformant payment apps; everything else uses
    /// "other".
    public static final String CATEGORY_OTHER = "other";
    public static final String CATEGORY_PAYMENT = "payment";

    /// The application identifiers (AIDs) this service is willing to
    /// answer. Each AID is 5-16 bytes long. Must be non-empty -- a service
    /// that returns an empty array is never invoked.
    ///
    /// The platform routes terminal APDUs to the longest matching AID, so
    /// list specific AIDs before catch-alls.
    public abstract String[] getAids();

    /// HCE category -- one of [#CATEGORY_OTHER] or [#CATEGORY_PAYMENT].
    /// Defaults to [#CATEGORY_OTHER].
    public String getCategory() {
        return CATEGORY_OTHER;
    }

    /// Human-readable description registered with Android (shown to the
    /// user when they pick a default HCE app in system settings). Default
    /// is the service class' simple name.
    public String getServiceDescription() {
        return getClass().getName();
    }

    /// Handles a single APDU command and returns the response. Must return
    /// in less than ~500 ms; longer responses are dropped by the
    /// controller. The return value must end with a 2-byte ISO 7816 status
    /// word; see [ApduResponse] helpers.
    ///
    /// The first APDU after activation is always a `SELECT` for one of the
    /// AIDs reported by [#getAids()]; subsequent APDUs are
    /// application-specific.
    public abstract byte[] processCommand(byte[] apdu);

    /// Called when the reader leaves the field or sends a `SELECT` for a
    /// different AID. `reason` is one of [#DEACTIVATION_LINK_LOSS] or
    /// [#DEACTIVATION_DESELECTED].
    public void onDeactivated(int reason) {
    }

    /// `reason` value for [#onDeactivated(int)] -- the reader left the
    /// field.
    public static final int DEACTIVATION_LINK_LOSS = 0;
    /// `reason` value for [#onDeactivated(int)] -- the reader sent a
    /// `SELECT` for a different AID.
    public static final int DEACTIVATION_DESELECTED = 1;
}
