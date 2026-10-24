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
package com.codenameone.examples.parley.chat;

import com.codename1.backend.Base64;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.parley.Ids;
import com.codenameone.examples.parley.api.AckDto;
import com.codenameone.examples.parley.api.DeviceAddressDto;
import com.codenameone.examples.parley.api.EnvelopeCopyDto;
import com.codenameone.examples.parley.api.EnvelopeDto;
import com.codenameone.examples.parley.api.KeyCountDto;
import com.codenameone.examples.parley.api.OutgoingDto;
import com.codenameone.examples.parley.api.SendResultDto;
import com.codenameone.examples.parley.api.SyncDto;
import com.codenameone.examples.parley.device.Devices;
import com.codenameone.examples.parley.domain.Conversation;
import com.codenameone.examples.parley.domain.Device;
import com.codenameone.examples.parley.domain.Envelope;
import com.codenameone.examples.parley.domain.MessageLog;
import com.codenameone.examples.parley.domain.Membership;
import com.codenameone.examples.parley.domain.Profile;
import com.codenameone.examples.parley.repository.ContactBlockRepository;
import com.codenameone.examples.parley.repository.ConversationRepository;
import com.codenameone.examples.parley.repository.DeviceRepository;
import com.codenameone.examples.parley.repository.EnvelopeRepository;
import com.codenameone.examples.parley.repository.MediaBlobRepository;
import com.codenameone.examples.parley.repository.MessageLogRepository;
import com.codenameone.examples.parley.repository.OneTimePreKeyRepository;
import com.codenameone.examples.parley.repository.ProfileRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;

/// Sending, fetching and acknowledging envelopes.
///
/// A sender encrypts a message once for every device in the conversation --
/// the other members' and its own other devices -- and hands the server the
/// lot. The server checks that the copies cover exactly the devices it knows
/// of: a copy missing means the sender has not heard of a device yet, and a
/// copy for a device that is gone means it is holding on to one. Either way
/// nothing is stored, and the answer lists the devices so the sender can fetch
/// their keys and send again. That is what keeps a newly linked phone from
/// silently missing messages.
///
/// What the server stores is opaque: a ratchet header and a ciphertext per
/// device. It deletes each copy when its device acknowledges it, and copies
/// nobody fetched when the conversation's disappearing timer runs out.
@Component
public class Messages {
    /// Kinds of envelope a client sends. MESSAGE is anything a person wrote;
    /// RECEIPT, TYPING and KEYS are protocol; SIGNAL is call setup.
    public static final String MESSAGE = "MESSAGE";
    public static final String RECEIPT = "RECEIPT";
    public static final String TYPING = "TYPING";
    public static final String KEYS = "KEYS";
    public static final String SIGNAL = "SIGNAL";

    /// The largest copy accepted. Attachments travel separately, so a message
    /// is text, a reference to an attachment, or protocol.
    static final int MAX_CIPHERTEXT = 64 * 1024;

    /// How long an envelope nobody fetches is kept.
    static final long UNCLAIMED = 30L * 24 * 60 * 60 * 1000;

    private final Conversations conversations;
    private final ConversationRepository conversationRows;
    private final Devices devices;
    private final DeviceRepository deviceRows;
    private final EnvelopeRepository envelopes;
    private final MessageLogRepository log;
    private final ContactBlockRepository blocks;
    private final ProfileRepository profiles;
    private final OneTimePreKeyRepository preKeys;
    private final MediaBlobRepository media;
    private final Fanout fanout;
    private final Relays relays;

    public Messages(Conversations conversations, ConversationRepository conversationRows,
            Devices devices, DeviceRepository deviceRows, EnvelopeRepository envelopes,
            MessageLogRepository log, ContactBlockRepository blocks, ProfileRepository profiles,
            OneTimePreKeyRepository preKeys, MediaBlobRepository media, Fanout fanout,
            Relays relays) {
        this.conversations = conversations;
        this.conversationRows = conversationRows;
        this.devices = devices;
        this.deviceRows = deviceRows;
        this.envelopes = envelopes;
        this.log = log;
        this.blocks = blocks;
        this.profiles = profiles;
        this.preKeys = preKeys;
        this.media = media;
        this.fanout = fanout;
        this.relays = relays;
    }

