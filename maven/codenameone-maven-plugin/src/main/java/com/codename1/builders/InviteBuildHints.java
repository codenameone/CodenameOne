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
package com.codename1.builders;

/**
 * The invite build hints, read once and normalized.
 *
 * <p>{@code BuildRequest.getArg} returns what the developer typed, and these
 * two values are copied into generated artefacts that must agree byte for
 * byte: the Android manifest's {@code android:host} and {@code pathPrefix},
 * the iOS associated-domain entitlement, and the runtime properties the client
 * mints its urls from. A stray space in the hint therefore produced a filter
 * and a url that do not match -- so the link opens a browser instead of the
 * app, on a build that succeeded with the filter plainly present.</p>
 *
 * <p>That happened once with the slug, where the generated startup code
 * trimmed and the manifest did not. This class exists so the next reader
 * cannot reintroduce it by forgetting: there is one accessor per hint and
 * every site goes through it.</p>
 */
class InviteBuildHints {

    /** The default link host, used when the hint is absent or blank. */
    static final String DEFAULT_DOMAIN = "cloud.codenameone.com";

    private InviteBuildHints() {
    }

    /**
     * The host invite links are served from.
     *
     * @param request the build request
     * @return the trimmed hint, or the default when it is absent or blank
     */
    static String domain(BuildRequest request) {
        String value = request.getArg("invite.domain", DEFAULT_DOMAIN);
        if (value == null) {
            return DEFAULT_DOMAIN;
        }
        String trimmed = value.trim();
        // A blank hint is not a host. Left as the empty string it produced an
        // intent filter with no host and an entitlement claiming nothing,
        // which is harder to spot than the default being used.
        if (trimmed.isEmpty()) {
            return DEFAULT_DOMAIN;
        }
        // Reduced to a bare HOST, because that is what every consumer needs
        // and none of them checks.
        //
        // "https://links.example.com" is the natural thing to write here, and
        // the runtime accepts it -- getLinkBase() adds the scheme only when it
        // is missing, so links mint correctly and nothing looks wrong. The
        // builders do not: the Android filter takes the raw string as
        // android:host and iOS emits applinks:https://links.example.com.
        // Neither matches the links being minted, so the build succeeds and
        // every invite opens outside the app, which is this feature's
        // signature failure.
        return hostOf(trimmed);
    }

    /**
     * The host part of a hint that may have been written as a URL.
     *
     * <p>Strips a scheme, any path, query or fragment, and any port -- an
     * intent filter names the port separately and an associated domain has no
     * place for one. A value that reduces to nothing is left alone rather than
     * silently replaced: the builders report an unusable host far better than
     * a default nobody asked for.</p>
     *
     * @param value the trimmed hint
     * @return the host it names
     */
    private static String hostOf(String value) {
        String host = value;
        int scheme = host.indexOf("://");
        if (scheme >= 0) {
            host = host.substring(scheme + 3);
        }
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if (c == '/' || c == '?' || c == '#' || c == ':') {
                host = host.substring(0, i);
                break;
            }
        }
        return host.isEmpty() ? value : host;
    }

    /**
     * The per-application path segment invite links carry.
     *
     * @param request the build request
     * @return the trimmed hint, or the empty string when it is absent
     */
    static String slug(BuildRequest request) {
        String value = request.getArg("invite.slug", "");
        return value == null ? "" : value.trim();
    }
}
