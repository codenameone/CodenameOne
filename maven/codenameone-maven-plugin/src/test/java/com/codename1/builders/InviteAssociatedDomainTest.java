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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the element-wise comparison used before appending the invite
 * associated domain to {@code ios.associatedDomains}.
 *
 * <p>The value is a comma delimited list, and a substring test on it gets the
 * wrong answer in both directions: a developer's entry for a longer host would
 * read as declaring ours, and ours would read as declaring theirs.</p>
 */
class InviteAssociatedDomainTest {

    private static final String WANT = "applinks:cloud.codenameone.com";

    @Test
    void anExactElementIsRecognised() {
        assertTrue(IPhoneBuilder.declaresAssociatedDomain(WANT, WANT));
        assertTrue(IPhoneBuilder.declaresAssociatedDomain(
                "webcredentials:example.com," + WANT, WANT));
        assertTrue(IPhoneBuilder.declaresAssociatedDomain(
                WANT + ",applinks:example.com", WANT));
    }

    @Test
    void whitespaceAroundAnElementDoesNotHideIt() {
        assertTrue(IPhoneBuilder.declaresAssociatedDomain(
                "applinks:example.com,  " + WANT + " ", WANT));
    }

    @Test
    void aLongerHostDoesNotReadAsOurs() {
        // The trap: applinks:staging.cloud.codenameone.com must not suppress
        // the production domain, or a developer with a staging entry ships an
        // app whose invite links open Safari.
        assertFalse(IPhoneBuilder.declaresAssociatedDomain(
                "applinks:staging.cloud.codenameone.com", WANT));
    }

    @Test
    void aShorterHostDoesNotReadAsOursEither() {
        assertFalse(IPhoneBuilder.declaresAssociatedDomain(
                "applinks:codenameone.com", WANT));
    }

    @Test
    void aDifferentPrefixForTheSameHostIsNotTheSameDeclaration() {
        // webcredentials: on our host grants password autofill, not links.
        assertFalse(IPhoneBuilder.declaresAssociatedDomain(
                "webcredentials:cloud.codenameone.com", WANT));
    }

    @Test
    void emptyAndNullAreHandled() {
        assertFalse(IPhoneBuilder.declaresAssociatedDomain("", WANT));
        assertFalse(IPhoneBuilder.declaresAssociatedDomain(null, WANT));
        assertFalse(IPhoneBuilder.declaresAssociatedDomain(WANT, null));
    }
}
