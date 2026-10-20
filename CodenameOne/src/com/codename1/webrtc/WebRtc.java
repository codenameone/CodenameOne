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

import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.codename1.webrtc.spi.WebRtcBridge;
import java.util.HashMap;
import java.util.Map;

/// Real-time audio and video between two devices: the entry point of the
/// package.
///
/// ```java
/// if (!WebRtc.isSupported()) {
///     return;
/// }
/// WebRtc.openUserMedia(true, true, true).ready(media -> {
///     PeerConnection pc = WebRtc.createPeerConnection(
///             new PeerConnectionConfig().addIceServer(IceServer.stun("stun:stun.example.com")),
///             listener);
///     pc.addTrack(media.getAudio());
///     pc.addTrack(media.getVideo());
///     form.add(new VideoView(media.getVideo(), true));
/// });
/// ```
///
/// The package carries media. It does not ring, place the call in the
/// system call log, or wake a sleeping phone: that is `com.codename1.call`,
/// which an app uses alongside this one. And it does not signal -- see
/// [PeerConnection].
///
/// On Android this is Google's WebRTC library and on iOS Apple-platform
/// builds of the same library; both are added to an app only when it uses
/// this package. In a browser it is the browser's own WebRTC. The simulator
/// connects calls without carrying media: tracks open, descriptions are
/// exchanged and connections reach [PeerConnection#CONNECTED], and the
/// video views show a test pattern, so an app's call flow can be developed
/// and tested end to end on the desktop.
public final class WebRtc {
    private static final Map<Integer, AsyncResource<?>> REQUESTS =
            new HashMap<Integer, AsyncResource<?>>();
    private static final Map<Integer, PeerConnection> PEERS = new HashMap<Integer, PeerConnection>();
    private static int nextRequest = 1;

    private WebRtc() {
    }

    static WebRtcBridge bridge() {
        WebRtcBridge bridge = Display.getInstance().getWebRtcBridge();
        if (bridge == null) {
            throw new IllegalStateException("WebRTC is not supported on this platform");
        }
        return bridge;
    }

    /// Whether this platform carries WebRTC media.
    public static boolean isSupported() {
        return Display.getInstance().getWebRtcBridge() != null;
    }

    /// Opens the camera and/or the microphone, asking the user's permission
    /// where the platform asks for it. Fails when the user refuses or the
    /// device has neither.
    ///
    /// #### Parameters
    ///
    /// - `frontCamera`: which camera to open first; see
    ///   [MediaTrack#switchCamera()]
    public static AsyncResource<LocalMedia> openUserMedia(boolean audio, boolean video,
            boolean frontCamera) {
        AsyncResource<LocalMedia> out = new AsyncResource<LocalMedia>();
        if (!audio && !video) {
            out.error(new IllegalArgumentException("Ask for audio, video or both"));
            return out;
        }
        bridge().openUserMedia(request(out), audio, video, frontCamera);
        return out;
    }

