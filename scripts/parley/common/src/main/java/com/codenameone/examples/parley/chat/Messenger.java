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

import com.codename1.io.Log;
import com.codename1.security.CryptoException;
import com.codename1.security.SecureRandom;
import com.codename1.ui.CN;
import com.codenameone.examples.parley.api.AckDto;
import com.codenameone.examples.parley.api.ConversationDto;
import com.codenameone.examples.parley.api.DeviceAddressDto;
import com.codenameone.examples.parley.api.EnvelopeCopyDto;
import com.codenameone.examples.parley.api.EnvelopeDto;
import com.codenameone.examples.parley.api.OutgoingDto;
import com.codenameone.examples.parley.api.SendResultDto;
import com.codenameone.examples.parley.crypto.Codec;
import com.codenameone.examples.parley.crypto.Identity;
import com.codenameone.examples.parley.crypto.Sessions;
import com.codenameone.examples.parley.crypto.Vault;
import com.codenameone.examples.parley.live.LiveChannel;
import com.codenameone.examples.parley.Prefs;
import com.codenameone.examples.parley.net.Api;
import com.codenameone.examples.parley.net.Net;
import com.codenameone.examples.parley.net.Session;
import com.codenameone.examples.parley.store.Message;
import com.codenameone.examples.parley.store.Store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;

/// Sends and receives messages: the part of the app that talks to the
/// server's queue, and the only part that sees a plaintext and a ciphertext
/// side by side.
///
/// **Sending.** A message is a small JSON payload. It is encrypted once for
/// every device in the conversation -- the other members' and this account's
/// other devices -- each with that device's own [Sessions] ratchet, and the
/// copies go up in one request. The server checks that the copies cover
/// exactly the conversation's devices and says which are missing or stale;
/// the device list is then fetched again and the message re-encrypted. Group
/// messages work the same way (pairwise fan-out, as Signal's groups did before
/// sender keys), so a member who leaves simply stops getting copies.
///
/// **Receiving.** Envelopes come down the [LiveChannel] while the app is open
/// and from [#sync()] when it opens or a push wakes it. Each is decrypted,
/// applied to the [Store], and acknowledged, and the acknowledgement is what
/// deletes it from the server.
///
/// Everything here runs on the EDT, one send at a time, so ratchet state never
/// changes under a request that is still on its way.
public final class Messenger implements LiveChannel.Handler {
    /// Payload types, the `t` of the JSON inside a ciphertext.
    static final String MSG = "msg";
    static final String REACT = "react";
    static final String EDIT = "edit";
    static final String DELETE = "delete";
    static final String RECEIPT = "receipt";
    static final String TYPING = "typing";

    /// Envelope kinds the server knows.
    static final String KIND_MESSAGE = "MESSAGE";
    static final String KIND_RECEIPT = "RECEIPT";
    static final String KIND_TYPING = "TYPING";

    /// Who is typing where: conversation -> username -> until when.
    public interface TypingListener {
        void typing(String conversationId);
    }

    private static Messenger instance;

    private final String me;
    private final List<Job> queue = new ArrayList<Job>();
    private boolean busy;
    private boolean syncing;
    private boolean syncAgain;
    private final Map<String, List<DeviceAddressDto>> devices = new HashMap<String, List<DeviceAddressDto>>();
    private final Set<String> processed = new LinkedHashSet<String>();
    private final List<String> toAck = new ArrayList<String>();
    private Timer ackTimer;
    private Timer housekeeping;
    private final Map<String, Map<String, Long>> typing = new HashMap<String, Map<String, Long>>();
    private TypingListener typingListener;
    private final Map<String, Long> lastTypingSent = new HashMap<String, Long>();

    private Messenger(String me) {
        this.me = me;
    }

