import Foundation
import UniformTypeIdentifiers

/// One entry of the index the app published. Mirrors com.codename1.documents.DocumentNode.
struct CN1DocumentNode: Decodable {
    let id: String
    let name: String?
    let folder: Bool
    let contentType: String?
    let path: String?
    let remoteId: String?
    let size: Int64?
    let lastModified: Int64?
    let readOnly: Bool?
    let children: [CN1DocumentNode]?
}

struct CN1DocumentIndexDocument: Decodable {
    let v: Int
    let root: CN1DocumentNode
}

/// The published tree, flattened once into identifier-keyed lookups.
final class CN1DocumentIndex {
    static let rootIdentifier = "root"

    private(set) var nodes: [String: CN1DocumentNode] = [:]
    private(set) var parents: [String: String] = [:]
    private(set) var childIds: [String: [String]] = [:]
    let rootId: String

    init(root: CN1DocumentNode) {
        rootId = root.id
        index(root, parent: nil)
    }

    private func index(_ node: CN1DocumentNode, parent: String?) {
        nodes[node.id] = node
        if let parent = parent {
            parents[node.id] = parent
            childIds[parent, default: []].append(node.id)
        }
        for child in node.children ?? [] {
            index(child, parent: node.id)
        }
    }

    /// Reads the index the app last published into the shared container. Returns nil when the app
    /// has published nothing yet, which is an empty file browser rather than an error.
    static func load(containerURL: URL) -> CN1DocumentIndex? {
        let url = containerURL.appendingPathComponent("cn1documents/index.json")
        guard let data = try? Data(contentsOf: url) else {
            return nil
        }
        guard let doc = try? JSONDecoder().decode(CN1DocumentIndexDocument.self, from: data) else {
            return nil
        }
        return CN1DocumentIndex(root: doc.root)
    }
}
