/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSFunctor;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.typedarrays.ArrayBuffer;
import com.codename1.io.Util;
import com.codename1.media.CompletionAwareSoundPoolPeer;
import com.codename1.media.VoiceCompletionListener;
import com.codename1.teavm.io.BlobUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import static com.codename1.ui.CN.invokeAndBlock;

/// Low latency `com.codename1.media.SoundPoolPeer` for the JavaScript port, built on
/// the browser's WebAudio API.
///
/// Each clip is decoded once to an `AudioBuffer` (PCM in memory) at load time via
/// `AudioContext.decodeAudioData`, so playback pays no decode cost. A play wires up a
/// fresh `AudioBufferSourceNode -> GainNode -> StereoPannerNode -> destination` graph
/// -- giving true per voice volume, stereo pan and playback rate -- and starts it
/// immediately. This is dramatically lower latency than the `MediaSoundPoolPeer`
/// fallback (which drives hidden `<audio>` elements), which is why
/// `com.codename1.gaming.SoundPool#isNativeAccelerated()` reports {@code true} here.
///
/// WebAudio voices are fire and forget: a source node cannot be paused and resumed
/// once started, so `#pauseVoice(int)` stops the voice and `#resumeVoice(int)` is a
/// no-op. Whole pool suspend/resume (`#autoPause()` / `#autoResume()`), used when the
/// tab is backgrounded, is supported through `AudioContext.suspend()`/`resume()`.
class WebAudioSoundPool implements CompletionAwareSoundPoolPeer {
    private final HTML5Implementation impl;
    private final JSObject ctx;
    private final int maxStreams;
    private final Map<Integer, Voice> voices = new HashMap<Integer, Voice>();
    private int nextVoiceId = 1;
    private int activeVoices;
    private VoiceCompletionListener completionListener;

    /// A loaded clip: its decoded PCM buffer.
    private static final class Sound {
        JSObject buffer;     // AudioBuffer
    }

    /// A playing voice: the node graph plus the bookkeeping needed to tell a natural
    /// end (fire completion) from an explicit stop (do not).
    private static final class Voice {
        JSObject graph;      // {src, gain, pan}
        int voiceId;
        boolean stopped;
    }

    /// Receives the decoded `AudioBuffer` from `decodeAudioData`.
    @JSFunctor
    private interface AudioBufferReceiver extends JSObject {
        void accept(JSObject buffer);
    }

    /// Fired by a source node's {@code onended}.
    @JSFunctor
    private interface EndedHandler extends JSObject {
        void onended();
    }

    private WebAudioSoundPool(HTML5Implementation impl, JSObject ctx, int maxStreams) {
        this.impl = impl;
        this.ctx = ctx;
        this.maxStreams = maxStreams < 1 ? 1 : maxStreams;
    }