    /// Creates a peer connection, which reports to `listener` on the EDT.
    public static PeerConnection createPeerConnection(PeerConnectionConfig config,
            PeerConnection.Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("A peer connection needs a listener");
        }
        PeerConnectionConfig used = config == null ? new PeerConnectionConfig() : config;
        int handle = bridge().createPeerConnection(used.wire(), used.isRelayOnly());
        if (handle < 0) {
            throw new IllegalStateException("The platform could not create a peer connection");
        }
        PeerConnection connection = new PeerConnection(handle, listener);
        synchronized (PEERS) {
            PEERS.put(Integer.valueOf(handle), connection);
        }
        return connection;
    }

    /// Routes call audio to the loudspeaker, or with false back to the
    /// earpiece or headset.
    public static void setSpeakerphone(boolean on) {
        bridge().setSpeakerphone(on);
    }

    static int request(AsyncResource<?> pending) {
        synchronized (REQUESTS) {
            int id = nextRequest++;
            REQUESTS.put(Integer.valueOf(id), pending);
            return id;
        }
    }

    static void forget(PeerConnection connection) {
        synchronized (PEERS) {
            PEERS.remove(Integer.valueOf(connection.handle()));
        }
    }

    private static AsyncResource<?> take(int requestId) {
        synchronized (REQUESTS) {
            return REQUESTS.remove(Integer.valueOf(requestId));
        }
    }

    private static PeerConnection peer(int handle) {
        synchronized (PEERS) {
            return PEERS.get(Integer.valueOf(handle));
        }
    }

    // ------------------------------------------------- answers from a port

    /// A port's answer to `createDescription`: the description, or a null
    /// `sdp` and the reason.
    public static void deliverDescription(final int requestId, final String type,
            final String sdp, final String error) {
        CN.callSerially(new Runnable() {
            public void run() {
                AsyncResource<?> pending = take(requestId);
                if (!(pending instanceof AsyncResource)) {
                    return;
                }
                @SuppressWarnings("unchecked")
                AsyncResource<SessionDescription> out = (AsyncResource<SessionDescription>) pending;
                if (sdp == null) {
                    out.error(new IllegalStateException(error == null ? "failed" : error));
                } else {
                    out.complete(new SessionDescription(type, sdp));
                }
            }
        });
    }

    /// A port's answer to `openUserMedia`: the track handles, 0 for none,
    /// or both 0 and the reason.
    public static void deliverTracks(final int requestId, final int audio, final int video,
            final String error) {
        CN.callSerially(new Runnable() {
            public void run() {
                AsyncResource<?> pending = take(requestId);
                if (pending == null) {
                    return;
                }
                @SuppressWarnings("unchecked")
                AsyncResource<LocalMedia> out = (AsyncResource<LocalMedia>) pending;
                if (audio == 0 && video == 0) {
                    out.error(new IllegalStateException(error == null
                            ? "The camera or microphone could not be opened" : error));
                    return;
                }
                out.complete(new LocalMedia(
                        audio == 0 ? null : new MediaTrack(audio, MediaTrack.AUDIO, true),
                        video == 0 ? null : new MediaTrack(video, MediaTrack.VIDEO, true)));
            }
        });
    }

    /// A port's answer to `setDescription`: null for success, else the
    /// reason.
    public static void deliverResult(final int requestId, final String error) {
        CN.callSerially(new Runnable() {
            public void run() {
                AsyncResource<?> pending = take(requestId);
                if (pending == null) {
                    return;
                }
                @SuppressWarnings("unchecked")
                AsyncResource<Boolean> out = (AsyncResource<Boolean>) pending;
                if (error == null) {
                    out.complete(Boolean.TRUE);
                } else {
                    out.error(new IllegalStateException(error));
                }
            }
        });
    }

    /// A candidate a peer connection gathered.
    public static void iceCandidate(final int peer, final String sdpMid, final int sdpMLineIndex,
            final String candidate) {
        CN.callSerially(new Runnable() {
            public void run() {
                PeerConnection connection = peer(peer);
                if (connection != null && candidate != null) {
                    connection.listener().iceCandidate(connection,
                            new IceCandidate(sdpMid, sdpMLineIndex, candidate));
                }
            }
        });
    }

    /// A peer connection moved to `state`, one of the names on
    /// [PeerConnection].
    public static void stateChanged(final int peer, final String state) {
        CN.callSerially(new Runnable() {
            public void run() {
                PeerConnection connection = peer(peer);
                if (connection != null && !state.equals(connection.getState())) {
                    connection.state(state);
                    connection.listener().stateChanged(connection, state);
                }
            }
        });
    }

    /// The other side's track arrived on a peer connection.
    public static void remoteTrack(final int peer, final int track, final boolean video) {
        CN.callSerially(new Runnable() {
            public void run() {
                PeerConnection connection = peer(peer);
                if (connection != null) {
                    connection.listener().remoteTrack(connection, new MediaTrack(track,
                            video ? MediaTrack.VIDEO : MediaTrack.AUDIO, false));
                }
            }
        });
    }
}
