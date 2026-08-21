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
import SwiftUI

/// Renders an intent result's snippet.
///
/// A snippet is a small layout the platform shows while the application may be off screen,
/// which is exactly what a home-screen widget is -- so it reuses the surfaces node catalog and
/// the same `cn1RenderNode` renderer rather than introducing a second layout vocabulary. The
/// renderer is data-driven, so the two use sites share it unchanged; the build writes it into
/// this target under its own filename, which is what keeps an app that uses both features from
/// getting two copies.
@available(iOS 16.0, *)
struct CN1IntentSnippetView: View {
    let node: [String: Any]?
    let imagesDir: URL?

    var body: some View {
        if let node = node {
            cn1RenderNode(node, CN1RenderContext(
                state: [:],
                imagesDir: imagesDir,
                // A snippet has no widget kind behind it, and no node-level link makes sense
                // here: the result already carries its own way to continue into the app.
                source: "intent",
                allowLinks: false,
                parentAxis: nil))
        } else {
            EmptyView()
        }
    }
}
