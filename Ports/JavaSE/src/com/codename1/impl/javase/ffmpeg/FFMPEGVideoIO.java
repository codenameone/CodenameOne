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

import com.codename1.media.VideoCodec;
import com.codename1.media.VideoIO;
import com.codename1.media.VideoReader;
import com.codename1.media.VideoWriter;
import com.codename1.media.VideoWriterBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// `com.codename1.media.VideoIO` implementation for the JavaSE simulator backed by the
/// `ffmpeg`/`ffprobe` executables (resolved by `FFMPEGMedia`, which bundles a binary via
/// the optional `org.bytedeco:ffmpeg-platform` dependency). This is a fully functional
/// reference implementation: it encodes application frames + audio, decodes clips to
/// frame accurate RGBA frames + PCM, and enumerates the codecs the local ffmpeg build
/// exposes.
public class FFMPEGVideoIO extends VideoIO {
    private static final String[] HARDWARE_MARKERS = {
        "videotoolbox", "nvenc", "nvdec", "cuvid", "qsv", "vaapi", "vdpau",
        "amf", "v4l2m2m", "mediacodec", "_mf", "mmal", "omx"
    };

    private VideoCodec[] encoders;
    private VideoCodec[] decoders;

    /// True when the ffmpeg and ffprobe executables are available, in which case the
    /// JavaSE port exposes this implementation.
    public static boolean isAvailable() {
        return FFMPEGMedia.isConfigured();
    }

    @Override
    public synchronized VideoCodec[] getAvailableEncoders() {
        if (encoders == null) {
            encoders = enumerate(true);
        }
        return encoders.clone();
    }

    @Override
    public synchronized VideoCodec[] getAvailableDecoders() {
        if (decoders == null) {
            decoders = enumerate(false);
        }
        return decoders.clone();
    }

    @Override
    public VideoWriter createWriter(VideoWriterBuilder cfg) throws IOException {
        return new FFMPEGVideoWriter(cfg, this);
    }

    @Override
    public VideoReader openReader(String filePath) throws IOException {
        return new FFMPEGVideoReader(filePath);
    }

    /// Resolves a concrete ffmpeg encoder executable name (e.g. {@code libx264} or
    /// {@code h264_videotoolbox}) for the supplied canonical codec id. Apple Silicon
    /// prefers VideoToolbox unless {@code video.hardwareEncoding=false}; other hosts
    /// retain the software-first behavior. {@code video.encoder} may name an exact
    /// encoder exposed for this codec.
    String resolveEncoderName(String codecId) {
        VideoCodec software = null;
        VideoCodec hardware = null;
        VideoCodec videoToolbox = null;
        String forced = System.getProperty("video.encoder", "").trim();
        for (VideoCodec c : getAvailableEncoders()) {
            if (codecId.equals(c.getId())) {
                if (!forced.isEmpty() && forced.equals(c.getName())) {
                    return c.getName();
                }
                if (c.isHardwareAccelerated()) {
                    if (hardware == null) {
                        hardware = c;
                    }
                    if (c.getName().contains("videotoolbox")) {
                        videoToolbox = c;
                    }
                } else if (software == null) {
                    software = c;
                }
            }
        }
        boolean appleSilicon = System.getProperty("os.name", "").toLowerCase().contains("mac")
                && ("aarch64".equals(System.getProperty("os.arch"))
                    || "arm64".equals(System.getProperty("os.arch")));
        boolean preferVideoToolbox = appleSilicon
                && !"false".equals(System.getProperty("video.hardwareEncoding"));
        VideoCodec pick = preferVideoToolbox && videoToolbox != null
                ? videoToolbox : (software != null ? software : hardware);
        return pick == null ? null : pick.getName();
    }

    private VideoCodec[] enumerate(boolean encoder) {
        List<VideoCodec> out = new ArrayList<VideoCodec>();
        try {
            List<String> command = new ArrayList<String>();
            command.add(FFMPEGSupport.ffmpeg());
            command.add("-hide_banner");
            command.add(encoder ? "-encoders" : "-decoders");
            String text = FFMPEGSupport.runCaptureMerged(command);
            boolean body = false;
            for (String line : text.split("\n")) {
                String trimmed = line.trim();
                if (!body) {
                    if (trimmed.startsWith("------")) {
                        body = true;
                    }
                    continue;
                }
                if (trimmed.isEmpty()) {
                    continue;
                }
                int sp = trimmed.indexOf(' ');
                if (sp < 0) {
                    continue;
                }
                String flags = trimmed.substring(0, sp);
                String rest = trimmed.substring(sp).trim();
                int sp2 = rest.indexOf(' ');
                String name = sp2 < 0 ? rest : rest.substring(0, sp2);
                char type = flags.charAt(0);
                boolean video = type == 'V';
                boolean audio = type == 'A';
                if (!video && !audio) {
                    continue;
                }
                String id = canonicalId(name, video);
                if (id == null) {
                    continue;
                }
                out.add(new VideoCodec(id, name, null, video,
                        encoder, !encoder, isHardware(name), -1, -1, new String[0]));
            }
        } catch (Exception ex) {
            // ffmpeg missing or unparsable; return whatever we collected
        }
        return out.toArray(new VideoCodec[out.size()]);
    }

    private static boolean isHardware(String name) {
        String n = name.toLowerCase();
        for (String m : HARDWARE_MARKERS) {
            if (n.contains(m)) {
                return true;
            }
        }
        return false;
    }

    private static String canonicalId(String name, boolean video) {
        String n = name.toLowerCase();
        if (video) {
            if (n.contains("264") || n.contains("avc")) {
                return CODEC_H264;
            }
            if (n.contains("hevc") || n.contains("265")) {
                return CODEC_HEVC;
            }
            if (n.contains("vp9")) {
                return CODEC_VP9;
            }
            if (n.contains("vp8") || n.equals("libvpx")) {
                return CODEC_VP8;
            }
            if (n.contains("av1")) {
                return CODEC_AV1;
            }
            return null;
        }
        if (n.equals("aac") || n.contains("aac")) {
            return CODEC_AAC;
        }
        if (n.contains("opus")) {
            return CODEC_OPUS;
        }
        if (n.startsWith("pcm_s16")) {
            return CODEC_PCM;
        }
        return null;
    }

    /// Maps a container id to the ffmpeg muxer format name.
    static String muxerFormat(String container) {
        if (VideoIO.CONTAINER_MKV.equals(container)) {
            return "matroska";
        }
        if (container == null) {
            return VideoIO.CONTAINER_MP4;
        }
        return container;
    }

    /// Maps an audio codec id to the ffmpeg encoder name (the native ffmpeg encoders
    /// {@code aac}, {@code libopus}/{@code opus} and {@code pcm_s16le} are used).
    static String audioEncoderName(String codecId, List<VideoCodec> available) {
        if (VideoIO.CODEC_OPUS.equals(codecId)) {
            return containsName(available, "libopus") ? "libopus" : "opus";
        }
        if (VideoIO.CODEC_PCM.equals(codecId)) {
            return "pcm_s16le";
        }
        return "aac";
    }

    private static boolean containsName(List<VideoCodec> list, String name) {
        for (VideoCodec c : list) {
            if (name.equals(c.getName())) {
                return true;
            }
        }
        return false;
    }

    List<VideoCodec> encoderList() {
        return Arrays.asList(getAvailableEncoders());
    }
}
