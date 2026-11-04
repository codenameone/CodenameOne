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
package com.codenameone.examples.dash.run;

import com.codename1.io.Log;
import com.codename1.webrtc.IceCandidate;
import com.codename1.webrtc.IceServer;
import com.codename1.webrtc.LocalMedia;
import com.codename1.webrtc.MediaTrack;
import com.codename1.webrtc.PeerConnection;
import com.codename1.webrtc.PeerConnectionConfig;
import com.codename1.webrtc.SessionDescription;
import com.codename1.webrtc.WebRtc;
import com.codenameone.examples.dash.api.GameProtocol;
import com.codenameone.examples.dash.net.GameLink;
import com.codenameone.examples.dash.rules.Json;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Party voice: a mesh of [PeerConnection]s, one per other member, audio
/// only. A party is at most [GameProtocol#MAX_RUNNERS], so each device sends
/// its voice a handful of times at most and no media server is needed.
///
/// The game's own socket carries the signaling. A `voice` message names who
/// it is for, and the server relays it to that member only if both are in
/// the same party or race; the text inside -- an offer, an answer or an ICE
/// candidate -- is opaque to it. Of each pair the member with the smaller
/// username offers, so two members never call each other at once.
///
/// Muting disables the local track and tells the party, so every screen can
/// show who is listening. The same instance carries over from the lobby into
/// the race, and is closed with the socket.
// tag::dash-voice[]
public final class Voice implements PeerConnection.Listener {
    /// Who is talking, for the lobby to draw.
    public interface Listener {
        void peers(String[] connected);

        void muted(String username, boolean on);
    }

    private static Voice current;
    private final GameLink link;
    private final String me;
    private final Map<String, PeerConnection> peers = new HashMap<String, PeerConnection>();
    private final Map<PeerConnection, String> owners = new HashMap<PeerConnection, String>();
    private final List<String[]> early = new ArrayList<String[]>();
    private String[] members = new String[0];
    private LocalMedia media;
    private boolean muted;
    private boolean closed;
    private Listener listener;

    private Voice(GameLink link, String me) {
        this.link = link;
        this.me = me;
    }

    /// Whether this device can do voice at all.
    public static boolean available() {
        return WebRtc.isSupported();
    }

    /// Starts voice over `link` for the signed-in `username`, replacing any
    /// earlier call. Asks for the microphone; nothing is sent until it is
    /// granted.
    public static Voice start(GameLink link, String username) {
        stop();
        final Voice v = new Voice(link, username);
        current = v;
        WebRtc.openUserMedia(true, false, false).ready(m -> {
            if (v.closed) {
                m.release();
                return;
            }
            v.media = m;
            v.connect();
            for (String[] s : v.early) {
                v.signal(s[0], s[1]);
            }
            v.early.clear();
        }).except(err -> Log.p("Voice unavailable: " + err));
        return v;
    }

    /// The call in progress, or null.
    public static Voice current() {
        return current;
    }

