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
package com.codename1.backend.aws;

import java.io.IOException;

/**
 * Test-only door onto the credential endpoint policy.
 *
 * <p>fromContainer() reads AWS_CONTAINER_CREDENTIALS_FULL_URI from the
 * environment, which a running process cannot set for itself, so driving the
 * rule through the public method would mean testing one value per process run.
 * The rule itself is package private, and this lives in its package -- the same
 * arrangement FileCountProbe has -- so the check can put every case to it
 * without widening the runtime's API for a test.
 */
public final class CredentialEndpointProbe {

    private CredentialEndpointProbe() {
    }

    /** The S3 endpoint this region resolves to; see S3.endpointFor. */
    public static String s3EndpointFor(String region) {
        return S3.endpointFor(region);
    }

    /** "allowed", or "refused" with the reason the runtime gave. */
    public static String verdictFor(String url) {
        try {
            Credentials.requireSafeCredentialEndpoint(url);
            return "allowed";
        } catch (IOException refused) {
            return "refused";
        }
    }
}
