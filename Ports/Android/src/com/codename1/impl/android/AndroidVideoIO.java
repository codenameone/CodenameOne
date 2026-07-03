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
package com.codename1.impl.android;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import com.codename1.media.VideoCodec;
import com.codename1.media.VideoIO;
import com.codename1.media.VideoReader;
import com.codename1.media.VideoWriter;
import com.codename1.media.VideoWriterBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Android `com.codename1.media.VideoIO` implementation built entirely on the platform
/// media stack: `MediaCodec` + `MediaMuxer` for encoding, `MediaExtractor` +
/// `MediaCodec` (and `MediaMetadataRetriever` for single frame seeks) for decoding, and
/// `MediaCodecList` for codec enumeration. Requires API level 21+.
class AndroidVideoIO extends VideoIO {
    static final String MIME_H264 = "video/avc";
    static final String MIME_HEVC = "video/hevc";
    static final String MIME_VP8 = "video/x-vnd.on2.vp8";
    static final String MIME_VP9 = "video/x-vnd.on2.vp9";
    static final String MIME_AV1 = "video/av01";
    static final String MIME_AAC = "audio/mp4a-latm";
    static final String MIME_OPUS = "audio/opus";
    static final String MIME_PCM = "audio/raw";

    private VideoCodec[] encoders;
    private VideoCodec[] decoders;

    @Override
    public synchronized VideoCodec[] getAvailableEncoders() {
        if (encoders == null) {
            enumerate();
        }
        return encoders.clone();
    }

    @Override
    public synchronized VideoCodec[] getAvailableDecoders() {
        if (decoders == null) {
            enumerate();
        }
        return decoders.clone();
    }

    @Override
    public VideoWriter createWriter(VideoWriterBuilder cfg) throws IOException {
        return new AndroidVideoWriter(cfg);
    }

    @Override
    public VideoReader openReader(String filePath) throws IOException {
        return new AndroidVideoReader(filePath);
    }

    /// Maps a Codename One codec id to the Android media mime type, or null if unknown.
    static String mimeForCodec(String codecId) {
        if (CODEC_H264.equals(codecId)) {
            return MIME_H264;
        }
        if (CODEC_HEVC.equals(codecId)) {
            return MIME_HEVC;
        }
        if (CODEC_VP8.equals(codecId)) {
            return MIME_VP8;
        }
        if (CODEC_VP9.equals(codecId)) {
            return MIME_VP9;
        }
        if (CODEC_AV1.equals(codecId)) {
            return MIME_AV1;
        }
        if (CODEC_AAC.equals(codecId)) {
            return MIME_AAC;
        }
        if (CODEC_OPUS.equals(codecId)) {
            return MIME_OPUS;
        }
        if (CODEC_PCM.equals(codecId)) {
            return MIME_PCM;
        }
        return null;
    }

    private static String codecForMime(String mime) {
        if (mime == null) {
            return null;
        }
        String m = mime.toLowerCase();
        if (m.equals(MIME_H264)) {
            return CODEC_H264;
        }
        if (m.equals(MIME_HEVC)) {
            return CODEC_HEVC;
        }
        if (m.equals(MIME_VP8)) {
            return CODEC_VP8;
        }
        if (m.equals(MIME_VP9)) {
            return CODEC_VP9;
        }
        if (m.equals(MIME_AV1)) {
            return CODEC_AV1;
        }
        if (m.equals(MIME_AAC)) {
            return CODEC_AAC;
        }
        if (m.equals(MIME_OPUS)) {
            return CODEC_OPUS;
        }
        if (m.startsWith("audio/raw")) {
            return CODEC_PCM;
        }
        return null;
    }

    private synchronized void enumerate() {
        List<VideoCodec> enc = new ArrayList<VideoCodec>();
        List<VideoCodec> dec = new ArrayList<VideoCodec>();
        try {
            MediaCodecList list = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
            for (MediaCodecInfo info : list.getCodecInfos()) {
                String name = info.getName();
                boolean isEncoder = info.isEncoder();
                boolean hardware = !isSoftwareName(name);
                for (String type : info.getSupportedTypes()) {
                    String id = codecForMime(type);
                    if (id == null) {
                        continue;
                    }
                    boolean video = type.toLowerCase().startsWith("video/");
                    int maxW = -1;
                    int maxH = -1;
                    if (video) {
                        try {
                            MediaCodecInfo.VideoCapabilities vc =
                                    info.getCapabilitiesForType(type).getVideoCapabilities();
                            if (vc != null) {
                                maxW = vc.getSupportedWidths().getUpper();
                                maxH = vc.getSupportedHeights().getUpper();
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                    VideoCodec c = new VideoCodec(id, name, type, video, isEncoder, !isEncoder,
                            hardware, maxW, maxH, new String[]{CONTAINER_MP4});
                    (isEncoder ? enc : dec).add(c);
                }
            }
        } catch (Throwable ignored) {
        }
        encoders = enc.toArray(new VideoCodec[enc.size()]);
        decoders = dec.toArray(new VideoCodec[dec.size()]);
    }

    /// API 29 exposes `MediaCodecInfo#isHardwareAccelerated()`, but the port compiles
    /// against an older platform jar, so we infer "software" from the well known Google /
    /// Codec2 software codec name prefixes instead.
    private static boolean isSoftwareName(String name) {
        if (name == null) {
            return true;
        }
        String n = name.toLowerCase();
        return n.startsWith("omx.google.") || n.startsWith("c2.android.") || n.contains(".sw.");
    }
}
