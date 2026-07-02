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
package com.codename1.media;

/// Describes a single codec made available by the underlying platform, as returned by
/// `VideoIO#getAvailableEncoders()` and `VideoIO#getAvailableDecoders()`. This lets an
/// application discover, at runtime, which codecs the device can actually use and pick
/// the best one for its needs.
///
/// Instances are immutable value objects. Capability fields that the platform cannot
/// report are returned as `-1` (integers) or empty arrays.
///
/// @author Shai Almog
///
/// #### Since
///
/// 8.0
public final class VideoCodec {
    private final String id;
    private final String name;
    private final String mimeType;
    private final boolean video;
    private final boolean encoder;
    private final boolean decoder;
    private final boolean hardwareAccelerated;
    private final int maxWidth;
    private final int maxHeight;
    private final String[] supportedContainers;

    /// Creates a codec descriptor. Normally created by platform implementations rather
    /// than application code.
    ///
    /// #### Parameters
    ///
    /// - `id`: the stable identifier of the codec, e.g. one of the `VideoIO#CODEC_H264` constants
    ///
    /// - `name`: a human readable name for display
    ///
    /// - `mimeType`: the codec mime type when known (may be null)
    ///
    /// - `video`: true for a video codec, false for an audio codec
    ///
    /// - `encoder`: true if this codec can encode
    ///
    /// - `decoder`: true if this codec can decode
    ///
    /// - `hardwareAccelerated`: true if the codec is backed by dedicated hardware
    ///
    /// - `maxWidth`: the maximum supported width in pixels, or -1 if unknown
    ///
    /// - `maxHeight`: the maximum supported height in pixels, or -1 if unknown
    ///
    /// - `supportedContainers`: the container formats this codec can be muxed into (may be empty)
    public VideoCodec(String id, String name, String mimeType, boolean video,
            boolean encoder, boolean decoder, boolean hardwareAccelerated,
            int maxWidth, int maxHeight, String[] supportedContainers) {
        this.id = id;
        this.name = name;
        this.mimeType = mimeType;
        this.video = video;
        this.encoder = encoder;
        this.decoder = decoder;
        this.hardwareAccelerated = hardwareAccelerated;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.supportedContainers = supportedContainers == null ? new String[0] : supportedContainers;
    }

    /// The stable identifier of this codec, e.g. `VideoIO#CODEC_H264`. Use this value
    /// when configuring a `VideoWriterBuilder`.
    public String getId() {
        return id;
    }

    /// A human readable name for this codec, suitable for display in a picker.
    public String getName() {
        return name;
    }

    /// The codec mime type when the platform reports one, otherwise null.
    public String getMimeType() {
        return mimeType;
    }

    /// True for a video codec, false for an audio codec.
    public boolean isVideo() {
        return video;
    }

    /// True if this codec can be used for encoding.
    public boolean isEncoder() {
        return encoder;
    }

    /// True if this codec can be used for decoding.
    public boolean isDecoder() {
        return decoder;
    }

    /// True if this codec is backed by dedicated hardware (and therefore typically much
    /// faster and more power efficient than a software codec).
    public boolean isHardwareAccelerated() {
        return hardwareAccelerated;
    }

    /// The maximum width in pixels supported by this codec, or -1 if the platform does
    /// not report a limit.
    public int getMaxWidth() {
        return maxWidth;
    }

    /// The maximum height in pixels supported by this codec, or -1 if the platform does
    /// not report a limit.
    public int getMaxHeight() {
        return maxHeight;
    }

    /// The container formats (e.g. `VideoIO#CONTAINER_MP4`) this codec can be muxed
    /// into. May be empty when unknown.
    public String[] getSupportedContainers() {
        return supportedContainers;
    }

    @Override
    public String toString() {
        return (video ? "video" : "audio") + " codec " + id + " ("
                + (encoder ? "E" : "") + (decoder ? "D" : "")
                + (hardwareAccelerated ? " hw" : "") + ")";
    }
}
