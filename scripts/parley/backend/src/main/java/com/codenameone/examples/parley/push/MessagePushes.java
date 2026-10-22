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
package com.codenameone.examples.parley.push;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.push.PushNotification;
import com.codename1.backend.push.PushTarget;
import com.codenameone.examples.parley.domain.Preference;
import com.codenameone.examples.parley.repository.PreferenceRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The pushes Parley sends, and what they are allowed to say.
///
/// A message push says nothing: not who wrote, not in which chat, not a word
/// of the text. Apple and Google carry every push, and the point of encrypting
/// a message is that nobody between the two phones learns anything from it. The
/// push only wakes the app, which fetches its envelopes over its own
/// connection, decrypts them, and shows a local notification with whatever the
/// person allowed.
///
/// A call push is the exception iOS forces: the system rings before any app
/// code runs, so the push has to carry the caller's name. That is the one
/// thing a push says, and only for a call.
@Component
public class MessagePushes {
    private final PushDevices devices;
    private final PushSender sender;
    private final PreferenceRepository preferences;

    public MessagePushes(PushDevices devices, PushSender sender,
            PreferenceRepository preferences) {
        this.devices = devices;
        this.sender = sender;
        this.preferences = preferences;
    }

    /// Wakes an account's devices for new envelopes, unless it turned these
    /// notifications off. Repeated wake-ups collapse into one on the device.
    public void wake(String username, boolean group) {
        Preference wanted = preferences.findById(username);
        if (wanted != null && !(group ? wanted.isNotifyGroups() : wanted.isNotifyMessages())) {
            return;
        }
        List<PushTarget> targets = devices.targets(username);
        if (targets.isEmpty()) {
            return;
        }
        sender.send(targets, PushNotification.builder()
                .title("Parley")
                .body("You have a new message")
                .collapseKey("parley-sync")
                .data("type", "sync")
                .build(), null);
    }

    /// Rings an account for a call: a VoIP push to iOS devices, and a
    /// high-priority data push to the rest, which the app turns into a ringing
    /// call itself.
    public void ring(String username, String callId, String uuid, String callerName,
            String callerHandle, boolean video) {
        Preference wanted = preferences.findById(username);
        if (wanted != null && !wanted.isNotifyCalls()) {
            return;
        }
        Map<String, Object> call = new HashMap<String, Object>();
        call.put("uuid", uuid);
        call.put("handle", callerHandle);
        call.put("handleType", "generic");
        call.put("displayName", callerName);
        call.put("video", Boolean.valueOf(video));
        call.put("ttl", Integer.valueOf(45));
        call.put("data", callId);
        List<PushTarget> voip = devices.voipTargets(username);
        if (!voip.isEmpty()) {
            Map<String, Object> apns = new HashMap<String, Object>();
            apns.put("pushType", "voip");
            sender.send(voip, PushNotification.builder()
                    .data("cn1call", call)
                    .platform("apns", apns)
                    .build(), null);
        }
        List<PushTarget> others = devices.targets(username);
        if (!others.isEmpty()) {
            sender.send(others, PushNotification.builder()
                    .title(callerName)
                    .body(video ? "Incoming video call" : "Incoming call")
                    .collapseKey("parley-call-" + callId)
                    .ttlSeconds(45)
                    .data("type", "call")
                    .data("callId", callId)
                    .build(), null);
        }
    }

    /// Takes back the ring of a call that ended before it was answered.
    public void cancelRing(String username, String uuid) {
        List<PushTarget> voip = devices.voipTargets(username);
        if (voip.isEmpty()) {
            return;
        }
        Map<String, Object> call = new HashMap<String, Object>();
        call.put("uuid", uuid);
        call.put("handle", "parley");
        call.put("cancel", Boolean.TRUE);
        Map<String, Object> apns = new HashMap<String, Object>();
        apns.put("pushType", "voip");
        sender.send(voip, PushNotification.builder().data("cn1call", call)
                .platform("apns", apns).build(), null);
    }
}
