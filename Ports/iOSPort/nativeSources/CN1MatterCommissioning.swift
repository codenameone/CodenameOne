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

//
//  CN1MatterCommissioning.swift
//  The MatterSupport half of com.codename1.home.commissioning.
//
//  This file exists because MatterSupport is Swift-only. MatterAddDeviceRequest
//  is a Swift struct whose perform() is async throws, and Apple exposes no
//  Objective-C interface to it -- so the rest of the smart-home bridge, which
//  is Objective-C, cannot reach it directly.
//
//  The builder adds this source, the MatterSupport framework, the
//  com.apple.developer.matter.allow-setup-payload entitlement, an app group
//  and a generated app-extension target only for apps that reference
//  com.codename1.home.commissioning. An app that only reads its lights carries
//  none of it.
//

import Foundation

#if canImport(MatterSupport)
import Matter
import MatterSupport

/// Runs Apple's add-device sheet and reports the outcome back to
/// `CN1SmartHome.m`.
///
/// #### Both directions are looked up by name
///
/// Swift reaches Objective-C in a mixed target through a bridging header, and
/// Objective-C reaches Swift through a generated `<Module>-Swift.h`. Both file
/// names contain the *application's* module name, which is whatever the
/// developer called their project -- and this file ships inside the port, long
/// before that name exists.
///
/// So neither header is used. Each side finds the other with
/// `NSClassFromString` and passes one dictionary, which
/// `performSelector:withObject:` can carry. The cost is a class lookup per
/// commissioning flow, which happens once per accessory a user adds by hand.
@objc(CN1MatterCommissioning)
public final class CN1MatterCommissioning: NSObject {

    /// The name Objective-C invokes, and the only entry point.
    ///
    /// - Parameter request: `requestId`, `setupPayload`, `structureId`,
    ///   `roomId`, `suggestedName` and `timeoutMillis`, exactly as
    ///   `homeCommission` packed them.
    @objc(commission:)
    public static func commission(_ request: [String: Any]) {
        let requestId = (request["requestId"] as? NSNumber)?.int32Value ?? 0
        let payloadText = request["setupPayload"] as? String ?? ""
        let suggestedName = request["suggestedName"] as? String ?? ""
        let structureId = request["structureId"] as? String ?? ""

        guard #available(iOS 16.1, *) else {
            deliver(requestId: requestId, structureId: structureId,
                    error: "COMMISSIONING_UNAVAILABLE\tadding a Matter "
                         + "accessory needs iOS 16.1 or newer")
            return
        }

        Task {
            do {
                let request = MatterAddDeviceRequest(
                    topology: topology(named: suggestedName),
                    setupPayload: setupPayload(from: payloadText))
                try await request.perform()
                // Success, and deliberately with no accessory id.
                //
                // MatterSupport reports that the flow finished and does not
                // say what was added: the accessory joins the user's HomeKit
                // home, and it turns up in the graph on the next refresh like
                // any other. Inventing an id here would make
                // wasCommissionedToThisApp() answer true for something the
                // app cannot address.
                deliver(requestId: requestId, structureId: structureId,
                        error: nil)
            } catch {
                deliver(requestId: requestId, structureId: structureId,
                        error: describe(error))
            }
        }
    }

    /// The ecosystem and home names Apple's sheet shows.
    ///
    /// The app's display name, because that is what the user recognizes --
    /// this is the "add to <ecosystem>" line in the sheet, and a bundle
    /// identifier there reads like a bug.
    @available(iOS 16.1, *)
    private static func topology(named suggestedName: String)
            -> MatterAddDeviceRequest.Topology {
        let bundle = Bundle.main
        let appName = (bundle.object(
                forInfoDictionaryKey: "CFBundleDisplayName") as? String)
            ?? (bundle.object(forInfoDictionaryKey: "CFBundleName") as? String)
            ?? "Codename One"
        let homeName = suggestedName.isEmpty ? appName : suggestedName
        return MatterAddDeviceRequest.Topology(
            ecosystemName: appName,
            homes: [MatterAddDeviceRequest.Home(displayName: homeName)])
    }

    /// Parses a scanned code, or answers nil so Apple's sheet scans one
    /// itself.
    ///
    /// Nil is a working outcome rather than a failure: with no payload the
    /// sheet asks the user to scan, which is exactly what an app that has not
    /// scanned one wants.
    ///
    /// #### Below iOS 17.6 a scanned code is not passed through
    ///
    /// `MTRSetupPayload(payload:)` arrived in 17.6. The spelling those
    /// releases have -- `setupPayloadWithOnboardingPayload:error:` -- was
    /// deprecated at the same time and is no longer surfaced in Swift by the
    /// current SDK, so there is nothing left to call.
    ///
    /// The consequence is worth stating plainly: on iOS 16.1 to 17.5 a code
    /// your app already scanned is dropped and Apple's sheet asks the user to
    /// scan it again. Annoying, and it still commissions the accessory. The
    /// alternative was reaching the removed selector dynamically, which trades
    /// a visible re-scan for a silent failure the first time Apple drops the
    /// symbol.
    @available(iOS 16.1, *)
    private static func setupPayload(from text: String) -> MTRSetupPayload? {
        if text.isEmpty {
            return nil
        }
        if #available(iOS 17.6, *) {
            return MTRSetupPayload(payload: text)
        }
        return nil
    }

    /// The portable HomeError name for a MatterSupport failure.
    ///
    /// A user backing out of the sheet is by far the most common outcome and
    /// is not an error worth reporting as one -- an app that showed a red
    /// banner every time somebody changed their mind would be wrong far more
    /// often than it was right.
    private static func describe(_ error: Error) -> String {
        let nsError = error as NSError
        if nsError.domain == NSCocoaErrorDomain
                && nsError.code == NSUserCancelledError {
            return "USER_CANCELED\tthe user closed the add-device sheet"
        }
        let message = nsError.localizedDescription
            .replacingOccurrences(of: "\t", with: " ")
            .replacingOccurrences(of: "\n", with: " ")
        return "COMMISSIONING_FAILED\t" + message
    }

    private static func deliver(requestId: Int32, structureId: String,
                                error: String?) {
        var result: [String: Any] = ["requestId": NSNumber(value: requestId)]
        if !structureId.isEmpty {
            result["structureId"] = structureId
        }
        if let error = error {
            result["error"] = error
        }
        guard let bridge: AnyClass = NSClassFromString("CN1MatterBridge") else {
            // The Objective-C half is missing, which means the build enabled
            // Matter setup without the smart-home natives. Nothing can be
            // reported, and there is nowhere to report it to -- so this is
            // the one silent failure in the file, and it cannot happen in a
            // build the builder produced.
            return
        }
        _ = (bridge as AnyObject).perform(NSSelectorFromString("deliver:"),
                                          with: result)
    }
}

#endif
