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
package com.codenameone.developerguide.backend;

import com.codename1.backend.Base64;
import com.codename1.backend.ClientCertificate;
import com.codename1.backend.Web;
import com.codename1.backend.WebClient;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Value;
import java.io.IOException;

/**
 * Calls a partner API that authenticates its clients with a certificate.
 * Referenced from the backend chapter's section on calling other services.
 */
@Component
public class PartnerApi {
    // tag::backend-web-client[]
    private final WebClient partner;

    /** The PKCS#12 archive and its password, from configuration. */
    public PartnerApi(@Value("${partner.p12}") String p12Base64,
                      @Value("${partner.p12Password}") String password) {
        byte[] archive = Base64.decode(p12Base64);
        this.partner = new WebClient()
                .setClientCertificate(ClientCertificate.pkcs12(archive, password))
                .setRequireHttp2(true)
                .setRequestTimeoutMillis(10000);
    }

    public String balance(String account) throws IOException {
        Web.Result result = partner.getJson(
                "https://api.partner.example/v1/accounts/" + account, null);
        if (!result.isSuccess()) {
            throw new IOException("The partner API answered " + result.getStatus());
        }
        return result.getBodyAsString();
    }
    // end::backend-web-client[]
}
