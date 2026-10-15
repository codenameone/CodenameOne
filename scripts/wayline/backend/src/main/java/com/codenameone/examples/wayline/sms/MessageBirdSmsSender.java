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

import com.codename1.backend.Web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Sends through MessageBird's (now Bird's) Messages API: a form POST to
/// `https://rest.messagebird.com/messages` carrying `recipients`, `originator`
/// and `body`, authenticated with an `Authorization: AccessKey ...` header.
/// MessageBird answers 201 when it has accepted the message.
///
/// Settings: `wayline.sms.messagebird.key` (a live access key) and
/// `wayline.sms.messagebird.originator` (a number, or up to eleven letters and
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
