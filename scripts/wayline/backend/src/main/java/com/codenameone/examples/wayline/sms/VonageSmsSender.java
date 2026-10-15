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
package com.codenameone.examples.wayline.sms;

import com.codename1.backend.Json;
import com.codename1.backend.Web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// tag::wayline-sms-provider[]
/// Sends through Vonage's (formerly Nexmo's) SMS API: a form POST to
/// `https://rest.nexmo.com/sms/json` carrying `api_key`, `api_secret`, `to`,
/// `from` and `text`.
///
/// Vonage answers 200 even for a message it refused; the outcome is the
/// `status` of each entry of `messages`, where `"0"` is success and anything
/// else is an error code. A long text is split into parts and each part has an
/// entry, so every one of them has to be `"0"`.
///
/// Settings: `wayline.sms.vonage.key`, `wayline.sms.vonage.secret` and
/// `wayline.sms.vonage.from` (a virtual number or an alphanumeric sender id).
/// The number is sent without its leading `+`, as Vonage asks.
public class VonageSmsSender implements SmsSender {
    private static final String URL = "https://rest.nexmo.com/sms/json";

    private final String apiKey;
    private final String apiSecret;
    private final String from;

    public VonageSmsSender(String apiKey, String apiSecret, String from) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.from = from;
    }

    @Override
    public void send(String phone, String text) throws IOException {
        String to = phone.startsWith("+") ? phone.substring(1) : phone;
        List headers = new ArrayList();
        headers.add("Content-Type: application/x-www-form-urlencoded");
        Web.Result sent = Web.request("POST", URL, headers, SmsForm.of("api_key", apiKey,
                "api_secret", apiSecret, "to", to, "from", from, "text", text));
        if (sent.getStatus() != 200) {
            throw new IOException("Vonage answered " + sent.getStatus());
        }
        Object messages = Json.parseObject(sent.getBodyAsString()).get("messages");
        if (!(messages instanceof List) || ((List) messages).isEmpty()) {
            throw new IOException("Vonage's answer lists no message");
        }
        for (Object entry : (List) messages) {
            Object status = entry instanceof Map ? ((Map) entry).get("status") : null;
            if (!"0".equals(status)) {
                // The code only; `error-text` can echo the number back.
                throw new IOException("Vonage refused the message with status " + status);
            }
        }
    }

    @Override
    public boolean delivers() {
        return true;
    }
}
// end::wayline-sms-provider[]