    /// Opens this account's vault and keys, registers the device the first
    /// time, connects the live channel and fetches what is waiting.
    public static void start(final Runnable ready, final Net.Failed failed) {
        stop();
        String owner = Session.owner();
        Vault.open(owner);
        Identity.load();
        Store.load();
        instance = new Messenger(Session.user().username());
        if (Identity.deviceId().length() > 0) {
            instance.started();
            ready.run();
            return;
        }
        Api.devices().register(Identity.registration(deviceName(), platform()),
                Net.to(device -> {
                    Identity.registered(device.id());
                    if (instance != null) {
                        instance.started();
                    }
                    ready.run();
                }, failed));
    }

    /// The server no longer knows this device -- unlinked elsewhere, or a
    /// server that started over -- so it becomes a new one, and the channel
    /// and the sync start again under the new id.
    private void registerAgain() {
        LiveChannel.stop();
        Api.devices().register(Identity.registration(deviceName(), platform()),
                Net.to(device -> {
                    Identity.registered(device.id());
                    if (instance == this) {
                        started();
                    }
                }, (status, message) -> Log.p("Could not register this device again: " + message)));
    }

    public static void stop() {
        if (instance != null) {
            if (instance.ackTimer != null) {
                instance.ackTimer.cancel();
            }
            if (instance.housekeeping != null) {
                instance.housekeeping.cancel();
            }
            instance = null;
        }
        LiveChannel.stop();
        Sessions.clear();
        Store.clear();
        Identity.forget();
        Vault.close();
    }

    /// Signing out of this device for good: the vault, keys and history go.
    public static void wipe(String owner) {
        stop();
        Vault.open(owner);
        Vault.destroy(owner);
    }

    public static boolean running() {
        return instance != null;
    }

    public static String me() {
        return instance == null ? "" : instance.me;
    }

    private void started() {
        LiveChannel.start(Identity.deviceId(), this);
        Calls.signedIn();
        refreshConversations();
        sync();
        topUpPreKeys();
        if (housekeeping != null) {
            housekeeping.cancel();
        }
        housekeeping = CN.setInterval(30000, () -> CN.callSerially(() -> {
            if (instance == this) {
                Store.expireAll();
                if (Identity.signedPreKeyAge() > 7L * 24 * 60 * 60 * 1000) {
                    Identity.rotateSignedPreKey();
                    Api.devices().uploadPreKeys(Identity.deviceId(), Identity.topUp(),
                            Net.to(count -> { }, (status, message) -> { }));
                }
            }
        }));
    }

    /// The platform in the server's words: `CN.getPlatformName()` says
    /// `and` and `HTML5`, and a simulator says whichever phone it shows.
    private static String platform() {
        if (CN.isSimulator()) {
            return "simulator";
        }
        String platform = CN.getPlatformName();
        if ("ios".equals(platform)) {
            return "ios";
        }
        if ("and".equals(platform)) {
            return "android";
        }
        return "HTML5".equals(platform) ? "web" : "desktop";
    }

    private static String deviceName() {
        String platform = CN.getPlatformName();
        if ("ios".equals(platform)) {
            return "iPhone";
        }
        if ("and".equals(platform)) {
            return "Android phone";
        }
        if ("HTML5".equals(platform)) {
            return "Web browser";
        }
        return CN.isSimulator() ? "Simulator" : "Desktop";
    }

    /// Fetches the conversation list again.
    public static void refreshConversations() {
        if (instance == null) {
            return;
        }
        Api.conversations().list(Net.to(list -> {
            if (instance != null) {
                Store.conversations(list);
            }
        }, (status, message) -> { }));
    }

    // ------------------------------------------------------------------
    // Sending

    /// Sends a text message; answers the local copy, already in the store.
    public static Message sendText(String conversationId, String text, Message replyTo) {
        Map<String, Object> payload = message(Message.TEXT, text, null, replyTo);
        return queueMessage(conversationId, payload);
    }

    /// Sends an attachment that [Media] has already uploaded.
    public static Message sendMedia(String conversationId, String kind, Map<String, Object> media,
            String caption) {
        return queueMessage(conversationId, message(kind, caption, media, null));
    }

