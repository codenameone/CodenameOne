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
package com.codename1.impl.android.webrtc;

import android.content.Context;
import android.media.AudioManager;
import com.codename1.impl.android.AndroidImplementation;
import com.codename1.ui.PeerComponent;
import com.codename1.webrtc.WebRtc;
import com.codename1.webrtc.spi.WebRtcBridge;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

/// The Android WebRTC bridge, on the `org.webrtc` SDK
/// (`io.github.webrtc-sdk:android`).
///
/// Optional: the builder deletes this package for every application that
/// never references `com.codename1.webrtc`, and adds the SDK only for the
/// ones that do. `AndroidImplementation` reaches it by name, so the port
/// itself compiles without the SDK.
///
/// Handles are allocated here, from one counter shared by peers and tracks.
/// All state is guarded by this object's monitor because the SDK answers on
/// its own signalling thread while the API calls in from the EDT.
public class AndroidWebRtc implements WebRtcBridge {
    private static final int VIDEO_WIDTH = 1280;
    private static final int VIDEO_HEIGHT = 720;
    private static final int VIDEO_FPS = 30;

    private final Context context;
    private EglBase egl;
    private PeerConnectionFactory factory;
    private int nextHandle = 1;
    private final Map<Integer, Peer> peers = new HashMap<Integer, Peer>();
    private final Map<Integer, MediaStreamTrack> tracks = new HashMap<Integer, MediaStreamTrack>();
    private final Map<Integer, LocalVideo> videos = new HashMap<Integer, LocalVideo>();
    private final Map<Object, Sink> views = new HashMap<Object, Sink>();
    private final java.util.Set<Integer> locals = new java.util.HashSet<Integer>();
    /// Each local audio track's source, a native object of its own that
    /// disposing the track leaves behind.
    private final Map<Integer, AudioSource> audioSources = new HashMap<Integer, AudioSource>();

    /// One peer connection and the candidates that arrived before its remote
    /// description, which the SDK would otherwise reject.
    private static final class Peer {
        PeerConnection connection;
        boolean remoteSet;
        final List<IceCandidate> pending = new ArrayList<IceCandidate>();
        /// Handles of the tracks the far side sent, dropped with the peer.
        final List<Integer> remote = new ArrayList<Integer>();
    }

    /// A local camera track and what feeds it.
    private static final class LocalVideo {
        CameraVideoCapturer capturer;
        SurfaceTextureHelper helper;
        VideoSource source;
        boolean front;
    }

    /// A renderer attached to a video track.
    private static final class Sink {
        SurfaceViewRenderer renderer;
        VideoTrack track;
    }

    /// Creates the bridge.
    ///
    /// #### Parameters
    ///
    /// - `context`: the application context
    public AndroidWebRtc(Context context) {
        this.context = context.getApplicationContext();
    }