    public static void stop() {
        if (current != null) {
            current.close();
            current = null;
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /// The party or race now has `usernames`; empty entries are bots.
    public void members(String[] usernames) {
        members = usernames == null ? new String[0] : usernames;
        if (media != null) {
            connect();
        }
    }

    private void connect() {
        List<String> keep = new ArrayList<String>();
        for (String u : members) {
            if (u == null || u.length() == 0 || u.equals(me)) {
                continue;
            }
            keep.add(u);
            if (!peers.containsKey(u) && me.compareTo(u) < 0) {
                offer(u);
            }
        }
        for (String u : new ArrayList<String>(peers.keySet())) {
            if (!keep.contains(u)) {
                PeerConnection pc = peers.remove(u);
                owners.remove(pc);
                pc.close();
            }
        }
        notifyPeers();
    }

    private PeerConnection peer(String u) {
        PeerConnection pc = peers.get(u);
        if (pc == null) {
            PeerConnectionConfig cfg = new PeerConnectionConfig()
                    .addIceServer(IceServer.stun("stun:stun.l.google.com:19302"));
            pc = WebRtc.createPeerConnection(cfg, this);
            MediaTrack audio = media.getAudio();
            if (audio != null) {
                audio.setEnabled(!muted);
                pc.addTrack(audio);
            }
            peers.put(u, pc);
            owners.put(pc, u);
        }
        return pc;
    }

    private void offer(final String u) {
        final PeerConnection pc = peer(u);
        pc.createOffer().ready(o -> pc.setLocalDescription(o).ready(ok -> send(u, "o\n" + o.getSdp())));
    }

    /// A `voice` message from `from`.
    public void signal(String from, String data) {
        if (closed || from == null || data == null || data.length() < 2) {
            return;
        }
        if (media == null) {
            early.add(new String[] {from, data});
            return;
        }
        char kind = data.charAt(0);
        String body = data.substring(2);
        if (kind == 'o') {
            final PeerConnection pc = peer(from);
            final String to = from;
            pc.setRemoteDescription(new SessionDescription(SessionDescription.OFFER, body)).ready(
                    ok -> pc.createAnswer().ready(a -> pc.setLocalDescription(a).ready(
                            done -> send(to, "a\n" + a.getSdp()))));
        } else if (kind == 'a') {
            PeerConnection pc = peers.get(from);
            if (pc != null) {
                pc.setRemoteDescription(new SessionDescription(SessionDescription.ANSWER, body));
            }
        } else if (kind == 'c') {
            PeerConnection pc = peers.get(from);
            int a = body.indexOf('\n');
            int b = a < 0 ? -1 : body.indexOf('\n', a + 1);
            if (pc != null && b > a) {
                pc.addIceCandidate(new IceCandidate(body.substring(0, a),
                        Integer.parseInt(body.substring(a + 1, b)), body.substring(b + 1)));
            }
        }
    }

    /// A `mute` message: `username` turned their microphone on or off.
    public void mutedBy(String username, boolean on) {
        if (listener != null) {
            listener.muted(username, on);
        }
    }

    public void setMuted(boolean m) {
        muted = m;
        if (media != null && media.getAudio() != null) {
            media.getAudio().setEnabled(!m);
        }
        link.send("{\"type\":\"" + GameProtocol.MUTE + "\",\"on\":" + m + "}");
    }

    public boolean isMuted() {
        return muted;
    }

    private void send(String to, String data) {
        if (!closed) {
            link.send("{\"type\":\"" + GameProtocol.VOICE + "\",\"to\":" + Json.write(to) + ",\"data\":"
                    + Json.write(data) + "}");
        }
    }

    public void close() {
        closed = true;
        for (PeerConnection pc : peers.values()) {
            pc.close();
        }
        peers.clear();
        owners.clear();
        if (media != null) {
            media.release();
            media = null;
        }
    }

    @Override
    public void iceCandidate(PeerConnection connection, IceCandidate c) {
        String u = owners.get(connection);
        if (u != null) {
            send(u, "c\n" + c.getSdpMid() + "\n" + c.getSdpMLineIndex() + "\n" + c.getCandidate());
        }
    }

    @Override
    public void stateChanged(PeerConnection connection, String state) {
        notifyPeers();
    }

    @Override
    public void remoteTrack(PeerConnection connection, MediaTrack track) {
        // Remote audio plays on its own once the track arrives.
        WebRtc.setSpeakerphone(true);
    }

    private void notifyPeers() {
        if (listener == null) {
            return;
        }
        List<String> on = new ArrayList<String>();
        for (Map.Entry<String, PeerConnection> e : peers.entrySet()) {
            if (PeerConnection.CONNECTED.equals(e.getValue().getState())) {
                on.add(e.getKey());
            }
        }
        listener.peers(on.toArray(new String[on.size()]));
    }
}
// end::dash-voice[]