    public static void react(String conversationId, Message target, String emoji) {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("t", REACT);
        payload.put("target", target.key());
        payload.put("emoji", emoji);
        if (instance != null) {
            if (emoji.length() == 0 || emoji.equals(target.reactions.get(instance.me))) {
                target.reactions.remove(instance.me);
                payload.put("emoji", "");
            } else {
                target.reactions.put(instance.me, emoji);
            }
            Store.save(conversationId);
            instance.enqueue(new Job(conversationId, KIND_MESSAGE, payload, null, null));
        }
    }

    public static void edit(String conversationId, Message target, String text) {
        if (instance == null || !target.mine(instance.me)) {
            return;
        }
        target.text = text;
        target.edited = true;
        Store.save(conversationId);
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("t", EDIT);
        payload.put("target", target.key());
        payload.put("text", text);
        instance.enqueue(new Job(conversationId, KIND_MESSAGE, payload, null, null));
    }

    public static void deleteForEveryone(String conversationId, Message target) {
        if (instance == null || !target.mine(instance.me)) {
            return;
        }
        erase(target);
        Store.save(conversationId);
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("t", DELETE);
        payload.put("target", target.key());
        instance.enqueue(new Job(conversationId, KIND_MESSAGE, payload, null, null));
    }

    /// Tells the others this account is typing, at most every few seconds.
    public static void typing(String conversationId) {
        if (instance == null || !Prefs.typingIndicators() || Store.bridged(conversationId)) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = instance.lastTypingSent.get(conversationId);
        if (last != null && now - last.longValue() < 4000) {
            return;
        }
        instance.lastTypingSent.put(conversationId, Long.valueOf(now));
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("t", TYPING);
        instance.enqueue(new Job(conversationId, KIND_TYPING, payload, null, null));
    }

    /// The conversation is on screen: everything in it is read. Sends read
    /// receipts when this account shares them.
    public static void read(String conversationId) {
        Store.markRead(conversationId);
        if (instance == null || Store.bridged(conversationId)) {
            return;
        }
        boolean share = Prefs.readReceipts();
        Map<String, List<String>> bySender = new LinkedHashMap<String, List<String>>();
        for (Message each : Store.messages(conversationId)) {
            if (!each.mine(instance.me) && each.status < Message.READ && !each.plain
                    && !Message.SYSTEM.equals(each.kind)) {
                each.status = Message.READ;
                List<String> ids = bySender.get(each.from);
                if (ids == null) {
                    ids = new ArrayList<String>();
                    bySender.put(each.from, ids);
                }
                ids.add(each.clientId);
            }
        }
        if (bySender.isEmpty()) {
            return;
        }
        Store.save(conversationId);
        if (share) {
            for (Map.Entry<String, List<String>> each : bySender.entrySet()) {
                instance.receipt(conversationId, each.getKey(), each.getValue(), Message.READ);
            }
        }
    }

    public static void listenTyping(TypingListener listener) {
        if (instance != null) {
            instance.typingListener = listener;
        }
    }

    /// Who is typing in a conversation right now.
    public static List<String> typists(String conversationId) {
        List<String> out = new ArrayList<String>();
        if (instance == null) {
            return out;
        }
        Map<String, Long> who = instance.typing.get(conversationId);
        long now = System.currentTimeMillis();
        if (who != null) {
            for (Map.Entry<String, Long> each : who.entrySet()) {
                if (each.getValue().longValue() > now) {
                    out.add(each.getKey());
                }
            }
        }
        return out;
    }

    /// Sends the message whose send failed again.
    public static void retry(String conversationId, Message failed) {
        if (instance == null || failed.status != Message.FAILED) {
            return;
        }
        failed.status = Message.SENDING;
        Store.save(conversationId);
        instance.enqueue(new Job(conversationId, KIND_MESSAGE, payloadOf(failed), null, failed));
    }

