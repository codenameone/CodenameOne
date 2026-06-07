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
package com.codename1.io.webauthn;

import com.codename1.util.AsyncResource;

/// Modern WebAuthn / passkey client. Wraps the OS public-key credential APIs
/// (`ASAuthorizationPlatformPublicKeyCredentialProvider` on iOS 16+,
/// `androidx.credentials.CredentialManager` on Android API 28+) behind a
/// portable, JSON-friendly Java surface so you can talk to any relying-party
/// server -- your own backend, Auth0, Firebase, or one of the WebAuthn server
/// libraries -- with the same code.
///
/// ### When to reach for this class
///
/// - Your app talks to *your own* backend and you want to add passkeys for
///   passwordless sign-in / step-up auth.
/// - You are wiring up a passkey flow against Auth0 or Firebase that those
///   providers' OIDC ceremonies don't already give you for free. (When the
///   user signs into Google / Apple / Microsoft via [com.codename1.io.oidc.OidcClient],
///   the IdP handles the passkey on its end -- you get the resulting tokens
///   without ever calling this class.)
///
/// ### Typical registration flow
///
/// ```java
/// // 1. Ask your server for the registration challenge JSON.
/// AsyncResource<String> challenge = httpPost("/passkey/register/start", body);
///
/// // 2. Hand it to the OS for the actual passkey creation.
/// PublicKeyCredentialCreationOptions opts =
///         PublicKeyCredentialCreationOptions.fromJson(challenge.get());
///
/// WebAuthnClient.getInstance().create(opts)
///         .ready(new SuccessCallback<PublicKeyCredential>() {
///             public void onSucess(PublicKeyCredential cred) {
///                 // 3. Forward the authenticator response back to the server.
///                 httpPost("/passkey/register/verify", cred.toJson());
///             }
///         });
/// ```
///
/// ### Typical sign-in flow
///
/// Symmetrical: ask the server for an assertion challenge, hand to
/// [#get(PublicKeyCredentialRequestOptions)], POST the response back. The
/// server verifies the signature and returns a session token.
///
/// ### What this class deliberately does NOT do
///
/// - **Verify the attestation / assertion.** That is the relying party's
///   responsibility -- it requires the server-side credential record and a
///   counter check that only the RP can do safely. Use a server library:
///   `webauthn4j` (Java), `@simplewebauthn/server` (Node), `webauthn-rs`
///   (Rust), or your IdP's built-in verifier.
/// - **Conditional UI (autofill).** The W3C `mediation: "conditional"` UX
///   is not currently exposed; pass a regular [#get] when the user clicks
///   a sign-in button.
/// - **Replace OIDC.** Most apps using [com.codename1.io.oidc.OidcClient]
///   already get passkey-backed sign-in for free (the IdP handles the
///   passkey ceremony). Use this class when you specifically have your own
///   relying party.
///
/// @since 7.0.245
public final class WebAuthnClient {

    private static WebAuthnClient INSTANCE = new WebAuthnClient();
    private static WebAuthnNative provider;

    private WebAuthnClient() {}

    public static WebAuthnClient getInstance() {
        return INSTANCE;
    }

    /// `true` when a native, OS-level passkey implementation is available on
    /// the current platform. When `false`, [#create] and [#get] fail with
    /// [WebAuthnException#NOT_IMPLEMENTED] so the caller can present a
    /// fallback UI.
    public static boolean isSupported() {
        WebAuthnNative n = getProvider();
        return n != null && n.isSupported();
    }

    /// Registers a port-supplied [WebAuthnNative] implementation. Called at
    /// app startup by the platform port (`WebAuthnNativeImpl.init()`).
    /// Cn1lib authors can also call this to plug in a custom implementation
    /// (e.g. a USB-HID security-key driver). Pass `null` to revert to "no
    /// platform support".
    public static void setProvider(WebAuthnNative p) {
        synchronized (WebAuthnClient.class) {
            provider = p;
        }
    }

    private static WebAuthnNative getProvider() {
        synchronized (WebAuthnClient.class) {
            return provider;
        }
    }

