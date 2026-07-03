/*
 * Copyright (c) 2012-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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
package com.codename1.io.oidc;

import com.codename1.ui.BrowserWindow;
import com.codename1.ui.CN;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.util.AsyncResource;

/// Routes an authorization-code-flow sign-in through the *system browser*
/// (`ASWebAuthenticationSession` on iOS, an Android Custom Tab on Android,
/// the user's default browser on JavaSE / Web) and resolves with the final
/// redirect URL once the OS hands it back. Replaces the embedded WebView
/// approach used by the legacy [com.codename1.io.Oauth2] class.
///
/// You normally do not call this directly -- [OidcClient.authorize] does it
/// for you. Use the public methods on this class when wiring up a custom
/// OAuth 2.0 flow that does not fit the OIDC client (e.g. device flow).
///
/// ### Why the system browser?
///
/// Modern identity providers (Google Identity Services, Apple, Microsoft
/// Entra ID, Auth0, Firebase Auth) refuse to render their sign-in pages
/// inside an embedded WebView -- it's flagged as a phishing surface and
/// blocked. Using the OS-provided sheet gives the user a trusted UI,
/// preserves cookies for single sign-on, and integrates with password and
/// passkey autofill.
///
public final class SystemBrowser {

    /// Port-supplied implementation, registered at app startup. Reads /
    /// writes are guarded by a synchronized block; no volatile required
    /// because every observer goes through [#getProvider()] which performs
    /// the synchronization itself.
    private static OidcBrowserNative provider;

    private SystemBrowser() {}

    /// `true` when a native, OS-level implementation is available on the
    /// current platform. When `false` the [#authenticate(String, String)]
    /// call falls back to an in-app [BrowserWindow]. Call this if you want
    /// to surface a clear UX warning to the user.
    public static boolean isNativeAvailable() {
        OidcBrowserNative n = getProvider();
        return n != null && n.isSupported();
    }

    /// Registers the native [OidcBrowserNative] implementation. Called at
    /// app startup by the port (`OidcBrowserNativeImpl.init()`); cn1lib
    /// authors can also call this to plug in their own implementation -- for
    /// example to wrap a 3rd-party SDK that drives the OS sheet differently.
    /// Pass `null` to revert to the [BrowserWindow] fallback.
    ///
    /// Class.forName-based lookup is intentionally avoided because
    /// Codename One obfuscates class names; the port instead instantiates
    /// the impl itself and passes the instance here.
    public static void setProvider(OidcBrowserNative p) {
        synchronized (SystemBrowser.class) {
            provider = p;
        }
    }

    private static OidcBrowserNative getProvider() {
        synchronized (SystemBrowser.class) {
            return provider;
        }
    }

    /// Launches the system browser at `authorizationUrl` and resolves with
    /// the redirect URL once the user is bounced to a location starting with
    /// `redirectUri`.
    ///
    /// #### Parameters
    ///
    /// - `authorizationUrl`: Fully-built authorization-endpoint URL.
    ///
    /// - `redirectUri`: Redirect URI registered with the authorization
    ///   server. Both custom-scheme URIs (`com.example:/oauth2redirect`)
    ///   and HTTPS URIs are accepted; the latter are recommended on
    ///   Android 11+ where custom schemes can be hijacked.
    ///
    /// #### Returns
    ///
    /// An [AsyncResource] that completes with the redirect URL (including
    /// query / fragment) or errors with [OidcException] on cancellation /
    /// failure.
    public static AsyncResource<String> authenticate(String authorizationUrl,
                                                     String redirectUri) {
        if (authorizationUrl == null) {
            throw new IllegalArgumentException("authorizationUrl must not be null");
        }
        if (redirectUri == null) {
            throw new IllegalArgumentException("redirectUri must not be null");
        }
        final AsyncResource<String> out = new AsyncResource<String>();
        OidcBrowserNative p = getProvider();
        if (p != null && p.isSupported()) {
            authenticateNative(p, authorizationUrl, redirectUri, out);
        } else {
            authenticateBrowserWindow(authorizationUrl, redirectUri, out);
        }
        return out;
    }

    private static void authenticateNative(final OidcBrowserNative provider,
                                           final String authUrl,
                                           final String redirectUri,
                                           final AsyncResource<String> out) {
        // Native calls usually need to happen off the EDT so the OS sheet can
        // present and the JVM can pump events. CN.scheduleBackgroundTask runs
        // on a pool thread.
        final String scheme = schemeOf(redirectUri);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    String result = provider.startAuthorization(authUrl, scheme);
                    if (result == null) {
                        out.error(new OidcException(OidcException.USER_CANCELLED,
                                "Sign-in sheet was dismissed before completion"));
                        return;
                    }
                    out.complete(result);
                } catch (Throwable t) {
                    out.error(new OidcException(OidcException.TRANSPORT_ERROR,
                            "Native sign-in sheet failed: " + t.getMessage(), t));
                }
            }
        };
        // Schedule on a background thread so we don't deadlock the EDT.
        new Thread(task, "OidcSystemBrowser").start();
    }

    private static void authenticateBrowserWindow(final String authUrl,
                                                  final String redirectUri,
                                                  final AsyncResource<String> out) {
        Runnable show = new Runnable() {
            @Override
            public void run() {
                final BrowserWindow window = new BrowserWindow(authUrl);
                window.setTitle("Sign in");
                final boolean[] resolved = new boolean[1];
                final ActionListener loadListener = new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        Object src = evt.getSource();
                        if (!(src instanceof String)) {
                            return;
                        }
                        String url = (String) src;
                        // `src` was already proven non-null by the
                        // instanceof above, so url cannot be null here --
                        // SpotBugs RCN_REDUNDANT_NULLCHECK was right.
                        if (!url.startsWith(redirectUri)) {
                            return;
                        }
                        if (resolved[0]) {
                            return;
                        }
                        resolved[0] = true;
                        window.close();
                        out.complete(url);
                    }
                };
                window.addLoadListener(loadListener);
                window.addCloseListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ev) {
                        if (!resolved[0]) {
                            resolved[0] = true;
                            out.error(new OidcException(OidcException.USER_CANCELLED,
                                    "Sign-in window was closed before completion"));
                        }
                    }
                });
                window.show();
            }
        };
        if (CN.isEdt()) {
            show.run();
        } else {
            CN.callSerially(show);
        }
    }

    /// Extracts the scheme of a redirect URI. For `"com.example.app:/oauth2"`
    /// this returns `"com.example.app"`; for `"https://example.com/cb"` it
    /// returns `"https"`. Used by native back-ends that need the scheme half
    /// only.
    static String schemeOf(String redirectUri) {
        int colon = redirectUri.indexOf(':');
        if (colon < 0) {
            return redirectUri;
        }
        return redirectUri.substring(0, colon);
    }
}