    @Transactional(rollbackFor = IOException.class)
    public SendResultDto send(String username, OutgoingDto message) throws IOException {
        if (message == null) {
            throw new ResponseStatusException(400, "Nothing to send");
        }
        String kind = message.kind() == null ? MESSAGE : message.kind();
        if (!(MESSAGE.equals(kind) || RECEIPT.equals(kind) || TYPING.equals(kind)
                || KEYS.equals(kind) || SIGNAL.equals(kind))) {
            throw new ResponseStatusException(400, "That is not a kind of message");
        }
        String id = message.conversationId();
        Membership mine = conversations.member(id, username);
        Conversation row = conversations.conversation(id);
        devices.owned(username, message.senderDevice());
        if (MESSAGE.equals(kind) && row.isAdminsOnly() && !Conversations.admin(mine)) {
            throw new ResponseStatusException(403, "Only admins send messages here");
        }
        boolean group = !Conversations.DIRECT.equals(row.getKind());
        long now = System.currentTimeMillis();
        long expiresAt = row.getDisappearingSeconds() > 0
                ? now + row.getDisappearingSeconds() * 1000L : 0L;

        // A retry of a message already stored answers what the first try did.
        String clientId = message.clientId() == null ? "" : message.clientId().trim();
        String logId = null;
        if (MESSAGE.equals(kind)) {
            if (clientId.length() == 0 || clientId.length() > 64) {
                throw new ResponseStatusException(400, "A message needs the sender's id for it");
            }
            logId = Ids.sha256(username + "/" + clientId);
            MessageLog seen = log.findById(logId);
            if (seen != null) {
                return new SendResultDto(seen.getMessageId(), seen.getSentAt(),
                        new ArrayList<DeviceAddressDto>(), new ArrayList<DeviceAddressDto>());
            }
        }

        String plain = conversations.bridge(id);
        if (plain.length() > 0) {
            return sendPlain(username, message, row, kind, logId, now, expiresAt, group);
        }

        List<String> members = conversations.usernames(id);
        List<Device> targets = deviceRows.findByUsernameIn(members);
        // Each device to the account that owns it: a copy names both, and a
        // copy whose pair does not match is stale, or a member could address
        // Bob's device under its own name and step around Bob's block.
        Map<String, String> expected = new HashMap<String, String>();
        for (int iter = 0; iter < targets.size(); iter++) {
            if (!targets.get(iter).getId().equals(message.senderDevice())) {
                expected.put(targets.get(iter).getId(), targets.get(iter).getUsername());
            }
        }
        List<EnvelopeCopyDto> copies = message.copies() == null
                ? new ArrayList<EnvelopeCopyDto>() : message.copies();
        Set<String> covered = new HashSet<String>();
        List<DeviceAddressDto> stale = new ArrayList<DeviceAddressDto>();
        for (int iter = 0; iter < copies.size(); iter++) {
            EnvelopeCopyDto copy = copies.get(iter);
            if (copy == null || copy.deviceId() == null) {
                throw new ResponseStatusException(400, "A copy for no device");
            }
            if (copy.username() == null || !copy.username().equals(expected.get(copy.deviceId()))) {
                stale.add(new DeviceAddressDto(copy.username(), copy.deviceId()));
            } else if (!covered.add(copy.deviceId())) {
                throw new ResponseStatusException(400, "Two copies for one device");
            }
        }
        List<DeviceAddressDto> missing = new ArrayList<DeviceAddressDto>();
        // Protocol envelopes may be addressed to some devices only: a receipt
        // goes to the devices of whoever sent the message, a session reset to
        // the one device it concerns. A message reaches everyone or nobody.
        if (MESSAGE.equals(kind)) {
            for (int iter = 0; iter < targets.size(); iter++) {
                Device each = targets.get(iter);
                if (expected.containsKey(each.getId()) && !covered.contains(each.getId())) {
                    missing.add(new DeviceAddressDto(each.getUsername(), each.getId()));
                }
            }
        }
        if (!missing.isEmpty() || !stale.isEmpty()) {
            return new SendResultDto("", now, missing, stale);
        }

        String messageId = Ids.next();
        List<Envelope> batch = new ArrayList<Envelope>();
        List<String> wake = new ArrayList<String>();
        for (int iter = 0; iter < copies.size(); iter++) {
            EnvelopeCopyDto copy = copies.get(iter);
            // Somebody who blocked the sender is not told the message exists,
            // and the sender is not told it was not delivered: a block that
            // announces itself invites working around it.
            if (!copy.username().equals(username) && blocked(copy.username(), username)) {
                continue;
            }
            byte[] payload = copy.ciphertext() == null ? null : Base64.decode(copy.ciphertext());
            if (payload == null || payload.length > MAX_CIPHERTEXT) {
                throw new ResponseStatusException(400, "A copy is not a ciphertext this server takes");
            }
            String header = copy.header() == null ? "" : copy.header();
            if (header.length() > 2048) {
                throw new ResponseStatusException(400, "That header is too long");
            }
            batch.add(Fanout.envelope(messageId, id, username, message.senderDevice(),
                    copy.username(), copy.deviceId(), kind, header, payload, now, expiresAt));
            if (MESSAGE.equals(kind) && !copy.username().equals(username)
                    && !wake.contains(copy.username()) && !muted(id, copy.username())) {
                wake.add(copy.username());
            }
        }
        if (TYPING.equals(kind)) {
            fanout.ephemeral(batch);
            return new SendResultDto(messageId, now, missing, stale);
        }
        fanout.deliver(batch, wake, group);
        if (MESSAGE.equals(kind)) {
            record(logId, messageId, id, username, kind, now);
            conversationRows.touched(id, now);
        }
        return new SendResultDto(messageId, now, missing, stale);
    }

