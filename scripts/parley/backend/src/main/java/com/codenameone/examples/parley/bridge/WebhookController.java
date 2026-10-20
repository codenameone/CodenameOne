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

import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/// Where Telegram and WhatsApp deliver messages.
///
/// Both read the raw request rather than a bound body: WhatsApp's signature
/// is over the bytes as they were sent, and a body parsed and written again
/// would not be them. A call that does not prove itself is answered 404, so a
/// stranger probing the path learns nothing about whether a bridge is set up.
/// Both networks retry anything but a 2xx, so a message that arrives for a
/// chat nobody linked is still answered 200, and dropped.
@RestController
public class WebhookController {
    private final Bridges bridges;
    private final TelegramBridge telegram;
    private final WhatsAppCloudBridge whatsapp;

    public WebhookController(Bridges bridges, TelegramBridge telegram,
            WhatsAppCloudBridge whatsapp) {
        this.bridges = bridges;
        this.telegram = telegram;
        this.whatsapp = whatsapp;
    }

    @PostMapping("/bridge/telegram/{secret}")
    public HttpServer.Response telegram(@PathVariable("secret") String secret,
            HttpServer.Request request) throws Exception {
        if (!telegram.authentic(secret, request.getHeader("X-Telegram-Bot-Api-Secret-Token"))) {
            return HttpServer.Response.text(404, "Not found");
        }
        Map update = Json.parseObject(new String(request.getBodyBytes(), StandardCharsets.UTF_8));
        String[] message = TelegramBridge.message(update);
        if (message != null) {
            bridges.incoming(Bridges.TELEGRAM, message[0], message[1], message[2]);
        }
        return HttpServer.Response.text(200, "ok");
    }

    @GetMapping("/bridge/whatsapp")
    public HttpServer.Response verify(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String token,
            @RequestParam(value = "hub.challenge", required = false) String challenge) {
        if (!whatsapp.verifies(mode, token) || challenge == null) {
            return HttpServer.Response.text(404, "Not found");
        }
        return HttpServer.Response.text(200, challenge);
    }

    @PostMapping("/bridge/whatsapp")
    public HttpServer.Response whatsapp(HttpServer.Request request) throws Exception {
        byte[] body = request.getBodyBytes();
        if (!whatsapp.signed(body, request.getHeader("X-Hub-Signature-256"))) {
            return HttpServer.Response.text(404, "Not found");
        }
        List<String[]> messages = WhatsAppCloudBridge.messages(
                Json.parseObject(new String(body, StandardCharsets.UTF_8)));
        for (String[] message : messages) {
            bridges.incoming(Bridges.WHATSAPP, message[0], message[1], message[2]);
        }
        return HttpServer.Response.text(200, "ok");
    }
}
