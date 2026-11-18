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
import com.codename1.backend.Web;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/// The default service account of the Google Cloud machine this server runs
/// on -- Compute Engine, Cloud Run, GKE, App Engine -- as an [AccessTokenGrant],
/// read from the metadata server. The fallback every Google client uses when no
/// key file is configured.
public final class GoogleMetadataServerGrant implements AccessTokenGrant {
    private static final String ACCOUNT = "http://metadata.google.internal/computeMetadata"
            + "/v1/instance/service-accounts/default/";

    @Override
    public AccessToken fetch() {
        try {
            Web.Result r = get("token");
            if (!r.isSuccess()) {
                throw new TokenAcquisitionException("The metadata server refused a token ("
                        + r.getStatus() + ")", r.getStatus(), null, true, null);
            }
            Map reply = Json.parseObject(r.getBodyAsString());
            Object token = reply.get("access_token");
            Object expires = reply.get("expires_in");
            if (!(token instanceof String)) {
                throw new TokenAcquisitionException("The metadata server sent no token",
                        r.getStatus(), null, true, null);
            }
            long seconds = expires instanceof Number ? ((Number) expires).longValue() : 300;
            return new AccessToken((String) token, "Bearer",
                    System.currentTimeMillis() + seconds * 1000,
                    Collections.<String>emptySet());
        } catch (IOException err) {
            throw new TokenAcquisitionException("No Google credentials: no key file is "
                    + "configured and the metadata server did not answer: "
                    + err.getMessage(), 0, null, true, err);
        }
    }

    /// The e-mail address of the machine's service account, which is what a
    /// signature made through the IAM `signBlob` call is attributed to.
    ///
    /// @throws IOException when the metadata server does not answer
    public static String serviceAccountEmail() throws IOException {
        Web.Result r = get("email");
        if (!r.isSuccess()) {
            throw new IOException("The metadata server did not name the service account ("
                    + r.getStatus() + ")");
        }
        return r.getBodyAsString().trim();
    }

    private static Web.Result get(String leaf) throws IOException {
        List<String> headers = new ArrayList<String>(1);
        headers.add("Metadata-Flavor: Google");
        return Web.request("GET", ACCOUNT + leaf, headers, null);
    }
}
