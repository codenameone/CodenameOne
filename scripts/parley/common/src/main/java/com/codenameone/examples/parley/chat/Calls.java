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
package com.codenameone.examples.parley.chat;

import com.codename1.call.CallEndReason;
import com.codename1.call.CallHandle;
import com.codename1.call.session.CallAction;
import com.codename1.call.session.CallActionAdapter;
import com.codename1.call.session.CallConfiguration;
import com.codename1.call.session.CallSession;
import com.codename1.call.voip.PushedCall;
import com.codename1.call.voip.VoipPush;
import com.codename1.call.voip.VoipPushListener;
import com.codename1.io.Log;
import com.codename1.ui.CN;
import com.codename1.util.AsyncResource;
import com.codename1.webrtc.IceCandidate;
import com.codename1.webrtc.IceServer;
import com.codename1.webrtc.LocalMedia;
import com.codename1.webrtc.MediaTrack;
import com.codename1.webrtc.PeerConnection;
import com.codename1.webrtc.PeerConnectionConfig;
import com.codename1.webrtc.SessionDescription;
import com.codename1.webrtc.WebRtc;
import com.codenameone.examples.parley.Telemetry;
import com.codenameone.examples.parley.api.CallSignalDto;
import com.codenameone.examples.parley.api.CallStartDto;
import com.codenameone.examples.parley.api.IceConfigDto;
import com.codenameone.examples.parley.api.IceServerDto;
import com.codenameone.examples.parley.api.PushDeviceDto;
import com.codenameone.examples.parley.crypto.Identity;
import com.codenameone.examples.parley.net.Api;
import com.codenameone.examples.parley.net.Net;
import com.codenameone.examples.parley.net.Session;
import com.codenameone.examples.parley.store.Store;
import com.codenameone.examples.parley.ui.CallScreen;
import com.codenameone.examples.parley.ui.Lang;
import com.codenameone.examples.parley.ui.Ui;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Voice and video calls: one at a time, between two people.
///
/// The server rings and relays; the media goes between the two phones over
/// WebRTC. The caller starts the call, opens its camera and microphone and
/// sends an OFFER at once, so the callee's phone already holds it when the
/// person answers. Answering sends ACCEPT, then the ANSWER; both sides send
/// every ICE candidate they find, and a candidate that comes before the
/// description it belongs to waits for it. Either side ends with END; the
/// callee can DECLINE instead of answering.
///
/// Where the platform has a system call screen -- CallKit, Android's
/// ConnectionService -- the call is reported to it as well, so it rings on
/// the lock screen and shows in the phone's call log, and the system's own
/// answer and hang-up buttons drive the same steps as the app's. A VoIP push
/// rings a phone the app is not running on.
public final class Calls {
    static final String OFFER = "OFFER";
    static final String ANSWER = "ANSWER";
    static final String ICE = "ICE";
    static final String ACCEPT = "ACCEPT";
    static final String DECLINE = "DECLINE";
    static final String END = "END";

    /// What the call screen shows.
    public static final String RINGING = "ringing";
    public static final String CALLING = "calling";
    public static final String CONNECTING = "connecting";
    public static final String CONNECTED = "connected";
    public static final String ENDED = "ended";

    private static Call current;
    private static boolean systemReady;

    private Calls() {
    }

    /// One call, from either side.
    public static final class Call {
        final String id;
        final String uuid;
        final String conversationId;
        final String peerName;
        final boolean video;
        final boolean outgoing;
        String state;
        LocalMedia media;
        PeerConnection connection;
        SessionDescription offer;
        final List<IceCandidate> early = new ArrayList<IceCandidate>();
        boolean remoteSet;
        /// The offer was taken. The server replays it to a callee that accepts,
        /// for a phone that was not connected when it was sent, so one that was
        /// sees it twice.
        boolean offerTaken;
        CallSession system;
        boolean muted;
        boolean speaker;
        boolean cameraOff;
        MediaTrack remoteVideo;
        long answeredAt;

        Call(String id, String uuid, String conversationId, String peerName, boolean video,
                boolean outgoing) {
            this.id = id;
            this.uuid = uuid;
            this.conversationId = conversationId;
            this.peerName = peerName;
            this.video = video;
            this.outgoing = outgoing;
            this.state = outgoing ? CALLING : RINGING;
        }

