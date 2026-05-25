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

import com.codename1.impl.ios.IOSImplementation;

/**
 * iOS port implementation of {@link WebAuthnNative}. Thin Java wrapper that
 * delegates to native methods on {@link com.codename1.impl.ios.IOSNative};
 * the C bodies live in {@code Ports/iOSPort/nativeSources/CN1WebAuthn.m} and
 * use {@code ASAuthorizationPlatformPublicKeyCredentialProvider} (iOS 16+).
 *
 * <p>{@link #init()} is invoked from the generated iOS app stub by
 * {@code IPhoneBuilder} when the classpath scanner sees any reference to
 * {@code com.codename1.io.webauthn.*}.
 *
 * <p>The native side encodes the result as a W3C RegistrationResponseJSON /
 * AuthenticationResponseJSON document with all binary fields base64url-encoded,
 * matching the format every WebAuthn server library expects.
 */
public class WebAuthnNativeImpl implements WebAuthnNative {

    /** Invoked from the generated app stub at startup. */
    public static void init() {
        WebAuthnClient.setProvider(new WebAuthnNativeImpl());
    }

    public boolean isSupported() {
        return IOSImplementation.nativeInstance.webauthnSupported();
    }

    public String createPasskey(String optionsJson) throws WebAuthnException {
        String r = IOSImplementation.nativeInstance.webauthnCreate(optionsJson);
        return unwrap(r);
    }

    public String getPasskey(String optionsJson) throws WebAuthnException {
        String r = IOSImplementation.nativeInstance.webauthnGet(optionsJson);
        return unwrap(r);
    }

    /**
     * The native side returns one of:
     * <ul>
     *   <li>A response JSON beginning with {@code '{'} -- success.</li>
     *   <li>{@code null} -- user cancelled (the Java layer maps this to
     *       {@link WebAuthnException#NOT_ALLOWED}).</li>
     *   <li>An error string of the form {@code "ERR:<code>:<message>"} --
     *       the OS authenticator failed; we unwrap it to a typed exception.</li>
     * </ul>
     */
    private static String unwrap(String raw) throws WebAuthnException {
        if (raw == null) {
            return null;
        }
        if (raw.startsWith("ERR:")) {
            int sep = raw.indexOf(':', 4);
            String code = sep > 4 ? raw.substring(4, sep) : raw.substring(4);
            String message = sep > 4 ? raw.substring(sep + 1) : "Native authenticator failed";
            throw new WebAuthnException(code, message);
        }
        return raw;
    }
}
