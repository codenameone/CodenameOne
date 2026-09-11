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
        return trimmed.isEmpty() ? DEFAULT_DOMAIN : trimmed;
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
