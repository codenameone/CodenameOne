/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import com.codename1.html5.js.JSBody;
import com.codename1.io.Util;
import com.codename1.media.AudioBuffer;
import com.codename1.media.VideoCodec;
import com.codename1.media.VideoFrame;
import com.codename1.media.VideoIO;
import com.codename1.media.VideoReader;
import com.codename1.media.VideoWriter;
import com.codename1.media.VideoWriterBuilder;
import com.codename1.ui.Image;
import com.codename1.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/// JavaScript `com.codename1.media.VideoIO` implementation.
///
/// **Decoding** uses an HTML5 `<video>` element seeked to the requested time, with each
/// frame captured from a `<canvas>` and decoded back through Codename One's image
/// pipeline (so pixels arrive as ARGB `int[]`). The green thread yields on `Thread.sleep`,
/// which lets the browser's asynchronous `seeked`/`loadedmetadata` events fire while a
/// decode call appears synchronous to the caller.
///
/// **Encoding** uses the WebCodecs `VideoEncoder`/`AudioEncoder` (H.264/VP9 + AAC/Opus)
/// muxed into MP4/WebM with the standalone `mp4-muxer`/`webm-muxer` libraries (loaded
/// from a CDN, the same mechanism the port uses for VideoJS). Frames are pushed as RGBA,
/// the muxer's in-memory buffer is handed back to Java on `close()` and written to the
/// output file. Encoding requires a browser with WebCodecs; `getAvailableEncoders()` is
/// empty and `createWriter` throws where it is unavailable.
class HTML5VideoIO extends VideoIO {
    @Override
    public VideoCodec[] getAvailableEncoders() {
        if (!cn1WebCodecsAvailable()) {
            return new VideoCodec[0];
        }
        List<VideoCodec> out = new ArrayList<VideoCodec>();
        out.add(new VideoCodec(CODEC_H264, "H.264 (WebCodecs)", "video/avc", true, true, false, true, -1, -1, new String[]{CONTAINER_MP4}));
        out.add(new VideoCodec(CODEC_VP9, "VP9 (WebCodecs)", "video/vp9", true, true, false, true, -1, -1, new String[]{CONTAINER_WEBM}));
        out.add(new VideoCodec(CODEC_AAC, "AAC (WebCodecs)", "audio/mp4a-latm", false, true, false, false, -1, -1, new String[]{CONTAINER_MP4}));
        out.add(new VideoCodec(CODEC_OPUS, "Opus (WebCodecs)", "audio/opus", false, true, false, false, -1, -1, new String[]{CONTAINER_WEBM}));
        return out.toArray(new VideoCodec[out.size()]);
    }

    @Override
    public VideoCodec[] getAvailableDecoders() {
        List<VideoCodec> out = new ArrayList<VideoCodec>();
        String[] both = new String[]{CONTAINER_MP4, CONTAINER_WEBM};
        out.add(new VideoCodec(CODEC_H264, "H.264 (HTML5)", "video/avc", true, false, true, true, -1, -1, both));
        out.add(new VideoCodec(CODEC_VP8, "VP8 (HTML5)", "video/vp8", true, false, true, true, -1, -1, new String[]{CONTAINER_WEBM}));
        out.add(new VideoCodec(CODEC_VP9, "VP9 (HTML5)", "video/vp9", true, false, true, true, -1, -1, new String[]{CONTAINER_WEBM}));
        return out.toArray(new VideoCodec[out.size()]);
    }

    @Override
    public VideoWriter createWriter(VideoWriterBuilder cfg) throws IOException {
        if (!cn1WebCodecsAvailable()) {
            throw new IOException("Video encoding requires WebCodecs, which this browser does not support");
        }
        return new Writer(cfg);
    }

    @Override
    public VideoReader openReader(String filePath) throws IOException {
        return new Reader(filePath);
    }

