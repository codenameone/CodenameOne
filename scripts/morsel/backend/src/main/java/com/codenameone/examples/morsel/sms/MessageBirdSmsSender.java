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
package com.codenameone.examples.morsel.sms;

import com.codename1.backend.Web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Sends through MessageBird's (now Bird's) Messages API: a form POST to
/// `https://rest.messagebird.com/messages` carrying `recipients`, `originator`
/// and `body`, authenticated with an `Authorization: AccessKey ...` header.
/// MessageBird answers 201 when it has accepted the message.
///
/// Settings: `morsel.sms.messagebird.key` (a live access key) and
/// `morsel.sms.messagebird.originator` (a number, or up to eleven letters and
/// digits).
public class MessageBirdSmsSender implements SmsSender {
    private static final String URL = "https://rest.messagebird.com/messages";

    private final String accessKey;
    private final String originator;

    public MessageBirdSmsSender(String accessKey, String originator) {
        this.accessKey = accessKey;
        this.originator = originator;
    }

    @Override
    public void send(String phone, String text) throws IOException {
        List headers = new ArrayList();
        headers.add("Authorization: AccessKey " + accessKey);
        headers.add("Content-Type: application/x-www-form-urlencoded");
        Web.Result sent = Web.request("POST", URL, headers, SmsForm.of("recipients", phone,
                "originator", originator, "body", text));
        if (sent.getStatus() != 201) {
            // The status only: the body can echo the number back.
            throw new IOException("MessageBird answered " + sent.getStatus());
        }
    }

    @Override
    public boolean delivers() {
        return true;
    }
}
