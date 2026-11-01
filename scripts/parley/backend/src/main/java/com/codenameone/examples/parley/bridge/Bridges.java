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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.Tasks;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.parley.Ids;
import com.codenameone.examples.parley.Text;
import com.codenameone.examples.parley.api.BridgeLinkDto;
import com.codenameone.examples.parley.api.BridgeRequestDto;
import com.codenameone.examples.parley.api.BridgeStatusDto;
import com.codenameone.examples.parley.chat.Conversations;
import com.codenameone.examples.parley.chat.Messages;
import com.codenameone.examples.parley.chat.Relays;
import com.codenameone.examples.parley.domain.BridgeEvent;
import com.codenameone.examples.parley.domain.BridgeLink;
import com.codenameone.examples.parley.domain.Conversation;
import com.codenameone.examples.parley.domain.Membership;
import com.codenameone.examples.parley.repository.BridgeEventRepository;
import com.codenameone.examples.parley.repository.BridgeLinkRepository;
import com.codenameone.examples.parley.repository.MembershipRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// The links between Parley conversations and chats on other networks, and
/// the traffic across them.
///
/// Only a group's owner or one of its admins links it, and the link is
/// announced in the group: everyone in it learns, in a notice they cannot
/// miss, that from now on what they write there leaves Parley in plain text.
@Component
public class Bridges {
    public static final String TELEGRAM = "TELEGRAM";
    public static final String WHATSAPP = "WHATSAPP";
    static final int MAX_TEXT = 4096;

    private final BridgeLinkRepository links;
    private final BridgeEventRepository events;
    private final MembershipRepository members;
    private final Conversations conversations;
    private final Messages messages;
    private final TelegramBridge telegram;
    private final WhatsAppCloudBridge whatsapp;

    public Bridges(BridgeLinkRepository links, BridgeEventRepository events,
            MembershipRepository members,
            Conversations conversations, Messages messages, Relays relays,
            TelegramBridge telegram, WhatsAppCloudBridge whatsapp) {
        this.links = links;
        this.events = events;
        this.members = members;
        this.conversations = conversations;
        this.messages = messages;
        this.telegram = telegram;
        this.whatsapp = whatsapp;
        relays.register(this::outgoing);
    }

    /// How long an event id is remembered: both networks give up retrying
    /// well within a day.
    static final long EVENT_MEMORY_MILLIS = 7L * 24 * 60 * 60 * 1000;

    /// Which networks this server can bridge to.
    public BridgeStatusDto status() {
        return new BridgeStatusDto(telegram.configured(), whatsapp.configured(),
                telegram.botName());
    }

    /// The links of the conversations `username` is in.
    @Transactional(readOnly = true)
    public List<BridgeLinkDto> list(String username) throws IOException {
        List<BridgeLinkDto> out = new ArrayList<>();
        for (Membership mine : members.findByUsername(username)) {
            for (BridgeLink link : links.findByConversationId(mine.getConversationId())) {
                out.add(toDto(link));
            }
        }
        return out;
    }

