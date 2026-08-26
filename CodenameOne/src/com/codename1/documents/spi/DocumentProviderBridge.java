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
package com.codename1.documents.spi;

/// The platform seam of the document provider framework, implemented by ports and returned from
/// `CodenameOneImplementation.getDocumentProviderBridge()` (null on unsupported ports, making the
/// whole public API an inert no-op).
///
/// Everything crosses this boundary as data -- a JSON index produced by the core serializer and
/// files on disk -- never as live model objects. On iOS and macOS the reader is an app extension
/// in a *separate process* that runs while the app is dead, so implementations MUST persist what
/// they are given into a container both processes can reach (the App Group container on Apple
/// platforms; ordinary app storage on Android, where the provider shares the app's process).
///
/// Nothing here blocks on the platform browser: publishing writes the index, and `signalChange`
/// is a hint that the browser should re-enumerate at its own convenience.
public interface DocumentProviderBridge {
    /// Returns true when this port can expose the app's documents to the system file browser.
    boolean isDocumentProviderSupported();

    /// Returns the absolute `com.codename1.io.FileSystemStorage` path of the directory whose
    /// contents both the app and the platform reader can see, creating it if needed.
    ///
    /// #### Returns
    ///
    /// the shared directory path, or null when this port has nowhere to put one
    String getSharedDirectory();

    /// Atomically replaces the persisted document index and leaves it where the platform reader
    /// will find it.
    ///
    /// Atomically matters: the reader may wake at any moment, including midway through this call,
    /// and a half-written index is a file browser showing half a tree.
    ///
    /// #### Parameters
    ///
    /// - `indexJson`: the serialized tree
    void publishIndex(String indexJson);

    /// Records where the platform reader should fetch content it cannot find locally.
    ///
    /// #### Parameters
    ///
    /// - `endpoint`: the HTTPS base URL, or null to serve only from the shared directory
    /// - `authToken`: a bearer token sent with each request, or null for none
    void setRemoteEndpoint(String endpoint, String authToken);

    /// Hints that the published index changed and the platform browser should re-enumerate.
    void signalChange();

    /// Forgets the published index, the endpoint and the token, and empties the shared directory.
    void clear();
}