    private SendResultDto sendPlain(String username, OutgoingDto message, Conversation row,
            String kind, String logId, long now, long expiresAt, boolean group)
            throws IOException {
        List<DeviceAddressDto> none = new ArrayList<DeviceAddressDto>();
        if (!MESSAGE.equals(kind)) {
            // Receipts and typing mean nothing on the other network, and the
            // bot reads everything it is sent.
            return new SendResultDto("", now, none, none);
        }
        String text = message.plaintext() == null ? "" : message.plaintext().trim();
        if (text.length() == 0 || text.length() > 4096) {
            throw new ResponseStatusException(400, "A message here is plain text, up to 4096 characters");
        }
        List<String> to = conversations.usernames(row.getId());
        String messageId = fanout.plain(row.getId(), username, message.senderDevice(), MESSAGE,
                to, text, expiresAt, group);
        record(logId, messageId, row.getId(), username, kind, now);
        conversationRows.touched(row.getId(), now);
        Profile profile = profiles.findById(username);
        relays.posted(row.getId(), username, profile == null ? username : profile.getDisplayName(),
                text);
        return new SendResultDto(messageId, now, none, none);
    }

    /// Posts plain text into a conversation as `sender`, from the server: the
    /// demo bot's replies and messages arriving over a bridge.
    @Transactional(rollbackFor = IOException.class)
    public String post(String conversationId, String sender, String text) throws IOException {
        Conversation row = conversations.conversation(conversationId);
        long now = System.currentTimeMillis();
        long expiresAt = row.getDisappearingSeconds() > 0
                ? now + row.getDisappearingSeconds() * 1000L : 0L;
        String messageId = fanout.plain(conversationId, sender, Fanout.SERVER_DEVICE, MESSAGE,
                conversations.usernames(conversationId), text, expiresAt,
                !Conversations.DIRECT.equals(row.getKind()));
        record(Ids.sha256(sender + "/" + messageId), messageId, conversationId, sender, MESSAGE,
                now);
        conversationRows.touched(conversationId, now);
        return messageId;
    }

    /// The oldest envelopes waiting for one of the caller's devices.
    @Transactional(readOnly = true)
    public SyncDto sync(String username, String device, int limit) {
        devices.owned(username, device);
        List<Envelope> rows = envelopes.findTop100ByRecipientDeviceOrderBySentAtAscIdAsc(device);
        int n = limit <= 0 ? rows.size() : Math.min(limit, rows.size());
        List<EnvelopeDto> out = new ArrayList<EnvelopeDto>();
        for (int iter = 0; iter < n; iter++) {
            out.add(Fanout.toDto(rows.get(iter)));
        }
        return new SyncDto(out, n < rows.size() || rows.size() == 100);
    }

    /// Deletes envelopes the device has stored. Answers how many one-time
    /// prekeys it has left, so the app knows when to upload more without
    /// asking separately.
    @Transactional(rollbackFor = IOException.class)
    public KeyCountDto ack(String username, String device, AckDto ack) {
        devices.owned(username, device);
        for (int iter = 0; ack != null && ack.ids() != null && iter < ack.ids().size(); iter++) {
            envelopes.deleteByIdAndRecipientDevice(ack.ids().get(iter), device);
        }
        return new KeyCountDto((int) preKeys.countByDeviceId(device));
    }

    /// Deletes what the disappearing timer ran out on, envelopes nobody
    /// fetched for a month, and attachments as old: an attachment's key is in
    /// its message, and the message is long gone from the server by then.
    @Scheduled(fixedDelay = 30000, initialDelay = 15000, lock = "parley-reaper")
    @Transactional
    public void reap() {
        long now = System.currentTimeMillis();
        envelopes.expire(now);
        envelopes.deleteBySentAtLessThan(now - UNCLAIMED);
        media.deleteByCreatedAtLessThan(now - UNCLAIMED);
        log.deleteBySentAtLessThan(now - 90L * 24 * 60 * 60 * 1000);
    }

    private void record(String logId, String messageId, String conversationId, String sender,
            String kind, long now) {
        if (logId == null) {
            return;
        }
        MessageLog row = new MessageLog();
        row.setId(logId);
        row.setMessageId(messageId);
        row.setConversationId(conversationId);
        row.setSender(sender);
        row.setKind(kind);
        row.setSentAt(now);
        log.insert(row);
    }

    private boolean muted(String conversationId, String username) {
        Membership row = conversations.member(conversationId, username);
        return row.isMuted();
    }

    private boolean blocked(String blocker, String blocked) throws IOException {
        return blocks.existsById(blockId(blocker, blocked));
    }

    /// The id of the row that says `blocker` blocked `blocked`.
    public static String blockId(String blocker, String blocked) throws IOException {
        return Ids.sha256(blocker + " " + blocked);
    }
}