        public String getPeerName() {
            return peerName;
        }

        public boolean isVideo() {
            return video;
        }

        public boolean isOutgoing() {
            return outgoing;
        }

        public String getState() {
            return state;
        }

        public boolean isMuted() {
            return muted;
        }

        public boolean isSpeaker() {
            return speaker;
        }

        public boolean isCameraOff() {
            return cameraOff;
        }

        public MediaTrack getLocalVideo() {
            return media == null ? null : media.getVideo();
        }

        public MediaTrack getRemoteVideo() {
            return remoteVideo;
        }
    }

    /// The call in progress, or null.
    public static Call current() {
        return current;
    }

    /// Whether this device can carry a call at all.
    public static boolean supported() {
        return WebRtc.isSupported();
    }

    // ------------------------------------------------------------------
    // Setting up

    /// Hooks the system call screen and VoIP pushes, where there are any.
    /// Called once a user is signed in.
    public static void signedIn() {
        if (!systemReady && com.codename1.call.session.Calls.isSupported()) {
            systemReady = true;
            com.codename1.call.session.Calls.configure(new CallConfiguration()
                    .displayName("Parley").videoSupported(true).includesCallsInRecents(true));
            com.codename1.call.session.Calls.addActionListener(new CallActionAdapter() {
                @Override
                public void answerRequested(String callId, CallAction action) {
                    Call call = byUuid(callId);
                    if (call != null && !call.outgoing && RINGING.equals(call.state)) {
                        answer();
                    }
                }

                @Override
                public void endRequested(String callId, CallAction action) {
                    Call call = byUuid(callId);
                    if (call != null) {
                        call.system = null;
                        if (RINGING.equals(call.state) && !call.outgoing) {
                            decline();
                        } else {
                            hangUp();
                        }
                    }
                }

                @Override
                public void muteRequested(String callId, boolean muted, CallAction action) {
                    Call call = byUuid(callId);
                    if (call != null && call.muted != muted) {
                        toggleMute();
                    }
                }

                @Override
                public void providerReset() {
                    if (current != null) {
                        current.system = null;
                        hangUp();
                    }
                }
            });
        }
        if (VoipPush.isSupported()) {
            VoipPush.setListener(new VoipPushListener() {
                @Override
                public void callReceived(PushedCall pushed) {
                    pushedCall(pushed);
                }

                @Override
                public void tokenChanged(String token) {
                    registerVoip(token);
                }
            });
            VoipPush.register().ready(Calls::registerVoip);
        }
    }

    /// The PushKit token registered for the account signed in, so signing
    /// out can take it back; null when none was registered.
    private static String voipToken;

    /// Removes this phone's PushKit token from the account before it signs
    /// out, then runs `then`. Left behind, it would keep ringing a phone
    /// nobody is signed in on, with the caller's name on its screen.
    public static void signingOut(Runnable then) {
        String token = voipToken;
        voipToken = null;
        if (token == null || Session.user() == null) {
            then.run();
            return;
        }
        Api.account().unregisterPush(new PushDeviceDto("apns", token, "ios-voip"),
                Net.to(gone -> then.run(), (status, message) -> then.run()));
    }

    private static void registerVoip(String token) {
        if (token != null && token.length() > 0 && Session.user() != null) {
            voipToken = token;
            Api.account().registerPush(new PushDeviceDto("apns", token, "ios-voip"),
                    Net.to(saved -> { }, (status, message) -> Log.p("VoIP push: " + message)));
        }
    }

    /// A phone woken by a VoIP push is already ringing on the system screen.
    /// The push names the call; the live channel, connecting now, brings the
    /// offer.
    private static void pushedCall(PushedCall pushed) {
        CallSession session = pushed.getSession();
        if (pushed.isStale() || session == null) {
            return;
        }
        if (current != null) {
            if (current.uuid.equals(session.getCallId())) {
                current.system = session;
            }
            return;
        }
        String callId = pushed.getData() == null ? "" : pushed.getData();
        Call call = new Call(callId, session.getCallId(), "", session.getDisplayName(),
                false, false);
        call.system = session;
        current = call;
        Messenger.sync();
    }

