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
/// Settings: `wayline.sms.twilio.sid`, `wayline.sms.twilio.token` and
/// `wayline.sms.twilio.from` (a Twilio number in E.164 form, or a messaging
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
