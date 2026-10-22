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

import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.util.AsyncResource;
import com.codename1.webrtc.spi.WebRtcBridge;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// The API over a scripted bridge: each answer reaches the right request on
/// the EDT, and a connection's events reach its listener.
class WebRtcTest extends UITestBase {

    /// Answers at once, the way the simulator's loopback does.
    static final class Fake implements WebRtcBridge {
        final List<String> calls = new ArrayList<String>();
        int peers;

        public int createPeerConnection(String iceServers, boolean relayOnly) {
            calls.add("create " + iceServers + " " + relayOnly);
            return ++peers;
        }

        public void openUserMedia(int requestId, boolean audio, boolean video, boolean front) {
            WebRtc.deliverTracks(requestId, audio ? 11 : 0, video ? 12 : 0, null);
        }

        public void addTrack(int peer, int track) {
            calls.add("add " + peer + " " + track);
        }

        public void createDescription(int peer, int requestId, boolean answer) {
            WebRtc.deliverDescription(requestId, answer ? "answer" : "offer", "v=0", null);
        }

        public void setDescription(int peer, int requestId, boolean remote, String type,
                String sdp) {
            WebRtc.deliverResult(requestId, remote && sdp.length() == 0 ? "empty" : null);
            if (remote) {
                WebRtc.remoteTrack(peer, 21, true);
                WebRtc.stateChanged(peer, PeerConnection.CONNECTED);
            } else {
                WebRtc.iceCandidate(peer, "0", 0, "candidate:1");
            }
        }

        public void addIceCandidate(int peer, String mid, int index, String candidate) {
            calls.add("ice " + peer + " " + candidate);
        }

        public void closePeerConnection(int peer) {
            calls.add("close " + peer);
        }

        public void setTrackEnabled(int track, boolean enabled) {
            calls.add("enabled " + track + " " + enabled);
        }

        public void switchCamera(int track) {
            calls.add("switch " + track);
        }

        public void setSpeakerphone(boolean on) {
        }

        public void releaseTrack(int track) {
            calls.add("release " + track);
        }

        public Object createVideoView(int track, boolean mirror) {
            return new com.codename1.ui.Label("video " + track);
        }

        public void releaseVideoView(Object view) {
            calls.add("releaseView");
        }
    }

    @AfterEach
    void clear() {
        TestCodenameOneImplementation.getInstance().setWebRtcBridge(null);
    }

    private <T> T await(AsyncResource<T> pending) {
        flushSerialCalls();
        assertTrue(pending.isDone(), "answered");
        return pending.get();
    }

    @Test
    void unsupportedWithoutABridge() {
        assertFalse(WebRtc.isSupported());
        assertThrows(IllegalStateException.class, () -> WebRtc.openUserMedia(true, false, true));
    }

    @Test
    void aCallRunsThroughTheBridge() {
        Fake fake = new Fake();
        TestCodenameOneImplementation.getInstance().setWebRtcBridge(fake);
        assertTrue(WebRtc.isSupported());

        LocalMedia media = await(WebRtc.openUserMedia(true, true, true));
        assertEquals(MediaTrack.AUDIO, media.getAudio().getKind());
        assertTrue(media.getVideo().isVideo());
        assertTrue(media.getVideo().isLocal());

        final List<String> heard = new ArrayList<String>();
        PeerConnection pc = WebRtc.createPeerConnection(new PeerConnectionConfig()
                .addIceServer(IceServer.stun("stun:stun.example.com")).setRelayOnly(true),
                new PeerConnection.Listener() {
                    public void iceCandidate(PeerConnection c, IceCandidate candidate) {
                        heard.add("ice " + candidate.getCandidate());
                    }

                    public void stateChanged(PeerConnection c, String state) {
                        heard.add("state " + state);
                    }

                    public void remoteTrack(PeerConnection c, MediaTrack track) {
                        heard.add("track " + track.getKind() + " " + track.isLocal());
                    }
                });
        assertEquals("create stun:stun.example.com\t\t true", fake.calls.get(0));
        pc.addTrack(media.getAudio());
        pc.addTrack(media.getVideo());

        SessionDescription offer = await(pc.createOffer());
        assertEquals(SessionDescription.OFFER, offer.getType());
        assertEquals(Boolean.TRUE, await(pc.setLocalDescription(offer)));
        assertEquals(Boolean.TRUE, await(pc.setRemoteDescription(
                new SessionDescription(SessionDescription.ANSWER, "v=0"))));
        assertEquals("ice candidate:1", heard.get(0));
        assertTrue(heard.contains("track video false"));
        assertTrue(heard.contains("state connected"));
        assertEquals(PeerConnection.CONNECTED, pc.getState());

        AsyncResource<Boolean> refused = pc.setRemoteDescription(
                new SessionDescription(SessionDescription.ANSWER, ""));
        flushSerialCalls();
        assertTrue(refused.isDone());
        assertThrows(Exception.class, refused::get);

        media.getAudio().setEnabled(false);
        media.getVideo().switchCamera();
        VideoView view = new VideoView(media.getVideo(), true);
        assertTrue(view.isRendering());
        view.release();
        pc.close();
        pc.addIceCandidate(new IceCandidate("0", 0, "late"));
        media.release();
        assertTrue(fake.calls.contains("enabled 11 false"));
        assertTrue(fake.calls.contains("switch 12"));
        assertTrue(fake.calls.contains("releaseView"));
        assertTrue(fake.calls.contains("close 1"));
        assertFalse(fake.calls.contains("ice 1 late"));
        assertTrue(fake.calls.contains("release 11"));
        assertEquals(PeerConnection.CLOSED, pc.getState());
    }

    @Test
    void onlyLocalTracksCanBeSent() {
        TestCodenameOneImplementation.getInstance().setWebRtcBridge(new Fake());
        PeerConnection pc = WebRtc.createPeerConnection(null, new PeerConnection.Listener() {
            public void iceCandidate(PeerConnection c, IceCandidate candidate) {
            }

            public void stateChanged(PeerConnection c, String state) {
            }

            public void remoteTrack(PeerConnection c, MediaTrack track) {
            }
        });
        assertThrows(IllegalArgumentException.class,
                () -> pc.addTrack(new MediaTrack(5, MediaTrack.VIDEO, false)));
    }
}
