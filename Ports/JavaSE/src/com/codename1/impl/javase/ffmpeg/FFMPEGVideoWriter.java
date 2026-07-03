/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.javase.ffmpeg;

import com.codename1.media.VideoIO;
import com.codename1.media.VideoWriter;
import com.codename1.media.VideoWriterBuilder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/// ffmpeg backed `com.codename1.media.VideoWriter`. Video frames are streamed as raw
/// RGBA into an ffmpeg process that encodes them with the requested codec; audio is
/// accumulated as raw 16 bit PCM and, on `#close()`, muxed together with the encoded
/// video into the final container.
///
/// Because the video frames are piped as constant frame rate raw video, the JavaSE
/// backend treats each `#writeFrame` call as the next frame at the configured frame
/// rate (the per frame timestamp is honoured by the native device backends). This keeps
/// the simulator implementation simple while remaining a faithful end-to-end test of the
/// API.
class FFMPEGVideoWriter extends VideoWriter {
    private final FFMPEGVideoIO io;
    private final int width;
    private final int height;
    private final float frameRate;
    private final String container;
    private final boolean hasVideo;
    private final boolean hasAudio;
    private final String audioCodec;
    private final int audioBitRate;
    private final int configuredSampleRate;
    private final int configuredChannels;

    private final File finalOut;
    private final File videoOut;
    private final File audioRaw;

    private Process videoProcess;
    private OutputStream videoStdin;
    private final StringBuilder videoErr = new StringBuilder();
    private OutputStream audioStream;

    private int actualSampleRate = -1;
    private int actualChannels = -1;
    private long audioFramesWritten;
    private byte[] rgbaScratch;
    private boolean closed;

    FFMPEGVideoWriter(VideoWriterBuilder cfg, FFMPEGVideoIO io) throws IOException {
        this.io = io;
        this.width = cfg.getWidth();
        this.height = cfg.getHeight();
        this.frameRate = cfg.getFrameRate();
        this.container = cfg.getContainer();
        this.hasVideo = cfg.isHasVideo();
        this.hasAudio = cfg.isHasAudio();
        this.audioCodec = cfg.getAudioCodec();
        this.audioBitRate = cfg.getAudioBitRate();
        this.configuredSampleRate = cfg.getSampleRate();
        this.configuredChannels = cfg.getAudioChannels();

        if (!hasVideo && !hasAudio) {
            throw new IllegalStateException("VideoWriter must have at least a video or audio track");
        }

        this.finalOut = new File(FFMPEGSupport.normalizeSource(cfg.getPath()));
        File parent = finalOut.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        this.audioRaw = hasAudio ? new File(finalOut.getAbsolutePath() + ".cn1audio.raw") : null;
        if (hasAudio) {
            audioStream = new BufferedOutputStream(new FileOutputStream(audioRaw));
        }

        if (hasVideo) {
            String encoder = io.resolveEncoderName(cfg.getVideoCodec());
            if (encoder == null) {
                throw new IOException("No ffmpeg encoder available for video codec '" + cfg.getVideoCodec() + "'");
            }
            int bitRate = cfg.getVideoBitRate();
            if (bitRate <= 0) {
                long derived = (long) (width * (long) height * Math.max(1f, frameRate) * 0.10);
                bitRate = (int) Math.max(800000L, Math.min(derived, 100000000L));
            }
            int gop = Math.max(1, Math.round(cfg.getKeyFrameInterval() * Math.max(1f, frameRate)));
            this.videoOut = hasAudio
                    ? new File(finalOut.getAbsolutePath() + ".cn1video." + container)
                    : finalOut;

            List<String> command = new ArrayList<String>();
            command.add(FFMPEGSupport.ffmpeg());
            command.add("-y");
            command.add("-hide_banner");
            command.add("-loglevel");
            command.add("error");
            command.add("-f");
            command.add("rawvideo");
            command.add("-pixel_format");
            command.add("rgba");
            command.add("-video_size");
            command.add(width + "x" + height);
            command.add("-framerate");
            command.add(String.valueOf(frameRate));
            command.add("-i");
            command.add("-");
            command.add("-an");
            command.add("-c:v");
            command.add(encoder);
            command.add("-b:v");
            command.add(String.valueOf(bitRate));
            command.add("-pix_fmt");
            command.add("yuv420p");
            command.add("-g");
            command.add(String.valueOf(gop));
            command.add("-f");
            command.add(FFMPEGVideoIO.muxerFormat(container));
            command.add(videoOut.getAbsolutePath());
            videoProcess = FFMPEGSupport.startWithErrorCapture(command, videoErr);
            videoStdin = new BufferedOutputStream(videoProcess.getOutputStream());
        } else {
            this.videoOut = null;
        }
    }

    @Override
    public void writeFrame(int[] argb, int frameWidth, int frameHeight, long presentationTimeMillis) throws IOException {
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
        int pixels = width * height;
        if (rgbaScratch == null) {
            rgbaScratch = new byte[pixels * 4];
        }
        int o = 0;
        for (int i = 0; i < pixels; i++) {
            int p = argb[i];
            rgbaScratch[o++] = (byte) ((p >> 16) & 0xff);
            rgbaScratch[o++] = (byte) ((p >> 8) & 0xff);
            rgbaScratch[o++] = (byte) (p & 0xff);
            rgbaScratch[o++] = (byte) ((p >> 24) & 0xff);
        }
        try {
            videoStdin.write(rgbaScratch);
        } catch (IOException ex) {
            throw new IOException("ffmpeg video encoder failed: " + videoErr.toString().trim(), ex);
        }
    }

