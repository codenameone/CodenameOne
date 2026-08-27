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
    @discardableResult
    static func fetch(remoteId: String, containerURL: URL,
                      completion: @escaping (URL?, Error?) -> Void) -> URLSessionTask? {
        guard let settings = settings(containerURL: containerURL),
              let base = settings.endpoint,
              var components = URLComponents(string: base.hasSuffix("/") ? base + "fetch"
                                                                         : base + "/fetch") else {
            completion(nil, CN1DocumentRemote.noEndpoint())
            return nil
        }
        components.queryItems = [URLQueryItem(name: "id", value: remoteId)]
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
            guard let location = location,
                  let http = response as? HTTPURLResponse, http.statusCode == 200 else {
                completion(nil, CN1DocumentRemote.badResponse(response))
                return
            }
            // The URL the session hands back is deleted the moment this closure returns, so the
            // bytes are moved somewhere the caller can still hand to the browser.
            let dest = FileManager.default.temporaryDirectory
                .appendingPathComponent(UUID().uuidString)
            do {
                try FileManager.default.moveItem(at: location, to: dest)
                completion(dest, nil)
            } catch {
                completion(nil, error)
            }
        }
        task.resume()
        return task
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