    @Override
    public VideoReader openReader(InputStream stream, String mimeType) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Util.copy(stream, bos);
        String b64 = Base64.encodeNoNewline(bos.toByteArray());
        String url = cn1VideoBlobUrl(b64, mimeType == null ? "video/mp4" : mimeType);
        return new Reader(url);
    }

    private static void sleep() {
        try {
            Thread.sleep(5);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================================================================== reader

    static final class Reader extends VideoReader {
        private final int id;
        private final int width, height;
        private final long duration;

        Reader(String url) throws IOException {
            this.id = cn1VideoOpen(url);
            if (id == 0) {
                throw new IOException("Failed to open video: " + url);
            }
            long start = System.currentTimeMillis();
            while (!cn1VideoReady(id)) {
                if (System.currentTimeMillis() - start > 15000) {
                    cn1VideoClose(id);
                    throw new IOException("Timed out loading video: " + url);
                }
                sleep();
            }
            this.width = cn1VideoWidth(id);
            this.height = cn1VideoHeight(id);
            this.duration = cn1VideoDuration(id);
        }

        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public long getDurationMillis() { return duration; }
        public float getFrameRate() { return 30f; }
        public boolean hasVideo() { return width > 0 && height > 0; }
        public boolean hasAudio() { return false; }
        public int getAudioSampleRate() { return -1; }
        public int getAudioChannels() { return -1; }

        public VideoFrame frameAt(long millis) throws IOException {
            if (!hasVideo()) {
                return null;
            }
            cn1VideoSeek(id, (int) Math.max(0, millis));
            long start = System.currentTimeMillis();
            while (!cn1VideoSeeked(id)) {
                if (System.currentTimeMillis() - start > 10000) {
                    return null;
                }
                sleep();
            }
            String dataUrl = cn1VideoCapture(id, width, height);
            if (dataUrl == null) {
                return null;
            }
            int comma = dataUrl.indexOf(',');
            String b64 = comma >= 0 ? dataUrl.substring(comma + 1) : dataUrl;
            byte[] png = Base64.decode(b64.getBytes());
            Image img = Image.createImage(png, 0, png.length);
            return new VideoFrame(img.getRGB(), width, height, millis);
        }

        public void readFrames(float fps, FrameCallback callback) throws IOException {
            if (!hasVideo()) {
                return;
            }
            if (fps <= 0f) {
                throw new IllegalArgumentException("fps must be positive");
            }
            long step = Math.max(1, Math.round(1000.0 / fps));
            for (long t = 0; duration <= 0 || t < duration; t += step) {
                VideoFrame f = frameAt(t);
                if (f == null || !callback.frame(f) || duration <= 0) {
                    break;
                }
            }
        }

        public AudioBuffer readAudio() throws IOException {
            return null;
        }

        public void close() throws IOException {
            cn1VideoClose(id);
        }
    }

    // ==================================================================== writer

    static final class Writer extends VideoWriter {
        private final int peer;
        private final int width, height;
        private final float frameRate;
        private final boolean hasVideo, hasAudio;
        private final String outPath;
        private boolean closed;

        Writer(VideoWriterBuilder cfg) throws IOException {
            this.width = cfg.getWidth();
            this.height = cfg.getHeight();
            this.frameRate = cfg.getFrameRate();
            this.hasVideo = cfg.isHasVideo();
            this.hasAudio = cfg.isHasAudio();
            this.outPath = cfg.getPath();

            cn1EncEnsureLibs();
            long start = System.currentTimeMillis();
            while (!cn1EncLibsReady(cfg.getContainer())) {
                if (System.currentTimeMillis() - start > 20000) {
                    throw new IOException("Timed out loading the WebCodecs muxer library");
                }
                sleep();
            }
            int br = cfg.getVideoBitRate();
            if (br <= 0) {
                br = (int) Math.max(800000L, Math.min((long) (width * (long) height * Math.max(1f, frameRate) * 0.10), 100000000L));
            }
            this.peer = cn1EncOpen(cfg.getContainer(), cfg.getVideoCodec(), width, height, Math.round(frameRate), br,
                    hasAudio, cfg.getAudioCodec(), cfg.getAudioBitRate(), cfg.getSampleRate(), cfg.getAudioChannels());
            if (peer == 0) {
                throw new IOException("Failed to configure the WebCodecs encoder: " + safeError(0));
            }
            String err = cn1EncError(peer);
            if (err != null) {
                cn1EncClose(peer);
                throw new IOException("WebCodecs encoder error: " + err);
            }
        }

        public void writeFrame(int[] argb, int frameWidth, int frameHeight, long pts) throws IOException {
            if (closed) {
                throw new IOException("writer is closed");
            }
            if (!hasVideo) {
                throw new IOException("video track is not enabled for this writer");
            }
            if (frameWidth != width || frameHeight != height) {
                throw new IllegalArgumentException("frame is " + frameWidth + "x" + frameHeight
                        + " but writer was configured for " + width + "x" + height);
            }
            byte[] rgba = new byte[width * height * 4];
            int o = 0;
            for (int i = 0; i < argb.length; i++) {
                int p = argb[i];
                rgba[o++] = (byte) ((p >> 16) & 0xff);
                rgba[o++] = (byte) ((p >> 8) & 0xff);
                rgba[o++] = (byte) (p & 0xff);
                rgba[o++] = (byte) ((p >> 24) & 0xff);
            }
            cn1EncFrame(peer, Base64.encodeNoNewline(rgba), width, height, (double) (Math.max(0, pts) * 1000L));
            checkError();
        }

        public void writeAudio(short[] interleavedPcm, int sampleRate, int channels, long pts) throws IOException {
            if (closed) {
                throw new IOException("writer is closed");
            }
            if (!hasAudio) {
                throw new IOException("audio track is not enabled for this writer");
            }
            byte[] bytes = new byte[interleavedPcm.length * 2];
            int o = 0;
            for (int i = 0; i < interleavedPcm.length; i++) {
                short s = interleavedPcm[i];
                bytes[o++] = (byte) (s & 0xff);
                bytes[o++] = (byte) ((s >> 8) & 0xff);
            }
            cn1EncAudio(peer, Base64.encodeNoNewline(bytes), sampleRate, channels, (double) (Math.max(0, pts) * 1000L));
            checkError();
        }

        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            try {
                cn1EncFlush(peer);
                long start = System.currentTimeMillis();
                while (!cn1EncDone(peer)) {
                    if (System.currentTimeMillis() - start > 60000) {
                        throw new IOException("Timed out finalizing the encoded video");
                    }
                    sleep();
                }
                String err = cn1EncError(peer);
                if (err != null) {
                    throw new IOException("WebCodecs encoder error: " + err);
                }
                String b64 = cn1EncResult(peer);
                if (b64 == null) {
                    throw new IOException("Encoder produced no output");
                }
                byte[] data = Base64.decode(b64.getBytes());
                OutputStream os = null; //NOPMD
                try {
                    os = com.codename1.io.FileSystemStorage.getInstance().openOutputStream(outPath);
                    os.write(data);
                } finally {
                    Util.cleanup(os);
                }
            } finally {
                cn1EncClose(peer);
            }
        }

        private void checkError() throws IOException {
            String err = cn1EncError(peer);
            if (err != null) {
                throw new IOException("WebCodecs encoder error: " + err);
            }
        }

        private static String safeError(int peer) {
            try {
                String e = cn1EncError(peer);
                return e == null ? "unknown" : e;
            } catch (Throwable t) {
                return "unknown";
            }
        }

        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public float getFrameRate() { return frameRate; }
    }

    // ============================================================= JS: feature

    @JSBody(params = {}, script = "return (typeof VideoEncoder !== 'undefined' && typeof VideoDecoder !== 'undefined');")
    private static native boolean cn1WebCodecsAvailable();

    // ============================================================= JS: decoder

    @JSBody(params = {"url"}, script =
            "var v = document.createElement('video');"
            + "v.muted = true; v.crossOrigin = 'anonymous'; v.preload = 'auto'; v.src = url;"
            + "var id = (window.__cn1vidSeq = (window.__cn1vidSeq || 0) + 1);"
            + "var s = { el: v, ready: false, seeked: false };"
            + "window['__cn1video_' + id] = s;"
            + "v.addEventListener('loadedmetadata', function() { s.ready = true; });"
            + "v.addEventListener('seeked', function() { s.seeked = true; });"
            + "v.load();"
            + "return id;")
    private static native int cn1VideoOpen(String url);

    @JSBody(params = {"id"}, script = "var s = window['__cn1video_' + id]; return s ? !!s.ready : false;")
    private static native boolean cn1VideoReady(int id);

    @JSBody(params = {"id"}, script = "var s = window['__cn1video_' + id]; return s ? (s.el.videoWidth | 0) : 0;")
    private static native int cn1VideoWidth(int id);

    @JSBody(params = {"id"}, script = "var s = window['__cn1video_' + id]; return s ? (s.el.videoHeight | 0) : 0;")
    private static native int cn1VideoHeight(int id);

    @JSBody(params = {"id"}, script = "var s = window['__cn1video_' + id]; return s ? Math.round((s.el.duration || 0) * 1000) : 0;")
    private static native int cn1VideoDuration(int id);

    @JSBody(params = {"id", "ms"}, script =
            "var s = window['__cn1video_' + id]; if (s) {"
            + "  var t = ms / 1000;"
            + "  if (Math.abs((s.el.currentTime || 0) - t) < 0.001) {"
            // No-op seek (e.g. the first frame at t=0, or the same timestamp
            // twice): browsers are not required to fire a 'seeked' event, so
            // waiting for one would hang. Treat it as done as soon as the frame
            // at the current position is decodable (readyState>=HAVE_CURRENT_DATA);
            // cn1VideoSeeked() re-checks readyState until then.
            + "    s.sameTime = true; s.seeked = (s.el.readyState >= 2);"
            + "  } else {"
            + "    s.sameTime = false; s.seeked = false; s.el.currentTime = t;"
            + "  }"
            + "}")
    private static native void cn1VideoSeek(int id, int ms);

    @JSBody(params = {"id"}, script = "var s = window['__cn1video_' + id];"
            + "if (!s) { return false; }"
            + "if (s.seeked) { return true; }"
            // A no-op seek that had not buffered a frame yet completes once the
            // current frame becomes available -- no 'seeked' event will arrive.
            + "if (s.sameTime && s.el.readyState >= 2) { s.seeked = true; return true; }"
            + "return false;")
    private static native boolean cn1VideoSeeked(int id);

    @JSBody(params = {"id", "w", "h"}, script =
            "var s = window['__cn1video_' + id]; if (!s) { return null; }"
            + "var c = document.createElement('canvas'); c.width = w; c.height = h;"
            + "var ctx = c.getContext('2d'); ctx.drawImage(s.el, 0, 0, w, h);"
            + "return c.toDataURL('image/png');")
    private static native String cn1VideoCapture(int id, int w, int h);

    @JSBody(params = {"id"}, script =
            "var s = window['__cn1video_' + id];"
            + "if (s) { try { s.el.removeAttribute('src'); s.el.load(); } catch (e) {} delete window['__cn1video_' + id]; }")
    private static native void cn1VideoClose(int id);

    @JSBody(params = {"b64", "mime"}, script =
            "var bin = atob(b64); var len = bin.length; var bytes = new Uint8Array(len);"
            + "for (var i = 0; i < len; i++) { bytes[i] = bin.charCodeAt(i); }"
            + "var blob = new Blob([bytes], { type: mime });"
            + "return URL.createObjectURL(blob);")
    private static native String cn1VideoBlobUrl(String b64, String mime);

    // ============================================================= JS: encoder

    /// Injects the mp4-muxer / webm-muxer UMD bundles (idempotent).
    @JSBody(params = {}, script =
            "if (!window.__cn1MuxLoad) {"
            + "  window.__cn1MuxLoad = true;"
            + "  var add = function(src) { var t = document.createElement('script'); t.src = src; t.async = false; document.head.appendChild(t); };"
            + "  add('https://cdn.jsdelivr.net/npm/mp4-muxer@5.1.5/build/mp4-muxer.min.js');"
            + "  add('https://cdn.jsdelivr.net/npm/webm-muxer@5.0.3/build/webm-muxer.min.js');"
            + "}")
    private static native void cn1EncEnsureLibs();

    @JSBody(params = {"container"}, script =
            "if (container === 'webm') { return typeof WebMMuxer !== 'undefined'; }"
            + "return typeof Mp4Muxer !== 'undefined';")
    private static native boolean cn1EncLibsReady(String container);

    /// Creates the muxer + encoders. Returns a peer id, or 0 on failure (see cn1EncError).
    @JSBody(params = {"container", "videoCodec", "w", "h", "fps", "videoBitRate", "hasAudio", "audioCodec", "audioBitRate", "sampleRate", "channels"},
            script =
            "try {"
            + "  var webm = (container === 'webm');"
            + "  var MUX = webm ? WebMMuxer : Mp4Muxer;"
            + "  var vmux, vcfg, amux, acfg;"
            + "  if (webm) {"
            + "    vmux = (videoCodec === 'vp8') ? 'V_VP8' : 'V_VP9';"
            + "    vcfg = (videoCodec === 'vp8') ? 'vp8' : 'vp09.00.10.08';"
            + "    amux = 'A_OPUS'; acfg = 'opus';"
            + "  } else {"
            + "    vmux = (videoCodec === 'hevc') ? 'hevc' : 'avc';"
            + "    vcfg = (videoCodec === 'hevc') ? 'hev1.1.6.L93.B0' : 'avc1.42001f';"
            + "    amux = 'aac'; acfg = 'mp4a.40.2';"
            + "  }"
            + "  var target = new MUX.ArrayBufferTarget();"
            + "  var muxOpts = { target: target, video: { codec: vmux, width: w, height: h }, firstTimestampBehavior: 'offset' };"
            + "  if (!webm) { muxOpts.fastStart = 'in-memory'; }"
            + "  if (hasAudio) { muxOpts.audio = { codec: amux, sampleRate: sampleRate, numberOfChannels: channels }; }"
            + "  var muxer = new MUX.Muxer(muxOpts);"
            + "  var peer = { muxer: muxer, target: target, error: null, done: false, result: null };"
            + "  peer.videoEncoder = new VideoEncoder({"
            + "    output: function(chunk, meta) { try { muxer.addVideoChunk(chunk, meta); } catch (e) { peer.error = '' + e; } },"
            + "    error: function(e) { peer.error = '' + e; } });"
            + "  peer.videoEncoder.configure({ codec: vcfg, width: w, height: h, bitrate: videoBitRate, framerate: fps,"
            + "      avc: webm ? undefined : { format: 'avc' } });"
            + "  if (hasAudio) {"
            + "    peer.audioEncoder = new AudioEncoder({"
            + "      output: function(chunk, meta) { try { muxer.addAudioChunk(chunk, meta); } catch (e) { peer.error = '' + e; } },"
            + "      error: function(e) { peer.error = '' + e; } });"
            + "    peer.audioEncoder.configure({ codec: acfg, sampleRate: sampleRate, numberOfChannels: channels, bitrate: audioBitRate });"
            + "  }"
            + "  var id = (window.__cn1encSeq = (window.__cn1encSeq || 0) + 1);"
            + "  window['__cn1enc_' + id] = peer;"
            + "  return id;"
            + "} catch (e) {"
            + "  window.__cn1encLastError = '' + e;"
            + "  return 0;"
            + "}")
    private static native int cn1EncOpen(String container, String videoCodec, int w, int h, int fps, int videoBitRate,
            boolean hasAudio, String audioCodec, int audioBitRate, int sampleRate, int channels);

    @JSBody(params = {"peer"}, script =
            "if (peer === 0) { return window.__cn1encLastError || null; }"
            + "var p = window['__cn1enc_' + peer]; return p ? p.error : null;")
    private static native String cn1EncError(int peer);

    @JSBody(params = {"peer", "b64", "w", "h", "ptsUs"}, script =
            "var p = window['__cn1enc_' + peer]; if (!p) { return; }"
            + "try {"
            + "  var bin = atob(b64); var n = bin.length; var bytes = new Uint8Array(n);"
            + "  for (var i = 0; i < n; i++) { bytes[i] = bin.charCodeAt(i); }"
            + "  var frame = new VideoFrame(bytes, { format: 'RGBA', codedWidth: w, codedHeight: h, timestamp: ptsUs });"
            + "  p.videoEncoder.encode(frame); frame.close();"
            + "} catch (e) { p.error = '' + e; }")
    private static native void cn1EncFrame(int peer, String b64, int w, int h, double ptsUs);

    @JSBody(params = {"peer", "b64", "sampleRate", "channels", "ptsUs"}, script =
            "var p = window['__cn1enc_' + peer]; if (!p || !p.audioEncoder) { return; }"
            + "try {"
            + "  var bin = atob(b64); var n = bin.length; var bytes = new Uint8Array(n);"
            + "  for (var i = 0; i < n; i++) { bytes[i] = bin.charCodeAt(i); }"
            + "  var samples = new Int16Array(bytes.buffer);"
            + "  var ad = new AudioData({ format: 's16', sampleRate: sampleRate, numberOfFrames: (samples.length / channels) | 0,"
            + "      numberOfChannels: channels, timestamp: ptsUs, data: samples });"
            + "  p.audioEncoder.encode(ad); ad.close();"
            + "} catch (e) { p.error = '' + e; }")
    private static native void cn1EncAudio(int peer, String b64, int sampleRate, int channels, double ptsUs);

    @JSBody(params = {"peer"}, script =
            "var p = window['__cn1enc_' + peer]; if (!p) { return; }"
            + "p.done = false;"
            + "(async function() {"
            + "  try {"
            + "    await p.videoEncoder.flush();"
            + "    if (p.audioEncoder) { await p.audioEncoder.flush(); }"
            + "    p.muxer.finalize();"
            + "    p.result = p.target.buffer;"
            + "  } catch (e) { p.error = '' + e; }"
            + "  p.done = true;"
            + "})();")
    private static native void cn1EncFlush(int peer);

    @JSBody(params = {"peer"}, script = "var p = window['__cn1enc_' + peer]; return p ? !!p.done : true;")
    private static native boolean cn1EncDone(int peer);

    @JSBody(params = {"peer"}, script =
            "var p = window['__cn1enc_' + peer]; if (!p || !p.result) { return null; }"
            + "var bytes = new Uint8Array(p.result); var out = ''; var CH = 0x8000;"
            + "for (var i = 0; i < bytes.length; i += CH) {"
            + "  out += String.fromCharCode.apply(null, bytes.subarray(i, Math.min(i + CH, bytes.length)));"
            + "}"
            + "return btoa(out);")
    private static native String cn1EncResult(int peer);

    @JSBody(params = {"peer"}, script =
            "var p = window['__cn1enc_' + peer]; if (!p) { return; }"
            + "try { if (p.videoEncoder && p.videoEncoder.state !== 'closed') { p.videoEncoder.close(); } } catch (e) {}"
            + "try { if (p.audioEncoder && p.audioEncoder.state !== 'closed') { p.audioEncoder.close(); } } catch (e) {}"
            + "delete window['__cn1enc_' + peer];")
    private static native void cn1EncClose(int peer);
}