    private static Map<String, Object> message(String kind, String text, Map<String, Object> media,
            Message replyTo) {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("t", MSG);
        payload.put("cid", newId());
        payload.put("kind", kind);
        payload.put("text", text == null ? "" : text);
        if (media != null) {
            payload.put("media", media);
        }
        if (replyTo != null) {
            payload.put("reply", replyTo.key());
            payload.put("replyText", Store.preview(replyTo));
        }
        return payload;
    }

    private static Map<String, Object> payloadOf(Message message) {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("t", MSG);
        payload.put("cid", message.clientId);
        payload.put("kind", message.kind);
        payload.put("text", message.text);
        if (message.media != null) {
            payload.put("media", message.media);
        }
        if (message.replyTo.length() > 0) {
            payload.put("reply", message.replyTo);
            payload.put("replyText", message.replyText);
        }
        return payload;
    }

    private static Message queueMessage(String conversationId, Map<String, Object> payload) {
        if (instance == null) {
            return null;
        }
        Message local = new Message();
        local.clientId = Codec.str(payload.get("cid"));
        local.from = instance.me;
        local.kind = Codec.str(payload.get("kind"));
        local.text = Codec.str(payload.get("text"));
        Object media = payload.get("media");
        if (media instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attached = (Map<String, Object>) media;
            local.media = attached;
        }
        local.replyTo = Codec.str(payload.get("reply"));
        local.replyText = Codec.str(payload.get("replyText"));
        local.at = System.currentTimeMillis();
        local.status = Message.SENDING;
        local.plain = Store.bridged(conversationId);
        ConversationDto row = Store.conversation(conversationId);
        if (row != null && row.disappearingSeconds() > 0) {
            local.expiresAt = local.at + row.disappearingSeconds() * 1000L;
        }
        Store.add(conversationId, local, instance.me);
        instance.enqueue(new Job(conversationId, KIND_MESSAGE, payload, null, local));
        return local;
    }

    static String newId() {
        byte[] random = SecureRandom.bytes(12);
        return Codec.b64(random).replace('/', '_').replace('+', '-');
    }

    private void receipt(String conversationId, String to, List<String> ids, int state) {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("t", RECEIPT);
        payload.put("targets", ids);
        payload.put("state", Long.valueOf(state));
        enqueue(new Job(conversationId, KIND_RECEIPT, payload, to, null));
    }

    /// One send waiting its turn.
    private static final class Job {
        final String conversationId;
        final String kind;
        final Map<String, Object> payload;
        /// Only this account's devices get it (a receipt); null for all.
        final String onlyTo;
        final Message local;
        int attempts;

        Job(String conversationId, String kind, Map<String, Object> payload, String onlyTo,
                Message local) {
            this.conversationId = conversationId;
            this.kind = kind;
            this.payload = payload;
            this.onlyTo = onlyTo;
            this.local = local;
        }
    }

    private void enqueue(Job job) {
        queue.add(job);
        next();
    }

    private void next() {
        if (busy || queue.isEmpty() || instance != this) {
            return;
        }
        busy = true;
        send(queue.get(0));
    }

    private void done(Job job, int status) {
        queue.remove(job);
        if (job.local != null && job.local.status < status) {
            job.local.status = status;
            Store.save(job.conversationId);
        } else if (job.local != null && status == Message.FAILED) {
            job.local.status = status;
            Store.save(job.conversationId);
        }
        busy = false;
        next();
    }

