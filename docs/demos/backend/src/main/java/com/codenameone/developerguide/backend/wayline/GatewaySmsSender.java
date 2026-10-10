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
package com.codenameone.developerguide.backend.wayline;

import com.codename1.backend.Web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// tag::wayline-sms-second[]
/// Sends through an SMS gateway that takes a form post and a bearer key.
public class GatewaySmsSender implements SmsSender {
    private final String url;
    private final String key;
    private final String from;

    public GatewaySmsSender(String url, String key, String from) {
        this.url = url;
        this.key = key;
        this.from = from;
    }

    @Override
    public void send(String phone, String text) throws IOException {
        List headers = new ArrayList();
        headers.add("Authorization: Bearer " + key);
        headers.add("Content-Type: application/x-www-form-urlencoded");
        String form = "to=" + UrlText.encode(phone) + "&from=" + UrlText.encode(from)
                + "&text=" + UrlText.encode(text);
        Web.Result sent = Web.request("POST", url, headers, form.getBytes("UTF-8"));
        if (!sent.isSuccess()) {
            // The status only: the body can echo the number back.
            throw new IOException("The SMS provider answered " + sent.getStatus());
        }
    }

    @Override
    public boolean delivers() {
        return true;
    }
}
// end::wayline-sms-second[]
