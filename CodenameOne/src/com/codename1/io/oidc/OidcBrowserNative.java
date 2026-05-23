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

import com.codename1.system.NativeInterface;

/// Native bridge into the platform's system-browser sign-in primitive
/// (`ASWebAuthenticationSession` on iOS, `androidx.browser.customtabs` /
/// `Credential Manager` on Android). Ports that implement this interface --
/// or apps that ship a cn1lib doing so -- let [SystemBrowser] dispatch
/// authorization-code flows through the OS's hardened, cookie-isolated
/// sign-in sheet instead of the in-app fallback.
///
/// `redirectScheme` is the scheme half of the registered redirect URI (e.g.
/// the `"com.example.app"` part of `"com.example.app:/oauth2redirect"`). The
/// native side completes by invoking the JavaScript-facing callback hosted by
/// [SystemBrowser]; see [#startAuthorization(String, String)].
///
/// @since 8.0
public interface OidcBrowserNative extends NativeInterface {

    /// Starts the OS sign-in sheet for `authUrl` and resolves when the user
    /// is redirected to a URL matching `redirectScheme`. The return value is
    /// the full redirect URL (including query / fragment).
    ///
    /// Implementations are expected to be asynchronous; they should block the
    /// calling thread and post the resolved URL back via a private
    /// completion path. The fallback [SystemBrowser] implementation already
    /// handles the cross-thread plumbing; native ports just need to deliver
    /// the URL on the EDT.
    ///
    /// #### Parameters
    ///
    /// - `authUrl`: Full authorization-endpoint URL with `client_id`,
    ///   `redirect_uri`, `state`, `code_challenge`, etc. already encoded.
    ///
    /// - `redirectScheme`: The redirect URI scheme registered for the app.
    ///   On iOS the OS uses this to dismiss `ASWebAuthenticationSession`
    ///   automatically; on Android it informs the trusted-browser intent.
    String startAuthorization(String authUrl, String redirectScheme);
}
