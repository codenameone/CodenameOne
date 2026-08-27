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
package com.codename1.documents;

import com.codename1.documents.spi.DocumentProviderBridge;
import com.codename1.io.Log;
import com.codename1.ui.Display;

/// The static entry point for exposing your app's documents to the system file browser: the Files
/// app on iOS, the storage picker on Android. Publish a tree, and your
/// content becomes browsable from outside your app -- and openable by other apps -- without the
/// user launching yours.
///
/// ```java
/// DocumentNode root = DocumentNode.folder("root", "My Invoices");
/// root.add(DocumentNode.file("inv-2031", "January.pdf")
///         .setContentType("application/pdf")
///         .setPath("invoices/january.pdf"));
/// DocumentProvider.publish(root);
/// ```
///
/// #### Two ways to supply content
///
/// *From the shared directory.* Write the bytes under `getSharedDirectory()` and point each node
/// at a relative `path`. Nothing else is involved: no server, no network code, and the content
/// opens instantly because it is already on the device.
///
/// *From your server.* Call `setRemoteEndpoint(String)` and give each node a `remoteId`. Content
/// is fetched on demand over HTTPS, which is what a cloud drive wants -- the index can list far
/// more than the device holds. A node may carry both, and a local copy always wins, which is how
/// a cached document opens without a round trip.
///
/// #### The reader is not your app
///
/// On iOS the browser talks to a generated app *extension*, a separate process that runs
/// while your app is dead and cannot call your Java code. That is why this API publishes data
/// rather than installing callbacks: the tree is serialized into a container both processes can
/// read, and the extension serves the browser from it. Publish whenever your data changes -- after
/// a sync, after a login -- not in response to being browsed, because you will not be asked.
///
/// Android has no such split (the provider runs in your app's process), but the same rule applies
/// so that one publishing discipline works everywhere.
///
/// #### Zero cost when unused
///
/// Merely referencing this package makes the build inject the native plumbing -- the file provider
/// extension and App Group on Apple platforms, the documents provider in the Android manifest.
/// Apps that never touch `com.codename1.documents` get none of it, and on unsupported ports the
/// whole API is an inert no-op.
///
/// If all you want is for your app's own documents folder to be visible in the Files app, you do
/// not need any of this -- see the developer guide, which describes the two build hints that do
/// that with no extension at all.
public final class DocumentProvider {
    private static DocumentProviderBridge bridge;
    private static boolean bridgeOverridden;

    private DocumentProvider() {
    }

    /// Returns true when this platform can expose documents to the system file browser.
    ///
    /// #### Returns
    ///
    /// true when document providing is supported
    public static boolean isSupported() {
        DocumentProviderBridge b = bridgeInternal();
        return b != null && b.isDocumentProviderSupported();
    }

    /// Returns the directory whose contents the platform reader can also see, creating it if
    /// needed. Write the bytes of any node that carries a `path` under here, using the ordinary
    /// `com.codename1.io.FileSystemStorage` API.
    ///
    /// On Apple platforms this is inside the App Group container rather than in your app's own
    /// sandbox, so a file written to `FileSystemStorage`'s home directory is *not* visible to the
    /// extension. Always resolve paths against this value.
    ///
    /// #### Returns
    ///
    /// the shared directory path, or null when unsupported on this port
    public static String getSharedDirectory() {
        DocumentProviderBridge b = bridgeInternal();
        return b == null ? null : b.getSharedDirectory();
    }

    /// Publishes the tree the file browser should show, replacing whatever was published before.
    ///
    /// The call returns as soon as the index is persisted; the browser picks it up on its own
    /// schedule. Publishing the same tree twice is harmless.
    ///
    /// #### Parameters
    ///
    /// - `root`: the root of the tree, must not be null
    public static void publish(DocumentNode root) {
        if (root == null) {
            throw new IllegalArgumentException("Cannot publish a null document tree");
        }
        DocumentProviderBridge b = bridgeInternal();
        if (b == null) {
            return;
        }
        try {
            b.publishIndex(DocumentIndexSerializer.serialize(root));
            b.signalChange();
        } catch (Throwable t) {
            // A publish is housekeeping on whatever thread the app's data happened to change on.
            // Letting a serialization or storage failure escape would take down that unrelated
            // flow, so it is logged and the previously published index simply stays current.
            Log.e(t);
        }
    }

