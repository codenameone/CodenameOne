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

/// A Telegram bot, over the Bot API.
///
/// The bot is added to a Telegram group and the group's id is linked to a
/// Parley conversation. Telegram sends the bot's updates to
/// `/bridge/telegram/{secret}` (see [WebhookController]), registered with
/// `setWebhook` and the same secret as its `secret_token`, so each update
/// proves itself twice: by the path, and by the
/// `X-Telegram-Bot-Api-Secret-Token` header. The bot must have privacy mode
/// off (`/setprivacy` with BotFather) to see a group's messages.
///
/// Configured by `parley.telegram.token`, `parley.telegram.secret` and
/// `parley.telegram.bot`, the bot's name, which the app shows so people know
/// whom to add; `parley.telegram.api` points it at another server, which the
/// tests use so that nothing they do reaches Telegram.
@Component
public class TelegramBridge implements Bridge {
    private final String api;
    private final String token;
    private final String secret;
    private final String bot;

    public TelegramBridge(@Value("${parley.telegram.token:}") String token,
            @Value("${parley.telegram.secret:}") String secret,
            @Value("${parley.telegram.bot:}") String bot,
            @Value("${parley.telegram.api:https://api.telegram.org}") String api) {
        this.api = api;
        this.token = token;
        this.secret = secret;
        this.bot = bot;
    }

    @Override
    public String network() {
        return Bridges.TELEGRAM;
    }

    @Override
    public boolean configured() {
        return token.length() > 0 && secret.length() >= 16;
    }

    String botName() {
        return configured() ? bot : "";
    }

    /// Whether an update came from Telegram: both the path and the header
    /// carry the secret.
    boolean authentic(String pathSecret, String header) {
        return configured() && Signatures.same(secret, pathSecret)
                && Signatures.same(secret, header);
    }

    @Override
    public void send(String externalChat, String senderName, String text) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chat_id", externalChat);
        body.put("text", senderName + ": " + text);
        List<String> headers = new ArrayList<>();
        headers.add("Content-Type: application/json");
        Web.Result sent = Web.request("POST", api + "/bot" + token + "/sendMessage", headers,
                Json.write(body).getBytes(StandardCharsets.UTF_8));
        if (!sent.isSuccess()) {
            // The token is part of the address; it is never printed.
            throw new IOException("Telegram answered " + sent.getStatus());
        }
    }

    /// The text message an update carries, as `{chat, sender, text}`; null
    /// for anything else: edits, joins, stickers, the bot's own messages.
    static String[] message(Map update) {
        Object message = update.get("message");
        if (!(message instanceof Map)) {
            return null;
        }
        Map m = (Map) message;
        Object chat = m.get("chat");
        Object text = m.get("text");
        if (!(chat instanceof Map) || !(text instanceof String)) {
            return null;
        }
        Object from = m.get("from");
        String sender = "Someone";
        if (from instanceof Map) {
            Map f = (Map) from;
            if (Boolean.TRUE.equals(f.get("is_bot"))) {
                return null;
            }
            Object first = f.get("first_name");
            if (first instanceof String) {
                sender = (String) first;
            }
        }
        Object id = ((Map) chat).get("id");
        if (id == null) {
            return null;
        }
        return new String[] {id(id), sender, (String) text};
    }

    /// A chat id as text. JSON numbers come back as a Long or a Double; a
    /// group's id is a negative integer either way.
    static String id(Object value) {
        if (value instanceof Number) {
            return Long.toString(((Number) value).longValue());
        }
        return String.valueOf(value);
    }
}
