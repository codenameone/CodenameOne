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

import Foundation

/// The Objective-C-visible face of the app intents support.
///
/// Two directions meet here and they use different mechanisms, for a reason worth stating:
///
/// - **Java to Swift** arrives as `objc_msgSend` on this class, looked up by
///   `NSClassFromString("CN1IntentBridge")` from `IOSNative.m`. That indirection is what lets an
///   app link this file only when it declares an intent, without the C side referencing a Swift
///   symbol that might not exist.
/// - **Swift to Java** never happens directly. It goes through `CN1IntentHost`, an Objective-C
///   class, because the translator's dead-code eliminator only scans `.m` sources for mangled
///   Java symbols; a Java method named solely from Swift is silently reduced to an empty stub.
@objc(CN1IntentBridge)
public class CN1IntentBridge: NSObject {

    /// The application's intent catalogue, published once at startup. Held so the generated
    /// declarations can look up titles and parameter prompts without crossing back into Java.
    private static var declarations: [String: [String: Any]] = [:]
    private static let lock = NSLock()

    @objc public static func registerIntents(_ json: String) {
        guard let data = json.data(using: .utf8),
              let doc = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let list = doc["intents"] as? [[String: Any]] else {
            return
        }
        lock.lock()
        defer { lock.unlock() }
        declarations.removeAll()
        for entry in list {
            if let id = entry["id"] as? String {
                declarations[id] = entry
            }
        }
    }

    static func declaration(_ id: String) -> [String: Any]? {
        lock.lock()
        defer { lock.unlock() }
        return declarations[id]
    }

    /// Called from Java with the outcome of an invocation the system started.
    @objc public static func completeInvocation(_ token: String, resultJson: String) {
        CN1IntentHost.completeToken(token, resultJson: resultJson)
    }

    // MARK: - Helpers shared by the generated declarations

    /// Runs an intent and waits for the framework's answer without blocking a thread.
    ///
    /// The continuation is resumed from `CN1IntentHost`, which removes the token before firing,
    /// so the deadline racing a slow handler cannot resume it twice -- that would be a hard
    /// crash rather than a recoverable error.
    static func run(id: String, params: [String: Any], headless: Bool) async -> CN1IntentOutcome {
        let json = encode(params)
        let raw: String = await withCheckedContinuation { continuation in
            CN1IntentHost.performIntent(id, paramsJson: json, headless: headless) { result in
                continuation.resume(returning: result ?? "{}")
            }
        }
        return CN1IntentOutcome(json: raw)
    }

    static func entities(type: String, kind: String, argument: String?) -> [CN1EntityValue] {
        guard let json = CN1IntentHost.queryEntities(type, kind: kind, argument: argument),
              let data = json.data(using: .utf8),
              let doc = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let list = doc["entities"] as? [[String: Any]] else {
            return []
        }
        return list.compactMap { CN1EntityValue(dictionary: $0) }
    }

    static func encode(_ params: [String: Any]) -> String {
        guard JSONSerialization.isValidJSONObject(params),
              let data = try? JSONSerialization.data(withJSONObject: params),
              let s = String(data: data, encoding: .utf8) else {
            return "{}"
        }
        return s
    }
}

/// One entity as the platform sees it: identity plus what to display. Deliberately not the
/// application's own type -- that lives in Java and never crosses.
public struct CN1EntityValue {
    public let type: String
    public let id: String
    public let title: String
    public let subtitle: String?

    init?(dictionary: [String: Any]) {
        guard let type = dictionary["type"] as? String,
              let id = dictionary["id"] as? String else {
            return nil
        }
        self.type = type
        self.id = id
        self.title = (dictionary["title"] as? String) ?? id
        self.subtitle = dictionary["subtitle"] as? String
    }
}

/// Thrown when a handler reported a failure.
///
/// An intent that fails has to *fail*: returning a dialog-only success would make Shortcuts
/// record the action as successful and run everything after it, which is worse than the error
/// itself. The message is the one the handler wrote for the user.
///
/// Availability-fenced because `LocalizedStringResource` is itself iOS 16+, and this file has to
/// compile on whatever deployment target the application chose. Only the generated App Intents
/// declarations reference it, and those carry the same fence.
#if canImport(AppIntents)
@available(iOS 16.0, *)
public struct CN1IntentFailure: Error, CustomLocalizedStringResourceConvertible {
    public let message: String

    public var localizedStringResource: LocalizedStringResource {
        LocalizedStringResource(stringLiteral: message.isEmpty
            ? "The action could not be completed" : message)
    }
}
#endif

/// The decoded result of one invocation.
public struct CN1IntentOutcome {
    public let ok: Bool
    public let value: String?
    public let dialog: String?
    public let openUrl: String?
    public let error: String?

    init(json: String) {
        let doc = (try? JSONSerialization.jsonObject(
            with: Data(json.utf8))) as? [String: Any] ?? [:]
        // A result with no "ok" key at all is treated as success: the framework always writes
        // one, so its absence means an empty payload rather than a reported failure.
        self.ok = (doc["ok"] as? Bool) ?? true
        self.dialog = doc["dialog"] as? String
        self.openUrl = doc["openUrl"] as? String
        self.error = doc["error"] as? String
        // Reduced to text on purpose, and it is a real limitation rather than an oversight.
        //
        // ReturnsValue<T> fixes T in the struct's signature at build time, but a handler's value
        // is IntentResult.value(Object) -- there is nothing in the declaration that says what
        // type it will be, and it can legitimately differ between two invocations of the same
        // intent. Declaring ReturnsValue<String> and stringifying is the only choice that is
        // type-stable for every handler; the alternative is refusing to return a value at all,
        // which is strictly worse for the following Shortcuts action.
        //
        // The wire document keeps the JSON type, so a consumer that cares can read it there.
        // Typing this properly needs the declaration to state a return type, which is a
        // deliberate future addition rather than something derivable from what exists today.
        if let v = doc["value"] {
            self.value = String(describing: v)
        } else {
            self.value = nil
        }
    }

    /// What the assistant says. Falls back to the failure message so an error is never silent.
    public var spoken: String {
        if let d = dialog, !d.isEmpty { return d }
        if !ok, let e = error, !e.isEmpty { return e }
        return ""
    }
}
