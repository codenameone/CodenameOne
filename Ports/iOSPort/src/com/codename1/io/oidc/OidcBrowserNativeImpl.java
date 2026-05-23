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

import com.codename1.impl.ios.IOSImplementation;

/**
 * iOS port implementation of {@link OidcBrowserNative}. Thin Java wrapper
 * that delegates to the native methods exposed on
 * {@link com.codename1.impl.ios.IOSNative} -- the C bodies live in
 * {@code Ports/iOSPort/nativeSources/CN1OidcBrowser.m} and use
 * {@code ASWebAuthenticationSession} (iOS 12+).
 *
 * <p>Loaded by {@link com.codename1.io.oidc.SystemBrowser} via
 * {@code Class.forName("com.codename1.io.oidc.OidcBrowserNativeImpl")}.
 */
public class OidcBrowserNativeImpl implements OidcBrowserNative {

    public boolean isSupported() {
        return IOSImplementation.nativeInstance.oidcSystemBrowserSupported();
    }

    public String startAuthorization(String authUrl, String redirectScheme) {
        return IOSImplementation.nativeInstance.oidcStartAuthorization(authUrl, redirectScheme);
    }
}
