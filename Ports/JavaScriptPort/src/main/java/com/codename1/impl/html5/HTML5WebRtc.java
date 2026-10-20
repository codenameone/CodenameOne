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
package com.codename1.impl.html5;

import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSFunctor;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.dom.HTMLElement;
import com.codename1.webrtc.WebRtc;
import com.codename1.webrtc.spi.WebRtcBridge;

/// The browser's own WebRTC behind the bridge.
///
/// The work is done by a small script object, `window.cn1WebRtc`, installed
/// on first use: it keeps the browser's peer connections and tracks in
/// tables keyed by the integer handles the API speaks in, and reports
/// everything back through one functor. Strings are converted on the way in
/// with `jvm.toNativeString`, as `@JSBody` does not marshal them.
final class HTML5WebRtc implements WebRtcBridge {

    /// Everything the script reports: what happened and up to five values.
    @JSFunctor
    interface Sink extends JSObject {
        void event(String kind, int a, int b, String s1, String s2, String s3);
    }

    private static boolean installed;

    @JSBody(params = {}, script = "return typeof RTCPeerConnection !== 'undefined'"
            + " && !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia);")
    static native boolean available();

    @JSBody(params = {"sink"}, script = ""
            + "if (window.cn1WebRtc) { window.cn1WebRtc.sink = sink; return; }\n"
            + "var R = { sink: sink, peers: {}, tracks: {}, pending: {}, next: 1 };\n"
            + "R.fire = function(k, a, b, s1, s2, s3) { try { R.sink(k, a|0, b|0,"
            + " s1 == null ? null : '' + s1, s2 == null ? null : '' + s2,"
            + " s3 == null ? null : '' + s3); } catch (e) { console.log('cn1WebRtc', e); } };\n"
            + "R.err = function(e) { return '' + ((e && (e.message || e.name)) || e); };\n"
            + "R.track = function(t) { var id = R.next++; R.tracks[id] = t; return id; };\n"
            + "R.create = function(wire, relay) {\n"
            + "  var servers = [];\n"
            + "  wire.split('\\n').forEach(function(line) { if (!line) return;\n"
            + "    var f = line.split('\\t'); var s = { urls: f[0].split(',') };\n"
            + "    if (f[1]) s.username = f[1]; if (f[2]) s.credential = f[2]; servers.push(s); });\n"
            + "  var id = R.next++;\n"
            + "  var pc = new RTCPeerConnection({ iceServers: servers,"
            + " iceTransportPolicy: relay ? 'relay' : 'all' });\n"
            + "  var p = { pc: pc, early: [], remoteSet: false };\n"
            + "  R.peers[id] = p;\n"
            + "  pc.onicecandidate = function(e) { if (e.candidate) R.fire('ice', id,"
            + " e.candidate.sdpMLineIndex || 0, e.candidate.sdpMid || '', e.candidate.candidate, null); };\n"
            + "  pc.onconnectionstatechange = function() { R.fire('state', id, 0, pc.connectionState, null, null); };\n"
            + "  pc.ontrack = function(e) { var t = R.track(e.track); R.fire('track', id, t, e.track.kind, null, null); };\n"
            + "  return id;\n"
            + "};\n"
            + "R.media = function(req, audio, video, front) {\n"
            + "  navigator.mediaDevices.getUserMedia({ audio: audio,"
            + " video: video ? { facingMode: front ? 'user' : 'environment' } : false })\n"
            + "  .then(function(stream) { var a = 0, v = 0;\n"
            + "    stream.getAudioTracks().forEach(function(t) { if (!a) a = R.track(t); });\n"
            + "    stream.getVideoTracks().forEach(function(t) { if (!v) { v = R.track(t);"
            + " R.tracks[v].cn1Front = front; } });\n"
            + "    R.fire('tracks', req, a, '' + v, null, null); })\n"
            + "  .catch(function(e) { R.fire('tracks', req, 0, '0', R.err(e), null); });\n"
            + "};\n"
            + "R.add = function(peer, track) { var p = R.peers[peer], t = R.tracks[track];\n"
            + "  if (p && t) { p.pc.addTrack(t, new MediaStream([t])); } };\n"
            + "R.describe = function(peer, req, answer) { var p = R.peers[peer];\n"
            + "  if (!p) { R.fire('desc', req, 0, null, null, 'closed'); return; }\n"
            + "  (answer ? p.pc.createAnswer() : p.pc.createOffer())\n"
            + "  .then(function(d) { R.fire('desc', req, 0, d.type, d.sdp, null); })\n"
            + "  .catch(function(e) { R.fire('desc', req, 0, null, null, R.err(e)); });\n"
            + "};\n"
            + "R.set = function(peer, req, remote, type, sdp) { var p = R.peers[peer];\n"
            + "  if (!p) { R.fire('result', req, 0, 'closed', null, null); return; }\n"
            + "  var d = { type: type, sdp: sdp };\n"
            + "  (remote ? p.pc.setRemoteDescription(d) : p.pc.setLocalDescription(d))\n"
            + "  .then(function() { if (remote) { p.remoteSet = true;\n"
            + "      p.early.forEach(function(c) { p.pc.addIceCandidate(c).catch(function() {}); });"
            + " p.early = []; }\n"
            + "    R.fire('result', req, 0, null, null, null); })\n"
            + "  .catch(function(e) { R.fire('result', req, 0, R.err(e), null, null); });\n"
            + "};\n"
            + "R.ice = function(peer, mid, index, cand) { var p = R.peers[peer]; if (!p) return;\n"
            + "  var c = { sdpMid: mid, sdpMLineIndex: index, candidate: cand };\n"
            + "  if (p.remoteSet) { p.pc.addIceCandidate(c).catch(function() {}); } else { p.early.push(c); } };\n"
            + "R.close = function(peer) { var p = R.peers[peer]; if (p) { delete R.peers[peer]; p.pc.close(); } };\n"
            + "R.enable = function(track, on) { var t = R.tracks[track]; if (t) t.enabled = on; };\n"
            + "R.release = function(track) { var t = R.tracks[track]; if (t) { delete R.tracks[track]; t.stop(); } };\n"
            + "R.switchCamera = function(track) { var t = R.tracks[track]; if (!t) return;\n"
            + "  var front = !t.cn1Front;\n"
            + "  navigator.mediaDevices.getUserMedia({ video: { facingMode: front ? 'user' : 'environment' } })\n"
            + "  .then(function(s) { var n = s.getVideoTracks()[0]; if (!n) return; n.cn1Front = front;\n"
            + "    n.enabled = t.enabled;\n"
            + "    Object.keys(R.peers).forEach(function(k) { R.peers[k].pc.getSenders().forEach(function(snd) {\n"
            + "      if (snd.track === t) snd.replaceTrack(n); }); });\n"
            + "    (t.cn1Views || []).forEach(function(v) { v.srcObject = new MediaStream([n]); });\n"
            + "    n.cn1Views = t.cn1Views; t.stop(); R.tracks[track] = n; })\n"
            + "  .catch(function(e) { console.log('cn1WebRtc switchCamera', e); }); };\n"
            + "R.view = function(track, mirror) { var t = R.tracks[track]; if (!t) return null;\n"
            + "  var v = document.createElement('video');\n"
            + "  v.autoplay = true; v.playsInline = true; v.muted = true;\n"
            + "  v.setAttribute('playsinline', ''); v.style.objectFit = 'cover';\n"
            + "  if (mirror) v.style.transform = 'scaleX(-1)';\n"
            + "  v.srcObject = new MediaStream([t]);\n"
            + "  (t.cn1Views = t.cn1Views || []).push(v);\n"
            + "  var play = v.play(); if (play && play.catch) play.catch(function() {});\n"
            + "  return v; };\n"
            + "window.cn1WebRtc = R;")
    private static native void install(Sink sink);