    /// Creates a WebAudio pool, or returns null if WebAudio is unavailable (in which
    /// case `com.codename1.gaming.SoundPool` uses the cross platform fallback).
    static WebAudioSoundPool tryCreate(HTML5Implementation impl, int maxStreams) {
        try {
            JSObject ctx = newAudioContext();
            if (ctx == null) {
                return null;
            }
            return new WebAudioSoundPool(impl, ctx, maxStreams);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public synchronized void setVoiceCompletionListener(VoiceCompletionListener listener) {
        this.completionListener = listener;
    }

    @Override
    public Object loadSound(InputStream data, String mimeType) throws IOException {
        byte[] bytes = Util.readInputStream(data);
        Util.cleanup(data);
        return decode(bytes);
    }

    @Override
    public Object loadSound(String uri) throws IOException {
        InputStream in = impl.getResourceAsStream(null, uri);
        if (in == null) {
            throw new IOException("Sound resource not found: " + uri);
        }
        byte[] bytes = Util.readInputStream(in);
        Util.cleanup(in);
        return decode(bytes);
    }

    /// Decodes raw encoded audio bytes to an `AudioBuffer`, blocking the calling
    /// (green) thread until the asynchronous decode resolves -- loads happen up front,
    /// not on the hot path, so blocking here is fine.
    private Sound decode(byte[] bytes) throws IOException {
        final ArrayBuffer ab = BlobUtil.byteArrayToUint8Array(bytes).getBuffer();
        final JSObject[] result = new JSObject[1];
        final boolean[] done = new boolean[1];
        final boolean[] failed = new boolean[1];
        AudioBufferReceiver ok = new AudioBufferReceiver() {
            @Override
            public void accept(final JSObject buffer) {
                new Thread() {
                    public void run() {
                        synchronized (done) {
                            result[0] = buffer;
                            done[0] = true;
                            done.notify();
                        }
                    }
                }.start();
            }
        };
        EndedHandler err = new EndedHandler() {
            @Override
            public void onended() {
                new Thread() {
                    public void run() {
                        synchronized (done) {
                            failed[0] = true;
                            done[0] = true;
                            done.notify();
                        }
                    }
                }.start();
            }
        };
        decodeAudioData(ctx, ab, ok, err);
        final long deadline = System.currentTimeMillis() + 8000;
        while (!done[0] && System.currentTimeMillis() < deadline) {
            invokeAndBlock(new Runnable() {
                public void run() {
                    synchronized (done) {
                        Util.wait(done, 250);
                    }
                }
            });
        }
        if (failed[0] || result[0] == null) {
            throw new IOException("WebAudio failed to decode sound");
        }
        Sound s = new Sound();
        s.buffer = result[0];
        return s;
    }

    @Override
    public synchronized int play(Object soundHandle, float volume, float pan, float rate, int loop) {
        if (activeVoices >= maxStreams) {
            return -1;
        }
        Sound s = (Sound) soundHandle;
        if (s == null || s.buffer == null) {
            return -1;
        }
        // browsers start the context suspended until a user gesture; nudge it
        resumeContext(ctx);
        final int vid = nextVoiceId++;
        final Voice v = new Voice();
        v.voiceId = vid;
        EndedHandler onEnded = new EndedHandler() {
            @Override
            public void onended() {
                voiceEnded(v);
            }
        };
        v.graph = startVoice(ctx, s.buffer, volume, pan, rate, loop == -1, onEnded);
        voices.put(Integer.valueOf(vid), v);
        activeVoices++;
        return vid;
    }

    /// Called from a source node's onended (natural end or our own stop).
    private void voiceEnded(Voice v) {
        VoiceCompletionListener notify = null;
        synchronized (this) {
            if (voices.remove(Integer.valueOf(v.voiceId)) == null) {
                return;
            }
            activeVoices--;
            if (!v.stopped) {
                notify = completionListener;
            }
        }
        if (notify != null) {
            notify.onVoiceComplete(v.voiceId);
        }
    }

    @Override
    public synchronized void setVolume(int voiceId, float volume) {
        Voice v = voices.get(Integer.valueOf(voiceId));
        if (v != null) {
            setVoiceVolume(v.graph, volume);
        }
    }

    @Override
    public synchronized void setRate(int voiceId, float rate) {
        Voice v = voices.get(Integer.valueOf(voiceId));
        if (v != null) {
            setVoiceRate(v.graph, rate);
        }
    }

    @Override
    public synchronized void setPan(int voiceId, float pan) {
        Voice v = voices.get(Integer.valueOf(voiceId));
        if (v != null) {
            setVoicePan(v.graph, pan);
        }
    }

    @Override
    public void pauseVoice(int voiceId) {
        // WebAudio source nodes cannot pause/resume; stop the voice instead.
        stopVoice(voiceId);
    }

    @Override
    public void resumeVoice(int voiceId) {
        // no-op: a stopped one shot voice cannot be resumed
    }

    @Override
    public synchronized void stopVoice(int voiceId) {
        Voice v = voices.get(Integer.valueOf(voiceId));
        if (v != null) {
            v.stopped = true;
            stopVoiceNative(v.graph);   // fires onended -> voiceEnded cleans up
        }
    }

    @Override
    public synchronized void stopAll() {
        for (Voice v : voices.values()) {
            v.stopped = true;
            stopVoiceNative(v.graph);
        }
    }

    @Override
    public void autoPause() {
        suspendContext(ctx);
    }

    @Override
    public void autoResume() {
        resumeContext(ctx);
    }

    @Override
    public void unloadSound(Object soundHandle) {
        Sound s = (Sound) soundHandle;
        if (s != null) {
            s.buffer = null;
        }
    }

    @Override
    public synchronized void release() {
        stopAll();
        voices.clear();
        activeVoices = 0;
        closeContext(ctx);
    }

    // ---- WebAudio bindings ----------------------------------------------------

    @JSBody(params = {}, script =
            "try { var C = window.AudioContext || window.webkitAudioContext;"
            + " return C ? new C() : null; } catch (e) { return null; }")
    private static native JSObject newAudioContext();

    @JSBody(params = {"ctx", "data", "ok", "err"}, script =
            "try { ctx.decodeAudioData(data.slice(0),"
            + " function(b){ ok(b); }, function(e){ err(); }); }"
            + " catch (e) { err(); }")
    private static native void decodeAudioData(JSObject ctx, ArrayBuffer data,
            AudioBufferReceiver ok, EndedHandler err);

    @JSBody(params = {"ctx", "buffer", "volume", "pan", "rate", "loop", "onended"}, script =
            "var s = ctx.createBufferSource(); s.buffer = buffer;"
            + " s.playbackRate.value = rate; s.loop = loop;"
            + " var g = ctx.createGain(); g.gain.value = volume;"
            + " var p = ctx.createStereoPanner ? ctx.createStereoPanner() : null;"
            + " if (p) { p.pan.value = pan; s.connect(g); g.connect(p); p.connect(ctx.destination); }"
            + " else { s.connect(g); g.connect(ctx.destination); }"
            + " s.onended = function(){ onended(); };"
            + " try { s.start(0); } catch (e) {}"
            + " return { src: s, gain: g, pan: p };")
    private static native JSObject startVoice(JSObject ctx, JSObject buffer,
            double volume, double pan, double rate, boolean loop, EndedHandler onended);

    @JSBody(params = {"v", "volume"}, script = "if (v && v.gain) v.gain.gain.value = volume;")
    private static native void setVoiceVolume(JSObject v, double volume);

    @JSBody(params = {"v", "rate"}, script = "if (v && v.src) v.src.playbackRate.value = rate;")
    private static native void setVoiceRate(JSObject v, double rate);

    @JSBody(params = {"v", "pan"}, script = "if (v && v.pan) v.pan.pan.value = pan;")
    private static native void setVoicePan(JSObject v, double pan);

    @JSBody(params = {"v"}, script = "try { if (v && v.src) v.src.stop(0); } catch (e) {}")
    private static native void stopVoiceNative(JSObject v);

    @JSBody(params = {"ctx"}, script = "try { if (ctx.state === 'suspended') ctx.resume(); } catch (e) {}")
    private static native void resumeContext(JSObject ctx);

    @JSBody(params = {"ctx"}, script = "try { ctx.suspend(); } catch (e) {}")
    private static native void suspendContext(JSObject ctx);

    @JSBody(params = {"ctx"}, script = "try { ctx.close(); } catch (e) {}")
    private static native void closeContext(JSObject ctx);
}