    /// Drives the W3C `navigator.credentials.create()` ceremony with the
    /// given options. The returned [AsyncResource] completes with the
    /// authenticator's [PublicKeyCredential] response, or errors with
    /// [WebAuthnException] (e.g. [WebAuthnException#NOT_ALLOWED] when the
    /// user dismisses the OS sheet).
    ///
    /// **The work is done off the EDT** -- a background thread blocks on the
    /// native call. Callers can attach `.ready()` and `.except()` listeners
    /// without worrying about thread affinity; both fire on the EDT.
    public AsyncResource<PublicKeyCredential> create(final PublicKeyCredentialCreationOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("options must not be null");
        }
        final AsyncResource<PublicKeyCredential> out = new AsyncResource<PublicKeyCredential>();
        final WebAuthnNative p = getProvider();
        if (p == null || !p.isSupported()) {
            out.error(new WebAuthnException(WebAuthnException.NOT_IMPLEMENTED,
                    "WebAuthn is not available on this platform"));
            return out;
        }
        Runnable task = new CreateRunnable(p, options.toJson(), out);
        new Thread(task, "WebAuthnCreate").start();
        return out;
    }

    /// Drives the W3C `navigator.credentials.get()` ceremony with the given
    /// options. Symmetrical to [#create(PublicKeyCredentialCreationOptions)].
    public AsyncResource<PublicKeyCredential> get(final PublicKeyCredentialRequestOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("options must not be null");
        }
        final AsyncResource<PublicKeyCredential> out = new AsyncResource<PublicKeyCredential>();
        final WebAuthnNative p = getProvider();
        if (p == null || !p.isSupported()) {
            out.error(new WebAuthnException(WebAuthnException.NOT_IMPLEMENTED,
                    "WebAuthn is not available on this platform"));
            return out;
        }
        Runnable task = new GetRunnable(p, options.toJson(), out);
        new Thread(task, "WebAuthnGet").start();
        return out;
    }

    /// Static-nested runnable wrappers (over anonymous inner classes) so
    /// SpotBugs SIC_INNER_SHOULD_BE_STATIC_ANON stays quiet and so the
    /// thread doesn't pin a [WebAuthnClient] reference.
    private static final class CreateRunnable implements Runnable {
        private final WebAuthnNative provider;
        private final String optionsJson;
        private final AsyncResource<PublicKeyCredential> out;

        CreateRunnable(WebAuthnNative provider, String optionsJson,
                       AsyncResource<PublicKeyCredential> out) {
            this.provider = provider;
            this.optionsJson = optionsJson;
            this.out = out;
        }

        @Override
        public void run() {
            try {
                String responseJson = provider.createPasskey(optionsJson);
                if (responseJson == null) {
                    out.error(new WebAuthnException(WebAuthnException.NOT_ALLOWED,
                            "Passkey registration sheet was dismissed"));
                    return;
                }
                out.complete(PublicKeyCredential.fromJson(responseJson));
            } catch (WebAuthnException wae) {
                out.error(wae);
            } catch (Throwable t) {
                out.error(new WebAuthnException(WebAuthnException.TRANSPORT_ERROR,
                        "Native passkey create failed: " + t.getMessage(), t));
            }
        }
    }

    private static final class GetRunnable implements Runnable {
        private final WebAuthnNative provider;
        private final String optionsJson;
        private final AsyncResource<PublicKeyCredential> out;

        GetRunnable(WebAuthnNative provider, String optionsJson,
                    AsyncResource<PublicKeyCredential> out) {
            this.provider = provider;
            this.optionsJson = optionsJson;
            this.out = out;
        }

        @Override
        public void run() {
            try {
                String responseJson = provider.getPasskey(optionsJson);
                if (responseJson == null) {
                    out.error(new WebAuthnException(WebAuthnException.NOT_ALLOWED,
                            "Passkey sign-in sheet was dismissed"));
                    return;
                }
                out.complete(PublicKeyCredential.fromJson(responseJson));
            } catch (WebAuthnException wae) {
                out.error(wae);
            } catch (Throwable t) {
                out.error(new WebAuthnException(WebAuthnException.TRANSPORT_ERROR,
                        "Native passkey get failed: " + t.getMessage(), t));
            }
        }
    }
}
