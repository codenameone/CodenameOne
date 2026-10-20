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
package com.codename1.webrtc;

import com.codename1.util.AsyncResource;

/// One end of a call: the media this device sends, what it receives, and
/// the network path between the two.
///
/// The sequence is the standard one. The caller adds its tracks, creates an
/// offer, sets it as its local description and sends it; the callee sets it
/// as its remote description, adds its own tracks, creates an answer, sets
/// that locally and sends it back; the caller sets it as remote. Meanwhile
/// both sides send each other every candidate their listener is given.
/// Carrying the descriptions and candidates is the app's job -- over its own
/// server, a WebSocket, anything -- and nothing here sends them.
///
/// ```java
/// PeerConnection pc = WebRtc.createPeerConnection(config, listener);
/// pc.addTrack(media.getAudio());
/// pc.createOffer().ready(offer -> pc.setLocalDescription(offer)
///         .ready(done -> signalling.send(offer)));
/// ```
public final class PeerConnection {
    /// [#getState()] before the two sides found each other.
    public static final String NEW = "new";
    /// [#getState()] while a path is being tried.
    public static final String CONNECTING = "connecting";
    /// [#getState()] while media flows.
    public static final String CONNECTED = "connected";
    /// [#getState()] after the path was lost; it may come back by itself.
    public static final String DISCONNECTED = "disconnected";
    /// [#getState()] when no path could be found or it was lost for good.
    public static final String FAILED = "failed";
    /// [#getState()] after [#close()].
    public static final String CLOSED = "closed";

    /// What a peer connection tells the app as the call proceeds. Every
    /// method is called on the EDT.
    public interface Listener {
        /// A path to this device was found; send it to the other side.
        void iceCandidate(PeerConnection connection, IceCandidate candidate);

        /// The connection moved to another of the states named on
        /// [PeerConnection].
        void stateChanged(PeerConnection connection, String state);

        /// The other side's audio or video arrived.
        void remoteTrack(PeerConnection connection, MediaTrack track);
    }

    private final int handle;
    private final Listener listener;
    private String state = NEW;

    PeerConnection(int handle, Listener listener) {
        this.handle = handle;
        this.listener = listener;
    }

    int handle() {
        return handle;
    }

    Listener listener() {
        return listener;
    }

    void state(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    /// Sends a local track to the other side. Add every track before
    /// creating the offer or the answer.
    public void addTrack(MediaTrack track) {
        if (track == null || !track.isLocal()) {
            throw new IllegalArgumentException("Only a local track can be sent");
        }
        if (!CLOSED.equals(state)) {
            WebRtc.bridge().addTrack(handle, track.handle());
        }
    }

    /// Describes this side for the caller's half of the exchange.
    public AsyncResource<SessionDescription> createOffer() {
        AsyncResource<SessionDescription> out = new AsyncResource<SessionDescription>();
        WebRtc.bridge().createDescription(handle, WebRtc.request(out), false);
        return out;
    }

    /// Describes this side in answer to the remote offer already set.
    public AsyncResource<SessionDescription> createAnswer() {
        AsyncResource<SessionDescription> out = new AsyncResource<SessionDescription>();
        WebRtc.bridge().createDescription(handle, WebRtc.request(out), true);
        return out;
    }

    public AsyncResource<Boolean> setLocalDescription(SessionDescription description) {
        return set(false, description);
    }

    public AsyncResource<Boolean> setRemoteDescription(SessionDescription description) {
        return set(true, description);
    }

    private AsyncResource<Boolean> set(boolean remote, SessionDescription description) {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        WebRtc.bridge().setDescription(handle, WebRtc.request(out), remote,
                description.getType(), description.getSdp());
        return out;
    }

    /// Tries a path the other side found. A candidate that arrives before
    /// the remote description is set is held by the port until it is.
    public void addIceCandidate(IceCandidate candidate) {
        if (!CLOSED.equals(state)) {
            WebRtc.bridge().addIceCandidate(handle, candidate.getSdpMid(),
                    candidate.getSdpMLineIndex(), candidate.getCandidate());
        }
    }

    /// Ends the connection. Local tracks stay open: release them separately.
    public void close() {
        if (CLOSED.equals(state)) {
            return;
        }
        state = CLOSED;
        WebRtc.forget(this);
        WebRtc.bridge().closePeerConnection(handle);
    }
}