    @Override
    public void writeAudio(short[] interleavedPcm, int sampleRate, int channels, long presentationTimeMillis) throws IOException {
        if (closed) {
            throw new IOException("writer is closed");
        }
        if (!hasAudio) {
            throw new IOException("audio track is not enabled for this writer");
        }
        if (actualSampleRate < 0) {
            actualSampleRate = sampleRate;
            actualChannels = channels;
        }
        // Honour the presentation timestamp (VideoWriter documents the argument
        // as the timestamp of the first sample). The audio track is a flat s16le
        // stream, so align blocks by padding forward gaps -- an initial non-zero
        // start or a gap between blocks -- with silence, keeping audio in sync
        // with the video. Overlaps cannot be represented in a flat stream, so a
        // timestamp earlier than the current position is appended as-is.
        int ch = actualChannels > 0 ? actualChannels : Math.max(1, channels);
        long targetFrame = presentationTimeMillis * Math.max(1, actualSampleRate) / 1000L;
        if (targetFrame > audioFramesWritten) {
            long gapBytes = (targetFrame - audioFramesWritten) * ch * 2L;
            byte[] silence = new byte[4096];
            while (gapBytes > 0) {
                int w = (int) Math.min(silence.length, gapBytes);
                audioStream.write(silence, 0, w);
                gapBytes -= w;
            }
            audioFramesWritten = targetFrame;
        }
        int n = interleavedPcm.length;
        byte[] bytes = new byte[n * 2];
        int o = 0;
        for (int i = 0; i < n; i++) {
            short s = interleavedPcm[i];
            bytes[o++] = (byte) (s & 0xff);
            bytes[o++] = (byte) ((s >> 8) & 0xff);
        }
        audioStream.write(bytes);
        audioFramesWritten += n / Math.max(1, ch);
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        try {
            if (hasVideo) {
                FFMPEGSupport.closeQuietly(videoStdin);
                int code;
                try {
                    code = videoProcess.waitFor();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("ffmpeg interrupted", ex);
                }
                if (code != 0) {
                    throw new IOException("ffmpeg video encoder failed (exit " + code + "): " + videoErr.toString().trim());
                }
            }
            if (hasAudio) {
                FFMPEGSupport.closeQuietly(audioStream);
            }

            int ar = actualSampleRate > 0 ? actualSampleRate : configuredSampleRate;
            int ac = actualChannels > 0 ? actualChannels : configuredChannels;
            String muxer = FFMPEGVideoIO.muxerFormat(container);
            String audioEnc = FFMPEGVideoIO.audioEncoderName(audioCodec, io.encoderList());

            if (hasVideo && hasAudio) {
                List<String> command = new ArrayList<String>();
                command.add(FFMPEGSupport.ffmpeg());
                command.add("-y");
                command.add("-hide_banner");
                command.add("-loglevel");
                command.add("error");
                command.add("-f");
                command.add("s16le");
                command.add("-ar");
                command.add(String.valueOf(ar));
                command.add("-ac");
                command.add(String.valueOf(ac));
                command.add("-i");
                command.add(audioRaw.getAbsolutePath());
                command.add("-i");
                command.add(videoOut.getAbsolutePath());
                command.add("-map");
                command.add("1:v:0");
                command.add("-map");
                command.add("0:a:0");
                command.add("-c:v");
                command.add("copy");
                command.add("-c:a");
                command.add(audioEnc);
                command.add("-b:a");
                command.add(String.valueOf(audioBitRate));
                command.add("-shortest");
                command.add("-f");
                command.add(muxer);
                command.add(finalOut.getAbsolutePath());
                FFMPEGSupport.runChecked(command);
            } else if (!hasVideo && hasAudio) {
                List<String> command = new ArrayList<String>();
                command.add(FFMPEGSupport.ffmpeg());
                command.add("-y");
                command.add("-hide_banner");
                command.add("-loglevel");
                command.add("error");
                command.add("-f");
                command.add("s16le");
                command.add("-ar");
                command.add(String.valueOf(ar));
                command.add("-ac");
                command.add(String.valueOf(ac));
                command.add("-i");
                command.add(audioRaw.getAbsolutePath());
                command.add("-c:a");
                command.add(audioEnc);
                command.add("-b:a");
                command.add(String.valueOf(audioBitRate));
                command.add("-f");
                command.add(muxer);
                command.add(finalOut.getAbsolutePath());
                FFMPEGSupport.runChecked(command);
            }
            // hasVideo && !hasAudio: the video process already wrote finalOut directly.
        } finally {
            FFMPEGSupport.destroyQuietly(videoProcess);
            if (videoOut != null && hasAudio && videoOut.exists()) {
                videoOut.delete();
            }
            if (audioRaw != null && audioRaw.exists()) {
                audioRaw.delete();
            }
        }
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public float getFrameRate() {
        return frameRate;
    }
}
