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
package com.codename1.backend.security.oauth2.client.credentials;

import com.codename1.backend.Json;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.KeyFiles;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/// A Google service account, read from the JSON key file the Cloud console
/// downloads, as a [JwtBearerGrant]. No Google library is involved: the key is
/// the file's PKCS#8 PEM, the assertion is signed by this runtime's own RSA, and
/// the exchange is one POST.
///
/// ```java
/// AccessTokenProvider storage = new AccessTokenProvider(
///         GoogleServiceAccountCredentials.fromFile("/etc/app/service-account.json")
///                 .grant("https://www.googleapis.com/auth/devstorage.read_only"));
/// Web.Result listing = Web.getJson(
///         "https://storage.googleapis.com/storage/v1/b/my-bucket/o",
///         storage.getToken().getTokenValue());
/// ```
///
/// The key file is a credential: keep it out of the repository and readable by
/// the server's user alone. [#toString()] names the account, never the key.
public final class GoogleServiceAccountCredentials {
    /// Where Google exchanges an assertion when the file does not say.
    public static final String TOKEN_URI = "https://oauth2.googleapis.com/token";

    private final String clientEmail;
    private final Jwk key;
    private final String tokenUri;
    private final String projectId;

    private GoogleServiceAccountCredentials(String clientEmail, Jwk key, String tokenUri,
                                            String projectId) {
        this.clientEmail = clientEmail;
        this.key = key;
        this.tokenUri = tokenUri;
        this.projectId = projectId;
    }

    /// Reads a service-account key file.
    ///
    /// @throws IOException when it cannot be read, or is not a service-account key
    public static GoogleServiceAccountCredentials fromFile(String path) throws IOException {
        return fromJson(KeyFiles.read(path));
    }

    /// Reads the contents of a service-account key file.
    ///
    /// @throws IOException when it is not a service-account key; the message
    /// names the field that is wrong and never repeats the key
    public static GoogleServiceAccountCredentials fromJson(String json) throws IOException {
        Map document;
        try {
            document = Json.parseObject(json);
        } catch (RuntimeException malformed) {
            throw new IOException("The service-account key is not JSON", malformed);
        }
        if (!"service_account".equals(document.get("type"))) {
            throw new IOException("The JSON is not a service-account key: its type is "
                    + (document.get("type") instanceof String ? document.get("type")
                    : "missing"));
        }
        String email = text(document, "client_email");
        String pem = text(document, "private_key");
        if (email == null || pem == null) {
            throw new IOException("The service-account key has no client_email or no "
                    + "private_key");
        }
        Jwk key;
        try {
            key = Jwk.ofPrivateKey(KeyFiles.privateKey(pem));
        } catch (IOException unreadable) {
            // KeyFiles' message describes the PEM's shape, not its contents.
            throw new IOException("The service-account private_key could not be read: "
                    + unreadable.getMessage(), unreadable);
        }
        String keyId = text(document, "private_key_id");
        if (keyId != null) {
            key = key.withKeyId(keyId);
        }
        String tokenUri = text(document, "token_uri");
        return new GoogleServiceAccountCredentials(email, key,
                tokenUri == null ? TOKEN_URI : tokenUri, text(document, "project_id"));
    }

    private static String text(Map document, String name) {
        Object value = document.get(name);
        return value instanceof String && ((String) value).length() > 0 ? (String) value : null;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    /// A grant for these scopes, acting as the service account itself.
    public JwtBearerGrant grant(String... scopes) {
        return builder().scopes(Arrays.asList(scopes)).build();
    }

    /// A grant for these scopes, acting as the service account itself.
    public JwtBearerGrant grant(Collection<String> scopes) {
        return builder().scopes(scopes).build();
    }

    /// A builder preset for this account, for a subject (domain-wide delegation),
    /// a transport or a clock.
    public JwtBearerGrant.Builder builder() {
        // Google reads the scopes from the assertion, not from the form.
        return JwtBearerGrant.builder(tokenUri, clientEmail, key).audience(tokenUri)
                .scopesInAssertion(true);
    }

    @Override
    public String toString() {
        return "GoogleServiceAccountCredentials[" + clientEmail + "]";
    }
}
