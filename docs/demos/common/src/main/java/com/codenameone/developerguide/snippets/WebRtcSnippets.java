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
package com.codenameone.developerguide.snippets;

import com.codename1.io.Log;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.webrtc.IceCandidate;
import com.codename1.webrtc.IceServer;
import com.codename1.webrtc.LocalMedia;
import com.codename1.webrtc.MediaTrack;
import com.codename1.webrtc.PeerConnection;
import com.codename1.webrtc.PeerConnectionConfig;
import com.codename1.webrtc.SessionDescription;
import com.codename1.webrtc.VideoView;
import com.codename1.webrtc.WebRtc;

/** Compiled source snippets for the WebRTC guide chapter. */
public class WebRtcSnippets {
    private PeerConnection pc;
    private VideoView remoteView;
    private Form form;

    /** The app's own channel to the other side: a server, a WebSocket, anything. */
    public interface Signalling {
        void send(SessionDescription description);

        void send(IceCandidate candidate);
    }

    /** Placing a call: the camera and microphone, a connection, and an offer. */
    public void call(Signalling signalling) {
        // tag::webrtc-call[]
        if (!WebRtc.isSupported()) {
            return;
        }
        WebRtc.openUserMedia(true, true, true).ready(media -> {
            PeerConnectionConfig config = new PeerConnectionConfig()
                    .addIceServer(IceServer.stun("stun:stun.example.com:3478"))
                    .addIceServer(new IceServer(new String[] {"turn:turn.example.com:3478"},
                            "username", "credential"));
            pc = WebRtc.createPeerConnection(config, new PeerConnection.Listener() {
                public void iceCandidate(PeerConnection connection, IceCandidate candidate) {
                    signalling.send(candidate);
                }

                public void stateChanged(PeerConnection connection, String state) {
                    if (PeerConnection.FAILED.equals(state)) {
                        Log.p("No path to the other side");
                    }
                }

                public void remoteTrack(PeerConnection connection, MediaTrack track) {
                    if (track.isVideo()) {
                        remoteView = new VideoView(track, false);
                        form.add(BorderLayout.CENTER, remoteView);
                        form.revalidate();
                    }
                }
            });
            pc.addTrack(media.getAudio());
            pc.addTrack(media.getVideo());
            form.add(BorderLayout.SOUTH, new VideoView(media.getVideo(), true));

            pc.createOffer().ready(offer -> pc.setLocalDescription(offer)
                    .ready(done -> signalling.send(offer)));
        });
        // end::webrtc-call[]
    }

    /** The other side's description and candidates arrive over the app's own channel. */
    public void answer(Signalling signalling, SessionDescription offer, LocalMedia media) {
        // tag::webrtc-answer[]
        pc.setRemoteDescription(offer).ready(done -> {
            pc.addTrack(media.getAudio());
            pc.addTrack(media.getVideo());
            pc.createAnswer().ready(answer -> pc.setLocalDescription(answer)
                    .ready(sent -> signalling.send(answer)));
        });
        // end::webrtc-answer[]
    }

    /** Candidates that arrive from the other side. */
    public void candidate(String sdpMid, int sdpMLineIndex, String sdp) {
        // tag::webrtc-candidate[]
        pc.addIceCandidate(new IceCandidate(sdpMid, sdpMLineIndex, sdp));
        // end::webrtc-candidate[]
    }

    /** Hanging up releases the camera, the microphone and the views. */
    public void hangUp(LocalMedia media) {
        // tag::webrtc-hangup[]
        pc.close();
        media.release();
        if (remoteView != null) {
            remoteView.release();
        }
        // end::webrtc-hangup[]
    }
}