    private synchronized PeerConnectionFactory factory() {
        if (factory == null) {
            PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions
                    .builder(context).createInitializationOptions());
            egl = EglBase.create();
            factory = PeerConnectionFactory.builder()
                    .setVideoEncoderFactory(new DefaultVideoEncoderFactory(
                            egl.getEglBaseContext(), true, true))
                    .setVideoDecoderFactory(new DefaultVideoDecoderFactory(
                            egl.getEglBaseContext()))
                    .createPeerConnectionFactory();
        }
        return factory;
    }

    private synchronized int allocate() {
        return nextHandle++;
    }

    private synchronized Peer peer(int handle) {
        return peers.get(Integer.valueOf(handle));
    }

    private synchronized MediaStreamTrack track(int handle) {
        return tracks.get(Integer.valueOf(handle));
    }

    private synchronized int register(MediaStreamTrack track) {
        int handle = nextHandle++;
        tracks.put(Integer.valueOf(handle), track);
        return handle;
    }

    private synchronized int registerLocal(MediaStreamTrack track) {
        int handle = register(track);
        locals.add(Integer.valueOf(handle));
        return handle;
    }

    @Override
    public int createPeerConnection(String iceServers, boolean relayOnly) {
        try {
            List<PeerConnection.IceServer> servers = new ArrayList<PeerConnection.IceServer>();
            if (iceServers != null) {
                String[] lines = iceServers.split("\n");
                for (int iter = 0; iter < lines.length; iter++) {
                    String line = lines[iter].trim();
                    if (line.length() == 0) {
                        continue;
                    }
                    String[] parts = line.split("\t", -1);
                    List<String> urls = new ArrayList<String>();
                    String[] split = parts[0].split(",");
                    for (int u = 0; u < split.length; u++) {
                        if (split[u].trim().length() > 0) {
                            urls.add(split[u].trim());
                        }
                    }
                    if (urls.isEmpty()) {
                        continue;
                    }
                    PeerConnection.IceServer.Builder b = PeerConnection.IceServer.builder(urls);
                    if (parts.length > 1 && parts[1].length() > 0) {
                        b.setUsername(parts[1]);
                    }
                    if (parts.length > 2 && parts[2].length() > 0) {
                        b.setPassword(parts[2]);
                    }
                    servers.add(b.createIceServer());
                }
            }
            PeerConnection.RTCConfiguration config = new PeerConnection.RTCConfiguration(servers);
            config.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
            config.iceTransportsType = relayOnly
                    ? PeerConnection.IceTransportsType.RELAY : PeerConnection.IceTransportsType.ALL;
            final int handle = allocate();
            PeerConnection connection = factory().createPeerConnection(config,
                    new Observer(handle));
            if (connection == null) {
                return -1;
            }
            Peer p = new Peer();
            p.connection = connection;
            synchronized (this) {
                peers.put(Integer.valueOf(handle), p);
            }
            inCall(true);
            return handle;
        } catch (Throwable t) {
            return -1;
        }
    }

    @Override
    public void openUserMedia(final int requestId, final boolean audio, final boolean video,
            final boolean frontCamera) {
        // Asked on the calling thread: checkForPermission prompts through the
        // port's usual flow and blocks until the user answers.
        if (audio && !AndroidImplementation.checkForPermission("android.permission.RECORD_AUDIO",
                "The microphone is needed for the call")) {
            WebRtc.deliverTracks(requestId, 0, 0, "Microphone permission denied");
            return;
        }
        if (video && !AndroidImplementation.checkForPermission("android.permission.CAMERA",
                "The camera is needed for the call")) {
            WebRtc.deliverTracks(requestId, 0, 0, "Camera permission denied");
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                int audioHandle = 0;
                int videoHandle = 0;
                String error = null;
                try {
                    PeerConnectionFactory f = factory();
                    if (audio) {
                        AudioSource source = f.createAudioSource(new MediaConstraints());
                        AudioTrack t = f.createAudioTrack("a" + allocate(), source);
                        audioHandle = registerLocal(t);
                        synchronized (AndroidWebRtc.this) {
                            audioSources.put(Integer.valueOf(audioHandle), source);
                        }
                    }
                    if (video) {
                        videoHandle = openCamera(f, frontCamera);
                        if (videoHandle == 0) {
                            error = "No camera could be opened";
                        }
                    }
                } catch (Throwable t) {
                    error = String.valueOf(t.getMessage());
                }
                if (error != null && audioHandle != 0) {
                    releaseTrack(audioHandle);
                    audioHandle = 0;
                }
                WebRtc.deliverTracks(requestId, audioHandle, videoHandle, error);
            }
        }, "CN1 WebRTC media").start();
    }

    private int openCamera(PeerConnectionFactory f, boolean front) {
        Camera2Enumerator enumerator = new Camera2Enumerator(context);
        String name = pickCamera(enumerator, front);
        if (name == null) {
            return 0;
        }
        LocalVideo lv = new LocalVideo();
        lv.front = enumerator.isFrontFacing(name);
        lv.capturer = enumerator.createCapturer(name, null);
        if (lv.capturer == null) {
            return 0;
        }
        lv.helper = SurfaceTextureHelper.create("CN1 WebRTC capture", egl.getEglBaseContext());
        lv.source = f.createVideoSource(false);
        lv.capturer.initialize(lv.helper, context, lv.source.getCapturerObserver());
        lv.capturer.startCapture(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FPS);
        VideoTrack t = f.createVideoTrack("v" + allocate(), lv.source);
        int handle = registerLocal(t);
        synchronized (this) {
            videos.put(Integer.valueOf(handle), lv);
        }
        return handle;
    }

    private static String pickCamera(Camera2Enumerator enumerator, boolean front) {
        String[] names = enumerator.getDeviceNames();
        for (int iter = 0; iter < names.length; iter++) {
            if (enumerator.isFrontFacing(names[iter]) == front) {
                return names[iter];
            }
        }
        return names.length > 0 ? names[0] : null;
    }

    @Override
    public void addTrack(int peer, int track) {
        Peer p = peer(peer);
        MediaStreamTrack t = track(track);
        if (p != null && t != null) {
            List<String> streams = new ArrayList<String>();
            streams.add("cn1");
            p.connection.addTrack(t, streams);
        }
    }

    @Override
    public void createDescription(int peer, final int requestId, boolean answer) {
        Peer p = peer(peer);
        if (p == null) {
            WebRtc.deliverDescription(requestId, null, null, "Unknown peer connection");
            return;
        }
        SdpObserver observer = new SdpObserver() {
            public void onCreateSuccess(SessionDescription desc) {
                WebRtc.deliverDescription(requestId, typeName(desc.type), desc.description, null);
            }

            public void onCreateFailure(String error) {
                WebRtc.deliverDescription(requestId, null, null, error);
            }

            public void onSetSuccess() {
            }

            public void onSetFailure(String error) {
            }
        };
        if (answer) {
            p.connection.createAnswer(observer, new MediaConstraints());
        } else {
            p.connection.createOffer(observer, new MediaConstraints());
        }
    }

    private static String typeName(SessionDescription.Type type) {
        switch (type) {
            case OFFER:
                return "offer";
            case PRANSWER:
                return "pranswer";
            case ROLLBACK:
                return "rollback";
            default:
                return "answer";
        }
    }

    @Override
    public void setDescription(int peer, final int requestId, final boolean remote, String type,
            String sdp) {
        final Peer p = peer(peer);
        if (p == null) {
            WebRtc.deliverResult(requestId, "Unknown peer connection");
            return;
        }
        SessionDescription.Type t;
        if ("offer".equals(type)) {
            t = SessionDescription.Type.OFFER;
        } else if ("answer".equals(type)) {
            t = SessionDescription.Type.ANSWER;
        } else if ("pranswer".equals(type)) {
            t = SessionDescription.Type.PRANSWER;
        } else {
            WebRtc.deliverResult(requestId, "Unknown description type " + type);
            return;
        }
        SdpObserver observer = new SdpObserver() {
            public void onCreateSuccess(SessionDescription desc) {
            }

            public void onCreateFailure(String error) {
            }

            public void onSetSuccess() {
                if (remote) {
                    flushCandidates(p);
                }
                WebRtc.deliverResult(requestId, null);
            }

            public void onSetFailure(String error) {
                WebRtc.deliverResult(requestId, error == null ? "failed" : error);
            }
        };
        SessionDescription desc = new SessionDescription(t, sdp);
        if (remote) {
            p.connection.setRemoteDescription(observer, desc);
        } else {
            p.connection.setLocalDescription(observer, desc);
        }
    }

    private void flushCandidates(Peer p) {
        List<IceCandidate> held;
        synchronized (this) {
            p.remoteSet = true;
            held = new ArrayList<IceCandidate>(p.pending);
            p.pending.clear();
        }
        for (int iter = 0; iter < held.size(); iter++) {
            p.connection.addIceCandidate(held.get(iter));
        }
    }

    @Override
    public void addIceCandidate(int peer, String sdpMid, int sdpMLineIndex, String candidate) {
        Peer p = peer(peer);
        if (p == null || candidate == null) {
            return;
        }
        IceCandidate c = new IceCandidate(sdpMid, sdpMLineIndex, candidate);
        synchronized (this) {
            if (!p.remoteSet) {
                p.pending.add(c);
                return;
            }
        }
        p.connection.addIceCandidate(c);
    }

    @Override
    public void closePeerConnection(int peer) {
        Peer p;
        synchronized (this) {
            p = peers.remove(Integer.valueOf(peer));
            if (p != null) {
                // The SDK disposes remote tracks with their connection, and
                // releaseTrack leaves them alone, so their handles go here.
                for (int iter = 0; iter < p.remote.size(); iter++) {
                    tracks.remove(p.remote.get(iter));
                }
            }
        }
        if (p != null) {
            p.connection.dispose();
        }
        boolean last;
        synchronized (this) {
            last = peers.isEmpty();
        }
        if (last) {
            inCall(false);
        }
    }

    /// The audio mode before the first call, restored after the last; -1
    /// outside a call.
    private int modeBeforeCall = -1;

    /// A call keeps the audio system in communication mode -- earpiece
    /// routing, echo cancellation -- for its whole length, whichever way the
    /// speaker is switched, and hands back the mode it found when it ends.
    private void inCall(boolean on) {
        Object service = context.getSystemService(Context.AUDIO_SERVICE);
        if (!(service instanceof AudioManager)) {
            return;
        }
        AudioManager am = (AudioManager) service;
        if (on && modeBeforeCall < 0) {
            modeBeforeCall = am.getMode();
            am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        } else if (!on && modeBeforeCall >= 0) {
            am.setSpeakerphoneOn(false);
            am.setMode(modeBeforeCall);
            modeBeforeCall = -1;
        }
    }

    @Override
    public void setTrackEnabled(int track, boolean enabled) {
        MediaStreamTrack t = track(track);
        if (t != null) {
            t.setEnabled(enabled);
        }
    }

    @Override
    public void switchCamera(int track) {
        LocalVideo lv;
        synchronized (this) {
            lv = videos.get(Integer.valueOf(track));
        }
        if (lv != null) {
            lv.capturer.switchCamera(null);
            lv.front = !lv.front;
        }
    }

    @Override
    public void setSpeakerphone(boolean on) {
        Object service = context.getSystemService(Context.AUDIO_SERVICE);
        if (service instanceof AudioManager) {
            AudioManager am = (AudioManager) service;
            am.setSpeakerphoneOn(on);
        }
    }

    @Override
    public void releaseTrack(int track) {
        MediaStreamTrack t;
        LocalVideo lv;
        AudioSource audioSource;
        boolean local;
        synchronized (this) {
            Integer key = Integer.valueOf(track);
            t = tracks.remove(key);
            audioSource = audioSources.remove(key);
            lv = videos.remove(key);
            local = locals.remove(key);
        }
        if (t != null && local) {
            // Remote tracks belong to their receiver and are disposed with the
            // peer connection; only the local ones are ours to free.
            t.dispose();
        }
        if (audioSource != null) {
            audioSource.dispose();
        }
        if (lv != null) {
            try {
                lv.capturer.stopCapture();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            lv.capturer.dispose();
            lv.source.dispose();
            lv.helper.dispose();
        }
    }

    @Override
    public Object createVideoView(int track, final boolean mirror) {
        MediaStreamTrack t = track(track);
        if (!(t instanceof VideoTrack)) {
            return null;
        }
        factory();
        final Sink sink = new Sink();
        sink.track = (VideoTrack) t;
        AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
            public void run() {
                SurfaceViewRenderer r = new SurfaceViewRenderer(AndroidImplementation.getActivity());
                r.init(egl.getEglBaseContext(), null);
                r.setMirror(mirror);
                r.setEnableHardwareScaler(true);
                sink.renderer = r;
            }
        });
        sink.track.addSink(sink.renderer);
        PeerComponent peer = PeerComponent.create(sink.renderer);
        synchronized (this) {
            views.put(peer, sink);
        }
        return peer;
    }

    @Override
    public void releaseVideoView(Object view) {
        final Sink sink;
        synchronized (this) {
            sink = views.remove(view);
        }
        if (sink == null) {
            return;
        }
        sink.track.removeSink(sink.renderer);
        AndroidImplementation.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                sink.renderer.release();
            }
        });
    }

    private static String stateName(PeerConnection.PeerConnectionState state) {
        switch (state) {
            case NEW:
                return "new";
            case CONNECTING:
                return "connecting";
            case CONNECTED:
                return "connected";
            case DISCONNECTED:
                return "disconnected";
            case FAILED:
                return "failed";
            default:
                return "closed";
        }
    }

    /// Forwards one peer connection's events to the API.
    private final class Observer implements PeerConnection.Observer {
        private final int handle;

        Observer(int handle) {
            this.handle = handle;
        }

        public void onSignalingChange(PeerConnection.SignalingState state) {
        }

        public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
        }

        public void onConnectionChange(PeerConnection.PeerConnectionState state) {
            WebRtc.stateChanged(handle, stateName(state));
        }

        public void onIceConnectionReceivingChange(boolean receiving) {
        }

        public void onIceGatheringChange(PeerConnection.IceGatheringState state) {
        }

        public void onIceCandidate(IceCandidate candidate) {
            WebRtc.iceCandidate(handle, candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp);
        }

        public void onIceCandidatesRemoved(IceCandidate[] candidates) {
        }

        public void onAddStream(MediaStream stream) {
        }

        public void onRemoveStream(MediaStream stream) {
        }

        public void onDataChannel(DataChannel channel) {
        }

        public void onRenegotiationNeeded() {
        }

        public void onAddTrack(RtpReceiver receiver, MediaStream[] streams) {
        }

        public void onTrack(RtpTransceiver transceiver) {
            MediaStreamTrack t = transceiver.getReceiver().track();
            if (t != null) {
                int trackHandle = register(t);
                synchronized (AndroidWebRtc.this) {
                    Peer p = peers.get(Integer.valueOf(handle));
                    if (p != null) {
                        p.remote.add(Integer.valueOf(trackHandle));
                    }
                }
                WebRtc.remoteTrack(handle, trackHandle, t instanceof VideoTrack);
            }
        }
    }
}