    @JSBody(params = {"wire", "relay"}, script =
            "return window.cn1WebRtc.create(jvm.toNativeString(wire), relay);")
    private static native int create(String wire, boolean relay);

    @JSBody(params = {"req", "audio", "video", "front"}, script =
            "window.cn1WebRtc.media(req, audio, video, front);")
    private static native void media(int req, boolean audio, boolean video, boolean front);

    @JSBody(params = {"peer", "track"}, script = "window.cn1WebRtc.add(peer, track);")
    private static native void add(int peer, int track);

    @JSBody(params = {"peer", "req", "answer"}, script =
            "window.cn1WebRtc.describe(peer, req, answer);")
    private static native void describe(int peer, int req, boolean answer);

    @JSBody(params = {"peer", "req", "remote", "type", "sdp"}, script =
            "window.cn1WebRtc.set(peer, req, remote, jvm.toNativeString(type),"
            + " jvm.toNativeString(sdp));")
    private static native void set(int peer, int req, boolean remote, String type, String sdp);

    @JSBody(params = {"peer", "mid", "index", "cand"}, script =
            "window.cn1WebRtc.ice(peer, mid == null ? null : jvm.toNativeString(mid), index,"
            + " jvm.toNativeString(cand));")
    private static native void ice(int peer, String mid, int index, String cand);