    private static Call byUuid(String uuid) {
        return current != null && current.uuid.equalsIgnoreCase(uuid) ? current : null;
    }

    // ------------------------------------------------------------------
    // Placing a call

    /// Calls the other person in a direct conversation.
    public static void start(final String conversationId, final boolean video) {
        if (current != null) {
            CallScreen.show(current);
            return;
        }
        if (!supported()) {
            Ui.say(Lang.tr("Calls are not available on this device"));
            return;
        }
        Api.calls().start(new CallStartDto(conversationId, video), Net.to(row -> {
            if ("BUSY".equals(row.state())) {
                Ui.say(Lang.tr("They are on another call"));
                return;
            }
            String name = Store.conversation(conversationId) == null ? row.callee()
                    : Store.conversation(conversationId).title();
            Call call = new Call(row.id(), row.uuid(), conversationId, name, video, true);
            current = call;
            Telemetry.callStarted(video);
            report(call);
            CallScreen.show(call);
            connect(call, () -> call.connection.createOffer().ready(offer ->
                    call.connection.setLocalDescription(offer).ready(done -> {
                        send(call, OFFER, offer.getSdp());
                    }).except(why -> failed(call, why))).except(why -> failed(call, why)));
        }, Ui::fail));
    }

    private static void report(Call call) {
        if (!systemReady || call.system != null) {
            return;
        }
        CallHandle handle = CallHandle.generic(call.peerName);
        AsyncResource<CallSession> reported = call.outgoing
                ? com.codename1.call.session.Calls.reportOutgoing(call.uuid, handle,
                        call.peerName, call.video)
                : com.codename1.call.session.Calls.reportIncoming(call.uuid, handle,
                        call.peerName, call.video);
        reported.ready(session -> {
            if (current == call) {
                call.system = session;
                if (call.outgoing) {
                    session.reportStartedConnecting();
                }
            } else {
                session.end(CallEndReason.REMOTE_ENDED);
            }
        });
    }

    // ------------------------------------------------------------------
    // Answering

    /// Answers the call that is ringing.
    public static void answer() {
        final Call call = current;
        if (call == null || call.outgoing || !RINGING.equals(call.state)) {
            return;
        }
        state(call, CONNECTING);
        send(call, ACCEPT, "");
        connect(call, () -> {
            if (call.offer != null) {
                answerOffer(call);
            }
        });
    }

    private static void answerOffer(final Call call) {
        final SessionDescription offer = call.offer;
        call.offer = null;
        call.offerTaken = true;
        call.connection.setRemoteDescription(offer).ready(set -> {
            remoteSet(call);
            call.connection.createAnswer().ready(answer ->
                    call.connection.setLocalDescription(answer).ready(done ->
                            send(call, ANSWER, answer.getSdp())).except(why -> failed(call, why)))
                    .except(why -> failed(call, why));
        }).except(why -> failed(call, why));
    }

    /// Turns the ringing call down.
    public static void decline() {
        Call call = current;
        if (call == null) {
            return;
        }
        send(call, DECLINE, "");
        finish(call, CallEndReason.LOCAL_ENDED);
    }

    /// Ends the call, from either side, in any state.
    public static void hangUp() {
        Call call = current;
        if (call == null) {
            return;
        }
        send(call, END, "");
        finish(call, CallEndReason.LOCAL_ENDED);
    }

    // ------------------------------------------------------------------
    // During the call

    public static void toggleMute() {
        Call call = current;
        if (call == null) {
            return;
        }
        call.muted = !call.muted;
        if (call.media != null && call.media.getAudio() != null) {
            call.media.getAudio().setEnabled(!call.muted);
        }
        if (call.system != null) {
            call.system.setMuted(call.muted);
        }
        CallScreen.refresh(call);
    }

    public static void toggleSpeaker() {
        Call call = current;
        if (call == null) {
            return;
        }
        call.speaker = !call.speaker;
        WebRtc.setSpeakerphone(call.speaker);
        CallScreen.refresh(call);
    }

    public static void toggleCamera() {
        Call call = current;
        if (call == null || call.media == null || call.media.getVideo() == null) {
            return;
        }
        call.cameraOff = !call.cameraOff;
        call.media.getVideo().setEnabled(!call.cameraOff);
        CallScreen.refresh(call);
    }

