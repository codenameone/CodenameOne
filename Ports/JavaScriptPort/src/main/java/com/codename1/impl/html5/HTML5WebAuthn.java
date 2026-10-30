/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.html5;

import com.codename1.io.webauthn.WebAuthnClient;
import com.codename1.io.webauthn.WebAuthnException;
import com.codename1.io.webauthn.WebAuthnNative;

import java.io.UnsupportedEncodingException;

/// Passkeys in a browser: [WebAuthnClient]'s ceremonies run through the page's own
/// `navigator.credentials`, so an application signs in with a passkey in its web build the way it
/// does on a phone, against the same server.
///
/// The translated application runs in a worker, which has no `navigator.credentials`; each
/// ceremony is forwarded to the `__cn1_webauthn__` host bridge in `browser_bridge.js`, which
/// runs it on the page and answers with the credential as WebAuthn JSON. The relying party is
/// the page's own origin, so a server accepts these only when it lists that origin.
///
/// Installed by [HTML5Implementation#init(Object)].
final class HTML5WebAuthn implements WebAuthnNative {
    /// The answer's first byte: the credential follows as UTF-8 JSON.
    private static final int OK = 0;
    /// The user dismissed the browser's sheet, or the browser refused without saying more.
    private static final int CANCELLED = 1;
    /// A failure; `name:message` follows, the name a DOMException name.
    private static final int FAILED = 2;

    static void install() {
        WebAuthnClient.setProvider(new HTML5WebAuthn());
    }

    @Override
    public boolean isSupported() {
        return nativeSupported();
    }

    @Override
    public String createPasskey(String creationOptionsJson) throws WebAuthnException {
        return answer(nativeCeremony("create", creationOptionsJson));
    }

    @Override
    public String getPasskey(String requestOptionsJson) throws WebAuthnException {
        return answer(nativeCeremony("get", requestOptionsJson));
    }

    /// The credential JSON, null for a dismissed sheet (the caller's NotAllowedError), or the
    /// failure as a typed exception.
    private static String answer(byte[] reply) throws WebAuthnException {
        if (reply == null || reply.length == 0) {
            throw new WebAuthnException(WebAuthnException.TRANSPORT_ERROR,
                    "The browser did not answer the passkey request");
        }
        String text;
        try {
            text = new String(reply, 1, reply.length - 1, "UTF-8");
        } catch (UnsupportedEncodingException impossible) {
            throw new WebAuthnException(WebAuthnException.TRANSPORT_ERROR, "UTF-8 is missing");
        }
        if (reply[0] == OK) {
            return text;
        }
        if (reply[0] == CANCELLED) {
            return null;
        }
        int colon = text.indexOf(':');
        String code = colon > 0 ? text.substring(0, colon) : WebAuthnException.TRANSPORT_ERROR;
        String message = colon > 0 ? text.substring(colon + 1) : text;
        throw new WebAuthnException(code, message.length() == 0 ? "Passkey request failed" : message);
    }

    // Bound in Ports/JavaScriptPort/src/main/webapp/port.js, forwarding to the __cn1_webauthn__
    // host bridge in vm/ByteCodeTranslator/src/javascript/browser_bridge.js.

    /// Whether the page has `navigator.credentials` and `PublicKeyCredential`.
    static native boolean nativeSupported();

    /// Runs `navigator.credentials.<op>` (`create` or `get`) with the options JSON. The first
    /// byte of the answer is [#OK], [#CANCELLED] or [#FAILED].
    static native byte[] nativeCeremony(String op, String optionsJson);
}
