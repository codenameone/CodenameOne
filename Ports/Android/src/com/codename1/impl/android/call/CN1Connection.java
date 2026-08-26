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
package com.codename1.impl.android.call;

import android.os.Build;
import android.telecom.CallAudioState;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.VideoProfile;

import com.codename1.call.CallEndReason;
import com.codename1.call.session.CallAudioRoute;
import com.codename1.call.session.Calls;

/// One call, as Telecom sees it.
///
/// Telecom owns this object: it is created by [CN1ConnectionService] and
/// destroyed when the call ends, and every method here is the **system**
/// asking the application to do something rather than the other way round.
/// Each one turns into a `Calls.deliver...` call carrying an action token the
/// facade answers.
///
/// #### Android has no audio-session handoff, so this synthesizes one
///
/// iOS activates the audio session and tells the app; Android just expects
/// the app to have started. Rather than let application code branch on the
/// platform -- which would mean the iOS path was the only one tested -- this
/// fires `audioSessionActivated` as soon as the connection goes active, so
/// "start media when the audio session arrives" is correct everywhere.
public class CN1Connection extends Connection {

    private final String callId;
    private final CN1ConnectionService service;
    private boolean audioAnnounced;
    private String callerName;
    private boolean video;

    CN1Connection(CN1ConnectionService service, String callId) {
        this.service = service;
        this.callId = callId;
        setConnectionProperties(PROPERTY_SELF_MANAGED);
        setAudioModeIsVoip(true);
    }

    /// Puts the call into Telecom's video state, if it is a video call.
    ///
    /// Both halves are needed and each is silent when it is missing: the
    /// capabilities say the connection CAN do video, and the video state says
    /// this call IS doing it. Without them Telecom treated every reported
    /// call as audio-only while the bridge advertised CAPABILITY_VIDEO, and
    /// onAnswer(videoState) had nothing to honour.
    void setVideo(boolean video) {
        this.video = video;
        if (!video) {
            return;
        }
        setConnectionCapabilities(getConnectionCapabilities()
                | CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL
                | CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL);
        setVideoState(VideoProfile.STATE_BIDIRECTIONAL);
    }

    /// The name to ring with, from the report that created this call.
    void setRingingName(String name) {
        this.callerName = name;
    }

    /// The identifier this call is known by everywhere else.
    public String getCallId() {
        return callId;
    }

    @Override
    public void onShowIncomingCallUi() {
        // Telecom asking the app to present the call, which for a
        // self-managed account it never does itself.
        CN1CallNotifications.showIncoming(callId, callerName);
    }

    @Override
    public void onAnswer() {
        CN1CallNotifications.dismiss(callId);
        setActive();
        announceAudio();
        Calls.deliverAnswer(callId, service.nextActionToken(this, ACTION_ANSWER));
    }

    @Override
    public void onAnswer(int videoState) {
        // The state the user answered WITH, which can differ from the one the
        // call was reported with -- answering a video call with audio only is
        // an ordinary choice the system offers.
        if (video) {
            setVideoState(videoState);
        }
        onAnswer();
    }

    @Override
    public void onReject() {
        // The notification is NOT dismissed here. A listener that fails this
        // action is saying it could not reject the call, and Telecom then
        // leaves it ringing -- with the only answer surface already gone.
        // finish() takes it down once the rejection is actually carried out,
        // which is the same "apply on acknowledgement" rule the session state
        // follows.
        Calls.deliverEndRequest(callId,
                service.nextActionToken(this, ACTION_REJECT));
    }

    @Override
    public void onDisconnect() {
        // Same reasoning as onReject: a refused hang-up leaves the call up,
        // and finish() clears the notification when the end is carried out.
        // A call the user answered has none anyway.
        Calls.deliverEndRequest(callId,
                service.nextActionToken(this, ACTION_DISCONNECT));
    }

    @Override
    public void onAbort() {
        onDisconnect();
    }

    @Override
    public void onHold() {
        setOnHold();
        Calls.deliverHold(callId, true,
                service.nextActionToken(this, ACTION_HOLD));
    }

    @Override
    public void onUnhold() {
        setActive();
        Calls.deliverHold(callId, false,
                service.nextActionToken(this, ACTION_UNHOLD));
    }

