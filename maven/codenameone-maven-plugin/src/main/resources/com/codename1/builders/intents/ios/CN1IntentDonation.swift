//
// Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
// This code is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License version 2 only, as
// published by the Free Software Foundation.  Codename One designates this
// particular file as subject to the "Classpath" exception as provided
// by Oracle in the LICENSE file that accompanied this code.
//
// Please contact Codename One through http://www.codenameone.com/ if you
// need additional information or have any questions.
//

import Foundation

#if canImport(AppIntents)
import AppIntents

/// Donation, kept in its own file so everything that touches the App Intents framework sits
/// behind one `canImport` boundary.
///
/// Donating is a hint, never a requirement: the system uses it to predict and suggest. So every
/// failure path here is a no-op rather than an error propagated back to the application, which
/// would turn "the OS declined to learn something" into a visible fault.
@available(iOS 16.0, *)
enum CN1IntentDonation {
    static func donate(intentId: String, paramsJson: String) {
        // An intent the app never declared cannot be donated; the catalogue is compiled in.
        guard CN1IntentBridge.declaration(intentId) != nil else {
            return
        }
        let activity = NSUserActivity(activityType: intentId)
        activity.isEligibleForPrediction = true
        activity.isEligibleForSearch = true
        if let decl = CN1IntentBridge.declaration(intentId) {
            activity.title = (decl["title"] as? String) ?? intentId
        }
        if let data = paramsJson.data(using: .utf8),
           let params = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
            // userInfo has to be plist-representable; anything else is dropped rather than
            // risking an exception inside a fire-and-forget hint.
            var safe: [String: Any] = [:]
            for (k, v) in params where v is String || v is NSNumber {
                safe[k] = v
            }
            activity.userInfo = safe
        }
        activity.becomeCurrent()
    }
}
#endif
