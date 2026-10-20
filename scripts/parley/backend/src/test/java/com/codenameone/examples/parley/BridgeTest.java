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
package com.codenameone.examples.parley;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.parley.account.Accounts;
import com.codename1.backend.Crypto;
import com.codenameone.examples.parley.chat.Conversations;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Linking conversations to Telegram and WhatsApp, and the webhooks that
/// carry their messages in. The test profile configures both bridges against
/// a port nobody listens on, so what goes out fails on this machine.
@BackendTest
class BridgeTest {
    private static final String TELEGRAM_SECRET = "test-telegram-secret-0123";
    private static final String WHATSAPP_SECRET = "test-whatsapp-app-secret";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    private int link(String who, String conversation, String network, String chat)
            throws Exception {
        return Chat.statusOf(mvc, People.asUser(Chat.body(post("/api/bridges"),
                Chat.map("conversationId", conversation, "network", network,
                        "externalChat", chat, "label", "")), who));
    }

    @Test
    void aGroupsAdminLinksItToTelegram() throws Exception {
        String owner = People.user(accounts, "tg-owner");
        String member = People.user(accounts, "tg-member");
        String members = Chat.device(mvc, member);
        String group = Chat.conversation(mvc, owner, Conversations.GROUP, "Hikers", member);
        assertTrue(Boolean.TRUE.equals(Chat.json(mvc, People.asUser(get("/api/bridges/status"),
                member)).get("telegram")));
        assertEquals(403, link(member, group, "TELEGRAM", "-100200"));
        assertEquals(200, link(owner, group, "TELEGRAM", "-100200"));
        assertEquals(409, link(owner, group, "TELEGRAM", "-100200"));
        assertEquals("TELEGRAM", Chat.json(mvc, People.asUser(get("/api/conversations/" + group),
                member)).get("bridge"));

        String update = "{\"update_id\":1,\"message\":{\"message_id\":5,\"from\":{\"id\":7,"
                + "\"is_bot\":false,\"first_name\":\"Tomas\"},\"chat\":{\"id\":-100200,"
                + "\"type\":\"group\"},\"text\":\"See you at eight\"}}";
        assertEquals(404, Chat.statusOf(mvc, Chat.body(post("/bridge/telegram/wrong"), update)
                .header("X-Telegram-Bot-Api-Secret-Token", TELEGRAM_SECRET)));
        assertEquals(404, Chat.statusOf(mvc, Chat.body(post("/bridge/telegram/" + TELEGRAM_SECRET),
                update)));
        assertEquals(200, Chat.statusOf(mvc, Chat.body(post("/bridge/telegram/" + TELEGRAM_SECRET),
                update).header("X-Telegram-Bot-Api-Secret-Token", TELEGRAM_SECRET)));
        Map arrived = Chat.await(mvc, member, members, Conversations.BOT);
        assertNotNull(arrived);
        assertEquals("Tomas (Telegram): See you at eight", Chat.text(arrived));
    }

    @Test
    void aDirectChatIsNeverBridged() throws Exception {
        String alice = People.user(accounts, "tg-direct-a");
        String bob = People.user(accounts, "tg-direct-b");
        String chat = Chat.conversation(mvc, alice, Conversations.DIRECT, "", bob);
        assertEquals(400, link(alice, chat, "TELEGRAM", "-100300"));
    }

    @Test
    void whatsAppProvesItselfWithASignature() throws Exception {
        String owner = People.user(accounts, "wa-owner");
        String owners = Chat.device(mvc, owner);
        String group = Chat.conversation(mvc, owner, Conversations.GROUP, "Support");
        assertEquals(200, link(owner, group, "WHATSAPP", "15550001"));

        assertEquals("abc", mvc.perform(get("/bridge/whatsapp?hub.mode=subscribe"
                + "&hub.verify_token=test-verify&hub.challenge=abc")).andReturn().getResponse()
                .getContentAsString());
        assertEquals(404, Chat.statusOf(mvc, get("/bridge/whatsapp?hub.mode=subscribe"
                + "&hub.verify_token=wrong&hub.challenge=abc")));

        String body = "{\"entry\":[{\"changes\":[{\"value\":{\"contacts\":[{\"profile\":"
                + "{\"name\":\"Rosa\"}}],\"messages\":[{\"from\":\"15550001\",\"type\":\"text\","
                + "\"text\":{\"body\":\"Is it open today?\"}}]}}]}]}";
        assertEquals(404, Chat.statusOf(mvc, Chat.body(post("/bridge/whatsapp"), body)
                .header("X-Hub-Signature-256", "sha256=00")));
        assertEquals(200, Chat.statusOf(mvc, Chat.body(post("/bridge/whatsapp"), body)
                .header("X-Hub-Signature-256", "sha256=" + hex(Crypto.hmacSha256(
                        WHATSAPP_SECRET.getBytes(StandardCharsets.UTF_8),
                        body.getBytes(StandardCharsets.UTF_8))))));
        Map arrived = Chat.await(mvc, owner, owners, Conversations.BOT);
        assertNotNull(arrived);
        assertEquals("Rosa (WhatsApp): Is it open today?", Chat.text(arrived));
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        for (byte b : bytes) {
            out.append("0123456789abcdef".charAt((b >> 4) & 15)).append("0123456789abcdef".charAt(b & 15));
        }
        return out.toString();
    }
}
