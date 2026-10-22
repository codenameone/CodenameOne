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
package com.codename1.webrtc.spi;

/// Internal service-provider interface a port implements to carry the
/// `com.codename1.webrtc` API onto its native WebRTC stack: Google's
/// `org.webrtc` on Android, the WebRTC framework on iOS, the browser's
/// `RTCPeerConnection` in the JavaScript port, and a loopback in the
/// simulator.
///
/// Application code never touches this interface; the public classes reach it
/// through `com.codename1.ui.Display#getWebRtcBridge()`, and a port that
/// returns `null` there is a port without WebRTC -- `WebRtc.isSupported()`
/// says so and nothing else needs a platform `if`.
///
/// #### Everything is ids, primitives and strings
///
/// As with `com.codename1.call.spi.CallBridge`, no method takes or returns a
/// framework type, so a port reached through ParparVM never constructs a
/// Java object from native code. Peer connections and tracks are integer
/// handles the port allocates; structured values cross as strings.
///
/// #### Every request answers exactly once
///
/// An operation that can fail takes a `requestId` from the caller and answers
/// it by calling exactly one of `WebRtc.deliverDescription`,
/// `WebRtc.deliverTracks` or `WebRtc.deliverResult`. A request that never
/// answers leaves an `AsyncResource` that never settles, which is worse than
/// one that fails. Unsolicited events -- a gathered candidate, a state
/// change, a remote track -- carry the peer handle instead. Every entry point
/// may be called from any thread; they marshal to the EDT themselves.
public interface WebRtcBridge {
    /// Creates a peer connection and answers its handle, or a negative number
    /// when it cannot be created.
    ///
    /// #### Parameters
    ///
    /// - `iceServers`: one server per line, each line tab-separated as
    ///   `urls` (comma-separated), `username`, `credential`
    ///
    /// - `relayOnly`: whether only relayed (TURN) candidates may be used, which
    ///   keeps the device's own addresses from the other side
    int createPeerConnection(String iceServers, boolean relayOnly);

    /// Starts the local camera and/or microphone. Answers with
    /// `WebRtc.deliverTracks`: the handles of the audio track and the video
    /// track, 0 for one not asked for.
    void openUserMedia(int requestId, boolean audio, boolean video, boolean frontCamera);

    /// Sends a local track over a peer connection.
    void addTrack(int peer, int track);

    /// Creates an offer, or with `answer` an answer to the remote description
    /// already set. Answers with `WebRtc.deliverDescription`.
    void createDescription(int peer, int requestId, boolean answer);

    /// Sets a description as the local one, or with `remote` as the remote
    /// one. Answers with `WebRtc.deliverResult`.
    ///
    /// #### Parameters
    ///
    /// - `type`: `offer` or `answer`
    void setDescription(int peer, int requestId, boolean remote, String type, String sdp);

    /// Adds a candidate the other side gathered.
    void addIceCandidate(int peer, String sdpMid, int sdpMLineIndex, String candidate);

    /// Closes a peer connection and frees its handle.
    void closePeerConnection(int peer);

    /// Mutes or unmutes a track, local or remote.
    void setTrackEnabled(int track, boolean enabled);

    /// Switches a local video track between the front and back cameras.
    void switchCamera(int track);

    /// Routes call audio to the loudspeaker or back to the earpiece.
    void setSpeakerphone(boolean on);

    /// Stops a local track and frees its handle.
    void releaseTrack(int track);

    /// A native view drawing a video track: a `com.codename1.ui.PeerComponent`,
    /// or any `com.codename1.ui.Component` on a port that draws it itself.
    /// Null when the track cannot be drawn.
    Object createVideoView(int track, boolean mirror);

    /// Detaches a view from its track, before the view is discarded.
    void releaseVideoView(Object view);
}