    private void send(final Job job) {
        if (Store.bridged(job.conversationId)) {
            if (!KIND_MESSAGE.equals(job.kind) || !MSG.equals(job.payload.get("t"))) {
                done(job, Message.SENT);
                return;
            }
            post(job, new ArrayList<EnvelopeCopyDto>(), Codec.str(job.payload.get("text")));
            return;
        }
        List<DeviceAddressDto> known = devices.get(job.conversationId);
        if (known == null) {
            Api.conversations().devices(job.conversationId, Net.to(list -> {
                devices.put(job.conversationId, list);
                send(job);
            }, (status, message) -> done(job, Message.FAILED)));
            return;
        }
        // Sessions first: fetch the bundle of the first device that has none,
        // then come back here, until every target has one.
        for (DeviceAddressDto each : targets(job, known)) {
            if (!Sessions.has(each.username(), each.deviceId())) {
                Api.devices().bundle(each.username(), each.deviceId(), Net.to(bundle -> {
                    try {
                        Sessions.start(bundle);
                    } catch (CryptoException forged) {
                        // A bundle whose signature does not hold is not a
                        // device to talk to; nothing is sent rather than
                        // sending to a key the server made up.
                        Log.e(forged);
                        done(job, Message.FAILED);
                        return;
                    }
                    send(job);
                }, (status, message) -> {
                    devices.remove(job.conversationId);
                    retry(job);
                }));
                return;
            }
        }
        byte[] plaintext = Codec.utf8(Codec.json(job.payload));
        List<EnvelopeCopyDto> copies = new ArrayList<EnvelopeCopyDto>();
        for (DeviceAddressDto each : targets(job, known)) {
            String[] sealed = Sessions.encrypt(each.username(), each.deviceId(), plaintext);
            copies.add(new EnvelopeCopyDto(each.username(), each.deviceId(), sealed[0], sealed[1]));
        }
        if (copies.isEmpty() && !KIND_MESSAGE.equals(job.kind)) {
            done(job, Message.SENT);
            return;
        }
        post(job, copies, "");
    }

    private List<DeviceAddressDto> targets(Job job, List<DeviceAddressDto> known) {
        List<DeviceAddressDto> out = new ArrayList<DeviceAddressDto>();
        String mine = Identity.deviceId();
        for (DeviceAddressDto each : known) {
            if (each.deviceId().equals(mine)) {
                continue;
            }
            if (job.onlyTo != null && !job.onlyTo.equals(each.username())) {
                continue;
            }
            out.add(each);
        }
        return out;
    }

    private void post(final Job job, List<EnvelopeCopyDto> copies, String plaintext) {
        String clientId = job.local != null ? job.local.clientId : newId();
        OutgoingDto out = new OutgoingDto(job.conversationId, Identity.deviceId(), clientId,
                job.kind, copies, plaintext);
        Api.messages().send(out, Net.to((SendResultDto result) -> {
            boolean incomplete = (result.missing() != null && !result.missing().isEmpty())
                    || (result.stale() != null && !result.stale().isEmpty());
            if (incomplete) {
                // A device joined or left since the list was fetched: fetch it
                // again and encrypt for the devices there are now.
                devices.remove(job.conversationId);
                retry(job);
                return;
            }
            done(job, Message.SENT);
        }, (status, message) -> {
            if (status == 0 && job.attempts < 5 && job.local != null) {
                // Offline: try again in a while; the message waits as sending.
                job.attempts++;
                busy = false;
                CN.setTimeout(3000 * job.attempts, () -> CN.callSerially(this::next));
                return;
            }
            done(job, Message.FAILED);
        }));
    }

    private void retry(Job job) {
        if (++job.attempts > 3) {
            done(job, Message.FAILED);
            return;
        }
        send(job);
    }

    // ------------------------------------------------------------------
    // Receiving

    /// Fetches everything waiting for this device. Called on start, when the
    /// live channel connects and when a push says there is something.
    public static void sync() {
        if (instance != null) {
            instance.fetch();
        }
    }

    private void fetch() {
        if (syncing) {
            syncAgain = true;
            return;
        }
        syncing = true;
        syncAgain = false;
        Api.messages().sync(Identity.deviceId(), 100, Net.to(batch -> {
            syncing = false;
            if (instance != this) {
                return;
            }
            for (EnvelopeDto each : batch.envelopes()) {
                receive(each.id(), each.conversationId(), each.sender(), each.senderDevice(),
                        each.kind(), each.header(), each.ciphertext(), each.sentAt(),
                        each.expiresAt());
            }
            flushAcks(() -> {
                if (batch.more() || syncAgain) {
                    fetch();
                }
            });
        }, (status, message) -> {
            syncing = false;
            if (status == 404 || status == 403) {
                // This device was unlinked from another one. Its keys are of
                // no use any more: start again as a new device.
                Identity.registered("");
                Sessions.clear();
                if (instance == this) {
                    registerAgain();
                }
            }
        }));
    }

