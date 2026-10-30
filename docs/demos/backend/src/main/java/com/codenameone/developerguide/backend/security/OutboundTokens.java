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
package com.codenameone.developerguide.backend.security;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.security.oauth2.client.credentials.AccessTokenProvider;
import com.codename1.backend.security.oauth2.client.credentials.AccessTokenProviders;
import com.codename1.backend.security.oauth2.client.credentials.ClientCredentialsGrant;
import com.codename1.backend.security.oauth2.client.credentials.GoogleServiceAccountCredentials;
import java.io.IOException;

/** The Backend security chapter's examples of calling other services. */
@Configuration
public class OutboundTokens {
// tag::backend-security-outbound-google[]
    @Bean
    AccessTokenProvider firestore() throws IOException {
        GoogleServiceAccountCredentials account =
                GoogleServiceAccountCredentials.fromFile("/run/secrets/service-account.json");
        return new AccessTokenProvider(
                account.grant("https://www.googleapis.com/auth/datastore"));
    }
// end::backend-security-outbound-google[]

// tag::backend-security-outbound-client[]
    @Bean
    AccessTokenProvider billing(Config config) throws IOException {
        return new AccessTokenProvider(ClientCredentialsGrant
                .builder("https://auth.example.com/oauth/token", "orders-service")
                .clientSecret(config.get("billing.client-secret"))
                .scopes("invoices:write")
                .build());
    }
// end::backend-security-outbound-client[]

// tag::backend-security-outbound-config[]
    @Bean
    AccessTokenProvider payments(Config config) throws IOException {
        // Reads cn1.security.oauth2.client.credentials.payments.*
        return AccessTokenProviders.fromConfig(config, "payments");
    }
// end::backend-security-outbound-config[]

// tag::backend-security-outbound-use[]
    static String authorization(AccessTokenProvider tokens) {
        // Cached until a minute before it expires; one refresh for every caller.
        return tokens.getToken().toAuthorizationHeader();
    }
// end::backend-security-outbound-use[]
}
