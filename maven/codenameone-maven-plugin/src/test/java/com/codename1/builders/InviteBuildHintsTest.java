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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The two invite hints are copied into artefacts that must agree byte for
 * byte -- the manifest's host and pathPrefix, the associated-domain
 * entitlement, and the runtime properties the client mints urls from.
 *
 * <p>{@code getArg} returns what the developer typed, so a stray space
 * produced a filter and a url that do not match: the link opens a browser
 * instead of the app, on a build that succeeded with the filter plainly
 * present. That shipped once with the slug, where the generated startup code
 * trimmed and the manifest did not.</p>
 */
public class InviteBuildHintsTest {

    private static BuildRequest request(String key, String value) {
        BuildRequest r = new BuildRequest();
        if (value != null) {
            r.putArgument(key, value);
        }
        return r;
    }

    @Test
    void aDomainWithStraySpaceIsNormalised() {
        assertEquals("links.example.com",
                InviteBuildHints.domain(request("invite.domain", "  links.example.com  ")));
    }

    @Test
    void anAbsentOrBlankDomainFallsBackToTheDefault() {
        // Blank is not a host: left as one it produced an intent filter with no
        // host and an entitlement claiming nothing, which is harder to see than
        // the default being used.
        assertEquals(InviteBuildHints.DEFAULT_DOMAIN,
                InviteBuildHints.domain(request("invite.domain", null)));
        assertEquals(InviteBuildHints.DEFAULT_DOMAIN,
                InviteBuildHints.domain(request("invite.domain", "   ")));
    }

    @Test
    void aSlugWithStraySpaceIsNormalised() {
        assertEquals("acme", InviteBuildHints.slug(request("invite.slug", " acme ")));
        assertEquals("", InviteBuildHints.slug(request("invite.slug", null)));
    }

    @Test
    public void aDomainWrittenAsAUrlIsReducedToItsHost() {
        // "https://links.example.com" is the natural thing to write, and the
        // runtime accepts it -- getLinkBase() adds the scheme only when it is
        // missing, so links mint correctly and nothing looks wrong. The
        // builders take the raw string: android:host gets the whole URL and
        // iOS emits applinks:https://links.example.com, so the build succeeds
        // and every invite opens outside the app.
        assertEquals("links.example.com",
                InviteBuildHints.domain(request("invite.domain", "https://links.example.com")));
        assertEquals("links.example.com",
                InviteBuildHints.domain(request("invite.domain", "https://links.example.com/")));
        assertEquals("links.example.com",
                InviteBuildHints.domain(request("invite.domain", "http://links.example.com/base")));
        // A port belongs in the filter's own attribute and has no place in an
        // associated domain.
        assertEquals("links.example.com",
                InviteBuildHints.domain(request("invite.domain", "links.example.com:8443")));
        // And a plain host is untouched.
        assertEquals("links.example.com",
                InviteBuildHints.domain(request("invite.domain", "links.example.com")));
    }
}
