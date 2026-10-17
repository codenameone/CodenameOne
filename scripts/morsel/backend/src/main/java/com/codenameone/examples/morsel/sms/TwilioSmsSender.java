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

import com.codename1.backend.Base64;
import com.codename1.backend.Web;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/// Sends through Twilio's Messages API: a form POST to
/// `/2010-04-01/Accounts/{sid}/Messages.json`, authenticated with the account
/// SID and auth token as HTTP Basic credentials. Twilio answers 201 when it
/// has queued the message.
///
/// Settings: `morsel.sms.twilio.sid`, `morsel.sms.twilio.token` and
/// `morsel.sms.twilio.from` (a Twilio number in E.164 form, or a messaging
/// service's sender).
///
/// The account's credentials are settings of the server and never leave it: the
/// app asks the server for a code and has no SMS credentials of its own to leak.
public class TwilioSmsSender implements SmsSender {
    private final String accountSid;
    private final String authToken;
    private final String from;

    public TwilioSmsSender(String accountSid, String authToken, String from) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.from = from;
    }

    @Override
    public void send(String phone, String text) throws IOException {
        List headers = new ArrayList();
        headers.add("Authorization: Basic "
                + Base64.encode((accountSid + ":" + authToken).getBytes("UTF-8")));
        headers.add("Content-Type: application/x-www-form-urlencoded");
        Web.Result sent = Web.request("POST", "https://api.twilio.com/2010-04-01/Accounts/"
                + URLEncoder.encode(accountSid, "UTF-8") + "/Messages.json", headers,
                SmsForm.of("To", phone, "From", from, "Body", text));
        if (!sent.isSuccess()) {
            // The status only: the body can echo the number back.
            throw new IOException("Twilio answered " + sent.getStatus());
        }
    }

    @Override
    public boolean delivers() {
        return true;
    }
}