    public static void flipCamera() {
        Call call = current;
        if (call != null && call.media != null && call.media.getVideo() != null) {
            call.media.getVideo().switchCamera();
        }
    }

    // ------------------------------------------------------------------
    // The media

    /// Opens the camera and microphone and the peer connection, then runs
    /// `then`.
    private static void connect(final Call call, final Runnable then) {
        WebRtc.openUserMedia(true, call.video, true).ready(media -> {
            if (current != call) {
                media.release();
                return;
            }
            call.media = media;
            CallScreen.refresh(call);
            Api.calls().iceServers(Net.to(ice -> open(call, ice, then),
                    (status, message) -> open(call, null, then)));
        }).except(failure -> {
            Ui.say(Lang.tr("Parley needs the microphone for calls"));
            hangUp();
        });
    }

    private static void open(final Call call, IceConfigDto ice, Runnable then) {
        if (current != call) {
            return;
        }
        PeerConnectionConfig config = new PeerConnectionConfig();
        if (ice != null && ice.servers() != null) {
            for (IceServerDto server : ice.servers()) {
                config.addIceServer(new IceServer(server.urls().toArray(new String[0]),
                        server.username(), server.credential()));
            }
        }
        call.connection = WebRtc.createPeerConnection(config, new PeerConnection.Listener() {
            @Override
            public void iceCandidate(PeerConnection connection, IceCandidate candidate) {
                if (current == call) {
                    send(call, ICE, (candidate.getSdpMid() == null ? "" : candidate.getSdpMid())
                            + "\t" + candidate.getSdpMLineIndex() + "\t" + candidate.getCandidate());
                }
            }

            @Override
            public void stateChanged(PeerConnection connection, String state) {
                if (current != call) {
                    return;
                }
                if (PeerConnection.CONNECTED.equals(state)) {
                    if (call.answeredAt == 0) {
                        call.answeredAt = System.currentTimeMillis();
                    }
                    state(call, CONNECTED);
                    if (call.system != null) {
                        call.system.reportConnected();
                    }
                } else if (PeerConnection.FAILED.equals(state)) {
                    Ui.say(Lang.tr("The call was lost"));
                    hangUp();
                }
            }

            @Override
            public void remoteTrack(PeerConnection connection, MediaTrack track) {
                if (current == call && track.isVideo()) {
                    call.remoteVideo = track;
                    CallScreen.refresh(call);
                }
            }
        });
        if (call.media.getAudio() != null) {
            call.connection.addTrack(call.media.getAudio());
        }
        if (call.media.getVideo() != null) {
            call.connection.addTrack(call.media.getVideo());
        }
        WebRtc.setSpeakerphone(call.video);
        call.speaker = call.video;
        then.run();
    }

    private static void remoteSet(Call call) {
        call.remoteSet = true;
        for (IceCandidate each : call.early) {
            call.connection.addIceCandidate(each);
        }
        call.early.clear();
    }

    /// A step of `call` failed. A peer connection answers late, so this can be
    /// a call that is already over while another one rings or runs: that
    /// one is left alone.
    private static void failed(Call call, Throwable why) {
        Log.p("Call failed: " + why);
        if (current != null && current == call) {
            Ui.say(Lang.tr("The call could not be connected"));
            hangUp();
        }
    }

    // ------------------------------------------------------------------
    // Signalling

    private static void send(Call call, String type, String payload) {
        if (call.id.length() == 0) {
            return;
        }
        Api.calls().signal(call.id, new CallSignalDto(call.id, type, payload, Identity.deviceId()),
                Net.to(row -> { }, (status, message) -> Log.p("Call signal " + type + ": " + message)));
    }

