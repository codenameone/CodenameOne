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
