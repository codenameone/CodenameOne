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
    static func fetch(remoteId: String, containerURL: URL,
                      completion: @escaping (URL?, Error?) -> Void) {
        guard let settings = settings(containerURL: containerURL),
              let base = settings.endpoint,
              var components = URLComponents(string: base.hasSuffix("/") ? base + "fetch"
                                                                         : base + "/fetch") else {
            completion(nil, CN1DocumentRemote.noEndpoint())
            return
        }
        components.queryItems = [URLQueryItem(name: "id", value: remoteId)]
        guard let url = components.url else {
            completion(nil, CN1DocumentRemote.noEndpoint())
            return
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