    /// Sets the HTTPS endpoint the platform reader fetches remote content from, and the bearer
    /// token it presents. Call before publishing nodes that carry a `remoteId`.
    ///
    /// The endpoint is contacted by the extension, not by your app, so it must be reachable
    /// without any state your app holds in memory -- the token given here is all it carries. Keep
    /// the token fresh by calling this again whenever you renew it; the extension reads the latest
    /// value each time it runs.
    ///
    /// The developer guide documents the two requests the reader makes and the JSON it expects
    /// back.
    ///
    /// #### Parameters
    ///
    /// - `endpoint`: the HTTPS base URL, or null to serve only from the shared directory
    /// - `authToken`: a bearer token sent with each request, or null for none
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the endpoint is not HTTPS
    public static void setRemoteEndpoint(String endpoint, String authToken) {
        if (endpoint != null && endpoint.length() > 0 && !isHttps(endpoint)) {
            // Refused rather than passed on. The readers send the bearer token as an
            // Authorization header on every fetch, and they run outside this app -- so an
            // "http://" typo here would hand the token, and the documents it unlocks, to the
            // network in the clear on any platform whose cleartext policy still allows it.
            throw new IllegalArgumentException("The document endpoint must be HTTPS; got \""
                    + endpoint + "\".");
        }
        // Both values are written to disk as UTF-8, by every bridge. An unpaired surrogate has
        // no UTF-8 encoding and becomes "?", so the endpoint that is read back is a different URL
        // and the token that is sent is a different credential -- a request to the wrong place,
        // or one the server rejects, with nothing in either message pointing at the character
        // that did it.
        int badEndpoint = endpoint == null ? -1
                : DocumentIndexSerializer.loneSurrogateAt(endpoint);
        if (badEndpoint >= 0) {
            throw new IllegalArgumentException("The document endpoint contains an unpaired "
                    + "surrogate at index " + badEndpoint + ". It cannot be encoded as UTF-8, "
                    + "and the readers store it as UTF-8, so the URL they use would not be the "
                    + "one given here.");
        }
        int badToken = authToken == null ? -1
                : DocumentIndexSerializer.loneSurrogateAt(authToken);
        if (badToken >= 0) {
            throw new IllegalArgumentException("The document endpoint's auth token contains an "
                    + "unpaired surrogate at index " + badToken + ". It cannot be encoded as "
                    + "UTF-8, and the readers store it as UTF-8, so the credential they send "
                    + "would not be the one given here.");
        }
        DocumentProviderBridge b = bridgeInternal();
        if (b == null) {
            return;
        }
        try {
            b.setRemoteEndpoint(endpoint, authToken);
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    /// Sets the HTTPS endpoint with no bearer token.
    ///
    /// #### Parameters
    ///
    /// - `endpoint`: the HTTPS base URL, or null to serve only from the shared directory
    public static void setRemoteEndpoint(String endpoint) {
        setRemoteEndpoint(endpoint, null);
    }

    /// Asks the platform browser to re-enumerate the published tree. `publish` already does this;
    /// call it directly only when the bytes behind an unchanged tree changed.
    public static void signalChange() {
        DocumentProviderBridge b = bridgeInternal();
        if (b == null) {
            return;
        }
        try {
            b.signalChange();
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    /// Withdraws everything: the published tree disappears from the file browser, the endpoint and
    /// token are forgotten and the shared directory is emptied. Call this on logout -- the shared
    /// container outlives your process, so documents left there stay browsable by anyone holding
    /// the device.
    public static void clear() {
        DocumentProviderBridge b = bridgeInternal();
        if (b == null) {
            return;
        }
        try {
            b.clear();
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    private static boolean isHttps(String endpoint) {
        // Deliberately not a URI parse: this runs on every port including ones with a reduced
        // java.net, and the only question is the scheme.
        if (endpoint.length() < 8) {
            return false;
        }
        String scheme = endpoint.substring(0, 8);
        return "https://".equals(scheme.toLowerCase());
    }

    /// Test seam: installs a bridge, bypassing platform resolution.
    ///
    /// #### Parameters
    ///
    /// - `b`: the bridge, or null to resolve from the platform again
    public static void setBridge(DocumentProviderBridge b) {
        bridge = b;
        bridgeOverridden = b != null;
    }

    static DocumentProviderBridge bridgeInternal() {
        if (bridgeOverridden) {
            return bridge;
        }
        if (!Display.isInitialized()) {
            return null;
        }
        try {
            return Display.getInstance().getDocumentProviderBridge();
        } catch (Throwable t) {
            Log.e(t);
            return null;
        }
    }

    /// Test seam: clears the bridge override.
    static void reset() {
        bridge = null;
        bridgeOverridden = false;
    }
}