    @JSBody(params = {"peer"}, script = "window.cn1WebRtc.close(peer);")
    private static native void close(int peer);

    @JSBody(params = {"track", "on"}, script = "window.cn1WebRtc.enable(track, on);")
    private static native void enable(int track, boolean on);

    @JSBody(params = {"track"}, script = "window.cn1WebRtc.switchCamera(track);")
    private static native void switchTrack(int track);

    @JSBody(params = {"track"}, script = "window.cn1WebRtc.release(track);")
    private static native void release(int track);

    @JSBody(params = {"track", "mirror"}, script = "return window.cn1WebRtc.view(track, mirror);")
    private static native HTMLElement view(int track, boolean mirror);

    HTML5WebRtc() {
        synchronized (HTML5WebRtc.class) {
            if (!installed) {
                installed = true;
                install(new Sink() {
                    public void event(String kind, int a, int b, String s1, String s2,
                            String s3) {
                        dispatch(kind, a, b, s1, s2, s3);
                    }
                });
            }
        }
    }

    static void dispatch(String kind, int a, int b, String s1, String s2, String s3) {
        if ("tracks".equals(kind)) {
            int video = 0;
            try {
                video = Integer.parseInt(s1);
            } catch (NumberFormatException none) {
                video = 0;
            }
            WebRtc.deliverTracks(a, b, video, s2);
        } else if ("desc".equals(kind)) {
            WebRtc.deliverDescription(a, s1, s2, s3);
        } else if ("result".equals(kind)) {
            WebRtc.deliverResult(a, s1);
        } else if ("ice".equals(kind)) {
            WebRtc.iceCandidate(a, s1, b, s2);
        } else if ("state".equals(kind)) {
            WebRtc.stateChanged(a, s1);
        } else if ("track".equals(kind)) {
            WebRtc.remoteTrack(a, b, "video".equals(s1));
        }
    }

    public int createPeerConnection(String iceServers, boolean relayOnly) {
        return create(iceServers == null ? "" : iceServers, relayOnly);
    }

    public void openUserMedia(int requestId, boolean audio, boolean video, boolean frontCamera) {
        media(requestId, audio, video, frontCamera);
    }

    public void addTrack(int peer, int track) {
        add(peer, track);
    }

    public void createDescription(int peer, int requestId, boolean answer) {
        describe(peer, requestId, answer);
    }

    public void setDescription(int peer, int requestId, boolean remote, String type, String sdp) {
        set(peer, requestId, remote, type, sdp == null ? "" : sdp);
    }

    public void addIceCandidate(int peer, String sdpMid, int sdpMLineIndex, String candidate) {
        if (candidate != null) {
            ice(peer, sdpMid, sdpMLineIndex, candidate);
        }
    }

    public void closePeerConnection(int peer) {
        close(peer);
    }

    public void setTrackEnabled(int track, boolean enabled) {
        enable(track, enabled);
    }

    public void switchCamera(int track) {
        switchTrack(track);
    }

    /// A browser routes audio where the system says; a page cannot choose
    /// the loudspeaker.
    public void setSpeakerphone(boolean on) {
    }

    public void releaseTrack(int track) {
        release(track);
    }

    public Object createVideoView(int track, boolean mirror) {
        HTMLElement element = view(track, mirror);
        return element == null ? null : new HTML5Peer(element);
    }

    public void releaseVideoView(Object view) {
    }
}