    @Override
    public void envelope(Map<String, Object> frame) {
        receive(Codec.str(frame.get("id")), Codec.str(frame.get("conversationId")),
                Codec.str(frame.get("sender")), Codec.str(frame.get("senderDevice")),
                Codec.str(frame.get("kind")), Codec.str(frame.get("header")),
                Codec.str(frame.get("ciphertext")), Codec.num(frame.get("sentAt")),
                Codec.num(frame.get("expiresAt")));
        if (ackTimer == null) {
            ackTimer = CN.setTimeout(400, () -> CN.callSerially(() -> {
                ackTimer = null;
                flushAcks(null);
            }));
        }
    }

    @Override
    public void conversationChanged(String id) {
        devices.remove(id);
        Api.conversations().get(id, Net.to(row -> {
            if (instance == this) {
                Store.conversation(row, true);
            }
        }, (status, message) -> refreshConversations()));
    }

    @Override
    public void call(Map<String, Object> frame) {
        Calls.frame(frame);
    }

    @Override
    public void connected() {
        sync();
    }

    private void flushAcks(final Runnable then) {
        if (toAck.isEmpty()) {
            if (then != null) {
                then.run();
            }
            return;
        }
        List<String> ids = new ArrayList<String>(toAck);
        toAck.clear();
        Api.messages().ack(Identity.deviceId(), new AckDto(ids), Net.to(ok -> {
            if (then != null) {
                then.run();
            }
        }, (status, message) -> {
            if (then != null) {
                then.run();
            }
        }));
    }

    private void receive(String id, String conversationId, String sender, String senderDevice,
            String kind, String header, String ciphertext, long sentAt, long expiresAt) {
        if (id.length() == 0) {
            return;
        }
        toAck.add(id);
        if (!processed.add(id)) {
            return;
        }
        while (processed.size() > 2000) {
            processed.remove(processed.iterator().next());
        }
        if (Store.conversation(conversationId) == null) {
            refreshConversations();
        }
        byte[] body = Codec.unb64(ciphertext);
        if (body == null) {
            return;
        }
        if (header.length() == 0) {
            plain(id, conversationId, sender, kind, Codec.text(body), sentAt, expiresAt);
            return;
        }
        Map<String, Object> payload;
        try {
            payload = Codec.parse(Codec.text(Sessions.decrypt(sender, senderDevice, header, body)));
        } catch (CryptoException | IllegalArgumentException undecryptable) {
            Log.p("Could not decrypt a message from " + sender + ": " + undecryptable.getMessage());
            if (KIND_MESSAGE.equals(kind)) {
                Message note = new Message();
                note.clientId = id;
                note.from = "";
                note.kind = Message.SYSTEM;
                note.text = "A message from " + sender + " could not be decrypted.";
                note.at = sentAt;
                Store.add(conversationId, note, me);
            }
            return;
        }
        if (payload == null) {
            return;
        }
        topUpPreKeysLater();
        apply(conversationId, sender, payload, sentAt, expiresAt);
    }

    private void plain(String id, String conversationId, String sender, String kind, String text,
            long sentAt, long expiresAt) {
        Message message = new Message();
        message.clientId = id;
        message.from = sender == null ? "" : sender;
        message.kind = "SYSTEM".equals(kind) ? Message.SYSTEM : Message.TEXT;
        message.text = text;
        message.at = sentAt;
        message.expiresAt = expiresAt;
        message.plain = true;
        if (sender != null && sender.equals(me)) {
            // This account's own message to a bridged chat, from another of
            // its devices: the server relays it in the clear like any other.
            message.status = Message.SENT;
        }
        if (Store.add(conversationId, message, me) && "SYSTEM".equals(kind)) {
            // A notice is about the conversation itself: who is in it, the timer.
            conversationChanged(conversationId);
        }
    }

