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
import com.codename1.html5.js.JSObject;
import com.codename1.impl.PushAudioOutput;
import com.codename1.media.audio.AudioRenderCallback;

/// Real-time output for `com.codename1.media.audio.AudioContext` in the
/// browser. The graph renders in Java like on every other port, so a graph
/// sounds the same here as on a device; the browser only plays the result.
///
/// Each rendered block becomes a browser `AudioBuffer` started at the exact
/// end of the previous one on the browser's own clock, which joins them
/// without a seam. The render thread stays [#AHEAD_SECONDS] ahead of what the
/// browser is playing. A browser that blocks audio until the user interacts
/// keeps the context suspended; it starts with the first tap or key, as all
/// Web Audio does.
final class HTML5AudioOutput extends PushAudioOutput {
    private static final double AHEAD_SECONDS = 0.06;
    private final JSObject ctx;

    private HTML5AudioOutput(JSObject ctx, int rate, int channels, AudioRenderCallback cb) {
        super(rate, channels, cb);
        this.ctx = ctx;
    }

    static HTML5AudioOutput open(int sampleRate, int channels, AudioRenderCallback cb) {
        JSObject ctx = newContext(sampleRate);
        if (ctx == null) {
            return null;
        }
        HTML5AudioOutput out = new HTML5AudioOutput(ctx, rate(ctx), channels == 1 ? 1 : 2, cb);
        out.startThread();
        return out;
    }

    @Override
    protected int offer(float[] pcm, int offset, int frames) {
        if (ahead(ctx) > AHEAD_SECONDS) {
            return 0;
        }
        push(ctx, pcm, offset, frames, getChannels());
        return frames;
    }

    @Override
    protected int idleSleepMillis() {
        return 5;
    }

    @Override
    protected void nativeStart() {
        resume(ctx);
    }

    @Override
    protected void nativeStop() {
        suspend(ctx);
    }

    @Override
    protected void nativeClose() {
        close(ctx);
    }

    @Override
    public double getOutputLatency() {
        return AHEAD_SECONDS + latency(ctx);
    }

    @JSBody(params = {"rate"}, script =
            "try { var C = window.AudioContext || window.webkitAudioContext; if (!C) return null;"
            + " var c = rate > 0 ? new C({sampleRate: rate}) : new C();"
            + " var r = function() { try { if (c.state === 'suspended' && c.__cn1Run) c.resume(); } catch (e) {} };"
            + " window.addEventListener('pointerdown', r, true); window.addEventListener('keydown', r, true);"
            + " return c; } catch (e) { return null; }")
    private static native JSObject newContext(int rate);

    @JSBody(params = {"ctx"}, script = "return ctx.sampleRate | 0;")
    private static native int rate(JSObject ctx);

    @JSBody(params = {"ctx"}, script =
            "var t = ctx.currentTime; return ctx.__cn1End > t ? ctx.__cn1End - t : 0;")
    private static native double ahead(JSObject ctx);

    @JSBody(params = {"ctx", "pcm", "offset", "frames", "ch"}, script =
            "var b = ctx.createBuffer(ch, frames, ctx.sampleRate);"
            + " for (var c = 0; c < ch; c++) { var d = b.getChannelData(c);"
            + " for (var i = 0; i < frames; i++) { d[i] = pcm[(offset + i) * ch + c]; } }"
            + " var s = ctx.createBufferSource(); s.buffer = b; s.connect(ctx.destination);"
            + " var t = ctx.currentTime; if (!(ctx.__cn1End > t)) { ctx.__cn1End = t + 0.02; }"
            + " s.start(ctx.__cn1End); ctx.__cn1End += frames / ctx.sampleRate;")
    private static native void push(JSObject ctx, float[] pcm, int offset, int frames, int ch);

    @JSBody(params = {"ctx"}, script = "ctx.__cn1Run = true; try { ctx.resume(); } catch (e) {}")
    private static native void resume(JSObject ctx);

    @JSBody(params = {"ctx"}, script = "ctx.__cn1Run = false; try { ctx.suspend(); } catch (e) {}")
    private static native void suspend(JSObject ctx);

    @JSBody(params = {"ctx"}, script = "ctx.__cn1Run = false; try { ctx.close(); } catch (e) {}")
    private static native void close(JSObject ctx);

    @JSBody(params = {"ctx"}, script = "return (ctx.baseLatency || 0) + (ctx.outputLatency || 0);")
    private static native double latency(JSObject ctx);
}
