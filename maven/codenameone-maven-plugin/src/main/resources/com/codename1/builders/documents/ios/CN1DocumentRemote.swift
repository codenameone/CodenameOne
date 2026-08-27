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
import CommonCrypto
import FileProvider
import Foundation

/// Fetches content the shared container does not hold from the endpoint the app configured.
///
/// The app is not running when this executes, so everything the request needs -- the base URL and
/// the bearer token -- was persisted into the shared container by
/// `com.codename1.documents.DocumentProvider.setRemoteEndpoint`.
enum CN1DocumentRemote {
    struct Settings: Decodable {
        let endpoint: String?
        let authToken: String?
    }

    /// Identifies the credentials a fetch was authorized with.
    ///
    /// A download outlives the request that started it, and the guard that it still names the
    /// same node and the same remote object is not enough on its own: an account switch reuses
    /// node ids -- "inbox", "invoice-1" -- and can reuse the server's keys for them too, at which
    /// point the previous account's bytes would pass both checks and land in the new account's
    /// file. What always changes across a switch is the credential this stamp names.
    ///
    /// The index revision was the obvious alternative and is the wrong one: it moves on every
    /// publish, so an app that syncs while a large file downloads would throw that download away
    /// and start again, over and over.
    ///
    /// The token is not returned, only compared, and it never leaves the extension.
    /// A short, non-secret value that changes whenever the credential does.
    ///
    /// Folded into the content version of every remote item, which is what makes an account
    /// switch invalidate what the browser has cached. Without it a switch that reuses a node id,
    /// its remote id and its declared size and date produces the same version, and File Provider
    /// keeps serving the previous account's materialized bytes -- never asking for the item at
    /// all, so the credential check on the download path is never reached.
    ///
    /// Hashed rather than used directly, because a content version is a value the system stores
    /// and hands around: the bearer token must not travel in it. Truncated to 128 bits, which
    /// nothing accidental collides.
    ///
    /// The settings are READ every time; only the hashing is memoized, and against the settings
    /// themselves. Keying the memo on the file's modification date instead meant two credentials
    /// written inside one filesystem tick shared a key, and the second account was handed the
    /// first one's generation -- an unchanged content version, so the browser kept serving what
    /// it had materialized under the old token. Reading a hundred bytes per item is cheap; the
    /// digest is what was worth keeping.
    /// A fixed-width digest of a string, for anywhere a stamp has to fit a budget.
    ///
    /// CommonCrypto rather than CryptoKit, for the reason given below: this file is compiled
    /// into the pre-iOS-16 provider too, whose deployment target can be iOS 11.
    static func digest(_ value: String) -> Data {
        let bytes = Array(value.utf8)
        var out = [UInt8](repeating: 0, count: Int(CC_SHA256_DIGEST_LENGTH))
        CC_SHA256(bytes, CC_LONG(bytes.count), &out)
        return Data(out)
    }

    static func credentialGeneration(containerURL: URL) -> String {
        let stamp = credentialStamp(containerURL: containerURL)
        return generationLock.withLock {
            if stamp == lastStamp {
                return lastGeneration
            }
            // CommonCrypto rather than CryptoKit: this file is also compiled into the
            // pre-iOS-16 provider, whose deployment target can be iOS 11 or 12, and CryptoKit
            // starts at 13. Swift checks availability at compile time whether or not the call is
            // reachable, so importing it there fails the build outright.
            let bytes = Array(stamp.utf8)
            var digest = [UInt8](repeating: 0, count: Int(CC_SHA256_DIGEST_LENGTH))
            CC_SHA256(bytes, CC_LONG(bytes.count), &digest)
            let hex = digest.prefix(16).map { String(format: "%02x", $0) }.joined()
            lastStamp = stamp
            lastGeneration = hex
            return hex
        }
    }

    private static let generationLock = NSLock()
    /// The credential the memoized generation belongs to. One entry, not a map: an enumeration
    /// asks about one credential over and over, and a changed one replaces it.
    private static var lastStamp: String?
    private static var lastGeneration = ""


    static func credentialStamp(containerURL: URL) -> String {
        stamp(settings(containerURL: containerURL))
    }

    /// The stamp of settings already in hand.
    ///
    /// A request has to be validated against the credential it was actually SENT with. Reading
    /// the file once for the stamp and again inside the fetch let the two disagree: an account
    /// changing A to B and back while a slow response was in flight left the check comparing A
    /// against A while the request had gone out with B's token, so B's bytes were served through
    /// A's publication. The caller captures the settings once and both use that.
    static func stamp(_ settings: Settings?) -> String {
        guard let settings = settings else {
            return ""
        }
        return (settings.endpoint ?? "") + "\u{0}" + (settings.authToken ?? "")
    }

    static func settings(containerURL: URL) -> Settings? {
        let url = containerURL.appendingPathComponent("cn1documents/endpoint.json")
        guard let data = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder().decode(Settings.self, from: data)
    }