    @Override
    public void onPlayDtmfTone(char c) {
        Calls.deliverDtmf(callId, String.valueOf(c),
                service.nextActionToken(this, ACTION_DTMF));
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState state) {
        if (state == null) {
            return;
        }
        // deliverMuteChanged, not deliverMute: Telecom is REPORTING a mute it
        // has already applied and takes no instruction about it from a
        // self-managed app, so there is nothing here for a listener to
        // refuse. Delivered as a refusable action, a fail() left the system
        // showing muted, the app still transmitting and isMuted() answering
        // for neither.
        Calls.deliverMuteChanged(callId, state.isMuted());
        CN1ConnectionService.setRoute(routeOf(state.getRoute()));
    }

    @Override
    public void onStateChanged(int state) {
        if (state == STATE_ACTIVE) {
            announceAudio();
        }
    }

    /// Tears down a call whose answer the app could not carry out.
    ///
    /// Telecom has no "this action failed" channel, so the only honest report
    /// is to end the call -- but onAnswer has already moved Telecom to ACTIVE
    /// and fired the synthesized audioSessionActivated, as Telecom's own API
    /// requires. Destroying the connection and stopping there left the app's
    /// media running against a call that no longer existed and the session in
    /// Calls.getSessions() for the life of the process, because nothing ever
    /// told the facade.
    void failAnswer() {
        CN1CallNotifications.dismiss(callId);
        setDisconnected(new DisconnectCause(DisconnectCause.ERROR));
        if (audioAnnounced) {
            Calls.deliverAudioDeactivated(callId);
            audioAnnounced = false;
        }
        // Only on this path. Where the app itself asked for the end, the
        // facade has already moved the session and would fire callEnded a
        // second time.
        Calls.deliverCallEnded(callId, CallEndReason.FAILED.ordinal());
        destroy();
    }

    /// Ends the call and removes it from Telecom.
    void finish(CallEndReason reason) {
        CN1CallNotifications.dismiss(callId);
        setDisconnected(new DisconnectCause(causeOf(reason)));
        if (audioAnnounced) {
            Calls.deliverAudioDeactivated(callId);
            audioAnnounced = false;
        }
        destroy();
    }

    /// Fires the synthesized audio-session activation, exactly once.
    private void announceAudio() {
        if (audioAnnounced) {
            return;
        }
        audioAnnounced = true;
        Calls.deliverAudioActivated(callId, CN1ConnectionService.getRoute());
    }

    private static int routeOf(int androidRoute) {
        if ((androidRoute & CallAudioState.ROUTE_BLUETOOTH) != 0) {
            return CallAudioRoute.BLUETOOTH.ordinal();
        }
        if ((androidRoute & CallAudioState.ROUTE_WIRED_HEADSET) != 0) {
            return CallAudioRoute.WIRED_HEADSET.ordinal();
        }
        if ((androidRoute & CallAudioState.ROUTE_SPEAKER) != 0) {
            return CallAudioRoute.SPEAKER.ordinal();
        }
        if ((androidRoute & CallAudioState.ROUTE_EARPIECE) != 0) {
            return CallAudioRoute.EARPIECE.ordinal();
        }
        return CallAudioRoute.UNKNOWN.ordinal();
    }

    /// Maps a portable reason onto the cause Telecom writes in the call log.
    ///
    /// The value is user-visible -- a call logged as REJECTED shows
    /// differently from one logged as MISSED -- so this is not cosmetic.
    private static int causeOf(CallEndReason reason) {
        if (reason == null) {
            return DisconnectCause.LOCAL;
        }
        switch (reason) {
            case REMOTE_ENDED:
                return DisconnectCause.REMOTE;
            case LOCAL_ENDED:
                return DisconnectCause.LOCAL;
            case UNANSWERED:
                return DisconnectCause.MISSED;
            case BUSY:
                return DisconnectCause.BUSY;
            case FILTERED:
                return DisconnectCause.REJECTED;
            default:
                return DisconnectCause.ERROR;
        }
    }

    static final int ACTION_ANSWER = 1;
    static final int ACTION_REJECT = 2;
    static final int ACTION_DISCONNECT = 3;
    static final int ACTION_HOLD = 4;
    static final int ACTION_UNHOLD = 5;
    static final int ACTION_DTMF = 6;
    /// Unused since the mute state became a fact rather than a request --
    /// see onCallAudioStateChanged. Kept so the neighbouring values, which
    /// index nothing but are read in logs, do not shift.
    static final int ACTION_MUTE = 7;
}
