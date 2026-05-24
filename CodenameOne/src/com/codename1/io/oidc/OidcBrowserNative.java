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

/// Service-provider interface that [SystemBrowser] uses to dispatch a sign-in
/// flow through the OS's hardened sign-in surface
/// (`ASWebAuthenticationSession` on iOS, `androidx.browser.customtabs` /
/// `Credential Manager` on Android).
///
/// The platform port supplies an implementation named
/// `com.codename1.io.oidc.OidcBrowserNativeImpl`; [SystemBrowser] loads it via
/// `Class.forName` at first use. Cn1lib authors who want to plug in their own
/// implementation (for example, one backed by a [com.codename1.system.NativeInterface]
/// so a 3rd-party SDK can drive the browser) can declare a subtype and
/// register it with [SystemBrowser#setNative(OidcBrowserNative)] -- there is
/// no need to extend `NativeInterface` from this interface itself.
///
/// `redirectScheme` is the scheme half of the registered redirect URI (e.g.
/// the `"com.example.app"` part of `"com.example.app:/oauth2redirect"`).
///
/// @since 7.0.245
public interface OidcBrowserNative {

    /// `true` if this implementation is usable on the current device / OS
    /// version. The default fallback ([SystemBrowser]'s in-app
    /// [com.codename1.ui.BrowserWindow]) takes over when this returns
    /// `false`, so a port that has a class on the file system but cannot
    /// satisfy the runtime requirements (e.g. iOS 11 lacks
    /// `ASWebAuthenticationSession`) should report `false` and the call
    /// will degrade gracefully.
    boolean isSupported();

    /// Starts the OS sign-in sheet for `authUrl` and resolves when the user
    /// is redirected to a URL matching `redirectScheme`. The return value is
    /// the full redirect URL (including query / fragment), or `null` if the
    /// user cancelled.
    ///
    /// Implementations are expected to be blocking: the caller is on a
    /// worker thread and waits for the result.
    String startAuthorization(String authUrl, String redirectScheme);
}
