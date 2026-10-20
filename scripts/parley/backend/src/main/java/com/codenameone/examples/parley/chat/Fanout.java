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
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.parley.Ids;
import com.codenameone.examples.parley.api.EnvelopeDto;
import com.codenameone.examples.parley.domain.Device;
import com.codenameone.examples.parley.domain.Envelope;
import com.codenameone.examples.parley.live.LiveHub;
import com.codenameone.examples.parley.push.MessagePushes;
import com.codenameone.examples.parley.repository.DeviceRepository;
import com.codenameone.examples.parley.repository.EnvelopeRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Puts envelopes in front of their devices: stores each one, sends it over
/// the live channel when its device is connected, and wakes the accounts that
/// have no device connected with a push that says nothing.
///
/// Delivery is at least once. An envelope is stored before it is sent live,
/// inside the sender's transaction, so a device can see it before the commit
/// and then again on its next sync; the app keeps envelope ids it has seen and
/// drops a repeat. Nothing is ever delivered zero times.
@Component
public class Fanout {
    /// The device id of messages the server writes itself: system notices,
    /// the demo bot, bridged messages.
    public static final String SERVER_DEVICE = "server";

    private final EnvelopeRepository envelopes;
    private final DeviceRepository devices;
    private final LiveHub live;
    private final MessagePushes pushes;

    public Fanout(EnvelopeRepository envelopes, DeviceRepository devices, LiveHub live,
            MessagePushes pushes) {
        this.envelopes = envelopes;
        this.devices = devices;
        this.live = live;
        this.pushes = pushes;
    }

    /// Stores and sends envelopes; `wake` lists the accounts to push to when
    /// none of their devices is connected, and `group` picks which of their
    /// notification settings applies.
    public void deliver(List<Envelope> batch, List<String> wake, boolean group) {
        for (int iter = 0; iter < batch.size(); iter++) {
            envelopes.insert(batch.get(iter));
        }
        send(batch);
        for (int iter = 0; wake != null && iter < wake.size(); iter++) {
            if (!live.online(wake.get(iter))) {
                pushes.wake(wake.get(iter), group);
            }
        }
    }

    /// Sends envelopes live without storing them: typing indicators, which
    /// are worthless a moment later, go only to devices that are connected.
    public void ephemeral(List<Envelope> batch) {
        send(batch);
    }

    /// A notice in plain text from the server to every device of these
    /// accounts: someone joined, the timer changed. It says nothing the server
    /// does not already know, so it is not encrypted.
    public void notice(String conversationId, List<String> to, String text, long expiresAt,
            boolean group) throws IOException {
        plain(conversationId, "", SERVER_DEVICE, "SYSTEM", to, text, expiresAt, group);
    }

    /// A plain-text message from `sender`, for conversations that are not end
    /// to end encrypted: the demo bot's and bridged ones.
    public String plain(String conversationId, String sender, String senderDevice, String kind,
            List<String> to, String text, long expiresAt, boolean group) throws IOException {
        String messageId = Ids.next();
        long now = System.currentTimeMillis();
        byte[] payload = text.getBytes("UTF-8");
        List<Device> targets = devices.findByUsernameIn(to);
        List<Envelope> batch = new ArrayList<Envelope>();
        for (int iter = 0; iter < targets.size(); iter++) {
            Device device = targets.get(iter);
            if (device.getId().equals(senderDevice)) {
                continue;
            }
            batch.add(envelope(messageId, conversationId, sender, senderDevice,
                    device.getUsername(), device.getId(), kind, "", payload, now, expiresAt));
        }
        List<String> wake = new ArrayList<String>();
        if (!"SYSTEM".equals(kind)) {
            for (int iter = 0; iter < to.size(); iter++) {
                if (!to.get(iter).equals(sender)) {
                    wake.add(to.get(iter));
                }
            }
        }
        deliver(batch, wake, group);
        return messageId;
    }

    static Envelope envelope(String messageId, String conversationId, String sender,
            String senderDevice, String recipient, String recipientDevice, String kind,
            String header, byte[] payload, long sentAt, long expiresAt) throws IOException {
        Envelope row = new Envelope();
        row.setId(Ids.next());
        row.setMessageId(messageId);
        row.setConversationId(conversationId);
        row.setSender(sender);
        row.setSenderDevice(senderDevice);
        row.setRecipient(recipient);
        row.setRecipientDevice(recipientDevice);
        row.setKind(kind);
        row.setHeader(header == null ? "" : header);
        row.setPayload(payload);
        row.setSentAt(sentAt);
        row.setExpiresAt(expiresAt);
        return row;
    }

    private void send(List<Envelope> batch) {
        for (int iter = 0; iter < batch.size(); iter++) {
            Envelope row = batch.get(iter);
            Map<String, Object> frame = new LinkedHashMap<String, Object>();
            frame.put("type", "envelope");
            frame.put("id", row.getId());
            frame.put("messageId", row.getMessageId());
            frame.put("conversationId", row.getConversationId());
            frame.put("sender", row.getSender());
            frame.put("senderDevice", row.getSenderDevice());
            frame.put("kind", row.getKind());
            frame.put("header", row.getHeader());
            frame.put("ciphertext", Base64.encode(row.getPayload()));
            frame.put("sentAt", Long.valueOf(row.getSentAt()));
            frame.put("expiresAt", Long.valueOf(row.getExpiresAt()));
            live.toDevice(row.getRecipientDevice(), frame);
        }
    }

    static EnvelopeDto toDto(Envelope row) {
        return new EnvelopeDto(row.getId(), row.getMessageId(), row.getConversationId(),
                row.getSender(), row.getSenderDevice(), row.getKind(), row.getHeader(),
                Base64.encode(row.getPayload()), row.getSentAt(), row.getExpiresAt());
    }
}