    private void apply(String conversationId, String sender, Map<String, Object> payload,
            long sentAt, long expiresAt) {
        String type = Codec.str(payload.get("t"));
        if (MSG.equals(type)) {
            Message message = new Message();
            message.clientId = Codec.str(payload.get("cid"));
            message.from = sender;
            message.kind = Codec.str(payload.get("kind"));
            message.text = Codec.str(payload.get("text"));
            Object media = payload.get("media");
            if (media instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> attached = (Map<String, Object>) media;
                message.media = attached;
            }
            message.replyTo = Codec.str(payload.get("reply"));
            message.replyText = Codec.str(payload.get("replyText"));
            message.at = sentAt;
            message.expiresAt = expiresAt;
            message.status = sender.equals(me) ? Message.SENT : Message.DELIVERED;
            if (Store.add(conversationId, message, me) && !sender.equals(me)) {
                List<String> ids = new ArrayList<String>();
                ids.add(message.clientId);
                receipt(conversationId, sender, ids, Message.DELIVERED);
            }
        } else if (REACT.equals(type)) {
            Message target = Store.find(conversationId, Codec.str(payload.get("target")));
            if (target != null) {
                String emoji = Codec.str(payload.get("emoji"));
                if (emoji.length() == 0) {
                    target.reactions.remove(sender);
                } else {
                    target.reactions.put(sender, emoji);
                }
                Store.save(conversationId);
            }
        } else if (EDIT.equals(type) || DELETE.equals(type)) {
            Message target = Store.find(conversationId, Codec.str(payload.get("target")));
            // Only the author edits or deletes, whatever a payload claims.
            if (target != null && target.from.equals(sender)) {
                if (EDIT.equals(type)) {
                    target.text = Codec.str(payload.get("text"));
                    target.edited = true;
                } else {
                    erase(target);
                }
                Store.save(conversationId);
            }
        } else if (RECEIPT.equals(type)) {
            int state = (int) Codec.num(payload.get("state"));
            Object targets = payload.get("targets");
            if (targets instanceof List) {
                boolean changed = false;
                for (Object each : (List<?>) targets) {
                    Message target = Store.find(conversationId, me + "/" + Codec.str(each));
                    if (target != null && target.status < state && state <= Message.READ) {
                        target.status = state;
                        changed = true;
                    }
                }
                if (changed) {
                    Store.save(conversationId);
                }
            }
        } else if (TYPING.equals(type) && !sender.equals(me)) {
            Map<String, Long> who = typing.get(conversationId);
            if (who == null) {
                who = new HashMap<String, Long>();
                typing.put(conversationId, who);
            }
            who.put(sender, Long.valueOf(System.currentTimeMillis() + 6000));
            if (typingListener != null) {
                typingListener.typing(conversationId);
                final TypingListener told = typingListener;
                CN.setTimeout(6500, () -> CN.callSerially(() -> told.typing(conversationId)));
            }
        }
    }

    private static void erase(Message target) {
        target.deleted = true;
        target.text = "";
        target.media = null;
        target.reactions.clear();
    }

    private boolean topUpPending;

    private void topUpPreKeysLater() {
        if (topUpPending) {
            return;
        }
        topUpPending = true;
        CN.setTimeout(5000, () -> CN.callSerially(() -> {
            topUpPending = false;
            if (instance == this) {
                topUpPreKeys();
            }
        }));
    }

    private void topUpPreKeys() {
        final String device = Identity.deviceId();
        Api.devices().preKeyCount(device, Net.to(count -> {
            if (instance == this && count.remaining() < Identity.LOW) {
                Api.devices().uploadPreKeys(device, Identity.topUp(),
                        Net.to(after -> { }, (status, message) -> { }));
            }
        }, (status, message) -> { }));
    }
}
