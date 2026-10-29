/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.webauth;

/**
 * flutter_web_auth_2's {@code FlutterWebAuth2Options}. {@code httpsHost} and
 * {@code httpsPath} are used to recognise an https callback; the rest is accepted and not
 * applied (the sign-in page always opens in an in-app browser).
 */
public class FlutterWebAuth2Options {
    private String httpsHost;
    private String httpsPath;
    private Long timeout;

    public FlutterWebAuth2Options() {
    }

    public void httpsHost(String v) {
        httpsHost = v;
    }

    public void httpsPath(String v) {
        httpsPath = v;
    }

    public void timeout(Long v) {
        timeout = v;
    }

    public FlutterWebAuth2Options(Boolean preferEphemeral, String debugOrigin, Long intentFlags, String windowName,
            Long timeout, String landingPageHtml, Boolean silentAuth, Boolean useWebview, String httpsHost,
            String httpsPath) {
        this.httpsHost = httpsHost;
        this.httpsPath = httpsPath;
        this.timeout = timeout;
    }

    /** Accepted, not applied. */
    public void preferEphemeral(Boolean v) {
    }

    /** Accepted, not applied. */
    public void debugOrigin(String v) {
    }

    /** Accepted, not applied. */
    public void intentFlags(Long v) {
    }

    /** Accepted, not applied. */
    public void windowName(String v) {
    }

    /** Accepted, not applied. */
    public void landingPageHtml(String v) {
    }

    /** Accepted, not applied. */
    public void silentAuth(Boolean v) {
    }

    /** Accepted, not applied. */
    public void useWebview(Boolean v) {
    }

    public String httpsHost() {
        return httpsHost;
    }

    public String httpsPath() {
        return httpsPath;
    }

    public Long timeout() {
        return timeout;
    }
}
