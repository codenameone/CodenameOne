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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.io.Log;
import com.codename1.webrtc.IceCandidate;
import com.codename1.webrtc.IceServer;
import com.codename1.webrtc.LocalMedia;
import com.codename1.webrtc.MediaTrack;
import com.codename1.webrtc.PeerConnection;
import com.codename1.webrtc.PeerConnectionConfig;
import com.codename1.webrtc.SessionDescription;
import com.codename1.webrtc.WebRtc;
import java.util.List;
import java.util.Map;

/// [ShopperCall.Media] over Codename One's WebRTC: the microphone (and the
/// camera for a video call), one peer connection built with the ICE servers
/// the chat socket's `hello` handed out -- STUN, and TURN credentials minted
/// for this connection when the server has a TURN server configured.
final class RtcMedia implements ShopperCall.Media {
    /// Told when something the call screen draws arrived or went.
    interface Changed {
        void changed();
    }

    private final List iceServers;
    private final Changed changed;
    private ShopperCall call;
    private LocalMedia local;
    private PeerConnection connection;
    private MediaTrack remoteVideo;
    private boolean released;

    RtcMedia(List iceServers, Changed changed) {
        this.iceServers = iceServers;
        this.changed = changed;
    }

    static boolean supported() {
        return WebRtc.isSupported();
    }

    MediaTrack localVideo() {
        return local == null ? null : local.getVideo();
    }

    MediaTrack remoteVideo() {
        return remoteVideo;
    }

    void mute(boolean muted) {
        if (local != null && local.getAudio() != null) {
            local.getAudio().setEnabled(!muted);
        }
    }

    @Override
    public void start(final ShopperCall owner, final boolean video, final String offer) {
        call = owner;
        WebRtc.openUserMedia(true, video, true).ready(media -> {
            if (released) {
                media.release();
                return;
            }
            local = media;
            open(offer);
        }).except(why -> fail(why));
    }

    private void open(String offer) {
        PeerConnectionConfig config = new PeerConnectionConfig();
        for (int iter = 0; iter < iceServers.size(); iter++) {
            Object each = iceServers.get(iter);
            if (each instanceof Map) {
                Map server = (Map) each;
                String[] urls = ChatFrames.urls(server);
                if (urls.length > 0) {
                    String user = ChatFrames.text(server, "username");
                    String password = ChatFrames.text(server, "credential");
                    config.addIceServer(new IceServer(urls, user.length() == 0 ? null : user,
                            password.length() == 0 ? null : password));
                }
            }
        }
        connection = WebRtc.createPeerConnection(config, new PeerConnection.Listener() {
            @Override
            public void iceCandidate(PeerConnection pc, IceCandidate candidate) {
                if (!released) {
                    call.localCandidate(candidate.getSdpMid(), candidate.getSdpMLineIndex(),
                            candidate.getCandidate());
                }
            }

            @Override
            public void stateChanged(PeerConnection pc, String state) {
                if (released) {
                    return;
                }
                if (PeerConnection.CONNECTED.equals(state)) {
                    call.connectedNow();
                    changed.changed();
                } else if (PeerConnection.FAILED.equals(state)) {
                    call.failed();
                    changed.changed();
                }
            }

            @Override
            public void remoteTrack(PeerConnection pc, MediaTrack track) {
                if (!released && track.isVideo()) {
                    remoteVideo = track;
                    changed.changed();
                }
            }
        });
        if (local.getAudio() != null) {
            connection.addTrack(local.getAudio());
        }
        if (local.getVideo() != null) {
            connection.addTrack(local.getVideo());
        }
        WebRtc.setSpeakerphone(local.getVideo() != null);
        changed.changed();
        connection.setRemoteDescription(new SessionDescription(SessionDescription.OFFER, offer))
                .ready(set -> {
                    if (released) {
                        return;
                    }
                    call.remoteSet();
                    connection.createAnswer().ready(answer ->
                            connection.setLocalDescription(answer).ready(done -> {
                                if (!released) {
                                    call.answered(answer.getSdp());
                                }
                            }).except(why -> fail(why))).except(why -> fail(why));
                }).except(why -> fail(why));
    }

    private void fail(Throwable why) {
        Log.p("Bazaar call failed: " + why);
        if (!released && call != null) {
            call.failed();
            changed.changed();
        }
    }

    @Override
    public void addRemoteCandidate(String sdpMid, int sdpMLineIndex, String candidate) {
        if (connection != null && !released) {
            connection.addIceCandidate(new IceCandidate(sdpMid.length() == 0 ? null : sdpMid,
                    sdpMLineIndex, candidate));
        }
    }

    @Override
    public void release() {
        released = true;
        if (connection != null) {
            connection.close();
            connection = null;
        }
        if (local != null) {
            local.release();
            local = null;
        }
        remoteVideo = null;
    }
}