    /// Every link, newest first, for the admins.
    @Transactional(readOnly = true)
    public List<BridgeLinkDto> all() throws IOException {
        List<BridgeLinkDto> out = new ArrayList<>();
        for (BridgeLink link : links.newestFirst()) {
            out.add(toDto(link));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public BridgeLinkDto link(String username, BridgeRequestDto request) throws IOException {
        if (request == null) {
            throw new ResponseStatusException(400, "Nothing to link");
        }
        Membership mine = conversations.member(request.conversationId(), username);
        Conversation row = conversations.conversation(mine.getConversationId());
        if (Conversations.DIRECT.equals(row.getKind())) {
            throw new ResponseStatusException(400, "Only a group or a channel can be bridged");
        }
        manager(mine);
        Bridge bridge = bridge(request.network());
        String chat = Text.required(request.externalChat(), 128, "the chat to link to");
        if (links.findByNetworkAndExternalChat(bridge.network(), chat) != null) {
            throw new ResponseStatusException(409, "That chat is already linked");
        }
        if (!links.findByConversationId(row.getId()).isEmpty()) {
            throw new ResponseStatusException(409, "This conversation is already bridged");
        }
        BridgeLink link = new BridgeLink();
        link.setId(Ids.next());
        link.setConversationId(row.getId());
        link.setNetwork(bridge.network());
        link.setExternalChat(chat);
        link.setLabel(Text.optional(request.label(), 80, "the link's name"));
        link.setCreatedBy(username);
        link.setCreatedAt(Instant.now());
        links.save(link);
        conversations.announce(row.getId(), "This conversation is now bridged to "
                + name(bridge.network()) + ". Messages here are no longer end-to-end encrypted.");
        return toDto(link);
    }

    @Transactional(rollbackFor = IOException.class)
    public BridgeLinkDto unlink(String username, String id) throws IOException {
        BridgeLink link = links.findById(id);
        if (link == null) {
            throw new ResponseStatusException(404, "No such bridge");
        }
        manager(conversations.member(link.getConversationId(), username));
        links.deleteById(link.getId());
        conversations.announce(link.getConversationId(), "The bridge to "
                + name(link.getNetwork()) + " was removed. New messages here are end-to-end"
                + " encrypted again; the ones already sent stay as they were.");
        return toDto(link);
    }

    /// A message arrived from a linked chat. Unknown chats are ignored: a
    /// bot added to a chat nobody linked hears what is said there and passes
    /// none of it on.
    @Transactional(rollbackFor = IOException.class)
    public void incoming(String network, String externalChat, String senderName, String text,
            String eventId) throws IOException {
        if (text == null || text.length() == 0) {
            return;
        }
        // A retried delivery names the event it already sent. The insert is
        // what claims it: of two copies arriving at once, the second commit
        // fails on the key, the network sees an error and retries, and the
        // retry finds the event here.
        if (eventId != null && eventId.length() > 0) {
            String key = network + ":" + eventId;
            if (events.findById(key) != null) {
                return;
            }
            long now = System.currentTimeMillis();
            events.deleteByReceivedAtLessThan(Instant.ofEpochMilli(now - EVENT_MEMORY_MILLIS));
            events.insert(new BridgeEvent(key, Instant.ofEpochMilli(now)));
        }
        BridgeLink link = links.findByNetworkAndExternalChat(network, externalChat);
        if (link == null) {
            return;
        }
        String said = senderName + " (" + name(network) + "): " + text;
        messages.post(link.getConversationId(), Conversations.BOT,
                said.length() > MAX_TEXT ? said.substring(0, MAX_TEXT) : said);
    }

    /// A member posted into a conversation; if it is bridged, the text goes
    /// out, on a task of its own so the sender does not wait on the network.
    private void outgoing(String conversationId, String sender, String senderName, String text) {
        List<BridgeLink> linked = links.findByConversationId(conversationId);
        for (BridgeLink link : linked) {
            Bridge bridge = TELEGRAM.equals(link.getNetwork()) ? telegram : whatsapp;
            if (!bridge.configured()) {
                continue;
            }
            String chat = link.getExternalChat();
            Tasks.platform(() -> {
                try {
                    bridge.send(chat, senderName, text);
                } catch (Exception failed) {
                    System.err.println("[bridge] " + bridge.network() + " send failed: " + failed);
                }
            });
        }
    }

    private Bridge bridge(String network) {
        Bridge bridge = TELEGRAM.equals(network) ? telegram
                : WHATSAPP.equals(network) ? whatsapp : null;
        if (bridge == null) {
            throw new ResponseStatusException(400, "There is no such network");
        }
        if (!bridge.configured()) {
            throw new ResponseStatusException(409, "This server is not set up to bridge to "
                    + name(network));
        }
        return bridge;
    }

    private static void manager(Membership mine) {
        if (!Conversations.OWNER.equals(mine.getRole())
                && !Conversations.ADMIN.equals(mine.getRole())) {
            throw new ResponseStatusException(403, "Only the group's admins can change bridges");
        }
    }

    static String name(String network) {
        return TELEGRAM.equals(network) ? "Telegram" : "WhatsApp";
    }

    static BridgeLinkDto toDto(BridgeLink row) {
        return new BridgeLinkDto(row.getId(), row.getConversationId(), row.getNetwork(),
                row.getExternalChat(), row.getLabel(), row.getCreatedAt().toEpochMilli());
    }
}