    /// Downloads one remote item into a temporary file.
    ///
    /// Returns the task so the caller can cancel it. File Provider hands the user's cancel or
    /// dismiss to the `Progress` returned by `fetchContents`; without the task behind it, that
    /// cancel stops nothing and a large download keeps running -- on the user's mobile data, in a
    /// process the system already memory-limits harder than the app.
    /// The task is returned SUSPENDED: the caller resumes it once it has been registered
    /// wherever a cancellation would look for it. Starting it here left a window in which a stop
    /// found nothing to cancel and the download ran to completion anyway, paid for out of the
    /// user's data rather than out of a check.
    ///
    /// - Parameter destination: the directory the downloaded file is moved into. The replicated
    ///   provider has to hand the system a file on the same volume as the user-visible URL, which
    ///   the process temporary directory is not guaranteed to be, so it passes the directory
    ///   `NSFileProviderManager` named. The classic provider copies the result itself and passes
    ///   the ordinary temporary directory.
    @discardableResult
    static func fetch(remoteId: String, settings: Settings?,
                      destination: URL = FileManager.default.temporaryDirectory,
                      completion: @escaping (URL?, Error?) -> Void) -> URLSessionTask? {
        guard let settings = settings,
              let base = settings.endpoint,
              var components = URLComponents(string: base) else {
            completion(nil, CN1DocumentRemote.noEndpoint())
            return nil
        }
        // Appended to the PATH, and the id added to whatever query the endpoint already carries.
        // Pasting "/fetch" onto the raw string put it inside the query of an endpoint like
        // "https://api.example.com/documents?tenant=42", and replacing queryItems then threw the
        // tenant away -- the request went to "/documents?id=..." with the suffix gone and the
        // caller's own parameter lost.
        var path = components.path
        if !path.hasSuffix("/") {
            path += "/"
        }
        components.path = path + "fetch"
        var items = components.queryItems ?? []
        items.append(URLQueryItem(name: "id", value: remoteId))
        components.queryItems = items
        guard let url = components.url else {
            completion(nil, CN1DocumentRemote.noEndpoint())
            return nil
        }
        var request = URLRequest(url: url)
        if let token = settings.authToken, !token.isEmpty {
            request.setValue("Bearer " + token, forHTTPHeaderField: "Authorization")
        }
        // A download task rather than a data task: an item in a document browser can be far larger
        // than the extension's memory budget, and app extensions are killed well below the app's.
        let task = URLSession.shared.downloadTask(with: request) { location, response, error in
            if let error = error {
                completion(nil, error)
                return
            }
            let status = (response as? HTTPURLResponse)?.statusCode ?? -1
            if status == 401 || status == 403 {
                // A rejected or expired token comes back as an ordinary HTTP response, not as a
                // URLSession error, so it has to be recognised here: the generic path below
                // produces an error in this file's own domain, which providerError can only wrap
                // as an opaque Cocoa failure. .notAuthenticated is the one the browser
                // understands -- it presents the location as needing to be signed in to again
                // instead of just failing the open.
                completion(nil, NSFileProviderError(.notAuthenticated))
                return
            }
            guard let location = location, status == 200 else {
                completion(nil, CN1DocumentRemote.badResponse(response))
                return
            }
            // The URL the session hands back is deleted the moment this closure returns, so the
            // bytes are moved somewhere the caller can still hand to the browser.
            let dest = destination.appendingPathComponent(UUID().uuidString)
            do {
                try FileManager.default.moveItem(at: location, to: dest)
                completion(dest, nil)
            } catch {
                completion(nil, error)
            }
        }
        return task
    }

    /// Maps anything that can go wrong here into a domain the replicated provider is allowed to
    /// report. Apple: "Errors must be in one of the following domains: NSCocoaErrorDomain,
    /// NSFileProviderErrorDomain." A raw URLSession failure is NSURLErrorDomain, which is neither,
    /// and reporting one leaves the browser with an error it does not know how to present.
    static func providerError(_ error: Error?) -> Error {
        guard let error = error else {
            return NSFileProviderError(.noSuchItem)
        }
        let ns = error as NSError
        if ns.domain == NSCocoaErrorDomain || ns.domain == NSFileProviderError.errorDomain {
            return error
        }
        if ns.domain == NSURLErrorDomain {
            switch ns.code {
            case NSURLErrorCancelled:
                return CocoaError(.userCancelled)
            case NSURLErrorUserAuthenticationRequired:
                // Distinct on purpose: the browser offers the user a way to sign the location in
                // again, which it cannot do for a generic transport failure.
                return NSFileProviderError(.notAuthenticated)
            default:
                // Everything else the network can do -- offline, DNS, TLS, timeout -- is the same
                // thing from the browser's point of view: try again later.
                return NSFileProviderError(.serverUnreachable)
            }
        }
        return NSError(domain: NSCocoaErrorDomain, code: NSXPCConnectionReplyInvalid, userInfo: [
            NSUnderlyingErrorKey: error
        ])
    }

    private static func noEndpoint() -> NSError {
        NSError(domain: "com.codename1.documents", code: 1, userInfo: [
            NSLocalizedDescriptionKey:
                "This item is stored remotely but no endpoint was configured. Call "
                + "DocumentProvider.setRemoteEndpoint before publishing remote items."
        ])
    }

    private static func badResponse(_ response: URLResponse?) -> NSError {
        let code = (response as? HTTPURLResponse)?.statusCode ?? -1
        return NSError(domain: "com.codename1.documents", code: 2, userInfo: [
            NSLocalizedDescriptionKey: "The document endpoint answered \(code)."
        ])
    }
}
