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
package com.codename1.impl.javase.webrtc;

import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.webrtc.WebRtc;
import com.codename1.webrtc.spi.WebRtcBridge;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The simulator's WebRTC: every step of a call without the media.
///
/// Tracks open at once, descriptions are generated with one `m=` line per
/// track a connection sends, a candidate is gathered when a local
/// description is set, and once both descriptions are in the connection
/// reports connected and hands over one remote track per `m=` line of the
/// other side's description. Nothing crosses the network: the two sides of a
/// call may be two simulators or a simulator and a test that answers through
/// the app's own signalling, and the flow is identical either way. A video
/// view paints a test pattern naming its track.
public final class LoopbackWebRtc implements WebRtcBridge {
    private static final int[] BARS = {0xc0c0c0, 0xc0c000, 0x00c0c0, 0x00c000, 0xc000c0, 0xc00000,
            0x0000c0};

    private final Map<Integer, Peer> peers = new HashMap<Integer, Peer>();
    private final Map<Integer, String> tracks = new HashMap<Integer, String>();
    private int nextPeer;
    private int nextTrack;

    private static final class Peer {
        final List<String> sending = new ArrayList<String>();
        String remote;
        boolean local;
        boolean connected;
        int session;
    }

    public synchronized int createPeerConnection(String iceServers, boolean relayOnly) {
        int id = ++nextPeer;
        Peer peer = new Peer();
        peer.session = 1000 + id;
        peers.put(Integer.valueOf(id), peer);
        return id;
    }

    public synchronized void openUserMedia(int requestId, boolean audio, boolean video,
            boolean frontCamera) {
        int a = 0;
        int v = 0;
        if (audio) {
            a = ++nextTrack;
            tracks.put(Integer.valueOf(a), "audio");
        }
        if (video) {
            v = ++nextTrack;
            tracks.put(Integer.valueOf(v), "video");
        }
        WebRtc.deliverTracks(requestId, a, v, null);
    }

    public synchronized void addTrack(int peer, int track) {
        Peer p = peers.get(Integer.valueOf(peer));
        String kind = tracks.get(Integer.valueOf(track));
        if (p != null && kind != null) {
            p.sending.add(kind);
        }
    }

    public synchronized void createDescription(int peer, int requestId, boolean answer) {
        Peer p = peers.get(Integer.valueOf(peer));
        if (p == null) {
            WebRtc.deliverDescription(requestId, null, null, "closed");
            return;
        }
        if (answer && p.remote == null) {
            WebRtc.deliverDescription(requestId, null, null, "No remote offer to answer");
            return;
        }
        StringBuilder sdp = new StringBuilder();
        sdp.append("v=0\r\no=- ").append(p.session).append(" 2 IN IP4 127.0.0.1\r\n")
                .append("s=-\r\nt=0 0\r\n");
        for (int iter = 0; iter < p.sending.size(); iter++) {
            String kind = p.sending.get(iter);
            sdp.append("m=").append(kind).append(" 9 UDP/TLS/RTP/SAVPF ")
                    .append("audio".equals(kind) ? "111" : "96").append("\r\n")
                    .append("a=mid:").append(iter).append("\r\na=sendrecv\r\n");
        }
        WebRtc.deliverDescription(requestId, answer ? "answer" : "offer", sdp.toString(), null);
    }

    public void setDescription(int peer, int requestId, boolean remote, String type, String sdp) {
        boolean connect = false;
        List<String> arriving = null;
        synchronized (this) {
            Peer p = peers.get(Integer.valueOf(peer));
            if (p == null) {
                WebRtc.deliverResult(requestId, "closed");
                return;
            }
            if (remote) {
                p.remote = sdp == null ? "" : sdp;
            } else {
                p.local = true;
            }
            if (p.local && p.remote != null && !p.connected) {
                p.connected = true;
                connect = true;
                arriving = kinds(p.remote);
            }
        }
        WebRtc.deliverResult(requestId, null);
        if (!remote) {
            WebRtc.iceCandidate(peer, "0", 0,
                    "candidate:1 1 udp 2122260223 127.0.0.1 9 typ host");
        }
        if (connect) {
            WebRtc.stateChanged(peer, "connecting");
            for (int iter = 0; iter < arriving.size(); iter++) {
                String kind = arriving.get(iter);
                int track;
                synchronized (this) {
                    track = ++nextTrack;
                    tracks.put(Integer.valueOf(track), kind);
                }
                WebRtc.remoteTrack(peer, track, "video".equals(kind));
            }
            WebRtc.stateChanged(peer, "connected");
        }
    }

    private static List<String> kinds(String sdp) {
        List<String> out = new ArrayList<String>();
        int at = 0;
        while (at < sdp.length()) {
            int end = sdp.indexOf('\n', at);
            if (end < 0) {
                end = sdp.length();
            }
            String line = sdp.substring(at, end).trim();
            if (line.startsWith("m=audio")) {
                out.add("audio");
            } else if (line.startsWith("m=video")) {
                out.add("video");
            }
            at = end + 1;
        }
        return out;
    }

    public void addIceCandidate(int peer, String sdpMid, int sdpMLineIndex, String candidate) {
        // Nothing to try: the loopback is connected by its descriptions.
    }

    public synchronized void closePeerConnection(int peer) {
        peers.remove(Integer.valueOf(peer));
    }

    public void setTrackEnabled(int track, boolean enabled) {
    }

    public void switchCamera(int track) {
    }

    public void setSpeakerphone(boolean on) {
    }

    public synchronized void releaseTrack(int track) {
        tracks.remove(Integer.valueOf(track));
    }

    public Object createVideoView(final int track, final boolean mirror) {
        Component view = new Component() {
            @Override
            public void paint(Graphics g) {
                int w = getWidth();
                int h = getHeight();
                int bar = Math.max(1, w / BARS.length);
                for (int iter = 0; iter < BARS.length; iter++) {
                    int color = BARS[mirror ? BARS.length - 1 - iter : iter];
                    g.setColor(color);
                    g.fillRect(getX() + iter * bar, getY(), iter == BARS.length - 1
                            ? w - iter * bar : bar, h);
                }
                g.setColor(0xffffff);
                g.drawString("track " + track, getX() + 8, getY() + 8);
            }

            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(320, 240);
            }
        };
        view.setName("videoTrack-" + track);
        return view;
    }

    public void releaseVideoView(Object view) {
    }
}