    /// A `{"type":"call"}` frame from the live channel.
    static void frame(Map<String, Object> frame) {
        String event = text(frame, "event");
        String id = text(frame, "id");
        if ("ring".equals(event)) {
            ring(frame, id);
            return;
        }
        Call call = current;
        if (call == null || !call.id.equals(id)) {
            return;
        }
        if ("answered".equals(event)) {
            // Another of this account's devices answered: stop ringing here,
            // or stop connecting, if this one answered too and lost.
            String by = text(frame, "answeredBy");
            if (!call.outgoing && (RINGING.equals(call.state)
                    || (by.length() > 0 && !by.equals(Identity.deviceId())))) {
                finish(call, CallEndReason.REMOTE_ENDED);
            }
            return;
        }
        String signal = text(frame, "signal");
        String payload = text(frame, "payload");
        String state = text(frame, "state");
        if (OFFER.equals(signal) && !call.outgoing) {
            if (call.offerTaken) {
                return;
            }
            call.offer = new SessionDescription(SessionDescription.OFFER, payload);
            if (call.connection != null) {
                answerOffer(call);
            }
        } else if (ANSWER.equals(signal) && call.outgoing && call.connection != null) {
            state(call, CONNECTING);
            call.connection.setRemoteDescription(new SessionDescription(
                    SessionDescription.ANSWER, payload)).ready(done -> remoteSet(call))
                    .except(why -> failed(call, why));
        } else if (ACCEPT.equals(signal) && call.outgoing) {
            state(call, CONNECTING);
        } else if (ICE.equals(signal)) {
            IceCandidate candidate = candidate(payload);
            if (candidate == null) {
                return;
            }
            if (call.connection != null && call.remoteSet) {
                call.connection.addIceCandidate(candidate);
            } else {
                call.early.add(candidate);
            }
        } else if (DECLINE.equals(signal) || END.equals(signal) || "ENDED".equals(state)
                || "MISSED".equals(state) || "DECLINED".equals(state)) {
            if (DECLINE.equals(signal) || "DECLINED".equals(state)) {
                Ui.say(Lang.tr("They declined the call"));
            }
            finish(call, CallEndReason.REMOTE_ENDED);
        }
    }

    private static void ring(Map<String, Object> frame, String id) {
        if (current != null) {
            // Only a call a VoIP push started is filled in: it knows its id
            // but not its conversation. A ring the server replays when the
            // socket reconnects names a call this phone already shows.
            if (current.conversationId.length() == 0
                    && (current.id.length() == 0 || current.id.equals(id))) {
                // Woken by a VoIP push, now with the details.
                Call pushed = new Call(id, text(frame, "uuid"), text(frame, "conversationId"),
                        text(frame, "callerName"), Boolean.TRUE.equals(frame.get("video"))
                                || "true".equals(String.valueOf(frame.get("video"))), false);
                pushed.system = current.system;
                current = pushed;
                CallScreen.show(pushed);
            }
            return;
        }
        if (!supported()) {
            return;
        }
        Call call = new Call(id, text(frame, "uuid"), text(frame, "conversationId"),
                text(frame, "callerName"), "true".equals(String.valueOf(frame.get("video"))),
                false);
        current = call;
        report(call);
        CallScreen.show(call);
    }

    private static IceCandidate candidate(String payload) {
        int first = payload.indexOf('\t');
        int second = first < 0 ? -1 : payload.indexOf('\t', first + 1);
        if (second < 0) {
            return null;
        }
        int index;
        try {
            index = Integer.parseInt(payload.substring(first + 1, second));
        } catch (NumberFormatException bad) {
            return null;
        }
        String mid = payload.substring(0, first);
        return new IceCandidate(mid.length() == 0 ? null : mid, index,
                payload.substring(second + 1));
    }

    private static void state(Call call, String state) {
        call.state = state;
        CallScreen.refresh(call);
    }

    private static void finish(Call call, CallEndReason reason) {
        if (current != call) {
            return;
        }
        current = null;
        call.state = ENDED;
        if (call.connection != null) {
            call.connection.close();
        }
        if (call.media != null) {
            call.media.release();
        }
        if (call.system != null) {
            if (reason == CallEndReason.LOCAL_ENDED) {
                call.system.end(reason);
            } else {
                call.system.reportEndedRemotely(reason);
            }
            call.system = null;
        }
        Telemetry.callEnded(call.answeredAt > 0, call.answeredAt == 0 ? 0
                : (System.currentTimeMillis() - call.answeredAt) / 1000);
        CallScreen.closed(call);
        CN.callSerially(Messenger::sync);
    }

    // Tested, never cast and caught: a failed cast does not throw on iOS.
    private static String text(Map<String, Object> json, String key) {
        Object value = json.get(key);
        return value instanceof String ? (String) value : "";
    }
}
