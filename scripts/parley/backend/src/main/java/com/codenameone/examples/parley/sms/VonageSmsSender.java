/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.parley.sms;

import com.codename1.backend.Json;
import com.codename1.backend.Web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// tag::parley-sms-provider[]
/// Sends through Vonage's (formerly Nexmo's) SMS API: a form POST to
/// `https://rest.nexmo.com/sms/json` carrying `api_key`, `api_secret`, `to`,
/// `from` and `text`.
///
/// Vonage answers 200 even for a message it refused; the outcome is the
/// `status` of each entry of `messages`, where `"0"` is success and anything
/// else is an error code. A long text is split into parts and each part has an
/// entry, so every one of them has to be `"0"`.
///
/// Settings: `parley.sms.vonage.key`, `parley.sms.vonage.secret` and
/// `parley.sms.vonage.from` (a virtual number or an alphanumeric sender id).
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
// end::parley-sms-provider[]
