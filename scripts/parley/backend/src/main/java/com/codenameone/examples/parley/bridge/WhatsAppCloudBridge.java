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
package com.codenameone.examples.parley.bridge;

import com.codename1.backend.Json;
import com.codename1.backend.Web;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A WhatsApp Business number, over Meta's Cloud API.
///
/// The Cloud API is for businesses talking to people, not for groups, so a
/// link here is to one WhatsApp user, by phone number: messages the person
/// sends to the business number arrive in the linked Parley conversation and
/// its members' replies go back. WhatsApp only lets a business write freely
/// within 24 hours of the person's last message; after that it needs an
/// approved template, which this sample does not send.
///
/// Meta verifies the webhook once with a GET carrying
/// `parley.whatsapp.verify` and then signs every POST with
/// `X-Hub-Signature-256`, an HMAC of the raw body under the app secret.
/// Configured by `parley.whatsapp.token`, `parley.whatsapp.phone` (the phone
/// number id, not the number), `parley.whatsapp.secret` and
/// `parley.whatsapp.verify`; `parley.whatsapp.api` points it at another
/// server.
@Component
public class WhatsAppCloudBridge implements Bridge {
    private final String api;
    private final String token;
    private final String phoneId;
    private final String appSecret;
    private final String verifyToken;

    public WhatsAppCloudBridge(@Value("${parley.whatsapp.token:}") String token,
            @Value("${parley.whatsapp.phone:}") String phoneId,
            @Value("${parley.whatsapp.secret:}") String appSecret,
            @Value("${parley.whatsapp.verify:}") String verifyToken,
            @Value("${parley.whatsapp.api:https://graph.facebook.com/v21.0}") String api) {
        this.api = api;
        this.token = token;
        this.phoneId = phoneId;
        this.appSecret = appSecret;
        this.verifyToken = verifyToken;
    }

    @Override
    public String network() {
        return Bridges.WHATSAPP;
    }

    @Override
    public boolean configured() {
        return token.length() > 0 && phoneId.length() > 0 && appSecret.length() > 0
                && verifyToken.length() > 0;
    }

    /// Whether Meta's subscription check carries the verify token.
    boolean verifies(String mode, String given) {
        return configured() && "subscribe".equals(mode) && Signatures.same(verifyToken, given);
    }

    /// Whether `header` is the signature of `body`.
    boolean signed(byte[] body, String header) {
        return configured() && header != null && header.startsWith("sha256=")
                && Signatures.same(Signatures.hmacHex(appSecret, body), header.substring(7));
    }

    @Override
    public void send(String externalChat, String senderName, String text) throws IOException {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("body", senderName + ": " + text);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", externalChat);
        body.put("type", "text");
        body.put("text", message);
        List<String> headers = new ArrayList<>();
        headers.add("Content-Type: application/json");
        headers.add("Authorization: Bearer " + token);
        Web.Result sent = Web.request("POST", api + "/" + phoneId + "/messages", headers,
                Json.write(body).getBytes(StandardCharsets.UTF_8));
        if (!sent.isSuccess()) {
            throw new IOException("WhatsApp answered " + sent.getStatus());
        }
    }

    /// The text messages a webhook call carries, each `{from, name, text}`.
    static List<String[]> messages(Map payload) {
        List<String[]> out = new ArrayList<>();
        for (Object entry : list(payload.get("entry"))) {
            if (!(entry instanceof Map)) {
                continue;
            }
            for (Object change : list(((Map) entry).get("changes"))) {
                if (!(change instanceof Map) || !(((Map) change).get("value") instanceof Map)) {
                    continue;
                }
                Map value = (Map) ((Map) change).get("value");
                String name = "";
                List contacts = list(value.get("contacts"));
                if (!contacts.isEmpty() && contacts.get(0) instanceof Map
                        && ((Map) contacts.get(0)).get("profile") instanceof Map) {
                    Object n = ((Map) ((Map) contacts.get(0)).get("profile")).get("name");
                    name = n instanceof String ? (String) n : "";
                }
                for (Object m : list(value.get("messages"))) {
                    if (!(m instanceof Map)) {
                        continue;
                    }
                    Map message = (Map) m;
                    Object from = message.get("from");
                    Object text = message.get("text");
                    if (!(from instanceof String) || !(text instanceof Map)
                            || !(((Map) text).get("body") instanceof String)) {
                        continue;
                    }
                    // The message's own id, which a retried delivery repeats.
                    Object wamid = message.get("id");
                    out.add(new String[] {(String) from, name.length() > 0 ? name : (String) from,
                            (String) ((Map) text).get("body"),
                            wamid instanceof String ? (String) wamid : ""});
                }
            }
        }
        return out;
    }

    private static List list(Object value) {
        return value instanceof List ? (List) value : new ArrayList();
    }
}
