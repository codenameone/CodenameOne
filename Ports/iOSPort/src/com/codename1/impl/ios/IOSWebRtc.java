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
package com.codename1.impl.ios;

import com.codename1.ui.PeerComponent;
import com.codename1.webrtc.spi.WebRtcBridge;
import java.util.HashMap;
import java.util.Map;

/// The iOS [WebRtcBridge]: a thin delegation onto the `webRtc*` natives of
/// [IOSNative], implemented in `CN1WebRTC.m` over the WebRTC framework.
///
/// Every request is answered by the native side calling the static
/// `com.codename1.webrtc.WebRtc` callbacks, which marshal to the EDT
/// themselves, so nothing here needs to.
class IOSWebRtc implements WebRtcBridge {
    private final IOSImplementation impl;
    private final IOSNative nativeInstance;

    /// The UIView peer behind each video view handed out, so
    /// [#releaseVideoView(Object)] can detach it from its track.
    private final Map<Object, Long> views = new HashMap<Object, Long>();

    IOSWebRtc(IOSImplementation impl, IOSNative nativeInstance) {
        this.impl = impl;
        this.nativeInstance = nativeInstance;
    }

    @Override
    public int createPeerConnection(String iceServers, boolean relayOnly) {
        return nativeInstance.webRtcCreatePeerConnection(
                iceServers == null ? "" : iceServers, relayOnly);
    }

    @Override
    public void openUserMedia(int requestId, boolean audio, boolean video, boolean frontCamera) {
        nativeInstance.webRtcOpenUserMedia(requestId, audio, video, frontCamera);
    }

    @Override
    public void addTrack(int peer, int track) {
        nativeInstance.webRtcAddTrack(peer, track);
    }

    @Override
    public void createDescription(int peer, int requestId, boolean answer) {
        nativeInstance.webRtcCreateDescription(peer, requestId, answer);
    }

    @Override
    public void setDescription(int peer, int requestId, boolean remote, String type, String sdp) {
        nativeInstance.webRtcSetDescription(peer, requestId, remote,
                type == null ? "" : type, sdp == null ? "" : sdp);
    }

    @Override
    public void addIceCandidate(int peer, String sdpMid, int sdpMLineIndex, String candidate) {
        if (candidate == null || candidate.length() == 0) {
            // The end-of-candidates marker; the framework needs nothing for it.
            return;
        }
        nativeInstance.webRtcAddIceCandidate(peer, sdpMid == null ? "" : sdpMid,
                sdpMLineIndex, candidate);
    }

    @Override
    public void closePeerConnection(int peer) {
        nativeInstance.webRtcClosePeerConnection(peer);
    }

    @Override
    public void setTrackEnabled(int track, boolean enabled) {
        nativeInstance.webRtcSetTrackEnabled(track, enabled);
    }

    @Override
    public void switchCamera(int track) {
        nativeInstance.webRtcSwitchCamera(track);
    }

    @Override
    public void setSpeakerphone(boolean on) {
        nativeInstance.webRtcSetSpeakerphone(on);
    }

    @Override
    public void releaseTrack(int track) {
        nativeInstance.webRtcReleaseTrack(track);
    }

    @Override
    public Object createVideoView(int track, boolean mirror) {
        long peer = nativeInstance.webRtcCreateVideoView(track, mirror);
        if (peer == 0) {
            return null;
        }
        PeerComponent view = impl.createNativePeer(new long[] {peer});
        if (view == null) {
            nativeInstance.webRtcReleaseVideoView(peer);
            return null;
        }
        views.put(view, Long.valueOf(peer));
        return view;
    }

    @Override
    public void releaseVideoView(Object view) {
        Long peer = views.remove(view);
        if (peer != null) {
            nativeInstance.webRtcReleaseVideoView(peer.longValue());
        }
    }
}
